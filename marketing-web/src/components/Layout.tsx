import { NavLink, Outlet } from 'react-router-dom'

const nav = [
  { to: '/privatnost', label: 'Privatnost' },
  { to: '/uslovi', label: 'Uslovi (kupci)' },
  { to: '/uslovi-prodavci', label: 'Uslovi (prodavci)' },
]

export function Layout() {
  return (
    <div className="site-shell">
      <header className="site-header">
        <div className="site-header__inner">
          <NavLink to="/" className="brand" end>
            <img src="/logo.png" alt="" />
            StillFresh
          </NavLink>
          <nav className="nav" aria-label="Glavna navigacija">
            {nav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                style={({ isActive }) => ({
                  color: isActive ? 'var(--green)' : undefined,
                })}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main className="site-main">
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="site-footer__inner">
          <p>© {new Date().getFullYear()} StillFresh d.o.o. Beograd</p>
          <nav aria-label="Pravni dokumenti">
            {nav.map((item) => (
              <NavLink key={item.to} to={item.to}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </footer>
    </div>
  )
}
