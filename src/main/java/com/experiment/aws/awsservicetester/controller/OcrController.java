package com.experiment.aws.awsservicetester.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.experiment.aws.awsservicetester.models.OcrJobResult;
import com.experiment.aws.awsservicetester.service.OcrService;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {
	
	private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }
    
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startOcrProcess(
            @RequestParam String bucket,
            @RequestParam String key
    ) {
        try {
            // Start the OCR job
            String jobId = ocrService.startOcrProcessing(bucket, key);
            
            // Return the job ID for polling
            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "OCR processing started. Use the jobId to poll for results.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/results/{jobId}")
    public ResponseEntity<?> getOcrResults(
            @PathVariable String jobId,
            @RequestParam(required = false) List<Integer> pages
    ) {
        try {
            // Get OCR results with optional page filtering
            OcrJobResult results = ocrService.getOcrResults(jobId, pages);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
