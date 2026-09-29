package co.wethinkcode.healthsafe;

/** Where staffing events go. An interface so tests can use a fake instead of a real broker. */
public interface StaffingEventPublisher {

    void publish(StaffingEvent event);

    class PublishException extends RuntimeException {
        public PublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
