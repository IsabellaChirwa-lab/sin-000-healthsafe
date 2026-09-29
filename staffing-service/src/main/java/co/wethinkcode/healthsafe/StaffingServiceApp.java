package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;

public class StaffingServiceApp {

    public static void main(String[] args) {
        String wardUrl = System.getenv().getOrDefault("WARD_SERVICE_URL", "http://localhost:7031");
        String alertUrl = System.getenv().getOrDefault("ALERT_LEVEL_URL", "http://localhost:7032");

        WardClient wards = new WardClient(wardUrl);
        AlertLevelClient alertLevel = new AlertLevelClient(alertUrl);
        StaffingService service = new StaffingService(wards::find, alertLevel::currentLevel);

        // Stage 3: broadcast staffing changes on the topic (best-effort, never fails a request).
        JmsStaffingEventPublisher jmsPublisher = new JmsStaffingEventPublisher();
        StaffingEventBroadcaster broadcaster = new StaffingEventBroadcaster(jmsPublisher);
        Runtime.getRuntime().addShutdownHook(new Thread(jmsPublisher::close));

        Javalin app = Javalin.create().start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/staffing/{wardId}", ctx -> {
            StaffingSchedule schedule = service.scheduleFor(ctx.pathParam("wardId"));
            broadcaster.onScheduleComputed(schedule);
            ctx.json(schedule);
        });

        app.exception(WardNotFoundException.class, (e, ctx) ->
                ctx.status(404).json(Map.of("error", e.getMessage())));

        // A dependency is down/slow/garbled: say so clearly instead of guessing a schedule.
        app.exception(DownstreamUnavailableException.class, (e, ctx) ->
                ctx.status(503).json(Map.of("error", "cannot compute schedule right now", "detail", e.getMessage())));
    }
}