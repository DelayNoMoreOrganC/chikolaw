"""
验证器单元测试
"""

import unittest
import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from utils.validators import (
    validate_amount,
    validate_rate,
    validate_date_field,
    validate_loan_contract,
    validate_repayment_records,
    ValidationError
)


class TestValidators(unittest.TestCase):
    """验证器测试类"""

    def test_validate_amount(self):
        """测试金额验证"""
        # 正常金额
        result = validate_amount("10000.50", "测试金额")
        self.assertEqual(result, 10000.50)

        # 带逗号的金额
        result = validate_amount("10,000.50", "测试金额")
        self.assertEqual(result, 10000.50)

        # 负数应该抛出异常
        with self.assertRaises(ValidationError):
            validate_amount("-100", "测试金额")

        # 无效格式应该抛出异常
        with self.assertRaises(ValidationError):
            validate_amount("abc", "测试金额")

    def test_validate_rate(self):
        """测试利率验证"""
        # 百分比格式
        result = validate_rate("4.35%", "测试利率")
        self.assertAlmostEqual(result, 0.0435)

        # 小数格式
        result = validate_rate("0.0435", "测试利率")
        self.assertAlmostEqual(result, 0.0435)

        # 大于1的数字视为百分比
        result = validate_rate("4.35", "测试利率")
        self.assertAlmostEqual(result, 0.0435)

        # 负数应该抛出异常
        with self.assertRaises(ValidationError):
            validate_rate("-5", "测试利率")

    def test_validate_date_field(self):
        """测试日期验证"""
        # 标准格式
        result = validate_date_field("2024-01-01", "测试日期")
        self.assertEqual(result.year, 2024)
        self.assertEqual(result.month, 1)
        self.assertEqual(result.day, 1)

        # 斜杠格式当前也支持
        result = validate_date_field("2024/01/01", "测试日期")
        self.assertEqual(result.year, 2024)
        self.assertEqual(result.month, 1)
        self.assertEqual(result.day, 1)

    def test_validate_loan_contract(self):
        """测试贷款合同验证"""
        # 有效数据
        valid_data = {
            "loan_amount": "1000000",
            "annual_interest_rate": "4.35%",
            "start_date": "2024-01-01",
            "end_date": "2025-01-01",
            "calculation_date": "2025-01-01",
            "remaining_principal": "800000"
        }

        result = validate_loan_contract(valid_data)
        self.assertTrue(result.is_valid)

        # 缺少必填字段
        invalid_data = {
            "loan_amount": "1000000"
        }

        result = validate_loan_contract(invalid_data)
        self.assertFalse(result.is_valid)

    def test_validate_repayment_records(self):
        """测试还款记录验证"""
        # 有效记录
        valid_records = [
            {"date": "2024-06-30", "type": "interest", "amount": "10000"},
            {"date": "2024-07-31", "type": "principal", "amount": "50000"}
        ]

        result = validate_repayment_records(valid_records)
        self.assertTrue(result.is_valid)

        # 无效记录
        invalid_records = [
            {"date": "2024-06-30", "type": "interest"}  # 缺少金额
        ]

        result = validate_repayment_records(invalid_records)
        self.assertFalse(result.is_valid)


if __name__ == "__main__":
    unittest.main()
