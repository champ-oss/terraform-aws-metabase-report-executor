package com.champtitles.metabasereportexecutor.notifier;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

public class App implements RequestHandler<S3Event, Void> {

    @Override
    public Void handleRequest(final S3Event event, final Context context) {
        System.out.println(event.toString());
        return null;
    }

}