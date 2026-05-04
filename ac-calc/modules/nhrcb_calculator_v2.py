"""
南海农商银行债权计算引擎 - 修正版
基于参考文件和输出样本分析实现
计算规则：算头不算尾
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from typing import List, Dict, Any, Optional, Tuple
import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from modules.models.calculation_result import CalculationResult, InterestDetail, PaymentApplication
from utils.date_utils import parse_date, format_date


@dataclass
class CalculationPeriod:
    """计息期间"""
    period_no: int                      # 期数
    start_date: datetime                # 起始日期
    end_date: datetime                  # 结束日期
    days: int                           # 计息天数（算头不算尾）
    principal_balance: Decimal          # 本金余额
    rate: Decimal                       # 年利率
    interest: Decimal = Decimal('0')    # 应计利息
    paid_interest: Decimal = Decimal('0')  # 已还利息

    def calculate_interest(self):
        """计算利息 - 算头不算尾"""
        # 公式：本金 × 年利率 × 天数 / 360
        # 天数计算：算头不算尾，即 (结束日期 - 开始日期)
        self.days = (self.end_date - self.start_date).days
        self.interest = (
            self.principal_balance *
            self.rate *
            Decimal(str(self.days)) /
            Decimal('360')
        )
        # 四舍五入到2位小数
        self.interest = self.interest.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)


@dataclass
class RepaymentEvent:
    """还款事件"""
    date: datetime
    amount: Decimal
    type: str                           # 'principal', 'interest'
    description: str = ""


class NHRCBCalculator:
    """南海农商银行计算器 - 修正版"""

    def __init__(
        self,
        loan_amount: float,
        start_date: str,
        end_date: str,
        calculation_date: str,
        annual_rate: float = 0.048,      # 默认4.8%
        penalty_rate_multiplier: float = 1.5,  # 罚息利率倍数
        quarterly_repayment_rate: float = 0.02  # 每季度还本2%
    ):
        """
        初始化计算器

        Args:
            loan_amount: 贷款金额
            start_date: 起息日
            end_date: 到期日
            calculation_date: 计算截止日
            annual_rate: 年利率（如0.048表示4.8%）
            penalty_rate_multiplier: 罚息利率倍数
            quarterly_repayment_rate: 每季度还本比例
        """
        self.loan_amount = Decimal(str(loan_amount))
        self.start_date = self._parse_date(start_date)
        self.end_date = self._parse_date(end_date)
        self.calculation_date = self._parse_date(calculation_date)
        self.annual_rate = Decimal(str(annual_rate))
        self.penalty_rate = self.annual_rate * Decimal(str(penalty_rate_multiplier))
        self.quarterly_repayment_rate = Decimal(str(quarterly_repayment_rate))
        self.quarterly_repayment_amount = self.loan_amount * self.quarterly_repayment_rate

        # 计算期间列表
        self.periods: List[CalculationPeriod] = []

        # 还款事件列表
        self.repayments: List[RepaymentEvent] = []

        # 当前状态
        self.current_principal = self.loan_amount
        self.unpaid_interest = Decimal('0')

    def _parse_date(self, date_str):
        """解析日期字符串"""
        if isinstance(date_str, datetime):
            return date_str
        if isinstance(date_str, str):
            for fmt in ['%Y-%m-%d', '%Y-%m-%d %H:%M:%S', '%Y-%m-%d 00:00:00']:
                try:
                    return datetime.strptime(date_str, fmt)
                except:
                    continue
        return None

    def add_repayment(self, date: str, amount: float, type: str, description: str = ""):
        """添加还款记录"""
        self.repayments.append(RepaymentEvent(
            date=self._parse_date(date),
            amount=Decimal(str(amount)),
            type=type,
            description=description
        ))

    def calculate_periods(self) -> List[CalculationPeriod]:
        """
        计算所有计息期间

        南海农商银行按月计息，每月21日为结息日
        下一期的起始日期是上一期的结束日期 + 1天
        """
        periods = []
        current_date = self.start_date
        period_no = 1

        while current_date < self.calculation_date:
            # 找下一个21日
            if current_date.day <= 21:
                # 本月21日
                period_end = current_date.replace(day=21)
            else:
                # 下月21日
                if current_date.month == 12:
                    period_end = current_date.replace(year=current_date.year + 1, month=1, day=21)
                else:
                    period_end = current_date.replace(month=current_date.month + 1, day=21)

            # 确保不超过计算截止日
            if period_end > self.calculation_date:
                period_end = self.calculation_date

            # 创建计息期间
            period = CalculationPeriod(
                period_no=period_no,
                start_date=current_date,
                end_date=period_end,
                days=0,  # 稍后计算
                principal_balance=self.current_principal,
                rate=self.annual_rate
            )

            # 计算天数（算头不算尾）
            period.calculate_interest()

            # 应用本期内的还款（在计算完利息后应用）
            self._apply_repayments_in_period(period)

            periods.append(period)

            # 下一期的起始日期是本期结束日期 + 1天
            current_date = period.end_date + timedelta(days=1)
            period_no += 1

            # 检查是否还有还款（防止无限循环）
            if period_no > 1000:
                break

        self.periods = periods
        return periods

    def _apply_repayments_in_period(self, period: CalculationPeriod):
        """应用期间内的还款事件"""
        for repayment in self.repayments:
            if period.start_date <= repayment.date < period.end_date:
                # 还款发生在期间内（不包括结束日）
                if repayment.type == 'principal':
                    # 本金还款
                    self.current_principal -= repayment.amount
                    if self.current_principal < 0:
                        self.current_principal = Decimal('0')
            elif repayment.date == period.end_date:
                # 还款正好是结息日，在下一期应用
                pass

    def calculate(self) -> CalculationResult:
        """
        执行完整计算

        Returns:
            CalculationResult: 计算结果
        """
        result = CalculationResult()
        result.loan_amount = float(self.loan_amount)
        result.start_date = format_date(self.start_date)
        result.end_date = format_date(self.end_date)
        result.calculation_date = format_date(self.calculation_date)

        # 1. 计算所有计息期间
        periods = self.calculate_periods()

        # 2. 生成结果
        result.interest_details = []
        total_interest = Decimal('0')

        for period in periods:
            detail = InterestDetail(
                period_start=format_date(period.start_date),
                period_end=format_date(period.end_date),
                days=period.days,
                principal_balance=float(period.principal_balance),
                rate=float(period.rate * 100),  # 转换为百分比
                interest=float(period.interest)
            )
            result.interest_details.append(detail)
            total_interest += period.interest

        result.total_interest = float(total_interest.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP))
        result.remaining_principal = float(self.current_principal.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP))

        # 计算债权总额
        result.calculate_total()

        return result


def calculate_nhrcb_from_flow(
    flow_data: List[Dict[str, Any]],
    calculation_date: Optional[str] = None
) -> CalculationResult:
    """
    从流水数据计算债权

    Args:
        flow_data: 流水数据列表
        calculation_date: 计算截止日（可选，默认为今天）

    Returns:
        CalculationResult: 计算结果
    """
    if not flow_data:
        raise ValueError("流水数据不能为空")

    # 提取基本信息
    loan_amount = None
    start_date = None
    end_date = None

    # 提取还款记录
    repayments = []

    for row in flow_data:
        trans_type = row.get('交易类型', '')
        amount_str = str(row.get('交易金额', '0'))
        amount_str = amount_str.replace(',', '')

        is_negative = '-' in amount_str
        amount_str = amount_str.replace('-', '')

        try:
            amount = float(amount_str)
        except:
            continue

        if not is_negative and '放款' in trans_type:
            # 放款记录
            loan_amount = amount
            start_date = row.get('起息日')
            end_date = row.get('到期日期')

        elif is_negative and '本金' in trans_type:
            # 本金还款（借方）
            date_val = row.get('实际发生日')
            if date_val:
                repayments.append({
                    'date': str(date_val),
                    'amount': amount,
                    'type': 'principal'
                })

    if not loan_amount:
        raise ValueError("未找到贷款金额")

    if not start_date:
        raise ValueError("未找到起息日")

    if not end_date:
        raise ValueError("未找到到期日")

    # 如果没有指定计算截止日，使用今天
    if not calculation_date:
        calculation_date = datetime.now().strftime('%Y-%m-%d')

    # 创建计算器
    calculator = NHRCBCalculator(
        loan_amount=loan_amount,
        start_date=start_date,
        end_date=end_date,
        calculation_date=calculation_date
    )

    # 添加还款记录
    for repayment in repayments:
        calculator.add_repayment(
            date=repayment['date'],
            amount=repayment['amount'],
            type=repayment['type']
        )

    # 执行计算
    return calculator.calculate()


# 测试函数
def test_with_reference_data():
    """使用参考数据测试"""
    source_dir = Path(r'D:\我的资料库\Documents\xwechat_files\wxid_w4uaqxmkyyp622_5242\msg\file\2026-03\cs')
    xlsx_file = list(source_dir.glob('*.xlsx'))[0]

    import openpyxl
    wb = openpyxl.load_workbook(xlsx_file)
    ws = wb.active

    # 读取流水数据
    flow_data = []
    for row in ws.iter_rows(min_row=2, values_only=True):
        if row[0]:  # 序号不为空
            data_row = {}
            data_row['序号'] = row[0]
            data_row['流水号'] = row[1]
            data_row['借贷标志'] = row[2]
            data_row['交易类型'] = row[3]
            data_row['实际发生日'] = row[4]
            data_row['交易金额'] = row[5]
            data_row['起息日'] = row[6]
            data_row['到期日期'] = row[7]
            data_row['还款方式'] = row[8]
            data_row['摘要信息'] = row[9]
            flow_data.append(data_row)

    # 执行计算
    result = calculate_nhrcb_from_flow(
        flow_data=flow_data,
        calculation_date="2026-03-09"
    )

    # 输出结果
    print("\n" + "=" * 80)
    print("计算结果")
    print("=" * 80)
    print(f"贷款金额: {result.loan_amount:,.2f}")
    print(f"剩余本金: {result.remaining_principal:,.2f}")
    print(f"利息总额: {result.total_interest:,.2f}")
    print(f"债权总额: {result.total_debt:,.2f}")
    print("\n前10期利息明细:")
    print(f"{'期数':<6} {'期间':<25} {'天数':<6} {'本金余额':<15} {'年利率':<10} {'利息':<12}")
    print("-" * 80)
    for detail in result.interest_details[:10]:
        print(f"{detail.period_start.split('-')[2] + '/' + detail.period_start.split('-')[1]:<6} "
              f"{detail.period_start.split('-')[1] + '-' + detail.period_start.split('-')[2]} ~ "
              f"{detail.period_end.split('-')[1] + '-' + detail.period_end.split('-')[2]:<11} "
              f"{detail.days:<6} {detail.principal_balance:<15,.2f} {detail.rate:<10.2f} {detail.interest:<12,.2f}")

    return result


if __name__ == "__main__":
    test_with_reference_data()
