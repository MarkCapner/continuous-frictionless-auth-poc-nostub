# Infra

Run the full PoC stack:

```bash
cd infra
docker compose up --build
```

Services:

- Postgres (risk DB)
- Risk API (port 8080)
- TLS Gateway (HTTPS, port 8443)

You must generate a keystore for the gateway first (see `gateway/README.md`).
