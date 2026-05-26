<template>
  <el-container class="main-layout" :class="{ 'is-mobile': isMobile }">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobile && mobileSidebarVisible"
      class="sidebar-overlay"
      @click="closeMobileSidebar"
    ></div>

    <!-- 侧边栏 -->
    <el-aside
      :width="sidebarWidth"
      class="sidebar"
      :class="{ 'mobile-visible': mobileSidebarVisible }"
    >
      <div class="logo">
        <h2 v-if="!isCollapse">律所管理系统</h2>
        <h2 v-else>律所</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        :default-openeds="defaultOpenedMenus"
        router
        class="sidebar-menu"
        @select="closeMobileSidebar"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-sub-menu v-if="route.children && route.children.length > 0" :index="route.path">
            <template #title>
              <el-icon class="menu-icon"><component :is="getIconName(route.meta?.icon)" /></el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in route.children"
              :key="child.path"
              :index="child.path"
            >
              {{ child.meta?.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="route.path">
            <el-icon class="menu-icon"><component :is="getIconName(route.meta?.icon)" /></el-icon>
            <span>{{ route.meta?.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb v-if="!isMobile" separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <span v-else class="mobile-title">{{ currentPageTitle }}</span>
        </div>

        <div class="header-right">
          <!-- 全局搜索 -->
          <el-input
            v-if="!isMobile"
            v-model="searchKeyword"
            placeholder="搜索案件、客户..."
            prefix-icon="Search"
            class="search-input"
            clearable
            @input="handleSearchInput"
          />
          <el-button v-else circle class="header-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>

          <!-- AI助手 -->
          <el-button circle class="header-btn" @click="showAIAssistant = true">
            <span class="ai-icon">🤖</span>
          </el-button>

          <!-- 通知 -->
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="header-btn">
            <el-button circle @click="showNotifications">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>

          <!-- 用户头像 -->
          <el-dropdown @command="handleUserAction">
            <div class="user-avatar">
              <el-avatar v-if="!isMobile" :size="32" :src="userAvatar">
                {{ userName?.charAt(0) }}
              </el-avatar>
              <span v-if="!isMobile" class="user-name">{{ userName }}</span>
              <el-icon v-else><User /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- AI助手 -->
    <AIAssistant v-model:visible="showAIAssistant" />

    <!-- 通知面板 -->
    <NotificationPanel
      ref="notificationPanelRef"
      v-model="showNotificationPanel"
      :unread-count="unreadCount"
      @update:unread-count="updateUnreadCount"
    />
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore, useAppStore } from '@/stores'
import { ElMessage, ElMessageBox } from 'element-plus'
import AIAssistant from '@/views/ai/assistant.vue'
import NotificationPanel from '@/components/NotificationPanel.vue'
import { getUnreadCount } from '@/api/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// Emoji到Element Plus图标组件名称的映射（使用全局注册的组件名）
const emojiToIconName = {
  '📊': 'DataAnalysis',
  '🔍': 'Search',
  '📅': 'Calendar',
  '⚖️': 'Files',
  '👥': 'User',
  '📁': 'FolderOpened',
  '💰': 'Finance',
  '✅': 'CircleCheck',
  '🏢': 'OfficeBuilding',
  '📚': 'Reading',
  '📝': 'Document',
  '📈': 'TrendCharts',
  '🔧': 'Tools',
  '⚙️': 'Setting'
}

const getIconName = (emoji) => {
  return emojiToIconName[emoji] || 'Document'
}

// 响应式相关
const windowWidth = ref(window.innerWidth)
const isMobile = computed(() => windowWidth.value < 768)
const isTablet = computed(() => windowWidth.value >= 768 && windowWidth.value < 992)
const isDesktop = computed(() => windowWidth.value >= 992)

// 移动端侧边栏抽屉状态
const mobileSidebarVisible = ref(false)

// 侧边栏状态 - 移动端自动折叠
const isCollapse = computed(() => {
  // 移动端始终折叠
  if (isMobile.value) {
    return true
  }
  // 平板和小屏幕桌面端根据store状态
  return !appStore.isSidebarOpened
})

const sidebarWidth = computed(() => {
  if (isMobile.value) {
    return '0px' // 移动端完全隐藏
  }
  return isCollapse.value ? '64px' : '200px'
})

// 当前页面标题（移动端）
const currentPageTitle = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.length > 0 ? matched[matched.length - 1].meta.title : '律所系统'
})

// 当前激活的菜单
const activeMenu = computed(() => {
  const { path } = route
  // 案件详情页高亮案件列表
  if (path.startsWith('/case/') && path.includes('/tab')) {
    return '/case/list'
  }
  // 案件相关页面保持高亮
  if (path.startsWith('/case/')) {
    return path
  }
  return path
})

// 默认展开的菜单
const defaultOpenedMenus = computed(() => {
  const { path } = route
  const openedMenus = []
  // 如果在案件相关页面，展开案件菜单
  if (path.startsWith('/case/')) {
    openedMenus.push('/case')
  }
  // 如果在客户相关页面，展开客户菜单
  if (path.startsWith('/client/')) {
    openedMenus.push('/client')
  }
  // 如果在知识库页面，展开知识库菜单
  if (path.startsWith('/knowledge/')) {
    openedMenus.push('/knowledge')
  }
  return openedMenus
})

// 菜单路由
const menuRoutes = computed(() => {
  const routes = [
    { path: '/dashboard', meta: { title: '工作台', icon: '📊' } },
    { path: '/calendar', meta: { title: '日程', icon: '📅' } },
    {
      path: '/case',
      meta: { title: '案件', icon: '⚖️' },
      children: [
        { path: '/case/list', meta: { title: '案件列表' } },
        { path: '/case/create', meta: { title: '新建案件' } },
        { path: '/case/batch-import', meta: { title: '批量收案' } },
        { path: '/case/archive', meta: { title: '归档库' } },
        { path: '/case/trash', meta: { title: '回收站' } }
      ]
    },
    {
      path: '/client',
      meta: { title: '客户', icon: '👥' },
      children: [
        { path: '/client/list', meta: { title: '客户列表' } },
        { path: '/client/create', meta: { title: '新建客户' } }
      ]
    },
    { path: '/document', meta: { title: '文档中心', icon: '📁' } },
    { path: '/finance', meta: { title: '财务', icon: '💰' } },
    { path: '/approval', meta: { title: '审批', icon: '✅' } },
    {
      path: '/admin-oa',
      meta: { title: '行政', icon: '🏢' },
      children: [
        { path: '/admin-oa', meta: { title: '行政OA' } },
        { path: '/office-supplies', meta: { title: '办公用品' } },
        { path: '/fixed-assets', meta: { title: '固定资产' } }
      ]
    },
    { path: '/statistics', meta: { title: '统计', icon: '📈' } },
    {
      path: '/knowledge',
      meta: { title: '知识库', icon: '📚' },
      children: [
        { path: '/knowledge/list', meta: { title: '知识库' } },
        { path: '/knowledge/rag', meta: { title: 'AI问答' } },
        { path: '/knowledge/create', meta: { title: '新建文章' } }
      ]
    },
    { path: '/legal-search', meta: { title: '法律检索', icon: '⚖️' } },
    { path: '/ai-hub', meta: { title: 'AI智能中心', icon: '🤖' } },
    { path: '/case-search', meta: { title: '类案检索', icon: '🔍' } },
    {
      path: '/tools',
      meta: { title: '工具集', icon: '🔧' },
      children: [
        { path: '/tools', meta: { title: '工具集' } },
        { path: '/ac-calculator', meta: { title: '债权精算' } },
        { path: '/ssb-time-saver', meta: { title: 'SSB省时宝' } }
      ]
    },
    { path: '/work-reports', meta: { title: '工作汇报', icon: '📝' } },
    { path: '/document-flow', meta: { title: '公文流转', icon: '📄' } },
    { path: '/settings', meta: { title: '设置', icon: '⚙️' } }
  ]
  return routes
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    path: item.path,
    title: item.meta.title
  }))
})

