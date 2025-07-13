package com.experiment.aws.awsservicetester.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.experiment.aws.awsservicetester.service.BedrockService;

@RestController
@RequestMapping("/api/bedrock")
public class BedrockController {

	private final BedrockService bedrockService;

    public BedrockController(BedrockService bedrockService) {
        this.bedrockService = bedrockService;
    }

    @PostMapping("/playground")
    public String playground(@RequestParam String prompt) {
        try {
            return bedrockService.getModelResponse(prompt);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @PostMapping("/sentiment")
    public ResponseEntity<Map<String, String>> sentiment(@RequestParam String text) {
        try {
            String sentiment = bedrockService.analyzeSentiment(text);
            return ResponseEntity.ok(Map.of(
                "text", text,
                "sentiment", sentiment
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "text", text,
                "sentiment", "error: " + e.getMessage()
            ));
        }
    }
}
