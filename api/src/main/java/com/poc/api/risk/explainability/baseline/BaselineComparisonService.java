
package com.poc.api.risk.explainability.baseline;

public class BaselineComparisonService {

    public BaselineComparisonResult compare(double value, double mean, double stdDev) {
        if (stdDev == 0) {
            return new BaselineComparisonResult(0, 50, DeviationLabel.NORMAL);
        }
        double z = (value - mean) / stdDev;
        double percentile = Math.min(100, Math.max(0, 50 + z * 15));
        DeviationLabel label =
                percentile >= 99 ? DeviationLabel.EXTREME :
                percentile >= 90 ? DeviationLabel.ELEVATED :
                DeviationLabel.NORMAL;

        return new BaselineComparisonResult(z, percentile, label);
    }
}
