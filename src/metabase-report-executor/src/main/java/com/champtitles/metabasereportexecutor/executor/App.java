package com.champtitles.metabasereportexecutor.executor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;

public class App implements RequestHandler<Map<String, String>, String> {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());

    private static final String bucket = System.getenv("BUCKET");
    private static final String metabaseUrl = System.getenv("METABASE_URL");
    private static final String metabaseUsername = System.getenv("METABASE_USERNAME");
    private static final String metabasePassword = System.getenv("METABASE_PASSWORD");
    private static final String metabaseCardId = System.getenv("METABASE_CARD_ID");
    private final MetabaseClient metabaseClient;
    private final S3Client s3Client;

    public App() {
        metabaseClient = new MetabaseClient(metabaseUrl, metabaseUsername, metabasePassword);
        s3Client = S3Client.builder().build();
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        logger.info("logging in to metabase: {}", metabaseUrl);
        metabaseClient.loginAndGetSession();

        logger.info("running query for card: {}", metabaseCardId);
        String xlsxBody = metabaseClient.queryCardGetXlsx(metabaseCardId);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key("test1.xlsx")
                .build();

        s3Client.putObject(objectRequest, RequestBody.fromString(xlsxBody));
        return null;
    }

}