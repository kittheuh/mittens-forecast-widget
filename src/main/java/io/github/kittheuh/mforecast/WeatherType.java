package io.github.kittheuh.mforecast;


import io.github.kittheuh.mforecast.util.EnumUtil;

import java.awt.image.BufferedImage;
import java.util.EnumSet;

public enum WeatherType {
    //============================
    // Generic weather
    //============================
    DAY_CLEAR(3, 4),
    DAY_PARTLY_CLOUDY(2, 3),
    DAY_MOSTLY_CLOUDY(2, 0),
    CLOUDY(0, 1),

    //============================
    // Rainy weather, year round
    //============================
    THUNDERSTORM(4, 0),
    RAIN(3, 0),
    SHOWERS(3, 1),
    DRIZZLE(0, 3),

    // Snowy like rainy weather events, also year round
    FREEZING_DRIZZLE(1, 1),
    ICE_COLD_DRIZZLE(0, 2),
    COLD_DRIZZLE(1, 3),

    // =================================================================================================================
    // =================================================================================================================
    // =================================================================================================================

    //============================
    // Spring weather - locked to spring
    //============================
    FLOOD(0, 4),

    //============================
    // Summer/Fall weather - locked to those respective seasons
    //============================
    HURRICANE(1, 2),

    //============================
    // Winter/Arctic weather - locked to the winter
    //============================
    BLIZZARD(0, 0),
    SNOWSTORM(3, 3),
    SNOW(3, 2),


    //============================
    // Summer weather events - locked to the summer
    //============================
    DROUGHT(2, 2),

    // =================================================================================================================
    // =================================================================================================================
    // =================================================================================================================

    //============================
    // Special weather events - locked to the summer
    //============================
    NIGHT_CLEAR(1, 4),
    NIGHT_PARTLY_CLOUDY(2, 4),
    NIGHT_MOSTLY_CLOUDY(2, 1),
    FOGGY(1, 0),
    ;

    public static final EnumSet<WeatherType> EXTREME_TYPES = EnumSet.of(WeatherType.FLOOD, WeatherType.DROUGHT, WeatherType.BLIZZARD, WeatherType.HURRICANE, WeatherType.THUNDERSTORM);
//    public static final BufferedImage ICON_SHEET = Util.tryLoadImage("./weather/weather_icons.png")

    private final int row, col;

    WeatherType(int r, int c) {
        this.row = r;
        this.col = c;
    }

    /**
     * if the weather type in this data holder has a nighttime variant, and it is currently night, the returned type will be the night time variant instead
     * @return the correct time variant of the current weather type
     */
    public WeatherType timeRelative(boolean isNight) {
        // replace valid day icons with equivalent night ones as needed
        if (name().startsWith("DAY") && isNight) {
            var tmp = EnumUtil.getEnum(WeatherType.class, name().replace("DAY", "NIGHT"));
            if (tmp != null) return tmp;
        }

        return this;
    }

    public BufferedImage icon() {
        return Constants.WEATHER_ICONS.frame(this.row, this.col);
    }
}
