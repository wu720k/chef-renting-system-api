package hu.chefrenting.api.exception;

import java.time.OffsetDateTime;

/** Uniform JSON error body. */
public record ApiError(OffsetDateTime timestamp, int status, String error, String message, String path) {
}
