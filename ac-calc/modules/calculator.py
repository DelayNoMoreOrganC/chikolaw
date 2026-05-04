"""
债权计算引擎。

当前版本按“每月21日至次月20日”为计息周期，并支持：
- 以贷款本金作为自动计算的起算本金
- 数据暂计日作为计算截止日
- 到期次日起按罚息利率计收罚息
- 利率调整按调整日当天生效
- 利息/罚息复利从次月起按上期结余余额计收
- 将次期首日（通常为 21 日）归还利息/罚息/复利的记录归属到上期
"""

from __future__ import annotations

from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP, getcontext
from typing import Any, Dict, List, Optional, Union

from dateutil.relativedelta import relativedelta

from utils.constants import (
    DEFAULT_DECIMAL_PRECISION,
    DEFAULT_REPAYMENT_PRIORITY,
    DateCalculationMethod,
    DayCountConvention,
    RepaymentType,
)
from utils.date_utils import format_date, get_current_date, parse_date


getcontext().prec = 28

MoneyDict = Dict[str, Decimal]


class DebtCalculator:
    """债权计算器。"""

    def __init__(
        self,
        day_count_convention: DayCountConvention = DayCountConvention.ACTUAL_360,
        date_calculation: DateCalculationMethod = DateCalculationMethod.HEAD_EXCLUSIVE,
        repayment_priority: Optional[List[RepaymentType]] = None,
        penalty_rate_multiplier: float = 1.5,
        enable_compound: bool = True,
        enable_penalty: bool = True,
        decimal_precision: int = DEFAULT_DECIMAL_PRECISION,
    ):
        self.day_count_convention = day_count_convention
        self.date_calculation = date_calculation
        self.repayment_priority = repayment_priority or DEFAULT_REPAYMENT_PRIORITY
        self.penalty_rate_multiplier = penalty_rate_multiplier
        self.enable_compound = enable_compound
        self.enable_penalty = enable_penalty
        self.decimal_precision = decimal_precision
        self.day_count_base = 365 if day_count_convention == DayCountConvention.ACTUAL_365 else 360

    def calculate(
        self,
        loan_data: Dict[str, Any],
        repayment_records: Optional[List[Dict[str, Any]]] = None,
        rate_adjustments: Optional[List[Dict[str, Any]]] = None,
        calculation_date: Optional[date] = None,
        expected_principal_schedule: Optional[List[Dict[str, Any]]] = None,
    ) -> Dict[str, Any]:
        loan_amount = Decimal(str(loan_data["loan_amount"]))
        annual_rate = Decimal(str(self._normalize_rate(loan_data["annual_interest_rate"])))
        start_date = parse_date(loan_data["start_date"])
        end_date = parse_date(loan_data["end_date"])
        expected_remaining_principal = Decimal(str(loan_data.get("remaining_principal", 0) or 0))
        expected_interest_total = Decimal(str(loan_data.get("accrued_interest", 0) or 0))
        expected_penalty_total = Decimal(str(loan_data.get("penalty_interest", 0) or 0))
        expected_compound_total = Decimal(str(loan_data.get("compound_interest", 0) or 0))
        penalty_start_date = end_date + timedelta(days=1)

        if calculation_date is None:
            calculation_date = get_current_date()
        if calculation_date < start_date:
            raise ValueError("数据暂计日不能早于合同起始日")

        repayment_items = self._prepare_repayments(repayment_records or [])
        adjustment_items = self._prepare_adjustments(rate_adjustments or [])
        repayments_by_date = self._group_by_date(repayment_items)
        adjustments_by_date = self._group_by_date(adjustment_items)
        periods = self._generate_monthly_periods(start_date, calculation_date)

        # ---------- 新增：解析预期还本计划 ----------
        expected_principal_by_date: Dict[date, Decimal] = {}
        actual_principal_by_date: Dict[date, Decimal] = {}
        if expected_principal_schedule:
            for item in expected_principal_schedule:
                d = parse_date(item["date"])
                expected_principal_by_date[d] = expected_principal_by_date.get(d, Decimal("0")) + Decimal(str(item["amount"]))
        for item in repayment_items:
            if item["type"] == RepaymentType.PRINCIPAL.value:
                d = item["parsed_date"]
                actual_principal_by_date[d] = actual_principal_by_date.get(d, Decimal("0")) + item["amount"]
        overdue_principal = Decimal("0")  # 累计逾期本金
        # ------------------------------------

        current_rate = annual_rate
        for adjustment in adjustment_items:
            if adjustment["parsed_date"] <= start_date:
                current_rate = adjustment["normalized_rate"]

        balances: MoneyDict = {
            "principal": loan_amount,
            "interest": Decimal("0"),
            "penalty": Decimal("0"),
            "compound_interest": Decimal("0"),
            "compound_penalty": Decimal("0"),
        }

        consumed_repayment_ids = set()
        detail_records: List[Dict[str, Any]] = []
        processed_repayments: List[Dict[str, Any]] = []

        for seq_no, period in enumerate(periods, start=1):
            period_start = period["start_date"]
            period_end = period["end_date"]
            opening_balances = self._copy_balances(balances)
            compound_base_interest = opening_balances["interest"]
            compound_base_penalty = opening_balances["penalty"]

            cycle_record = self._build_cycle_record(seq_no, period_start, period_end, opening_balances)
            interest_formula_parts: List[str] = []
            interest_compound_formula_parts: List[str] = []
            penalty_formula_parts: List[str] = []
            penalty_compound_formula_parts: List[str] = []
            repayment_formula_parts: List[str] = []
            remark_parts: List[str] = []

            cursor = period_start
            while cursor <= period_end:
                if cursor in adjustments_by_date:
                    current_rate = adjustments_by_date[cursor][-1]["normalized_rate"]
                    adjustment_text = "、".join(
                        self._format_rate(item["normalized_rate"]) for item in adjustments_by_date[cursor]
                    )
                    remark_parts.append(f"{format_date(cursor)}利率调整为{adjustment_text}")

                segment_end = self._resolve_segment_end(
                    cursor=cursor,
                    period_end=period_end,
                    repayments_by_date=repayments_by_date,
                    adjustments_by_date=adjustments_by_date,
                    penalty_start_date=penalty_start_date,
                    consumed_repayment_ids=consumed_repayment_ids,
                )

                # ---------- 检查该段起始日是否有预期还本到期 ----------
                if expected_principal_schedule and cursor in expected_principal_by_date:
                    expected_amt = expected_principal_by_date[cursor]
                    actual_amt = actual_principal_by_date.get(cursor, Decimal("0"))
                    cycle_record["principal_due"] += float(expected_amt)
                    shortfall = max(Decimal("0"), expected_amt - actual_amt)
                    overdue_principal += shortfall
                    if actual_amt > expected_amt:
                        overdue_principal = max(Decimal("0"), overdue_principal - (actual_amt - expected_amt))
                
                days = self._calculate_segment_days(cursor, segment_end)
                normal_rate = current_rate
                penalty_rate = normal_rate * Decimal(str(self.penalty_rate_multiplier)) if self.enable_penalty else Decimal("0")

                principal_base = balances["principal"]
                interest_amount = self._calculate_amount(principal_base, normal_rate, days)
                # 罚息基于逾期本金计算（不受 penalty_start_date 限制）
                interest_days = days  # 利息照常计算
                penalty_amount = self._calculate_amount(overdue_principal, penalty_rate, days) if self.enable_penalty and overdue_principal > 0 else Decimal("0")
                interest_compound = self._calculate_amount(compound_base_interest, normal_rate, days if self.enable_compound else 0)
                # 仅利息余额计收复利（参考行标准），罚息不计收复利
                penalty_compound = Decimal("0")

                balances["interest"] += interest_amount
                balances["penalty"] += penalty_amount
                balances["compound_interest"] += interest_compound
                balances["compound_penalty"] += penalty_compound

                cycle_record["new_interest"] += self._to_float(interest_amount)
                cycle_record["new_penalty"] += self._to_float(penalty_amount)
                cycle_record["new_interest_compound"] += self._to_float(interest_compound)
                cycle_record["new_penalty_compound"] += self._to_float(penalty_compound)

                cycle_record["segments"].append(
                    {
                        "start_date": format_date(cursor),
                        "end_date": format_date(segment_end),
                        "days": days,
                        "principal_base": self._to_float(principal_base),
                        "normal_rate": self._to_float(normal_rate * Decimal("100")),
                        "penalty_rate": self._to_float(penalty_rate * Decimal("100")),
                        "interest_amount": self._to_float(interest_amount),
                        "penalty_amount": self._to_float(penalty_amount),
                        "interest_compound_amount": self._to_float(interest_compound),
                        "penalty_compound_amount": self._to_float(penalty_compound),
                    }
                )

                if interest_amount > 0:
                    interest_formula_parts.append(self._render_amount_formula(principal_base, normal_rate, interest_days, interest_amount))
                if interest_compound > 0:
                    interest_compound_formula_parts.append(
                        self._render_amount_formula(compound_base_interest, normal_rate, days, interest_compound, label="上期利息余额")
                    )
                if penalty_amount > 0:
                    penalty_formula_parts.append(self._render_amount_formula(overdue_principal, penalty_rate, days, penalty_amount))
                if penalty_compound > 0:
                    penalty_compound_formula_parts.append(
                        self._render_amount_formula(compound_base_penalty, penalty_rate, days, penalty_compound, label="上期罚息余额")
                    )

                # 检查段结束日是否有预期还本到期
                if expected_principal_schedule and segment_end in expected_principal_by_date and segment_end != cursor:
                    expected_amt = expected_principal_by_date[segment_end]
                    actual_amt = actual_principal_by_date.get(segment_end, Decimal("0"))
                    cycle_record["principal_due"] += float(expected_amt)
                    shortfall = max(Decimal("0"), expected_amt - actual_amt)
                    overdue_principal += shortfall
                    if actual_amt > expected_amt:
                        overdue_principal = max(Decimal("0"), overdue_principal - (actual_amt - expected_amt))
                    # 加速到期到达：逾期本金不超过剩余本金
                    if overdue_principal > balances["principal"]:
                        overdue_principal = balances["principal"]

                if segment_end in repayments_by_date:
                    for repayment in repayments_by_date[segment_end]:
                        if repayment["id"] in consumed_repayment_ids:
                            continue
                        allocation = self._allocate_repayment(repayment["amount"], balances, repayment["type"])
                        consumed_repayment_ids.add(repayment["id"])
                        self._apply_allocation(balances, allocation)
                        compound_base_interest = max(Decimal("0"), compound_base_interest - allocation["interest"])
                        compound_base_penalty = max(Decimal("0"), compound_base_penalty - allocation["penalty"])
                        self._merge_repayment_into_cycle(
                            cycle_record,
                            repayment,
                            allocation,
                            repayment_formula_parts,
                            processed_repayments,
                            "当期内还款",
                        )

                cursor = segment_end + timedelta(days=1)

            next_period_start = period_end + timedelta(days=1)
            if next_period_start <= calculation_date and next_period_start in repayments_by_date:
                for repayment in repayments_by_date[next_period_start]:
                    if repayment["id"] in consumed_repayment_ids:
                        continue
                    if repayment["type"] not in {RepaymentType.INTEREST.value, RepaymentType.PENALTY.value, RepaymentType.COMPOUND.value}:
                        continue
                    allocation = self._allocate_repayment(repayment["amount"], balances, repayment["type"])
                    consumed_repayment_ids.add(repayment["id"])
                    self._apply_allocation(balances, allocation)
                    self._merge_repayment_into_cycle(
                        cycle_record,
                        repayment,
                        allocation,
                        repayment_formula_parts,
                        processed_repayments,
                        "次期首日归属上期",
                    )
                    remark_parts.append(f"{format_date(next_period_start)}的{RepaymentType.get_display_name(repayment['type'])}归属本期")

            # 每期末四舍五入各余额到2位小数，防止尾差累积
            for bal_key in ["interest", "penalty", "compound_interest", "compound_penalty"]:
                balances[bal_key] = balances[bal_key].quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

            cycle_record["principal_balance"] = self._to_float(balances["principal"])
            cycle_record["cumulative_overdue_principal"] = float(overdue_principal)
            cycle_record["interest_balance"] = self._to_float(balances["interest"])
            cycle_record["interest_compound_balance"] = self._to_float(balances["compound_interest"])
            cycle_record["penalty_balance"] = self._to_float(balances["penalty"])
            cycle_record["penalty_compound_balance"] = self._to_float(balances["compound_penalty"])

            cycle_record["new_interest_formula"] = self._combine_formula_parts(interest_formula_parts, Decimal(str(cycle_record["new_interest"])), "本期无正常利息")
            cycle_record["new_interest_compound_formula"] = self._combine_formula_parts(interest_compound_formula_parts, Decimal(str(cycle_record["new_interest_compound"])), "本期无应计利息复利")
            cycle_record["new_penalty_formula"] = self._combine_formula_parts(penalty_formula_parts, Decimal(str(cycle_record["new_penalty"])), "本期无新增罚息")
            cycle_record["new_penalty_compound_formula"] = self._combine_formula_parts(penalty_compound_formula_parts, Decimal(str(cycle_record["new_penalty_compound"])), "本期无应计罚息复利")
            cycle_record["repayment_formula"] = "；".join(repayment_formula_parts) if repayment_formula_parts else "本期无还款"
            cycle_record["principal_balance_formula"] = (
                f"{self._format_money(opening_balances['principal'])} - {self._format_money(Decimal(str(cycle_record['principal_repaid'])))} = {self._format_money(balances['principal'])}"
            )
            cycle_record["interest_balance_formula"] = (
                f"{self._format_money(opening_balances['interest'])} + {self._format_money(Decimal(str(cycle_record['new_interest'])))} - {self._format_money(Decimal(str(cycle_record['repaid_interest'])))} = {self._format_money(balances['interest'])}"
            )
            cycle_record["interest_compound_balance_formula"] = (
                f"{self._format_money(opening_balances['compound_interest'])} + {self._format_money(Decimal(str(cycle_record['new_interest_compound'])))} - {self._format_money(Decimal(str(cycle_record['repaid_interest_compound'])))} = {self._format_money(balances['compound_interest'])}"
            )
            cycle_record["penalty_balance_formula"] = (
                f"{self._format_money(opening_balances['penalty'])} + {self._format_money(Decimal(str(cycle_record['new_penalty'])))} - {self._format_money(Decimal(str(cycle_record['repaid_penalty'])))} = {self._format_money(balances['penalty'])}"
            )
            cycle_record["penalty_compound_balance_formula"] = (
                f"{self._format_money(opening_balances['compound_penalty'])} + {self._format_money(Decimal(str(cycle_record['new_penalty_compound'])))} - {self._format_money(Decimal(str(cycle_record['repaid_penalty_compound'])))} = {self._format_money(balances['compound_penalty'])}"
            )

            if remark_parts:
                cycle_record["remark"] = "；".join(remark_parts) if not cycle_record["remark"] else f"{cycle_record['remark']} | {'；'.join(remark_parts)}"

            detail_records.append(self._round_cycle_record(cycle_record))

        computed_compound_total = balances["compound_interest"] + balances["compound_penalty"]
        total_amount = balances["principal"] + balances["interest"] + balances["penalty"] + balances["compound_interest"] + balances["compound_penalty"]

        return {
            "summary": {
                "loan_amount": self._to_float(loan_amount),
                "remaining_principal": self._to_float(balances["principal"]),
                "input_remaining_principal": self._to_float(expected_remaining_principal),
                "remaining_principal_difference": self._to_float(balances["principal"] - expected_remaining_principal),
                "accrued_interest": self._to_float(balances["interest"]),
                "input_accrued_interest": self._to_float(expected_interest_total),
                "accrued_interest_difference": self._to_float(balances["interest"] - expected_interest_total),
                "penalty_interest": self._to_float(balances["penalty"]),
                "input_penalty_interest": self._to_float(expected_penalty_total),
                "penalty_interest_difference": self._to_float(balances["penalty"] - expected_penalty_total),
                "compound_interest": self._to_float(computed_compound_total),
                "input_compound_interest": self._to_float(expected_compound_total),
                "compound_difference": self._to_float(computed_compound_total - expected_compound_total),
                "total_amount": self._to_float(total_amount),
                "calculation_date": format_date(calculation_date),
            },
            "detail": detail_records,
            "repayment_details": processed_repayments,
        }

    def _build_cycle_record(self, seq_no: int, period_start: date, period_end: date, opening_balances: MoneyDict) -> Dict[str, Any]:
        return {
            "seq_no": seq_no,
            "period": f"{format_date(period_start)} 至 {format_date(period_end)}",
            "start_date": format_date(period_start),
            "end_date": format_date(period_end),
            "days": self._calculate_segment_days(period_start, period_end),
            "opening_principal_balance": self._to_float(opening_balances["principal"]),
            "principal_balance": self._to_float(opening_balances["principal"]),
            "principal_repaid": 0.0,
            "new_interest": 0.0,
            "repaid_interest": 0.0,
            "interest_balance": self._to_float(opening_balances["interest"]),
            "new_interest_compound": 0.0,
            "repaid_interest_compound": 0.0,
            "interest_compound_balance": self._to_float(opening_balances["compound_interest"]),
            "new_penalty": 0.0,
            "repaid_penalty": 0.0,
            "penalty_balance": self._to_float(opening_balances["penalty"]),
            "new_penalty_compound": 0.0,
            "repaid_penalty_compound": 0.0,
            "penalty_compound_balance": self._to_float(opening_balances["compound_penalty"]),
            "principal_balance_formula": "",
            "new_interest_formula": "",
            "interest_balance_formula": "",
            "new_interest_compound_formula": "",
            "interest_compound_balance_formula": "",
            "new_penalty_formula": "",
            "penalty_balance_formula": "",
            "new_penalty_compound_formula": "",
            "penalty_compound_balance_formula": "",
            "repayment_formula": "",
            "remark": "",
            "repayments": [],
            "segments": [],
            "principal_due": 0.0,
            "cumulative_overdue_principal": 0.0,
        }

    def _prepare_repayments(self, repayment_records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        items = []
        for idx, record in enumerate(repayment_records):
            items.append({
                "id": idx,
                "date": record["date"],
                "parsed_date": parse_date(record["date"]),
                "type": record.get("type", RepaymentType.INTEREST.value),
                "amount": Decimal(str(record["amount"])),
            })
        return sorted(items, key=lambda item: (item["parsed_date"], item["id"]))

    def _prepare_adjustments(self, rate_adjustments: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        items = []
        for idx, record in enumerate(rate_adjustments):
            items.append({
                "id": idx,
                "date": record["date"],
                "parsed_date": parse_date(record["date"]),
                "normalized_rate": Decimal(str(self._normalize_rate(record["rate"]))),
            })
        return sorted(items, key=lambda item: (item["parsed_date"], item["id"]))

    def _group_by_date(self, records: List[Dict[str, Any]]) -> Dict[date, List[Dict[str, Any]]]:
        grouped: Dict[date, List[Dict[str, Any]]] = {}
        for item in records:
            grouped.setdefault(item["parsed_date"], []).append(item)
        return grouped

    def _generate_monthly_periods(self, start_date: date, end_date: date) -> List[Dict[str, Any]]:
        periods = []
        current_start = start_date
        while current_start <= end_date:
            if current_start.day <= 20:
                period_end = date(current_start.year, current_start.month, 20)
            else:
                next_month = current_start + relativedelta(months=1)
                period_end = date(next_month.year, next_month.month, 20)
            if period_end > end_date:
                period_end = end_date
            periods.append({"start_date": current_start, "end_date": period_end})
            current_start = period_end + timedelta(days=1)
        return periods

    def _resolve_segment_end(
        self,
        cursor: date,
        period_end: date,
        repayments_by_date: Dict[date, List[Dict[str, Any]]],
        adjustments_by_date: Dict[date, List[Dict[str, Any]]],
        penalty_start_date: date,
        consumed_repayment_ids: set,
    ) -> date:
        segment_end = period_end
        future_repayment_dates = [
            rep_date
            for rep_date, items in repayments_by_date.items()
            if cursor <= rep_date <= period_end and any(item["id"] not in consumed_repayment_ids for item in items)
        ]
        if future_repayment_dates:
            segment_end = min(segment_end, min(future_repayment_dates))
        future_adjustment_dates = [adj_date for adj_date in adjustments_by_date.keys() if cursor < adj_date <= period_end]
        if future_adjustment_dates:
            segment_end = min(segment_end, min(future_adjustment_dates) - timedelta(days=1))
        if self.enable_penalty and cursor < penalty_start_date <= period_end:
            segment_end = min(segment_end, penalty_start_date - timedelta(days=1))
        return segment_end

    def _calculate_segment_days(self, start_date: date, end_date: date) -> int:
        return 0 if start_date > end_date else (end_date - start_date).days + 1

    def _calculate_amount(self, base: Decimal, rate: Decimal, days: int) -> Decimal:
        if base <= 0 or rate <= 0 or days <= 0:
            return Decimal("0")
        raw = base * rate * Decimal(days) / Decimal(self.day_count_base)
        return raw.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    def _allocate_repayment(self, amount: Decimal, balances: MoneyDict, payment_type: str) -> MoneyDict:
        allocation: MoneyDict = {
            "principal": Decimal("0"),
            "interest": Decimal("0"),
            "penalty": Decimal("0"),
            "compound_interest": Decimal("0"),
            "compound_penalty": Decimal("0"),
            "fee": Decimal("0"),
        }
        remaining = amount
        direct_allocation_map = {
            RepaymentType.PRINCIPAL.value: "principal",
            RepaymentType.INTEREST.value: "interest",
            RepaymentType.PENALTY.value: "penalty",
            RepaymentType.COMPOUND.value: "compound_interest",
        }
        direct_key = direct_allocation_map.get(payment_type)
        if direct_key and balances.get(direct_key, Decimal("0")) > 0:
            deductible = min(remaining, balances[direct_key])
            allocation[direct_key] = deductible
            remaining -= deductible
        priority_map = {
            RepaymentType.FEE.value: ["fee"],
            RepaymentType.PENALTY.value: ["penalty"],
            RepaymentType.COMPOUND.value: ["compound_penalty", "compound_interest"],
            RepaymentType.INTEREST.value: ["interest"],
            RepaymentType.PRINCIPAL.value: ["principal"],
        }
        for priority in self.repayment_priority:
            if remaining <= 0:
                break
            priority_key = priority.value if isinstance(priority, RepaymentType) else str(priority)
            for balance_key in priority_map.get(priority_key, []):
                if remaining <= 0:
                    break
                balance_value = balances.get(balance_key, Decimal("0"))
                if balance_value <= 0:
                    continue
                deductible = min(remaining, balance_value)
                allocation[balance_key] += deductible
                remaining -= deductible
        allocation["unapplied"] = remaining
        return allocation

    def _apply_allocation(self, balances: MoneyDict, allocation: MoneyDict) -> None:
        for key in ["principal", "interest", "penalty", "compound_interest", "compound_penalty"]:
            balances[key] -= allocation.get(key, Decimal("0"))

    def _merge_repayment_into_cycle(
        self,
        cycle_record: Dict[str, Any],
        repayment: Dict[str, Any],
        allocation: MoneyDict,
        repayment_formula_parts: List[str],
        processed_repayments: List[Dict[str, Any]],
        attributed_source: str,
    ) -> None:
        cycle_record["principal_repaid"] += self._to_float(allocation["principal"])
        cycle_record["repaid_interest"] += self._to_float(allocation["interest"])
        cycle_record["repaid_penalty"] += self._to_float(allocation["penalty"])
        cycle_record["repaid_interest_compound"] += self._to_float(allocation["compound_interest"])
        cycle_record["repaid_penalty_compound"] += self._to_float(allocation["compound_penalty"])

        repayment_formula_parts.append(
            f"{repayment['date']} {RepaymentType.get_display_name(repayment['type'])} {self._format_money(repayment['amount'])}"
            f"（本金{self._format_money(allocation['principal'])}，利息{self._format_money(allocation['interest'])}，"
            f"利息复利{self._format_money(allocation['compound_interest'])}，罚息{self._format_money(allocation['penalty'])}，"
            f"罚息复利{self._format_money(allocation['compound_penalty'])}）"
        )

        cycle_record["repayments"].append(
            {
                "date": repayment["date"],
                "type": repayment["type"],
                "amount": self._to_float(repayment["amount"]),
                "source": attributed_source,
                "allocation": {
                    "principal": self._to_float(allocation["principal"]),
                    "interest": self._to_float(allocation["interest"]),
                    "penalty": self._to_float(allocation["penalty"]),
                    "compound_interest": self._to_float(allocation["compound_interest"]),
                    "compound_penalty": self._to_float(allocation["compound_penalty"]),
                    "unapplied": self._to_float(allocation["unapplied"]),
                },
            }
        )

        processed_repayments.append(
            {
                "date": repayment["date"],
                "type": repayment["type"],
                "amount": self._to_float(repayment["amount"]),
                "attributed_period_seq": cycle_record["seq_no"],
                "attributed_period": cycle_record["period"],
                "source": attributed_source,
                "allocation": {
                    "principal": self._to_float(allocation["principal"]),
                    "interest": self._to_float(allocation["interest"]),
                    "penalty": self._to_float(allocation["penalty"]),
                    "compound_interest": self._to_float(allocation["compound_interest"]),
                    "compound_penalty": self._to_float(allocation["compound_penalty"]),
                    "unapplied": self._to_float(allocation["unapplied"]),
                },
            }
        )

    def _combine_formula_parts(self, parts: List[str], total: Decimal, empty_text: str) -> str:
        if not parts:
            return empty_text
        return " + ".join(parts) + f" = {self._format_money(total)}"

    def _render_amount_formula(self, base: Decimal, rate: Decimal, days: int, amount: Decimal, label: str = "本金") -> str:
        return f"{label}{self._format_money(base)} × {self._format_rate(rate)} × {days} ÷ {self.day_count_base} = {self._format_money(amount)}"

    def _copy_balances(self, balances: MoneyDict) -> MoneyDict:
        return {key: Decimal(str(value)) for key, value in balances.items()}

    def _round_cycle_record(self, cycle_record: Dict[str, Any]) -> Dict[str, Any]:
        rounded = dict(cycle_record)
        for key in [
            "opening_principal_balance",
            "principal_balance",
            "principal_repaid",
            "new_interest",
            "repaid_interest",
            "interest_balance",
            "new_interest_compound",
            "repaid_interest_compound",
            "interest_compound_balance",
            "new_penalty",
            "repaid_penalty",
            "penalty_balance",
            "new_penalty_compound",
            "repaid_penalty_compound",
            "penalty_compound_balance",
        ]:
            rounded[key] = round(float(rounded[key]), self.decimal_precision)
        return rounded

    def _normalize_rate(self, rate_value: Union[str, float, Decimal]) -> float:
        if isinstance(rate_value, str):
            rate_value = rate_value.strip().replace("%", "")
        rate = float(rate_value)
        return rate / 100 if rate > 1 else rate

    def _format_money(self, value: Decimal) -> str:
        quantized = Decimal(str(value)).quantize(Decimal("1." + ("0" * self.decimal_precision)), rounding=ROUND_HALF_UP)
        return f"{quantized:.{self.decimal_precision}f}"

    def _format_rate(self, value: Decimal) -> str:
        display = (Decimal(str(value)) * Decimal("100")).quantize(Decimal("1.0000"), rounding=ROUND_HALF_UP)
        return f"{display:.4f}%"

    def _to_float(self, value: Decimal) -> float:
        quantized = Decimal(str(value)).quantize(Decimal("1." + ("0" * self.decimal_precision)), rounding=ROUND_HALF_UP)
        return float(quantized)


def create_calculator(config: Dict[str, Any]) -> DebtCalculator:
    calc_config = config.get("calculation", {})
    return DebtCalculator(
        day_count_convention=DayCountConvention(calc_config.get("day_count_convention", "actual/360")),
        date_calculation=DateCalculationMethod(calc_config.get("date_calculation", "head_exclusive")),
        penalty_rate_multiplier=calc_config.get("penalty_rate_multiplier", 1.5),
        enable_compound=calc_config.get("enable_compound_interest", True),
        enable_penalty=calc_config.get("enable_penalty_interest", True),
        decimal_precision=calc_config.get("decimal_precision", 2),
    )
