package edu.example.students.service;

import static org.junit.jupiter.api.Assertions.*;

import edu.example.students.model.StudentRequest;
import edu.example.students.model.Subject;
import edu.example.students.repository.InMemoryStudentRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies use-case results and missing-resource behavior independently of HTTP. */
class StudentServiceTest {
    private StudentService service;
    private StudentRequest request;

    @BeforeEach
    void setUp() {
        service = new StudentService(new InMemoryStudentRepository());
        request =
                new StudentRequest(
                        "Ada", 20, 3.8f, true, 'A', List.of(new Subject("CS", "Computing", 6)));
    }

    @Test
    void supportsAllUseCases() {
        var student = service.create(request);
        assertEquals(List.of(student), service.findAll());
        assertEquals(student, service.findById(student.id()));
        assertEquals(request.subjects(), service.findSubjects(student.id()));
        var replacement = new StudentRequest("Grace", 30, 4, false, 'B', List.of());
        assertEquals("Grace", service.update(student.id(), replacement).name());
        service.delete(student.id());
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void reportsMissingStudentsForAllItemOperations() {
        assertThrows(NoSuchElementException.class, () -> service.findById(99));
        assertThrows(NoSuchElementException.class, () -> service.findSubjects(99));
        assertThrows(NoSuchElementException.class, () -> service.update(99, request));
        assertThrows(NoSuchElementException.class, () -> service.delete(99));
    }

    @Test
    void rejectsInvalidIdentifiersAndMissingRequests() {
        assertThrows(IllegalArgumentException.class, () -> service.findById(0));
        assertThrows(IllegalArgumentException.class, () -> service.update(-1, request));
        assertThrows(IllegalArgumentException.class, () -> service.delete(0));
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
        assertThrows(IllegalArgumentException.class, () -> service.update(1, null));
        assertThrows(NullPointerException.class, () -> new StudentService(null));
    }
}
