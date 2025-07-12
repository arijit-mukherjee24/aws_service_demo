package com.experiment.aws.awsservicetester.controller;

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
            return bedrockService.invokeBedrockModel(prompt);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
