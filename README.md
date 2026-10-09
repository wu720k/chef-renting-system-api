# Chef_Renting_System_API

Spring Boot REST API séfbérlések kezelésére (listázás, lekérdezés, létrehozás, törlés). A későbbi Angular frontend (`Chef_Renting_System_Web`) ezt a szolgáltatást fogja hívni.

## Technológiák

- Java 17+ (a fejlesztés JDK 21-gyel történt), Maven 3.8+
- Spring Boot 3.5 (Spring Web, Spring Data JPA, Bean Validation)
- H2 memóriabeli adatbázis: nincs külső adatbázis, nincs Docker, semmit nem kell telepíteni a JDK-n és a Mavenen kívül.

## Mappaszerkezet

    Chef_Renting_System_API/
      pom.xml
      src/main/resources/
        application.properties             port, H2, CORS, seed fájl neve
        data/chef_berlesek_api.csv         az API induló adatai (a konzolos CSV átalakított másolata)
      src/main/java/hu/chefrenting/api/
        ChefRentingApiApplication.java     indítás
        entity/Berles.java                 JPA entitás (totalPrice @Transient)
        repository/BerlesRepository.java   Spring Data JPA, az átfedés-vizsgálat lekérdezése
        service/BerlesService.java         üzleti szabályok
        controller/BerlesController.java   REST végpontok
        dto/BerlesRequest.java             a POST kérés törzse (id és totalPrice nélkül)
        dto/LenientDateDeserializer.java   dátum (yyyy-MM-dd) vagy dátum-idő bemenet
        exception/                         hibák és egységes JSON hibaválasz
        config/CorsConfig.java             http://localhost:4200 engedélyezése
        seed/SeedDataLoader.java           CSV betöltése induláskor
      src/test/java/...                    tesztek (31 db)

## Végpontok

| Metódus | Útvonal | Leírás | Válasz |
|---|---|---|---|
| GET | `/api/berlesek` | az összes bérlés | 200 |
| GET | `/api/berlesek/{id}` | egy bérlés | 200, ismeretlen id esetén 404 |
| POST | `/api/berlesek` | új bérlés | 201 + `Location` fejléc + a létrehozott bérlés; szabálysértésnél 400 |
| DELETE | `/api/berlesek/{id}` | bérlés törlése | 204, ismeretlen id esetén 404 |

## Bérlés JSON

```json
{
  "id": 301,
  "uid": 5,
  "chefId": 101,
  "startDate": "2026-10-15",
  "endDate": "2026-10-19",
  "dailyRate": 320,
  "baseFee": 60,
  "totalPrice": 1660
}
```

| Mező | Típus | Leírás |
|---|---|---|
| id | int | azonosító (adatbázis generálja) |
| uid | int | a bérlő azonosítója |
| chefId | int | a bérelt séf azonosítója |
| startDate | dátum | a bérlés első napja |
| endDate | dátum | a bérlés utolsó napja |
| dailyRate | int | napi díj euróban |
| baseFee | int | alapdíj euróban |
| totalPrice | int | **számított**, nincs tárolva |

A dátumok `yyyy-MM-dd` formában jelennek meg. Bemenetként a `2026-10-15T00:00:00` alakú dátum-idő is elfogadott, ilyenkor csak a dátumrész számít (a bérlés egész napokban számolódik).

A POST törzsében nem kell (és nem is érdemes) `id`-t vagy `totalPrice`-t küldeni: ezeket a szerver figyelmen kívül hagyja. Az `id`-t az adatbázis generálja, a `totalPrice`-t a szerver számolja.

## totalPrice számítása

    napok száma = (endDate - startDate) + 1        (a kezdő- és a záró nap is beleszámít)
    totalPrice  = baseFee + napok száma × dailyRate

Értelmezési döntés: a feladat nem adott másik képletet, ezért az alapdíj egyszer kerül hozzáadásra a napi díjak összegéhez. Példa: 5 nap, napi díj 320, alapdíj 60: 60 + 5 × 320 = 1660. A `totalPrice` a `Berles` entitás `@Transient` getterében számolódik, ezért nincs adatbázisoszlopa, de a JSON-ban szerepel.

## A POST üzleti szabályai

Hibánál 400 Bad Request és magyar nyelvű üzenet jön. A szabályok csak az új (POST) bérlésekre vonatkoznak, az induló adatokra nem.

1. **Kezdő dátum:** legkorábban holnap (a mai és a múltbeli kezdés érvénytelen).
2. **Minimális hossz:** legalább 3 nap (kezdő- és záró nap is számít, pl. hétfő-szerda = 3 nap).
3. **Maximális hossz:** legfeljebb 14 nap (ugyanígy számolva).
4. **Séf foglaltsága:** ugyanannak a `chefId`-nak nem lehet átfedő bérlése. Átfedés: `újKezdet <= létezőVég && újVég >= létezőKezdet`, a határnapok is számítanak, tehát ha egy bérlés egy napon ér véget, ugyanazon a napon nem kezdődhet új. Más séffel nincs ütközés. A vizsgálatot az adatbázis végzi (`existsBy...` lekérdezés), nem Java-ciklus.

