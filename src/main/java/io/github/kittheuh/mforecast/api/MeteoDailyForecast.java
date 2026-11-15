package io.github.kittheuh.mforecast.api;

import com.google.gson.JsonObject;
import io.github.kittheuh.mforecast.Config;
import io.github.kittheuh.mforecast.Constants;
import io.github.kittheuh.mforecast.WeatherType;
import io.github.kittheuh.mforecast.util.Util;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class MeteoDailyForecast {
    private final ZonedDateTime[] sunrises, sunsets, times;
    private final float[] temperatureMin, temperatureMax;
    private final int[] rainChance, weatherCode;

    public MeteoDailyForecast(ZoneId zoneId, JsonObject object) {
        this.times = Util.dateMapper(zoneId, object.getAsJsonArray("time"));
        this.sunrises = Util.dateTimeMapper(zoneId, object.getAsJsonArray("sunrise"));
        this.sunsets = Util.dateTimeMapper(zoneId, object.getAsJsonArray("sunset"));
        this.temperatureMin = Util.floatMapper(object.getAsJsonArray("temperature_2m_min"));
        this.temperatureMax = Util.floatMapper(object.getAsJsonArray("temperature_2m_max"));

        this.rainChance = Util.intMapper(object.getAsJsonArray("precipitation_probability_mean"));
        this.weatherCode = Util.intMapper(object.getAsJsonArray("weather_code"));
    }

    /*public boolean shouldRefetch(LocalDateTime fetchTime) {
        return MeteoForecast.super.shouldRefetch(fetchTime) ||
                fetchTime.until(LocalDateTime.now(), ChronoUnit.HOURS) >= 2;
    }*/

    public int rightNow(ZoneId zoneId) {
        var now = ZonedDateTime.now(zoneId).truncatedTo(ChronoUnit.DAYS);
        for (int i = 0; i < times.length; i++) {
            if (times[i].isEqual(now)) return i;
        }
        return -1;
    }

    public List<MeteoForecast.ExtraEntry> extraEntries(int i) {
        int chance = rainChance[i];
        ZonedDateTime sunrise = sunrises[i], sunset = sunsets[i];

        return List.of(
                new MeteoForecast.ExtraEntry(
                        (chance == 0 ? WeatherType.DROUGHT : WeatherType.FLOOD).icon(),
                        chance + "%"
                ), new MeteoForecast.ExtraEntry(
                        Constants.WEATHER_ICONS.frame(4, 2),
                        Util.hourMinToString(sunrise.getHour(), sunrise.getMinute(), Config.use24HrFmt)
                ), new MeteoForecast.ExtraEntry(
                        Constants.WEATHER_ICONS.frame(4, 3),
                        Util.hourMinToString(sunset.getHour(), sunset.getMinute(), Config.use24HrFmt)
                )
        );
    }

    public String timeFormat(int i) {
        return times[i].getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
    }

    public boolean useAltColoring(int i) {
        DayOfWeek day = times[i].getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public float[] minTemperatures() {
        return temperatureMin;
    }

    public float[] maxTemperatures() {
        return temperatureMax;
    }

    public float[] temperatures() {
        return temperatureMin;
    }

    public int[] rainChances() {
        return rainChance;
    }

    public int[] weatherCodes() {
        return weatherCode;
    }

    public boolean isDay(ZoneId zoneId, int i) {
        var now = ZonedDateTime.now(zoneId).plusDays(i);
        return sunrises[i].isBefore(now) && sunsets[i].isAfter(now);
    }
}
