#!/usr/bin/env python3
"""Source-verified GEO platform annotation eligibility for cross-platform analysis.

Authentic NCBI annotation records ONLY. This inventory never claims that
equivalent genes imply comparable expression values or independent replication.
"""
import gzip
import hashlib
import json
import re
import sys
import urllib.request
from pathlib import Path

PLATFORMS = ("GPL571", "GPL96", "GPL97")
MAX_FILE_BYTES = 30_000_000
MAX_ROWS = 150000
MAX_COLUMNS = 120
MAX_UNCOMPRESSED_BYTES = 150_000_000


def fetch_annotation(gpl, directory):
    number = gpl[3:]
    stub = "GPL" + number[:-3] + "nnn" if len(number) > 3 else "GPLnnn"
    url = (f"https://ftp.ncbi.nlm.nih.gov/geo/platforms/{stub}/"
           f"{gpl}/annot/{gpl}.annot.gz")
    target = directory / f"{gpl}.annot.gz"
    digest = hashlib.sha256()
    total = 0
    request = urllib.request.Request(
        url, headers={"User-Agent": "BIOQRA-reproducible-public-platform-annotation/1.0"})
    with urllib.request.urlopen(request, timeout=45) as response:
        if response.status != 200 or response.geturl() != url:
            raise ValueError("Unexpected remote URL or HTTP response")
        with target.open("wb") as dest:
            while True:
                piece = response.read(16384)
                if not piece:
                    break
                total += len(piece)
                if total > MAX_FILE_BYTES:
                    raise ValueError("Annotation larger than bounded file limit")
                dest.write(piece)
                digest.update(piece)
    with target.open("rb") as check:
        if check.read(2) != b"\x1f\x8b":
            raise ValueError("Not a valid gzip header")
    return target, url, total, digest.hexdigest()


def gene_tokens(field):
    # Gene IDs can contain multiple mappings. Do not silently force a
    # one-to-many probe mapping into a single gene.
    value = field.strip().strip('"')
    if not value or value.lower() in ("na", "null", "---", "none"):
        return ()
    tokens = re.split(r"\s*(?://+|;|,|\|)\s*", value)
    if not tokens or not all(re.fullmatch(r"[1-9][0-9]{0,11}", token) for token in tokens):
        return ()
    return tuple(dict.fromkeys(tokens))


def read_annotation(file, expected):
    # NCBI-curated GEO annotations contain a table with ID and Entrez Gene ID.
    header = None
    gene_index = None
    id_index = None
    in_table = False
    file_size = 0
    rows = 0
    direct = {}
    ambiguous = 0
    missing = 0
    with gzip.open(file, "rt", encoding="utf-8", errors="replace") as src:
        for line in src:
            file_size += len(line)
            if file_size > MAX_UNCOMPRESSED_BYTES:
                raise ValueError("Uncompressed annotation data limit exceeded")
            trimmed = line.rstrip("\r\n")
            if trimmed.startswith("!annotation_table_begin") or trimmed.startswith("!platform_table_begin"):
                in_table = True
                continue
            if trimmed.startswith("!annotation_table_end") or trimmed.startswith("!platform_table_end"):
                break
            if not in_table:
                continue
            if header is None:
                header = trimmed.split("\t")
                if len(header) > MAX_COLUMNS:
                    raise ValueError("Unexpected number of annotation columns")
                names = [x.strip().lower().replace("_", " ") for x in header]
                id_index = next((i for i, n in enumerate(names) if n == "id"), None)
                gene_index = next((i for i, n in enumerate(names)
                                   if n in ("gene id", "entrez gene", "entrez gene id", "gene id (entrez)")), None)
                if id_index is None or gene_index is None:
                    raise ValueError("No unambiguous probe ID and Entrez Gene ID columns")
                continue
            rows += 1
            if rows > MAX_ROWS:
                raise ValueError("Annotation exceeds maximum probe rows")
            fields = trimmed.split("\t")
            if len(fields) != len(header):
                missing += 1
                continue
            probe = fields[id_index].strip().strip('"')
            if not probe or probe in direct:
                ambiguous += 1
                if probe in direct:
                    del direct[probe]
                continue
            genes = gene_tokens(fields[gene_index])
            if len(genes) != 1:
                if len(genes) > 1:
                    ambiguous += 1
                else:
                    missing += 1
                continue
            direct[probe] = genes[0]
    if header is None or rows == 0:
        raise ValueError("No annotation table found")
    return {
        "platform": expected, "rows": rows, "single_gene_probe_count": len(direct),
        "ambiguous_probe_rows": ambiguous, "unannotated_or_incomplete_rows": missing,
        "annotation_header": header,
        "unique_gene_count": len(set(direct.values())),
    }, set(direct.values())


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else "validation-results/platform-annotations")
    root.mkdir(parents=True, exist_ok=True)
    results = []
    genes = {}
    for gpl in PLATFORMS:
        try:
            file, url, size, sha = fetch_annotation(gpl, root)
            result, resolved_genes = read_annotation(file, gpl)
            result.update(url=url, compressed_bytes=size, sha256=sha, status="ANNOTATION_VERIFIED")
            genes[gpl] = resolved_genes
        except Exception as exc:
            result = {"platform": gpl, "status": "UNAVAILABLE_OR_UNSUITABLE",
                      "reason": str(exc)}
        results.append(result)
    overlap = []
    for gpl in ("GPL96", "GPL97"):
        if "GPL571" in genes and gpl in genes:
            common = genes["GPL571"] & genes[gpl]
            overlap.append({"platforms": ["GPL571", gpl],
                            "shared_entrez_gene_ids": len(common),
                            "status": "GENE_ANNOTATION_OVERLAP_ONLY"})
    report = {
        "source": "NCBI GEO curated platform annotation archives",
        "platforms": results, "gene_annotation_overlap": overlap,
        "expression_replication_status": "NOT_TESTED",
        "clinical_validation": False, "challenge_solved": False,
        "reason": "Gene annotation overlap does not establish platform comparability, identical probe measurement, patient independence or replicated tumor effects. A prespecified gene-level workflow and independent validation are still required.",
    }
    path = root / "annotation-eligibility.json"
    path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))
    if not any(r["status"] == "ANNOTATION_VERIFIED" for r in results):
        raise RuntimeError("No official GPL annotation was verified")


if __name__ == "__main__":
    main()
