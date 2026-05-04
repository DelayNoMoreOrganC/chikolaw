"""
招商银行对账单处理页面
"""

import streamlit as st
import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))


def show_cmb_header():
    """显示招商银行页面头部"""
    st.markdown("""
    <div style="background: linear-gradient(135deg, #c8102e 0%, #8b0a1f 100%); padding: 1.5rem; border-radius: 10px; margin-bottom: 2rem; color: white;">
        <h2 style="margin: 0; font-size: 1.8rem;">🦁 招商银行 - 债权计算</h2>
        <p style="margin: 0.5rem 0 0 0; opacity: 0.9;">招商银行对账单处理系统</p>
    </div>
    """, unsafe_allow_html=True)


def main():
    """招商银行处理主函数"""

    # 设置页面配置
    st.set_page_config(
        page_title="招商银行 - 债权计算",
        page_icon="🦁",
        layout="wide",
        initial_sidebar_state="expanded"
    )

    # 显示招商银行样式
    st.markdown("""
    <style>
        .main {
            background-color: #f8fafc;
        }
        .stButton > button {
            background: linear-gradient(135deg, #c8102e 0%, #8b0a1f 100%);
        }
        .stButton > button:hover {
            background: linear-gradient(135deg, #a30d25 0%, #6b0818 100%);
        }
    </style>
    """, unsafe_allow_html=True)

    # 侧边栏
    with st.sidebar:
        st.markdown("### 🦁 招商银行")
        st.markdown("---")
        st.markdown("**支持的文件格式**")
        st.markdown("- PDF (扫描版/文本版)")
        st.markdown("- Excel (.xlsx, .xls)")
        st.markdown("---")
        if st.button("← 返回银行选择"):
            st.switch_page("app_bank_selector.py")

    # 页面头部
    show_cmb_header()

    # 设置session state
    if 'selected_bank' not in st.session_state:
        st.session_state.selected_bank = 'CMB'
    if 'bank_name' not in st.session_state:
        st.session_state.bank_name = '招商银行'

    st.info("👋 欢迎使用招商银行对账单处理系统！")

    # 上传界面
    st.markdown("### 📄 上传对账单文件")
    uploaded_file = st.file_uploader(
        "请上传招商银行对账单文件",
        type=['pdf', 'xlsx', 'xls'],
        help="支持PDF、Excel格式的对账单"
    )

    if uploaded_file:
        st.success(f"✅ 已上传文件: {uploaded_file.name}")
        st.info("🔍 正在识别文件内容...")

        with st.expander("查看文件信息", expanded=False):
            st.json({
                "文件名": uploaded_file.name,
                "文件类型": uploaded_file.type,
                "文件大小": f"{uploaded_file.size / 1024:.2f} KB",
                "识别银行": "招商银行 (CMB)",
                "模板版本": "v1.0"
            })


if __name__ == "__main__":
    main()
