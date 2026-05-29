<template>
  <div class="case-detail" v-loading="loading">
    <!-- 顶部固定区域 -->
    <div class="detail-header">
      <div class="header-left">
        <el-button circle @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="case-info">
          <h2 class="case-name">{{ caseDetail.caseName }}</h2>
          <span class="case-number">{{ caseDetail.caseNumber }}</span>
        </div>
      </div>

      <div class="header-right">
        <el-button @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button type="primary" @click="showAIAssistant">
          <el-icon><ChatDotRound /></el-icon>
          AI助手
        </el-button>
        <el-button :loading="aiAnalysisLoading" @click="handleAiCaseAnalysis">
          案件分析
        </el-button>
        <el-button @click="handleArchive">
          <el-icon><FolderOpened /></el-icon>
          归档
        </el-button>
        <el-dropdown @command="handleMoreAction">
          <el-button>
            更多
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="termination">终止委托申请</el-dropdown-item>
              <el-dropdown-item command="copy">复制案件</el-dropdown-item>
              <el-dropdown-item command="export">导出信息</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除案件</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

    <!-- AI助手对话框 -->
    <AIAssistant
      v-model:visible="aiAssistantVisible"
      :case-id="currentCaseId"
      :case-mode="true"
    />

    <el-dialog v-model="aiAnalysisVisible" title="AI 案件分析" width="640px">
      <div v-loading="aiAnalysisLoading" class="ai-analysis-body">
        <p v-if="aiAnalysisMeta" class="ai-analysis-meta">
          引擎：{{ aiAnalysisMeta.provider || '-' }} · 模式：{{ aiAnalysisMeta.mode || 'llm' }}
        </p>
        <pre v-if="aiAnalysisText" class="ai-analysis-text">{{ aiAnalysisText }}</pre>
        <el-empty v-else description="暂无分析结果" />
      </div>
    </el-dialog>
    </div>

    <!-- 进度条 -->
    <div class="progress-section">
      <div class="progress-bar">
        <div
          v-for="(stage, index) in caseStages"
          :key="stage.key"
          class="progress-item"
          :class="{
            'active': isStageActive(stage.key),
            'completed': isStageCompleted(stage.key),
            'current': isCurrentStage(stage.key)
          }"
          @click="handleStageClick(stage)"
        >
          <div class="stage-icon">
            <el-icon v-if="isStageCompleted(stage.key)"><Select /></el-icon>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <div class="stage-label">{{ stage.label }}</div>
          <div v-if="index < caseStages.length - 1" class="stage-line"></div>
        </div>
      </div>
      <div class="progress-info">
        <span>当前阶段：{{ caseDetail.currentStage }}</span>
        <el-button text type="primary" size="small" @click="handleUpdateStage">
          更新阶段
        </el-button>
        <el-button text type="success" size="small" @click="handleViewApprovals">
          查看审批
        </el-button>
      </div>
    </div>

    <!-- Tab导航 -->
    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="基本案情" name="basic" />
      <el-tab-pane label="办案记录" name="record" />
      <el-tab-pane label="受理单位" name="unit" />
      <el-tab-pane label="案件文档" name="doc" />
      <el-tab-pane label="案件动态" name="timeline" />
    </el-tabs>

    <!-- Tab内容 -->
    <div class="tab-content route-transition-host">
      <router-view v-slot="{ Component, route: tabRoute }">
        <transition name="route-soft" mode="default">
          <component
            :is="Component"
            :key="tabRoute.name"
            class="route-page-root"
            :case-data="caseDetail"
            @refresh="fetchCaseDetail"
          />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Edit, FolderOpened, ArrowDown, Select, ChatDotRound
} from '@element-plus/icons-vue'
import { getCaseDetail, updateCaseStatus, rollbackCaseStatus, archiveCase, deleteCase, analyzeCaseById } from '@/api/case'
import { getStagesByCaseType } from '@/config/case-lifecycle'
import AIAssistant from '@/views/ai/assistant.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('basic')
const aiAssistantVisible = ref(false)
const aiAnalysisVisible = ref(false)
const aiAnalysisLoading = ref(false)
const aiAnalysisText = ref('')
const aiAnalysisMeta = ref(null)
const caseDetail = reactive({
  id: '',
  caseName: '',
  caseNumber: '',
  currentStage: '咨询',
  caseType: '',
  level: '',
  ownerId: '',
  stageProgress: []
})

