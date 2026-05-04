"""
核心模块包初始化
"""

from .calculator import DebtCalculator, create_calculator
from .excel_exporter import ExcelExporter
from .history_store import HistoryStore, create_history_store

__all__ = [
    "DebtCalculator",
    "create_calculator",
    "ExcelExporter",
    "create_excel_exporter",
    "HistoryStore",
    "create_history_store"
]

try:
    from .ocr_processor import OCRProcessor, create_ocr_processor

    __all__.extend([
        "OCRProcessor",
        "create_ocr_processor"
    ])
except Exception:
    pass

try:
    from .llm_extractor import LLMExtractor, create_llm_extractor

    __all__.extend([
        "LLMExtractor",
        "create_llm_extractor"
    ])
except Exception:
    pass

# 分段还本计划模块（v0.3新增）
from .repayment_plan import (
    build_segment,
    default_original_segment,
    segment_from_supplementary,
    generate_expected_schedule,
    generate_payment_dates,
    calc_overdue_penalty_by_plan,
    FREQ_LABELS,
    REPAYMENT_METHODS,
)

__all__.extend([
    "build_segment",
    "default_original_segment",
    "segment_from_supplementary",
    "generate_expected_schedule",
    "generate_payment_dates",
    "calc_overdue_penalty_by_plan",
    "FREQ_LABELS",
    "REPAYMENT_METHODS",
])
