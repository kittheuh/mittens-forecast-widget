package io.github.kittheuh.mforecast;

import java.awt.image.BufferedImage;

public enum WeatherOverlay {
    FORECAST_DAY(0, 0),
    FORECAST_NIGHT(0, 1),
    FOG(1, 0),
    SNOW(2, 1),
    RAIN(2, 0),
    ;

    private final BufferedImage overlay;

    WeatherOverlay(int row, int col) {
        this.overlay = Constants.WEATHER_BG_FG.frame(row, col);
    }

    public BufferedImage overlay() {
        return overlay;
    }
}
