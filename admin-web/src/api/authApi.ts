import { apiClient } from './apiClient'
import type { LoginResponse } from '../types'

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', {
    identifier: username,
    password,
  })
  return data
}

export async function logout(refreshToken: string | null): Promise<void> {
  try {
    await apiClient.post('/auth/logout', refreshToken ? { refreshToken } : {})
  } catch {
    // ignore logout errors
  }
}

export async function refreshToken(refresh: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/refresh-token', { refreshToken: refresh })
  return data
}
