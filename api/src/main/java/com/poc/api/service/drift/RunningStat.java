package com.poc.api.service.drift;

public final class RunningStat {
  private long n;
  private double mean;
  private double m2;

  public RunningStat(long n, double mean, double m2) {
    this.n = Math.max(0, n);
    this.mean = mean;
    this.m2 = m2;
  }

  public void push(double x) {
    n++;
    double delta = x - mean;
    mean += delta / n;
    double delta2 = x - mean;
    m2 += delta * delta2;
  }

  public long n() { return n; }
  public double mean() { return mean; }
  public double m2() { return m2; }

  public double variance() {
    if (n < 2) return 0.0;
    return m2 / (n - 1);
  }

  public double stddev() {
    return Math.sqrt(Math.max(0.0, variance()));
  }

  public double zscore(double x) {
    double sd = stddev();
    if (sd < 1e-9) return 0.0;
    return (x - mean) / sd;
  }
}
