# 银行对账单智能识别与债权计算系统 - 架构设计方案

## 📋 项目概述

### 核心目标
建立一个**可扩展、可配置**的银行对账单处理系统，能够：
1. **自动识别**不同银行的对账单格式
2. **智能解析**对账单中的关键信息
3. **灵活配置**各银行的计算规则
4. **按需输出**符合各银行要求的债权计算表格式

### 业务场景
```
用户上传对账单（PDF/Excel/图片）
    ↓
系统识别银行和版本
    ↓
按模板提取数据
    ↓
应用对应银行的计算规则
    ↓
生成对应格式的债权计算表
```

---

## 🏗️ 系统架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    用户界面层                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ 文件上传 │  │ 数据预览 │  │ 结果导出 │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   业务逻辑层                              │
│  ┌──────────────────┐  ┌──────────────────┐           │
│  │  文档处理引擎     │  │  计算引擎        │           │
│  │  - OCR识别       │  │  - 利息计算      │           │
│  │  - Excel解析     │  │  - 罚息计算      │           │
│  │  - 格式转换      │  │  - 还款冲销      │           │
│  └──────────────────┘  └──────────────────┘           │
│  ┌──────────────────┐  ┌──────────────────┐           │
│  │  银行识别器       │  │  模板管理器      │           │
│  │  - 银行检测      │  │  - 模板加载      │           │
│  │  - 版本识别      │  │  - 版本管理      │           │
│  └──────────────────┘  └──────────────────┘           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   模板配置层                              │
│  ┌──────────────────────────────────────────────┐     │
│  │           银行模板仓库                        │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐ │     │
│  │  │工商银行  │  │建设银行  │  │招商银行  │ │     │
│  │  │ v1.0     │  │ v2.1     │  │ v1.3     │ │     │
│  │  │ v2.0     │  │          │  │          │ │     │
│  │  └──────────┘  └──────────┘  └──────────┘ │     │
│  └──────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   数据存储层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │模板数据库│  │历史记录  │  │配置文件  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 核心模块设计

### 模块1: 银行识别器 (BankDetector)

**职责**: 自动识别对账单所属银行和版本

```python
class BankDetector:
    """银行识别器"""

    def detect(self, file_bytes: bytes, filename: str, ocr_text: str) -> BankInfo:
        """
        识别银行和版本

        识别策略（优先级从高到低）:
        1. 文件名模式匹配 (e.g., "工商银行_20240101.xlsx")
        2. OCR文本中的银行特征词 (e.g., "中国工商银行", "ICBC")
        3. 文档结构特征 (e.g., 特定的表格布局)
        4. 用户手动选择

        Returns:
            BankInfo: {
                "bank_code": "ICBC",           # 银行代码
                "bank_name": "中国工商银行",    # 银行名称
                "template_version": "v2.0",     # 模板版本
                "confidence": 0.95              # 识别置信度
            }
        """
        pass
```

**识别特征库**:

```yaml
# data/bank_templates/registry.yaml
banks:
  ICBC:
    name: "中国工商银行"
    code: "ICBC"
    keywords:
      - "中国工商银行"
      - "工商银行"
      - "ICBC"
      - "工行"
    filename_patterns:
      - "工商银行_*"
      - "ICBC_*"
      - "工行_*"
    versions:
      - "v1.0"
      - "v2.0"
    default_version: "v2.0"

  CCB:
    name: "中国建设银行"
    code: "CCB"
    keywords:
      - "中国建设银行"
      - "建设银行"
      - "CCB"
      - "建行"
    filename_patterns:
      - "建设银行_*"
      - "CCB_*"
      - "建行_*"
    versions:
      - "v1.0"
      - "v2.1"
    default_version: "v2.1"

  BCM:
    name: "交通银行"
    code: "BCM"
    keywords: ["交通银行", "BoCom", "交行"]
    # ...
```

---

### 模块2: 模板管理器 (TemplateManager)

**职责**: 加载、管理、验证银行模板

```python
class TemplateManager:
    """模板管理器"""

    def __init__(self, template_dir: str = "data/bank_templates"):
        self.template_dir = Path(template_dir)
        self.templates = {}  # {bank_code: {version: template}}

    def load_template(self, bank_code: str, version: str) -> BankTemplate:
        """
        加载指定银行和版本的模板

        Args:
            bank_code: 银行代码 (e.g., "ICBC")
            version: 版本号 (e.g., "v2.0")

        Returns:
            BankTemplate: 银行模板对象
        """
        template_path = self.template_dir / bank_code / f"{version}.yaml"
        return BankTemplate.from_yaml(template_path)

    def list_templates(self) -> List[Dict]:
        """列出所有可用模板"""
        pass

    def validate_template(self, template: BankTemplate) -> bool:
        """验证模板完整性"""
        pass
```

---

### 模块3: 银行模板 (BankTemplate)

**职责**: 定义单个银行的数据提取和计算规则

