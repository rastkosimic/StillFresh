# Ugovor po pristupu i opšti uslovi korišćenja Platforme za prodavce

**Relacija: StillFresh (Platforma) – Prodavac**

*Poslednje ažuriranje: 23. jul 2026.*

Ovim Opštim uslovima korišćenja (u daljem tekstu: **Uslovi**) uređuju se prava i obaveze između privrednog društva **StillFresh d.o.o. Beograd** (u daljem tekstu: **Platforma**) i pravnog lica ili preduzetnika koji se registruje kao ponuđač hrane putem StillFresh aplikacije i povezanih sistema (u daljem tekstu: **Prodavac**).

Prihvatanjem ovih Uslova (prilikom prijave / registracije / aktivacije naloga) smatra se da je zaključen **ugovor po pristupu** između Platforme i Prodavca.

---

## 1. Strane ugovora i definicije

| Pojam | Značenje |
|---|---|
| **Platforma** | StillFresh d.o.o. Beograd, sedište: [Uneti adresu], PIB: [Uneti PIB] |
| **Prodavac** | Pravno lice ili preduzetnik koji nudi pakete / viškove hrane preko Platforme |
| **Kupac** | Fizičko lice koje rezerviše Paket putem aplikacije |
| **Paket / Ponuda** | Ponuda hrane koju Prodavac objavljuje (cena, količina, vreme i mesto preuzimanja) |
| **Vremenski okvir** | Interval preuzimanja koji Prodavac definiše za Ponudu (npr. 20:00–21:00) |
| **Provizija** | Naknada Platforme za uslugu posredovanja / tehničkog i platnog posredovanja |

Kontakt za ugovorna i operativna pitanja: **[Uneti imejl, npr. vendors@stillfresh.rs]**.  
Politika privatnosti: dokument `PRIVACY.md` / javna stranica Platforme.

---

## 2. Pravni status i priroda saradnje

2.1. Platforma obezbeđuje tehničku infrastrukturu (mobilna aplikacija, API, obaveštenja) i platni tok preko licenciranog pružaoca platnih usluga (**AllSecure** i/ili drugih partnera koje Platforma odredi).

2.2. Prihvatanjem ovih Uslova, Prodavac ovlašćuje Platformu da za transakcije generisane kroz aplikaciju:

- nastupa kao **posrednik u naplati** (komisioni / zastupnički model u naplati) u ime i za račun Prodavca, **i/ili**
- u modelu **Merchant of Record (MoR)** tehnički i finansijski prima uplatu Kupca na račun Platforme, a Prodavcu isplaćuje neto iznos nakon odbitka Provizije,

u skladu sa konfiguracijom isplata koja važi za Prodavca (npr. MoR za Srbiju, Stripe Connect gde je omogućeno).

2.3. Sredstva koja Kupac plati za Paket (osim Provizije Platforme) smatraju se, u odnosu na Prodavca, sredstvima namenjenim Prodavcu i isplaćuju se prema članu 5. ovih Uslova. Platforma ne postaje vlasnik hrane; **Prodavac ostaje vlasnik robe** do predaje Kupcu i snosi odgovornost za bezbednost hrane i fiskalne obaveze iz člana 6.

2.4. Platforma nije strana u ugovoru o kupoprodaji hrane između Prodavca i Kupca u pogledu kvaliteta, sastava i zdravstvene ispravnosti robe, osim u meri u kojoj zakonom ili ovim Uslovima drugačije proizlazi iz uloge u naplati.

---

## 3. Registracija, verifikacija i nalog

### 3.1. Registracioni podaci

Prodavac je dužan da dostavi **tačne i ažurne** podatke, uključujući (u meri u kojoj Platforma zahteva):

- pun naziv / ime preduzetnika i podatke iz APR-a;
- **PIB** i matični broj (ili drugi poslovni identifikator);
- adresu i lokaciju prodajnog mesta (uključujući koordinate radi mape i geoprolvere);
- kontakt osobu, telefon i imejl;
- broj važećeg tekućeg računa u formatu domaćeg platnog prometa (**3-13-2**) / IBAN, vlasnika računa i banku.

