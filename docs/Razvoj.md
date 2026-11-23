# Lokalni razvoj

## Preduslovi

- JDK 21 i Maven 3.9+ za backend.
- Node.js 22 LTS (minimalno 20.19) i npm za web.
- Docker Desktop / Docker Compose za PostgreSQL/PostGIS i Keycloak.
- Android Studio, JDK 17 i Android SDK 35 za Android. Gradle wrapper je u repozitoriju; prvo pokretanje preuzima Gradle 8.11.1 i zavisnosti.

U IntelliJ IDEA uvezi korijenski `pom.xml`. Android projekt se otvara iz `mobile/android`. Za web se koristi `frontend/package.json`.

## Infrastruktura

Iz korijena, prvi put:

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f infra/compose.yml up -d
docker compose --env-file .env -f infra/compose.yml ps
```

Postojeći `.env` ne treba prepisivati. Primjer sadrži javne lokalne lozinke; za zajedničko/produkcijsko okruženje koriste se druge tajne i konfiguracija.

| Servis | Adresa |
| --- | --- |
| Web razvojni server | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Keycloak | http://localhost:8180 |
| PostgreSQL | localhost:5432, baza `ribolov` |

Compose portovi baze i Keycloaka vezani su za lokalni računar. PostgreSQL volume čuva podatke između pokretanja. Init skripta izvršava se samo na praznom volume-u. Promjena lozinke u `.env` ne mijenja već kreirani DB račun; tada lozinke treba uskladiti u bazi.

`POSTGRES_OWNER_PASSWORD` i `MIGRATION_PASSWORD` moraju odgovarati istom lokalnom računu `ribolov_owner`. Runtime račun je `ribolov_app`. Keycloak ima zasebnu bazu `keycloak` i račun `keycloak_owner`.

Keycloak uvozi realm pri prvom pokretanju. Izmjena `realm.json` ne prepisuje postojeći realm. Kasnije promjene primijeni namjenski kroz administraciju ili kontrolisanu migraciju konfiguracije.

Zaustavljanje uz zadržavanje podataka:

```powershell
docker compose --env-file .env -f infra/compose.yml stop
```

## Backend i web

Terminal 1, iz korijena:

```powershell
mvn -pl backend spring-boot:run
```

Spring konfiguracija čita `.env` iz korijena ili roditeljskog direktorija modula. Ne navoditi tajne kroz komandu koja ostaje u shell historiji.

Terminal 2:

```powershell
npm --prefix frontend ci
npm --prefix frontend run dev
```

Web koristi relativne URL-ove. Vite prosljeđuje `/api`, `/oauth2`, `/login` i `/logout` backendu. Nije potreban CORS wildcard. Konfiguracija je obični ESM `vite.config.mjs` i učitava se izvorno preko Vite `--configLoader native`.

Za provjeru prijave:

1. Otvori Keycloak na portu 8180; prijavi se kao `admin` s lokalnom administratorskom lozinkom.
2. Odaberi realm `ribolov`, kreiraj testnog korisnika i postavi mu lozinku.
3. Otvori web na portu 5173 i odaberi „Prijavite se”.
4. Provjeri da se nakon prijave prikazuje identitet korisnika i da odjava završava na početnom ekranu.

Testni korisnik nema poslovna ovlaštenja. Nisu kreirani demo članovi, dozvole ili uplate. Keycloak administratorski nalog nije aplikacijski korisnik.

Liveness je na `/actuator/health/liveness`, readiness na `/actuator/health/readiness`, a ukupno stanje na `/actuator/health`. Health detalji nisu javni.

## Automatske provjere

```powershell
mvn verify
npm --prefix frontend run build
```

Backend testovi provjeravaju svih 11 modula, zabranu nepredviđenih zavisnosti, sesijske/mobilne rute, CSRF i audience tokena. Za ove testove nije potreban Keycloak ili PostgreSQL; koriste testne identitete i H2 uz isključene migracije. H2 nije zamjena za provjeru PostgreSQL migracija.

Web build uključuje TypeScript provjeru. Web CI trenutno koristi Windows runner, kao i početna lokalna provjera. Prije uvođenja drugih build platformi treba provjeriti da lockfile obuhvata njihove opcionalne native pakete za Vite/Rolldown; početni lockfile pripremljen je iz dostupnih Windows zavisnosti.

### PostgreSQL integracijski test

Na pokrenutoj razvojnoj bazi ili zasebnoj testnoj bazi postavi:

```powershell
$env:IT_DATABASE_URL = 'jdbc:postgresql://localhost:5432/ribolov'
$env:IT_MIGRATION_USERNAME = 'ribolov_owner'
$env:IT_DATABASE_USERNAME = 'ribolov_app'
# IT_MIGRATION_PASSWORD i IT_DATABASE_PASSWORD postavi iz lokalne konfiguracije.
mvn -Ppostgres-it verify
```

Test primjenjuje stvarne migracije. Zato koristiti razvojnu/testnu bazu, ne produkcijsku. Provjerava šeme, ponovljeno izvršenje, runtime prava i zabranu kreiranja tabela. Privremene probne tabele se rollback-uju.

GitHub Actions podiže zaseban PostGIS servis i pokreće ovaj profil. Lokalni obični `mvn verify` ne pokreće `*IT` testove.

## Android

Otvori `mobile/android` u Android Studiju, instaliraj SDK 35 i postavi Gradle JDK 17. Putanja SDK-a pripada lokalnom `local.properties` ili `ANDROID_HOME`, ne verzionisanom kodu.

```powershell
Push-Location mobile/android
.\gradlew.bat testDebugUnitTest assembleDebug
Pop-Location
```

Na Linuxu/macOS-u koristi `bash ./gradlew`. APK je u `mobile/android/app/build/outputs/apk/debug/`.

Početni ekran ne prikuplja podatke. Unit testovi pokrivaju granice 24/72 sata, kraće ovlaštenje i nepouzdan sat. Room/WorkManager zavisnosti su pripremljene, ali nisu implementirani baza, mrežni adapteri ili login. Manifest ne dopušta cleartext saobraćaj.

Prije povezivanja emulatora treba urediti dostupan HTTPS OIDC issuer i backend adresu. `localhost` na emulatoru označava emulator; nije adresa razvojnog računara.

## Zabilježena provjera početne osnove

Provjera 23.11.2026. u ograničenom Windows izvršnom okruženju:

| Provjera | Rezultat |
| --- | --- |
| Backend testovi kroz Surefire | 9 prošlo, 0 neuspješnih |
| Vue/TypeScript i produkcijski bundle | Prošlo |
| `docker compose ... config --quiet` | Prošlo |
| Potpuni Maven lifecycle | Nije potvrđen: javac pri zatvaranju arhiva prijavljuje sandbox `AccessDeniedException`; emitovane klase testirane su zasebno |
| PostgreSQL migracije i stvarni OIDC tok | Nisu pokrenuti: pristup Docker daemonu nije dostupan |
| Android build i testovi | Nisu završeni: AGP 8.9.2 nije u lokalnom cache-u, a preuzimanje nije dostupno |
| GitHub Actions | Konfigurisan; izvršenje tek nakon slanja izmjena |

Rezultat Surefire testova nije tvrdnja da je kompletan `mvn verify` prošao. Provjera prijave preko stvarnog Keycloaka i PostgreSQL integracijski test ostaju dio prve pune lokalne/CI provjere.
