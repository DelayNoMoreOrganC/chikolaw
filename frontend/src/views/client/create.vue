<template>
  <div class="client-create">
    <PageHeader title="新建客户" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button type="warning" :loading="checkingConflict" @click="handleConflictCheck">
          利益冲突审查
        </el-button>
        <el-button @click="handleCancel">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="conflictStatus === 'CONFLICT'"
          @click="handleSubmit"
        >
          提交
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="conflictStatus === 'PASSED'"
      type="success"
      :closable="false"
      title="利冲审查已通过，可提交新建客户"
      style="margin: 0 20px 12px"
    />
    <el-alert
      v-else-if="conflictStatus === 'CONFLICT'"
      type="error"
      :closable="false"
      title="存在利益冲突，须申请豁免审批后方可继续"
      style="margin: 0 20px 12px"
    />
    <el-alert
      v-else
      type="info"
      :closable="false"
      title="请先完成利益冲突审查（行政要求：无冲突方可进入立案流程）"
      style="margin: 0 20px 12px"
    />

    <div class="create-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="130px"
        class="client-form"
      >
        <div class="form-section">
          <div class="section-header">
            <h3>基本信息（行政必填）</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户类型" prop="clientType">
                <el-select v-model="formData.clientType" placeholder="请选择" @change="handleTypeChange">
                  <el-option label="个人" value="INDIVIDUAL" />
                  <el-option label="企业" value="ENTERPRISE" />
                  <el-option label="金融机构" value="FINANCIAL" />
                  <el-option label="事业单位" value="INSTITUTION" />
                  <el-option label="党政机关" value="GOVERNMENT" />
                  <el-option label="社会团体" value="SOCIAL_ORG" />
                  <el-option label="其他" value="OTHER" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="客户角色" prop="clientRole">
                <el-select v-model="formData.clientRole" placeholder="请选择">
                  <el-option label="委托人" value="ENTRUSTED" />
                  <el-option label="当事人" value="PARTY" />
                  <el-option label="对方当事人" value="OPPOSING" />
                  <el-option label="第三人" value="THIRD_PARTY" />
                  <el-option label="顾问单位" value="ADVISORY_UNIT" />
                  <el-option label="其他同案人" value="OTHER_PARTY" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="isPersonal ? '客户姓名' : '客户名称'" prop="clientName">
            <el-input
              v-model="formData.clientName"
              :placeholder="isPersonal ? '请输入客户姓名' : '请输入客户名称'"
              maxlength="100"
              show-word-limit
              @blur="onNameBlur"
            />
          </el-form-item>

          <el-row :gutter="20" v-if="isEntrustedRole">
            <el-col :span="8">
              <el-form-item label="案源人" prop="sourcePerson">
                <el-input v-model="formData.sourcePerson" placeholder="委托人必填" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="所属部门" prop="ownerDepartment">
                <el-input v-model="formData.ownerDepartment" placeholder="委托人必填" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="客户所属人" prop="ownerName">
                <el-input v-model="formData.ownerName" placeholder="委托人必填" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20" v-if="isPersonal">
            <el-col :span="12">
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="formData.gender">
                  <el-radio label="男">男</el-radio>
                  <el-radio label="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="身份证号" prop="idCard">
                <el-input v-model="formData.idCard" placeholder="个人客户必填" maxlength="18" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="统一社会信用代码" prop="creditCode" v-if="!isPersonal">
            <el-input v-model="formData.creditCode" placeholder="单位客户可填" maxlength="18" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系人">
                <el-input v-model="formData.contactName" placeholder="可选" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="phone">
                <el-input v-model="formData.phone" placeholder="可选" maxlength="20" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="邮箱">
                <el-input v-model="formData.email" placeholder="可选" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="微信号">
                <el-input v-model="formData.wechat" placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="地址">
            <el-input v-model="formData.address" type="textarea" :rows="2" placeholder="可选" />
          </el-form-item>

          <template v-if="!isPersonal">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="法人姓名">
                  <el-input v-model="formData.legalRepresentative" placeholder="可选" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="法人身份证号">
                  <el-input v-model="formData.legalRepIdCard" placeholder="可选" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <el-divider content-position="left">开票信息（可选）</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="购方名称">
                <el-input v-model="formData.invoiceTitle" placeholder="开票购方名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="税号">
                <el-input v-model="formData.invoiceTaxNo" placeholder="纳税人识别号" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="地址电话">
                <el-input v-model="formData.invoiceAddressPhone" placeholder="地址和电话" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="开户行及账号">
                <el-input v-model="formData.invoiceBankAccount" placeholder="开户行及账号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="备注">
            <el-input v-model="formData.notes" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createClient, checkClientNameConflict } from '@/api/client'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const checkingConflict = ref(false)
