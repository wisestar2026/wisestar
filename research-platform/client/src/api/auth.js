import request from './request'

export const login = (data) => request.post('/auth/login', data)

export const fetchMe = () => request.get('/auth/me')

export const logout = () => request.post('/auth/logout')
