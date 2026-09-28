package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/** Synchronous REST call to ingestion-service: GET /wards. Every failure becomes IngestionUnavailableException. */
public class IngestionClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    // Ignore unknown fields so ingestion can add fields without breaking us.
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final URI wardsUri;

    public IngestionClient(String baseUrl) {
        this.wardsUri = URI.create(baseUrl + "/wards");
    }

    public List<WardRecord> fetchWards() {
        HttpRequest request = HttpRequest.newBuilder(wardsUri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IngestionUnavailableException(
                        "ingestion-service returned HTTP " + response.statusCode());
            }
            return mapper.readValue(response.body(), new TypeReference<List<WardRecord>>() {
            });
        } catch (IOException e) { // covers connection refused, timeouts and malformed JSON
            throw new IngestionUnavailableException("could not read wards from ingestion-service: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IngestionUnavailableException("interrupted while calling ingestion-service", e);
        }
    }
}