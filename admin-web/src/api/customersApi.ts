import { apiClient } from './apiClient'
import type { CustomerProfile } from '../types'

export async function getAllCustomers(): Promise<CustomerProfile[]> {
  const { data } = await apiClient.get<CustomerProfile[]>('/users/allUsers')
  return data
}
