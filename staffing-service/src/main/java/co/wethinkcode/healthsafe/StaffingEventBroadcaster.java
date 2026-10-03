package co.wethinkcode.healthsafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broadcasts a staffing event only when a ward's staffing actually CHANGED, so repeated identical
 * requests don't spam subscribers.
 *
 * <p>Broadcasting is best-effort by design: if the broker is down we log it and carry on. The REST
 * response must never fail because a topic subscriber can't be reached (that is the point of
 * decoupling), and because a failed event isn't recorded as sent, the next request retries it.
 */
public class StaffingEventBroadcaster {

    private final StaffingEventPublisher publisher;
    private final Map<String, StaffingEvent> lastPublished = new ConcurrentHashMap<>();

    public StaffingEventBroadcaster(StaffingEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void onScheduleComputed(StaffingSchedule schedule) {
        StaffingEvent event = StaffingEvent.from(schedule);
        if (event.sameStaffingAs(lastPublished.get(event.wardId()))) {
            return;
        }
        try {
            publisher.publish(event);
            lastPublished.put(event.wardId(), event);
        } catch (RuntimeException e) {
            System.err.println("staffing-service: could not broadcast staffing event for "
                    + event.wardId() + " (" + e.getMessage() + ")");
        }
    }
}