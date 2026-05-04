<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>律所智能案件管理系统</h1>
        <p>Law Firm Intelligent Case Management System</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>© 2026 律所智能案件管理系统 版权所有</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 20, message: '密码长度在 4 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  try {
    await loginFormRef.value?.validate()
    loading.value = true

    await userStore.login(loginForm)

    ElMessage.success('登录成功')

    // 跳转到原页面或首页
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;

  .login-box {
    width: 100%;
    max-width: 450px;
    padding: 40px;
    background-color: #fff;
    border-radius: 10px;
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);

    .login-header {
      text-align: center;
      margin-bottom: 40px;

      h1 {
        margin: 0 0 10px;
        font-size: 28px;
        font-weight: bold;
        color: #333;
      }

      p {
        margin: 0;
        font-size: 14px;
        color: #999;
      }
    }

    .login-form {
      .login-button {
        width: 100%;
        height: 45px;
        font-size: 16px;
      }
    }

    .login-footer {
      margin-top: 30px;
      text-align: center;

      p {
        margin: 0;
        font-size: 12px;
        color: #999;
      }
    }
  }
}
</style>