// 用户信息
const userName = computed(() => userStore.userName)
const userAvatar = computed(() => userStore.userInfo?.avatar)

// 搜索
const searchKeyword = ref('')
// 全局搜索（带debounce 300ms）
let searchDebounce = null
const handleSearchInput = () => {
  clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => {
    handleSearch()
  }, 300)
}

const handleSearch = () => {
  if (searchKeyword.value.trim() || isMobile.value) {
    router.push({ path: '/search', query: { q: searchKeyword.value } })
  }
}

// 通知
const unreadCount = ref(0)
const showNotificationPanel = ref(false)
const notificationPanelRef = ref(null)
const showNotifications = () => {
  showNotificationPanel.value = true
}

// 更新未读数量
const updateUnreadCount = async (count) => {
  if (typeof count === 'number') {
    unreadCount.value = count
    return
  }
  try {
    const res = await getUnreadCount()
    if (res.code === 200) {
      unreadCount.value = res.data ?? 0
    }
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

// AI助手
const showAIAssistant = ref(false)

// 切换侧边栏
const toggleSidebar = () => {
  if (isMobile.value) {
    // 移动端显示抽屉式侧边栏
    mobileSidebarVisible.value = !mobileSidebarVisible.value
  } else {
    // 桌面端切换折叠状态
    appStore.toggleSidebar()
  }
}

// 关闭移动端侧边栏
const closeMobileSidebar = () => {
  mobileSidebarVisible.value = false
}

// 用户操作
const handleUserAction = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/settings')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      } catch {
        // 用户取消
      }
      break
  }
}

