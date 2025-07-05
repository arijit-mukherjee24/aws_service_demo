# AWS Service Tester

A Spring Boot REST API for working with AWS S3: download files, generate presigned URLs, and upload files.

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

- Make sure your IAM user has the required S3 permissions (`s3:GetObject`, `s3:PutObject`).
- The API returns HTTP 404 if the file is not found, and 400 for upload errors.

---

## **Contact**

For questions or issues, please open an issue on this repository.