package com.example.gatlingrunner.k8s;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class KubernetesJobScheduler {

  private final String namespace;
  private final String jobImage;
  private final String serviceAccount;
  private final String cpu;
  private final String memory;
  private final String resultsPvcName;

  public KubernetesJobScheduler(
      @Value("${app.kubernetes.namespace:default}") String namespace,
      @Value("${app.kubernetes.jobImage:example/gatling-web:latest}") String jobImage,
      @Value("${app.kubernetes.jobServiceAccount:default}") String serviceAccount,
      @Value("${app.kubernetes.jobCpu:1000m}") String cpu,
      @Value("${app.kubernetes.jobMemory:1Gi}") String memory,
      @Value("${app.kubernetes.resultsPvcName:gatling-results-pvc}") String resultsPvcName) {
    this.namespace = namespace;
    this.jobImage = jobImage;
    this.serviceAccount = serviceAccount;
    this.cpu = cpu;
    this.memory = memory;
    this.resultsPvcName = resultsPvcName;
  }

  public void scheduleJob(String runId, String scenario, String resultsPath, String s3Bucket) {
    String jobYaml = renderJobYaml(runId, scenario, resultsPath, s3Bucket);
    applyYaml(jobYaml);
  }

  private String renderJobYaml(String runId, String scenario, String resultsPath, String s3Bucket) {
    return """
apiVersion: batch/v1
kind: Job
metadata:
  name: gatling-run-%s
  namespace: %s
spec:
  backoffLimit: 0
  template:
    spec:
      serviceAccountName: %s
      restartPolicy: Never
      containers:
      - name: gatling-worker
        image: %s
        imagePullPolicy: IfNotPresent
        env:
        - name: MODE
          value: "worker"
        - name: RUN_ID
          value: "%s"
        - name: SCENARIO
          value: "%s"
        - name: RESULTS_PATH
          value: "%s"
        - name: S3_BUCKET
          value: "%s"
        resources:
          limits:
            cpu: "%s"
            memory: "%s"
        volumeMounts:
        - name: scenarios
          mountPath: /etc/gatling/scenarios
        - name: results
          mountPath: /var/gatling/results
      volumes:
      - name: scenarios
        configMap:
          name: gatling-scenarios
      - name: results
        persistentVolumeClaim:
          claimName: %s
""" .formatted(runId, namespace, serviceAccount, jobImage,
               runId, scenario, resultsPath, s3Bucket, cpu, memory, resultsPvcName);
  }

  private void applyYaml(String yaml) {
    ProcessBuilder pb = new ProcessBuilder("kubectl", "apply", "-f", "-");
    pb.redirectErrorStream(true);
    try {
      Process p = pb.start();
      try (OutputStream os = p.getOutputStream()) {
        os.write(yaml.getBytes(StandardCharsets.UTF_8));
        os.flush();
      }
      try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = br.readLine()) != null) {
          System.out.println(line);
        }
      }
      int rc = p.waitFor();
      if (rc != 0) {
        throw new RuntimeException("kubectl apply failed with exit code " + rc);
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to apply job yaml", e);
    }
  }
}
