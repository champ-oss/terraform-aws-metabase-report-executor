package com.champtitles.metabasereportexecutor.executor;

import com.champtitles.metabasereportexecutor.executor.model.SessionPropertiesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

class MetabaseClientTest {
    @InjectMocks
    MetabaseClient metabaseClient;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponseString;

    @Mock
    private HttpResponse<byte[]> httpResponseBytes;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        metabaseClient = new MetabaseClient("http://localhost:12345", "test@example.com", "test123", "abc123", httpClient);
    }

    @Test
    void getSessionProperties_returnsSetupToken_with200Response() throws IOException, InterruptedException, URISyntaxException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("""
                {"setup-token":"abc123"}
                """);

        SessionPropertiesResponse sessionPropertiesResponse = metabaseClient.getSessionProperties();
        HttpRequest expectedHttpRequest = HttpRequest.newBuilder().uri(new URI("http://localhost:12345/api/session/properties")).GET().build();
        Mockito.verify(this.httpClient, Mockito.times(1)).send(Mockito.eq(expectedHttpRequest), eq(HttpResponse.BodyHandlers.ofString()));
        assertEquals("abc123", sessionPropertiesResponse.setupToken());
    }

    @Test
    void getSessionProperties_throwsRuntimeException_withBadResponseBody() throws IOException, InterruptedException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("foo");

        assertThrows(RuntimeException.class, () -> {
            metabaseClient.getSessionProperties();
        });
        Mockito.verify(this.httpClient, Mockito.times(1)).send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void getSessionProperties_throwsRuntimeException_withUnexpectedStatusCode() throws IOException, InterruptedException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(400);
        Mockito.when(httpResponseString.body()).thenReturn("invalid request");

        assertThrows(RuntimeException.class, () -> {
            metabaseClient.getSessionProperties();
        });
        Mockito.verify(this.httpClient, Mockito.times(1)).send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void completeInitialSetup() throws IOException, InterruptedException, URISyntaxException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("done");

        metabaseClient.completeInitialSetup("abc123");

        HttpRequest expectedHttpRequest = HttpRequest
                .newBuilder()
                .uri(new URI("http://localhost:12345/api/setup"))
                .header("Content-Type", "application/json")
                .header("Cookie", "metabase.DEVICE=abc123")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "token": "abc123",
                          "prefs": {
                            "site_name": "test",
                            "site_locale": "en",
                            "allow_tracking": false
                          },
                          "database": null,
                          "user": {
                            "first_name": "test",
                            "last_name": "test",
                            "site_name": "test",
                            "email": "test@example.com",
                            "password": "test123"
                          }
                        }
                        """))
                .build();
        Mockito.verify(this.httpClient, Mockito.times(1)).send(Mockito.eq(expectedHttpRequest), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void loginAndGetSession_returnsSessionId_with200Response() throws IOException, InterruptedException, URISyntaxException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("""
                {"id":"abc123"}
                """);

        metabaseClient.loginAndGetSession();

        HttpRequest expectedHttpRequest = HttpRequest
                .newBuilder()
                .uri(new URI("http://localhost:12345/api/session"))
                .header("Content-Type", "application/json")
                .header("Cookie", "metabase.DEVICE=abc123")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "username": "test@example.com",
                          "password": "test123",
                        }
                        """))
                .build();
        Mockito.verify(this.httpClient, Mockito.times(1)).send(Mockito.eq(expectedHttpRequest), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void loginAndGetSession_throwsRuntimeException_withBadResponseBody() throws IOException, InterruptedException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("foo");

        assertThrows(RuntimeException.class, () -> {
            metabaseClient.loginAndGetSession();
        });
        Mockito.verify(this.httpClient, Mockito.times(1)).send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void createCard_returnsCardId_with202Response() throws IOException, InterruptedException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("""
                {"id":"abc123"}
                """);
        metabaseClient.loginAndGetSession();

        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(202);
        Mockito.when(httpResponseString.body()).thenReturn("""
                {"id":"1"}
                """);
        String cardId = metabaseClient.createCard("test");
        assertEquals("1", cardId);
        Mockito.verify(this.httpClient, Mockito.times(2)).send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void createCard_throwsRuntimeException_withMissingSessionId() throws IOException, InterruptedException {
        assertThrows(RuntimeException.class, () -> {
            metabaseClient.createCard("test");
        });
        Mockito.verify(this.httpClient, Mockito.times(0)).send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void createCard_throwsRuntimeException_withBadResponseBody() throws IOException, InterruptedException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("""
                {"id":"abc123"}
                """);
        metabaseClient.loginAndGetSession();

        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("foo");

        assertThrows(RuntimeException.class, () -> {
            metabaseClient.createCard("test");
        });
        Mockito.verify(this.httpClient, Mockito.times(2)).send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void queryCardGetXlsx_returnsString_with200Response() throws IOException, InterruptedException, URISyntaxException {
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponseString);
        Mockito.when(httpResponseString.statusCode()).thenReturn(200);
        Mockito.when(httpResponseString.body()).thenReturn("""
                {"id":"abc123"}
                """);
        metabaseClient.loginAndGetSession();

        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray()))).thenReturn(httpResponseBytes);
        Mockito.when(httpResponseBytes.statusCode()).thenReturn(200);
        Mockito.when(httpResponseBytes.body()).thenReturn("data".getBytes(StandardCharsets.US_ASCII));

        byte[] data = metabaseClient.queryCardGetXlsx("1");
        assertArrayEquals("data".getBytes(StandardCharsets.US_ASCII), data);
        HttpRequest expectedHttpRequest = HttpRequest
                .newBuilder()
                .uri(new URI("http://localhost:12345/api/card/1/query/xlsx"))
                .header("X-Metabase-Session", "abc123")
                .header("Cookie", "metabase.DEVICE=abc123")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        Mockito.verify(this.httpClient, Mockito.times(1)).send(Mockito.eq(expectedHttpRequest), eq(HttpResponse.BodyHandlers.ofByteArray()));
    }

    @Test
    void queryCardGetXlsx_throwsRuntimeException_withMissingSessionId() throws IOException, InterruptedException {
        assertThrows(RuntimeException.class, () -> {
            metabaseClient.queryCardGetXlsx("1");
        });
        Mockito.verify(this.httpClient, Mockito.times(0)).send(Mockito.any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }
}