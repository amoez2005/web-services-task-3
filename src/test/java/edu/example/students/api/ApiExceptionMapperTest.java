package edu.example.students.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Checks error status mapping, HTTP headers, and hiding internal error details. */
class ApiExceptionMapperTest {
    private final ApiExceptionMapper mapper = new ApiExceptionMapper();

    @Test
    void mapsDomainAndJsonFailures() {
        assertError(new IllegalArgumentException("Invalid age"), 400, "Invalid age");
        assertError(new NoSuchElementException("Missing"), 404, "Missing");
        try (var response = mapper.toResponse(new JsonParseException(null, "bad JSON"))) {
            assertEquals(400, response.getStatus());
            assertTrue(
                    ((ApiError) response.getEntity()).message().startsWith("Invalid student JSON"));
        }
    }

    @Test
    void preservesHttpStatusAndAllowHeader() {
        try (var response = mapper.toResponse(new NotFoundException())) {
            assertEquals(404, response.getStatus());
        }
        try (var response =
                mapper.toResponse(new NotAllowedException("GET", new String[] {"POST"}))) {
            assertEquals(405, response.getStatus());
            assertEquals(java.util.Set.of("GET", "POST"), response.getAllowedMethods());
        }
    }

    @Test
    void hidesUnexpectedFailureDetails() {
        assertError(
                new IllegalStateException("secret internal detail"),
                500,
                "An unexpected server error occurred.");
    }

    private void assertError(Exception exception, int status, String message) {
        try (var response = mapper.toResponse(exception)) {
            assertEquals(status, response.getStatus());
            assertEquals("application/json", response.getMediaType().toString());
            assertEquals(new ApiError(status, message), response.getEntity());
        }
    }
}
