package com.poc.api.tls;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the gateway's X-TLS-Meta header.
 *
 * Current gateway format (gateway/TlsFingerprintFilter#buildMeta):
 *   v1;sub=<X500 DN>;iss=<X500 DN>;sid=<sessionId>
 */
public final class TlsMetaParser {

  private TlsMetaParser() {}

  /**
   * Parses a semicolon-delimited k=v header into a map.
   * Unknown tokens are ignored.
   */
  public static Map<String, String> parseKv(String tlsMeta) {
    if (tlsMeta == null || tlsMeta.isBlank()) {
      return Collections.emptyMap();
    }
    String[] parts = tlsMeta.split(";");
    Map<String, String> out = new LinkedHashMap<>();
    for (String p : parts) {
      if (p == null) continue;
      String s = p.trim();
      if (s.isEmpty()) continue;
      // allow a leading version token like "v1" with no '='
      int eq = s.indexOf('=');
      if (eq <= 0 || eq == s.length() - 1) {
        continue;
      }
      String k = s.substring(0, eq).trim().toLowerCase(Locale.ROOT);
      String v = s.substring(eq + 1).trim();
      if (!k.isEmpty()) {
        out.put(k, v);
      }
    }
    return out;
  }

  /**
   * Parses an X.500 DN into a simple attribute map (CN, O, OU, C, etc.).
   *
   * We keep this deliberately lightweight for the PoC. It does not implement
   * full RFC2253 escaping rules, but it handles the common OpenSSL/Java DN
   * formats seen in subject/issuer strings.
   */
  public static Map<String, String> parseDnAttrs(String dn) {
    if (dn == null || dn.isBlank()) {
      return Collections.emptyMap();
    }
    Map<String, String> out = new LinkedHashMap<>();
    String[] tokens = dn.split(",");
    for (String t : tokens) {
      if (t == null) continue;
      String s = t.trim();
      int eq = s.indexOf('=');
      if (eq <= 0 || eq == s.length() - 1) continue;
      String k = s.substring(0, eq).trim().toUpperCase(Locale.ROOT);
      String v = s.substring(eq + 1).trim();
      if (!k.isEmpty() && !v.isEmpty()) {
        out.put(k, v);
      }
    }
    return out;
  }
}
