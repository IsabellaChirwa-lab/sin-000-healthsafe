package co.wethinkcode.healthsafe;

import java.util.List;

/** The on-call schedule for one ward at the current Emergency Status. */
public record StaffingSchedule(
        String wardId,
        String wing,
        String department,
        int alertLevel,
        int doctorsOnCall,
        List<Shift> shifts,
        List<String> notes) {

    public record Shift(String name, String hours, int doctors) {
    }
}