package hu.chefrenting.api.exception;

/** No rental with the requested id; returned with HTTP 404. */
public class RentalNotFoundException extends RuntimeException {
    public RentalNotFoundException(int id) {
        super("Nem található bérlés a megadott azonosítóval: " + id);
    }
}
