#!/usr/bin/env python3
"""Read-only, bounded check of REAL public NCBI GEO data, never synthetic science.

Run: python3 tools/geo_real_data_smoke.py
A failure means the exact live source could not be verified here, not that the
cancer research hypothesis was disproven. No patient records are stored.
"""
import gzip
import hashlib
import io
import json
import urllib.request

ACCESSION = "GSE17351"  # Published public ESCC study (not clinical validation).
URL = ("https://ftp.ncbi.nlm.nih.gov/geo/series/GSE17nnn/"
       "GSE17351/matrix/GSE17351_series_matrix.txt.gz")
MAX_BYTES = 8_000_000

def main():
    req = urllib.request.Request(
        URL, headers={"User-Agent": "BIOQRA-public-GEO-reproducibility-check/1.0"})
    with urllib.request.urlopen(req, timeout=25) as response:
        if response.geturl() != URL:
            raise RuntimeError("Unexpected GEO redirect")
        buf = io.BytesIO()
        while True:
            part = response.read(32768)
            if not part:
                break
            if buf.tell() + len(part) > MAX_BYTES:
                raise RuntimeError("GEO download exceeds 8 MB Android safeguard")
            buf.write(part)
    source = buf.getvalue()
    if source[:2] != b"\x1f\x8b":
        raise RuntimeError("GEO response is not gzip data")
    sample_ids = []
    matrix = False
    with gzip.open(io.BytesIO(source), "rt", encoding="utf-8") as data:
        for idx, line in enumerate(data):
            if idx > 100000:
                raise RuntimeError("GEO matrix exceeded bounded scan")
            if line.startswith("!Sample_geo_accession"):
                sample_ids = [x.strip('"') for x in line.rstrip("\n").split("\t")[1:]]
            if line.strip() == "!series_matrix_table_begin":
                matrix = True
                break
    if not matrix or not sample_ids or any(not x.startswith("GSM") for x in sample_ids):
        raise RuntimeError("GEO study metadata is not in expected Series Matrix format")
    print(json.dumps({
        "source": URL, "accession": ACCESSION, "download_bytes": len(source),
        "sha256": hashlib.sha256(source).hexdigest(),
        "GSM_samples_in_series_matrix": len(sample_ids),
        "matrix_table_detected": matrix,
        "scientific_validation": False,
        "note": "Authentic file-format smoke test only. No case-control effect was calculated.",
    }, indent=2))

if __name__ == "__main__":
    main()