```python
@dataclass
class BankTemplate:
    """银行模板类"""

    # 基本信息
    bank_code: str                      # 银行代码
    bank_name: str                      # 银行名称
    version: str                        # 版本号
    version_name: str                   # 版本名称

    # 文件识别
    supported_formats: List[str]        # 支持的文件格式
    file_encodings: List[str]           # 文件编码列表

    # 字段映射 (核心配置)
    field_mappings: Dict[str, FieldMapping]  # 字段映射配置

    # 布局识别
    layout_rules: LayoutRules           # 布局识别规则

    # 计算规则
    calculation_rules: CalculationRules # 计算规则

    # 输出格式
    output_format: OutputFormat         # 输出格式配置

    @classmethod
    def from_yaml(cls, yaml_path: Path) -> "BankTemplate":
        """从YAML文件加载模板"""
        pass

    def extract_data(self, file_bytes: bytes, ocr_text: str) -> ExtractedData:
        """根据模板提取数据"""
        pass

    def calculate(self, data: ExtractedData) -> CalculationResult:
        """根据计算规则执行计算"""
        pass

    def format_output(self, result: CalculationResult) -> ExcelWorkbook:
        """格式化输出结果"""
        pass
```

---

### 模块4: 字段映射 (FieldMapping)

**职责**: 定义如何从对账单中提取各个字段

```python
@dataclass
class FieldMapping:
    """字段映射配置"""

    # 目标字段（标准字段名）
    target_field: str                   # e.g., "loan_amount"

    # 提取策略
    extraction_methods: List[ExtractionMethod]

    # 数据转换
    data_type: str                      # "decimal", "date", "string", "int"
    required: bool = True               # 是否必填
    default_value: Any = None           # 默认值

    # 验证规则
    validators: List[Validator] = None

    # 示例值（用于调试）
    example_values: List[str] = None


@dataclass
class ExtractionMethod:
    """提取方法"""
    method_type: str                    # "regex", "position", "table", "llm"
    priority: int = 0                   # 优先级（数字越小优先级越高）

    # 正则提取
    regex_pattern: str = None
    regex_group: int = 1
    case_sensitive: bool = False

    # 位置提取（用于固定格式）
    sheet_name: str = None              # 工作表名称
    cell_range: str = None              # 单元格范围 (e.g., "A1:B10")
    row_index: int = None               # 行号
    column_index: int = None            # 列号

    # 表格提取
    table_header: str = None            # 表头关键词
    table_column: int = None            # 列索引
    lookup_key: str = None              # 查找键

    # LLM提取
    llm_prompt: str = None              # LLM提示词

    # 后处理
    post_process: List[str] = None      # 后处理操作 (e.g., ["remove_comma", "multiply_12"])
```

**字段映射配置示例**:

```yaml
# data/bank_templates/ICBC/v2.0.yaml

field_mappings:
  # 贷款金额
  loan_amount:
    target_field: "loan_amount"
    data_type: "decimal"
    required: true
    extraction_methods:
      # 方法1: 从特定单元格提取
      - method_type: "position"
        priority: 1
        sheet_name: "基本信息"
        cell_range: "B2"

      # 方法2: 从关键词后的值提取
      - method_type: "regex"
        priority: 2
        regex_pattern: "贷款金额[:：]\s*([0-9,]+\.?[0-9]*)"
        post_process: ["remove_comma"]

      # 方法3: LLM提取（兜底）
      - method_type: "llm"
        priority: 3
        llm_prompt: "请提取贷款金额"

  # 年利率
  annual_interest_rate:
    target_field: "annual_interest_rate"
    data_type: "decimal"
    required: true
    extraction_methods:
      # 从表格中提取
      - method_type: "table"
        priority: 1
        sheet_name: "利率信息"
        table_header: "执行利率"
        table_column: 2  # 第2列

      # 正则兜底
      - method_type: "regex"
        priority: 2
        regex_pattern: "年利率[:：]\s*([0-9]+\.?[0-9]*)\s*%"

  # 贷款起始日
  start_date:
    target_field: "start_date"
    data_type: "date"
    required: true
    extraction_methods:
      - method_type: "position"
        priority: 1
        sheet_name: "基本信息"
        cell_range: "B3"

      - method_type: "regex"
        priority: 2
        regex_pattern: "贷款起始日[：:]\s*([0-9]{4}[-年][0-9]{1,2}[-月][0-9]{1,2})"

  # 还款记录（特殊字段）
  repayment_records:
    target_field: "repayment_records"
    data_type: "array"
    required: false
    extraction_methods:
      - method_type: "table"
        priority: 1
        sheet_name: "还款明细"
        table_header: "交易日期"
        # 表格列映射
        column_mappings:
          date: 0           # 第0列：日期
          type: 1           # 第1列：交易类型
          amount: 2         # 第2列：金额
          balance: 3        # 第3列：余额
        # 行过滤
        row_filters:
          - column: 1       # 交易类型列
            exclude_values: ["开户", "放款", "利息结转"]
```

