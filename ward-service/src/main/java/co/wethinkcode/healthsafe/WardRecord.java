package co.wethinkcode.healthsafe;

import java.util.List;

/**
 * ward-service's own view of a ward. Deliberately NOT shared code with ingestion-service:
 * each service is an independent project, and the JSON contract is the only coupling between them.
 */
public record WardRecord(
        String wardId,
        String wing,
        String department,
        Integer bedsAvailable,
        List<String> notes) {
}