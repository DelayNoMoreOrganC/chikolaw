"""
债权精算工具 v0.3
双模式：本息债权表（银行） / 最新债权统计（判决）
"""
import streamlit as st
import os
import sys, pandas as pd
from pathlib import Path
from datetime import date, datetime, timedelta

root = Path(__file__).parent
sys.path.insert(0, str(root))

from modules.calculator import DebtCalculator
from modules.excel_exporter import ExcelExporter
from modules.bank_loan_flow import maybe_extract_loan_flow_preview
from modules.repayment_plan import (
    FREQ_LABELS, REPAYMENT_METHODS, build_segment, calc_overdue_penalty_by_plan,
    default_original_segment, generate_expected_schedule, segment_from_supplementary,
)
from utils.date_utils import format_date, parse_date
from decimal import Decimal


def safe_date(d):
    try:
        return date.fromisoformat(str(d)[:10])
    except:
        return date.today()


FREQ_MAP = {"每月": 1, "每季度": 3, "每半年": 6, "每年": 12}
TYPE_MAP = {"利息": "interest", "本金": "principal", "罚息": "penalty", "复利": "compound", "费用": "fee"}

st.set_page_config(page_title="债权精算", layout="centered")
st.markdown("# 债权精算")

# ===================== Mode Tabs =====================
tab1, tab2 = st.tabs(["📋 本息债权表（银行）", "⚖️ 最新债权统计（判决）"])

