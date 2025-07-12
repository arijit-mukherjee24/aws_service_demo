package com.experiment.aws.awsservicetester.service;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;


@Service
public class BedrockService {
	
	private final BedrockRuntimeClient bedrockRuntimeClient;
    private static final String CLAUDE_SONNET_MODEL_ID = "anthropic.claude-3-sonnet-20240229-v1:0";
    
    public BedrockService(BedrockRuntimeClient bedrockRuntimeClient) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
    } 
    /*
     * This method sends a prompt to the Anthropic Claude 3 Sonnet model hosted on AWS Bedrock, 
     * and returns the model's AI-generated response as a String.
     *
     * Step-by-step breakdown:
     *
     * 1. Prepare the request payload:
     *    - Claude 3 Sonnet expects the prompt message in a specific JSON format. 
     *      The structure includes:
     *        - "anthropic_version": The Claude API version string required by Bedrock ("bedrock-2023-05-31").
     *        - "max_tokens": The maximum length for the model's response (in tokens). 
     *        - "messages": An array of chat message objects, each with:
     *            - "role": Should be "user" for your prompt.
     *            - "content": The actual prompt text.
     *    - The prompt text is inserted into the JSON using String formatting.
     *    - Any double quotes in the prompt are escaped to prevent malformed JSON.
     *
     * 2. Build the Bedrock model invocation request:
     *    - Use InvokeModelRequest.builder() to create the request object.
     *    - Specify the modelId for Claude 3 Sonnet (hardcoded as CLAUDE_SONNET_MODEL_ID).
     *    - Set contentType to "application/json" to indicate the request body is JSON.
     *    - Set accept to "application/json" to request a JSON response from Bedrock.
     *    - Convert the JSON request body String to SdkBytes (required by AWS SDK).
     *    - Build the final request object.
     *
     * 3. Send the request to AWS Bedrock:
     *    - Use the BedrockRuntimeClient (already configured with credentials and region) to call invokeModel().
     *    - Pass the constructed request object.
     *    - AWS Bedrock processes the request and responds with the model's output.
     *
     * 4. Extract and return the response:
     *    - The response from Bedrock comes as raw bytes.
     *    - Convert the response body to a UTF-8 String (so it's human-readable).
     *    - Return this String, which contains the model's answer.
     */
    public String invokeBedrockModel(String prompt) {
        // Claude 3 expects the prompt wrapped in a structured JSON format
        // For chat models, the structure is typically: {"messages":[{"role":"user","content":"..."}]}
    	String requestBody = """
    			{
    			  "anthropic_version": "bedrock-2023-05-31",
    			  "max_tokens": 1024,
    			  "messages": [
    			    {
    			      "role": "user",
    			      "content": "%s"
    			    }
    			  ]
    			}
    			""".formatted(prompt.replace("\"", "\\\""));

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(CLAUDE_SONNET_MODEL_ID)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();

        InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
        return response.body().asUtf8String();
    }
}
