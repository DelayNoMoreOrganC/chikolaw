import unittest
from pathlib import Path

from modules.bank_loan_flow import (
    TEMPLATE_KEY,
    calculate_loan_flow_result,
    classify_flow_events,
    extract_flow_rows_from_text,
    is_loan_flow_text,
    maybe_extract_loan_flow_preview,
)


SAMPLE_FLOW_TEXT = """
2023年08月17日 借方 本金 3000000.00 存款账户转账放款
2023年08月20日 借方 利息 1600.00 正常本金结息
2023年08月21日 贷方 利息 1600.00 现金回收 利息I还款
2023年11月20日 借方 本金 60000.00 未到期本金到期转桶子（批量）
2023年11月20日 贷方 本金 60000.00 现金回收 本金P还款
2024年02月20日 借方 本金 60000.00 未到期本金到期转桶子（批量）
2024年02月20日 借方 利息 380.00 逾期本金罚息结息
"""


class TestBankLoanFlow(unittest.TestCase):
    def test_detect_and_preview_from_text(self):
        self.assertTrue(is_loan_flow_text(SAMPLE_FLOW_TEXT))
        preview = maybe_extract_loan_flow_preview(text=SAMPLE_FLOW_TEXT, filename="sample.pdf")

        self.assertIsNotNone(preview)
        self.assertEqual(preview["template_key"], TEMPLATE_KEY)
        self.assertEqual(preview["data"]["loan_amount"], 3000000.0)
        self.assertEqual(preview["data"]["start_date"], "2023-08-17")
        self.assertEqual(preview["data"]["calculation_date"], "2024-02-20")
        self.assertEqual(preview["data"]["remaining_principal"], 2940000.0)
        self.assertEqual(len(preview["repayment_records"]), 2)

    def test_classify_flow_events(self):
        rows = extract_flow_rows_from_text(SAMPLE_FLOW_TEXT)
        events = classify_flow_events(rows)
        event_types = [item["event_type"] for item in events]
        self.assertIn("disbursement", event_types)
        self.assertIn("principal_due_transfer", event_types)
        self.assertIn("interest_repayment", event_types)
        self.assertIn("principal_repayment", event_types)

    def test_calculate_loan_flow_result_handles_due_row_and_acceleration(self):
        loan_data = {
            "loan_amount": 100000,
            "annual_interest_rate": 4.8,
            "start_date": "2024-01-17",
            "end_date": "2024-12-31",
            "calculation_date": "2024-01-21",
            "remaining_principal": 98000,
            "accrued_interest": 0,
            "penalty_interest": 0,
            "compound_interest": 0,
            "acceleration_date": "2024-01-20",
            "_template_key": TEMPLATE_KEY,
            "_template_payload": {
                "events": [
                    {
                        "id": 0,
                        "date": "2024-01-17",
                        "parsed_date": __import__("datetime").date(2024, 1, 17),
                        "amount": 100000.0,
                        "event_type": "disbursement",
                        "description": "存款账户转账放款",
                        "raw_line": "2024-01-17 放款 100000",
                    },
                    {
                        "id": 1,
                        "date": "2024-01-20",
                        "parsed_date": __import__("datetime").date(2024, 1, 20),
                        "amount": 2000.0,
                        "event_type": "principal_due_transfer",
                        "description": "未到期本金到期转桶子（批量）",
                        "raw_line": "2024-01-20 到期转逾期 2000",
                    },
                ]
            },
        }
        repayment_records = [
            {"date": "2024-01-20", "type": "principal", "amount": 2000},
            {"date": "2024-01-21", "type": "interest", "amount": 53.33},
        ]

        result = calculate_loan_flow_result(
            loan_data,
            repayment_records=repayment_records,
            rate_adjustments=[],
            calculation_date=__import__("datetime").date(2024, 1, 21),
        )

        self.assertEqual(result["template_key"], TEMPLATE_KEY)
        self.assertEqual(len(result["detail"]), 3)
        due_row = result["detail"][1]
        accel_row = result["detail"][2]

        self.assertEqual(due_row["due_principal"], 2000.0)
        self.assertEqual(due_row["paid_principal"], 2000.0)
        self.assertEqual(due_row["overdue_principal"], 0.0)
        self.assertEqual(due_row["accrued_penalty"], 0.0)
        self.assertEqual(accel_row["normal_principal_balance"], 98000.0)
        self.assertAlmostEqual(accel_row["accrued_penalty"], 19.6, places=2)

    def test_real_sample_xlsx_if_present(self):
        sample_path = Path(
            r"D:/我的资料库/Documents/xwechat_files/wxid_w4uaqxmkyyp622_5242/msg/file/2026-03/伍钧乐欠款流水.xlsx"
        )
        if not sample_path.exists():
            self.skipTest("真实样本 xlsx 不存在，跳过冒烟测试")

        preview = maybe_extract_loan_flow_preview(
            file_bytes=sample_path.read_bytes(),
            filename=sample_path.name,
        )

        self.assertIsNotNone(preview)
        self.assertEqual(preview["template_key"], TEMPLATE_KEY)
        self.assertEqual(preview["data"]["loan_amount"], 3000000.0)
        self.assertEqual(preview["data"]["start_date"], "2023-08-17")
        self.assertEqual(preview["data"]["end_date"], "2026-08-15")
        self.assertEqual(preview["data"]["calculation_date"], "2026-02-21")

    def test_real_sample_calculation_matches_reference_summary_if_present(self):
        sample_path = Path(
            r"D:/我的资料库/Documents/xwechat_files/wxid_w4uaqxmkyyp622_5242/msg/file/2026-03/伍钧乐欠款流水.xlsx"
        )
        if not sample_path.exists():
            self.skipTest("真实样本 xlsx 不存在，跳过结果对比测试")

        preview = maybe_extract_loan_flow_preview(
            file_bytes=sample_path.read_bytes(),
            filename=sample_path.name,
        )
        loan_data = dict(preview["data"])
        loan_data["annual_interest_rate"] = 4.8
        loan_data["calculation_date"] = "2026-03-09"
        loan_data["acceleration_date"] = "2026-03-04"
        rate_adjustments = [
            {"date": "2024-01-01", "rate": 4.7},
            {"date": "2025-01-01", "rate": 4.35},
            {"date": "2026-01-01", "rate": 4.25},
        ]

        result = calculate_loan_flow_result(
            loan_data,
            repayment_records=preview["repayment_records"],
            rate_adjustments=rate_adjustments,
            calculation_date=__import__("datetime").date(2026, 3, 9),
        )
        summary = result["summary"]
        self.assertAlmostEqual(summary["remaining_principal"], 2580000.0, places=2)

        detail = result["detail"]
        due_rows = [item for item in detail if item["due_principal"]]
        self.assertEqual([item["start_date"] for item in due_rows[-3:]], ["2025-08-20", "2025-11-20", "2026-02-20"])
        self.assertEqual([item["overdue_principal"] for item in due_rows[-3:]], [60000.0, 120000.0, 180000.0])

        final_row = detail[-1]
        self.assertEqual(final_row["start_date"], "2026-03-05")
        self.assertEqual(final_row["end_date"], "2026-03-09")
        self.assertAlmostEqual(final_row["overdue_principal"], 2580000.0, places=2)
        self.assertAlmostEqual(final_row["accrued_penalty"], 2284.375, places=3)

    def test_real_sample_huangjun_xlsx_preview_if_present(self):
        sample_path = Path(
            r"D:/我的资料库/Documents/xwechat_files/wxid_w4uaqxmkyyp622_5242/msg/file/2026-03/200万贷款交易明细-20231021.xlsx"
        )
        if not sample_path.exists():
            self.skipTest("黄军样本不存在，跳过 xlsx 冒烟测试")

        file_bytes = sample_path.read_bytes()
        preview = maybe_extract_loan_flow_preview(
            file_bytes=file_bytes,
            filename=sample_path.name,
        )
        self.assertIsNotNone(preview)
        self.assertEqual(preview["template_key"], TEMPLATE_KEY)
        self.assertEqual(preview["data"]["loan_amount"], 2000000.0)
        self.assertEqual(preview["data"]["start_date"], "2023-09-07")
        self.assertEqual(preview["data"]["end_date"], "2026-09-05")
        self.assertEqual(preview["data"]["calculation_date"], "2026-02-21")
        self.assertEqual(preview["data"]["remaining_principal"], 1940000.0)
        self.assertGreaterEqual(len(preview["repayment_records"]), 10)
        self.assertTrue(
            any("多笔借据" in warning for warning in preview["warnings"])
        )
        self.assertTrue(
            any("应还本金转逾期" in warning for warning in preview["warnings"])
        )

    def test_huangjun_manual_principal_plan_generates_due_rows_if_present(self):
        sample_path = Path(
            r"D:/我的资料库/Documents/xwechat_files/wxid_w4uaqxmkyyp622_5242/msg/file/2026-03/200万贷款交易明细-20231021.xlsx"
        )
        if not sample_path.exists():
            self.skipTest("黄军样本不存在，跳过合同规则补录测试")

        preview = maybe_extract_loan_flow_preview(
            file_bytes=sample_path.read_bytes(),
            filename=sample_path.name,
        )
        loan_data = dict(preview["data"])
        loan_data["annual_interest_rate"] = 4.71
        loan_data["calculation_date"] = "2026-03-06"
        loan_data["acceleration_date"] = "2026-03-02"
        loan_data["interest_cycle_months"] = "1"
        loan_data["interest_cycle_changes"] = [
            {"effective_date": "2025-03-21", "months": 6},
        ]
        loan_data["principal_plan_records"] = [
            {"date": "2023-12-21", "amount": 20000},
            {"date": "2024-03-21", "amount": 20000},
            {"date": "2024-06-21", "amount": 20000},
            {"date": "2025-03-21", "amount": 4000},
            {"date": "2025-09-21", "amount": 4000},
        ]

        result = calculate_loan_flow_result(
            loan_data,
            repayment_records=preview["repayment_records"],
            rate_adjustments=[
                {"date": "2025-01-01", "rate": 4.36},
                {"date": "2026-01-01", "rate": 4.26},
            ],
            calculation_date=__import__("datetime").date(2026, 3, 6),
        )

        due_rows = {
            item["start_date"]: item for item in result["detail"] if float(item.get("due_principal", 0) or 0) > 0
        }
        self.assertIn("2023-12-21", due_rows)
        self.assertIn("2024-03-21", due_rows)
        self.assertIn("2024-06-21", due_rows)
        self.assertIn("2025-03-21", due_rows)
        self.assertIn("2025-09-21", due_rows)

        self.assertAlmostEqual(due_rows["2023-12-21"]["due_principal"], 20000.0, places=2)
        self.assertAlmostEqual(due_rows["2023-12-21"]["paid_principal"], 20000.0, places=2)
        self.assertAlmostEqual(due_rows["2024-03-21"]["paid_principal"], 20000.0, places=2)
        self.assertAlmostEqual(due_rows["2024-06-21"]["paid_principal"], 20000.0, places=2)
        self.assertAlmostEqual(due_rows["2025-03-21"]["overdue_principal"], 4000.0, places=2)
        self.assertAlmostEqual(due_rows["2025-09-21"]["overdue_principal"], 8000.0, places=2)


if __name__ == "__main__":
    unittest.main()
