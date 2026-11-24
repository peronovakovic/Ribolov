# Pozivi korisnicima i provjera prijave

Implementirano 23.11.2026. Pozivni tok zamjenjuje ručno povezivanje naloga i direktnu dodjelu nove uloge kroz web/API.

## Poslovni tok

1. SYS poziva analitičara Ministarstva/Saveza ili administratora udruženja. ADM poziva operatera ili ribočuvara samo svog aktivno izabranog udruženja.
2. Administrator unosi adresu e-pošte, ulogu, organizaciju i razlog. Opcioni kraj ovlaštenja mora biti nakon isteka poziva.
3. Poziv i audit upisuju se u istoj transakciji. Poziv važi tačno 48 sati. Još ne postoji nova dodjela uloge.
4. Pozadinski proces šalje vezu. Primalac se prijavljuje ili registruje u Keycloaku, potvrđuje svoju adresu i podešava OTP.
5. Server traži svježu MFA prijavu i potvrđenu adresu koja odgovara pozivu. Adresa, potvrda, issuer, subject, acr i auth_time dolaze iz verifikovanog ID tokena.
6. Ekran prikazuje organizaciju, ulogu i rok. Tek dugme „Prihvati poziv” kreira/povezuje lokalni nalog, kreira JPA dodjelu, označava poziv prihvaćenim i evidentira audit, u jednoj transakciji.

Otvaranje veze i GET zahtjev ne aktiviraju ulogu. Dva istovremena prihvatanja daju samo jednu dodjelu. Ponovljeni pokušaj nakon uspjeha se odbija. Pogrešna/nepotvrđena adresa, neaktivan lokalni nalog, istek, opoziv i povučeno ovlaštenje pošiljaoca sprečavaju prihvatanje. Poziv ne reaktivira deaktiviran nalog.

Adresu normalizujemo uklanjanjem vanjskih praznina i pretvaranjem u mala slova. U ovoj verziji prihvatamo ASCII adrese; ne spajamo plus-adrese niti uklanjamo tačke.

Postojeće uloge i nalozi ostaju u evidenciji. SYS i dalje može mijenjati ime/aktivnost lokalnog naloga, a dozvoljeni administratori opozivati postojeće uloge. Početni SYS se i dalje postavlja zasebnom bootstrap skriptom. HTTP `POST /admin/users` i `POST /admin/roles` su uklonjeni; novi nalozi i uloge nastaju prihvatanjem poziva.

## JPA, transakcije i RLS

`RoleInvitation` ima stvarne `@ManyToOne` veze prema organizaciji, dodjeli pošiljaoca i prihvaćenoj dodjeli. `AdministrationEvent` referencira poziv. Ne postoje skalarni FK atributi ili kolona `code`.

Migracija V004 dodaje tabelu, FK i uske RLS politike. Pri prihvatanju se zaključava poziv; kreiranje istog issuer/subject para kroz različite pozive dodatno serijalizuje PostgreSQL transakcijsko zaključavanje. Postojeće exclusion ograničenje sprečava preklapajuće iste uloge.

RLS kontekst za primaoca sadrži samo provjereni identitet, potvrđenu adresu i hash poziva. Otkriva taj poziv i minimalno potrebne povezane zapise. Ne otvara globalnu pretragu korisnika ili organizacija. Pozadinski proces ima zaseban transakcijski kontekst za dostavu; trigger mu ne dopušta prihvatanje ili opoziv. Sve GUC vrijednosti postavlja pouzdani server kroz parametrizovane upite; DB runtime kredencijali nisu korisnički interfejs.

## Veze i dostava

Tajni dio veze nastaje kroz HMAC-SHA256 s privatnim ključem od 32 bajta i slučajnim UUID-om poziva. U bazi je samo SHA-256 digest; pozadinski proces može ponovo formirati istu vezu. Administrativni DTO i audit nikada ne vraćaju tajni dio.

Veza koristi URL fragment `#invite=...`, koji se ne šalje HTTP serveru kao URL. Web ga odmah uklanja iz adresne trake i privremeno čuva u `sessionStorage` istog taba, radi nastavka poslije OIDC preusmjeravanja. Uspješno prihvatanje ili zatvaranje poziva uklanjaju ga iz pohrane. Zahtjevi za pregled i prihvatanje šalju tajnu u JSON tijelu uz CSRF; nema automatskog prihvatanja ili ponavljanja upisa.

Trajni red je dio zapisa poziva u modulu `access`. Modul `notifications` pruža samo SMTP transport kroz javni `EmailDelivery` ugovor, bez povratne zavisnosti na access model. Ovim nije uveden opšti outbox za sve module.

Proces preuzima jedan zaključan poziv, koristi ograničene SMTP timeoute i čuva rezultat. Automatski pokušava najviše pet puta, s rastućim razmakom. Nakon toga administrator vidi neuspjeh i može zatražiti ponovno slanje. Ponovno slanje zadržava prvobitnu vezu i rok; za novi rok treba opozvati stari poziv i napraviti novi.

