package com.experiment.aws.awsservicetester.models;

import java.util.ArrayList;
import java.util.List;

public class PageResult {
	public int page;
    public String text;
    public List<LineInfo> lines;
    
    public PageResult(int page, String text) {
        this.page = page;
        this.text = text;
        this.lines = new ArrayList<>();
    }
}
