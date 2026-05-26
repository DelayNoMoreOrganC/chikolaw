/**
 * 行政《系统行政管理要求》+《系统问题.xlsx》立案字段配置
 * 五类立案：民事、刑事、行政、非诉、顾问
 */

export const ADMIN_CASE_TYPES = [
  { label: '民事', value: 'CIVIL', group: '立案大类（行政表1）' },
  { label: '刑事', value: 'CRIMINAL', group: '立案大类（行政表1）' },
  { label: '行政', value: 'ADMINISTRATIVE', group: '立案大类（行政表1）' },
  { label: '非诉', value: 'NON_LITIGATION', group: '立案大类（行政表1）' },
  { label: '顾问', value: 'ADVISORY', group: '立案大类（行政表1）' },
  { label: '商事', value: 'COMMERCIAL', group: '扩展' },
  { label: '仲裁', value: 'ARBITRATION', group: '扩展' },
  { label: '金融不良资产', value: 'FINANCIAL_NPA', group: '扩展' }
]

export const CASE_NUMBER_TEMPLATES = {
  CIVIL: '[年]粤至高民字第XXX号',
  CRIMINAL: '[年]粤至高刑字第XXX号',
  ADMINISTRATIVE: '[年]粤至高行字第XXX号',
  NON_LITIGATION: '[年]粤至高非字第XXX号',
  ADVISORY: '[年]粤至高顾字第XXX号',
  COMMERCIAL: '[年]粤至高民字第XXX号',
  ARBITRATION: '[年]粤至高民字第XXX号',
  FINANCIAL_NPA: '[年]粤至高民字第XXX号'
}

const F = {
  acceptanceDate: 'acceptanceDate',
  court: 'court',
  courtCaseNumber: 'courtCaseNumber',
  hearingDate: 'hearingDate',
  criminalSuspect: 'criminalSuspect',
  representationType: 'representationType',
  contractStart: 'contractStartDate',
  contractEnd: 'contractEndDate',
  disputedAmount: 'disputedAmount',
  subjectMatter: 'subjectMatter',
  businessType: 'businessType',
  procedureLevels: 'procedureLevels',
  allocation: 'allocation'
}

function def(overrides) {
  return {
    businessTypes: [],
    procedureLevels: [],
    feeMethods: ['固定', '免费代理', '未确定'],
    partyAttributes: ['委托人', '当事人', '对方当事人', '第三人'],
    requireOpposingParty: true,
    fields: {},
    caseNameRule: 'litigation',
    documentTemplates: [],
    ...overrides
  }
}

