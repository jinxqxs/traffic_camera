// src/utils/request.js  —— 双 Token 无感刷新版
import axios from 'axios'
import router from '@/router'

const request = axios.create({
    baseURL: '/api',
    timeout: 10000
})

// ==================== 工具函数 ====================
const getAccessToken = () => localStorage.getItem('accessToken')
const getRefreshToken = () => localStorage.getItem('refreshToken')
const setTokens = (accessToken, refreshToken, username, role) => {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    if (username) localStorage.setItem('username', username)
    if (role) localStorage.setItem('role', role)
}
const clearTokens = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
}

// ==================== 无感刷新控制器 ====================
let isRefreshing = false
let refreshSubscribers = []

/**
 * 将因 401 挂起的请求加入队列，等待 Token 刷新完成后重放
 */
function subscribeTokenRefresh(callback) {
    refreshSubscribers.push(callback)
}

/**
 * Token 刷新成功后，重放所有挂起的请求
 */
function onTokenRefreshed(newAccessToken) {
    refreshSubscribers.forEach(cb => cb(newAccessToken))
    refreshSubscribers = []
}

/**
 * Token 刷新失败，清空所有挂起请求，跳转登录
 */
function onTokenRefreshFailed() {
    refreshSubscribers = []
    clearTokens()
    router.push('/login')
}

/**
 * 调用后端 /refresh 接口，使用 Refresh Token 换取新双 Token
 */
async function refreshTokens() {
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
        throw new Error('无 Refresh Token')
    }

    const res = await axios.post('/api/refresh', { refreshToken })
    if (res.data.code === 200) {
        const { accessToken, refreshToken: newRefresh, username, role } = res.data.data
        setTokens(accessToken, newRefresh, username, role)
        return accessToken
    } else {
        throw new Error(res.data.msg || '刷新失败')
    }
}

// ==================== 请求拦截器 ====================
request.interceptors.request.use(
    config => {
        const accessToken = getAccessToken()
        if (accessToken) {
            config.headers['Authorization'] = 'Bearer ' + accessToken
        }
        return config
    },
    error => Promise.reject(error)
)

// ==================== 响应拦截器 ====================
request.interceptors.response.use(
    response => response.data,   // 统一返回 response.data
    async error => {
        const originalRequest = error.config

        // 只对 401 且不是来自 /refresh 本身的请求做无感刷新
        if (
            error.response &&
            error.response.status === 401 &&
            !originalRequest._retry &&
            originalRequest.url !== '/refresh'
        ) {
            // 如果当前正在刷新 Token，将请求挂起排队
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    subscribeTokenRefresh(newToken => {
                        originalRequest.headers['Authorization'] = 'Bearer ' + newToken
                        resolve(request(originalRequest))
                    })
                })
            }

            originalRequest._retry = true
            isRefreshing = true

            try {
                const newAccessToken = await refreshTokens()
                onTokenRefreshed(newAccessToken)

                // 重放当前请求
                originalRequest.headers['Authorization'] = 'Bearer ' + newAccessToken
                return request(originalRequest)
            } catch (refreshError) {
                onTokenRefreshFailed()
                return Promise.reject(refreshError)
            } finally {
                isRefreshing = false
            }
        }

        // 非 401 或 /refresh 本身的 401 → 直接 reject
        return Promise.reject(error)
    }
)

export default request

// // src/utils/request.js
// import axios from 'axios'
//
// const request = axios.create({
//     baseURL: 'http://localhost:8080', // 你的后端接口地址
//     timeout: 5000
// })
//
// // 请求拦截器：直接去掉 Token 相关逻辑
// request.interceptors.request.use(
//     config => {
//         // 原来的 getToken()、config.headers['Authorization'] 相关代码全部删掉
//         return config
//     },
//     error => Promise.reject(error)
// )
//
// // 响应拦截器：可以保留错误提示，但去掉 401 跳登录的逻辑
// request.interceptors.response.use(
//     response => response.data,
//     error => {
//         // 去掉 error.response.status === 401 时跳 /login 的代码
//         return Promise.reject(error)
//     }
// )
//
//
// export default request