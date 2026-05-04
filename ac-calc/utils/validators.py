"""
数据验证模块
提供数据格式验证、字段校验、异常处理等功能
"""

import re
from typing import Any, Dict, List, Optional, Union
from datetime import date, datetime

import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from utils.date_utils import parse_date, validate_date_order
from utils.constants import RepaymentType, ErrorMessages


class ValidationError(Exception):
    """数据验证错误"""
    def __init__(self, message: str, field: str = None):
        self.message = message
        self.field = field
        super().__init__(self.message)


class ValidationResult:
    """验证结果类"""
    def __init__(self):
        self.is_valid = True
        self.errors = []  # 错误列表
        self.warnings = []  # 警告列表

    def add_error(self, field: str, message: str):
        """添加错误"""
        self.is_valid = False
        self.errors.append({
            "field": field,
            "message": message
        })

    def add_warning(self, field: str, message: str):
        """添加警告"""
        self.warnings.append({
            "field": field,
            "message": message
        })

    def get_error_messages(self) -> List[str]:
        """获取所有错误消息"""
        return [f"{err['field']}: {err['message']}" for err in self.errors]

    def get_warning_messages(self) -> List[str]:
        """获取所有警告消息"""
        return [f"{warn['field']}: {warn['message']}" for warn in self.warnings]


def validate_amount(value: Any, field_name: str = "金额", allow_zero: bool = False) -> float:
    """
    验证金额格式

    Args:
        value: 金额值
        field_name: 字段名称
        allow_zero: 是否允许零值

    Returns:
        float: 验证后的金额

    Raises:
        ValidationError: 验证失败
    """
    if value is None or value == "":
        raise ValidationError(f"{field_name}不能为空")

    try:
        # 如果是字符串，去除逗号和空格
        if isinstance(value, str):
            value = value.replace(",", "").replace(" ", "").strip()

        amount = float(value)

        if not allow_zero and amount <= 0:
            raise ValidationError(f"{field_name}必须大于0")

        if amount < 0:
            raise ValidationError(f"{field_name}不能为负数")

        return round(amount, 2)

    except (ValueError, TypeError):
        raise ValidationError(f"{field_name}格式错误，请输入有效的数字")


def validate_rate(value: Any, field_name: str = "利率") -> float:
    """
    验证利率格式

    Args:
        value: 利率值（可以是百分比字符串如"4.35%"或数字4.35）
        field_name: 字段名称

    Returns:
        float: 验证后的利率（小数形式，如0.0435）

    Raises:
        ValidationError: 验证失败
    """
    if value is None or value == "":
        raise ValidationError(f"{field_name}不能为空")

    try:
        # 如果是字符串，处理百分比格式
        if isinstance(value, str):
            value = value.replace(",", "").replace(" ", "").strip()
            if "%" in value:
                value = value.replace("%", "")
                rate = float(value) / 100
            else:
                rate = float(value) / 100 if float(value) > 1 else float(value)
        else:
            # 如果是数字，大于1视为百分比（如4.35表示4.35%）
            rate = float(value) / 100 if float(value) > 1 else float(value)

        if rate <= 0:
            raise ValidationError(f"{field_name}必须大于0")

        if rate > 1:  # 超过100%的利率警告
            pass  # 可以添加警告逻辑

        return round(rate, 6)  # 保留6位小数精度

    except (ValueError, TypeError):
        raise ValidationError(f"{field_name}格式错误，请输入有效的利率（如4.35或4.35%）")


def validate_date_field(
    value: Any,
    field_name: str = "日期",
    allow_future: bool = True
) -> date:
    """
    验证日期字段

    Args:
        value: 日期值
        field_name: 字段名称
        allow_future: 是否允许未来日期

    Returns:
        datetime.date: 验证后的日期

    Raises:
        ValidationError: 验证失败
    """
    if value is None or value == "":
        raise ValidationError(f"{field_name}不能为空")

    try:
        date_obj = parse_date(value)

        if not allow_future and date_obj > datetime.now().date():
            raise ValidationError(f"{field_name}不能是未来日期")

        return date_obj

    except ValueError as e:
        if "无法解析" in str(e):
            raise ValidationError(f"{field_name}格式错误，请使用 YYYY-MM-DD 格式")
        raise


