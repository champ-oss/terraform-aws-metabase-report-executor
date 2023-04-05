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

import static java.net.http.HttpClient.Version.HTTP_1_1;

public class MetabaseClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetabaseClient.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String baseUrl;
    private final String email;
    private final String password;
    private final String deviceUuid;
    private final HttpClient httpClient;
    private String sessionId;

    /**
     * Create a MetabaseClient
     *
     * @param baseUrl    URL of the Metabase server
     * @param email      login email
     * @param password   login password
     * @param deviceUuid cookie to set on each request
     */
    public MetabaseClient(String baseUrl, String email, String password, String deviceUuid) {
        this(baseUrl, email, password, deviceUuid, HttpClient.newBuilder().version(HTTP_1_1).build());
    }

    /**
     * Create a MetabaseClient
     *
     * @param baseUrl    URL of the Metabase server
     * @param email      login email
     * @param password   login password
     * @param deviceUuid cookie to set on each request
     * @param httpClient inject a HttpClient
     */
    MetabaseClient(String baseUrl, String email, String password, String deviceUuid, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.password = password;
        this.deviceUuid = deviceUuid;
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
            return OBJECT_MAPPER.readValue(response, SessionPropertiesResponse.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("failed to parse session properties response: {}", response);
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
                .header("Cookie", "metabase.DEVICE=" + deviceUuid)
                .POST(createBody(setupRequest))
                .build();
        String response = sendHttpRequestGetString(httpRequest, 200);
        LOGGER.info("setup response: {}", response);
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
                .header("Cookie", "metabase.DEVICE=" + deviceUuid)
                .POST(createBody(sessionRequest))
                .build();
        String response = sendHttpRequestGetString(httpRequest, 200);

        try {
            SessionResponse sessionResponse = OBJECT_MAPPER.readValue(response, SessionResponse.class);
            LOGGER.info("logged in successfully");
            LOGGER.debug("session id: {}", sessionResponse.id());
            sessionId = sessionResponse.id();
            return sessionResponse.id();

        } catch (JsonProcessingException e) {
            LOGGER.error("failed to parse session properties response: {}", response);
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
                .header("Cookie", "metabase.DEVICE=" + deviceUuid)
                .header("X-Metabase-Session", sessionId)
                .POST(createBody(createCardRequest))
                .build();
        String response = sendHttpRequestGetString(httpRequest, 202);

        try {
            CreateCardResponse createCardResponse = OBJECT_MAPPER.readValue(response, CreateCardResponse.class);
            LOGGER.info("card created successfully: {}", createCardResponse.id());
            return createCardResponse.id();

        } catch (JsonProcessingException e) {
            LOGGER.error("failed to parse create card response: {}", response);
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
                .header("Cookie", "metabase.DEVICE=" + deviceUuid)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        byte[] response = sendHttpRequestGetBytes(httpRequest, 200);
        LOGGER.info("query card response size in bytes={}", response.length);
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
            LOGGER.error("unable to create URI from string: {}{}", baseUrl, path);
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
            LOGGER.info("sending HTTP {} request to {}", httpRequest.method(), httpRequest.uri());
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != expectedStatusCode) {
                LOGGER.error("expected {} response but received {}", expectedStatusCode, response.statusCode());
                LOGGER.error("response body: {}", response.body());
                throw new RuntimeException("unexpected response status code from HTTP request");
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            LOGGER.error("HTTP request failed");
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
            LOGGER.info("sending HTTP {} request to {}", httpRequest.method(), httpRequest.uri());
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != expectedStatusCode) {
                LOGGER.error("expected {} response but received {}", expectedStatusCode, response.statusCode());
                LOGGER.error("response body: {}", response.body());
                throw new RuntimeException("unexpected response status code from HTTP request");
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            LOGGER.error("HTTP request failed");
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
            return HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(e));
        } catch (JsonProcessingException ex) {
            LOGGER.error("failed to write object as string {}", e.toString());
            throw new RuntimeException(ex);
        }
    }
}
