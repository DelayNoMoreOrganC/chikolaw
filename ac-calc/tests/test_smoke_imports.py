import ast
import unittest
from pathlib import Path

from modules.excel_exporter import ExcelExporter


class TestSmokeImports(unittest.TestCase):
    def test_app_source_has_valid_syntax(self):
        app_path = Path(__file__).parent.parent / "app.py"
        source = app_path.read_text(encoding="utf-8")
        ast.parse(source)

    def test_excel_exporter_can_be_imported(self):
        exporter = ExcelExporter()
        self.assertIsNotNone(exporter)


if __name__ == "__main__":
    unittest.main()
