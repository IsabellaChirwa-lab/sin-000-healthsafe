package co.wethinkcode.healthsafe;

import java.time.Instant;

/** What staffing-service broadcasts on staffing-events-topic when a ward's staffing changes. */
public record StaffingEvent(String wardId, int alertLevel, int doctorsOnCall, String occurredAt) {

    public static StaffingEvent from(StaffingSchedule schedule) {
        return new StaffingEvent(schedule.wardId(), schedule.alertLevel(), schedule.doctorsOnCall(),
                Instant.now().toString());
    }

    /** Same staffing situation? (Timestamps are ignored on purpose.) */
    boolean sameStaffingAs(StaffingEvent other) {
        return other != null && alertLevel == other.alertLevel && doctorsOnCall == other.doctorsOnCall;
    }
}
