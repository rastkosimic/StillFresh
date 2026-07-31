# AllSecure local development (Windows)

AllSecure must reach your machine over the **public internet** for:

- **Browser return** after 3DS: `{PUBLIC_URL}/payment/allsecure/return?status=success`
- **Server callback** (saves the card): `{PUBLIC_URL}/payment/allsecure/callback`

`localhost` only works on your PC; AllSecure servers cannot call it. Use **ngrok** (or similar) tunneling to **api-gateway port 8080**.

## One-time setup

1. Copy environment file (if you do not have `.env` yet):

   ```powershell
   copy .env.example .env
   ```

   Fill in AllSecure sandbox credentials in `.env` (see `.env.example` comments).

2. Install [ngrok](https://ngrok.com/download) and authenticate:

   ```powershell
   ngrok config add-authtoken YOUR_NGROK_TOKEN
   ```

3. Start the stack:

   ```powershell
   docker-compose up -d --build
   ```

## Every dev session (ngrok URL changes on free tier)

1. **Terminal 1** — keep running:

   ```powershell
   ngrok http 8080
   ```

2. **Terminal 2** — sync `.env` from ngrok and restart payment-service:

   ```powershell
   .\scripts\update-allsecure-public-url.ps1
   docker-compose up -d --build payment-service
   ```

3. **Register a card** (must call register-card **after** step 2 so URLs are correct):

   ```http
   POST http://localhost:8080/payment/allsecure/register-card
   Authorization: Bearer <customer access token>
   ```

   Open the `redirectUrl` from the response.

4. On the hosted page: enter sandbox card `4200 0000 0000 0000`, click **Pay**.

5. On 3DS simulator: choose **Authenticated (ECI 05)** → **Submit**.

6. Verify:

   ```http
   GET http://localhost:8080/payment/allsecure/payment-methods
   Authorization: Bearer <customer access token>
   ```

   Or check logs:

   ```powershell
   docker-compose logs -f payment-service
   ```

   Look for `Received AllSecure callback` and `transactionType=REGISTER`.

## Configuration reference

| Variable | Purpose |
|----------|---------|
| `PAYMENT_PROVIDER=allsecure` | Use AllSecure for order preauth/capture/void |
| `ALLSECURE_PUBLIC_BASE_URL` | ngrok `https://....ngrok-free.dev` URL (no trailing slash) |
| `ALLSECURE_SHARED_SECRET` | HMAC key for callbacks — must match sandbox merchant |
| `GATEWAY_INTERNAL_SECRET` | Same value in all services (gateway stamps `X-Gateway-Secret`) |

Traffic path:

```
AllSecure → ngrok → localhost:8080 (api-gateway) → payment-service
```

Callback and return paths are public at the gateway; signature is verified inside `AllSecureController`.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| ngrok "endpoint offline" | ngrok not running, or old `redirectUrl` — run script + register-card again |
| `Forbidden` on callback | Request bypassed gateway — URLs must use ngrok → :8080, not :8086 |
| `INVALID SIGNATURE` on callback | Wrong `ALLSECURE_SHARED_SECRET` in `.env` |
| 3DS OK but no card stored | Callback never arrived — check ngrok inspector http://127.0.0.1:4040 |
| Hosted page says "Pay" | Normal for register flow; Total is empty (no charge) |

## ngrok inspector

While ngrok runs, open http://127.0.0.1:4040 to see every request (including AllSecure `POST /payment/allsecure/callback`).
