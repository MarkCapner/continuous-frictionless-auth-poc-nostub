package com.poc.api.ml;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lightweight Isolation Forest implementation for EPIC 5.
 *
 * We intentionally keep this simple and in-process, so that the anomaly
 * scoring does not introduce any additional runtime dependencies beyond
 * Tribuo and the JDK.
 */
public class IsolationForest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final List<TreeNode> trees = new ArrayList<>();
  private final int subsampleSize;
  private final int numTrees;
  private final int maxDepth;
  private final Random random;

  public IsolationForest(int numTrees, int subsampleSize, long seed) {
    this.numTrees = numTrees;
    this.subsampleSize = subsampleSize;
    this.random = new Random(seed);
    this.maxDepth = (int) Math.ceil(Math.log(subsampleSize) / Math.log(2));
  }

  public void fit(List<double[]> data) {
    trees.clear();
    if (data == null || data.isEmpty()) {
      return;
    }
    int n = data.size();
    for (int i = 0; i < numTrees; i++) {
      List<double[]> sample = new ArrayList<>();
      for (int j = 0; j < Math.min(subsampleSize, n); j++) {
        sample.add(data.get(random.nextInt(n)));
      }
      TreeNode root = buildTree(sample, 0);
      trees.add(root);
    }
  }

  private TreeNode buildTree(List<double[]> data, int depth) {
    if (data.isEmpty() || depth >= maxDepth || allSame(data)) {
      return new TreeNode(data.size());
    }

    int numFeatures = data.get(0).length;
    int featureIndex = random.nextInt(numFeatures);

    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (double[] v : data) {
      min = Math.min(min, v[featureIndex]);
      max = Math.max(max, v[featureIndex]);
    }
    if (min == max) {
      return new TreeNode(data.size());
    }

    double split = min + random.nextDouble() * (max - min);
    List<double[]> left = new ArrayList<>();
    List<double[]> right = new ArrayList<>();
    for (double[] v : data) {
      if (v[featureIndex] < split) {
        left.add(v);
      } else {
        right.add(v);
      }
    }

    TreeNode node = new TreeNode(featureIndex, split);
    node.left = buildTree(left, depth + 1);
    node.right = buildTree(right, depth + 1);
    return node;
  }

  private boolean allSame(List<double[]> data) {
    if (data.size() <= 1) {
      return true;
    }
    double[] first = data.get(0);
    for (int i = 1; i < data.size(); i++) {
      double[] v = data.get(i);
      if (v.length != first.length) return false;
      for (int j = 0; j < v.length; j++) {
        if (Double.compare(v[j], first[j]) != 0) {
          return false;
        }
      }
    }
    return true;
  }

  public double score(double[] point) {
    if (trees.isEmpty()) {
      return 0.0;
    }
    double totalPathLength = 0.0;
    for (TreeNode tree : trees) {
      totalPathLength += pathLength(point, tree, 0);
    }
    double avgPathLength = totalPathLength / trees.size();
    double c = c(subsampleSize);
    return Math.pow(2.0, -avgPathLength / c);
  }

  private double pathLength(double[] point, TreeNode node, int depth) {
    if (node.isExternal()) {
      if (node.size <= 1) {
        return depth;
      }
      return depth + c(node.size);
    }
    if (point[node.featureIndex] < node.splitValue) {
      return pathLength(point, node.left, depth + 1);
    } else {
      return pathLength(point, node.right, depth + 1);
    }
  }

  /**
   * Average path length normalisation constant as used in Isolation Forest.
   */
  static double c(int n) {
    if (n <= 1) {
      return 0.0;
    }
    return 2.0 * (Math.log(n - 1) + 0.5772156649) - (2.0 * (n - 1) / n);
  }

  private static class TreeNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    int featureIndex;
    double splitValue;
    int size;
    TreeNode left;
    TreeNode right;

    // Internal node
    TreeNode(int featureIndex, double splitValue) {
      this.featureIndex = featureIndex;
      this.splitValue = splitValue;
      this.size = -1;
    }

    // External node
    TreeNode(int size) {
      this.size = size;
      this.featureIndex = -1;
      this.splitValue = Double.NaN;
    }

    boolean isExternal() {
      return size > 0;
    }
  }
}
