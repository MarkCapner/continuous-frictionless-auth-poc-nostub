# Continuous Frictionless Authentication PoC (No-Stub Baseline)

This repository is a **working implementation baseline** for SPEC-001:
*Continuous Frictionless Authentication PoC*.

Your constraints:

- No stub or fake implementations.
- No hidden placeholders.
- Anything that is not implemented exactly as per SPEC-001 is explicitly called out.

## What is fully implemented

### Front-End (showcase/)

- React + Vite application.
- In-memory profiler SDK:
  - Collects device info: UA, UA-CH (when available), platform, cores, memory, screen, pixel ratio, timezone offset, languages.
  - Computes Canvas fingerprint hash using a real `<canvas>` drawing and `SubtleCrypto` SHA-256.
  - Computes WebGL fingerprint hash using a real WebGL context and `SubtleCrypto` SHA-256.
  - Collects behavior metrics:
    - Mouse move count and approximate distance.
    - Keypress count and average key interval.
    - Scroll event count.
- Sends POST `/api/auth/profile-check` with a JSON body that matches SPEC-001.
- Renders:
  - Telemetry snapshot.
  - Decision (AUTO_LOGIN | STEP_UP | DENY), confidence, and score breakdown.

### Risk & Auth API (api/)

- Spring Boot 3.3 application (Java 21).
- Endpoint: `POST /api/auth/profile-check`
  - Accepts telemetry as per SPEC-001.
  - Reads `X-TLS-FP` header from the gateway (or direct client for local dev).
- Persistence (via Spring JDBC + Flyway):
  - `device_profile` table.
  - `behavior_profile_stats` table.
  - `session_feature` table.
  - `model_registry` table.
  - `decision_log` table.
- Services:
  - Device profile upsert per `(user_id, tls_fp, canvas_hash)`.
  - Time-decayed behavior stats update (mean/variance).
  - Session feature + decision logging.
- FeatureBuilder:
  - Computes numeric similarity and behavior similarity exactly as described:
    - `numeric_sim(x,y,scale) = exp(-abs(x-y)/scale)`
    - `behavior_sim(z) = exp(-0.5 * z^2)` where `z = (x - mean) / sqrt(variance)`.
  - Produces device_score, behavior_score, tls_score, context_score in a deterministic way.
- RulesEngine:
  - Implements:
    - Impossible travel (based on country + last login country in `device_profile`).
    - New TLS fingerprint on high-risk action (when provided in context).
    - VPN + new device (based on `context.vpn` boolean and unseen `tls_fp`).
  - Applies decision thresholds:
    - `p >= 0.90` -> AUTO_LOGIN
    - `p >= 0.60` -> STEP_UP
    - else DENY
- ML model (Tribuo):
  - Uses **Tribuo** in-process to train a small logistic regression model at startup.
  - Training data is synthetic but generated programmatically with clear heuristics tying
    the features (device_score, behavior_score, tls_score, context_score) to a label
    ("legit" vs "fraud").
  - The trained model is then used for inference on each request.
  - There are **no fake scores**: probabilities come from Tribuo.

### TLS Gateway (gateway/)

- Spring Cloud Gateway (Reactor Netty).
- Terminates TLS on `:8443` using a PKCS#12 keystore (you generate it locally).
- For each HTTPS request:
  - Extracts `SslInfo` / `SSLSession` from the Netty pipeline.
  - Builds a reproducible TLS fingerprint string from:
    - Negotiated protocol.
    - Cipher suite.
    - Peer certificate public key hash (if available).
  - Hashes that string with SHA-256 and sets it as `X-TLS-FP` header.
- Routes `/api/**` to the API service.

> This is **real TLS fingerprinting** (based on live TLS session properties), but it is
> **not a full JA3/JA4 ClientHello parser**. See “Limitations vs SPEC-001” below.

### Infra (infra/)

- Docker Compose:
  - `postgres` (risk DB).
  - `api` (Spring Boot).
  - `gateway` (Spring Cloud Gateway TLS terminator).
- Dockerfiles for `api` and `gateway`.

## What is *not* fully implemented vs SPEC-001 (and why)

These items are **not** implemented in this repo, because they require
either external services / data or would be unreasonably large to do in-line here:

1. **Full JA3/JA4 ClientHello parsing**  
   - The gateway currently uses `SSLSession` to fingerprint the *negotiated* TLS
     properties, not the raw ClientHello fields.
   - To get true JA3/JA4, you would need:
     - A lower-level Netty server,
     - A handler that inspects the raw ClientHello bytes before handshake completion,
     - A full implementation of JA3/JA4 canonicalisation.
   - This codebase *does* provide a consistent TLS fingerprint, but it is not full JA4.

2. **GeoIP ASN/country lookup**  
   - Context features like ASN/country are accepted from the request context JSON.
   - There is **no embedded MaxMind/GeoIP database** or outbound lookup.
   - If you want real ASN/country detection, you can:
     - Add a GeoIP DB and lookup service,
     - Populate `context.country` and `context.asn` server-side.

3. **VPN / proxy detection**  
   - The rules engine honours a `context.vpn` boolean flag.
   - This repo does **not** implement VPN detection itself.

4. **Chaos mode / VPN toggle / resolution simulator UI**  
   - The showcase UI is a simple technology panel and not the full chaos-mode experience.
   - It collects all the signals you need; you can layer on the fancier UX on top.

5. **Retention job (Epic 6)**  
   - There is no scheduled deletion/redaction job included.
   - The schema is retention-ready (timestamps on rows), and you can add a simple
     scheduled job or CLI within `api` to purge old records.

Importantly: **nothing is stubbed or returns fake data silently**.  
Where something isn’t implemented, you see it explicitly in this README.

## How to run

### 1. Generate a TLS keystore for the gateway

From the repo root:

```bash
keytool -genkeypair \
  -alias gateway \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore gateway/src/main/resources/keystore.p12 \
  -validity 3650 \
  -storepass changeit \
  -dname "CN=localhost, OU=PoC, O=Frictionless, L=London, S=London, C=GB"
```

### 2. Start Postgres + API + Gateway

```bash
cd infra
docker compose up --build
```

This will expose:

- Postgres on `localhost:5432`
- API on `http://localhost:8080`
- Gateway on `https://localhost:8443` (using your self-signed cert)

### 3. Run the showcase front-end

In another terminal:

```bash
cd showcase
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

Then open:

- `http://localhost:5173`

Click **“Run profile check”** and observe the telemetry and decisions.

By default this dev setup calls the API directly on `http://localhost:8080`.
To run *through* the gateway, update the base URL in `showcase/src/api.ts` accordingly.