const conflictStatus = ref('PENDING')

const formData = reactive({
  clientType: 'INDIVIDUAL',
  clientRole: 'ENTRUSTED',
  clientName: '',
  sourcePerson: '',
  ownerDepartment: '',
  ownerName: '',
  gender: '男',
  idCard: '',
  creditCode: '',
  contactName: '',
  phone: '',
  email: '',
  wechat: '',
  address: '',
  legalRepresentative: '',
  legalRepIdCard: '',
  invoiceTitle: '',
  invoiceTaxNo: '',
  invoiceAddressPhone: '',
  invoiceBankAccount: '',
  notes: ''
})

const isPersonal = computed(() => formData.clientType === 'INDIVIDUAL')
const isEntrustedRole = computed(() => formData.clientRole === 'ENTRUSTED')

const formRules = computed(() => ({
  clientType: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  clientRole: [{ required: true, message: '请选择客户角色', trigger: 'change' }],
  clientName: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度 2-100 个字符', trigger: 'blur' }
  ],
  sourcePerson: isEntrustedRole.value
    ? [{ required: true, message: '委托人类型须填写案源人', trigger: 'blur' }]
    : [],
  ownerDepartment: isEntrustedRole.value
    ? [{ required: true, message: '委托人类型须填写所属部门', trigger: 'blur' }]
    : [],
  ownerName: isEntrustedRole.value
    ? [{ required: true, message: '委托人类型须填写客户所属人', trigger: 'blur' }]
    : [],
  idCard: isPersonal.value
    ? [
        { required: true, message: '个人客户须填写身份证号', trigger: 'blur' },
        { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证号格式不正确', trigger: 'blur' }
      ]
    : [],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}))

const handleTypeChange = () => {
  if (!isPersonal.value) {
    formData.idCard = ''
  }
}

const onNameBlur = () => {
  if (formData.clientName?.trim() && conflictStatus.value === 'PENDING') {
    handleConflictCheck()
  }
}

const handleConflictCheck = async () => {
  if (!formData.clientName?.trim()) {
    ElMessage.warning('请先填写客户名称')
    return
  }
  checkingConflict.value = true
  try {
    const res = await checkClientNameConflict(formData.clientName.trim())
    const data = res.data || {}
    if (data.hasConflict) {
      conflictStatus.value = 'CONFLICT'
      const names = (data.conflicts || []).map((c) => c.relatedName).filter(Boolean)
      const hint = names.length
        ? `发现冲突或高度相似客户：${names.join('、')}`
        : '存在利益冲突，请修改名称或申请豁免'
      ElMessage.error(hint)
    } else {
      conflictStatus.value = 'PASSED'
      ElMessage.success('未发现利益冲突，可提交新建')
    }
  } catch (e) {
    ElMessage.error(e.message || '利冲检查失败')
  } finally {
    checkingConflict.value = false
  }
}

const buildPayload = () => {
  const notesParts = [
    formData.notes,
    formData.clientRole ? `角色:${formData.clientRole}` : '',
    formData.sourcePerson ? `案源人:${formData.sourcePerson}` : '',
    formData.ownerDepartment ? `部门:${formData.ownerDepartment}` : '',
    formData.wechat ? `微信:${formData.wechat}` : '',
    formData.invoiceTitle ? `开票:${formData.invoiceTitle}/${formData.invoiceTaxNo || ''}` : ''
  ].filter(Boolean)
  return {
    clientType: formData.clientType,
    clientName: formData.clientName.trim(),
    gender: formData.gender,
    idCard: formData.idCard || null,
    creditCode: formData.creditCode || null,
    phone: formData.phone || null,
    email: formData.email || null,
    address: formData.address || null,
    legalRepresentative: formData.legalRepresentative || formData.contactName || null,
    notes: notesParts.join('；'),
    source: formData.sourcePerson || null,
    ownerName: formData.ownerName || null
  }
}

const handleSubmit = async () => {
  if (conflictStatus.value !== 'PASSED') {
    ElMessage.warning('请先完成利益冲突审查且无冲突后再提交')
    return
  }
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const res = await createClient(buildPayload())
    if (res.code === 200) {
      ElMessage.success('客户创建成功')
      router.push('/client/list')
    } else {
      ElMessage.error(res.message || '客户创建失败')
    }
  } catch (error) {
    console.error('客户创建失败:', error)
    ElMessage.error(error.message || '客户创建失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => router.back()
</script>

<style scoped lang="scss">
.client-create {
  .create-container {
    padding: 20px;
    background: #fff;
    margin: 20px;
    border-radius: 4px;
  }

  .form-section .section-header h3 {
    margin: 0 0 16px;
    font-size: 16px;
    color: #303133;
  }
}
</style>
