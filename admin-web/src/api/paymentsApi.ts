import { apiClient } from './apiClient'
import type { BankTransferPayment, SpringPage } from '../types'

export async function getPlatformFee(): Promise<{ feePercent: number }> {
  const { data } = await apiClient.get<{ feePercent: number }>('/admin/platform/fee')
  return data
}

export async function updatePlatformFee(feePercent: number): Promise<{ feePercent: number }> {
  const { data } = await apiClient.put<{ feePercent: number }>('/admin/platform/fee', { feePercent })
  return data
}

export async function getMorPendingPayouts(): Promise<Record<string, unknown>[]> {
  const { data } = await apiClient.get<Record<string, unknown>[]>('/admin/mor/payouts/pending')
  return data
}

export async function getMorVendorBalances(): Promise<Record<string, unknown>[]> {
  const { data } = await apiClient.get<Record<string, unknown>[]>('/admin/mor/vendors/balances')
  return data
}

export async function getMorOrderTransactions(from?: string, to?: string): Promise<Record<string, unknown>[]> {
  const { data } = await apiClient.get<Record<string, unknown>[]>('/admin/mor/transactions/orders', {
    params: { from, to },
  })
  return data
}

export async function getMorPayoutSummary(): Promise<Record<string, unknown>> {
  const { data } = await apiClient.get<Record<string, unknown>>('/admin/mor/payouts/summary')
  return data
}

export async function updateMorPayoutStatus(payoutId: number, status: string): Promise<void> {
  await apiClient.put(`/admin/mor/payouts/${payoutId}/status`, { status })
}

export async function getPendingBankTransfers(page = 0, size = 20): Promise<SpringPage<BankTransferPayment>> {
  const { data } = await apiClient.get<SpringPage<BankTransferPayment>>('/payment/bank-transfer/pending', {
    params: { page, size },
  })
  return data
}

export async function confirmBankTransfer(reference: string): Promise<void> {
  await apiClient.post(`/payment/bank-transfer/confirm/${reference}`)
}

export async function cancelBankTransfer(reference: string): Promise<void> {
  await apiClient.post(`/payment/bank-transfer/cancel/${reference}`)
}

export async function getVendorBalance(vendorId: number): Promise<Record<string, unknown>> {
  const { data } = await apiClient.get<Record<string, unknown>>(`/ledger/balance/${vendorId}`)
  return data
}

export async function getVendorLedger(vendorId: number, page = 0, size = 20): Promise<SpringPage<Record<string, unknown>>> {
  const { data } = await apiClient.get<SpringPage<Record<string, unknown>>>(`/ledger/vendor/${vendorId}`, {
    params: { page, size },
  })
  return data
}
