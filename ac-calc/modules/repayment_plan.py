"""
分段还本计划模块。

支持多段还款计划，每段可以有不同的：
- 起始日期（补充协议生效日）
- 还本方式（按计划还本、一次性还本付息、等额本息、等额本金）
- 还本频率、金额
- 结息频率

典型场景：原计划按季度还本 → 补充协议改为按半年还本
"""

from __future__ import annotations

from datetime import date, timedelta
from decimal import Decimal
from typing import Any, Dict, List, Optional, Tuple

from dateutil.relativedelta import relativedelta

from utils.date_utils import format_date, parse_date

# ============================================
# 频率映射
# ============================================
FREQ_MAP = {
    "每月": 1,
    "每季度": 3,
    "每半年": 6,
    "每年": 12,
    "月": 1,
    "季度": 3,
    "半年": 6,
    "年": 12,
}

FREQ_LABELS = ["每月", "每季度", "每半年", "每年"]

# ============================================
# 还本方式
# ============================================
REPAYMENT_METHODS = [
    "按计划还本（自定义）",
    "一次性还本付息",
    "等额本息",
    "等额本金",
]

# ============================================
# 计划段数据结构
# ============================================
"""RepaymentPlanSegment:
{
    "id": str,                    # 唯一标识
    "start_date": str (date),     # 生效起始日
    "method": str,                # 还本方式（见 REPAYMENT_METHODS）
    "frequency": str,             # 还本频率（仅"按计划还本"需要）
    "amount": float,              # 每期还本金额（仅"按计划还本"需要）
    "percent": float,             # 每期还本比例%（可选，与amount二选一）
    "first_repayment_date": str (date),  # 首次还本日
    "installment_count": int,     # 还款总月数（等额本息/等额本金需要）
    "interest_frequency": str,    # 结息频率
    "source": str,                # 来源："original" / "supplementary"
    "supplementary_text": str,    # 补充协议描述（仅supplementary）
}
"""


def build_segment(
    start_date: str,
    method: str = "按计划还本（自定义）",
    frequency: str = "每季度",
    amount: float = 0.0,
    percent: float = 0.0,
    first_repayment_date: Optional[str] = None,
    installment_count: int = 36,
    interest_frequency: str = "每月",
    source: str = "original",
    supplementary_text: str = "",
) -> Dict[str, Any]:
    """构建一个计划段字典。"""
    import uuid
    return {
        "id": str(uuid.uuid4())[:8],
        "start_date": start_date,
        "method": method,
        "frequency": frequency,
        "amount": amount,
        "percent": percent,
        "first_repayment_date": first_repayment_date or start_date,
        "installment_count": installment_count,
        "interest_frequency": interest_frequency,
        "source": source,
        "supplementary_text": supplementary_text,
    }


def generate_payment_dates(
    first_date: date,
    frequency: str,
    cutoff_date: date,
    start_from: Optional[date] = None,
) -> List[date]:
    """
    生成还款日期序列。
    
    Args:
        first_date: 首次还款日
        frequency: 频率（每月/每季度/每半年/每年）
        cutoff_date: 截止日
        start_from: 只返回 >= 此日期的日期（用于分段过滤）
    
    Returns:
        排序后的日期列表
    """
    months = FREQ_MAP.get(frequency, 3)
    dates = []
    cur = first_date
    while cur <= cutoff_date:
        if not start_from or cur >= start_from:
            dates.append(cur)
        # 下期日期
        m = cur.month + months
        y = cur.year + (m - 1) // 12
        m = ((m - 1) % 12) + 1
        try:
            cur = date(y, m, cur.day)
        except ValueError:
            # 处理月末日期不存在的问题（如1月31日+1月=2月28日）
            import calendar
            last_day = calendar.monthrange(y, m)[1]
            cur = date(y, m, min(cur.day, last_day))
    return dates