---

### 模块5: 计算规则 (CalculationRules)

**职责**: 定义不同银行的债权计算规则

```python
@dataclass
class CalculationRules:
    """计算规则配置"""

    # 利息计算
    interest_calculation:
      day_count_convention: str        # "actual/360", "actual/365", "actual/actual"
      rounding_mode: str               # "ROUND_HALF_UP", "ROUND_DOWN"
      decimal_places: int = 2

    # 复利计算
    compound_interest:
      enabled: bool = true
      base: str = "accrued_interest"   # 基于累计未还利息

    # 罚息计算
    penalty_interest:
      enabled: bool = true
      rate_type: str = "percentage"    # "percentage" 或 "fixed"
      rate_value: float = 1.5          # 上浮50%
      start_date: str = "due_date"     # 从到期日开始

    # 还款冲销顺序
    repayment_priority: List[str]      # ["fee", "penalty", "compound", "interest", "principal"]

    # 分段计息
    rate_adjustments:
      enabled: bool = true
      auto_detect: bool = true         # 自动识别利率调整

    # 特殊规则
    special_rules: List[SpecialRule]   # 银行特殊规则
```

**计算规则配置示例**:

```yaml
# data/bank_templates/ICBC/v2.0.yaml (续)

calculation_rules:
  # 工商银行计算规则
  interest_calculation:
    day_count_convention: "actual/360"      # 实际天数/360
    rounding_mode: "ROUND_HALF_UP"          # 四舍五入
    decimal_places: 2

  compound_interest:
    enabled: true
    calculation_method: "monthly_compound"  # 按月复利
    compound_rate_same_as_penalty: true     # 复利利率=罚息利率

  penalty_interest:
    enabled: true
    rate_type: "percentage"
    rate_value: 1.5                         # 罚息利率 = 原利率 × 1.5
    calculation_date_rule: "overdue_date"   # 从逾期日开始计算
    penalty_on_penalty: true                # 罚息也要计算复利

  repayment_priority: ["费用", "罚息", "复利", "利息", "本金"]

  rate_adjustments:
    enabled: true
    detection_sources:
      - type: "table"
        sheet_name: "利率调整记录"
      - type: "keyword"
        keywords: ["利率调整", "执行利率变更"]

  special_rules:
    # 规则1: 逾期90天后本金全额到期
    - name: "principal_due_after_90days"
      trigger:
        field: "days_overdue"
        operator: ">"
        value: 90
      action:
        type: "mark_principal_due"
        value: "full_amount"

    # 规则2: 逾期180天后停止计息
    - name: "stop_interest_after_180days"
      trigger:
        field: "days_overdue"
        operator: ">"
        value: 180
      action:
        type: "stop_interest"
```

---

### 模块6: 输出格式 (OutputFormat)

**职责**: 定义Excel输出的格式和样式

```python
@dataclass
class OutputFormat:
    """输出格式配置"""

    # Excel结构
    workbook_structure: WorkbookStructure

    # 样式定义
    styles: StyleDefinition

    # 自定义公式
    custom_formulas: List[FormulaDefinition]

    # 页眉页脚
    header_footer: HeaderFooterConfig


@dataclass
class WorkbookStructure:
    """工作簿结构"""
    sheets: List[SheetConfig]

@dataclass
class SheetConfig:
    """工作表配置"""
    name: str                            # 工作表名称
    type: str                            # "summary", "detail", "payment", "adjustment"
    data_source: str                     # 数据来源
    columns: List[ColumnConfig]          # 列配置
    filters: List[FilterConfig] = None   # 数据过滤规则
    sorting: List[SortConfig] = None     # 排序规则

@dataclass
class ColumnConfig:
    """列配置"""
    field: str                           # 字段名
    header: str                          # 表头文本
    width: float = 12.0                  # 列宽
    data_type: str = "string"            # 数据类型
    number_format: str = None            # 数字格式 (e.g., "#,##0.00")
    date_format: str = "yyyy-mm-dd"      # 日期格式
    alignment: str = "center"            # 对齐方式
    font: FontConfig = None              # 字体配置
    background_color: str = None         # 背景色
```

**输出格式配置示例**:

