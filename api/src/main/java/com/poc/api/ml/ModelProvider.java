package com.poc.api.ml;

import com.poc.api.persistence.ModelRegistryRepository;
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

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelProvider {

    private static final LabelFactory LABELS = new LabelFactory();
    private static final Label LEGIT = LABELS.generateOutput("legit");
    private static final Label FRAUD = LABELS.generateOutput("fraud");

    private final ModelRegistryRepository registry;

    private volatile long activeId = 0L;
    private volatile Artifact activeArtifact;

    private final Map<Long, Artifact> cache = new ConcurrentHashMap<>();
    private volatile String modelVersion = "rules-only";

    public ModelProvider(ModelRegistryRepository registry) {
        this.registry = registry;
    }

    public static class TrainingExample {
        public final double[] features;
        public final boolean legit;
        public TrainingExample(double[] features, boolean legit) {
            this.features = features;
            this.legit = legit;
        }
    }

    private static class Artifact implements Serializable {
        final String version;
        final Model<Label> model;
        final IsolationForest iforest;

        Artifact(String version, Model<Label> model, IsolationForest iforest) {
            this.version = version;
            this.model = model;
            this.iforest = iforest;
        }
    }

    /**
     * EPIC 11 fix:
     * - Do NOT train a synthetic model at startup.
     * - Load an active model if present; otherwise stay in "rules-only" mode.
     * - First real model is created via Admin -> Retrain using session_feature_store data.
     */
    @PostConstruct
    public void init() {
        registry.findActive().ifPresent(rec -> {
            Artifact a = decode(rec.bytes());
            if (a != null) {
                activeId = rec.id();
                activeArtifact = a;
                cache.put(activeId, a);
                modelVersion = a.version;
            }
        });

        // If no model found, intentionally remain in rules-only mode.
        // This prevents boot failures due to schema mismatch / synthetic training.
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void train(List<TrainingExample> examples, List<double[]> vectorsForIf) {
        if (examples == null || examples.isEmpty()) {
            // no-op; keep current model/rules-only
            return;
        }

        // Determine schema and enforce correct dimensionality.
        String[] names = FeatureVectorSchema.featureNames().toArray(new String[0]);
        int expectedDim = names.length;

        // Filter/validate incoming examples so we never crash inside Tribuo.
        List<TrainingExample> valid = new ArrayList<>(examples.size());
        for (TrainingExample ex : examples) {
            if (ex == null || ex.features == null) continue;
            if (ex.features.length != expectedDim) {
                // Skip mismatched vectors (prevents names/values mismatch)
                continue;
            }
            valid.add(ex);
        }

        if (valid.isEmpty()) {
            // Nothing usable; keep current model/rules-only
            return;
        }

        // Build dataset
        MutableDataset<Label> dataset = new MutableDataset<>(
                new SimpleDataSourceProvenance("session_feature_store", LABELS),
                LABELS
        );

        for (TrainingExample ex : valid) {
            Label y = ex.legit ? LEGIT : FRAUD;
            dataset.add(new ArrayExample<>(y, names, ex.features));
        }

        // Train classifier
        LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
        Model<Label> m = trainer.train(dataset);

        // Fit Isolation Forest (use provided vectors or derive from valid examples)
        List<double[]> vectors = new ArrayList<>();
        if (vectorsForIf != null && !vectorsForIf.isEmpty()) {
            for (double[] v : vectorsForIf) {
                if (v != null && v.length == expectedDim) vectors.add(v);
            }
        }
        if (vectors.isEmpty()) {
            for (TrainingExample ex : valid) vectors.add(ex.features);
        }

        IsolationForest iforest = new IsolationForest(
                50,
                Math.min(256, Math.max(1, vectors.size())),
                42L
        );
        iforest.fit(vectors);

        // Activate in-memory (registry persistence is handled elsewhere via Admin retrain flow)
        this.modelVersion = "trained-" + Instant.now();
        this.activeArtifact = new Artifact(modelVersion, m, iforest);
        this.activeId = 0L; // "in-memory" until persisted/activated via registry
    }

    public byte[] exportArtifactBytes() {
        if (activeArtifact == null) return new byte[0];
        return encode(activeArtifact);
    }

    public void setActiveFromRegistry(long modelId, byte[] bytes) {
        Artifact a = decode(bytes);
        if (a == null) return;
        this.activeId = modelId;
        this.activeArtifact = a;
        this.modelVersion = a.version;
        cache.put(modelId, a);
    }

    /**
     * Predict returns P(legit). If no model is loaded, returns a rule-based fallback score.
     */
    public double predict(double deviceScore, double behaviorScore, double tlsScore, double contextScore) {
        return predictWithModelId(activeId, deviceScore, behaviorScore, tlsScore, contextScore);
    }

    public double predictWithModelId(long modelId, double deviceScore, double behaviorScore, double tlsScore, double contextScore) {
        Artifact a = resolve(modelId);

        // Rules-only fallback if no model
        if (a == null || a.model == null) {
            double raw = 0.5 * deviceScore + 0.2 * behaviorScore + 0.2 * tlsScore + 0.1 * contextScore;
            return clamp01(raw);
        }

        String[] names = FeatureVectorSchema.featureNames().toArray(new String[0]);
        int expectedDim = names.length;

        // Build feature vector to match schema length
        double[] vals = buildVectorForSchema(names, deviceScore, behaviorScore, tlsScore, contextScore);
        if (vals.length != expectedDim) {
            // Should never happen, but keep safe.
            double raw = 0.5 * deviceScore + 0.2 * behaviorScore + 0.2 * tlsScore + 0.1 * contextScore;
            return clamp01(raw);
        }

        // Single example dataset for prediction
        MutableDataset<Label> ds = new MutableDataset<>(
                new SimpleDataSourceProvenance("predict", LABELS),
                LABELS
        );
        ds.add(new ArrayExample<>(LEGIT, names, vals));

        Prediction<Label> pred = a.model.predict(ds.getExample(0));
        var score = pred.getOutputScores().get(LEGIT);
        double pLegit = (score == null) ? 0.5 : score.getScore();
        return clamp01(pLegit);
    }

    public double anomalyScore(double deviceScore, double behaviorScore, double tlsScore, double contextScore) {
        return anomalyScoreWithModelId(activeId, deviceScore, behaviorScore, tlsScore, contextScore);
    }

    public double anomalyScoreWithModelId(long modelId, double deviceScore, double behaviorScore, double tlsScore, double contextScore) {
        Artifact a = resolve(modelId);
        if (a == null || a.iforest == null) return 0.0;

        String[] names = FeatureVectorSchema.featureNames().toArray(new String[0]);
        double[] vals = buildVectorForSchema(names, deviceScore, behaviorScore, tlsScore, contextScore);
        return a.iforest.score(vals);
    }

    private Artifact resolve(long modelId) {
        if (modelId <= 0) return activeArtifact;
        Artifact cached = cache.get(modelId);
        if (cached != null) return cached;

        return registry.findById(modelId).map(r -> {
            Artifact a = decode(r.bytes());
            if (a != null) cache.put(modelId, a);
            return a;
        }).orElse(activeArtifact);
    }

    /**
     * Builds a vector aligned to FeatureVectorSchema.featureNames().
     *
     * Supports two cases:
     * 1) If schema is exactly 4-dim: [device, behavior, tls, context]
     * 2) If schema has more dims: fill known "core" scores by matching name strings,
     *    and set unknown dims to 0.
     *
     * This prevents the Tribuo "names.length != values.length" crash permanently.
     */
    private static double[] buildVectorForSchema(String[] names,
                                                 double deviceScore,
                                                 double behaviorScore,
                                                 double tlsScore,
                                                 double contextScore) {
        if (names == null || names.length == 0) {
            return new double[] { deviceScore, behaviorScore, tlsScore, contextScore };
        }

        // If schema is exactly 4, keep it simple
        if (names.length == 4) {
            return new double[] { deviceScore, behaviorScore, tlsScore, contextScore };
        }

        // Otherwise map by name (best-effort, robust)
        double[] v = new double[names.length];
        for (int i = 0; i < names.length; i++) {
            String n = (names[i] == null) ? "" : names[i].toLowerCase(Locale.ROOT);

            // Common name patterns we might have in FeatureVectorSchema
            if (n.contains("device") && n.contains("score")) v[i] = deviceScore;
            else if (n.equals("device_score")) v[i] = deviceScore;

            else if (n.contains("behavior") && n.contains("score")) v[i] = behaviorScore;
            else if (n.equals("behaviour_score") || n.equals("behavior_score")) v[i] = behaviorScore;

            else if (n.contains("tls") && n.contains("score")) v[i] = tlsScore;
            else if (n.equals("tls_score")) v[i] = tlsScore;

            else if (n.contains("context") && n.contains("score")) v[i] = contextScore;
            else if (n.equals("context_score")) v[i] = contextScore;

            // Unknown dims default to 0.0
        }
        return v;
    }

    private static byte[] encode(Artifact a) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(a);
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static Artifact decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object o = ois.readObject();
            return (Artifact) o;
        } catch (Exception e) {
            return null;
        }
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