// 优先使用后端 stageProgress，与流程模板一致
const caseStages = computed(() => {
  if (caseDetail.stageProgress?.length) {
    return caseDetail.stageProgress.map((s, index) => ({
      key: `stage_${s.stageOrder ?? index}`,
      label: s.stageName,
      order: s.stageOrder ?? index + 1,
      status: s.status
    }))
  }
  return getStagesByCaseType(caseDetail.caseType)
})

const currentStageIndex = computed(() =>
  caseStages.value.findIndex(s => s.label === caseDetail.currentStage)
)

const isStageCompleted = (stageKey) => {
  const stage = caseStages.value.find(s => s.key === stageKey)
  if (stage?.status) {
    return stage.status === 'COMPLETED'
  }
  const stageIndex = caseStages.value.findIndex(s => s.key === stageKey)
  return stageIndex >= 0 && stageIndex < currentStageIndex.value
}

// 判断阶段是否已激活（已完成或当前）
const isStageActive = (stageKey) => {
  return isStageCompleted(stageKey) || isCurrentStage(stageKey)
}

// 判断是否为当前阶段
const isCurrentStage = (stageKey) => {
  const stage = caseStages.value.find(s => s.key === stageKey)
  return stage?.label === caseDetail.currentStage
}

// 获取案件详情
const fetchCaseDetail = async () => {
  try {
    loading.value = true
    const { id } = route.params
    const res = await getCaseDetail(id)
    Object.assign(caseDetail, res.data)
  } catch (error) {
    ElMessage.error('获取案件详情失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 编辑案件
const handleEdit = () => {
  router.push(`/case/${caseDetail.id}/edit`)
}

// 显示AI助手
const showAIAssistant = () => {
  aiAssistantVisible.value = true
}

const handleAiCaseAnalysis = async () => {
  if (!caseDetail.id) return
  aiAnalysisVisible.value = true
  aiAnalysisLoading.value = true
  aiAnalysisText.value = ''
  aiAnalysisMeta.value = null
  try {
    const res = await analyzeCaseById(caseDetail.id)
    const data = res.data || {}
    aiAnalysisText.value = data.analysis || ''
    aiAnalysisMeta.value = { provider: data.provider, mode: data.mode }
  } catch (e) {
    ElMessage.error(e.message || '案件分析失败')
    aiAnalysisVisible.value = false
  } finally {
    aiAnalysisLoading.value = false
  }
}

// 当前案件ID（传递给AI助手）
const currentCaseId = computed(() => caseDetail.id)

// 归档案件
const handleArchive = async () => {
  try {
    await ElMessageBox.confirm('确定要归档该案件吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await archiveCase(caseDetail.id)
    ElMessage.success('归档成功')
    fetchCaseDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('归档失败')
    }
  }
}

// 查看审批流程
const handleViewApprovals = () => {
  router.push({
    path: '/approval',
    query: { caseId: caseDetail.id }
  })
}

// 更多操作
const handleMoreAction = async (command) => {
  switch (command) {
    case 'termination': {
      try {
        const { value } = await ElMessageBox.prompt(
          '请填写终止委托理由及情况说明（行政表2）',
          '案件终止委托申请',
          { confirmButtonText: '提交审批', inputType: 'textarea', inputPlaceholder: '委托终止理由...' }
        )
        const { createApproval } = await import('@/api/approval')
        await createApproval({
          approvalType: 'CASE_TERMINATION',
          title: `终止委托 - ${caseDetail.value.caseName || caseDetail.value.caseNumber}`,
          content: value || '',
          caseId: caseDetail.value.id
        })
        ElMessage.success('终止委托申请已提交审批中心')
      } catch (e) {
        if (e !== 'cancel') ElMessage.error(e.message || '提交失败')
      }
      break
    }
    case 'copy':
      try {
        await ElMessageBox.confirm(
          '复制案件将创建一个新案件，包含当前案件的所有信息。是否继续？',
          '复制案件',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        // 获取当前案件详细信息
        const caseDetail = await getCaseDetail(caseData.value.id)

        // 构造新案件数据
        const newCaseData = {
          ...caseDetail.data,
          id: undefined, // 移除ID，让系统生成新的
          caseNumber: `${caseDetail.data.caseNumber}-副本`,
          caseName: `${caseDetail.data.caseName}（副本）`,
          status: 'CONSULTATION', // 重置状态为咨询
          createdAt: undefined,
          updatedAt: undefined
        }

        // 创建新案件
        const newCase = await createCase(newCaseData)

        ElMessage.success('案件复制成功')
        router.push(`/case/${newCase.data.id}`)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('复制案件失败:', error)
          ElMessage.error('复制失败')
        }
      }
      break
    case 'export':
      try {
        await ElMessageBox.confirm(
          '导出案件信息将生成包含案件详细信息的文档，是否继续？',
          '导出案件',
          {
            confirmButtonText: '确定导出',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        // 生成案件信息文本内容
        const caseText = `
案件信息
==================
案件编号：${caseDetail.value.caseNumber || '无'}
案件名称：${caseDetail.value.caseName || '无'}
案件类型：${caseDetail.value.caseType || '无'}
案件程序：${caseDetail.value.procedure || '无'}
案由：${caseDetail.value.caseReason || '无'}
管辖法院：${caseDetail.value.court || '无'}
立案日期：${caseDetail.value.filingDate || '无'}
结案日期：${caseDetail.value.deadlineDate || '无'}
委托日期：${caseDetail.value.commissionDate || '无'}
案件等级：${caseDetail.value.level || '无'}
案件状态：${caseDetail.value.status || '无'}

案件摘要
------------------
${caseDetail.value.summary || '无'}

主办律师：${caseDetail.value.ownerName || '无'}
协办律师：${caseDetail.value.coOwners?.map(o => o.name).join('、') || '无'}
律师助理：${caseDetail.value.assistants?.map(a => a.name).join('、') || '无'}

当事人信息
------------------
${caseDetail.value.parties?.map(p => `
${p.attribute} - ${p.name}
  类型：${p.type}
  联系电话：${p.phone || '无'}
  身份证号/信用代码：${p.idCard || p.creditCode || '无'}
  地址：${p.address || '无'}
`).join('') || '无'}

律师费信息
------------------
收费方式：${caseDetail.value.feeTypes?.join('、') || '无'}
争议标的：${caseDetail.value.subjectMatter || '无'}
律师费金额：${caseDetail.value.lawyerFee || '无'}
收费摘要：${caseDetail.value.feeSummary || '无'}
收费备注：${caseDetail.value.feeRemark || '无'}

应收款信息
------------------
${caseDetail.value.receivables?.map(r => `
  ${r.name}：${r.amount}元
  应收日期：${r.dueDate || '无'}
  备注：${r.notes || '无'}
`).join('') || '无'}

备注
------------------
${caseDetail.value.remark || '无'}
        `.trim()

        // 创建Blob并下载
        const blob = new Blob([caseText], { type: 'text/plain;charset=utf-8' })
        const link = document.createElement('a')
        link.href = window.URL.createObjectURL(blob)
        link.download = `案件_${caseDetail.value.caseNumber}_${caseDetail.value.caseName}.txt`
        link.click()
        window.URL.revokeObjectURL(link.href)

        ElMessage.success('导出成功')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('导出案件失败:', error)
          ElMessage.error('导出失败')
        }
      }
      break
    case 'delete':
      try {
        await ElMessageBox.confirm('确定要删除该案件吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteCase(caseDetail.id)
        ElMessage.success('删除成功')
        router.push('/case/list')
      } catch {
        // 用户取消
      }
      break
  }
}

// 点击阶段
const handleStageClick = async (stage) => {
  try {
    const currentStageKey = caseStages.value.find(s => s.label === caseDetail.currentStage)?.key

    // 检查是否是当前阶段
    if (stage.key === currentStageKey) {
      ElMessage.info('当前已是该阶段')
      return
    }

    // 检查是前进还是回退
    const currentStageIndex = caseStages.value.findIndex(s => s.key === currentStageKey)
    const targetStageIndex = caseStages.value.findIndex(s => s.key === stage.key)
    const isRollback = targetStageIndex < currentStageIndex

    let confirmMessage = `确定要将案件阶段从"${caseDetail.currentStage}"变更为"${stage.label}"吗？`

    // 如果是回退，需要填写原因
    if (isRollback) {
      try {
        const { value } = await ElMessageBox.prompt(
          `回退阶段需要填写原因\n确定要将案件从"${caseDetail.currentStage}"回退到"${stage.label}"吗？`,
          '回退确认',
          {
            confirmButtonText: '确定回退',
            cancelButtonText: '取消',
            inputPlaceholder: '请输入回退原因',
            inputPattern: /^.{2,100}$/,
            inputErrorMessage: '回退原因长度为2-100个字符'
          }
        )

        confirmMessage = `确定要回退到"${stage.label}"吗？原因：${value}`

        await ElMessageBox.confirm(confirmMessage, '确认回退', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        // 调用更新状态API（带原因）
        await rollbackCaseStatus(caseDetail.id, {
          status: stage.label,
          reason: value
        })

        ElMessage.success(`已回退到「${stage.label}」阶段`)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('回退失败:', error)
          ElMessage.error('回退失败')
        }
        return
      }
    } else {
      // 前进阶段，只需确认
      await ElMessageBox.confirm(confirmMessage, '确认变更', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      })

      // 调用更新状态API
      await updateCaseStatus(caseDetail.id, {
        status: stage.label
      })

      ElMessage.success(`已更新到「${stage.label}」阶段（待办由系统自动生成）`)
    }

    // 刷新案件详情
    await fetchCaseDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新阶段失败:', error)
      ElMessage.error('更新阶段失败')
    }
  }
}

// 更新阶段（按钮触发，显示阶段选择）
const handleUpdateStage = async () => {
  try {
    const validStages = caseStages.value.map(s => s.label)
    const { value } = await ElMessageBox.prompt(
      `请输入要变更到的阶段（${validStages.join('、')}）`,
      '更新案件阶段',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: caseDetail.currentStage,
        inputValue: caseDetail.currentStage
      }
    )

    // 验证输入的阶段是否有效
    if (!validStages.includes(value)) {
      ElMessage.error(`无效的阶段，请输入：${validStages.join('、')}`)
      return
    }

    const stage = caseStages.value.find(s => s.label === value)
    if (stage) {
      await handleStageClick(stage)
    }
  } catch (error) {
    // 用户取消
  }
}

// 监听路由变化，同步tab
watch(() => route.path, (newPath) => {
  const tabMap = {
    '/case/:id/basic': 'basic',
    '/case/:id/record': 'record',
    '/case/:id/unit': 'unit',
    '/case/:id/doc': 'doc',
    '/case/:id/timeline': 'timeline'
  }

  for (const [path, tab] of Object.entries(tabMap)) {
    if (newPath.match(path.replace(':id', '\\d+'))) {
      activeTab.value = tab
      break
    }
  }
})

// 监听tab变化，同步路由
watch(activeTab, (newTab) => {
  router.push(`/case/${caseDetail.id}/${newTab}`)
})

onMounted(() => {
  fetchCaseDetail()
})
</script>

<style scoped lang="scss">
.case-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    background-color: #fff;
    border-radius: 4px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .header-left {
      display: flex;
      align-items: center;
      gap: 20px;

      .case-info {
        .case-name {
          margin: 0 0 5px;
          font-size: 18px;
          font-weight: 500;
          color: #333;
        }

        .case-number {
          font-size: 14px;
          color: #999;
        }
      }
    }

    .header-right {
      display: flex;
      gap: 10px;
    }
  }

  .progress-section {
    background-color: #fff;
    padding: 25px 30px;
    border-radius: 4px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .progress-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      .progress-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        position: relative;
        cursor: pointer;

        .stage-icon {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background-color: #e4e7ed;
          color: #909399;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
          font-weight: 500;
          transition: all 0.3s;
          z-index: 2;
        }

        .stage-label {
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
          transition: all 0.3s;
        }

        .stage-line {
          position: absolute;
          top: 16px;
          left: 50%;
          width: 100%;
          height: 2px;
          background-color: #e4e7ed;
          z-index: 1;
        }

        &.active {
          .stage-icon {
            background-color: #1890ff;
            color: #fff;
          }

          .stage-label {
            color: #1890ff;
          }

          .stage-line {
            background-color: #1890ff;
          }
        }

        &.completed {
          .stage-icon {
            background-color: #67c23a;
            color: #fff;
          }

          .stage-label {
            color: #67c23a;
          }

          .stage-line {
            background-color: #67c23a;
          }
        }

        &.current {
          .stage-icon {
            background-color: #1890ff;
            color: #fff;
            box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2);
            animation: pulse 2s infinite;
          }

          .stage-label {
            color: #1890ff;
            font-weight: 500;
          }
        }
      }
    }

    .progress-info {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 15px;
      padding-top: 15px;
      border-top: 1px solid #e4e7ed;
      font-size: 14px;
      color: #606266;
    }
  }

  .ai-analysis-body {
    min-height: 120px;
    .ai-analysis-meta {
      font-size: 12px;
      color: #909399;
      margin-bottom: 12px;
    }
    .ai-analysis-text {
      white-space: pre-wrap;
      word-break: break-word;
      font-size: 14px;
      line-height: 1.6;
      margin: 0;
    }
  }

  .detail-tabs {
    background-color: #fff;
    padding: 0 20px;
    border-radius: 4px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    :deep(.el-tabs__header) {
      margin: 0;
    }

    :deep(.el-tabs__nav-wrap::after) {
      display: none;
    }
  }

  .tab-content {
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(24, 144, 255, 0.1);
  }
  100% {
    box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2);
  }
}
</style>