```yaml
# data/bank_templates/ICBC/v2.0.yaml (续)

output_format:
  workbook_structure:
    sheets:
      # 工作表1: 债权汇总
      - name: "债权汇总"
        type: "summary"
        data_source: "summary"
        columns:
          - field: "loan_amount"
            header: "贷款金额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "remaining_principal"
            header: "剩余本金"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "accrued_interest"
            header: "应计利息"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "penalty_interest"
            header: "罚息"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "total_debt"
            header: "债权总额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"
            font:
              bold: true
              color: "FF0000"

      # 工作表2: 计息明细
      - name: "计息明细"
        type: "detail"
        data_source: "calculation_details"
        columns:
          - field: "period_start"
            header: "计息期间起"
            width: 12
            data_type: "date"
            date_format: "yyyy-mm-dd"

          - field: "period_end"
            header: "计息期间止"
            width: 12
            data_type: "date"
            date_format: "yyyy-mm-dd"

          - field: "days"
            header: "计息天数"
            width: 10
            data_type: "int"
            alignment: "center"

          - field: "principal_balance"
            header: "本金余额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"

          - field: "rate"
            header: "年利率(%)"
            width: 10
            data_type: "decimal"
            number_format: "0.0000"

          - field: "interest"
            header: "利息金额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"

      # 工作表3: 还款记录
      - name: "还款记录"
        type: "payment"
        data_source: "payment_records"
        # ...

  # 样式定义
  styles:
    default_font:
      name: "宋体"
      size: 10
    header_font:
      name: "宋体"
      size: 11
      bold: true
    header_background:
      pattern_type: "solid"
      fg_color: "D3D3D3"
    border_style:
      style: "thin"
      color: "000000"

  # 页眉页脚
  header_footer:
    header:
      left: "&[Date]"
      center: "中国工商银行 - 债权计算表"
      right: "第 &P 页，共 &N 页"
    footer:
      center: "本表由系统自动生成，请核对"
```

---

### 模块7: 数据提取引擎 (DataExtractionEngine)

**职责**: 根据模板配置提取数据

```python
class DataExtractionEngine:
    """数据提取引擎"""

    def __init__(self, template: BankTemplate):
        self.template = template

    def extract(self, file_bytes: bytes, filename: str, ocr_text: str) -> ExtractedData:
        """
        提取数据

        Args:
            file_bytes: 文件字节数据
            filename: 文件名
            ocr_text: OCR提取的文本

        Returns:
            ExtractedData: 提取的数据对象
        """
        result = ExtractedData()

        # 遍历所有字段映射
        for field_name, field_mapping in self.template.field_mappings.items():
            extracted_value = None
            source_info = None

            # 按优先级尝试各种提取方法
            for method in sorted(field_mapping.extraction_methods, key=lambda m: m.priority):
                try:
                    if method.method_type == "regex":
                        extracted_value, source_info = self._extract_by_regex(
                            ocr_text, method
                        )
                    elif method.method_type == "position":
                        extracted_value, source_info = self._extract_by_position(
                            file_bytes, method
                        )
                    elif method.method_type == "table":
                        extracted_value, source_info = self._extract_from_table(
                            file_bytes, method
                        )
                    elif method.method_type == "llm":
                        extracted_value, source_info = self._extract_by_llm(
                            ocr_text, method
                        )

                    if extracted_value is not None:
                        break

            # 数据验证
            if field_mapping.validators:
                for validator in field_mapping.validators:
                    if not validator.validate(extracted_value):
                        extracted_value = field_mapping.default_value
                        break

            # 存储结果
            setattr(result, field_name, extracted_value)

        return result

    def _extract_by_regex(self, text: str, method: ExtractionMethod):
        """正则表达式提取"""
        match = re.search(
            method.regex_pattern,
            text,
            flags=0 if method.case_sensitive else re.IGNORECASE
        )
        if match:
            value = match.group(method.regex_group or 1)
            return self._post_process(value, method.post_process)
        return None, None

    def _extract_by_position(self, file_bytes: bytes, method: ExtractionMethod):
        """位置提取（Excel）"""
        wb = load_workbook(filename=io.BytesIO(file_bytes))
        ws = wb[method.sheet_name]

        if method.cell_range:
            cell = ws[method.cell_range]
            value = cell.value
        elif method.row_index and method.column_index:
            cell = ws.cell(row=method.row_index, column=method.column_index)
            value = cell.value
        else:
            return None, None

        return self._post_process(value, method.post_process)

    def _extract_from_table(self, file_bytes: bytes, method: ExtractionMethod):
        """从表格提取"""
        wb = load_workbook(filename=io.BytesIO(file_bytes))
        ws = wb[method.sheet_name]

        # 查找表头行
        header_row = None
        for row in ws.iter_rows(values_only=True):
            if method.table_header in str(row):
                header_row = row
                break

        if not header_row:
            return None, None

        # 提取数据行
        records = []
        for row in ws.iter_rows(min_row=header_row[0].row + 1, values_only=True):
            if not row[0]:  # 空行
                continue
            record = {}
            for field, col_idx in method.column_mappings.items():
                record[field] = row[col_idx]
            records.append(record)

        return records, f"Table: {method.sheet_name}"

    def _post_process(self, value: Any, post_process: List[str]) -> Any:
        """后处理"""
        if not post_process:
            return value

        for operation in post_process:
            if operation == "remove_comma":
                value = str(value).replace(",", "")
            elif operation == "multiply_12":
                value = float(value) * 12
            elif operation == "divide_100":
                value = float(value) / 100
            # ... 更多后处理操作

        return value
```

---

### 模块8: 计算引擎 (CalculationEngine)

**职责**: 根据计算规则执行债权计算

