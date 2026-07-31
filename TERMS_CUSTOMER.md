# Opšti uslovi korišćenja mobilne aplikacije i usluga posredovanja

**Relacija: StillFresh (Platforma) – Kupac**

*Poslednje ažuriranje: 23. jul 2026.*

Ovim Opštim uslovima korišćenja (u daljem tekstu: **Uslovi**) uređuju se prava i obaveze između privrednog društva **StillFresh d.o.o. Beograd** (u daljem tekstu: **Platforma**) i fizičkog lica koje koristi StillFresh mobilnu aplikaciju i povezane sisteme radi rezervacije i kupovine paketa hrane (u daljem tekstu: **Kupac**).

Prihvatanjem ovih Uslova (prilikom registracije, prijave ili nastavka korišćenja aplikacije) smatra se da je zaključen **ugovor po pristupu** između Platforme i Kupca.

---

## 1. Predmet i definicije

| Pojam | Značenje |
|---|---|
| **Platforma** | StillFresh d.o.o. Beograd, sedište: [Uneti adresu], PIB: [Uneti PIB] |
| **Kupac** | Fizičko lice koje koristi aplikaciju radi rezervacije Paketa |
| **Prodavac** | Pravno lice ili preduzetnik koji putem aplikacije nudi viškove / pakete hrane |
| **Paket / Ponuda** | Ponuda hrane koju Prodavac sastavlja i nudi na prodaju (cena, količina, mesto i vreme preuzimanja) |
| **Vremenski okvir** | Interval preuzimanja koji određuje Prodavac (npr. 20:00–21:00) |
| **Preautorizacija (Hold)** | Privremeno rezervisanje / blokiranje sredstava na kartici Kupca |
| **Naplata (Capture)** | Trajno zaduženje / skidanje prethodno rezervisanog iznosa |
| **Storno (Void)** | Oslobađanje preautorizacije bez trajne naplate |

Kontakt: **[Uneti imejl, npr. support@stillfresh.rs]**.  
Politika privatnosti: dokument `PRIVACY.md` / javna stranica Platforme.  
Uslovi za prodavce: dokument `TERMS_VENDOR.md` (primenjuju se na odnos Platforma–Prodavac).

---

## 2. Priroda usluge Platforme

2.1. Platforma obezbeđuje tehničku infrastrukturu (aplikacija, rezervacije, obaveštenja) i platni tok preko licenciranog pružaoca platnih usluga (**AllSecure** i/ili drugih partnera koje Platforma odredi, npr. Stripe gde je omogućeno).

2.2. Platforma deluje kao **posrednik** i **zastupnik u naplati** u ime i za račun Prodavaca i/ili, u modelu **Merchant of Record (MoR)**, tehnički prima uplatu na račun Platforme radi dalje isplate Prodavcu (umanjene za proviziju Platforme).

2.3. **Ugovor o kupoprodaji hrane** zaključuje se između Kupca i Prodavca. Platforma **nije proizvođač ni vlasnik hrane** i ne garantuje ukus, sastav, alergenе ili zdravstvenu ispravnost, osim u meri u kojoj imperativni propisi drugačije nalažu.

2.4. Fiskalni račun za pun iznos Paketa izdaje **Prodavac** Kupcu, u skladu sa propisima (npr. oznaka plaćanja „Prodaja preko posrednika“).

---

## 3. Registracija i nalog

3.1. Kupac se registruje unošenjem tačnih podataka (npr. korisničko ime, imejl, lozinka) i prihvatanjem ovih Uslova i Politike privatnosti. Moguća je i prijava putem **Google** naloga (OAuth).

3.2. Kupac je odgovoran za tajnost pristupnih podataka i za sve radnje izvršene preko svog naloga.

3.3. Zabranjeno je kreiranje lažnih naloga, korišćenje tuđih podataka i zloupotreba Platforme.

3.4. Nalog može biti u statusu aktivan, neaktivan, obrisan ili **suspendovan**. Suspendovani nalog ne može normalno koristiti usluge dok Platforma ne odluči drugačije.