def validate_loan_contract(data: Dict[str, Any]) -> ValidationResult:
    """
    验证贷款合同数据

    Args:
        data: 贷款合同数据字典

    Returns:
        ValidationResult: 验证结果
    """
    result = ValidationResult()

    # 必填字段
    required_fields = {
        "loan_amount": "贷款金额",
        "annual_interest_rate": "年利率",
        "start_date": "起始日期",
        "end_date": "到期日期",
        "calculation_date": "数据暂计日",
        "remaining_principal": "剩余本金"
    }

    # 检查必填字段
    for field, name in required_fields.items():
        if field not in data or data[field] is None or data[field] == "":
            result.add_error(field, f"{name}不能为空")
            continue

    # 验证贷款金额
    if "loan_amount" in data and data["loan_amount"]:
        try:
            validate_amount(data["loan_amount"], "贷款金额")
        except ValidationError as e:
            result.add_error("loan_amount", str(e))

    # 验证年利率
    if "annual_interest_rate" in data and data["annual_interest_rate"]:
        try:
            validate_rate(data["annual_interest_rate"], "年利率")
        except ValidationError as e:
            result.add_error("annual_interest_rate", str(e))

    # 验证日期
    if "start_date" in data and data["start_date"]:
        try:
            validate_date_field(data["start_date"], "起始日期")
        except ValidationError as e:
            result.add_error("start_date", str(e))

    if "end_date" in data and data["end_date"]:
        try:
            validate_date_field(data["end_date"], "到期日期")
        except ValidationError as e:
            result.add_error("end_date", str(e))

    if "calculation_date" in data and data["calculation_date"]:
        try:
            validate_date_field(data["calculation_date"], "数据暂计日")
        except ValidationError as e:
            result.add_error("calculation_date", str(e))

    # 验证日期顺序
    try:
        if ("start_date" in data and data["start_date"] and
            "end_date" in data and data["end_date"]):
            validate_date_order(data["start_date"], data["end_date"], "合同")
    except ValueError as e:
        result.add_error("date_order", str(e))

    try:
        if ("start_date" in data and data["start_date"] and
            "calculation_date" in data and data["calculation_date"]):
            validate_date_order(data["start_date"], data["calculation_date"], "数据暂计")
    except ValueError as e:
        result.add_error("calculation_date", str(e))

    # 验证剩余本金（不能大于贷款金额）
    try:
        if ("loan_amount" in data and "remaining_principal" in data and
            data["loan_amount"] and data["remaining_principal"]):
            loan_amt = validate_amount(data["loan_amount"], "贷款金额")
            remain_amt = validate_amount(data["remaining_principal"], "剩余本金")
            if remain_amt > loan_amt:
                result.add_warning(
                    "remaining_principal",
                    "剩余本金大于贷款金额，请确认数据正确性"
                )
    except ValidationError:
        pass  # 已在上面的验证中处理

    # 可选字段：已计利息、罚息、复利
    optional_fields = {
        "accrued_interest": "已计利息",
        "penalty_interest": "罚息",
        "compound_interest": "复利"
    }

    for field, name in optional_fields.items():
        if field in data and data[field]:
            try:
                validate_amount(data[field], name, allow_zero=True)
            except ValidationError as e:
                result.add_error(field, str(e))

    return result


def validate_repayment_record(record: Dict[str, Any]) -> ValidationResult:
    """
    验证单条还款记录

    Args:
        record: 还款记录字典

    Returns:
        ValidationResult: 验证结果
    """
    result = ValidationResult()

    # 必填字段
    if "date" not in record or not record["date"]:
        result.add_error("date", "还款日期不能为空")
    else:
        try:
            validate_date_field(record["date"], "还款日期")
        except ValidationError as e:
            result.add_error("date", str(e))

    if "type" not in record or not record["type"]:
        result.add_error("type", "还款类型不能为空")
    else:
        # 验证还款类型是否有效
        valid_types = [t.value for t in RepaymentType]
        if record["type"] not in valid_types:
            result.add_error("type", f"无效的还款类型: {record['type']}")

    if "amount" not in record or not record["amount"]:
        result.add_error("amount", "还款金额不能为空")
    else:
        try:
            validate_amount(record["amount"], "还款金额")
        except ValidationError as e:
            result.add_error("amount", str(e))

    return result


