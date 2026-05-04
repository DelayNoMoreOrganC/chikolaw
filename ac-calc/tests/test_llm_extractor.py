"""
LLM提取器规则兜底测试
"""

import os
import unittest
import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from modules.llm_extractor import LLMExtractor


class TestLLMExtractorFallbacks(unittest.TestCase):
    """规则兜底提取测试"""

    def setUp(self):
        self.extractor = object.__new__(LLMExtractor)
        self.extractor.requested_model = "auto"
        self.extractor.preferred_models = []

    def test_extract_rate_by_patterns(self):
        """显式利率字段应能被正则兜底识别"""
        text = "贷款金额: 400,000.00 年利率: 4.35% 合同起始日: 2023年10月17日"
        rate = self.extractor._extract_rate_by_patterns(text)
        self.assertEqual(rate, 4.35)

    def test_apply_rule_based_fallbacks_keeps_missing_rate_empty(self):
        """当文档里没有明确利率字段时，年利率应保持为空"""
        text = """
        中国建设银行个人贷款对账单
        贷款金额: 400,000.00
        合同起始日: 2023年10月17日
        合同到期日期: 2024年10月17日
        2023年11月21日 自动扣款入账 1,463.89
        2023年12月21日 自动扣款入账 1,416.67
        2024年01月21日 自动扣款入账 1,463.89
        """
        llm_data = {
            "loan_amount": 400000,
            "annual_interest_rate": None,
            "start_date": "2023-10-17",
            "end_date": "2024-10-17",
            "remaining_principal": 400000,
            "accrued_interest": 0,
            "penalty_interest": 0,
            "compound_interest": 0,
            "rate_adjustments": [],
            "repayment_records": [
                {"date": "2023-11-21", "type": "interest", "amount": 1463.89},
                {"date": "2023-12-21", "type": "interest", "amount": 1416.67},
                {"date": "2024-01-21", "type": "interest", "amount": 1463.89},
            ]
        }
        result = self.extractor._apply_rule_based_fallbacks(text, llm_data)
        self.assertIsNone(result["annual_interest_rate"])

    def test_extract_statement_preview_prefers_template_records(self):
        text = """
        中国建设银行个人贷款对账单
        贷款金额: 400, 000. 00
        合同起始日， 2023年10月17日 合同到期日期: 2024年10月17日
        查询结束日期，2024年07月22日
        3 _|2023年10月21日|自动扣款入账|188.88 |0.00 |188.88 |0. 00 |400, 000.00|1082014201697743330099510
        """
        self.extractor.extract_loan_info = lambda *_args, **_kwargs: {
            "loan_amount": 400000,
            "annual_interest_rate": None,
            "start_date": "2023-10-17",
            "end_date": "2024-10-17",
            "remaining_principal": None,
            "repayment_records": [
                {"date": "2023-10-17", "type": "interest", "amount": 188.88}
            ]
        }

        preview = self.extractor.extract_statement_preview(text)
        self.assertEqual(preview["data"]["loan_amount"], 400000.0)
        self.assertEqual(preview["data"]["calculation_date"], "2024-07-22")
        self.assertEqual(preview["repayment_records"][0]["date"], "2023-10-21")
        self.assertEqual(preview["repayment_records"][0]["type"], "interest")

    def test_resolve_model_name_falls_back_to_available_model(self):
        self.extractor.requested_model = "qwen3:8b"
        self.extractor.preferred_models = ["qwen3:8b", "deepseek-r1:32b"]

        resolved = self.extractor._resolve_model_name(["deepseek-r1:32b", "llama3:8b"])
        self.assertEqual(resolved, "deepseek-r1:32b")

    def test_resolve_model_name_uses_first_available_when_auto(self):
        self.extractor.requested_model = "auto"
        self.extractor.preferred_models = []

        resolved = self.extractor._resolve_model_name(["mistral:7b", "llama3:8b"])
        self.assertEqual(resolved, "mistral:7b")

    def test_prepare_base_url_rewrites_localhost_and_updates_no_proxy(self):
        base_url = self.extractor._prepare_base_url("http://localhost:11434")
        self.assertEqual(base_url, "http://127.0.0.1:11434")
        self.assertIn("127.0.0.1", os.environ.get("NO_PROXY", ""))


if __name__ == "__main__":
    unittest.main()
