<template>
  <router-view v-slot="{ Component, route }">
    <template v-if="route.meta.keepAlive">
      <keep-alive :max="12">
        <transition name="route-soft" mode="default">
          <component :is="Component" :key="route.name" class="route-page-root" />
        </transition>
      </keep-alive>
    </template>
    <transition v-else name="route-soft" mode="default">
      <component :is="Component" :key="route.fullPath" class="route-page-root" />
    </transition>
  </router-view>
</template>

<script setup>
/** 主布局内子路由：柔和切换 + keep-alive */
</script>
