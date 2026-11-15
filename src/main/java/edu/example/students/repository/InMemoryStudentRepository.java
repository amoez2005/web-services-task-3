package edu.example.students.repository;

import edu.example.students.model.Student;
import edu.example.students.model.StudentRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Thread-safe, process-local storage; data resets when the application restarts. */
public final class InMemoryStudentRepository implements StudentRepository {
    /** Students indexed by their stable identifier. */
    private final Map<Long, Student> students = new TreeMap<>();

    /** Next unused identifier; access is protected by this repository's monitor. */
    private long nextId = 1;

    /** Creates an empty repository. */
    public InMemoryStudentRepository() {}

    @Override
    public synchronized List<Student> findAll() {
        return List.copyOf(students.values());
    }

    @Override
    public synchronized Optional<Student> findById(long id) {
        return Optional.ofNullable(students.get(id));
    }

    @Override
    public synchronized Student create(StudentRequest request) {
        Student student = Student.from(nextId, request);
        students.put(nextId++, student);
        return student;
    }

    @Override
    public synchronized Optional<Student> update(long id, StudentRequest request) {
        if (!students.containsKey(id)) {
            return Optional.empty();
        }
        Student student = Student.from(id, request);
        students.put(id, student);
        return Optional.of(student);
    }

    @Override
    public synchronized boolean delete(long id) {
        return students.remove(id) != null;
    }
}
