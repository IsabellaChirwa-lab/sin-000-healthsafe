package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class StaffingServiceTest {

    private static WardInfo ward(String department) {
        return new WardInfo("W-01", "East Wing", department);
    }

    // --- StaffingCalculator ---

    @Test
    void baselineAtLevelZero() {
        StaffingSchedule s = StaffingCalculator.calculate(ward("Cardiology"), 0);
        assertEquals(2, s.doctorsOnCall());
        assertEquals(1, s.shifts().get(2).doctors()); // night is halved
    }

    @Test
    void levelFourDoublesTheBaseline() {
        assertEquals(4, StaffingCalculator.calculate(ward("Cardiology"), 4).doctorsOnCall());
    }

    @Test
    void oddLevelsRoundUp() {
        assertEquals(3, StaffingCalculator.calculate(ward("Cardiology"), 1).doctorsOnCall());
    }

    @Test
    void codeBlueTriplesTheBaselineAndFullyStaffsEveryShift() {
        StaffingSchedule s = StaffingCalculator.calculate(ward("ICU"), 8);
        assertEquals(9, s.doctorsOnCall());
        assertTrue(s.shifts().stream().allMatch(shift -> shift.doctors() == 9));
    }

    @Test
    void nightIsHalvedBelowLevelSeven() {
        StaffingSchedule s = StaffingCalculator.calculate(ward("ICU"), 3);
        assertEquals(6, s.doctorsOnCall());
        assertEquals(3, s.shifts().get(2).doctors());
    }

    @Test
    void unknownOrMissingDepartmentGetsBaselineOneAndANote() {
        StaffingSchedule unknown = StaffingCalculator.calculate(ward("Neurology"), 0);
        StaffingSchedule missing = StaffingCalculator.calculate(ward(null), 0);
        assertEquals(1, unknown.doctorsOnCall());
        assertEquals(1, missing.doctorsOnCall());
        assertFalse(unknown.notes().isEmpty());
        assertFalse(missing.notes().isEmpty());
    }

    // --- StaffingService: not assuming the happy path ---

    @Test
    void unknownWardIsNotFoundAndAlertLevelIsNeverRead() {
        AtomicBoolean levelRead = new AtomicBoolean(false);
        StaffingService service = new StaffingService(id -> Optional.empty(), () -> {
            levelRead.set(true);
            return 0;
        });

        assertThrows(WardNotFoundException.class, () -> service.scheduleFor("W-99"));
        assertFalse(levelRead.get());
    }

    @Test
    void wardServiceDownSurfacesAsDownstreamUnavailable() {
        StaffingService service = new StaffingService(id -> {
            throw new DownstreamUnavailableException("ward-service unreachable");
        }, () -> 0);

        assertThrows(DownstreamUnavailableException.class, () -> service.scheduleFor("W-01"));
    }

    @Test
    void alertLevelServiceDownSurfacesAsDownstreamUnavailable() {
        StaffingService service = new StaffingService(id -> Optional.of(ward("Cardiology")), () -> {
            throw new DownstreamUnavailableException("alert-level-service unreachable");
        });

        assertThrows(DownstreamUnavailableException.class, () -> service.scheduleFor("W-01"));
    }

    @Test
    void happyPathUsesTheCurrentAlertLevel() {
        StaffingService service = new StaffingService(id -> Optional.of(ward("Cardiology")), () -> 4);
        StaffingSchedule s = service.scheduleFor("W-01");
        assertEquals(4, s.alertLevel());
        assertEquals(4, s.doctorsOnCall());
    }
}