<template>
  <div class="ssb-time-saver">
    <PageHeader title="SSB省时宝" />

    <div class="tool-container">
      <el-alert
        title="工具说明"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        SSB省时宝是高效的办公效率工具，提供文档处理、数据分析、流程自动化等功能。
      </el-alert>

      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>功能模块</span>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="8" v-for="module in modules" :key="module.name">
            <el-card shadow="hover" class="module-card" @click="openModule(module)">
              <div class="module-icon">{{ module.icon }}</div>
              <div class="module-name">{{ module.name }}</div>
              <div class="module-desc">{{ module.desc }}</div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="coming-soon-card" v-if="!isActivated">
        <el-empty description="功能开发中，敬请期待">
          <el-button type="primary" @click="requestFeature">申请开通</el-button>
        </el-empty>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'

const isActivated = ref(false)
const modules = ref([
  { name: '文档处理', icon: '📄', desc: '智能文档识别、格式转换、批量处理' },
  { name: '数据分析', icon: '📊', desc: '数据可视化、报表生成、趋势分析' },
  { name: '流程自动化', icon: '⚡', desc: '工作流自动化、任务调度、定时执行' },
  { name: '智能搜索', icon: '🔍', desc: '全文检索、智能推荐、相关度排序' },
  { name: '协作办公', icon: '👥', desc: '团队协作、任务分配、进度跟踪' },
  { name: '移动办公', icon: '📱', desc: '移动端适配、随时随地办公' }
])

const openModule = (module) => {
  if (!isActivated.value) {
    ElMessage.info(`${module.name}功能即将上线，敬请期待！`)
  } else {
    ElMessage.success(`正在打开${module.name}...`)
  }
}

const requestFeature = () => {
  ElMessage.success('申请已提交，我们会尽快联系您！')
}
</script>

<style scoped>
.ssb-time-saver {
  height: 100%;
  padding: 20px;
}

.tool-container {
  max-width: 1200px;
  margin: 0 auto;
}

.info-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
}

.module-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.module-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.module-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.module-name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.module-desc {
  font-size: 12px;
  color: #666;
}

.coming-soon-card {
  margin-top: 20px;
}
</style>