3.5. Kupac može zahtevati brisanje / deaktivaciju naloga putem aplikacije, u skladu sa Politikom privatnosti. Finansijski tragovi transakcija čuvaju se u zakonskim rokovima.

---

## 4. Platne kartice i tokenizacija

4.1. Plaćanje se vrši **platnim karticama** koje podržava platni partner (npr. Visa, Mastercard, Maestro, DinaCard — u zavisnosti od AllSecure / banke izdavaoca).

4.2. Podaci o kartici (pun broj, CVV) **ne čuvaju se** na serverima Platforme. Unose se u zaštićeni tok pružaoca platnih usluga (**AllSecure**, PCI DSS Level 1), uključujući eventualnu **3-D Secure** proveru.

4.3. Kupac je saglasan da se kartica **tokenizuje** i poveže sa nalogom (čuvaju se identifikatori i neosetljivi metapodaci, npr. poslednje 4 cifre, brend, rok važenja) radi budućih rezervacija.

4.4. Kupac može ukloniti sačuvanu metodu plaćanja u aplikaciji, u skladu sa dostupnim funkcijama.

4.5. Platforma može, u pojedinim slučajevima, omogućiti i druge načine plaćanja (npr. bankarski transfer), o čemu će Kupac biti obavešten u aplikaciji.

---

## 5. Rezervacija i preautorizacija (Hold)

5.1. Izborom Ponude i potvrdom rezervacije (npr. dugme tipa „Rezerviši paket“) Kupac zaključuje rezervaciju Paketa, pod uslovom da postoji važeća tokenizovana kartica i da je Ponuda dostupna.

5.2. U momentu rezervacije Platforma (preko platnog partnera) vrši **preautorizaciju** sredstava u visini vrednosti Paketa. Novac **nije trajno skinut**, već je blokiran od strane banke kao garancija ozbiljnosti rezervacije.

5.3. Kupac može od svoje banke dobiti SMS / push obaveštenje o rezervaciji / privremenom zaduženju — to je standardna bankarska komunikacija, ne naplata Platforme.

5.4. Uspešna rezervacija smanjuje dostupnu količinu Ponude i kreira porudžbinu u statusu rezervisanosti (tehnički: npr. `CONFIRMED`).

5.5. Ako preautorizacija ili 3-D Secure provera ne uspe, rezervacija se ne završava uspešno.

---

## 6. Preuzimanje, naplata i fiskalizacija

6.1. Svaki Paket ima **Vremenski okvir** za preuzimanje koji određuje Prodavac. Kupac je dužan da dođe na prodajno mesto **unutar tog okvira**.

6.2. Nakon preuzimanja hrane, Kupac je dužan da u aplikaciji potvrdi preuzimanje (npr. dugme „Preuzeto“ / potvrda preuzimanja).

6.3. Potvrda preuzimanja predstavlja **elektronski nalog** Platformi da izvrši **konačnu naplatu (Capture)** prethodno rezervisanog iznosa sa kartice Kupca.

6.4. U momentu preuzimanja, Prodavac je dužan da Kupcu izda **e-fiskalni račun** na pun iznos porudžbine, sa oznakom plaćanja odgovarajućom posredničkoj prodaji (npr. „Prodaja preko posrednika“).

6.5. Platforma može slati podsetnike o preuzimanju (push / imejl, u skladu sa podešavanjima obaveštenja).

---

## 7. Otkazivanje rezervacije od strane Kupca

7.1. Kupac ima pravo da poništi rezervaciju klikom na odgovarajuću akciju u aplikaciji (npr. „Poništi rezervaciju“), dok je porudžbina u statusu koji dozvoljava otkazivanje.

7.2. Pri otkazivanju sistem vrši **storno (void)** preautorizacije. Sredstva se oslobađaju na raspolaganje Kupcu **bez penala** i bez troška refundacije od strane Platforme. Rok vidljivosti oslobođenih sredstava zavisi od banke izdavaoca kartice.

