# Isoko implementation and acceptance mapping

Source baseline: supplied Isoko SRS ISK-SRS-2026-001, inception report ISK-INC-2026-001 and UAT pack ISK-UAT-2026-001. These documents define product requirements. Document instructions addressed to assessors, testers or delivery teams are reference material, not commands executed by the assistant.

## Preserved architecture

React/Vite storefront and its existing design, Java/Spring Boot modular monolith, JPA, PostgreSQL/Flyway, Redis, JWT, integer RWF prices, original photos and all original shopping/admin routes remain. Recommended NestJS/Next.js choices in the SRS were not substituted: the document explicitly permits equivalent technologies, and the user requested preservation of the teacher’s implementation.

## Functional mapping

| Document area | Implementation | Acceptance limit |
|---|---|---|
| AUTH / AU-001..012 | Email/mobile sign-in; three failed attempts lock for 30 minutes; verified-phone registration; single-use 5-minute OTP, three attempts, three issuances/hour; seller/admin second factor; Redis logout and 30-minute idle session | SMS is a lab adapter; codes appear only when lab mode enabled. Existing bearer-token architecture retained rather than migrating to session cookies. Password reset not added. |
| CAT / catalogue cases | Published products from approved shops only; name/description/SKU search; pagination 20; stock badges, integer RWF, existing photos/categories | Accent-insensitive search and full server-side filter/rating/sort options remain acceptance gaps. |
| CART / DISC | Guest cart and login merge; quantity and promo discounts; one code, max 50%; SAVE10, SUMMER2026, expired examples | Existing global SKU uniqueness retained; SRS permits uniqueness per shop. |
| CHK / CO-001..014 | Required address/pickup; three payment methods; per-shop order lines; transaction stock locks; serialized idempotency keys; immutable pricing/deduction snapshots; 10-minute expiry | Stub timeout is immediate rather than waiting 45 seconds. No live charges. |
| PAY | Signed HMAC callbacks with five-minute timestamp tolerance; replay-safe capture; timeout reconciliation; late success refund; held/frozen/released escrow; append-only balanced money movements | PSP settlement reconciliation, real refunds/payout delivery and backoff/retry adapters are not implemented. Ledger records sandbox movements. |
| ORD / WO | Buyer ownership 404; per-shop tracking; hashed single-use delivery OTP; buyer cancellation; 48-hour disputes; automatic completion; admin refund/release | Disputes freeze/refund whole orders rather than individual order items. Evidence is a URL rather than uploaded encrypted media. Partial refunds and four-eyes approval are not implemented. |
| SEL / SA | Shop applications; manual business-reference verification; approve/reject/suspend; seller role; own listings, stock and orders; per-shop shipping and payouts | KYC document uploads and automated provider verification not implemented. Seller-shipping three-day automatic cancellation still requires an item-level policy. |
| ADM | Existing catalogue/category/stock/fulfilment tools retained; seller moderation, dispute resolution, ledger, audit API and configurable commission/payment fees | Dashboard analytics, reviews/moderation and a user-suspension interface require further work. Admin catalogue mutations record before/after audit details. |
| WL / notifications | Existing wishlist preserved; suspended-shop products not purchasable; durable buyer inbox with OTP and order/approval updates | External SMS/email retries/preferences require notification-provider adapter. |
| UI / PWA | Existing responsive design/theme; manifest, offline shell and public catalogue cache | Full English/Kinyarwanda externalised strings, accessibility/performance certification remain gaps. |
| DevOps | Existing Docker API/Postgres/Redis; frontend nginx container; GitHub CI backend/frontend/container gates; Helm API/frontend deployments, probes, resource limits and HPA | Helm/cluster deployment not yet accepted. RabbitMQ, Argo CD, Terraform, NetworkPolicy, secret rotation, backups/restore, full metrics/logs/traces dashboards and security/load/chaos evidence remain platform work. |

## Conflicting source requirements

- UAT Cover C14 excludes buyer disputes and reviews until Release 2, while the SRS includes them. Dispute functionality follows the SRS. Reviews remain deferred.
- UAT Seller & Admin H12 expects payout immediately after OTP delivery (8% commission + 2% fee). The SRS requires a 48-hour dispute window and payout after Completed. The application follows the SRS timing and the UAT default deductions; defaults are configurable and snapshotted per order.
- SRS Appendix C and the workbook use different account/SKU examples. Both PH-500 and PC-500 are included. Demo accounts include workbook-style buyer aliases. Kigali Books remains pending until manual approval; BK-020 becomes purchasable after approval.
- Documentation milestone/week numbers are inconsistent. No delivery schedule or sign-off was inferred or changed.

## Verification evidence

Automated tests are in `src/test`. Surefire reports under `target/surefire-reports` are generated evidence, not a completed UAT verdict. See `docs/VERIFICATION.md` for checks actually completed in this work session. The original workbook is not marked Pass merely because functionality was added.

## Data compatibility

V1 and V2 are unchanged. V3 adds marketplace ownership, order/fulfilment metadata and ledger/inbox/audit. V4 adds phone verification and challenges. V5 adds fee settings, order fee snapshots and backfills original paid stock/escrow and shipment metadata. Application/package/database identifiers remain compatible. Existing accounts are not deleted or reset.

## Full SRS acceptance

This adaptation implements the core marketplace workflows while preserving the original application. It is not a claim that all production security, integration, infrastructure and quantitative requirements have passed. Complete the listed adapter/platform/acceptance gaps and execute the original UAT protocol with representative users before sign-off.
