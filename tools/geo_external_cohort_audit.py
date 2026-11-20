#!/usr/bin/env python3
"""BIOQRA official GEO cross-platform cohort eligibility, no fabricated data.

The external cohort GSE23400 uses GPL96 and GPL97, while GSE20347 uses GPL571.
Never compare probes as genes without independently reviewed platform mappings.
Fetch independently published, unmodified public series matrices from NCBI.
"""
import gzip
import hashlib
import json
from pathlib import Path
import urllib.request
import sys

OFFICIAL = "https://ftp.ncbi.nlm.nih.gov/geo/series/GSE23nnn/GSE23400/matrix/"
MAX_BYTES = 10_000_000
EXPECTED = ("GSE23400-GPL96_series_matrix.txt.gz",
            "GSE23400-GPL97_series_matrix.txt.gz")


def fetch_one(name, directory):
    url = OFFICIAL + name
    req = urllib.request.Request(url, headers={"User-Agent": "BIOQRA-audited-public-GEO-study/1.0"})
    size = 0
    hash_value = hashlib.sha256()
    target = directory / name
    with urllib.request.urlopen(req, timeout=35) as response:
        if response.geturl() != url or response.status != 200:
            raise ValueError("Unexpected NCBI host, redirect, or response status")
        with target.open("wb") as out:
            while True:
                chunk = response.read(16384)
                if not chunk:
                    break
                size += len(chunk)
                if size > MAX_BYTES:
                    raise ValueError("GEO series matrix exceeds per-file audit limit")
                hash_value.update(chunk)
                out.write(chunk)
    if size < 100 or target.read_bytes()[:2] != b"\x1f\x8b":
        raise ValueError("Invalid gzip file")
    samples = []
    platform = name.split("-")[1].split("_")[0]
    metadata = []
    seen_table = False
    with gzip.open(target, "rt", encoding="utf-8") as f:
        for i, line in enumerate(f):
            if i > 35000:
                raise ValueError("Metadata exceeds bounded scan")
            if line.startswith("!Sample_geo_accession\t"):
                samples = [x.strip().strip('"') for x in line.split("\t")[1:]]
            if line.startswith(("!Sample_platform_id\t", "!Series_platform_id\t")):
                metadata.append(line.split("\t")[1:])
            if line.startswith("!series_matrix_table_begin"):
                seen_table = True
                break
    if not seen_table or not samples or not all(s.startswith("GSM") for s in samples):
        raise ValueError("Original GSM annotations or table absent")
    if any(p.strip().strip('"') != platform for row in metadata for p in row):
        raise ValueError("GEO platform metadata contradicts requested GPL")
    return {"accession": "GSE23400", "platform": platform, "url": url,
            "file": name, "sha256": hash_value.hexdigest(), "bytes": size,
            "sample_count": len(samples), "real_study": True,
            "scientific_analysis_executed": False}


def main():
    folder = Path(sys.argv[1] if len(sys.argv) > 1 else "external-cohort-input")
    folder.mkdir(parents=True, exist_ok=True)
    outputs = []
    for name in EXPECTED:
        try:
            outputs.append(fetch_one(name, folder))
        except Exception as e:
            outputs.append({"file": name, "status": "DOWNLOAD_OR_METADATA_CHECK_FAILED", "reason": str(e)})
    audit = {
        "discovery": {"accession": "GSE20347", "platform": "GPL571"},
        "external": {"accession": "GSE23400", "platforms": ["GPL96","GPL97"]},
        "files": outputs,
        "cross_platform_probe_comparison": "BLOCKED",
        "reason": "GPL571, GPL96 and GPL97 probe IDs cannot be treated as equivalent gene IDs. Validated per-platform probe-to-gene annotation and independent cohort design checks are required.",
        "independent_replication_performed": False,
        "challenge_solved": False
    }
    (folder / "cross-platform-audit.json").write_text(
        json.dumps(audit, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(audit, indent=2))
    if not any("sha256" in f for f in outputs):
        raise RuntimeError("No authentic GSE23400 matrix was downloaded")


if __name__ == "__main__":
    main()
