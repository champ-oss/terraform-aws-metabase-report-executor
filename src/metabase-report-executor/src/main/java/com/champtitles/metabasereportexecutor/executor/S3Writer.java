package com.champtitles.metabasereportexecutor.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class S3Writer {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Writer.class.getName());
    private final S3Client s3Client;
    private final String bucket;

    public S3Writer(String bucket) {
        s3Client = S3Client.builder().build();
        this.bucket = bucket;
    }

    S3Writer(String bucket, S3Client s3Client) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    /**
     * Upload the given data as an XLSX document to S3
     *
     * @param xlsxBody   contents of the XLSX document
     * @param namePrefix file name prefix
     */
    public void uploadXlsx(byte[] xlsxBody, String namePrefix) {
        String key = getKey(namePrefix);
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try {
            LOGGER.info("uploading {} to bucket {}", key, bucket);
            s3Client.putObject(objectRequest, RequestBody.fromBytes(xlsxBody));

        } catch (AwsServiceException | SdkClientException e) {
            LOGGER.error("error uploading {} to bucket {}", key, bucket);
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a full s3 key path using the current date and time
     *
     * @param namePrefix file name prefix
     * @return full s3 path
     */
    private String getKey(String namePrefix) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%s/%s/%s/%s-%s.xlsx",
                now.format(DateTimeFormatter.ofPattern("yyyy")),
                now.format(DateTimeFormatter.ofPattern("MM")),
                now.format(DateTimeFormatter.ofPattern("dd")),
                namePrefix,
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")));
    }
}
