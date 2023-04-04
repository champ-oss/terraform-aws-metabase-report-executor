package com.champtitles.metabasereportexecutor.executor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App implements RequestHandler<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String awsRegion = System.getenv("AWS_REGION");
    private static final String bucket = System.getenv("BUCKET");
    private static final String metabaseUrl = System.getenv("METABASE_URL");
    private static final String metabaseUsername = System.getenv("METABASE_USERNAME");
    private static final String metabasePasswordKms = System.getenv("METABASE_PASSWORD_KMS");
    private static final String metabaseCardId = System.getenv("METABASE_CARD_ID");
    private static final String metabaseDeviceUuid = System.getenv("METABASE_DEVICE_UUID");
    private final MetabaseClient metabaseClient;
    private final S3Writer s3Writer;

    public App() {
        KmsDecrypt kmsDecrypt = new KmsDecrypt(awsRegion);
        metabaseClient = new MetabaseClient(metabaseUrl, metabaseUsername, kmsDecrypt.decrypt(metabasePasswordKms), metabaseDeviceUuid);
        s3Writer = new S3Writer(bucket);
    }

    @Override
    public String handleRequest(Object event, Context context) {
        logger.info("logging in to metabase: {}", metabaseUrl);
        metabaseClient.loginAndGetSession();

        logger.info("running query for card: {}", metabaseCardId);
        byte[] xlsxBody = metabaseClient.queryCardGetXlsx(metabaseCardId);
        s3Writer.uploadXlsx(xlsxBody, "card" + metabaseCardId);
        return null;
    }

}