```python
class CalculationEngine:
    """债权计算引擎"""

    def __init__(self, rules: CalculationRules):
        self.rules = rules

    def calculate(self, data: ExtractedData) -> CalculationResult:
        """
        执行债权计算

        Args:
            data: 提取的数据

        Returns:
            CalculationResult: 计算结果
        """
        result = CalculationResult()

        # 1. 基础信息
        result.loan_amount = data.loan_amount
        result.start_date = data.start_date
        result.end_date = data.end_date

        # 2. 初始化本金余额
        principal_balance = Decimal(str(data.loan_amount))

        # 3. 应用还款记录
        for payment in data.repayment_records:
            principal_balance = self._apply_payment(
                principal_balance, payment, result
            )

        # 4. 计算利息
        if self.rules.interest_calculation:
            result.interest_details = self._calculate_interest(
                principal_balance, data, result
            )

        # 5. 计算罚息
        if self.rules.penalty_interest.enabled:
            result.penalty_details = self._calculate_penalty(
                principal_balance, data, result
            )

        # 6. 计算复利
        if self.rules.compound_interest.enabled:
            result.compound_details = self._calculate_compound(
                data, result
            )

        # 7. 汇总
        result.remaining_principal = float(principal_balance)
        result.total_interest = sum(detail.amount for detail in result.interest_details)
        result.total_penalty = sum(detail.amount for detail in result.penalty_details)
        result.total_compound = sum(detail.amount for detail in result.compound_details)
        result.total_debt = (
            result.remaining_principal +
            result.total_interest +
            result.total_penalty +
            result.total_compound
        )

        return result

    def _apply_payment(
        self,
        principal_balance: Decimal,
        payment: PaymentRecord,
        result: CalculationResult
    ) -> Decimal:
        """应用还款，按优先级冲销"""

        remaining_payment = Decimal(str(payment.amount))

        # 按优先级依次冲销
        for priority_type in self.rules.repayment_priority:
            if remaining_payment <= 0:
                break

            if priority_type == "费用":
                # 处理费用...
                pass
            elif priority_type == "罚息":
                # 冲销罚息
                pass
            elif priority_type == "复利":
                # 冲销复利
                pass
            elif priority_type == "利息":
                # 冲销利息
                pass
            elif priority_type == "本金":
                # 冲销本金
                payment_amount = min(remaining_payment, principal_balance)
                principal_balance -= payment_amount
                remaining_payment -= payment_amount

        return principal_balance

    def _calculate_interest(
        self,
        principal_balance: Decimal,
        data: ExtractedData,
        result: CalculationResult
    ) -> List[InterestDetail]:
        """计算利息"""
        details = []

        # 分段计算（考虑利率调整）
        periods = self._split_calculation_periods(data)

        for period in periods:
            days = (period.end_date - period.start_date).days
            interest = (
                Decimal(str(principal_balance)) *
                Decimal(str(period.rate)) / 100 *
                Decimal(days) /
                Decimal(360)  # 或 365
            )

            # 四舍五入
            interest = interest.quantize(
                Decimal('0.01'),
                rounding=ROUND_HALF_UP
            )

            details.append(InterestDetail(
                period_start=period.start_date,
                period_end=period.end_date,
                days=days,
                principal_balance=float(principal_balance),
                rate=period.rate,
                amount=float(interest)
            ))

        return details

    def _split_calculation_periods(self, data: ExtractedData) -> List[CalculationPeriod]:
        """根据利率调整分段"""
        periods = []

        # 获取所有利率调整点
        adjustment_dates = sorted(
            [adj.date for adj in data.rate_adjustments],
            reverse=False
        )

        # 分段...
        return periods
```

---

### 模块9: 输出生成器 (OutputGenerator)

**职责**: 根据输出格式生成Excel文件

```python
class OutputGenerator:
    """输出生成器"""

    def __init__(self, format_config: OutputFormat):
        self.format = format_config

    def generate(self, result: CalculationResult) -> bytes:
        """
        生成Excel文件

        Args:
            result: 计算结果

        Returns:
            bytes: Excel文件字节数据
        """
        from openpyxl import Workbook
        from openpyxl.styles import Font, Alignment, PatternFill, Border, Side

        wb = Workbook()

        # 根据配置生成各个工作表
        for sheet_config in self.format.workbook_structure.sheets:
            ws = wb.create_sheet(sheet_config.name)

            # 写入表头
            self._write_header(ws, sheet_config)

            # 写入数据
            self._write_data(ws, sheet_config, result)

            # 应用样式
            self._apply_styles(ws, sheet_config)

        return self._save_to_bytes(wb)

    def _write_header(self, worksheet, sheet_config: SheetConfig):
        """写入表头"""
        for col_idx, column in enumerate(sheet_config.columns, 1):
            cell = worksheet.cell(row=1, column=col_idx)
            cell.value = column.header
            # 应用表头样式...

    def _write_data(self, worksheet, sheet_config: SheetConfig, result: CalculationResult):
        """写入数据"""
        data_source = getattr(result, sheet_config.data_source)

        for row_idx, record in enumerate(data_source, 2):
            for col_idx, column in enumerate(sheet_config.columns, 1):
                cell = worksheet.cell(row=row_idx, column=col_idx)

                # 获取字段值
                value = getattr(record, column.field, None)

                # 格式化
                if column.data_type == "decimal":
                    cell.value = float(value) if value else 0
                    cell.number_format = column.number_format
                elif column.data_type == "date":
                    cell.value = value
                    cell.number_format = column.date_format
                else:
                    cell.value = value
```

