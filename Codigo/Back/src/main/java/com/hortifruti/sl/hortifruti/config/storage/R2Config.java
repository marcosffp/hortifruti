package com.hortifruti.sl.hortifruti.config.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class R2Config {

  @Value("${r2.access-key-id}")
  private String accessKeyId;

  @Value("${r2.secret-access-key}")
  private String secretAccessKey;

  @Value("${r2.endpoint}")
  private String endpoint;

  @Bean
  public S3Client r2Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of("auto"))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
