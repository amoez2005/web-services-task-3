package edu.example.students.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validated immutable data accepted by create and replace operations. Primitive Java fields
 * demonstrate String, int, float, boolean, and char JSON mapping.
 *
 * @param name student name
 * @param age age in years
 * @param gpa grade point average on a four-point scale
 * @param active whether the student is currently enrolled
 * @param studyGroup uppercase group letter
 * @param subjects dependent subjects; an empty list is allowed
 */
public record StudentRequest(
        String name, int age, float gpa, boolean active, char studyGroup, List<Subject> subjects) {
    /**
     * Validates student data and takes an immutable snapshot of the subjects.
     *
     * @param name nonblank name of at most 100 characters
     * @param age age from 16 to 120
     * @param gpa finite GPA from 0 to 4
     * @param active current enrollment flag
     * @param studyGroup a letter from A to Z
     * @param subjects nonnull list with distinct subject codes and no null entries
     * @throws IllegalArgumentException if any value is invalid
     */
    public StudentRequest {
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new IllegalArgumentException("Student name must contain 1 to 100 characters.");
        }
        if (age < 16 || age > 120) {
            throw new IllegalArgumentException("Student age must be between 16 and 120.");
        }
        if (!Float.isFinite(gpa) || gpa < 0 || gpa > 4) {
            throw new IllegalArgumentException("Student GPA must be between 0 and 4.");
        }
        if (studyGroup < 'A' || studyGroup > 'Z') {
            throw new IllegalArgumentException("Study group must be one uppercase letter A to Z.");
        }
        if (subjects == null || subjects.stream().anyMatch(subject -> subject == null)) {
            throw new IllegalArgumentException("Subjects must be a list without null entries.");
        }
        Set<String> codes = new HashSet<>();
        for (Subject subject : subjects) {
            if (!codes.add(subject.code())) {
                throw new IllegalArgumentException("Subject codes must be unique for a student.");
            }
        }
        name = name.strip();
        subjects = List.copyOf(subjects);
    }
}
