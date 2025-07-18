package com.experiment.aws.awsservicetester.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.experiment.aws.awsservicetester.models.FileMetadataResponse;
import com.experiment.aws.awsservicetester.service.FileMetadataService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata/files")
public class FileMetadataController {
	
	private static final Logger log = LoggerFactory.getLogger(FileMetadataController.class);
    private final FileMetadataService fileMetadataService;

    public FileMetadataController(FileMetadataService fileMetadataService) {
        this.fileMetadataService = fileMetadataService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            
            log.info("Received file upload: {}, size: {}", file.getOriginalFilename(), file.getSize());
            
            Map<Integer, Map<String, Object>> metadata = fileMetadataService.extractMetadata(file);
            
            // Build response using the model class
            FileMetadataResponse response = FileMetadataResponse.builder()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .totalSize(file.getSize())
                .pageCount(metadata.size())
                .pages(metadata)
                .processedAt(ZonedDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
            
            return ResponseEntity.ok(response);
        } catch (UnsupportedOperationException e) {
            log.warn("Unsupported file type", e);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(null);
        } catch (Exception e) {
            log.error("Error processing file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.warn("File size limit exceeded", e);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of(
                    "error", "File size limit exceeded",
                    "message", "Maximum allowed file size is defined in application properties"
                ));
    }
}
