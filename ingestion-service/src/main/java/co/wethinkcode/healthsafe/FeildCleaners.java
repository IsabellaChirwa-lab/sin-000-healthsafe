package co.wethinkcode.healthsafe;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Small, single-purpose cleaning helpers. Every method is total: bad input gives {@code null},
 * never an exception, so one malformed cell can't take down the whole parse.
 */
public final class FieldCleaners {

    private static final Set<String> PLACEHOLDERS =
            Set.of("", "n/a", "na", "tbd", "unknown", "-", "nan", "null");

    // Separator decides the meaning, which avoids the 03/04 ambiguity:
    //   2024-03-15 = ISO, 03/15/2024 = month first (US), 15-03-2024 = day first.
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            strict("uuuu-M-d"),
            strict("M/d/uuuu"),
            strict("d-M-uuuu"));

    private FieldCleaners() {
    }

    /** Trim, collapse internal whitespace, and turn every placeholder ("N/A", "TBD", "-", ...) into null. */
    public static String clean(String raw) {
        if (raw == null) return null;
        String s = raw.strip().replaceAll("\\s+", " ");
        return PLACEHOLDERS.contains(s.toLowerCase(Locale.ROOT)) ? null : s;
    }

    /** "east wing" / "EAST WING" -> "East Wing". */
    public static String titleCase(String s) {
        return Arrays.stream(s.toLowerCase(Locale.ROOT).split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    /** Tries each known format strictly; impossible dates (13/45/2024, 31 Feb) return null. */
    public static LocalDate parseDate(String raw) {
        String v = clean(raw);
        if (v == null) return null;
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(v, f);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    /** Y/yes/true/1 -> TRUE, N/no/false/0 -> FALSE, anything else -> null (unknown). */
    public static Boolean parseBoolean(String raw) {
        String v = clean(raw);
        if (v == null) return null;
        return switch (v.toLowerCase(Locale.ROOT)) {
            case "y", "yes", "true", "t", "1" -> Boolean.TRUE;
            case "n", "no", "false", "f", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }
}