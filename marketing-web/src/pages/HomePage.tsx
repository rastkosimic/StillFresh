import { Link } from 'react-router-dom'

export function HomePage() {
  return (
    <div className="page">
      <section className="hero" aria-labelledby="brand-title">
        <h1 id="brand-title" className="hero__brand">
          StillFresh
        </h1>
        <p className="hero__headline">Hrana koja zaslužuje drugu šansu.</p>
        <p className="hero__lead">
          Povezujemo lokalne prodavce i kupce oko viškova hrane — jeftinije za vas,
          manje bacanja za planetu.
        </p>
        <div className="hero__actions">
          <Link className="btn btn--primary" to="/uslovi">
            Uslovi za kupce
          </Link>
          <Link className="btn btn--ghost" to="/privatnost">
            Politika privatnosti
          </Link>
        </div>
      </section>

      <p className="home-note">
        Marketing sadržaj sajta stiže uskoro. Za sada ovde možete pročitati pravne
        dokumente Platforme, uključujući i{' '}
        <Link to="/uslovi-prodavci">uslove za prodavce</Link>.
      </p>
    </div>
  )
}
