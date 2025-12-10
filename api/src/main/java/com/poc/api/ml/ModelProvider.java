package com.poc.api.ml;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;

import java.util.Map;
import java.util.Random;

@Component
public class ModelProvider {

    private volatile Model<Label> model;
    private final LabelFactory labelFactory = new LabelFactory();

    @PostConstruct
    public void init() {
        //
        // Train a small logistic regression model on synthetic data.
        // Features: device_score, behavior_score, tls_score, context_score
        // Label: legit / fraud
        //
        String[] featureNames = new String[]{
                "device_score",
                "behavior_score",
                "tls_score",
                "context_score"
        };

        // Simple synthetic data provenance
        SimpleDataSourceProvenance provenance =
                new SimpleDataSourceProvenance("synthetic-risk-data", labelFactory);

        MutableDataset<Label> dataset = new MutableDataset<>(provenance, labelFactory);
        Random rnd = new Random(42L);

        // Generate synthetic rows
        for (int i = 0; i < 200; i++) {
            double device = rnd.nextDouble();
            double behavior = rnd.nextDouble();
            double tls = rnd.nextDouble();
            double context = rnd.nextDouble();

            double raw = 0.5 * device + 0.2 * behavior + 0.2 * tls + 0.1 * context;
            String labelName = raw >= 0.6 ? "legit" : "fraud";
            Label label = labelFactory.generateOutput(labelName);

            double[] featureValues = new double[]{device, behavior, tls, context};

            // Explicit type argument to avoid "cannot infer type arguments" error
            ArrayExample<Label> ex = new ArrayExample<Label>(
                    label,
                    featureNames,
                    featureValues
            );

            dataset.add(ex);
        }

        // Train logistic regression
        LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
        this.model = trainer.train(dataset);
    }

    /**
     * Returns the model's estimated probability that this event is "legit".
     */
    public double predict(double deviceScore,
                          double behaviorScore,
                          double tlsScore,
                          double contextScore) {
        if (model == null) {
            throw new IllegalStateException("Model not initialized");
        }

        String[] featureNames = new String[]{
                "device_score",
                "behavior_score",
                "tls_score",
                "context_score"
        };

        double[] featureValues = new double[]{
                deviceScore, behaviorScore, tlsScore, contextScore
        };

        // Unknown label for the example we’re scoring
        Label unknown = labelFactory.generateOutput("unknown");

        ArrayExample<Label> ex = new ArrayExample<Label>(
                unknown,
                featureNames,
                featureValues
        );

        Prediction<Label> prediction = model.predict(ex);
        Label output = prediction.getOutput();

        double pLegit;
        Map<String, Label> scores = prediction.getOutputScores();

        if (prediction.hasProbabilities() && scores != null && !scores.isEmpty()) {
            Label legitLabel = scores.get("legit");
            if (legitLabel != null) {
                // In Tribuo, when hasProbabilities() == true, Label.getScore() is the probability
                pLegit = legitLabel.getScore();
            } else {
                pLegit = "legit".equals(output.getLabel()) ? 0.9 : 0.1;
            }
        } else {
            // Fallback if probabilities are not available
            pLegit = "legit".equals(output.getLabel()) ? 0.9 : 0.1;
        }

        return pLegit;
    }
}
