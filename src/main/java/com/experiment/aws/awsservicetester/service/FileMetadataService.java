package com.experiment.aws.awsservicetester.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class FileMetadataService {

	private static final Logger log = LoggerFactory.getLogger(FileMetadataService.class);
    private final Detector detector = new DefaultDetector();

    /**
     * Extract metadata from various file types
     * @param file The uploaded file
     * @return Map of page number to metadata for each page
     */
    public Map<Integer, Map<String, Object>> extractMetadata(MultipartFile file) throws Exception {
        Map<Integer, Map<String, Object>> pageMetadata = new LinkedHashMap<>();
        
        // Get the file bytes once to avoid multiple stream operations
        byte[] fileBytes = file.getBytes();
        
        // Use Tika for more accurate content type detection
        String detectedMimeType = detectMimeType(fileBytes);
        String fileName = file.getOriginalFilename();
        
        log.info("Processing file: {}, detected MIME type: {}", fileName, detectedMimeType);
        
        if (detectedMimeType.equals("application/pdf")) {
            extractPdfMetadata(fileBytes, pageMetadata, file.getSize());
        } else if (detectedMimeType.startsWith("image/tiff")) {
            extractTiffMetadata(fileBytes, pageMetadata, file.getSize());
        } else if (detectedMimeType.startsWith("image/")) {
            extractImageMetadata(fileBytes, pageMetadata, file.getSize(), detectedMimeType);
        } else {
            throw new UnsupportedOperationException("Unsupported file type: " + detectedMimeType);
        }
        
        return pageMetadata;
    }

    /**
     * Detect MIME type using Apache Tika
     */
    private String detectMimeType(byte[] fileBytes) throws IOException {
        try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(fileBytes))) {
            Metadata metadata = new Metadata();
            MediaType mediaType = detector.detect(is, metadata);
            return mediaType.toString();
        }
    }

    /**
     * Extract PDF metadata using PDFBox
     */
    private void extractPdfMetadata(byte[] fileBytes, Map<Integer, Map<String, Object>> pageMetadata, long fileSize) throws IOException {
        try (PDDocument document = PDDocument.load(fileBytes)) {
            int pageCount = document.getNumberOfPages();
            
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                
                Map<String, Object> meta = new HashMap<>();
                meta.put("width", (int) mediaBox.getWidth());
                meta.put("height", (int) mediaBox.getHeight());
                meta.put("type", "pdf");
                meta.put("size", fileSize / pageCount); // Approximate size per page
                meta.put("dpi", 72); // Default PDF resolution
                
             // Method 1: Direct rotation property
                int primaryRotation = page.getRotation();
                
                // Method 2: Calculate rotation from page's transformation matrix
                int calculatedRotation = 0;
                try {
                    // Some PDFs encode rotation in the CTM (Current Transformation Matrix)
                    PDPageContentStream contentStream = new PDPageContentStream(document, page, 
                            PDPageContentStream.AppendMode.APPEND, true, true);
                    contentStream.close();
                    
                    // Get the page dictionary
                    COSDictionary pageDict = page.getCOSObject();
                    
                    // Check if there are additional rotation hints in the dictionary
                    if (pageDict.containsKey(COSName.ROTATE)) {
                        int dictRotation = pageDict.getInt(COSName.ROTATE);
                        log.debug("Found rotation hint in page dictionary: {}", dictRotation);
                        calculatedRotation = dictRotation;
                    }
                } catch (Exception e) {
                    log.debug("Could not determine rotation from transformation matrix: {}", e.getMessage());
                }
                
                // Choose the most reliable rotation value
                int rotation = primaryRotation;
                if (rotation == 0 && calculatedRotation != 0) {
                    rotation = calculatedRotation;
                }
                
                // Normalize to 0, 90, 180, 270
                rotation = ((rotation % 360) + 360) % 360;
                
                // Only allow standard rotations
                if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
                    // Round to nearest 90 degrees
                    rotation = Math.round(rotation / 90.0f) * 90;
                    if (rotation == 360) rotation = 0;
                }
                
                log.debug("Page {} rotation - Direct: {}, Calculated: {}, Final: {}", 
                          (i+1), primaryRotation, calculatedRotation, rotation);
                
                meta.put("rotation", rotation);
                
                pageMetadata.put(i + 1, meta);
            }
        }
    }

    /**
     * Extract TIFF metadata using Apache Commons Imaging
     */
    private void extractTiffMetadata(byte[] fileBytes, Map<Integer, Map<String, Object>> pageMetadata, long fileSize) throws Exception {
        try {
            // Get basic image info
            ImageInfo info = Imaging.getImageInfo(fileBytes);
            
            // Try to get TIFF-specific metadata
            ImageMetadata metadata = null;
            try {
                metadata = Imaging.getMetadata(fileBytes);
            } catch (Exception e) {
                log.warn("Could not extract metadata from TIFF", e);
            }
            
            TiffImageMetadata tiffMetadata = metadata instanceof TiffImageMetadata ? 
                                           (TiffImageMetadata) metadata : null;
            
            // Determine page count using TiffImageMetadata
            int pageCount = 1; // Default to 1 page
            
            if (tiffMetadata != null) {
                try {
                    // Try to get the number of directories from TIFF metadata
                    List<?> directories = tiffMetadata.getDirectories();
                    if (directories != null && !directories.isEmpty()) {
                        pageCount = directories.size();
                        log.debug("Found {} pages in TIFF from metadata directories", pageCount);
                    }
                } catch (Exception e) {
                    log.warn("Could not determine page count from TIFF metadata directories", e);
                    
                    // Try an alternative approach using fields
                    try {
                        // Check for TIFF_TAG_PAGE_NUMBER which might have total page count
                        TiffField pageNumberField = tiffMetadata.findField(TiffTagConstants.TIFF_TAG_PAGE_NUMBER);
                        if (pageNumberField != null && pageNumberField.getCount() >= 2) {
                            // Second value in this field is usually total pages
                            int[] values = pageNumberField.getIntArrayValue();
                            if (values.length >= 2 && values[1] > 0) {
                                pageCount = values[1];
                                log.debug("Found {} pages in TIFF from page number field", pageCount);
                            }
                        }
                    } catch (Exception e2) {
                        log.warn("Could not determine page count from TIFF page number field", e2);
                    }
                }
            }
            
            log.info("Processing TIFF with {} page(s)", pageCount);
            
            for (int i = 0; i < pageCount; i++) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("width", info.getWidth());
                meta.put("height", info.getHeight());
                meta.put("type", "tiff");
                meta.put("size", fileSize / pageCount); // Approximate size per page
                meta.put("bitsPerPixel", info.getBitsPerPixel());
                meta.put("dpi", info.getPhysicalWidthDpi() > 0 ? info.getPhysicalWidthDpi() : 96);
                meta.put("colorType", info.getColorType().name());
                
                // Add page number
                meta.put("pageNumber", i + 1);
                meta.put("totalPages", pageCount);
                
				// Add rotation detection
				int rotation = 0; // Default to 0 degrees
				boolean flipHorizontal = false;
				boolean flipVertical = false;
				if (tiffMetadata != null) {
					try {
						// Try to get orientation from TIFF metadata
						TiffField orientationField = tiffMetadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION);
						if (orientationField != null) {
							int orientation = orientationField.getIntValue();
							// Convert TIFF orientation to degrees
							// 1 = 0°, 3 = 180°, 6 = 90° CW, 8 = 270° CW
							switch (orientation) {
							case 1:
							case 2:
								rotation = 0;
								break;
							case 3:
							case 4:
								rotation = 180;
								break;
							case 5:
							case 6:
								rotation = 90;
								break;
							case 7:
							case 8:
								rotation = 270;
								break;
							default:
								rotation = 0;
								break;
							}
						}
					} catch (Exception e) {
						log.debug("Could not extract orientation information", e);
					}
				}
				meta.put("rotation", rotation);
                
                // If we have TIFF metadata, try to extract compression info
                if (tiffMetadata != null) {
                    try {
                        // Get compression from TIFF tags
                        TiffField compressionField = tiffMetadata.findField(TiffTagConstants.TIFF_TAG_COMPRESSION);
                        if (compressionField != null) {
                            int compressionValue = compressionField.getIntValue();
                            String compressionName = getTiffCompressionName(compressionValue);
                            meta.put("compression", compressionName);
                        } else {
                            meta.put("compression", "unknown");
                        }
                    } catch (Exception e) {
                        log.debug("Could not extract compression information", e);
                        meta.put("compression", "unknown");
                    }
                }
                
                pageMetadata.put(i + 1, meta);
            }
        } catch (Exception e) {
            log.error("Error processing TIFF image", e);
            
            // Create at least basic metadata even if processing fails
            Map<String, Object> meta = new HashMap<>();
            meta.put("type", "tiff");
            meta.put("size", fileSize);
            meta.put("error", "Failed to fully process TIFF: " + e.getMessage());
            meta.put("pageNumber", 1);
            meta.put("totalPages", 1);
            
            pageMetadata.put(1, meta);
        }
    }
    
    /**
     * Convert TIFF compression value to human-readable name
     */
    private String getTiffCompressionName(int compressionValue) {
        switch (compressionValue) {
            case 1: return "Uncompressed";
            case 2: return "CCITT 1D";
            case 3: return "CCITT Group 3";
            case 4: return "CCITT Group 4";
            case 5: return "LZW";
            case 6: return "JPEG (old)";
            case 7: return "JPEG";
            case 8: return "Deflate/Adobe";
            case 9: return "JBIG B&W";
            case 10: return "JBIG Color";
            case 99: return "JPEG";
            case 262: return "Kodak 262";
            case 32773: return "PackBits";
            case 32946: return "Deflate/PKZIP";
            case 34712: return "JPEG 2000";
            default: return "Unknown (" + compressionValue + ")";
        }
    }

    /**
     * Extract metadata from other image formats using Apache Commons Imaging
     */
    private void extractImageMetadata(byte[] fileBytes, Map<Integer, Map<String, Object>> pageMetadata, long fileSize, String mimeType) throws Exception {
        try {
            ImageInfo info = Imaging.getImageInfo(fileBytes);
            
            Map<String, Object> meta = new HashMap<>();
            meta.put("width", info.getWidth());
            meta.put("height", info.getHeight());
            meta.put("type", mimeType.replace("image/", ""));
            meta.put("size", fileSize);
            meta.put("bitsPerPixel", info.getBitsPerPixel());
            meta.put("dpi", info.getPhysicalWidthDpi() > 0 ? info.getPhysicalWidthDpi() : 96);
            meta.put("colorType", info.getColorType().name());
            
            // Add rotation detection
            int rotation = 0; // Default to 0 degrees
            try {
                ImageMetadata metadata = Imaging.getMetadata(fileBytes);
                // For JPEG images with EXIF data
                if (metadata instanceof org.apache.commons.imaging.formats.jpeg.JpegImageMetadata) {
                    org.apache.commons.imaging.formats.jpeg.JpegImageMetadata jpegMetadata = 
                        (org.apache.commons.imaging.formats.jpeg.JpegImageMetadata) metadata;
                    
                    TiffField orientationField = jpegMetadata.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION);
                    if (orientationField != null) {
                        int orientation = orientationField.getIntValue();
                        // Convert EXIF orientation to degrees (same as TIFF)
                        switch (orientation) {
                            case 1: rotation = 0; break;
                            case 3: rotation = 180; break;
                            case 6: rotation = 90; break;
                            case 8: rotation = 270; break;
                            default: rotation = 0; break;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("No rotation metadata available", e);
            }
            meta.put("rotation", rotation);
            
            // Check for embedded metadata
            try {
                ImageMetadata metadata = Imaging.getMetadata(fileBytes);
                if (metadata != null) {
                    meta.put("hasMetadata", true);
                }
            } catch (Exception e) {
                log.debug("No extended metadata available");
            }
            
            pageMetadata.put(1, meta);
        } catch (ImageReadException e) {
            log.error("Error reading image", e);
            throw new IOException("Failed to process image file", e);
        }
    }
}
