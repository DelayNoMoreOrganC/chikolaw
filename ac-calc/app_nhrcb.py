"""
南海农商银行对账单处理页面
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

from modules.nhrcb_data_extractor import extract_from_uploaded_file
from modules.nhrcb_calculator_engine import create_nhrcb_calculator
from modules.models.extracted_data import ExtractedData, PaymentRecord


def show_nhrcb_header():
    """显示南海农商银行页面头部"""
    st.markdown("""
    <div style="background: linear-gradient(135deg, #128057 0%, #0e5c40 100%); padding: 1.5rem; border-radius: 10px; margin-bottom: 2rem; color: white;">
        <h2 style="margin: 0; font-size: 1.8rem;">🌾 南海农商银行 - 债权计算</h2>
        <p style="margin: 0.5rem 0 0 0; opacity: 0.9;">南海农商银行贷款流水处理系统</p>
    </div>
    """, unsafe_allow_html=True)


def main():
    """南海农商银行处理主函数"""

    # 设置页面配置
    st.set_page_config(
        page_title="南海农商银行 - 债权计算",
        page_icon="🌾",
        layout="wide",
        initial_sidebar_state="expanded"
    )

    # 显示南海农商银行样式
    st.markdown("""
    <style>
        .main {
            background-color: #f8fafc;
        }
        .stButton > button {
            background: linear-gradient(135deg, #128057 0%, #0e5c40 100%);
        }
        .stButton > button:hover {
            background: linear-gradient(135deg, #0e6347 0%, #0a422f 100%);
        }
    </style>
    """, unsafe_allow_html=True)

    # 侧边栏
    with st.sidebar:
        st.markdown("### 🌾 南海农商银行")
        st.markdown("---")
        st.markdown("**支持的文件格式**")
        st.markdown("- PDF (扫描版/文本版)")
        st.markdown("- Excel (.xlsx, .xls)")
        st.markdown("---")
        st.markdown("**特色功能**")
        st.markdown("- 贷款流水识别")
        st.markdown("- 利率分段计算")
        st.markdown("- 罚息复利计算")
        st.markdown("---")
        if st.button("← 返回银行选择"):
            st.switch_page("app_bank_selector.py")

    # 页面头部
    show_nhrcb_header()

    # 设置session state
    if 'selected_bank' not in st.session_state:
        st.session_state.selected_bank = 'NHRCB'
    if 'bank_name' not in st.session_state:
        st.session_state.bank_name = '南海农商银行'

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

        # 提取数据
        with st.spinner("正在提取数据..."):
            extracted_data = extract_from_uploaded_file(uploaded_file)

        if extracted_data is None:
            st.error("❌ 数据提取失败，请检查文件格式是否正确")
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
            st.write("")  # 占位
            st.write("")  # 占位
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

                # 显示前10期
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
                    # 创建Excel文件
                    output = BytesIO()
                    with pd.ExcelWriter(output, engine='openpyxl') as writer:
                        # 汇总表
                        summary_df = pd.DataFrame([
                            {'项目': '贷款金额', '金额': result.loan_amount},
                            {'项目': '起息日', '金额': result.start_date},
                            {'项目': '到期日', '金额': result.end_date},
                            {'项目': '计算截止日', '金额': result.calculation_date},
                            {'项目': '剩余本金', '金额': result.remaining_principal},
                            {'项目': '利息总额', '金额': result.total_interest},
                            {'项目': '罚息总额', '金额': result.total_penalty},
                            {'项目': '复利总额', '金额': result.total_compound},
                            {'项目': '债权总额', '金额': result.total_debt}
                        ])
                        summary_df.to_excel(writer, sheet_name='汇总', index=False)

                        # 利息明细
                        interest_df = pd.DataFrame([
                            {
                                '期数': i + 1,
                                '计息期间': f"{d.period_start} ~ {d.period_end}",
                                '天数': d.days,
                                '本金余额': d.principal_balance,
                                '年利率': d.rate,
                                '应计利息': d.interest
                            }
                            for i, d in enumerate(result.interest_details)
                        ])
                        interest_df.to_excel(writer, sheet_name='利息明细', index=False)

                    output.seek(0)

                    st.download_button(
                        label="⬇️ 下载Excel文件",
                        data=output,
                        file_name=f"NHRCB_债权计算_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx",
                        mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        use_container_width=True
                    )


if __name__ == "__main__":
    main()
