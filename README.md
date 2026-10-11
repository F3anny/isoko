# Isoko Marketplace

Isoko is the teacher’s original React/Vite and Java/Spring Boot project adapted to the supplied Isoko requirements. Its existing shop design, theme switcher, catalogue, product photos, cart, discounts, wishlist, orders and admin catalogue tools are retained.

## Structure

| Location | Purpose |
|---|---|
| `frontend/src/pages` | Buyer storefront, seller portal, admin console and inbox |
| `identity`, `security` | Phone verification, buyer login, seller/admin second factor, JWT sessions and Redis limits |
| `catalog`, `cart`, `wishlist`, `commerce` | Existing shopping modules, shop visibility, RWF pricing and delivery fees |
| `seller` | Shop applications, manual approval, seller-owned listings and fulfilment |
| `order`, `payment` | Stock reservation, idempotent checkout, signed sandbox callbacks, delivery OTP and order lifecycle |
| `marketplace` | Durable inbox, append-only audit and money ledger, scheduled expiry and payout |
| `src/main/resources/db/migration` | Flyway database evolution; original V1 and V2 preserved |
| `src/test` | Pricing/state rules and API integration tests with PostgreSQL/Redis |
| `.github/workflows` | Backend, frontend and container CI checks |
| `deploy/helm/isoko` | Kubernetes application deployments and CPU autoscaling |
| `docs/reference` | Original supplied documents, unchanged |
| `docs/REQUIREMENTS.md` | Implementation mapping, document discrepancies and remaining acceptance work |
| `docs/VERIFICATION.md` | Automated build, test and configuration evidence |
| `docs/DEMO_GUIDE.md` | Concise teacher demonstration script and technical talking points |

The application, Maven artifact, Docker containers, database and deployment identifiers use the Isoko name. The internal Java package and `cloud_sphere` database schema retain their original identifiers so the existing mapped entities and Flyway history remain compatible.

## Run on Windows

Use Docker Desktop for the presentation. This starts the storefront, API, PostgreSQL and Redis on one internal network and avoids conflicts with any PostgreSQL service already installed on Windows. From this directory:

```powershell
docker compose --profile app up --build -d
```

Wait until `docker compose ps` shows the API, PostgreSQL and Redis as running, then open http://localhost:5173. API documentation: http://localhost:8080/swagger-ui. Health: http://localhost:8080/api/v1/health.

The database inside Docker is named `isoko`; its development login is `postgres` / `Fanny1234`.

For local Java development only, start the database services first:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

If a different local PostgreSQL server already owns port 5432, keep using the full Docker command above.

## Lab accounts

All newly seeded accounts use **IsokoDemo1**. Existing accounts retain their previous passwords.

| Email | Purpose |
|---|---|
| `demo@isoko.rw` / `amina@isoko.test` | Standard buyer |
| `gold@isoko.rw` / `gold@isoko.test` | Gold buyer |
| `eric.seller@isoko.rw` | Approved Eric Fashion seller |
| `books.seller@isoko.rw` | Kigali Books, pending approval |
| `ops@isoko.rw` | Administrator |
| `locked@isoko.rw` / `locked@isoko.test` | Locked-account test |

Seller and admin logins require a six-digit second factor. The lab SMS adapter displays its code in the verification screen. New registration creates a user only after phone verification. Delivery codes appear in the buyer’s inbox. `ISOKO_LAB_CODES=false` hides authentication codes; an SMS adapter must be connected before using that mode.

## Seller quick start

1. Register, verify your phone, then select **Sell** and submit your shop name and business/KYC reference.
2. An admin opens **Admin → Marketplace operations** to review and approve the shop.
3. Sign out and sign in again to complete seller verification.
4. In **Sell**, create a listing with a positive integer RWF price, non-negative stock, category and draft/published visibility.
5. For a paid order, enter a tracking number and select **Ship my items**. Other sellers’ lines are not displayed.
6. The buyer opens **Inbox**, retrieves the delivery code and confirms receipt in **Orders**.
7. After all parcels arrive and the 48-hour dispute window closes, the scheduled worker releases sandbox seller payouts. Default deductions are 8% commission plus 2% payment fee. Rates are snapshotted at checkout.

## Buyer and administrator flows

Buyers can use MTN MoMo, Airtel Money or card in the sandbox. Checkout supports pickup (1,000 RWF), home delivery in Kigali (2,500 RWF), or elsewhere (4,000 RWF). Gold buyers or baskets with subtotal at least 50,000 RWF ship free. Quantity five or more receives 10% off; a promo code does not stack and total discount is capped at 50%.

Paid funds are held in escrow. Buyers can cancel before any parcel ships. Delivery codes are single-use. Buyers have 48 hours after full delivery to open a dispute, which freezes payout. Administrators can refund or release an upheld/disallowed dispute with a required resolution note. Money movements are balanced debit/credit records and cannot be updated or deleted.

## Tests

```powershell
.\mvnw.cmd test
cd frontend
npm run build
npm audit --audit-level=high
```

Docker is required for the API integration tests. For recent Docker Desktop engines that reject the older client API, run Maven with `-Dapi.version=1.44` and set `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` on Windows.

On a memory-constrained computer:

```powershell
$env:MAVEN_OPTS='-Xmx256m -XX:ActiveProcessorCount=2'
.\mvnw.cmd '-DforkCount=0' '-Dapi.version=1.44' test
```

## Payment callback contract

`POST /api/v1/payments/callback` accepts `{orderId, status, reference}`, with status `SUCCESS`, `DECLINE` or `TIMEOUT`. Set `X-Payment-Timestamp` to Unix seconds and `X-Payment-Signature` to lowercase hexadecimal HMAC-SHA256 using `PAYMENT_CALLBACK_SECRET` over:

```text
timestamp\norderId\nstatus\nreference
```

Here `\n` denotes a newline; omit the reference content when null. Timestamps outside five minutes are rejected. Repeated success is idempotent. Late success after cancellation is recorded and refunded in the sandbox. A timeout after captured payment freezes reconciliation and blocks another charge.

## Deployment and acceptance

The Helm chart expects existing PostgreSQL and Redis services and an `isoko-secrets` Secret containing database credentials, JWT secret and payment callback secret. Build the images, set image names in values, then install the chart. Use a dedicated namespace. Do not deploy over an existing application without reviewing the rendered resources.

PWA shell/catalogue caching and CI/deployment scaffolding are included. External PSP/SMS delivery, real payouts, performance targets and complete cluster observability require separate adapters/environment validation. Read `docs/REQUIREMENTS.md` before treating the application as accepted against every document requirement. The supplied UAT workbook is preserved with its original results; automated tests do not substitute for participant UAT/sign-off.
