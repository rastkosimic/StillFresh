import { apiClient } from './apiClient'
import type { Vendor, VendorDashboard } from '../types'

export async function getAllVendors(): Promise<Vendor[]> {
  const { data } = await apiClient.get<Vendor[]>('/vendors/admin/all-vendors')
  return data
}

export async function getPendingVendors(): Promise<Vendor[]> {
  const { data } = await apiClient.get<Vendor[]>('/vendors/admin/pending-vendors')
  return data
}

export async function verifyAndActivate(vendorId: number): Promise<unknown> {
  const { data } = await apiClient.put(`/vendors/admin/${vendorId}/verify-and-activate`)
  return data
}

export async function activateVendor(id: number): Promise<void> {
  await apiClient.put(`/vendors/admin/vendors/${id}/activate`)
}

export async function deactivateVendor(id: number): Promise<void> {
  await apiClient.put(`/vendors/admin/vendors/${id}/deactivate`)
}

export async function deleteVendor(id: number): Promise<void> {
  await apiClient.delete(`/vendors/admin/vendors/${id}`)
}

export async function getVendorDashboard(vendorId: number, period = '7d'): Promise<VendorDashboard> {
  const { data } = await apiClient.get<VendorDashboard>(`/vendors/${vendorId}/dashboard`, {
    params: { period },
  })
  return data
}

export async function registerPendingVendor(payload: Record<string, unknown>): Promise<void> {
  await apiClient.post('/vendors/admin/register-pending', payload)
}

export async function createVendor(payload: Record<string, unknown>): Promise<void> {
  await apiClient.post('/vendors/admin/create-vendor', payload)
}

export async function updateVendor(id: number, payload: Record<string, unknown>): Promise<void> {
  await apiClient.put(`/vendors/admin/vendors/${id}`, payload)
}

export async function registerAdmin(payload: Record<string, unknown>): Promise<void> {
  await apiClient.post('/vendors/register-admin', payload)
}

export async function promoteToVendorAdmin(id: number): Promise<void> {
  await apiClient.put(`/vendors/${id}/promote-to-vendor-admin`)
}

export async function demoteFromVendorAdmin(id: number): Promise<void> {
  await apiClient.put(`/vendors/${id}/demote-from-vendor-admin`)
}
