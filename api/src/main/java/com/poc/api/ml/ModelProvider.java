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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * EPIC 5 ModelProvider:
 *
 *  - Wraps a Tribuo logistic-regression classifier over a fixed 4D feature vector
 *    defined in {@link FeatureVectorSchema}.
 *  - Maintains a small IsolationForest instance for anomaly scoring on the same
 *    feature space.
 *  - Still exposes the existing predict(device, behavior, tls, context) method so
 *    RiskService doesn't have to know about Tribuo.
 *
 *  Training is coordinated by MlTrainingService, which calls {@link #train(List, List)}
 *  with vectors and labels built from persisted session_feature rows.
 *
 *  On startup, if no training has occurred yet, we fall back to a tiny synthetic
 *  model so the PoC still works from a clean database.
 */
@Component
public class ModelProvider {

  private volatile Model<Label> model;
  private volatile IsolationForest isolationForest;
  private volatile String modelVersion = "uninitialized";

  private final LabelFactory labelFactory = new LabelFactory();

  @PostConstruct
  public void init() {
    // Synthetic bootstrap model – only used if no proper training has been run yet.
    if (model == null) {
      trainSynthetic();
    }
  }

  public boolean isReady() {
    return model != null && isolationForest != null;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public static final class TrainingExample {
    public final double[] features;
    public final String label;

    public TrainingExample(double[] features, String label) {
      this.features = features;
      this.label = label;
    }
  }

  /**
   * Called by MlTrainingService with real data from session_feature.
   *
   * @param examples labelled examples
   * @param vectors the raw feature vectors (same objects as in examples, but passed
   *                separately for convenience when training the IsolationForest)
   */
  public synchronized void train(List<TrainingExample> examples, List<double[]> vectors) {
    if (examples == null || examples.isEmpty()) {
      return;
    }

    // Build Tribuo dataset
    SimpleDataSourceProvenance provenance =
        new SimpleDataSourceProvenance("session-feature-training", labelFactory);
    MutableDataset<Label> dataset = new MutableDataset<>(provenance, labelFactory);

    for (TrainingExample ex : examples) {
      double[] vals = ex.features;
      String[] featureNames = FeatureVectorSchema.featureNames().toArray(new String[0]);
      if (vals.length != featureNames.length) {
        throw new IllegalArgumentException("Feature length " + vals.length +
            " does not match schema size " + featureNames.length);
      }
      Label label = labelFactory.generateOutput(ex.label);
      ArrayExample<Label> tribuoExample = new ArrayExample<>(label, featureNames, vals);
      dataset.add(tribuoExample);
    }

    // Train logistic regression
    LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
    this.model = trainer.train(dataset);

    // Train IsolationForest on the same feature vectors
    List<double[]> dataForIf = (vectors != null && !vectors.isEmpty()) ? vectors : new ArrayList<>();
    if (dataForIf.isEmpty()) {
      for (TrainingExample ex : examples) {
        dataForIf.add(ex.features);
      }
    }
    IsolationForest iforest = new IsolationForest(50, Math.min(256, dataForIf.size()), 42L);
    iforest.fit(dataForIf);
    this.isolationForest = iforest;

    this.modelVersion = "trained-" + Instant.now();
  }

  /**
   * Original API used by RiskService – returns probability that the session is
   * "legit" according to the current classifier. If the model isn’t ready for
   * whatever reason, we fall back to a simple heuristic.
   */
  public double predict(double deviceScore, double behaviorScore, double tlsScore, double contextScore) {
    if (model == null) {
      // Heuristic fallback: weighted sum of scores
      double raw = 0.5 * deviceScore + 0.2 * behaviorScore + 0.2 * tlsScore + 0.1 * contextScore;
      return Math.max(0.0, Math.min(1.0, raw));
    }

    String[] featureNames = FeatureVectorSchema.featureNames().toArray(new String[0]);
    double[] featureValues = FeatureVectorSchema.fromScores(deviceScore, behaviorScore, tlsScore, contextScore);

    Label unknown = labelFactory.generateOutput("unknown");
    ArrayExample<Label> ex = new ArrayExample<>(unknown, featureNames, featureValues);

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

  /**
   * Isolation-Forest-based anomaly score in [0,1] where higher means "more anomalous".
   */
  public double anomalyScore(double deviceScore, double behaviorScore, double tlsScore, double contextScore) {
    if (isolationForest == null) {
      return 0.0;
    }
    double[] v = FeatureVectorSchema.fromScores(deviceScore, behaviorScore, tlsScore, contextScore);
    return isolationForest.score(v);
  }

  private synchronized void trainSynthetic() {
    String[] featureNames = FeatureVectorSchema.featureNames().toArray(new String[0]);

    SimpleDataSourceProvenance provenance =
        new SimpleDataSourceProvenance("synthetic-risk-data", labelFactory);
    MutableDataset<Label> dataset = new MutableDataset<>(provenance, labelFactory);
    Random rnd = new Random(42L);

    List<double[]> vectors = new ArrayList<>();

    // Generate synthetic rows
    for (int i = 0; i < 200; i++) {
      double device = rnd.nextDouble();
      double behavior = rnd.nextDouble();
      double tls = rnd.nextDouble();
      double context = rnd.nextDouble();

      double raw = 0.5 * device + 0.2 * behavior + 0.2 * tls + 0.1 * context;
      String labelName = raw >= 0.6 ? "legit" : "fraud";
      Label label = labelFactory.generateOutput(labelName);

      double[] featureValues = FeatureVectorSchema.fromScores(device, behavior, tls, context);
      vectors.add(featureValues);

      ArrayExample<Label> ex = new ArrayExample<>(label, featureNames, featureValues);
      dataset.add(ex);
    }

    LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
    this.model = trainer.train(dataset);

    IsolationForest iforest = new IsolationForest(50, Math.min(256, vectors.size()), 42L);
    iforest.fit(vectors);
    this.isolationForest = iforest;

    this.modelVersion = "synthetic-" + Instant.now();
  }
}
