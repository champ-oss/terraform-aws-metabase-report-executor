package com.champtitles.metabasereportexecutor.notifier;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App implements RequestHandler<S3Event, Void> {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String bucket = System.getenv("BUCKET");
    private final S3Reader s3Reader;

    public App() {
        ;
        s3Reader = new S3Reader(bucket);
    }

    @Override
    public Void handleRequest(final S3Event event, final Context context) {
        logger.info(event.toString());

        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            byte[] data = s3Reader.downloadXlsx(record.getS3().getObject().getKey());
            logger.info("downloaded {} bytes", data.length);
        }

        return null;
    }

}