def generate_expected_schedule(
    segments: List[Dict[str, Any]],
    loan_amount: float,
    end_date: date,
    cutoff_date: date,
) -> List[Dict[str, Any]]:
    """
    根据多段计划生成预期还本时间表。
    
    核心逻辑：
    - 每段计划只生成从该段生效日到下一段生效日之间的还款日期
    - 按段顺序依次消耗本金
    - 同一日期的还款会合并（如果两个段的还款日相同）
    
    Args:
        segments: 计划段列表（按 start_date 排序）
        loan_amount: 贷款本金
        end_date: 合同到期日
        cutoff_date: 计算截止日
    
    Returns:
        预期还本记录列表，每项包含：
        - date: 应还日期
        - amount: 应还本金
        - segment_id: 所属计划段
        - source: 段来源
    """
    if not segments:
        return []
    
    # 按生效日排序
    sorted_segments = sorted(segments, key=lambda s: parse_date(s["start_date"]))
    
    # 计算每个段的生效日期范围 [seg_start, next_seg_start)
    segment_ranges = []
    for i, seg in enumerate(sorted_segments):
        seg_start = parse_date(seg["start_date"])
        if i + 1 < len(sorted_segments):
            seg_end = parse_date(sorted_segments[i + 1]["start_date"]) - timedelta(days=1)
        else:
            seg_end = cutoff_date
        segment_ranges.append((seg, seg_start, seg_end))
    
    schedule = []
    remaining = Decimal(str(loan_amount))
    
    for seg, seg_start, seg_end in segment_ranges:
        if remaining <= 0:
            break
        if seg_end < seg_start:
            continue  # 段区间无效
        
        seg_method = seg["method"]
        
        if seg_method == "一次性还本付息":
            repay_date = min(end_date, seg_end)
            if repay_date >= seg_start and remaining > 0:
                schedule.append({
                    "date": repay_date,
                    "amount": float(remaining),
                    "segment_id": seg["id"],
                    "source": seg.get("source", "original"),
                })
                remaining = Decimal("0")
            continue
        
        if seg_method in ("等额本息", "等额本金"):
            total_months = seg.get("installment_count", 36)
            monthly_principal = float(remaining) / max(total_months, 1)
            first_date = parse_date(seg.get("first_repayment_date", seg["start_date"]))
            
            for i in range(total_months):
                repay_date = first_date + relativedelta(months=i)
                if repay_date > seg_end or repay_date > cutoff_date:
                    break
                if repay_date < seg_start:
                    continue
                if remaining <= Decimal("0.01"):
                    break
                amt = min(monthly_principal, float(remaining))
                if i == total_months - 1:
                    amt = float(remaining)  # 尾差调整
                schedule.append({
                    "date": repay_date,
                    "amount": round(amt, 2),
                    "segment_id": seg["id"],
                    "source": seg.get("source", "original"),
                })
                remaining -= Decimal(str(amt))
            continue
        
        # "按计划还本（自定义）"
        freq = seg.get("frequency", "每季度")
        amount_per = Decimal(str(seg.get("amount", 0)))
        
        if amount_per <= 0 and seg.get("percent", 0) > 0:
            amount_per = Decimal(str(loan_amount * seg["percent"] / 100))
        
        if amount_per <= 0:
            continue
        
        first_date = parse_date(seg.get("first_repayment_date", seg["start_date"]))
        # 生成的日期限制在本段生效范围内 [seg_start, seg_end]
        payment_dates = generate_payment_dates(first_date, freq, seg_end, seg_start)
        
        for repay_date in payment_dates:
            if remaining <= Decimal("0.01"):
                break
            if repay_date > seg_end or repay_date < seg_start:
                continue
            
            # 每期还 amount_per，除非剩余本金不足
            amt = min(amount_per, remaining)
            schedule.append({
                "date": repay_date,
                "amount": round(float(amt), 2),
                "segment_id": seg["id"],
                "source": seg.get("source", "original"),
            })
            remaining -= amt
    
    # 如果还有剩余本金，追加到期还本
    if remaining > Decimal("0.01"):
        last_scheduled = max(s["date"] for s in schedule) if schedule else end_date
        residual_date = max(end_date, min(last_scheduled, cutoff_date))
        schedule.append({
            "date": residual_date,
            "amount": round(float(remaining), 2),
            "segment_id": "residual",
            "source": "residual",
        })
    
    return schedule


