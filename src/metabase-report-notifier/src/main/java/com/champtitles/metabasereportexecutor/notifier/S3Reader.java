package com.champtitles.metabasereportexecutor.notifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;

public class S3Reader {
    private static final Logger logger = LoggerFactory.getLogger(S3Reader.class.getName());
    private final S3Client s3Client;
    private final String bucket;

    public S3Reader(String bucket) {
        s3Client = S3Client.builder().build();
        this.bucket = bucket;
    }

    S3Reader(String bucket, S3Client s3Client) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    /**
     * Download the given document from S3
     *
     * @param s3Key S3 key to download
     */
    public byte[] downloadXlsx(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        logger.info("downloading file: s3://{}/{}", bucket, s3Key);
        ResponseInputStream<GetObjectResponse> getObjectResponse = s3Client.getObject(getObjectRequest);

        try {
            return getObjectResponse.readAllBytes();

        } catch (IOException e) {
            logger.error("error downloading {} from bucket {}", s3Key, bucket);
            throw new RuntimeException(e);
        }
    }
}
