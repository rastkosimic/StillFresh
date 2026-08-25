import { NavLink, Link, Outlet } from 'react-router-dom'

const sectionNav = [
  { id: 'kako-radi', label: 'Kako radi' },
  { id: 'za-prodavce', label: 'Za prodavce' },
  { id: 'preuzmi', label: 'Preuzmi' },
]

const legalNav = [
  { to: '/privatnost', label: 'Privatnost' },
  { to: '/uslovi', label: 'Uslovi (kupci)' },
  { to: '/uslovi-prodavci', label: 'Uslovi (prodavci)' },
]

function handleSectionClick(
  e: React.MouseEvent<HTMLAnchorElement>,
  id: string,
) {
  if (window.location.pathname === '/') {
    e.preventDefault()
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  }
}

export function Layout() {
  return (
    <div className="site-shell">
      <header className="site-header">
        <div className="site-header__inner">
          <Link to="/" className="brand">
            <img src="/logo.png" alt="" />
            Još Sveže
          </Link>
          <nav className="nav" aria-label="Glavna navigacija">
            {sectionNav.map((item) => (
              <a
                key={item.id}
                href={`/#${item.id}`}
                onClick={(e) => handleSectionClick(e, item.id)}
              >
                {item.label}
              </a>
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
            {legalNav.map((item) => (
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
