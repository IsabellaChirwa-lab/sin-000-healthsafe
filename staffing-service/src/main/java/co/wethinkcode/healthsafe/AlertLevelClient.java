package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpResponse;

/** GET {alert-level-service}/alert-level -> {"level": 0-8}. Anything else is treated as a failure. */
public class AlertLevelClient {

    private final URI uri;

    public AlertLevelClient(String baseUrl) {
        this.uri = URI.create(baseUrl + "/alert-level");
    }

    public int currentLevel() {
        HttpResponse<String> response = Http.get("alert-level-service", uri);
        if (response.statusCode() != 200) {
            throw new DownstreamUnavailableException("alert-level-service returned HTTP " + response.statusCode());
        }
        try {
            JsonNode level = Http.MAPPER.readTree(response.body()).get("level");
            if (level == null || !level.isInt() || level.asInt() < 0 || level.asInt() > 8) {
                throw new DownstreamUnavailableException("alert-level-service returned an invalid level");
            }
            return level.asInt();
        } catch (JsonProcessingException e) {
            throw new DownstreamUnavailableException("alert-level-service returned unreadable data", e);
        }
    }
}