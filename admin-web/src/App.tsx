import { BrowserRouter, Navigate, Route, Routes, Outlet } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from './auth/AuthContext'
import { ProtectedRoute, SuperAdminRoute } from './auth/ProtectedRoute'
import { AppLayout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { CustomersPage } from './pages/CustomersPage'
import { VendorsPage } from './pages/VendorsPage'
import { OrdersPage } from './pages/OrdersPage'
import { PaymentsPage } from './pages/PaymentsPage'
import { AdminsPage } from './pages/AdminsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
})

function LayoutShell() {
  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<LayoutShell />}>
                <Route index element={<DashboardPage />} />
                <Route path="customers" element={<CustomersPage />} />
                <Route path="vendors" element={<VendorsPage />} />
                <Route path="payments" element={<PaymentsPage />} />
                <Route element={<SuperAdminRoute />}>
                  <Route path="orders" element={<OrdersPage />} />
                  <Route path="admins" element={<AdminsPage />} />
                </Route>
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
