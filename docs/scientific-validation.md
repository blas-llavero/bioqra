# BIOQRA — Reproducible scientific analysis checks

## What this workflow verifies

BIOQRA's Android statistics use authentic GEO files, checksums and conservative case/control classifications. The `Android Debug APK` workflow runs Java/JUnit mathematical regression tests and a bounded public GEO file-format check. Those checks **do not** establish differential gene expression, treatment efficacy, or that a challenge has been solved.

The separate `Independent GEO / limma scientific audit` GitHub Actions workflow:

1. Installs free R, Bioconductor **limma**, and `jsonlite`.
2. Downloads the public **GSE17351** GEO Series Matrix directly from NCBI, up to 8 MB.
3. Stores its accession, official URL, SHA-256 and byte count in `source-provenance.json`.
4. Inspects original GSM accessions, phenotypes, processing description and GPL platform.
5. **Skips** differential expression if groups are ambiguous, fewer than three samples per group are explicitly labelled, paired/donor/batch/time/treatment metadata appear, or logged and normalized measurements are not documented.
6. If the study qualifies, applies a two-group limma model, uses BH-adjusted p-values, and saves probe-level results as `limma-probe-results.csv`.

The `bioqra-independent-geo-audit` Actions artifact includes an audit status (JSON or text), package versions, provenance and probe-level results **only if actually computed**.

## Scientific limitations

- Being a real public dataset does not mean data are compatible with the scientific question.
- The Android **Welch test** and Bioconductor **limma's moderated linear model** are different methods; they need not have identical p-values. Compare cohort definitions, sample ordering and effect directions; do not assert matching p-values.
- GSE17351 is used first as a **real-world ingestion and eligibility test**, not a verified independent replication cohort.
- Matching GPL accessions or disjoint GSM IDs alone does not prove donor-level independence or control of biological and technical confounding.
- A `SKIPPED` audit is an appropriate result when the design metadata are insufficient; do not weaken checks to force apparently significant results.
- No scientific or clinical claims should be based on exploratory data alone.

## Run locally

With **R** and free Bioconductor limma installed:

```bash
python3 tools/geo_fetch_for_r.py --accession GSE17351 --out validation-input
Rscript tools/geo_limma_audit.R validation-input/GSE17351_series_matrix.txt.gz validation-results
```

For mathematical-only Java tests (no biological data involved):

```bash
gradle :app:testDebugUnitTest --no-daemon
```

## Next scientific milestones

A truly externally validated finding requires a fully documented biological hypothesis, prespecified model, correctly labelled independent patients, study-specific normalization, covariate/batch or pairing adjustments where relevant, harmonized gene/probe annotation, and independent replication with full result reporting. BIOQRA must show these requirements as **pending** until verified.
