package com.champtitles.metabasereportexecutor.executor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App implements RequestHandler<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class.getName());
    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String BUCKET = System.getenv("BUCKET");
    private static final String METABASE_URL = System.getenv("METABASE_URL");
    private static final String METABASE_USERNAME = System.getenv("METABASE_USERNAME");
    private static final String METABASE_PASSWORD_KMS = System.getenv("METABASE_PASSWORD_KMS");
    private static final String METABASE_CARD_ID = System.getenv("METABASE_CARD_ID");
    private static final String METABASE_DEVICE_UUID = System.getenv("METABASE_DEVICE_UUID");
    private final MetabaseClient metabaseClient;
    private final S3Writer s3Writer;

    public App() {
        KmsDecrypt kmsDecrypt = new KmsDecrypt(AWS_REGION);
        metabaseClient = new MetabaseClient(METABASE_URL, METABASE_USERNAME, kmsDecrypt.decrypt(METABASE_PASSWORD_KMS), METABASE_DEVICE_UUID);
        s3Writer = new S3Writer(BUCKET);
    }

    @Override
    public String handleRequest(Object event, Context context) {
        LOGGER.info("logging in to metabase: {}", METABASE_URL);
        metabaseClient.loginAndGetSession();

        LOGGER.info("running query for card: {}", METABASE_CARD_ID);
        byte[] xlsxBody = metabaseClient.queryCardGetXlsx(METABASE_CARD_ID);
        s3Writer.uploadXlsx(xlsxBody, "card" + METABASE_CARD_ID);

        LOGGER.info("finished processing card {} and uploaded to bucket: {}", METABASE_CARD_ID, BUCKET);
        return null;
    }

}