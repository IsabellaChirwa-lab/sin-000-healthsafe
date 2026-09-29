package co.wethinkcode.healthsafe;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Orchestrates the two synchronous calls staffing needs: validate the ward (ward-service),
 * read the Emergency Status (alert-level-service), then compute the schedule.
 *
 * <p>Dependencies are injected as functions so tests can simulate "ward unknown" or "service down".
 */
public class StaffingService {

    private final Function<String, Optional<WardInfo>> wardLookup;
    private final IntSupplier alertLevel;

    public StaffingService(Function<String, Optional<WardInfo>> wardLookup, IntSupplier alertLevel) {
        this.wardLookup = wardLookup;
        this.alertLevel = alertLevel;
    }

    /**
     * @throws WardNotFoundException           unknown ward (404)
     * @throws DownstreamUnavailableException  ward-service or alert-level-service failed (503)
     */
    public StaffingSchedule scheduleFor(String wardId) {
        // Fail fast: don't bother reading the alert level for a ward that doesn't exist.
        WardInfo ward = wardLookup.apply(wardId).orElseThrow(() -> new WardNotFoundException(wardId));
        int level = alertLevel.getAsInt();
        return StaffingCalculator.calculate(ward, level);
    }
}