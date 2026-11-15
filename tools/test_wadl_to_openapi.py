"""Contract-generation checks; run with python -m unittest discover -s tools."""

import copy
import json
import unittest

from wadl_to_openapi import ROOT, convert


class WadlConversionTest(unittest.TestCase):
    """Ensure generated paths and metadata remain synchronized."""

    def setUp(self):
        self.wadl = (ROOT / "api/application.wadl").read_bytes()
        self.metadata = json.loads((ROOT / "api/contract-metadata.json").read_text(encoding="utf-8"))

    def test_reproduces_committed_contract_with_all_six_operations(self):
        result = convert(self.wadl, self.metadata)
        self.assertEqual(6, sum(len(methods) for methods in result["paths"].values()))
        self.assertEqual(result, json.loads((ROOT / "api/openapi.json").read_text(encoding="utf-8")))
        self.assertEqual("id", result["paths"]["/students/{id}"]["get"]["parameters"][0]["name"])

    def test_rejects_missing_operation_metadata(self):
        del self.metadata["operations"]["createStudent"]
        with self.assertRaisesRegex(ValueError, "Missing metadata"):
            convert(self.wadl, self.metadata)

    def test_rejects_changed_path_and_unpublished_operations(self):
        changed = copy.deepcopy(self.metadata)
        changed["operations"]["createStudent"]["path"] = "/wrong"
        with self.assertRaisesRegex(ValueError, "disagree"):
            convert(self.wadl, changed)
        self.metadata["operations"]["unpublished"] = {}
        with self.assertRaisesRegex(ValueError, "missing operations"):
            convert(self.wadl, self.metadata)

    def test_rejects_dtd_and_non_wadl(self):
        for xml in (b'<!DOCTYPE test><test/>', b"<test/>"):
            with self.assertRaises(ValueError):
                convert(xml, self.metadata)


if __name__ == "__main__":
    unittest.main()
