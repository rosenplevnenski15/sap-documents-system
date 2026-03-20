package com.sap.documentssystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

@Configuration
@Slf4j
public class S3Config {

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {


        return S3Client.builder()
                .region(Region.of(region))


                .credentialsProvider(DefaultCredentialsProvider.create())


                .httpClientBuilder(
                        UrlConnectionHttpClient.builder()
                )


                .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .apiCallTimeout(Duration.ofSeconds(10))
                                .apiCallAttemptTimeout(Duration.ofSeconds(5))
                                .build()
                )

                .build();
    }
}