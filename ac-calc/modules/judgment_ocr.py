"""
判决文书OCR模块
支持上传PDF判决书，自动提取关键要素填充表单。
优先提取"判决如下："后面的核心判项内容，大幅提升速度和准确率。
"""
import re
from typing import Any, Dict, Optional, List
from datetime import date

import fitz  # PyMuPDF


# ============================================
# 判决书结构切分标记
# ============================================
CORE_MARKERS = [
    "判决如下",
    "判令",
]

# ============================================
# 判项金额提取正则
# ============================================

# 核心判项中的金额提取：模式是 "本金98975.32 元、利息3730.32 元、罚息1160.16 元、复利138.28 元"
# 各种写法：借款本金/贷款本金/尚欠本金

# 本金：匹配"借款本金/贷款本金×××元"
PATTERN_PRINCIPAL = [
    r'借款本金\s*([\d,]+\.?\d*)',
    r'贷款本金\s*([\d,]+\.?\d*)',
    r'偿还\s*本金\s*([\d,]+\.?\d*)',
]

# 利息：匹配"利息×××元"（但后面不跟"为基数"）
PATTERN_INTEREST = [
    r'利息\s*([\d,]+\.?\d*)\s*元(?!.*为基数)',
]

# 罚息：匹配"罚息×××元"
PATTERN_PENALTY = [
    r'罚息\s*([\d,]+\.?\d*)\s*元',
]

# 复利：匹配"复利×××元"
PATTERN_COMPOUND = [
    r'复利\s*([\d,]+\.?\d*)\s*元',
]

# 判决后利率：匹配"罚息年利率××%"或"年利率××%"
PATTERN_RATE = [
    r'罚息年利率\s*([\d.]+)\s*%',
    r'年利率\s*([\d.]+)\s*%',
]

# 日期
# 中文数字映射
CN_DIGITS = {"零": 0, "〇": 0, "一": 1, "二": 2, "三": 3, "四": 4,
             "五": 5, "六": 6, "七": 7, "八": 8, "九": 9}

def _cn_number_to_int(s: str) -> int:
    """将中文数字串转为整数，如 '十五'→15, '二十三'→23, '一'→1"""
    # 纯个位数
    if s in CN_DIGITS:
        return CN_DIGITS[s]
    # 含 '十' 的组合数
    if "十" in s:
        parts = s.split("十")
        tens = CN_DIGITS.get(parts[0], 1) if parts[0] else 1  # 十 → 1*10, 二十 → 2*10
        ones = CN_DIGITS.get(parts[1], 0) if len(parts) > 1 and parts[1] else 0
        return tens * 10 + ones
    return 0

# 全中文数字日期，如 "二〇二四年一月十五日"
CN_YEAR_PATTERN = r'([一二三四五六七八九〇零]+)年'
CN_MONTH_PATTERN = r'([一二三四五六七八九十]+)月'
CN_DAY_PATTERN = r'([一二三四五六七八九十]+)日'

def _cn_date_to_iso(text: str) -> Optional[str]:
    """从文本中提取中文数字日期，返回 ISO 格式 'YYYY-MM-DD' 或 None"""
    m = re.search(CN_YEAR_PATTERN + r'\s*' + CN_MONTH_PATTERN + r'\s*' + CN_DAY_PATTERN, text)
    if not m:
        return None
    year_str = m.group(1)
    month_str = m.group(2)
    day_str = m.group(3)
    # 年份：逐个字符转换
    year = int("".join(str(CN_DIGITS.get(c, 0)) for c in year_str if c in CN_DIGITS))
    if year < 100:
        year += 2000  # 二四年 → 2024
    month = _cn_number_to_int(month_str)
    day = _cn_number_to_int(day_str)
    if not (2000 <= year <= 2030 and 1 <= month <= 12 and 1 <= day <= 31):
        return None
    return f"{year:04d}-{month:02d}-{day:02d}"

PATTERN_DATE = [
    r'二〇(\d{2})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日',  # 混合：二〇xx年x月x日
    r'(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日',
    r'(\d{4})-(\d{2})-(\d{2})',
]

# 判决生效相关
PATTERN_PERFORMANCE = [
    r'本判决发生法律效力之日起(\d+)日内',
    r'判决生效之日起(\d+)日内',
]

# 律师费、诉讼费
PATTERN_LAWYER_FEE = [
    r'律师费[\s]*([\d,]+\.?\d*)\s*元',
    r'律师费[\s]*([\d,]+)',
]
PATTERN_COURT_FEE = [
    r'案件受理费[^\d]*([\d,]+\.?\d*)',
    r'受理费[^\d]*([\d,]+\.?\d*)',
]


# ============================================
# PDF文本提取
# ============================================
def extract_text_from_pdf(file_bytes: bytes) -> str:
    """从PDF提取文本。仅使用PyMuPDF。"""
    try:
        doc = fitz.open(stream=file_bytes, filetype="pdf")
        text = "\n".join(page.get_text().strip() for page in doc)
        doc.close()
        return text
    except Exception:
        return ""


