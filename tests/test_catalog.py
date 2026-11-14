import json
import unittest
from pathlib import Path
from tools.list_challenges import filter_entries

CATALOG = Path(__file__).resolve().parents[1] / "data" / "challenges.json"

class CatalogTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.entries = json.loads(CATALOG.read_text(encoding="utf-8"))["entries"]

    def test_identifiers_are_unique(self):
        ids = [entry["id"] for entry in self.entries]
        self.assertEqual(len(ids), len(set(ids)))

    def test_all_entries_have_provenance(self):
        for entry in self.entries:
            self.assertTrue(entry["source_url"].startswith("https://"))
            self.assertEqual(entry["status"], "not_attempted")

    def test_filters(self):
        self.assertEqual(len(filter_entries(self.entries, source="Rosalind")), 12)
        self.assertEqual(len(filter_entries(self.entries, kind="benchmark")), 14)
        self.assertTrue(all("dna" in x["title"].lower() or "dna" in x["category"].lower()
                            for x in filter_entries(self.entries, search="dna")))

if __name__ == "__main__":
    unittest.main()
