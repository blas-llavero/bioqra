# BIOQRA Challenge Catalog — first milestone

The first release is a **curated offline snapshot**, not a live scraper. Data is stored in `data/challenges.json`.

## Verified sources

- [Rosalind problem list](https://rosalind.info/problems/list-view/): 284 problems shown on the public list at the time of research. BIOQRA selects 12 starter problems.
- [Open Problems benchmarks](https://www.openproblems.bio/benchmarks/): 14 single-cell benchmarks listed at the time of research. Some may be under development. Check source pages for current status.

## Run locally

Requires Python 3.10+ and no external Python packages or paid APIs.

```bash
python tools/list_challenges.py
python tools/list_challenges.py --source Rosalind
python tools/list_challenges.py --search DNA
python tools/list_challenges.py --kind benchmark --json
python -m unittest discover -s tests -v
```

## Interpretation

- `not_attempted` means BIOQRA has **not** solved or scientifically validated the challenge.
- Educational exercises are separate from research benchmarks.
- The catalog contains links to the original challenge sources. Descriptions and live benchmark status must be checked on the official sites.
- Later milestones: refreshable discovery, reproducible solver implementations, test fixtures, result/provenance tracking, and a web/Android interface.
- Patient data must never be uploaded to or processed by this module.
