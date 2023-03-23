package com.champtitles.metabasereportexecutor.executor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class App implements RequestHandler<Map<String, String>, String> {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String metabaseUrl = System.getenv("METABASE_URL");
    private static final String metabaseUsername = System.getenv("METABASE_USERNAME");
    private static final String metabasePassword = System.getenv("METABASE_PASSWORD");
    private static final String metabaseCardId = System.getenv("METABASE_CARD_ID");
    private final MetabaseClient metabaseClient;

    public App() {
        metabaseClient = new MetabaseClient(metabaseUrl, metabaseUsername, metabasePassword);
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        logger.info("logging in to metabase: {}", metabaseUrl);
        metabaseClient.loginAndGetSession();

        logger.info("running query for card: {}", metabaseCardId);
        metabaseClient.queryCardGetXlsx(metabaseCardId);
        return null;
    }

}