# FakeShop – Kotlin + Spring Boot REST backend

A [dummyjson.com/products](https://dummyjson.com/products) adatszerkezetére épülő webshop-backend.
Kotlin nyelven, Spring Boot keretrendszerrel, PostgreSQL adatbázissal, Railway-re telepíthető.

A termékek adatai induláskor automatikusan betöltődnek a dummyjson API-ból (ha üres az adatbázis).

---

## Mit tud

- **Lekérdezés**: termékek listázása kereséssel, szűréssel, rendezéssel, lapozással
- **Szerkesztés**: csak a `price`, `discountPercentage`, `rating` és `stock` írható
- **Csak olvasható**: minden más termékmező (cím, leírás, kategória, kép, méret, meta stb.)
- **Vélemények**: termékenként lekérdezhetők és újak hozzáadhatók

---

## Adatmodell

A dummyjson struktúrája három táblára bontva:

| Tábla | Tartalom |
|-------|----------|
| `products` | A termék fő adatai. A `dimensions` beágyazva (`dim_width/height/depth`), a `meta` mezői kibontva (`barcode`, `qr_code`, `meta_created_at`, `meta_updated_at`). |
| `product_tags` | Címkék (1‑n, `products`-hoz kötve). |
| `product_images` | Képek URL-jei, sorrend tartva. |
| `reviews` | Vélemények külön táblában (n‑1 a `products`-hoz), így bővíthetők és önállóan lekérdezhetők. |

A sémát a Flyway hozza létre (`src/main/resources/db/migration/V1__init.sql`).

---

## Végpontok

| Metódus | Útvonal | Leírás |
|--------|---------|--------|
| `GET` | `/` | Állapot és végpontlista |
| `POST` | `/api/auth/login` | Bejelentkezés, JWT token kérése |
| `GET` | `/api/auth/me` | A bejelentkezett felhasználó (token szükséges) |
| `GET` | `/api/products` | Termékek listája (szűrés/rendezés/lapozás) |
| `GET` | `/api/products/{id}` | Egy termék teljes adatai |
| `GET` | `/api/products/categories` | Kategóriák listája |
| `PATCH` | `/api/products/{id}` | Szerkeszthető mezők módosítása **(token szükséges)** |
| `PUT` | `/api/products/{id}` | Ugyanaz, mint a PATCH **(token szükséges)** |
| `GET` | `/api/products/{id}/reviews` | Egy termék véleményei (lapozva) |
| `POST` | `/api/products/{id}/reviews` | Új vélemény hozzáadása **(token szükséges)** |
| `GET` | `/actuator/health` | Egészség-ellenőrzés (Railway healthcheck) |

### Lekérdezési paraméterek a `GET /api/products`-hoz

| Paraméter | Típus | Példa |
|-----------|-------|-------|
| `q` | szöveg | keresés címben, leírásban, márkában |
| `category` | szöveg | `beauty` |
| `brand` | szöveg | `Chanel` |
| `tag` | szöveg | `perfumes` |
| `minPrice` / `maxPrice` | szám | `10` / `100` |
| `minRating` | szám | `4` |
| `inStock` | logikai | `true` |
| `sortBy` | szöveg | `id`, `title`, `price`, `rating`, `stock`, `discountPercentage`, `category` |
| `order` | szöveg | `asc` / `desc` |
| `limit` | szám | `30` (max 100) |
| `skip` | szám | `0` |

Példa:
```
GET /api/products?category=beauty&minRating=4&sortBy=price&order=desc&limit=10
```

### Szerkesztés példa
```http
PATCH /api/products/1
Content-Type: application/json

{ "price": 12.49, "discountPercentage": 5.0, "rating": 4.1, "stock": 120 }
```
A többi mezőt küldeni felesleges; ha mégis küldöd, nem változnak.

További hívásminták: `api-examples.http`.

---

## Beléptetés (JWT)

A módosító végpontok (termék PATCH/PUT, új vélemény POST) csak érvényes JWT tokennel hívhatók.
A lekérdező (GET) végpontok és az `/api/auth/login` bárki számára nyitottak.

Induláskor 5 kamu felhasználó jön létre (a jelszavak BCrypttel titkosítva tárolódnak):

| Felhasználónév | Jelszó |
|----------------|--------|
| `user1` | `pass1` |
| `user2` | `pass2` |
| `user3` | `pass3` |
| `user4` | `pass4` |
| `user5` | `pass5` |

**1) Bejelentkezés – token kérése:**
```http
POST /api/auth/login
Content-Type: application/json

{ "username": "user1", "password": "pass1" }
```
Válasz:
```json
{ "token": "eyJhbGciOi...", "tokenType": "Bearer", "expiresIn": 86400, "username": "user1" }
```

**2) A token használata védett végpontnál** – tedd az `Authorization` fejlécbe `Bearer <token>` formában:
```http
PATCH /api/products/1
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{ "price": 12.49 }
```

