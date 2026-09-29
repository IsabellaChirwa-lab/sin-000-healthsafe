package co.wethinkcode.healthsafe;

/** The parts of ward-service's response that staffing-service cares about. Other fields are ignored. */
public record WardInfo(String wardId, String wing, String department) {
}