package com.champtitles.metabasereportexecutor.notifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class S3ReaderTest {

    @InjectMocks
    S3Reader s3Reader;

    @Mock
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3Reader = new S3Reader("test-bucket", s3Client);
    }

    @Test
    void downloadXlsx_downloadsFileSuccessfully() {
        byte[] expected = "data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(expected);
        ResponseInputStream<GetObjectResponse> getObjectResponse = new ResponseInputStream<>(
                GetObjectResponse.builder().build(), AbortableInputStream.create(inputStream));

        Mockito.when(s3Client.getObject(Mockito.any(GetObjectRequest.class))).thenReturn(getObjectResponse);

        byte[] result = s3Reader.downloadXlsx("test-file.xlsx");
        assertArrayEquals(expected, result);
    }
}
