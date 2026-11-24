# ADR 0002: JPA relacije bez poslovnih šifara

Datum: 23.11.2026.  
Status: usvojeno po izričitom zahtjevu korisnika.

## Odluka

Lokalne entitete povezujemo JPA relacijama i stranim ključevima. Polje/kolona `code` se ne uvodi. Kod, naziv ili zaseban skalarni identifikator ne zamjenjuju objektni odnos između entiteta.

Organizacije izdvajamo iz `access` u samostalan modul `organizations`. Njegov named interface `model` izlaže tipove potrebne za međumodulske JPA veze. `access` zavisi od `organizations :: model`; organizacije ne zavise od korisničkih uloga. Isto pravilo omogućava buduće veze članstva i područja bez kružnih zavisnosti.

## Primjena

- `OrganizationAffiliation.child` i `.parent` su `@ManyToOne Organization`.
- `RoleAssignment.user` je `@ManyToOne ApplicationUser`.
- `RoleAssignment.organization` je `@ManyToOne Organization`.
- Inverzne kolekcije koriste `@OneToMany(mappedBy = ...)`.
- Veze su lazy, bez kaskadnog brisanja historije. Entiteti imaju UUID ključ i `@Version`.
- Tip, status i fiksni skup uloga su enum-i, a ne zasebni entiteti povezani šifrom.
- UUID u DTO-u služi API granici. Pri budućem upisu mora se razriješiti u entitet i provjeriti ovlaštenje.
- OIDC `issuer + subject` predstavlja vanjski identitet; Keycloak korisnik nije lokalni JPA entitet.

Javne JPA klase ne prenose vlasništvo nad upisom. Mutacije organizacije ostaju u modulu `organizations`. Entitete ne serijalizujemo kroz HTTP; aplikacijski servis vraća DTO unutar transakcije.

## Provjere

Testovi čuvaju i ponovo učitavaju relacije, provjeravaju historiju, optimističku verziju i zabranu brisanja referenciranog entiteta. PostgreSQL dodatno provjerava strane ključeve, preklapanje perioda i RLS s runtime računom.

Reference: [JPA čuvanje i detekcija novih entiteta](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html), [PostgreSQL RLS](https://www.postgresql.org/docs/17/ddl-rowsecurity.html).
