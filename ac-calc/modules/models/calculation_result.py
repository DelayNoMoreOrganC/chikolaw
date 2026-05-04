"""
计算结果数据模型
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Optional
from datetime import datetime


@dataclass
class InterestDetail:
    """利息计算明细"""
    period_start: str                  # 计息期间起
    period_end: str                    # 计息期间止
    days: int                          # 计息天数
    principal_balance: float           # 本金余额
    rate: float                        # 年利率(%)
    interest: float                    # 利息金额

    def __post_init__(self):
        self.interest = round(self.interest, 2)
        self.principal_balance = round(self.principal_balance, 2)
        self.rate = round(self.rate, 4)


@dataclass
class PenaltyDetail:
    """罚息计算明细"""
    period_start: str                  # 计息期间起
    period_end: str                    # 计息期间止
    days: int                          # 计息天数
    overdue_principal: float           # 逾期本金
    penalty_rate: float                # 罚息利率(%)
    penalty: float                     # 罚息金额

    def __post_init__(self):
        self.penalty = round(self.penalty, 2)
        self.overdue_principal = round(self.overdue_principal, 2)
        self.penalty_rate = round(self.penalty_rate, 4)


@dataclass
class CompoundDetail:
    """复利计算明细"""
    period_start: str                  # 计息期间起
    period_end: str                    # 计息期间止
    days: int                          # 计息天数
    unpaid_interest: float             # 未还利息
    compound_rate: float               # 复利利率(%)
    compound: float                    # 复利金额

    def __post_init__(self):
        self.compound = round(self.compound, 2)
        self.unpaid_interest = round(self.unpaid_interest, 2)
        self.compound_rate = round(self.compound_rate, 4)


@dataclass
class PaymentApplication:
    """还款冲销明细"""
    date: str                          # 还款日期
    payment_amount: float              # 还款金额
    principal_payment: float = 0.0     # 冲销本金
    interest_payment: float = 0.0      # 冲销利息
    penalty_payment: float = 0.0       # 冲销罚息
    compound_payment: float = 0.0      # 冲销复利
    fee_payment: float = 0.0           # 冲销费用
    remaining_principal: float = 0.0   # 冲销后本金余额
    remaining_interest: float = 0.0    # 冲销后利息余额

    def __post_init__(self):
        self.payment_amount = round(self.payment_amount, 2)
        self.principal_payment = round(self.principal_payment, 2)
        self.interest_payment = round(self.interest_payment, 2)
        self.penalty_payment = round(self.penalty_payment, 2)
        self.compound_payment = round(self.compound_payment, 2)
        self.fee_payment = round(self.fee_payment, 2)
        self.remaining_principal = round(self.remaining_principal, 2)
        self.remaining_interest = round(self.remaining_interest, 2)


@dataclass
class CalculationResult:
    """债权计算结果"""

    # 基础信息
    loan_amount: float = 0.0
    start_date: str = ""
    end_date: str = ""
    calculation_date: str = ""

    # 计算结果汇总
    remaining_principal: float = 0.0      # 剩余本金
    total_interest: float = 0.0           # 利息总额
    total_penalty: float = 0.0            # 罚息总额
    total_compound: float = 0.0           # 复利总额
    total_debt: float = 0.0               # 债权总额

    # 计算明细
    interest_details: List[InterestDetail] = field(default_factory=list)
    penalty_details: List[PenaltyDetail] = field(default_factory=list)
    compound_details: List[CompoundDetail] = field(default_factory=list)
    payment_applications: List[PaymentApplication] = field(default_factory=list)

    # 元数据
    calculated_at: str = field(default_factory=lambda: datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    calculation_rules: Optional[str] = None  # 使用的计算规则描述

    def __post_init__(self):
        self.loan_amount = round(self.loan_amount, 2)
        self.remaining_principal = round(self.remaining_principal, 2)
        self.total_interest = round(self.total_interest, 2)
        self.total_penalty = round(self.total_penalty, 2)
        self.total_compound = round(self.total_compound, 2)
        self.total_debt = round(self.total_debt, 2)

    def calculate_total(self):
        """计算债权总额"""
        self.total_debt = (
            self.remaining_principal +
            self.total_interest +
            self.total_penalty +
            self.total_compound
        )
        self.total_debt = round(self.total_debt, 2)

    def get_summary_dict(self) -> dict:
        """获取汇总信息字典"""
        return {
            "贷款金额": self.loan_amount,
            "剩余本金": self.remaining_principal,
            "利息总额": self.total_interest,
            "罚息总额": self.total_penalty,
            "复利总额": self.total_compound,
            "债权总额": self.total_debt,
            "计算日期": self.calculation_date
        }

    def get_payment_summary(self) -> List[dict]:
        """获取还款记录汇总"""
        return [
            {
                "还款日期": app.date,
                "还款金额": app.payment_amount,
                "冲销本金": app.principal_payment,
                "冲销利息": app.interest_payment,
                "冲销罚息": app.penalty_payment,
                "冲销复利": app.compound_payment,
                "剩余本金": app.remaining_principal
            }
            for app in self.payment_applications
        ]
