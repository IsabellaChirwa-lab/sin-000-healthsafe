package co.wethinkcode.healthsafe;

/** A service we depend on is down, too slow, or returned unusable data. Mapped to HTTP 503. */
public class DownstreamUnavailableException extends RuntimeException {

    public DownstreamUnavailableException(String message) {
        super(message);
    }

    public DownstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}