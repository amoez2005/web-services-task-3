package edu.example.students;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.example.students.api.ApiExceptionMapper;
import edu.example.students.api.CorsFilter;
import edu.example.students.api.StudentResource;
import edu.example.students.model.StudentRequest;
import edu.example.students.model.Subject;
import edu.example.students.repository.InMemoryStudentRepository;
import edu.example.students.service.StudentService;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/** Composition root and standalone entry point for the JAX-RS application. */
public final class StudentApplication {
    /** Default loopback endpoint; the application is a local coursework demonstration. */
    public static final URI DEFAULT_BASE_URI = URI.create("http://localhost:8085/api/");

    /** Prevents construction of a utility entry point. */
    private StudentApplication() {}

    /**
     * Assembles resources, JSON handling, and an isolated seeded repository.
     *
     * @return application resource configuration
     */
    public static ResourceConfig createConfig() {
        StudentService service = new StudentService(new InMemoryStudentRepository());
        service.create(
                new StudentRequest(
                        "Ada Lovelace",
                        20,
                        3.8f,
                        true,
                        'A',
                        List.of(
                                new Subject("CS101", "Programming", 6),
                                new Subject("MA101", "Mathematics", 5))));
        service.create(
                new StudentRequest(
                        "Alan Turing",
                        22,
                        3.9f,
                        false,
                        'B',
                        List.of(new Subject("CS201", "Algorithms", 6))));

        JsonMapper mapper =
                JsonMapper.builder()
                        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                        .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                        .build();
        JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
        jsonProvider.setMapper(mapper);

        return new ResourceConfig()
                .register(StudentResource.class)
                .register(
                        new AbstractBinder() {
                            @Override
                            protected void configure() {
                                bind(service).to(StudentService.class);
                            }
                        })
                .register(JacksonFeature.withoutExceptionMappers())
                .register(jsonProvider)
                .register(ApiExceptionMapper.class)
                .register(CorsFilter.class)
                .property(ServerProperties.WADL_FEATURE_DISABLE, false);
    }

    /**
     * Starts an HTTP server with bounded thread pools and generated WADL.
     *
     * @param baseUri HTTP base URI, ending in a slash
     * @return the running server; the caller must shut it down
     * @throws IOException if the port cannot be bound
     */
    public static HttpServer startServer(URI baseUri) throws IOException {
        HttpServer server =
                GrizzlyHttpServerFactory.createHttpServer(baseUri, createConfig(), false);
        server.getListeners()
                .forEach(
                        listener -> {
                            listener.getTransport().setSelectorRunnersCount(2);
                            listener.getTransport()
                                    .setWorkerThreadPoolConfig(
                                            ThreadPoolConfig.defaultConfig()
                                                    .setCorePoolSize(2)
                                                    .setMaxPoolSize(8));
                        });
        try {
            server.start();
            return server;
        } catch (IOException | RuntimeException exception) {
            server.shutdownNow();
            throw exception;
        }
    }

    /**
     * Runs the server until interrupted; use Ctrl+C to stop it.
     *
     * @param args optional base URI, for example http://localhost:9090/api/
     * @throws IOException if startup fails
     * @throws InterruptedException if the main thread is interrupted
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        URI baseUri = args.length == 0 ? DEFAULT_BASE_URI : URI.create(args[0]);
        HttpServer server = startServer(baseUri);
        Runtime.getRuntime()
                .addShutdownHook(new Thread(server::shutdownNow, "student-api-shutdown"));
        System.out.println("Student API: " + baseUri.resolve("students"));
        System.out.println("WADL:        " + baseUri.resolve("application.wadl"));
        System.out.println("Press Ctrl+C to stop. Data is held in memory.");
        try {
            new CountDownLatch(1).await();
        } finally {
            server.shutdownNow();
        }
    }
}
