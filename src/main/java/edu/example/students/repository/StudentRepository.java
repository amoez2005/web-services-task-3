package edu.example.students.repository;

import edu.example.students.model.Student;
import edu.example.students.model.StudentRequest;
import java.util.List;
import java.util.Optional;

/** Storage contract independent of HTTP and of any particular database. */
public interface StudentRepository {
    /**
     * Lists every stored student.
     *
     * @return all students ordered by ascending identifier
     */
    List<Student> findAll();

    /**
     * Finds one student.
     *
     * @param id student identifier
     * @return the matching student, or an empty result
     */
    Optional<Student> findById(long id);

    /**
     * Stores a student with a new unique identifier.
     *
     * @param request validated student data
     * @return the stored student
     */
    Student create(StudentRequest request);

    /**
     * Atomically replaces an existing student, preserving its identifier.
     *
     * @param id student identifier
     * @param request replacement data
     * @return the replacement, or an empty result if the student does not exist
     */
    Optional<Student> update(long id, StudentRequest request);

    /**
     * Atomically deletes a student.
     *
     * @param id student identifier
     * @return true if a student was removed
     */
    boolean delete(long id);
}
