package com.poc.api.service;

import com.poc.api.persistence.DeviceProfile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Simple rules layer on top of the ML probability.
 * Combines pLegit (ML output) with device and context signals to produce
 * an ALLOW / CHALLENGE / DENY decision.
 */
@Component
public class RulesEngine {

    public enum Decision {
        ALLOW,
        CHALLENGE,
        DENY
    }

    public static class FeaturesWithContext {
        public final DeviceProfile profile;
        public final String country;
        public final boolean vpn;
        public final boolean newDevice;
        public final boolean newTlsFingerprint;
        public final boolean highRiskAction;
        public final OffsetDateTime lastSeen;

        public FeaturesWithContext(DeviceProfile profile,
                                   String country,
                                   boolean vpn,
                                   boolean newDevice,
                                   boolean newTlsFingerprint,
                                   boolean highRiskAction,
                                   OffsetDateTime lastSeen) {
            this.profile = profile;
            this.country = country;
            this.vpn = vpn;
            this.newDevice = newDevice;
            this.newTlsFingerprint = newTlsFingerprint;
            this.highRiskAction = highRiskAction;
            this.lastSeen = lastSeen;
        }
    }

    /**
     * Combines pLegit from the ML model with contextual features to produce a decision.
     *
     * @param fctx   contextual features around this session
     * @param pLegit ML probability that this event is "legit" in [0,1]
     * @return ALLOW, CHALLENGE or DENY
     */
    public Decision apply(FeaturesWithContext fctx, double pLegit) {
        // Base risk from ML: low pLegit = high risk
        double risk = 1.0 - clamp01(pLegit);

        // New device / TLS fingerprint → increase risk
        if (fctx.newDevice) {
            risk += 0.20;
        }
        if (fctx.newTlsFingerprint) {
            risk += 0.10;
        }

        // VPN usage → increase risk
        if (fctx.vpn) {
            risk += 0.15;
        }

        // High-risk action (e.g. payment, password reset) → increase risk
        if (fctx.highRiskAction) {
            risk += 0.25;
        }

        // Long inactivity → small risk bump
        if (fctx.lastSeen != null) {
            Duration since = Duration.between(fctx.lastSeen, OffsetDateTime.now());
            if (since.toDays() > 30) {
                risk += 0.10;
            }
        }

        // Clamp
        if (risk < 0.0) risk = 0.0;
        if (risk > 1.0) risk = 1.0;

        // Map risk → decision thresholds
        if (risk < 0.30) {
            return Decision.ALLOW;
        } else if (risk < 0.70) {
            return Decision.CHALLENGE;
        } else {
            return Decision.DENY;
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
