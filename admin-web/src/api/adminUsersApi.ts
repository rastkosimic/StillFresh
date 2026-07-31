import { apiClient } from './apiClient'
import type { AuthUser } from '../types'

export async function getAllAuthUsers(): Promise<AuthUser[]> {
  const { data } = await apiClient.get<AuthUser[]>('/admin/users')
  return data
}

export async function getAuthUser(id: number): Promise<AuthUser> {
  const { data } = await apiClient.get<AuthUser>(`/admin/users/${id}`)
  return data
}

export async function activateUser(id: number): Promise<void> {
  await apiClient.put(`/admin/users/${id}/activate`)
}

export async function deactivateUser(id: number): Promise<void> {
  await apiClient.put(`/admin/users/${id}/deactivate`)
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/admin/users/${id}`)
}

export async function promoteUser(id: number): Promise<void> {
  await apiClient.post(`/admin/users/${id}/promote`)
}

export async function demoteUser(id: number): Promise<void> {
  await apiClient.put(`/admin/users/${id}/demote`)
}

export async function deleteAdmin(id: number): Promise<void> {
  await apiClient.delete(`/admin/admins/${id}`)
}
