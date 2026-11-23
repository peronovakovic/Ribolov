# Specifikacija prve operativne verzije sistema za sportski ribolov Republike Srpske

Verzija 0.1 | Datum 23. novembar 2026. | Status Prijedlog za usaglašavanje

Prva operativna verzija omogućava udruženju da vodi članstvo, evidentira uplate i izdaje dozvole, ribolovcu da preuzme svoje dokumente, ribočuvaru da obavi i evidentira kontrolu sa internetom i bez njega, a Savezu i Ministarstvu da prate jasno definisane pokazatelje. Cjelovit tok od registracije do kontrole i izvještaja predstavlja osnovu prihvatanja verzije.

Poslovna osnova je [RibolovniSistemRS.docx](RibolovniSistemRS.docx), posebno poglavlja 2–8 i 11–15. Ova specifikacija razrađuje taj koncept. Pravila, ograničenja i brojčane vrijednosti koje koncept ne određuje predstavljaju predloženu osnovu za razvoj, a ne ranije odobrene odluke. Vlasnici odluka i uslovi potvrde navedeni su u poglavlju 3.

Izrazi „mora“ i „ne smije“ određuju ponašanje predložene verzije. Nakon usaglašavanja, odobrena verzija dokumenta postaje osnova za implementaciju i prihvatanje. Promjene obuhvata, finansijskih pravila ili offline režima zahtijevaju novu verziju specifikacije i pregled povezanih kriterija prihvatanja.

Za poslovni pregled prvo pročitati poglavlja 1–4 i 20; za implementaciju procese iz poglavlja 5–12, model i ugovore iz 13–14; za testiranje koristiti poglavlja 15–18. Poglavlje 19 povezuje zahtjeve s izvornim konceptom i redoslijedom razvoja. Oznake zahtjeva, odluka i kriterija ostaju stabilne pri narednim revizijama.

## 1 Cilj i granice prve verzije

### 1.1 Poslovni rezultat

Udruženje može voditi osnovne evidencije bez paralelnog ručnog objedinjavanja tabela. Jedna osoba ima jedinstven identitet u sistemu i može imati više članstava i dozvola. Svako izdavanje, evidentiranje uplate, poništavanje i terenska kontrola imaju odgovornu osobu, vrijeme i istoriju događaja.

Prva verzija obuhvata administrativni web portal, korisnički portal prilagođen telefonu i terensku aplikaciju ribočuvara. Izbor tehnologije terenske aplikacije ostaje predmet tehničkog projekta; funkcionalno mora zadovoljiti offline rad, kameru, bezbjedno lokalno čuvanje i oporavak nakon prekida aplikacije.

### 1.2 Obavezni obuhvat

| Cjelina | Rezultat prve verzije |
| --- | --- |
| Organizacije i pristup | Hijerarhija Ministarstvo–Savez–udruženje, korisnici, uloge, teritorijalna ovlaštenja i audit |
| Ribolovci i članstvo | Jedinstvena evidencija osoba, zahtjev za članstvo, članarina, status i digitalna članska kartica |
| Područja i pravila | Registar voda, područja i revira, granice, upravljači, objavljena pravila i vremenski određene zabrane |
| Dozvole | Katalog, cjenovnik, zahtjev, provjera uslova, izdavanje, QR, preuzimanje, provjera i opoziv |
| Uplate | Evidencija gotovine i bankovne uplate, raspoređivanje na obavezu, ispravke i evidencija izvršenih povrata |
| Ribočuvarski rad | Ovlaštenja, registracija uređaja, online i offline provjera, unos kontrole i sinhronizacija |
| Ribolovački portal | Profil, zahtjevi, instrukcije za uplatu, članstva, kartice, dozvole i osnovni pregled područja |
| Izvještavanje | Članstvo, dozvole, evidentirane naplate i povrati, kontrole, zabilježene nepravilnosti i svježina podataka |
| Uvođenje i rad | Kontrolisani uvoz članova, obuka, podrška, sigurnosne kopije i nadzor rada |

### 1.3 Funkcije odložene za naredne verzije

Izvan ove verzije su kartično plaćanje i automatski uvoz bankovnih izvoda, fiskalizacija i glavno knjigovodstvo, dnevnik ulova, poribljavanje, prijave građana i njihov predmetni postupak, vođenje prekršajnih postupaka i kazni, društvene objave, dopisivanje, video, takmičenja, rezervacije vodiča ili smještaja i turistički oglasi.

Osnovna turistička upotreba ostaje obuhvaćena: posjetilac nalazi područje, čita pravila, podnosi zahtjev i preuzima dozvolu nakon provjera i evidentiranja uplate. Ne obećava se trenutna online kupovina. Prije podnošenja zahtjeva prikazuju se radno vrijeme izdavaoca i očekivani rok obrade koji udruženje održava.

Nema izdavanja dozvola bez veze sa serverom, međusobnog izdavanja u ime drugog udruženja, zajedničke dozvole više izdavalaca, automatske obnove članstva ili dozvole i prenosa dozvole na drugu osobu. Uvoz ranije izdatih dozvola nije dio osnovnog uvoza; njihov tretman mora biti potvrđen odlukom ODL-13.

## 2 Pojmovi i zajedničke konvencije

| Pojam | Značenje u prvoj verziji |
| --- | --- |
| Osoba | Ribolovac evidentiran jednim internim identifikatorom, nezavisno od naloga i broja članstava |
| Korisnički nalog | Sredstvo prijave; može biti povezan s jednom osobom i imati više vremenski ograničenih uloga |
| Članstvo | Odnos osobe i jednog udruženja za određen period i kategoriju |
| Aktivan član | Osoba s odobrenim, nesuspendovanim članstvom koje obuhvata posmatrani datum i s izmirenom ili odobreno oslobođenom članarinom |
| Aktivna dozvola | Izdata, neopozvana dozvola čiji period obuhvata posmatrani trenutak; dozvoljenost ribolova dodatno zavisi od lokacije i pravila |
| Aktivan ribolovac | Osoba s najmanje jednom aktivnom dozvolom na posmatrani trenutak; pokazatelj se ne izjednačava s aktivnim članstvom |
| Turistička dozvola | Dozvola čiji proizvod ima namjenu TURISTIČKA; državljanstvo, prebivalište i članstvo nisu zamjena za ovu klasifikaciju |
| Područje | Geografska cjelina kojom u određenom periodu upravlja jedno udruženje |
| Revir | Dio jednog područja s vlastitim nazivom, granicom i po potrebi posebnim pravilima |
| Kontrola | Jedan zabilježen susret ribočuvara i kontrolisane osobe na mjestu i u vremenu kontrole |
| Nepravilnost | Nalaz ribočuvara tokom kontrole; ne predstavlja automatski pravosnažno utvrđen prekršaj |
| Opoziv dozvole | Trajno ukidanje važenja konkretne izdate dozvole od evidentiranog trenutka, uz razlog |
| Referentni trenutak izvještaja | Trenutak za koji se računa stanje, uz odvojeno vrijeme posljednjeg osvježavanja podataka |

Identifikatori su stabilni i ne sadrže lične podatke. Prikazni broj dozvole ili članske kartice razlikuje se od internog identifikatora i QR sadržaja. JMBG i broj identifikacionog dokumenta ne prikupljaju se u osnovnom prijedlogu; eventualna promjena zahtijeva posebnu potvrdu obima i pristupa.

Vrijeme događaja server čuva u UTC, a korisniku prikazuje u zoni Europe/Sarajevo. Datumi rođenja i poslovni kalendarski datumi čuvaju se kao datumi. Vremenski intervali su oblika [početak, kraj): početak je uključen, kraj isključen. Dnevna dozvola za budući dan važi od lokalne ponoći izabranog datuma do lokalne ponoći narednog dana; za tekući dan počinje trenutkom izdavanja. Ne pretpostavlja se uvijek trajanje od 24 sata.

Novčani iznosi vode se u BAM, kao tačan decimalni iznos s dvije decimale ili cijeli broj feninga. Prva verzija nema druge valute, automatske popuste ni poreske obračune. Koordinate se razmjenjuju kao geografska širina i dužina, uz zabilježenu tačnost mjerenja kada je dostupna.

## 3 Predložene odluke i odgovornost za potvrdu

Sve odluke u tabeli imaju status PREDLOŽENO. Mogu se koristiti za prototip i procjenu, ali njihov vlasnik mora potvrditi konačan oblik prije naznačene kontrolne tačke. Razvojni tim bilježi datum, odgovornu osobu i obrazloženje svake potvrde ili promjene.

| ID | Predložena odluka | Vlasnik potvrde | Rok potvrde |
| --- | --- | --- | --- |
| ODL-01 | Ministarstvo i Savez imaju agregate; imenovane evidencije dostupne su samo operativno ovlaštenim korisnicima | Ministarstvo i Savez | Prije razvoja autorizacije |
| ODL-02 | Jedna osoba može biti član više udruženja; turistička dozvola može se izdati bez članstva kada proizvod to dopušta | Savez i pilot udruženja | Prije modela članstva |
| ODL-03 | Plaćanje gotovinom ili bankovnim prenosom evidentira službenik; kartice i automatizovano bankarstvo se odlažu | Pilot udruženja | Prije razvoja naplate |
| ODL-04 | Cijela obaveza mora biti izmirena ili izričito oslobođena prije izdavanja; nema djelimičnih uplata u osnovnom toku | Pilot udruženja | Prije razvoja izdavanja |
| ODL-05 | Članska dozvola zahtijeva odgovarajuće članstvo za cijeli period dozvole; kasnija promjena članstva ne opoziva dozvolu automatski | Savez i pilot udruženja | Prije razvoja izdavanja |
| ODL-06 | Offline podaci su svježi najviše 24 sata; offline ovlaštenje uređaja traje najviše 72 sata od potpune uspješne sinhronizacije | Ribočuvarska služba i vlasnik sistema | Prije terenskog prototipa |
| ODL-07 | Jedan proizvod ima jednog izdavaoca i fiksni obuhvat; potpuna zabrana u bilo kojem dijelu tog obuhvata i perioda blokira novu prodaju prema POD-06 | Savez | Prije kataloga dozvola |
| ODL-08 | Osnovni jezik je srpski latinica; javni katalog, korisnički zahtjev i dozvola imaju engleski prikaz uz odobren prevod pravila | Vlasnik proizvoda i pilot udruženja | Prije razvoja korisničkog portala |
| ODL-09 | Digitalna i odštampana dozvola koriste isti identifikator i QR; formalni obrazac i prihvatljivost potvrđuje nadležni predstavnik | Nadležni predstavnik i Savez | Prije izdavanja stvarnih dozvola |
| ODL-10 | Periodi čuvanja podataka, odgovornost za obradu, uslovi korištenja i pristup fotografijama potvrđuju se za pilot | Vlasnik sistema i odgovorna osoba za podatke | Prije unošenja stvarnih podataka |
| ODL-11 | Model opterećenja, podržani uređaji i ciljevi oporavka iz poglavlja 16 predstavljaju početne ciljeve | Vlasnik sistema i tehnički tim | Prije infrastrukturnog projekta |
| ODL-12 | Pilot obuhvata punoljetne osobe; obrada maloljetnika, zastupnika, posebnih povlastica i dodatnih uslova za dozvolu zahtijeva poseban dogovor | Nadležni predstavnik i pilot udruženja | Prije konačnog obuhvata pilota |
| ODL-13 | U pilotu se nove dozvole izdaju kroz sistem; stare papirne dozvole obrađuju se po izričito dogovorenoj paralelnoj proceduri | Savez i ribočuvarska služba | Prije početka pilota |
| ODL-14 | Puni povrat se može evidentirati samo uz trajni opoziv povezane dozvole kada je izdata; nema djelimičnih povrata u prvoj verziji | Pilot udruženja | Prije razvoja povrata |
| ODL-15 | Pilot uključuje najmanje dva udruženja, najmanje dva terenska uređaja i test izolacije podataka između udruženja | Vlasnik proizvoda | Prije plana prihvatanja |

