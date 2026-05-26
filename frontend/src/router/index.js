import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'

const SETTINGS_PERMISSIONS = ['SYSTEM_CONFIG', 'USER_VIEW', 'ROLE_VIEW']

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      // 工作台
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: '📊' }
      },
      // 全局搜索
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/index.vue'),
        meta: { title: '全局搜索', icon: '🔍' }
      },
      // 日程管理
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('@/views/calendar/index.vue'),
        meta: { title: '日程', icon: '📅' }
      },
      // 案件管理
      {
        path: 'case',
        name: 'Case',
        redirect: '/case/list',
        meta: { title: '案件', icon: '⚖️' },
        children: [
          {
            path: 'list',
            name: 'CaseList',
            component: () => import('@/views/case/list.vue'),
            meta: { title: '案件列表' }
          },
          {
            path: 'create',
            name: 'CaseCreate',
            component: () => import('@/views/case/create.vue'),
            meta: { title: '新建案件' }
          },
          {
            path: 'batch-import',
            name: 'CaseBatchImport',
            component: () => import('@/views/case/batch-import.vue'),
            meta: { title: '批量收案' }
          },
          {
            path: ':id/edit',
            name: 'CaseEdit',
            component: () => import('@/views/case/create.vue'),
            meta: { title: '编辑案件' }
          },
          {
            path: ':id',
            name: 'CaseDetail',
            component: () => import('@/views/case/detail.vue'),
            redirect: '/case/:id/basic',
            meta: { title: '案件详情' },
            children: [
              {
                path: 'basic',
                name: 'CaseBasic',
                component: () => import('@/views/case/tabs/basic.vue'),
                meta: { title: '基本案情' }
              },
              {
                path: 'record',
                name: 'CaseRecord',
                component: () => import('@/views/case/tabs/record.vue'),
                meta: { title: '办案记录' }
              },
              {
                path: 'unit',
                name: 'CaseUnit',
                component: () => import('@/views/case/tabs/unit.vue'),
                meta: { title: '受理单位' }
              },
              {
                path: 'doc',
                name: 'CaseDoc',
                component: () => import('@/views/case/tabs/doc.vue'),
                meta: { title: '案件文档' }
              },
              {
                path: 'timeline',
                name: 'CaseTimeline',
                component: () => import('@/views/case/tabs/timeline.vue'),
                meta: { title: '案件动态' }
              }
            ]
          },
          {
            path: 'archive',
            name: 'CaseArchive',
            component: () => import('@/views/case/archive.vue'),
            meta: { title: '归档库' }
          },
          {
            path: 'trash',
            name: 'CaseTrash',
            component: () => import('@/views/case/trash.vue'),
            meta: { title: '回收站' }
          }
        ]
      },
      // 客户管理
      {
        path: 'client',
        name: 'Client',
        redirect: '/client/list',
        meta: { title: '客户', icon: '👥' },
        children: [
          {
            path: 'list',
            name: 'ClientList',
            component: () => import('@/views/client/index.vue'),
            meta: { title: '客户列表' }
          },
          {
            path: 'create',
            name: 'ClientCreate',
            component: () => import('@/views/client/create.vue'),
            meta: { title: '新建客户' }
          },
          {
            path: ':id/edit',
            name: 'ClientEdit',
            component: () => import('@/views/client/create.vue'),
            meta: { title: '编辑客户' }
          },
          {
            path: ':id',
            name: 'ClientDetail',
            component: () => import('@/views/client/detail.vue'),
            meta: { title: '客户详情' }
          }
        ]
      },
      // 文档中心
      {
        path: 'document',
        name: 'Document',
        component: () => import('@/views/document/index.vue'),
        meta: { title: '文档中心', icon: '📁' }
      },
      // 财务管理
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/views/finance/index.vue'),
        meta: { title: '财务', icon: '💰' }
      },
      // 审批管理
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/approval/index.vue'),
        meta: { title: '审批', icon: '✅' }
      },
      // 行政OA
      {
        path: 'admin-oa',
        name: 'AdminOA',
        component: () => import('@/views/admin-oa/index.vue'),
        meta: { title: '行政', icon: '🏢' }
      },
      // 办公用品管理
      {
        path: 'office-supplies',
        name: 'OfficeSupplies',
        component: () => import('@/views/office-supplies/index.vue'),
        meta: { title: '办公用品', icon: '📦' }
      },
      // 固定资产管理
      {
        path: 'fixed-assets',
        name: 'FixedAssets',
        component: () => import('@/views/fixed-assets/index.vue'),
        meta: { title: '固定资产', icon: '🖥️' }
      },
      // 统计报表
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '统计', icon: '📈' }
      },
      {
        path: 'legal-search',
        name: 'LegalSearch',
        component: () => import('@/views/legal-search/index.vue'),
        meta: { title: '法律检索', icon: '📚' }
      },
      // AI智能中心
      {
        path: 'ai-hub',
        name: 'AIHub',
        component: () => import('@/views/ai/index.vue'),
        meta: { title: 'AI智能中心', icon: '🤖' }
      },
      // 知识库
      {
        path: 'knowledge',
        name: 'Knowledge',
        redirect: '/knowledge/list',
        meta: { title: '知识库', icon: '📚' },
        children: [
          {
            path: 'rag',
            name: 'KnowledgeRAG',
            component: () => import('@/views/knowledge/rag.vue'),
            meta: { title: 'AI知识问答', icon: '🤖' }
          },
          {
            path: 'list',
            name: 'KnowledgeList',
            component: () => import('@/views/knowledge/index.vue'),
            meta: { title: '知识库' }
          },
          {
            path: 'create',
            name: 'KnowledgeCreate',
            component: () => import('@/views/knowledge/edit.vue'),
            meta: { title: '新建文章' }
          },
          {
            path: ':id/edit',
            name: 'KnowledgeEdit',
            component: () => import('@/views/knowledge/edit.vue'),
            meta: { title: '编辑文章' }
          },
          {
            path: ':id',
            name: 'KnowledgeDetail',
            component: () => import('@/views/knowledge/detail.vue'),
            meta: { title: '文章详情' }
          }
        ]
      },
      // 工作汇报
      {
        path: 'work-reports',
        name: 'WorkReport',
        redirect: '/work-reports/list',
        meta: { title: '工作汇报', icon: '📝' },
        children: [
          {
            path: 'list',
            name: 'WorkReportList',
            component: () => import('@/views/work-report/index.vue'),
            meta: { title: '工作汇报列表' }
          },
          {
            path: ':id',
            name: 'WorkReportDetail',
            component: () => import('@/views/work-report/index.vue'),
            meta: { title: '工作汇报详情' }
          }
        ]
      },
      // 公文流转
      {
        path: 'document-flow',
        name: 'DocumentFlow',
        component: () => import('@/views/document-flow/index.vue'),
        meta: { title: '公文流转', icon: '📄' }
      },
      // 类案检索
      {
        path: 'case-search',
        name: 'CaseSearch',
        component: () => import('@/views/case-search/index.vue'),
        meta: { title: '类案检索', icon: '🔍' }
      },
      // 工具集
      {
        path: 'tools',
        name: 'Tools',
        component: () => import('@/views/tools/index.vue'),
        meta: { title: '工具集', icon: '🔧' }
      },
      // AC精算工具
      {
        path: 'ac-calculator',
        name: 'ACCalculator',
        component: () => import('@/views/tools/ac-calculator.vue'),
        meta: { title: '债权精算', icon: '🧮' }
      },
      // SSB省时宝
      {
        path: 'ssb-time-saver',
        name: 'SSBTimeSaver',
        component: () => import('@/views/tools/ssb-time-saver.vue'),
        meta: { title: 'SSB省时宝', icon: '⏰' }
      },
      // 系统设置
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: '设置', icon: '⚙️', requiresSettings: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from) => {
  const userStore = useUserStore()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 律所智能案件管理系统` : '律所智能案件管理系统'

  // 已登录时不再进入登录页（开发环境自动登录后常见）
  if (to.path === '/login') {
    if (userStore.isLoggedIn) {
      return { path: '/' }
    }
    return
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  // 系统设置：前端路由守卫（与后端 SYSTEM_* / USER_VIEW 权限对齐）
  if (to.meta.requiresSettings) {
    const perms = userStore.permissions || []
    const allowed = perms.some((p) => SETTINGS_PERMISSIONS.includes(p))
    if (!allowed) {
      ElMessage.warning('无权访问系统设置')
      return { path: '/dashboard' }
    }
  }

  // 允许导航
})

export default router
