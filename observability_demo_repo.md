# observability-demo

Full GitHub-ready repository containing a Spring Boot 3.5 application instrumented with Micrometer + OTLP, a Helm chart for deployment to Kubernetes (EKS), and a Skaffold configuration for local/CI-driven deployments. This repository implements the metrics, SLI/SLO pattern, and OTLP pipeline to Grafana Alloy.

---

## Repo layout

```
observability-demo/
├─ README.md
├─ pom.xml
├─ Dockerfile
├─ skaffold.yaml
├─ charts/
│  └─ observability-demo/
│     ├─ Chart.yaml
│     ├─ values.yaml
│     └─ templates/
│        ├─ deployment.yaml
│        ├─ service.yaml
│        └─ hpa.yaml
├─ k8s/
│  ├─ prometheus-recording-rules.yaml
│  └─ alloy-values.yaml
├─ src/
│  └─ main/
│     ├─ java/com/example/observability/
│     │  ├─ DemoApplication.java
│     │  ├─ config/MetricsConfiguration.java
│     │  ├─ metrics/ApiMetrics.java
│     │  ├─ metrics/ApiLatencyMetrics.java
│     │  ├─ web/MetricsFilter.java
│     │  ├─ web/ApiMetricsAspect.java
│     │  └─ webflux/MetricsWebFluxFilter.java
│     └─ resources/
│        └─ application.yml
```

---

## README.md
```markdown
# observability-demo

This repository contains a Spring Boot 3.5 application instrumented with Micrometer and configured to export metrics via OTLP to a Grafana Alloy (OpenTelemetry) Collector. It also includes a Helm chart and Skaffold configuration for deployment to EKS (or any Kubernetes cluster).

## What is included

- Spring Boot app with Micrometer OTLP exporter and custom metrics (counters, timers, distribution summaries).
- Filter, AOP and WebFlux instrumentation examples.
- Dockerfile to build the application image.
- Helm chart (`charts/observability-demo`) for deploying the application.
- Skaffold configuration to build and deploy via Helm.
- Example Grafana Alloy `alloy-values.yaml` for running the Alloy collector on EKS.
- Prometheus recording rules and burn-rate alert examples.

## How to use (quick start)

1. Build and push the Docker image to your registry:

```bash
mvn -DskipTests package
docker build -t <registry>/observability-demo:latest .
docker push <registry>/observability-demo:latest
```

2. Install Grafana Alloy (OTel collector) into your cluster (see `k8s/alloy-values.yaml`).

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
kubectl create namespace observability
helm install alloy grafana/alloy -n observability -f k8s/alloy-values.yaml
```

3. Deploy the app using Skaffold (or Helm directly):

```bash
skaffold run -p prod
# or
helm upgrade --install observability-demo charts/observability-demo -n default -f charts/observability-demo/values.yaml
```

4. Import Grafana dashboards (provided separately) and verify metrics appear.

## Notes
- Update `application.yml` OTLP endpoint to point to your Alloy collector service.
- Templated Helm values expose `otel.endpoint` that is injected into the deployment.

```
```

---

## pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>observability-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.5.0</spring-boot.version>
        <micrometer.version>1.13.0</micrometer.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-otlp</artifactId>
            <version>${micrometer.version}</version>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Dockerfile
```dockerfile
FROM eclipse-temurin:17-jdk-alpine
ARG JAR_FILE=target/observability-demo-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

## skaffold.yaml
```yaml
apiVersion: skaffold/v4
kind: Config
metadata:
  name: observability-demo
build:
  artifacts:
    - image: observability-demo
      context: .
      docker:
        dockerfile: Dockerfile
deploy:
  helm:
    releases:
      - name: observability-demo
        chartPath: charts/observability-demo
        namespace: default
        valuesFiles:
          - charts/observability-demo/values.yaml
profiles:
  - name: prod
    deploy:
      helm:
        releases:
          - name: observability-demo
            chartPath: charts/observability-demo
            namespace: default
            valuesFiles:
              - charts/observability-demo/values.yaml
```

---

## Helm chart

### charts/observability-demo/Chart.yaml
```yaml
apiVersion: v2
name: observability-demo
description: Observability demo app
type: application
version: 0.1.0
appVersion: "0.0.1"
```

### charts/observability-demo/values.yaml
```yaml
replicaCount: 2
image:
  repository: observability-demo
  tag: latest
  pullPolicy: IfNotPresent
service:
  port: 80
otel:
  endpoint: "http://otel-collector-alloy.observability:4317"
