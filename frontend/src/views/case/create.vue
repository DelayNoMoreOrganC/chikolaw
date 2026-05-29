<template>
  <div class="case-create">
    <PageHeader title="新建案件" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button @click="handleSaveDraft">保存草稿</el-button>
        <el-button
          type="success"
          :loading="filing"
          :disabled="establishmentDisabled"
          @click="handleFiling"
        >
          确认建案
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交案件
        </el-button>
        <el-button type="warning" :loading="approving" @click="handleSubmitApproval">
          提交审批
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="filingDraftBanner"
      class="filing-draft-banner"
      :type="filingBannerType"
      :closable="false"
      show-icon
      :title="filingDraftBanner"
    />

    <div class="create-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="case-form"
      >
        <!-- A. 基本信息 -->
        <div class="form-section">
          <div class="section-header">
            <h3>A. 基本信息</h3>
            <div>
              <el-button type="warning" size="small" @click="handleConflictCheck">
                <el-icon><Warning /></el-icon>
                利益冲突审查
              </el-button>
              <el-button type="primary" size="small" @click="handleAIFill">
                <el-icon><MagicStick /></el-icon>
                文书智能识别填充
              </el-button>
            </div>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="案件类型" prop="caseType">
                <el-select v-model="formData.caseType" placeholder="请选择立案大类" @change="handleCaseTypeChange">
                  <el-option-group
                    v-for="group in adminCaseTypeGroups"
                    :key="group.label"
                    :label="group.label"
                  >
                    <el-option
                      v-for="opt in group.options"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-option-group>
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件程序" prop="procedure">
                <el-select v-model="formData.procedure" placeholder="请选择案件程序">
                  <el-option label="一审" value="FIRST_INSTANCE" />
                  <el-option label="二审" value="SECOND_INSTANCE" />
                  <el-option label="再审" value="RETRIAL" />
                  <el-option label="执行" value="EXECUTION" />
                  <el-option label="其他" value="OTHER" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件名称" prop="caseName">
                <el-input
                  v-model="formData.caseName"
                  placeholder="为空时根据当事人生成"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="合同号/案件编号" prop="caseNumber">
                <el-input
                  v-model="formData.caseNumber"
                  :placeholder="caseNumberTemplateHint"
                  @blur="handleCheckDuplicate"
                />
                <div class="field-hint">审批通过后由行政部自动生成；可留空</div>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案由" prop="caseReason">
                <el-select
                  v-model="formData.caseReason"
                  filterable
                  allow-create
                  placeholder="请选择或输入案由"
                >
                  <el-option
                    v-for="reason in caseReasonList"
                    :key="reason"
                    :label="reason"
                    :value="reason"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('businessType')">
              <el-form-item label="业务类型" prop="businessType">
                <el-select
                  v-model="formData.businessType"
                  placeholder="请选择业务类型（单选）"
                  filterable
                >
                  <el-option
                    v-for="type in businessTypeOptions"
                    :key="type"
                    :label="type"
                    :value="type"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('court')">
              <el-form-item label="受理法院" prop="court">
                <el-select
                  v-model="formData.court"
                  filterable
                  remote
                  :remote-method="searchCourt"
                  placeholder="请搜索法院"
                >
                  <el-option
                    v-for="court in courtList"
                    :key="court"
                    :label="court"
                    :value="court"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="立案时间" prop="filingDate">
                <el-date-picker
                  v-model="formData.filingDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="审限时间" prop="deadlineDate">
                <el-date-picker
                  v-model="formData.deadlineDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="委托时间" prop="commissionDate">
                <el-date-picker
                  v-model="formData.commissionDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="案件标签" prop="tags">
                <el-select
                  v-model="formData.tags"
                  multiple
                  filterable
                  allow-create
                  placeholder="请选择或创建标签"
                >
                  <el-option
                    v-for="tag in commonTags"
                    :key="tag"
                    :label="tag"
                    :value="tag"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="案件简述" prop="summary">
                <el-input
                  v-model="formData.summary"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入案件简述"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件等级" prop="level">
                <el-radio-group v-model="formData.level">
                  <el-radio label="重要">重要</el-radio>
                  <el-radio label="一般">一般</el-radio>
                  <el-radio label="次要">次要</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="结案/归档">
                <el-checkbox v-model="showArchiveInfo">填写结案或归档信息</el-checkbox>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件主办" prop="ownerId">
                <el-select
                  v-model="formData.ownerId"
                  filterable
                  placeholder="选择主办律师"
                >
                  <el-option
                    v-for="lawyer in lawyerList"
                    :key="lawyer.id"
                    :label="lawyer.name"
                    :value="lawyer.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="协办律师" prop="coOwners">
                <el-select
                  v-model="formData.coOwners"
                  multiple
                  filterable
                  placeholder="选择协办律师"
                >
                  <el-option
                    v-for="lawyer in lawyerList"
                    :key="lawyer.id"
                    :label="lawyer.name"
                    :value="lawyer.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="律师助理" prop="assistants">
                <el-select
                  v-model="formData.assistants"
                  multiple
                  filterable
                  placeholder="选择律师助理"
                >
                  <el-option
                    v-for="assistant in assistantList"
                    :key="assistant.id"
                    :label="assistant.name"
                    :value="assistant.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- A2. 立案专项信息（行政表1） -->
        <div class="form-section">
          <div class="section-header">
            <h3>A2. 立案专项信息</h3>
            <el-button text type="primary" size="small" @click="applyAdminCaseName">
              按规则生成案件名称
            </el-button>
          </div>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="收案日期" prop="acceptanceDate">
                <el-date-picker
                  v-model="formData.acceptanceDate"
                  type="date"
                  placeholder="案件立案日期"
                  value-format="YYYY-MM-DD"
                  :disabled-date="(time) => time.getTime() > Date.now()"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8" v-if="showAdminField('courtCaseNumber')">
              <el-form-item label="法院案号" prop="courtCaseNumber">
                <el-input
                  v-model="formData.courtCaseNumber"
                  placeholder="格式：2025-京0105民初1234号"
                  clearable
                >
                  <template #append>
                    <el-tooltip content="法院案号格式示例：（2025）京0105民初1234号、（2025）粤0106民初5678号" placement="top">
                      <el-icon><QuestionFilled /></el-icon>
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>

            <el-col :span="8" v-if="showAdminField('hearingDate')">
              <el-form-item label="开庭日期" prop="hearingDate">
                <el-date-picker
                  v-model="formData.hearingDate"
                  type="date"
                  placeholder="选择开庭日期"
                  value-format="YYYY-MM-DD"
                  :disabled-date="(time) => {
                    if (!formData.acceptanceDate) return false
                    return time.getTime() < new Date(formData.acceptanceDate).getTime()
                  }"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('representationType')">
              <el-form-item label="代理类型" prop="representationType">
                <el-radio-group v-model="formData.representationType">
                  <el-radio label="PLAINTIFF">原告（被害人）</el-radio>
                  <el-radio label="DEFENDANT">被告</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('contractStartDate')">
              <el-form-item label="合同服务开始" prop="contractStartDate">
                <el-date-picker
                  v-model="formData.contractStartDate"
                  type="date"
                  placeholder="选择开始日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('contractEndDate')">
              <el-form-item label="合同服务结束" prop="contractEndDate">
                <el-date-picker
                  v-model="formData.contractEndDate"
                  type="date"
                  placeholder="选择结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="案源人（可多选）" prop="sourcePerson">
                <el-select
                  v-model="formData.sourcePerson"
                  placeholder="请选择案源人（可多选）"
                  multiple
                  filterable
                  allow-create
                  style="width: 100%"
                >
                  <el-option
                    v-for="lawyer in lawyerList"
                    :key="lawyer.id"
                    :label="lawyer.name"
                    :value="lawyer.name"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="其他委托人（可多选）">
                <el-select
                  v-model="formData.otherClients"
                  placeholder="请选择其他委托人（可多选）"
                  multiple
                  filterable
                  allow-create
                  style="width: 100%"
                >
                  <el-option
                    v-for="client in clientList"
                    :key="client.id"
                    :label="client.name"
                    :value="client.name"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('procedureLevels')">
              <el-form-item label="审级（可多选）" prop="procedureLevels">
                <el-select
                  v-model="formData.procedureLevels"
                  placeholder="请选择审级"
                  multiple
                  style="width: 100%"
                >
                  <el-option
                    v-for="lv in procedureLevelOptions"
                    :key="lv"
                    :label="lv"
                    :value="lv"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="formData.caseType === 'CRIMINAL'">
              <el-form-item label="是否法律援助">
                <el-switch v-model="formData.isLegalAid" />
                <span style="margin-left: 10px; color: #909399; font-weight: bold;">
                  {{ formData.isLegalAid ? '✅ 是' : '❌ 否' }}
                </span>
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('criminalSuspect')">
              <el-form-item label="犯罪嫌疑人" prop="criminalSuspect">
                <el-input
                  v-model="formData.criminalSuspect"
                  placeholder="请输入犯罪嫌疑人姓名"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('disputedAmount')">
              <el-form-item label="涉案标的(万元)" prop="disputedAmount">
                <el-input-number
                  v-model="formData.disputedAmount"
                  :min="0"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  style="width: 100%"
                  :placeholder="riskFeeRequiresAmount ? '风险/固定+风险时必填' : '可选'"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="showAdminField('subjectMatter')">
              <el-form-item label="涉案主体/标的物" prop="subjectMatter">
                <el-input v-model="formData.subjectMatter" placeholder="非诉案件填写涉案主体或标的物" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="主办部门（可多选）">
                <el-select
                  v-model="formData.hostDepartment"
                  placeholder="根据主办律师自动关联"
                  multiple
                  disabled
                  style="width: 100%"
                >
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="协办部门（可多选）">
                <el-select
                  v-model="formData.coDepartments"
                  placeholder="根据协办律师自动关联"
                  multiple
                  disabled
                  style="width: 100%"
                >
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <el-alert
          v-if="documentTemplateList.length"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
          title="关联模版/表格（行政表1）"
        >
          <template #default>
            {{ documentTemplateList.join('、') }}
          </template>
        </el-alert>

        <!-- A3. 分配情况 -->
        <div class="form-section">
          <div class="section-header">
            <h3>A3. 分配情况</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="案源人比例(%)" prop="sourcePersonPercentage">
                <el-input-number
                  v-model="formData.sourcePersonPercentage"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  style="width: 100%"
                  @change="validatePercentageSum"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="承办部门比例(%)" prop="departmentPercentage">
                <el-input-number
                  v-model="formData.departmentPercentage"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  style="width: 100%"
                  @change="validatePercentageSum"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="律所提留比例(%)" prop="firmPercentage">
                <el-input-number
                  v-model="formData.firmPercentage"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  style="width: 100%"
                  @change="validatePercentageSum"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-alert
                v-if="percentageSum !== 100 && percentageSum !== null"
                :title="`当前总比例: ${percentageSum}%，必须等于100%`"
                type="warning"
                :closable="false"
                style="margin-top: 10px"
              />
              <el-alert
                v-else-if="percentageSum === 100"
                title="分配比例正确（100%）"
                type="success"
                :closable="false"
                style="margin-top: 10px"
              />
            </el-col>
          </el-row>
        </div>

        <!-- A1. 金融不良资产专项 -->
        <div v-if="formData.caseType === 'FINANCIAL_NPA'" class="form-section">
          <div class="section-header">
            <h3>A1. 金融不良资产专项</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="业务子类">
                <el-select v-model="formData.npaSubtype" placeholder="请选择业务子类">
                  <el-option label="信用贷" value="CREDIT_LOAN" />
                  <el-option label="抵押贷" value="MORTGAGE_LOAN" />
                  <el-option label="担保贷" value="GUARANTEE_LOAN" />
                  <el-option label="信用卡" value="CREDIT_CARD" />
                  <el-option label="承兑汇票" value="ACCEPTANCE_BILL" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="委托银行">
                <el-input v-model="formData.entrustingBankName" placeholder="请输入委托银行名称" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="资产批次号">
                <el-input v-model="formData.assetBatchNo" placeholder="请输入批次号" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="转让协议号">
                <el-input v-model="formData.transferAgreementNo" placeholder="请输入转让协议编号" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="借款合同号">
                <el-input v-model="formData.loanContractNo" placeholder="请输入借款合同号" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="担保方式">
                <el-input v-model="formData.guaranteeType" placeholder="保证/抵押/质押/信用" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="本金余额">
                <el-input-number v-model="formData.principalBalance" :precision="2" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="利息余额">
                <el-input-number v-model="formData.interestBalance" :precision="2" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="执行回款">
                <el-input-number v-model="formData.executionRecoveryAmount" :precision="2" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="抵押物状态">
                <el-input v-model="formData.collateralStatus" placeholder="查封/轮候/已处置等" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="保全状态">
                <el-input v-model="formData.preservationStatus" placeholder="未申请/已保全/已解除等" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="终本状态">
                <el-input v-model="formData.terminationStatus" placeholder="非终本/终本/恢复执行" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- B. 当事人及关联方 -->
        <div class="form-section">
          <div class="section-header">
            <h3>B. 当事人及关联方</h3>
            <el-button type="primary" size="small" @click="handleAddParty">
              <el-icon><Plus /></el-icon>
              添加当事人
            </el-button>
          </div>

          <div v-if="formData.parties.length === 0" class="empty-tip">
            <el-empty description="暂无当事人，请添加" />
          </div>

          <div v-for="(party, index) in formData.parties" :key="index" class="party-item">
            <div class="party-header">
              <span>当事人 #{{ index + 1 }}</span>
              <div>
                <el-button text size="small" @click="handleCopyParty(index)">
                  <el-icon><DocumentCopy /></el-icon>
                  复制
                </el-button>
                <el-button text type="danger" size="small" @click="handleDeleteParty(index)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>

            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item
                  label="类型"
                  :prop="`parties.${index}.type`"
                  :rules="{ required: true, message: '请选择类型', trigger: 'change' }"
                >
                  <el-radio-group v-model="party.type">
                    <el-radio label="个人">个人</el-radio>
                    <el-radio label="单位">单位</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item
                  label="委托方"
                  :prop="`parties.${index}.isClient`"
                  :rules="{ type: 'boolean', required: true, message: '请选择是否委托方', trigger: 'change' }"
                >
                  <el-switch v-model="party.isClient" />
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item
                  label="属性"
                  :prop="`parties.${index}.attribute`"
                  :rules="{ required: true, message: '请选择属性', trigger: 'change' }"
                >
                  <el-select v-model="party.attribute" placeholder="请选择诉讼地位/角色">
                    <el-option
                      v-for="attr in partyAttributeOptions"
                      :key="attr"
                      :label="attr"
                      :value="attr"
                    />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :span="12">
                <el-form-item
                  :label="party.type === '个人' ? '姓名' : '单位名称'"
                  :prop="`parties.${index}.name`"
                  :rules="[
                    { required: true, message: `请输入${party.type === '个人' ? '姓名' : '单位名称'}`, trigger: 'blur' },
                    { min: 2, max: 50, message: '长度在2-50个字符', trigger: 'blur' }
                  ]"
                >
                  <el-select
                    v-model="party.name"
                    filterable
                    allow-create
                    remote
                    :remote-method="searchClient"
                    placeholder="可从客户库选择"
                  >
                    <el-option
                      v-for="client in clientList"
                      :key="client"
                      :label="client"
                      :value="client"
                    />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :span="12">
                <el-form-item
                  label="联系电话"
                  :prop="`parties.${index}.phone`"
                  :rules="[
                    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
                  ]"
                >
                  <el-input v-model="party.phone" placeholder="请输入联系电话" />
                </el-form-item>
              </el-col>

              <!-- 个人类型额外字段 -->
              <template v-if="party.type === '个人'">
                <el-col :span="12">
                  <el-form-item
                    label="身份证号"
                    :prop="`parties.${index}.idCard`"
                    :rules="[
                      { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号码', trigger: 'blur' }
                    ]"
                  >
                    <el-input v-model="party.idCard" placeholder="请输入身份证号" />
                  </el-form-item>
                </el-col>

                <el-col :span="6">
                  <el-form-item label="性别" :prop="`parties.${index}.gender`">
                    <el-radio-group v-model="party.gender">
                      <el-radio label="男">男</el-radio>
                      <el-radio label="女">女</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>

                <el-col :span="6">
                  <el-form-item label="民族" :prop="`parties.${index}.nation`">
                    <el-select v-model="party.nation" placeholder="请选择">
                      <el-option label="汉族" value="汉族" />
                      <el-option label="少数民族" value="少数民族" />
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="24">
                  <el-form-item label="住址" :prop="`parties.${index}.address`">
                    <el-input v-model="party.address" placeholder="请输入住址" />
                  </el-form-item>
                </el-col>
              </template>

              <!-- 单位类型额外字段 -->
              <template v-if="party.type === '单位'">
                <el-col :span="12">
                  <el-form-item
                    label="信用代码"
                    :prop="`parties.${index}.creditCode`"
                    :rules="[
                      { pattern: /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/, message: '请输入正确的统一社会信用代码', trigger: 'blur' }
                    ]"
                  >
                    <el-input v-model="party.creditCode" placeholder="请输入统一社会信用代码" />
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item label="法定代表人" :prop="`parties.${index}.legalRep`">
                    <el-input v-model="party.legalRep" placeholder="请输入法定代表人" />
                  </el-form-item>
                </el-col>

                <el-col :span="24">
                  <el-form-item label="地址" :prop="`parties.${index}.address`">
                    <el-input v-model="party.address" placeholder="请输入单位地址" />
                  </el-form-item>
                </el-col>
              </template>

              <el-col :span="12">
                <el-form-item label="代理律师" :prop="`parties.${index}.opposingLawyer`">
                  <el-input v-model="party.opposingLawyer" placeholder="对方律师信息" />
                </el-form-item>
              </el-col>

              <el-col :span="24">
                <el-form-item label="备注" :prop="`parties.${index}.remark`">
                  <el-input
                    v-model="party.remark"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入备注"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="24">
                <el-form-item>
                  <el-checkbox v-model="party.syncToClient">
                    同步创建到客户库
                  </el-checkbox>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- C. 代理律师费 -->
        <div class="form-section">
          <div class="section-header">
            <h3>C. 代理律师费</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="收费方式" prop="feeMethodChoice">
                <el-radio-group v-model="formData.feeMethodChoice">
                  <el-radio
                    v-for="m in feeMethodOptions"
                    :key="m"
                    :label="m"
                  >
                    {{ m }}
                  </el-radio>
                </el-radio-group>
                <div v-if="formData.feeMethodChoice === '免费代理'" class="field-hint">
                  免费代理提交审批时须填写备注理由并经主任审批
                </div>
                <div v-if="formData.feeMethodChoice === '未确定'" class="field-hint">
                  未确定金额案件系统将按月提醒补录合同金额
                </div>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="标的额(元)" prop="amount">
                <el-input-number
                  v-model="formData.amount"
                  :min="0"
                  :precision="2"
                  :step="1000"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="标的物" prop="subjectMatter">
                <el-input v-model="formData.subjectMatter" placeholder="请输入标的物" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="代理费(元)" prop="lawyerFee">
                <el-input-number
                  v-model="formData.lawyerFee"
                  :min="0"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="收费简介" prop="feeSummary">
                <el-input
                  v-model="formData.feeSummary"
                  type="textarea"
                  :rows="2"
                  maxlength="200"
                  show-word-limit
                  placeholder="请输入收费简介"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="收费备注" prop="feeRemark">
                <el-input
                  v-model="formData.feeRemark"
                  type="textarea"
                  :rows="3"
                  maxlength="250"
                  show-word-limit
                  placeholder="请输入收费备注"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12" style="background: #f6ffed; padding: 10px; border: 2px solid #52c41a;">
              <el-form-item label="🆕 固定费用(元)">
                <el-input-number
                  v-model="formData.fixedFee"
                  :min="0"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                  placeholder="固定收费案件填写"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12" style="background: #f6ffed; padding: 10px; border: 2px solid #52c41a;">
              <el-form-item label="🆕 风险比例(%)">
                <el-input-number
                  v-model="formData.riskRatio"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  style="width: 100%"
                  placeholder="按比例收费时填写"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12" style="background: #f6ffed; padding: 10px; border: 2px solid #52c41a;">
              <el-form-item label="🆕 风险费用(元)">
                <el-input-number
                  v-model="formData.riskFee"
                  :min="0"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                  placeholder="按金额收费时填写"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24" style="background: #f6ffed; padding: 10px; border: 2px solid #52c41a;">
              <el-form-item label="🆕 收费方式详细说明">
                <el-input
                  v-model="formData.feeRemarkDetail"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  placeholder="其他审级收费约定、风险费用支付约定细则等"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- D. 应收款信息 -->
        <div class="form-section">
          <div class="section-header">
            <h3>D. 应收款信息</h3>
            <el-button type="primary" size="small" @click="handleAddReceivable">
              <el-icon><Plus /></el-icon>
              添加应收款
            </el-button>
          </div>

          <div v-if="formData.receivables.length === 0" class="empty-tip">
            <el-empty description="暂无应收款，请添加" />
          </div>

          <div v-for="(receivable, index) in formData.receivables" :key="index" class="receivable-item">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-form-item
                  label="款项名称"
                  :prop="`receivables.${index}.name`"
                  :rules="{ required: true, message: '请输入款项名称', trigger: 'blur' }"
                >
                  <el-input v-model="receivable.name" placeholder="请输入款项名称" />
                </el-form-item>
              </el-col>

              <el-col :span="6">
                <el-form-item
                  label="应收金额(元)"
                  :prop="`receivables.${index}.amount`"
                  :rules="{ required: true, message: '请输入应收金额', trigger: 'blur' }"
                >
                  <el-input-number
                    v-model="receivable.amount"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item
                  label="约定收款日期"
                  :prop="`receivables.${index}.dueDate`"
                  :rules="{ required: true, message: '请选择收款日期', trigger: 'change' }"
                >
                  <el-date-picker
                    v-model="receivable.dueDate"
                    type="date"
                    placeholder="选择日期"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="4">
                <el-form-item label="操作">
                  <el-button type="danger" text @click="handleDeleteReceivable(index)">
                    删除
                  </el-button>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- E. 结案/归档信息 -->
        <div class="form-section" v-if="showArchiveInfo">
          <div class="section-header">
            <h3>E. 结案/归档信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="结案状态" prop="closeStatus">
                <el-select v-model="formData.closeStatus" placeholder="请选择结案状态" clearable>
                  <el-option label="达成诉求" value="达成诉求" />
                  <el-option label="部分达成" value="部分达成" />
                  <el-option label="未达成" value="未达成" />
                  <el-option label="未委托" value="未委托" />
                  <el-option label="终止" value="终止" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="结案日期" prop="closeDate">
                <el-date-picker
                  v-model="formData.closeDate"
                  type="date"
                  placeholder="选择结案日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="归档日期" prop="archiveDate">
                <el-date-picker
                  v-model="formData.archiveDate"
                  type="date"
                  placeholder="选择归档日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="档案保管地" prop="archiveLocation">
                <el-input
                  v-model="formData.archiveLocation"
                  placeholder="请输入档案保管地点"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- F. 关联信息 -->
        <div class="form-section">
          <div class="section-header">
            <h3>F. 关联信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="关联客户" prop="relatedClients">
                <el-select
                  v-model="formData.relatedClients"
                  multiple
                  filterable
                  placeholder="从客户库选择"
                >
                  <el-option
                    v-for="client in clientList"
                    :key="client"
                    :label="client"
                    :value="client"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="关联案件" prop="relatedCases">
                <el-select
                  v-model="formData.relatedCases"
                  multiple
                  filterable
                  placeholder="从案件库选择"
                >
                  <el-option
                    v-for="caseItem in caseOptions"
                    :key="caseItem.id"
                    :label="caseItem.name"
                    :value="caseItem.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="关联项目" prop="relatedProject">
                <el-input v-model="formData.relatedProject" placeholder="请输入关联项目" />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="formData.remark"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入备注信息"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
    </div>

    <!-- AI智能填充组件 -->
    <AIDocumentFill
      v-model="aiFillDialogVisible"
      @confirm="handleAIFillConfirm"
    />

    <!-- 查重对话框 -->
    <el-dialog v-model="duplicateDialogVisible" title="疑似重复案件" width="800px">
      <el-table :data="duplicateCases" border>
        <el-table-column prop="caseName" label="案件名称" />
        <el-table-column prop="caseNumber" label="案号" />
        <el-table-column prop="court" label="法院" />
        <el-table-column prop="ownerName" label="主办律师" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDuplicateCase(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 利益冲突审查结果对话框 -->
    <el-dialog v-model="conflictDialogVisible" title="利益冲突审查结果" width="900px">
      <el-alert
        v-if="conflictCheckResult && conflictCheckResult.hasConflict"
        title="发现利益冲突！"
        type="error"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #default>
          <p>建议：{{ conflictCheckResult.recommendation === 'APPLY_FOR_WAIVER' ? '申请利益冲突豁免' : '修改当事人信息' }}</p>
        </template>
      </el-alert>

      <el-alert
        v-else
        title="未发现利益冲突"
        type="success"
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-table
        v-if="conflictCheckResult && conflictCheckResult.conflicts"
        :data="conflictCheckResult.conflicts"
        border
      >
        <el-table-column prop="type" label="冲突类型" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.type === 'CLIENT_NAME'" type="danger">客户名称冲突</el-tag>
            <el-tag v-else-if="row.type === 'PARTY_CONFLICT'" type="danger">当事人冲突</el-tag>
            <el-tag v-else-if="row.type === 'HIGH_SIMILARITY'" type="warning">高度相似</el-tag>
            <el-tag v-else type="info">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="冲突描述" />
        <el-table-column prop="relatedName" label="关联案件/客户" width="200" />
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.severity === 'HIGH'" type="danger">高</el-tag>
            <el-tag v-else-if="row.severity === 'MEDIUM'" type="warning">中</el-tag>
            <el-tag v-else-if="row.severity === 'LOW'" type="info">低</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="conflictDialogVisible = false">关闭</el-button>
          <el-button
            v-if="conflictCheckResult && conflictCheckResult.hasConflict"
            type="warning"
            @click="handleApplyForWaiver"
          >
            申请豁免
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, MagicStick, DocumentCopy, Delete, UploadFilled, Warning
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import AIDocumentFill from '@/components/AIDocumentFill.vue'
import { createCase, updateCase, checkDuplicate, getCaseDetail, comprehensiveConflictCheck, confirmCaseEstablishment } from '@/api/case'
import { createApproval } from '@/api/approval'
import { getIntakePrefill, attachCaseIntakeFromPending } from '@/api/caseIntake'
import { searchClients } from '@/api/client'
import { useSubmitForm } from '@/composables/useSubmitForm'
import {
  ADMIN_CASE_TYPES,
  CASE_NUMBER_TEMPLATES,
  getCaseSchema,
  isAdminFieldVisible,
  generateAdminCaseName,
  validateAdminCaseForm,
  isRiskFeeMethod,
  ADMIN_PARTY_ROLE_MAP,
  FEE_METHOD_BACKEND_MAP
} from '@/config/case-create-admin'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const aiFillDialogVisible = ref(false)
const duplicateDialogVisible = ref(false)
const duplicateCases = ref([])

// ==================== 新增响应式变量（对标行政管理要求）====================
const conflictDialogVisible = ref(false)
const conflictCheckResult = ref(null)

// 判断是否为编辑模式
const isEditMode = computed(() => !!route.params.id)
const caseId = computed(() => route.params.id)

// ==================== 新增computed属性（对标行政管理要求）====================

const adminCaseTypeGroups = computed(() => {
  const groups = {}
  ADMIN_CASE_TYPES.forEach((t) => {
    if (!groups[t.group]) groups[t.group] = { label: t.group, options: [] }
    groups[t.group].options.push(t)
  })
  return Object.values(groups)
})

const caseNumberTemplateHint = computed(() => {
  return CASE_NUMBER_TEMPLATES[formData.caseType] || '审批通过后自动生成'
})

const showAdminField = (key) => isAdminFieldVisible(formData.caseType, key)

const procedureLevelOptions = computed(() => getCaseSchema(formData.caseType).procedureLevels || [])

const feeMethodOptions = computed(() => getCaseSchema(formData.caseType).feeMethods || [])

const partyAttributeOptions = computed(() => getCaseSchema(formData.caseType).partyAttributes || [])

const businessTypeOptions = computed(() => getCaseSchema(formData.caseType).businessTypes || [])

const documentTemplateList = computed(() => getCaseSchema(formData.caseType).documentTemplates || [])

const riskFeeRequiresAmount = computed(() => isRiskFeeMethod(formData.feeMethodChoice))

const applyAdminCaseName = () => {
  const name = generateAdminCaseName(formData)
  if (name) {
    formData.caseName = name
    ElMessage.success('已按行政规则生成案件名称')
  } else {
    ElMessage.warning('请先填写案由、当事人或犯罪嫌疑人等信息')
  }
}

// 计算分配比例总和
const percentageSum = computed(() => {
  const sourcePerson = formData.sourcePersonPercentage || 0
  const department = formData.departmentPercentage || 0
  const firm = formData.firmPercentage || 0
  const sum = sourcePerson + department + firm
  return sum === 0 ? null : Math.round(sum * 100) / 100 // 保留两位小数
})

// 案件类型变化处理（行政表1）
const handleCaseTypeChange = () => {
  formData.businessType = ''
  formData.procedureLevels = []
  formData.feeMethodChoice = ''
  if (formData.caseType !== 'CRIMINAL') {
    formData.criminalSuspect = ''
    formData.representationType = ''
  }
  if (formData.caseType !== 'ADVISORY') {
    formData.contractStartDate = ''
    formData.contractEndDate = ''
  }
  if (!isAdminFieldVisible(formData.caseType, 'court')) {
    formData.court = ''
    formData.courtCaseNumber = ''
    formData.hearingDate = ''
  }
}


// 转换formData为后端DTO格式
const transformToRequest = () => {
  // 转换收费方式数组为字符串
  const feeMethodMap = {
    '固定收费': 'FIXED',
    '按比例收费': 'PERCENTAGE',
    '风险代理': 'CONTINGENT',
    '计时收费': 'HOURLY',
    '协商收费': 'NEGOTIATED'
  }

  // 转换当事人类型和属性
  const partyTypeMap = {
    '个人': 'INDIVIDUAL',
    '单位': 'ORGANIZATION'
  }

  const partyRoleMap = { ...ADMIN_PARTY_ROLE_MAP }

  let feeMethod = null
  if (formData.feeMethodChoice) {
    feeMethod = FEE_METHOD_BACKEND_MAP[formData.feeMethodChoice] || formData.feeMethodChoice
  } else if (formData.feeTypes && formData.feeTypes.length > 0) {
    feeMethod = formData.feeTypes.map((type) => feeMethodMap[type] || type).join(',')
  }

  // 转换当事人数据
  const parties = (formData.parties || []).map(party => ({
    id: party.id || null,
    partyType: partyTypeMap[party.type] || 'INDIVIDUAL',
    partyRole: partyRoleMap[party.attribute] || party.attribute,
    name: party.name || '',
    isClient: party.isClient || false,
    syncToClient: party.syncToClient || false,
    gender: party.gender || null,
    ethnicity: party.nation || null,
    idCard: party.idCard || null,
    creditCode: party.creditCode || null,
    phone: party.phone || null,
    address: party.address || null,
    legalRepresentative: party.legalRep || null,
    opposingLawyer: party.opposingLawyer || null,
    notes: party.remark || null
  }))

  return {
    // A. 基本信息
    caseType: formData.caseType,
    procedure: formData.procedure,
    caseName: formData.caseName,
    caseNumber: formData.caseNumber,
    caseReason: formData.caseReason,
    court: formData.court,
    filingDate: formData.filingDate || null,
    deadlineDate: formData.deadlineDate || null,
    commissionDate: formData.commissionDate || null,
    tags: formData.tags?.join(',') || null,
    summary: formData.summary,
    level: formData.level === '重要' ? 'IMPORTANT' : formData.level === '次要' ? 'MINOR' : 'GENERAL',
    ownerId: formData.ownerId,
    coOwnerIds: formData.coOwners || [],
    assistantIds: formData.assistants || [],

    // 律师费信息（映射到后端字段）
    amount: formData.amount || null,
    attorneyFee: formData.lawyerFee || null,
    feeMethod: feeMethod,
    feeDescription: formData.feeSummary || null,
    feeNotes: formData.feeRemark || null,
    npaSubtype: formData.npaSubtype || null,
    entrustingBankName: formData.entrustingBankName || null,
    assetBatchNo: formData.assetBatchNo || null,
    transferAgreementNo: formData.transferAgreementNo || null,
    loanContractNo: formData.loanContractNo || null,
    principalBalance: formData.principalBalance || null,
    interestBalance: formData.interestBalance || null,
    guaranteeType: formData.guaranteeType || null,
    collateralStatus: formData.collateralStatus || null,
    preservationStatus: formData.preservationStatus || null,
    executionRecoveryAmount: formData.executionRecoveryAmount || null,
    terminationStatus: formData.terminationStatus || null,

    // B. 当事人
    parties: parties,

    // D. 应收款
    receivables: formData.receivables || [],

    // E. 关联信息
    clientIds: formData.relatedClients?.map(c => c.id || c) || [],
    relatedCaseIds: formData.relatedCases?.map(c => c.id || c) || [],

    // ==================== 新增字段（对标行政管理要求）====================
    acceptanceDate: formData.acceptanceDate || null,
    courtCaseNumber: formData.courtCaseNumber || null,
    hearingDate: formData.hearingDate || null,
    contractStartDate: formData.contractStartDate || null,
    contractEndDate: formData.contractEndDate || null,
    representationType: formData.representationType || null,
    sourcePerson: Array.isArray(formData.sourcePerson) && formData.sourcePerson.length > 0
      ? JSON.stringify(formData.sourcePerson)
      : null,
    sourcePersonPercentage: formData.sourcePersonPercentage || null,
    departmentPercentage: formData.departmentPercentage || null,
    firmPercentage: formData.firmPercentage || null,
    conflictCheckStatus: formData.conflictCheckStatus || 'PENDING',
    conflictWaiverApprovalId: formData.conflictWaiverApprovalId || null,

    // 新增字段（对标系统问题.xlsx）
    businessType: formData.businessType || null,
    criminalSuspect: formData.criminalSuspect || null,
    disputedAmount: formData.disputedAmount || null,
    hostDepartment: Array.isArray(formData.hostDepartment) && formData.hostDepartment.length > 0
      ? JSON.stringify(formData.hostDepartment)
      : null,
    coDepartments: Array.isArray(formData.coDepartments) && formData.coDepartments.length > 0
      ? JSON.stringify(formData.coDepartments)
      : null,
    remark: formData.remark || null,

    // 新增字段（对标案件登记及系统立结案流程）
    otherClients: Array.isArray(formData.otherClients) && formData.otherClients.length > 0
      ? JSON.stringify(formData.otherClients)
      : null,
    procedureLevels: Array.isArray(formData.procedureLevels) && formData.procedureLevels.length > 0
      ? JSON.stringify(formData.procedureLevels)
      : null,
    isLegalAid: formData.isLegalAid || false,
    fixedFee: formData.fixedFee || null,
    riskRatio: formData.riskRatio || null,
    riskFee: formData.riskFee || null,
    feeRemark: formData.feeRemarkDetail || null
  }
}

// 立案状态
const filing = ref(false)

// 提交审批状态
const approving = ref(false)
const createdCaseId = ref(null) // 记录创建的案件ID，用于审批关联
const intakePendingId = computed(() => route.query.intakePendingId
  ? Number(route.query.intakePendingId)
  : null)
const intakeAlreadyAttached = ref(false)
const filingDraftBanner = ref('')
const filingBannerType = ref('info')
const canConfirmEstablishment = ref(true)
const hasPendingFilingApproval = ref(false)
const filingApprovalStatus = ref(null)

const establishmentDisabled = computed(() =>
  isEditMode.value && canConfirmEstablishment.value === false
)

const applyFilingMetaFromCase = (caseData) => {
  canConfirmEstablishment.value = caseData.canConfirmEstablishment !== false
  hasPendingFilingApproval.value = !!caseData.hasPendingFilingApproval
  filingApprovalStatus.value = caseData.filingApprovalStatus || null

  if (hasPendingFilingApproval.value) {
    filingBannerType.value = 'warning'
    filingDraftBanner.value = '立案审批进行中，审批通过后可点击「确认建案」。'
    return
  }
  if (filingApprovalStatus.value === 'APPROVED') {
    filingBannerType.value = 'success'
    filingDraftBanner.value = '立案审批已通过，请核对当事人、收费方式等信息后点击「确认建案」。'
    return
  }
  if (filingApprovalStatus.value === 'REJECTED') {
    filingBannerType.value = 'error'
    filingDraftBanner.value = '立案审批已驳回，请修改后重新提交审批。'
    return
  }
  if ((caseData.summary || '').includes('[卷宗立案草稿]')) {
    filingBannerType.value = 'info'
    filingDraftBanner.value =
      '本案件为立案审批自动生成的草稿，请核对必填项后提交审批或确认建案。'
  }
}

const attachIntakeAfterCreate = async (newCaseId) => {
  if (!intakePendingId.value || !newCaseId || intakeAlreadyAttached.value) return
  try {
    const res = await attachCaseIntakeFromPending(
      intakePendingId.value,
      newCaseId,
      formData.summary || ''
    )
    if (res.code === 200 || res.success) {
      ElMessage.success('卷宗文件已归入新案件')
    }
  } catch (e) {
    ElMessage.warning('案件已创建，卷宗挂接失败：' + (e.message || '请稍后在工作台重试'))
  }
}

const parsePartiesFromRecognitionText = (text) => {
  if (!text || typeof text !== 'string') return []
  const trimmed = text.trim()
  const vsMatch = trimmed.match(/^(.+?)(?:诉|起诉|与)(.+)$/)
  if (vsMatch) {
    return [
      { type: '个人', attribute: '原告', name: vsMatch[1].trim(), phone: '', address: '' },
      { type: '个人', attribute: '被告', name: vsMatch[2].trim(), phone: '', address: '' }
    ]
  }
  return [{ type: '个人', attribute: '原告', name: trimmed, phone: '', address: '' }]
}

const applyAiRecognitionPrefill = () => {
  if (isEditMode.value) return
  const raw = sessionStorage.getItem('case_create_prefill')
  if (!raw) return
  try {
    const p = JSON.parse(raw)
    if (p.source !== 'ai_recognition') return
    sessionStorage.removeItem('case_create_prefill')
    if (!formData.caseType) formData.caseType = 'CIVIL'
    if (!formData.procedure) formData.procedure = 'FIRST_INSTANCE'
    if (p.caseName) formData.caseName = p.caseName
    if (p.caseReason) formData.caseReason = p.caseReason
    if (p.court) formData.court = p.court
    if (p.courtCaseNumber) formData.courtCaseNumber = p.courtCaseNumber
    if (p.summary) formData.summary = p.summary
    if (Array.isArray(p.parties) && p.parties.length) {
      formData.parties = p.parties
    } else if (p.partiesText) {
      const parties = parsePartiesFromRecognitionText(p.partiesText)
      if (parties.length) formData.parties = parties
    }
    ElMessage.success('已从文书识别结果预填，请核对后保存')
  } catch (e) {
    console.warn('AI识别预填失败', e)
  }
}

const applyIntakePrefill = async () => {
  if (!intakePendingId.value || isEditMode.value) return
  try {
    const res = await getIntakePrefill(intakePendingId.value)
    if (!(res.code === 200 || res.success) || !res.data) return
    const p = res.data
    if (p.draftCaseId) {
      intakeAlreadyAttached.value = true
      router.replace(`/case/${p.draftCaseId}/edit`)
      return
    }
    if (!formData.caseType) formData.caseType = 'CIVIL'
    if (!formData.procedure) formData.procedure = 'FIRST_INSTANCE'
    if (p.suggestedCaseName) formData.caseName = p.suggestedCaseName
    if (p.caseReason) formData.caseReason = p.caseReason
    if (p.courtName) formData.court = p.courtName
    if (p.caseNumber) formData.courtCaseNumber = p.caseNumber
    if (p.hearingDate) formData.hearingDate = p.hearingDate
    if (p.remark) formData.summary = p.remark
    const parties = []
    if (p.plaintiffName) {
      parties.push({ type: '个人', attribute: '原告', name: p.plaintiffName, phone: '', address: '' })
    }
    if (p.defendantName) {
      parties.push({ type: '个人', attribute: '被告', name: p.defendantName, phone: '', address: '' })
    }
    if (parties.length) formData.parties = parties
    ElMessage.info('已从卷宗识别结果预填，请核对后保存')
  } catch (e) {
    console.warn('卷宗预填失败', e)
  }
}

// 使用表单防重复提交hook
const { submitting, canSubmit, handleSubmit: handleFormSubmit } = useSubmitForm(
  async () => {
    await formRef.value?.validate()
    const requestData = transformToRequest()

    // 根据是否为编辑模式调用不同API
    if (isEditMode.value) {
      await updateCase(caseId.value, requestData)
      router.push('/case/list')
    } else {
      const response = await createCase(requestData)
      const newId = response.data?.id || response.data
      await attachIntakeAfterCreate(newId)
      router.push(newId ? `/case/${newId}` : '/case/list')
    }
  },
  {
    get successMessage() {
      return isEditMode.value ? '案件更新成功' : '案件创建成功'
    },
    confirmMessage: null,
    beforeSubmit: async () => {
      // 验证至少有一个当事人
      if (!formData.parties || formData.parties.length === 0) {
        ElMessage.warning('请至少添加一个当事人')
        return false
      }

      // 验证当事人必填字段
      for (let i = 0; i < formData.parties.length; i++) {
        const party = formData.parties[i]
        if (!party.type) {
          ElMessage.warning(`第${i + 1}个当事人：请选择类型`)
          return false
        }
        if (!party.name || party.name.trim() === '') {
          ElMessage.warning(`第${i + 1}个当事人：请输入${party.type === '个人' ? '姓名' : '单位名称'}`)
          return false
        }
        if (!party.attribute) {
          ElMessage.warning(`第${i + 1}个当事人：请选择属性`)
          return false
        }
      }
      if (formData.conflictCheckStatus === 'PENDING') {
        ElMessage.warning('请先完成利益冲突审查')
        return false
      }
      if (formData.conflictCheckStatus === 'CONFLICT') {
        ElMessage.warning('存在利益冲突，请申请豁免或修改当事人后再保存')
        return false
      }
      const adminCheck = validateAdminCaseForm(formData)
      if (!adminCheck.ok) {
        ElMessage.warning(adminCheck.errors[0])
        return false
      }
      return true
    }
  }
)

// 确认正式建案（草稿 → 审理中，需利冲通过）
const handleFiling = async () => {
  try {
    if (establishmentDisabled.value) {
      ElMessage.warning(hasPendingFilingApproval.value
        ? '立案审批进行中，请等待审批通过'
        : '请先完成利冲审查或立案审批')
      return
    }
    const valid = await formRef.value?.validate()
    if (!valid) {
      ElMessage.warning('请先完善必填信息')
      return
    }
    if (!formData.parties || formData.parties.length === 0) {
      ElMessage.warning('请至少添加一个当事人')
      return
    }
    const sum = percentageSum.value
    if (sum !== null && sum !== 100) {
      ElMessage.error(`分配比例总和必须为100%，当前为${sum}%，请检查A3.分配情况`)
      return
    }
    if (formData.conflictCheckStatus === 'PENDING') {
      ElMessage.warning('请先完成利益冲突审查')
      return
    }
    if (formData.conflictCheckStatus === 'CONFLICT') {
      ElMessage.warning('存在利益冲突，请申请豁免或修改当事人')
      return
    }

    await ElMessageBox.confirm(
      '确认建案后案件将进入「审理中」，并初始化阶段卷宗目录。是否继续？',
      '确认建案',
      { confirmButtonText: '确认建案', cancelButtonText: '取消', type: 'success' }
    )

    filing.value = true
    let targetId = isEditMode.value ? caseId.value : null

    if (!targetId) {
      const draftPayload = { ...transformToRequest(), saveAsDraft: true, status: 'PENDING_FILING' }
      const response = await createCase(draftPayload)
      targetId = response.data?.id || response.data
      if (!targetId) throw new Error('创建草稿失败')
      await attachIntakeAfterCreate(targetId)
    } else {
      await updateCase(targetId, transformToRequest())
    }

    const res = await confirmCaseEstablishment(targetId)
    if (res.code === 200 || res.success) {
      ElMessage.success('案件已正式建立')
      router.push(`/case/${targetId}`)
    } else {
      ElMessage.error(res.message || '确认建案失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认建案失败:', error)
      ElMessage.error('确认建案失败: ' + (error.message || '未知错误'))
    }
  } finally {
    filing.value = false
  }
}

// 是否显示结案/归档信息
const showArchiveInfo = ref(false)

// 表单数据
const formData = reactive({
  // A. 基本信息
  caseType: '',
  procedure: '',
  caseName: '',
  caseNumber: '',
  caseReason: '',
  court: '',
  filingDate: '',
  deadlineDate: '',
  commissionDate: '',
  tags: [],
  summary: '',
  level: '一般',
  ownerId: '',
  coOwners: [],
  assistants: [],
  npaSubtype: '',
  entrustingBankName: '',
  assetBatchNo: '',
  transferAgreementNo: '',
  loanContractNo: '',
  principalBalance: null,
  interestBalance: null,
  guaranteeType: '',
  collateralStatus: '',
  preservationStatus: '',
  executionRecoveryAmount: null,
  terminationStatus: '',

  // B. 当事人
  parties: [],

  // C. 代理律师费
  feeTypes: [],
  feeMethodChoice: '',
  amount: null,
  subjectMatter: '',
  lawyerFee: null,
  feeSummary: '',
  feeRemark: '',

  // D. 应收款
  receivables: [],

  // E. 关联信息
  relatedClients: [],
  relatedCases: [],
  relatedProject: '',
  remark: '',

  // F. 结案/归档信息
  closeStatus: '',
  closeDate: '',
  archiveDate: '',
  archiveLocation: '',

  // ==================== 新增字段（对标行政管理要求）====================
  // 收案日期
  acceptanceDate: '',
  // 法院案号
  courtCaseNumber: '',
  // 开庭日期
  hearingDate: '',
  // 合同服务开始时间（顾问类）
  contractStartDate: '',
  // 合同服务结束时间（顾问类）
  contractEndDate: '',
  // 代理类型（刑事：原告/被告）
  representationType: '',
  // 案源人（支持多人，JSON数组）
  sourcePerson: [],
  // 案源人分配比例（%）
  sourcePersonPercentage: null,
  // 承办部门分配比例（%）
  departmentPercentage: null,
  // 律所提留比例（%）
  firmPercentage: null,
  // 利益冲突审查状态
  conflictCheckStatus: 'PENDING',
  // 利益冲突豁免审批ID
  conflictWaiverApprovalId: null,

  // ==================== 新增字段（对标系统问题.xlsx）====================
  // 业务类型（根据案件类型变化）
  businessType: '',
  // 犯罪嫌疑人（刑事案件专用）
  criminalSuspect: '',
  // 涉案标的（单位：万元）
  disputedAmount: null,
  // 主办部门（支持多部门，JSON数组）
  hostDepartment: [],
  // 协办部门（支持多部门，JSON数组）
  coDepartments: [],

  // ==================== 新增字段（对标案件登记及系统立结案流程）====================
  // 其他委托人（可多选，JSON数组）
  otherClients: [],
  // 审级（可多选，JSON数组）
  procedureLevels: [],
  // 是否法律援助案件
  isLegalAid: false,
  // 固定费用金额
  fixedFee: null,
  // 风险比例（%）
  riskRatio: null,
  // 风险费用金额
  riskFee: null,
  // 收费方式详细说明
  feeRemarkDetail: ''
})

// 表单验证规则
const formRules = {
  caseType: [{ required: true, message: '请选择案件类型', trigger: 'change' }],
  procedure: [{ required: true, message: '请选择案件程序', trigger: 'change' }],
  caseName: [{ required: true, message: '请输入案件名称', trigger: 'blur' }],
  caseReason: [{ required: true, message: '请选择案由', trigger: 'change' }],
  court: [{ required: true, message: '请选择管辖法院', trigger: 'change' }],
  level: [{ required: true, message: '请选择案件等级', trigger: 'change' }],
  ownerId: [{ required: true, message: '请选择主办律师', trigger: 'change' }],
  feeMethodChoice: [{ required: true, message: '请选择收费方式', trigger: 'change' }],
  lawyerFee: [{ required: true, message: '请输入代理费', trigger: 'blur' }],
  // 新增字段验证（对标行政管理要求）
  acceptanceDate: [{ required: true, message: '请选择收案日期', trigger: 'change' }],
  courtCaseNumber: [
    {
      validator: (rule, value, callback) => {
        // 诉讼类案件必填
        const litigationTypes = ['CIVIL', 'COMMERCIAL', 'ARBITRATION', 'CRIMINAL', 'ADMINISTRATIVE']
        if (litigationTypes.includes(formData.caseType) && !value) {
          callback(new Error('诉讼类案件必须填写法院案号'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  hearingDate: [
    {
      validator: (rule, value, callback) => {
        // 诉讼类案件必填
        const litigationTypes = ['CIVIL', 'COMMERCIAL', 'ARBITRATION', 'CRIMINAL', 'ADMINISTRATIVE']
        if (litigationTypes.includes(formData.caseType) && !value) {
          callback(new Error('诉讼类案件必须选择开庭日期'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  contractStartDate: [
    {
      validator: (rule, value, callback) => {
        // 顾问类案件必填
        if (formData.caseType === 'ADVISORY' && !value) {
          callback(new Error('顾问类案件必须选择合同开始日期'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  contractEndDate: [
    {
      validator: (rule, value, callback) => {
        // 顾问类案件必填
        if (formData.caseType === 'ADVISORY' && !value) {
          callback(new Error('顾问类案件必须选择合同结束日期'))
        } else if (value && formData.contractStartDate && value < formData.contractStartDate) {
          callback(new Error('结束日期不能早于开始日期'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  representationType: [
    {
      validator: (rule, value, callback) => {
        // 刑事案件必填
        if (formData.caseType === 'CRIMINAL' && !value) {
          callback(new Error('刑事案件必须选择代理类型'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  sourcePerson: [{ required: true, message: '请输入案源人', trigger: 'blur' }]
}

// 预置数据
const caseReasonList = ref([
  '买卖合同纠纷', '借款合同纠纷', '租赁合同纠纷', '劳动争议',
  '机动车交通事故责任纠纷', '离婚纠纷', '继承纠纷', '侵权责任纠纷'
])

const commonTags = ref(['紧急', 'VIP客户', '群体性案件', '媒体关注', '复杂案件'])

const lawyerList = ref([
  { id: 1, name: '张律师' },
  { id: 2, name: '李律师' },
  { id: 3, name: '王律师' }
])

const assistantList = ref([
  { id: 4, name: '小张' },
  { id: 5, name: '小李' }
])

const courtList = ref([])
const clientList = ref([])
const caseOptions = ref([])

// 搜索法院
const searchCourt = async (query) => {
  if (!query) return
  // 使用全国主要法院数据库
  const majorCourts = [
    '北京市朝阳区人民法院', '北京市海淀区人民法院', '北京市东城区人民法院', '北京市西城区人民法院',
    '上海市浦东新区人民法院', '上海市黄浦区人民法院', '上海市徐汇区人民法院', '上海市静安区人民法院',
    '广州市越秀区人民法院', '广州市天河区人民法院', '广州市海珠区人民法院', '广州市白云区人民法院',
    '深圳市福田区人民法院', '深圳市罗湖区人民法院', '深圳市南山区人民法院', '深圳市宝安区人民法院',
    '杭州市西湖区人民法院', '杭州市上城区人民法院', '杭州市下城区人民法院', '杭州市江干区人民法院',
    '南京市鼓楼区人民法院', '南京市玄武区人民法院', '南京市秦淮区人民法院', '南京市建邺区人民法院',
    '成都市武侯区人民法院', '成都市锦江区人民法院', '成都市青羊区人民法院', '成都市金牛区人民法院',
    '武汉市江汉区人民法院', '武汉市武昌区人民法院', '武汉市洪山区人民法院', '武汉市汉阳区人民法院',
    '西安市雁塔区人民法院', '西安市碑林区人民法院', '西安市莲湖区人民法院', '西安市新城人民法院',
    '重庆市渝中区人民法院', '重庆市江北区人民法院', '重庆市南岸区人民法院', '重庆市九龙坡区人民法院',
    '天津市和平区人民法院', '天津市河西区人民法院', '天津市南开区人民法院', '天津市河北区人民法院',
    '苏州市姑苏区人民法院', '苏州市虎丘区人民法院', '苏州市吴中区人民法院', '苏州市相城区人民法院',
    '青岛市市南区人民法院', '青岛市市北区人民法院', '青岛市崂山区人民法院', '青岛市李沧区人民法院',
    '大连市中山区人民法院', '大连市西岗区人民法院', '大连市沙河口区人民法院', '大连市甘井子区人民法院',
    '厦门市思明区人民法院', '厦门市湖里区人民法院', '厦门市海沧区人民法院', '厦门市集美区人民法院',
    '长沙市岳麓区人民法院', '长沙市芙蓉区人民法院', '长沙市天心区人民法院', '长沙市开福区人民法院',
    '济南市历下区人民法院', '济南市市中区人民法院', '济南市槐荫区人民法院', '济南市天桥区人民法院',
    '沈阳市和平区人民法院', '沈阳市沈河区人民法院', '沈阳市大东区人民法院', '沈阳市铁西区人民法院',
    '哈尔滨市南岗区人民法院', '哈尔滨市道里区人民法院', '哈尔滨市道外区人民法院', '哈尔滨市香坊区人民法院',
    '郑州市金水区人民法院', '郑州市中原区人民法院', '郑州市二七区人民法院', '郑州市管城回族区人民法院'
  ]
  courtList.value = majorCourts.filter(court => court.includes(query))
}

// 搜索客户
const searchClient = async (query) => {
  if (!query) return
  try {
    const response = await searchClients(query)
    if (response.success) {
      clientList.value = response.data.map(client => client.name || client.clientName)
    }
  } catch (error) {
    console.error('搜索客户失败:', error)
    // 降级到本地搜索
    clientList.value = [
      '张三',
      '李四',
      '某某科技有限公司'
    ].filter(client => client.includes(query))
  }
}

// 查重
const handleCheckDuplicate = async () => {
  if (!formData.caseName && !formData.caseNumber) return

  try {
    const res = await checkDuplicate({
      name: formData.caseName,
      caseNumber: formData.caseNumber
    })

    if (res.data && res.data.length > 0) {
      duplicateCases.value = res.data
      duplicateDialogVisible.value = true
    }
  } catch (error) {
    console.error('查重失败:', error)
  }
}

// AI智能填充
const handleAIFill = () => {
  aiFillDialogVisible.value = true
}

// 处理AI智能填充确认
const handleAIFillConfirm = (result) => {
  if (!result) return

  // 填充基本信息
  if (result.caseNumber) formData.caseNumber = result.caseNumber
  if (result.courtName) formData.court = result.courtName
  if (result.caseReason) formData.caseReason = result.caseReason
  if (result.hearingDate) {
    // 如果识别到开庭时间，自动设置为审限时间
    formData.deadlineDate = result.hearingDate
  }

  // 填充当事人信息
  if (result.plaintiffName || result.defendantName) {
    const parties = []

    if (result.plaintiffName) {
      parties.push({
        type: '个人',
        name: result.plaintiffName,
        isClient: false,
        attribute: '原告',
        phone: result.contactPhone || '',
        idCard: '',
        gender: '',
        nation: '',
        address: '',
        creditCode: '',
        legalRep: '',
        opposingLawyer: '',
        remark: '',
        syncToClient: false
      })
    }

    if (result.defendantName) {
      parties.push({
        type: '个人',
        name: result.defendantName,
        isClient: false,
        attribute: '被告',
        phone: result.contactPhone || '',
        idCard: '',
        gender: '',
        nation: '',
        address: '',
        creditCode: '',
        legalRep: '',
        opposingLawyer: '',
        remark: '',
        syncToClient: false
      })
    }

    if (parties.length > 0) {
      formData.parties = parties
    }
  }

  // 自动生成案件名称（如果为空）
  if (!formData.caseName && result.plaintiffName && result.defendantName) {
    formData.caseName = `${result.plaintiffName} Vs ${result.defendantName}`
  }

  ElMessage.success('信息已填充到表单，请核对后提交')
}

// 添加当事人
const handleAddParty = () => {
  formData.parties.push({
    type: '个人',
    name: '',
    isClient: false,
    attribute: '',
    phone: '',
    idCard: '',
    gender: '',
    nation: '',
    address: '',
    creditCode: '',
    legalRep: '',
    opposingLawyer: '',
    remark: '',
    syncToClient: false
  })
}

// 复制当事人
const handleCopyParty = (index) => {
  const party = formData.parties[index]
  formData.parties.splice(index + 1, 0, { ...party })
}

// 删除当事人
const handleDeleteParty = (index) => {
  formData.parties.splice(index, 1)
}

// 添加应收款
const handleAddReceivable = () => {
  formData.receivables.push({
    name: '',
    amount: null,
    dueDate: '',
    remark: ''
  })
}

// 删除应收款
const handleDeleteReceivable = (index) => {
  formData.receivables.splice(index, 1)
}

// ==================== 新增方法（对标行政管理要求）====================

// 验证分配比例总和
const validatePercentageSum = () => {
  const sum = percentageSum.value
  if (sum !== null && sum !== 100) {
    ElMessage.warning(`分配比例总和必须为100%，当前为${sum}%`)
  }
  return sum === 100
}

// 利益冲突检查
const handleConflictCheck = async () => {
  if (!formData.parties || formData.parties.length === 0) {
    ElMessage.warning('请先添加当事人')
    return
  }

  // 验证当事人基本信息
  for (let i = 0; i < formData.parties.length; i++) {
    const party = formData.parties[i]
    if (!party.name || party.name.trim() === '') {
      ElMessage.warning(`第${i + 1}个当事人：请输入姓名或单位名称`)
      return
    }
  }

  try {
    // 调用利益冲突检查API
    const response = await comprehensiveConflictCheck(formData.parties)
    if (response.success || response.code === 200) {
      conflictCheckResult.value = response.data

      if (response.data.hasConflict) {
        // 有冲突，显示对话框
        conflictDialogVisible.value = true
        // 更新冲突状态
        formData.conflictCheckStatus = 'CONFLICT'
      } else {
        // 无冲突
        ElMessage.success('利益冲突审查通过')
        formData.conflictCheckStatus = 'PASSED'
      }
    }
  } catch (error) {
    console.error('利益冲突检查失败:', error)
    ElMessage.error('利益冲突检查失败：' + (error.message || '未知错误'))
  }
}

// 申请利益冲突豁免
const handleApplyForWaiver = () => {
  ElMessageBox.confirm(
    '申请利益冲突豁免需要经过主任审批，是否继续？',
    '申请豁免',
    {
      confirmButtonText: '继续',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const partyNames = formData.parties.map((p) => p.name).filter(Boolean).join('、')
      const res = await createApproval({
        approvalType: 'OTHER',
        title: `利益冲突豁免 - ${formData.caseName || partyNames}`,
        content: `当事人：${partyNames}\n冲突摘要：${conflictCheckResult.value?.conflicts?.length || 0} 条\n请主任审批是否受理本案。`
      })
      if (res.code === 200 || res.success) {
        formData.conflictWaiverApprovalId = res.data?.id || null
        formData.conflictCheckStatus = 'WAIVER_PENDING'
        ElMessage.success('豁免申请已提交审批中心')
        conflictDialogVisible.value = false
      }
    } catch (e) {
      ElMessage.error(e.message || '提交豁免申请失败')
    }
  }).catch(() => {
    // 用户取消
  })
}


// 保存草稿 - 真正保存到后端数据库
const handleSaveDraft = async () => {
  try {
    // 验证基本必填项（比正式立案宽松）
    if (!formData.caseName || formData.caseName.trim() === '') {
      ElMessage.warning('请输入案件名称')
      return
    }
    if (!formData.caseType) {
      ElMessage.warning('请选择案件类型')
      return
    }

    ElMessageBox.confirm(
      '保存草稿将创建案件，状态为"咨询中"，是否继续？',
      '保存草稿',
      {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        type: 'info'
      }
    ).then(async () => {
      try {
        const requestData = transformToRequest()

        // 创建草稿案件（咨询状态）
        const response = await createCase(requestData)
        const caseData = response.data || response
        const caseId = caseData.id || caseData.data?.id

        if (!caseId) {
          throw new Error('保存草稿失败：未获取到案件ID')
        }

        ElMessage.success('草稿已保存到数据库')
        localStorage.removeItem('case_draft') // 清除本地草稿

        // 跳转到案件详情页
        setTimeout(() => {
          router.push({ name: 'CaseDetail', params: { id: caseId } })
        }, 1000)

      } catch (error) {
        console.error('保存草稿失败:', error)
        ElMessage.error('保存草稿失败：' + (error.message || '未知错误'))
      }
    }).catch(() => {
      // 用户取消
    })

  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败')
  }
}

// 提交表单 - 使用防重复提交hook
const handleSubmit = () => {
  handleFormSubmit()
}

// 提交立案审批：保存草稿 + 发起 CASE_FILING
const handleSubmitApproval = async () => {
  try {
    approving.value = true
    await formRef.value?.validate()
    if (!formData.parties || formData.parties.length === 0) {
      ElMessage.warning('请至少添加一个当事人')
      return
    }
    const sum = percentageSum.value
    if (sum !== null && sum !== 100) {
      ElMessage.error(`分配比例总和必须为100%，当前为${sum}%，请检查A3.分配情况`)
      return
    }
    if (formData.conflictCheckStatus === 'PENDING') {
      ElMessage.warning('请先完成利益冲突审查')
      return
    }
    if (formData.conflictCheckStatus === 'CONFLICT') {
      ElMessage.warning('存在利益冲突，请申请豁免或修改当事人后再提交审批')
      return
    }

    const requestData = { ...transformToRequest(), saveAsDraft: true, status: 'PENDING_FILING' }
    let savedCaseId = isEditMode.value ? caseId.value : null

    if (isEditMode.value) {
      await updateCase(savedCaseId, requestData)
    } else {
      const response = await createCase(requestData)
      const caseData = response.data || response
      savedCaseId = caseData.id || caseData.data?.id
      if (!savedCaseId) throw new Error('创建草稿失败：未获取到案件ID')
      await attachIntakeAfterCreate(savedCaseId)
    }

    await createApproval({
      approvalType: 'CASE_FILING',
      caseId: savedCaseId,
      title: `立案申请 - ${formData.caseName || '未命名案件'}`,
      description: formData.summary || '请审批后确认建案'
    })

    ElMessage.success('立案申请已提交，审批通过后可在此页确认建案')
    hasPendingFilingApproval.value = true
    canConfirmEstablishment.value = false
    filingBannerType.value = 'warning'
    filingDraftBanner.value = '立案审批进行中，审批通过后可点击「确认建案」。'
    router.push(`/case/${savedCaseId}/edit`)
  } catch (error) {
    console.error('提交审批失败:', error)
    ElMessage.error('提交审批失败：' + (error.message || '未知错误'))
  } finally {
    approving.value = false
  }
}

onMounted(async () => {
  await applyIntakePrefill()
  applyAiRecognitionPrefill()

  // 如果是编辑模式，加载案件数据
  if (isEditMode.value) {
    try {
      const response = await getCaseDetail(caseId.value)
      const caseData = response.data

      // 将后端数据转换为表单数据
      formData.caseType = caseData.caseType || ''
      formData.procedure = caseData.procedure || ''
      formData.caseName = caseData.caseName || ''
      formData.caseNumber = caseData.caseNumber || ''
      formData.caseReason = caseData.caseReason || ''
      formData.court = caseData.court || ''
      formData.amount = caseData.amount || null
      formData.attorneyFee = caseData.attorneyFee || null
      formData.filingDate = caseData.filingDate || null
      formData.deadlineDate = caseData.deadlineDate || null
      formData.summary = caseData.summary || ''
      formData.npaSubtype = caseData.npaSubtype || ''
      formData.entrustingBankName = caseData.entrustingBankName || ''
      formData.assetBatchNo = caseData.assetBatchNo || ''
      formData.transferAgreementNo = caseData.transferAgreementNo || ''
      formData.loanContractNo = caseData.loanContractNo || ''
      formData.principalBalance = caseData.principalBalance || null
      formData.interestBalance = caseData.interestBalance || null
      formData.guaranteeType = caseData.guaranteeType || ''
      formData.collateralStatus = caseData.collateralStatus || ''
      formData.preservationStatus = caseData.preservationStatus || ''
      formData.executionRecoveryAmount = caseData.executionRecoveryAmount || null
      formData.terminationStatus = caseData.terminationStatus || ''

      // 当事人数据转换
      if (caseData.parties && caseData.parties.length > 0) {
        formData.parties = caseData.parties.map(p => ({
          id: p.id,
          type: p.partyType === 'INDIVIDUAL' ? '个人' : '单位',
          attribute: p.partyRole,
          name: p.name,
          phone: p.phone,
          address: p.address
        }))
      }

      // 应收款数据
      if (caseData.receivables && caseData.receivables.length > 0) {
        formData.receivables = caseData.receivables
      }

      // 关联客户和案件
      if (caseData.relatedClients && caseData.relatedClients.length > 0) {
        formData.relatedClients = caseData.relatedClients
      }
      if (caseData.relatedCases && caseData.relatedCases.length > 0) {
        formData.relatedCases = caseData.relatedCases
      }

      // 结案/归档信息
      if (caseData.closeDate || caseData.archiveDate) {
        showArchiveInfo.value = true
        formData.closeDate = caseData.closeDate || null
        formData.archiveDate = caseData.archiveDate || null
      }

      if (caseData.conflictCheckStatus) {
        formData.conflictCheckStatus = caseData.conflictCheckStatus
      }
      if (caseData.conflictWaiverApprovalId) {
        formData.conflictWaiverApprovalId = caseData.conflictWaiverApprovalId
      }

      applyFilingMetaFromCase(caseData)

      if (route.query.action === 'confirmEstablishment') {
        ElMessage.success('立案审批已通过，请核对信息后确认建案')
      }
    } catch (error) {
      ElMessage.error('加载案件数据失败')
      router.push('/case/list')
    }
  }
})
</script>

<style scoped lang="scss">
.case-create {
  .filing-draft-banner {
    margin: 0 0 16px;
  }

  .create-container {
    background-color: #fff;
    padding: 30px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  .form-section {
    margin-bottom: 40px;
    padding-bottom: 30px;
    border-bottom: 1px dashed #e4e7ed;

    &:last-child {
      border-bottom: none;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: #333;
        border-left: 4px solid #1890ff;
        padding-left: 12px;
      }
    }

    .party-item,
    .receivable-item {
      background-color: #fafafa;
      padding: 20px;
      border-radius: 4px;
      margin-bottom: 15px;
      border: 1px solid #e4e7ed;

      .party-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
        padding-bottom: 10px;
        border-bottom: 1px solid #e4e7ed;
        font-weight: 500;
        color: #333;
      }

      &:last-child {
        margin-bottom: 0;
      }
    }

    .empty-tip {
      text-align: center;
      padding: 20px;
      background-color: #fafafa;
      border-radius: 4px;
    }
  }

  :deep(.el-form-item__label) {
    font-weight: 400;
    color: #606266;
  }

  :deep(.el-input-number) {
    width: 100%;
  }

  .upload-demo {
    margin-bottom: 20px;
  }

  .field-hint {
    font-size: 12px;
    color: #909399;
    line-height: 1.4;
    margin-top: 4px;
  }

  .ai-result {
    margin-top: 20px;
    padding: 15px;
    background-color: #f5f7fa;
    border-radius: 4px;

    h4 {
      margin: 0 0 10px;
      font-size: 14px;
      color: #333;
    }

    pre {
      margin: 0;
      font-size: 12px;
      color: #666;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
  }
}
</style>
