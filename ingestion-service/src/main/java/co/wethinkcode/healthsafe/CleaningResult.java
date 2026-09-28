package co.wethinkcode.healthsafe;

import java.util.List;

/** Output of one cleaning run: usable records plus the rows we had to throw away (with reasons). */
public record CleaningResult(List<WardRecord> records, List<RejectedRow> rejected) {

    /** A row that could not be turned into a record at all. rowNumber counts data rows from 1. */
    public record RejectedRow(int rowNumber, String raw, String reason) {
    }
}