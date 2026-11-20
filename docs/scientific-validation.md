# BIOQRA — Reproducible scientific analysis checks

## What this workflow verifies

BIOQRA's Android statistics use authentic GEO files, checksums and conservative case/control classifications. The `Android Debug APK` workflow runs Java/JUnit mathematical regression tests and a bounded public GEO file-format check. Those checks **do not** establish differential gene expression, treatment efficacy, or that a challenge has been solved.

The separate `Independent GEO / limma scientific audit` GitHub Actions workflow:

1. Installs free R, Bioconductor **limma**, and `jsonlite`.
2. Downloads the public **GSE17351** GEO Series Matrix directly from NCBI, up to 8 MB.
3. Stores its accession, official URL, SHA-256 and byte count in `source-provenance.json`.
4. Inspects original GSM accessions, phenotypes, processing description and GPL platform.
5. **Skips** differential expression if groups are ambiguous, fewer than three samples per group are explicitly labelled, batch/time/treatment confounders appear, or logged and normalized measurements are not documented. Patient/subject/donor identifiers are allowed **only** if they form unambiguous pairs: one control and one tumor sample per patient, with at least three pairs.
6. If the study qualifies, applies either an unpaired two-group limma model or an explicit **patient fixed-effects paired model** (`~ patient + group`), uses BH-adjusted p-values, and saves probe-level results as `limma-probe-results.csv`. The report records the chosen design but does not export patient identifiers.

The `bioqra-independent-geo-audit` Actions artifact includes an audit status (JSON or text), package versions, provenance and probe-level results **only if actually computed**.

## Scientific limitations

- Being a real public dataset does not mean data are compatible with the scientific question.
- The Android **Welch test** and Bioconductor **limma's moderated linear model** are different methods; they need not have identical p-values. Compare cohort definitions, sample ordering and effect directions; do not assert matching p-values.
- GSE17351 is used first as a **real-world ingestion and eligibility test**, not a verified independent replication cohort.
- Matching GPL accessions or disjoint GSM IDs alone does not prove donor-level independence or control of biological and technical confounding.
- A `SKIPPED` audit is an appropriate result when the design metadata are insufficient; do not weaken checks to force apparently significant results.
- Pairing is never inferred from sample order or publication text. Explicit study metadata must identify each patient, one tumor and one control sample per patient. Paired design is not safe where treatment, batch or time metadata require more complex models.
- The separate `tools/test_geo_pairing.R` uses **synthetic unit-test identifiers only**, not research evidence. Real GEO matrices and scientific results remain restricted to actual public repository files.
- No scientific or clinical claims should be based on exploratory data alone.

## Run locally

With **R** and free Bioconductor limma installed:

```bash
python3 tools/geo_fetch_for_r.py --accession GSE17351 --out validation-input
Rscript tools/test_geo_pairing.R
Rscript tools/geo_limma_audit.R validation-input/GSE17351_series_matrix.txt.gz validation-results
```

For mathematical-only Java tests (no biological data involved):

```bash
gradle :app:testDebugUnitTest --no-daemon
```

## Real paired ESCC cohort: GSE20347

A second allowlisted real GEO study, **GSE20347**, contains 34 publicly deposited samples from 17 documented esophageal tumor–normal patient pairs. The original GEO sample titles specify an E-number patient code and tissue category. An accession-specific parser, `geo_title_pairing.R`, reconstructs the 17 complete pairs **by matching these explicit codes**, not by assuming pairs based on sample order.

Primary source: https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE20347

NCBI's study record documents RMA background correction and normalization across all samples on GPL571. The Bioconductor `affy::rma` manual confirms RMA output uses the log2 scale. These are the provenance checks behind the strictly accession-specific RMA processing exception in the audit; it is **not** a blanket bypass for other GEO files.

- Exact patient titles and their pairing were validated against the real downloaded GSE20347 Series Matrix.
- The matched patient fixed-effects limma model (`~ patient + tumor_vs_normal`) ran on real processed GEO measurements in the independent R workflow.
- Numerical probe-level results and BH-adjusted p-values are exported in `validation-results/GSE20347/limma-probe-results.csv` when successfully calculated.
- The audit includes library versions and original file checksums, and explicitly states that scientific and clinical validation have **not** been established.
- GSE23400 is another public candidate, but platform overlap, cohort independence, annotations, access and processing must be validated before claiming external replication.

The Android application does not yet implement this accession-specific paired model locally; the R audit executes within GitHub Actions.

## External cohort GSE23400: cross-platform gate

The separate scientific validation workflow now audits the authentic, freely available GSE23400 Series Matrix files for **GPL96** and **GPL97** and stores a SHA-256 provenance manifest if successfully retrieved. This is a candidate external cohort, **not** a completed independent replication.

The discovery study GSE20347 uses **GPL571**, so the GPL571 probe IDs and GPL96/GPL97 probe IDs are not interchangeable. BIOQRA explicitly blocks probe-level claims across platforms until we have validated versioned platform annotation, gene mapping, unique-gene aggregation rules, independent patient confirmation and comparable clinical definitions.

The audit report `validation-results/GSE23400/cross-platform-audit.json` summarizes file availability, GPL annotations, sample counts, checksums and the reason replication remains blocked. If a download fails, this is reported and is not silently replaced by simulated measurements.