Broj tekućeg računa pravnog lica je javan podatak i može biti proveren (npr. putem Registra računa **NBS**) pre ili posle aktivacije profila. Platforma zadržava pravo da odbije ili suspenduje nalog dok podaci nisu potvrđeni.

### 3.2. Verifikacija i aktivacija

- Prijava / aplikacija Prodavca može biti u statusu čekanja verifikacije (**INACTIVE** / `PENDING_VERIFICATION`).
- Platforma (administrator) **verifikuje i aktivira** nalog; Prodavac zatim prolazi onboarding (tip poslovanja, lokacije, bankarsko podešavanje, konfiguracija plaćanja) do statusa **COMPLETED**.
- Do završetka obaveznih koraka onboardinga, mogućnost objave Ponuda i/ili prijema isplata može biti ograničena.

### 3.3. Tipovi naloga i uloge

| Tip | Opis |
|---|---|
| **Jedinstveni prodavac (UNIQUE)** | Jedna lokacija |
| **Lanac (CHAIN)** | Više lokacija; može postojati sedište (HQ) i radne lokacije |

| Uloga | Ovlašćenja (sažeto) |
|---|---|
| **VENDOR_ADMIN** | Upravljanje profilom, onboardingom, bankarskim podacima, brisanje naloga, upravljanje radnicima / lokacijama u okviru ovlašćenja |
| **VENDOR** (radnik) | Operativni rad na dodeljenoj lokaciji (npr. Ponude / porudžbine za tu lokaciju) |

Prodavac je odgovoran za sve radnje koje preduzimaju njegovi ovlašćeni korisnici.

### 3.4. Model bankarstva unutar lanca

Za lance, Platforma može omogućiti:

- **SHARED** — sve lokacije dele platni / isplatni nalog sedišta;
- **INDIVIDUAL** — svaka lokacija ima sopstvenu isplatnu konfiguraciju.

Promena modela može privremeno **deaktivirati Ponude** dok se isplatna podešavanja ne kompletiraju. Prodavac će biti obavešten (npr. push / imejl).

---

## 4. Obaveze Prodavca u vezi sa Ponudama i zalihama

4.1. Prodavac je dužan da maksimalno **ažurno** vodi zalihe i podatke o Ponudi (naziv, opis, cena, količina, kategorija, vreme i mesto preuzimanja, slike).

4.2. Vreme preuzimanja mora biti u **budućnosti** (u vremenskoj zoni lokacije). Istekle ili rasprodate Ponude mogu biti deaktivirane automatski; Prodavac može, u skladu sa funkcijama aplikacije, resetovati / ponovo aktivirati Ponudu uz tačne podatke.

4.3. Prodavac garantuje da je Ponuda stvarno dostupna u navedenoj količini tokom Vremenskog okvira.

4.4. Zabranjeno je objavljivanje lažnih, obmanjujućih ili nezakonitih Ponuda, kao i hrane koja nije bezbedna za ljudsku ishranu.

---

## 5. Tok novca, Provizija i isplata

### 5.1. Naplata od Kupca

5.1.1. Uplate Kupaca procesira spoljni pružalac platnih usluga (**AllSecure** u Republici Srbiji, osim ako Platforma drugačije odredi). Podaci o platnim karticama **ne** prolaze u punom obliku preko servera Platforme.

5.1.2. Pri rezervaciji, vrši se **preautorizacija** (hold) sredstava na kartici Kupca. Trajna naplata (**capture**) vrši se po uspešnom preuzimanju (potvrda preuzimanja u aplikaciji), odnosno u drugim slučajevima predviđenim uslovima za Kupce i ovim Uslovima (npr. pravo Platforme na naplatu zbog nepojavljivanja).