# ================================================================
# TAB 1: 银行模式（原有功能）
# ================================================================
with tab1:
    if "repay_records" not in st.session_state:
        st.session_state.repay_records = []
    if "extracted" not in st.session_state:
        st.session_state.extracted = {}
    if "plan_segments" not in st.session_state:
        st.session_state.plan_segments = []
    if "editing_segment_id" not in st.session_state:
        st.session_state.editing_segment_id = None
    if "last_plan_init" not in st.session_state:
        st.session_state.last_plan_init = None

    # ===================== Step 1: Upload =====================
    st.markdown("### 1. 上传银行流水")
    uploaded = st.file_uploader("支持 Excel（.xlsx/.xls）或 PDF", type=["xlsx", "xls", "pdf"])

    if uploaded:
        ext = Path(uploaded.name).suffix.lower()
        with st.spinner("解析中..."):
            result = maybe_extract_loan_flow_preview(
                file_bytes=uploaded.read() if ext in (".xlsx", ".xls") else None,
                filename=uploaded.name,
                text="" if ext in (".xlsx", ".xls") else "pdf_placeholder",
            )
        if result and result.get("data"):
            d = result["data"]
            st.session_state.extracted = {
                "loan_amount": d.get("loan_amount") or 0,
                "start_date": str(d.get("start_date", ""))[:10],
                "end_date": str(d.get("end_date", ""))[:10],
            }
            st.session_state.repay_records = [
                {"date": r["date"][:10], "type": r["type"], "amount": r["amount"]}
                for r in result.get("repayment_records", [])
            ]
            st.success(f"解析成功！提取到 {len(st.session_state.repay_records)} 条还款记录")
            for w in (result.get("warnings") or []):
                st.warning(w)
        else:
            st.error("未能解析此文件，请手动填写")

    # ===================== Step 2: Case Info =====================
    st.markdown("### 2. 案件信息")
    ex = st.session_state.extracted

    c1, c2 = st.columns(2)
    with c1:
        loan_amount = st.number_input("借款本金（元）", min_value=0.0, value=float(ex.get("loan_amount") or 0), step=10000.0, format="%.2f")
    with c2:
        penalty_mult = st.number_input("罚息上浮（%）", min_value=0, value=50, step=5)

    c1, c2 = st.columns(2)
    with c1:
        start_date = st.date_input("放款日", value=safe_date(ex.get("start_date", "")))
    with c2:
        end_date = st.date_input("合同到期日", value=safe_date(ex.get("end_date", "")))

    c1, c2 = st.columns(2)
    with c1:
        day_base = st.selectbox("计息基准", [360, 365], index=0)
    with c2:
        accel_date = st.date_input("加速到期日（可选）", value=None)

    cutoff = st.date_input("计算截止日（利息算到这一天）", value=date.today(),
        help="系统计算利息、罚息、复利将截止到此日。暂计日。")

    rate_mode = st.radio("利率模式", ["固定利率", "浮动利率（LPR+基点）"], horizontal=True)

    rate_adjustments = []
    if rate_mode == "固定利率":
        fixed_rate = st.number_input("年利率（%）", min_value=0.0, value=0.0, step=0.01, format="%.2f")
        if fixed_rate > 0:
            rate_adjustments.append({"date": str(start_date), "rate": fixed_rate / 100.0})
    elif rate_mode == "浮动利率（LPR+基点）":
        c1, c2, c3 = st.columns(3)
        with c1:
            base_lpr = st.number_input("放款日LPR（%）", min_value=0.0, value=3.45, step=0.05, format="%.2f",
                help="放款日前一工作日生效的一年期LPR")
        with c2:
            bp = st.number_input("加减基点", min_value=-500, value=126, step=1, format="%d",
                help="1基点=0.01%，正数为加，负数为减")
        with c3:
            adj_rule = st.selectbox("浮动规则", ["每年1月1日调整", "固定日期调整"], index=0)
        initial_rate = base_lpr + bp / 100.0
        st.caption(f"初始执行利率：{base_lpr}% + {bp}BP = **{initial_rate:.2f}%**")

        if adj_rule == "每年1月1日调整":
            years_needed = set()
            for y in range(start_date.year, cutoff.year + 1):
                years_needed.add(y)
            lpr_inputs = st.columns(len(years_needed))
            lpr_values = {}
            for i, y in enumerate(sorted(years_needed)):
                with lpr_inputs[min(i, len(lpr_inputs)-1)]:
                    default_lpr = base_lpr if y == start_date.year else 3.10
                    lpr_values[y] = st.number_input(f"{y}年LPR(%)", min_value=0.0, value=default_lpr, step=0.05, format="%.2f", key=f"lpr_{y}")
            for y in sorted(years_needed):
                adj_date = date(y, 1, 1)
                rate_val = lpr_values[y] + bp / 100.0
                rate_adjustments.append({"date": str(adj_date), "rate": round(rate_val, 4)})
        else:
            rate_adjustments.append({"date": str(start_date), "rate": round(initial_rate, 4)})
            adj_dates = st.text_area("利率调整表（每行：日期,新利率%），例如：",
                "2025-01-01, 3.80\n2026-01-01, 3.70", height=100)
            if adj_dates.strip():
                for line in adj_dates.strip().split("\n"):
                    parts = line.strip().split(",")
                    if len(parts) == 2:
                        try:
                            d = parts[0].strip()
                            r = float(parts[1].strip())
                            rate_adjustments.append({"date": d, "rate": r / 100.0})
                        except:
                            pass

    # ===================== Step 2.5: Supplementary Agreement =====================
    st.markdown("### 补充协议（可选）")
    with st.expander("如有补充协议，在此填写由AI自动解析"):
        supp_text = st.text_area("粘贴补充协议描述文本", height=100,
            placeholder='例：2025年9月14日签订补充协议：约定按半年结息，每半年末月20日为结息日，21日为付息日。归还本金时间为2025年3月21日4000元；2025年9月21日4000元。')
        if st.button("AI解析补充协议", use_container_width=True):
            if not supp_text.strip():
                st.warning("请先输入补充协议内容")
            else:
                with st.spinner("正在调用Qwen3解析..."):
                    import urllib.request, json as _json
                    prompt = f"""你是一位法律文书解析助手。请从以下补充协议描述中提取关键信息。

补充协议内容：
{supp_text.strip()[:3000]}

请以严格的JSON格式输出以下字段（只输出JSON，不要其他文字）：
- effective_date: 协议生效日期（格式 YYYY-MM-DD）
- repayment_frequency: 还本频率（每月/每季度/每半年/每年）
- repayment_amount: 每期还本金额（纯数字，不含单位）
- interest_frequency: 结息频率（每月/每季度/每半年/每年）
- first_repayment_date: 首次按新计划还本日期（格式 YYYY-MM-DD）
- notes: 其他重要条款（如利率调整、提前到期等）"""
                    payload = _json.dumps({
                        "model": "qwen3:8b", "prompt": prompt,
                        "stream": False, "temperature": 0.1, "max_tokens": 1024
                    }, ensure_ascii=False).encode('utf-8')
                    req = urllib.request.Request("http://localhost:11434/api/generate",
                        data=payload, headers={"Content-Type": "application/json"}, method="POST")
                    try:
                        with urllib.request.urlopen(req, timeout=120) as resp:
                            response = _json.loads(resp.read().decode('utf-8'))
                    except Exception as e:
                        st.error(f"AI解析失败：请检查Ollama是否运行")
                        st.info("如需AI解析功能，请确保Ollama已启动且qwen3:8b模型可用。\n您也可以手动填写还本计划。\n\nOllama安装：ollama.com\n模型安装：ollama pull qwen3:8b")
                        st.stop()
                    output = response.get('response', '')
                    import re
                    json_match = re.search(r'\{[^{}]*\}', output, re.DOTALL)
                    if json_match:
                        supp_data = _json.loads(json_match.group())
                        st.session_state.supplementary = supp_data
                        st.success("解析成功！请确认以下信息：")
                        cf1, cf2, cf3 = st.columns(3)
                        with cf1:
                            st.metric("生效日期", supp_data.get("effective_date", "-"))
                            st.metric("还本频率", supp_data.get("repayment_frequency", "-"))
                        with cf2:
                            amt = supp_data.get("repayment_amount", "0")
                            st.metric("每期还本", f"{float(amt):,.2f}元" if str(amt).replace('.','').isdigit() else amt)
                            st.metric("结息频率", supp_data.get("interest_frequency", "-"))
                        with cf3:
                            st.metric("首次新还本日", supp_data.get("first_repayment_date", "-"))
                        if st.button("确认应用补充协议", use_container_width=True):
                            st.rerun()
                    else:
                        st.error("解析失败，请手动填写")

        if "supplementary" in st.session_state and st.session_state.supplementary:
            sd = st.session_state.supplementary
            st.info(f"已解析补充协议：{sd.get('effective_date','')}起，每{sd.get('repayment_frequency','')}还本{sd.get('repayment_amount','')}元")
            c1, c2 = st.columns(2)
            with c1:
                if st.button("✅ 应用为新的还本计划段", use_container_width=True):
                    new_seg = segment_from_supplementary(sd, end_date)
                    existing = [s for s in st.session_state.plan_segments if s.get("source") != "supplementary"]
                    existing.append(new_seg)
                    st.session_state.plan_segments = existing
                    st.success("已添加补充协议计划段！请在上方检查并编辑")
                    st.rerun()
            with c2:
                if st.button("✖️ 丢弃", use_container_width=True):
                    del st.session_state.supplementary
                    st.rerun()

    # ===================== Step 3: 分段还本计划 =====================
    st.markdown("### 3. 分段还本计划")

    init_key = (loan_amount, str(start_date), str(end_date))
    if init_key != st.session_state.last_plan_init or not st.session_state.plan_segments:
        default_seg = default_original_segment(
            loan_amount=loan_amount,
            start_date=start_date,
            frequency="每季度",
            percent=5.0,
            first_repay_date=start_date,
        )
        st.session_state.plan_segments = [default_seg]
        st.session_state.last_plan_init = init_key

    segments = st.session_state.plan_segments
    for idx, seg in enumerate(segments):
        is_supp = seg.get("source") == "supplementary"
        prefix = "🔄 补充协议" if is_supp else "📄 原始计划"
        with st.container(border=True):
            cols = st.columns([4, 1])
            with cols[0]:
                st.markdown(f"**{prefix}** (生效: {seg['start_date']})")
            with cols[1]:
                if is_supp and st.button("删除", key=f"del_seg_{seg['id']}", use_container_width=True):
                    st.session_state.plan_segments = [s for s in segments if s["id"] != seg["id"]]
                    st.rerun()
            seg_method = seg["method"]
            seg_freq = seg.get("frequency", "每季度")
            seg_pct = seg.get("percent", 0)
            seg_first = seg.get("first_repayment_date", seg["start_date"])
            seg_install = seg.get("installment_count", 36)
            c1, c2 = st.columns(2)
            with c1:
                new_method = st.selectbox("还本方式", REPAYMENT_METHODS,
                    index=REPAYMENT_METHODS.index(seg_method) if seg_method in REPAYMENT_METHODS else 0,
                    key=f"seg_method_{seg['id']}", label_visibility="collapsed")
            with c2:
                new_start = st.date_input("生效日", value=parse_date(seg["start_date"]), key=f"seg_start_{seg['id']}", label_visibility="collapsed")
            if new_method == "按计划还本（自定义）":
                cc1, cc2, cc3 = st.columns(3)
                with cc1:
                    new_pct = st.number_input("每期还本比例（%）", min_value=0.0, value=seg_pct or 0.0, step=0.5, format="%.1f", key=f"seg_pct_{seg['id']}")
                with cc2:
                    new_freq = st.selectbox("还本频率", FREQ_LABELS, index=FREQ_LABELS.index(seg_freq) if seg_freq in FREQ_LABELS else 1, key=f"seg_freq_{seg['id']}")
                with cc3:
                    new_first = st.date_input("首次还本日", value=parse_date(seg_first), key=f"seg_first_{seg['id']}")
                new_amt = loan_amount * new_pct / 100
                if new_amt > 0:
                    st.caption(f"每期还本 **{new_amt:,.2f}** 元，每 **{new_freq}** 还一次")
            elif new_method == "一次性还本付息":
                st.info("到期一次性归还全部本金及利息")
                new_freq = seg_freq; new_pct = 100.0; new_first = parse_date(seg_first)
            else:
                new_install = st.number_input("还款总月数", min_value=1, max_value=600, value=seg_install, step=1, key=f"seg_install_{seg['id']}")
                new_freq = "每月"; new_pct = 0.0; new_first = parse_date(seg_first)
            if is_supp and seg.get("supplementary_text"):
                with st.expander("补充协议原文"):
                    st.text(seg["supplementary_text"])
            seg["method"] = new_method
            seg["start_date"] = str(new_start)
            seg["frequency"] = new_freq
            if new_method == "按计划还本（自定义）":
                seg["percent"] = new_pct; seg["amount"] = loan_amount * new_pct / 100; seg["first_repayment_date"] = str(new_first)
            elif new_method == "一次性还本付息":
                seg["percent"] = 100.0; seg["amount"] = loan_amount; seg["first_repayment_date"] = str(new_first)
            else:
                seg["percent"] = 0.0; seg["amount"] = 0.0; seg["installment_count"] = new_install; seg["first_repayment_date"] = str(new_first)

    if segments:
        with st.expander("📊 预期还本计划时间线", expanded=False):
            expected = generate_expected_schedule(segments, loan_amount, end_date or start_date, cutoff)
            if expected:
                total_expected = sum(e["amount"] for e in expected)
                st.caption(f"共 {len(expected)} 期，计划还本合计 {total_expected:,.2f} 元")
                df_plan = pd.DataFrame(expected)
                df_plan.columns = ["日期", "金额", "段ID", "来源"]
                st.dataframe(df_plan.style.format({"金额": "{:,.2f}"}), use_container_width=True, hide_index=True)

    plan_amount = 0
    if segments:
        first_seg = segments[0]
        if first_seg["method"] == "按计划还本（自定义）":
            plan_amount = loan_amount * first_seg.get("percent", 0) / 100
        elif first_seg["method"] in ("等额本息", "等额本金"):
            plan_amount = loan_amount / max(first_seg.get("installment_count", 36), 1)

    # ===================== Step 4: Repayment Records =====================
    st.markdown("### 4. 还款记录（已提取的流水数据）")
    if st.session_state.repay_records:
        df = pd.DataFrame(st.session_state.repay_records)
        st.dataframe(df.style.format({"amount": "{:,.2f}"}), use_container_width=True, hide_index=True)
    else:
        st.caption("暂无记录（上传流水后自动提取，也可手动添加）")

    col1, col2, col3, col4 = st.columns([2, 2, 2, 1])
    with col1: r_date = st.date_input("日期", key="r_date", value=date.today())
    with col2: r_type = st.selectbox("类型", ["利息", "本金", "罚息", "复利", "费用"], key="r_type")
    with col3: r_amount = st.number_input("金额", min_value=0.0, value=0.0, key="r_amount", format="%.2f")
    with col4:
        if st.button("添加", use_container_width=True):
            st.session_state.repay_records.append({"date": str(r_date), "type": r_type, "amount": r_amount})
            st.rerun()

    if st.session_state.repay_records and st.button("清空全部记录", use_container_width=True):
        st.session_state.repay_records = []
        st.rerun()

    # ===================== Step 5: Calculate =====================
    st.divider()
    if st.button("计算债权", type="primary", use_container_width=True):
        errs = []
        if loan_amount <= 0: errs.append("请填写借款本金")
        if not rate_adjustments or rate_adjustments[0]["rate"] <= 0:
            errs.append("请填写利率")
        if errs:
            for e in errs: st.error(e)
        else:
            with st.spinner("计算中..."):
                repayments = [{"date": r["date"], "type": TYPE_MAP.get(r["type"], r["type"]), "amount": r["amount"]}
                             for r in st.session_state.repay_records]

                segments = st.session_state.get("plan_segments", [])
                expected_schedule = generate_expected_schedule(
                    segments, loan_amount, accel_date or end_date, cutoff
                ) if segments else []
                expected_principal_schedule = [
                    {"date": str(e["date"]), "amount": e["amount"]}
                    for e in expected_schedule
                ] if expected_schedule else None

                ld = {
                    "loan_amount": loan_amount, "annual_interest_rate": rate_adjustments[0]["rate"],
                    "start_date": format_date(start_date), "end_date": format_date(accel_date or end_date),
                    "remaining_principal": 0, "accrued_interest": 0, "penalty_interest": 0, "compound_interest": 0,
                }
                calc = DebtCalculator(
                    day_count_convention=f"actual/{day_base}",
                    penalty_rate_multiplier=1 + penalty_mult / 100.0,
                )
                result = calc.calculate(
                    ld, repayments,
                    rate_adjustments=rate_adjustments,
                    calculation_date=cutoff,
                    expected_principal_schedule=expected_principal_schedule,
                )
                s = result["summary"]

                st.success(f"总债权：{s['total_amount']:,.2f} 元")
                a, b, c, d = st.columns(4)
                with a: st.metric("剩余本金", f"{s['remaining_principal']:,.2f}")
                with b: st.metric("欠付利息", f"{s['accrued_interest']:,.2f}")
                with c: st.metric("欠付罚息", f"{s['penalty_interest']:,.2f}")
                with d: st.metric("欠付复利", f"{s['compound_interest']:,.2f}")

                if s['penalty_interest'] > 0:
                    st.caption("罚息基于分段还本计划的预期还本日期计算，已累加至当期欠付罚息")

                rows = []
                for dd in result["detail"]:
                    rows.append({"期": dd["seq_no"], "起": dd["start_date"], "止": dd["end_date"],
                        "天数": dd["days"], "本金余额": dd["principal_balance"],
                        "新增利息": dd["new_interest"], "新增复利": dd["new_interest_compound"],
                        "新增罚息": dd["new_penalty"], "还本": dd["principal_repaid"],
                        "还息": dd["repaid_interest"], "余额利息": dd["interest_balance"],
                        "余额罚息": dd["penalty_balance"]})
                st.dataframe(pd.DataFrame(rows), use_container_width=True, hide_index=True,
                    column_config={col: st.column_config.NumberColumn(col, format="%.2f")
                                  for col in pd.DataFrame(rows).columns if col not in ["期", "起", "止", "天数"]})

                excel_bytes = ExcelExporter().export_calculation_result(result, ld, repayments)
                out_name = f"债权计算表_{datetime.now().strftime('%Y%m%d')}.xlsx"
                export_dir = st.session_state.get("export_dir", "")
                if export_dir and os.path.isdir(export_dir):
                    with open(os.path.join(export_dir, out_name), "wb") as f:
                        f.write(excel_bytes)
                    st.success(f"已保存到: {export_dir}")
                st.download_button("导出Excel",
                    data=excel_bytes, file_name=out_name,
                    mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    use_container_width=True)

    if "export_dir" not in st.session_state:
        st.session_state.export_dir = ""
    col1, col2 = st.columns([3, 1])
    with col1:
        st.session_state.export_dir = st.text_input("导出目录（留空为浏览器下载）",
            value=st.session_state.export_dir, placeholder="例如：C:\\Users\\MyDoc\\债权计算表")
    with col2:
        st.caption(" "); st.caption("输入完整路径后点计算，Excel将保存到该目录")

    st.divider()
    st.caption("债权精算 v0.3 | 银行模式")


