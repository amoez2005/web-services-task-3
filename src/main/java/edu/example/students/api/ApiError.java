package edu.example.students.api;

/**
 * Consistent JSON error representation.
 *
 * @param status HTTP status code
 * @param message human-readable error description
 */
public record ApiError(int status, String message) {}