**Verified GitHub Actions GSE23400 ingestion (20 November 2026):** GPL96 compressed matrix: **11,754,825 bytes and 106 GSM annotations**, SHA-256 `72d435f084731bc346804363f598721b075152d562f7f9fc247ca7f011c5fc13`; GPL97 compressed matrix: **6,111,273 bytes and 102 GSM annotations**, SHA-256 `cd1862ac2f465c4002355557ed2f96917e827513bf803a8c1a561455c60a1e2d`. These are platform-specific sample records and **not 208 independent people**. Independent tumor–normal patient pairing across these matrices remains unverified. No gene-annotation mapping or replicated effects have been calculated.

Primary source: https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE23400

## Official GEO probe-to-gene annotation availability

The CI audit downloads official, free NCBI GEO annotation files for **GPL571**, **GPL96**, and **GPL97**, recording exact archive URLs, SHA-256 hashes, table headers, counts and rejected ambiguous rows in `validation-results/platform-annotations/annotation-eligibility.json`.

Verified run (20 November 2026):

| GPL platform | Total annotation rows | Probe rows with one unambiguous Entrez Gene ID |
| --- | ---: | ---: |
| GPL571 | 22,277 | 19,931 |
| GPL96 | 22,283 | 19,933 |
| GPL97 | 22,645 | 15,961 |

The strict Entrez-ID inventories share **12,502 gene IDs** for GPL571 vs GPL96 and **4,378 gene IDs** for GPL571 vs GPL97. These counts refer to *annotation overlap only*; probe-to-gene ambiguity and many-to-one probes still require a prespecified gene-level summarization strategy. No cross-study effect replication or clinical validation has been performed.

The annotation sources are official NCBI GEO archives:

- https://ftp.ncbi.nlm.nih.gov/geo/platforms/GPLnnn/GPL571/annot/GPL571.annot.gz
- https://ftp.ncbi.nlm.nih.gov/geo/platforms/GPLnnn/GPL96/annot/GPL96.annot.gz
- https://ftp.ncbi.nlm.nih.gov/geo/platforms/GPLnnn/GPL97/annot/GPL97.annot.gz

## Frozen-gene exploratory external analysis — real cohorts

A versioned, deterministic discovery-to-external analysis now runs entirely with authentic public GEO files and Bioconductor limma. GEO **GSE20347** (GPL571, 17 verified matched patients) is the *only discovery dataset*. Its BH-FDR-selected probe hits are mapped to unique Entrez gene IDs through the original GPL571 annotations. Any discovery gene with ambiguous probe mapping is excluded before accessing the external expression results.

The resulting **3,420 discovery-frozen Entrez genes** are retested on independent study **GSE23400**. Its original sample titles and source tissues verify 53 paired patients on GPL96 and 51 paired patients on GPL97. Individual patients are not necessarily independent **between GPL96 and GPL97**, and these arrays must not be counted as separate external cohorts. Both platform comparisons use original matrices and R/limma patient fixed-effects models. GSE23400 source sample metadata document RMA normalization, which supplies the accession-specific processing justification.

The external retest uses only uniquely annotated probes for each gene. Benjamini–Hochberg correction is applied separately to all prespecified genes that can actually be assessed in each platform. The observed exploratory results on 20 November 2026 were:

| Cohort/platform | Frozen candidates tested | Same effect direction | Same direction and external BH-FDR below 0.05 |
| --- | ---: | ---: | ---: |
| GSE23400 GPL96 | 3,420 | 3,212 | 2,801 |
| GSE23400 GPL97 | 736 | 570 | 416 |

**These are exploratory statistical signals, not clinically validated biomarkers, a proven independent-donor replication, or a solved cancer challenge.** Different platforms have different numbers of evaluable probes and share a source study. Limitations include differences in tissue composition, normalization, individual identity checks across studies, batch effects, lack of preregistration and clinical outcomes, and biological validation.

Auditable outputs in the `bioqra-independent-geo-audit` GitHub Actions artifact:

- `frozen-gene-validation/frozen-discovery-gene-candidates.tsv`: discovery-only frozen gene list.
- `external-gene-retest/GPL96-frozen-gene-retest.tsv` and `GPL97-frozen-gene-retest.tsv`: real external model effects, p-values, and BH adjustments.
- `external-gene-retest/external-gene-retest.json`: summary with explicit limitations.
- `GSE20347/limma-probe-results.csv`, `GSE23400/GPL96/limma-probe-results.csv`, and `GSE23400/GPL97/limma-probe-results.csv`.
- `source-files/`: original downloaded public GEO Series Matrix files.
- `platform-annotations/`: original NCBI platform annotations and provenance.

The workflow and test fixtures are free and open source; the public scientific datasets retain their respective source terms. No generated patient records have been used as evidence.

## Next scientific milestones

A truly externally validated finding requires a fully documented biological hypothesis, prespecified model, correctly labelled independent patients, study-specific normalization, covariate/batch or pairing adjustments where relevant, harmonized gene/probe annotation, and independent replication with full result reporting. BIOQRA must show these requirements as **pending** until verified.
