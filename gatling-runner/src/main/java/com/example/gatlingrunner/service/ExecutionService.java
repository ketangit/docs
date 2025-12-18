package com.example.gatlingrunner.service;

import com.example.gatlingrunner.k8s.KubernetesJobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

  private final Path scenariosPath;
  private final Path resultsPath;
  private final KubernetesJobScheduler jobScheduler;
  private final String s3Bucket;

  public ExecutionService(
      @Value("${app.scenarios.path:/etc/gatling/scenarios}") String scenariosDir,
      @Value("${app.results.path:/var/gatling/results}") String resultsDir,
      @Value("${app.s3.bucket}") String s3Bucket,
      KubernetesJobScheduler jobScheduler) {
    this.scenariosPath = Paths.get(scenariosDir);
    this.resultsPath = Paths.get(resultsDir);
    this.jobScheduler = jobScheduler;
    this.s3Bucket = s3Bucket;
    try {
      Files.createDirectories(resultsPath);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create results directory", e);
    }
  }

  public List<String> listScenarios() {
    if (!Files.exists(scenariosPath)) return Collections.emptyList();
    try {
      return Files.list(scenariosPath)
          .filter(p -> p.toString().endsWith(".scala") || p.toString().endsWith(".conf"))
          .map(p -> p.getFileName().toString().replaceAll("\\.scala$|\\.conf$", ""))
          .collect(Collectors.toList());
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }

  public String startScenario(String scenario) {
    String runId = generateRunId(scenario);
    Path runDir = resultsPath.resolve(runId);
    try {
      Files.createDirectories(runDir);
      Files.writeString(runDir.resolve("metadata.txt"),
          "scenario=" + scenario + "\nstartedAt=" + Instant.now(),
          StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create run directory", e);
    }

    // Schedule Kubernetes Job with MODE=worker
    jobScheduler.scheduleJob(runId, scenario, runDir.toString(), s3Bucket);

    return runId;
  }

  public String tailLog(String runId) {
    Path log = resultsPath.resolve(runId).resolve("run.log");
    if (!Files.exists(log)) {
      return "No logs yet. Worker may be starting.";
    }
    try {
      List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
      int max = 500;
      int from = Math.max(0, lines.size() - max);
      return String.join("\n", lines.subList(from, lines.size()));
    } catch (IOException e) {
      return "Unable to read log: " + e.getMessage();
    }
  }

  public boolean isFinished(String runId) {
    Path done = resultsPath.resolve(runId).resolve("DONE");
    return Files.exists(done);
  }

  private String generateRunId(String scenario) {
    String ts = String.valueOf(System.currentTimeMillis());
    String safe = scenario.replaceAll("[^a-zA-Z0-9\\-_.]", "-");
    return safe + "-" + ts;
  }
}