Konkretne cijene, kategorije dozvola, nadležnosti nad vodama, dozvoljene vrste ribolova, minimalne mjere, zabrane i dokumentacija potrebna za izdavanje predstavljaju poslovne ulazne podatke. Ovaj dokument ne utvrđuje njihovu zakonsku sadržinu. Pilot udruženja dostavljaju odobrene vrijednosti i potvrđuju da je predloženi tok izdavanja primjenjiv.

## 4 Korisničke uloge i prava pristupa

### 4.1 Uloge

| Oznaka | Uloga | Osnovna odgovornost |
| --- | --- | --- |
| SYS | Administrator sistema | Organizacije, početni administratori, tehnička konfiguracija i kontrolisana podrška |
| MIN | Analitičar Ministarstva | Agregirani pregled svih uključenih organizacija |
| SAV | Analitičar Saveza | Agregirani pregled udruženja svog Saveza |
| ADM | Administrator udruženja | Operativna administracija vlastitog udruženja, pravila, cjenovnik, ovlaštenja i opozivi |
| OPE | Operater udruženja | Članovi, zahtjevi, uplate i izdavanje u vlastitom udruženju |
| CUV | Ribočuvar | Provjera dozvola i evidentiranje kontrola u dodijeljenim područjima |
| RIB | Ribolovac | Vlastiti profil, zahtjevi, članstva, kartice i dozvole |
| GOST | Neprijavljeni posjetilac | Objavljene informacije o područjima, pravilima i proizvodima |

### 4.2 Matrica pristupa

„Svoje“ označava udruženje aktivne uloge. „Dodijeljeno“ označava vremenski važeća područja ribočuvara. „Agregati“ isključuju ime, kontakt, fotografiju, identifikatore osoba i pojedinačne dozvole. Oznaka „—“ znači da funkcija nije dostupna toj ulozi.

| Radnja ili podaci | SYS | MIN | SAV | ADM | OPE | CUV | RIB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Organizacije i veze | Uređuje | Pregled svih | Pregled svog Saveza | Pregled svoje | Pregled svoje | — | — |
| Dodjela ADM i analitičkih uloga | Uređuje | — | — | — | — | — | — |
| Dodjela OPE i CUV uloga | — | — | — | Svoje | — | — | — |
| Lični podaci i članstvo | Samo podrška | Agregati | Agregati | Svoje | Svoje | Minimum za provjeru | Vlastito |
| Cjenovnik i pravila | — | Objavljeno | Objavljeno | Svoje | Pregled svojih | Objavljeno | Objavljeno |
| Potvrda uplate i izdavanje | — | — | — | Svoje | Svoje | — | Podnosi zahtjev |
| Opoziv i finansijske korekcije | — | — | — | Svoje | — | — | Podnosi zahtjev podršci |
| Provjera dozvole | — | — | — | Izdate svojim udruženjem | Izdate svojim udruženjem | Dodijeljeno | Vlastita |
| Kontrole i nalazi | Samo podrška | Agregati | Agregati | Svoje područje | — | Unos i vlastite | — |
| Operativni izvještaji | — | Agregati svih | Agregati svog Saveza | Svoje | Članstvo i naplate svojih | Vlastite kontrole | — |
| Audit | Tehnički i odobrena podrška | — | — | Poslovni svog udruženja | — | — | — |

### 4.3 Obavezna pravila autorizacije

**IAM-01** Svaki zahtjev serveru provjerava aktivni nalog, ulogu, organizaciju, teritorijalni opseg i pravo na konkretan zapis. Provjera važi i za pretragu, broj rezultata, izvoz, PDF, fotografije, pozadinske zadatke i sinhronizaciju. Skriveno dugme u interfejsu nije kontrola pristupa.

**IAM-02** Višestruke uloge su dozvoljene. Korisnik bira aktivnu organizacionu ulogu; interfejs stalno prikazuje taj kontekst. Operacije ne spajaju ovlaštenja iz različitih konteksta. Promjena konteksta prazni prethodno učitane podatke.

**IAM-03** Ribočuvar vidi minimum podataka potrebnih za provjeru dozvole koja obuhvata dodijeljeno područje, uključujući dozvolu drugog izdavaoca samo ako pokrivenost i njegovo ovlaštenje to opravdavaju. Ne dobija opštu pretragu svih članova, kontakata ili uplata.

**IAM-04** SYS nema redovan pristup poslovnim ličnim podacima. Privremeni pristup podrške zahtijeva konkretan predmet, odobrenje administratora pogođenog udruženja, opseg podataka i trajanje do 60 minuta. Svaki pristup se bilježi. Podrška ne potvrđuje uplate i ne izdaje dozvole u ime operatera.

**IAM-05** Deaktiviranje naloga ili uloge blokira naredni online zahtjev i buduće sinhronizacije. Offline ograničenje opoziva obrađeno je u poglavlju 11. Brisanje naloga ne briše istorijske poslovne zapise.

**IAM-06** Pozivi administratorima i operaterima su jednokratni, važe 48 sati i vezani su za potvrđenu adresu elektronske pošte. Operativne privilegovane uloge koriste drugi faktor prijave. Lozinke, tokeni i ključevi nikada se ne bilježe u audit zapisima.

## 5 Organizacije i početno postavljanje

**ORG-01** Organizacija ima identifikator, tip, naziv, šifru, kontakt, status i vremenski važeću vezu s nadređenom organizacijom. U istom trenutku udruženje pripada jednom Savezu. Ciklusi i nevažeće kombinacije tipova nisu dozvoljeni.

**ORG-02** SYS otvara organizaciju i prvog ADM korisnika na osnovu potvrđenog zahtjeva. ADM unosi kontakt, radno vrijeme, instrukcije za uplatu i službenike. Područje i period upravljanja unosi SYS po potvrđenoj evidenciji nadležnosti; ADM ne može sam sebi dodijeliti tuđe područje.

**ORG-03** Organizacija se arhivira, a ne fizički briše. Arhiviranje onemogućava nove zahtjeve i izdavanje. Postojeće dozvole i dalje se provjeravaju do isteka ili pojedinačnog opoziva. Prije arhiviranja mora biti određen odgovorni korisnik za otvorene uplate, povrate i podršku.

**ORG-04** Promjena nadređene organizacije ima datum dejstva. Izvještaji o prometu pripisuju događaj organizacionoj vezi važećoj u trenutku događaja; izvještaji o trenutnom stanju koriste vezu važeću u referentnom trenutku. Stari zapisi se ne prepisuju novim nadređenim identifikatorom.

## 6 Osobe i korisnički nalozi

### 6.1 Podaci i registracija

**OSO-01** Osoba sadrži ime, prezime, datum rođenja, državu prebivališta, kontakt kada postoji, opcionu fotografiju, status provjere identiteta i istoriju provjere. Država prebivališta nije državljanstvo. Polje se u izvještajima tako i naziva. Za postojeće uvezene osobe nepoznata vrijednost ostaje označena kao nepoznata.

**OSO-02** Samostalna registracija traži ime, prezime, datum rođenja, državu prebivališta, adresu elektronske pošte i prihvatanje važeće verzije uslova korištenja. Potvrda adrese potvrđuje kontrolu nad adresom, a ne lični identitet. Do završetka provjere identiteta nalog može pregledati katalog i pripremati zahtjeve, ali ne može dobiti izdatu dozvolu.

Datum rođenja ne smije biti u budućnosti. Predloženi pilot prag je 18 navršenih godina na lokalni datum podnošenja; ista provjera važi za operaterski unos i uvoz. Ako ODL-12 proširi obuhvat, prvo se definišu zastupanje, kategorije i potrebni podaci, pa se mijenja ova validacija.

**OSO-03** Operater može evidentirati osobu bez naloga i bez elektronske pošte. Za takvu osobu može voditi članstvo, naplatu i izdavanje i predati odštampanu karticu ili dozvolu. Funkcionisanje osnovnih procesa ne zavisi od posjedovanja telefona.

**OSO-04** U pilotu operater provjerava identitet lično, bilježi način provjere, datum i svoj identifikator, bez kopije identifikacionog dokumenta. Potvrđena fotografija je opciona; bez nje ekran navodi da fotografija nije dostupna i ribočuvar koristi dogovorenu alternativnu provjeru identiteta. Nepotvrđena korisnička fotografija se ne predstavlja kao potvrđena.

### 6.2 Duplikati i povezivanje naloga

**OSO-05** Kandidati za duplikat određuju se normalizovanim imenom, prezimenom i datumom rođenja, uz pomoć potvrđenog kontakta kada postoji. Podudaranje nije dokaz da je riječ o istoj osobi. Sistem ne spaja osobe automatski.

**OSO-06** Operater pretražuje osobe vezane za svoje udruženje. Za osobu drugog udruženja koristi jednokratni kod povezivanja koji osoba daje iz vlastitog portala ili zahtjev za kontrolisano razrješenje identiteta. Ne postoji javna ni operaterska globalna pretraga ličnih podataka. Na nejasno podudaranje izdavanje čeka razrješenje.

**OSO-07** Povezivanje postojećeg članstva s nalogom zahtijeva potvrđen nalog i jednokratni poziv operatera nakon provjere identiteta. Sam unos broja članske kartice ili poznavanje datuma rođenja nije dovoljan. Jedna osoba može imati najviše jedan aktivni nalog za portal.

**OSO-08** Kontrolisano spajanje duplikata provodi SYS kroz odobreni pristup podrške i odobrenje pogođenih udruženja. Čuvaju se izvorni identifikatori, referenca na kanonsku osobu i audit. Istorijski sadržaj izdatih dozvola ostaje nepromijenjen; izvještaji jedinstvenih osoba koriste kanonski identitet.

**OSO-09** Ribolovac sam mijenja kontakt i postavlja prijedlog fotografije; novo potvrđivanje kontakta prethodi njegovoj upotrebi za prijavu. Promjena imena ili datuma rođenja zahtijeva operatersku provjeru i audit. Podaci utisnuti na ranije izdatu dozvolu mijenjaju se samo kroz zamjenu dozvole.

## 7 Članstvo i članarina

### 7.1 Zahtjev i stanja

**CLA-01** Zahtjev za članstvo unosi ribolovac ili operater. Sadrži osobu, udruženje, kategoriju, početak i kraj perioda, verziju cjenovnika članarine i napomenu. Sistem sprečava preklapanje odobrenih ili suspendovanih članstava iste osobe u istom udruženju. Članstva različitih udruženja mogu se preklapati.

| Stanje | Dozvoljeni naredni koraci | Odgovorna uloga |
| --- | --- | --- |
| ZAHTJEV | ODOBRENO, ODBIJENO ili OTKAZANO | ADM ili OPE odobrava i odbija; podnosilac otkazuje prije odobrenja |
| ODOBRENO | SUSPENDOVANO ili PREKINUTO | ADM |
| SUSPENDOVANO | ODOBRENO ili PREKINUTO | ADM, uz razlog |
| ODBIJENO, OTKAZANO, PREKINUTO | Novi zahtjev po potrebi | Nema ponovnog otvaranja istog zapisa |

