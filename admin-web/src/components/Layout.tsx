import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Button } from './ui'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/customers', label: 'Customers' },
  { to: '/vendors', label: 'Vendors' },
  { to: '/orders', label: 'Orders', superAdminOnly: true },
  { to: '/payments', label: 'Payments' },
  { to: '/admins', label: 'Admin Users', superAdminOnly: true },
]

export function Sidebar() {
  const { role, isSuperAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <aside className="fixed inset-y-0 left-0 flex w-64 flex-col bg-brand-green text-white">
      <div className="flex items-center gap-3 border-b border-white/10 px-5 py-5">
        <img src="/logo.png" alt="StillFresh" className="h-10 w-10 rounded-full object-cover" />
        <div>
          <p className="font-semibold leading-tight">StillFresh</p>
          <p className="text-xs text-white/70">Super Admin</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 px-3 py-4">
        {navItems
          .filter((item) => !item.superAdminOnly || isSuperAdmin)
          .map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `block rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                  isActive
                    ? 'bg-brand-orange text-white'
                    : 'text-white/85 hover:bg-white/10 hover:text-white'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
      </nav>

      <div className="border-t border-white/10 px-5 py-4">
        <p className="truncate text-xs text-white/60">Signed in as</p>
        <p className="truncate text-sm font-medium">{role}</p>
        <Button variant="danger" className="mt-3 w-full" onClick={handleLogout}>
          Log out
        </Button>
      </div>
    </aside>
  )
}

export function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-brand-cream">
      <Sidebar />
      <main className="ml-64 min-h-screen p-8">{children}</main>
    </div>
  )
}
