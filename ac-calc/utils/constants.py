"""
常量定义模块
包含系统中使用的所有常量、枚举类型和默认值
"""

from enum import Enum
from typing import List

# ============================================
# 还款类型枚举
# ============================================
class RepaymentType(str, Enum):
    """还款类型"""
    PRINCIPAL = "principal"      # 本金
    INTEREST = "interest"        # 利息
    PENALTY = "penalty"          # 罚息
    COMPOUND = "compound"        # 复利
    FEE = "fee"                  # 费用

    @classmethod
    def get_display_name(cls, value: str) -> str:
        """获取中文显示名称"""
        mapping = {
            "principal": "归还本金",
            "interest": "归还利息",
            "penalty": "归还罚息",
            "compound": "归还复利",
            "fee": "支付费用"
        }
        return mapping.get(value, value)

    @classmethod
    def get_options(cls) -> List[tuple]:
        """获取选项列表（用于Streamlit Selectbox）"""
        return [
            ("归还本金", "principal"),
            ("归还利息", "interest"),
            ("归还罚息", "penalty"),
            ("归还复利", "compound"),
            ("支付费用", "fee")
        ]


# ============================================
# 抵扣顺序（优先级从高到低）
# ============================================
DEFAULT_REPAYMENT_PRIORITY = [
    RepaymentType.FEE,
    RepaymentType.PENALTY,
    RepaymentType.COMPOUND,
    RepaymentType.INTEREST,
    RepaymentType.PRINCIPAL
]

# ============================================
# 日期相关常量
# ============================================
class DayCountConvention(str, Enum):
    """天数计算规则"""
    ACTUAL_360 = "actual/360"      # 实际天数/360
    ACTUAL_365 = "actual/365"      # 实际天数/365
    ACTUAL_ACTUAL = "actual/actual"  # 实际天数/实际天数

class DateCalculationMethod(str, Enum):
    """日期计算方式"""
    BILATERAL = "bilateral"              # 双边计算（算头又算尾）
    HEAD_EXCLUSIVE = "head_exclusive"    # 算头不算尾（推荐）
    TAIL_EXCLUSIVE = "tail_exclusive"    # 算尾不算头


# ============================================
# 默认数值常量
# ============================================
DEFAULT_DECIMAL_PRECISION = 2  # 金额小数位数
DEFAULT_PENALTY_RATE_MULTIPLIER = 1.5  # 默认罚息利率上浮倍数
DEFAULT_ANNUAL_INTEREST_RATE = 4.35  # 默认年利率（参考央行基准利率）

# ============================================
# Excel列名常量
# ============================================
class SummaryColumns:
    """汇总表列名"""
    LOAN_AMOUNT = "贷款金额"
    REMAINING_PRINCIPAL = "剩余本金"
    ACCRUED_INTEREST = "已计利息"
    PENALTY_INTEREST = "罚息"
    COMPOUND_INTEREST = "复利"
    TOTAL_AMOUNT = "债权总额"
    CALCULATION_DATE = "计算日期"

class DetailColumns:
    """明细表列名"""
    SEQ_NO = "序号"
    START_DATE = "起始日期"
    END_DATE = "截止日期"
    DAYS = "天数"
    PRINCIPAL_BALANCE = "本金余额"
    ANNUAL_RATE = "年利率(%)"
    INTEREST_AMOUNT = "利息金额"
    COMPOUND_AMOUNT = "复利金额"
    PENALTY_AMOUNT = "罚息金额"
    REMARK = "备注"

class RecordColumns:
    """还款记录列名"""
    DATE = "还款日期"
    TYPE = "还款类型"
    AMOUNT = "还款金额"
    PRINCIPAL_PORTION = "本金部分"
    INTEREST_PORTION = "利息部分"
    PENALTY_PORTION = "罚息部分"
    COMPOUND_PORTION = "复利部分"
    FEE_PORTION = "费用部分"
    REMARK = "备注"

