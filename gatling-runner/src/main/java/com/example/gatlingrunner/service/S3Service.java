package com.example.gatlingrunner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class S3Service {

  private final S3Client s3;
  private final String bucket;
  private final Region region;

  public S3Service(@Value("${app.s3.bucket}") String bucket,
                   @Value("${app.s3.region:us-east-1}") String regionName) {
    this.bucket = bucket;
    this.region = Region.of(regionName);
    this.s3 = S3Client.builder()
        .region(this.region)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }

  public String presignReportUrl(String runId) {
    String key = runId + "/index.html";
    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();
      return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region.id(), key);
    } catch (Exception e) {
      return "Unable to generate report URL: " + e.getMessage();
    }
  }

  public void uploadFile(String key, byte[] content, String contentType) {
    PutObjectRequest req = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(contentType)
        .build();
    s3.putObject(req, RequestBody.fromBytes(content));
  }
}
