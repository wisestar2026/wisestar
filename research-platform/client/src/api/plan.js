import request from './request'

export const fetchPlans = (params) => request.get('/plans', { params })

export const fetchPlan = (id) => request.get(`/plans/${id}`)

export const createPlan = (data) => request.post('/plans', data)

export const updatePlan = (id, data) => request.put(`/plans/${id}`, data)

export const deletePlan = (id) => request.delete(`/plans/${id}`)

export const submitPlan = (id) => request.post(`/plans/${id}/submit`)

export const reviewPlan = (id, data) => request.post(`/plans/${id}/review`, data)

export const withdrawPlan = (id) => request.post(`/plans/${id}/withdraw`)

export const fetchPlanLogs = (id) => request.get(`/plans/${id}/logs`)
