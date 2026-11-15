package edu.example.students.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.example.students.StudentApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Sends real HTTP requests through JAX-RS, JSON mapping, and the embedded server. */
class StudentResourceTest {
    private static HttpServer server;
    private static URI baseUri;
    private static final HttpClient CLIENT =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
    private static final JsonMapper MAPPER = new JsonMapper();
    private static final String VALID_BODY =
            """
            {"name":"Grace Hopper","age":25,"gpa":3.7,"active":true,"studyGroup":"C",
             "subjects":[{"code":"CS301","name":"Compilers","credits":6}]}
            """;

    @BeforeAll
    static void startServer() throws Exception {
        server = StudentApplication.startServer(URI.create("http://localhost:0/api/"));
        int port = server.getListeners().iterator().next().getPort();
        assertTrue(port > 0);
        baseUri = URI.create("http://localhost:" + port + "/api/");
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void completesCrudLifecycleWithDependentObjects() throws Exception {
        var created = send("POST", "students", VALID_BODY);
        assertEquals(201, created.statusCode(), created.body());
        JsonNode student = MAPPER.readTree(created.body());
        long id = student.get("id").asLong();
        String path = "students/" + id;
        assertEquals(
                baseUri.resolve(path).toString(),
                created.headers().firstValue("Location").orElseThrow());
        assertEquals("Grace Hopper", student.get("name").asText());
        assertEquals("C", student.get("studyGroup").asText());
        assertTrue(student.get("active").asBoolean());
        assertEquals(200, send("GET", path, null).statusCode());
        var subjects = send("GET", path + "/subjects", null);
        assertEquals(200, subjects.statusCode());
        assertEquals("CS301", MAPPER.readTree(subjects.body()).get(0).get("code").asText());

        var updated = send("PUT", path, VALID_BODY.replace("Grace Hopper", "Grace Murray Hopper"));
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(id, MAPPER.readTree(updated.body()).get("id").asLong());
        assertEquals("Grace Murray Hopper", MAPPER.readTree(updated.body()).get("name").asText());
        var deleted = send("DELETE", path, null);
        assertEquals(204, deleted.statusCode());
        assertEquals("", deleted.body());
        assertJsonError(send("GET", path, null), 404);
        assertJsonError(send("DELETE", path, null), 404);
    }

    @Test
    void listsSeededStudentsAndSubjects() throws Exception {
        var list = send("GET", "students", null);
        assertEquals(200, list.statusCode());
        assertTrue(MAPPER.readTree(list.body()).size() >= 2);
        var student = send("GET", "students/1", null);
        assertEquals("Ada Lovelace", MAPPER.readTree(student.body()).get("name").asText());
        assertEquals(2, MAPPER.readTree(send("GET", "students/1/subjects", null).body()).size());
    }

    @Test
    void rejectsInvalidJsonTypesMissingFieldsAndInvalidDomainValues() throws Exception {
        for (String body :
                new String[] {
                    "{",
                    "null",
                    "{}",
                    VALID_BODY + " {}",
                    VALID_BODY.replace("25", "15"),
                    VALID_BODY.replace("3.7", "4.1"),
                    VALID_BODY.replace("25", "25.5"),
                    VALID_BODY.replace("25", "\"25\""),
                    VALID_BODY.replace("true", "\"true\""),
                    VALID_BODY.replace("true", "null"),
                    VALID_BODY.replace("\"active\":true,", ""),
                    VALID_BODY.replace("\"gpa\":3.7,", ""),
                    VALID_BODY.replace("\"C\"", "\"CC\""),
                    VALID_BODY.replace("\"C\"", "\"c\""),
                    VALID_BODY.replace("\"name\":\"Grace Hopper\"", "\"unknown\":\"Grace Hopper\""),
                    VALID_BODY.replace("\"credits\":6", "\"credits\":0"),
                    VALID_BODY.replace("\"subjects\":[", "\"subjects\":[null,")
                }) {
            assertJsonError(send("POST", "students", body), 400);
        }
        assertJsonError(send("POST", "students", null), 400);
        assertJsonError(send("GET", "students/0", null), 400);
        assertJsonError(send("GET", "students/999999", null), 404);
        assertJsonError(send("GET", "students/999999/subjects", null), 404);
        assertJsonError(send("PUT", "students/999999", VALID_BODY), 404);
    }

    @Test
    void rejectsUnsupportedMediaAndMethods() throws Exception {
        var unsupported =
                CLIENT.send(
                        HttpRequest.newBuilder(baseUri.resolve("students"))
                                .header("Content-Type", "text/plain")
                                .POST(HttpRequest.BodyPublishers.ofString(VALID_BODY))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertJsonError(unsupported, 415);
        assertJsonError(send("PATCH", "students/1", VALID_BODY), 405);
        assertJsonError(send("GET", "unknown", null), 404);
    }

    private static HttpResponse<String> send(String method, String path, String body)
            throws Exception {
        return CLIENT.send(
                HttpRequest.newBuilder(baseUri.resolve(path))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .method(
                                method,
                                body == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void assertJsonError(HttpResponse<String> response, int status)
            throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(
                response.headers()
                        .firstValue("Content-Type")
                        .orElse("")
                        .contains("application/json"),
                response.headers() + " " + response.body());
        assertEquals(status, MAPPER.readTree(response.body()).get("status").asInt());
        assertFalse(MAPPER.readTree(response.body()).get("message").asText().isBlank());
    }
}