# ================================================================
# TAB 2: 判决模式（新增）
# ================================================================
with tab2:
    st.markdown("### ⚖️ 最新债权统计（判决）")
    st.caption("从判决文书确认的余额起算，统计至最新欠款日")

    # ---------- session state ----------
    if "jdg_principal" not in st.session_state:
        st.session_state.jdg_principal = 0.0
    if "jdg_interest" not in st.session_state:
        st.session_state.jdg_interest = 0.0
    if "jdg_penalty" not in st.session_state:
        st.session_state.jdg_penalty = 0.0
    if "jdg_compound" not in st.session_state:
        st.session_state.jdg_compound = 0.0
    if "jdg_judgment_date" not in st.session_state:
        st.session_state.jdg_judgment_date = date.today()
    if "jdg_effective_date" not in st.session_state:
        st.session_state.jdg_effective_date = None
    if "jdg_performance_days" not in st.session_state:
        st.session_state.jdg_performance_days = 10
    if "jdg_cutoff" not in st.session_state:
        st.session_state.jdg_cutoff = date.today()
    if "jdg_post_rate" not in st.session_state:
        st.session_state.jdg_post_rate = 0.0
    if "jdg_penalty_mult" not in st.session_state:
        st.session_state.jdg_penalty_mult = 50
    if "jdg_result" not in st.session_state:
        st.session_state.jdg_result = None
    if "jdg_pdf_processed" not in st.session_state:
        st.session_state.jdg_pdf_processed = False
    if "jdg_lawyer_fee" not in st.session_state:
        st.session_state.jdg_lawyer_fee = 0.0
    if "jdg_court_fee" not in st.session_state:
        st.session_state.jdg_court_fee = 0.0
    if "jdg_preservation_fee" not in st.session_state:
        st.session_state.jdg_preservation_fee = 0.0

    # ===================== Upload PDF =====================
    jdg_pdf = st.file_uploader("上传判决书PDF（自动提取关键要素）", type=["pdf"], key="jdg_pdf")
    if jdg_pdf and not st.session_state.get("jdg_pdf_processed"):
        st.session_state.jdg_pdf_processed = True
        with st.spinner("正在解析判决书..."):
            from modules.judgment_ocr import extract_text_from_pdf, extract_judgment_data, try_ai_extraction
            pdf_text = extract_text_from_pdf(jdg_pdf.read())
            if pdf_text and len(pdf_text) > 50:
                data = extract_judgment_data(pdf_text)
                if not data.get("principal"):
                    ai_data = try_ai_extraction(pdf_text)
                    if ai_data and ai_data.get("principal"):
                        data.update({k: v for k, v in ai_data.items() if v is not None})
                if data.get("principal"):
                    st.session_state.jdg_principal = data["principal"]
                if data.get("interest"):
                    st.session_state.jdg_interest = data["interest"]
                if data.get("penalty"):
                    st.session_state.jdg_penalty = data["penalty"]
                if data.get("compound"):
                    st.session_state.jdg_compound = data["compound"]
                if data.get("rate"):
                    st.session_state.jdg_post_rate = data["rate"]
                if data.get("judgment_date"):
                    try:
                        st.session_state.jdg_judgment_date = date.fromisoformat(data["judgment_date"])
                    except:
                        pass
                if data.get("lawyer_fee"):
                    st.session_state.jdg_lawyer_fee = data["lawyer_fee"]
                if data.get("court_fee"):
                    st.session_state.jdg_court_fee = data["court_fee"]
                if data.get("performance_days") and data["performance_days"] != 10:
                    st.session_state.jdg_performance_days = data["performance_days"]
                found = sum([1 for k in ["principal","interest","penalty","compound"] if data.get(k)])
                st.success(f"PDF解析完成，已提取 {found} 项数据，请确认并修改")
            else:
                st.warning("未能从PDF提取文字，请手动填写")

    # ===================== Step 1: Judgment Info =====================
    st.markdown("#### 1. 判决确认金额")



    c1, c2 = st.columns(2)
    with c1:
        st.session_state.jdg_principal = st.number_input(
            "判决确认本金余额（元）", min_value=0.0,
            value=st.session_state.jdg_principal, step=10000.0, format="%.2f")
    with c2:
        st.session_state.jdg_interest = st.number_input(
            "判决确认欠付利息（元）", min_value=0.0,
            value=st.session_state.jdg_interest, step=1000.0, format="%.2f")

    c1, c2 = st.columns(2)
    with c1:
        st.session_state.jdg_penalty = st.number_input(
            "判决确认欠付罚息（元）", min_value=0.0,
            value=st.session_state.jdg_penalty, step=1000.0, format="%.2f")
    with c2:
        st.session_state.jdg_compound = st.number_input(
            "判决确认欠付复利（元）", min_value=0.0,
            value=st.session_state.jdg_compound, step=1000.0, format="%.2f")

    st.markdown("##### 其他费用（自动提取后可修改）")
    c1, c2, c3 = st.columns(3)
    with c1:
        st.session_state.jdg_lawyer_fee = st.number_input(
            "律师费（元）", min_value=0.0,
            value=st.session_state.jdg_lawyer_fee, step=500.0, format="%.2f")
    with c2:
        st.session_state.jdg_court_fee = st.number_input(
            "案件受理费（元）", min_value=0.0,
            value=st.session_state.jdg_court_fee, step=500.0, format="%.2f")
    with c3:
        st.session_state.jdg_preservation_fee = st.number_input(
            "保全费（元）", min_value=0.0,
            value=st.session_state.jdg_preservation_fee, step=500.0, format="%.2f")

    # ===================== Step 2: Calculation Parameters =====================
    st.markdown("#### 2. 计算参数")

    c1, c2 = st.columns(2)
    with c1:
        st.session_state.jdg_judgment_date = st.date_input(
            "判决日", value=st.session_state.jdg_judgment_date,
            help="判决文书作出的日期，作为新一轮计息起算点")
    with c2:
        st.session_state.jdg_cutoff = st.date_input(
            "计算截止日（最新欠款日）", value=st.session_state.jdg_cutoff)

    st.markdown("##### 判决后利率")
    c1, c2 = st.columns(2)
    with c1:
        st.session_state.jdg_post_rate = st.number_input(
            "判决后年利率（%）", min_value=0.0,
            value=st.session_state.jdg_post_rate, step=0.01, format="%.4f",
            help="通常为合同约定利率或LPR，如3.45表示3.45%")
    with c2:
        st.session_state.jdg_penalty_mult = st.number_input(
            "罚息上浮比例（%）", min_value=0, value=st.session_state.jdg_penalty_mult, step=5)

    st.caption("判决后罚息/复利按 **365日/年** 为计息基数（银行模式为360日/年）")

    # ===================== 迟延履行利息 =====================
    with st.expander("迟延履行利息（可选）"):
        st.caption("""
        根据《民事诉讼法》第264条，被执行人未按判决指定的期间履行给付金钱义务的，应当加倍支付迟延履行期间的债务利息。
        **计算方式：** 日万分之一点七五（0.0175%/日），以判决生效日+履行期间届满后起算
        """)

        has_effective = st.checkbox("录入判决生效日", value=st.session_state.jdg_effective_date is not None,
            help="勾选后录入判决生效日和履行期间，系统将自动计算迟延履行利息")

        if has_effective:
            st.session_state.jdg_effective_date = st.date_input(
                "判决生效日",
                value=st.session_state.jdg_effective_date or st.session_state.jdg_judgment_date,
                help="通常为判决送达之日或判决书载明的生效日期")
            st.session_state.jdg_performance_days = st.selectbox(
                "履行期间", [3, 5, 10],
                index=[3, 5, 10].index(st.session_state.jdg_performance_days)
                    if st.session_state.jdg_performance_days in [3, 5, 10] else 2,
                help="判决指定的履行期间")
        else:
            st.session_state.jdg_effective_date = None

    # ===================== Step 3: Calculate =====================
    st.divider()
    if st.button("计算最新债权", type="primary", use_container_width=True):
        errs = []
        if st.session_state.jdg_principal <= 0:
            errs.append("请填写判决确认的本金余额")
        if st.session_state.jdg_post_rate <= 0:
            errs.append("请填写判决后的年利率")
        if errs:
            for e in errs: st.error(e)
        else:
            with st.spinner("计算中..."):
                jdg_date = st.session_state.jdg_judgment_date
                cutoff = st.session_state.jdg_cutoff
                rate = st.session_state.jdg_post_rate / 100.0
                pen_mult = 1 + st.session_state.jdg_penalty_mult / 100.0
                penalty_rate = rate * pen_mult

                # 用365天基数（判决模式专用）
                day_base_jdg = 365

                # 从判决日到截止日的天数
                total_days = (cutoff - jdg_date).days

                # 利息（按判决后利率，365天基数）
                interest_new = st.session_state.jdg_principal * rate * total_days / day_base_jdg if total_days > 0 else 0

                # 罚息（本金全额视为逾期，365天基数）
                penalty_new = st.session_state.jdg_principal * penalty_rate * total_days / day_base_jdg if total_days > 0 else 0

                # 复利（对判决确认的欠息 + 新欠息，365天基数）
                compound_base = st.session_state.jdg_interest + interest_new
                compound_new = compound_base * rate * total_days / day_base_jdg if total_days > 0 else 0

                # 迟延履行利息
                delay_interest = 0.0
                delay_start = None
                delay_days = 0
                if st.session_state.jdg_effective_date:
                    perf_end = st.session_state.jdg_effective_date + timedelta(days=st.session_state.jdg_performance_days)
                    if cutoff > perf_end:
                        delay_start = perf_end + timedelta(days=1)
                        delay_days = (cutoff - delay_start).days + 1
                        delay_interest = st.session_state.jdg_principal * 0.000175 * delay_days

                # 汇总
                total_principal = st.session_state.jdg_principal
                total_interest = st.session_state.jdg_interest + interest_new
                total_penalty = st.session_state.jdg_penalty + penalty_new
                total_compound = st.session_state.jdg_compound + compound_new
                grand_total = total_principal + total_interest + total_penalty + total_compound + delay_interest

                st.session_state.jdg_result = {
                    "principal_original": st.session_state.jdg_principal,
                    "interest_original": st.session_state.jdg_interest,
                    "penalty_original": st.session_state.jdg_penalty,
                    "compound_original": st.session_state.jdg_compound,
                    "lawyer_fee": st.session_state.jdg_lawyer_fee,
                    "court_fee": st.session_state.jdg_court_fee,
                    "preservation_fee": st.session_state.jdg_preservation_fee,
                    "interest_new": interest_new,
                    "penalty_new": penalty_new,
                    "compound_new": compound_new,
                    "delay_interest": delay_interest,
                    "delay_days": delay_days,
                    "penalty_rate": rate * pen_mult,  # 罚息利率（年化）
                    "judgment_date": str(jdg_date),
                    "effective_date": str(st.session_state.jdg_effective_date) if st.session_state.jdg_effective_date else "",
                    "performance_days": st.session_state.jdg_performance_days,
                    "total_principal": total_principal,
                    "total_interest": total_interest,
                    "total_penalty": total_penalty,
                    "total_compound": total_compound,
                    "grand_total": grand_total,
                    "total_days": total_days,
                    "cutoff": str(cutoff),
                    "post_rate": st.session_state.jdg_post_rate,
                    "day_base": day_base_jdg,
                }

                # ===== 展示结果 =====
                r = st.session_state.jdg_result

                st.success(f"最新总债权：{r['grand_total']:,.2f} 元")

                # 判决确认部分
                st.markdown("##### 判决确认金额（截至判决日）")
                col_a, col_b, col_c, col_d = st.columns(4)
                with col_a: st.metric("本金", f"{r['principal_original']:,.2f}")
                with col_b: st.metric("利息", f"{r['interest_original']:,.2f}")
                with col_c: st.metric("罚息", f"{r['penalty_original']:,.2f}")
                with col_d: st.metric("复利", f"{r['compound_original']:,.2f}")

                # 新增部分
                st.markdown(f"##### 新增部分（判决日{r['cutoff']} → 截止日，{total_days}天，按365日/年）")
                col_a, col_b, col_c = st.columns(3)
                with col_a: st.metric("新增利息", f"{r['interest_new']:,.2f}", delta=f"本金×{r['post_rate']}%×{total_days}/365")
                with col_b: st.metric("新增罚息", f"{r['penalty_new']:,.2f}", delta=f"本金×{r['post_rate']*pen_mult:.2f}%×{total_days}/365")
                with col_c: st.metric("新增复利", f"{r['compound_new']:,.2f}", delta="欠息余额×利率×天数/365")

                if delay_interest > 0:
                    st.markdown("##### 迟延履行利息")
                    st.metric("迟延履行利息", f"{delay_interest:,.2f}",
                        delta=f"本金 × 0.0175%/日 × {delay_days}天（{delay_start}起算）")

                # 债权汇总表
                st.markdown("##### 债权汇总")
                cols = st.columns(2)
                with cols[0]:
                    st.markdown("**项目**")
                    st.markdown(f"本金")
                    st.markdown(f"利息")
                    st.markdown(f"罚息")
                    st.markdown(f"复利")
                    if r['lawyer_fee'] > 0: st.markdown(f"律师费")
                    if r['court_fee'] > 0: st.markdown(f"案件受理费")
                    if r['preservation_fee'] > 0: st.markdown(f"保全费")
                    if r['delay_interest'] > 0: st.markdown(f"迟延履行利息")
                    st.markdown("---")
                    st.markdown("**最新总债权**")
                with cols[1]:
                    st.markdown("**金额（元）**")
                    st.markdown(f"{r['total_principal']:,.2f}")
                    st.markdown(f"{r['total_interest']:,.2f}")
                    st.markdown(f"{r['total_penalty']:,.2f}")
                    st.markdown(f"{r['total_compound']:,.2f}")
                    if r['lawyer_fee'] > 0: st.markdown(f"{r['lawyer_fee']:,.2f}")
                    if r['court_fee'] > 0: st.markdown(f"{r['court_fee']:,.2f}")
                    if r['preservation_fee'] > 0: st.markdown(f"{r['preservation_fee']:,.2f}")
                    if r['delay_interest'] > 0: st.markdown(f"{r['delay_interest']:,.2f}")
                    st.markdown("---")
                    grand = r['grand_total']
                    st.markdown(f"**{grand:,.2f}**")

    # ===================== Export =====================
    if st.session_state.jdg_result:
        st.divider()
        r = st.session_state.jdg_result
        # 创建完整的Excel输出
        from modules.excel_exporter import ExcelExporter as _Exporter
        exporter = _Exporter()
        excel_bytes = exporter.export_judgment_result(r)
        st.download_button(
            "导出Excel",
            data=excel_bytes,
            file_name=f"判决债权统计_{datetime.now().strftime('%Y%m%d')}.xlsx",
            mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            use_container_width=True,
        )

    st.divider()
    st.caption("债权精算 v0.3 | 判决模式 | 计息基数365日/年")
