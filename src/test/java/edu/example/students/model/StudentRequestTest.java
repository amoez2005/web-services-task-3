package edu.example.students.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies all input rules, boundaries, and defensive list copying. */
class StudentRequestTest {
    @Test
    void preservesEveryFieldAndCopiesSubjects() {
        List<Subject> subjects = new ArrayList<>(List.of(new Subject("CS", "Computing", 6)));
        StudentRequest request = new StudentRequest(" Ada ", 20, 3.8f, true, 'A', subjects);
        subjects.clear();
        assertAll(
                () -> assertEquals("Ada", request.name()),
                () -> assertEquals(20, request.age()),
                () -> assertEquals(3.8f, request.gpa()),
                () -> assertTrue(request.active()),
                () -> assertEquals('A', request.studyGroup()),
                () -> assertEquals(1, request.subjects().size()),
                () ->
                        assertThrows(
                                UnsupportedOperationException.class,
                                () -> request.subjects().clear()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankNames(String name) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest(name, 20, 3, true, 'A', List.of()));
    }

    @Test
    void rejectsOverlongNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest("x".repeat(101), 20, 3, true, 'A', List.of()));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 15, 121})
    void rejectsInvalidAges(int age) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest("Ada", age, 3, true, 'A', List.of()));
    }

    @ParameterizedTest
    @ValueSource(
            floats = {-0.1f, 4.1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void rejectsInvalidGpas(float gpa) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest("Ada", 20, gpa, true, 'A', List.of()));
    }

    @ParameterizedTest
    @ValueSource(chars = {'a', '1', '@', '[', '\0'})
    void rejectsInvalidStudyGroups(char group) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest("Ada", 20, 3, true, group, List.of()));
    }

    @Test
    void rejectsMissingNullAndDuplicateSubjects() {
        Subject subject = new Subject("CS", "Computing", 6);
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest("Ada", 20, 3, true, 'A', null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest("Ada", 20, 3, true, 'A', Arrays.asList(subject, null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StudentRequest("Ada", 20, 3, true, 'A', List.of(subject, subject)));
    }

    @Test
    void acceptsInclusiveBoundsAndEmptySubjects() {
        assertNotNull(new StudentRequest("Ada", 16, 0, false, 'A', List.of()));
        assertNotNull(new StudentRequest("x".repeat(100), 120, 4, true, 'Z', List.of()));
    }
}
