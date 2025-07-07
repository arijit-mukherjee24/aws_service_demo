package com.experiment.aws.awsservicetester.controller;

import com.experiment.aws.awsservicetester.service.S3Service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/document")
    public ResponseEntity<byte[]> fetchDocument(
            @RequestParam String bucket,
            @RequestParam String key) {
        try {
            byte[] content = s3Service.fetchDocument(bucket, key);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/presigned-url")
    public ResponseEntity<String> getPresignedUrl(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam(defaultValue = "15") int expiryMinutes) {
        try {
            String presignedUrl = s3Service.generatePresignedUrl(bucket, key, expiryMinutes);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to generate presigned URL: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam("file") MultipartFile file) {
        try {
            s3Service.uploadDocument(bucket, key, file);
            return ResponseEntity.ok("File uploaded successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }  
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listObjectsWithMetadata(
            @RequestParam String bucket) {
        try {
            List<Map<String, Object>> result = s3Service.listAllObjectsWithMetadata(bucket);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}