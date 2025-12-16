package com.poc.api.ml.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.ml.persistence.RetrainJobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class RetrainJobRunner {

  private final RetrainJobRepository jobs;
  private final MlTrainingService training;
  private final ScorecardService scorecards;
  private final ObjectMapper om = new ObjectMapper();

  public RetrainJobRunner(RetrainJobRepository jobs,
                          MlTrainingService training,
                          ScorecardService scorecards) {
    this.jobs = jobs;
    this.training = training;
    this.scorecards = scorecards;
  }

  @Scheduled(fixedDelayString = "${poc.retrain.runner.delay-ms:30000}")
  public void tick() {
    jobs.nextQueued().ifPresent(job -> {
      jobs.markRunning(job.id());
      OffsetDateTime pivot = OffsetDateTime.now();
      try {
        MlTrainingService.TrainResult result = training.retrainFromRecentWithResult(500);
        jobs.markSucceeded(job.id(), result.modelId(), om.writeValueAsString(result.metrics()));
        scorecards.generateGlobalScorecard("retrain", pivot, 200, result.modelId(), result.version());
      } catch (Exception e) {
        jobs.markFailed(job.id(), e.getMessage());
      }
    });
  }
}
