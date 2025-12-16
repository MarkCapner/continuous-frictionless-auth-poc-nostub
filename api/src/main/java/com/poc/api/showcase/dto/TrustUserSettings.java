package com.poc.api.showcase.dto;

import java.time.OffsetDateTime;

/**
 * EPIC 12.6: lightweight privacy/consent & trust reset hooks for the PoC.
 *
 * This is intentionally simple: it does not attempt to implement full compliance,
 * but provides credible UX hooks and server-side state that can be extended later.
 */
public class TrustUserSettings {
    public String userId;
    public boolean consentGranted;
    public OffsetDateTime consentUpdatedAt;

    public OffsetDateTime baselineResetAt;
    public String baselineResetReason;
}
