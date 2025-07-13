# AWS Service Tester

A Spring Boot REST API for working with AWS services.

---

## **API Endpoints**

### 1. Download a document from S3

**GET** `/api/s3/document`

**Query Parameters:**
- `bucket` (required): your S3 bucket name
- `key` (required): the S3 object key (file path in bucket)

**Example:**
```sh
curl -X GET "http://localhost:8080/api/s3/document?bucket=my-bucket&key=folder/file.pdf" -o file.pdf
```

---

### 2. Generate a presigned URL for a document

**GET** `/api/s3/presigned-url`

**Query Parameters:**
- `bucket` (required): your S3 bucket name
- `key` (required): the S3 object key
- `expiryMinutes` (optional): URL expiry in minutes (default: 15)

**Example:**
```sh
curl "http://localhost:8080/api/s3/presigned-url?bucket=my-bucket&key=folder/file.pdf&expiryMinutes=30"
```
The response will be a URL string, valid for the specified time, allowing anyone with the link to download the file.

---

### 3. Upload a document to S3

**POST** `/api/s3/upload`

**Query Parameters:**
- `bucket` (required): your S3 bucket name
- `key` (required): the S3 object key (desired file path in bucket)

**Form Data:**
- `file` (required): the file to upload (use multipart/form-data)

**Example (using curl):**
```sh
curl -X POST "http://localhost:8080/api/s3/upload?bucket=my-bucket&key=folder/newfile.pdf" \
  -F "file=@/path/to/local/file.pdf"
```

---

### 4. Start OCR processing on a document using Textract

**POST** `/api/ocr/start`

**Query Parameters:**
- `bucket` (required): your S3 bucket name
- `key` (required): the S3 object key of the document to process

**Example (using curl):**
```sh
curl -X POST "http://localhost:8080/api/ocr/start?bucket=my-bucket&key=folder/document.pdf"
```

---

### 5. Get OCR results

**GET** `/api/ocr/results/{jobId}`

**Path Variables:**
- `jobId` (required): the job ID returned from the start OCR endpoint

**Query Parameters:**
- `pages` (optional): List of page numbers to retrieve. If not provided, all pages will be returned.

**Example: (all pages)**
```sh
curl -X GET "http://localhost:8080/api/ocr/results/1234567890abcdef"
```

**Example: (specific pages)**
```sh
curl -X GET "http://localhost:8080/api/ocr/results/1234567890abcdef?pages=1&pages=3"
```

**Response: (when processing)**
```json
{
  "status": "IN_PROGRESS",
  "results": null
}
```

**Response: (when complete)**
```json
{
  "status": "SUCCEEDED",
  "results": [
    {
      "page": 1,
      "text": "This is the full text from page 1\nMultiple lines...",
      "lines": [
        {
          "id": "line-1",
          "text": "This is the full text from page 1",
          "confidence": 99.5
        },
        {
          "id": "line-2",
          "text": "Multiple lines...",
          "confidence": 98.7
        }
      ]
    }
  ]
}
```

---

### 6. Interact with AWS Bedrock Model

**POST** `/api/bedrock/playground`

**Form Data:**
- `prompt` (required): The prompt to send to the model (use `x-www-form-urlencoded`)

**Example (using curl):**
```sh
curl -X POST http://localhost:8080/api/bedrock/playground -d "prompt=Tell me a joke about software developers"
```
---

### 7. Analyze text sentiment

**POST** `/api/bedrock/sentiment`

**Form Data:**
- `text` (required): The text to analyze (use `x-www-form-urlencoded`)

**Example (using curl):**
```sh
curl -X POST http://localhost:8080/api/bedrock/sentiment -d "text=This is amazing!"
```
---

## **AWS Credentials**

- Ensure your AWS credentials are in `~/.aws/credentials` (Linux/Mac) or `C:\Users\<username>\.aws\credentials` (Windows).
- Example contents:
  ```
  [default]
  aws_access_key_id=YOUR_ACCESS_KEY
  aws_secret_access_key=YOUR_SECRET_KEY
  ```

---

## **Running the Application**

1. Build the project:
   ```sh
   mvn clean install
   ```
2. Run the application:
   ```sh
   mvn spring-boot:run
   ```
   or run the generated jar:
   ```sh
   java -jar target/aws-service-tester-0.0.1-SNAPSHOT.jar
   ```

---

## **Notes**

- Make sure your IAM user has the required S3 and Bedrock permissions (`s3:GetObject`, `s3:PutObject`, `bedrock:InvokeModel`).
- The API returns HTTP 404 if the file is not found, and 400 for upload errors.

---

## **Contact**

For questions or issues, please open an issue on this repository.