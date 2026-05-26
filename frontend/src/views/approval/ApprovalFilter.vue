<template>
  <div class="approval-filter">
    <el-form :inline="true" :model="filterForm" class="filter-form">
      <el-form-item label="审批类型">
        <el-select v-model="filterForm.approvalType" placeholder="全部" clearable style="width: 150px">
          <el-option label="全部" value="" />
          <el-option
            v-for="t in typeOptions"
            :key="t.code"
            :label="t.name"
            :value="t.code"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="PENDING" />
          <el-option label="已同意" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已撤回" value="WITHDRAWN" />
        </el-select>
      </el-form-item>

      <el-form-item label="申请时间">
        <el-date-picker
          v-model="filterForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>

      <el-form-item label="关键词">
        <el-input
          v-model="filterForm.keyword"
          placeholder="搜索标题/申请人"
          clearable
          style="width: 200px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { getApprovalTypes } from '@/api/approval'

const emit = defineEmits(['search', 'reset'])

const filterForm = ref({
  approvalType: '',
  status: '',
  dateRange: [],
  keyword: ''
})

const typeOptions = ref([])

onMounted(async () => {
  try {
    const res = await getApprovalTypes()
    if (res.code === 200 || res.success) {
      typeOptions.value = res.data || []
    }
  } catch {
    typeOptions.value = []
  }
})

const handleSearch = () => {
  emit('search', { ...filterForm.value })
}

const handleReset = () => {
  filterForm.value = {
    approvalType: '',
    status: '',
    dateRange: [],
    keyword: ''
  }
  emit('reset')
}
</script>

<style scoped lang="scss">
.approval-filter {
  background: #fff;
  padding: 16px 24px;
  border-radius: 12px;
  margin-bottom: 16px;

  .filter-form {
    margin-bottom: 0;
  }
}
</style>
