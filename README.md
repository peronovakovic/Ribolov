# Ribolov

Informacioni sistem za upravljanje sportskim ribolovom Republike Srpske.

Postavljena je početna arhitektura projekta: modularni Java backend, Vue web aplikacija, Android aplikacija i lokalna infrastruktura. Implementirani su JPA model i administracija organizacija, korisnika i dodjela uloga, uz MFA provjeru, audit i PostgreSQL RLS. Novi korisnici preuzimaju uloge kroz jednokratne pozive s potvrđenom adresom e-pošte. Evidencija članova, izdavanje dozvola i terenske kontrole tek slijede.

## Struktura

| Direktorij | Namjena |
| --- | --- |
| `backend/` | Java 21, Spring Boot 4.1, Spring Modulith, JPA/Hibernate, sigurnost i Flyway migracije |
| `frontend/` | Vue 3, TypeScript i Vite; web sesija i administrativni ekrani |
| `mobile/android/` | Kotlin, Compose, početak offline pravila i Gradle wrapper |
| `infra/` | PostgreSQL/PostGIS i Keycloak za lokalni razvoj |
| `docs/` | Specifikacija, arhitektura, odluke i OpenAPI ugovor |

## Pokretanje

Potrebni su JDK 21, Maven 3.9+, Node.js 22 LTS i Docker Desktop s Compose dodatkom. Za Android su potrebni JDK 17, Android SDK 35 i pristup Google/Maven repozitorijima.

Iz korijena projekta, u PowerShellu:

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f infra/compose.yml up -d
mvn -pl backend spring-boot:run
```

Ako `.env` već postoji, zadrži njegov sadržaj. Backend automatski čita taj lokalni fajl. U drugom terminalu:

```powershell
npm --prefix frontend ci
npm --prefix frontend run dev
```

Otvori **http://localhost:5173**. Keycloak je na **http://localhost:8180**. Za prvi test prijave u Keycloak administraciji odaberi realm `ribolov`, kreiraj testnog korisnika i postavi lozinku. Administratorsko ime je `admin`, a lozinka vrijednost `KEYCLOAK_ADMIN_PASSWORD` iz lokalnog `.env`.

Za početnog administratora i OTP koristi [upute za prvi administratorski nalog](docs/Razvoj.md#prvi-administrator-i-mfa). Prijava prikazuje identitet i postojeća važeća ovlaštenja korisnika; sama po sebi ne dodjeljuje poslovne uloge. Novi nalog dobija prazan pregled dok ne postoje odobrene dodjele u bazi. Web sesija ostaje na backendu. Postavke iz `.env.example` služe isključivo lokalnom razvoju.

Detaljne upute, adrese servisa i način testiranja nalaze se u [razvojnom vodiču](docs/Razvoj.md).

## Provjere

```powershell
mvn verify
npm --prefix frontend run build
Push-Location mobile/android
.\gradlew.bat testDebugUnitTest assembleDebug
Pop-Location
```

PostgreSQL integracijski test pokreće se zasebnim profilom `postgres-it`, prema [uputama](docs/Razvoj.md#postgresql-integracijski-test). GitHub Actions konfiguracija provjerava backend, migracije, web i Android nakon slanja izmjena na GitHub.

## Dokumentacija

- [Izvorni funkcionalni koncept](docs/RibolovniSistemRS.docx)
- [Detaljna specifikacija prve operativne verzije](docs/Specifikacija_prve_operativne_verzije.md)
- [Arhitektura i granice modula](docs/Arhitektura.md)
- [Razvoj i provjere](docs/Razvoj.md)
- [Početne arhitektonske odluke](docs/adr/0001-pocetna-arhitektura.md)
- [JPA relacije bez poslovnih šifara](docs/adr/0002-jpa-relacije-bez-poslovnih-sifara.md)
- [Organizacije i pristup: implementirano i naredni koraci](docs/Organizacije_i_pristup.md)
- [Pozivi, e-pošta i provjera prijave](docs/Pozivi_i_prijava.md)
- [OpenAPI ugovor](docs/api/openapi.yaml)

Slijede teritorijalna ovlaštenja te osobe i članstvo. Detaljan implementirani opseg i preostali posao opisani su u dokumentu o organizacijama i pristupu.
