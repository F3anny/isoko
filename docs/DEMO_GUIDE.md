# Isoko teacher demonstration guide

## Opening statement

“Isoko is a Rwanda-focused multi-seller marketplace. I preserved the original React and Spring Boot architecture, then extended it with verified identity, seller approval, secure checkout, escrow, delivery confirmation, disputes, audit records and deployment automation.”

## Five-minute walkthrough

1. Open the home page and point out prices in integer RWF, public catalogue browsing, theme support, cart and saved wishlist.
2. Sign in with `demo@isoko.rw` / `IsokoDemo1`. Add an item and show the checkout options: pickup, Kigali delivery, other-district delivery, MTN MoMo, Airtel Money and card sandbox modes.
3. Explain that checkout locks stock and uses an idempotency key, so a refresh cannot charge twice. Successful sandbox payment is held in escrow rather than immediately paid to the seller.
4. Sign in as `eric.seller@isoko.rw` / `IsokoDemo1`. Use the displayed lab second-factor code. Show the seller’s own listings, stock controls and shipment action; a seller cannot view another seller’s orders.
5. Return to the buyer account. Show Inbox and Orders: the buyer retrieves a single-use delivery code and confirms delivery.
6. Sign in as `ops@isoko.rw` / `IsokoDemo1`. Show seller approval, dispute resolution, ledger entries, audit history and configurable commission/payment fees.

## Questions worth inviting

| Topic | Evidence in the project |
|---|---|
| “How do you prevent double checkout?” | Pessimistic stock locks, idempotency keys and integration test coverage for two buyers racing for the last unit. |
| “What happens to the money?” | Funds move through HELD, FROZEN and RELEASED escrow states. The append-only double-entry ledger requires balanced debits and credits. |
| “How do you protect accounts?” | Phone verification, expiring single-use challenges, attempt/issuance limits, lockout, seller/admin second factor, JWT session controls and Redis rate limiting. |
| “How is the system deployable?” | Docker API/storefront images, Compose services, CI workflow, Helm chart, health probes, non-root containers and autoscaling configuration. |
| “What was tested?” | 22 automated tests passed locally, including PostgreSQL/Redis integration, callback replay handling and concurrent last-stock checkout. See `VERIFICATION.md`. |

## Honest scope statement

Payment and SMS providers run as secured academic sandbox adapters. The application is ready for demonstration and core workflow assessment; connecting real providers, conducting participant UAT and completing the production platform items in `REQUIREMENTS.md` are needed before real-world release.