Token nélkül (vagy lejárt/hibás tokennel) a védett végpontok `401 Unauthorized` választ adnak.

A token élettartama alapból 24 óra. Éles környezetben állítsd be a `JWT_SECRET`
környezeti változót (legalább 32 karakter); a lejárat a `JWT_EXPIRATION_MS`-szel módosítható.

A `user1` **admin** jogot kap (a rendelés-állapotok kezeléséhez), a `user2`–`user5` sima felhasználó.

---

## Rendelési folyamat és készletkezelés

A vásárlás menete: a felhasználó a kosarába tesz termékeket, majd a pénztárnál (checkout)
a kosárból rendelés készül, és **a készlet atomian levonódik**. Minden végpont tokent igényel.

**Kosár**

| Metódus | Útvonal | Leírás |
|--------|---------|--------|
| `GET` | `/api/cart` | A kosár tartalma (aktuális árakkal) |
| `POST` | `/api/cart/items` | Termék hozzáadása: `{ "productId": 1, "quantity": 2 }` |
| `PUT` | `/api/cart/items/{productId}` | Mennyiség módosítása: `{ "quantity": 3 }` |
| `DELETE` | `/api/cart/items/{productId}` | Termék eltávolítása |
| `DELETE` | `/api/cart` | Kosár kiürítése |

**Rendelés**

| Metódus | Útvonal | Leírás |
|--------|---------|--------|
| `POST` | `/api/orders/checkout` | Pénztár: kosárból rendelés + készletlevonás |
| `GET` | `/api/orders` | A saját rendelések (legújabb elöl) |
| `GET` | `/api/orders/{id}` | Egy saját rendelés részletei |
| `POST` | `/api/orders/{id}/cancel` | Saját rendelés lemondása (készlet visszaírás) |
| `PATCH` | `/api/orders/{id}/status` | **Admin**: állapot módosítása: `{ "status": "SHIPPED" }` |

**Hogyan működik a készlet:**
- Checkoutkor minden tételnél egy atomi `UPDATE ... WHERE stock >= mennyiség` fut. Ha bármelyik
  terméknél nincs elég készlet, az egész rendelés visszagördül (semmi nem változik), és `409` a válasz.
- A rendeléstételek a leadáskori **árat és nevet pillanatképként** tárolják, így egy későbbi
  ármódosítás nem írja át a már leadott rendelés értékét.
- Lemondáskor (vagy admin `CANCELLED` állapotkor) a levont készlet **visszaíródik**.

**Rendelés-állapotok:** `PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED`, illetve bármelyik
nem lezárt állapotból `CANCELLED`. A `DELIVERED` és `CANCELLED` végállapot.

> Megjegyzés: fizetés még nincs a rendszerben – a rendelés `PENDING` állapotban jön létre.
> A fizetés integrálása külön lépés lenne.

---

## Helyi futtatás

Kell hozzá: JDK 21 és Docker (a PostgreSQL-hez).

```bash
# 1) Adatbázis indítása
docker compose up -d

# 2) Alkalmazás indítása
./gradlew bootRun
```

Az app a `http://localhost:8080` címen fut, és induláskor betölti a dummyjson adatait.

> A `./gradlew` első futtatáskor letölti a megfelelő Gradle-verziót, ehhez internet kell.

---

## Feltöltés GitHubra

A projekt már egy kész git repó (van benne kezdeti commit). Csak a távoli repót kell beállítani és feltölteni:

```bash
cd fakeShopKotlinSpringBackend

# ha még nem inicializált git repó nálad:
# git init && git add . && git commit -m "Initial commit"

git remote add origin https://github.com/DViktor-refs/fakeShopKotlinSpringBackend.git
git branch -M main
git push -u origin main
```

A `push` során a GitHub bejelentkezést/Personal Access Tokent kér – ez a te fiókod, ezért ezt csak te tudod megtenni.

---

## Telepítés Railway-re

1. **Új projekt**: Railway → *New Project* → *Deploy from GitHub repo* → válaszd a `fakeShopKotlinSpringBackend` repót.
   A `Dockerfile` és a `railway.json` alapján automatikusan buildel.

2. **PostgreSQL hozzáadása**: a projektben *New* → *Database* → *Add PostgreSQL*.

3. **Környezeti változó** a backend service *Variables* fülén – elég **egyetlen** változó:

   ```
   DATABASE_URL=${{Postgres.DATABASE_URL}}
   ```

   Az app a `DATABASE_URL`-t automatikusan JDBC-re alakítja és beállítja belőle a kapcsolatot
   (lásd `DatabaseUrlEnvironmentPostProcessor`). Ha a Postgres service neve nem „Postgres", igazítsd a hivatkozást.

   A `PORT`-ot a Railway adja automatikusan, az app abból olvassa ki a portot.

   > Alternatíva (kézi beállítás): ha jobban szeretnéd külön megadni, a
   > `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
   > változók is működnek, és elsőbbséget élveznek a `DATABASE_URL`-lel szemben.

4. **Publikus URL**: a backend service → *Settings* → *Networking* → *Generate Domain*.

Az első indításkor a Flyway létrehozza a táblákat, majd betöltődnek a dummyjson termékei.

### Hasznos változók

| Változó | Alapérték | Mire jó |
|---------|-----------|---------|
| `SEED_ENABLED` | `true` | a kezdeti betöltés ki/be kapcsolása |
| `SEED_URL` | `https://dummyjson.com/products?limit=0` | a forrás URL |