Istek i buduće važenje izvedeni su iz datuma, a ne ručno promjenjivi statusi. Svaka promjena stanja čuva prethodno i novo stanje, autora, vrijeme i razlog. Suspendovanje zahtijeva efektivni trenutak koji ne može biti prije vremena unosa u redovnom toku.

**CLA-02** Odobravanje članstva kreira obavezu članarine. Članstvo je aktivno tek kada su ispunjeni uslovi iz definicije u poglavlju 2. Oslobađanje od plaćanja dopušteno je ADM korisniku za unaprijed definisanu kategoriju, uz razlog i audit. Prikazuju se odvojeno odobrenje članstva i stanje članarine.

**CLA-03** Nova sezona kreira novo članstvo ili novi period u kojem nema preklapanja; prethodna sezona se ne prepisuje. Cjenovnik i kategorije imaju vremensko važenje, a ugovoreni iznos članarine ostaje sačuvan na obavezi.

### 7.2 Digitalna članska kartica

**CLA-04** Kartica prikazuje ime, udruženje, članski broj, period i status. Dostupna je u portalu i za štampu. Jasno je označena kao članska kartica i sadrži obavještenje da se pravo ribolova provjerava zasebnom dozvolom. QR članske kartice, ako postoji, koristi drugu vrstu sadržaja i ne prolazi kao QR dozvole.

**CLA-05** Suspenzija ili prestanak članstva odmah mijenja online prikaz kartice. Ranije preuzeti prikaz sadrži vrijeme generisanja. Promjena članstva ne opoziva automatski već izdatu dozvolu; ADM dobija listu povezanih aktivnih dozvola za odvojenu odluku prema ODL-05.

## 8 Vode i ribolovna područja

### 8.1 Registar i granice

**POD-01** Voda ima naziv, alternativne nazive i tip, npr. rijeka ili jezero. Područje ima šifru, naziv, geometriju granice, opis pristupa i veze s jednom ili više voda. Revir pripada jednom području i ima vlastitu granicu. Jedna voda može prolaziti kroz više područja različitih udruženja.

**POD-02** SYS evidentira vremenski određenu nadležnost udruženja nad područjem iz potvrđenog ulaznog dokumenta. ADM održava sadržaj vlastitog područja. Neobjavljena područja nisu javno vidljiva i ne mogu se koristiti za novi proizvod dozvole. Geometrija se unosi ili uvozi uz validaciju i pregled na karti; neispravni poligoni se odbijaju.

**POD-03** Promjena granice ili upravljača čuva prethodnu verziju. Ako promjena zahvata buduće ili aktivne dozvole, objavljivanje se zaustavlja dok se ne utvrdi tretman tih dozvola. Sistem ne proširuje niti sužava istorijski obuhvat dozvole prepisivanjem geometrije.

### 8.2 Pravila i privremene zabrane

**POD-04** Verzija pravila sadrži područje ili revir, period dejstva, dozvoljene tehnike, vrste riba, minimalne mjere s jedinicom, ograničenja količine s periodom obračuna, periode zabrane i tekst dodatnih uslova. ADM objavljuje pravila uz izvor ili oznaku odgovorne odluke. Povučena i zamijenjena pravila ostaju u istoriji.

**POD-05** Pravilo revira može pooštriti ograničenje područja. Sukob koji se ne može jednoznačno protumačiti blokira objavljivanje; aplikacija ne pretpostavlja da lokalno pravilo smije ublažiti zabranu područja. Za automatsku ocjenu prve verzije koristi se samo potpuna zabrana ribolova za tačno određeni prostor i vrijeme. Ostala ograničenja prikazuju se ribočuvaru za procjenu.

**POD-06** Potpuna zabrana ima prostor, početak, kraj ili oznaku do daljnjeg, razlog i autora. Blokira nove zahtjeve čiji period zahvata zabranu u bilo kojem dijelu fiksnog obuhvata proizvoda. Već izdatu dozvolu ne opoziva automatski, ali provjera na zahvaćenoj lokaciji prikazuje da ribolov nije dozvoljen. Nema automatskog povrata; zahtjev razmatra ADM.

**POD-07** Katalog je javno čitljiv i pretraživ po vodi, području, reviru i udruženju. Prikazuje pravila za izabrani datum, aktuelne zabrane, dostupne proizvode, cijene, kontakt i način dobijanja dozvole. Ako odobren engleski prevod bitnih pravila ne postoji, engleski tok jasno upozorava i ne nudi samostalno podnošenje zahtjeva za taj proizvod do objave prevoda.

## 9 Katalog i životni ciklus dozvole

### 9.1 Proizvod i cijena

**DOZ-01** Verzija proizvoda ima izdavaoca, šifru, naziv, namjenu STANDARDNA ili TURISTIČKA, uslov članstva, cijenu, period prodaje, način trajanja, fiksni skup područja ili revira i povezanu verziju uslova. Obuhvat mora biti unutar važeće nadležnosti izdavaoca za cijeli period ponuđenog važenja.

**DOZ-02** Podržana trajanja su jedan kalendarski dan, izabrani raspon kalendarskih datuma s ograničenjem maksimalnog broja dana i kalendarska godina. Za raspon se krajnji datum u interfejsu prikazuje kao uključen, a čuva se kraj narednog lokalnog dana. Ako izabrani početni datum već traje, konačni početak je trenutak izdavanja; budući datum počinje u lokalnu ponoć. Godišnja dozvola kupljena tokom godine počinje trenutkom izdavanja i završava početkom naredne godine. Nema retroaktivnog važenja; zahtjev za završeni period se odbija.

**DOZ-03** Cijena i uslovi su verzionisani. Objavljena verzija se ne mijenja; objavljuje se nova. Promjena cijene ne mijenja ranije prihvaćenu obavezu niti izdatu dozvolu. Proizvod se povlači iz nove prodaje bez brisanja istorije.

Besplatno izdavanje moguće je samo kroz proizvod s cijenom nula i obaveznom osnovom oslobađanja koju objavljuje ADM. Takva obaveza označava se OSLOBOĐENA. OPE ne mijenja cijenu niti odobrava ad hoc oslobađanje; besplatna izdata dozvola ulazi u broj izdatih, ali ne u broj prodatih dozvola.

### 9.2 Zahtjev i izdavanje

**DOZ-04** Zahtjev sadrži osobu, proizvod i njegovu verziju, traženi početak i kraj, režim početka ODREĐENI_DATUM ili PRI_IZDAVANJU, izračunati iznos, način plaćanja i kanal nastanka. Budući početak koristi ODREĐENI_DATUM; tekući dan ili godina koriste PRI_IZDAVANJU, pri čemu se tačan početni trenutak određuje tek u transakciji izdavanja. Operater može unijeti zahtjev za osobu bez naloga. Ribolovac vidi sve troškove i pravila prije konačnog podnošenja.

**DOZ-05** Prelazi zahtjeva su NACRT → PODNESEN → ODOBREN_ZA_UPLATU → REALIZOVAN. PODNESEN može preći u ODBIJEN; nacrt i zahtjev bez evidentirane uplate mogu preći u OTKAZAN; odobren zahtjev bez uplate može preći u ISTEKAO. Odbijanje sadrži razlog. Sprovedena uplata se ne briše otkazivanjem zahtjeva. Plaćen zahtjev bez izdate dozvole može završiti kao ZATVOREN_BEZ_IZDAVANJA samo kada ADM potvrdi puni povrat ili dopuštenu preraspodjelu na drugi zahtjev i zatvori prvobitnu obavezu.

**DOZ-06** Pri odobravanju operater provjerava identitet, podobnost osobe, period, članstvo kada je uslov, područja, zabrane i važeću cijenu. Odobren zahtjev dobija referencu i instrukcije za uplatu. Za budući početak predloženi rok plaćanja je raniji od trenutaka: 48 sati od odobrenja i traženi početak. Za početak u tekućem danu ili godini odobrenje i neposredno operatersko evidentiranje naplate vode u izdavanje u istoj radnoj sesiji; ne izdaje se bankovna instrukcija s rokom koji je već prošao. Besplatni proizvod poslije odobrenja prelazi neposredno u izdavanje uz ponovnu provjeru uslova.

**DOZ-07** Prije konačnog izdavanja server ponavlja provjeru uslova, uključujući nove zabrane i promjene ovlaštenja. Ako je uplata već evidentirana, a uslovi više nisu ispunjeni, dozvola se ne izdaje; nastaje predmet „uplaćeno bez izdavanja“ za ADM. Potrebni su novi zahtjev s dogovorenim uslovima i kontrolisana preraspodjela ili evidencija povrata. Plaćeni stari zahtjev ostaje vidljiv. Ako je za režim ODREĐENI_DATUM odobreni početak već prošao, ne mijenja se neprimjetno period starog zahtjeva: kreira se novi zahtjev s početkom najranije u trenutku budućeg izdavanja. Za PRI_IZDAVANJU početak nastaje pri uspješnom izdavanju, a traženi krajnji datum mora i dalje biti u budućnosti.

**DOZ-08** Izdavanje je jedna atomska poslovna operacija: zaključa obavezu, provjeri da nema postojeće dozvole, kreira jedinstven broj i nepromjenjivi sadržaj dozvole, evidentira audit i označi zahtjev realizovanim. Ponavljanje istog zahtjeva za izdavanje vraća istu dozvolu. Slanje e-pošte i izrada kopije PDF-a ne smiju uzrokovati novo izdavanje.

**DOZ-09** Za istu osobu, isti proizvod i period koji se preklapa prikazuje se postojeća dozvola i blokira duplo izdavanje. Drugi proizvod ili dopunski revir mogu biti dozvoljeni uz jasno prikazivanje postojećih dozvola. Administrativna zamjena koristi poseban tok DOZ-12.

### 9.3 Sadržaj i stanja izdate dozvole

Dozvola sadrži jedinstveni prikazni broj, identifikator, izdavaoca, ime i prezime nosioca u trenutku izdavanja, potvrđenu fotografiju ili oznaku da nije dostupna, naziv proizvoda, obuhvat i njegovu verziju, početak i kraj, ugovoreni iznos, vrijeme izdavanja, verziju uslova i QR. Datum rođenja se ne prikazuje na javno dostupnoj strani dokumenta; ribočuvaru je dostupan u autentifikovanoj provjeri kada je potreban za identifikaciju.

**DOZ-10** Trajno stanje dozvole je IZDATA ili OPOZVANA. Prikazni status BUDUĆA, VAŽEĆA ili ISTEKLA izvodi se iz vremena za neopozvanu dozvolu. OPOZVANA ima prednost nad vremenskim prikazom. Opoziv sadrži razlog, autora i trenutak dejstva, važi odmah po potvrdi servera i ne može se poništiti vraćanjem statusa.

**DOZ-11** Samo ADM izdavaoca može opozvati dozvolu. Online provjera odmah koristi opoziv. Ribolovac dobija obavještenje u portalu i pokušaj e-pošte. Offline uređaji saznaju za opoziv pri narednoj uspješnoj sinhronizaciji; ekran uvijek pokazuje starost podataka.

**DOZ-12** Ispravka imena, pogrešnog proizvoda ili obuhvata zahtijeva opoziv i novu dozvolu. Kod administrativne zamjene iste plaćene usluge kreira se veza STARA → NOVA, razlog i prenos iste osnove naplate bez novog finansijskog prihoda. Zamjena ne dopušta dva važeća dokumenta niti povećanje trajanja ili obuhvata bez novog zahtjeva i odgovarajuće obaveze.