---

## 📁 目录结构设计

```
F:\AC\
├── app.py                           # Streamlit主应用
├── config.yaml                      # 全局配置
├── requirements.txt                 # Python依赖
│
├── modules/                         # 核心模块
│   ├── __init__.py
│   ├── bank_detector.py            # 银行识别器
│   ├── template_manager.py         # 模板管理器
│   ├── data_extraction_engine.py   # 数据提取引擎
│   ├── calculation_engine.py       # 计算引擎
│   ├── output_generator.py         # 输出生成器
│   ├── ocr_processor.py            # OCR处理（已有）
│   ├── calculator.py               # 计算器（已有）
│   ├── excel_exporter.py           # Excel导出（已有）
│   │
│   ├── models/                     # 数据模型
│   │   ├── __init__.py
│   │   ├── bank_template.py       # 银行模板模型
│   │   ├── field_mapping.py       # 字段映射模型
│   │   ├── calculation_rules.py   # 计算规则模型
│   │   ├── extracted_data.py      # 提取数据模型
│   │   └── calculation_result.py  # 计算结果模型
│   │
│   └── validators/                 # 验证器
│       ├── __init__.py
│       ├── field_validator.py
│       └── calculation_validator.py
│
├── data/                          # 数据目录
│   ├── bank_templates/           # 银行模板仓库
│   │   ├── registry.yaml         # 模板注册表
│   │   │
│   │   ├── ICBC/                 # 工商银行
│   │   │   ├── v1.0.yaml
│   │   │   └── v2.0.yaml
│   │   │
│   │   ├── CCB/                  # 建设银行
│   │   │   ├── v1.0.yaml
│   │   │   └── v2.1.yaml
│   │   │
│   │   ├── BCM/                  # 交通银行
│   │   │   └── v1.0.yaml
│   │   │
│   │   └── TEMPLATE_SCHEMA.md   # 模板编写指南
│   │
│   ├── history/                  # 历史记录
│   ├── output/                   # 输出文件
│   └── temp/                     # 临时文件
│
├── utils/                        # 工具函数
│   ├── __init__.py
│   ├── date_utils.py            # 日期工具
│   ├── number_utils.py          # 数字工具
│   └── excel_utils.py           # Excel工具
│
├── prompts/                      # LLM提示词
│   └── ...
│
├── tests/                        # 测试
│   ├── test_bank_detector.py
│   ├── test_template_manager.py
│   └── ...
│
└── docs/                         # 文档
    ├── ARCHITECTURE.md          # 架构文档
    ├── TEMPLATE_GUIDE.md        # 模板编写指南
    └── API.md                   # API文档
```

---

## 🔄 工作流程

### 完整处理流程

```
1. 用户上传文件
   ↓
2. BankDetector 识别银行和版本
   │
   ├─ 文件名模式匹配
   ├─ OCR关键词检测
   ├─ 文档结构分析
   └─ 用户手动选择（兜底）
   ↓
3. TemplateManager 加载对应模板
   │
   ├─ 读取模板YAML文件
   ├─ 验证模板完整性
   └─ 初始化提取引擎
   ↓
4. DataExtractionEngine 提取数据
   │
   ├─ Excel解析（如果是Excel）
   ├─ OCR识别（如果是PDF/图片）
   ├─ 按字段映射提取各字段
   └─ 数据验证
   ↓
5. 用户确认/修改提取的数据
   ↓
6. CalculationEngine 执行计算
   │
   ├─ 应用还款冲销
   ├─ 分段计息
   ├─ 计算罚息
   ├─ 计算复利
   └─ 生成计算结果
   ↓
7. OutputGenerator 生成Excel
   │
   ├─ 按模板配置创建工作表
   ├─ 填充数据
   ├─ 应用样式
   └─ 输出文件
   ↓
8. 下载Excel文件
```

---

## 🎯 核心优势

### 1. 可扩展性
- 新增银行：只需添加模板YAML文件
- 新增版本：在对应银行目录下添加新版本YAML
- 无需修改核心代码

### 2. 可配置性
- 字段映射：支持正则、位置、表格、LLM多种提取方式
- 计算规则：完整配置计息、罚息、复利规则
- 输出格式：自定义Excel结构、样式、公式