---

## Fizetés (Barion)

A rendelések Barion Smart Gateway-en fizethetők ki. Szükséges Railway változók:

| Változó | Alapérték | Mire jó |
|---------|-----------|---------|
| `POSKEY` | – (kötelező) | a Barion POS titkos kulcsa |
| `PAYEE` | – (kötelező) | a pénzt fogadó Barion-fiók e-mail címe |
| `PUBLIC_BASE_URL` | `http://localhost:8080` | a backend publikus URL-je (Railway → *Generate Domain* után ide másold, `https://`-sel) |
| `BARION_BASE_URL` | `https://api.test.barion.com` (sandbox) | élesben `https://api.barion.com` |
| `BARION_CURRENCY` | `HUF` | a Barion POS fióknak támogatnia kell |
| `BARION_REDIRECT_URL` | – (opcionális) | ha van külön frontend "köszönjük" oldal; ha üres, a backend saját, beépített oldalát használja |

> ⚠️ **Fontos**: a `PUBLIC_BASE_URL` beállítása nélkül a Barion callback-je (`{PUBLIC_BASE_URL}/api/payments/barion/callback`) nem talál vissza a szerverhez, és a fizetés állapota nem frissül automatikusan – ilyenkor a `/api/orders/{id}/payment/refresh` végponttal lehet manuálisan lekérdezni.
>
> ⚠️ A termékek ára (dummyjson) dollárban van megadva, de a fizetés jelenleg **nem vált át pénznemet** – a tárolt `totalAmount` értéket küldi el a beállított `BARION_CURRENCY`-ben (alapból HUF). Ez egy demo webshophoz elfogadható, de éles használat előtt érdemes átgondolni.

### Fizetési folyamat

1. `POST /api/orders/{id}/pay` (token szükséges, a rendelés a sajátod kell legyen) → elindítja a Barion fizetést, és visszaadja a `gatewayUrl`-t.
2. A kliens (böngésző vagy Android WebView) a `gatewayUrl`-re navigál, ahol a vásárló befejezi a fizetést.
3. A Barion (a) szerver-szerver hívást küld a `POST /api/payments/barion/callback` végpontra, és (b) visszairányítja a böngészőt a `RedirectUrl`-re (`GET /api/payments/barion/result`, ha nincs külön frontend).
4. Mindkét esetben a szerver lekérdezi a tényleges állapotot a Barion API-tól (`GetPaymentState`), és frissíti a rendelést: sikeres fizetésnél `paymentStatus=SUCCEEDED` és `status=CONFIRMED`; sikertelen/megszakított/lejárt fizetésnél a készlet visszaíródik és `status=CANCELLED`.
5. Ha a kliens nem akar várni a callback-re (pl. mobilappnál gyakori), a `POST /api/orders/{id}/payment/refresh` végponttal bármikor kikényszeríthető a friss állapot lekérdezése.

Az `OrderResponse` mostantól tartalmazza a `paymentStatus` (`NOT_STARTED` / `PENDING` / `SUCCEEDED` / `FAILED` / `CANCELED` / `EXPIRED`) és `paymentId` mezőket is.

---

## Technológiák

Kotlin 2.0 · Spring Boot 3.3 · Spring Data JPA · Flyway · PostgreSQL · Gradle (Kotlin DSL) · JDK 21

## Hibakeresés

- **„Healthcheck failed" / „service unavailable" a Railway deploy végén**: ez azt jelenti, hogy az app nem indult el – szinte mindig az adatbázis-kapcsolat hiányzik. Ellenőrizd: (1) van-e PostgreSQL service a projektben, (2) be van-e állítva a `DATABASE_URL=${{Postgres.DATABASE_URL}}` változó a backend service-en. A tényleges hibát a *Deployments → (a deploy) → Deploy Logs* mutatja (pl. `Connection refused`, `UnknownHostException`, vagy Flyway/JPA hiba).
- **„Schema-validation” hiba indításkor**: ha az adatbázis sémája eltér a vártól (pl. korábbi kézi módosítás), átmenetileg állítsd az `application.yml`-ben a `spring.jpa.hibernate.ddl-auto` értéket `none`-ra. A sémát a Flyway kezeli.
- **Nem töltődnek be a termékek**: ellenőrizd, hogy a futtató környezet eléri-e a `dummyjson.com`-ot, vagy add meg a saját `SEED_URL`-t.
