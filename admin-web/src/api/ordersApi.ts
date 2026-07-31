import { apiClient } from './apiClient'
import type { Order, SpringPage } from '../types'

export async function getOrders(params: {
  page?: number
  size?: number
  status?: string
}): Promise<SpringPage<Order>> {
  const { data } = await apiClient.get<SpringPage<Order>>('/orders', { params })
  return data
}

export async function getOrder(id: number): Promise<Order> {
  const { data } = await apiClient.get<Order>(`/orders/${id}`)
  return data
}

export async function updateOrderStatus(id: number, status: string): Promise<Order> {
  const { data } = await apiClient.put<Order>(`/orders/${id}/status`, { status })
  return data
}

export async function cancelOrder(id: number, reason?: string): Promise<void> {
  await apiClient.put(`/orders/${id}/cancel`, reason ? { reason } : {})
}

export async function confirmPickup(id: number): Promise<void> {
  await apiClient.put(`/orders/${id}/confirm-pickup`)
}

export async function deleteOrder(id: number): Promise<void> {
  await apiClient.delete(`/orders/${id}`)
}
