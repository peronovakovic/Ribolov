# Projektna pravila

## Entiteti i veze

- Koristiti JPA entitete i stvarne relacije: `@ManyToOne`, `@OneToMany`, `@OneToOne` ili `@ManyToMany`, prema modelu.
- Vezu prema drugom entitetu ne predstavljati njegovim kodom, nazivom, stringom ili zasebnim skalarnim UUID/ID poljem u entitetu. U bazi koristiti strani ključ preko `@JoinColumn`.
- Ne dodavati polje ili kolonu `code`, niti zamjensku poslovnu šifru bez novog izričitog zahtjeva korisnika.
- UUID identifikatori u API zahtjevima/DTO-ovima su dozvoljeni; pri upisu se razrješavaju u entitete. OIDC `issuer` i `subject` su identitet vanjskog sistema, ne lokalne JPA veze.
- Konačni skupovi vrijednosti kao tip organizacije, status i uloga mogu biti enum-i. Ako se uvede zaseban entitet, referencirati ga JPA relacijom.
- JPA entitete ne vraćati direktno kroz HTTP. DTO formirati unutar transakcije; `open-in-view` ostaje isključen.
- Ne koristiti `CascadeType.REMOVE` ili `orphanRemoval` za historijske organizacione veze i dodjele uloga.

## Granice i provjere

- Domenski model organizacija pripada modulu `organizations`. Drugi moduli koriste njegov named interface `organizations :: model` za JPA veze.
- Poslovne mutacije pripadaju vlasničkom modulu. Sama dostupnost javnog entiteta ne daje pravo drugom modulu da mijenja njegove podatke.
- Flyway je izvor produkcijske šeme; Hibernate koristi `ddl-auto=validate`.
- H2 testovi provjeravaju JPA mapiranje, a profil `postgres-it` provjerava PostgreSQL ograničenja i RLS s runtime računom.
