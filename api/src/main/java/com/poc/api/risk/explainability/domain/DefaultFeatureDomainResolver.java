
package com.poc.api.risk.explainability.domain;

import java.util.Map;

import static com.poc.api.risk.explainability.domain.FeatureDomain.*;

public class DefaultFeatureDomainResolver implements FeatureDomainResolver {

    private static final Map<String, FeatureDomain> MAP = Map.ofEntries(
            Map.entry("device.ua_entropy", DEVICE),
            Map.entry("tls.family.drift", TLS),
            Map.entry("behaviour.keystroke.z", BEHAVIOUR),
            Map.entry("identity.device_link_score", IDENTITY),
            Map.entry("ml.anomaly_score", ML)
    );

    @Override
    public FeatureDomain resolve(String featureKey) {
        return MAP.getOrDefault(featureKey, UNKNOWN);
    }
}