5.1.3. Ukoliko se rezervacija otkaže ili istekne bez naplate, preautorizacija se **stornira (void)**, a Prodavcu se za tu transakciju ne pripisuje kredit.

### 5.2. Provizija Platforme

5.2.1. Platforma za uslugu posredovanja naplaćuje Proviziju u procentu od bruto vrednosti **uspešno naplaćenog** Paketa.

5.2.2. **Podrazumevana Provizija iznosi 10% (deset procenata)**, osim ako Platforma drugačije objavi ili ugovori sa Prodavcem. Aktuelni procenat može biti podešen administrativno u sistemu; važeći procenat u trenutku preautorizacije / rezervacije primenjuje se na tu transakciju.

5.2.3. Od naplaćenog iznosa:

- **neto iznos** (bruto minus Provizija) pripisuje se Prodavcu u internom knjigovodstvenom / ledger zapisu;
- Provizija pripada Platformi.

### 5.3. Isplata Prodavcu (MoR / B2B)

5.3.1. Nakon uspešne naplate (capture) i knjiženja, neto iznos ulazi u **ledger** Platforme kao kredit Prodavcu.

5.3.2. Isplata na tekući račun Prodavca vrši se **periodično** (npr. dnevni paket isplata), automatizovanim B2B nalogom preko poslovne banke / rail sistema (ciljani tok: npr. **Raiffeisen CMIplus**), pod odgovarajućom šifrom plaćanja (npr. **221** — promet robe i usluga), osim ako tehnički ili bankarski razlozi zahtevaju drugačiji postupak.

5.3.3. Isplata **nije trenutna** u momentu preuzimanja; zavisi od ciklusa paketa isplata, statusa naloga, tačnosti bankarskih podataka i eventualnog zadržavanja / pauze zbog istrage, duga ili povrede Uslova.

5.3.4. Ako Prodavac koristi **Stripe Connect** (gde je omogućeno), isplata se vrši prema pravilima Stripe Connect modela, a ne putem MoR ledger paketa.

5.3.5. Platforma može privremeno **pauzirati, zadržati ili otkazati** stavku / paket isplate u slučaju sumnje na prevaru, pogrešnih podataka računa, spora ili povrede Uslova.

---

## 6. Fiskalizacija i fakturisanje

6.1. Prodavac je **jedini vlasnik robe (hrane)** do predaje Kupcu i snosi zakonsku odgovornost za izdavanje fiskalnog računa Kupcu, u skladu sa propisima Republike Srbije.

6.2. U momentu preuzimanja robe, radnik Prodavca dužan je da izda **e-fiskalni račun** na pun iznos porudžbine (100%), uz način plaćanja odgovarajući posredničkoj prodaji (npr. oznaka / šifra **„Prodaja preko posrednika“** ili druga instrukcija knjigovođe / poreskog savetnika).

6.3. Platforma zadržava pravo da tehnički podrži ili zahteva integraciju fiskalnog toka; dok takva integracija nije potpuno aktivna, **obaveza fiskalizacije i dalje leži na Prodavcu**.

6.4. Platforma će, u skladu sa svojim poslovnim i poreskim obavezama, Prodavcu ispostavljati fakturu (npr. putem Sistema elektronskih faktura — **SEF**) na iznos **zadržane Provizije**, sa iskazanim PDV-om gde je primenljivo. Ta faktura služi Prodavcu kao osnov za knjiženje troška posredovanja.

6.5. Prodavac je dužan da blagovremeno dostavi sve podatke potrebne za ispravno fakturisanje.

---

## 7. Porudžbine: preuzimanje, otkazivanje i nepojavljivanje

### 7.1. Tok rezervacije (sažeto)

1. Kupac rezerviše Paket → preautorizacija sredstava → porudžbina u statusu rezervisanosti;
2. Kupac preuzima hranu u Vremenskom okviru i potvrđuje preuzimanje u aplikaciji → naplata (capture) → kredit Prodavcu;
3. Otkazivanje / istek bez naplate → void hold-a → bez kredita Prodavcu (osim ako Uslovi ili odluka Platforme drugačije ne predvide).

