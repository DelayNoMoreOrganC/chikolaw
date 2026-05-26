/**

 * 案件生命周期流程配置

 * 阶段 label 与后端 CaseFlowDefinitionService / PRD §3.1 保持一致

 */



const civilStages = [

  { key: 'consult', label: '咨询', order: 1 },

  { key: 'contract', label: '签约', order: 2 },

  { key: 'drafting', label: '起草文书', order: 3 },

  { key: 'pending_filing', label: '待立案', order: 4 },

  { key: 'filed', label: '已立案', order: 5 },

  { key: 'trial1', label: '一审审理中', order: 6 },

  { key: 'judgment1', label: '一审结案', order: 7 },

  { key: 'execution', label: '执行', order: 8 },

  { key: 'closed', label: '结案归档', order: 9 }

]



export const CASE_TYPE_WORKFLOWS = {

  CIVIL: {

    name: '民事案件',

    stages: civilStages

  },



  COMMERCIAL: {

    name: '商事案件',

    stages: civilStages

  },



  CRIMINAL: {

    name: '刑事案件',

    stages: [

      { key: 'consult', label: '咨询', order: 1 },

      { key: 'contract', label: '签约', order: 2 },

      { key: 'meeting', label: '会见', order: 3 },

      { key: 'investigation', label: '审查起诉', order: 4 },

      { key: 'trial1', label: '一审', order: 5 },

      { key: 'trial2', label: '二审', order: 6 },

      { key: 'closed', label: '结案归档', order: 7 }

    ]

  },



  ADMINISTRATIVE: {

    name: '行政案件',

    stages: [

      { key: 'consult', label: '咨询', order: 1 },

      { key: 'contract', label: '签约', order: 2 },

      { key: 'drafting', label: '起草文书', order: 3 },

      { key: 'pending_filing', label: '待立案', order: 4 },

      { key: 'filed', label: '已立案', order: 5 },

      { key: 'trial1', label: '一审', order: 6 },

      { key: 'trial2', label: '二审', order: 7 },

      { key: 'closed', label: '结案归档', order: 8 }

    ]

  },



  ARBITRATION: {

    name: '商事仲裁',

    stages: [

      { key: 'consult', label: '咨询', order: 1 },

      { key: 'contract', label: '签约', order: 2 },

      { key: 'drafting', label: '起草文书', order: 3 },

      { key: 'application', label: '申请仲裁', order: 4 },

      { key: 'tribunal', label: '组庭', order: 5 },

      { key: 'hearing', label: '开庭', order: 6 },

      { key: 'award', label: '裁决', order: 7 },

      { key: 'closed', label: '结案归档', order: 8 }

    ]

  },



  NON_LITIGATION: {

    name: '非诉案件',

    stages: [

      { key: 'consult', label: '咨询', order: 1 },

      { key: 'contract', label: '签约', order: 2 },

      { key: 'investigation', label: '尽职调查', order: 3 },

      { key: 'drafting', label: '出具文书', order: 4 },

      { key: 'delivery', label: '交付', order: 5 },

      { key: 'closed', label: '结案归档', order: 6 }

    ]

  },



  DEFAULT: {

    name: '默认流程',

    stages: [

      { key: 'consult', label: '咨询', order: 1 },

      { key: 'contract', label: '签约', order: 2 },

      { key: 'processing', label: '办理', order: 3 },

      { key: 'closed', label: '结案归档', order: 4 }

    ]

  }

}



/**

 * 根据案件类型获取流程配置

 */

export function getWorkflowByCaseType(caseType) {

  if (!caseType) return CASE_TYPE_WORKFLOWS.DEFAULT

  if (caseType === 'ADVISORY') return CASE_TYPE_WORKFLOWS.NON_LITIGATION

  return CASE_TYPE_WORKFLOWS[caseType] || CASE_TYPE_WORKFLOWS.DEFAULT

}



/**

 * 按阶段中文名查找（与后端 currentStage 对齐）

 */

export function findStageByLabel(caseType, stageLabel) {

  const stages = getStagesByCaseType(caseType)

  return stages.find((s) => s.label === stageLabel)

    || stages.find((s) => stageLabel && (s.label.includes(stageLabel) || stageLabel.includes(s.label)))

}



export function getStagesByCaseType(caseType) {

  const workflow = getWorkflowByCaseType(caseType)

  return workflow.stages || []

}



export function getStageInfo(caseType, stageKey) {

  const stages = getStagesByCaseType(caseType)

  return stages.find((s) => s.key === stageKey) || null

}



export function getStageAutoTodos(caseType, stageKey) {

  const stageInfo = getStageInfo(caseType, stageKey)

  return stageInfo?.autoTodos || []

}



export function calculateStageProgress(caseType, currentStage) {

  const stages = getStagesByCaseType(caseType)

  const byKey = stages.findIndex((s) => s.key === currentStage)

  if (byKey >= 0) return Math.round(((byKey + 1) / stages.length) * 100)

  const byLabel = stages.findIndex((s) => s.label === currentStage)

  if (byLabel >= 0) return Math.round(((byLabel + 1) / stages.length) * 100)

  return 0

}



export function getNextStage(caseType, currentStage) {

  const stages = getStagesByCaseType(caseType)

  let idx = stages.findIndex((s) => s.key === currentStage || s.label === currentStage)

  if (idx === -1 || idx === stages.length - 1) return null

  return stages[idx + 1]

}



export function canRollbackTo(caseType, currentStage, targetStage) {

  const stages = getStagesByCaseType(caseType)

  const resolveIndex = (name) => stages.findIndex((s) => s.key === name || s.label === name)

  const currentIndex = resolveIndex(currentStage)

  const targetIndex = resolveIndex(targetStage)

  return targetIndex >= 0 && targetIndex < currentIndex && targetIndex > 0

}



export function generateStageTodos(caseType, stageKey, caseId, caseName, assigneeId) {

  const autoTodos = getStageAutoTodos(caseType, stageKey)

  return autoTodos.map((title, index) => ({

    title: `[${caseName}] ${title}`,

    caseId,

    caseName,

    assigneeId,

    priority: index === 0 ? 'high' : 'medium',

    status: 'PENDING',

    dueDate: calculateDueDate(index),

    description: `案件"${caseName}"进入"${stageKey}"阶段，需要完成：${title}`

  }))

}



function calculateDueDate(daysFromNow) {

  const date = new Date()

  date.setDate(date.getDate() + daysFromNow + 1)

  return date.toISOString().split('T')[0]

}



export default {

  CASE_TYPE_WORKFLOWS,

  getWorkflowByCaseType,

  getStagesByCaseType,

  getStageInfo,

  findStageByLabel,

  getStageAutoTodos,

  calculateStageProgress,

  getNextStage,

  canRollbackTo,

  generateStageTodos

}


