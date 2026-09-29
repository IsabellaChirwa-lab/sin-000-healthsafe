package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaffingEventBroadcasterTest {

    private static class FakePublisher implements StaffingEventPublisher {
        final List<StaffingEvent> published = new ArrayList<>();
        boolean failing = false;

        @Override
        public void publish(StaffingEvent event) {
            if (failing) throw new PublishException("broker down", null);
            published.add(event);
        }
    }

    private static StaffingSchedule schedule(String wardId, int level) {
        WardInfo ward = new WardInfo(wardId, "East Wing", "Cardiology");
        return StaffingCalculator.calculate(ward, level);
    }

    @Test
    void firstScheduleForAWardIsBroadcast() {
        FakePublisher fake = new FakePublisher();
        new StaffingEventBroadcaster(fake).onScheduleComputed(schedule("W-01", 0));
        assertEquals(1, fake.published.size());
        assertEquals("W-01", fake.published.get(0).wardId());
    }

    @Test
    void unchangedStaffingIsNotBroadcastAgain() {
        FakePublisher fake = new FakePublisher();
        StaffingEventBroadcaster broadcaster = new StaffingEventBroadcaster(fake);
        broadcaster.onScheduleComputed(schedule("W-01", 3));
        broadcaster.onScheduleComputed(schedule("W-01", 3));
        assertEquals(1, fake.published.size());
    }

    @Test
    void changedAlertLevelIsBroadcast() {
        FakePublisher fake = new FakePublisher();
        StaffingEventBroadcaster broadcaster = new StaffingEventBroadcaster(fake);
        broadcaster.onScheduleComputed(schedule("W-01", 0));
        broadcaster.onScheduleComputed(schedule("W-01", 8));
        assertEquals(2, fake.published.size());
        assertEquals(8, fake.published.get(1).alertLevel());
    }

    @Test
    void eachWardIsTrackedSeparately() {
        FakePublisher fake = new FakePublisher();
        StaffingEventBroadcaster broadcaster = new StaffingEventBroadcaster(fake);
        broadcaster.onScheduleComputed(schedule("W-01", 2));
        broadcaster.onScheduleComputed(schedule("W-02", 2));
        assertEquals(2, fake.published.size());
    }

    @Test
    void brokerFailureNeverBreaksTheCallerAndIsRetriedNextTime() {
        FakePublisher fake = new FakePublisher();
        StaffingEventBroadcaster broadcaster = new StaffingEventBroadcaster(fake);

        fake.failing = true;
        assertDoesNotThrow(() -> broadcaster.onScheduleComputed(schedule("W-01", 4)));
        assertTrue(fake.published.isEmpty());

        fake.failing = false; // broker is back: the same schedule must still go out
        broadcaster.onScheduleComputed(schedule("W-01", 4));
        assertEquals(1, fake.published.size());
    }
}