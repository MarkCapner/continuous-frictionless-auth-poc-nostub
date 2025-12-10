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
import java.security.cert.X509Certificate;
import java.util.HexFormat;

@Component
public class TlsFingerprintFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        SslInfo sslInfo = exchange.getRequest().getSslInfo();
        String fp = "no-tls";
        String meta = "no-tls";

        if (sslInfo != null) {
            try {
                String sessionId = sslInfo.getSessionId();
                X509Certificate[] peerCerts = sslInfo.getPeerCertificates();

                MessageDigest digest = MessageDigest.getInstance("SHA-256");

                if (sessionId != null) {
                    digest.update(sessionId.getBytes(StandardCharsets.UTF_8));
                }

                int certLen = 0;
                boolean peerPresent = false;

                if (peerCerts != null && peerCerts.length > 0 && peerCerts[0] != null) {
                    peerPresent = true;
                    byte[] encoded = peerCerts[0].getEncoded();
                    certLen = encoded.length;
                    digest.update((byte) 0);
                    digest.update(encoded);
                }

                fp = HexFormat.of().formatHex(digest.digest());
                meta = "session_present=" + (sessionId != null ? "1" : "0")
                        + ";peer_present=" + (peerPresent ? "1" : "0")
                        + ";peer_cert_len=" + certLen;
            } catch (Exception e) {
                fp = "tls-error";
                meta = "tls-error";
            }
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-TLS-FP", fp)
                .header("X-TLS-Meta", meta)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // run early
        return -100;
    }
}
