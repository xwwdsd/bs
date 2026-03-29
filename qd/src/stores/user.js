import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

/**
 * 用户状态管理Store
 * 使用Pinia管理用户登录状态和用户信息
 */
export const useUserStore = defineStore('user', () => {
  // ==================== State ====================
  
  /**
   * 访问令牌
   */
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  
  /**
   * 刷新令牌
   */
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  
  /**
   * 用户信息
   */
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  
  // ==================== Getters ====================
  
  /**
   * 是否已登录
   */
  const isLoggedIn = computed(() => {
    return !!accessToken.value && !!userInfo.value
  })
  
  /**
   * 是否是管理员
   */
  const isAdmin = computed(() => {
    return userInfo.value?.userLevel >= 2
  })
  
  /**
   * 是否是超级管理员
   */
  const isSuperAdmin = computed(() => {
    return userInfo.value?.userLevel === 3
  })
  
  // ==================== Actions ====================
  
  /**
   * 设置登录信息
   * @param {Object} data 登录响应数据
   */
  const setLoginInfo = (data) => {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    userInfo.value = data.userInfo
    
    // 持久化存储
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
  }
  
  /**
   * 更新访问令牌
   * @param {string} token 新的访问令牌
   */
  const setAccessToken = (token) => {
    accessToken.value = token
    localStorage.setItem('accessToken', token)
  }
  
  /**
   * 更新用户信息
   * @param {Object} info 用户信息
   */
  const setUserInfo = (info) => {
    userInfo.value = { ...userInfo.value, ...info }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  /**
   * 获取最新用户信息
   */
  const getUserInfo = async () => {
    try {
      const userRes = await request.get('/v1/user/profile')
      if (userRes) {
        let steamBound = false
        let steamId = null
        
        try {
          const steamRes = await request.get('/v1/steam/status')
          if (steamRes) {
            steamBound = steamRes.bound || false
            steamId = steamRes.steamId || null
          }
        } catch (steamError) {
          console.warn('获取Steam状态失败:', steamError)
        }
        
        const newInfo = {
          ...userRes,
          steamBound,
          steamId
        }
        
        setUserInfo(newInfo)
      }
    } catch (error) {
      console.error('更新用户信息失败:', error)
    }
  }
  
  /**
   * 清除登录状态
   */
  const logout = () => {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    
    // 清除本地存储
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }
  
  /**
   * 初始化（应用启动时调用）
   */
  const init = () => {
    const storedToken = localStorage.getItem('accessToken')
    const storedUserInfo = localStorage.getItem('userInfo')
    
    if (storedToken) {
      accessToken.value = storedToken
    }
    
    if (storedUserInfo) {
      try {
        userInfo.value = JSON.parse(storedUserInfo)
      } catch (e) {
        console.error('解析用户信息失败:', e)
        logout()
      }
    }
  }
  
  return {
    // State
    accessToken,
    refreshToken,
    userInfo,
    // Getters
    isLoggedIn,
    isAdmin,
    isSuperAdmin,
    // Actions
    setLoginInfo,
    setAccessToken,
    setUserInfo,
    getUserInfo,
    logout,
    init
  }
})
