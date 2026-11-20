#!/usr/bin/env python3
"""Numerical software regression tests only; never biological study data."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from geo_external_gene_retest import bh_adjust, valid_number

def run():
    q = bh_adjust([0.20, 0.01, 0.03, 0.02])
    assert len(q) == 4
    for got, ref in zip(q, [0.20, 0.04, 0.04, 0.04]):
        assert abs(got - ref) < 1e-12, (q, ref)
    assert bh_adjust([]) == []
    assert bh_adjust([1, 1]) == [1, 1]
    assert valid_number("nan") is None
    assert valid_number("inf") is None
    assert valid_number("0.02") == 0.02
    print("Frozen-gene external BH/number tests PASSED — numerical fixtures only")

if __name__ == "__main__":
    run()
