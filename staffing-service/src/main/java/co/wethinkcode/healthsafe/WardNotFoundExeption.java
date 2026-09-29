package co.wethinkcode.healthsafe;

/** The ward doesn't exist according to ward-service. Mapped to HTTP 404. */
public class WardNotFoundException extends RuntimeException {

    public WardNotFoundException(String wardId) {
        super("ward not found: " + wardId);
    }
}