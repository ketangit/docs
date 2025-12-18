## Used below prompt to generate gatling-runner spring-boot java application

Create a spring-boot version 3.5.1 application using jaav21 for Gatling Runner & Viewer
- no Thymeleaf dependency
- no Gatling dependencies in pom.xml
- use pure htmx + server-rendered HTML fragments only. Spring MVC returns plain HTML strings/fragments, not template views.
- Dynamic HTML fragments returned directly from controllers
- generate production ready source code, ready for deployment on AWS EKS
- htmx handles, Scenario loading, Form submission, Polling for execution output
- Gatling is heavy, uses Scala, and pulls many transitive dependencies. Keeping it outside the web JVM
- A multi-stage Dockerfile (Spring Boot + Gatling)
- A worker-pod execution model
Incremental UI updates
- The application provides:
    - Spring Boot does not compile, load, or run Gatling classes
    - Gatling scenario execution from UI (htmx) via external process (gatling.sh)
    - Scenario definitions loaded from Kubernetes ConfigMap
    - Results are written to S3
    - Live execution output via htmx polling
    - S3-backed Gatling HTML report viewer
    - All Gatling Simulations are packaged with the Gatling distribution or mounted volume
    - Kubernetes-ready deployment
- Use Kubernetes Jobs/CronJobs to spawn workers; ensure pod lifecycle, resource limits, node selectors, and tolerations are set.
- No need for Kubernetes manifests, Helm chart, CI/CD pipelines, IAM role details
