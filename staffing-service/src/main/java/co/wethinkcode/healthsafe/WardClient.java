package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** GET {ward-service}/wards/{id}: 200 = found, 404 = unknown ward, anything else = service problem. */
public class WardClient {

    private final String baseUrl;

    public WardClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Optional<WardInfo> find(String wardId) {
        String encoded = URLEncoder.encode(wardId.strip(), StandardCharsets.UTF_8).replace("+", "%20");
        HttpResponse<String> response = Http.get("ward-service", URI.create(baseUrl + "/wards/" + encoded));

        return switch (response.statusCode()) {
            case 200 -> Optional.of(parse(response.body()));
            case 404 -> Optional.empty();
            default -> throw new DownstreamUnavailableException(
                    "ward-service returned HTTP " + response.statusCode());
        };
    }

    private static WardInfo parse(String body) {
        try {
            return Http.MAPPER.readValue(body, WardInfo.class);
        } catch (JsonProcessingException e) {
            throw new DownstreamUnavailableException("ward-service returned unreadable data", e);
        }
    }
}