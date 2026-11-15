package io.github.kittheuh.mforecast.api;

public enum ForecastFrequency {
    DAILY("Daily"),
    HOURLY("Hourly"),
    FIFTEEN_MIN("15 Minutes"),
    ;

    private final String displayName;

    ForecastFrequency(String displayName) {
        this.displayName = displayName;
    }

    public Class<? extends MeteoForecast> forecastClass() {
        return null;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
