import tempfile
import unittest
from pathlib import Path

from modules.history_store import HistoryStore


class TestHistoryStore(unittest.TestCase):
    def test_save_and_load_history_record(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            store = HistoryStore(base_dir=Path(tmp_dir))

            loan_data = {
                "loan_amount": "1000000",
                "annual_interest_rate": "4.35",
                "start_date": "2024-01-01",
                "end_date": "2024-12-31",
                "calculation_date": "2024-03-20",
                "remaining_principal": "900000",
                "accrued_interest": "0",
                "penalty_interest": "0",
                "compound_interest": "0"
            }
            repayment_records = [
                {"date": "2024-02-20", "type": "interest", "amount": "1000"}
            ]
            rate_adjustments = [
                {"date": "2024-02-01", "rate": "4.50"}
            ]
            calculation_result = {
                "summary": {
                    "total_amount": 905000,
                    "calculation_date": "2024-03-20"
                },
                "detail": [
                    {"seq_no": 1, "period": "2024-01-01 至 2024-01-20"}
                ]
            }
            excel_bytes = b"fake-excel-bytes"

            saved = store.save_record(
                loan_data,
                repayment_records,
                rate_adjustments,
                calculation_result,
                excel_bytes=excel_bytes
            )

            self.assertIsNotNone(saved)
            self.assertIn("id", saved)
            self.assertIn("snapshot", saved)
            self.assertEqual(saved["snapshot"]["loan_data"]["loan_amount"], "1000000")

            listed = store.list_records()
            self.assertEqual(len(listed), 1)
            self.assertEqual(listed[0]["id"], saved["id"])

            loaded = store.get_record(saved["id"])
            self.assertEqual(loaded["snapshot"]["repayment_records"][0]["amount"], "1000")
            self.assertEqual(store.get_excel_bytes(saved["id"]), excel_bytes)


if __name__ == "__main__":
    unittest.main()
