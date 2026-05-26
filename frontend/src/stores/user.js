import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    permissions: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.realName || state.userInfo?.username || '',
    userId: (state) => state.userInfo?.id || '',
    userRole: (state) => state.userInfo?.role || ''
  },

  actions: {
    // 登录
    async login(loginForm) {
      try {
        const res = await loginApi(loginForm)
        this.token = res.data.token
        // 后端直接返回用户信息，不是 res.data.user
        this.userInfo = {
          id: res.data.userId,
          username: res.data.username,
          realName: res.data.realName,
          email: res.data.email,
          phone: res.data.phone,
          avatar: res.data.avatar
        }
        this.permissions = res.data.permissions || []

        localStorage.setItem('token', this.token)
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))

        return res
      } catch (error) {
        throw error
      }
    },

    // 获取用户信息
    async getUserInfo() {
      try {
        const res = await getCurrentUser()
        this.userInfo = res.data
        this.permissions = res.data.permissions || []
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        return res
      } catch (error) {
        throw error
      }
    },

    // 仅清除本地登录态（不请求后端，用于登录前丢弃过期 Token）
    clearLocalAuth() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },

    // 登出
    async logout() {
      try {
        await logoutApi()
      } catch (error) {
        console.error('登出失败:', error)
      } finally {
        this.token = ''
        this.userInfo = null
        this.permissions = []
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
      }
    },

    // 更新用户信息
    updateUserInfo(data) {
      this.userInfo = { ...this.userInfo, ...data }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    }
  }
})
