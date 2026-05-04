"""
债权计算表自动生成系统 - 主应用（按月计息版）
基于Streamlit的Web应用程序
"""

import streamlit as st
import os
from datetime import datetime
from pathlib import Path
import sys
from urllib.parse import urlparse, urlunparse

# 添加项目根目录到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

# 导入配置
import yaml
try:
    from dotenv import load_dotenv
except ImportError:
    def load_dotenv(*args, **kwargs):
        return False

# 加载环境变量
load_dotenv()

# 导入模块
from modules.calculator import create_calculator
from modules.excel_exporter import create_excel_exporter
from modules.history_store import create_history_store
from modules import create_ocr_processor, create_llm_extractor
from modules.bank_loan_flow import (
    TEMPLATE_KEY as FLOW_TEMPLATE_KEY,
    calculate_loan_flow_result,
    maybe_extract_loan_flow_preview,
)
from utils import (
    RepaymentType,
    validate_loan_contract,
    validate_repayment_records,
    validate_rate_adjustments,
    parse_date,
    format_date,
    get_current_date,
    get_resource_path
)
from utils.constants import APP_TITLE


# ============================================
# 页面配置
# ============================================
st.set_page_config(
    page_title=APP_TITLE,
    page_icon="📊",
    layout="wide",
    initial_sidebar_state="expanded"
)

# 蓝色系CSS样式
st.markdown("""
<style>
    /* 全局样式 */
    .main {
        background-color: #f8fafc;
    }

    .main .block-container {
        padding-top: 2rem;
        padding-bottom: 2rem;
        max-width: 1400px;
    }

    /* 标题样式 - 蓝色系 */
    .main-title {
        background:
            radial-gradient(circle at top right, rgba(255,255,255,0.22), transparent 30%),
            linear-gradient(135deg, #2563eb 0%, #1d4ed8 55%, #0f172a 100%);
        color: white;
        padding: 2rem;
        border-radius: 20px;
        text-align: center;
        margin-bottom: 2rem;
        box-shadow: 0 18px 40px rgba(37, 99, 235, 0.18);
        border: 1px solid rgba(255,255,255,0.12);
    }

    .main-title h1 {
        margin: 0;
        font-size: 2.2rem;
        font-weight: 700;
        color: white;
    }

    .main-title p {
        margin: 0.5rem 0 0 0;
        opacity: 0.95;
        font-size: 1rem;
        color: white;
    }

    /* 章节标题 - 蓝色系 */
    .section-header {
        background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        margin: 1.5rem 0 1rem 0;
        font-size: 1.4rem;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
    }

    /* 卡片容器 - 白色 */
    .card-container {
        background: white;
        border: 1px solid #e5e7eb;
        border-radius: 16px;
        padding: 1.5rem 1.6rem;
        margin: 1rem 0;
        box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
    }

    /* 表单容器 */
    .form-container {
        background: white;
        border: 1px solid #e5e7eb;
        border-radius: 16px;
        padding: 2rem;
        margin: 1rem 0;
        box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
    }

    div[data-testid="stMetric"] {
        background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
        border: 1px solid #dbeafe;
        padding: 0.85rem 1rem;
        border-radius: 14px;
        box-shadow: 0 8px 20px rgba(37, 99, 235, 0.06);
    }

    div[data-testid="stTabs"] button {
        border-radius: 999px;
    }

    div[data-testid="stDataFrame"] {
        border: 1px solid #dbeafe;
        border-radius: 14px;
        overflow: hidden;
    }

    /* 成功消息 */
    .success-card {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        margin: 1rem 0;
        font-weight: 500;
        box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
    }

    /* 警告消息 */
    .warning-card {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        margin: 1rem 0;
        font-weight: 500;
        box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
    }

    /* 错误消息 */
    .error-card {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        margin: 1rem 0;
        font-weight: 500;
        box-shadow: 0 2px 8px rgba(239, 68, 68, 0.2);
    }

    /* 信息消息 */
    .info-card {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        margin: 1rem 0;
        font-weight: 500;
        box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
    }

    /* 按钮样式 - 蓝色系 */
    .stButton > button {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
        color: white;
        border: none;
        border-radius: 8px;
        padding: 0.5rem 2rem;
        font-weight: 600;
        transition: all 0.2s;
        box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
    }

    .stButton > button:hover {
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
    }

    /* 次要按钮 */
    .stButton > button[kind="secondary"] {
        background: white;
        color: #3b82f6;
        border: 2px solid #3b82f6;
    }

    /* 侧边栏样式 - 蓝色系 */
    .sidebar .sidebar-content {
        background: linear-gradient(180deg, #3b82f6 0%, #1e40af 100%);
    }

    /* 滚动条样式 - 蓝色系 */
    ::-webkit-scrollbar {
        width: 10px;
        height: 10px;
    }

    ::-webkit-scrollbar-track {
        background: #f1f5f9;
        border-radius: 5px;
    }

    ::-webkit-scrollbar-thumb {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
        border-radius: 5px;
    }

    ::-webkit-scrollbar-thumb:hover {
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    }

    /* 步骤指示器 */
    .step-indicator {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem 0;
        margin: 1rem 0;
    }

    .step-item {
        flex: 1;
        text-align: center;
        padding: 0.5rem;
        opacity: 0.4;
        transition: all 0.3s;
    }

    .step-item.active {
        opacity: 1;
    }

    .step-item.completed {
        opacity: 0.7;
    }

    .step-number {
        display: inline-block;
        width: 40px;
        height: 40px;
        line-height: 40px;
        border-radius: 50%;
        background: white;
        color: #3b82f6;
        font-weight: bold;
        margin-bottom: 0.5rem;
    }

    .step-item.active .step-number {
        background: #3b82f6;
        color: white;
        box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2);
    }

    .step-item.completed .step-number {
        background: #10b981;
        color: white;
    }

    .step-label {
        color: white;
        font-size: 0.85rem;
        font-weight: 500;
    }

    /* 隐藏默认元素 */
    #MainMenu {visibility: hidden;}
    footer {visibility: hidden;}
    .stDeployButton {display:none;}
</style>
""", unsafe_allow_html=True)


