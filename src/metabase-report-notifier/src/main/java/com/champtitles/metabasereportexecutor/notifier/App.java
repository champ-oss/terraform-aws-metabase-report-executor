package com.champtitles.metabasereportexecutor.notifier;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App implements RequestHandler<SNSEvent, String> {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String bucket = System.getenv("BUCKET");
    private final S3Reader s3Reader;

    public App() {
        ;
        s3Reader = new S3Reader(bucket);
    }

    @Override
    public String handleRequest(SNSEvent snsEvent, Context context) {
        logger.info(snsEvent.toString());

//        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
//            byte[] data = s3Reader.downloadXlsx(record.getS3().getObject().getUrlDecodedKey());
//            logger.info("downloaded {} bytes", data.length);
//        }

        return null;
    }

}