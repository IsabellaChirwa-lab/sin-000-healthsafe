package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.StaffingSchedule.Shift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns (ward, emergency level) into an on-call schedule. Pure logic, no I/O, so it is easy to test.
 *
 * <p>Rules (assumptions, documented on purpose):
 * <ul>
 *   <li>Each department has a baseline number of on-call doctors per shift at level 0.</li>
 *   <li>Every alert level adds 25% on top of the baseline, rounded up: level 4 doubles it and
 *       level 8 (Code Blue) triples it.</li>
 *   <li>Day and Evening shifts get the full number. Night gets half (rounded up), except at level 7+
 *       where every shift is fully staffed.</li>
 *   <li>Unknown or missing departments get a baseline of 1 and a note, rather than an error.</li>
 * </ul>
 */
public final class StaffingCalculator {

    private static final Map<String, Integer> BASELINE = Map.of(
            "Cardiology", 2,
            "Paediatrics", 2,
            "Oncology", 2,
            "Radiology", 1,
            "ICU", 3,
            "Maternity", 2);

    private static final int DEFAULT_BASELINE = 1;
    private static final int FULL_NIGHT_STAFFING_FROM_LEVEL = 7;

    private StaffingCalculator() {
    }

    public static StaffingSchedule calculate(WardInfo ward, int level) {
        List<String> notes = new ArrayList<>();

        Integer known = ward.department() == null ? null : BASELINE.get(ward.department());
        int baseline = DEFAULT_BASELINE;
        if (known != null) {
            baseline = known;
        } else {
            notes.add("department " + (ward.department() == null ? "unknown" : "'" + ward.department() + "' not recognised")
                    + ", assumed baseline of " + DEFAULT_BASELINE + " doctor per shift");
        }

        // baseline * (1 + level * 0.25), rounded up, in integer maths to avoid floating point surprises
        int doctors = (baseline * (4 + level) + 3) / 4;
        int night = level >= FULL_NIGHT_STAFFING_FROM_LEVEL ? doctors : (doctors + 1) / 2;

        List<Shift> shifts = List.of(
                new Shift("Day", "07:00-15:00", doctors),
                new Shift("Evening", "15:00-23:00", doctors),
                new Shift("Night", "23:00-07:00", night));

        return new StaffingSchedule(ward.wardId(), ward.wing(), ward.department(), level, doctors, shifts, notes);
    }
}