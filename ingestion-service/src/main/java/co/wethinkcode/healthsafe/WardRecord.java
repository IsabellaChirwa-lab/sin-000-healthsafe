package co.wethinkcode.healthsafe;

import java.util.List;

/**
 * One cleaned ward. Any field we could not trust is {@code null} and explained in {@code notes},
 * so downstream services never have to guess whether "0" means "no beds" or "bad data".
 */
public record WardRecord(
        String wardId,
        String wing,
        String department,
        Integer bedsAvailable,
        List<String> notes) {

    int missingFields() {
        int missing = 0;
        if (wing == null) missing++;
        if (department == null) missing++;
        if (bedsAvailable == null) missing++;
        return missing;
    }
}