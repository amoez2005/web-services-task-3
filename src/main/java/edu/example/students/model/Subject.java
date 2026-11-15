package edu.example.students.model;

/**
 * An immutable subject associated with a student.
 *
 * @param code subject identifier, for example CS101
 * @param name human-readable subject name
 * @param credits credit count from 1 to 30
 */
public record Subject(String code, String name, int credits) {
    /**
     * Validates and normalizes a subject.
     *
     * @param code nonblank subject identifier, at most 20 characters
     * @param name nonblank subject name, at most 100 characters
     * @param credits credit count from 1 to 30
     * @throws IllegalArgumentException if a value is invalid
     */
    public Subject {
        if (code == null || code.isBlank() || code.strip().length() > 20) {
            throw new IllegalArgumentException("Subject code must contain 1 to 20 characters.");
        }
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new IllegalArgumentException("Subject name must contain 1 to 100 characters.");
        }
        if (credits < 1 || credits > 30) {
            throw new IllegalArgumentException("Subject credits must be between 1 and 30.");
        }
        code = code.strip();
        name = name.strip();
    }
}
