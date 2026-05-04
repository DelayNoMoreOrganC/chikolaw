"""
提取的数据模型
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional
from datetime import date


@dataclass
class PaymentRecord:
    """还款记录"""
    date: str                           # 还款日期
    type: str                           # 还款类型: principal, interest, penalty, compound, fee
    amount: float                       # 还款金额
    description: str = ""               # 备注
    source: str = ""                    # 数据来源

    def __post_init__(self):
        if self.amount:
            self.amount = round(self.amount, 2)


@dataclass
class RateAdjustment:
    """利率调整记录"""
    date: str                           # 调整日期
    rate: float                         # 新利率（百分比）
    reason: str = ""                    # 调整原因

    def __post_init__(self):
        if self.rate:
            self.rate = round(self.rate, 4)


@dataclass
class ExtractedData:
    """从对账单中提取的数据"""

    # 基本信息
    loan_amount: Optional[float] = None
    annual_interest_rate: Optional[float] = None
    start_date: Optional[str] = None
    end_date: Optional[str] = None
    calculation_date: Optional[str] = None

    # 余额信息
    remaining_principal: Optional[float] = None
    accrued_interest: float = 0
    penalty_interest: float = 0
    compound_interest: float = 0

    # 详细记录
    repayment_records: List[PaymentRecord] = field(default_factory=list)
    rate_adjustments: List[RateAdjustment] = field(default_factory=list)

    # 元数据
    bank_code: Optional[str] = None
    template_version: Optional[str] = None
    extraction_confidence: float = 0.0
    field_sources: Dict[str, str] = field(default_factory=dict)
    warnings: List[str] = field(default_factory=list)

    def __post_init__(self):
        if self.loan_amount:
            self.loan_amount = round(self.loan_amount, 2)
        if self.annual_interest_rate:
            self.annual_interest_rate = round(self.annual_interest_rate, 4)
        if self.remaining_principal:
            self.remaining_principal = round(self.remaining_principal, 2)

    def is_complete(self) -> bool:
        """检查是否包含所有必需字段"""
        return all([
            self.loan_amount is not None,
            self.start_date is not None,
            self.end_date is not None
        ])

    def get_missing_fields(self) -> List[str]:
        """获取缺失的必需字段"""
        missing = []
        if self.loan_amount is None:
            missing.append("loan_amount")
        if self.start_date is None:
            missing.append("start_date")
        if self.end_date is None:
            missing.append("end_date")
        return missing

    def add_warning(self, message: str):
        """添加警告信息"""
        self.warnings.append(message)

    def set_field_source(self, field: str, source: str):
        """设置字段来源"""
        self.field_sources[field] = source
