package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WardCsvCleanerTest {

    private static final String HEADER = "ward_id, Wing ,department,beds_available";

    private static CleaningResult run(String... rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.addAll(List.of(rows));
        return new WardCsvCleaner().clean(lines);
    }

    private static WardRecord only(CleaningResult r) {
        assertEquals(1, r.records().size());
        return r.records().get(0);
    }

    @Test
    void normalisesCasingAndPadding() {
        WardRecord w = only(run(" w-02 , west  WING ,cardiology,3"));
        assertEquals("W-02", w.wardId());
        assertEquals("West Wing", w.wing());
        assertEquals("Cardiology", w.department());
        assertEquals(3, w.bedsAvailable());
    }

    @Test
    void mapsRegionalSpellingAndAcronyms() {
        assertEquals("Paediatrics", only(run("W-11,East Wing,Pediatrics,3")).department());
        assertEquals("ICU", only(run("W-09,North Wing,icu,3")).department());
    }

    @Test
    void nonNumericBedsBecomeNullWithNote() {
        WardRecord w = only(run("w-05,east wing ,PAEDIATRICS,five"));
        assertNull(w.bedsAvailable());
        assertTrue(w.notes().get(0).contains("non-numeric"));
    }

    @Test
    void negativeAndUnrealisticBedsAreFlagged() {
        assertNull(only(run("W-04,North Wing,Oncology,-1")).bedsAvailable());
        assertNull(only(run("W-13,North Wing,Oncology,2023")).bedsAvailable());
    }

    @Test
    void zeroBedsIsAValidCount() {
        assertEquals(0, only(run("W-03,East Wing,Cardiology,0")).bedsAvailable());
    }

    @Test
    void placeholdersAllBecomeNull() {
        for (String p : List.of("N/A", "TBD", "unknown", "-", "NaN", "")) {
            assertNull(only(run("W-01,East Wing,Cardiology," + p)).bedsAvailable(), p);
        }
    }

    @Test
    void duplicateIsMergedAndBestValueWins() {
        CleaningResult r = run("W-05,East Wing,Paediatrics,5", "w-05,east wing ,PAEDIATRICS,five");
        WardRecord w = only(r);
        assertEquals(5, w.bedsAvailable());
        assertTrue(w.notes().get(0).startsWith("merged 2 duplicate rows"));
    }

    @Test
    void duplicateGapsAreFilledFromTheOtherRow() {
        WardRecord w = only(run("W-20,,Oncology,4", "w-20,North Wing,Oncology,N/A"));
        assertEquals("North Wing", w.wing());
        assertEquals(4, w.bedsAvailable());
    }

    @Test
    void duplicateConflictKeepsWinnerAndRecordsIt() {
        WardRecord w = only(run("W-21,East Wing,Oncology,4", "W-21,East Wing,Oncology,7"));
        assertEquals(4, w.bedsAvailable());
        assertTrue(w.notes().stream().anyMatch(n -> n.contains("conflict on bedsAvailable")));
    }

    @Test
    void badRowsAreRejectedWithoutStoppingTheRun() {
        CleaningResult r = run("garbage", ",East Wing,ICU,3", "X-99,East Wing,ICU,3", "W-01,East Wing,ICU,3");
        assertEquals(1, r.records().size());
        assertEquals(3, r.rejected().size());
    }

    @Test
    void realCsvCleansToSeventeenUniqueWards() throws IOException {
        try (var in = getClass().getResourceAsStream("/wards-outdated.csv")) {
            assertNotNull(in);
            List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
            CleaningResult r = new WardCsvCleaner().clean(lines);
            assertEquals(17, r.records().size());
            assertTrue(r.rejected().isEmpty());
        }
    }

    // --- FieldCleaners: the sample CSV has no date/boolean columns, but the cleaners are ready for them ---

    @Test
    void parsesAllDateFormatsAndRejectsInvalid() {
        assertEquals(LocalDate.of(2024, 3, 5), FieldCleaners.parseDate("2024-03-05"));
        assertEquals(LocalDate.of(2024, 3, 5), FieldCleaners.parseDate("3/5/2024"));
        assertEquals(LocalDate.of(2024, 3, 5), FieldCleaners.parseDate("05-03-2024"));
        assertNull(FieldCleaners.parseDate("13/45/2024"));
        assertNull(FieldCleaners.parseDate("31-02-2024"));
        assertNull(FieldCleaners.parseDate("N/A"));
    }

    @Test
    void normalisesBooleanVariants() {
        for (String t : List.of("Y", "yes", "1", "true", "TRUE")) assertEquals(Boolean.TRUE, FieldCleaners.parseBoolean(t), t);
        for (String f : List.of("N", "no", "0", "FALSE")) assertEquals(Boolean.FALSE, FieldCleaners.parseBoolean(f), f);
        assertNull(FieldCleaners.parseBoolean("maybe"));
        assertNull(FieldCleaners.parseBoolean("N/A"));
    }
}