import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/authApi'
import { tokenStorage } from '../api/apiClient'
import type { Role } from '../types'

interface AuthContextValue {
  role: Role | null
  isAuthenticated: boolean
  isSuperAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

const ADMIN_ROLES: Role[] = ['ADMIN', 'SUPER_ADMIN']

export function AuthProvider({ children }: { children: ReactNode }) {
  const [role, setRole] = useState<Role | null>(() => (tokenStorage.getRole() as Role) || null)

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    const userRole = res.role as Role
    if (!ADMIN_ROLES.includes(userRole)) {
      throw new Error('Access denied. Admin credentials required.')
    }
    const access = res.accessJwt || res.jwt
    tokenStorage.set(access, res.refreshToken, userRole)
    setRole(userRole)
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout(tokenStorage.getRefresh())
    tokenStorage.clear()
    setRole(null)
  }, [])

  const value = useMemo(
    () => ({
      role,
      isAuthenticated: !!role && !!tokenStorage.getAccess(),
      isSuperAdmin: role === 'SUPER_ADMIN',
      login,
      logout,
    }),
    [role, login, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