export const CASE_TYPE_SCHEMA = {
  CIVIL: def({
    businessTypes: [
      '婚姻家庭', '公司', '金融', '证券', '保险', '海事海商', '建设工程', '劳动', '知识产权'
    ],
    procedureLevels: [
      '仲裁', '一审', '二审', '执行', '再审', '重审一审', '重审二审', '特别程序', '破产程序'
    ],
    feeMethods: ['固定', '风险', '固定+风险', '免费代理', '未确定'],
    fields: {
      [F.acceptanceDate]: true,
      [F.court]: true,
      [F.courtCaseNumber]: true,
      [F.hearingDate]: true,
      [F.disputedAmount]: true,
      [F.businessType]: true,
      [F.procedureLevels]: true,
      [F.allocation]: true
    },
    documentTemplates: [
      '立案审批表', '授权委托书', '民事委托代理合同', '民事所函',
      '律师办案质量监督卡+委托人须知', '律师所送达材料清单', '结案报告表', '业务档案卷宗（诉讼类）'
    ]
  }),
  CRIMINAL: def({
    businessTypes: [
      '一般代理', '当事人自行委托', '法律援助', '法定通知辩护',
      '扩大通知辩护', '刑事附带民事诉讼'
    ],
    procedureLevels: [
      '侦查', '审查起诉', '一审', '二审', '申诉', '再审', '重审一审', '重审二审'
    ],
    feeMethods: ['固定', '免费代理', '未确定'],
    partyAttributes: ['委托人', '当事人', '犯罪嫌疑人', '第三人', '原告', '被告'],
    fields: {
      [F.acceptanceDate]: true,
      [F.court]: true,
      [F.courtCaseNumber]: true,
      [F.hearingDate]: true,
      [F.criminalSuspect]: true,
      [F.representationType]: true,
      [F.businessType]: true,
      [F.procedureLevels]: true,
      [F.allocation]: true
    },
    caseNameRule: 'criminal',
    documentTemplates: [
      '立案审批表', '授权委托书', '刑事委托代理合同', '刑事所函',
      '律师办案质量监督卡+委托人须知', '律师所送达材料清单', '结案报告表', '业务档案卷宗（诉讼类）'
    ]
  }),
  ADMINISTRATIVE: def({
    businessTypes: ['一般代理/应诉', '行政申诉'],
    procedureLevels: [
      '行政复议', '行政裁决', '一审', '二审', '执行', '再审', '重审一审', '重审二审'
    ],
    feeMethods: ['固定', '免费代理', '未确定'],
    requireOpposingParty: false,
    fields: {
      [F.acceptanceDate]: true,
      [F.court]: true,
      [F.courtCaseNumber]: true,
      [F.hearingDate]: true,
      [F.businessType]: true,
      [F.procedureLevels]: true,
      [F.allocation]: true
    },
    documentTemplates: [
      '立案审批表', '授权委托书', '行政委托代理合同', '行政所函',
      '律师办案质量监督卡+委托人须知', '律师所送达材料清单', '结案报告表', '业务档案卷宗（诉讼类）'
    ]
  }),
  NON_LITIGATION: def({
    businessTypes: [
      '公司', '金融', '证券', '保险', '反垄断', '建设工程与房地产', '劳动', '知识产权', '税法'
    ],
    procedureLevels: [],
    feeMethods: ['固定', '风险', '固定+风险', '免费代理', '未确定'],
    requireOpposingParty: false,
    fields: {
      [F.acceptanceDate]: true,
      [F.subjectMatter]: true,
      [F.disputedAmount]: true,
      [F.businessType]: true,
      [F.allocation]: true
    },
    caseNameRule: 'nonLitigation',
    documentTemplates: [
      '立案审批表', '授权委托书', '非诉委托代理合同', '非诉所函',
      '律师办案质量监督卡+委托人须知', '律师所送达材料清单', '结案报告表', '业务档案卷宗（非诉讼类）'
    ]
  }),
  ADVISORY: def({
    businessTypes: ['常年法律顾问', '专项法律顾问'],
    procedureLevels: [],
    feeMethods: ['固定', '风险', '固定+风险', '免费代理', '未确定'],
    requireOpposingParty: false,
    fields: {
      [F.acceptanceDate]: true,
      [F.contractStart]: true,
      [F.contractEnd]: true,
      [F.businessType]: true,
      [F.allocation]: true
    },
    caseNameRule: 'advisory',
    documentTemplates: [
      '立案审批表', '顾问委托代理合同', '质量监督卡+委托人须知',
      '律师所送达材料清单', '结案报告表', '业务档案卷宗（非诉讼类）'
    ]
  }),
  COMMERCIAL: def({
    businessTypes: [
      '婚姻家庭', '公司', '金融', '证券', '保险', '海事海商', '建设工程', '劳动', '知识产权'
    ],
    procedureLevels: ['仲裁', '一审', '二审', '执行', '再审'],
    feeMethods: ['固定', '风险', '固定+风险', '免费代理', '未确定'],
    fields: {
      [F.acceptanceDate]: true,
      [F.court]: true,
      [F.courtCaseNumber]: true,
      [F.hearingDate]: true,
      [F.disputedAmount]: true,
      [F.procedureLevels]: true,
      [F.allocation]: true
    }
  }),
  ARBITRATION: def({
    businessTypes: ['公司', '金融', '建设工程', '劳动', '知识产权'],
    procedureLevels: ['仲裁', '一审', '二审', '执行'],
    feeMethods: ['固定', '风险', '固定+风险', '免费代理', '未确定'],
    fields: {
      [F.acceptanceDate]: true,
      [F.court]: true,
      [F.disputedAmount]: true,
      [F.procedureLevels]: true,
      [F.allocation]: true
    }
  }),
  FINANCIAL_NPA: def({
    businessTypes: ['金融'],
    procedureLevels: ['一审', '二审', '执行'],
    feeMethods: ['固定', '风险', '固定+风险', '免费代理', '未确定'],
    fields: {
      [F.acceptanceDate]: true,
      [F.disputedAmount]: true,
      [F.allocation]: true
    }
  })
}

export function getCaseSchema(caseType) {
  return CASE_TYPE_SCHEMA[caseType] || CASE_TYPE_SCHEMA.CIVIL
}

export function isAdminFieldVisible(caseType, fieldKey) {
  const schema = getCaseSchema(caseType)
  if (!caseType) return false
  if (Object.keys(schema.fields).length === 0) return true
  return schema.fields[fieldKey] === true
}

export function partyNamesByRoles(parties, roles) {
  const set = new Set(roles)
  return (parties || [])
    .filter((p) => set.has(p.attribute) || (set.has('委托人') && p.isClient))
    .map((p) => p.name)
    .filter(Boolean)
}

