package com.experiment.aws.awsservicetester.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.json.JSONObject;

import com.experiment.aws.awsservicetester.models.ExtractionJobResult;
import com.experiment.aws.awsservicetester.models.OcrJobResult;

@Service
public class ExtractionService {

    private final OcrService ocrService;
    private final BedrockService bedrockService;
    
    // In-memory store for jobs
    private final Map<String, ExtractionJobResult> jobStore = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ExtractionService(OcrService ocrService, BedrockService bedrockService) {
        this.ocrService = ocrService;
        this.bedrockService = bedrockService;
    }
    
    /**
     * Starts an asynchronous extraction job for a given S3 document and specified fields.
     * Generates a unique jobId, marks the job as IN_PROGRESS, and launches extraction in a background thread.
     * Returns the jobId for polling status/results.
     */
    public String startExtractionJob(String bucket, String key, String fields) {
        String jobId = UUID.randomUUID().toString();
        jobStore.put(jobId, new ExtractionJobResult("IN_PROGRESS", null));
        executor.submit(() -> runExtraction(jobId, bucket, key, fields));
        return jobId;
    }
    
    /**
     * Returns the status and result of an extraction job by jobId.
     * If the job is not found, returns a NOT_FOUND status with an error message.
     */
    public ExtractionJobResult getExtractionResult(String jobId) {
        ExtractionJobResult result = jobStore.get(jobId);
        if (result == null) {
            return new ExtractionJobResult("NOT_FOUND", null, "Job ID not found");
        }
        return result;
    }

    /**
     * The core extraction logic that runs asynchronously:
     * 1. Starts OCR on the S3 document.
     * 2. Polls for OCR completion.
     * 3. Aggregates OCR text from all pages.
     * 4. Builds a prompt for Bedrock LLM to extract fields.
     * 5. Parses and stores the extracted fields in the job store.
     * Handles and records failures if any step fails.
     */
    private void runExtraction(String jobId, String bucket, String key, String fields) {
        try {
            // 1. Start OCR
            String ocrJobId = ocrService.startOcrProcessing(bucket, key);

            // 2. Poll for OCR result (blocking)
            OcrJobResult ocrResult = pollForOcrResult(ocrJobId);

            if (!"SUCCEEDED".equals(ocrResult.status)) {
                jobStore.put(jobId, new ExtractionJobResult("FAILED", null, "OCR failed: " + ocrResult.status));
                return;
            }

            // 3. Aggregate text
            StringBuilder fullText = new StringBuilder();
            if (ocrResult.results != null) {
                for (var page : ocrResult.results) {
                    fullText.append(page.text).append("\n");
                }
            }

            // 4. Build prompt & Bedrock extraction
            String prompt = buildExtractionPrompt(fullText.toString(), fields);
            String bedrockResponse = bedrockService.getModelResponse(prompt);

            // 5. Parse extracted fields
            Map<String, String> extractedFields = parseBedrockFields(bedrockResponse);

            jobStore.put(jobId, new ExtractionJobResult("SUCCEEDED", extractedFields));
        } catch (Exception e) {
            jobStore.put(jobId, new ExtractionJobResult("FAILED", null, e.getMessage()));
        }
    }

    /**
     * Repeatedly polls the OCR job status until it completes (SUCCEEDED/FAILED) or times out.
     * Throws an exception if the job does not finish in the given attempts.
     */
    private OcrJobResult pollForOcrResult(String jobId) {
        int maxAttempts = 30;
        int waitMillis = 1000;
        for (int i = 0; i < maxAttempts; i++) {
            OcrJobResult result = ocrService.getOcrResults(jobId, null);
            if ("SUCCEEDED".equals(result.status) || "FAILED".equals(result.status)) {
                return result;
            }
            try { Thread.sleep(waitMillis); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("OCR job timed out");
    }

    /**
     * Constructs a prompt string for the LLM to extract the specified fields from OCR text.
     * Defaults to extracting name, date_of_birth, address if fields are not provided.
     */
    private String buildExtractionPrompt(String ocrText, String fields) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extract the following fields from this document's OCR text. ")
              .append("Reply in JSON format with the fields as keys. ")
              .append("Fields: ")
              .append(fields)
              .append("\n---\nOCR Text:\n")
              .append(ocrText);
        return prompt.toString();
    }

    /**
     * Parses the Bedrock model's response, attempting to extract fields as a Map.
     * Tries JSON parsing first, falls back to simple key-value line parsing if needed.
     */
    private Map<String, String> parseBedrockFields(String response) {
        try {
            JSONObject json = new JSONObject(response);
            Map<String, String> fields = new HashMap<>();
            for (String key : json.keySet()) {
                String cleanKey = cleanValue(key);
                String cleanValueStr = cleanValue(json.getString(key));
                fields.put(cleanKey, cleanValueStr);
            }
            return fields;
        } catch (Exception e) {
            Map<String, String> fields = new HashMap<>();
            String[] lines = response.split("\\r?\\n");
            for (String line : lines) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    String key = cleanValue(line.substring(0, idx).trim());
                    String value = cleanValue(line.substring(idx + 1).trim());
                    fields.put(key, value);
                }
            }
            return fields;
        }
    }
    
    /**
     * Cleans field keys and values by trimming whitespace, quotes, commas, and escaped quotes.
     */
    private String cleanValue(String value) {
        // Remove leading/trailing spaces, quotes, commas, and escaped quotes
        value = value.trim();
        // Remove leading/trailing quotes and commas (including escaped quotes)
        value = value.replaceAll("^[\"']+", "");
        value = value.replaceAll("[\"',]+$", "");
        value = value.replaceAll("\\\\\"", ""); // Remove escaped quotes
        return value.trim();
    }
}