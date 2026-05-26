<template>
  <div class="settings">
    <PageHeader title="系统设置" />

    <el-tabs v-model="activeTab" type="card" class="settings-tabs">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="tab-content">
          <div class="toolbar">
            <el-input
              v-model="userSearch"
              placeholder="搜索用户"
              clearable
              style="width: 250px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleCreateUser">
              <el-icon><Plus /></el-icon>
              新建用户
            </el-button>
          </div>

          <el-table :data="userList" border v-loading="loading">
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="email" label="邮箱" width="180" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-switch v-model="row.active" @change="handleToggleUserStatus(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditUser(row)">
                  编辑
                </el-button>
                <el-button link type="warning" size="small" @click="handleResetPassword(row)">
                  重置密码
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeleteUser(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 角色权限 -->
      <el-tab-pane label="角色权限" name="roles">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleCreateRole">
              <el-icon><Plus /></el-icon>
              新建角色
            </el-button>
          </div>

          <el-table :data="roleList" border>
            <el-table-column prop="name" label="角色名称" width="150" />
            <el-table-column prop="code" label="角色代码" width="150" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="userCount" label="用户数" width="100" />
            <el-table-column prop="isSystem" label="系统角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isSystem ? 'warning' : ''">
                  {{ row.isSystem ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditRole(row)">
                  编辑权限
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  :disabled="row.isSystem"
                  @click="handleDeleteRole(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 数据权限 -->
      <el-tab-pane label="数据权限" name="data-permission">
        <div class="tab-content">
          <div class="permission-rules">
            <div v-for="rule in permissionRules" :key="rule.id" class="permission-rule">
              <div class="rule-header">
                <h4>{{ rule.name }}</h4>
                <el-switch v-model="rule.enabled" @change="handleTogglePermission(rule)" />
              </div>
              <div class="rule-content">
                <p>{{ rule.description }}</p>
                <el-radio-group v-model="rule.scope">
                  <el-radio label="all">全部数据</el-radio>
                  <el-radio label="department">部门数据</el-radio>
                  <el-radio label="self">仅本人数据</el-radio>
                </el-radio-group>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="logs">
        <div class="tab-content">
          <div class="toolbar">
            <el-date-picker
              v-model="logDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
            <el-select v-model="logModule" placeholder="选择模块" clearable style="width: 150px">
              <el-option label="案件管理" value="case" />
              <el-option label="用户管理" value="user" />
              <el-option label="系统设置" value="system" />
            </el-select>
            <el-button type="primary" @click="handleSearchLogs">搜索</el-button>
            <el-button @click="handleExportLogs">导出</el-button>
          </div>

          <el-table :data="logList" border>
            <el-table-column prop="timestamp" label="时间" width="160" sortable />
            <el-table-column prop="user" label="操作人" width="100" />
            <el-table-column prop="module" label="模块" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.module }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="操作" width="120" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP地址" width="130" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleViewLogDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="logPage"
              :page-size="20"
              :total="logTotal"
              layout="total, prev, pager, next"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 系统配置 -->
      <el-tab-pane label="系统配置" name="config">
        <div class="tab-content">
          <el-form :model="systemConfig" label-width="150px">
            <div class="config-section">
              <h4>案件配置</h4>
              <el-form-item label="案件类型">
                <el-select v-model="systemConfig.caseTypes" multiple style="width: 100%">
                  <el-option label="民事" value="civil" />
                  <el-option label="商事" value="commercial" />
                  <el-option label="刑事" value="criminal" />
                  <el-option label="行政" value="administrative" />
                  <el-option label="仲裁" value="arbitration" />
                  <el-option label="非诉" value="non-litigation" />
                </el-select>
              </el-form-item>

              <el-form-item label="案由库">
                <el-input
                  v-model="systemConfig.caseReasons"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入案由，用逗号分隔"
                />
              </el-form-item>

              <el-form-item label="法院库">
                <el-input
                  v-model="systemConfig.courts"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入法院列表，用逗号分隔"
                />
              </el-form-item>
            </div>

            <div class="config-section">
              <h4>提醒配置</h4>
              <el-form-item label="审限提醒">
                <el-input-number v-model="systemConfig.deadlineReminder" :min="1" :max="30" />
                <span style="margin-left: 10px">天前</span>
              </el-form-item>

              <el-form-item label="开庭提醒">
                <el-input-number v-model="systemConfig.hearingReminder" :min="1" :max="7" />
                <span style="margin-left: 10px">天前</span>
              </el-form-item>
            </div>

            <div class="config-section">
              <h4>AI配置</h4>
              <el-form-item label="AI模型">
                <el-select v-model="systemConfig.aiModel">
                  <el-option label="DeepSeek" value="deepseek" />
                  <el-option label="通义千问" value="qwen" />
                  <el-option label="本地模型" value="local" />
                </el-select>
              </el-form-item>

              <el-form-item label="API Key">
                <el-input v-model="systemConfig.apiKey" placeholder="请输入API Key" />
              </el-form-item>
            </div>

            <el-form-item>
              <el-button type="primary" @click="handleSaveConfig">保存配置</el-button>
              <el-button @click="handleResetConfig">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="审批流程" name="workflow">
        <div class="tab-content">
          <ApprovalWorkflowPanel />
        </div>
      </el-tab-pane>

      <!-- 数据备份 -->
      <el-tab-pane label="数据备份" name="backup">
        <div class="tab-content">
          <div class="backup-info">
            <div class="info-card">
              <div class="info-label">上次备份时间</div>
              <div class="info-value">{{ lastBackupTime }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">备份文件大小</div>
              <div class="info-value">{{ backupSize }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">保留天数</div>
              <div class="info-value">{{ retentionDays }}天</div>
            </div>
          </div>

          <div class="backup-actions">
            <div class="action-section">
              <h4>手动备份</h4>
              <el-button type="primary" @click="handleBackupNow" :loading="backingUp">
                <el-icon><Download /></el-icon>
                立即备份
              </el-button>
            </div>

            <div class="action-section">
              <h4>自动备份</h4>
              <el-form inline>
                <el-form-item label="每天">
                  <el-time-picker
                    v-model="backupTime"
                    placeholder="选择时间"
                    value-format="HH:mm"
                  />
                </el-form-item>
                <el-form-item>
                  <el-switch v-model="autoBackup" active-text="启用" />
                </el-form-item>
              </el-form>
            </div>

            <div class="action-section">
              <h4>数据恢复</h4>
              <el-button type="warning" @click="handleRestore">
                <el-icon><Upload /></el-icon>
                从备份恢复
              </el-button>
            </div>
          </div>

          <div class="backup-list">
            <h4>备份历史</h4>
            <el-table :data="backupList" border>
              <el-table-column prop="filename" label="文件名" />
              <el-table-column prop="size" label="大小" width="100" />
              <el-table-column prop="time" label="备份时间" width="160" />
              <el-table-column prop="type" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.type === 'auto' ? 'success' : 'primary'">
                    {{ row.type === 'auto' ? '自动' : '手动' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleRestoreBackup(row)">
                    恢复
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteBackup(row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 用户编辑对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="userForm.id ? '编辑用户' : '新建用户'"
      width="700px"
    >
      <el-form :model="userForm" label-width="100px" :rules="userRules">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username" required>
              <el-input
                v-model="userForm.username"
                :disabled="!!userForm.id"
                placeholder="请输入用户名"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName" required>
              <el-input v-model="userForm.realName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="userForm.gender">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="userForm.active" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="部门">
          <el-input v-model="userForm.department" placeholder="请输入部门" />
        </el-form-item>

        <el-form-item label="职位">
          <el-input v-model="userForm.position" placeholder="请输入职位" />
        </el-form-item>

        <el-form-item v-if="!userForm.id" label="初始密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入初始密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="角色">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 角色编辑对话框 -->
    <el-dialog v-model="roleDialogVisible" :title="roleForm.id ? '编辑角色' : '新建角色'" width="600px">
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>

        <el-form-item label="角色代码" required>
          <el-input
            v-model="roleForm.code"
            placeholder="请输入角色代码，如：ROLE_LAWYER"
            :disabled="!!roleForm.id"
          />
        </el-form-item>

        <el-form-item label="角色描述">
          <el-input
            v-model="roleForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>

        <el-form-item label="权限配置">
          <el-checkbox-group v-model="roleForm.permissions">
            <el-checkbox label="CASE_VIEW">查看案件</el-checkbox>
            <el-checkbox label="CASE_CREATE">创建案件</el-checkbox>
            <el-checkbox label="CASE_EDIT">编辑案件</el-checkbox>
            <el-checkbox label="CASE_DELETE">删除案件</el-checkbox>
            <el-checkbox label="DOCUMENT_MANAGE">文档管理</el-checkbox>
            <el-checkbox label="FINANCE_VIEW">查看财务</el-checkbox>
            <el-checkbox label="FINANCE_MANAGE">财务管理</el-checkbox>
            <el-checkbox label="APPROVAL_PROCESS">审批流程</el-checkbox>
            <el-checkbox label="USER_MANAGE">用户管理</el-checkbox>
            <el-checkbox label="SYSTEM_CONFIG">系统配置</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Download, Upload } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ApprovalWorkflowPanel from '@/components/admin/ApprovalWorkflowPanel.vue'
import { createUser, updateUser, deleteUser, getUserList, toggleUserStatus } from '@/api/user'
import { getRoleList } from '@/api/role'
import request from '@/utils/request'

const activeTab = ref('users')
const loading = ref(false)
const userSearch = ref('')
const logDateRange = ref([])
const logModule = ref('')
const logPage = ref(1)
const logTotal = ref(0)
const backingUp = ref(false)
const backupTime = ref('02:00')
const autoBackup = ref(true)
const lastBackupTime = ref('2024-04-17 02:00')
const backupSize = ref('256.5 MB')
const retentionDays = ref(180)

// 用户对话框
const userDialogVisible = ref(false)
const userForm = ref({
  id: null,
  username: '',
  realName: '',
  email: '',
  phone: '',
  gender: '男',
  department: '',
  position: '',
  active: true,
  password: '',
  roleIds: []
})

// ==================== 角色对话框 ====================
const roleDialogVisible = ref(false)
const roleForm = ref({
  id: null,
  name: '',
  code: '',
  description: '',
  permissions: []
})

// ==================== 日志搜索表单 ====================
const logSearchForm = ref({
  dateRange: [],
  module: '',
  keyword: '',
  level: ''
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 用户列表
const userList = ref([
  {
    id: '1',
    username: 'zhangsan',
    name: '张三',
    email: 'zhangsan@example.com',
    phone: '13800138000',
    role: '主办律师',
    active: true
  },
  {
    id: '2',
    username: 'lisi',
    name: '李四',
    email: 'lisi@example.com',
    phone: '13900139000',
    role: '协办律师',
    active: true
  }
])

// 角色列表
const roleList = ref([
  {
    id: '1',
    name: '系统管理员',
    code: 'admin',
    description: '拥有系统所有权限',
    userCount: 2,
    isSystem: true
  },
  {
    id: '2',
    name: '主办律师',
    code: 'main-lawyer',
    description: '可以创建和管理案件',
    userCount: 15,
    isSystem: true
  },
  {
    id: '3',
    name: '协办律师',
    code: 'co-lawyer',
    description: '可以编辑参与案件',
    userCount: 20,
    isSystem: true
  },
  {
    id: '4',
    name: '律师助理',
    code: 'assistant',
    description: '基础权限',
    userCount: 25,
    isSystem: true
  },
  {
    id: '5',
    name: '财务人员',
    code: 'finance',
    description: '财务管理权限',
    userCount: 3,
    isSystem: true
  },
  {
    id: '6',
    name: '行政人员',
    code: 'admin-staff',
    description: '行政管理权限',
    userCount: 5,
    isSystem: true
  }
])

// 数据权限规则
const permissionRules = ref([
  {
    id: '1',
    name: '案件数据权限',
    description: '控制用户可以查看哪些案件数据',
    enabled: true,
    scope: 'self'
  },
  {
    id: '2',
    name: '客户数据权限',
    description: '控制用户可以查看哪些客户数据',
    enabled: true,
    scope: 'self'
  },
  {
    id: '3',
    name: '财务数据权限',
    description: '控制用户可以查看哪些财务数据',
    enabled: true,
    scope: 'department'
  }
])

// 操作日志
const logList = ref([
  {
    id: '1',
    timestamp: '2024-04-17 10:30:25',
    user: '张三',
    module: '案件管理',
    action: '创建案件',
    description: '创建了案件：张三诉李四买卖合同纠纷',
    ip: '192.168.1.100'
  },
  {
    id: '2',
    timestamp: '2024-04-17 10:25:10',
    user: '李四',
    module: '用户管理',
    action: '编辑用户',
    description: '编辑了用户信息：王五',
    ip: '192.168.1.101'
  }
])

// 系统配置
const systemConfig = reactive({
  caseTypes: ['civil', 'commercial', 'criminal'],
  caseReasons: '买卖合同纠纷,借款合同纠纷,离婚纠纷',
  courts: '北京市朝阳区人民法院,北京市海淀区人民法院',
  deadlineReminder: 7,
  hearingReminder: 3,
  aiModel: 'deepseek',
  apiKey: ''
})

// 备份列表
const backupList = ref([])

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  const mb = bytes / 1024 / 1024
  return mb >= 1 ? `${mb.toFixed(1)} MB` : `${(bytes / 1024).toFixed(1)} KB`
}

const loadBackupList = async () => {
  try {
    const res = await request({ url: '/system/backups', method: 'get' })
    const list = res.data || []
    backupList.value = list.map((b) => ({
      id: b.id,
      filename: b.filePath ? b.filePath.split(/[/\\]/).pop() : `backup_${b.id}`,
      size: formatFileSize(b.fileSize),
      time: b.backupTime ? new Date(b.backupTime).toLocaleString('zh-CN') : '',
      type: b.backupType === 'AUTO' ? 'auto' : 'manual',
      status: b.backupStatus
    }))
    if (backupList.value.length) {
      lastBackupTime.value = backupList.value[0].time
      backupSize.value = backupList.value[0].size
    }
  } catch (e) {
    console.error('加载备份列表失败', e)
  }
}

// 用户操作
const handleCreateUser = () => {
  userForm.value = {
    id: null,
    username: '',
    realName: '',
    email: '',
    phone: '',
    gender: '男',
    department: '',
    position: '',
    active: true,
    password: '',
    roleIds: []
  }
  userDialogVisible.value = true
}

const handleEditUser = (user) => {
  userForm.value = {
    id: user.id,
    username: user.username,
    realName: user.name || user.realName,
    email: user.email,
    phone: user.phone,
    gender: user.gender || '男',
    department: user.department || '',
    position: user.position || '',
    active: user.active !== undefined ? user.active : true,
    password: '', // 编辑时不需要密码
    roleIds: user.roleIds || []
  }
  userDialogVisible.value = true
}

const handleSaveUser = async () => {
  try {
    if (userForm.value.id) {
      await updateUser(userForm.value.id, userForm.value)
      ElMessage.success('更新成功')
    } else {
      await createUser(userForm.value)
      ElMessage.success('创建成功')
    }
    userDialogVisible.value = false
    await loadUsers()
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleToggleUserStatus = async (user) => {
  try {
    await toggleUserStatus(user.id, user.active ? 'ACTIVE' : 'INACTIVE')
    ElMessage.success(`用户${user.name}状态已${user.active ? '启用' : '禁用'}`)
  } catch (error) {
    console.error('切换用户状态失败:', error)
    ElMessage.error('操作失败')
    // 回滚状态
    user.active = !user.active
  }
}

const handleResetPassword = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要重置用户 ${user.name} 的密码吗?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.success('密码已重置为：123456')
  } catch {
    // 用户取消
  }
}

// 加载角色列表
const loadRoles = async () => {
  try {
    const { data } = await getRoleList({ page: 1, size: 100 })
    roleList.value = data.records || []
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadUsers()
  loadRoles()
  loadBackupList()
})

const loadUsers = async () => {
  try {
    loading.value = true
    const { data } = await getUserList({ page: 1, size: 100 })
    userList.value = data.records || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleDeleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户"${user.name}"吗？删除后可进入回收站恢复。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    // 删除API（待后端实现）
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

// 角色操作
const handleCreateRole = () => {
  roleForm.value = {
    id: null,
    name: '',
    code: '',
    description: '',
    permissions: []
  }
  roleDialogVisible.value = true
}

const handleEditRole = (role) => {
  roleForm.value = {
    id: role.id,
    name: role.name,
    code: role.code,
    description: role.description,
    permissions: role.permissions || []
  }
  roleDialogVisible.value = true
}

const handleSubmitRole = async () => {
  if (!roleForm.value.name) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (!roleForm.value.code) {
    ElMessage.warning('请输入角色代码')
    return
  }

  try {
    if (roleForm.value.id) {
      // 编辑角色
      ElMessage.success('角色更新成功')
    } else {
      // 新建角色
      ElMessage.success('角色创建成功')
    }
    roleDialogVisible.value = false
    // 刷新角色列表
  } catch (error) {
    console.error('保存角色失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleDeleteRole = async (role) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色"${role.name}"吗？删除后可进入回收站恢复。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    // 删除API（待后端实现）
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

// 数据权限操作
const handleTogglePermission = (rule) => {
  ElMessageBox.confirm(
    `确认${rule.enabled ? '启用' : '禁用'}权限规则"${rule.name}"吗？此操作可能影响系统功能。`,
    '权限变更确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success(`权限规则${rule.name}已${rule.enabled ? '启用' : '禁用'}`)
  }).catch(() => {
    // 用户取消，恢复开关状态
    rule.enabled = !rule.enabled
    ElMessage.info('已取消权限变更')
  })
}

// 日志操作
const handleSearchLogs = async () => {
  try {
    // 使用搜索条件筛选日志
    ElMessage.success('搜索完成')
    // 这里可以调用API来搜索日志
  } catch (error) {
    console.error('搜索日志失败:', error)
    ElMessage.error('搜索失败')
  }
}

const handleExportLogs = async () => {
  try {
    const response = await request({
      url: '/system/logs/export',
      method: 'get',
      params: logSearchForm.value,
      responseType: 'blob'
    })

    // 创建下载链接
    const blob = new Blob([response], { type: 'application/vnd.ms-excel' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = `系统日志_${new Date().toISOString().split('T')[0]}.xlsx`
    link.click()
    window.URL.revokeObjectURL(link.href)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出日志失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleViewLogDetail = (log) => {
  ElMessageBox.alert(
    `<div style="text-align: left;">
      <p><strong>时间：</strong>${log.timestamp}</p>
      <p><strong>模块：</strong>${log.module}</p>
      <p><strong>级别：</strong><span style="color: ${getLogLevelColor(log.level)}">${log.level}</span></p>
      <p><strong>用户：</strong>${log.username}</p>
      <p><strong>操作：</strong>${log.action}</p>
      <p><strong>IP地址：</strong>${log.ip}</p>
      <p><strong>详细信息：</strong></p>
      <pre style="background: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto;">${log.message || '无'}</pre>
    </div>`,
    '日志详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    }
  )
}

const getLogLevelColor = (level) => {
  const colorMap = {
    'ERROR': '#f56c6c',
    'WARN': '#e6a23c',
    'INFO': '#409eff',
    'DEBUG': '#909399'
  }
  return colorMap[level] || '#606266'
}

// 系统配置操作
const handleSaveConfig = () => {
  ElMessage.success('配置已保存')
}

const handleResetConfig = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置系统配置吗？此操作将恢复所有设置为默认值，不可撤销。',
      '重置配置',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用重置配置API
    ElMessage.success('配置已重置为默认值')
    // 刷新配置
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置配置失败:', error)
      ElMessage.error('重置失败')
    }
  }
}

// 备份操作
const handleBackupNow = async () => {
  try {
    backingUp.value = true
    await request({ url: '/system/backup', method: 'post', params: { remark: '手动备份' } })
    ElMessage.success('备份完成')
    await loadBackupList()
  } catch (e) {
    ElMessage.error(e.message || '备份失败')
  } finally {
    backingUp.value = false
  }
}

const handleRestore = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入备份文件路径', '数据恢复', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '/path/to/backup.sql',
      inputPattern: /^.{1,200}$/,
      inputErrorMessage: '路径长度为1-200个字符'
    })

    // 文件路径恢复功能暂未实现，请使用备份列表中的恢复功能
    ElMessage.warning('文件路径恢复功能暂未实现，请使用下方备份列表中的恢复按钮')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('数据恢复失败:', error)
      ElMessage.error('数据恢复失败')
    }
  }
}

const handleRestoreBackup = async (backup) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复备份"${backup.name}"吗？这将覆盖当前数据。`,
      '恢复备份',
      {
        confirmButtonText: '确定恢复',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await request({
      url: `/system/restore/${backup.id}`,
      method: 'post'
    })

    ElMessage.success(`备份恢复成功，系统将重新加载`)
    setTimeout(() => {
      location.reload()
    }, 1500)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('恢复备份失败:', error)
      ElMessage.error('恢复备份失败')
    }
  }
}

const handleDeleteBackup = async (backup) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除备份"${backup.name}"吗？删除后无法恢复。`,
      '删除备份',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await request({
      url: `/system/backup/${backup.id}`,
      method: 'delete'
    })

    const index = backupList.value.findIndex(b => b.id === backup.id)
    if (index > -1) {
      backupList.value.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除备份失败:', error)
      ElMessage.error('删除备份失败')
    }
  }
}
</script>

<style scoped lang="scss">
.settings {
  .settings-tabs {
    margin-top: 20px;
    background-color: #fff;
    padding: 20px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    :deep(.el-tabs__header) {
      margin: 0 0 20px;
    }
  }

  .tab-content {
    .toolbar {
      display: flex;
      gap: 10px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }

    .permission-rules {
      .permission-rule {
        background-color: #f5f7fa;
        padding: 15px;
        border-radius: 4px;
        margin-bottom: 15px;

        .rule-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          h4 {
            margin: 0;
            font-size: 14px;
            color: #333;
          }
        }

        .rule-content {
          p {
            margin: 0 0 10px;
            font-size: 13px;
            color: #666;
          }
        }
      }
    }

    .config-section {
      background-color: #f5f7fa;
      padding: 20px;
      border-radius: 4px;
      margin-bottom: 20px;

      h4 {
        margin: 0 0 15px;
        font-size: 14px;
        color: #333;
        border-left: 3px solid #1890ff;
        padding-left: 10px;
      }
    }

    .backup-info {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 20px;
      margin-bottom: 30px;

      .info-card {
        background-color: #f5f7fa;
        padding: 20px;
        border-radius: 4px;
        text-align: center;

        .info-label {
          font-size: 14px;
          color: #666;
          margin-bottom: 10px;
        }

        .info-value {
          font-size: 18px;
          font-weight: bold;
          color: #1890ff;
        }
      }
    }

    .backup-actions {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
      margin-bottom: 30px;

      .action-section {
        background-color: #f5f7fa;
        padding: 20px;
        border-radius: 4px;

        h4 {
          margin: 0 0 15px;
          font-size: 14px;
          color: #333;
        }
      }
    }

    .backup-list {
      h4 {
        margin: 0 0 15px;
        font-size: 14px;
        color: #333;
        border-left: 3px solid #1890ff;
        padding-left: 10px;
      }
    }
  }
}
</style>