**DOZ-13** Kopije se preuzimaju iz portala ili štampaju kod operatera i koriste isti broj i QR. Gubitak papira ne stvara novu dozvolu. Otvaranje QR-a bez autentifikacije ne prikazuje ime, fotografiju ili status osobe i ne pokreće proizvoljnu web adresu.

## 10 Evidencija uplata i finansijskih korekcija

### 10.1 Osnovni tok

**UPL-01** Obaveza se odnosi na tačno jednu članarinu ili jedan odobreni zahtjev za dozvolu. Ima udruženje primaoca, osobu, referencu, iznos, BAM, datum nastanka, rok i stanje NEIZMIRENA, IZMIRENA, OSLOBOĐENA ili ZATVORENA. Stanje se ponovo izračunava iz važećih uplata, korekcija, oslobađanja i kontrolisanog zatvaranja; ne uređuje se proizvoljnim unosom. ZATVORENA znači da se naplata više ne očekuje zbog otkazanog ili isteklog zahtjeva ili odobrenog završetka postupka povrata. Zatvorena obaveza se ne prikazuje kao dospjeli dug i ne predstavlja osnov za aktivno članstvo ili novo izdavanje.

**UPL-02** Operater evidentira stvarno primljenu gotovinu ili provjerenu bankovnu uplatu. Zapis sadrži iznos, metod, datum naplate, vrijeme unosa, obavezu, referencu izvoda ili blagajničke potvrde i autora. Korisnikov prilog ili tvrdnja da je platio ne potvrđuju automatski uplatu. Brojevi kartica i podaci za kartično plaćanje se ne prikupljaju.

**UPL-03** Osnovni tok prihvata jednu punu uplatu za jednu obavezu. Manji ili veći iznos ne aktivira članstvo i ne izdaje dozvolu. Operater evidentira neraspoređenu uplatu i razlog odstupanja; ADM razrješava punim povratom ili ispravkom dokazano pogrešnog unosa uz očuvanje originalnog zapisa. Stvarno primljeni iznos se ne mijenja da bi odgovarao obavezi. Djelimične doplate i razbijanje jedne uplate na više obaveza nisu dostupni u ovoj verziji. Preraspodjela pune uplate na drugi zahtjev dozvoljena je samo za istu osobu, udruženje i identičan iznos, bez druge aktivne raspodjele i bez aktivne dozvole starog zahtjeva.

**UPL-04** Referenca bankovne transakcije mora biti jedinstvena unutar računa primaoca. Broj gotovinske potvrde mora biti jedinstven unutar udruženja i poslovne godine. Ponavljanje tehničkog zahtjeva s istim ključem operacije ne kreira novu uplatu. Istovremene potvrde iste obaveze završavaju jednom raspoređenom uplatom.

**UPL-05** Potvrđena uplata se čuva i kada izdavanje kasnije ne uspije. Sistem prikazuje listu plaćenih, a nerealizovanih zahtjeva s razlogom i starošću. Operater može ponoviti izdavanje bez ponovnog knjiženja uplate. Izmirenje i izdavanje ne zavise od uspješnosti e-pošte.

### 10.2 Ispravke i povrati

**UPL-06** Potvrđena uplata se ne mijenja i ne briše. ADM kreira korektivni zapis s referencom na original, razlogom i iznosom. Storno pogrešno unesenog zapisa znači ispravku evidencije i razlikuje se od stvarnog izlaza novca kroz povrat.

**UPL-07** Zahtjev za puni povrat ima stanja ZATRAŽEN, ODOBREN, IZVRŠEN ili ODBIJEN. ADM odobrava i bilježi dokaz/reference stvarno izvršenog povrata. Sistem sam ne prenosi novac. Povrat povezan s dozvolom zahtijeva prethodni ili istovremeni opoziv aktivnog dokumenta u cijelom lancu administrativnih zamjena. Iznos izvršenih povrata ne može premašiti potvrđeni neto iznos konkretne uplate.

Ista uplata ne može biti u cijelosti neutralisana i stornom i povratom. Ove operacije provjeravaju preostali raspoloživi iznos pod istom transakcijskom zaštitom. Storno služi ispravci evidencije; stvarno primljeni pa vraćeni novac evidentira se kao naplata i povrat. Završetak povrata zatvara obavezu kada se usluga otkazuje, bez ponovnog stvaranja duga.

**UPL-08** Prije storniranja naplate na osnovu koje postoji važeća dozvola, dozvola se opoziva ili se kroz audit vezuje zamjenska potvrđena uplata iste obaveze. Ne smije ostati aktivna dozvola bez validne osnove naplate ili odobrenog oslobađanja. Povrat ili storno članarine ponovo obračunava aktivnost članstva i prikazuje povezane dozvole za odvojenu odluku ADM.

**UPL-09** Izvoz razlikuje izdate dozvole, bruto evidentirane naplate, korekcije i stvarno izvršene povrate. Neto naplata u izvještaju je bruto naplata umanjena za storna i izvršene povrate u izabranom periodu događaja. Naziv „računovodstveni prihod“ se ne koristi bez zasebno odobrene računovodstvene metodologije.

## 11 Provjera dozvola i offline rad

### 11.1 Rezultat provjere

**PRO-01** Provjera uvijek ima dva odvojena rezultata: status dokumenta i procjenu važenja za izabrano mjesto i vrijeme. Prikazuje identitet u dozvoljenom obimu, fotografiju ili njeno odsustvo, izdavaoca, proizvod, obuhvat, početak, kraj, opoziv kada je poznat, režim online ili offline i vrijeme podataka.

| Situacija | Obavezni prikaz |
| --- | --- |
| Server potvrđuje aktivnu dozvolu, izabrana lokacija je pokrivena i nema potpune zabrane | „Dozvola važi za izabrano područje i vrijeme“ uz prikaz ostalih pravila |
| Dozvola je aktivna, ali lokacija nije izabrana ili je neodređena | „Dozvola je aktivna; važenje na lokaciji nije potvrđeno“ |
| Dozvola je aktivna, ali izvan obuhvata | „Dozvola ne pokriva izabrano područje“ |
| Dozvola je buduća, istekla ili poznato opozvana | Odgovarajući razlog, relevantni datumi i izvor podatka |
| Potpuna zabrana važi na lokaciji u trenutku kontrole | „Ribolov je zabranjen na ovoj lokaciji“ uz razlog i period zabrane |
| Ispravna offline provjera sa svježim potpunim podacima | „Važi prema podacima od [vrijeme]“; nikada tvrdnja da je provjeren trenutno stanje servera |
| Podaci su stari, nedostaju ili vrijeme nije pouzdano | „Status nije moguće pouzdano potvrditi“ uz konkretan razlog |
| QR sadržaj je izmijenjen ili potpis nije ispravan | „Dokument nije potvrđen“; ne prikazivati podatke iz neprovjerenog sadržaja kao pouzdane |

**PRO-02** Ribočuvar bira područje ili revir, a aplikacija po mogućnosti predlaže lokaciju na osnovu GPS-a. GPS nije obavezan za unos kontrole. Ako je procijenjena tačnost lošija od 50 metara, ako se lokacija preklapa s više granica ili nema pouzdane geometrije, automatska potvrda lokacije izostaje. Ribočuvar može ručno odabrati lokaciju uz oznaku ručnog izbora. Takav izbor ostaje vidljiv u zapisu.

**PRO-03** Boja dopunjuje tekst, ali ga ne zamjenjuje. Provjera ne utvrđuje usklađenost pribora, vrste ribe ili ulova kada ti podaci nisu uneseni; relevantna pravila su dostupna ribočuvaru. Skeniranje samo po sebi još nije sačuvana kontrola.

### 11.2 QR i autentičnost

**OFF-01** QR sadrži verziju formata, tip dokumenta, identifikator dozvole, izdavaoca, identifikator ključa i digitalni potpis. Ne sadrži ime, fotografiju, datum rođenja ili kontakt. Potpis je vezan i za nepromjenjivi sadržaj dozvole preko njegovog sažetka. Podaci dozvole za prikaz dolaze iz ovlaštenog online odgovora ili provjerenog lokalnog paketa.

**OFF-02** Potpisivanje koristi provjerenu kriptografsku biblioteku i ključeve pod kontrolom serverskog servisa. Privatni ključ ne ulazi u mobilnu aplikaciju. Tehnički projekat određuje format, algoritam, rotaciju, distribuciju javnih ključeva i postupak kompromitacije; interoperabilni testovi moraju biti završeni prije terenskog pilota. Nepoznat ključ zahtijeva sinhronizaciju i daje neodređen rezultat, a ne potvrdu falsifikata.

**OFF-03** Kopija QR-a ostaje kopija iste dozvole. Ribočuvar upoređuje nosioca s potvrđenom fotografijom ili dogovorenim identifikacionim postupkom. Sistem ne tvrdi da digitalni potpis sprečava dijeljenje fotografije QR-a. QR članske kartice aplikacija prepoznaje i traži dozvolu.

### 11.3 Registracija uređaja i paket za teren

**OFF-04** ADM odobrava terenski uređaj aktivnom ribočuvaru. Uređaj ima stabilan identifikator, vlasnika, status, posljednju sinhronizaciju i istoriju opoziva. Prva prijava, promjena korisnika i obnova isteklog offline ovlaštenja zahtijevaju internet i drugi faktor prijave.

**OFF-05** Paket sadrži samo ovlaštena područja, geometrije i pravila, poznate zabrane, potrebne javne ključeve i minimum podataka o relevantnim dozvolama. Uključuje dozvole čiji obuhvat siječe dodijeljeno područje, čiji kraj nije stariji od 24 sata i čiji početak nije dalje od sedam dana od vremena paketa. Aktivne godišnje dozvole ulaze po istom pravilu. Uključuju se poznati opozivi za obuhvaćene zapise. Kontakti i uplate se ne preuzimaju.

**OFF-06** Paket ima identifikator, serversko vrijeme, verzije podataka, kontrolu integriteta, vremenski ograničeno ovlaštenje i kursor za narednu sinhronizaciju. Početno preuzimanje može biti segmentirano, ali se paket aktivira tek kada su svi potrebni dijelovi provjereni. Prekinuto preuzimanje ne zamjenjuje posljednji ispravan paket niti pomjera vrijeme svježine.

**OFF-07** Naredna sinhronizacija preuzima izmjene, opozive, izmjene prava i oznake uklanjanja. Svi dijelovi jedne verzije aktiviraju se zajedno. Nepotpuni paket ne može prikazati „nema zabrane“ ili „nije opozvana“ kao konačan zaključak. Server može zahtijevati novu punu sinhronizaciju ako inkrementalni kursor više nije važeći.

### 11.4 Svježina i ograničenja rada

**OFF-08** Dok je proteklo manje od 24 sata od potpune uspješne sinhronizacije dozvoljen je rezultat „važi prema podacima od…“ ako su identitet dokumenta, vrijeme, područje i pravila provjerljivi. Od tačno 24 sata do manje od 72 sata prikazuju se podaci s upozorenjem o zastarjelosti i neodređenom konačnom ocjenom. Poznat opoziv ili istek ostaje eksplicitno vidljiv.

**OFF-09** Na isteku 72 sata offline ovlaštenje prestaje: nema novih provjera ličnih podataka niti kreiranja novih kontrola dok se ne obnovi autorizacija. Ako uloga ili teritorijalno ovlaštenje ističu ranije, koristi se taj raniji rok. Već sačuvani neposlani zapisi ostaju šifrovani i mogu se poslati kroz autentifikovan oporavak. Administrativni opoziv uređaja ili uloge prekida online pristup odmah; na potpuno nepovezanom uređaju ograničenje zavisi od isteka izdatog ovlaštenja.

