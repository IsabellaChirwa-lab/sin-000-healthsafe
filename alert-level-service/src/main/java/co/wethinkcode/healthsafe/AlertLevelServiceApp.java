package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;

public class AlertLevelServiceApp {

    /** Request body for PUT /alert-level, e.g. {"level": 5}. */
    public record LevelRequest(Integer level) {
    }

    public static void main(String[] args) {
        AlertLevelStore store = new AlertLevelStore();

        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/alert-level", ctx -> ctx.json(Map.of("level", store.get())));

        app.put("/alert-level", ctx -> {
            LevelRequest request;
            try {
                request = ctx.bodyAsClass(LevelRequest.class);
            } catch (Exception e) { // empty body, invalid JSON, or a non-numeric level like "high"
                throw new IllegalArgumentException("body must be JSON like {\"level\": 3}");
            }
            store.set(request == null ? null : request.level());
            ctx.json(Map.of("level", store.get()));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(400).json(Map.of("error", e.getMessage())));
    }
}