resources: {}
```

### charts/observability-demo/templates/deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "observability-demo.fullname" . }}
  labels:
    app: {{ include "observability-demo.name" . }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      app: {{ include "observability-demo.name" . }}
  template:
    metadata:
      labels:
        app: {{ include "observability-demo.name" . }}
    spec:
      containers:
        - name: {{ include "observability-demo.name" . }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          env:
            - name: OTEL_EXPORTER_OTLP_ENDPOINT
              value: "{{ .Values.otel.endpoint }}"
            - name: OTEL_EXPORTER_OTLP_PROTOCOL
              value: "grpc"
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
```

### charts/observability-demo/templates/service.yaml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "observability-demo.fullname" . }}
spec:
  selector:
    app: {{ include "observability-demo.name" . }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: 8080
```

### charts/observability-demo/templates/hpa.yaml
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {{ include "observability-demo.fullname" . }}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ include "observability-demo.fullname" . }}
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

---

## k8s/alloy-values.yaml
```yaml
replicaCount: 2
serviceAccount:
  create: true
alloy:
  receiver:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318
  prometheus:
    enabled: true
    scrapeConfigs:
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
          - role: pod
        relabel_configs:
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: true
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
            target_label: __metrics_path__
            regex: (.+)
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
            target_label: __address__
            replacement: $1
exporters:
  prometheusremotewrite:
    - url: "https://prometheus-remote.example/write"
```

---

## k8s/prometheus-recording-rules.yaml
```yaml
groups:
- name: api-recording.rules
  rules:
  - record: api_requests_total
    expr: sum(increase(api_requests_success_total[5m]) + increase(api_requests_error_total[5m]))
  - record: api_error_rate_5m
    expr: sum(rate(api_requests_error_total[5m])) / sum(rate(api_requests_total[5m]))
  - record: api_latency_p95_5m
    expr: histogram_quantile(0.95, sum(rate(api_request_latency_bucket[5m])) by (le))

- name: slo-alerts
  rules:
  - alert: FastBurn
    expr: (rate(api_requests_error_total[5m]) / rate(api_requests_total[5m])) / 0.01 > 14
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Fast burn: error budget is being consumed quickly"

  - alert: SlowBurn
    expr: (rate(api_requests_error_total[6h]) / rate(api_requests_total[6h])) / 0.01 > 2
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "Slow burn: sustained error rate above threshold"
```

---

## Java source files

### src/main/java/com/example/observability/DemoApplication.java
```java
package com.example.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### src/main/java/com/example/observability/config/MetricsConfiguration.java
```java
package com.example.observability.config;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    @Bean
    public Counter successCounter(MeterRegistry registry) {
        return Counter.builder("api.requests.success")
                .description("Count of successful API requests")
                .register(registry);
    }

    @Bean
    public Counter errorCounter(MeterRegistry registry) {
        return Counter.builder("api.requests.error")
                .description("Count of failed API requests")
                .register(registry);
    }

    @Bean
    public Timer requestLatencyTimer(MeterRegistry registry) {
        return Timer.builder("api.request.latency")
                .description("API request latency")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }
}
```

### src/main/java/com/example/observability/metrics/ApiMetrics.java
```java
package com.example.observability.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

@Component
public class ApiMetrics {

    private final MeterRegistry registry;

    public ApiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incrementSuccess(String endpoint, String tenant) {
        registry.counter("api.requests.success", "endpoint", endpoint, "tenant", tenant).increment();
    }

    public void incrementError(String endpoint, String tenant) {
        registry.counter("api.requests.error", "endpoint", endpoint, "tenant", tenant).increment();
    }

    public void recordPayloadSize(long bytes) {
        registry.summary("api.payload.size", "unit", "bytes").record(bytes);
    }
}
```

### src/main/java/com/example/observability/metrics/ApiLatencyMetrics.java
```java
package com.example.observability.metrics;

import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class ApiLatencyMetrics {
    private final Timer timer;

    public ApiLatencyMetrics(Timer timer) {
        this.timer = timer;
    }

    public <T> T recordCallable(Callable<T> callable) throws Exception {
        return timer.recordCallable(callable);
    }

    public void recordRunnable(Runnable runnable) {
        timer.record(runnable);
    }
}
```

### src/main/java/com/example/observability/web/MetricsFilter.java
```java
package com.example.observability.web;

import com.example.observability.metrics.ApiMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsFilter implements Filter {

    private final ApiMetrics apiMetrics;
    private final Timer latency;

    public MetricsFilter(ApiMetrics apiMetrics, Timer latency) {
        this.apiMetrics = apiMetrics;
        this.latency = latency;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = System.nanoTime();
        HttpServletRequest req = (HttpServletRequest) request;
        String endpoint = req.getRequestURI();
        try {
            chain.doFilter(request, response);
            apiMetrics.incrementSuccess(endpoint, "unknown");
        } catch (Exception ex) {
            apiMetrics.incrementError(endpoint, "unknown");
            throw ex;
        } finally {
            long elapsed = System.nanoTime() - start;
            latency.record(elapsed, TimeUnit.NANOSECONDS);
        }
    }
}
```

### src/main/java/com/example/observability/web/ApiMetricsAspect.java
```java
package com.example.observability.web;

import com.example.observability.metrics.ApiMetrics;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiMetricsAspect {

    private final ApiMetrics metrics;
    private final Timer latency;

    public ApiMetricsAspect(ApiMetrics metrics, Timer latency) {
        this.metrics = metrics;
        this.latency = latency;
    }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        return latency.recordCallable(() -> {
            try {
                Object res = pjp.proceed();
                metrics.incrementSuccess(pjp.getSignature().toShortString(), "unknown");
                return res;
            } catch (Exception e) {
                metrics.incrementError(pjp.getSignature().toShortString(), "unknown");
                throw e;
            }
        });
    }
}
```

### src/main/java/com/example/observability/webflux/MetricsWebFluxFilter.java
```java
package com.example.observability.webflux;

import com.example.observability.metrics.ApiMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
public class MetricsWebFluxFilter implements WebFilter {

    private final ApiMetrics metrics;
    private final Timer latency;

    public MetricsWebFluxFilter(ApiMetrics metrics, Timer latency) {
        this.metrics = metrics;
        this.latency = latency;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long start = System.nanoTime();
        String endpoint = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange)
                .doOnSuccess(v -> metrics.incrementSuccess(endpoint, "unknown"))
                .doOnError(e -> metrics.incrementError(endpoint, "unknown"))
                .doFinally(sig -> latency.record(System.nanoTime() - start, TimeUnit.NANOSECONDS));
    }
}
```

---

## src/main/resources/application.yml
```yaml
spring:
  application:
    name: observability-demo

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    prometheus:
      enabled: true
  endpoints:
    web:
      base-path: /actuator

management.otlp.metrics.export:
  enabled: true
  endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://otel-collector-alloy.observability:4317}
  timeout: 10s

management.otlp.resource:
  service.name: ${spring.application.name}
  service.instance.id: ${HOSTNAME:unknown}
```

---

## Next steps / how I can help further

- I can produce a GitHub repository (zip or push to a repo you own) with these files already created.
- I can expand the Helm chart to include PrometheusServiceMonitor, PodAnnotations for scraping, Grafana dashboards as config maps, and Alertmanager rules.
- I can add CI/CD pipeline (GitHub Actions) that builds, pushes, and deploys using Skaffold/Helm.

If you want the repository zipped or pushed to an existing GitHub repository, provide the target repo or tell me to produce a downloadable zip and I will create it.




Key Concepts and Their Importance

SLI (Service Level Indicator)

A quantitative measure of service performance (e.g., request success rate, latency).

Importance: Provides the objective data needed to understand how healthy a service is. Without SLIs, reliability discussions are subjective.

SLO (Service Level Objective)

A target value or acceptable range for an SLI over a defined period (e.g., 95% of requests succeed within 1 minute).

Importance: Acts as the internal benchmark for engineering and product teams to align reliability goals with business needs. SLOs are flexible, iterative, and help prioritize improvements.

SLA (Service Level Agreement)

A formal contract with customers that defines promised service levels and remedies if not met (e.g., credits, discounts).

Importance: Builds trust and accountability with customers. SLAs are external-facing, while SLOs/SLIs are internal tools that inform them.

Error Budget

The allowable margin of failure between 100% reliability and the SLO target (e.g., if SLO is 99%, error budget is 1%).

Importance: Provides a practical balance between innovation and reliability. Teams can release new features as long as they stay within the error budget, ensuring reliability without stifling progress.

🌟 Why These Matter Together

SLIs → provide the measurements.

SLOs → set the targets based on those measurements.

SLAs → communicate commitments externally.

Error Budgets → enforce realistic reliability trade-offs.

Together, they create a framework for reliability management:

Align business and technical teams.

Prevent unrealistic “100% reliability” goals.

Enable proactive monitoring and faster incident response.

Support continuous improvement and customer satisfaction.


https://aws.amazon.com/blogs/mt/improve-application-reliability-with-effective-slos/


https://www.datadoghq.com/blog/burn-rate-is-better-error-rate/


https://sre.google/sre-book/table-of-contents/