**OFF-10** Aplikacija prati proteklo vrijeme od pouzdanog serverskog trenutka korištenjem pouzdanog lokalnog mjerača. Pomjeranje sistemskog sata ne produžava svježinu ili ovlaštenje. Ako restart uređaja ili promjena sata onemogući dokazivanje trajanja, pristup lokalnim ličnim podacima i nove kontrole zahtijevaju online obnovu. Ovaj uslov mora biti provjeren na podržanim uređajima.

**OFF-11** Dozvola izdata nakon posljednje sinhronizacije može imati ispravan potpis, ali nedostajati u paketu. Aplikacija prikazuje „dozvola nije u preuzetim podacima“ i omogućava evidentiranje neodređenog nalaza. Nedostajući zapis, oštećen QR i nepoznat ključ nisu isto što i potvrđeno nevažeća dozvola.

**OFF-12** Lokalna baza i fotografije su šifrovane; pristup se otključava PIN-om uređaja ili biometrijom, a aplikacija se zaključava nakon pet minuta neaktivnosti. Keš je isključen iz uobičajenih korisničkih rezervnih kopija i ne izlaže se drugim aplikacijama. Odjava uklanja čitljivi keš; kod neposlatih kontrola upozorava korisnika i zadržava šifrovani red za kontrolisan oporavak. Promjena naloga ne daje pristup redu prethodnog korisnika.

## 12 Kontrole i sinhronizacija terenskih zapisa

### 12.1 Unos kontrole

**KON-01** Ribočuvar nakon provjere bira „Sačuvaj kontrolu“. Obavezni su identifikator kontrole, ribočuvar, uređaj, efektivno ovlaštenje, vrijeme nastanka, područje ili revir, način određivanja lokacije, način provjere i nalaz. Kada postoji dozvola, zapisuje se njena referenca, rezultat provjere i verzija podataka. GPS, tačnost i napomena su opciona polja, osim razloga ručne izmjene kada je ona izvršena.

**KON-02** Nalazi su BEZ_UOČENE_NEPRAVILNOSTI, UOČENA_NEPRAVILNOST i NIJE_MOGUĆE_POTVRDITI. Za nepravilnost se bira kategorija i unosi opis; za neodređen nalaz razlog. Prva verzija ne vodi kazne, pravosnažne prekršaje ni prijave građana. Fotografije dokaza nisu dio osnovne kontrole; eventualno proširenje zahtijeva pravila pristupa i čuvanja.

**KON-03** Ako dozvola nije predočena, kontrola se može sačuvati bez veze na osobu i bez slobodnog unosa nepotrebnih ličnih podataka. Kasnije povezivanje moguće je samo kroz evidentiranu dopunu. Stara papirna dozvola dobija tip STARI_DOKUMENT i nalaz prema dogovorenoj pilot proceduri; odsustvo u sistemu nije automatski dokaz nevaženja.

**KON-04** Skica kontrole može se uređivati dok nije potvrđena. Nakon potvrde nastaje nepromjenjiv događaj. Ispravke se dodaju kao dopuna s autorom, razlogom i vremenom. Ribočuvar dodaje dopunu vlastite kontrole; ADM može dodati administrativnu napomenu i oznaku opravdanog isključenja iz pojedinog izvještaja, bez izmjene izvornog nalaza.

### 12.2 Red slanja i konflikti

**SIN-01** Identifikator kontrole generiše se prije prvog čuvanja i koristi pri svakom ponovnom slanju. Lokalna stanja su NACRT, ČEKA_SLANJE, POSLATO i ZA_PROVJERU. Potvrđena lokalna kontrola mora preživjeti gašenje procesa i gubitak veze. Broj neposlatih zapisa i zadnja uspješna sinhronizacija stalno su vidljivi.

**SIN-02** Server atomarno prihvata kontrolu i vraća potvrdu s identifikatorom i vremenom prijema. Ako odgovor izostane, uređaj šalje isti identifikator; server vraća raniju potvrdu. Isti identifikator s različitim sadržajem predstavlja konflikt, ne novo izdavanje zapisa. Uređaj ne uklanja zapis iz reda prije potvrde servera.

**SIN-03** Provjera uloge uzima u obzir vrijeme nastanka kontrole, potpisano offline ovlaštenje i vrijeme opoziva. Zapis nastao dok je ovlaštenje važilo može se naknadno prihvatiti nakon obnove online prijave. Ako je nalog blokiran, ADM pokreće ograničen postupak oporavka koji dozvoljava samo predaju šifrovanih neposlatih zapisa, bez čitanja drugih podataka ili novih kontrola. Sumnjivo vrijeme ili zapis nakon opoziva odlazi u ZA_PROVJERU; ne odbacuje se nevidljivo i ne ulazi u zvanične agregate prije odluke ADM.

**SIN-04** Čuvaju se lokalno vrijeme događaja, korišteni serverski vremenski oslonac, vrijeme prijema i procijenjena pouzdanost vremena. Nova zabrana ili opoziv dozvole saznati pri sinhronizaciji ne prepisuju ono što je ribočuvar vidio. Server dodaje odvojenu naknadnu ocjenu prema tada važećim podacima i označava razliku za pregled.

**SIN-05** Prekid veze, djelimično slanje i ponovljeno pokretanje aplikacije ne smiju stvoriti dodatnu kontrolu. Neispravni zapisi imaju konkretan razlog i postupak razrješenja. Sinhronizacija prvo obnavlja autorizaciju, zatim šalje postojeće zapise i preuzima nove podatke; slanje starih zapisa ne osvježava automatski lokalni paket dozvola.

## 13 Model podataka i integritet

### 13.1 Glavni entiteti

Model je logički i ne propisuje konkretan programski jezik ili bazu. Svi poslovni zapisi imaju interni identifikator, vrijeme nastanka, autora gdje je primjenjivo i verziju za kontrolu istovremenih izmjena. Istorijski događaji se dopunjavaju novim događajem, a ne prepisuju.

| Entitet | Ključni podaci i veze |
| --- | --- |
| Organizacija | Tip, naziv, šifra, status, kontakt |
| Organizaciona veza | Nadređena i podređena organizacija, početak i kraj važenja |
| Osoba | Identitet, datum rođenja, država prebivališta, kontakt, status provjere, kanonski identitet |
| Provjera identiteta | Osoba, način, operater, vrijeme, potvrđena fotografija kada postoji |
| Korisnički nalog | Osoba kada je primjenjivo, potvrđena adresa, autentifikaciona referenca, status |
| Dodjela uloge | Nalog, uloga, organizacija, početak, kraj i opoziv |
| Članstvo | Osoba, udruženje, kategorija, period, stanje i istorija promjena |
| Cjenovnik članarine | Udruženje, kategorija, verzija, period, iznos, uslovi oslobađanja |
| Voda | Naziv, alternativni nazivi, tip |
| Područje i verzija granice | Šifra, naziv, geometrija, period verzije, povezana voda ili vode |
| Revir i verzija granice | Područje, naziv, geometrija i period verzije |
| Nadležnost nad područjem | Područje, udruženje, početak, kraj i osnova nadležnosti |
| Verzija pravila | Prostor, strukturisana ograničenja, tekstovi i prevodi, period, objava |
| Zabrana | Prostor, period, obim zabrane, razlog i autor |
| Proizvod dozvole i verzija | Izdavalac, namjena, cijena, uslovi, trajanje, period prodaje i obuhvat |
| Zahtjev za dozvolu | Osoba, verzija proizvoda, period, režim početka, cijena, stanje, rok i kanal |
| Finansijska obaveza | Vrsta, članstvo ili zahtjev, primalac, iznos, BAM, rok i izvedeno stanje |
| Uplata | Obaveza kada je raspoređena, metod, iznos, referenca, datum naplate, autor |
| Korekcija i raspodjela | Originalna uplata, tip korekcije, iznos, razlog, obaveza i autor |
| Povrat | Uplata, iznos, stanje, odluka, izvršenje i dokazna referenca |
| Izdata dozvola | Zahtjev, osoba, broj, snapshot identiteta i uslova, verzije obuhvata, period, osnov naplate |
| Opoziv i zamjena | Dozvola, vrijeme, razlog, autor i referenca zamjenske dozvole |
| Ovlaštenje ribočuvara | Osoba ili nalog, organizacija, područja, početak, kraj i opoziv |
| Uređaj | Ribočuvar, identifikator, status, registracija i posljednja veza |
| Offline paket i ovlaštenje | Uređaj, opseg, verzija, kursor, serversko vrijeme, isteci i integritet |
| Kontrola i dopuna | Lokalni ID, ribočuvar, uređaj, ovlaštenje, lokacija, dozvola opciono, vremena, nalaz, verzija provjere |
| Obavještenje | Primalac, poslovni događaj, kanal, pokušaji isporuke, status i vrijeme |
| Audit događaj | Akter, aktivna uloga, organizacija, radnja, predmet, vrijeme, razlog i korelacijska oznaka |
| Uvozni paket i red | Izvor, udruženje, hash datoteke, ključ reda, validacija, odluka i rezultat |

### 13.2 Veze i ograničenja

**PODACI-01** Osoba ima nula ili jedan aktivni korisnički nalog za portal, više članstava, više zahtjeva i više dozvola. Osoblje može imati nalog bez ribolovačkog profila. Članstvo pripada jednom udruženju; dozvola ima jednog izdavaoca i jedan realizovani zahtjev.

**PODACI-02** Jedna obaveza ima najviše jednu aktivnu raspodjelu pune uplate, uz istoriju korekcija. Odnos s dozvolom čuva lanac administrativnih zamjena: jedna obaveza može podržati više istorijskih dokumenata, ali najviše jedan neopozvan dokument iz tog lanca. Povrat se vezuje za stvarnu uplatu, a ne za broj kopija dozvole.

**PODACI-03** Jedinstveni su broj dozvole, referenca zahtjeva, ključ tehničke operacije u svom opsegu, identifikator kontrole i navedene finansijske reference. Ograničenja se sprovode i na nivou trajnog skladištenja, a ne samo provjerom interfejsa. Preklapanje članstava i vremenskih nadležnosti provjerava se unutar transakcije.

**PODACI-04** Izdata dozvola čuva ugovoreni iznos i nepromjenjivi prikaz identiteta, obuhvata i uslova. Aktuelne zabrane i operativna pravila prikazuju se zasebno pri provjeri. Promjena današnjeg cjenovnika, naziva udruženja ili profila ne mijenja dokaz o ranijem izdavanju.

**PODACI-05** Istovremeno uređivanje šifrarnika ili nacrta koristi provjeru verzije. Zastarjela izmjena se odbija uz mogućnost ponovnog učitavanja; nema tihog prepisivanja. Uplate, izdavanje, opozivi, zamjene i prihvat kontrola koriste transakciju i zaštitu od ponavljanja.

**PODACI-06** Povlačenje i arhiviranje referentnih podataka ne brišu povezane istorijske evidencije. Rokovi čuvanja i postupak uklanjanja ličnih podataka određuju se ODL-10. Aplikacija podržava administrativni zahtjev za ograničenje, izvoz ili uklanjanje podataka s odlukom odgovorne osobe; ne briše automatski poslovnu istoriju zbog zahtjeva iz portala.

## 14 Ekrani i granice servisa

