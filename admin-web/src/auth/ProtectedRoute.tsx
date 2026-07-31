import { Navigate, Outlet } from 'react-router-dom'
import { tokenStorage } from '../api/apiClient'
import { useAuth } from './AuthContext'

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated && !tokenStorage.getAccess()) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}

export function SuperAdminRoute() {
  const { isSuperAdmin } = useAuth()
  if (!isSuperAdmin) {
    return (
      <div className="rounded-lg border border-amber-300 bg-amber-50 p-6 text-amber-900">
        This section requires SUPER_ADMIN access.
      </div>
    )
  }
  return <Outlet />
}