def calc_overdue_penalty_by_plan(
    loan_amount: float,
    segments: List[Dict[str, Any]],
    actual_repayments: List[Dict[str, Any]],
    penalty_rate: float,
    end_date: date,
    cutoff_date: date,
) -> Tuple[float, List[Dict[str, Any]]]:
    """
    基于分段计划计算逾期本金罚息。
    
    逻辑：
    1. 用 generate_expected_schedule 生成预期还本时间表
    2. 按每期汇总实际还本金额
    3. 对比预期与实际的差额，差额部分从到期日起按罚息利率计息
    4. 用截至下期到期日（或截止日）的天数计算罚息
    
    Args:
        loan_amount: 贷款本金
        segments: 分段还本计划
        actual_repayments: 实际还款记录
        penalty_rate: 罚息年利率（小数，如 0.05 表示 5%）
        end_date: 合同到期日
        cutoff_date: 计算截止日
    
    Returns:
        (total_penalty: float, details: list)
    """
    if not segments:
        return 0.0, []
    
    expected_schedule = generate_expected_schedule(segments, loan_amount, end_date, cutoff_date)
    if not expected_schedule:
        return 0.0, []
    
    # 按日期汇总实际还本
    actual_principal_map: Dict[date, Decimal] = {}
    for r in actual_repayments:
        r_type = r.get("type", "")
        if r_type in ("principal", "本金"):
            d = parse_date(r["date"])
            actual_principal_map[d] = actual_principal_map.get(d, Decimal("0")) + Decimal(str(r["amount"]))
    
    # 逐期计算逾期
    overdue_principal = Decimal("0")
    total_penalty = Decimal("0")
    details = []
    
    for i, expected in enumerate(expected_schedule):
        due_date = expected["date"]
        amt_due = Decimal(str(expected["amount"]))
        amt_paid = actual_principal_map.get(due_date, Decimal("0"))
        shortfall = max(Decimal("0"), amt_due - amt_paid)
        
        overdue_principal += shortfall
        
        if overdue_principal > 0:
            # 罚息期间：从到期日到下一期到期日（或截止日），但不超过计算截止日
            if i + 1 < len(expected_schedule):
                next_date = expected_schedule[i + 1]["date"]
                period_end = min(next_date - timedelta(days=1), cutoff_date)
            else:
                period_end = cutoff_date
            
            start = due_date
            if period_end < start:
                continue
            
            days = (period_end - start).days + 1
            penalty = float(overdue_principal) * penalty_rate * days / 360.0
            
            total_penalty += Decimal(str(round(penalty, 2)))
            details.append({
                "period": f"{format_date(start)} 至 {format_date(period_end)}",
                "due_date": format_date(due_date),
                "next_due": format_date(next_date) if i + 1 < len(expected_schedule) else "-",
                "days": days,
                "expected_principal": float(amt_due),
                "actual_principal": float(amt_paid),
                "shortfall": float(shortfall),
                "cumulative_overdue_principal": float(overdue_principal),
                "penalty": round(penalty, 2),
                "segment_id": expected.get("segment_id", ""),
            })
    
    return round(float(total_penalty), 2), details


def default_original_segment(
    loan_amount: float,
    start_date: date,
    frequency: str = "每季度",
    percent: float = 0.0,
    amount: float = 0.0,
    first_repay_date: Optional[date] = None,
) -> Dict[str, Any]:
    """
    创建默认的原始计划段。
    
    如果 percent/amount 未设置，自动按每期5%计算。
    amount 优先于 percent，如果两个都设置了，amount 生效。
    """
    if amount <= 0:
        if percent > 0:
            amount = loan_amount * percent / 100
        else:
            amount = loan_amount * 0.05  # 默认每期5%
            percent = 5.0
    
    return build_segment(
        start_date=str(start_date),
        method="按计划还本（自定义）",
        frequency=frequency,
        amount=round(amount, 2),
        percent=percent,
        first_repayment_date=str(first_repay_date or start_date),
        source="original",
    )


def segment_from_supplementary(supp_data: Dict[str, Any], original_end_date: date) -> Dict[str, Any]:
    """
    从补充协议AI解析结果构建计划段。
    
    Args:
        supp_data: AI解析结果字典
        original_end_date: 原合同到期日
    
    Returns:
        计划段字典
    """
    effective_date = supp_data.get("effective_date", str(original_end_date))
    freq = supp_data.get("repayment_frequency", "每季度")
    amount_str = supp_data.get("repayment_amount", "0")
    try:
        amount = float(amount_str)
    except (ValueError, TypeError):
        amount = 0.0
    interest_freq = supp_data.get("interest_frequency", "每月")
    first_repay = supp_data.get("first_repayment_date", effective_date)
    
    return build_segment(
        start_date=effective_date,
        method="按计划还本（自定义）",
        frequency=freq,
        amount=amount,
        first_repayment_date=first_repay,
        interest_frequency=interest_freq,
        source="supplementary",
        supplementary_text=supp_data.get("notes", ""),
    )
