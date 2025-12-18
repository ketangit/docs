# Runtime stage: Distroless Java 21 Debian 12
FROM eclipse-temurin:21-jre-jammy

# Update the package lists and install the 'unzip' command
# The 'rm -rf /var/lib/apt/lists/*' part helps keep the image size down
RUN apt-get update && apt-get install -y unzip dos2unix --no-install-recommends && rm -rf /var/lib/apt/lists/*

# Download and unpack Gatling distribution
ENV GATLING_VERSION=3.10.5
RUN curl -L --retry 3 \
      https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/${GATLING_VERSION}/gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip \
      -o /tmp/gatling.zip \
    && unzip /tmp/gatling.zip -d /opt \
    && mv /opt/gatling-charts-highcharts-bundle-${GATLING_VERSION} /opt/gatling \
    && rm /tmp/gatling.zip

# Copy the built spring-boot jar from builder stage
COPY target/gatling-runner-*.jar /opt/app.jar

ENV GATLING_HOME=/opt/gatling

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN dos2unix /entrypoint.sh
RUN chmod +x /entrypoint.sh

WORKDIR /opt

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]