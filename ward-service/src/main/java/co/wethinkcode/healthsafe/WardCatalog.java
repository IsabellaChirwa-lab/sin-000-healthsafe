package co.wethinkcode.healthsafe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * In-memory cache of the wards ingestion-service gave us.
 *
 * <p>Takes a {@code Supplier} rather than the HTTP client directly, so tests can plug in a fake source.
 * If the cache is empty (ingestion was down at startup) the next request tries to load it again.
 */
public class WardCatalog {

    private final Supplier<List<WardRecord>> source;
    private volatile Map<String, WardRecord> byId = Map.of();

    public WardCatalog(Supplier<List<WardRecord>> source) {
        this.source = source;
    }

    /** Re-pull from the source. Throws IngestionUnavailableException on failure and keeps the old cache. */
    public synchronized void refresh() {
        Map<String, WardRecord> fresh = new LinkedHashMap<>();
        for (WardRecord ward : source.get()) {
            fresh.put(ward.wardId(), ward);
        }
        byId = Collections.unmodifiableMap(fresh);
    }

    public List<WardRecord> all() {
        ensureLoaded();
        return List.copyOf(byId.values());
    }

    /** Case-insensitive, padding-tolerant: "w-05 " finds W-05. */
    public Optional<WardRecord> find(String wardId) {
        ensureLoaded();
        return Optional.ofNullable(byId.get(wardId.strip().toUpperCase(Locale.ROOT)));
    }

    /** Distinct, sorted department names (wards with an unknown department are skipped). */
    public List<String> departments() {
        return all().stream()
                .map(WardRecord::department)
                .filter(d -> d != null)
                .distinct()
                .sorted()
                .toList();
    }

    public int size() {
        return byId.size();
    }

    private void ensureLoaded() {
        if (byId.isEmpty()) {
            refresh();
        }
    }
}