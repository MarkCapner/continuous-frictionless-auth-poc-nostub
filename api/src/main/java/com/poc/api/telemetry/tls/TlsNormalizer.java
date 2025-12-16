package com.poc.api.telemetry.tls;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * EPIC 9: TLS normalisation.
 *
 * The gateway TLS fingerprint (X-TLS-FP) includes the TLS session id in its hash
 * (subject|issuer|sessionId) which causes variants per connection. For family
 * extraction we normalise to the stable parts: certificate subject + issuer.
 */
public final class TlsNormalizer {

  private static final HexFormat HEX = HexFormat.of();

  private TlsNormalizer() {}

  public static TlsNormalizationResult normalize(String tlsFp, String tlsMeta) {
    String fp = (tlsFp == null || tlsFp.isBlank()) ? "none" : tlsFp.trim();
    String meta = (tlsMeta == null) ? null : tlsMeta.trim();

    Map<String, String> kv = TlsMetaParser.parseKv(meta);
    String subDn = kv.get("sub");
    String issDn = kv.get("iss");
    boolean metaPresent = (subDn != null && !subDn.isBlank()) || (issDn != null && !issDn.isBlank());

    Map<String, String> subAttrs = TlsMetaParser.parseDnAttrs(subDn);
    Map<String, String> issAttrs = TlsMetaParser.parseDnAttrs(issDn);

    String familyKey = buildFamilyKey(subAttrs, issAttrs);
    String familyId = sha256Hex(familyKey);

    return new TlsNormalizationResult(fp, meta, familyId, familyKey, subAttrs, issAttrs, metaPresent);
  }

  /**
   * Canonical family key.
   *
   * We intentionally *ignore* the TLS session id and include only stable DN fields.
   */
  static String buildFamilyKey(Map<String, String> subjectAttrs, Map<String, String> issuerAttrs) {
    String subCn = norm(subjectAttrs.get("CN"));
    String subO = norm(subjectAttrs.get("O"));
    String subOu = norm(subjectAttrs.get("OU"));
    String issCn = norm(issuerAttrs.get("CN"));
    String issO = norm(issuerAttrs.get("O"));
    String issOu = norm(issuerAttrs.get("OU"));

    // Use a stable ordering and explicit field names.
    return "sub.cn=" + subCn + "|sub.o=" + subO + "|sub.ou=" + subOu +
        "|iss.cn=" + issCn + "|iss.o=" + issO + "|iss.ou=" + issOu;
  }

  private static String norm(String v) {
    if (v == null) return "";
    // collapse whitespace and lower-case for stable grouping
    String s = v.trim().replaceAll("\\s+", " ");
    return s.toLowerCase(Locale.ROOT);
  }

  public static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
      return HEX.formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      // Should never happen for SHA-256
      return "sha256-unavailable";
    }
  }
}
