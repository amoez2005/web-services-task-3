package edu.example.students.api;

import edu.example.students.model.Student;
import edu.example.students.model.StudentRequest;
import edu.example.students.model.Subject;
import edu.example.students.service.StudentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/** JAX-RS HTTP adapter for the student use cases. */
@Path("students")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class StudentResource {
    /** Application use cases; the resource contains no persistence logic. */
    private final StudentService service;

    /**
     * Creates the HTTP resource.
     *
     * @param service student use cases
     */
    @Inject
    public StudentResource(StudentService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    /**
     * Lists all student representations.
     *
     * @return all students as JSON
     */
    @GET
    public List<Student> listStudents() {
        return service.findAll();
    }

    /**
     * Retrieves a student.
     *
     * @param id path identifier
     * @return the matching student
     */
    @GET
    @Path("{id}")
    public Student getStudent(@PathParam("id") long id) {
        return service.findById(id);
    }

    /**
     * Creates a student and supplies the new resource's Location header.
     *
     * @param request JSON input
     * @param uriInfo request URI context
     * @return HTTP 201 with the created representation
     */
    @POST
    public Response createStudent(StudentRequest request, @Context UriInfo uriInfo) {
        Student student = service.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(student.id())).build();
        return Response.created(location).entity(student).build();
    }

    /**
     * Replaces a student's complete editable data.
     *
     * @param id path identifier
     * @param request replacement JSON input
     * @return the updated representation
     */
    @PUT
    @Path("{id}")
    public Student updateStudent(@PathParam("id") long id, StudentRequest request) {
        return service.update(id, request);
    }

    /**
     * Deletes a student.
     *
     * @param id path identifier
     * @return HTTP 204 with no body
     */
    @DELETE
    @Path("{id}")
    public Response deleteStudent(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    /**
     * Retrieves dependent subjects.
     *
     * @param id student path identifier
     * @return the student's subjects
     */
    @GET
    @Path("{id}/subjects")
    public List<Subject> listSubjects(@PathParam("id") long id) {
        return service.findSubjects(id);
    }
}
