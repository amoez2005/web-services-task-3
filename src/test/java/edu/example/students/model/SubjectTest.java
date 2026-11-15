package edu.example.students.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks subject invariants and immutable value semantics. */
class SubjectTest {
    @Test
    void normalizesTextAndPreservesFields() {
        Subject subject = new Subject(" CS101 ", " Programming ", 6);
        assertAll(
                () -> assertEquals("CS101", subject.code()),
                () -> assertEquals("Programming", subject.name()),
                () -> assertEquals(6, subject.credits()),
                () -> assertEquals(new Subject("CS101", "Programming", 6), subject));
    }

    static Stream<String> invalidText() {
        return Stream.of(null, "", "   ", "x".repeat(101));
    }

    @ParameterizedTest
    @MethodSource("invalidText")
    void rejectsInvalidCodeAndName(String text) {
        assertThrows(IllegalArgumentException.class, () -> new Subject(text, "Valid", 1));
        assertThrows(IllegalArgumentException.class, () -> new Subject("CS", text, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 31})
    void rejectsInvalidCredits(int credits) {
        assertThrows(IllegalArgumentException.class, () -> new Subject("CS", "Computing", credits));
    }

    @Test
    void acceptsInclusiveCreditBounds() {
        assertEquals(1, new Subject("A", "A", 1).credits());
        assertEquals(30, new Subject("B", "B", 30).credits());
    }
}
