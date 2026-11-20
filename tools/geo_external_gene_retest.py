#!/usr/bin/env python3
"""Prespecified real gene-level GEO external retest (GSE20347 -> GSE23400).

This computes descriptive same-direction expression contrasts and BH-adjusted
p-values only after original GEO provenance and independent R/limma runs exist.
Do not infer independent donor samples or clinical validity from study IDs.
"""
import csv
import json
import math
import sys
from collections import Counter
from pathlib import Path

from geo_frozen_gene_candidates import platform_map

MAX_PROBES = 150000


def valid_number(value):
    try:
        n = float(value)
        if math.isfinite(n):
            return n
    except (ValueError, TypeError):
        pass
    return None


def external_results(file, platform, annotations):
    if not file.is_file():
        return None, "External limma probe result file not generated"
    gene_frequency = Counter(annotations.values())
    results = {}
    with file.open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        if not {"probe_id", "logFC", "P.Value"}.issubset(reader.fieldnames or []):
            return None, "Missing original limma probe IDs/effects/p values"
        for i, row in enumerate(reader):
            if i >= MAX_PROBES:
                return None, "External limma output exceeds bounded scan"
            probe = row["probe_id"]
            gene = annotations.get(probe)
            if gene is None or gene_frequency[gene] != 1:
                continue
            effect = valid_number(row["logFC"])
            p = valid_number(row["P.Value"])
            if effect is None or p is None or not 0 <= p <= 1:
                continue
            if gene in results:
                return None, "Unexpected duplicate Entrez gene after one-to-one mapping"
            results[gene] = (effect, p, probe)
    return results, None


def bh_adjust(values):
    indexed = sorted(enumerate(values), key=lambda x: x[1])
    q = [1.0] * len(values)
    previous = 1.0
    for rank in range(len(indexed) - 1, -1, -1):
        index, p = indexed[rank]
        previous = min(previous, min(1.0, p * len(values) / (rank + 1)))
        q[index] = previous
    return q


def main():
    if len(sys.argv) != 5:
        raise SystemExit("Usage: tool FROZEN_TSV PLATFORM_ANNOTATION_DIR EXTERNAL_LIMMA_DIR OUTPUT_DIR")
    frozen_path, annotation_dir, source_dir, out_dir = map(Path, sys.argv[1:])
    out_dir.mkdir(parents=True, exist_ok=True)
    with frozen_path.open(newline="", encoding="utf-8") as f:
        frozen = list(csv.DictReader(f, delimiter="\t"))
    if not frozen or len(frozen) > 10000:
        raise ValueError("Frozen prespecified discovery gene candidate list absent or oversized")
    if len({row["entrez_gene_id"] for row in frozen}) != len(frozen):
        raise ValueError("Duplicated frozen discovery gene IDs")
    summary = {"discovery": "GSE20347", "external": "GSE23400",
               "frozen_candidates": len(frozen), "source": "Real GEO/NCBI + Bioconductor limma",
               "platforms": [], "clinical_validation": False,
               "independent_donor_validation": False,
               "challenge_solved": False}
    for platform in ("GPL96", "GPL97"):
        audit_file = source_dir / platform / "limma-audit.json"
        audit = json.loads(audit_file.read_text(encoding="utf-8")) if audit_file.is_file() else {}
        status = audit.get("status", "NOT_RUN")
        record = {"platform": platform, "limma_status": status,
                  "status": "NOT_EVALUATED", "reason": "External model absent or ineligible"}
        if status != "EXPLORATORY_ANALYSIS_COMPLETED":
            record["reason"] = audit.get("reason", record["reason"])
            summary["platforms"].append(record)
            continue
        if audit.get("design") != "paired":
            record["reason"] = "Source cohort model is not independently verified paired design"
            summary["platforms"].append(record)
            continue
        map_file = annotation_dir / f"{platform}.annot.gz"
        annotations = platform_map(map_file)
        measurements, error = external_results(
            source_dir / platform / "limma-probe-results.csv", platform, annotations
        )
        if error:
            record["reason"] = error
            summary["platforms"].append(record)
            continue
        tested = []
        for candidate in frozen:
            values = measurements.get(candidate["entrez_gene_id"])
            if values is None:
                continue
            discovery_effect = valid_number(candidate["discovery_log2_effect"])
            if discovery_effect is None:
                continue
            effect, p, probe = values
            tested.append({"entrez_gene_id": candidate["entrez_gene_id"],
                           "discovery_effect": discovery_effect, "external_effect": effect,
                           "external_p": p, "external_probe": probe})
        q = bh_adjust([item["external_p"] for item in tested])
        concordant = 0
        fdr_signals = 0
        for item, value in zip(tested, q):
            same_direction = (
                item["discovery_effect"] * item["external_effect"] > 0
            )
            item["same_direction"] = same_direction
            item["external_candidate_set_bh_fdr"] = value
            if same_direction:
                concordant += 1
                if value < 0.05:
                    fdr_signals += 1
        result_file = out_dir / f"{platform}-frozen-gene-retest.tsv"
        with result_file.open("w", newline="", encoding="utf-8") as f:
            writer = csv.writer(f, delimiter="\t")
            writer.writerow(["entrez_gene_id", "discovery_effect", "external_effect",
                             "external_p", "external_candidate_set_bh_fdr",
                             "same_direction", "external_probe"])
            for row in tested:
                writer.writerow([row[key] for key in
                    ("entrez_gene_id", "discovery_effect", "external_effect",
                     "external_p", "external_candidate_set_bh_fdr",
                     "same_direction", "external_probe")])
        record.update(status="EXPLORATORY_RETEST_COMPUTED",
                      reason="Frozen discovery genes retested on real external paired GEO data",
                      genes_tested=len(tested),
                      direction_concordant_genes=concordant,
                      same_direction_and_external_fdr_under_0_05=fdr_signals,
                      statistical_method="External limma paired effects; BH correction across frozen mapped candidate genes on this GPL",
                      independent_replication_proven=False,
                      donor_independence_unverified=True)
        summary["platforms"].append(record)
    summary["interpretation"] = (
        "Signal replication is exploratory: GPL96 and GPL97 originate from the same "
        "GSE23400 study and must not be counted as independent external cohorts. "
        "Cross-study processing, donor overlap and probe mapping require review."
    )
    (out_dir / "external-gene-retest.json").write_text(
        json.dumps(summary, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
