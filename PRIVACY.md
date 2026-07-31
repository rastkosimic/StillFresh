# Politika privatnosti i zaštite podataka o ličnosti

**Platforma StillFresh**

*Poslednje ažuriranje: 23. jul 2026.*

Ova Politika privatnosti objašnjava koje podatke o ličnosti prikuplja i obrađuje privredno društvo **StillFresh d.o.o.** putem mobilne aplikacije i povezanih tehničkih sistema (u daljem tekstu: **Platforma**), u koje svrhe, na kom pravnom osnovu, kome se podaci mogu saopštiti i koja prava imate u skladu sa Zakonom o zaštiti podataka o ličnosti („**ZZPL**“).

---

## 1. Rukovalac podacima

Rukovalac vašim podacima o ličnosti je:

| | |
|---|---|
| **Naziv** | StillFresh d.o.o. Beograd |
| **Sedište** | [Uneti adresu] |
| **PIB** | [Uneti PIB] |
| **Kontakt za zaštitu podataka** | [Uneti imejl, npr. privacy@stillfresh.rs] |

Za sva pitanja u vezi sa obradom podataka o ličnosti, ostvarivanje prava iz ZZPL-a ili povlačenje pristanka, možete nas kontaktirati na navedeni imejl.

---

## 2. Kategorije korisnika

Platforma obrađuje podatke sledećih kategorija lica:

- **Kupci** — fizička lica koja kreiraju korisnički nalog i rezervišu pakete hrane;
- **Prodavci** — privredni subjekti i njihovi ovlašćeni korisnici (admin lokacije / radnik) koji objavljuju ponude i predaju pakete;
- **Administratori Platforme** — interni korisnici StillFresh-a koji upravljaju sistemom (njihova obrada regulisana je i internim aktima).

---

## 3. Koje podatke prikupljamo i u koje svrhe

Prikupljamo samo podatke koji su neophodni za funkcionisanje Platforme, izvršenje ugovora, bezbednost i poštovanje zakonskih obaveza.

### 3.1. Podaci o nalogu kupca

| Podaci | Svrha |
|---|---|
| Korisničko ime, imejl adresa, lozinka (u heširanom obliku) | Kreiranje naloga, autentifikacija, komunikacija |
| Ime, prezime, broj telefona, adresa, država | Profil, kontakt u vezi sa rezervacijom |
| Datum rođenja, prehrambene preferencije (opciono) | Personalizacija iskustva na Platformi |
| Identifikator Google naloga (`sub`) ako se prijavljujete preko Google-a | OAuth prijava bez lozinke |

**Pravni osnov:** izvršenje ugovora (član 12. stav 1. tačka 2) ZZPL-a); za opcione podatke profila — pristanak ili legitimni interes, zavisno od konteksta.

### 3.2. Podaci o nalogu prodavca

| Podaci | Svrha |
|---|---|
| Korisničko ime, imejl, lozinka (heš), telefon, kontakt osoba | Nalog i komunikacija |
| Naziv lokacije / lanca, adresa, poštanski broj, država, GPS koordinate lokacije, radno vreme, opis poslovanja, slike | Prikaz ponuda, navigacija kupaca do preuzimanja |
| Poreski / matični identifikatori poslovanja | Identifikacija privrednog subjekta, usklađenost |
| Podaci o tekućem računu (vlasnik računa, broj računa / IBAN, banka, SWIFT) | Isplata sredstava prodavcu (MoR model) |
| Identifikator Stripe Connect naloga (ako je uključen taj model isplate) | Isplata preko Stripe Connect-a |

**Pravni osnov:** izvršenje ugovora; zakonske obaveze (računovodstvo, poreski propisi); legitimni interes za sprečavanje zloupotreba.

### 3.3. Podaci o rezervacijama i porudžbinama

- Identifikatori kupca, prodavca i ponude;
- količina, cene, valuta (npr. RSD), status porudžbine;
- rok i podaci o preuzimanju (adresa / naziv lokacije, koordinate mesta preuzimanja);
- referenca plaćanja i podaci o obračunu (bruto iznos, provizija Platforme, neto iznos za prodavca).

**Svrha:** rezervacija paketa, obaveštavanje, preuzimanje, finansijsko zatvaranje transakcije i podrška korisnicima.

### 3.4. Omiljene ponude i ocene

- Lista omiljenih ponuda (`favorites`);
- ocene prodavca nakon preuzimanja (proces preuzimanja, kvalitet, količina, raznovrsnost) vezane za porudžbinu.

**Svrha:** funkcionalnost aplikacije i unapređenje kvaliteta usluge.

### 3.5. Podaci o lokaciji (geolokacija)

