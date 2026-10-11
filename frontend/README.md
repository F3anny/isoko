# Isoko storefront

React 19, React Router and Vite, retaining the teacher’s existing design, product images and light/dark/system themes.

```powershell
npm ci
npm run dev
```

Open http://localhost:5173 with the API on port 8080. Vite proxies `/api` to the API.

Pages include the shop, product details, cart/checkout, authentication/phone verification, wishlist, buyer orders, seller portal, notification inbox and administrator operations. Read the parent README for lab accounts and workflow instructions.

```powershell
npm run build
npm run preview
```

The production build emits a PWA manifest and service worker. It caches the public catalogue and offline shell, never authenticated account/order API responses. Offline cart additions are checked against live price and stock when the same account reconnects. The frontend Docker image uses nginx to serve the SPA and proxy API requests.
