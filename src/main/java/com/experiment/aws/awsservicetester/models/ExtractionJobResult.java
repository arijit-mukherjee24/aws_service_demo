package com.experiment.aws.awsservicetester.models;

import java.util.Map;

public class ExtractionJobResult {
	public String status; // e.g. "IN_PROGRESS", "SUCCEEDED", "FAILED"
    public Map<String, String> fields; // The extracted fields, if available
    public String error; // Error message, if any

    public ExtractionJobResult(String status, Map<String, String> fields) {
        this.status = status;
        this.fields = fields;
        this.error = null;
    }

    public ExtractionJobResult(String status, Map<String, String> fields, String error) {
        this.status = status;
        this.fields = fields;
        this.error = error;
    }
}
