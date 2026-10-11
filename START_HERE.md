# Start Isoko

Open PowerShell and run:

```powershell
cd "D:\classwork y3\Devops\isoko"
docker compose --profile app up --build -d
docker compose ps
```

When `isoko-api`, `isoko-postgres` and `isoko-redis` are running, open:

- Storefront: http://localhost:5173
- API health: http://localhost:8080/api/v1/health
- API documentation: http://localhost:8080/swagger-ui

To stop the application after your demonstration:

```powershell
docker compose --profile app down
```

Demo accounts use password `IsokoDemo1`:

- Buyer: `demo@isoko.rw`
- Seller: `eric.seller@isoko.rw`
- Administrator: `ops@isoko.rw`

The seller and administrator second-factor code is displayed by the academic lab adapter in the app.
