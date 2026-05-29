<template>
  <router-view v-slot="{ Component, route }">
    <!-- 仅登录页 ↔ 主框架 切换时过渡；框架内由 RouteContent 处理 -->
    <transition name="route-soft" mode="default">
      <component :is="Component" :key="layoutShellKey(route)" />
    </transition>
  </router-view>
</template>

<script setup>
/** 顶层壳：auth 与 main 分离，避免工作台↔案件列表重复动画 */
function layoutShellKey(route) {
  return route.meta.requiresAuth === false ? 'auth-shell' : 'main-shell'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
</style>