Ako proces prekine rad nakon što SMTP prihvati poruku, može poslati istu vezu ponovo. To ne stvara novu ulogu i ne produžava rok. Oznaka „Predato pošti” potvrđuje prihvatanje od SMTP servera, ne isporuku u sandučić. Greške transporta ne spremaju sadržaj poruke, adresu ili token u log.

## Lokalno pokretanje

Dopuni postojeći `.env` novim parametrima iz `.env.example`, bez prepisivanja postojećih vrijednosti. `INVITATION_SIGNING_KEY` mora biti base64 kodiran privatni ključ od 32 bajta. Objavljeni primjer je samo za lokalni razvoj. Primjer generisanja bez ispisa ključa u terminal:

```powershell
$invitationKeyBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($invitationKeyBytes)
# Vrijednost [Convert]::ToBase64String($invitationKeyBytes) sačuvaj kao INVITATION_SIGNING_KEY u lokalnu konfiguraciju.
```

Razvojne postavke su:
- `INVITATION_DELIVERY_ENABLED=true`;
- `SMTP_HOST=localhost`, `SMTP_PORT=1025`, `SMTP_FROM=noreply@ribolov.local`, `SMTP_STARTTLS=false`;
- prazni SMTP username/password samo za lokalni Mailpit.

`docker compose --env-file .env -f infra/compose.yml up -d` sada podiže i Mailpit. Poruke se pregledaju na **http://localhost:8025** i ne prosljeđuju se na internet. Keycloak u kontejneru koristi SMTP host `mailpit`. Backend na hostu koristi `localhost`.

Za zajedničko okruženje podesiti stvarni SMTP, njegove kredencijale, `SMTP_STARTTLS=true`, HTTPS i privatni ključ. SMTP transport zahtijeva STARTTLS kada je uključen; ne nastavlja nezaštićeno ako ga server ne podržava.

Promjena signing ključa ne poništava već poslane veze: njihovi digest-i ostaju važeći do prihvatanja, opoziva ili isteka. Neposlani stari pozivi ne mogu se regenerisati novim ključem, pa dostava prijavljuje neuspjeh. Planirana rotacija zahtijeva završetak ili opoziv tih poziva; nakon kompromitovanja treba opozvati sve pogođene pozive.

## Keycloak i provjere

Realm sada dopušta registraciju, zahtijeva potvrdu e-pošte i OTP, a web klijent traži email scope i potpisane email/email_verified tvrdnje. Sama registracija ne daje poslovnu ulogu. Postojeći realm se ne mijenja automatski: primijeni promjene iz `infra/keycloak/realm.json` kroz namjensku administraciju, čuvajući korisnike.

`npm run test:oidc` iz `frontend` priprema zaseban slučajno imenovan testni realm, korisnika i OTP, pokreće izgrađeni backend JAR i poslužuje izgrađeni web. Provjerava stvarnu OIDC prijavu, svježe MFA/email tvrdnje, obnovu OTP-a, CSRF, odsustvo automatskih poslovnih uloga i odjavu. Nakon toga uklanja samo realm koji je sam kreirao.

Preduslovi: slobodni portovi 8080/5173, lokalni Keycloak na 8180, izgrađen JAR i `frontend/dist`, instaliran Playwright Chromium te zasebna testna baza. Postavi `IT_DATABASE_URL`, `IT_DATABASE_USERNAME`, `IT_DATABASE_PASSWORD`, `IT_MIGRATION_USERNAME`, `IT_MIGRATION_PASSWORD` i `TEST_KEYCLOAK_ADMIN_PASSWORD`; opcionalno `TEST_KEYCLOAK_ADMIN_USERNAME`. CI ih postavlja nad izolovanim servisima. Testni korisnik ima unaprijed potvrđenu adresu; potpuni tok samostalne registracije i klika na Keycloak verifikacijsku poruku treba dodatno potvrditi ručno.

Lokalno je prošlo **54 backend testa**, uključujući **17 na PostgreSQL-u** i dva stvarna SMTP transport testa, te web build i browser scenarij s kontrolisanim API odgovorima. Browser scenarij provjerava pozive, opoziv, explicitno prihvatanje, čuvanje veze kroz ponovno učitavanje, uklanjanje tajne iz URL-a/pohrane i CSRF.

Stvarna Keycloak provjera je pripremljena u CI-ju, ali ovdje nije izvršena: Docker pristup je odbijen, a servis na 8180 nije dostupan. Standardni Maven lifecycle ponovo je zaustavljen na sandbox pristupu javac arhivama; izvor je kompajliran kroz Java Compiler API, testovi su pokrenuti kroz Surefire. CI rezultat tek treba potvrditi nakon slanja izmjena.

Izvori konfiguracije: [Keycloak 26.7 administracija](https://www.keycloak.org/docs/26.7.0/server_admin/), [Angus SMTP postavke](https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/package-summary.html), [Mailpit 1.34.0](https://github.com/axllent/mailpit/releases/tag/v1.34.0).
