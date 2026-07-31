import { apiClient } from './apiClient'
import type { PayoutBatch, SpringPage, VendorPayoutItem } from '../types'

export async function getPayoutBatches(page = 0, size = 20): Promise<SpringPage<PayoutBatch>> {
  const { data } = await apiClient.get<SpringPage<PayoutBatch>>('/ledger/payouts', { params: { page, size } })
  return data
}

export async function getPayoutBatch(batchId: number): Promise<PayoutBatch & { items?: VendorPayoutItem[] }> {
  const { data } = await apiClient.get(`/ledger/payouts/${batchId}`)
  return data
}

export async function getBatchItems(batchId: number): Promise<VendorPayoutItem[]> {
  const { data } = await apiClient.get<VendorPayoutItem[]>(`/ledger/payouts/${batchId}/items`)
  return data
}

export async function runPayoutJob(): Promise<unknown> {
  const { data } = await apiClient.post('/ledger/payouts/run')
  return data
}

export async function dryRunBatch(batchId: number): Promise<unknown> {
  const { data } = await apiClient.get(`/ledger/payouts/${batchId}/dry-run`)
  return data
}

export async function approveBatch(batchId: number): Promise<void> {
  await apiClient.post(`/ledger/payouts/${batchId}/approve`)
}

export async function executeBatch(batchId: number): Promise<void> {
  await apiClient.post(`/ledger/payouts/${batchId}/execute`)
}

export async function retryFailedBatch(batchId: number): Promise<void> {
  await apiClient.post(`/ledger/payouts/${batchId}/retry-failed`)
}

export async function holdBatch(batchId: number): Promise<void> {
  await apiClient.post(`/ledger/payouts/${batchId}/hold`)
}

export async function releaseBatch(batchId: number): Promise<void> {
  await apiClient.post(`/ledger/payouts/${batchId}/release`)
}

export async function cancelBatch(batchId: number): Promise<void> {
  await apiClient.post(`/ledger/payouts/${batchId}/cancel`)
}

export async function pauseAutoPayouts(): Promise<void> {
  await apiClient.post('/ledger/payouts/auto/pause')
}

export async function resumeAutoPayouts(): Promise<void> {
  await apiClient.post('/ledger/payouts/auto/resume')
}

export async function getAutoPayoutStatus(): Promise<Record<string, unknown>> {
  const { data } = await apiClient.get<Record<string, unknown>>('/ledger/payouts/auto/status')
  return data
}

export async function getReconciliationReport(): Promise<Record<string, unknown>> {
  const { data } = await apiClient.get<Record<string, unknown>>('/ledger/payouts/reconciliation/report')
  return data
}
