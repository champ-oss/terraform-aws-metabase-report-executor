package com.champtitles.metabasereportexecutor.test;

import com.champtitles.metabasereportexecutor.executor.MetabaseClient;
import com.champtitles.metabasereportexecutor.executor.model.SessionPropertiesResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String metabaseUrl = System.getenv("METABASE_URL");
    private static final String metabaseUsername = System.getenv("METABASE_USERNAME");
    private static final String metabasePassword = System.getenv("METABASE_PASSWORD");
    private static final String awsRegion = System.getenv("AWS_REGION");
    private static final String executorFunctionName = System.getenv("EXECUTOR_FUNCTION_NAME");
    private static final String bucket = System.getenv("BUCKET");

    public static void main(String[] args) {
        MetabaseClient metabaseClient = new MetabaseClient(metabaseUrl, metabaseUsername, metabasePassword);

        SessionPropertiesResponse sessionPropertiesResponse = metabaseClient.getSessionProperties();
        if (StringUtils.isBlank(sessionPropertiesResponse.setupToken())) {
            logger.info("initial setup has already been completed");
        } else {
            logger.info("performing initial metabase setup using setup token: {}", sessionPropertiesResponse.setupToken());
            metabaseClient.completeInitialSetup(sessionPropertiesResponse.setupToken());
        }

        logger.info("logging in to metabase: {}", metabaseUrl);
        metabaseClient.loginAndGetSession();

        logger.info("creating a card in metabase for testing");
        metabaseClient.createCard("test");

        logger.info("invoking executor lambda");
        invokeExecutorLambda();

        logger.info("checking xlsx files in s3 bucket: {}", bucket);
        List<String> objects = listS3Objects();
        assertTrue(objects.size() > 0);

        for (String s3Key : objects) {
            assertTrue(getXlsxRowCount(s3Key) >= 200);
        }

    }

    private static void invokeExecutorLambda() {
        LambdaClient awsLambda = LambdaClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        InvokeRequest request = InvokeRequest.builder().functionName(executorFunctionName).build();
        InvokeResponse invokeResponse = awsLambda.invoke(request);
        logger.info("invoke response: {}", invokeResponse.statusCode());
    }

    private static List<String> listS3Objects() {
        S3Client s3Client = S3Client.builder().build();
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket).build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

        List<String> objects = new ArrayList<String>();
        for (S3Object s3Object : listObjectsV2Response.contents()) {
            logger.info("s3 object: key={} bytes={}", s3Object.key(), s3Object.size());
            objects.add(s3Object.key());
        }
        logger.info("found {} files in s3 bucket: {}", objects.size(), bucket);
        return objects;
    }

    private static int getXlsxRowCount(String s3Key) {
        logger.info("downloading file s3://{}/{}", bucket, s3Key);
        S3Client s3Client = S3Client.builder().build();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(s3Key).build();
        ResponseInputStream<GetObjectResponse> getObjectResponse = s3Client.getObject(getObjectRequest);

        try {
            logger.info("opening xlsx file");
            Workbook workbook = new XSSFWorkbook(getObjectResponse);
            Sheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getPhysicalNumberOfRows();
            logger.info("xlsx file has {} rows", rows);
            return rows;

        } catch (IOException e) {
            logger.error("unable to open xlsx file: {}", s3Key);
            throw new RuntimeException(e);
        }
    }
}