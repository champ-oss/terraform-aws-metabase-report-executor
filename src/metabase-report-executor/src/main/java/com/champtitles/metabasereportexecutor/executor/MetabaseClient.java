package com.champtitles.metabasereportexecutor.executor;

import com.champtitles.metabasereportexecutor.executor.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MetabaseClient {
    private static final Logger logger = LoggerFactory.getLogger(MetabaseClient.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String email;
    private final String password;
    private final HttpClient httpClient;
    private String sessionId;

    public MetabaseClient(String baseUrl, String email, String password) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.password = password;
        httpClient = HttpClient.newBuilder().build();
    }

    public SessionPropertiesResponse getSessionProperties() {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/session/properties"))
                .GET()
                .build();
        String response = sendHttpRequest(httpRequest, 200);

        try {
            return objectMapper.readValue(response, SessionPropertiesResponse.class);
        } catch (JsonProcessingException e) {
            logger.error("failed to parse session properties response: {}", response);
            throw new RuntimeException(e);
        }
    }

    public void completeInitialSetup(String setupToken) {
        SetupRequest setupRequest = new SetupRequest(setupToken, email, password);

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/setup"))
                .header("Content-Type", "application/json")
                .POST(createBody(setupRequest))
                .build();
        String response = sendHttpRequest(httpRequest, 200);
        logger.info("setup response: {}", response);
    }

    public String loginAndGetSession() {
        SessionRequest sessionRequest = new SessionRequest(email, password);

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/session"))
                .header("Content-Type", "application/json")
                .POST(createBody(sessionRequest))
                .build();
        String response = sendHttpRequest(httpRequest, 200);

        try {
            SessionResponse sessionResponse = objectMapper.readValue(response, SessionResponse.class);
            logger.info("logged in successfully");
            logger.debug("session id: {}", sessionResponse.id());
            sessionId = sessionResponse.id();
            return sessionResponse.id();

        } catch (JsonProcessingException e) {
            logger.error("failed to parse session properties response: {}", response);
            throw new RuntimeException(e);
        }
    }

    public String createCard(String name) {
        if (StringUtils.isBlank(sessionId)) {
            throw new RuntimeException("you must login before creating a card");
        }

        CreateCardRequest createCardRequest = new CreateCardRequest(name);

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/card"))
                .header("Content-Type", "application/json")
                .header("X-Metabase-Session", sessionId)
                .POST(createBody(createCardRequest))
                .build();
        String response = sendHttpRequest(httpRequest, 202);

        try {
            CreateCardResponse createCardResponse = objectMapper.readValue(response, CreateCardResponse.class);
            logger.info("card created successfully: {}", createCardResponse.id());
            return createCardResponse.id();

        } catch (JsonProcessingException e) {
            logger.error("failed to parse create card response: {}", response);
            throw new RuntimeException(e);
        }
    }

    public void queryCardGetXlsx(String cardId) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/card/" + cardId + "/query/xlsx"))
                .header("X-Metabase-Session", sessionId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        String response = sendHttpRequest(httpRequest, 200);
        logger.info("query card response size: {} bytes", response.length());
    }

    private URI createUri(String path) {
        try {
            return new URI(baseUrl + path);
        } catch (URISyntaxException e) {
            logger.error("unable to create URI from string: {}{}", baseUrl, path);
            throw new RuntimeException(e);
        }
    }

    private String sendHttpRequest(HttpRequest httpRequest, Integer expectedStatusCode) {
        try {
            logger.info("sending HTTP {} request to {}", httpRequest.method(), httpRequest.uri());
            HttpResponse<?> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != expectedStatusCode) {
                logger.error("expected {} response but received {}", expectedStatusCode, response.statusCode());
                logger.error("response body: {}", response.body());
                throw new RuntimeException("unexpected response status code from HTTP request");
            }

            return response.body().toString();

        } catch (IOException | InterruptedException e) {
            logger.error("HTTP request failed");
            throw new RuntimeException(e);
        }
    }

    private <T> HttpRequest.BodyPublisher createBody(Record e) {
        try {
            return HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(e));
        } catch (JsonProcessingException ex) {
            logger.error("failed to write object as string {}", e.toString());
            throw new RuntimeException(ex);
        }
    }
}
