package com.poc.gateway;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

/**
 * Global filter that computes a TLS fingerprint for each HTTPS request and
 * forwards it to downstream services via X-TLS-FP and X-TLS-Meta headers.
 *
 * This is a JA3/JA4-inspired fingerprint – it uses the TLS session id and
 * certificate details available via Spring's SslInfo. It's not a full spec
 * JA3 implementation but is sufficient for PoC device correlation.
 */
@Component
public class TlsFingerprintFilter implements GlobalFilter, Ordered {

    private static final HexFormat HEX = HexFormat.of();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        SslInfo sslInfo = exchange.getRequest().getSslInfo();
        if (sslInfo == null) {
            // Plain HTTP, nothing to do.
            return chain.filter(exchange);
        }

        String fp = computeFingerprint(sslInfo);
        String meta = buildMeta(sslInfo);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-TLS-FP", fp)
                .header("X-TLS-Meta", meta)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String computeFingerprint(SslInfo sslInfo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            X509Certificate[] chain = sslInfo.getPeerCertificates();
            String subject = "unknown";
            String issuer = "unknown";
            if (chain != null && chain.length > 0) {
                X509Certificate cert = chain[0];
                subject = cert.getSubjectX500Principal().getName();
                issuer = cert.getIssuerX500Principal().getName();
            }
            String sessionId = sslInfo.getSessionId();
            if (sessionId == null) {
                sessionId = "";
            }

            // Canonical string – JA3/JA4-style, but using what we can see here.
            String canonical = subject + "|" + issuer + "|" + sessionId;
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen for SHA-256; fall back to a simple marker.
            return "tls-fp-unavailable";
        }
    }

    private String buildMeta(SslInfo sslInfo) {
        StringBuilder sb = new StringBuilder("v1;");
        X509Certificate[] chain = sslInfo.getPeerCertificates();
        if (chain != null && chain.length > 0) {
            X509Certificate cert = chain[0];
            sb.append("sub=").append(safe(cert.getSubjectX500Principal().getName()));
            sb.append(";iss=").append(safe(cert.getIssuerX500Principal().getName()));
        } else {
            sb.append("sub=none;iss=none");
        }
        String sessionId = sslInfo.getSessionId();
        if (sessionId != null) {
            sb.append(";sid=").append(sessionId);
        }
        return sb.toString();
    }

    private String safe(String s) {
        if (s == null) {
            return "";
        }
        // Avoid header control chars
        return s.replaceAll("[\r\n]", " ");
    }

    @Override
    public int getOrder() {
        // run early
        return -100;
    }
}
