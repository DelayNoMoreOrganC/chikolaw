"""
对账单模板解析器

面向“对账单 + 系统截图”场景，优先使用规则提取稳定字段和还款流水，
再由上层决定是否使用 LLM 进行补充。
"""

import re
from typing import Any, Dict, List, Optional, Tuple


def extract_statement_template_data(text: str) -> Dict[str, Any]:
    """
    从 OCR 文本中提取对账单模板字段。

    Returns:
        {
            "data": {...},
            "repayment_records": [...],
            "pending_repayment_records": [...],
            "field_sources": {...},
            "warnings": [...]
        }
    """
    normalized_text = normalize_statement_text(text)
    field_sources: Dict[str, str] = {}
    warnings: List[str] = []

    data: Dict[str, Any] = {
        "loan_amount": None,
        "annual_interest_rate": None,
        "start_date": None,
        "end_date": None,
        "calculation_date": None,
        "remaining_principal": None,
        "accrued_interest": 0,
        "penalty_interest": 0,
        "compound_interest": 0,
        "rate_adjustments": [],
    }

    field_extractors = {
        "loan_amount": [
            r"(贷款金额[:：]?\s*[0-9,\.\s]+)",
            r"(借款金额[:：]?\s*[0-9,\.\s]+)",
        ],
        "start_date": [
            r"(合同起始日(?:期)?[，,:：]?\s*[0-9]{4}[年./-][0-9]{1,2}[月./-][0-9]{1,2}日?)",
        ],
        "end_date": [
            r"(合同到期日(?:期)?[，,:：]?\s*[0-9]{4}[年./-][0-9]{1,2}[月./-][0-9]{1,2}日?)",
        ],
        "calculation_date": [
            r"(查询结束日期?[，,:：]?\s*[0-9]{4}[年./-][0-9]{1,2}[月./-][0-9]{1,2}日?)",
            r"(统计截止日期?[，,:：]?\s*[0-9]{4}[年./-][0-9]{1,2}[月./-][0-9]{1,2}日?)",
        ],
        "annual_interest_rate": [
            r"(年利率[:：]?\s*[0-9]+(?:\.[0-9]+)?\s*%)",
            r"(执行利率[:：]?\s*[0-9]+(?:\.[0-9]+)?\s*%)",
            r"(利率[:：]?\s*[0-9]+(?:\.[0-9]+)?\s*%)",
            r"(月利率[:：]?\s*[0-9]+(?:\.[0-9]+)?\s*%)",
        ],
    }

    loan_amount_match = _find_first_match(normalized_text, field_extractors["loan_amount"])
    if loan_amount_match:
        data["loan_amount"] = parse_amount(loan_amount_match)
        field_sources["loan_amount"] = loan_amount_match

    start_date_match = _find_first_match(normalized_text, field_extractors["start_date"])
    if start_date_match:
        data["start_date"] = normalize_date_string(start_date_match)
        field_sources["start_date"] = start_date_match

    end_date_match = _find_first_match(normalized_text, field_extractors["end_date"])
    if end_date_match:
        data["end_date"] = normalize_date_string(end_date_match)
        field_sources["end_date"] = end_date_match

    calculation_date_match = _find_first_match(normalized_text, field_extractors["calculation_date"])
    if calculation_date_match:
        data["calculation_date"] = normalize_date_string(calculation_date_match)
        field_sources["calculation_date"] = calculation_date_match

    rate_match = _find_first_match(normalized_text, field_extractors["annual_interest_rate"])
    if rate_match:
        parsed_rate = parse_rate(rate_match)
        if parsed_rate is not None:
            data["annual_interest_rate"] = parsed_rate
            field_sources["annual_interest_rate"] = rate_match

    repayment_records, pending_records, balance_hint = extract_repayment_records(normalized_text)
    if repayment_records:
        field_sources["repayment_records"] = "流水明细逐行解析"

    if balance_hint is not None:
        data["remaining_principal"] = balance_hint["amount"]
        field_sources["remaining_principal"] = balance_hint["source"]

    if not repayment_records and not pending_records:
        warnings.append("未从对账单流水中识别出可直接写入的还款记录。")

    return {
        "data": data,
        "repayment_records": repayment_records,
        "pending_repayment_records": pending_records,
        "field_sources": field_sources,
        "warnings": warnings,
    }


def normalize_statement_text(text: str) -> str:
    normalized = text or ""
    replacements = {
        "，": ",",
        "。": ".",
        "：": ":",
        "；": ";",
        "（": "(",
        "）": ")",
        "【": "[",
        "】": "]",
        "“": "\"",
        "”": "\"",
        "‘": "'",
        "’": "'",
        "L,": "1,",
        "I,": "1,",
        "l,": "1,",
    }
    for source, target in replacements.items():
        normalized = normalized.replace(source, target)

    normalized = re.sub(r"[ \t]+", " ", normalized)
    return normalized


