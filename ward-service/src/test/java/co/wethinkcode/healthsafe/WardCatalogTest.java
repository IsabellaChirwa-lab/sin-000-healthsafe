package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class WardCatalogTest {

    private static WardRecord ward(String id, String department) {
        return new WardRecord(id, "East Wing", department, 3, List.of());
    }

    private static final List<WardRecord> SAMPLE = List.of(
            ward("W-01", "Cardiology"),
            ward("W-05", "Paediatrics"),
            ward("W-06", "Cardiology"),
            ward("W-09", null));

    @Test
    void findIsCaseAndPaddingInsensitive() {
        WardCatalog catalog = new WardCatalog(() -> SAMPLE);
        assertEquals("W-05", catalog.find(" w-05 ").orElseThrow().wardId());
    }

    @Test
    void unknownWardIsEmpty() {
        WardCatalog catalog = new WardCatalog(() -> SAMPLE);
        assertTrue(catalog.find("W-99").isEmpty());
    }

    @Test
    void departmentsAreDistinctSortedAndSkipNulls() {
        WardCatalog catalog = new WardCatalog(() -> SAMPLE);
        assertEquals(List.of("Cardiology", "Paediatrics"), catalog.departments());
    }

    @Test
    void sourceFailureSurfacesAsIngestionUnavailable() {
        WardCatalog catalog = new WardCatalog(() -> {
            throw new IngestionUnavailableException("down");
        });
        assertThrows(IngestionUnavailableException.class, catalog::all);
    }

    @Test
    void emptyCacheRetriesOnNextRequestAfterIngestionRecovers() {
        AtomicBoolean up = new AtomicBoolean(false);
        WardCatalog catalog = new WardCatalog(() -> {
            if (!up.get()) throw new IngestionUnavailableException("down");
            return SAMPLE;
        });

        assertThrows(IngestionUnavailableException.class, catalog::all);
        up.set(true);
        assertEquals(4, catalog.all().size());
    }

    @Test
    void failedRefreshKeepsTheOldData() {
        AtomicBoolean up = new AtomicBoolean(true);
        WardCatalog catalog = new WardCatalog(() -> {
            if (!up.get()) throw new IngestionUnavailableException("down");
            return SAMPLE;
        });
        catalog.refresh();

        up.set(false);
        assertThrows(IngestionUnavailableException.class, catalog::refresh);
        assertEquals(4, catalog.all().size());
    }
}