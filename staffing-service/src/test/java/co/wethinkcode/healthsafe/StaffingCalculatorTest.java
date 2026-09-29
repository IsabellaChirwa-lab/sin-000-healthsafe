package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaffingCalculatorTest {

    @Test
    void calculate_knownDepartment_level0_correctBaseline() {
        WardInfo ward = new WardInfo("W-101", "North", "Cardiology");
        StaffingSchedule schedule = StaffingCalculator.calculate(ward, 0);

        assertEquals("W-101", schedule.wardId());
        assertEquals("North", schedule.wing());
        assertEquals("Cardiology", schedule.department());
        assertEquals(0, schedule.alertLevel());
        assertEquals(2, schedule.doctorsOnCall());
        assertTrue(schedule.notes().isEmpty());

        List<StaffingSchedule.Shift> shifts = schedule.shifts();
        assertEquals(3, shifts.size());
        assertEquals(2, shifts.get(0).doctors()); // Day
        assertEquals(2, shifts.get(1).doctors()); // Evening
        assertEquals(1, shifts.get(2).doctors()); // Night (half rounded up)
    }

    @Test
    void calculate_level4_doublesDoctors() {
        WardInfo ward = new WardInfo("W-102", "South", "ICU"); // ICU baseline = 3
        StaffingSchedule schedule = StaffingCalculator.calculate(ward, 4);

        assertEquals(6, schedule.doctorsOnCall());
    }

    @Test
    void calculate_level7Plus_fullNightStaffing() {
        WardInfo ward = new WardInfo("W-103", "East", "Paediatrics"); // Baseline = 2
        StaffingSchedule schedule = StaffingCalculator.calculate(ward, 7);

        List<StaffingSchedule.Shift> shifts = schedule.shifts();
        assertEquals(shifts.get(0).doctors(), shifts.get(2).doctors());
    }

    @Test
    void calculate_unknownDepartment_usesDefaultBaselineAndAddsNote() {
        WardInfo ward = new WardInfo("W-999", "West", "UnknownDept");
        StaffingSchedule schedule = StaffingCalculator.calculate(ward, 0);

        assertEquals(1, schedule.doctorsOnCall());
        assertFalse(schedule.notes().isEmpty());
        assertTrue(schedule.notes().get(0).contains("not recognised"));
    }
}