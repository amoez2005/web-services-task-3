package edu.example.students.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Converts domain, JSON, and HTTP exceptions to predictable JSON responses. */
@Provider
public final class ApiExceptionMapper implements ExceptionMapper<Exception> {
    /** Server-side logging for unexpected failures. */
    private static final Logger LOGGER = Logger.getLogger(ApiExceptionMapper.class.getName());

    /** Creates the exception adapter. */
    public ApiExceptionMapper() {}

    @Override
    public Response toResponse(Exception exception) {
        int status;
        String message;
        Response.ResponseBuilder builder;
        if (exception instanceof JsonProcessingException) {
            status = 400;
            message =
                    "Invalid student JSON: check field names, types, required fields and allowed values.";
        } else if (exception instanceof IllegalArgumentException) {
            status = 400;
            message = exception.getMessage();
        } else if (exception instanceof NoSuchElementException) {
            status = 404;
            message = exception.getMessage();
        } else if (exception instanceof WebApplicationException httpException) {
            status = httpException.getResponse().getStatus();
            message = httpException.getMessage();
        } else {
            status = 500;
            message = "An unexpected server error occurred.";
            LOGGER.log(Level.SEVERE, message, exception);
        }
        if (exception instanceof WebApplicationException httpException) {
            builder = Response.fromResponse(httpException.getResponse());
        } else {
            builder = Response.status(status);
        }
        return builder.type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ApiError(status, message))
                .build();
    }
}