### 7.2. Otkazivanje / odbijanje od strane Prodavca

7.2.1. Prodavac je dužan da maksimalno izbegava otkazivanje potvrđenih rezervacija.

7.2.2. **Ugovorno pravilo:** Prodavac sme da otkaže / odbije rezervaciju Kupca **isključivo pre početka** definisanog Vremenskog okvira za preuzimanje. Od početka Vremenskog okvira, otkazivanje na strani Prodavca smatra se zabranjenim, osim uz izričitu saglasnost Platforme (force majeure, bezbednost hrane i slično).

7.2.3. Ako Prodavac otkaže na vreme, Kupcu se oslobađa preautorizacija, a Prodavcu se ne pripisuje naplata za tu porudžbinu.

### 7.3. Nedostatak hrane na licu mesta

7.3.1. Ako Kupac stigne u objekat unutar Vremenskog okvira, a Prodavac odbije predaju zbog „nema paketa“, Kupac može podneti prijavu u aplikaciji (uz geolokaciju, ako je zahtevana).

7.3.2. Platforma pokreće unutrašnju istragu. Sistem penala (ugovorna prava Platforme):

| Prekršaj u kalendarskom mesecu | Posledica |
|---|---|
| **1.** | Sistemsko upozorenje (imejl i/ili Vendor aplikacija) |
| **2.** | Penal **2.000 RSD**, koji se može odbiti od naredne isplate |
| **3. i svaki naredni** | Suspendovanje profila na **7 dana**, ručna revizija, penal **5.000 RSD**; pri daljem ponavljanju — trajni raskid |

7.3.3. Platforma može primeniti penale ručno i/ili automatski, u zavisnosti od dostupnih tehničkih kontrola.

### 7.4. Nepojavljivanje Kupca (no-show)

Ako Kupac ne otkaže i ne preuzme Paket do isteka Vremenskog okvira, Platforma može:

- osloboditi preautorizaciju, **ili**
- izvršiti naplatu u skladu sa uslovima za Kupce,

zavisno od važeće politike i tehničke konfiguracije. Ukoliko dođe do naplate, Prodavcu pripada neto iznos po članu 5, a Prodavac je dužan da izda fiskalni dokument u skladu sa propisima (uključujući elektronsku dostavu Kupcu gde je potrebno).

---

## 8. Zabrana zaobilaženja sistema (Anti-Bypassing)

8.1. Prodavac se obavezuje da **neće** podstrekivati Kupce niti učestvovati u radnjama koje zaobilaze Platformu, uključujući ali ne ograničavajući se na:

- predlog Kupcu da otkaže rezervaciju u objektu kako bi platio gotovinom „na ruke“ van aplikacije;
- namerno omogućavanje takvih radnji zaposlenima Prodavca.

8.2. Platforma koristi **geofencing** i evidenciju otkazivanja. Ako se otkazivanje Kupca dogodi unutar radijusa od oko **50 metara** od mesta preuzimanja tokom aktivnog Vremenskog okvira, događaj može biti označen kao sumnjiv (bypass), a brojači prekršaja mogu se povećati i za Kupca i za Prodavca.

8.3. Učestala sumnjiva otkazivanja kod istog Prodavca smatraju se **teškom povredom** Uslova.

8.4. Za potvrđenu malverzaciju (npr. više od **3** puta u periodu koji odredi Platforma), Platforma zadržava pravo da:

- privremeno ili trajno **suspenduje** profil Prodavca;
- **obustavi isplate**;
- naplati ugovornu kaznu u fiksnom iznosu od **50.000 RSD**;
- raskine ugovor bez otkaznog roka.

---

## 9. Ocene i reputacija

9.1. Nakon uspešno završene porudžbine, Kupac može oceniti Prodavca (npr. proces preuzimanja, kvalitet, količina, raznovrsnost).

