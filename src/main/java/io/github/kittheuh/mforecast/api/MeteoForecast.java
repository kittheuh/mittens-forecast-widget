package io.github.kittheuh.mforecast.api;

import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

public interface MeteoForecast {
    record ExtraEntry(BufferedImage image, String text) {}

    // ========
    // OPTIONAL
    // ========
    default float[] minTemperatures() {
        return temperatures();
    }
    default float[] maxTemperatures() {
        return temperatures();
    }
    default boolean displayTwoTemps() {
        return false;
    }

    default List<ExtraEntry> extraEntries(int i) {
        return Collections.emptyList();
    }

    // ========
    // REQUIRED
    // ========
    ZoneId zoneId();
    String forecastPrefix();

    float[] temperatures();
    int[] rainChances();
    int[] weatherCodes();

    String timeFormat(int i);
    default int timeFontSize() { return 30; }
    boolean useAltColoring(int i);


    boolean isDay(int i);

    default int rainChance(int i) {
        return rainChances()[i];
    }

    default int weatherCode(int i) {
        return weatherCodes()[i];
    }

    int rightNow();
}
