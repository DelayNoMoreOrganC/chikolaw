"""
银行债权计算系统 - 统一主应用
支持建设银行、招商银行、南海农商银行
"""

import streamlit as st
import sys
import pandas as pd
from pathlib import Path
from datetime import datetime
from io import BytesIO

# 添加项目根目录到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

# 导入数据处理模块
from modules.nhrcb_data_extractor import extract_from_uploaded_file, NHRCBDataExtractor
from modules.nhrcb_pdf_extractor import extract_from_pdf, NHRCBPDFExtractor
from modules.nhrcb_calculator_engine import create_nhrcb_calculator
from modules.nhrcb_exporter import export_calculation_result
from modules.models.extracted_data import ExtractedData, PaymentRecord


def show_bank_selector():
    """显示银行选择页面"""

    st.markdown("""
    <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 3rem; border-radius: 15px; margin-bottom: 2rem; text-align: center; color: white;">
        <h1 style="margin: 0; font-size: 3rem;">🏦 银行债权计算系统</h1>
        <p style="margin: 1rem 0 0 0; font-size: 1.2rem; opacity: 0.9;">支持建设银行、招商银行、南海农商银行对账单处理</p>
    </div>
    """, unsafe_allow_html=True)

    col1, col2, col3 = st.columns(3)

    with col1:
        st.markdown("""
        <div style="background: linear-gradient(135deg, #1e88e5 0%, #1565c0 100%);
                   padding: 2rem; border-radius: 15px; text-align: center; color: white; height: 250px; display: flex; flex-direction: column; justify-content: center;">
            <div style="font-size: 4rem; margin-bottom: 1rem;">🏦</div>
            <h3 style="margin: 0 0 1rem 0;">建设银行</h3>
            <p style="margin: 0; opacity: 0.9;">China Construction Bank</p>
        </div>
        """, unsafe_allow_html=True)
        if st.button("进入建设银行处理页面", key="ccb-button", use_container_width=True):
            st.session_state.current_page = "ccb"

    with col2:
        st.markdown("""
        <div style="background: linear-gradient(135deg, #d32f2f 0%, #c62828 100%);
                   padding: 2rem; border-radius: 15px; text-align: center; color: white; height: 250px; display: flex; flex-direction: column; justify-content: center;">
            <div style="font-size: 4rem; margin-bottom: 1rem;">💳</div>
            <h3 style="margin: 0 0 1rem 0;">招商银行</h3>
            <p style="margin: 0; opacity: 0.9;">China Merchants Bank</p>
        </div>
        """, unsafe_allow_html=True)
        if st.button("进入招商银行处理页面", key="cmb-button", use_container_width=True):
            st.session_state.current_page = "cmb"

    with col3:
        st.markdown("""
        <div style="background: linear-gradient(135deg, #128057 0%, #0e5c40 100%);
                   padding: 2rem; border-radius: 15px; text-align: center; color: white; height: 250px; display: flex; flex-direction: column; justify-content: center;">
            <div style="font-size: 4rem; margin-bottom: 1rem;">🌾</div>
            <h3 style="margin: 0 0 1rem 0;">南海农商银行</h3>
            <p style="margin: 0; opacity: 0.9;">Nanhai Rural Commercial Bank</p>
        </div>
        """, unsafe_allow_html=True)
        if st.button("进入南海农商银行处理页面", key="nhrcb-button", use_container_width=True):
            st.session_state.current_page = "nhrcb"

    # 使用说明
    st.markdown("""
    <div style="background: #f5f5f5; padding: 1.5rem; border-radius: 10px; margin-top: 2rem;">
        <h3>📖 使用说明</h3>
        <ul>
            <li><strong>建设银行 (CCB)</strong>：处理建设银行企业网银对账单，支持标准版对账单格式</li>
            <li><strong>招商银行 (CMB)</strong>：处理招商银行企业网银对账单，支持企业网银对账单格式</li>
            <li><strong>南海农商银行 (NHRCB)</strong>：处理南海农商银行贷款流水对账单，支持流水台账格式</li>
        </ul>
    </div>
    """, unsafe_allow_html=True)


