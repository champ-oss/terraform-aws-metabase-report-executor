package com.champtitles.metabasereportexecutor.executor;

import com.champtitles.metabasereportexecutor.executor.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Create a MetabaseClient
     *
     * @param baseUrl  URL of the Metabase server
     * @param email    login email
     * @param password login password
     */
    public MetabaseClient(String baseUrl, String email, String password) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.password = password;
        httpClient = HttpClient.newBuilder().build();
    }

    /**
     * Create a MetabaseClient
     *
     * @param baseUrl    URL of the Metabase server
     * @param email      login email
     * @param password   login password
     * @param httpClient inject a HttpClient
     */
    MetabaseClient(String baseUrl, String email, String password, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.password = password;
        this.httpClient = httpClient;
    }

    /**
     * Query the metabase server for session properties. This will include the setup token which will only
     * be present if the server has not yet been initialized.
     * <p>
     * metabase.com/docs/latest/api/session#get-apisessionproperties
     *
     * @return metabase session properties
     */
    public SessionPropertiesResponse getSessionProperties() {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/session/properties"))
                .GET()
                .build();
        String response = sendHttpRequestGetString(httpRequest, 200);

        try {
            return objectMapper.readValue(response, SessionPropertiesResponse.class);
        } catch (JsonProcessingException e) {
            logger.error("failed to parse session properties response: {}", response);
            throw new RuntimeException(e);
        }
    }

    /**
     * Complete the initial setup of the metabase server by creating a user and setting basic properties
     * <p>
     * metabase.com/docs/latest/api/setup#post-apisetup
     *
     * @param setupToken setup token obtained by querying the session properties API
     */
    public void completeInitialSetup(String setupToken) {
        SetupRequest setupRequest = new SetupRequest(setupToken, email, password);

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/setup"))
                .header("Content-Type", "application/json")
                .POST(createBody(setupRequest))
                .build();
        String response = sendHttpRequestGetString(httpRequest, 200);
        logger.info("setup response: {}", response);
    }

    /**
     * Log in to metabase with the configured email and password and store the session id
     * <p>
     * metabase.com/docs/latest/api/session#post-apisession
     *
     * @return session id (cookie) to be used for subsequent requests
     */
    public String loginAndGetSession() {
        SessionRequest sessionRequest = new SessionRequest(email, password);

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/session"))
                .header("Content-Type", "application/json")
                .POST(createBody(sessionRequest))
                .build();
        String response = sendHttpRequestGetString(httpRequest, 200);

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

    /**
     * Create a Card in Metabase
     * <p>
     * metabase.com/glossary/card
     * metabase.com/docs/latest/api/card#post-apicard
     *
     * @param name name of the card to create
     * @return id of the created card
     */
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
        String response = sendHttpRequestGetString(httpRequest, 202);

        try {
            CreateCardResponse createCardResponse = objectMapper.readValue(response, CreateCardResponse.class);
            logger.info("card created successfully: {}", createCardResponse.id());
            return createCardResponse.id();

        } catch (JsonProcessingException e) {
            logger.error("failed to parse create card response: {}", response);
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute a query on a Metabase Card and return the results as XLSX data
     * <p>
     * metabase.com/docs/latest/api/card#post-apicardcard-idqueryexport-format
     *
     * @param cardId metabase card to query
     * @return XLSX data
     */
    public byte[] queryCardGetXlsx(String cardId) {
        if (StringUtils.isBlank(sessionId)) {
            throw new RuntimeException("you must login before querying a card");
        }

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(createUri("/api/card/" + cardId + "/query/xlsx"))
                .header("X-Metabase-Session", sessionId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        byte[] response = sendHttpRequestGetBytes(httpRequest, 200);
        logger.info("query card response size: {} bytes", response.length);
        return response;
    }

    /**
     * Generate a URI using the Metabase base URL and the supplied path
     *
     * @param path URL path relative to the Metabase server URL
     * @return URI
     */
    private URI createUri(String path) {
        try {
            return new URI(baseUrl + path);
        } catch (URISyntaxException e) {
            logger.error("unable to create URI from string: {}{}", baseUrl, path);
            throw new RuntimeException(e);
        }
    }

    /**
     * Send a HttpRequest, check the response status code, and return the response body as a string
     *
     * @param httpRequest        pre-created request to send
     * @param expectedStatusCode HTTP status code response expected
     * @return HTTP response body as a string
     */
    private String sendHttpRequestGetString(HttpRequest httpRequest, Integer expectedStatusCode) {
        try {
            logger.info("sending HTTP {} request to {}", httpRequest.method(), httpRequest.uri());
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != expectedStatusCode) {
                logger.error("expected {} response but received {}", expectedStatusCode, response.statusCode());
                logger.error("response body: {}", response.body());
                throw new RuntimeException("unexpected response status code from HTTP request");
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            logger.error("HTTP request failed");
            throw new RuntimeException(e);
        }
    }

    /**
     * Send a HttpRequest, check the response status code, and return the response body as a byte array
     *
     * @param httpRequest        pre-created request to send
     * @param expectedStatusCode HTTP status code response expected
     * @return HTTP response body as a byte array
     */
    private byte[] sendHttpRequestGetBytes(HttpRequest httpRequest, Integer expectedStatusCode) {
        try {
            logger.info("sending HTTP {} request to {}", httpRequest.method(), httpRequest.uri());
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != expectedStatusCode) {
                logger.error("expected {} response but received {}", expectedStatusCode, response.statusCode());
                logger.error("response body: {}", response.body());
                throw new RuntimeException("unexpected response status code from HTTP request");
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            logger.error("HTTP request failed");
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize the given Record object as JSON string and return a HttpRequest body
     *
     * @param e Record object to serialize as JSON
     * @return HttpRequest body
     */
    private <T> HttpRequest.BodyPublisher createBody(Record e) {
        try {
            return HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(e));
        } catch (JsonProcessingException ex) {
            logger.error("failed to write object as string {}", e.toString());
            throw new RuntimeException(ex);
        }
    }
}