Ezen felül hibás kérés (400): hiányzó mező, érvénytelen dátum vagy JSON, `endDate < startDate`, nem pozitív `dailyRate`, negatív `baseFee`.

Hibaválasz (minden hibánál ugyanez a szerkezet):

```json
{
  "timestamp": "2026-10-05T19:43:46.0593827+02:00",
  "status": 400,
  "error": "Bad Request",
  "message": "A séf (chefId: 101) a megadott időszakban már foglalt: a bérlések nem fedhetik át egymást.",
  "path": "/api/berlesek"
}
```

## Az induló CSV

`src/main/resources/data/chef_berlesek_api.csv` (UTF-8, `;` elválasztó), fejléc: `id;uid;chefId;startDate;endDate;dailyRate;baseFee`. A `totalPrice` nincs benne.

- A `FORRAS_26_05_22/chef_berlesek_2025.csv` alapján készült: 300 bérlés, azonos uid, chefId, dátumok és napi díj (a nem létező 2025-02-29 dátum 2025-02-28-ra javítva); az `id` 1-300 (kezdő dátum, majd uid szerint rendezve), a `baseFee` 40 és 120 EUR közötti érték (40 + (uid × 37 mod 17) × 5).
- Az `id` a CSV-ből kerül az adatbázisba, utána az azonosítószámláló a legnagyobb id mögé áll, így az új bérlések id-ja 301-től indul.
- Az adatbázis memóriabeli, ezért minden indításkor újra a CSV tartalmával indul; a POST/DELETE módosítások újraindításkor elvesznek.
- A betöltés az indulás részeként automatikus (`SeedDataLoader`), csak akkor fut, ha a tábla üres.

## Megnyitás IntelliJ IDEA-ban

1. File > Open, válassza ki a `Chef_Renting_System_API` mappát (amelyben a `pom.xml` van), majd Trust Project. A Maven automatikusan importál.
2. File > Project Structure > Project SDK: JDK 17 vagy újabb.
3. Futtatás: nyissa meg a `ChefRentingApiApplication.java` fájlt, és kattintson a `main` melletti zöld nyílra > Run. A szerver a http://localhost:8080 címen indul.
4. Tesztek: jobb klikk a `src/test/java` mappán > Run 'All Tests'.

## Fordítás és futtatás Mavennel

    cd Chef_Renting_System_API
    mvn test                          # tesztek
    mvn package                       # target/chef-renting-system-api-1.0.0.jar
    mvn spring-boot:run               # futtatás Mavenből
    java -jar target/chef-renting-system-api-1.0.0.jar     # futtatás a jarból

Ha a 8080-as port foglalt: `java -jar target/chef-renting-system-api-1.0.0.jar --server.port=8081`. Az adatbázis böngészője: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:chefrenting`, felhasználó: `sa`, jelszó üres).

## Példa kérések

Linux/macOS/Git Bash (curl):

    curl http://localhost:8080/api/berlesek
    curl http://localhost:8080/api/berlesek/1
    curl -X POST http://localhost:8080/api/berlesek -H "Content-Type: application/json" \
         -d '{"uid":5,"chefId":101,"startDate":"2030-05-10","endDate":"2030-05-14","dailyRate":320,"baseFee":60}'
    curl -X DELETE http://localhost:8080/api/berlesek/301

Windows PowerShell:

    Invoke-RestMethod http://localhost:8080/api/berlesek/1
    Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/berlesek -ContentType "application/json" `
      -Body '{"uid":5,"chefId":101,"startDate":"2030-05-10","endDate":"2030-05-14","dailyRate":320,"baseFee":60}'
    Invoke-RestMethod -Method Delete -Uri http://localhost:8080/api/berlesek/301

(A POST példa dátumait mindig a mai napnál későbbire kell cserélni, különben 400 a válasz.)

## Tesztek

31 automatikus teszt (`BerlesApiTest`: MockMvc + memóriabeli H2 + az induló CSV, `BerlesTest`: az ár számítása). Lefedik a GET/POST/DELETE végpontokat, a 404-eket, a kezdő dátum és a 3/14 napos határok szabályait (pontosan 3 és pontosan 14 nap engedélyezett), az átfedéseket (azonos séf tiltott, más séf engedett, szomszédos napok engedettek), a hibás kéréseket, és azt, hogy a `totalPrice` szerepel a JSON-ban, de nincs adatbázisoszlopa. A tesztek a mai naphoz viszonyított dátumokat használnak, ezért idővel sem évülnek el.
