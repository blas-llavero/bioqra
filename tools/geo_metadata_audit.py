#!/usr/bin/env python3
"""Inspect real GEO metadata structure without inventing sample classes.

Produces a machine-readable overview of which fields exist and whether sample
titles contain potentially useful labels. It does NOT assign tumor/control or
patient IDs and does NOT reproduce identifiers in the public audit report.
"""
import gzip
import json
from collections import Counter
from pathlib import Path
import re
import sys

if len(sys.argv) != 3:
    raise SystemExit("Usage: python3 tools/geo_metadata_audit.py MATRIX.gz REPORT.json")
path, output = map(Path,sys.argv[1:])
if not path.is_file() or path.stat().st_size>32_000_000:
    raise SystemExit("Input missing or exceeds limit")
prefixes=("!Sample_title","!Sample_source_name_ch1","!Sample_characteristics_ch1",
          "!Sample_geo_accession","!Series_platform_id","!Sample_platform_id",
          "!Series_data_processing","!Sample_data_processing")
metadata={}
with gzip.open(path,"rt",encoding="utf-8",errors="replace") as input:
    for n,line in enumerate(input):
        if n>30000:
            raise SystemExit("Exceeded metadata line safety limit")
        if line.startswith("!series_matrix_table_begin"):
            break
        for prefix in prefixes:
            if line.startswith(prefix+"\t"):
                fields=line.rstrip("\n").split("\t")[1:]
                metadata.setdefault(prefix,[]).append([v.strip().strip('"') for v in fields])
                break
def shape(key):
    return [len(v) for v in metadata.get(key,[])]
def summary_counts(key):
    # Count exact common label vocabulary only. Never output patient IDs, names,
    # tissue titles or raw donor-linked metadata.
    counter=Counter()
    for row in metadata.get(key,[]):
        for text in row:
            val=text.lower()
            for tag in ("normal","control","tumor","tumour","cancer","carcinoma","matched",
                        "adjacent","healthy","patient","subject","donor"):
                if re.search(r"\b"+tag+r"\b",val):
                    counter[tag]+=1
    return dict(counter)
def generalized_templates(key):
    out=[]
    for row in metadata.get(key,[]):
        for text in row[:500]:
            # Remove possible names/donor IDs and any numbers. Output only a
            # controlled lexical grammar of source-title tokens, no raw titles.
            words=[]
            for w in re.findall(r"[A-Za-z]+|[0-9]+",text.lower()):
                if w.isdigit():
                    words.append("<number>")
                elif w in {"patient","tumor","tumour","normal","adjacent","esophageal",
                           "squamous","cell","carcinoma","cancer","tissue","sample",
                           "of","from","matched","case","control","non","neoplastic"}:
                    words.append(w)
                else:
                    words.append("<other>")
            template=" ".join(words[:15])
            if template not in out:out.append(template)
    return out[:8]

report={
    "real_input":path.name,
    "fictional_data_used":False,
    "sample_accession_counts":shape("!Sample_geo_accession"),
    "sample_title_columns":shape("!Sample_title"),
    "sample_source_columns":shape("!Sample_source_name_ch1"),
    "characteristics_row_sizes":shape("!Sample_characteristics_ch1"),
    "sample_title_label_word_counts":summary_counts("!Sample_title"),
    "sample_title_grammar_templates":generalized_templates("!Sample_title"),
    "sample_source_label_word_counts":summary_counts("!Sample_source_name_ch1"),
    "sample_source_grammar_templates":generalized_templates("!Sample_source_name_ch1"),
    "sample_characteristics_label_word_counts":summary_counts("!Sample_characteristics_ch1"),
    "platform_fields_present":bool(metadata.get("!Series_platform_id") or metadata.get("!Sample_platform_id")),
    "processing_fields_present":bool(metadata.get("!Series_data_processing") or metadata.get("!Sample_data_processing")),
    "automatic_pairing_resolved":False,
    "clinical_validation":False,
    "interpretation":"Metadata inventory only; do not infer patient identity or group label from sample order.",
}
output.parent.mkdir(parents=True,exist_ok=True)
output.write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
print(json.dumps(report,indent=2))
