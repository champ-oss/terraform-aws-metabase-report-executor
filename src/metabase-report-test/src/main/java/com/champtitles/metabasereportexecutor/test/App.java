package com.champtitles.metabasereportexecutor.test;

import com.champtitles.metabasereportexecutor.executor.MetabaseClient;
import com.champtitles.metabasereportexecutor.executor.model.SessionPropertiesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        MetabaseClient metabaseClient = new MetabaseClient("http://localhost:12345", "test@example.com", "1289hdf198hjd192je");

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