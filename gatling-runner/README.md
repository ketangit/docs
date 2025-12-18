## Gatling Runner

Lightweight Spring Boot web application that provides an HTMX-driven UI to launch Gatling simulations in separate worker pods. The web JVM does not include Gatling dependencies. Gatling runs in worker pods that execute the Gatling distribution and upload results to S3.

### Features

- Spring Boot 3.5.1 and Java 21
- No Gatling dependencies in the web JVM
- HTMX-driven UI with server-rendered HTML fragments returned directly from controllers
- Worker pod model: Gatling runs outside the web JVM via a Gatling worker image
- Kubernetes Jobs spawn worker pods; scenarios are provided via ConfigMap
- Worker uploads Gatling results to S3 and writes a DONE marker
- Live log polling via HTMX
- Use S3 via AWS API for Gatling HTML report viewing


### Build and run locally

### Build image
mvn -DskipTests clean package
docker build -t example/gatling-runner:local .
docker run --rm -it --entrypoint "/busybox/sh" example/gatling-runner:local

### Run web app locally for testing
docker run --rm -e MODE=web -p 8080:8080 example/gatling-runner:local

### Run worker locally for testing
docker run --rm -e MODE=worker -e RUN_ID=test-123 -e SCENARIO=MySimulation \
  -e S3_BUCKET= -v $(pwd)/scenarios:/etc/gatling/scenarios \
  -v $(pwd)/results:/var/gatling/results example/gatling-runner:local
