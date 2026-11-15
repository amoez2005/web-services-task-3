package edu.example.students.model;

import java.util.List;

/**
 * An immutable student representation with a server-assigned identifier.
 *
 * @param id positive student identifier
 * @param name student name
 * @param age age in years
 * @param gpa grade point average
 * @param active current enrollment flag
 * @param studyGroup uppercase group letter
 * @param subjects associated subjects
 */
public record Student(
        long id,
        String name,
        int age,
        float gpa,
        boolean active,
        char studyGroup,
        List<Subject> subjects) {
    /**
     * Creates a valid student and copies its dependent subjects.
     *
     * @param id positive identifier
     * @param name student name
     * @param age age in years
     * @param gpa grade point average
     * @param active current enrollment flag
     * @param studyGroup uppercase group letter
     * @param subjects associated subjects
     * @throws IllegalArgumentException if student data or the identifier is invalid
     */
    public Student {
        if (id < 1) {
            throw new IllegalArgumentException("Student identifier must be positive.");
        }
        StudentRequest validated = new StudentRequest(name, age, gpa, active, studyGroup, subjects);
        name = validated.name();
        subjects = validated.subjects();
    }

    /**
     * Combines a server-assigned identifier with validated request data.
     *
     * @param id positive identifier
     * @param request validated student data
     * @return a new student
     */
    public static Student from(long id, StudentRequest request) {
        return new Student(
                id,
                request.name(),
                request.age(),
                request.gpa(),
                request.active(),
                request.studyGroup(),
                request.subjects());
    }
}