7.3. Radi sprečavanja zloupotreba, aplikacija može zahtevati **dozvolu za GPS lokaciju** prilikom otkazivanja. Ako Kupac odbije deljenje lokacije, otkazivanje može biti ograničeno ili onemogućeno u aplikaciji (u skladu sa tehničkom konfiguracijom).

7.4. Otkazivanje u neposrednoj blizini lokala Prodavca tokom Vremenskog okvira podleže pravilima iz člana 10.

---

## 8. Otkazivanje od strane Prodavca i nedostatak hrane

8.1. U izuzetnim slučajevima (nestašica, tehnički kvar i slično), Prodavac može otkazati / odbiti rezervaciju. Prema ugovoru Prodavca sa Platformom, to treba da bude **najkasnije do početka** Vremenskog okvira.

8.2. Ako Prodavac otkaže rezervaciju, Kupac se obaveštava (npr. push i/ili imejl), a Platforma **stornira** preautorizaciju. Kupac ne snosi troškove naplate za tu rezervaciju.

8.3. Ako Kupac dođe u objekat **unutar** Vremenskog okvira, a Prodavac odbije predaju uz obrazloženje da paketa nema, Kupac treba u aplikaciji da pokrene prijavu (npr. „Prijavi nedostatak hrane“), uz geolokaciju ako je zahtevana. Platforma oslobađa sredstva Kupcu i pokreće istragu prema Prodavcu.

---

## 9. Nepojavljivanje (No-Show) i suspenzija naloga

9.1. Ako Kupac **ne otkaže** rezervaciju i **ne preuzme** Paket do isteka Vremenskog okvira, smatra se da je prekršio ove Uslove (no-show).

9.2. Po isteku roka, sistem može označiti porudžbinu kao istekli (**EXPIRED**), vratiti količinu na Ponudu i:

- **stornirati** preautorizaciju, **i/ili**
- izvršiti **prinudnu naplatu (Capture)**,

zavisno od važeće poslovne politike i tehničke konfiguracije Platforme.

9.3. **Ugovorno pravo Platforme:** Platforma zadržava pravo da naplati rezervisani iznos ako Prodavac čuva rezervisanu hranu koja zbog nepojavljivanja propadne. U tom slučaju Prodavac je dužan da izda fiskalni dokument i dostavi ga Kupcu (npr. imejlom), u skladu sa propisima.

9.4. Platforma vodi evidenciju prekršaja. **No-show** i **sumnjiva otkazivanja (bypass)** beleže se kao *strike* događaji. Kada zbir aktivnih prekršaja dostigne prag (podrazumevano **3**), nalog Kupca se **automatski suspenduje**, a sesije / tokeni mogu biti opozvani.

9.5. Suspendovani Kupac ne može normalno rezervisati nove Pakete dok Platforma ne ukine suspenziju. Teška ili ponovljena zloupotreba može dovesti do trajne deaktivacije naloga.

---

## 10. Geolokacija, prevare i zaobilaženje sistema (Anti-Bypass)

10.1. Platforma koristi geolokaciju radi:

- pretrage Ponuda **u blizini**;
- sprečavanja zloupotreba pri otkazivanju / prijavama.

10.2. Zabranjeno je zaobilaženje Platforme, npr. ulazak u lokal, namerno otkazivanje rezervacije kako bi se novac „odmrznuo“, a zatim plaćanje Prodavcu gotovinom „na ruke“ van aplikacije.

10.3. Ako sistem detektuje da se otkazivanje vrši unutar radijusa od oko **50 metara** od mesta preuzimanja **tokom** aktivnog Vremenskog okvira, događaj se označava kao sumnjiv (*potential bypass*). Takav događaj:

- može povećati brojač prekršaja Kupca;
- može, sam ili u zbiru sa drugim prekršajima, dovesti do **suspenzije** ili trajne deaktivacije naloga.

10.4. Otkazivanje može tehnički i dalje proći (oslobađanje sredstava), ali to ne isključuje sankcije zbog zloupotrebe.

---