### 3. 灵活性
- 多策略提取：按优先级尝试多种方法
- 数据验证：确保提取数据的准确性
- 用户介入：识别不确定时可手动选择

### 4. 可维护性
- 模板独立：配置与代码分离
- 版本管理：清晰追踪模板版本
- 易于调试：记录提取来源和置信度

---

## 📊 模板YAML完整示例

```yaml
# data/bank_templates/ICBC/v2.0.yaml

# ===============================
# 基本信息
# ===============================
meta:
  bank_code: "ICBC"
  bank_name: "中国工商银行"
  version: "2.0"
  version_name: "标准版对账单"
  author: "系统"
  created_date: "2024-01-01"
  updated_date: "2024-03-28"
  description: "适用于工商银行标准版企业网银对账单"

# ===============================
# 文件识别
# ===============================
file_recognition:
  supported_formats:
    - "xlsx"
    - "xls"
    - "pdf"

  filename_patterns:
    - "工商银行_*"
    - "ICBC_*"

  ocr_keywords:
    - "中国工商银行"
    -工商银行股份有限公司"
    - "ICBC (中国工商银行)"

# ===============================
# 字段映射
# ===============================
field_mappings:
  loan_amount:
    target_field: "loan_amount"
    data_type: "decimal"
    required: true
    extraction_methods:
      - method_type: "position"
        priority: 1
        sheet_name: "基本信息"
        cell_range: "B2"
        post_process: ["remove_comma"]

      - method_type: "regex"
        priority: 2
        regex_pattern: "贷款金额[:：]\s*([0-9,]+\.?[0-9]*)"
        post_process: ["remove_comma"]

      - method_type: "llm"
        priority: 3
        llm_prompt: "请提取贷款金额"

    validators:
      - type: "range"
        min: 0
        max: 1000000000

  annual_interest_rate:
    target_field: "annual_interest_rate"
    data_type: "decimal"
    required: true
    extraction_methods:
      - method_type: "table"
        priority: 1
        sheet_name: "利率信息"
        table_header: "执行利率"
        table_column: 2

      - method_type: "regex"
        priority: 2
        regex_pattern: "年利率[:：]\s*([0-9]+\.?[0-9]*)\s*%"

    validators:
      - type: "range"
        min: 0
        max: 100

  start_date:
    target_field: "start_date"
    data_type: "date"
    required: true
    extraction_methods:
      - method_type: "position"
        priority: 1
        sheet_name: "基本信息"
        cell_range: "B3"

      - method_type: "regex"
        priority: 2
        regex_pattern: "贷款起始日[：:]\s*([0-9]{4}[-年][0-9]{1,2}[-月][0-9]{1,2}[日]?)"
        post_process: ["normalize_date"]

  end_date:
    target_field: "end_date"
    data_type: "date"
    required: true
    extraction_methods:
      - method_type: "position"
        priority: 1
        sheet_name: "基本信息"
        cell_range: "B4"

      - method_type: "regex"
        priority: 2
        regex_pattern: "贷款到期日[：:]\s*([0-9]{4}[-年][0-9]{1,2}[-月][0-9]{1,2}[日]?)"
        post_process: ["normalize_date"]

  calculation_date:
    target_field: "calculation_date"
    data_type: "date"
    required: false
    extraction_methods:
      - method_type: "regex"
        priority: 1
        regex_pattern: "查询截止日期[：:]\s*([0-9]{4}[-年][0-9]{1,2}[-月][0-9]{1,2}[日]?)"
        post_process: ["normalize_date"]

    default_value: "TODAY"

  remaining_principal:
    target_field: "remaining_principal"
    data_type: "decimal"
    required: false
    extraction_methods:
      - method_type: "position"
        priority: 1
        sheet_name: "账户信息"
        cell_range: "B5"
        post_process: ["remove_comma"]

      - method_type: "regex"
        priority: 2
        regex_pattern: "本金余额[:：]\s*([0-9,]+\.?[0-9]*)"
        post_process: ["remove_comma"]

  accrued_interest:
    target_field: "accrued_interest"
    data_type: "decimal"
    required: false
    extraction_methods:
      - method_type: "position"
        priority: 1
        sheet_name: "账户信息"
        cell_range: "B6"
        post_process: ["remove_comma"]

    default_value: 0

  repayment_records:
    target_field: "repayment_records"
    data_type: "array"
    required: false
    extraction_methods:
      - method_type: "table"
        priority: 1
        sheet_name: "交易明细"
        table_header: "交易日期"
        column_mappings:
          date: 0
          description: 1
          type: 2
          amount: 3
          balance: 4
        row_filters:
          - column: 2
            exclude_values: ["开户", "放款", "利息结转", "交易类型"]
        type_mapping:
          "还款": "principal"
          "利息回收": "interest"
          "罚息回收": "penalty"
          "费用回收": "fee"

  rate_adjustments:
    target_field: "rate_adjustments"
    data_type: "array"
    required: false
    extraction_methods:
      - method_type: "table"
        priority: 1
        sheet_name: "利率调整记录"
        table_header: "调整日期"
        column_mappings:
          date: 0
          rate: 1

# ===============================
# 计算规则
# ===============================
calculation_rules:
  interest_calculation:
    day_count_convention: "actual/360"
    rounding_mode: "ROUND_HALF_UP"
    decimal_places: 2

  compound_interest:
    enabled: true
    calculation_method: "monthly_compound"
    compound_rate_same_as_penalty: true

  penalty_interest:
    enabled: true
    rate_type: "percentage"
    rate_value: 1.5
    calculation_date_rule: "overdue_date"
    penalty_on_penalty: true

  repayment_priority: ["费用", "罚息", "复利", "利息", "本金"]

  rate_adjustments:
    enabled: true
    auto_detect: true

  special_rules:
    - name: "principal_due_after_90days"
      description: "逾期90天后本金全额到期"
      enabled: true
      trigger:
        field: "days_overdue"
        operator: ">"
        value: 90
      action:
        type: "mark_principal_due"
        value: "full_amount"

# ===============================
# 输出格式
# ===============================
output_format:
  workbook_structure:
    sheets:
      - name: "债权汇总"
        type: "summary"
        data_source: "summary"
        columns:
          - field: "loan_amount"
            header: "贷款金额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "remaining_principal"
            header: "剩余本金"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "total_interest"
            header: "利息总额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "total_penalty"
            header: "罚息总额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "total_compound"
            header: "复利总额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "total_debt"
            header: "债权总额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"
            font:
              bold: true
              color: "FF0000"

      - name: "计息明细"
        type: "detail"
        data_source: "interest_details"
        columns:
          - field: "period_start"
            header: "计息期间起"
            width: 12
            data_type: "date"
            date_format: "yyyy-mm-dd"

          - field: "period_end"
            header: "计息期间止"
            width: 12
            data_type: "date"
            date_format: "yyyy-mm-dd"

          - field: "days"
            header: "计息天数"
            width: 10
            data_type: "int"
            alignment: "center"

          - field: "principal_balance"
            header: "本金余额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"

          - field: "rate"
            header: "年利率(%)"
            width: 10
            data_type: "decimal"
            number_format: "0.0000"

          - field: "interest"
            header: "利息金额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"

      - name: "还款记录"
        type: "payment"
        data_source: "payment_records"
        columns:
          - field: "date"
            header: "还款日期"
            width: 12
            data_type: "date"
            date_format: "yyyy-mm-dd"

          - field: "type"
            header: "还款类型"
            width: 10
            alignment: "center"
            value_mapping:
              principal: "本金"
              interest: "利息"
              penalty: "罚息"
              compound: "复利"
              fee: "费用"

          - field: "amount"
            header: "还款金额"
            width: 15
            data_type: "decimal"
            number_format: "#,##0.00"
            alignment: "right"

          - field: "description"
            header: "备注"
            width: 30

  styles:
    default_font:
      name: "宋体"
      size: 10

    header_font:
      name: "宋体"
      size: 11
      bold: true

    header_background:
      pattern_type: "solid"
      fg_color: "D3D3D3"

    border_style:
      style: "thin"
      color: "000000"

  header_footer:
    header:
      left: "&[Date]"
      center: "中国工商银行 - 债权计算表"
      right: "第 &P 页，共 &N 页"

    footer:
      center: "本表由系统自动生成，请核对"
```

