import unittest
from datetime import date
from pathlib import Path
import sys

project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from modules.calculator import DebtCalculator


class TestDebtCalculator(unittest.TestCase):
    def setUp(self):
        self.calculator = DebtCalculator()

    def test_uses_loan_amount_as_starting_principal(self):
        loan_data = {
            "loan_amount": 1000000,
            "annual_interest_rate": 0.12,
            "start_date": "2024-01-01",
            "end_date": "2024-12-31",
            "remaining_principal": 700000,
            "compound_interest": 0,
        }

        result = self.calculator.calculate(loan_data, calculation_date=date(2024, 1, 20))

        self.assertAlmostEqual(result["detail"][0]["opening_principal_balance"], 1000000.0, places=2)

    def test_days_are_inclusive_of_start_and_end(self):
        self.assertEqual(
            self.calculator._calculate_segment_days(date(2024, 1, 1), date(2024, 1, 20)),
            20,
        )

    def test_penalty_rate_applies_after_contract_end(self):
        loan_data = {
            "loan_amount": 1000000,
            "annual_interest_rate": 0.12,
            "start_date": "2024-12-21",
            "end_date": "2025-01-10",
            "remaining_principal": 1000000,
            "compound_interest": 0,
        }

        result = self.calculator.calculate(loan_data, calculation_date=date(2025, 1, 20))

        detail = result["detail"][0]
        self.assertEqual(detail["days"], 31)
        self.assertAlmostEqual(detail["new_penalty"], 5000.0, places=2)

    def test_rate_adjustment_splits_period(self):
        loan_data = {
            "loan_amount": 1000000,
            "annual_interest_rate": 0.06,
            "start_date": "2024-01-01",
            "end_date": "2024-01-20",
            "remaining_principal": 1000000,
            "compound_interest": 0,
        }

        result = self.calculator.calculate(
            loan_data,
            rate_adjustments=[{"date": "2024-01-10", "rate": "8"}],
            calculation_date=date(2024, 1, 20),
        )

        self.assertAlmostEqual(result["detail"][0]["new_interest"], 3944.44, places=2)
        self.assertIn("利率调整", result["detail"][0]["remark"])

    def test_interest_repayment_on_next_cycle_start_is_attributed_to_previous_cycle(self):
        loan_data = {
            "loan_amount": 1000000,
            "annual_interest_rate": 0.12,
            "start_date": "2024-01-01",
            "end_date": "2024-12-31",
            "remaining_principal": 1000000,
            "compound_interest": 0,
        }

        result = self.calculator.calculate(
            loan_data,
            repayment_records=[{"date": "2024-01-21", "type": "interest", "amount": 6666.67}],
            calculation_date=date(2024, 2, 20),
        )

        first_period = result["detail"][0]
        second_period = result["detail"][1]

        self.assertAlmostEqual(first_period["new_interest"], 6666.67, places=2)
        self.assertAlmostEqual(first_period["repaid_interest"], 6666.67, places=2)
        self.assertAlmostEqual(first_period["interest_balance"], 0.0, places=2)
        self.assertAlmostEqual(second_period["new_interest_compound"], 0.0, places=2)

    def test_compound_starts_from_next_period_opening_balance(self):
        loan_data = {
            "loan_amount": 1000000,
            "annual_interest_rate": 0.12,
            "start_date": "2024-01-01",
            "end_date": "2024-12-31",
            "remaining_principal": 1000000,
            "compound_interest": 0,
        }

        result = self.calculator.calculate(loan_data, calculation_date=date(2024, 2, 20))

        first_period = result["detail"][0]
        second_period = result["detail"][1]

        self.assertAlmostEqual(first_period["new_interest_compound"], 0.0, places=2)
        self.assertAlmostEqual(second_period["new_interest_compound"], 68.89, places=2)

    def test_formula_fields_are_present(self):
        loan_data = {
            "loan_amount": 1000,
            "annual_interest_rate": 0.36,
            "start_date": "2024-01-01",
            "end_date": "2024-12-31",
            "remaining_principal": 1000,
            "compound_interest": 0,
        }

        result = self.calculator.calculate(loan_data, calculation_date=date(2024, 1, 20))
        detail = result["detail"][0]

        self.assertIn("本金余额计算式", {"本金余额计算式": detail["principal_balance_formula"]})
        self.assertTrue(detail["new_interest_formula"])
        self.assertTrue(detail["interest_balance_formula"])

    def test_summary_contains_interest_and_penalty_differences(self):
        loan_data = {
            "loan_amount": 1000,
            "annual_interest_rate": 0.36,
            "start_date": "2024-01-01",
            "end_date": "2024-12-31",
            "remaining_principal": 1000,
            "accrued_interest": 10,
            "penalty_interest": 20,
            "compound_interest": 30,
        }

        result = self.calculator.calculate(loan_data, calculation_date=date(2024, 1, 20))
        summary = result["summary"]

        self.assertIn("input_accrued_interest", summary)
        self.assertIn("accrued_interest_difference", summary)
        self.assertIn("input_penalty_interest", summary)
        self.assertIn("penalty_interest_difference", summary)


if __name__ == "__main__":
    unittest.main()
