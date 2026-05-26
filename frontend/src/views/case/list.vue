<template>
  <div class="case-list">
    <PageHeader title="案件列表">
      <template #extra>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建案件
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选条件区 -->
    <div class="filter-section">
      <el-form :model="filterForm" inline>
        <el-form-item label="案件类型">
          <el-select v-model="filterForm.caseType" placeholder="请选择" clearable style="width: 150px">
            <el-option label="民事" value="民事" />
            <el-option label="商事" value="商事" />
            <el-option label="仲裁" value="仲裁" />
            <el-option label="刑事" value="刑事" />
            <el-option label="行政" value="行政" />
            <el-option label="非诉" value="非诉" />
          </el-select>
        </el-form-item>

        <el-form-item label="案件状态">
          <el-select v-model="filterForm.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="待立案" value="pending" />
            <el-option label="审理中" value="active" />
            <el-option label="结案" value="closed" />
            <el-option label="归档" value="archived" />
          </el-select>
        </el-form-item>

        <el-form-item label="案件等级">
          <el-select v-model="filterForm.level" placeholder="请选择" clearable style="width: 120px">
            <el-option label="重要" value="重要" />
            <el-option label="一般" value="一般" />
            <el-option label="次要" value="次要" />
          </el-select>
        </el-form-item>

        <el-form-item label="主办律师">
          <el-select v-model="filterForm.ownerId" placeholder="请选择" clearable filterable style="width: 150px">
            <el-option
              v-for="lawyer in lawyerList"
              :key="lawyer.id"
              :label="lawyer.name"
              :value="lawyer.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="管辖法院">
          <el-select
            v-model="filterForm.court"
            placeholder="请选择法院"
            clearable
            filterable
            remote
            :remote-method="searchCourt"
            style="width: 200px"
          >
            <el-option
              v-for="court in courtList"
              :key="court"
              :label="court"
              :value="court"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 视图切换和批量操作 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-radio-group v-model="viewMode" @change="handleViewChange">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
            列表视图
          </el-radio-button>
          <el-radio-button label="kanban">
            <el-icon><Grid /></el-icon>
            看板视图
          </el-radio-button>
        </el-radio-group>

        <el-dropdown v-if="selectedCases.length > 0" trigger="click" @command="handleBatchAction">
          <el-button type="primary">
            批量操作 ({{ selectedCases.length }})
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="close">批量结案</el-dropdown-item>
              <el-dropdown-item command="archive">批量归档</el-dropdown-item>
              <el-dropdown-item command="changeOwner">修改主办律师</el-dropdown-item>
              <el-dropdown-item command="delete" divided>批量删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="toolbar-right">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-show="viewMode === 'list'" class="list-view">
      <DataTable
        :table-data="caseList"
        :loading="loading"
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        :show-selection="true"
        @selection-change="handleSelectionChange"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      >
        <el-table-column prop="level" label="等级" width="60" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.level === '重要'" color="#f56c6c"><Star /></el-icon>
            <el-icon v-else-if="row.level === '一般'" color="#e6a23c"><Warning /></el-icon>
            <el-icon v-else color="#909399"><CircleCheck /></el-icon>
          </template>
        </el-table-column>

        <el-table-column prop="caseType" label="类型" width="80" sortable />

        <el-table-column prop="caseName" label="案件名称" width="200" sortable>
          <template #default="{ row }">
            <el-link type="primary" @click="handleViewDetail(row)">
              {{ row.caseName }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column prop="caseNumber" label="案号" width="150" sortable />

        <el-table-column prop="parties" label="当事人" width="150">
          <template #default="{ row }">
            <div class="parties-cell">
              <div class="party">{{ row.plaintiff }}</div>
              <div class="vs">vs</div>
              <div class="party">{{ row.defendant }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="currentStage" label="当前阶段" width="100">
          <template #default="{ row }">
            <el-progress
              :percentage="getStageProgress(row.currentStage)"
              :color="getStageColor(row.currentStage)"
              :show-text="false"
              :stroke-width="6"
            />
            <div class="stage-text">{{ row.currentStage }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="ownerName" label="主办律师" width="80" sortable />

        <el-table-column prop="court" label="管辖法院" width="120" />

        <el-table-column prop="nextHearing" label="下次开庭" width="110" sortable>
          <template #default="{ row }">
            <div v-if="row.nextHearing" :class="getHearingClass(row.nextHearing)">
              {{ formatDate(row.nextHearing) }}
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="warning" size="small" @click="handleArchive(row)">
            归档
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </DataTable>
    </div>

    <!-- 看板视图 -->
    <div v-show="viewMode === 'kanban'" class="kanban-view">
      <div class="kanban-board">
        <div
          v-for="stage in caseStages"
          :key="stage.key"
          class="kanban-column"
        >
          <div class="column-header">
            <h3>{{ stage.label }}</h3>
            <el-badge :value="getStageCount(stage.key)" class="stage-badge" />
          </div>
          <div class="column-cards">
            <div
              v-for="caseItem in getCasesByStage(stage.key)"
              :key="caseItem.id"
              class="case-card"
              :class="`level-${caseItem.level}`"
              @click="handleViewDetail(caseItem)"
            >
              <div class="card-header">
                <span class="level-indicator" :class="caseItem.level">
                  <el-icon v-if="caseItem.level === '重要'" color="#f56c6c"><Star /></el-icon>
                  <el-icon v-else-if="caseItem.level === '一般'" color="#e6a23c"><Warning /></el-icon>
                  <el-icon v-else color="#909399"><CircleCheck /></el-icon>
                </span>
                <el-tag size="small" :type="getTypeTagType(caseItem.caseType)">
                  {{ caseItem.caseType }}
                </el-tag>
              </div>
              <div class="card-title">{{ caseItem.caseName }}</div>
              <div class="card-meta">
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  {{ caseItem.ownerName }}
                </div>
                <div class="meta-item" v-if="caseItem.nextHearing">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(caseItem.nextHearing) }}
                </div>
              </div>
              <div class="card-footer">
                <span class="case-number">{{ caseItem.caseNumber }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, List, Grid, ArrowDown, Refresh, User, Calendar, Star, Warning, CircleCheck
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import { getStagesByCaseType } from '@/config/case-lifecycle'
import {
  getCaseList,
  deleteCase,
  archiveCase,
  batchCloseCases,
  batchArchiveCases,
  batchDeleteCases,
  batchChangeOwner
} from '@/api/case'

const router = useRouter()
const route = useRoute()

// 筛选表单
const filterForm = reactive({
  caseType: '',
  status: '',
  level: '',
  ownerId: '',
  court: '',
  clientId: null, // 客户ID筛选
  dateRange: []
})

// 视图模式
const viewMode = ref('list')

// 数据
const caseList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const selectedCases = ref([])

// 律师列表
const lawyerList = ref([
  { id: 1, name: '张律师' },
  { id: 2, name: '李律师' },
  { id: 3, name: '王律师' }
])

// 法院列表
const courtList = ref([])

// 看板列：默认民事流程（与后端 CaseFlowDefinitionService 一致）
const caseStages = computed(() => getStagesByCaseType(filterForm.caseType || 'CIVIL'))

// 获取案件列表
const fetchCaseList = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      caseType: filterForm.caseType,
      status: filterForm.status,
      level: filterForm.level,
      ownerId: filterForm.ownerId,
      court: filterForm.court,
      clientId: filterForm.clientId,
      startDate: filterForm.dateRange?.[0] || null,
      endDate: filterForm.dateRange?.[1] || null
    }
    const res = await getCaseList(params)
    caseList.value = res.data?.records || []  // 后端PageResult的data字段包含records
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error('获取案件列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

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

// 获取阶段进度
const getStageProgress = (stage) => {
  const stageMap = {
    '咨询': 10,
    '签约': 20,
    '立案': 35,
    '一审': 60,
    '二审': 80,
    '执行': 90,
    '结案': 100
  }
  return stageMap[stage] || 0
}

// 获取阶段颜色
const getStageColor = (stage) => {
  const colorMap = {
    '咨询': '#909399',
    '签约': '#409eff',
    '立案': '#67c23a',
    '一审': '#e6a23c',
    '二审': '#f56c6c',
    '执行': '#ff6600',
    '结案': '#303133'
  }
  return colorMap[stage] || '#909399'
}

// 获取开庭日期样式
const getHearingClass = (date) => {
  const days = Math.ceil((new Date(date) - new Date()) / (1000 * 60 * 60 * 24))
  if (days < 0) return 'hearing-overdue'
  if (days <= 3) return 'hearing-urgent'
  if (days <= 7) return 'hearning-soon'
  return 'hearing-normal'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return `${d.getMonth() + 1}-${d.getDate()}`
}

// 获取案件类型标签颜色
const getTypeTagType = (type) => {
  const typeMap = {
    '民事': 'primary',
    '商事': 'success',
    '仲裁': 'warning',
    '刑事': 'danger',
    '行政': 'info',
    '非诉': ''
  }
  return typeMap[type] || ''
}

// 看板视图：根据阶段筛选案件
const getCasesByStage = (stageKey) => {
  const stage = caseStages.value.find(s => s.key === stageKey)
  if (!stage) return []
  return caseList.value.filter(item => item.currentStage === stage.label)
}

// 看板视图：获取阶段案件数
const getStageCount = (stageKey) => {
  return getCasesByStage(stageKey).length
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchCaseList()
}

// 重置
const handleReset = () => {
  Object.assign(filterForm, {
    caseType: '',
    status: '',
    level: '',
    ownerId: '',
    court: '',
    dateRange: []
  })
  handleSearch()
}

// 视图切换
const handleViewChange = () => {
  // removed debug log
}

// 刷新
const handleRefresh = () => {
  fetchCaseList()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedCases.value = selection
}

// 分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1 // 改变每页大小时重置到第一页
  fetchCaseList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchCaseList()
}

// 新建案件
const handleCreate = () => {
  router.push('/case/create')
}

// 查看详情
const handleViewDetail = (row) => {
  router.push(`/case/${row.id}`)
}

// 编辑
const handleEdit = (row) => {
  router.push(`/case/${row.id}/edit`)
}

// 归档
const handleArchive = async (row) => {
  try {
    await ElMessageBox.confirm('确定要归档该案件吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await archiveCase(row.id)
    ElMessage.success('归档成功')
    fetchCaseList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('归档失败')
    }
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该案件吗? 删除后可进入回收站恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCase(row.id)
    ElMessage.success('删除成功')
    fetchCaseList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 批量操作
const handleBatchAction = async (command) => {
  switch (command) {
    case 'close':
      try {
        await ElMessageBox.confirm(
          `将为选中的 ${selectedCases.value.length} 个案件结案，是否继续？`,
          '批量结案',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        // 使用批量操作API
        const caseIds = selectedCases.value.map(c => c.id)
        await batchCloseCases({ caseIds })

        ElMessage.success(`成功结案 ${selectedCases.value.length} 个案件`)
        selectedCases.value = []
        await loadCases()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量结案失败: ' + (error.message || '未知错误'))
        }
      }
      break
    case 'archive':
      try {
        const { value } = await ElMessageBox.prompt(
          `将为选中的 ${selectedCases.value.length} 个案件归档，请输入档案保管地`,
          '批量归档',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPattern: /^.{1,100}$/,
            inputErrorMessage: '请输入档案保管地（1-100字符）'
          }
        )

        if (!value) return

        // 使用批量操作API
        const caseIds = selectedCases.value.map(c => c.id)
        await batchArchiveCases({ caseIds, reason: value })

        ElMessage.success(`成功归档 ${selectedCases.value.length} 个案件`)
        selectedCases.value = []
        await loadCases()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量归档失败: ' + (error.message || '未知错误'))
        }
      }
      break
    case 'changeOwner':
      try {
        const { value } = await ElMessageBox.prompt(
          `将修改选中的 ${selectedCases.value.length} 个案件的主办律师，请输入新主办律师ID`,
          '修改主办律师',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPattern: /^\d+$/,
            inputErrorMessage: '请输入有效的律师ID'
          }
        )

        if (!value) return

        // 使用批量操作API
        const caseIds = selectedCases.value.map(c => c.id)
        await batchChangeOwner({ caseIds, ownerId: parseInt(value) })

        ElMessage.success(`成功修改 ${selectedCases.value.length} 个案件的主办律师`)
        selectedCases.value = []
        await loadCases()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('修改主办律师失败: ' + (error.message || '未知错误'))
        }
      }
      break
    case 'delete':
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedCases.value.length} 个案件吗？此操作不可恢复！`, '批量删除', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        // 使用批量操作API
        const caseIds = selectedCases.value.map(c => c.id)
        await batchDeleteCases({ caseIds })

        ElMessage.success(`成功删除 ${selectedCases.value.length} 个案件`)
        selectedCases.value = []
        fetchCaseList()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量删除失败: ' + (error.message || '未知错误'))
        }
      }
      break
  }
}

onMounted(() => {
  // 检查是否有客户筛选参数
  if (route.query.clientId) {
    filterForm.clientId = route.query.clientId
    // 可选：在页面顶部显示筛选信息
    if (route.query.clientName) {
      ElMessage.info(`正在查看客户"${route.query.clientName}"的关联案件`)
    }
  }

  fetchCaseList()
})
</script>

<style scoped lang="scss">
.case-list {
  .filter-section {
    background-color: #fff;
    padding: 20px;
    margin-bottom: 20px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    :deep(.el-form--inline .el-form-item) {
      margin-right: 15px;
      margin-bottom: 10px;
    }
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 15px 20px;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .toolbar-left {
      display: flex;
      gap: 15px;
      align-items: center;
    }
  }

  .list-view {
    .parties-cell {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;

      .party {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .vs {
        color: #999;
        font-size: 10px;
      }
    }

    .stage-text {
      font-size: 12px;
      color: #666;
      margin-top: 4px;
    }

    .hearing-overdue {
      color: #f56c6c;
      font-weight: bold;
    }

    .hearing-urgent {
      color: #f56c6c;
    }

    .hearning-soon {
      color: #e6a23c;
    }

    .hearing-normal {
      color: #606266;
    }

    .text-muted {
      color: #999;
    }
  }

  .kanban-view {
    .kanban-board {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 20px;
      overflow-x: auto;
      padding-bottom: 20px;

      .kanban-column {
        background-color: #f5f7fa;
        border-radius: 8px;
        padding: 15px;
        min-height: 600px;
        display: flex;
        flex-direction: column;

        .column-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;
          padding-bottom: 10px;
          border-bottom: 2px solid #e4e7ed;

          h3 {
            margin: 0;
            font-size: 14px;
            font-weight: 500;
            color: #333;
          }
        }

        .column-cards {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 12px;
          overflow-y: auto;

          .case-card {
            background-color: #fff;
            border-radius: 6px;
            padding: 12px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
            cursor: pointer;
            transition: all 0.3s;
            border-left: 3px solid transparent;

            &:hover {
              box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
              transform: translateY(-2px);
            }

            &.level-重要 {
              border-left-color: #f56c6c;
            }

            &.level-一般 {
              border-left-color: #e6a23c;
            }

            &.level-次要 {
              border-left-color: #909399;
            }

            .card-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 8px;

              .level-indicator {
                font-size: 16px;
              }
            }

            .card-title {
              font-size: 14px;
              font-weight: 500;
              color: #333;
              margin-bottom: 10px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .card-meta {
              display: flex;
              gap: 12px;
              margin-bottom: 8px;
              font-size: 12px;
              color: #666;

              .meta-item {
                display: flex;
                align-items: center;
                gap: 4px;
              }
            }

            .card-footer {
              display: flex;
              justify-content: space-between;
              align-items: center;

              .case-number {
                font-size: 12px;
                color: #999;
              }
            }
          }
        }
      }
    }
  }
}
</style>
