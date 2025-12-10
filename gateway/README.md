# TLS Gateway

- Terminates TLS on :8443 using a PKCS#12 keystore.
- Uses Reactor Netty under Spring Cloud Gateway.
- Computes a deterministic TLS fingerprint from:
  - Negotiated protocol
  - Cipher suite
  - Peer certificate (when present)
- Hashes these values using SHA-256 and sends as `X-TLS-FP` header to the API.

This is **real TLS fingerprinting**, but not a full JA3/JA4 ClientHello parser.
