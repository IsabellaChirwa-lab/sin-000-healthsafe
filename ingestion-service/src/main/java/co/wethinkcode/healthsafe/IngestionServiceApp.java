package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class IngestionServiceApp {

    public static void main(String[] args) throws IOException {
        // Clean once at startup; the CSV is a static legacy export so there is nothing to refresh.
        CleaningResult result = loadAndClean();
        System.out.printf("Ingestion: %d cleaned records, %d rejected rows%n",
                result.records().size(), result.rejected().size());

        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/wards", ctx -> ctx.json(result.records()));
        app.get("/wards/rejected", ctx -> ctx.json(result.rejected()));
    }

    static CleaningResult loadAndClean() throws IOException {
        try (InputStream in = IngestionServiceApp.class.getResourceAsStream("/wards-outdated.csv")) {
            if (in == null) {
                throw new IllegalStateException("wards-outdated.csv not found on the classpath");
            }
            List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().toList();
            return new WardCsvCleaner().clean(lines);
        }
    }
}