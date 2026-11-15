package edu.example.students.service;

import edu.example.students.model.Student;
import edu.example.students.model.StudentRequest;
import edu.example.students.model.Subject;
import edu.example.students.repository.StudentRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Student use cases, depending only on a storage interface and domain values. */
public final class StudentService {
    /** Storage dependency supplied by the composition root. */
    private final StudentRepository repository;

    /**
     * Creates a service with an explicitly supplied repository.
     *
     * @param repository storage implementation
     */
    public StudentService(StudentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Lists the current students.
     *
     * @return an immutable snapshot of all students
     */
    public List<Student> findAll() {
        return repository.findAll();
    }

    /**
     * Finds an existing student.
     *
     * @param id positive student identifier
     * @return the requested student
     * @throws NoSuchElementException if no student exists
     */
    public Student findById(long id) {
        requirePositiveId(id);
        return repository.findById(id).orElseThrow(() -> missingStudent(id));
    }

    /**
     * Creates a student.
     *
     * @param request validated input
     * @return the stored student
     */
    public Student create(StudentRequest request) {
        return repository.create(requireRequest(request));
    }

    /**
     * Replaces an existing student.
     *
     * @param id positive student identifier
     * @param request complete replacement data
     * @return the updated student
     * @throws NoSuchElementException if no student exists
     */
    public Student update(long id, StudentRequest request) {
        requirePositiveId(id);
        return repository.update(id, requireRequest(request)).orElseThrow(() -> missingStudent(id));
    }

    /**
     * Deletes an existing student.
     *
     * @param id positive student identifier
     * @throws NoSuchElementException if no student exists
     */
    public void delete(long id) {
        requirePositiveId(id);
        if (!repository.delete(id)) {
            throw missingStudent(id);
        }
    }

    /**
     * Lists a student's dependent subjects.
     *
     * @param id positive student identifier
     * @return an immutable subject list
     */
    public List<Subject> findSubjects(long id) {
        return findById(id).subjects();
    }

    /**
     * Rejects nonpositive identifiers before accessing storage.
     *
     * @param id candidate identifier
     */
    private static void requirePositiveId(long id) {
        if (id < 1) {
            throw new IllegalArgumentException("Student identifier must be positive.");
        }
    }

    /**
     * Rejects an absent request body.
     *
     * @param request candidate input
     * @return the nonnull input
     */
    private static StudentRequest requireRequest(StudentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A student JSON request body is required.");
        }
        return request;
    }

    /**
     * Builds the domain-level missing-student exception.
     *
     * @param id missing identifier
     * @return an exception suitable for propagation to an adapter
     */
    private static NoSuchElementException missingStudent(long id) {
        return new NoSuchElementException("Student " + id + " was not found.");
    }
}
