package com.champtitles.metabasereportexecutor.test;

import com.champtitles.metabasereportexecutor.executor.MetabaseClient;
import com.champtitles.metabasereportexecutor.executor.model.SessionPropertiesResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String metabaseHost = System.getenv("METABASE_HOST");
    private static final String metabaseUsername = System.getenv("METABASE_USERNAME");
    private static final String metabasePassword = System.getenv("METABASE_PASSWORD");


    public static void main(String[] args) {
        MetabaseClient metabaseClient = new MetabaseClient(metabaseHost, metabaseUsername, metabasePassword);

        SessionPropertiesResponse sessionPropertiesResponse = metabaseClient.getSessionProperties();
        if (StringUtils.isBlank(sessionPropertiesResponse.setupToken())) {
            logger.info("initial setup has already been completed");
        } else {
            logger.info("setup token: {}", sessionPropertiesResponse.setupToken());
            metabaseClient.completeInitialSetup(sessionPropertiesResponse.setupToken());
        }

        metabaseClient.loginAndGetSession();

        String cardId = metabaseClient.createCard("test");

        metabaseClient.queryCardGetXlsx(cardId);
    }
}