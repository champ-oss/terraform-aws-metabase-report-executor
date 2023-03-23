package com.champtitles.metabasereportexecutor.test;

import com.champtitles.metabasereportexecutor.executor.MetabaseClient;
import com.champtitles.metabasereportexecutor.executor.model.SessionPropertiesResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String metabaseUrl = System.getenv("METABASE_URL");
    private static final String metabaseUsername = System.getenv("METABASE_USERNAME");
    private static final String metabasePassword = System.getenv("METABASE_PASSWORD");


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
        String cardId = metabaseClient.createCard("test");
    }
}