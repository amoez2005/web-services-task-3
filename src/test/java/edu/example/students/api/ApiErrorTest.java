package edu.example.students.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

/** Verifies the error representation's JSON contract. */
class ApiErrorTest {
    @Test
    void serializesStatusAndMessage() throws Exception {
        var mapper = new JsonMapper();
        var error = new ApiError(404, "Student was not found.");
        var json = mapper.readTree(mapper.writeValueAsString(error));
        assertEquals(404, json.get("status").asInt());
        assertEquals(error.message(), json.get("message").asText());
        assertEquals(error, mapper.treeToValue(json, ApiError.class));
    }
}