// 监听窗口大小变化
const handleResize = () => {
  windowWidth.value = window.innerWidth

  // 窗口变大时，关闭移动端侧边栏
  if (!isMobile.value) {
    mobileSidebarVisible.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)

  // 移动端初始化时自动折叠侧边栏
  if (isMobile.value) {
    appStore.closeSidebar()
  }

  // 获取未读通知数量（仅在用户已登录时）
  if (userStore.isLoggedIn) {
    updateUnreadCount()
  }

  // 定时刷新未读数量（每30秒，仅在用户已登录时）
  if (userStore.isLoggedIn) {
    setInterval(updateUnreadCount, 30000)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
  width: 100vw;

  .sidebar {
    background-color: #001529;
    transition: width 0.3s, transform 0.3s;
    overflow: hidden;
    position: relative;
    z-index: 1000;

    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 18px;
      font-weight: bold;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      white-space: nowrap;
      overflow: hidden;
    }

    .sidebar-menu {
      border-right: none;
      background-color: #001529;

      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        color: rgba(255, 255, 255, 0.65);

        &:hover {
          background-color: rgba(255, 255, 255, 0.08);
          color: #fff;
        }

        &.is-active {
          background-color: #1890ff;
          color: #fff;
        }
      }

      // 确保子菜单项正确显示激活状态
      :deep(.el-sub-menu .el-menu-item) {
        color: rgba(255, 255, 255, 0.65);

        &:hover {
          background-color: rgba(255, 255, 255, 0.08);
          color: #fff;
        }

        &.is-active {
          background-color: #1890ff;
          color: #fff;
        }
      }

      .menu-icon {
        margin-right: 8px;
        font-size: 16px;
      }
    }
  }

  .main-container {
    display: flex;
    flex-direction: column;

    .header {
      background-color: #fff;
      border-bottom: 1px solid #f0f0f0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 20px;
      height: 60px;
      box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

      .header-left {
        display: flex;
        align-items: center;
        gap: 20px;

        .collapse-btn {
          font-size: 20px;
          cursor: pointer;
          transition: color 0.3s;

          &:hover {
            color: #1890ff;
          }
        }

        .mobile-title {
          font-size: 16px;
          font-weight: 500;
          color: #333;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 15px;

        .search-input {
          width: 300px;
        }

        .header-btn {
          background-color: transparent;
          border: none;
          font-size: 18px;

          .ai-icon {
            font-size: 20px;
          }
        }

        .user-avatar {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          padding: 0 10px;
          border-radius: 4px;
          transition: background-color 0.3s;

          &:hover {
            background-color: #f5f5f5;
          }

          .user-name {
            font-size: 14px;
            color: #333;
          }
        }
      }
    }

    .main-content {
      background-color: #f0f2f5;
      overflow-y: auto;
      padding: 20px;
    }
  }

  // 移动端适配样式
  &.is-mobile {
    .sidebar {
      position: fixed;
      top: 0;
      left: 0;
      height: 100vh;
      width: 200px !important;
      transform: translateX(-100%);
      box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);

      &.mobile-visible {
        transform: translateX(0);
      }
    }

    .sidebar-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      z-index: 999;
    }

    .main-container {
      .header {
        padding: 0 15px;

        .header-right {
          gap: 10px;

          .search-input {
            width: auto;
          }
        }
      }

      .main-content {
        padding: 15px;
      }
    }
  }
}

// 响应式断点
@media (max-width: 768px) {
  .main-layout {
    .main-container {
      .header {
        .header-left {
          gap: 10px;

          .collapse-btn {
            font-size: 18px;
          }
        }

        .header-right {
          gap: 8px;

          .header-btn {
            padding: 8px;

            .ai-icon {
              font-size: 16px;
            }
          }
        }
      }
    }
  }
}

@media (min-width: 769px) and (max-width: 991px) {
  .main-layout {
    .sidebar {
      width: 64px !important;
    }

    .main-container {
      .header {
        .search-input {
          width: 200px;
        }
      }
    }
  }
}

// 页面切换动画
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}
</style>