# ============================================
# 辅助函数
# ============================================
def load_config():
    """加载配置文件"""
    config_path = get_resource_path("config.yaml")
    with open(config_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


def init_session_state():
    """初始化Session State"""
    if 'current_step' not in st.session_state:
        st.session_state.current_step = 1

    if 'loan_data' not in st.session_state:
        st.session_state.loan_data = {}

    if 'repayment_records' not in st.session_state:
        st.session_state.repayment_records = []

    if 'rate_adjustments' not in st.session_state:
        st.session_state.rate_adjustments = []

    if 'calculation_result' not in st.session_state:
        st.session_state.calculation_result = None

    if 'last_history_record_id' not in st.session_state:
        st.session_state.last_history_record_id = ""

    if 'ocr_text' not in st.session_state:
        st.session_state.ocr_text = ""

    if 'page_images' not in st.session_state:
        st.session_state.page_images = []

    if 'ai_extraction_completed' not in st.session_state:
        st.session_state.ai_extraction_completed = False

    if 'ai_extraction_raw' not in st.session_state:
        st.session_state.ai_extraction_raw = {}

    if 'auto_fill_preview' not in st.session_state:
        st.session_state.auto_fill_preview = {}

    if 'field_sources' not in st.session_state:
        st.session_state.field_sources = {}

    if 'extraction_warnings' not in st.session_state:
        st.session_state.extraction_warnings = []

    if 'pending_repayment_records' not in st.session_state:
        st.session_state.pending_repayment_records = []

    if 'uploaded_statement_name' not in st.session_state:
        st.session_state.uploaded_statement_name = ""

    # 步骤完成状态
    if 'step_completed' not in st.session_state:
        st.session_state.step_completed = {
            1: False,
            2: False,
            3: False
        }


def build_empty_loan_data():
    """构造空白贷款数据，供OCR/AI未识别字段手动补录"""
    return {
        "loan_amount": "",
        "annual_interest_rate": "",
        "start_date": "",
        "end_date": "",
        "calculation_date": format_date(get_current_date()),
        "remaining_principal": "",
        "accrued_interest": "0",
        "penalty_interest": "0",
        "compound_interest": "0",
        "acceleration_date": "",
        "interest_cycle_months": "1",
        "interest_cycle_changes": [],
        "principal_plan_records": [],
        "_template_key": "",
        "_template_name": "",
        "_bank_name": "",
        "_template_payload": {},
    }


def normalize_extracted_loan_data(extracted_data):
    """将AI提取结果归一化，未识别字段统一留空"""
    normalized = build_empty_loan_data()
    extracted_data = extracted_data or {}

    required_fields = [
        "loan_amount",
        "annual_interest_rate",
        "start_date",
        "end_date",
        "calculation_date",
        "remaining_principal",
        "acceleration_date",
    ]
    optional_amount_fields = [
        "accrued_interest",
        "penalty_interest",
        "compound_interest"
    ]

    for field in required_fields:
        value = extracted_data.get(field)
        normalized[field] = "" if value in (None, "") else value

    for field in optional_amount_fields:
        value = extracted_data.get(field)
        normalized[field] = "0" if value in (None, "") else value

    return normalized


def invalidate_calculation_result():
    """数据发生变化后，使旧计算结果失效"""
    st.session_state.calculation_result = None
    st.session_state.step_completed[3] = False


def reset_auto_fill_state(clear_preview_only: bool = False):
    """清理自动填写相关状态"""
    st.session_state.ocr_text = ""
    st.session_state.page_images = []
    st.session_state.ai_extraction_completed = False
    st.session_state.ai_extraction_raw = {}
    st.session_state.auto_fill_preview = {}
    st.session_state.field_sources = {}
    st.session_state.extraction_warnings = []
    st.session_state.pending_repayment_records = []
    st.session_state.uploaded_statement_name = ""

    if not clear_preview_only:
        st.session_state.loan_data = {}
        st.session_state.repayment_records = []
        st.session_state.rate_adjustments = []
        st.session_state.calculation_result = None
        st.session_state.last_history_record_id = ""
        st.session_state.step_completed[2] = False
        st.session_state.step_completed[3] = False


def serialize_form_value(field: str, value):
    """将自动提取结果转换为表单可用的字符串值"""
    if value in (None, ""):
        return "0" if field in {"accrued_interest", "penalty_interest", "compound_interest"} else ""
    if field in {"loan_amount", "annual_interest_rate", "remaining_principal", "accrued_interest", "penalty_interest", "compound_interest"}:
        return str(value)
    return value


def merge_loan_data(current_data, incoming_data, only_missing: bool = False):
    """将自动提取字段写入当前表单数据"""
    merged = build_empty_loan_data()
    merged.update(current_data or {})
    normalized_incoming = normalize_extracted_loan_data(incoming_data or {})

    for field, value in normalized_incoming.items():
        if value in (None, ""):
            continue
        if only_missing and str(merged.get(field, "")).strip() not in ("", "0", "0.0"):
            continue
        merged[field] = serialize_form_value(field, value)

    for extra_key in ("_template_key", "_template_name", "_bank_name", "_template_payload"):
        if extra_key in (incoming_data or {}):
            merged[extra_key] = incoming_data.get(extra_key)

    return merged


def merge_repayment_records(existing_records, incoming_records):
    """按日期+类型+金额去重合并还款记录"""
    merged = list(existing_records or [])
    seen = {
        (
            record.get("date"),
            record.get("type"),
            str(record.get("amount")),
        )
        for record in merged
    }

    for record in incoming_records or []:
        key = (record.get("date"), record.get("type"), str(record.get("amount")))
        if key in seen:
            continue
        seen.add(key)
        merged.append(record)

    return merged


def apply_auto_fill_preview(preview, only_missing: bool = False):
    """将预览结果写入当前会话"""
    if not preview:
        return

    st.session_state.loan_data = merge_loan_data(
        st.session_state.loan_data,
        preview.get("data", {}),
        only_missing=only_missing,
    )

    extracted_records = preview.get("repayment_records", [])
    if only_missing:
        st.session_state.repayment_records = merge_repayment_records(
            st.session_state.repayment_records,
            extracted_records,
        )
    else:
        st.session_state.repayment_records = list(extracted_records)

    st.session_state.pending_repayment_records = list(preview.get("pending_repayment_records", []))
    st.session_state.field_sources = dict(preview.get("field_sources", {}))
    st.session_state.extraction_warnings = list(preview.get("warnings", []))
    st.session_state.ai_extraction_completed = True
    st.session_state.ai_extraction_raw = dict(preview.get("raw_llm_data", {}))
    invalidate_calculation_result()


def check_tesseract_connection(config):
    """检查 Tesseract 是否可用"""
    tesseract_path = (config.get("tesseract") or {}).get("tesseract_cmd", "")
    return {
        "available": bool(tesseract_path) and Path(tesseract_path).exists(),
        "path": tesseract_path
    }


def run_auto_fill_pipeline(config, uploaded_file):
    """执行 OCR + 规则提取 + AI 提取流水线"""
    file_bytes = uploaded_file.getvalue()
    suffix = Path(uploaded_file.name).suffix.lower()

    preview = None
    ocr_text = ""
    page_images = []

    if suffix in {".xlsx", ".xls"}:
        preview = maybe_extract_loan_flow_preview(file_bytes=file_bytes, filename=uploaded_file.name)
    else:
        ocr_processor = create_ocr_processor(config)
        if suffix == ".pdf":
            ocr_text, page_images = ocr_processor.extract_text_from_pdf(
                file_bytes,
                max_pages=config.get("pdf", {}).get("max_pages", 50),
                use_ocr_fallback=True,
                ocr_all_pages=True
            )
        else:
            ocr_text, page_images = ocr_processor.extract_text_from_image_bytes(file_bytes)

        preview = maybe_extract_loan_flow_preview(
            file_bytes=file_bytes,
            filename=uploaded_file.name,
            text=ocr_text,
        )
        if preview is None:
            llm_extractor = create_llm_extractor(config)
            preview = llm_extractor.extract_statement_preview(ocr_text)

    st.session_state.ocr_text = ocr_text
    st.session_state.page_images = page_images
    st.session_state.auto_fill_preview = preview
    st.session_state.field_sources = dict(preview.get("field_sources", {}))
    st.session_state.extraction_warnings = list(preview.get("warnings", []))
    st.session_state.pending_repayment_records = list(preview.get("pending_repayment_records", []))
    st.session_state.uploaded_statement_name = uploaded_file.name
    st.session_state.ai_extraction_completed = True
    st.session_state.ai_extraction_raw = dict(preview.get("raw_llm_data", {}))


def render_field_source_table(preview):
    """展示字段来源片段"""
    rows = []
    label_map = {
        "loan_amount": "贷款本金",
        "annual_interest_rate": "年利率",
        "start_date": "合同起始日",
        "end_date": "合同到期日",
        "calculation_date": "数据暂计日",
        "remaining_principal": "剩余本金",
        "repayment_records": "还款记录",
    }
    preview_data = (preview or {}).get("data", {})
    for field, source in (preview or {}).get("field_sources", {}).items():
        rows.append({
            "字段": label_map.get(field, field),
            "识别值": preview_data.get(field, ""),
            "来源片段": source,
        })

    if rows:
        st.dataframe(rows, hide_index=True, width='stretch')


def render_auto_fill_preview():
    """展示 OCR/AI 自动填写预览结果"""
    preview = st.session_state.auto_fill_preview or {}
    if not preview:
        return

    preview_data = normalize_extracted_loan_data(preview.get("data", {}))

    st.markdown("---")
    display_section_header("自动填写预览", "🤖", "先核对识别结果，再决定是否写入表单")

    col_left, col_right = st.columns([1.15, 1])

    with col_left:
        st.markdown('<div class="card-container">', unsafe_allow_html=True)
        st.subheader("📄 文档与 OCR 预览")
        if st.session_state.uploaded_statement_name:
            st.caption(f"当前样本：{st.session_state.uploaded_statement_name}")
        if st.session_state.page_images:
            st.image(st.session_state.page_images[:2], width=320)
        with st.expander("查看 OCR 文本摘要", expanded=False):
            st.text_area(
                "OCR 文本",
                value=st.session_state.ocr_text[:3000],
                height=260,
                disabled=True,
                label_visibility="collapsed"
            )
        st.markdown('</div>', unsafe_allow_html=True)

    with col_right:
        st.markdown('<div class="card-container">', unsafe_allow_html=True)
        st.subheader("🧾 结构化识别结果")
        metric_col1, metric_col2 = st.columns(2)
        with metric_col1:
            st.metric("贷款本金", preview_data.get("loan_amount") or "未识别")
            st.metric("合同起始日", preview_data.get("start_date") or "未识别")
            st.metric("数据暂计日", preview_data.get("calculation_date") or "未识别")
        with metric_col2:
            st.metric("年利率", preview_data.get("annual_interest_rate") or "未识别")
            st.metric("合同到期日", preview_data.get("end_date") or "未识别")
            st.metric("剩余本金", preview_data.get("remaining_principal") or "未识别")

        st.caption(
            f"已识别正式还款记录 {len(preview.get('repayment_records', []))} 条，"
            f"待确认记录 {len(preview.get('pending_repayment_records', []))} 条"
        )
        st.markdown('</div>', unsafe_allow_html=True)

    if st.session_state.extraction_warnings:
        for warning in st.session_state.extraction_warnings:
            display_warning(warning)

    if preview.get("repayment_records"):
        st.markdown("#### 已识别的还款记录")
        st.dataframe(preview.get("repayment_records"), hide_index=True, width='stretch')

    if preview.get("pending_repayment_records"):
        st.markdown("#### 待确认记录")
        st.dataframe(preview.get("pending_repayment_records"), hide_index=True, width='stretch')
        st.caption("待确认记录暂不直接写入正式还款表，进入下一步后可逐条指定类型。")

    with st.expander("字段来源与命中片段", expanded=False):
        render_field_source_table(preview)

    with st.expander("查看 AI 原始结构化结果", expanded=False):
        st.json(preview.get("raw_llm_data", {}))


def load_history_record_to_session(record):
    """将历史记录回填到当前会话"""
    snapshot = (record or {}).get("snapshot") or {}
    loan_data = snapshot.get("loan_data", {})
    repayment_records = snapshot.get("repayment_records", [])
    rate_adjustments = snapshot.get("rate_adjustments", [])
    calculation_result = snapshot.get("calculation_result")

    st.session_state.loan_data = loan_data
    st.session_state.repayment_records = repayment_records
    st.session_state.rate_adjustments = rate_adjustments
    st.session_state.calculation_result = calculation_result
    st.session_state.last_history_record_id = record.get("id", "")
    st.session_state.ocr_text = ""
    st.session_state.page_images = []
    st.session_state.ai_extraction_completed = False
    st.session_state.ai_extraction_raw = {}
    st.session_state.auto_fill_preview = {}
    st.session_state.field_sources = {}
    st.session_state.extraction_warnings = []
    st.session_state.pending_repayment_records = []
    st.session_state.uploaded_statement_name = ""
    st.session_state.step_completed = {
        1: True,
        2: True,
        3: calculation_result is not None
    }
    st.session_state.current_step = 4 if calculation_result else 2


def render_history_panel():
    """侧边栏历史记录入口"""
    history_store = create_history_store()
    history_records = history_store.list_records(limit=50)

    st.markdown("---")
    st.markdown("### 🕘 历史记录")

    if not history_records:
        st.caption("暂无历史记录")
        return

    record_ids = [item["id"] for item in history_records]
    default_index = 0
    if st.session_state.last_history_record_id in record_ids:
        default_index = record_ids.index(st.session_state.last_history_record_id)

    selected_record_id = st.selectbox(
        "选择历史记录",
        options=record_ids,
        index=default_index,
        format_func=lambda rid: next(
            (item["title"] for item in history_records if item["id"] == rid),
            rid
        ),
        key="selected_history_record_id"
    )

    selected_record = history_store.get_record(selected_record_id)
    if not selected_record:
        st.caption("选中的历史记录不存在")
        return

    total_amount = selected_record.get("total_amount")
    total_amount_text = f"{total_amount:,.2f} 元" if isinstance(total_amount, (int, float)) else "-"
    st.caption(f"创建时间：{selected_record.get('created_at', '-')}")
    st.caption(f"数据暂计日：{selected_record.get('calculation_date', '-')}")
    st.caption(f"债权总额：{total_amount_text}")

    col1, col2 = st.columns(2)
    with col1:
        if st.button("打开复查", key=f"open_history_{selected_record_id}", width='stretch'):
            load_history_record_to_session(selected_record)
            st.rerun()

    with col2:
        excel_bytes = history_store.get_excel_bytes(selected_record_id)
        if excel_bytes:
            st.download_button(
                label="下载Excel",
                data=excel_bytes,
                file_name=f"债权计算表_{selected_record.get('created_at', '').replace(':', '').replace(' ', '_')}.xlsx",
                mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                key=f"download_history_{selected_record_id}",
                width='stretch'
            )
        else:
            st.caption("该记录暂无Excel快照")

def can_proceed_to_step(step):
    """检查是否可以进入指定步骤"""
    if step == 1:
        return True
    elif step == 2:
        return st.session_state.step_completed.get(1, False)
    elif step == 3:
        return st.session_state.step_completed.get(2, False)
    elif step == 4:
        return st.session_state.step_completed.get(3, False)
    return False


def mark_step_completed(step):
    """标记步骤为已完成"""
    st.session_state.step_completed[step] = True


def go_to_step(step):
    """跳转到指定步骤"""
    if can_proceed_to_step(step):
        st.session_state.current_step = step


def display_section_header(title, icon="📌", subtitle=""):
    """显示章节标题"""
    st.markdown(f"""
    <div class="section-header">
        {icon} {title}
        {f'<p style="margin: 0.5rem 0 0 0; opacity: 0.9; font-size: 0.9rem;">{subtitle}</p>' if subtitle else ''}
    </div>
    """, unsafe_allow_html=True)


def display_success(message):
    """显示成功消息"""
    st.markdown(f"""
    <div class="success-card">
        ✅ {message}
    </div>
    """, unsafe_allow_html=True)


def display_warning(message):
    """显示警告消息"""
    st.markdown(f"""
    <div class="warning-card">
        ⚠️ {message}
    </div>
    """, unsafe_allow_html=True)


def display_error(message):
    """显示错误消息"""
    st.markdown(f"""
    <div class="error-card">
        ❌ {message}
    </div>
    """, unsafe_allow_html=True)


def display_info(message):
    """显示信息消息"""
    st.markdown(f"""
    <div class="info-card">
        ℹ️ {message}
    </div>
    """, unsafe_allow_html=True)


def check_ollama_connection(config):
    """检查Ollama连接状态"""
    try:
        import ollama
        base_url = config["ollama"]["base_url"]
        parsed = urlparse(base_url or "http://127.0.0.1:11434")
        host = parsed.hostname or "127.0.0.1"
        if host.lower() == "localhost":
            host = "127.0.0.1"
        netloc = host if not parsed.port else f"{host}:{parsed.port}"
        normalized_base_url = urlunparse((
            parsed.scheme or "http",
            netloc,
            parsed.path or "",
            parsed.params or "",
            parsed.query or "",
            parsed.fragment or "",
        ))
        for env_name in ("NO_PROXY", "no_proxy"):
            existing = os.environ.get(env_name, "")
            values = [item.strip() for item in existing.split(",") if item.strip()]
            for item in ("127.0.0.1", "localhost"):
                if item not in values:
                    values.append(item)
            os.environ[env_name] = ",".join(values)

        client = ollama.Client(host=normalized_base_url)
        result = client.list()

        model_list = []
        if hasattr(result, 'models'):
            model_list = [m.model for m in result.models]

        configured_model = (config.get("ollama", {}) or {}).get("model", "auto")
        preferred_models = (config.get("ollama", {}) or {}).get("preferred_models", [])
        resolved_model = None
        if configured_model and str(configured_model).lower() != "auto" and configured_model in model_list:
            resolved_model = configured_model
        else:
            for model_name in preferred_models:
                if model_name in model_list:
                    resolved_model = model_name
                    break
        if not resolved_model and model_list:
            resolved_model = model_list[0]

        return {
            "connected": True,
            "models": model_list,
            "configured_model": configured_model,
            "current_model": resolved_model
        }
    except Exception as e:
        return {
            "connected": False,
            "error": str(e)
        }


def render_step_indicator():
    """渲染步骤指示器"""
    steps = [
        (1, "录入方式", "🧭"),
        (2, "数据录入", "✏️"),
        (3, "债权计算", "🧮"),
        (4, "结果导出", "📊")
    ]

    current_step = st.session_state.current_step

    step_html = '<div class="step-indicator">'
    for step_num, step_name, step_icon in steps:
        is_active = step_num == current_step
        is_completed = st.session_state.step_completed.get(step_num, False)

        status_class = "active" if is_active else ("completed" if is_completed else "")

        step_html += f'''
        <div class="step-item {status_class}">
            <div class="step-number">{"✓" if is_completed else step_num}</div>
            <div class="step-label">{step_icon} {step_name}</div>
        </div>
        '''

        if step_num < len(steps):
            step_html += '<div style="flex:1; height:2px; background:rgba(255,255,255,0.3); margin:0 0.5rem;"></div>'

    step_html += '</div>'

    return step_html


# ============================================
# 第一步：录入方式
# ============================================
def step_upload_extract(config):
    """步骤1：录入方式选择"""
    display_section_header("录入方式", "🧭", "支持人工录入，也支持通过对账单自动填写")

    tesseract_status = check_tesseract_connection(config)
    ollama_status = check_ollama_connection(config)

    col_manual, col_auto = st.columns([0.95, 1.25], gap="large")

    with col_manual:
        st.markdown('<div class="form-container">', unsafe_allow_html=True)
        st.subheader("📝 人工录入")
        st.write("适合资料不完整、需要直接核对口径，或暂时不使用自动识别的场景。")
        st.caption("建议顺序：基础信息 -> 还款记录 -> 利率调整 -> 开始计算")

        if st.button("➡️ 开始人工录入", type="primary", width='stretch'):
            reset_auto_fill_state(clear_preview_only=False)
            st.session_state.loan_data = build_empty_loan_data()
            mark_step_completed(1)
            go_to_step(2)
            st.rerun()

        st.markdown('</div>', unsafe_allow_html=True)

    with col_auto:
        st.markdown('<div class="form-container">', unsafe_allow_html=True)
        st.subheader("🤖 上传对账单并自动填写")
        st.write("适合先用 OCR + AI 读取对账单，再把识别结果写入表单，未识别部分继续人工补录。")

        status_col1, status_col2 = st.columns(2)
        with status_col1:
            if tesseract_status["available"]:
                st.success("OCR 环境可用")
            else:
                st.error("OCR 环境不可用")
        with status_col2:
            if ollama_status.get("connected"):
                st.success(f"AI 模型就绪：{ollama_status.get('current_model')}")
                configured_model = ollama_status.get("configured_model")
                current_model = ollama_status.get("current_model")
                if configured_model and str(configured_model).lower() != "auto" and current_model != configured_model:
                    st.caption(f"已从配置模型 {configured_model} 自动回退到本机模型 {current_model}")
            else:
                st.error("AI 服务不可用")

        uploaded_file = st.file_uploader(
            "上传对账单或系统截图",
            type=["pdf", "png", "jpg", "jpeg", "webp"],
            help="支持 PDF、PNG、JPG、JPEG、WEBP。建议优先上传完整对账单。"
        )

        extract_disabled = uploaded_file is None or not tesseract_status["available"] or not ollama_status.get("connected")
        if st.button("🔍 开始自动填写", width='stretch', disabled=extract_disabled):
            with st.spinner("正在执行 OCR 与 AI 识别，请稍候..."):
                try:
                    run_auto_fill_pipeline(config, uploaded_file)
                    display_success("自动填写预览已生成，请先核对后再写入表单。")
                except Exception as exc:
                    display_error(f"自动填写失败：{exc}")

        if extract_disabled and uploaded_file is None:
            st.caption("先上传对账单后再开始自动填写。")

        st.markdown('</div>', unsafe_allow_html=True)

    if st.session_state.auto_fill_preview:
        render_auto_fill_preview()

        action_col1, action_col2, action_col3 = st.columns(3)
        with action_col1:
            if st.button("✅ 覆盖写入表单并继续", type="primary", width='stretch'):
                apply_auto_fill_preview(st.session_state.auto_fill_preview, only_missing=False)
                mark_step_completed(1)
                go_to_step(2)
                st.rerun()
        with action_col2:
            if st.button("➕ 仅写入已识别字段", width='stretch'):
                if not st.session_state.loan_data:
                    st.session_state.loan_data = build_empty_loan_data()
                apply_auto_fill_preview(st.session_state.auto_fill_preview, only_missing=True)
                mark_step_completed(1)
                go_to_step(2)
                st.rerun()
        with action_col3:
            if st.button("↩️ 放弃自动填写，改为人工录入", width='stretch'):
                reset_auto_fill_state(clear_preview_only=False)
                st.session_state.loan_data = build_empty_loan_data()
                st.rerun()


# ============================================
# 第二步：数据核对与编辑
# ============================================
def step_verify_edit(config):
    """步骤2：数据核对与编辑"""
    display_section_header("数据核对与编辑", "✏️", "检查并修正贷款信息")

    if not st.session_state.loan_data:
        display_warning("请先完成第一步")
        return

    if st.session_state.uploaded_statement_name:
        display_info(
            f"当前表单已根据文档《{st.session_state.uploaded_statement_name}》自动填写。你可以继续补齐空白字段，并确认还款记录。"
        )

    # 基本信息编辑
    st.markdown('<div class="form-container">', unsafe_allow_html=True)
    st.subheader("📋 基本信息")
    st.write("请填写或修改以下信息：")

    with st.form("loan_info_form"):
        current_date = get_current_date()
        start_date_value = parse_date(st.session_state.loan_data.get("start_date")) if st.session_state.loan_data.get("start_date") else current_date
        end_date_value = parse_date(st.session_state.loan_data.get("end_date")) if st.session_state.loan_data.get("end_date") else current_date
        calculation_date_value = parse_date(st.session_state.loan_data.get("calculation_date")) if st.session_state.loan_data.get("calculation_date") else current_date
        col1, col2, col3 = st.columns(3)

        with col1:
            loan_amount = st.text_input(
                "💰 贷款金额（元）*",
                value=str(st.session_state.loan_data.get("loan_amount", "")),
                help="合同约定的贷款总金额"
            )

            start_date = st.date_input(
                "📅 合同起始日 *",
                value=start_date_value,
                format="YYYY-MM-DD"
            )

        with col2:
            annual_rate = st.text_input(
                "📊 年利率（%）*",
                value=str(st.session_state.loan_data.get("annual_interest_rate", "")),
                help="如：4.35 表示 4.35%"
            )

            end_date = st.date_input(
                "📅 合同到期日 *",
                value=end_date_value,
                format="YYYY-MM-DD"
            )

        with col3:
            remaining_principal = st.text_input(
                "💵 剩余本金（元）*",
                value=str(st.session_state.loan_data.get("remaining_principal", "")),
                help="该值不作为自动计算起算本金，仅用于与最终一行本金余额校对"
            )

            calculation_date = st.date_input(
                "📌 数据暂计日 *",
                value=calculation_date_value,
                format="YYYY-MM-DD",
                help="作为本次数据统计和计息计算的结束日"
            )

            accrued_interest = st.text_input(
                "💸 已计利息（元）",
                value=str(st.session_state.loan_data.get("accrued_interest", "0")),
                help="仅作参考，不参与自动计息表生成"
            )

        col4, col5 = st.columns(2)

        with col4:
            penalty_interest = st.text_input(
                "⚠️ 罚息（元）",
                value=str(st.session_state.loan_data.get("penalty_interest", "0")),
                help="仅作参考，不参与自动计息表生成"
            )

        with col5:
            compound_interest = st.text_input(
                "📈 复利（元）",
                value=str(st.session_state.loan_data.get("compound_interest", "0")),
                help="该值不参与自动计算，仅用于与最终复利合计校对"
            )

        submitted = st.form_submit_button("💾 保存基本信息", type="primary", width='stretch')
        if submitted:
            st.session_state.loan_data.update({
                "loan_amount": loan_amount,
                "annual_interest_rate": annual_rate,
                "start_date": format_date(start_date),
                "end_date": format_date(end_date),
                "calculation_date": format_date(calculation_date),
                "remaining_principal": remaining_principal,
                "accrued_interest": accrued_interest,
                "penalty_interest": penalty_interest,
                "compound_interest": compound_interest
            })
            invalidate_calculation_result()
            display_success("✅ 基本信息已保存")

    st.markdown('</div>', unsafe_allow_html=True)

    # 还款记录管理
    st.markdown('<div class="form-container">', unsafe_allow_html=True)
    st.subheader("💰 还款记录")

    if st.session_state.pending_repayment_records:
        st.markdown("#### 待确认记录")
        st.caption("以下记录已从对账单中识别出日期和金额，但类型需要你确认后再写入正式还款表。")
        for idx, record in enumerate(list(st.session_state.pending_repayment_records)):
            row_col1, row_col2, row_col3, row_col4, row_col5 = st.columns([1.4, 1.2, 1.4, 1, 1])
            with row_col1:
                st.write(record.get("date", ""))
                st.caption(record.get("description", ""))
            with row_col2:
                st.write(f"{float(record.get('amount', 0)):,.2f} 元")
            with row_col3:
                selected_type = st.selectbox(
                    "待确认类型",
                    options=RepaymentType.get_options(),
                    format_func=lambda item: item[0],
                    index=next(
                        (
                            i for i, item in enumerate(RepaymentType.get_options())
                            if item[1] == record.get("suggested_type", "interest")
                        ),
                        1
                    ),
                    key=f"pending_repayment_type_{idx}",
                    label_visibility="collapsed"
                )
            with row_col4:
                if st.button("写入", key=f"approve_pending_{idx}", width='stretch'):
                    st.session_state.repayment_records.append({
                        "date": record.get("date"),
                        "type": selected_type[1],
                        "amount": record.get("amount"),
                        "description": record.get("description", ""),
                        "source": record.get("source", ""),
                    })
                    st.session_state.pending_repayment_records.pop(idx)
                    invalidate_calculation_result()
                    display_success("待确认记录已写入正式还款表。")
                    st.rerun()
            with row_col5:
                if st.button("忽略", key=f"skip_pending_{idx}", width='stretch'):
                    st.session_state.pending_repayment_records.pop(idx)
                    display_warning("该待确认记录已忽略，不会进入计算。")
                    st.rerun()

        st.markdown("---")

    if st.session_state.repayment_records:
        st.dataframe(
            st.session_state.repayment_records,
            column_config={
                "date": "还款日期",
                "type": "还款类型",
                "amount": st.column_config.NumberColumn("还款金额", format="%.2f 元")
            },
            hide_index=True
        )

        st.caption("已添加的还款记录可逐条删除")
        for idx, record in enumerate(st.session_state.repayment_records):
            try:
                amount_text = f"{float(record.get('amount', 0)):,.2f} 元"
            except (TypeError, ValueError):
                amount_text = f"{record.get('amount', 0)} 元"
            col1, col2, col3, col4 = st.columns([2, 2, 2, 1])
            with col1:
                st.write(record.get("date", ""))
            with col2:
                st.write(RepaymentType.get_display_name(record.get("type", "")))
            with col3:
                st.write(amount_text)
            with col4:
                if st.button("删除", key=f"delete_repayment_{idx}", width='stretch'):
                    deleted_record = st.session_state.repayment_records.pop(idx)
                    invalidate_calculation_result()
                    display_success(
                        f"✅ 已删除还款记录：{deleted_record.get('date', '')} - {deleted_record.get('amount', '')}元"
                    )
                    st.rerun()

    with st.expander("➕ 添加还款记录"):
        with st.form("add_repayment_form"):
            col1, col2, col3 = st.columns(3)

            with col1:
                rep_date = st.date_input("还款日期 *", value=get_current_date(), format="YYYY-MM-DD")

            with col2:
                rep_type = st.selectbox(
                    "还款类型 *",
                    options=RepaymentType.get_options(),
                    format_func=lambda x: x[0]
                )

            with col3:
                rep_amount = st.text_input("还款金额（元）*")

            submitted = st.form_submit_button("➕ 添加记录", width='stretch')
            if submitted:
                if rep_date and rep_amount:
                    st.session_state.repayment_records.append({
                        "date": format_date(rep_date),
                        "type": rep_type[1],
                        "amount": rep_amount
                    })
                    invalidate_calculation_result()
                    display_success(f"✅ 已添加还款记录：{format_date(rep_date)} - {rep_amount}元")
                    st.rerun()
                else:
                    display_error("请填写完整的还款信息")

    if st.button("🗑️ 清空还款记录"):
        st.session_state.repayment_records = []
        invalidate_calculation_result()
        display_success("✅ 还款记录已清空")
        st.rerun()

    st.markdown('</div>', unsafe_allow_html=True)

    # 利率调整记录管理
    st.markdown('<div class="form-container">', unsafe_allow_html=True)
    st.subheader("📈 利率调整记录")
    st.caption("如合同履行期间发生利率变更，可在此录入；系统会按调整日期自动分段计算。")

    if st.session_state.rate_adjustments:
        st.dataframe(
            st.session_state.rate_adjustments,
            column_config={
                "date": "调整日期",
                "rate": st.column_config.NumberColumn("新年利率（%）", format="%.4f")
            },
            hide_index=True
        )

    with st.expander("➕ 添加利率调整"):
        with st.form("add_rate_adjustment_form"):
            col1, col2 = st.columns(2)

            with col1:
                adj_date = st.date_input("调整日期 *", value=get_current_date(), format="YYYY-MM-DD")

            with col2:
                adj_rate = st.text_input("新年利率（%）*", "")

            submitted = st.form_submit_button("➕ 添加调整记录", width='stretch')
            if submitted:
                if adj_date and adj_rate:
                    st.session_state.rate_adjustments.append({
                        "date": format_date(adj_date),
                        "rate": adj_rate
                    })
                    invalidate_calculation_result()
                    display_success(f"✅ 已添加利率调整：{format_date(adj_date)} -> {adj_rate}%")
                    st.rerun()
                else:
                    display_error("请填写完整的利率调整信息")

    if st.button("🗑️ 清空利率调整记录"):
        st.session_state.rate_adjustments = []
        invalidate_calculation_result()
        display_success("✅ 利率调整记录已清空")
        st.rerun()

    st.markdown('</div>', unsafe_allow_html=True)

    # 完成按钮
    st.markdown("---")
    col1, col2 = st.columns(2)

    with col1:
        if st.button("⬅️ 返回上一步", width='stretch'):
            go_to_step(1)
            st.rerun()

    with col2:
        if st.button("✅ 完成并进入下一步", type="primary", width='stretch'):
            loan_validation = validate_loan_contract(st.session_state.loan_data)
            repayment_validation = validate_repayment_records(st.session_state.repayment_records)
            rate_adjustment_validation = validate_rate_adjustments(st.session_state.rate_adjustments)

            if (
                loan_validation.is_valid and
                repayment_validation.is_valid and
                rate_adjustment_validation.is_valid
            ):
                mark_step_completed(2)
                go_to_step(3)
                st.rerun()
            else:
                display_error("数据验证失败，请检查以下项目：")
                for error in loan_validation.get_error_messages():
                    st.error(error)
                for error in repayment_validation.get_error_messages():
                    st.error(error)
                for error in rate_adjustment_validation.get_error_messages():
                    st.error(error)


# ============================================
# 第三步：债权计算
# ============================================
def step_calculate(config):
    """步骤3：债权计算"""
    display_section_header("债权计算", "🧮", "执行按月计息的债权计算")

    # 显示计算参数
    st.markdown('<div class="card-container">', unsafe_allow_html=True)
    st.subheader("📊 计算参数预览")

    col1, col2, col3, col4, col5 = st.columns(5)

    with col1:
        st.metric("💰 贷款金额", f"{float(st.session_state.loan_data.get('loan_amount', 0)):,.0f} 元")
    with col2:
        st.metric("📊 年利率", f"{st.session_state.loan_data.get('annual_interest_rate', 0)}%")
    with col3:
        st.metric("📅 起始日期", st.session_state.loan_data.get('start_date', ''))
    with col4:
        st.metric("📅 到期日期", st.session_state.loan_data.get('end_date', ''))
    with col5:
        st.metric("📌 数据暂计日", st.session_state.loan_data.get('calculation_date', ''))

    st.markdown("---")
    st.metric("💵 剩余本金", f"{float(st.session_state.loan_data.get('remaining_principal', 0)):,.2f} 元")

    if st.session_state.repayment_records:
        st.markdown("---")
        st.subheader("💳 还款记录摘要")
        total_repayment = sum(float(r.get('amount', 0)) for r in st.session_state.repayment_records)
        col1, col2 = st.columns(2)
        with col1:
            st.metric("📝 还款笔数", len(st.session_state.repayment_records))
        with col2:
            st.metric("💵 还款总额", f"{total_repayment:,.2f} 元")

    if st.session_state.rate_adjustments:
        st.markdown("---")
        st.subheader("📈 利率调整摘要")
        st.dataframe(
            st.session_state.rate_adjustments,
            column_config={
                "date": "调整日期",
                "rate": st.column_config.NumberColumn("新年利率（%）", format="%.4f")
            },
            hide_index=True,
            width='stretch'
        )

    st.markdown('</div>', unsafe_allow_html=True)

    # 计算说明
    st.markdown('<div class="info-card">', unsafe_allow_html=True)
    st.info("""
    **📅 计息周期说明**
    - 按月计息，每月21日至次月20日为一个计息周期
    - 合同到期日之前按正常利率计息，合同到期日后一日起按上浮50%的罚息利率计息
    - 如当期内发生还款或利率调整，系统会在对应日期自动分段计算
    - 分别计算：当月利息、当月罚息、利息的复利、罚息的复利
    - 利息的复利用正常利率计收，罚息的复利用罚息利率计收
    """)
    st.markdown('</div>', unsafe_allow_html=True)

    # 执行计算
    st.markdown('<div class="card-container">', unsafe_allow_html=True)

    if st.button("🚀 开始计算", type="primary", width='stretch'):
        with st.spinner("⚡ 正在按月计算债权，请稍候..."):
            try:
                loan_data = {
                    "loan_amount": float(st.session_state.loan_data["loan_amount"]),
                    "annual_interest_rate": float(st.session_state.loan_data["annual_interest_rate"]) / 100,
                    "start_date": st.session_state.loan_data["start_date"],
                    "end_date": st.session_state.loan_data["end_date"],
                    "remaining_principal": float(st.session_state.loan_data["remaining_principal"]),
                    "accrued_interest": float(st.session_state.loan_data.get("accrued_interest", 0) or 0),
                    "penalty_interest": float(st.session_state.loan_data.get("penalty_interest", 0) or 0),
                    "compound_interest": float(st.session_state.loan_data.get("compound_interest", 0) or 0)
                }

                calculator = create_calculator(config)
                result = calculator.calculate(
                    loan_data,
                    st.session_state.repayment_records,
                    st.session_state.rate_adjustments,
                    parse_date(st.session_state.loan_data["calculation_date"])
                )

                st.session_state.calculation_result = result
                try:
                    exporter = create_excel_exporter(config)
                    excel_data = exporter.export_calculation_result(
                        result,
                        st.session_state.loan_data,
                        st.session_state.repayment_records
                    )
                    history_store = create_history_store()
                    saved_record = history_store.save_record(
                        st.session_state.loan_data,
                        st.session_state.repayment_records,
                        st.session_state.rate_adjustments,
                        result,
                        excel_bytes=excel_data
                    )
                    st.session_state.last_history_record_id = saved_record.get("id", "")
                except Exception as history_error:
                    display_warning(f"计算结果已生成，但历史记录保存失败：{history_error}")
                display_success("🎉 计算完成！")
                mark_step_completed(3)

            except Exception as e:
                display_error(f"计算失败: {str(e)}")
                import traceback
                st.error(traceback.format_exc())

    st.markdown('</div>', unsafe_allow_html=True)

    # 显示计算结果
    if st.session_state.calculation_result:
        st.markdown("---")

        display_section_header("计算结果汇总", "📈", "债权总额与各项明细")

        summary = st.session_state.calculation_result["summary"]

        # 主要指标
        col1, col2, col3, col4 = st.columns(4)

        with col1:
            st.metric("💵 剩余本金", f"{summary['remaining_principal']:,.2f} 元")
        with col2:
            st.metric("📊 利息总额", f"{summary['accrued_interest']:,.2f} 元")
        with col3:
            st.metric("⚠️ 罚息总额", f"{summary['penalty_interest']:,.2f} 元")
        with col4:
            st.metric("📈 复利总额", f"{summary['compound_interest']:,.2f} 元")

        # 总额
        st.markdown("---")
        col_total, _ = st.columns([2, 1])
        with col_total:
            st.metric(
                "🎯 债权总额",
                f"{summary['total_amount']:,.2f} 元",
                help="本金 + 利息 + 罚息 + 复利"
            )

        principal_diff = float(summary.get("remaining_principal_difference", 0) or 0)
        interest_diff = float(summary.get("accrued_interest_difference", 0) or 0)
        penalty_diff = float(summary.get("penalty_interest_difference", 0) or 0)
        compound_diff = float(summary.get("compound_difference", 0) or 0)

        if abs(principal_diff) > 0.01:
            st.warning(
                f"剩余本金校对不一致：自动计算为 {summary['remaining_principal']:,.2f} 元，"
                f"手工填写为 {summary.get('input_remaining_principal', 0):,.2f} 元，"
                f"差额 {principal_diff:,.2f} 元。"
            )
        else:
            st.success("剩余本金校对一致。")

        if abs(interest_diff) > 0.01:
            st.warning(
                f"利息校对不一致：自动计算为 {summary['accrued_interest']:,.2f} 元，"
                f"手工填写为 {summary.get('input_accrued_interest', 0):,.2f} 元，"
                f"差额 {interest_diff:,.2f} 元。"
            )
        else:
            st.success("利息校对一致。")

        if abs(penalty_diff) > 0.01:
            st.warning(
                f"罚息校对不一致：自动计算为 {summary['penalty_interest']:,.2f} 元，"
                f"手工填写为 {summary.get('input_penalty_interest', 0):,.2f} 元，"
                f"差额 {penalty_diff:,.2f} 元。"
            )
        else:
            st.success("罚息校对一致。")

        if abs(compound_diff) > 0.01:
            st.warning(
                f"复利校对不一致：自动计算为 {summary['compound_interest']:,.2f} 元，"
                f"手工填写为 {summary.get('input_compound_interest', 0):,.2f} 元，"
                f"差额 {compound_diff:,.2f} 元。"
            )
        else:
            st.success("复利校对一致。")

        st.markdown("#### 校对差额一览")
        diff_col1, diff_col2, diff_col3, diff_col4 = st.columns(4)
        with diff_col1:
            st.metric("本金差额", f"{principal_diff:,.2f} 元")
        with diff_col2:
            st.metric("利息差额", f"{interest_diff:,.2f} 元")
        with diff_col3:
            st.metric("罚息差额", f"{penalty_diff:,.2f} 元")
        with diff_col4:
            st.metric("复利差额", f"{compound_diff:,.2f} 元")

        # 明细记录
        st.markdown("---")
        st.subheader("📋 按月计息明细")

        concise_detail = []
        for item in st.session_state.calculation_result["detail"]:
            concise_detail.append({
                "序号": item.get("seq_no"),
                "计息区间": item.get("period"),
                "计息天数": item.get("days"),
                "本金余额(元)": item.get("principal_balance"),
                "当月新增利息(元)": item.get("new_interest"),
                "当月还款利息(元)": item.get("repaid_interest"),
                "利息余额(元)": item.get("interest_balance"),
                "当月新增利息的复利(元)": item.get("new_interest_compound"),
                "利息的复利余额(元)": item.get("interest_compound_balance"),
                "当月新增罚息(元)": item.get("new_penalty"),
                "当月还款罚息(元)": item.get("repaid_penalty"),
                "罚息余额(元)": item.get("penalty_balance"),
                "当月新增罚息的复利(元)": item.get("new_penalty_compound"),
                "罚息的复利余额(元)": item.get("penalty_compound_balance"),
                "备注": item.get("remark", "")
            })

        st.dataframe(
            concise_detail,
            column_config={
                "序号": st.column_config.NumberColumn("序号", width="small"),
                "计息区间": st.column_config.TextColumn("计息区间", width="medium"),
                "计息天数": st.column_config.NumberColumn("计息天数", width="small"),
                "本金余额(元)": st.column_config.NumberColumn("本金余额(元)", format="%.2f", width="medium"),
                "当月新增利息(元)": st.column_config.NumberColumn("当月新增利息(元)", format="%.2f", width="medium"),
                "当月还款利息(元)": st.column_config.NumberColumn("当月还款利息(元)", format="%.2f", width="medium"),
                "利息余额(元)": st.column_config.NumberColumn("利息余额(元)", format="%.2f", width="medium"),
                "当月新增利息的复利(元)": st.column_config.NumberColumn("当月新增利息的复利(元)", format="%.2f", width="medium"),
                "利息的复利余额(元)": st.column_config.NumberColumn("利息的复利余额(元)", format="%.2f", width="medium"),
                "当月新增罚息(元)": st.column_config.NumberColumn("当月新增罚息(元)", format="%.2f", width="medium"),
                "当月还款罚息(元)": st.column_config.NumberColumn("当月还款罚息(元)", format="%.2f", width="medium"),
                "罚息余额(元)": st.column_config.NumberColumn("罚息余额(元)", format="%.2f", width="medium"),
                "当月新增罚息的复利(元)": st.column_config.NumberColumn("当月新增罚息的复利(元)", format="%.2f", width="medium"),
                "罚息的复利余额(元)": st.column_config.NumberColumn("罚息的复利余额(元)", format="%.2f", width="medium"),
                "备注": st.column_config.TextColumn("备注", width="large")
            },
            hide_index=True,
            width='stretch',
            height=420
        )

        formula_detail = []
        for item in st.session_state.calculation_result["detail"]:
            formula_detail.append({
                "序号": item.get("seq_no"),
                "计息区间": item.get("period"),
                "本金余额计算式": item.get("principal_balance_formula", ""),
                "当月新增利息计算式": item.get("new_interest_formula", ""),
                "利息余额计算式": item.get("interest_balance_formula", ""),
                "当月新增利息复利计算式": item.get("new_interest_compound_formula", ""),
                "利息复利余额计算式": item.get("interest_compound_balance_formula", ""),
                "当月新增罚息计算式": item.get("new_penalty_formula", ""),
                "罚息余额计算式": item.get("penalty_balance_formula", ""),
                "当月新增罚息复利计算式": item.get("new_penalty_compound_formula", ""),
                "罚息复利余额计算式": item.get("penalty_compound_balance_formula", ""),
                "还款分配说明": item.get("repayment_formula", ""),
            })

        st.markdown("#### 公式核对表")
        st.dataframe(
            formula_detail,
            column_config={
                "序号": st.column_config.NumberColumn("序号", width="small"),
                "计息区间": st.column_config.TextColumn("计息区间", width="medium"),
                "本金余额计算式": st.column_config.TextColumn("本金余额计算式", width="large"),
                "当月新增利息计算式": st.column_config.TextColumn("当月新增利息计算式", width="large"),
                "利息余额计算式": st.column_config.TextColumn("利息余额计算式", width="large"),
                "当月新增利息复利计算式": st.column_config.TextColumn("当月新增利息复利计算式", width="large"),
                "利息复利余额计算式": st.column_config.TextColumn("利息复利余额计算式", width="large"),
                "当月新增罚息计算式": st.column_config.TextColumn("当月新增罚息计算式", width="large"),
                "罚息余额计算式": st.column_config.TextColumn("罚息余额计算式", width="large"),
                "当月新增罚息复利计算式": st.column_config.TextColumn("当月新增罚息复利计算式", width="large"),
                "罚息复利余额计算式": st.column_config.TextColumn("罚息复利余额计算式", width="large"),
                "还款分配说明": st.column_config.TextColumn("还款分配说明", width="large"),
            },
            hide_index=True,
            width='stretch',
            height=360
        )

    # 导航按钮
    st.markdown("---")
    col1, col2 = st.columns(2)

    with col1:
        if st.button("⬅️ 返回修改数据", width='stretch'):
            go_to_step(2)
            st.rerun()

    with col2:
        if st.session_state.step_completed.get(3, False):
            if st.button("➡️ 下一步：导出结果", type="primary", width='stretch'):
                go_to_step(4)
                st.rerun()


# ============================================
# 第四步：结果导出
# ============================================
def step_export(config):
    """步骤4：结果导出"""
    display_section_header("结果导出", "📊", "生成并下载Excel文件")

    if not st.session_state.calculation_result:
        display_warning("请先完成债权计算")
        return

    # 文件名
    default_filename = f"债权计算表_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"

    st.markdown('<div class="card-container">', unsafe_allow_html=True)
    st.subheader("📁 导出选项")

    filename = st.text_input("文件名", value=default_filename)

    if st.button("📥 生成Excel文件", type="primary", width='stretch'):
        with st.spinner("📊 正在生成Excel文件..."):
            try:
                exporter = create_excel_exporter(config)
                excel_data = exporter.export_calculation_result(
                    st.session_state.calculation_result,
                    st.session_state.loan_data,
                    st.session_state.repayment_records
                )

                st.download_button(
                    label="💾 下载Excel文件",
                    data=excel_data,
                    file_name=filename,
                    mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    width='stretch'
                )

                display_success("✨ Excel文件生成成功！请点击上方按钮下载")

            except Exception as e:
                display_error(f"导出失败: {str(e)}")

    st.markdown('</div>', unsafe_allow_html=True)

    # 数据预览
    st.markdown("---")
    st.subheader("🔍 数据预览")

    tabs = st.tabs(["📊 汇总信息", "📋 计息明细", "💰 还款记录"])

    with tabs[0]:
        st.json(st.session_state.calculation_result["summary"])

    with tabs[1]:
        st.dataframe(
            st.session_state.calculation_result["detail"],
            width='stretch'
        )

    with tabs[2]:
        if st.session_state.repayment_records:
            st.dataframe(st.session_state.repayment_records)
        else:
            st.info("无还款记录")

    # 导航按钮
    st.markdown("---")
    col1, col2 = st.columns(2)

    with col1:
        if st.button("⬅️ 返回计算结果", width='stretch'):
            go_to_step(3)
            st.rerun()

    with col2:
        if st.button("🔄 重新开始", width='stretch'):
            # 清空所有数据
            for key in list(st.session_state.keys()):
                del st.session_state[key]
            init_session_state()
            st.rerun()


# ============================================
# 贷款流水模板包装页面
# ============================================
def step_upload_extract_modern(config):
    """新版步骤1：兼容贷款流水模板。"""
    tesseract_status = check_tesseract_connection(config)
    ollama_status = check_ollama_connection(config)

    display_section_header("录入方式", "🧾", "支持人工录入，也支持通过对账单或贷款流水自动填写")
    col_manual, col_auto = st.columns([0.95, 1.25], gap="large")

    with col_manual:
        st.markdown('<div class="form-container">', unsafe_allow_html=True)
        st.subheader("📝 人工录入")
        st.write("适合资料不完整、需要直接核对业务口径，或暂时不使用自动识别的场景。")
        if st.button("➡️ 开始人工录入", type="primary", width='stretch', key="manual_entry_v2"):
            reset_auto_fill_state(clear_preview_only=False)
            st.session_state.loan_data = build_empty_loan_data()
            mark_step_completed(1)
            go_to_step(2)
            st.rerun()
        st.markdown('</div>', unsafe_allow_html=True)

    with col_auto:
        st.markdown('<div class="form-container">', unsafe_allow_html=True)
        st.subheader("🤖 上传材料并自动填写")
        st.write("支持普通对账单，也支持银行贷款流水。系统会优先按模板识别，再把识别结果写入表单。")

        status_col1, status_col2 = st.columns(2)
        with status_col1:
            if tesseract_status["available"]:
                st.success("OCR 环境可用")
            else:
                st.warning("OCR 环境不可用，仅可直接解析结构化表格")
        with status_col2:
            if ollama_status.get("connected"):
                st.success(f"AI 模型就绪：{ollama_status.get('current_model')}")
            else:
                st.info("AI 服务未连接，模板化规则识别仍可继续")

        uploaded_file = st.file_uploader(
            "上传对账单、贷款流水或系统截图",
            type=["pdf", "png", "jpg", "jpeg", "webp", "xlsx", "xls"],
            help="支持 PDF、图片、XLSX、XLS。若是银行贷款流水，优先上传结构化表格更稳定。",
            key="auto_fill_uploader_v2",
        )

        uploaded_suffix = Path(uploaded_file.name).suffix.lower() if uploaded_file else ""
        is_spreadsheet = uploaded_suffix in {".xlsx", ".xls"}
        extract_disabled = uploaded_file is None or (not is_spreadsheet and not tesseract_status["available"])

        if st.button("📳 开始自动填写", width='stretch', disabled=extract_disabled, key="auto_fill_start_v2"):
            with st.spinner("正在执行识别与结构化提取，请稍候..."):
                try:
                    run_auto_fill_pipeline(config, uploaded_file)
                    display_success("自动填写预览已生成，请先核对后再写入表单。")
                except Exception as exc:
                    display_error(f"自动填写失败：{exc}")

        st.markdown('</div>', unsafe_allow_html=True)

    if st.session_state.auto_fill_preview:
        render_auto_fill_preview()
        action_col1, action_col2, action_col3 = st.columns(3)
        with action_col1:
            if st.button("✅ 覆盖写入表单并继续", type="primary", width='stretch', key="apply_preview_full_v2"):
                apply_auto_fill_preview(st.session_state.auto_fill_preview, only_missing=False)
                mark_step_completed(1)
                go_to_step(2)
                st.rerun()
        with action_col2:
            if st.button("➡️ 仅写入已识别字段", width='stretch', key="apply_preview_partial_v2"):
                if not st.session_state.loan_data:
                    st.session_state.loan_data = build_empty_loan_data()
                apply_auto_fill_preview(st.session_state.auto_fill_preview, only_missing=True)
                mark_step_completed(1)
                go_to_step(2)
                st.rerun()
        with action_col3:
            if st.button("↩️ 放弃自动填写，改为人工录入", width='stretch', key="discard_preview_v2"):
                reset_auto_fill_state(clear_preview_only=False)
                st.session_state.loan_data = build_empty_loan_data()
                st.rerun()


def render_loan_flow_template_fields():
    """贷款流水模板的额外字段。"""
    loan_data = st.session_state.loan_data or {}
    if loan_data.get("_template_key") != FLOW_TEMPLATE_KEY:
        return

    st.markdown('<div class="form-container">', unsafe_allow_html=True)
    st.subheader("🏦 贷款流水模板补充信息")
    st.caption("这类材料通常无法直接从流水中识别利率、提前到期日、计息周期和应还本金节点，请在这里补充。")

    current_date = get_current_date()
    acceleration_date_value = (
        parse_date(loan_data.get("acceleration_date"))
        if loan_data.get("acceleration_date")
        else current_date
    )
    try:
        interest_cycle_months_value = max(int(loan_data.get("interest_cycle_months", 1) or 1), 1)
    except (TypeError, ValueError):
        interest_cycle_months_value = 1

    with st.form("flow_template_extra_form"):
        col1, col2, col3 = st.columns(3)
        with col1:
            st.text_input("模板类型", value=loan_data.get("_template_name", "贷款流水模板"), disabled=True)
        with col2:
            acceleration_date = st.date_input(
                "提前到期日",
                value=acceleration_date_value,
                format="YYYY-MM-DD",
                help="若未提前到期，可填写合同到期日。"
            )
        with col3:
            interest_cycle_months = st.number_input(
                "初始计息周期（月）",
                min_value=1,
                max_value=12,
                value=interest_cycle_months_value,
                step=1,
                help="默认按月计息填 1；如后续有补充协议变更，可在下方继续添加。"
            )

        submitted = st.form_submit_button("💾 保存基础规则", type="primary", width='stretch')
        if submitted:
            st.session_state.loan_data["acceleration_date"] = format_date(acceleration_date)
            st.session_state.loan_data["interest_cycle_months"] = str(int(interest_cycle_months))
            invalidate_calculation_result()
            display_success("贷款流水模板基础规则已保存。")

    st.markdown("#### 计息周期变更")
    st.caption("如补充协议约定从某日起改为按半年等其他周期结息，可在此录入。未录入时默认沿用上方初始计息周期。")

    interest_cycle_changes = st.session_state.loan_data.setdefault("interest_cycle_changes", [])
    if interest_cycle_changes:
        st.dataframe(interest_cycle_changes, hide_index=True, width='stretch')
        for idx, record in enumerate(list(interest_cycle_changes)):
            col1, col2, col3 = st.columns([2, 2, 1])
            with col1:
                st.write(record.get("effective_date", ""))
            with col2:
                st.write(f"{record.get('months', '')} 个月")
            with col3:
                if st.button("删除", key=f"delete_cycle_change_{idx}", width='stretch'):
                    interest_cycle_changes.pop(idx)
                    invalidate_calculation_result()
                    display_success("已删除计息周期变更记录。")
                    st.rerun()

    with st.expander("➕ 添加计息周期变更"):
        with st.form("add_interest_cycle_change_form"):
            col1, col2 = st.columns(2)
            with col1:
                cycle_change_date = st.date_input(
                    "变更生效日 *",
                    value=current_date,
                    format="YYYY-MM-DD",
                    key="cycle_change_date_input"
                )
            with col2:
                cycle_change_months = st.number_input(
                    "变更后周期（月）*",
                    min_value=1,
                    max_value=12,
                    value=6,
                    step=1,
                    key="cycle_change_months_input"
                )

            submitted = st.form_submit_button("添加周期变更", width='stretch')
            if submitted:
                interest_cycle_changes.append(
                    {
                        "effective_date": format_date(cycle_change_date),
                        "months": int(cycle_change_months),
                    }
                )
                interest_cycle_changes.sort(key=lambda item: item.get("effective_date", ""))
                invalidate_calculation_result()
                display_success("已添加计息周期变更记录。")
                st.rerun()

    st.markdown("#### 应还本金计划")
    st.caption("当银行流水没有明确反映“应还本金转逾期”节点时，可在此按合同补录每一期应还本金日期和金额。")

    principal_plan_records = st.session_state.loan_data.setdefault("principal_plan_records", [])
    if principal_plan_records:
        st.dataframe(principal_plan_records, hide_index=True, width='stretch')
        for idx, record in enumerate(list(principal_plan_records)):
            col1, col2, col3 = st.columns([2, 2, 1])
            with col1:
                st.write(record.get("date", ""))
            with col2:
                try:
                    amount_text = f"{float(record.get('amount', 0) or 0):,.2f} 元"
                except (TypeError, ValueError):
                    amount_text = f"{record.get('amount', '')} 元"
                st.write(amount_text)
            with col3:
                if st.button("删除", key=f"delete_principal_plan_{idx}", width='stretch'):
                    principal_plan_records.pop(idx)
                    invalidate_calculation_result()
                    display_success("已删除应还本金计划记录。")
                    st.rerun()

    with st.expander("➕ 添加应还本金计划"):
        with st.form("add_principal_plan_form"):
            col1, col2 = st.columns(2)
            with col1:
                principal_due_date = st.date_input(
                    "应还日期 *",
                    value=current_date,
                    format="YYYY-MM-DD",
                    key="principal_due_date_input"
                )
            with col2:
                principal_due_amount = st.number_input(
                    "应还本金金额（元）*",
                    min_value=0.0,
                    value=0.0,
                    step=1000.0,
                    key="principal_due_amount_input"
                )

            submitted = st.form_submit_button("添加应还本金计划", width='stretch')
            if submitted:
                if principal_due_amount > 0:
                    principal_plan_records.append(
                        {
                            "date": format_date(principal_due_date),
                            "amount": float(principal_due_amount),
                        }
                    )
                    principal_plan_records.sort(key=lambda item: item.get("date", ""))
                    invalidate_calculation_result()
                    display_success("已添加应还本金计划记录。")
                    st.rerun()
                display_error("应还本金金额需大于 0。")

    st.markdown('</div>', unsafe_allow_html=True)


def step_verify_edit_modern(config):
    loan_data = st.session_state.loan_data or {}
    if loan_data.get("_template_key") == FLOW_TEMPLATE_KEY:
        render_loan_flow_template_fields()
    return step_verify_edit(config)


def step_calculate_modern(config):
    """新版步骤3：按模板选择计算器。"""
    loan_data = st.session_state.loan_data or {}
    if loan_data.get("_template_key") != FLOW_TEMPLATE_KEY:
        return step_calculate(config)

    display_section_header("债权计算", "🧮", "执行贷款流水模板计算")

    st.markdown('<div class="card-container">', unsafe_allow_html=True)
    st.subheader("📊 计算参数预览")
    col1, col2, col3, col4 = st.columns(4)
    with col1:
        st.metric("贷款金额", f"{float(loan_data.get('loan_amount', 0) or 0):,.0f} 元")
    with col2:
        st.metric("起始日期", loan_data.get("start_date", ""))
    with col3:
        st.metric("合同到期日", loan_data.get("end_date", ""))
    with col4:
        st.metric("数据暂计日", loan_data.get("calculation_date", ""))

    col5, col6, col7 = st.columns(3)
    with col5:
        st.metric("起始年利率", f"{loan_data.get('annual_interest_rate', '')}%")
    with col6:
        st.metric("提前到期日", loan_data.get("acceleration_date", "") or "未填写")
    with col7:
        st.metric("流水识别还款笔数", len(st.session_state.repayment_records))
    st.markdown('</div>', unsafe_allow_html=True)

    st.markdown('<div class="card-container">', unsafe_allow_html=True)
    if st.button("🚀 开始计算", type="primary", width='stretch', key="start_flow_calc_v2"):
        with st.spinner("正在根据贷款流水生成债权计算表，请稍候..."):
            try:
                result = calculate_loan_flow_result(
                    st.session_state.loan_data,
                    st.session_state.repayment_records,
                    st.session_state.rate_adjustments,
                    parse_date(st.session_state.loan_data["calculation_date"]),
                )
                st.session_state.calculation_result = result
                exporter = create_excel_exporter(config)
                excel_data = exporter.export_calculation_result(
                    result,
                    st.session_state.loan_data,
                    st.session_state.repayment_records,
                )
                history_store = create_history_store()
                saved_record = history_store.save_record(
                    st.session_state.loan_data,
                    st.session_state.repayment_records,
                    st.session_state.rate_adjustments,
                    result,
                    excel_bytes=excel_data,
                )
                st.session_state.last_history_record_id = saved_record.get("id", "")
                mark_step_completed(3)
                display_success("贷款流水模板计算完成。")
            except Exception as exc:
                display_error(f"贷款流水模板计算失败：{exc}")
                st.exception(exc)
    st.markdown('</div>', unsafe_allow_html=True)

    if st.session_state.calculation_result:
        summary = st.session_state.calculation_result["summary"]
        display_section_header("计算结果汇总", "📋", "与模板主表口径一致")
        col1, col2, col3, col4 = st.columns(4)
        with col1:
            st.metric("本金合计", f"{summary['remaining_principal']:,.2f} 元")
        with col2:
            st.metric("尚欠利息", f"{summary['accrued_interest']:,.2f} 元")
        with col3:
            st.metric("尚欠罚息", f"{summary['penalty_interest']:,.2f} 元")
        with col4:
            st.metric("尚欠复利", f"{summary['compound_interest']:,.2f} 元")

        diff_col1, diff_col2, diff_col3 = st.columns(3)
        with diff_col1:
            st.metric("本金差额", f"{float(summary.get('remaining_principal_difference', 0) or 0):,.2f} 元")
        with diff_col2:
            st.metric("利息差额", f"{float(summary.get('accrued_interest_difference', 0) or 0):,.2f} 元")
        with diff_col3:
            st.metric("罚息/复利差额", f"{float(summary.get('penalty_interest_difference', 0) or 0) + float(summary.get('compound_difference', 0) or 0):,.2f} 元")

        detail_rows = []
        for item in st.session_state.calculation_result["detail"]:
            detail_rows.append(
                {
                    "序号": item.get("seq_no"),
                    "正常本金余额": item.get("normal_principal_balance"),
                    "起算日": item.get("start_date"),
                    "暂计至": item.get("end_date"),
                    "天数": item.get("days"),
                    "应还本金": item.get("due_principal"),
                    "已还本金": item.get("paid_principal"),
                    "累计逾期本金": item.get("overdue_principal"),
                    "正常贷款年利率": item.get("normal_rate"),
                    "应付利息": item.get("accrued_interest"),
                    "已还利息": item.get("paid_interest"),
                    "累计尚欠利息": item.get("interest_balance"),
                    "罚息年利率": item.get("penalty_rate"),
                    "应付罚息": item.get("accrued_penalty"),
                    "已还罚息": item.get("paid_penalty"),
                    "累计尚欠罚息": item.get("penalty_balance"),
                    "复利年利率": item.get("compound_rate"),
                    "应付复利": item.get("accrued_compound"),
                    "累计尚欠复利": item.get("compound_balance"),
                    "备注": item.get("remark", ""),
                }
            )

        st.markdown("#### 计息明细")
        st.dataframe(detail_rows, hide_index=True, width='stretch', height=420)

        formula_rows = []
        for item in st.session_state.calculation_result["detail"]:
            formula_rows.append(
                {
                    "序号": item.get("seq_no"),
                    "计息区间": f"{item.get('start_date', '')} 至 {item.get('end_date', '')}",
                    "利息计算式": item.get("interest_formula", ""),
                    "利息余额计算式": item.get("interest_balance_formula", ""),
                    "罚息计算式": item.get("penalty_formula", ""),
                    "罚息余额计算式": item.get("penalty_balance_formula", ""),
                    "复利计算式": item.get("compound_formula", ""),
                    "复利余额计算式": item.get("compound_balance_formula", ""),
                }
            )
        st.markdown("#### 公式核对")
        st.dataframe(formula_rows, hide_index=True, width='stretch', height=320)

    st.markdown("---")
    col1, col2 = st.columns(2)
    with col1:
        if st.button("⬅️ 返回修改数据", width='stretch', key="back_to_verify_flow_v2"):
            go_to_step(2)
            st.rerun()
    with col2:
        if st.session_state.step_completed.get(3, False):
            if st.button("➡️ 下一步：导出结果", type="primary", width='stretch', key="to_export_flow_v2"):
                go_to_step(4)
                st.rerun()


# ============================================
# 主应用
# ============================================
def main():
    """主应用入口"""
    # 加载配置
    try:
        config = load_config()
    except Exception as e:
        st.error(f"配置文件加载失败: {e}")
        st.stop()

    # 初始化Session State
    init_session_state()

    # 应用标题
    st.markdown(f"""
    <div class="main-title">
        <h1>📊 {APP_TITLE}</h1>
        <p>按月计息债权计算系统 | 支持人工录入与对账单自动填写</p>
    </div>
    """, unsafe_allow_html=True)

    # 侧边栏
    with st.sidebar:
        # 步骤指示器
        st.markdown("""
        <div style='text-align: center; padding: 1rem 0 2rem 0;'>
            <h2 style='color: white; margin: 0;'>📌</h2>
            <h3 style='color: white; margin: 0.5rem 0;'>操作流程</h3>
        </div>
        """, unsafe_allow_html=True)

        st.markdown(render_step_indicator(), unsafe_allow_html=True)
        render_history_panel()

        # 当前步骤信息
        current_step_names = {
            1: "录入方式",
            2: "数据录入",
            3: "债权计算",
            4: "结果导出"
        }

        st.markdown(f"""
        <div style='padding: 1rem; background: rgba(255,255,255,0.1); border-radius: 8px;'>
            <h4 style='color: white; margin-top: 0;'>📍 当前步骤</h4>
            <p style='color: white; font-size: 1.1rem; font-weight: 600;'>
                步骤 {st.session_state.current_step}: {current_step_names.get(st.session_state.current_step, '')}
            </p>
        </div>
        """, unsafe_allow_html=True)

    # 根据当前步骤显示对应页面
    current_step = st.session_state.current_step

    if current_step == 1:
        step_upload_extract_modern(config)
    elif current_step == 2:
        step_verify_edit_modern(config)
    elif current_step == 3:
        step_calculate_modern(config)
    elif current_step == 4:
        step_export(config)


if __name__ == "__main__":
    main()
