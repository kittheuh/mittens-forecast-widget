package io.github.kittheuh.mforecast.api;

public record GeocodingResult(float latitude, float longitude, String countryCode,
                              String countryName, String cityName,
                              String admin1, String admin2, String admin3, String admin4) {
    @Override
    public String toString() {
        if (countryName == null && cityName == null && countryCode == null)
            return "%s, %s".formatted(latitude, longitude);

        StringBuilder builder = new StringBuilder();

        builder.append('[').append(countryCode).append("] ").append(countryName);
        if (admin1 != null && !admin1.isBlank()) builder.append(", ").append(admin1);
        if (admin2 != null && !admin2.isBlank()) builder.append(", ").append(admin2);
        if (admin3 != null && !admin3.isBlank()) builder.append(", ").append(admin3);
        if (admin4 != null && !admin4.isBlank()) builder.append(", ").append(admin4);
        builder.append(" - ").append(cityName);
        return builder.toString();
    }
}
