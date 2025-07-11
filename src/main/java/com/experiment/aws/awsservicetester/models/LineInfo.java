package com.experiment.aws.awsservicetester.models;

public class LineInfo {
	public String id;
    public String text;
    public float confidence;
    
    public LineInfo(String id, String text, Float confidence) {
        this.id = id;
        this.text = text;
        this.confidence = confidence != null ? confidence : 0;
    }
}
