package hu.chefrenting.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidRentalException.class)
    ResponseEntity<ApiError> invalidRental(InvalidRentalException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(RentalNotFoundException.class)
    ResponseEntity<ApiError> notFound(RentalNotFoundException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).sorted().collect(Collectors.joining(" "));
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "A kérés törzse hiányzik vagy hibás JSON (pl. érvénytelen dátum vagy szám). "
                        + "Dátumformátum: yyyy-MM-dd.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Érvénytelen paraméter: " + e.getName()
                + " egész szám kell legyen.", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Ez a HTTP metódus ezen az útvonalon nem támogatott.", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "A kért útvonal nem létezik.", req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest req) {
        log.error("Váratlan hiba: {}", req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Váratlan szerverhiba történt.", req);
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(new ApiError(OffsetDateTime.now(), status.value(),
                status.getReasonPhrase(), message, req.getRequestURI()));
    }
}
