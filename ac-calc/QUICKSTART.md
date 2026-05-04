# 快速开始指南

## 📋 前置要求

在运行应用之前，请确保已安装以下软件：

### 1. Python (3.8+)
- 下载地址：https://www.python.org/downloads/
- 安装时勾选 "Add Python to PATH"

### 2. Tesseract OCR
**Windows:**
- 下载：https://github.com/UB-Mannheim/tesseract/wiki
- 安装到默认路径：`C:\Program Files\Tesseract-OCR`
- 如果安装到其他位置，请修改 `config.yaml` 中的 `tesseract_cmd` 路径

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install tesseract-ocr tesseract-ocr-chi-sim
```

**Mac:**
```bash
brew install tesseract tesseract-lang
```

### 3. Ollama (本地大模型)
- 下载：https://ollama.ai
- 安装后运行以下命令拉取模型：
```bash
ollama pull qwen2.5:7b
```
- 确保Ollama服务正在运行（通常安装后自动启动）

## 🚀 启动应用

### 方法1：使用启动脚本（推荐）

**Windows:**
双击运行 `start.bat`

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

### 方法2：手动启动

1. 安装依赖：
```bash
pip install -r requirements.txt
```

2. 启动Ollama（如果未运行）：
```bash
# 新终端窗口
ollama serve
```

3. 启动应用：
```bash
streamlit run app.py
```

4. 浏览器访问：http://localhost:8501

## 🔧 常见问题

### Q1: Ollama连接失败
**解决方法：**
1. 确认Ollama已启动：`ollama list`
2. 检查配置文件中的地址是否正确（默认 `http://localhost:11434`）
3. 尝试在浏览器访问 http://localhost:11434

### Q2: OCR识别不准确
**解决方法：**
1. 确认Tesseract已正确安装中文语言包
2. 提高PDF的DPI设置（在 `config.yaml` 中调整）
3. 使用更清晰的扫描件

### Q3: 依赖安装失败
**解决方法：**
1. 使用虚拟环境：
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows
pip install -r requirements.txt
```

2. 单独安装失败的包：
```bash
pip install streamlit pandas openpyxl
```

## 📖 使用流程

1. **上传PDF** - 选择债权相关PDF文件（合同、判决书等）
2. **AI提取** - 系统自动提取关键信息（需Ollama运行）
3. **核对数据** - 检查并修正提取的信息
4. **执行计算** - 生成债权计算结果
5. **导出Excel** - 下载标准化台账

## 🎯 测试数据

如果没有真实PDF，可以：

1. 点击"⌨️ 跳过上传，手动录入"
2. 填写测试数据：
   - 贷款金额：1000000
   - 年利率：4.35
   - 起始日期：2024-01-01
   - 到期日期：2025-01-01
   - 剩余本金：800000
3. 添加几条还款记录
4. 点击"🚀 开始计算"

## 📞 技术支持

如遇问题：
1. 查看 `logs/app.log` 日志文件
2. 检查 `config.yaml` 配置是否正确
3. 确认所有前置软件已正确安装

祝使用愉快！ 🎉
