package edu.example.students.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;

/** Permits browser-based exploration from Swagger Editor and the local Swagger UI. */
@Provider
@PreMatching
public final class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    /** Explicit browser origins allowed by this local educational service. */
    private static final Set<String> ALLOWED_ORIGINS =
            Set.of("https://editor.swagger.io", "http://localhost:8081", "http://127.0.0.1:8081");

    /** Creates the browser-access filter. */
    public CorsFilter() {}

    @Override
    public void filter(ContainerRequestContext request) {
        if ("OPTIONS".equals(request.getMethod()) && isAllowed(request)) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().add("Vary", "Origin");
        if (isAllowed(request)) {
            response.getHeaders()
                    .putSingle("Access-Control-Allow-Origin", request.getHeaderString("Origin"));
            response.getHeaders()
                    .putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type, Accept");
            response.getHeaders().putSingle("Access-Control-Expose-Headers", "Location");
            response.getHeaders().putSingle("Access-Control-Max-Age", "600");
        }
    }

    /**
     * Checks whether a browser origin is trusted for this demonstration.
     *
     * @param request incoming HTTP context
     * @return true for an explicitly allowed origin
     */
    private static boolean isAllowed(ContainerRequestContext request) {
        String origin = request.getHeaderString("Origin");
        return origin != null && ALLOWED_ORIGINS.contains(origin);
    }
}
