package co.wethinkcode.healthsafe;

/** Thrown when ingestion-service can't be reached or returns something unusable. Mapped to HTTP 503. */
public class IngestionUnavailableException extends RuntimeException {

    public IngestionUnavailableException(String message) {
        super(message);
    }

    public IngestionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}