### 14.1 Obavezni ekrani

| Interfejs | Ekrani i ključne radnje |
| --- | --- |
| Zajednički | Prijava, drugi faktor, oporavak pristupa, izbor aktivne uloge, obavještenja i odjava |
| SYS | Organizacije, nadležnosti, početni administratori, podrška, tehnički audit i status sinhronizacije |
| ADM i OPE | Početni pregled, članovi, osoba i članstva, zahtjevi, naplata, izdavanje, dozvole i štampa |
| Samo ADM | Katalog, cijene, područja i pravila, osoblje, ribočuvari i uređaji, opozivi, korekcije, povrati i audit |
| RIB | Profil, članstva i kartice, zahtjevi, instrukcije za uplatu, dozvole i preuzimanje |
| Javni portal | Pretraga voda i revira, karta i tekstualna alternativa, pravila, zabrane, cijene i kontakt izdavaoca |
| CUV | Početna provjera spremnosti za teren, skeniranje ili unos broja, rezultat, nova kontrola, vlastite kontrole i red sinhronizacije |
| MIN i SAV | Pregled pokazatelja, filteri, izvještaji, izvoz i oznake svježine i pokrivenosti podataka |

**UI-01** Obavezna polja, validacije i razlozi odbijanja prikazuju se uz relevantnu radnju. Liste imaju pretragu, filtere, stabilno sortiranje i paginaciju. Prazna lista se razlikuje od greške i nedostatka ovlaštenja. Dugotrajna radnja prikazuje stanje i dozvoljava bezbjedno ponavljanje.

**UI-02** Mobilni prikaz dozvole ostaje dostupan ribolovcu iz ranije preuzetog dokumenta bez veze. To je kopija dokumenta s vremenom preuzimanja, bez tvrdnje o trenutnoj provjeri opoziva. Korisnički portal nema širi offline unos zahtjeva ili uplata.

**UI-03** Kritične potvrde navode predmet i posljedicu: opoziv, finansijska korekcija, povrat, zamjena, objavljivanje zabrane i uklanjanje ovlaštenja. Ribočuvar može završiti običnu kontrolu bez nepravilnosti kroz skeniranje, izbor ili potvrdu područja, pregled i jedno čuvanje.

### 14.2 Logički servisi i ugovori

Centralni servis je autoritet za identitet, prava, poslovna stanja i vrijeme. Administrativni portal, korisnički portal i terenska aplikacija koriste iste poslovne provjere. Logičke cjeline su pristup i organizacije, članstvo, područja, dozvole, uplate, kontrole i sinhronizacija, izvještavanje i obavještenja. One mogu biti moduli jedne aplikacije; specifikacija ne zahtijeva zasebne mikroservise.

**API-01** Svaka promjena novca, dozvole ili potvrđene kontrole ima jedinstven ključ operacije. Isti ključ s istim sadržajem vraća prethodni rezultat; isti ključ s drugačijim sadržajem daje konflikt. Ključ, opseg i rezultat čuvaju se najmanje tokom životnog ciklusa poslovnog zapisa; vremenski ograničen HTTP keš nije jedina zaštita od duplikata.

**API-02** Provjera dozvole prima QR ili broj, izabrano područje i vrijeme kontrole; vraća odvojene dimenzije statusa, obuhvata, zabrane, identiteta i svježine. Javno otvaranje QR-a ne koristi ovaj privilegovani odgovor. Ručni unos broja dostupan je samo ovlaštenom korisniku i ograničen je radi sprečavanja masovnog pretraživanja.

**API-03** Sinhronizacija razmjenjuje paket, kursor, ovlaštenje i pojedinačne potvrde prijema kontrola. Protokol mora definisati maksimalnu veličinu segmenta, redoslijed, nastavak prekinutog prenosa i verzije kompatibilnosti. Prije implementacije objavljuju se ugovori API-ja i primjer kompletne online i offline razmjene sa sintetičkim podacima.

**API-04** Greške imaju stabilnu šifru, korisničku poruku i korelacijsku oznaku. Minimalne kategorije su VALIDATION_ERROR, NOT_AUTHORIZED, VERSION_CONFLICT, DUPLICATE_OPERATION, PAYMENT_NOT_SETTLED, MEMBERSHIP_REQUIRED, AREA_CLOSED, REQUEST_EXPIRED, SYNC_REQUIRED i OFFLINE_AUTH_EXPIRED. Greška pristupa tuđem objektu ne otkriva njegove podatke niti potvrđuje identitet osobe.

**API-05** Obavještenja se kreiraju pouzdano iz potvrđenog poslovnog događaja i šalju naknadno. Neuspjela isporuka se ponavlja uz ograničenje pokušaja i prikazuje podršci. U portalu ostaje mjerodavan poslovni status. E-pošta za novu dozvolu sadrži sigurnu vezu ka prijavi, bez nepotrebnih ličnih podataka i bez javnog trajnog linka na PDF.

## 15 Izvještaji i definicije pokazatelja

### 15.1 Obavezni izvještaji

**IZV-01** Operativni izvještaji uključuju članstvo po statusu i periodu, neizmirene članarine, zahtjeve po stanju, izdate i opozvane dozvole, naplate i povrate, plaćene zahtjeve bez dozvole, kontrole po ribočuvaru i području i zastarjelost terenskih uređaja. OPE nema pristup operativnom sadržaju kontrola.

**IZV-02** MIN vidi agregate svih uključenih organizacija; SAV samo svog Saveza. Filteri su period, referentni datum, Savez gdje je primjenjivo, udruženje, područje, namjena i proizvod dozvole. U prvoj verziji nema izvještaja o ulovima, vrstama ribe u ulovu, poribljavanju ili stvarnom broju turističkih posjeta, jer se ti događaji ne prikupljaju.

| Pokazatelj | Precizan obračun |
| --- | --- |
| Registrovani ribolovci na dan | Broj različitih kanonskih osoba koje do referentnog trenutka imaju prihvaćenu ribolovačku evidenciju kroz članstvo ili izdati dokument; nepotvrđeni nalozi i osoblje bez takve evidencije se isključuju |
| Aktivni članovi na dan | Broj različitih osoba s najmanje jednim aktivnim članstvom u izabranom opsegu na referentni trenutak |
| Broj aktivnih članstava | Broj aktivnih odnosa osoba–udruženje; ista osoba u dva udruženja daje dva članstva |
| Aktivni ribolovci na dan | Broj različitih osoba s aktivnom dozvolom na referentni trenutak; zabrana pojedinog revira ne mijenja status osobe po ovoj definiciji |
| Izdate dozvole u periodu | Broj izdavanja s vremenom izdavanja u periodu, uz posebno iskazane administrativne zamjene i opozive; kopije se ne broje |
| Nove prodate dozvole | Broj prvih izdavanja na izmirenim obavezama u periodu; zamjene i besplatna izdavanja prikazuju se odvojeno |
| Turističke dozvole | Broj dozvola proizvoda namjene TURISTIČKA po istoj metodologiji izdavanja |
| Korisnici turističkih dozvola | Broj različitih kanonskih osoba s takvom dozvolom u izabranom periodu izdavanja; naziv ne tvrdi broj dolazaka ili noćenja |
| Prebivalište korisnika | Raspodjela po državi prebivališta sačuvanoj pri izdavanju; nepoznato je posebna kategorija i ne označava državljanstvo |
| Bruto naplate i neto naplate | Obračun po finansijskim događajima iz UPL-09, odvojeno za članarine i dozvole i za gotovinu i bankovni prenos |
| Broj kontrola | Broj različitih prihvaćenih kontrola prema vremenu nastanka; ponovljeno slanje ne povećava broj |
| Kontrole s nepravilnostima | Broj kontrola s nalazom UOČENA_NEPRAVILNOST; ne predstavlja broj pravosnažnih prekršaja |
| Nepouzdane ili sporne kontrole | Broj zapisa ZA_PROVJERU; prikazuje se zasebno, izvan prihvaćenih kontrola |
| Pokrivenost podataka | Uključena udruženja, početak evidentiranja i vrijeme posljednjeg prijema podataka; broj ne predstavlja automatski cjelokupno stanje Republike Srpske |

**IZV-03** Ukupan broj jedinstvenih osoba računa se iz unije identiteta, ne sabiranjem broja članova udruženja. Dozvola koja obuhvata više područja može biti prisutna u više filtriranih prikaza, ali ukupan broj dozvola i ukupna naplata deduplikuju identifikatore. Finansijski izvještaj po području mora označiti da nije raspodjela prihoda na područja; ne dijeli iznos proizvoljno.

**IZV-04** Izvještaji razlikuju stanje na datum i promet tokom perioda. Kontrole pristigle naknadno mijenjaju istorijski period prema vremenu nastanka, uz oznaku osvježavanja. Finansijski događaj prikazuje poslovni datum naplate ili povrata i vrijeme kada je unesen. Sačuvani izvoz sadrži filtere, metodologiju verzije, vrijeme generisanja i upozorenje na nepotpune offline podatke.

**IZV-05** Pregled i CSV izvoz poštuju istu autorizaciju. Agregati nemaju mogućnost izvoza imena kroz skrivene kolone. Izvoz sprečava izvršavanje sadržaja ćelija kao formula. Prva verzija nema proizvoljni konstruktor izvještaja; dostupni su unaprijed definisani prikazi.

Za promet po državi koristi se vrijednost sačuvana pri događaju izdavanja. Za izvještaj stanja osobe na datum koristi se istorijski važeća vrijednost profila, a ako je nema, prikazuje se nepoznato. Zbir razvrstanih i nepoznatih vrijednosti mora odgovarati ukupnom broju u istom opsegu. Skup „registrovanih ribolovaca“ iz tabele pripisuje se Savezu prema vezi relevantnog udruženja u referentnom trenutku i deduplikuje unutar tog opsega.

## 16 Nefunkcionalni zahtjevi

Brojčane vrijednosti su predloženi ciljevi za ODL-11. Testovi koriste sintetičke podatke, dogovorenu infrastrukturu i dokumentovane uređaje. Nisu tvrdnja o postojećim kapacitetima.

### 16.1 Opterećenje i brzina

**NFR-01** Referentni skup sadrži 100 udruženja, 100.000 osoba, 200.000 dozvola, 1.000.000 kontrola i najmanje tri poslovne godine istorije. Cilj je 200 istovremenih online sesija, 20 poslovnih zahtjeva u sekundi u mješavini čitanja i upisa i nalet od 100 terenskih sinhronizacija u deset minuta. Konkretna mješavina operacija i hardver ulaze u izvještaj testa.

**NFR-02** Za obične preglede i online provjeru ciljani 95. percentil odgovora servera je najviše dvije sekunde, a za potvrdu uplate i izdavanje tri sekunde, pri referentnom opterećenju. Mjeri se odvojeno vrijeme servera i krajnje korisničko vrijeme; e-pošta nije dio sinhronog zahtjeva. Agregirani prikaz za jednu godinu vraća se do deset sekundi; veći izvoz ide u pozadinski zadatak.

**NFR-03** Offline provjera preuzetog QR-a s do 20.000 lokalnih dozvola završava do dvije sekunde na dogovorenom pilot uređaju. Test uključuje hladno otvaranje aplikacije, zaključavanje, obnovu i slabu bateriju. Paket od 5.000 dozvola s optimizovanim fotografijama cilja najviše 150 MB, a početna sinhronizacija završava do pet minuta na stabilnoj vezi od 10 Mb/s. Prikazuje se veličina prije preuzimanja.

