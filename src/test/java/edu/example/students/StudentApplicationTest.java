package edu.example.students;

import static org.junit.jupiter.api.Assertions.*;

import edu.example.students.api.StudentResource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

/** Verifies application wiring and actual generated WADL publication. */
class StudentApplicationTest {
    @Test
    void registersTheStudentResourceAndUsesTheDocumentedDefaultUri() {
        assertEquals(URI.create("http://localhost:8085/api/"), StudentApplication.DEFAULT_BASE_URI);
        assertTrue(StudentApplication.createConfig().getClasses().contains(StudentResource.class));
    }

    @Test
    void publishesWadlContainingAllExplicitResourceMethods() throws Exception {
        var server = StudentApplication.startServer(URI.create("http://localhost:0/api/"));
        try {
            int port = server.getListeners().iterator().next().getPort();
            var response =
                    HttpClient.newHttpClient()
                            .send(
                                    HttpRequest.newBuilder(
                                                    URI.create(
                                                            "http://localhost:"
                                                                    + port
                                                                    + "/api/application.wadl"))
                                            .GET()
                                            .build(),
                                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document =
                    factory.newDocumentBuilder()
                            .parse(
                                    new java.io.ByteArrayInputStream(
                                            response.body()
                                                    .getBytes(
                                                            java.nio.charset.StandardCharsets
                                                                    .UTF_8)));
            assertEquals("application", document.getDocumentElement().getLocalName());
            for (String method : new String[] {"GET", "POST", "PUT", "DELETE"}) {
                assertTrue(response.body().contains("name=\"" + method + "\""), response.body());
            }
            assertTrue(response.body().contains("subjects"));
        } finally {
            server.shutdownNow();
        }
    }
}
