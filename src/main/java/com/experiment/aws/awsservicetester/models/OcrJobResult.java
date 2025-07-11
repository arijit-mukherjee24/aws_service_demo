package com.experiment.aws.awsservicetester.models;

import java.util.List;

public class OcrJobResult {
	 public String status;
     public List<PageResult> results;
     
     public OcrJobResult(String status, List<PageResult> results) {
         this.status = status;
         this.results = results;
     }
}
