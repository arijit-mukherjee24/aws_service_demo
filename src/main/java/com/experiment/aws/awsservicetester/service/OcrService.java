package com.experiment.aws.awsservicetester.service;

import org.springframework.stereotype.Service;

import com.experiment.aws.awsservicetester.models.LineInfo;
import com.experiment.aws.awsservicetester.models.OcrJobResult;
import com.experiment.aws.awsservicetester.models.PageResult;

import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;
import java.util.*;

@Service
public class OcrService {

	private final TextractClient textractClient;

	public OcrService(TextractClient textractClient) {
		this.textractClient = textractClient;
	}

	public String startOcrProcessing(String bucket, String key) {
		StartDocumentTextDetectionRequest request = StartDocumentTextDetectionRequest.builder().documentLocation(
				DocumentLocation.builder().s3Object(S3Object.builder().bucket(bucket).name(key).build()).build())
				.build();

		StartDocumentTextDetectionResponse startResponse = textractClient.startDocumentTextDetection(request);
		return startResponse.jobId();
	}

	// Method to check the job status and get results
	public OcrJobResult getOcrResults(String jobId, List<Integer> pages) {
		try {
			GetDocumentTextDetectionRequest getRequest = GetDocumentTextDetectionRequest.builder().jobId(jobId).build();

			GetDocumentTextDetectionResponse response = textractClient.getDocumentTextDetection(getRequest);
			JobStatus status = response.jobStatus();

			// If job is still in progress, return status only
			if (status == JobStatus.IN_PROGRESS) {
				return new OcrJobResult(status.toString(), null);
			}

			// If job failed, return with error
			if (status == JobStatus.FAILED) {
				return new OcrJobResult(status.toString(), null);
			}

			// If job succeeded, process and return results with line info
			if (status == JobStatus.SUCCEEDED) {
				// Maps to store page results and text by page
				Map<Integer, PageResult> pageResultMap = new HashMap<>();
				Map<Integer, StringBuilder> pageTextMap = new HashMap<>();

				// Process all pages
				String nextToken = null;

				while (true) {
					GetDocumentTextDetectionRequest pageRequest = GetDocumentTextDetectionRequest.builder().jobId(jobId)
							.nextToken(nextToken).build();

					response = textractClient.getDocumentTextDetection(pageRequest);

					for (Block block : response.blocks()) {
						if (block.blockType() == BlockType.PAGE) {
							int pageNum = block.page();
							// Skip if page filtering is active and this page is not in the list
							if (pages != null && !pages.isEmpty() && !pages.contains(pageNum)) {
								continue;
							}

							// Create page result if not exists
							if (!pageResultMap.containsKey(pageNum)) {
								pageResultMap.put(pageNum, new PageResult(pageNum, ""));
								pageTextMap.put(pageNum, new StringBuilder());
							}
						} else if (block.blockType() == BlockType.LINE) {
							int pageNum = block.page();
							// Skip if page filtering is active and this page is not in the list
							if (pages != null && !pages.isEmpty() && !pages.contains(pageNum)) {
								continue;
							}

							// Create page result if not exists
							if (!pageResultMap.containsKey(pageNum)) {
								pageResultMap.put(pageNum, new PageResult(pageNum, ""));
								pageTextMap.put(pageNum, new StringBuilder());
							}

							// Add line to page result (without geometry)
							PageResult pageResult = pageResultMap.get(pageNum);
							LineInfo lineInfo = new LineInfo(block.id(), block.text(), block.confidence());

							// Add line text to page text
							pageTextMap.get(pageNum).append(block.text()).append("\n");
							pageResult.lines.add(lineInfo);
						}
						// We're not processing WORD blocks
					}

					// Get next token before we break out of the loop
					nextToken = response.nextToken();
					if (nextToken == null) {
						break;
					}
				}

				// Set the text for each page result and create the final list
				List<PageResult> resultList = new ArrayList<>();
				for (Map.Entry<Integer, PageResult> entry : pageResultMap.entrySet()) {
					int pageNum = entry.getKey();
					PageResult result = entry.getValue();
					// Set the complete text for the page
					result.text = pageTextMap.get(pageNum).toString();
					resultList.add(result);
				}

				// Sort by page number
				resultList.sort(Comparator.comparingInt(p -> p.page));

				return new OcrJobResult(status.toString(), resultList);
			}

			// Default case for unexpected status
			return new OcrJobResult("UNKNOWN", null);

		} catch (Exception e) {
			return new OcrJobResult("ERROR: " + e.getMessage(), null);
		}
	}
}
