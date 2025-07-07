package com.experiment.aws.awsservicetester.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public byte[] fetchDocument(String bucket, String key) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = s3Object.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
    
    public String generatePresignedUrl(String bucket, String key, int expiryMinutes) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .build();

        URL presignedUrl = s3Presigner.presignGetObject(presignRequest).url();
        return presignedUrl.toString();
    }
    
    public void uploadDocument(String bucket, String key, MultipartFile file) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(
            putObjectRequest,
            software.amazon.awssdk.core.sync.RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
    }
    
    public List<Map<String, Object>> listAllObjectsWithMetadata(String bucket) {
    	/*How does it work?
    			Prepare an empty list to store information about each file.

    			Set up a loop:

    			S3 might not return all files at once if there are many, so we loop through "pages" of results using a continuationToken.
    			Inside the loop:

    			Request a "page" of files from S3.
    			For each file in that page:
    			Make a second request to fetch its metadata (like content type).
    			Store all info in a map, and add it to our result list.
    			Check if there are more files:

    			If S3 says there are more files (isTruncated is true), update the token and go for the next batch.
    			Otherwise, stop the loop.
    			Return the list of file info.*/
    	List<Map<String, Object>> result = new ArrayList<>();
        String continuationToken = null;
        boolean isTruncated = true;

        while (isTruncated) {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder().bucket(bucket);
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }
            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            for (S3Object s3Object : response.contents()) {
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Object.key())
                        .build();
                HeadObjectResponse headResponse = s3Client.headObject(headRequest);

                Map<String, Object> objectInfo = new HashMap<>();
                objectInfo.put("key", s3Object.key());
                objectInfo.put("size", s3Object.size());
                objectInfo.put("lastModified", s3Object.lastModified());
                objectInfo.put("eTag", s3Object.eTag());
                objectInfo.put("contentType", headResponse.contentType());
                objectInfo.put("userMetadata", headResponse.metadata());

                result.add(objectInfo);
            }
            continuationToken = response.nextContinuationToken();
            isTruncated = response.isTruncated();
        }

        return result;
    }
}