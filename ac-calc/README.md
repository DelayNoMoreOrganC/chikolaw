# 债权计算表自动生成系统

## 项目简介

本系统是一个基于 Python 和 Streamlit 的Web应用程序，用于自动化处理银行及金融不良资产（NPL）的债权计算。系统支持从扫描版PDF提取关键要素、人工复核修改、执行复杂的逾期利息与罚息计算，最终导出标准化Excel台账。

## 核心功能

- 📄 **智能文档解析**：支持PDF上传，使用OCR技术提取文本信息
- 🤖 **本地大模型提取**：通过Ollama调用本地部署的大模型（如Qwen2.5、Llama3）进行结构化信息抽取
- ✏️ **可视化数据编辑**：提供友好的Web界面进行数据核对和修改
- 🧮 **精确债权计算**：支持分段利率、复利、罚息、还款冲销等复杂计算逻辑
- 📊 **专业Excel导出**：生成标准化的债权计算台账，包含汇总、明细、还款记录等多个Sheet

## 技术栈

- **前端框架**：Streamlit
- **OCR处理**：PyMuPDF, pdfplumber, pytesseract
- **大模型**：Ollama（本地部署）
- **数据处理**：pandas, numpy
- **Excel导出**：openpyxl, xlsxwriter

## 安装步骤

### 1. 克隆项目

```bash
cd F:\AC
```

### 2. 安装Python依赖

```bash
pip install -r requirements.txt
```

### 3. 安装Tesseract OCR

**Windows:**
- 下载安装程序：https://github.com/UB-Mannheim/tesseract/wiki
- 安装后修改 `config.yaml` 中的 `tesseract.tesseract_cmd` 路径

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install tesseract-ocr tesseract-ocr-chi-sim
```

**Mac:**
```bash
brew install tesseract tesseract-lang
```

### 4. 安装并启动Ollama

```bash
# 安装Ollama（访问 https://ollama.ai 下载）
# 启动Ollama服务（自动在后台运行）

# 拉取模型（以qwen2.5为例）
ollama pull qwen2.5:7b
```

### 5. 配置文件

根据实际环境修改 `config.yaml` 配置文件。

### 6. 运行应用

```bash
streamlit run app.py
```

应用将在浏览器中打开：http://localhost:8501

## 项目结构

```
F:\AC\
├── app.py                  # Streamlit主应用入口
├── config.yaml             # 配置文件
├── requirements.txt        # Python依赖
├── modules/                # 核心功能模块
│   ├── llm_extractor.py    # 本地大模型信息提取
│   ├── calculator.py       # 债权计算引擎
│   ├── ocr_processor.py    # OCR文本提取
│   └── excel_exporter.py   # Excel导出
├── utils/                  # 工具类
│   ├── date_utils.py       # 日期处理工具
│   ├── validators.py       # 数据验证
│   └── constants.py        # 常量定义
└── data/                   # 数据目录
    ├── temp/               # 临时文件
    └── output/             # 导出文件
```

## 使用说明

### 基本流程

1. **上传PDF文件**：选择债权相关的PDF文档（合同、判决书等）
2. **信息提取**：系统自动提取关键信息（贷款金额、利率、起止日期等）
3. **数据核对**：检查提取的信息，必要时进行修改
4. **添加还款记录**：输入历史还款记录（支持多条）
5. **执行计算**：生成详细的债权计算明细
6. **导出Excel**：下载标准化的债权计算表

### 注意事项

- 确保Ollama服务在本地运行（`http://localhost:11434`）
- PDF文件建议使用清晰扫描件，提高OCR识别准确率
- 计算结果请人工复核后再使用

## 计算规则

### 利息计算公式

```
利息 = 本金余额 × 年利率 × 计息天数 / 360
```

### 复利计算

```
复利 = 累计欠利息 × 年利率 × 计息天数 / 360
```

### 罚息计算

```
罚息 = 逾期本金 × 罚息利率 × 计息天数 / 360
```

### 还款抵扣顺序

1. 费用
2. 罚息/违约金
3. 复利
4. 利息
5. 本金

## 开发说明

### 运行测试

```bash
python -m pytest tests/
```

### 添加新功能

1. 在 `modules/` 目录创建新模块
2. 在 `utils/` 添加必要的工具函数
3. 在 `app.py` 中集成UI

## 常见问题

**Q: Ollama连接失败？**
- 检查Ollama服务是否运行：`ollama list`
- 确认配置文件中的地址和端口正确

**Q: OCR识别不准确？**
- 提高PDF的DPI设置（在config.yaml中调整）
- 确保Tesseract已正确安装中文语言包

**Q: 计算结果有误？**
- 检查利率分段是否正确设置
- 核对还款记录的日期和金额
- 查看日志文件排查问题

## 许可证

MIT License

## 联系方式

如有问题或建议，请提交Issue。
