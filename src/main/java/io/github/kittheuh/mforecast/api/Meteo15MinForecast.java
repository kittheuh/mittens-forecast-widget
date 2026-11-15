package io.github.kittheuh.mforecast.api;

import com.google.gson.JsonObject;
import io.github.kittheuh.mforecast.Config;
import io.github.kittheuh.mforecast.Constants;
import io.github.kittheuh.mforecast.WeatherType;
import io.github.kittheuh.mforecast.util.Util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Meteo15MinForecast {
    private final ZonedDateTime[] times;
    private final float[] temperatures;
    private final int[] weatherCodes, windDirection, windSpeed;
    private final boolean[] isDay;
    private int[] rainChance;

    public Meteo15MinForecast(ZoneId zoneId, JsonObject object) {
        this.times = Util.dateTimeMapper(zoneId, object.getAsJsonArray("time"));
        this.temperatures = Util.floatMapper(object.getAsJsonArray("temperature_2m"));
        this.isDay = Util.boolMapper(object.getAsJsonArray("is_day"));
        this.weatherCodes = Util.intMapper(object.getAsJsonArray("weather_code"));
        this.windSpeed = Util.intMapper(object.getAsJsonArray("wind_speed_10m"));
        this.windDirection = Util.intMapper(object.getAsJsonArray("wind_direction_10m"));
    }

    /*public boolean shouldRefetch(LocalDateTime fetchTime) {
        return MeteoForecast.super.shouldRefetch(fetchTime) ||
                fetchTime.until(LocalDateTime.now(), ChronoUnit.MINUTES) >= 30;
    }*/

    public int rightNow(ZoneId zoneId) {
        int closest = -1;

        var now = ZonedDateTime.now(zoneId).truncatedTo(ChronoUnit.MINUTES);

        for (int i = 0; i < times.length; i++) {
            if (times[i].isEqual(now)) return i;

            if (times[i].isBefore(now)) {
                closest = i;
            } else if (times[i].isAfter(now)) {
                if (closest == -1) return i; // can't compare, assume latest

                var beforeDist = times[closest].until(now, ChronoUnit.MINUTES);
                var afterDist = now.until(times[i], ChronoUnit.MINUTES);

                // favor the one that is closer to our current time
                if (afterDist > beforeDist) return closest;
                return i;
            }

        }
        return -1;
    }

    public List<MeteoForecast.ExtraEntry> extraEntries(int i, int[] hourlyChances) {
        int chance = rainChances(hourlyChances)[i];

        BufferedImage directionArrow = new BufferedImage(26, 26, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = directionArrow.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.rotate(windDirection[i], 13, 13);
        g.drawRenderedImage(Constants.WEATHER_ICONS.frame(4, 4), AffineTransform.getScaleInstance(0.1, 0.1));
        g.dispose();

        return List.of(
                new MeteoForecast.ExtraEntry(
                        (chance == 0 ? WeatherType.DROUGHT : WeatherType.FLOOD).icon(),
                        chance + "%"
                ), new MeteoForecast.ExtraEntry(
                        directionArrow,
                        windSpeed[i] + "m/s"
                )
        );
    }

    public boolean useAltColoring(int i) {
        return times[i].getMinute() == 0;
    }

    public String timeFormat(int i) {
        ZonedDateTime time = times[i];
        return Util.hourMinToString(time.getHour(), time.getMinute(), Config.use24HrFmt);
    }

    public int timeFontSize() {
        return 24;
    }


    public float[] temperatures() {
        return temperatures;
    }

    public int[] rainChances(int[] hourlyChances) {
        if (rainChance != null) return rainChance;

        int[] minutelyChances = new int[weatherCodes.length];

        int hourlyIndex = 0;
        int minutelyIndex = 0;

        for (ZonedDateTime time : times) {
            if (minutelyIndex != 0 && time.getMinute() == 0) hourlyIndex++;
            minutelyChances[minutelyIndex++] = hourlyChances[hourlyIndex];
        }
        return rainChance = minutelyChances;
    }

    public int[] weatherCodes() {
        return weatherCodes;
    }

    public boolean isDay(int i) {
        return isDay[i];
    }

    public int[] windDirection() {
        return windDirection;
    }

    public int[] windSpeed() {
        return windSpeed;
    }
}