| Situacija | Šta se obrađuje | Svrha |
|---|---|---|
| Pretraga ponuda u blizini | GPS koordinate koje aplikacija pošalje u zahtev (ne čuvaju se kao trajni profil kupca) | Prikaz i sortiranje ponuda po udaljenosti |
| Lokacija prodavca / ponude | Trajno sačuvane koordinate mesta preuzimanja | Prikaz mape i navigacija |
| Otkazivanje rezervacije od strane kupca | Opcione koordinate kupca u trenutku otkazivanja, upoređene sa mestom preuzimanja (prag ~50 m) | Sprečavanje zloupotreba i neovlašćenog zaobilaženja Platforme („bypass“) |

**Pravni osnov za anti-bypass geolokaciju:** legitimni interes Rukovaoca (zaštita Platforme i prodavaca od prevara). Opoziv dozvole za lokaciju može ograničiti ili onemogućiti pojedine funkcije (npr. pretragu u blizini ili potpunu anti-bypass proveru pri otkazivanju).

### 3.6. Obaveštenja

- Token uređaja za push notifikacije (Firebase Cloud Messaging);
- podešavanja obaveštenja (push / imejl, tipovi događaja);
- sadržaj transakcionih i uslužnih obaveštenja (status porudžbine, podsetnik za preuzimanje, plaćanje, promena modela isplate i slično).

**Pravni osnov:** izvršenje ugovora za transakciona obaveštenja; **pristanak** za marketinške / neobavezne push i imejl poruke.

### 3.7. Tehnički i bezbednosni podaci

- JWT pristupni i refresh tokeni (uključujući evidenciju opozvanih tokena);
- brojači kršenja pravila (npr. nepojavljivanje / sumnja na bypass);
- opciona povratna informacija pri brisanju naloga (razlog i poruka).

---

## 4. Podaci o platnim karticama i plaćanjima

Za procesiranje uplata i preautorizaciju sredstava Platforma koristi spoljne licencirane pružaoce platnih usluga, u zavisnosti od konfiguracije:

### 4.1. AllSecure (primarni model za Srbiju)

- Partner: **AllSecure d.o.o. Beograd** (PCI DSS Level 1).
- **StillFresh d.o.o. nikada ne prikuplja, ne vidi i ne skladišti** broj platne kartice (PAN), CVV kod niti potpune podatke kartice na svojim serverima.
- Podaci kartice unose se u zaštićeni AllSecure tok (npr. hosted / redirect sa 3-D Secure proverom).
- AllSecure sistemu Platforme vraća identifikatore (npr. `referenceId` / `registrationId`), a Platforma lokalno može čuvati samo tokenizovane reference i neosetljive metapodatke (brend kartice, poslednje 4 cifre, mesec/godina isteka) radi ponovnog plaćanja i prikaza u aplikaciji.

### 4.2. Stripe / Stripe Connect (ako je omogućeno)

- Za pojedine tokove plaćanja ili isplate prodavcima Platforma može koristiti **Stripe** (uključujući Stripe Connect).
- Podaci kartice se obrađuju kod Stripe-a; Platforma čuva identifikatore kupca / naloga i metode plaćanja koje Stripe dodeli.

### 4.3. Ostali načini plaćanja

- **Bankarski transfer** na račun Platforme — čuvaju se referenca uplate i iznosi;
- u modelu **Merchant of Record (MoR)** Platforma nastupa kao prodavac prema kupcu, a prodavci kao dobavljači kojima se vrši isplata.

---

## 5. Pravni osnov za obradu

U skladu sa članom 12. ZZPL-a, podatke obrađujemo na sledećim osnovama:

1. **Izvršenje ugovora** — nalog, rezervacija, plaćanje, preuzimanje, isplata prodavcu, korisnička podrška;
2. **Zakonska obaveza** — računovodstvena i poreska dokumentacija, zahtevi nadležnih organa;
3. **Legitimni interes** — sprečavanje prevara i zloupotreba (uključujući geoprolu pri otkazivanju), bezbednost sistema, unapređenje usluge u meri koja ne narušava vaša prava;
4. **Pristanak** — marketinške komunikacije i opcione funkcije koje zahtevaju pristanak; pristanak možete povući u bilo kom trenutku bez uticaja na zakonitost obrade pre povlačenja.

---

## 6. Sa kim delimo podatke

Vaši podaci se **ne prodaju**. Saopštavaju se samo kada je to neophodno:

| Primalac | Razlog |
|---|---|
| **Prodavac** kod koga rezervišete paket | Ime / identifikacija rezervacije radi verifikacije preuzimanja i eventualne fiskalizacije |
| **AllSecure** | Autorizacija, preautorizacija, naplata i poništavanje transakcija |
| **Stripe** (ako je uključeno) | Plaćanje kupaca i/ili Connect isplate prodavcima |
| **Poslovna banka / rail za isplate (npr. Raiffeisen CMIplus)** | B2B nalozi za prenos sredstava prodavcima |
| **Mailgun** | Slanje transakcionih i (uz pristanak) drugih imejl poruka |
| **Google** | Prijava preko Google naloga (OAuth) |
| **Firebase (Google)** | Dostava push notifikacija |
| **Hosting / infrastruktura** (baze podataka, keš, red poruka) | Tehnički rad Platforme pod kontrolom Rukovaoca |
| **Državni organi** | Isključivo na osnovu zakona ili naloga (npr. Poreska uprava, NBS, sud) |

