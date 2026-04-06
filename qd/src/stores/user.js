import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import request from '@/utils/request'

const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_INFO_KEY = 'userInfo'

const readJson = (storage, key) => {
  const raw = storage.getItem(key)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    storage.removeItem(key)
    return null
  }
}

const readAuthState = (storage) => ({
  accessToken: storage.getItem(ACCESS_TOKEN_KEY) || '',
  refreshToken: storage.getItem(REFRESH_TOKEN_KEY) || '',
  userInfo: readJson(storage, USER_INFO_KEY)
})

const writeAuthState = (storage, accessToken, refreshToken, userInfo) => {
  if (accessToken) {
    storage.setItem(ACCESS_TOKEN_KEY, accessToken)
  } else {
    storage.removeItem(ACCESS_TOKEN_KEY)
  }

  if (refreshToken) {
    storage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  } else {
    storage.removeItem(REFRESH_TOKEN_KEY)
  }

  if (userInfo) {
    storage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  } else {
    storage.removeItem(USER_INFO_KEY)
  }
}

const hydrateFromStorage = () => {
  const sessionState = readAuthState(sessionStorage)
  if (sessionState.accessToken || sessionState.userInfo) {
    return { ...sessionState, persistent: false }
  }

  const persistentState = readAuthState(localStorage)
  if (persistentState.accessToken || persistentState.userInfo) {
    writeAuthState(
      sessionStorage,
      persistentState.accessToken,
      persistentState.refreshToken,
      persistentState.userInfo
    )

    return { ...persistentState, persistent: true }
  }

  return {
    accessToken: '',
    refreshToken: '',
    userInfo: null,
    persistent: false
  }
}

export const useUserStore = defineStore('user', () => {
  const initialState = hydrateFromStorage()

  const accessToken = ref(initialState.accessToken)
  const refreshToken = ref(initialState.refreshToken)
  const userInfo = ref(initialState.userInfo)
  const persistLogin = ref(initialState.persistent)

  const isLoggedIn = computed(() => !!accessToken.value && !!userInfo.value)
  const isAdmin = computed(() => userInfo.value?.userLevel >= 2)
  const isSuperAdmin = computed(() => userInfo.value?.userLevel === 3)

  const persistCurrentState = () => {
    writeAuthState(sessionStorage, accessToken.value, refreshToken.value, userInfo.value)

    if (persistLogin.value) {
      writeAuthState(localStorage, accessToken.value, refreshToken.value, userInfo.value)
    }
  }

  const setLoginInfo = (data, options = {}) => {
    const { rememberMe = false } = options
    const persistedUserId = readAuthState(localStorage).userInfo?.userId

    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    userInfo.value = data.userInfo
    persistLogin.value = rememberMe

    persistCurrentState()

    if (!rememberMe && persistedUserId === data.userInfo?.userId) {
      writeAuthState(localStorage, '', '', null)
    }
  }

  const setAccessToken = (token) => {
    accessToken.value = token
    persistCurrentState()
  }

  const setUserInfo = (info) => {
    userInfo.value = { ...userInfo.value, ...info }
    persistCurrentState()
  }

  const getUserInfo = async () => {
    try {
      const profile = await request.get('/v1/user/profile')
      if (!profile) {
        return
      }

      let steamBound = false
      let steamId = null

      try {
        const steamStatus = await request.get('/v1/steam/status')
        if (steamStatus) {
          steamBound = steamStatus.bound || false
          steamId = steamStatus.steamId || null
        }
      } catch (steamError) {
        console.warn('Failed to load Steam status:', steamError)
      }

      setUserInfo({
        ...profile,
        steamBound,
        steamId
      })
    } catch (error) {
      console.error('Failed to refresh user profile:', error)
    }
  }

  const logout = (options = {}) => {
    const { clearPersistent = persistLogin.value } = options

    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    persistLogin.value = false

    writeAuthState(sessionStorage, '', '', null)

    if (clearPersistent) {
      writeAuthState(localStorage, '', '', null)
    }
  }

  const init = () => {
    const state = hydrateFromStorage()
    accessToken.value = state.accessToken
    refreshToken.value = state.refreshToken
    userInfo.value = state.userInfo
    persistLogin.value = state.persistent
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    persistLogin,
    isLoggedIn,
    isAdmin,
    isSuperAdmin,
    setLoginInfo,
    setAccessToken,
    setUserInfo,
    getUserInfo,
    logout,
    init
  }
})