---

## 🚀 实施步骤

### 阶段1: 核心架构搭建（2-3天）
1. 创建数据模型（models/）
2. 实现BankDetector
3. 实现TemplateManager
4. 创建模板YAML schema

### 阶段2: 数据提取引擎（2-3天）
1. 实现DataExtractionEngine
2. 支持正则提取
3. 支持Excel位置提取
4. 支持表格提取

### 阶段3: 计算引擎（2-3天）
1. 重构CalculationEngine
2. 支持配置化计算规则
3. 实现分段计息
4. 实现特殊规则

### 阶段4: 输出生成器（1-2天）
1. 实现OutputGenerator
2. 支持配置化Excel输出
3. 应用样式和格式

### 阶段5: 模板创建（持续）
1. 为每个银行创建模板
2. 测试和优化
3. 版本迭代

### 阶段6: UI集成（1-2天）
1. 修改Streamlit界面
2. 支持银行选择
3. 支持模板管理
4. 支持结果预览

---

## 📝 总结

这个设计方案的核心优势：

1. **完全可配置**：新增银行只需添加YAML模板，无需改代码
2. **多策略提取**：正则、位置、表格、LLM多种方法配合
3. **灵活计算**：支持各银行的特殊计算规则
4. **自定义输出**：按银行要求定制Excel格式
5. **易于维护**：清晰的目录结构和模块划分

**下一步建议**：
1. 我可以开始实现核心模块
2. 或者先创建1-2个银行模板作为示例
3. 或者细化某个模块的设计

**你想从哪里开始？** 🎯
