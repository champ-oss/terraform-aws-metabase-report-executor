package com.champtitles.metabasereportexecutor.test;

import com.champtitles.metabasereportexecutor.executor.KmsDecrypt;
import com.champtitles.metabasereportexecutor.executor.MetabaseClient;
import com.champtitles.metabasereportexecutor.executor.model.SessionPropertiesResponse;
import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.Status;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.evanlennick.retry4j.exception.RetriesExhaustedException;
import com.evanlennick.retry4j.exception.UnexpectedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class.getName());
    private static final String METABASE_URL = System.getenv("METABASE_URL");
    private static final String METABASE_USERNAME = System.getenv("METABASE_USERNAME");
    private static final String METABASE_PASSWORD_KMS = System.getenv("METABASE_PASSWORD_KMS");
    private static final String METABASE_DEVICE_UUID = System.getenv("METABASE_DEVICE_UUID");
    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String EXECUTOR_FUNCTION_NAME = System.getenv("EXECUTOR_FUNCTION_NAME");
    private static final String LAMBDA_EXECUTOR_CLOUDWATCH_LOG_GROUP = System.getenv("LAMBDA_EXECUTOR_CLOUDWATCH_LOG_GROUP");
    private static final String LAMBDA_NOTIFIER_CLOUDWATCH_LOG_GROUP = System.getenv("LAMBDA_NOTIFIER_CLOUDWATCH_LOG_GROUP");
    private static final String BUCKET = System.getenv("BUCKET");
    private static final int RETRIES = 90;
    private static final int DELAY_SECONDS = 10;
    private static final RetryConfig config = new RetryConfigBuilder()
            .retryOnAnyException()
            .withMaxNumberOfTries(RETRIES)
            .withDelayBetweenTries(DELAY_SECONDS, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

    public static void main(String[] args) throws InterruptedException {
        KmsDecrypt kmsDecrypt = new KmsDecrypt(AWS_REGION);
        MetabaseClient metabaseClient = new MetabaseClient(METABASE_URL, METABASE_USERNAME, kmsDecrypt.decrypt(METABASE_PASSWORD_KMS), METABASE_DEVICE_UUID);

        SessionPropertiesResponse sessionPropertiesResponse = waitForSessionProperties(metabaseClient);
        assertNotNull(sessionPropertiesResponse);

        if (StringUtils.isBlank(sessionPropertiesResponse.setupToken())) {
            LOGGER.info("initial setup has already been completed");
        } else {
            LOGGER.info("performing initial metabase setup using setup token: {}", sessionPropertiesResponse.setupToken());
            metabaseClient.completeInitialSetup(sessionPropertiesResponse.setupToken());
        }

        LOGGER.info("logging in to metabase: {}", METABASE_URL);
        metabaseClient.loginAndGetSession();

        LOGGER.info("creating a card in metabase for testing");
        metabaseClient.createCard("test");

        LOGGER.info("invoking executor lambda");
        invokeExecutorLambda();

        LOGGER.info("waiting 30 seconds for executor to run");
        Thread.sleep(30 * 1000);

        LOGGER.info("checking xlsx files in s3 bucket: {}", BUCKET);
        List<String> objects = listS3Objects();
        assertTrue(objects.size() > 0);

        for (String s3Key : objects) {
            assertTrue(getXlsxRowCount(s3Key) >= 200);
        }

        LOGGER.info("getting executor lambda logs");
        boolean executorSuccess = false;
        String lambdaExecutorCloudwatchLogStream = getCloudWatchLogStream(LAMBDA_EXECUTOR_CLOUDWATCH_LOG_GROUP);
        for (String log : getCloudWatchLogs(lambdaExecutorCloudwatchLogStream, LAMBDA_EXECUTOR_CLOUDWATCH_LOG_GROUP)) {
            System.out.println("executor lambda - " + log);
            if (log.contains("done processing")) {
                executorSuccess = true;
            }
        }
        assertTrue(executorSuccess);

        LOGGER.info("getting notifier lambda logs");
        boolean notifierSuccess = false;
        String lambdaNotifierCloudwatchLogStream = getCloudWatchLogStream(LAMBDA_NOTIFIER_CLOUDWATCH_LOG_GROUP);
        for (String log : getCloudWatchLogs(lambdaNotifierCloudwatchLogStream, LAMBDA_NOTIFIER_CLOUDWATCH_LOG_GROUP)) {
            System.out.println("notifier lambda - " + log);
            if (log.contains("address is not verified")) {
                notifierSuccess = true;
            }
        }
        assertTrue(notifierSuccess);
    }

    /**
     * Get metabase session properties with retry logic
     *
     * @param metabaseClient instance of the Metabase Client library
     * @return Metabase session properties
     */
    private static SessionPropertiesResponse waitForSessionProperties(MetabaseClient metabaseClient) {
        Callable<SessionPropertiesResponse> callable = metabaseClient::getSessionProperties;

        try {
            Status<SessionPropertiesResponse> status = new CallExecutorBuilder().config(config).build().execute(callable);
            return status.getResult();

        } catch (RetriesExhaustedException | UnexpectedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoke the AWS Lambda function for the report executor
     */
    private static void invokeExecutorLambda() {
        LambdaClient awsLambda = LambdaClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        InvokeRequest request = InvokeRequest.builder().functionName(EXECUTOR_FUNCTION_NAME).build();
        InvokeResponse invokeResponse = awsLambda.invoke(request);
        LOGGER.info("invoke response statusCode={}", invokeResponse.statusCode());
    }

    /**
     * List and return the object keys in the report output S3 bucket
     */
    private static List<String> listS3Objects() {
        S3Client s3Client = S3Client.builder().build();
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(BUCKET).build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

        List<String> objects = new ArrayList<String>();
        for (S3Object s3Object : listObjectsV2Response.contents()) {
            LOGGER.info("s3 object: key={} bytes={}", s3Object.key(), s3Object.size());
            objects.add(s3Object.key());
        }
        LOGGER.info("found {} files in s3 bucket: {}", objects.size(), BUCKET);
        return objects;
    }

    /**
     * Download and open the XLSX file from S3 and return the number of rows
     */
    private static int getXlsxRowCount(String s3Key) {
        LOGGER.info("downloading file s3://{}/{}", BUCKET, s3Key);
        S3Client s3Client = S3Client.builder().build();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(BUCKET).key(s3Key).build();
        ResponseInputStream<GetObjectResponse> getObjectResponse = s3Client.getObject(getObjectRequest);

        try {
            LOGGER.info("opening xlsx file");
            Workbook workbook = new XSSFWorkbook(getObjectResponse);
            Sheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getPhysicalNumberOfRows();
            LOGGER.info("xlsx file has {} rows", rows);
            return rows;

        } catch (IOException e) {
            LOGGER.error("unable to open xlsx file: {}", s3Key);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the latest log stream from the CloudWatch log group
     *
     * @param logGroupName CloudWatch log group
     * @return name of the latest log stream
     */
    private static String getCloudWatchLogStream(String logGroupName) {
        CloudWatchLogsClient cloudWatchLogsClient = CloudWatchLogsClient
                .builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        DescribeLogStreamsRequest describeLogStreamsRequest = DescribeLogStreamsRequest
                .builder()
                .logGroupName(logGroupName)
                .orderBy("LastEventTime")
                .descending(true)
                .build();

        DescribeLogStreamsResponse describeLogStreamsResponse = cloudWatchLogsClient.describeLogStreams(describeLogStreamsRequest);
        return describeLogStreamsResponse.logStreams().get(0).logStreamName();
    }

    /**
     * Get log messages from the specified CloudWatch log stream
     *
     * @param logStreamName name of the log stream to query
     * @param logGroupName  CloudWatch log group
     * @return array of log messages
     */
    private static String[] getCloudWatchLogs(String logStreamName, String logGroupName) {
        CloudWatchLogsClient cloudWatchLogsClient = CloudWatchLogsClient
                .builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        GetLogEventsRequest getLogEventsRequest = GetLogEventsRequest
                .builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .build();

        GetLogEventsResponse getLogEventsResponse = cloudWatchLogsClient.getLogEvents(getLogEventsRequest);
        return getLogEventsResponse.events().stream().map(OutputLogEvent::message).toArray(String[]::new);
    }
}