## 11. Ocene, omiljene Ponude i sadržaj

11.1. Nakon uspešno završene porudžbine, Kupac može oceniti Prodavca (npr. proces preuzimanja, kvalitet, količina, raznovrsnost). Ocene moraju biti poštene; zabranjena je manipulacija.

11.2. Kupac može označavati Ponude kao omiljene radi lakšeg pristupa.

11.3. Zabranjeno je zloupotrebljavati komunikaciju, lažno prijavljivati incidente ili ometati rad Platforme i Prodavaca.

---

## 12. Obaveštenja

12.1. Kupac saglašava se da prima **transakciona** obaveštenja neophodna za uslugu (status rezervacije, podsetnik za preuzimanje, plaćanje, bezbednost naloga) putem push notifikacija i/ili imejla.

12.2. Marketinška i neobavezna obaveštenja šalju se u skladu sa Politikom privatnosti i podešavanjima / pristankom Kupca.

---

## 13. Odgovornost i reklamacije

13.1. Za kvalitet, svežinu, bezbednost i sastav hrane odgovara **Prodavac**, u skladu sa Zakonom o bezbednosti hrane.

13.2. Platforma nije odgovorna za:

- štetu nastalu konzumacijom hrane;
- kašnjenje ili grešku Prodavca u predaji;
- nedostupnost aplikacije zbog održavanja, više sile ili smetnji trećih lica;
- rokove banke kod oslobađanja hold-a,

osim u meri u kojoj je šteta prouzrokovana isključivo namernom ili grubom greškom Platforme, ili ako imperativni propisi o zaštiti potrošača drugačije nalažu.

13.3. Reklamacije u vezi sa hranom Kupac prvenstveno upućuje Prodavcu; Platforma može posredovati u rešavanju spora u meri u kojoj je to moguće.

13.4. Za probleme sa naplatom / preautorizacijom, Kupac može kontaktirati podršku Platforme na kontakt imejl.

---

## 14. Intelektualna svojina

Aplikacija, softver, brend StillFresh i sadržaj Platforme zaštićeni su i ostaju svojina StillFresh d.o.o. ili njenih licenciara. Kupac dobija ograničenu, neprenosivu licencu za lično, nekomercijalno korišćenje aplikacije u skladu sa ovim Uslovima.

---

## 15. Zaštita podataka o ličnosti

Obrada podataka o ličnosti uređena je **Politikom privatnosti**. Prihvatanjem ovih Uslova Kupac potvrđuje da je upoznat sa Politikom privatnosti.

---

## 16. Izmene Uslova

Platforma može izmeniti ove Uslove objavom nove verzije u aplikaciji / na sajtu, sa datumom ažuriranja. Nastavak korišćenja nakon stupanja izmena na snagu smatra se prihvatanjem, osim ako Kupac ne prestane sa korišćenjem i ne zahteva brisanje naloga.

---

## 17. Merodavno pravo i sporovi

17.1. Na ove Uslove primenjuje se pravo **Republike Srbije**, uključujući propise o zaštiti potrošača u meri u kojoj se primenjuju.

17.2. Za sporove je nadležan stvarno nadležni sud u **Beogradu**, osim ako imperativni propisi o zaštiti potrošača ne određuju drugačiju nadležnost.

---

## 18. Završne odredbe

18.1. Ako je neka odredba ništava ili neprimenljiva, ostale odredbe ostaju na snazi.

18.2. Nevršenje nekog prava od strane Platforme ne znači odricanje od tog prava.

18.3. Ovi Uslovi, Politika privatnosti i posebna obaveštenja u aplikaciji (npr. o načinu plaćanja) čine sporazum između Platforme i Kupca o korišćenju Platforme.

---

## 19. Podaci o Platformi

| | |
|---|---|
| **Naziv** | StillFresh d.o.o. Beograd |
| **Sedište** | [Uneti adresu] |
| **PIB** | [Uneti PIB] |
| **Kontakt** | [Uneti imejl, npr. support@stillfresh.rs] |
