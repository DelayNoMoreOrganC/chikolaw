package com.lawfirm.service;

import com.lawfirm.entity.CaseFlowTemplate;
import com.lawfirm.entity.CaseStageTodoTemplate;
import com.lawfirm.repository.CaseFlowTemplateRepository;
import com.lawfirm.repository.CaseStageTodoTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 案件流程模板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseFlowTemplateService implements CommandLineRunner {

    private final CaseFlowTemplateRepository flowTemplateRepository;
    private final CaseStageTodoTemplateRepository stageTodoTemplateRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已初始化
        if (flowTemplateRepository.count() > 0) {
            log.info("案件流程模板已初始化，跳过");
            return;
        }

        log.info("开始初始化案件流程模板...");
        initializeSystemTemplates();
        log.info("案件流程模板初始化完成");
    }

    /**
     * 初始化系统预置流程模板
     */
    @Transactional
    public void initializeSystemTemplates() {
        // 1. 民事案件流程模板
        createCivilFlowTemplate();

        // 2. 商事案件流程模板
        createCommercialFlowTemplate();

        // 3. 仲裁案件流程模板
        createArbitrationFlowTemplate();

        // 4. 刑事案件流程模板
        createCriminalFlowTemplate();

        // 5. 行政案件流程模板
        createAdministrativeFlowTemplate();
    }

    /**
     * 创建民事案件流程模板
     */
    private void createCivilFlowTemplate() {
        CaseFlowTemplate template = new CaseFlowTemplate();
        template.setTemplateName("民事案件标准流程");
        template.setCaseType("CIVIL");
        template.setIsSystem(true);
        template.setEnabled(true);
        template.setSortOrder(1);
        template.setDescription("适用于一审、二审、再审等民事案件的标准流程");
        template = flowTemplateRepository.save(template);

        // 创建各阶段待办模板
        List<StageTodo> todos = new ArrayList<>();

        // 咨询阶段
        todos.add(new StageTodo("咨询", 1, "联系客户了解案情", "与客户进行初步沟通，了解案件基本情况", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("咨询", 1, "评估案件可行性", "分析案件证据和法律依据，评估胜诉可能性", "HIGH", 2, "OWNER"));
        todos.add(new StageTodo("咨询", 1, "确定委托意向", "确认客户委托意愿，商谈代理费用", "MEDIUM", 3, "OWNER"));

        // 签约阶段
        todos.add(new StageTodo("签约", 2, "准备委托合同", "起草委托代理合同，明确双方权利义务", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("签约", 2, "签署授权委托书", "办理正式委托手续，签署授权委托书", "HIGH", 2, "OWNER"));
        todos.add(new StageTodo("签约", 2, "收取律师费", "按照约定收取首期律师费", "MEDIUM", 3, "OWNER"));

        // 起草文书
        todos.add(new StageTodo("起草文书", 3, "收集整理证据", "全面收集案件相关证据材料", "HIGH", 3, "ASSISTANT"));
        todos.add(new StageTodo("起草文书", 3, "起草起诉状", "根据案件事实和法律起草起诉状", "HIGH", 5, "OWNER"));
        todos.add(new StageTodo("起草文书", 3, "准备证据目录", "制作证据目录和证据说明", "MEDIUM", 5, "ASSISTANT"));

        // 待立案
        todos.add(new StageTodo("待立案", 4, "提交立案材料", "向法院提交起诉状及证据材料", "HIGH", 1, "ASSISTANT"));
        todos.add(new StageTodo("待立案", 4, "跟进立案进度", "联系法院立案庭，跟进立案审批", "HIGH", 3, "ASSISTANT"));

        // 已立案
        todos.add(new StageTodo("已立案", 5, "缴纳诉讼费", "按照法院通知缴纳案件受理费", "MEDIUM", 5, "OWNER"));
        todos.add(new StageTodo("已立案", 5, "确认受理通知", "领取受理通知书并告知当事人", "MEDIUM", 3, "ASSISTANT"));

        // 一审审理中
        todos.add(new StageTodo("一审审理中", 6, "研究对方答辩状", "分析对方答辩观点和证据", "HIGH", 7, "OWNER"));
        todos.add(new StageTodo("一审审理中", 6, "准备代理词", "根据庭审情况起草代理词", "HIGH", 14, "OWNER"));
        todos.add(new StageTodo("一审审理中", 6, "参加庭审", "出庭参加法庭审理", "HIGH", 21, "OWNER"));
        todos.add(new StageTodo("一审审理中", 6, "提交补充材料", "根据庭审情况提交补充证据和代理意见", "MEDIUM", 21, "ASSISTANT"));

        // 一审结案
        todos.add(new StageTodo("一审结案", 7, "领取判决书", "领取一审判决书并告知当事人", "HIGH", 3, "ASSISTANT"));
        todos.add(new StageTodo("一审结案", 7, "分析判决结果", "评估是否上诉", "HIGH", 5, "OWNER"));

        // 执行
        todos.add(new StageTodo("执行", 8, "申请强制执行", "向法院申请强制执行判决", "HIGH", 3, "OWNER"));
        todos.add(new StageTodo("执行", 8, "提供财产线索", "向法院提供被执行人财产线索", "MEDIUM", 10, "OWNER"));
        todos.add(new StageTodo("执行", 8, "跟进执行进度", "定期与执行法官沟通执行进展", "MEDIUM", 30, "ASSISTANT"));

        // 结案归档
        todos.add(new StageTodo("结案归档", 9, "整理案件材料", "整理归档案件所有材料", "MEDIUM", 3, "ASSISTANT"));
        todos.add(new StageTodo("结案归档", 9, "撰写结案报告", "编写案件结案总结报告", "LOW", 5, "OWNER"));
        todos.add(new StageTodo("结案归档", 9, "归档卷宗", "将案件材料归档保存", "LOW", 7, "ASSISTANT"));

        // 保存待办模板
        saveStageTodos(template.getId(), todos);
    }

    /**
     * 创建商事案件流程模板
     */
    private void createCommercialFlowTemplate() {
        CaseFlowTemplate template = new CaseFlowTemplate();
        template.setTemplateName("商事案件标准流程");
        template.setCaseType("COMMERCIAL");
        template.setIsSystem(true);
        template.setEnabled(true);
        template.setSortOrder(2);
        template.setDescription("适用于合同纠纷、公司纠纷等商事案件");
        template = flowTemplateRepository.save(template);

        List<StageTodo> todos = new ArrayList<>();

        // 咨询阶段
        todos.add(new StageTodo("咨询", 1, "了解商业背景", "深入了解客户商业诉求和背景", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("咨询", 1, "分析法律风险", "评估案件法律风险和商业风险", "HIGH", 2, "OWNER"));

        // 签约阶段
        todos.add(new StageTodo("签约", 2, "商定代理方案", "根据客户需求制定代理方案", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("签约", 2, "签署委托合同", "签署委托代理合同和相关文件", "HIGH", 2, "OWNER"));

        // 起草阶段
        todos.add(new StageTodo("起草", 3, "审查商业合同", "仔细审查涉案合同条款", "HIGH", 3, "OWNER"));
        todos.add(new StageTodo("起草", 3, "梳理交易流程", "还原案件涉及的交易流程", "MEDIUM", 5, "ASSISTANT"));
        todos.add(new StageTodo("起草", 3, "准备起诉材料", "起草商事起诉状和证据材料", "HIGH", 7, "OWNER"));

        // 立案阶段
        todos.add(new StageTodo("立案", 4, "确定管辖法院", "分析确定有管辖权的法院", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("立案", 4, "提交立案申请", "向法院提交立案材料", "HIGH", 3, "ASSISTANT"));

        // 一审阶段
        todos.add(new StageTodo("一审", 5, "参加庭前会议", "参加法院组织的庭前会议", "MEDIUM", 7, "OWNER"));
        todos.add(new StageTodo("一审", 5, "准备质证意见", "针对对方证据准备质证意见", "HIGH", 10, "OWNER"));
        todos.add(new StageTodo("一审", 5, "参加庭审", "出庭参加法庭审理", "HIGH", 14, "OWNER"));
        todos.add(new StageTodo("一审", 5, "提交代理词", "提交书面代理意见", "HIGH", 17, "OWNER"));

        // 调解阶段（商事案件重视调解）
        todos.add(new StageTodo("调解", 6, "评估调解方案", "评估调解的可行性和方案", "MEDIUM", 1, "OWNER"));
        todos.add(new StageTodo("调解", 6, "参与调解谈判", "参加法院组织的调解", "MEDIUM", 3, "OWNER"));
        todos.add(new StageTodo("调解", 6, "草拟调解协议", "如达成调解，起草调解协议", "HIGH", 5, "OWNER"));

        // 结案阶段
        todos.add(new StageTodo("结案", 7, "整理案卷材料", "整理归档案件材料", "MEDIUM", 3, "ASSISTANT"));
        todos.add(new StageTodo("结案", 7, "总结办案经验", "总结案件办理经验和教训", "LOW", 5, "OWNER"));

        saveStageTodos(template.getId(), todos);
    }

    /**
     * 创建仲裁案件流程模板
     */
    private void createArbitrationFlowTemplate() {
        CaseFlowTemplate template = new CaseFlowTemplate();
        template.setTemplateName("仲裁案件标准流程");
        template.setCaseType("ARBITRATION");
        template.setIsSystem(true);
        template.setEnabled(true);
        template.setSortOrder(3);
        template.setDescription("适用于国内及国际商事仲裁案件");
        template = flowTemplateRepository.save(template);

        List<StageTodo> todos = new ArrayList<>();

        // 咨询阶段
        todos.add(new StageTodo("咨询", 1, "分析仲裁条款", "分析合同中的仲裁条款效力", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("咨询", 1, "确定仲裁机构", "确定有管辖权的仲裁机构", "HIGH", 2, "OWNER"));

        // 签约阶段
        todos.add(new StageTodo("签约", 2, "签署仲裁委托", "签署仲裁代理委托合同", "HIGH", 1, "OWNER"));

        // 起草阶段
        todos.add(new StageTodo("起草", 3, "准备仲裁申请书", "起草仲裁申请书和证据材料", "HIGH", 5, "OWNER"));
        todos.add(new StageTodo("起草", 3, "选定仲裁员", "研究仲裁员名册，准备选定意见", "HIGH", 7, "OWNER"));

        // 申请仲裁阶段
        todos.add(new StageTodo("申请仲裁", 4, "提交仲裁申请", "向仲裁委提交仲裁申请", "HIGH", 1, "ASSISTANT"));
        todos.add(new StageTodo("申请仲裁", 4, "缴纳仲裁费", "按照规定缴纳仲裁费用", "HIGH", 3, "OWNER"));

        // 组庭阶段
        todos.add(new StageTodo("组庭", 5, "参与组庭程序", "参与仲裁庭组庭过程", "MEDIUM", 7, "OWNER"));
        todos.add(new StageTodo("组庭", 5, "提交管辖权异议", "如需要，提出管辖权异议", "MEDIUM", 10, "OWNER"));

        // 开庭阶段
        todos.add(new StageTodo("开庭", 6, "准备开庭材料", "准备开庭陈述和质证意见", "HIGH", 5, "OWNER"));
        todos.add(new StageTodo("开庭", 6, "参加仲裁开庭", "参加仲裁庭审", "HIGH", 7, "OWNER"));
        todos.add(new StageTodo("开庭", 6, "提交代理意见", "提交书面代理意见", "HIGH", 10, "OWNER"));

        // 裁决阶段
        todos.add(new StageTodo("裁决", 7, "跟进裁决进度", "与仲裁庭沟通裁决进展", "LOW", 30, "ASSISTANT"));
        todos.add(new StageTodo("裁决", 7, "接收裁决书", "领取仲裁裁决书", "MEDIUM", 1, "ASSISTANT"));

        // 结案阶段
        todos.add(new StageTodo("结案", 8, "分析裁决结果", "分析裁决书的裁决结果", "HIGH", 3, "OWNER"));
        todos.add(new StageTodo("结案", 8, "评估执行可能性", "评估裁决的执行可能性", "MEDIUM", 5, "OWNER"));
        todos.add(new StageTodo("结案", 8, "整理归档", "整理案件材料并归档", "LOW", 7, "ASSISTANT"));

        saveStageTodos(template.getId(), todos);
    }

    /**
     * 创建刑事案件流程模板
     */
    private void createCriminalFlowTemplate() {
        CaseFlowTemplate template = new CaseFlowTemplate();
        template.setTemplateName("刑事案件标准流程");
        template.setCaseType("CRIMINAL");
        template.setIsSystem(true);
        template.setEnabled(true);
        template.setSortOrder(4);
        template.setDescription("适用于刑事辩护案件");
        template = flowTemplateRepository.save(template);

        List<StageTodo> todos = new ArrayList<>();

        // 咨询阶段
        todos.add(new StageTodo("咨询", 1, "了解基本案情", "向家属了解案件基本情况", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("咨询", 1, "评估辩护空间", "分析案件的辩护方向和空间", "HIGH", 2, "OWNER"));

        // 签约阶段
        todos.add(new StageTodo("签约", 2, "签署辩护委托", "签署刑事辩护委托合同", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("签约", 2, "办理会见手续", "办理会见在押嫌疑人的手续", "HIGH", 2, "ASSISTANT"));

        // 会见阶段
        todos.add(new StageTodo("会见", 3, "首次会见嫌疑人", "第一次会见犯罪嫌疑人", "HIGH", 3, "OWNER"));
        todos.add(new StageTodo("会见", 3, "了解案发经过", "详细了解案件发生的经过", "HIGH", 5, "OWNER"));
        todos.add(new StageTodo("会见", 3, "提供法律咨询", "为嫌疑人提供法律咨询", "HIGH", 7, "OWNER"));

        // 审查起诉阶段
        todos.add(new StageTodo("审查起诉", 4, "查阅案卷材料", "到检察院查阅复制案卷材料", "HIGH", 10, "OWNER"));
        todos.add(new StageTodo("审查起诉", 4, "制作法律意见书", "出具法律意见书", "HIGH", 15, "OWNER"));
        todos.add(new StageTodo("审查起诉", 4, "与检察官沟通", "与承办检察官沟通辩护意见", "MEDIUM", 20, "OWNER"));

        // 一审阶段
        todos.add(new StageTodo("一审", 5, "参加庭审", "出庭参加法庭审理", "HIGH", 30, "OWNER"));
        todos.add(new StageTodo("一审", 5, "发表辩护意见", "在法庭上发表辩护意见", "HIGH", 30, "OWNER"));
        todos.add(new StageTodo("一审", 5, "提交辩护词", "提交书面辩护词", "HIGH", 35, "OWNER"));

        // 结案阶段
        todos.add(new StageTodo("结案", 6, "领取判决书", "领取法院判决书", "HIGH", 1, "ASSISTANT"));
        todos.add(new StageTodo("结案", 6, "分析判决结果", "分析判决结果，评估是否上诉", "HIGH", 3, "OWNER"));
        todos.add(new StageTodo("结案", 6, "整理案件材料", "整理归档案件材料", "MEDIUM", 5, "ASSISTANT"));

        saveStageTodos(template.getId(), todos);
    }

    /**
     * 创建行政案件流程模板
     */
    private void createAdministrativeFlowTemplate() {
        CaseFlowTemplate template = new CaseFlowTemplate();
        template.setTemplateName("行政案件标准流程");
        template.setCaseType("ADMINISTRATIVE");
        template.setIsSystem(true);
        template.setEnabled(true);
        template.setSortOrder(5);
        template.setDescription("适用于行政诉讼案件");
        template = flowTemplateRepository.save(template);

        List<StageTodo> todos = new ArrayList<>();

        // 咨询阶段
        todos.add(new StageTodo("咨询", 1, "分析行政行为", "分析被诉行政行为的合法性", "HIGH", 1, "OWNER"));
        todos.add(new StageTodo("咨询", 1, "评估诉讼时效", "确认是否在法定起诉期限内", "HIGH", 2, "OWNER"));

        // 签约阶段
        todos.add(new StageTodo("签约", 2, "签署代理合同", "签署行政诉讼代理合同", "HIGH", 1, "OWNER"));

        // 起草阶段
        todos.add(new StageTodo("起草", 3, "调取行政文书", "申请调取相关行政文书", "HIGH", 3, "OWNER"));
        todos.add(new StageTodo("起草", 3, "起草行政起诉状", "起草行政起诉状", "HIGH", 5, "OWNER"));
        todos.add(new StageTodo("起草", 3, "准备证据材料", "收集整理相关证据", "HIGH", 7, "ASSISTANT"));

        // 立案阶段
        todos.add(new StageTodo("立案", 4, "提交起诉材料", "向法院提交行政起诉状", "HIGH", 1, "ASSISTANT"));
        todos.add(new StageTodo("立案", 4, "跟进立案审查", "跟进法院立案审查进度", "HIGH", 5, "OWNER"));

        // 一审阶段
        todos.add(new StageTodo("一审", 5, "研究行政行为", "深入研究被诉行政行为", "HIGH", 10, "OWNER"));
        todos.add(new StageTodo("一审", 5, "参加庭审", "出庭参加法庭审理", "HIGH", 20, "OWNER"));
        todos.add(new StageTodo("一审", 5, "发表代理意见", "在法庭上发表代理意见", "HIGH", 20, "OWNER"));
        todos.add(new StageTodo("一审", 5, "提交代理词", "提交书面代理词", "HIGH", 25, "OWNER"));

        // 结案阶段
        todos.add(new StageTodo("结案", 6, "领取判决书", "领取行政判决书", "HIGH", 1, "ASSISTANT"));
        todos.add(new StageTodo("结案", 6, "分析判决结果", "分析判决结果，评估是否上诉", "HIGH", 3, "OWNER"));
        todos.add(new StageTodo("结案", 6, "整理案件材料", "整理归档案件材料", "MEDIUM", 5, "ASSISTANT"));

        saveStageTodos(template.getId(), todos);
    }

    /**
     * 保存阶段待办模板
     */
    private void saveStageTodos(Long flowTemplateId, List<StageTodo> todos) {
        for (StageTodo todo : todos) {
            CaseStageTodoTemplate template = new CaseStageTodoTemplate();
            template.setFlowTemplateId(flowTemplateId);
            template.setStageName(todo.stageName);
            template.setStageOrder(todo.stageOrder);
            template.setTodoTitle(todo.title);
            template.setTodoDescription(todo.description);
            template.setPriority(todo.priority);
            template.setDueDays(todo.dueDays);
            template.setAssigneeType(todo.assigneeType);
            template.setSortOrder(todos.indexOf(todo));
            stageTodoTemplateRepository.save(template);
        }
    }

    /**
     * 阶段待办内部类
     */
    private static class StageTodo {
        String stageName;
        int stageOrder;
        String title;
        String description;
        String priority;
        int dueDays;
        String assigneeType;

        StageTodo(String stageName, int stageOrder, String title, String description,
                 String priority, int dueDays, String assigneeType) {
            this.stageName = stageName;
            this.stageOrder = stageOrder;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.dueDays = dueDays;
            this.assigneeType = assigneeType;
        }
    }
}
