import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()

    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }

    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res.code === 200) {
      return res.data
    }

    const message = res.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const { response } = error
    const serverMessage = response?.data?.message
    let message = '请求失败'

    if (response) {
      switch (response.status) {
        case 401: {
          message = serverMessage || '登录已过期，请重新登录'
          ElMessage.error(message)
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        }
        case 403:
          message = serverMessage || '没有权限访问该资源'
          ElMessage.error(message)
          break
        case 404:
          message = serverMessage || '请求的资源不存在'
          ElMessage.error(message)
          break
        case 500:
          message = serverMessage || '服务器内部错误'
          ElMessage.error(message)
          break
        default:
          message = serverMessage || '请求失败'
          ElMessage.error(message)
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
      ElMessage.error(message)
    } else {
      message = '网络连接失败，请检查网络'
      ElMessage.error(message)
    }

    error.message = message
    return Promise.reject(error)
  }
)

export const get = (url, params = {}, config = {}) => {
  return request.get(url, { params, ...config })
}

export const post = (url, data = {}, config = {}) => {
  return request.post(url, data, config)
}

export const put = (url, data = {}, config = {}) => {
  return request.put(url, data, config)
}

export const del = (url, params = {}, config = {}) => {
  return request.delete(url, { params, ...config })
}

export default request
