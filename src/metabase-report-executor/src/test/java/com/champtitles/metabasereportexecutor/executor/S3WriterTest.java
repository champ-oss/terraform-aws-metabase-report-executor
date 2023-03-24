package com.champtitles.metabasereportexecutor.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class S3WriterTest {

    @InjectMocks
    S3Writer s3Writer;

    @Mock
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3Writer = new S3Writer("test-bucket", s3Client);
    }

    @Test
    void uploadXlsx_uploadsFileSuccessfully() {
        byte[] data = "data".getBytes();
        s3Writer.uploadXlsx(data, "card1");

        Mockito.verify(s3Client, Mockito.times(1)).putObject((PutObjectRequest) Mockito.argThat(putObjectRequest -> {
            System.out.println(putObjectRequest.toString());
            // Should match pattern like: PutObjectRequest(Bucket=test-bucket, Key=2023/03/24/card1-2023-03-24T11-59-11.xlsx)
            assertTrue(putObjectRequest.toString().matches("PutObjectRequest\\(Bucket=test-bucket, Key=\\d\\d\\d\\d/\\d\\d/\\d\\d/card1-\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d-\\d\\d-\\d\\d.xlsx\\)"));
            return true;
        }), (RequestBody) any());
    }
}
