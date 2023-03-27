package com.champtitles.metabasereportexecutor.test;

import com.champtitles.metabasereportexecutor.executor.MetabaseClient;
import com.champtitles.metabasereportexecutor.executor.model.SessionPropertiesResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;


public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String metabaseUrl = System.getenv("METABASE_URL");
    private static final String metabaseUsername = System.getenv("METABASE_USERNAME");
    private static final String metabasePassword = System.getenv("METABASE_PASSWORD");
    private static final String awsRegion = System.getenv("AWS_REGION");
    private static final String executorFunctionName = System.getenv("EXECUTOR_FUNCTION_NAME");

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

        //logger.info("checking xlsx file on s3");

        //logger.info("cleaning up s3 test file");
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
}