def extract_repayment_records(text: str) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]], Optional[Dict[str, Any]]]:
    records: List[Dict[str, Any]] = []
    pending_records: List[Dict[str, Any]] = []
    last_balance_hint: Optional[Dict[str, Any]] = None

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or "|" not in line:
            continue
        if not re.match(r"^\d+\s*[_-]?\|", line):
            continue

        date_match = re.search(r"(20\d{2}[年./-]\d{1,2}[月./-]\d{1,2}日?)", line)
        if not date_match:
            continue

        date_text = date_match.group(1)
        normalized_date = normalize_date_string(date_text)
        if not normalized_date:
            continue

        segments = [segment.strip() for segment in line.split("|")]
        description = segments[2] if len(segments) > 2 else ""
        numeric_segment = "|".join(segments[3:]) if len(segments) > 3 else ""
        numeric_values = _extract_amount_candidates(numeric_segment)
        if not numeric_values:
            continue

        total_amount = numeric_values[0] if len(numeric_values) > 0 else None
        principal_amount = numeric_values[1] if len(numeric_values) > 1 else 0.0
        interest_amount = numeric_values[2] if len(numeric_values) > 2 else 0.0
        penalty_amount = numeric_values[3] if len(numeric_values) > 3 else 0.0
        balance_amount = numeric_values[4] if len(numeric_values) > 4 else None

        positive_components = [
            amount for amount in [principal_amount, interest_amount, penalty_amount]
            if amount and amount > 0.005
        ]
        if total_amount and len(positive_components) == 1 and positive_components[0] < total_amount:
            if principal_amount > 0.005:
                principal_amount = total_amount
            elif interest_amount > 0.005:
                interest_amount = total_amount
            elif penalty_amount > 0.005:
                penalty_amount = total_amount

        if balance_amount is not None:
            last_balance_hint = {
                "amount": round(balance_amount, 2),
                "source": line
            }

        if any(keyword in description for keyword in ["开户", "提款", "发放", "授信"]):
            continue

        components = [
            ("principal", principal_amount),
            ("interest", interest_amount),
            ("penalty", penalty_amount),
        ]

        added = False
        for repayment_type, amount in components:
            if amount and amount > 0.005:
                records.append({
                    "date": normalized_date,
                    "type": repayment_type,
                    "amount": round(amount, 2),
                    "description": description,
                    "source": line,
                })
                added = True

        if not added and total_amount and total_amount > 0.005:
            pending_records.append({
                "date": normalized_date,
                "amount": round(total_amount, 2),
                "description": description,
                "source": line,
                "suggested_type": guess_repayment_type(description),
            })

    return _deduplicate_records(records), pending_records, last_balance_hint


def guess_repayment_type(description: str) -> str:
    desc = (description or "").strip()
    if "罚息" in desc:
        return "penalty"
    if "复利" in desc:
        return "compound"
    if "本金" in desc:
        return "principal"
    if "利息" in desc or "扣款" in desc:
        return "interest"
    return "interest"


def parse_amount(value: Any) -> Optional[float]:
    if value is None:
        return None

    if isinstance(value, (int, float)):
        return float(value)

    text = str(value).strip()
    text = (
        text.replace(",", "")
        .replace(" ", "")
        .replace("O", "0")
        .replace("o", "0")
        .replace("L", "1")
        .replace("I", "1")
        .replace("l", "1")
        .replace("。", ".")
    )
    text = re.sub(r"[^0-9.\-]", "", text)

    while text.endswith("."):
        text = text[:-1]

    if text.count(".") > 1:
        last_dot_index = text.rfind(".")
        text = text[:last_dot_index].replace(".", "") + text[last_dot_index:]

    if not text or text in {"-", ".", "-."}:
        return None

    try:
        return float(text)
    except ValueError:
        return None


def parse_rate(text: str) -> Optional[float]:
    match = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*%", text or "")
    if not match:
        return None

    rate = parse_amount(match.group(1))
    if rate is None:
        return None

    if "月利率" in text:
        rate *= 12

    return round(rate, 4)


def normalize_date_string(value: Any) -> Optional[str]:
    if value is None:
        return None

    text = str(value).strip()
    text = text.replace("年", "-").replace("月", "-").replace("日", "")
    text = text.replace(".", "-").replace("/", "-").replace(",", "-")
    text = re.sub(r"[^0-9-]", "", text)
    text = re.sub(r"-{2,}", "-", text).strip("-")

    parts = text.split("-")
    if len(parts) < 3:
        return None

    try:
        year, month, day = int(parts[0]), int(parts[1]), int(parts[2])
    except ValueError:
        return None

    return f"{year:04d}-{month:02d}-{day:02d}"


def _find_first_match(text: str, patterns: List[str]) -> Optional[str]:
    for pattern in patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            return match.group(1)
    return None


def _extract_amount_candidates(line: str) -> List[float]:
    amounts: List[float] = []

    for token in re.findall(r"[0-9OLIl][0-9OLIl,\.\s]{0,18}", line):
        parsed = parse_amount(token)
        if parsed is None:
            continue
        if parsed >= 10_000_000:
            continue
        amounts.append(parsed)

    return amounts[:5]


def _deduplicate_records(records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    seen = set()
    deduplicated: List[Dict[str, Any]] = []
    for record in records:
        key = (record.get("date"), record.get("type"), record.get("amount"))
        if key in seen:
            continue
        seen.add(key)
        deduplicated.append(record)
    return deduplicated
