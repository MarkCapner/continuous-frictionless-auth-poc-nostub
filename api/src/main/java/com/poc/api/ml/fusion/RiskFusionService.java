
package com.poc.api.ml.fusion;

public class RiskFusionService {
    public double fuse(double ruleScore, double mlScore, double confidence) {
        return ruleScore + (mlScore * confidence);
    }
}
