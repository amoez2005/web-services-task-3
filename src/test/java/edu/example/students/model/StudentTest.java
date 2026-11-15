package edu.example.students.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks the persisted representation and server-assigned identifier invariant. */
class StudentTest {
    @Test
    void buildsRepresentationFromValidatedRequest() {
        StudentRequest request =
                new StudentRequest(
                        "Ada", 20, 3.8f, true, 'A', List.of(new Subject("CS", "Computing", 6)));
        Student student = Student.from(42, request);
        assertAll(
                () -> assertEquals(42, student.id()),
                () -> assertEquals(request.name(), student.name()),
                () -> assertEquals(request.age(), student.age()),
                () -> assertEquals(request.gpa(), student.gpa()),
                () -> assertEquals(request.active(), student.active()),
                () -> assertEquals(request.studyGroup(), student.studyGroup()),
                () -> assertEquals(request.subjects(), student.subjects()));
    }

    @Test
    void rejectsInvalidIdentifiersAndInvalidData() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Student(0, "Ada", 20, 3, true, 'A', List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Student(-1, "Ada", 20, 3, true, 'A', List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Student(1, "", 20, 3, true, 'A', List.of()));
    }
}
