#!/usr/bin/env python3
"""Fetch an exact, allowlisted REAL NCBI GEO matrix for R re-analysis.

No generated/synthetic biological data. This script never reaches arbitrary
model-supplied URLs, never authenticates, never follows redirect hosts and
limits file size. The downloaded matrix is ephemeral in GitHub Actions.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import urllib.request

MAX_BYTES = 8_000_000
ALLOWED_STUDIES = {"GSE17351"}  # Extend only after primary-source review.


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--accession", default="GSE17351")
    parser.add_argument("--out", default="validation-input")
    args = parser.parse_args()
    accession = args.accession
    if accession not in ALLOWED_STUDIES or not re.fullmatch(r"GSE[0-9]{1,9}", accession):
        raise SystemExit("Study is not allowlisted")
    digits = accession[3:]
    group = "GSE" + digits[:-3] + "nnn" if len(digits) > 3 else "GSEnnn"
    url = (f"https://ftp.ncbi.nlm.nih.gov/geo/series/{group}/"
           f"{accession}/matrix/{accession}_series_matrix.txt.gz")
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    target = out / (accession + "_series_matrix.txt.gz")
    digest = hashlib.sha256()
    size = 0
    request = urllib.request.Request(url, headers={"User-Agent": "BIOQRA-scientific-validation/1.0"})
    with urllib.request.urlopen(request, timeout=35) as response:
        if response.geturl() != url or response.status != 200:
            raise RuntimeError("Source changed or HTTP request failed")
        with target.open("wb") as file:
            while True:
                part = response.read(32768)
                if not part:
                    break
                size += len(part)
                if size > MAX_BYTES:
                    raise RuntimeError("Source exceeds bounded download limit")
                digest.update(part)
                file.write(part)
    with target.open("rb") as file:
        if file.read(2) != b"\x1f\x8b":
            raise RuntimeError("Not a valid gzip file header")
    provenance = {
        "accession": accession,
        "source_url": url,
        "sha256": digest.hexdigest(),
        "size_bytes": size,
        "data_type": "NCBI GEO real public Series Matrix",
        "synthetic_patient_data": False,
        "scientific_validation": False,
    }
    (out / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(provenance, indent=2))


if __name__ == "__main__":
    main()
