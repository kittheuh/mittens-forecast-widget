package io.github.kittheuh.mforecast.util;

import com.google.gson.JsonElement;
import io.github.kittheuh.mforecast.api.GeocodingResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GeocodingUtil {
    public static final GeocodingResult DEFAULT_RESULT = new GeocodingResult(
            34.05223f, -118.24368f, "US", "United States", "Los Angeles",
            "California", "Los Angeles", null, null
    );
    private static final String API_URL = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=10&language=en&format=json";

    public static List<GeocodingResult> geocoding(String query) {
        query = URLEncoder.encode(query, StandardCharsets.UTF_8);

        JsonElement element = Util.tryGetPage(API_URL.formatted(query));
        if (element == null || !element.isJsonObject()) return Collections.emptyList();

        var json = element.getAsJsonObject();
        if (!json.has("results") || !json.get("results").isJsonArray()) return Collections.emptyList();

        return json.getAsJsonArray("results").asList()
                .stream()
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .map(o -> {
                    if (!o.has("latitude") || !o.has("longitude")) return null;
                    float latitude = o.get("latitude").getAsFloat();
                    float longitude = o.get("longitude").getAsFloat();

                    String countryCode = o.has("country_code") ? o.get("country_code").getAsString() : null;
                    String countryName = o.has("country") ? o.get("country").getAsString() : null;
                    String cityName = o.has("name") ? o.get("name").getAsString() : null;
                    String admin1 = o.has("admin1") ? o.get("admin1").getAsString() : null;
                    String admin2 = o.has("admin2") ? o.get("admin2").getAsString() : null;
                    String admin3 = o.has("admin3") ? o.get("admin3").getAsString() : null;
                    String admin4 = o.has("admin4") ? o.get("admin4").getAsString() : null;
                    return new GeocodingResult(latitude, longitude, countryCode, countryName, cityName, admin1, admin2, admin3, admin4);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
