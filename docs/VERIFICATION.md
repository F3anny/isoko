# Verification record

This record distinguishes checks run in the local development environment from work that still needs an external environment or human UAT.

## Completed locally — 10–11 October 2026

| Check | Result | Evidence |
|---|---|---|
| Backend automated suite | Passed: 22 tests, 0 failures, 0 errors | `target/surefire-reports` |
| PostgreSQL and Redis integration | Passed through Testcontainers | `IsokoMarketplaceTests` |
| Registration and authentication | Passed verified registration, single-use OTP handling, lockout persistence and seller restrictions | `IsokoMarketplaceTests`, `AuthAndCatalogTests` |
| Marketplace lifecycle | Passed checkout, escrow, delivery OTP, dispute/refund and balanced payout paths | `IsokoMarketplaceTests` |
| Concurrent stock protection | Passed: only one checkout can claim the last unit | `IsokoMarketplaceTests` |
| Payment callback protection | Passed signed callback, replay protection and timeout reconciliation paths | `IsokoMarketplaceTests` |
| Pricing and state rules | Passed 7 pricing tests and 4 order-state tests | `PricingTest`, `OrderStateMachineTest` |
| Frontend production build | Passed: Vite production bundle generated | `frontend/dist` |
| Frontend dependency audit | Passed: 0 vulnerabilities at high severity or above | `npm audit --audit-level=high` |
| Docker Compose definition | Passed validation for base and `app` profiles | `docker compose config --quiet` |

Expected HTTP warnings in the integration log correspond to negative tests such as invalid OTP, invalid credentials, seller ownership and duplicate payment protection.

## Required before formal sign-off

- Build the Docker images and render/install the Helm chart in the target Kubernetes environment.
- Execute the supplied UAT workbook with users and record the outcome in the workbook; do not mark UAT rows as passed solely from automated checks.
- Connect real SMS and payment-service adapters before any non-lab deployment.
- Complete the gaps listed in `REQUIREMENTS.md` if full production/SRS acceptance is required.

## Presentation smoke test

1. Start PostgreSQL and Redis with `docker compose up -d`.
2. Start the API, then the frontend, following the root README.
3. Sign in as `demo@isoko.rw` with `IsokoDemo1`, browse the catalogue and add an item to cart.
4. Show the seller journey using `eric.seller@isoko.rw`; show the administrator journey using `ops@isoko.rw`. Both use the lab verification code displayed by the app.
5. Explain that live payment/SMS delivery is deliberately represented by secured lab adapters, while the escrow, callback and audit workflows are verified locally.
