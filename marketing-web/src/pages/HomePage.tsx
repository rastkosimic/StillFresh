import { FormEvent, useState } from 'react'

// TODO: zameni pravim linkom kad aplikacija bude objavljena na Google Play-u.
// Dok je prazno, dugme prikazuje "Uskoro na Google Play" bez linka.
const PLAY_STORE_URL = ''

// TODO: zameni svojim Formspree endpoint-om (npr. "https://formspree.io/f/xxxx").
// Dok je prazno, forma radi u demo režimu (lead se ispiše u konzolu).
const VENDOR_FORM_ENDPOINT = ''

const VENDOR_TYPES = [
  'Pekara',
  'Restoran',
  'Kafić / fast food',
  'Market / prodavnica',
  'Hotel',
  'Drugo',
] as const

type FormState = {
  name: string
  business: string
  type: string
  phone: string
  email: string
}

type Status = 'idle' | 'sending' | 'sent' | 'error'

const initialForm: FormState = {
  name: '',
  business: '',
  type: VENDOR_TYPES[0],
  phone: '',
  email: '',
}

export function HomePage() {
  const [form, setForm] = useState<FormState>(initialForm)
  const [status, setStatus] = useState<Status>('idle')

  function handleChange(
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setStatus('sending')
    try {
      if (VENDOR_FORM_ENDPOINT) {
        const res = await fetch(VENDOR_FORM_ENDPOINT, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
          },
          body: JSON.stringify(form),
        })
        if (!res.ok) throw new Error('Mrežna greška')
      } else {
        console.info('[Još Sveže] Vendor lead (demo):', form)
      }
      setStatus('sent')
    } catch {
      setStatus('error')
    }
  }

  return (
    <>
      {/* HERO */}
      <section className="hero" id="vrh">
        <p className="hero__badge">🌿 Beograd · Uskoro</p>
        <h1 className="hero__brand">Još Sveže</h1>
        <p className="hero__headline">Hrana koja zaslužuje drugu šansu.</p>
        <p className="hero__lead">
          Pekare, restorani i marketi na kraju dana prodaju višak kao „paket
          iznenađenja“ po ~1/3 cene. Rezervišeš u aplikaciji i podigneš u
          zakazano vreme — jeftinije za tebe, manje bacanja za planetu.
        </p>
        <div className="hero__actions">
          {PLAY_STORE_URL ? (
            <a
              className="btn btn--primary"
              href={PLAY_STORE_URL}
              target="_blank"
              rel="noreferrer"
            >
              Preuzmi aplikaciju
            </a>
          ) : (
            <span className="btn btn--primary btn--soon">
              ⏳ Uskoro na Google Play
            </span>
          )}
          <a className="btn btn--ghost" href="#za-prodavce">
            Postani partner →
          </a>
        </div>
        <p className="hero__note">
          Za prodavce: <strong>0% provizije</strong> prva 3 meseca.
        </p>
      </section>

      {/* KAKO RADI */}
      <section className="section" id="kako-radi">
        <div className="section__head">
          <p className="section__eyebrow">Kako radi</p>
          <h2 className="section__title">
            Dobra hrana ne završava u kanti.
          </h2>
          <p className="section__lead">
            Tri koraka — jednostavno za kupce, isplativo za prodavce.
          </p>
        </div>
        <div className="steps">
          <article className="step">
            <span className="step__num" aria-hidden="true">
              1
            </span>
            <h3 className="step__title">Prodavac pakuje višak</h3>
            <p className="step__text">
              Na kraju radnog dana, umesto bacanja — „paket iznenađenja“ po
              ~1/3 redovne cene.
            </p>
          </article>
          <article className="step">
            <span className="step__num" aria-hidden="true">
              2
            </span>
            <h3 className="step__title">Ti rezervišeš</h3>
            <p className="step__text">
              U aplikaciji vidiš koji objekti imaju pakete, biraš i rezervišeš
              termin preuzimanja.
            </p>
          </article>
          <article className="step">
            <span className="step__num" aria-hidden="true">
              3
            </span>
            <h3 className="step__title">Podigneš i uživaš</h3>
            <p className="step__text">
              Pokupiš paket u zakazano vreme. Ušteda, iznenađenje i malo manje
              bačene hrane.
            </p>
          </article>
        </div>
      </section>

      {/* ZA PRODAVCE */}
      <section className="section section--vendor" id="za-prodavce">
        <div className="vendor-grid">
          <div className="vendor-info">
            <p className="section__eyebrow">Za prodavce</p>
            <h2 className="section__title">
              Zaradi na hrani koju bi bacio.
            </h2>
            <p className="section__lead">
              Višak nije gubitak — može da postane prihod. Mi donosimo
              mušterije, ti biraš cenu i vreme.
            </p>
            <ul className="vendor-benefits">
              <li>
                <strong>0% provizije</strong> prva 3 meseca
              </li>
              <li>Ti postavljaš cenu paketa i termin preuzimanja</li>
              <li>Nove mušterije i besplatna promocija tvog objekta</li>
              <li>Probni period od 2 nedelje — pa odluči</li>
            </ul>
          </div>

          <form className="lead-form" onSubmit={handleSubmit}>
            <h3 className="lead-form__title">Prijavi svoj objekat</h3>
            <p className="lead-form__hint">
              Ostavi kontakt — javljamo se u roku od 24h.
            </p>

            {status === 'sent' ? (
              <div className="lead-form__success" role="status">
                <strong>Hvala!</strong> 🎉 Tvoja prijava je stigla. Javljamo se
                u roku od 24h sa sledećim koracima.
              </div>
            ) : (
              <>
                <label className="field">
                  <span>Ime i prezime *</span>
                  <input
                    name="name"
                    value={form.name}
                    onChange={handleChange}
                    required
                    placeholder="npr. Marko Marković"
                  />
                </label>
                <label className="field">
                  <span>Naziv objekta *</span>
                  <input
                    name="business"
                    value={form.business}
                    onChange={handleChange}
                    required
                    placeholder="npr. Pekara „Zlatno klasje“"
                  />
                </label>
                <label className="field">
                  <span>Tip objekta</span>
                  <select name="type" value={form.type} onChange={handleChange}>
                    {VENDOR_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Telefon *</span>
                  <input
                    name="phone"
                    type="tel"
                    value={form.phone}
                    onChange={handleChange}
                    required
                    placeholder="+381 6x xxx xxxx"
                  />
                </label>
                <label className="field">
                  <span>Email</span>
                  <input
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={handleChange}
                    placeholder="adresa@email.com"
                  />
                </label>

                {status === 'error' && (
                  <p className="lead-form__error" role="alert">
                    Došlo je do greške pri slanju. Pokušaj ponovo ili piši na
                    partneri@jossveze.rs
                  </p>
                )}

                <button
                  className="btn btn--submit"
                  type="submit"
                  disabled={status === 'sending'}
                >
                  {status === 'sending' ? 'Šaljemo...' : 'Prijavi se kao partner'}
                </button>
                <p className="lead-form__privacy">
                  Slanjem prijave prihvataš{' '}
                  <a href="/uslovi-prodavci">uslove za prodavce</a> i{' '}
                  <a href="/privatnost">politiku privatnosti</a>.
                </p>
              </>
            )}
          </form>
        </div>
      </section>

      {/* PREUZMI */}
      <section className="section section--download" id="preuzmi">
        <h2 className="section__title">Spasi hranu. Uštedi novac.</h2>
        <p className="section__lead">
          Još Sveže uskoro u Beogradu — prvi paketi čim objavimo aplikaciju.
          Prijavi se i budi među prvima.
        </p>
        {PLAY_STORE_URL ? (
          <a
            className="btn btn--primary btn--lg"
            href={PLAY_STORE_URL}
            target="_blank"
            rel="noreferrer"
          >
            Preuzmi sa Google Play
          </a>
        ) : (
          <span className="btn btn--primary btn--lg btn--soon">
            ⏳ Uskoro na Google Play
          </span>
        )}
      </section>
    </>
  )
}
