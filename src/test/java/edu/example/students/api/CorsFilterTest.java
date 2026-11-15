package edu.example.students.api;

import static org.junit.jupiter.api.Assertions.*;

import edu.example.students.StudentApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/** Verifies allowed and disallowed browser origins against a running server. */
class CorsFilterTest {
    @Test
    void permitsSwaggerPreflightAndExposesLocationWithoutTrustingOtherOrigins() throws Exception {
        var server = StudentApplication.startServer(URI.create("http://localhost:0/api/"));
        try {
            int port = server.getListeners().iterator().next().getPort();
            URI uri = URI.create("http://localhost:" + port + "/api/students");
            var client = HttpClient.newHttpClient();
            var preflight =
                    client.send(
                            HttpRequest.newBuilder(uri)
                                    .header("Origin", "https://editor.swagger.io")
                                    .header("Access-Control-Request-Method", "POST")
                                    .header("Access-Control-Request-Headers", "content-type")
                                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertEquals(204, preflight.statusCode());
            assertEquals(
                    "https://editor.swagger.io",
                    preflight.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
            assertTrue(
                    preflight
                            .headers()
                            .firstValue("Access-Control-Allow-Methods")
                            .orElseThrow()
                            .contains("POST"));
            assertEquals(
                    "Location",
                    preflight.headers().firstValue("Access-Control-Expose-Headers").orElseThrow());
            for (String origin :
                    new String[] {
                        "http://localhost:8081",
                        "http://127.0.0.1:8081",
                        "https://untrusted.example"
                    }) {
                var response =
                        client.send(
                                HttpRequest.newBuilder(uri).header("Origin", origin).GET().build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertEquals(
                        !origin.contains("untrusted"),
                        response.headers().firstValue("Access-Control-Allow-Origin").isPresent());
            }
        } finally {
            server.shutdownNow();
        }
    }
}