Kada Platforma angažuje obrađivače, sa njima se ugovaraju odgovarajuće obaveze zaštite podataka.

---

## 7. Prenos podataka u inostranstvo

Pojedini pružaoci usluga (npr. Google, Firebase, Stripe, Mailgun) mogu obrađivati podatke van Republike Srbije. U tom slučaju Rukovalac preduzima mere predviđene ZZPL-om (ugovorne klauzule, ocena rizika i slično), u meri u kojoj se ti prenosi primenjuju na konkretnu konfiguraciju Platforme.

---

## 8. Bezbednost podataka

- Podaci se čuvaju u odvojenim **PostgreSQL** bazama po mikroservisima;
- komunikacija aplikacije sa API-jem vrši se preko **HTTPS (TLS)**;
- pristup API-ju autorizuje se **vremenski ograničenim JWT tokenima**; pri odjavi / brisanju naloga tokeni se opozivaju (blacklist);
- lozinke se ne čuvaju u čitljivom obliku (heširanje);
- podaci kartica ne prolaze kroz StillFresh servere u punom obliku (tokenizacija kod platnog partnera).

Nijedna mera ne garantuje apsolutnu bezbednost; u slučaju incidenta koji ugrožava vaša prava, postupićemo u skladu sa ZZPL-om.

---

## 9. Rokovi čuvanja

| Kategorija | Rok |
|---|---|
| Aktivni korisnički nalog | Dok je nalog aktivan i koliko je potrebno za pružanje usluge |
| Zahtev za brisanje naloga | Nalog se deaktivira (status obrisan); podaci o ličnosti se brišu ili anonimizuju u roku od **30 dana**, osim ispod navedenih izuzetaka |
| Finansijski i računovodstveni tragovi (transakcije, isplate, fakturisanje) | U skladu sa Zakonom o računovodstvu i drugim propisima — tipično do **5 godina** ili duže ako propis zahteva |
| Push / in-app obaveštenja | Ograničen rok zadržavanja (npr. oko 90 dana za starije zapise obaveštenja) |
| Refresh tokeni | Ograničen TTL (red veličine do oko 30 dana) |
| Povratna informacija o razlogu brisanja naloga | Može se zadržati u anonimizovanom ili agregiranom obliku radi analitike kvaliteta usluge |

**Napomena:** ponovna prijava na prethodno obrisani nalog može, u tehničkom smislu, ponovo aktivirati isti nalog ako sistem to podržava; ako želite trajno brisanje bez mogućnosti reaktivacije, naznačite to u zahtevu na kontakt imejl.

---

## 10. Vaša prava (ZZPL)

Imate pravo na:

1. **Pristup** — da saznate da li obrađujemo vaše podatke i da dobijete kopiju;
2. **Ispravku** — ispravku netačnih ili dopunu nepotpunih podataka;
3. **Brisanje** („pravo na zaborav“) — ako su ispunjeni uslovi ZZPL-a i nemate aktivne obaveze ili sporove na Platformi;
4. **Ograničenje obrade** — u slučajevima predviđenim zakonom;
5. **Prigovor** — na obradu zasnovanu na legitimnom interesu;
6. **Opoziv pristanka** — za obrade koje se zasnivaju na pristanku (npr. marketing, opcione notifikacije);
7. **Podnošenje pritužbe** — Povereniku za informacije od javnog značaja i zaštitu podataka o ličnosti.

Za ostvarivanje prava kontaktirajte: **[Uneti imejl, npr. privacy@stillfresh.rs]**.

Opoziv dozvole za lokaciju na uređaju može uticati na funkcije koje od nje zavise (pretraga u blizini, anti-bypass provera pri otkazivanju).

---

## 11. Deca

Platforma nije namenjena licima mlađim od 15 godina (odnosno uzrasta ispod kog je potreban pristanak roditelja / staratelja prema ZZPL-u). Ne prikupljamo svesno podatke dece. Ako smatrate da je dete dostavilo podatke, kontaktirajte nas radi brisanja.

---

## 12. Izmene ove Politike

Ovu Politiku možemo povremeno ažurirati. Nova verzija biće objavljena na Platformi / na ovoj stranici, sa datumom poslednjeg ažuriranja. Za bitne izmene možemo vas dodatno obavestiti putem aplikacije ili imejla.

---

## 13. Kontakt

**StillFresh d.o.o. Beograd**  
Sedište: [Uneti adresu]  
PIB: [Uneti PIB]  
Imejl: [Uneti imejl, npr. privacy@stillfresh.rs]