9.2. Ocene utiču na prikazani prosečni rejting Prodavca. Zabranjeno je manipulisanje ocenama.

---

## 10. Odgovornost za kvalitet i bezbednost hrane

10.1. Prodavac garantuje ispravnost, svežinu i bezbednost hrane u skladu sa **Zakonom o bezbednosti hrane** i drugim propisima Republike Srbije, kao i da poseduje sve potrebne dozvole za obavljanje delatnosti.

10.2. Platforma **ne snosi odgovornost** za zdravstvene probleme Kupaca nastale konzumacijom hrane, niti za štete nastale greškom, kašnjenjem ili neispunjenjem obaveza Prodavca, osim u meri u kojoj je šteta prouzrokovana isključivo namernom ili grubom greškom Platforme.

10.3. Prodavac će obeštetiti Platformu zbog zahteva trećih lica koji proističu iz hrane, fiskalnih propusta ili povrede ovih Uslova od strane Prodavca.

---

## 11. Obaveštenja i komunikacija

Prodavac saglašava se da Platforma šalje **transakciona i operativna** obaveštenja (push, imejl) u vezi sa porudžbinama, isplatama, promenom bankarskog modela, bezbednošću naloga i povredama Uslova. Marketinška komunikacija — u skladu sa Politikom privatnosti i pristankom gde je potreban.

---

## 12. Suspenzija, raskid i brisanje naloga

12.1. Platforma može suspendovati ili trajno deaktivirati nalog Prodavca zbog:

- povrede ovih Uslova;
- sumnje na prevaru / bypass;
- netačnih podataka;
- neplaćenih penala;
- zahteva nadležnog organa;
- prestanka poslovanja Prodavca.

12.2. Prodavac (VENDOR_ADMIN) može zahtevati brisanje / deaktivaciju naloga putem aplikacije. Deaktivacija može invalidirati aktivne Ponude; finansijski tragovi čuvaju se u skladu sa zakonom i Politikom privatnosti.

12.3. Po raskidu, Platforma isplaćuje eventualni nesporni saldo, umanjen za penale, potraživanja i zakonska zadržavanja, u razumnom roku.

---

## 13. Intelektualna svojina i licenca

13.1. Aplikacija, softver, brend i sadržaj Platforme ostaju svojina StillFresh d.o.o. ili njenih licenciara.

13.2. Prodavac daje Platformi neekskluzivnu licencu da prikazuje naziv, logo, fotografije i opis Ponuda u aplikaciji i u marketinškim materijalima vezanim za Platformu.

---

## 14. Izmene Uslova

Platforma može izmeniti ove Uslove objavom nove verzije u aplikaciji / na sajtu, sa datumom ažuriranja. Nastavak korišćenja Platforme nakon stupanja izmena na snagu smatra se prihvatanjem, osim ako Prodavac u roku koji Platforma odredi ne raskine ugovor.

---

## 15. Merodavno pravo i sporovi

15.1. Na ove Uslove i ugovor primenjuje se pravo **Republike Srbije**.

15.2. Za sporove je nadležan stvarno nadležni sud u **Beogradu**, osim ako imperativni propisi drugačije ne nalažu.

---

## 16. Završne odredbe

16.1. Ako je neka odredba ništava ili neprimenljiva, ostale odredbe ostaju na snazi.

16.2. Nevršenje nekog prava od strane Platforme ne znači odricanje od tog prava.

16.3. Ovi Uslovi, zajedno sa Politikom privatnosti i posebnim pisanim aneksima (ako postoje), čine celokupan sporazum između Platforme i Prodavca u pogledu korišćenja Platforme.

---

## 17. Podaci o Platformi

| | |
|---|---|
| **Naziv** | StillFresh d.o.o. Beograd |
| **Sedište** | [Uneti adresu] |
| **PIB** | [Uneti PIB] |
| **Kontakt** | [Uneti imejl, npr. vendors@stillfresh.rs] |