def validate_repayment_records(records: List[Dict[str, Any]]) -> ValidationResult:
    """
    验证所有还款记录

    Args:
        records: 还款记录列表

    Returns:
        ValidationResult: 验证结果
    """
    result = ValidationResult()

    if not records:
        return result  # 空记录是允许的

    # 检查是否有重复的日期和类型
    seen_records = set()
    for idx, record in enumerate(records):
        # 验证单条记录
        record_result = validate_repayment_record(record)
        if not record_result.is_valid:
            for error in record_result.errors:
                result.add_error(
                    f"record_{idx}",
                    f"第{idx+1}条记录: {error['message']}"
                )

        # 检查重复
        if "date" in record and "type" in record:
            record_key = (record["date"], record["type"])
            if record_key in seen_records:
                result.add_warning(
                    f"record_{idx}",
                    f"第{idx+1}条记录: 可能存在重复的日期和类型"
                )
            seen_records.add(record_key)

    return result


def validate_rate_adjustments(adjustments: List[Dict[str, Any]]) -> ValidationResult:
    """
    验证利率调整记录

    Args:
        adjustments: 利率调整记录列表

    Returns:
        ValidationResult: 验证结果
    """
    result = ValidationResult()

    if not adjustments:
        return result

    for idx, adj in enumerate(adjustments):
        # 验证调整日期
        if "date" not in adj or not adj["date"]:
            result.add_error(f"adjustment_{idx}", "调整日期不能为空")
        else:
            try:
                validate_date_field(adj["date"], f"第{idx+1}条调整记录的日期")
            except ValidationError as e:
                result.add_error(f"adjustment_{idx}", str(e))

        # 验证新利率
        if "rate" not in adj or not adj["rate"]:
            result.add_error(f"adjustment_{idx}", "新利率不能为空")
        else:
            try:
                validate_rate(adj["rate"], f"第{idx+1}条调整记录的利率")
            except ValidationError as e:
                result.add_error(f"adjustment_{idx}", str(e))

    return result


def sanitize_filename(filename: str) -> str:
    """
    清理文件名，移除非法字符

    Args:
        filename: 原始文件名

    Returns:
        str: 清理后的文件名
    """
    # 移除或替换非法字符
    illegal_chars = r'[<>:"/\\|?*]'
    cleaned = re.sub(illegal_chars, '_', filename)

    # 去除首尾空格和点
    cleaned = cleaned.strip('. ')

    # 限制长度
    if len(cleaned) > 200:
        cleaned = cleaned[:200]

    return cleaned


def validate_json_structure(data: Any, required_keys: List[str]) -> ValidationResult:
    """
    验证JSON数据结构

    Args:
        data: JSON数据（字典）
        required_keys: 必需的键列表

    Returns:
        ValidationResult: 验证结果
    """
    result = ValidationResult()

    if not isinstance(data, dict):
        result.add_error("structure", "数据必须是JSON对象（字典）")
        return result

    for key in required_keys:
        if key not in data:
            result.add_error(key, f"缺少必需字段: {key}")

    return result


if __name__ == "__main__":
    # 测试代码
    print("验证工具测试")
    print("=" * 50)

    # 测试金额验证
    print("\n测试金额验证:")
    test_amounts = ["10000", "10,000.50", "-100", "0", "abc"]
    for amt in test_amounts:
        try:
            result = validate_amount(amt, "测试金额")
            print(f"  ✓ {amt} -> {result}")
        except ValidationError as e:
            print(f"  ✗ {amt} -> {e}")

    # 测试利率验证
    print("\n测试利率验证:")
    test_rates = ["4.35", "4.35%", "0.0435", "-5", "abc"]
    for rate in test_rates:
        try:
            result = validate_rate(rate, "测试利率")
            print(f"  ✓ {rate} -> {result:.6f}")
        except ValidationError as e:
            print(f"  ✗ {rate} -> {e}")

    # 测试贷款合同验证
    print("\n测试贷款合同验证:")
    contract_data = {
        "loan_amount": "1000000",
        "annual_interest_rate": "4.35%",
        "start_date": "2024-01-01",
        "end_date": "2025-01-01",
        "remaining_principal": "800000",
        "accrued_interest": "50000"
    }
    result = validate_loan_contract(contract_data)
    if result.is_valid:
        print("  ✓ 合同数据验证通过")
    else:
        print(f"  ✗ 合同数据验证失败:")
        for error in result.get_error_messages():
            print(f"    - {error}")

    # 测试还款记录验证
    print("\n测试还款记录验证:")
    repayment_records = [
        {"date": "2024-06-30", "type": "interest", "amount": "10000"},
        {"date": "2024-07-31", "type": "principal", "amount": "50000"}
    ]
    result = validate_repayment_records(repayment_records)
    if result.is_valid:
        print("  ✓ 还款记录验证通过")
    else:
        print(f"  ✗ 还款记录验证失败:")
        for error in result.get_error_messages():
            print(f"    - {error}")
