import axios from 'axios'

const BASE_URL = import.meta.env.BASE_URL || '/'
const API_BASE = import.meta.env.VITE_API_BASE || (import.meta.env.PROD ? `${BASE_URL}api` : '/api')

const request = axios.create({ baseURL: API_BASE, timeout: 15000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('research-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function toLogin() {
  localStorage.removeItem('research-token')
  localStorage.removeItem('research-user')
  const loginPath = `${BASE_URL}login`.replace(/\/+/g, '/')
  if (!window.location.pathname.endsWith('/login')) window.location.href = loginPath
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 200) return body.data
      if (body.code === 401) toLogin()
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    if (error.response?.status === 401) toLogin()
    const message = error.response?.data?.message || error.message || '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

export default request
