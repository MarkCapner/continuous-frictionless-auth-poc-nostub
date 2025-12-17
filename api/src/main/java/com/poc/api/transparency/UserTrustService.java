
package com.poc.api.transparency;

public class UserTrustService {
    public String trustMessage(double drift) {
        return drift < 0.3
            ? "Everything looks normal."
            : "Some unusual changes were detected.";
    }
}
