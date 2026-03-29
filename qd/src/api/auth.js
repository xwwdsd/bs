import { post, get } from '@/utils/request'

/**
 * 认证相关API
 * 包含登录、注册等接口
 */

/**
 * 用户登录
 * @param {Object} data 登录数据 { email, password }
 * @returns {Promise}
 */
export const login = (data) => {
  return post('/v1/auth/login', data)
}

/**
 * 用户注册
 * @param {Object} data 注册数据 { username, email, password, confirmPassword }
 * @returns {Promise}
 */
export const register = (data) => {
  return post('/v1/auth/register', data)
}

/**
 * 检查邮箱是否已注册
 * @param {string} email 邮箱地址
 * @returns {Promise}
 */
export const checkEmail = (email) => {
  return get('/v1/auth/check-email', { email })
}

/**
 * 检查用户名是否已存在
 * @param {string} username 用户名
 * @returns {Promise}
 */
export const checkUsername = (username) => {
  return get('/v1/auth/check-username', { username })
}

export const forgotPassword = (email) => {
  return post('/v1/auth/forgot-password', { email })
}

export const resetPassword = (token, newPassword) => {
  return post('/v1/auth/reset-password', { token, newPassword })
}

export const validateResetToken = (token) => {
  return get('/v1/auth/validate-reset-token', { token })
}
