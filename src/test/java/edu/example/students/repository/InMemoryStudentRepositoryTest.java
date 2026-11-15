package edu.example.students.repository;

import static org.junit.jupiter.api.Assertions.*;

import edu.example.students.model.Student;
import edu.example.students.model.StudentRequest;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Exercises the repository contract, atomic mutations, and concurrent identifiers. */
class InMemoryStudentRepositoryTest {
    private static StudentRequest request(String name) {
        return new StudentRequest(name, 20, 3, true, 'A', List.of());
    }

    @Test
    void fulfillsCrudContractThroughInterface() {
        StudentRepository repository = new InMemoryStudentRepository();
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findById(1).isEmpty());
        Student first = repository.create(request("Ada"));
        Student second = repository.create(request("Alan"));
        assertEquals(List.of(first, second), repository.findAll());
        assertEquals(first, repository.findById(first.id()).orElseThrow());
        Student updated = repository.update(first.id(), request("Grace")).orElseThrow();
        assertEquals(first.id(), updated.id());
        assertEquals("Grace", updated.name());
        assertTrue(repository.update(999, request("Nobody")).isEmpty());
        assertTrue(repository.delete(first.id()));
        assertFalse(repository.delete(first.id()));
        assertTrue(repository.findById(first.id()).isEmpty());
        assertTrue(repository.create(request("New")).id() > second.id());
    }

    @Test
    void returnsImmutableSnapshots() {
        StudentRepository repository = new InMemoryStudentRepository();
        repository.create(request("Ada"));
        List<Student> snapshot = repository.findAll();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        repository.create(request("Alan"));
        assertEquals(1, snapshot.size());
    }

    @Test
    void assignsUniqueIdsUnderConcurrentCreation() throws Exception {
        StudentRepository repository = new InMemoryStudentRepository();
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Long>> tasks =
                    IntStream.range(0, 100)
                            .mapToObj(
                                    index ->
                                            (Callable<Long>)
                                                    () ->
                                                            repository
                                                                    .create(
                                                                            request(
                                                                                    "Student "
                                                                                            + index))
                                                                    .id())
                            .toList();
            var ids = new HashSet<Long>();
            for (var result : executor.invokeAll(tasks)) {
                ids.add(result.get());
            }
            assertEquals(100, ids.size());
            assertEquals(100, repository.findAll().size());
        } finally {
            executor.shutdownNow();
        }
    }
}
