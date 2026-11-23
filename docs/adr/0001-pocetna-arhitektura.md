# ADR 0001: Modularni monolit i odvojeni klijenti

Datum: 23.11.2026.  
Status: usvojeno za početnu razvojnu osnovu.

## Kontekst

Prva operativna verzija povezuje organizacije, članstvo, uplate, dozvole i rad na terenu. Pravila pristupa i transakcijska dosljednost važniji su od nezavisnog skaliranja pojedinih domena. Terenski rad mora imati kontrolisan offline režim.

## Odluka

Koristimo Java 21 / Spring Boot modularni monolit s granicama koje provjerava Spring Modulith. Jedan PostgreSQL/PostGIS ima šeme po modulima i poseban runtime račun. Flyway upravlja migracijama.

Administracija je Vue/TypeScript web klijent preko BFF sesija. Android je Kotlin/Compose aplikacija s planiranom šifriranom lokalnom bazom. Keycloak upravlja identitetom; domenski backend odlučuje o pravima pristupa. Mobilni klijent koristi Authorization Code + PKCE i audience-ograničene tokene.

Trajne događaje/outbox, objektno spremište i RLS uvodimo uz prve slučajeve koji ih koriste, uz testove. Ne predstavljamo ih kao već implementirane mogućnosti.

## Posljedice

- Jednostavniji razvoj, transakcije i lokalno pokretanje; jedna verzija backenda za isporuku.
- Arhitektonski testovi i jasni javni ugovori sprečavaju preplitanje modula.
- Dijeljena baza traži disciplinu vlasništva nad podacima i migracijama.
- Svaki HTTP i domenski slučaj mora provjeriti organizacijski/teritorijalni opseg; OIDC uloga sama nije dovoljna.
- Android zahtijeva zaseban razvojni ciklus i testove offline ponašanja.
- Razdvajanje u servise ostaje moguća kasnija odluka uz dokazanu operativnu potrebu.

## Razmotrene alternative

Mikroservisi bi odmah uveli distribuirane transakcije, složeniju isporuku i nadzor. U ovom obimu nema potvrđene potrebe koja opravdava taj trošak.

Jedan web/PWA klijent smanjio bi broj tehnologija, ali zaseban Android klijent daje neposrednu kontrolu nad lokalnom pohranom, uređajem i pozadinskom sinhronizacijom.

## Reference

- [Spring Modulith: granice modula](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
- [Spring Security: OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)
- [Android: aplikacije s offline radom](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Vite: izvorno učitavanje konfiguracije](https://vite.dev/config/)
