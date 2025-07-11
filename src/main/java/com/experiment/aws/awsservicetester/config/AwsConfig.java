package com.experiment.aws.awsservicetester.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.textract.TextractClient;

@Configuration
public class AwsConfig {
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.US_EAST_1) // Change as needed
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
    
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.US_EAST_1) // Change as needed
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();	
    }
    
    @Bean
    public TextractClient textractClient() {
        return TextractClient.builder()
                .region(Region.US_EAST_1) // Change to your region
                .build();
    }	
}