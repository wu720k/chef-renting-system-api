package hu.chefrenting.api.exception;

/** A POST business rule was violated; the message is Hungarian and returned with HTTP 400. */
public class InvalidRentalException extends RuntimeException {
    public InvalidRentalException(String message) {
        super(message);
    }
}
