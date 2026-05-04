<template>
  <div class="office-supplies-page">
    <PageHeader title="办公用品管理">
      <template #extra>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用品
        </el-button>
        <el-button type="success" @click="handleInbound">
          <el-icon><Download /></el-icon>
          入库
        </el-button>
        <el-button type="warning" @click="handleOutbound">
          <el-icon><Upload /></el-icon>
          出库
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索筛选 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="用品名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="searchForm.category" placeholder="请选择" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="文具类" value="文具" />
            <el-option label="办公耗材" value="耗材" />
            <el-option label="办公设备" value="设备" />
            <el-option label="日用品" value="日用品" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存状态">
          <el-select v-model="searchForm.stockStatus" placeholder="请选择" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="充足" value="sufficient" />
            <el-option label="不足" value="low" />
            <el-option label="缺货" value="out" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSupplies">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="supplies" border v-loading="loading" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="用品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="类别" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="specification" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="quantity" label="库存数量" width="100" align="right" sortable>
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.quantity <= row.minStock, 'text-warning': row.quantity <= row.minStock * 2 }">
              {{ row.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="minStock" label="最低库存" width="100" align="right" />
        <el-table-column prop="unitPrice" label="单价(元)" width="100" align="right">
          <template #default="{ row }">
            ¥{{ row.unitPrice?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column label="库存状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.quantity === 0" type="danger" size="small">缺货</el-tag>
            <el-tag v-else-if="row.quantity <= row.minStock" type="warning" size="small">不足</el-tag>
            <el-tag v-else type="success" size="small">充足</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="存放位置" width="120" show-overflow-tooltip />
        <el-table-column prop="lastInboundDate" label="最近入库" width="110" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="success" size="small" @click="handleQuickInbound(row)">
              入库
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadSupplies"
          @current-change="loadSupplies"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" @submit.native.prevent>
        <el-form-item label="用品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入用品名称" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
            <el-option label="文具类" value="文具" />
            <el-option label="办公耗材" value="耗材" />
            <el-option label="办公设备" value="设备" />
            <el-option label="日用品" value="日用品" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格" prop="specification">
          <el-input v-model="form.specification" placeholder="例如：A4/0.5mm/黑色" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择" style="width: 100%">
                <el-option label="个" value="个" />
                <el-option label="支" value="支" />
                <el-option label="本" value="本" />
                <el-option label="包" value="包" />
                <el-option label="箱" value="箱" />
                <el-option label="件" value="件" />
                <el-option label="套" value="套" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="初始库存" prop="quantity">
              <el-input-number v-model="form.quantity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低库存" prop="minStock">
              <el-input-number v-model="form.minStock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="存放位置" prop="location">
          <el-input v-model="form.location" placeholder="例如：A柜-1层-3格" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 入库对话框 -->
    <el-dialog v-model="inboundDialogVisible" title="入库" width="500px">
      <el-form :model="inboundForm" :rules="inboundRules" ref="inboundFormRef" label-width="100px" @submit.native.prevent>
        <el-form-item label="用品" prop="supplyId">
          <el-select v-model="inboundForm.supplyId" placeholder="请选择用品" filterable style="width: 100%">
            <el-option
              v-for="item in supplies"
              :key="item.id"
              :label="`${item.name} (${item.category})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="inboundForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="经办人" prop="operator">
          <el-input v-model="inboundForm.operator" placeholder="请输入经办人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitInbound">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 出库对话框 -->
    <el-dialog v-model="outboundDialogVisible" title="出库" width="500px">
      <el-form :model="outboundForm" :rules="outboundRules" ref="outboundFormRef" label-width="100px" @submit.native.prevent>
        <el-form-item label="用品" prop="supplyId">
          <el-select v-model="outboundForm.supplyId" placeholder="请选择用品" filterable style="width: 100%">
            <el-option
              v-for="item in supplies.filter(s => s.quantity > 0)"
              :key="item.id"
              :label="`${item.name} (库存：${item.quantity})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="outboundForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="领用人" prop="receiver">
          <el-input v-model="outboundForm.receiver" placeholder="请输入领用人" />
        </el-form-item>
        <el-form-item label="用途" prop="purpose">
          <el-input v-model="outboundForm.purpose" placeholder="请输入用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outboundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitOutbound">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="用品详情" width="600px">
      <el-descriptions :column="2" border v-if="currentSupply">
        <el-descriptions-item label="用品名称">{{ currentSupply.name }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ currentSupply.category }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentSupply.specification || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ currentSupply.unit }}</el-descriptions-item>
        <el-descriptions-item label="库存数量">
          <span :class="{ 'text-danger': currentSupply.quantity <= currentSupply.minStock }">
            {{ currentSupply.quantity }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="最低库存">{{ currentSupply.minStock }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ currentSupply.unitPrice?.toFixed(2) || '0.00' }}</el-descriptions-item>
        <el-descriptions-item label="存放位置">{{ currentSupply.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近入库" :span="2">{{ currentSupply.lastInboundDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentSupply.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">出入库记录</el-divider>
      <el-table :data="currentSupply.records" size="small" max-height="300">
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === '入库' ? 'success' : 'warning'" size="small">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="operator" label="经办人" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Upload, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getOfficeSupplies,
  createOfficeSupply,
  updateOfficeSupply,
  deleteOfficeSupply,
  getOfficeSupplyDetail,
  stockIn,
  stockOut
} from '@/api/officeSupplies'

const loading = ref(false)
const supplies = ref([])

// 表单ref
const formRef = ref(null)
const inboundFormRef = ref(null)
const outboundFormRef = ref(null)

// 搜索表单
const searchForm = reactive({
  name: '',
  category: '',
  stockStatus: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const inboundDialogVisible = ref(false)
const outboundDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const dialogTitle = computed(() => form.id ? '编辑用品' : '新增用品')
const submitting = ref(false)

// 表单
const form = reactive({
  id: null,
  name: '',
  category: '',
  specification: '',
  quantity: 0,
  unit: '',
  unitPrice: 0,
  minStock: 10,
  location: '',
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入用品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入初始库存', trigger: 'blur' }],
  minStock: [{ required: true, message: '请输入最低库存', trigger: 'blur' }]
}

// 入库表单
const inboundForm = reactive({
  supplyId: null,
  quantity: 1,
  operator: '',
  remark: ''
})

const inboundRules = {
  supplyId: [{ required: true, message: '请选择用品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }],
  operator: [{ required: true, message: '请输入经办人', trigger: 'blur' }]
}

// 出库表单
const outboundForm = reactive({
  supplyId: null,
  quantity: 1,
  receiver: '',
  purpose: ''
})

const outboundRules = {
  supplyId: [{ required: true, message: '请选择用品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入出库数量', trigger: 'blur' }],
  receiver: [{ required: true, message: '请输入领用人', trigger: 'blur' }],
  purpose: [{ required: true, message: '请输入用途', trigger: 'blur' }]
}

// 当前查看的用品
const currentSupply = ref(null)

// 加载用品列表
const loadSupplies = async () => {
  try {
    loading.value = true
    const { data } = await getOfficeSupplies({
      ...searchForm,
      page: pagination.page - 1,
      size: pagination.size
    })

    supplies.value = data.content || data.records || []
    pagination.total = data.totalElements || data.total || 0
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 重置搜索
const handleReset = () => {
  searchForm.name = ''
  searchForm.category = ''
  searchForm.stockStatus = ''
  pagination.page = 1
  loadSupplies()
}

// 获取类别标签颜色
const getCategoryTagType = (category) => {
  const typeMap = {
    '文具': 'primary',
    '耗材': 'success',
    '设备': 'warning',
    '日用品': 'info',
    '其他': ''
  }
  return typeMap[category] || ''
}

// 新增
const handleAdd = () => {
  Object.assign(form, {
    id: null,
    name: '',
    category: '',
    specification: '',
    quantity: 0,
    unit: '',
    unitPrice: 0,
    minStock: 10,
    location: '',
    remark: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

// 查看
const handleView = async (row) => {
  try {
    const { data } = await getOfficeSupplyDetail(row.id)
    currentSupply.value = data
    detailDialogVisible.value = true
  } catch (error) {
    console.error('加载详情失败:', error)
    ElMessage.error('加载详情失败')
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    await deleteOfficeSupply(row.id)
    ElMessage.success('删除成功')
    loadSupplies()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 入库
const handleInbound = () => {
  Object.assign(inboundForm, {
    supplyId: null,
    quantity: 1,
    operator: '',
    remark: ''
  })
  inboundDialogVisible.value = true
}

// 快速入库
const handleQuickInbound = (row) => {
  Object.assign(inboundForm, {
    supplyId: row.id,
    quantity: 1,
    operator: '',
    remark: ''
  })
  inboundDialogVisible.value = true
}

// 出库
const handleOutbound = () => {
  Object.assign(outboundForm, {
    supplyId: null,
    quantity: 1,
    receiver: '',
    purpose: ''
  })
  outboundDialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    submitting.value = true

    if (form.id) {
      await updateOfficeSupply(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createOfficeSupply(form)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    loadSupplies()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败：' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 提交入库
const handleSubmitInbound = async () => {
  try {
    const valid = await inboundFormRef.value.validate()
    if (!valid) return

    submitting.value = true

    await stockIn(inboundForm.supplyId, inboundForm)
    ElMessage.success('入库成功')
    inboundDialogVisible.value = false
    loadSupplies()
  } catch (error) {
    console.error('入库失败:', error)
    ElMessage.error('入库失败：' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 提交出库
const handleSubmitOutbound = async () => {
  try {
    const valid = await outboundFormRef.value.validate()
    if (!valid) return

    // 检查库存
    const supply = supplies.value.find(s => s.id === outboundForm.supplyId)
    if (supply.quantity < outboundForm.quantity) {
      ElMessage.error('库存不足')
      return
    }

    submitting.value = true

    await stockOut(outboundForm.supplyId, outboundForm)
    ElMessage.success('出库成功')
    outboundDialogVisible.value = false
    loadSupplies()
  } catch (error) {
    console.error('出库失败:', error)
    ElMessage.error('出库失败：' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 初始化
onMounted(() => {
  loadSupplies()
})
</script>

<style scoped lang="scss">
.office-supplies-page {
  .search-card {
    margin-bottom: 20px;
  }

  .table-card {
    .text-danger {
      color: #f56c6c;
      font-weight: bold;
    }

    .text-warning {
      color: #e6a23c;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
