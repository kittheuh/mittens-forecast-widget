package io.github.kittheuh.mforecast.api;

import com.google.gson.JsonObject;
import io.github.kittheuh.mforecast.Config;
import io.github.kittheuh.mforecast.WeatherType;
import io.github.kittheuh.mforecast.util.Util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MeteoHourlyForecast {
    private final ZonedDateTime[] times;
    private final float[] temperatures;
    private final int[] rainChance, weatherCode;
    private final boolean[] isDay;

    public MeteoHourlyForecast(ZoneId zoneId, JsonObject object) {
        this.times = Util.dateTimeMapper(zoneId, object.getAsJsonArray("time"));
        this.temperatures = Util.floatMapper(object.getAsJsonArray("temperature_2m"));
        this.rainChance = Util.intMapper(object.getAsJsonArray("precipitation_probability"));
        this.isDay = Util.boolMapper(object.getAsJsonArray("is_day"));
        this.weatherCode = Util.intMapper(object.getAsJsonArray("weather_code"));
    }

    /*public boolean shouldRefetch(LocalDateTime fetchTime) {
        return MeteoForecast.super.shouldRefetch(fetchTime) ||
                fetchTime.until(LocalDateTime.now(), ChronoUnit.MINUTES) >= 90;
    }*/

    public int rightNow(ZoneId zoneId) {
        var now = ZonedDateTime.now(zoneId).truncatedTo(ChronoUnit.HOURS);
        for (int i = 0; i < times.length; i++) {
            if (times[i].isEqual(now)) return i;
        }
        return -1;
    }

    public List<MeteoForecast.ExtraEntry> extraEntries(int i) {
        int chance = rainChance[i];

        return List.of(
                new MeteoForecast.ExtraEntry(
                        (chance == 0 ? WeatherType.DROUGHT : WeatherType.FLOOD).icon(),
                        chance + "%"
                )/*, new MeteoForecast.ExtraEntry(
                        WeatherType.FOGGY.icon(),
                        vis > 10_000 ? "%.2fkm".formatted(vis / 1000) : (int) vis + "m"
                )*/
        );
    }

    public float[] temperatures() {
        return temperatures;
    }

    public int[] rainChances() {
        return rainChance;
    }

    public int[] weatherCodes() {
        return weatherCode;
    }

    public String timeFormat(int i) {
        return Util.hourToString(times[i].getHour(), Config.use24HrFmt);
    }

    public boolean useAltColoring(int i) {
        return isDay[i];
    }

    public boolean[] isDay() {
        return isDay;
    }

    public boolean isDay(int i) {
        return isDay[i];
    }
}
