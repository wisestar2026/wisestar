import { create } from 'zustand'
import * as authApi from '../api/auth'

const readUser = () => {
  try {
    return JSON.parse(localStorage.getItem('research-user') || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = create((set) => ({
  token: localStorage.getItem('research-token'),
  user: readUser(),
  loading: false,
  async login(values) {
    set({ loading: true })
    try {
      const data = await authApi.login(values)
      localStorage.setItem('research-token', data.token)
      localStorage.setItem('research-user', JSON.stringify(data.user))
      set({ token: data.token, user: data.user })
      return data.user
    } finally {
      set({ loading: false })
    }
  },
  async logout() {
    try {
      await authApi.logout()
    } catch {
      // 忽略网络异常，本地状态照常清理
    }
    localStorage.removeItem('research-token')
    localStorage.removeItem('research-user')
    set({ token: null, user: null })
  },
  canReview() {
    const role = readUser()?.role
    return role === 'ADMIN' || role === 'SUPERVISOR'
  },
}))