/** 按行政表1规则自动生成案件名称 */
export function generateAdminCaseName(formData) {
  const reason = formData.caseReason || ''
  const parties = formData.parties || []
  const schema = getCaseSchema(formData.caseType)

  if (schema.caseNameRule === 'criminal') {
    const suspect =
      formData.criminalSuspect ||
      partyNamesByRoles(parties, ['犯罪嫌疑人', '被告'])[0] ||
      ''
    const part = suspect && reason ? `${suspect}${reason}` : suspect || reason
    return part || ''
  }

  if (schema.caseNameRule === 'nonLitigation') {
    const ent = partyNamesByRoles(parties, ['委托人', '当事人']).join('、')
    return ent && reason ? `${ent}${reason}` : ent || reason || ''
  }

  if (schema.caseNameRule === 'advisory') {
    const ent = partyNamesByRoles(parties, ['委托人', '当事人']).join('、')
    const bt = formData.businessType || ''
    return ent && bt ? `${ent}${bt}` : ent || bt || ''
  }

  const ours = partyNamesByRoles(parties, ['委托人', '当事人']).join('、')
  const opp = partyNamesByRoles(parties, ['对方当事人']).join('、')
  if (ours && opp && reason) return `${ours}与${opp}${reason}`
  if (ours && reason) return `${ours}${reason}`
  return ''
}

export function isRiskFeeMethod(feeMethodChoice) {
  if (!feeMethodChoice) return false
  return feeMethodChoice.includes('风险')
}

/** 提交前校验（行政表1必填项） */
export function validateAdminCaseForm(formData) {
  const errors = []
  const schema = getCaseSchema(formData.caseType)

  if (!formData.caseType) errors.push('请选择案件类型')
  if (!formData.acceptanceDate) errors.push('请选择收案日期')
  if (!formData.caseReason) {
    errors.push('请填写或选择案由')
  }
  if (schema.fields[F.businessType] && !formData.businessType) {
    errors.push('请选择业务类型')
  }

  const entrusting = partyNamesByRoles(formData.parties, ['委托人'])
  const parties = partyNamesByRoles(formData.parties, ['当事人'])
  if (entrusting.length === 0 && parties.length === 0) {
    errors.push('请至少添加一名委托人或当事人')
  }
  if (schema.requireOpposingParty) {
    const opp = partyNamesByRoles(formData.parties, ['对方当事人'])
    if (opp.length === 0 && ['CIVIL', 'COMMERCIAL', 'ARBITRATION'].includes(formData.caseType)) {
      errors.push('请添加对方当事人')
    }
  }

  if (schema.fields[F.criminalSuspect] && !formData.criminalSuspect) {
    errors.push('请填写犯罪嫌疑人')
  }
  if (schema.fields[F.representationType] && !formData.representationType) {
    errors.push('请选择代理类型（刑事）')
  }
  if (schema.fields[F.contractStart] && !formData.contractStartDate) {
    errors.push('请选择合同服务开始时间')
  }
  if (schema.fields[F.contractEnd] && !formData.contractEndDate) {
    errors.push('请选择合同服务结束时间')
  }
  if (schema.fields[F.court] && !formData.court) {
    errors.push('请选择受理法院')
  }
  if (schema.fields[F.courtCaseNumber] && !formData.courtCaseNumber) {
    errors.push('请填写法院案号')
  }
  if (schema.fields[F.hearingDate] && !formData.hearingDate) {
    errors.push('请选择开庭日期')
  }
  if (schema.fields[F.procedureLevels] && (!formData.procedureLevels || formData.procedureLevels.length === 0)) {
    errors.push('请选择审级')
  }
  if (!formData.feeMethodChoice) {
    errors.push('请选择收费方式')
  }
  if (isRiskFeeMethod(formData.feeMethodChoice) && schema.fields[F.disputedAmount] && !formData.disputedAmount) {
    errors.push('收费方式为风险代理时，涉案标的（万元）为必填')
  }
  if (formData.feeMethodChoice === '免费代理' && !formData.feeRemark?.trim()) {
    errors.push('免费代理请在收费备注中说明理由（提交审批时须主任审批）')
  }

  const sum =
    (formData.sourcePersonPercentage || 0) +
    (formData.departmentPercentage || 0) +
    (formData.firmPercentage || 0)
  if (schema.fields[F.allocation] && Math.round(sum * 100) / 100 !== 100) {
    errors.push('案源人、承办部门、律所提留分配比例之和须为 100%')
  }

  return { ok: errors.length === 0, errors }
}

export const FEE_METHOD_BACKEND_MAP = {
  固定: 'FIXED',
  风险: 'CONTINGENT',
  '固定+风险': 'FIXED,CONTINGENT',
  免费代理: 'FREE',
  未确定: 'UNDETERMINED',
  定额: 'FIXED',
  风险代理: 'CONTINGENT',
  免费: 'FREE'
}

export const ADMIN_PARTY_ROLE_MAP = {
  委托人: 'CLIENT',
  当事人: 'PARTY',
  对方当事人: 'OPPOSING',
  犯罪嫌疑人: 'DEFENDANT',
  原告: 'PLAINTIFF',
  被告: 'DEFENDANT',
  第三人: 'THIRD_PARTY',
  共同原告: 'CO_PLAINTIFF',
  共同被告: 'CO_DEFENDANT',
  申请人: 'APPLICANT',
  被申请人: 'RESPONDENT',
  上诉人: 'APPELLANT',
  被上诉人: 'APPELLEE'
}