def extract_core_section(text: str) -> str:
    """
    提取"判决如下"之后的核心判项部分。
    这里的金额是法院最终确认的数据，才是我们要的。
    """
    if not text:
        return ""

    # 找"判决如下"
    pos = text.find("判决如下")
    if pos >= 0:
        start = max(0, pos - 50)
        return text[start:start + 5000]

    # 找不到"判决如下"，退而求其次找"判令"
    pos = text.find("判令")
    if pos >= 0:
        start = max(0, pos - 50)
        return text[start:start + 5000]

    return text[-3000:] if len(text) > 3000 else text


def _match_first(pattern_list: List[str], text: str) -> Optional[float]:
    """从文本中用正则列表匹配第一个金额。"""
    for pattern in pattern_list:
        m = re.search(pattern, text, re.IGNORECASE)
        if m:
            try:
                val_str = m.group(1).replace(",", "").strip()
                return float(val_str)
            except (ValueError, IndexError):
                continue
    return None


def extract_judgment_data(full_text: str) -> Dict[str, Any]:
    """
    从判决书全文提取关键要素。
    只分析"判决如下"后面的判项内容。
    """
    result: Dict[str, Any] = {
        "principal": 0.0,
        "interest": 0.0,
        "penalty": 0.0,
        "compound": 0.0,
        "rate": 0.0,
        "lawyer_fee": 0.0,
        "court_fee": 0.0,
        "performance_days": 10,
        "judgment_date": None,
        "has_principal": False,
        "has_interest": False,
        "has_penalty": False,
        "has_compound": False,
        "has_rate": False,
    }

    if not full_text:
        return result

    # 只提取"判决如下"之后的判项
    core = extract_core_section(full_text)
    if len(core) < 50:
        return result

    # 在判项中提取金额
    principal = _match_first(PATTERN_PRINCIPAL, core)
    if principal:
        result["principal"] = principal
        result["has_principal"] = True

    interest = _match_first(PATTERN_INTEREST, core)
    if interest:
        result["interest"] = interest
        result["has_interest"] = True

    penalty = _match_first(PATTERN_PENALTY, core)
    if penalty:
        result["penalty"] = penalty
        result["has_penalty"] = True

    compound = _match_first(PATTERN_COMPOUND, core)
    if compound:
        result["compound"] = compound
        result["has_compound"] = True

    rate = _match_first(PATTERN_RATE, core)
    if rate:
        result["rate"] = rate
        result["has_rate"] = True

    # 律师费
    lawyer_fee = _match_first(PATTERN_LAWYER_FEE, core)
    if lawyer_fee:
        result["lawyer_fee"] = lawyer_fee

    # 诉讼费（案件受理费）
    court_fee = _match_first(PATTERN_COURT_FEE, core)
    if court_fee:
        result["court_fee"] = court_fee

    # 履行期间
    perf = _match_first(PATTERN_PERFORMANCE, core)
    if perf:
        result["performance_days"] = int(perf)

    # 日期：从全文提取，取按位置最后出现的有效日期（通常是判决书制作日期）
    last_valid_date = None
    # 中文数字日期（二〇二四年一月十五日）— 优先级最高
    cn_iso = _cn_date_to_iso(full_text)
    if cn_iso:
        parts = cn_iso.split("-")
        last_valid_date = date(int(parts[0]), int(parts[1]), int(parts[2]))
    # 混合中文+阿拉伯数字日期（二〇24年1月15日）
    for m in re.finditer(PATTERN_DATE[0], full_text):
        try:
            y, mo, d = 2000 + int(m.group(1)), int(m.group(2)), int(m.group(3))
            if 2000 <= y <= 2030 and 1 <= mo <= 12 and 1 <= d <= 31:
                last_valid_date = date(y, mo, d)
        except:
            pass
    # 阿拉伯数字日期（2024年1月15日）
    for m in re.finditer(PATTERN_DATE[1], full_text):
        try:
            y, mo, d = int(m.group(1)), int(m.group(2)), int(m.group(3))
            if 2000 <= y <= 2030 and 1 <= mo <= 12 and 1 <= d <= 31:
                last_valid_date = date(y, mo, d)
        except:
            pass

    if last_valid_date:
        result["judgment_date"] = str(last_valid_date)

    return result


def try_ai_extraction(full_text: str) -> Optional[Dict[str, Any]]:
    """AI增强提取。先快速检测Ollama，不可用直接跳过。"""
    import json as _json
    import urllib.request
    import socket

    try:
        sock = socket.create_connection(("localhost", 11434), timeout=0.5)
        sock.close()
    except Exception:
        return None

    # 只取"判决如下"后面给AI
    core = extract_core_section(full_text)[:1500]
    if len(core) < 50:
        return None

    prompt = f"""从判决书判项中提取数字（只输出JSON）：

{core}

{{"principal": 借款本金, "interest": 利息, "penalty": 罚息, "compound": 复利, "annual_rate": 年利率}}
"""

    payload = _json.dumps({
        "model": "qwen3:8b", "prompt": prompt,
        "stream": False, "temperature": 0.1, "max_tokens": 256,
    }, ensure_ascii=False).encode('utf-8')

    req = urllib.request.Request(
        "http://localhost:11434/api/generate",
        data=payload, headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            out = _json.loads(resp.read().decode('utf-8')).get("response", "")
        m = re.search(r'\{[^{}]*\}', out, re.DOTALL)
        return _json.loads(m.group()) if m else None
    except Exception:
        return None