### 16.2 Pouzdanost i oporavak

**NFR-04** Predloženi cilj dostupnosti produkcionog servisa je 99,5% mjesečno, računajući i planirane prekide koji se dodatno iskazuju zasebno. Dostupnost se mjeri spoljašnjom provjerom prijave, čitanja i provjere dozvole; nedostupnost korisnikove mreže nije serverski prekid. Cilj gubitka serverskih podataka pri oporavku je najviše jedan sat, a cilj obnove usluge četiri sata. Ovi ciljevi ne štite neposlate zapise na izgubljenom terenskom uređaju; interfejs zato jasno prikazuje red neposlatih kontrola.

**NFR-05** Sigurnosne kopije su šifrovane i odvojene od primarne instance. Predloženo čuvanje operativnih kopija je 30 dana, uz potvrdu ODL-10. Prije pilota izvodi se dokumentovan oporavak podataka i provjera referenci, uplata i dozvola. Kopija bez testa vraćanja ne zadovoljava prihvatanje.

**NFR-06** Prate se greške prijave, neuspjela izdavanja, plaćeni zahtjevi bez dozvole, redovi sinhronizacije, greške obavještenja, zauzeće skladišta i stanje sigurnosnih kopija. Alarm ima odgovornu osobu i postupak. Logovi koriste korelacijske oznake i ne sadrže lozinke, tokene, QR sadržaj, fotografije ili puni lični profil.

### 16.3 Zaštita i odgovornost za podatke

**NFR-07** Saobraćaj je šifrovan; servisne tajne i privatni ključevi nisu u izvornom kodu ili klijentskoj aplikaciji. Serverski pristup dokumentima i fotografijama uvijek provjerava ovlaštenje, uključujući privremene veze za preuzimanje. Administrativna i terenska prijava koriste drugi faktor; oporavak pristupa bilježi se i opoziva stare sesije.

**NFR-08** Online sesija administracije zaključava se nakon 30 minuta neaktivnosti. Ponovna provjera autentifikacije traži se prije promjene privilegija i pristupa podrške. Ograničenje pokušaja prijave i pretrage sprečava pogađanje lozinki i masovno ispitivanje brojeva dozvola, uz postupak odblokiranja legitimnog korisnika.

**NFR-09** Audit obuhvata prijave i dodjelu prava, pristup podrške, promjene identiteta i članstva, cijene i pravila, uplate, korekcije, izdavanja, opozive, zamjene, kontrole, izvoze i sinhronizacije. Audit nije promjenjiv kroz poslovni interfejs; pristup je ograničen. Ne kopira nepotrebno pune osjetljive vrijednosti kada su dovoljni identifikatori, promijenjena polja i razlog.

**NFR-10** Prije stvarnih podataka potvrđuju se rokovi čuvanja za profil, fotografije, članstva, dozvole, finansijske zapise, kontrole, audit i rezervne kopije. Korisniku se objavljuju objašnjenja obrade i kanal za zahtjeve. Fotografija se ne koristi za automatsko biometrijsko prepoznavanje. Maloljetnički tok nije aktiviran u predloženom pilotu.

### 16.4 Upotrebljivost i kompatibilnost

**NFR-11** Web portal radi na desktopu i mobilnom prikazu širine od 360 CSS piksela, s tastaturom, jasnim oznakama polja, čitljivim kontrastom i tekstualnim objašnjenjem statusa. Karta ima tekstualnu alternativu. PDF i štampa imaju čitljiv QR i identične poslovne podatke.

**NFR-12** Prije terenskog razvoja potvrđuje se lista konkretnih verzija operativnih sistema, uređaja i preglednika. Minimalno se testiraju dvije različite klase Android uređaja za ribočuvara i desktop i mobilni preglednik za portal. Podrška ribočuvaru na iOS-u nije podrazumijevana; zahtjev za nju mijenja listu testova i procjenu.

**NFR-13** Osnovni interfejs i pretraga podržavaju slova č, ć, š, đ i ž, kao i unos imena na drugim pismima. Engleski prikaz ne mijenja značenje odobrenih pravila. Iznosi, datumi i vremenska zona prikazuju se dosljedno; dokument navodi vremensku zonu važenja dozvole.

**NFR-14** Operativni prikaz odmah nakon potvrđene radnje pokazuje potvrđeni rezultat. Agregati se osvježavaju najkasnije pet minuta nakon potvrde servera, uz prikaz vremena osvježavanja. Za offline kontrole rok počinje potvrdom prijema na serveru, a ne vremenom nastanka na uređaju. Nije dopušteno prikazati zastario agregat kao trenutno stanje bez oznake vremena.

## 17 Uvoz podataka i operativno uvođenje

### 17.1 Uvoz početnih evidencija

**UVO-01** Osnovni uvoz prihvata dogovoreni UTF-8 CSV obrazac za osobe i članstva. Obavezne kolone su izvorni ključ reda, udruženje, ime, prezime, datum rođenja, period i kategorija članstva. Kontakt i država prebivališta mogu nedostajati u istorijskim podacima, uz vidljivu oznaku kvaliteta. Nepotvrđeni identitet ne postaje potvrđen samo zato što je uvezen.

**UVO-02** Uvoz prvo radi probnu validaciju bez poslovnog upisa. Izvještaj sadrži ispravne redove, greške, moguće duplikate, preklapanja i nepoznate šifre. Nema tihog preskakanja redova ili automatskog spajanja osoba. ADM potvrđuje konkretan očišćen paket; neispravni redovi se ispravljaju prije potvrde tog paketa.

**UVO-03** Hash datoteke i izvorni ključ sprečavaju ponovni uvoz istog paketa i reda. Potvrđeni paket ima izvještaj rezultata i audit. Poništavanje paketa dopušteno je samo dok zapisi nisu korišteni u izdavanju ili drugim procesima; kasnije se rade pojedinačne kontrolisane korekcije.

Probna validacija vezana je za tačan sadržaj datoteke i verziju relevantnih šifrarnika. Njihova promjena zahtijeva novu validaciju. Potvrda paketa upisuje sve odobrene redove atomarno ili ne upisuje nijedan; veliki uvoz se unaprijed dijeli na jasno označene zasebne pakete. Izvještaj ne prikazuje uspješan paket ako se transakcija nije završila.

**UVO-04** Ranije izmirena članarina uvozi se kao označeno početno stanje, uz period, iznos i referencu izvora, a ne kao nova današnja gotovinska naplata. Uvezene potvrde zahtijevaju odgovorno odobrenje ADM. Uvoz starih dozvola, uplata i kontrola zahtijeva poseban dodatak specifikaciji i nije uključen u osnovni CSV tok.

### 17.2 Priprema i podrška

**OPS-01** Prije pilota moraju postojati odobreni korisnici i uloge, granice područja, uslovi i cijene dozvola, pravila i prevodi, instrukcije za uplatu, dogovorena procedura identifikacije i tretmana starih dokumenata. Nijedno prazno obavezno pravilo ne zamjenjuje se proizvoljnom podrazumijevanom vrijednošću.

**OPS-02** Pilot uključuje obuku ADM, OPE i CUV korisnika, izdavanje uz pomoć operatera, samostalni zahtjev ribolovca, štampanu dozvolu, rad bez interneta i povratak u mrežu. Korisničke upute objašnjavaju razliku između važenja dozvole, teritorijalnog obuhvata i svježine provjere.

**OPS-03** Podrška ima imenovane kontakte, radno vrijeme, način prijave greške i eskalaciju. Kritični slučajevi su gubitak ili izlaganje podataka, duple finansijske evidencije, pogrešno izdavanje i nemogućnost terenske provjere na većem broju uređaja. Dogovaraju se rokovi reakcije i operativni postupak tokom nedostupnosti.

**OPS-04** Tokom prekida servera postojeće preuzete dozvole ostaju dostupne, a ribočuvar koristi dozvoljeni offline režim. Novo izdavanje čeka povratak servera. Ako organizacija želi papirno rezervno izdavanje, mora prvo usaglasiti numeraciju, kontrolu i naknadni unos kroz izmjenu ODL-13; takav proces se ne improvizuje na dan prekida.

## 18 Kriteriji prihvatanja

Svaki scenario izvodi se s poznatim početnim podacima, očekivanim rezultatom i sačuvanim dokazom. Za provjere pristupa koriste se najmanje dva udruženja. „Prolazi“ znači da su ispravni korisnički prikaz, trajni podaci i audit gdje je propisan. Sljedeći scenariji su minimalni skup; validacije pojedinačnih obaveznih polja ostaju dio funkcionalnog testiranja.

