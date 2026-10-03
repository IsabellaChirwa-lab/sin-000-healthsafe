package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class WardServiceApp {

    public static void main(String[] args) {
        String ingestionUrl = System.getenv().getOrDefault("INGESTION_URL", "http://localhost:7030");
        WardCatalog catalog = new WardCatalog(new IngestionClient(ingestionUrl)::fetchWards);

        // Start even if ingestion is down: the service stays independently runnable and retries on demand.
        try {
            catalog.refresh();
            System.out.println("ward-service: loaded " + catalog.size() + " wards from " + ingestionUrl);
        } catch (IngestionUnavailableException e) {
            System.err.println("ward-service: starting without data (" + e.getMessage() + "), will retry on first request");
        }

        // Publish equipment failure alerts based on the loaded ward data.
        publishEquipmentFailures(catalog.all());

        StaffingEventSubscriber subscriber = new StaffingEventSubscriber();
        subscriber.start();
        // Ensure subscriber is closed on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));

        Javalin app = Javalin.create().start(7031);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/wards", ctx -> {
            String department = ctx.queryParam("department");
            String wing = ctx.queryParam("wing");
            List<WardRecord> wards = catalog.all().stream()
                    .filter(w -> department == null || department.equalsIgnoreCase(w.department()))
                    .filter(w -> wing == null || wing.equalsIgnoreCase(w.wing()))
                    .toList();
            ctx.json(wards);
        });

        app.get("/wards/{id}", ctx -> {
            String id = ctx.pathParam("id");
            catalog.find(id).ifPresentOrElse(
                    ctx::json,
                    () -> ctx.status(404).json(Map.of("error", "ward not found: " + id)));
        });

        app.get("/departments", ctx -> ctx.json(catalog.departments()));

        app.post("/wards/refresh", ctx -> {
            catalog.refresh();
            publishEquipmentFailures(catalog.all());
            ctx.json(Map.of("wardsLoaded", catalog.size()));
        });

        // Ingestion down or returning garbage -> 503, not a crash and not a fake empty list.
        app.exception(IngestionUnavailableException.class, (e, ctx) ->
                ctx.status(503).json(Map.of("error", "ward data unavailable", "detail", e.getMessage())));
    }

    private static void publishEquipmentFailures(List<WardRecord> wards) {
        EquipmentFailurePublisher publisher = new EquipmentFailurePublisher();
        try {
            for (WardRecord ward : wards) {
                if (isEquipmentFailure(ward)) {
                    String issue = buildIssueDescription(ward);
                    publisher.publish(ward.wardId(), issue);
                    System.out.println("ward-service: published equipment failure alert for ward " + ward.wardId() + ": " + issue);
                }
            }
        } finally {
            publisher.close();
        }
    }

    private static boolean isEquipmentFailure(WardRecord ward) {
        // Equipment failure indicated by missing/invalid bedsAvailable with explanatory notes.
        if (ward.bedsAvailable() != null) {
            return false;
        }
        return ward.notes().stream()
                .anyMatch(note -> note.contains("non-numeric") ||
                        note.contains("missing") ||
                        note.contains("negative") ||
                        note.contains("unrealistic"));
    }

    private static String buildIssueDescription(WardRecord ward) {
        // Combine all notes into a single issue string.
        return String.join("; ", ward.notes());
    }
}