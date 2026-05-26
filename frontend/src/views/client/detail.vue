<template>
  <div class="client-detail">
    <PageHeader :title="clientData.name" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button type="primary" @click="handleCreateCase">
          <el-icon><Plus /></el-icon>
          新建案件
        </el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" type="card" class="detail-tabs">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <div class="tab-content">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户类型">
              <el-tag :type="clientData.type === 'personal' ? 'primary' : 'success'">
                {{ clientData.type === 'personal' ? '个人' : '企业' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="客户等级">
              <el-tag v-if="clientData.level === 'vip'" type="danger">VIP</el-tag>
              <el-tag v-else-if="clientData.level === 'important'" type="warning">重要</el-tag>
              <el-tag v-else>普通</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ clientData.phone }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ clientData.email || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系地址" :span="2">
              {{ clientData.address || '-' }}
            </el-descriptions-item>

            <!-- 个人信息 -->
            <template v-if="clientData.type === 'personal'">
              <el-descriptions-item label="性别">
                {{ clientData.gender }}
              </el-descriptions-item>
              <el-descriptions-item label="身份证号">
                {{ clientData.idCard || '-' }}
              </el-descriptions-item>
            </template>

            <!-- 企业信息 -->
            <template v-if="clientData.type === 'company'">
              <el-descriptions-item label="统一社会信用代码" :span="2">
                {{ clientData.creditCode || '-' }}
              </el-descriptions-item>
            </template>

            <el-descriptions-item label="备注" :span="2">
              {{ clientData.remark || '-' }}
            </el-descriptions-item>

            <el-descriptions-item label="创建时间">
              {{ clientData.createTime }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ clientData.updateTime }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-tab-pane>

      <!-- 关联案件 -->
      <el-tab-pane name="cases">
        <template #label>
          关联案件 <el-badge :value="caseList.length" :hidden="caseList.length === 0" />
        </template>
        <div class="tab-content">
          <div v-if="caseList.length > 0">
            <el-table :data="caseList" border>
              <el-table-column prop="caseName" label="案件名称" width="200">
                <template #default="{ row }">
                  <el-link type="primary" @click="goToCase(row.id)">
                    {{ row.caseName }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="caseNumber" label="案号" width="150" />
              <el-table-column prop="caseType" label="案件类型" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.caseType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="标的额(元)" width="120">
                <template #default="{ row }">
                  ¥{{ (row.amount || 0).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="ownerName" label="主办律师" width="100" />
              <el-table-column prop="createTime" label="创建时间" width="160" />
            </el-table>
          </div>
          <el-empty v-else description="暂无关联案件" />
        </div>
      </el-tab-pane>

      <!-- 沟通记录 -->
      <el-tab-pane name="communications">
        <template #label>
          沟通记录 <el-badge :value="communicationList.length" :hidden="communicationList.length === 0" />
        </template>
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddCommunication">
              <el-icon><Plus /></el-icon>
              新建记录
            </el-button>
            <el-button @click="handleConflictCheck">
              <el-icon><Warning /></el-icon>
              利益冲突检索
            </el-button>
          </div>

          <el-timeline v-if="communicationList.length > 0" class="communication-timeline">
            <el-timeline-item
              v-for="record in communicationList"
              :key="record.id"
              :timestamp="record.communicationDate"
              placement="top"
            >
              <el-card class="communication-card" shadow="hover">
                <div class="communication-header">
                  <div class="communication-user">
                    <el-avatar :size="32" :src="record.userAvatar">
                      {{ record.userName?.charAt(0) }}
                    </el-avatar>
                    <div class="user-info">
                      <div class="user-name">{{ record.userName }}</div>
                      <div class="communication-way">{{ record.way }}</div>
                    </div>
                  </div>
                  <div class="communication-actions">
                    <el-button text type="primary" size="small" @click="handleEditCommunication(record)">
                      编辑
                    </el-button>
                    <el-button text type="danger" size="small" @click="handleDeleteCommunication(record)">
                      删除
                    </el-button>
                  </div>
                </div>
                <div class="communication-content">
                  {{ record.content }}
                </div>
                <div v-if="record.nextFollowUp" class="communication-followup">
                  <el-icon><Calendar /></el-icon>
                  下次跟进：{{ record.nextFollowUp }}
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无沟通记录" />
        </div>
      </el-tab-pane>

      <!-- 收费统计 -->
      <el-tab-pane label="收费统计" name="finance">
        <div class="tab-content">
          <div class="finance-cards">
            <div class="finance-card">
              <div class="card-label">案件总数</div>
              <div class="card-value">{{ financeStats.totalCases }}</div>
            </div>
            <div class="finance-card">
              <div class="card-label">应收总额</div>
              <div class="card-value">¥{{ (financeStats.totalReceivable || 0).toLocaleString() }}</div>
            </div>
            <div class="finance-card">
              <div class="card-label">已收总额</div>
              <div class="card-value income">¥{{ (financeStats.totalReceived || 0).toLocaleString() }}</div>
            </div>
            <div class="finance-card">
              <div class="card-label">待收总额</div>
              <div class="card-value pending">¥{{ ((financeStats.totalReceivable || 0) - (financeStats.totalReceived || 0)).toLocaleString() }}</div>
            </div>
          </div>

          <div class="finance-list">
            <h4>收费明细</h4>
            <el-table :data="financeList" border>
              <el-table-column prop="caseName" label="案件名称" width="200" />
              <el-table-column prop="caseNumber" label="案号" width="150" />
              <el-table-column prop="lawyerFee" label="律师费(元)" width="120">
                <template #default="{ row }">
                  ¥{{ (row.lawyerFee || 0).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="receivedAmount" label="已收(元)" width="120">
                <template #default="{ row }">
                  <span class="income">¥{{ (row.receivedAmount || 0).toLocaleString() }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="pendingAmount" label="待收(元)" width="120">
                <template #default="{ row }">
                  <span class="pending">¥{{ ((row.lawyerFee || 0) - (row.receivedAmount || 0)).toLocaleString() }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small">
                    {{ row.status === 'completed' ? '已结清' : '进行中' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建沟通记录对话框 -->
    <el-dialog
      v-model="communicationDialogVisible"
      :title="communicationForm.id ? '编辑沟通记录' : '新建沟通记录'"
      width="600px"
    >
      <el-form :model="communicationForm" label-width="100px">
        <el-form-item label="沟通方式" required>
          <el-select v-model="communicationForm.way" placeholder="请选择">
            <el-option label="电话" value="电话" />
            <el-option label="微信" value="微信" />
            <el-option label="邮件" value="邮件" />
            <el-option label="面谈" value="面谈" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="沟通内容" required>
          <el-input
            v-model="communicationForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入沟通内容"
          />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker
            v-model="communicationForm.nextFollowUp"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="communicationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCommunication">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Plus, Warning, Calendar } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getClientDetail,
  getClientCases,
  getCommunications,
  createCommunication,
  updateCommunication,
  deleteCommunication,
  checkConflict
} from '@/api/client'

const route = useRoute()
const router = useRouter()

const activeTab = ref('basic')
const loading = ref(false)
const clientId = route.params.id

const clientData = reactive({
  id: '',
  name: '',
  type: 'personal',
  level: 'normal',
  phone: '',
  email: '',
  address: '',
  gender: '',
  idCard: '',
  creditCode: '',
  remark: '',
  createTime: '',
  updateTime: ''
})

const caseList = ref([])
const communicationList = ref([])
const financeStats = reactive({
  totalCases: 0,
  totalReceivable: 0,
  totalReceived: 0
})
const financeList = ref([])

// 沟通记录对话框
const communicationDialogVisible = ref(false)
const communicationForm = reactive({
  id: null,
  way: '电话',
  content: '',
  nextFollowUp: ''
})

// 获取客户详情
const fetchClientDetail = async () => {
  try {
    loading.value = true
    const response = await getClientDetail(clientId)
    if (response.success) {
      Object.assign(clientData, response.data)
    }
  } catch (error) {
    ElMessage.error('获取客户详情失败')
  } finally {
    loading.value = false
  }
}

// 获取关联案件
const fetchClientCases = async () => {
  try {
    const response = await getClientCases(clientId)
    if (response.success) {
      caseList.value = response.data.list || []

      // 计算收费统计
      financeStats.totalCases = caseList.value.length
      financeList.value = caseList.value.map(caseItem => ({
        ...caseItem,
        lawyerFee: caseItem.lawyerFee || 0,
        receivedAmount: caseItem.receivedAmount || 0,
        pendingAmount: (caseItem.lawyerFee || 0) - (caseItem.receivedAmount || 0)
      }))

      financeStats.totalReceivable = financeList.value.reduce((sum, item) => sum + (item.lawyerFee || 0), 0)
      financeStats.totalReceived = financeList.value.reduce((sum, item) => sum + (item.receivedAmount || 0), 0)
    }
  } catch (error) {
    console.error('获取关联案件失败:', error)
  }
}

// 获取沟通记录
const fetchCommunications = async () => {
  try {
    const response = await getCommunications(clientId, {
      pageSize: 100
    })
    if (response.success) {
      communicationList.value = response.data.list || []
    }
  } catch (error) {
    console.error('获取沟通记录失败:', error)
  }
}

// 编辑客户
const handleEdit = () => {
  router.push(`/client/${clientId}/edit`)
}

// 新建案件
const handleCreateCase = () => {
  router.push({
    path: '/case/create',
    query: { clientId, clientName: clientData.name }
  })
}

// 跳转到案件详情
const goToCase = (caseId) => {
  router.push(`/case/${caseId}`)
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const typeMap = {
    '咨询': 'info',
    '签约': 'primary',
    '立案': 'success',
    '审理中': 'warning',
    '结案': '',
    '归档': 'info'
  }
  return typeMap[status] || 'info'
}

// 新建沟通记录
const handleAddCommunication = () => {
  Object.assign(communicationForm, {
    id: null,
    way: '电话',
    content: '',
    nextFollowUp: ''
  })
  communicationDialogVisible.value = true
}

// 编辑沟通记录
const handleEditCommunication = (record) => {
  Object.assign(communicationForm, {
    id: record.id,
    way: record.way,
    content: record.content,
    nextFollowUp: record.nextFollowUp
  })
  communicationDialogVisible.value = true
}

// 提交沟通记录
const handleSubmitCommunication = async () => {
  try {
    const data = {
      way: communicationForm.way,
      content: communicationForm.content,
      nextFollowUp: communicationForm.nextFollowUp
    }

    let response
    if (communicationForm.id) {
      response = await updateCommunication(clientId, communicationForm.id, data)
    } else {
      response = await createCommunication(clientId, data)
    }

    if (response.success) {
      ElMessage.success(communicationForm.id ? '编辑成功' : '创建成功')
      communicationDialogVisible.value = false
      await fetchCommunications()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除沟通记录
const handleDeleteCommunication = async (record) => {
  try {
    await ElMessageBox.confirm('确定要删除这条沟通记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteCommunication(clientId, record.id)
    ElMessage.success('删除成功')
    await fetchCommunications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 利益冲突检索
const handleConflictCheck = async () => {
  try {
    ElMessage.info('正在进行利益冲突检索...')
    const response = await checkConflict(clientId)
    if (response.success) {
      if (response.data.hasConflict) {
        ElMessageBox.alert(
          `检测到潜在利益冲突：${response.data.conflictDescription}`,
          '利益冲突警告',
          { type: 'warning' }
        )
      } else {
        ElMessage.success('未检测到利益冲突')
      }
    }
  } catch (error) {
    ElMessage.error('利益冲突检索失败')
  }
}

onMounted(() => {
  fetchClientDetail()
  fetchClientCases()
  fetchCommunications()
})
</script>

<style scoped lang="scss">
.client-detail {
  .detail-tabs {
    margin-top: 20px;

    .tab-content {
      padding: 20px;
      min-height: 400px;
    }
  }

  .communication-timeline {
    margin-top: 20px;

    .communication-card {
      .communication-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .communication-user {
          display: flex;
          align-items: center;
          gap: 12px;

          .user-info {
            .user-name {
              font-weight: 500;
              color: #333;
            }

            .communication-way {
              font-size: 12px;
              color: #999;
            }
          }
        }
      }

      .communication-content {
        color: #606266;
        line-height: 1.6;
        margin-bottom: 12px;
      }

      .communication-followup {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 8px 12px;
        background: #f0f5ff;
        border-radius: 4px;
        color: #1890ff;
        font-size: 13px;
      }
    }
  }

  .finance-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 30px;

    .finance-card {
      background: linear-gradient(135deg, #eef3fc 0%, #dce8f8 100%);
      color: var(--lawos-text, #1c1c1e);
      color: #fff;
      padding: 24px;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);

      .card-label {
        font-size: 14px;
        opacity: 0.9;
        margin-bottom: 8px;
      }

      .card-value {
        font-size: 24px;
        font-weight: bold;
      }

      .income {
        color: #67c23a;
      }

      .pending {
        color: #e6a23c;
      }
    }
  }

  .finance-list {
    h4 {
      margin-bottom: 16px;
      color: #333;
    }

    .income {
      color: #67c23a;
      font-weight: 500;
    }

    .pending {
      color: #e6a23c;
      font-weight: 500;
    }
  }

  .toolbar {
    margin-bottom: 20px;
    display: flex;
    gap: 12px;
  }
}
</style>