def show_ccb_page():
    """建设银行处理页面"""
    st.markdown("""
    <div style="background: linear-gradient(135deg, #1e88e5 0%, #1565c0 100%); padding: 1.5rem; border-radius: 10px; margin-bottom: 2rem; color: white;">
        <h2 style="margin: 0; font-size: 1.8rem;">🏦 建设银行 - 债权计算</h2>
        <p style="margin: 0.5rem 0 0 0; opacity: 0.9;">建设银行对账单处理系统</p>
    </div>
    """, unsafe_allow_html=True)

    st.info("⚙️ 建设银行模块正在开发中，敬请期待...")


def show_cmb_page():
    """招商银行处理页面"""
    st.markdown("""
    <div style="background: linear-gradient(135deg, #d32f2f 0%, #c62828 100%); padding: 1.5rem; border-radius: 10px; margin-bottom: 2rem; color: white;">
        <h2 style="margin: 0; font-size: 1.8rem;">💳 招商银行 - 债权计算</h2>
        <p style="margin: 0.5rem 0 0 0; opacity: 0.9;">招商银行对账单处理系统</p>
    </div>
    """, unsafe_allow_html=True)

    st.info("⚙️ 招商银行模块正在开发中，敬请期待...")


def show_nhrcb_page():
    """南海农商银行处理页面"""

    # 页面配置
    st.markdown("""
    <style>
        .stButton > button {
            background: linear-gradient(135deg, #128057 0%, #0e5c40 100%);
        }
        .stButton > button:hover {
            background: linear-gradient(135deg, #0e6347 0%, #0a422f 100%);
        }
    </style>
    """, unsafe_allow_html=True)

    # 页面头部
    st.markdown("""
    <div style="background: linear-gradient(135deg, #128057 0%, #0e5c40 100%); padding: 1.5rem; border-radius: 10px; margin-bottom: 2rem; color: white;">
        <h2 style="margin: 0; font-size: 1.8rem;">🌾 南海农商银行 - 债权计算</h2>
        <p style="margin: 0.5rem 0 0 0; opacity: 0.9;">南海农商银行贷款流水处理系统</p>
    </div>
    """, unsafe_allow_html=True)

    # 返回按钮
    if st.button("← 返回银行选择"):
        st.session_state.current_page = "selector"
        st.rerun()

    st.info("👋 欢迎使用南海农商银行对账单处理系统！")
    st.success("✨ 本系统基于现有流水台账模板优化，支持更精准的流水识别！")

    # 上传界面
    st.markdown("### 📄 上传对账单文件")
    uploaded_file = st.file_uploader(
        "请上传南海农商银行对账单文件",
        type=['pdf', 'xlsx', 'xls'],
        help="支持PDF、Excel格式的对账单"
    )

    if uploaded_file:
        st.success(f"✅ 已上传文件: {uploaded_file.name}")
        st.info("🔍 正在识别文件内容...")

        # 提取数据 - 根据文件类型选择提取器
        with st.spinner("正在提取数据..."):
            file_ext = uploaded_file.name.split('.')[-1].lower()

            if file_ext in ['pdf']:
                # PDF文件
                extracted_data = extract_from_pdf(uploaded_file)
            elif file_ext in ['xlsx', 'xls']:
                # Excel文件
                extracted_data = extract_from_uploaded_file(uploaded_file)
            else:
                st.error(f"❌ 不支持的文件格式: .{file_ext}")
                st.info("支持的格式: PDF (.pdf), Excel (.xlsx, .xls)")
                st.stop()

        if extracted_data is None:
            st.error("❌ 数据提取失败，请检查文件内容是否正确")
            st.warning("💡 提示：请确保文件包含完整的流水台账表格数据")
            st.stop()

        # 显示提取摘要
        loan_info = extracted_data['loan_info']
        repayments = extracted_data['repayments']

        st.success("✅ 数据提取成功！")

        # 显示贷款信息
        col1, col2, col3, col4 = st.columns(4)
        with col1:
            st.metric("贷款金额", f"¥{loan_info['loan_amount']:,.2f}")
        with col2:
            st.metric("起息日", loan_info['start_date'])
        with col3:
            st.metric("到期日", loan_info['end_date'])
        with col4:
            st.metric("还款次数", len(repayments))

        # 计算截止日设置
        st.markdown("---")
        col1, col2 = st.columns([2, 1])
        with col1:
            calculation_date = st.date_input(
                "计算截止日",
                value=datetime.now(),
                help="计算债权金额的截止日期"
            )
        with col2:
            st.write("")
            st.write("")
            calculate_btn = st.button("开始计算", type="primary", use_container_width=True)

        if calculate_btn or 'calculation_result' in st.session_state:
            # 执行计算
            if calculate_btn or st.session_state.get('calculation_date') != calculation_date.strftime('%Y-%m-%d'):
                with st.spinner("正在计算债权..."):
                    # 创建ExtractedData对象
                    extracted_data_obj = ExtractedData(
                        loan_amount=loan_info['loan_amount'],
                        start_date=loan_info['start_date'],
                        end_date=loan_info['end_date'],
                        calculation_date=calculation_date.strftime('%Y-%m-%d'),
                        annual_interest_rate=loan_info['annual_interest_rate']
                    )

                    # 添加还款记录
                    for repayment in repayments:
                        extracted_data_obj.repayment_records.append(
                            PaymentRecord(
                                date=repayment['date'],
                                amount=repayment['amount'],
                                type=repayment['type']
                            )
                        )

                    # 创建计算器并计算
                    calculator = create_nhrcb_calculator(extracted_data_obj)
                    result = calculator.calculate()

                    # 保存到session state
                    st.session_state.calculation_result = result
                    st.session_state.calculation_date = calculation_date.strftime('%Y-%m-%d')

            # 显示计算结果
            result = st.session_state.calculation_result

            st.markdown("---")
            st.markdown("### 📊 计算结果")

            # 总览卡片
            col1, col2, col3, col4 = st.columns(4)
            with col1:
                st.metric("剩余本金", f"¥{result.remaining_principal:,.2f}")
            with col2:
                st.metric("利息总额", f"¥{result.total_interest:,.2f}")
            with col3:
                if result.total_penalty > 0:
                    st.metric("罚息总额", f"¥{result.total_penalty:,.2f}")
                else:
                    st.metric("罚息总额", "¥0.00")
            with col4:
                if result.total_compound > 0:
                    st.metric("复利总额", f"¥{result.total_compound:,.2f}")
                else:
                    st.metric("复利总额", "¥0.00")

            # 债权总额
            st.markdown("---")
            col1, col2, col3 = st.columns([1, 2, 1])
            with col2:
                st.markdown(f"""
                    <div style="background: linear-gradient(135deg, #128057 0%, #0e5c40 100%);
                               padding: 1.5rem; border-radius: 10px; text-align: center; color: white;">
                        <h3 style="margin: 0; font-size: 1rem; opacity: 0.9;">债权总额</h3>
                        <h2 style="margin: 0.5rem 0 0 0; font-size: 2.5rem;">¥{result.total_debt:,.2f}</h2>
                    </div>
                """, unsafe_allow_html=True)

            # 利息明细
            if result.interest_details:
                st.markdown("---")
                st.markdown("### 📋 利息计算明细")

                # 显示前20期
                details_df = pd.DataFrame([
                    {
                        '期数': i + 1,
                        '计息期间': f"{d.period_start} ~ {d.period_end}",
                        '天数': d.days,
                        '本金余额': f"¥{d.principal_balance:,.2f}",
                        '年利率': f"{d.rate:.2f}%",
                        '应计利息': f"¥{d.interest:,.2f}"
                    }
                    for i, d in enumerate(result.interest_details[:20])
                ])

                st.dataframe(details_df, use_container_width=True, height=400)

                if len(result.interest_details) > 20:
                    st.info(f"ℹ️ 共 {len(result.interest_details)} 期利息，以上仅显示前20期")

            # 导出功能
            st.markdown("---")
            col1, col2, col3 = st.columns([1, 2, 1])
            with col2:
                export_excel = st.button("📥 导出Excel报表", type="secondary", use_container_width=True)

                if export_excel:
                    with st.spinner("正在生成Excel报表..."):
                        # 使用新的导出格式
                        output = export_calculation_result(result, loan_info)

                    st.download_button(
                        label="⬇️ 下载Excel文件",
                        data=output,
                        file_name=f"NHRCB_债权计算_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx",
                        mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        use_container_width=True
                    )


def main():
    """主函数"""

    # 初始化session state
    if 'current_page' not in st.session_state:
        st.session_state.current_page = 'selector'

    # 根据当前页面显示内容
    if st.session_state.current_page == 'selector':
        show_bank_selector()
    elif st.session_state.current_page == 'ccb':
        show_ccb_page()
    elif st.session_state.current_page == 'cmb':
        show_cmb_page()
    elif st.session_state.current_page == 'nhrcb':
        show_nhrcb_page()
    else:
        show_bank_selector()


if __name__ == "__main__":
    main()
