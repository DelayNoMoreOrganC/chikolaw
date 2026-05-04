"""
银行对账单模板核心数据模型
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Callable
from pathlib import Path
from datetime import datetime
import yaml


@dataclass
class ExtractionMethod:
    """数据提取方法配置"""
    method_type: str                    # "regex", "position", "table", "llm"
    priority: int = 0                   # 优先级（数字越小优先级越高）

    # 正则提取
    regex_pattern: Optional[str] = None
    regex_group: int = 1
    case_sensitive: bool = False

    # 位置提取（用于固定格式）
    sheet_name: Optional[str] = None
    cell_range: Optional[str] = None
    row_index: Optional[int] = None
    column_index: Optional[int] = None

    # 表格提取
    table_header: Optional[str] = None
    table_column: Optional[int] = None
    lookup_key: Optional[str] = None
    column_mappings: Optional[Dict[str, int]] = None
    row_filters: Optional[List[Dict[str, Any]]] = None
    type_mapping: Optional[Dict[str, str]] = None

    # LLM提取
    llm_prompt: Optional[str] = None

    # 后处理
    post_process: Optional[List[str]] = None


@dataclass
class Validator:
    """验证器配置"""
    validator_type: str                 # "range", "required", "custom"
    min_value: Optional[float] = None
    max_value: Optional[float] = None
    custom_function: Optional[str] = None


@dataclass
class FieldMapping:
    """字段映射配置"""
    target_field: str                   # 目标字段（标准字段名）
    extraction_methods: List[ExtractionMethod]
    data_type: str = "string"           # "decimal", "date", "string", "int", "array"
    required: bool = True
    default_value: Any = None
    validators: List[Validator] = field(default_factory=list)
    example_values: List[str] = field(default_factory=list)
    description: str = ""


@dataclass
class SpecialRule:
    """特殊计算规则"""
    name: str
    description: str = ""
    enabled: bool = True
    trigger: Optional[Dict[str, Any]] = None
    action: Optional[Dict[str, Any]] = None


@dataclass
class InterestCalculation:
    """利息计算规则"""
    day_count_convention: str = "actual/360"  # "actual/360", "actual/365", "actual/actual"
    rounding_mode: str = "ROUND_HALF_UP"
    decimal_places: int = 2


@dataclass
class CompoundInterest:
    """复利计算规则"""
    enabled: bool = True
    base: str = "accrued_interest"          # 基于累计未还利息
    calculation_method: str = "simple"      # "simple", "monthly_compound"
    compound_rate_same_as_penalty: bool = False


@dataclass
class PenaltyInterest:
    """罚息计算规则"""
    enabled: bool = True
    rate_type: str = "percentage"           # "percentage" 或 "fixed"
    rate_value: float = 1.5                 # 上浮50%
    start_date: str = "due_date"            # 从到期日开始
    penalty_on_penalty: bool = False        # 罚息是否也要计算复利


@dataclass
class RateAdjustments:
    """利率调整配置"""
    enabled: bool = True
    auto_detect: bool = True
    detection_sources: List[Dict[str, Any]] = field(default_factory=list)


@dataclass
class CalculationRules:
    """计算规则配置"""
    interest_calculation: InterestCalculation = field(default_factory=InterestCalculation)
    compound_interest: CompoundInterest = field(default_factory=CompoundInterest)
    penalty_interest: PenaltyInterest = field(default_factory=PenaltyInterest)
    repayment_priority: List[str] = field(default_factory=lambda: ["费用", "罚息", "复利", "利息", "本金"])
    rate_adjustments: RateAdjustments = field(default_factory=RateAdjustments)
    special_rules: List[SpecialRule] = field(default_factory=list)


@dataclass
class FontConfig:
    """字体配置"""
    name: str = "宋体"
    size: int = 10
    bold: bool = False
    color: str = "000000"


@dataclass
class ColumnConfig:
    """列配置"""
    field: str
    header: str
    width: float = 12.0
    data_type: str = "string"
    number_format: Optional[str] = None
    date_format: str = "yyyy-mm-dd"
    alignment: str = "center"
    font: Optional[FontConfig] = None
    background_color: Optional[str] = None
    value_mapping: Optional[Dict[str, str]] = None


@dataclass
class SheetConfig:
    """工作表配置"""
    name: str
    type: str                            # "summary", "detail", "payment", "adjustment"
    data_source: str
    columns: List[ColumnConfig] = field(default_factory=list)
    filters: List[Dict[str, Any]] = field(default_factory=list)
    sorting: List[Dict[str, Any]] = field(default_factory=list)


@dataclass
class StyleDefinition:
    """样式定义"""
    default_font: FontConfig = field(default_factory=FontConfig)
    header_font: FontConfig = field(default_factory=lambda: FontConfig(name="宋体", size=11, bold=True))
    header_background: Dict[str, str] = field(default_factory=lambda: {"pattern_type": "solid", "fg_color": "D3D3D3"})
    border_style: Dict[str, str] = field(default_factory=lambda: {"style": "thin", "color": "000000"})


@dataclass
class HeaderFooterConfig:
    """页眉页脚配置"""
    header: Optional[Dict[str, str]] = None
    footer: Optional[Dict[str, str]] = None


@dataclass
class OutputFormat:
    """输出格式配置"""
    workbook_structure: Dict[str, Any] = field(default_factory=dict)
    styles: StyleDefinition = field(default_factory=StyleDefinition)
    header_footer: HeaderFooterConfig = field(default_factory=HeaderFooterConfig)


@dataclass
class FileRecognition:
    """文件识别配置"""
    supported_formats: List[str] = field(default_factory=lambda: ["xlsx", "xls", "pdf"])
    filename_patterns: List[str] = field(default_factory=list)
    ocr_keywords: List[str] = field(default_factory=list)


@dataclass
class TemplateMeta:
    """模板元数据"""
    bank_code: str
    bank_name: str
    version: str
    version_name: str
    author: str = "系统"
    created_date: str = ""
    updated_date: str = ""
    description: str = ""

    def __post_init__(self):
        if not self.created_date:
            self.created_date = datetime.now().strftime("%Y-%m-%d")
        if not self.updated_date:
            self.updated_date = datetime.now().strftime("%Y-%m-%d")


@dataclass
class BankTemplate:
    """银行模板类 - 核心配置对象"""

    # 基本信息
    meta: TemplateMeta

    # 文件识别
    file_recognition: FileRecognition = field(default_factory=FileRecognition)

    # 字段映射 (核心配置)
    field_mappings: Dict[str, FieldMapping] = field(default_factory=dict)

    # 计算规则
    calculation_rules: CalculationRules = field(default_factory=CalculationRules)

    # 输出格式
    output_format: OutputFormat = field(default_factory=OutputFormat)

    @classmethod
    def from_yaml(cls, yaml_path: Path) -> "BankTemplate":
        """从YAML文件加载模板"""
        with open(yaml_path, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f)

        return cls.from_dict(data)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "BankTemplate":
        """从字典创建模板"""
        # 解析元数据
        meta_data = data.get("meta", {})
        meta = TemplateMeta(**meta_data)

        # 解析文件识别
        file_recognition = FileRecognition(**data.get("file_recognition", {}))

        # 解析字段映射
        field_mappings = {}
        for field_name, field_data in data.get("field_mappings", {}).items():
            extraction_methods = []
            for method_data in field_data.get("extraction_methods", []):
                extraction_methods.append(ExtractionMethod(**method_data))

            validators = []
            for validator_data in field_data.get("validators", []):
                validators.append(Validator(**validator_data))

            field_mappings[field_name] = FieldMapping(
                target_field=field_data.get("target_field", field_name),
                extraction_methods=extraction_methods,
                data_type=field_data.get("data_type", "string"),
                required=field_data.get("required", True),
                default_value=field_data.get("default_value"),
                validators=validators,
                example_values=field_data.get("example_values", []),
                description=field_data.get("description", "")
            )

        # 解析计算规则
        calc_data = data.get("calculation_rules", {})
        interest_calc = InterestCalculation(**calc_data.get("interest_calculation", {}))
        compound_int = CompoundInterest(**calc_data.get("compound_interest", {}))
        penalty_int = PenaltyInterest(**calc_data.get("penalty_interest", {}))
        rate_adj = RateAdjustments(**calc_data.get("rate_adjustments", {}))

        special_rules = []
        for rule_data in calc_data.get("special_rules", []):
            special_rules.append(SpecialRule(**rule_data))

        calculation_rules = CalculationRules(
            interest_calculation=interest_calc,
            compound_interest=compound_int,
            penalty_interest=penalty_int,
            repayment_priority=calc_data.get("repayment_priority", ["费用", "罚息", "复利", "利息", "本金"]),
            rate_adjustments=rate_adj,
            special_rules=special_rules
        )

        # 解析输出格式
        output_data = data.get("output_format", {})
        styles_data = output_data.get("styles", {})

        styles = StyleDefinition(
            default_font=FontConfig(**styles_data.get("default_font", {})),
            header_font=FontConfig(**styles_data.get("header_font", {})),
            header_background=styles_data.get("header_background", {}),
            border_style=styles_data.get("border_style", {})
        )

        header_footer = HeaderFooterConfig(
            header=output_data.get("header_footer", {}).get("header"),
            footer=output_data.get("header_footer", {}).get("footer")
        )

        output_format = OutputFormat(
            workbook_structure=output_data.get("workbook_structure", {}),
            styles=styles,
            header_footer=header_footer
        )

        return cls(
            meta=meta,
            file_recognition=file_recognition,
            field_mappings=field_mappings,
            calculation_rules=calculation_rules,
            output_format=output_format
        )

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（用于保存）"""
        return {
            "meta": {
                "bank_code": self.meta.bank_code,
                "bank_name": self.meta.bank_name,
                "version": self.meta.version,
                "version_name": self.meta.version_name,
                "author": self.meta.author,
                "created_date": self.meta.created_date,
                "updated_date": self.meta.updated_date,
                "description": self.meta.description
            },
            "file_recognition": {
                "supported_formats": self.file_recognition.supported_formats,
                "filename_patterns": self.file_recognition.filename_patterns,
                "ocr_keywords": self.file_recognition.ocr_keywords
            },
            "field_mappings": {
                field_name: {
                    "target_field": mapping.target_field,
                    "data_type": mapping.data_type,
                    "required": mapping.required,
                    "default_value": mapping.default_value,
                    "extraction_methods": [
                        {
                            "method_type": m.method_type,
                            "priority": m.priority,
                            "regex_pattern": m.regex_pattern,
                            "sheet_name": m.sheet_name,
                            "cell_range": m.cell_range,
                            # ... 其他字段
                        } for m in mapping.extraction_methods
                    ]
                } for field_name, mapping in self.field_mappings.items()
            },
            "calculation_rules": {
                # ... 计算规则
            },
            "output_format": {
                # ... 输出格式
            }
        }

    def validate(self) -> tuple[bool, List[str]]:
        """验证模板完整性"""
        errors = []

        # 检查必需字段
        required_fields = ["loan_amount", "start_date", "end_date"]
        for field_name in required_fields:
            if field_name not in self.field_mappings:
                errors.append(f"缺少必需字段: {field_name}")

        # 检查计算规则
        if not self.calculation_rules.repayment_priority:
            errors.append("缺少还款优先级配置")

        return len(errors) == 0, errors


# 预定义的标准字段名
class StandardFields:
    """标准字段名常量"""
    LOAN_AMOUNT = "loan_amount"
    ANNUAL_INTEREST_RATE = "annual_interest_rate"
    START_DATE = "start_date"
    END_DATE = "end_date"
    CALCULATION_DATE = "calculation_date"
    REMAINING_PRINCIPAL = "remaining_principal"
    ACCRUED_INTEREST = "accrued_interest"
    PENALTY_INTEREST = "penalty_interest"
    COMPOUND_INTEREST = "compound_interest"
    REPAYMENT_RECORDS = "repayment_records"
    RATE_ADJUSTMENTS = "rate_adjustments"


# 工厂函数
def create_template(
    bank_code: str,
    bank_name: str,
    version: str = "1.0"
) -> BankTemplate:
    """创建一个基础模板"""
    return BankTemplate(
        meta=TemplateMeta(
            bank_code=bank_code,
            bank_name=bank_name,
            version=version,
            version_name=f"{bank_name}模板 v{version}"
        )
    )
