package com.experiment.aws.awsservicetester.models;

import java.util.Map;
import java.util.Objects;

public class FileMetadataResponse {
    private String filename;
    private String contentType;
    private long totalSize;
    private int pageCount;
    private Map<Integer, Map<String, Object>> pages;
    private String processedAt;

    // Default constructor
    public FileMetadataResponse() {
    }

    // All-args constructor
    public FileMetadataResponse(String filename, String contentType, long totalSize, 
                             int pageCount, Map<Integer, Map<String, Object>> pages, 
                             String processedAt) {
        this.filename = filename;
        this.contentType = contentType;
        this.totalSize = totalSize;
        this.pageCount = pageCount;
        this.pages = pages;
        this.processedAt = processedAt;
    }

    // Getters and setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public Map<Integer, Map<String, Object>> getPages() {
        return pages;
    }

    public void setPages(Map<Integer, Map<String, Object>> pages) {
        this.pages = pages;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
    
    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }
    
    // equals, hashCode, and toString methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMetadataResponse that = (FileMetadataResponse) o;
        return totalSize == that.totalSize &&
                pageCount == that.pageCount &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(pages, that.pages) &&
                Objects.equals(processedAt, that.processedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, contentType, totalSize, pageCount, pages, processedAt);
    }

    @Override
    public String toString() {
        return "FileUploadResponse{" +
                "filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", totalSize=" + totalSize +
                ", pageCount=" + pageCount +
                ", pages=" + pages +
                ", processedAt='" + processedAt + '\'' +
                '}';
    }
    
    public static class Builder {
        private String filename;
        private String contentType;
        private long totalSize;
        private int pageCount;
        private Map<Integer, Map<String, Object>> pages;
        private String processedAt;
        
        private Builder() {
        }
        
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }
        
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder totalSize(long totalSize) {
            this.totalSize = totalSize;
            return this;
        }
        
        public Builder pageCount(int pageCount) {
            this.pageCount = pageCount;
            return this;
        }
        
        public Builder pages(Map<Integer, Map<String, Object>> pages) {
            this.pages = pages;
            return this;
        }
        
        public Builder processedAt(String processedAt) {
            this.processedAt = processedAt;
            return this;
        }
        
        public FileMetadataResponse build() {
            return new FileMetadataResponse(filename, contentType, totalSize, pageCount, pages, processedAt);
        }
    }
}