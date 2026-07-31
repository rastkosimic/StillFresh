import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

const TOKEN_KEY = 'sf_admin_access'
const REFRESH_KEY = 'sf_admin_refresh'
const ROLE_KEY = 'sf_admin_role'

export const tokenStorage = {
  getAccess: () => sessionStorage.getItem(TOKEN_KEY),
  getRefresh: () => sessionStorage.getItem(REFRESH_KEY),
  getRole: () => sessionStorage.getItem(ROLE_KEY) as string | null,
  set: (access: string, refresh: string, role: string) => {
    sessionStorage.setItem(TOKEN_KEY, access)
    sessionStorage.setItem(REFRESH_KEY, refresh)
    sessionStorage.setItem(ROLE_KEY, role)
  },
  clear: () => {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_KEY)
    sessionStorage.removeItem(ROLE_KEY)
  },
}

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refresh = tokenStorage.getRefresh()
  if (!refresh) return null
  try {
    const { data } = await axios.post(`${API_BASE}/auth/refresh-token`, { refreshToken: refresh })
    const access = data.accessJwt || data.jwt
    if (access && data.refreshToken) {
      tokenStorage.set(access, data.refreshToken, tokenStorage.getRole() || '')
      return access
    }
  } catch {
    tokenStorage.clear()
  }
  return null
}

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.getAccess()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true
      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null
        })
      }
      const newToken = await refreshPromise
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`
        return apiClient(original)
      }
      tokenStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export function getErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data
    if (typeof data === 'string') return data
    if (data && typeof data === 'object') {
      const obj = data as Record<string, unknown>
      if (typeof obj.message === 'string') return obj.message
      if (typeof obj.error === 'string') return obj.error
    }
    return err.message
  }
  if (err instanceof Error) return err.message
  return 'An unexpected error occurred'
}
