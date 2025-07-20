package com.experiment.aws.awsservicetester.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.experiment.aws.awsservicetester.models.ExtractionJobResult;
import com.experiment.aws.awsservicetester.service.ExtractionService;

@RestController
@RequestMapping("/api/extraction")
public class ExtractionController {

    private final ExtractionService extractionService;

    public ExtractionController(ExtractionService fieldExtractionService) {
        this.extractionService = fieldExtractionService;
    }
    
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startExtraction(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam String fields
    ) {
        try {
            String jobId = extractionService.startExtractionJob(bucket, key, fields);
            if (fields == null || fields.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Field must not be empty or null"));
            }
            return ResponseEntity.ok(Map.of(
                    "jobId", jobId,
                    "message", "Extraction job started. Use the jobId to poll for results."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/results")
    public ResponseEntity<?> getExtractionResult(@RequestParam String jobId) {
        ExtractionJobResult result = extractionService.getExtractionResult(jobId);
        if ("NOT_FOUND".equals(result.status)) {
            return ResponseEntity.status(404).body(Map.of("error", result.error));
        }
        return ResponseEntity.ok(result);
    }
}