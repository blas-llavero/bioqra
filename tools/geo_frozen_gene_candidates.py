#!/usr/bin/env python3
"""Freeze gene-level discovery candidates using only real GSE20347 limma results
and official GEO platform annotation. No use of external cohort outcomes in
candidate selection. No direct comparison of GPL probe IDs.
"""
import csv
import gzip
import json
import re
import sys
from collections import Counter
from pathlib import Path

MAX_ROWS = 150000
HEADER_LIMIT = 120
PLATFORMS = ("GPL571", "GPL96", "GPL97")


def platform_map(path):
    entries = {}
    conflicts = set()
    header = None
    with gzip.open(path, "rt", encoding="utf-8", errors="replace") as stream:
        in_table = False
        for line in stream:
            line = line.rstrip("\r\n")
            if line in ("!annotation_table_begin", "!platform_table_begin"):
                in_table = True
                continue
            if line in ("!annotation_table_end", "!platform_table_end"):
                break
            if not in_table:
                continue
            if header is None:
                header = [x.strip().lower().replace("_", " ") for x in line.split("\t")]
                if len(header) > HEADER_LIMIT or "id" not in header or "gene id" not in header:
                    raise ValueError("Official probe and gene ID columns absent")
                probe_col = header.index("id")
                gene_col = header.index("gene id")
                continue
            parts = line.split("\t")
            if len(parts) != len(header):
                continue
            probe = parts[probe_col].strip().strip('"')
            gene = parts[gene_col].strip().strip('"')
            if not probe or not re.fullmatch(r"[1-9][0-9]{0,11}", gene):
                continue
            if probe in entries:
                conflicts.add(probe)
            else:
                entries[probe] = gene
            if len(entries) > MAX_ROWS:
                raise ValueError("Annotation too large")
    for probe in conflicts:
        entries.pop(probe, None)
    return entries


def read_discovery(path):
    candidates = []
    with path.open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        if not {"probe_id", "logFC", "adj.P.Val"}.issubset(reader.fieldnames or []):
            raise ValueError("Original limma probe ID/logFC/adjusted p columns missing")
        for idx, row in enumerate(reader):
            if idx > MAX_ROWS:
                raise ValueError("Too many limma probes")
            try:
                p = float(row["adj.P.Val"])
                effect = float(row["logFC"])
            except (TypeError, ValueError):
                continue
            if not (-1e5 < effect < 1e5) or not (0 <= p <= 1):
                continue
            if p < 0.05:
                candidates.append((row["probe_id"], effect, p))
    return candidates


def main():
    if len(sys.argv) != 4:
        raise SystemExit("Usage: script DISCOVERY_LIMMA_CSV ANNOTATION_DIR OUTPUT_DIR")
    limma_file, annotation_dir, output_dir = map(Path, sys.argv[1:])
    output_dir.mkdir(parents=True, exist_ok=True)
    maps = {gpl: platform_map(annotation_dir / f"{gpl}.annot.gz") for gpl in PLATFORMS}
    discovery = read_discovery(limma_file)
    gene_frequency = Counter(maps["GPL571"].values())
    eligible = []
    excluded = 0
    for probe, effect, q in discovery:
        gene = maps["GPL571"].get(probe)
        if gene is None or gene_frequency[gene] != 1:
            excluded += 1
            continue
        eligible.append((gene, probe, effect, q))
    # A candidate is frozen only from GSE20347; no validation cohort data
    # participate in selection. Replication remains untested.
    genes_used = set()
    frozen = []
    for gene, probe, effect, q in sorted(eligible, key=lambda x: (x[3], x[0])):
        if gene in genes_used:
            continue
        genes_used.add(gene)
        frozen.append({"entrez_gene_id": gene, "discovery_probe": probe,
                       "discovery_log2_effect": effect, "discovery_fdr": q})
    table = output_dir / "frozen-discovery-gene-candidates.tsv"
    with table.open("w", encoding="utf-8", newline="") as out:
        writer = csv.writer(out, delimiter="\t")
        writer.writerow(["entrez_gene_id", "discovery_probe", "discovery_log2_effect", "discovery_fdr"])
        for row in frozen:
            writer.writerow([row["entrez_gene_id"], row["discovery_probe"],
                             row["discovery_log2_effect"], row["discovery_fdr"]])
    coverage = {}
    for gpl in ("GPL96", "GPL97"):
        freq = Counter(maps[gpl].values())
        uniquely_measurable = {g for g, n in freq.items() if n == 1}
        coverage[gpl] = {
            "frozen_genes_with_exactly_one_annotated_probe": sum(
                row["entrez_gene_id"] in uniquely_measurable for row in frozen
            ),
            "eligible_probe_to_gene_pairs": len(maps[gpl]),
        }
    report = {
        "source_discovery": "GSE20347 real limma / GPL571 official GEO annotation",
        "selection_rule": "Discovery BH FDR < 0.05; one unambiguous GPL571 probe per Entrez Gene ID; no external outcomes used",
        "significant_discovery_probes": len(discovery),
        "excluded_nonunique_or_unmapped_discovery_probes": excluded,
        "frozen_gene_candidates": len(frozen),
        "external_platform_coverage": coverage,
        "external_expression_test_executed": False,
        "independent_replication": False,
        "clinical_validation": False,
        "challenge_solved": False,
        "limitations": "Coverage is based only on official gene annotation. External group labels, donor independence, processing, comparability, and validation tests must be verified before any replication claim.",
    }
    (output_dir / "frozen-gene-eligibility.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