# ============================================
# LLM相关常量
# ============================================
class ExtractionFields:
    """信息提取字段"""
    LOAN_AMOUNT = "loan_amount"
    ANNUAL_INTEREST_RATE = "annual_interest_rate"
    START_DATE = "start_date"
    END_DATE = "end_date"
    REMAINING_PRINCIPAL = "remaining_principal"
    ACCRUED_INTEREST = "accrued_interest"
    PENALTY_INTEREST = "penalty_interest"
    COMPOUND_INTEREST = "compound_interest"
    REPAYMENT_RECORDS = "repayment_records"
    RATE_ADJUSTMENTS = "rate_adjustments"  # 利率调整记录

# ============================================
# UI显示常量
# ============================================
APP_TITLE = "📊 债权计算表自动生成系统"
APP_ICON = "📊"

# 侧边栏页面
PAGE_UPLOAD = "📤 文档上传与提取"
PAGE_VERIFY = "✏️ 数据核对与编辑"
PAGE_CALCULATE = "🧮 债权计算"
PAGE_EXPORT = "📊 结果导出"

# 状态消息
MSG_SUCCESS = "✅ 操作成功"
MSG_ERROR = "❌ 操作失败"
MSG_WARNING = "⚠️ 警告"
MSG_INFO = "ℹ️ 提示"
MSG_PROCESSING = "⏳ 正在处理..."

# ============================================
# 文件路径常量
# ============================================
import os
from pathlib import Path

# 项目根目录
PROJECT_ROOT = Path(__file__).parent.parent

# 数据目录
DATA_DIR = PROJECT_ROOT / "data"
TEMP_DIR = DATA_DIR / "temp"
OUTPUT_DIR = DATA_DIR / "output"
TEMPLATE_DIR = DATA_DIR / "templates"
LOGS_DIR = PROJECT_ROOT / "logs"

# 确保目录存在
for dir_path in [TEMP_DIR, OUTPUT_DIR, TEMPLATE_DIR, LOGS_DIR]:
    dir_path.mkdir(parents=True, exist_ok=True)

# ============================================
# 系统提示信息
# ============================================
HELP_TEXTS = {
    "loan_amount": "贷款合同总金额，单位：元",
    "annual_interest_rate": "合同约定的年利率，如4.35表示4.35%",
    "start_date": "贷款起算日，即合同生效日期",
    "end_date": "合同到期日或宣布提前到期日",
    "remaining_principal": "截至计算日尚未归还的本金余额",
    "accrued_interest": "截至计算日已产生但未支付的利息",
    "penalty_interest": "截至计算日已产生的罚息总额",
    "compound_interest": "截至计算日已产生的复利总额",
    "rate_adjustments": "利率调整记录，包含调整日期和新利率",
    "repayment_records": "借款人的历史还款记录"
}

# ============================================
# 错误信息模板
# ============================================
class ErrorMessages:
    """错误信息模板"""
    OLLAMA_NOT_CONNECTED = "无法连接到Ollama服务，请确认服务已启动（{url}）"
    OLLAMA_TIMEOUT = "Ollama请求超时，请稍后重试或检查模型是否正常加载"
    PDF_PARSE_FAILED = "PDF解析失败，请确认文件格式正确"
    OCR_FAILED = "OCR识别失败，请确认Tesseract已正确安装"
    INVALID_DATE_FORMAT = "日期格式错误：{field}，应为 YYYY-MM-DD 格式"
    INVALID_AMOUNT = "金额格式错误：{field}，应为正数"
    INVALID_RATE = "利率格式错误：{field}，应为正数（如4.35表示4.35%）"
    DATE_LOGIC_ERROR = "日期逻辑错误：{start} 不能晚于 {end}"
    NO_DATA = "未找到有效数据，请上传文件或手动录入"
    CALCULATION_ERROR = "计算过程出现错误：{error}"
    EXPORT_FAILED = "Excel导出失败：{error}"
