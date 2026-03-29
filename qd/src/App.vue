<template>
  <router-view />
</template>

<script setup>
import { watch, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import websocketService from '@/utils/websocket'

const userStore = useUserStore()

// 监听登录状态变化
watch(() => userStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    // 用户登录后连接WebSocket
    websocketService.connect()
    // 获取最新用户信息（包括Steam绑定状态）
    userStore.getUserInfo()
  } else {
    // 用户登出后断开WebSocket
    websocketService.disconnect()
  }
})

// 页面加载时检查
onMounted(() => {
  if (userStore.isLoggedIn) {
    websocketService.connect()
    // 获取最新用户信息（包括Steam绑定状态）
    userStore.getUserInfo()
  }
})
</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

body {
  background-color: #171a21;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #171a21;
}

::-webkit-scrollbar-thumb {
  background: #3a3f50;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #4a4f60;
}
</style>