| ID | Zahtjevi | Scenario i očekivani rezultat |
| --- | --- | --- |
| KP-01 | IAM-01, IZV-05 | OPE udruženja A direktno zatraži osobu, PDF, fotografiju i izvoz udruženja B. Nema podataka ni zaobilaženja kroz pretragu ili broj rezultata. |
| KP-02 | IAM-02 | Korisnik s ulogama u dva udruženja promijeni kontekst. Stari podaci se uklanjaju i naredna operacija koristi samo novi kontekst. |
| KP-03 | IAM-04, NFR-09 | SYS bez odobrenja ne čita profil. Privremeni odobreni pristup radi samo u opsegu i vremenu i ostavlja audit; ne izdaje dozvolu. |
| KP-04 | OSO-02, OSO-03, OSO-07 | Osoba bez naloga dobije članstvo i štampanu dozvolu; kasnije se poveže s potvrđenim nalogom bez nove osobe. Potvrda e-pošte sama ne potvrđuje identitet. |
| KP-05 | OSO-05, OSO-06, OSO-08 | Dvije osobe istog imena ne spajaju se automatski. Razriješeni pravi duplikat čuva istoriju i u zbiru se broji jednom. |
| KP-06 | CLA-01, PODACI-03 | Dva paralelna odobravanja preklapajućeg članstva u istom udruženju daju jedno prihvaćeno članstvo. Članstvo iste osobe u drugom udruženju je dozvoljeno. |
| KP-07 | CLA-02, CLA-03, CLA-04 | Odobreno članstvo bez plaćene članarine nije aktivno. Potvrđena puna uplata ga aktivira za važeći period; nova sezona ne mijenja prethodnu. Kartica se ne prikazuje kao dozvola. |
| KP-08 | POD-01, POD-02, POD-03 | Jedna rijeka ima revire dva udruženja. Portal prikazuje odgovarajućeg upravljača, a tuđi ADM ne mijenja granicu niti dodaje sebi nadležnost. |
| KP-09 | POD-04, POD-05 | Konfliktna pravila blokiraju objavljivanje. Objavljena nova verzija čuva prethodnu i prikazuje tačan period dejstva. |
| KP-10 | DOZ-01, DOZ-04, DOZ-06 | Potvrđena osoba bez članstva podnosi zahtjev za proizvod dozvoljen nečlanovima. Za proizvod koji traži članstvo dobija precizan razlog odbijanja. |
| KP-11 | DOZ-02 | Dnevna i višednevna dozvola ispravno koriste lokalne ponoći, uključujući promjenu računanja vremena. Dozvola za tekući dan ne važi prije izdavanja. |
| KP-12 | DOZ-03, CLA-03 | Izmjena cjenovnika ne mijenja ranije odobren iznos ni ranije izdatu dozvolu. Novi zahtjev koristi novu verziju. |
| KP-13 | UPL-02, UPL-03 | Prilog korisnika ne potvrđuje uplatu. Manja ili veća uplata ostaje neraspoređena i ne izdaje dozvolu; razrješenje čuva original. |
| KP-14 | UPL-04, DOZ-08, API-01 | Dva paralelna potvrđivanja i ponavljanje poslije izgubljenog odgovora proizvode jednu uplatu i jednu dozvolu. Isti ključ s drugim sadržajem daje konflikt. |
| KP-15 | DOZ-07, UPL-05 | Nakon potvrđene uplate objavi se zabrana prije izdavanja. Nema dozvole, uplata je sačuvana i zahtjev je vidljiv ADM kao uplaćen bez izdavanja. |
| KP-16 | DOZ-10, DOZ-11 | ADM opozove dozvolu; naredna online provjera prikazuje opoziv. OPE i ribolovac ne mogu opozvati dozvolu niti vratiti opozvanu u aktivno stanje. |
| KP-17 | DOZ-12, PODACI-02 | Zamjena pogrešno upisanog imena opoziva stari dokument, izdaje novi i vezuje isti osnov naplate. Nema duplog prihoda ni dvije aktivne dozvole. |
| KP-18 | UPL-06, UPL-07, UPL-08 | Evidentira se puni povrat uz opoziv, razlog i dokaznu referencu. Ponovljeni povrat, povrat veći od preostalog iznosa i dodatni storno već vraćene naplate se odbijaju. Storno i stvarni povrat imaju različite izvještajne stavke. |
| KP-19 | PRO-01, PRO-02, POD-06 | Aktivna dozvola izvan svog područja ili na potpuno zabranjenoj lokaciji ne daje pozitivan zaključak za ribolov. Neodređen GPS zahtijeva ručni izbor ili neodređen rezultat. |
| KP-20 | OFF-01, OFF-02, DOZ-13 | Izmijenjeni QR ne prolazi provjeru potpisa. Neprijavljeni čitač ne dobija lične podatke. Nepoznat ključ prikazuje potrebu za sinhronizacijom. |
| KP-21 | OFF-03, CLA-04 | Skenirana članska kartica ne prolazi kao dozvola. Kopija QR-a drugog ribolovca i dalje prikazuje izvornog nosioca za provjeru identiteta. |
| KP-22 | OFF-05, OFF-06, OFF-07 | Prekinuto preuzimanje novog paketa ostavlja prethodni paket i njegovo vrijeme. Tuđe nepokrivene dozvole i kontakti nisu u lokalnoj bazi. |
| KP-23 | OFF-08, OFF-09 | Na 23 sata paket omogućava uslovnu offline potvrdu; nakon 24 sata rezultat je zastario; nakon 72 sata prestaju nove provjere i kontrole, a neposlani zapisi ostaju sačuvani. |
| KP-24 | OFF-10 | Pomjeranje sata unazad i restart uređaja ne produžavaju offline ovlaštenje. Bez pouzdanog vremenskog oslonca potrebna je online obnova. |
| KP-25 | OFF-11 | Nova ispravna dozvola koja nije u paketu daje „nije u preuzetim podacima“, bez automatskog označavanja nosioca kao prekršioca. |
| KP-26 | KON-01, KON-03 | Kontrola bez GPS-a, bez predočene dozvole i kontrola starog dokumenta mogu se sačuvati s odgovarajućim razlogom i bez izmišljene osobe ili koordinata. |
| KP-27 | SIN-01, SIN-02, SIN-05 | Deset kontrola sačuva se u režimu rada bez veze, aplikacija se ugasi i ponovo pokrene. Ponovljena slanja nakon obnove veze daju tačno deset serverskih kontrola. |
| KP-28 | SIN-03, IAM-05 | Uloga se opozove tokom rada bez veze. Ranije legitimna kontrola ima put oporavka; kontrola poslije opoziva ide na pregled, a uređaj ne dobija novi paket. |
| KP-29 | SIN-04, KON-04 | Naknadno poznat opoziv dodaje novu ocjenu bez prepisivanja originalne provjere. Ispravka potvrđene kontrole čuva izvorni nalaz i autora. |
| KP-30 | IZV-01, IZV-03 | Osoba s članstvom u dva udruženja daje dva članstva i jednu jedinstvenu osobu u ukupnom opsegu. Dozvola s dva područja ne udvostručuje ukupnu naplatu. |
| KP-31 | IZV-02, IZV-04, NFR-14 | Turističke dozvole i jedinstveni korisnici se razlikuju; zakašnjela kontrola mijenja period nastanka uz novo vrijeme osvježavanja najkasnije pet minuta od serverske potvrde. Izvoz sadrži filtere i pokrivenost. |
| KP-32 | UVO-01, UVO-02, UVO-03 | Neispravan CSV daje greške bez poslovnog upisa; potvrđeni očišćeni paket se uvozi jednom i ponovljeni uvoz ne duplira članove. |
| KP-33 | UVO-04, UPL-09 | Uvezena prethodno izmirena članarina prikazana je kao početno stanje, bez lažne nove gotovinske naplate. |
| KP-34 | API-05, DOZ-08 | Nedostupna e-pošta ne poništava dozvolu i ne kreira novu. Portal prikazuje izdat dokument, a obavještenje se ponavlja zasebno. |
| KP-35 | PODACI-05 | Dva korisnika uređuju istu verziju nacrta pravila. Drugi dobija konflikt bez tihog gubitka prve izmjene. |
| KP-36 | NFR-01, NFR-02, NFR-03 | Izvedu se dogovoreni testovi opterećenja i offline brzine; izmjereni percentili, veličine i uslovi zadovoljavaju potvrđene ciljeve. |
| KP-37 | NFR-04, NFR-05 | Obnovi se zasebno testno okruženje iz kopija i provjere uplate, dozvole, kontrole i audit unutar potvrđenih ciljeva gubitka i oporavka. |
| KP-38 | NFR-07, NFR-09, OFF-12 | Provjere se šifrovanje lokalnih podataka, zaključavanje, opoziv sesija, odsustvo tajni u logovima i izolacija podataka pri promjeni naloga. |
| KP-39 | NFR-11, NFR-12, NFR-13 | Ključni tokovi prolaze na dogovorenim uređajima, tastaturom i na mobilnoj širini; QR se čita s ekrana i sa štampe, a dijakritici i engleski prikaz su ispravni. |
| KP-40 | ORG-03, ORG-04 | Arhiviranje sprečava novo izdavanje, ali stare dozvole ostaju provjerljive. Promjena Saveza ne prepisuje istorijski promet prethodnog Saveza. |
| KP-41 | DOZ-05, DOZ-06 | Neplaćen zahtjev nakon roka prelazi u istekao; zakašnjela stvarna uplata se zadržava za razrješenje i ne pokreće automatsko retroaktivno izdavanje. |
| KP-42 | CLA-05, ODL-05 | Suspendovano članstvo mijenja karticu i aktivne članove; povezane dozvole ostaju zasebno vidljive za odluku, bez skrivenog automatskog opoziva. |
| KP-43 | DOZ-03, DOZ-06, UPL-01 | ADM objavi proizvod s cijenom nula i osnovom oslobađanja. Provjerenoj podobnoj osobi izdaje se dozvola bez lažne uplate; OPE ne može proizvoljno spustiti cijenu drugog proizvoda. |
| KP-44 | DOZ-05, DOZ-07, UPL-03 | Plaćeni zahtjev bez dozvole razriješi se dopuštenom preraspodjelom ili punim povratom. Prvobitni zahtjev i obaveza se zatvaraju i ne ostaju kao dug; preraspodjela ne stvara novi prihod. |

## 19 Redoslijed implementacije i sljedivost

### 19.1 Razvojne cjeline

| Korak | Cjelina | Uslov završetka |
| --- | --- | --- |
| 1 | Potvrda obuhvata i rizičnih pretpostavki | ODL-01 do ODL-07 razriješene za razvoj; definisan pilot i nadležni vlasnici preostalih odluka |
| 2 | Organizacije, identitet, uloge i audit | Izolacija podataka dokazana; član bez naloga i povezivanje naloga prolaze |
| 3 | Članstvo, područja, pravila i katalog | Uvozni obrazac, vremenska pravila i verzionisanje prolaze prihvatanje |
| 4 | Uplate, izdavanje, opoziv i zamjena | Cjelovit operaterski tok radi; paralelna i ponovljena obrada ne stvaraju duplikate |
| 5 | Terenski prototip i konačni offline tok | QR, ograničenja paketa i vremena, restart, red slanja i opoziv ovlaštenja provjereni na uređajima |
| 6 | Korisnički portal i izvještaji | Samostalni zahtjev, preuzimanje, engleski tok i precizni agregati prolaze |
| 7 | Operativni pilot i primopredaja | Prihvaćeni funkcionalni i nefunkcionalni testovi, oporavak, obuka i podrška |

Terenski prototip iz koraka 5 započinje čim postoje sintetičke dozvole i model prava iz koraka 2; ne čeka kompletan portal. Time se rano provjeravaju kamera, lokalno skladištenje, vrijeme i ponašanje nakon prekida. Navedeni koraci predstavljaju zavisnosti, ne procjenu broja sedmica.

### 19.2 Veza s izvornim konceptom

| Poglavlja izvornog koncepta | Razrada u ovoj verziji | Ograničenje |
| --- | --- | --- |
| 2–4 i 11–13 | Organizacije, prava, jedinstveni identitet i agregati | Viši nivoi imaju predloženi agregirani pristup |
| 5 i 14 | Članstvo, članarina, dozvole, područja i ribočuvari | Bez kompletnog knjigovodstva |
| 6 i 15 | Profil, članstva, kartice i preuzimanje dozvola | Dnevnik ulova i objave odloženi |
| 7 | Online i offline provjera i kontrole | Rad zavisi od prethodne sinhronizacije i ograničenog offline ovlaštenja |
| 8 i 16 | Katalog voda, pravila i osnovni turistički zahtjev | Bez kartične naplate, vodiča i rezervacija |
| 9 | Nije uključen | Društvena mreža je zasebna faza |
| 10 | Evidencija nalaza ribočuvara | Prijave građana, poribljavanje, ulovi i prekršajni postupci odloženi |
| 1, 17 i 18 | Poslovni cilj i povezivanje nivoa | Ne predstavljaju dodatne neograničene funkcionalnosti prve verzije |

## 20 Uslovi spremnosti za pilot i prihvatanje verzije

Prije početka pilota vlasnik proizvoda potvrđuje odobrenu verziju specifikacije, sve odluke koje utiču na stvarne korisnike i podatke i listu podržanih uređaja. Pilot udruženja potvrđuju kvalitet uvoza, cijene, obrasce dozvola, granice i pravila, a ribočuvarska služba potvrđuje postupanje s neodređenim offline rezultatima i starim dokumentima.

Verzija se prihvata kada svi primjenjivi scenariji KP-01 do KP-44 prolaze, nema otvorenih grešaka koje izlažu tuđe podatke, mijenjaju finansijski rezultat, pogrešno potvrđuju dozvolu ili gube terensku kontrolu, i kada su uspješno izvedeni oporavak, obuka i probni rad najmanje dva udruženja. Manji nedostaci mogu se prihvatiti samo kroz imenovanu odluku, opis posljedice i rok ispravke.

Primopredaja uključuje izvorni kod i uputstvo za izgradnju, migracije podataka, ugovore API-ja, konfiguraciju okruženja bez tajni, korisničke i operativne upute, proceduru upravljanja ključevima, izvještaje testova, rezultat uvoza i postupak oporavka. Konkretni nosioci razvoja, hostinga, održavanja i poslovne administracije unose se u plan pilota prije produkcionog puštanja.

