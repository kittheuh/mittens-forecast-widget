package io.github.kittheuh.mforecast;

public enum ForecastStatus {
    GOOD,
    API_PROB_DOWN("Failed to contact forecast API server. Try again in a bit."),
    API_FAIL("Failed to retrieve forecast data for the set location."),
    NO_LOCATION_SET("No forecast location set!"),
    NO_RIGHT_NOW("The current forecast data is outdated"),
    NOT_ENOUGH_DATA("The current forecast data does not have enough to display!"),
    ;

    private final String message;

    ForecastStatus(String message) {
        this.message = message;
    }

    ForecastStatus() {
        this.message = null;
    }

    public String message() {
        return message;
    }
}
