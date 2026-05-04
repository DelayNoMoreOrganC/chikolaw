"""
债权计算系统 - 银行选择主界面
支持建设银行、招商银行、南海农商银行三个入口
"""

import streamlit as st
import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

# 导入原始app
import app as original_app


def show_bank_selection_page():
    """显示银行选择页面"""

    # 页面配置
    st.set_page_config(
        page_title="债权计算系统 - 银行选择",
        page_icon="🏦",
        layout="wide",
        initial_sidebar_state="collapsed"
    )

    # 自定义CSS
    st.markdown("""
    <style>
        .main {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }

        .main .block-container {
            padding-top: 3rem;
            max-width: 1200px;
        }

        .title-container {
            text-align: center;
            margin-bottom: 3rem;
            color: white;
        }

        .title-container h1 {
            font-size: 3.5rem;
            font-weight: 800;
            margin-bottom: 1rem;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
        }

        .title-container p {
            font-size: 1.3rem;
            opacity: 0.95;
        }

        .bank-cards-container {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 2rem;
            margin-top: 2rem;
        }

        .bank-card {
            background: white;
            border-radius: 20px;
            padding: 2.5rem 2rem;
            text-align: center;
            box-shadow: 0 10px 30px rgba(0,0,0,0.15);
            transition: all 0.3s ease;
            cursor: pointer;
            border: 3px solid transparent;
            position: relative;
            overflow: hidden;
        }

        .bank-card::before {
            content: "";
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 6px;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
        }

        .bank-card:hover {
            transform: translateY(-10px);
            box-shadow: 0 20px 40px rgba(0,0,0,0.25);
            border-color: #667eea;
        }

        .bank-icon {
            font-size: 5rem;
            margin-bottom: 1.5rem;
        }

        .bank-name {
            font-size: 1.8rem;
            font-weight: 700;
            margin-bottom: 1rem;
            color: #1f2937;
        }

        .bank-code {
            font-size: 1rem;
            color: #6b7280;
            margin-bottom: 1.5rem;
            font-weight: 500;
        }

        .bank-description {
            font-size: 0.95rem;
            color: #9ca3af;
            line-height: 1.6;
        }

        .info-box {
            background: rgba(255, 255, 255, 0.15);
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255, 255, 255, 0.3);
            border-radius: 12px;
            padding: 1.5rem;
            margin-top: 3rem;
            color: white;
        }

        .info-box h3 {
            margin-top: 0;
            font-size: 1.3rem;
            margin-bottom: 1rem;
        }

        .info-box ul {
            margin-bottom: 0;
            padding-left: 1.5rem;
        }

        .info-box li {
            margin-bottom: 0.5rem;
        }
    </style>
    """, unsafe_allow_html=True)

    # 标题
    st.markdown("""
    <div class="title-container">
        <h1>🏦 债权计算系统</h1>
        <p>请选择银行对账单类型开始处理</p>
    </div>
    """, unsafe_allow_html=True)

    # 银行卡片
    st.markdown("""
    <div class="bank-cards-container">
        <!-- 建设银行卡片 -->
        <div class="bank-card" onclick="document.getElementById('ccb-button').click();">
            <div class="bank-icon">🏛️</div>
            <div class="bank-name">建设银行</div>
            <div class="bank-code">CCB - China Construction Bank</div>
            <div class="bank-description">
                适用于建设银行企业网银对账单<br>
                支持 PDF、Excel 格式
            </div>
        </div>

        <!-- 招商银行卡片 -->
        <div class="bank-card" onclick="document.getElementById('cmb-button').click();">
            <div class="bank-icon">🦁</div>
            <div class="bank-name">招商银行</div>
            <div class="bank-code">CMB - China Merchants Bank</div>
            <div class="bank-description">
                适用于招商银行企业网银对账单<br>
                支持 PDF、Excel 格式
            </div>
        </div>

        <!-- 南海农商银行卡片 -->
        <div class="bank-card" onclick="document.getElementById('nhrcb-button').click();">
            <div class="bank-icon">🌾</div>
            <div class="bank-name">南海农商银行</div>
            <div class="bank-code">NHRCB - Nanhai Rural Commercial Bank</div>
            <div class="bank-description">
                适用于南海农商银行贷款流水对账单<br>
                支持 PDF、Excel 格式
            </div>
        </div>
    </div>
    """, unsafe_allow_html=True)

    # 隐藏按钮（用于点击触发）
    col1, col2, col3 = st.columns(3)

    with col1:
        if st.button("进入建设银行处理页面", key="ccb-button", use_container_width=True):
            st.session_state.selected_bank = "CCB"
            st.session_state.bank_name = "建设银行"
            st.switch_page("app_ccb.py")

    with col2:
        if st.button("进入招商银行处理页面", key="cmb-button", use_container_width=True):
            st.session_state.selected_bank = "CMB"
            st.session_state.bank_name = "招商银行"
            st.switch_page("app_cmb.py")

    with col3:
        if st.button("进入南海农商银行处理页面", key="nhrcb-button", use_container_width=True):
            st.session_state.selected_bank = "NHRCB"
            st.session_state.bank_name = "南海农商银行"
            st.switch_page("app_nhrcb.py")

    # 使用说明
    st.markdown("""
    <div class="info-box">
        <h3>📖 使用说明</h3>
        <ul>
            <li><strong>建设银行 (CCB)</strong>：处理建设银行企业网银对账单，支持标准版对账单格式</li>
            <li><strong>招商银行 (CMB)</strong>：处理招商银行企业网银对账单，支持企业网银对账单格式</li>
            <li><strong>南海农商银行 (NHRCB)</strong>：处理南海农商银行贷款流水对账单，支持流水台账格式</li>
        </ul>
        <p style="margin-top: 1rem; margin-bottom: 0;">
            💡 <strong>提示</strong>：如果您不确定对账单类型，可以直接上传文件，系统会自动识别银行类型
        </p>
    </div>
    """, unsafe_allow_html=True)


def main():
    """主函数"""
    show_bank_selection_page()


if __name__ == "__main__":
    main()
