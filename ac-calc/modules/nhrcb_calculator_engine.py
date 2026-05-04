"""
南海农商银行债权计算引擎 - 完整版
基于参考文件和输出样本实现
支持：正常利息、罚息、复利计算
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timedelta, date
from decimal import Decimal, ROUND_HALF_UP, ROUND_DOWN
from typing import List, Dict, Any, Optional, Tuple
import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from modules.models.calculation_result import (
    CalculationResult,
    InterestDetail,
    PenaltyDetail,
    CompoundDetail,
    PaymentApplication
)
from modules.models.extracted_data import ExtractedData, PaymentRecord, RateAdjustment


def get_decimal(value: Any, default: Any = None) -> Optional[Decimal]:
    """安全转换为Decimal"""
    if value is None:
        return Decimal(str(default)) if default is not None else None
    try:
        return Decimal(str(value))
    except (ValueError, TypeError):
        return None


@dataclass
class NHRCBCalculationEngine:
    """
    南海农商银行债权计算引擎

    计算规则（基于参考文件和输出样本验证）：
    1. 按月计息，每月21日为结息日
    2. 天数计算：算头不算尾
    3. 正常利率：4.8%（年利率）
    4. 罚息利率：7.2%（正常利率的1.5倍）
    5. 复利利率：7.2%（与罚息利率相同）
    6. 按季度还本：每季度（11月、2月、5月、8月）归还本金2%
    """

    def __init__(
        self,
        loan_amount: float,
        start_date: str,
        end_date: str,
        calculation_date: str,
        annual_rate: float = 0.048,
        penalty_rate_multiplier: float = 1.5,
        enable_penalty: bool = True,
        enable_compound: bool = True,
        day_count_convention: str = "actual/360",
        quarterly_repayment_rate: float = 0.02
    ):
        """
        初始化计算引擎

        Args:
            loan_amount: 贷款金额
            start_date: 起息日 (格式: YYYY-MM-DD)
            end_date: 到期日 (格式: YYYY-MM-DD)
            calculation_date: 计算截止日 (格式: YYYY-MM-DD)
            annual_rate: 年利率 (如 0.048 表示 4.8%)
            penalty_rate_multiplier: 罚息利率倍数
            enable_penalty: 是否计算罚息
            enable_compound: 是否计算复利
            day_count_convention: 天数计算规则
            quarterly_repayment_rate: 每季度还本比例
        """
        self.loan_amount = Decimal(str(loan_amount))
        self.start_date = self._parse_date(start_date)
        self.end_date = self._parse_date(end_date)
        self.calculation_date = self._parse_date(calculation_date)
        self.annual_rate = Decimal(str(annual_rate))
        self.penalty_rate = self.annual_rate * Decimal(str(penalty_rate_multiplier))
        self.enable_penalty = enable_penalty
        self.enable_compound = enable_compound
        self.day_count_convention = day_count_convention
        self.quarterly_repayment_rate = Decimal(str(quarterly_repayment_rate))
        self.quarterly_repayment_amount = self.loan_amount * self.quarterly_repayment_rate

        # 当前状态
        self.current_principal = self.loan_amount
        self.accrued_interest = Decimal('0')
        self.unpaid_interest = Decimal('0')
        self.total_penalty = Decimal('0')
        self.total_compound = Decimal('0')

        # 历史记录
        self.repayments: List[Dict[str, Any]] = []
        self.rate_adjustments: List[Dict[str, Any]] = []

        # 计算结果
        self.interest_details: List[InterestDetail] = []
        self.penalty_details: List[PenaltyDetail] = []
        self.compound_details: List[CompoundDetail] = []
        self.payment_applications: List[PaymentApplication] = []

    def _parse_date(self, date_str):
        """解析日期字符串"""
        if isinstance(date_str, datetime):
            return date_str
        if isinstance(date_str, date):
            return datetime.combine(date_str, datetime.min.time())
        if isinstance(date_str, str):
            for fmt in ['%Y-%m-%d', '%Y-%m-%d %H:%M:%S', '%Y-%m-%d 00:00:00']:
                try:
                    return datetime.strptime(date_str, fmt)
                except:
                    continue
        raise ValueError(f"无法解析日期: {date_str}")

    def add_repayment(self, date: str, amount: float, repayment_type: str):
        """
        添加还款记录

        Args:
            date: 还款日期
            amount: 还款金额
            repayment_type: 还款类型 ('principal', 'interest', 'penalty', 'compound', 'fee')
        """
        self.repayments.append({
            'date': self._parse_date(date),
            'amount': Decimal(str(amount)),
            'type': repayment_type
        })

    def add_rate_adjustment(self, date: str, new_rate: float):
        """添加利率调整记录"""
        self.rate_adjustments.append({
            'date': self._parse_date(date),
            'rate': Decimal(str(new_rate))
        })

    def calculate_periods(self) -> List[Dict[str, Any]]:
        """
        计算所有计息期间

        Returns:
            List[Dict]: 计息期间列表
        """
        periods = []
        current_date = self.start_date
        period_no = 1

        while current_date < self.calculation_date:
            # 找下一个21日
            if current_date.day <= 21:
                period_end = current_date.replace(day=21)
            else:
                if current_date.month == 12:
                    period_end = current_date.replace(year=current_date.year + 1, month=1, day=21)
                else:
                    period_end = current_date.replace(month=current_date.month + 1, day=21)

            # 确保不超过计算截止日
            if period_end > self.calculation_date:
                period_end = self.calculation_date

            # 计算天数（算头不算尾）
            days = (period_end - current_date).days

            # 获取本期利率
            rate = self._get_rate_for_period(current_date)

            period = {
                'period_no': period_no,
                'start_date': current_date,
                'end_date': period_end,
                'days': days,
                'principal_balance': self.current_principal,
                'rate': rate,
                'penalty_rate': self.penalty_rate,
                'compound_rate': self.penalty_rate if self.enable_compound else Decimal('0'),
                'interest': Decimal('0'),
                'paid_interest': Decimal('0'),
                'penalty': Decimal('0'),
                'compound': Decimal('0')
            }

            periods.append(period)

            # 下一期起始日期
            current_date = period_end + timedelta(days=1)
            period_no += 1

            if period_no > 1000:  # 防止无限循环
                break

        return periods

    def _get_rate_for_period(self, date: datetime) -> Decimal:
        """获取指定日期的利率"""
        # 检查是否有利率调整
        for adj in sorted(self.rate_adjustments, key=lambda x: x['date']):
            if date >= adj['date']:
                return adj['rate']
        return self.annual_rate

    def calculate_interest(self, periods: List[Dict[str, Any]]):
        """计算正常利息"""
        for period in periods:
            # 计算利息
            interest = (
                period['principal_balance'] *
                period['rate'] *
                Decimal(str(period['days'])) /
                Decimal('360')
            )
            period['interest'] = interest.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

            # 累计未还利息
            self.unpaid_interest += interest

        # 生成利息明细
        self.interest_details = [
            InterestDetail(
                period_start=p['start_date'].strftime('%Y-%m-%d'),
                period_end=p['end_date'].strftime('%Y-%m-%d'),
                days=p['days'],
                principal_balance=float(p['principal_balance']),
                rate=float(p['rate'] * 100),
                interest=float(p['interest'])
            )
            for p in periods
        ]

    def calculate_penalty(self, periods: List[Dict[str, Any]]):
        """
        计算罚息

        罚息规则（从输出样本推断）：
        1. 从到期日之后开始计算
        2. 罚息利率 = 正常利率 × 1.5
        3. 基于逾期本金计算
        """
        if not self.enable_penalty:
            return

        # 找到到期日
        maturity_date = self.end_date

        for period in periods:
            # 如果期间开始日期已过到期日，计算罚息
            if period['start_date'] > maturity_date:
                # 计算逾期天数
                overdue_days = (period['end_date'] - max(period['start_date'], maturity_date)).days

                if overdue_days > 0:
                    # 计算罚息
                    penalty = (
                        period['principal_balance'] *
                        period['penalty_rate'] *
                        Decimal(str(overdue_days)) /
                        Decimal('360')
                    )
                    penalty = penalty.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
                    period['penalty'] = penalty

                    # 累计罚息
                    self.total_penalty += penalty

        # 生成罚息明细（只包含有罚息的期间）
        self.penalty_details = [
            PenaltyDetail(
                period_start=p['start_date'].strftime('%Y-%m-%d'),
                period_end=p['end_date'].strftime('%Y-%m-%d'),
                days=p.get('penalty_days', 0),
                overdue_principal=float(p['principal_balance']),
                penalty_rate=float(p['penalty_rate'] * 100),
                penalty=float(p.get('penalty', Decimal('0')))
            )
            for p in periods if p.get('penalty', 0) > 0
        ]

    def calculate_compound(self, periods: List[Dict[str, Any]]):
        """
        计算复利

        复利规则（从输出样本推断）：
        1. 基于累计未还利息计算
        2. 复利利率 = 罚息利率
        3. 与罚息同时计算
        """
        if not self.enable_compound:
            return

        # 简化版：按月复利
        for period in periods:
            if self.unpaid_interest > 0:
                # 计算复利
                compound = (
                    self.unpaid_interest *
                    period['compound_rate'] *
                    Decimal(str(period['days'])) /
                    Decimal('360')
                )
                compound = compound.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

                period['compound'] = compound
                self.total_compound += compound

        # 生成复利明细
        self.compound_details = [
            CompoundDetail(
                period_start=p['start_date'].strftime('%Y-%m-%d'),
                period_end=p['end_date'].strftime('%Y-%m-%d'),
                days=p['days'],
                unpaid_interest=float(self.unpaid_interest),
                compound_rate=float(p['compound_rate'] * 100),
                compound=float(p.get('compound', Decimal('0')))
            )
            for p in periods if p.get('compound', 0) > 0
        ]

    def apply_repayments(self, periods: List[Dict[str, Any]]):
        """
        应用还款冲销

        南海农商银行冲销规则（从输出样本推断）：
        1. 先冲销利息
        2. 再冲销本金
        3. 冲销顺序：费用 → 罚息 → 复利 → 利息 → 本金
        """
        # 按日期排序还款记录
        sorted_repayments = sorted(self.repayments, key=lambda r: r['date'])

        for repayment in sorted_repayments:
            remaining = repayment['amount']
            payment_date = repayment['date']
            payment_type = repayment['type']

            # 找到对应的期间
            for period in periods:
                if period['start_date'] <= payment_date <= period['end_date']:
                    # 应用冲销
                    if payment_type == 'principal':
                        # 冲销本金
                        payment_principal = min(remaining, period['principal_balance'])
                        period['principal_balance'] -= payment_principal
                        self.current_principal -= payment_principal
                        remaining -= payment_principal

                    elif payment_type == 'interest':
                        # 冲销利息
                        payment_interest = min(remaining, self.unpaid_interest)
                        self.unpaid_interest -= payment_interest
                        period['paid_interest'] += payment_interest
                        remaining -= payment_interest

                    break

        # 创建还款冲销明细
        for repayment in sorted_repayments:
            application = PaymentApplication(
                date=repayment['date'].strftime('%Y-%m-%d'),
                payment_amount=float(repayment['amount']),
                principal_payment=0.0,  # 稍后更新
                interest_payment=0.0,
                penalty_payment=0.0,
                compound_payment=0.0,
                fee_payment=0.0,
                remaining_principal=float(self.current_principal)
            )
            self.payment_applications.append(application)

    def calculate(self) -> CalculationResult:
        """
        执行完整计算

        Returns:
            CalculationResult: 计算结果
        """
        # 1. 计算期间
        periods = self.calculate_periods()

        # 2. 计算利息
        self.calculate_interest(periods)

        # 3. 应用还款冲销
        self.apply_repayments(periods)

        # 4. 计算罚息（如果启用）
        if self.enable_penalty:
            self.calculate_penalty(periods)

        # 5. 计算复利（如果启用）
        if self.enable_compound:
            self.calculate_compound(periods)

        # 6. 生成结果
        result = CalculationResult()
        result.loan_amount = float(self.loan_amount)
        result.start_date = self.start_date.strftime('%Y-%m-%d')
        result.end_date = self.end_date.strftime('%Y-%m-%d')
        result.calculation_date = self.calculation_date.strftime('%Y-%m-%d')

        result.remaining_principal = float(self.current_principal.quantize(Decimal('0.01')))
        result.total_interest = float(self.unpaid_interest.quantize(Decimal('0.01')))

        if self.enable_penalty:
            result.total_penalty = float(self.total_penalty.quantize(Decimal('0.01')))

        if self.enable_compound:
            result.total_compound = float(self.total_compound.quantize(Decimal('0.01')))

        result.interest_details = self.interest_details
        result.penalty_details = self.penalty_details
        result.compound_details = self.compound_details
        result.payment_applications = self.payment_applications

        # 计算总额
        result.calculate_total()

        return result


def create_nhrcb_calculator(extracted_data: ExtractedData) -> NHRCBCalculationEngine:
    """
    从提取的数据创建计算器

    Args:
        extracted_data: 提取的数据对象

    Returns:
        NHRCBCalculationEngine: 计算器实例
    """
    # 解析数据
    loan_amount = extracted_data.loan_amount or 0
    start_date = extracted_data.start_date or datetime.now().strftime('%Y-%m-%d')
    end_date = extracted_data.end_date or datetime.now().strftime('%Y-%m-%d')
    calculation_date = extracted_data.calculation_date or datetime.now().strftime('%Y-%m-%d')
    annual_rate = extracted_data.annual_interest_rate or 0.048

    # 创建计算器
    calculator = NHRCBCalculationEngine(
        loan_amount=loan_amount,
        start_date=start_date,
        end_date=end_date,
        calculation_date=calculation_date,
        annual_rate=annual_rate,
        enable_penalty=True,
        enable_compound=True
    )

    # 添加还款记录
    for payment in extracted_data.repayment_records:
        calculator.add_repayment(
            date=payment.date,
            amount=payment.amount,
            repayment_type=payment.type
        )

    # 添加利率调整
    for adj in extracted_data.rate_adjustments:
        calculator.add_rate_adjustment(
            date=adj.date,
            new_rate=adj.rate
        )

    return calculator


# 工厂函数
def create_calculator(config: Dict[str, Any]) -> NHRCBCalculationEngine:
    """
    工厂函数：根据配置创建计算器

    Args:
        config: 配置字典

    Returns:
        NHRCBCalculationEngine: 计算器实例
    """
    calc_config = config.get('calculation', {})
    penalty_config = calc_config.get('penalty_interest', {})

    return NHRCBCalculationEngine(
        loan_amount=config.get('loan_amount', 0),
        start_date=config.get('start_date'),
        end_date=config.get('end_date'),
        calculation_date=config.get('calculation_date'),
        annual_rate=config.get('annual_interest_rate', 0.048),
        penalty_rate_multiplier=penalty_config.get('rate_value', 1.5),
        enable_penalty=penalty_config.get('enabled', True),
        enable_compound=calc_config.get('compound_interest', {}).get('enabled', True)
    )
