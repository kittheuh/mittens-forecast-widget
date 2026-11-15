package io.github.kittheuh.mforecast.api;

import io.github.kittheuh.mforecast.Config;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CumulativeMeteoForecast implements MeteoForecast {
    private final ZoneId zoneId;
    private final Meteo15MinForecast meteo15MinForecast;
    private final MeteoHourlyForecast meteoHourlyForecast;
    private final MeteoDailyForecast meteoDailyForecast;

    public CumulativeMeteoForecast(ZoneId zoneId, Meteo15MinForecast meteo15MinForecast, MeteoHourlyForecast meteoHourlyForecast, MeteoDailyForecast meteoDailyForecast) {
        this.meteo15MinForecast = meteo15MinForecast;
        this.meteoHourlyForecast = meteoHourlyForecast;
        this.meteoDailyForecast = meteoDailyForecast;
        this.zoneId = zoneId;
    }

    public boolean shouldRefetch() {
        if (ForecastManager.cacheLastUpdated() + TimeUnit.MINUTES.toMillis(90) < System.currentTimeMillis()) return true;

        // check each forecast and verify they have enough data
        int i = meteoDailyForecast.rightNow(zoneId);
        if (i < 0 || i+7 < meteoDailyForecast.weatherCodes().length) return true;

        i = meteoHourlyForecast.rightNow(zoneId);
        if (i < 0 || i+7 < meteoHourlyForecast.weatherCodes().length) return true;

        i = meteo15MinForecast.rightNow(zoneId);
        return i < 0 || i+7 > meteo15MinForecast.weatherCodes().length;
    }

    @Override
    public ZoneId zoneId() {
        return zoneId;
    }

    @Override
    public List<ExtraEntry> extraEntries(int i) {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.extraEntries(i);
            case HOURLY -> meteoHourlyForecast.extraEntries(i);
            case FIFTEEN_MIN -> meteo15MinForecast.extraEntries(i, meteoHourlyForecast.rainChances());
        };
    }

    @Override
    public int timeFontSize() {
        if (Config.frequency == ForecastFrequency.FIFTEEN_MIN) return 24;
        return MeteoForecast.super.timeFontSize();
    }

    @Override
    public String forecastPrefix() {
        return switch (Config.frequency) {
            case DAILY -> "7-Day";
            case HOURLY -> "7-Hour";
            case FIFTEEN_MIN -> "15-Minute";
        };
    }

    @Override
    public float[] temperatures() {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.temperatures();
            case HOURLY -> meteoHourlyForecast.temperatures();
            case FIFTEEN_MIN -> meteo15MinForecast.temperatures();
        };
    }

    @Override
    public boolean displayTwoTemps() {
        return Config.frequency == ForecastFrequency.DAILY;
    }

    @Override
    public float[] maxTemperatures() {
        return meteoDailyForecast.maxTemperatures();
    }

    @Override
    public float[] minTemperatures() {
        return meteoDailyForecast.minTemperatures();
    }

    @Override
    public int[] rainChances() {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.rainChances();
            case HOURLY -> meteoHourlyForecast.rainChances();
            // 15-min doesn't provide rain chances, so we need to infer from hourly
            case FIFTEEN_MIN ->
                    meteo15MinForecast.rainChances(meteoDailyForecast.rainChances());
        };
    }

    @Override
    public int[] weatherCodes() {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.weatherCodes();
            case HOURLY -> meteoHourlyForecast.weatherCodes();
            case FIFTEEN_MIN -> meteo15MinForecast.weatherCodes();
        };
    }

    @Override
    public String timeFormat(int i) {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.timeFormat(i);
            case HOURLY -> meteoHourlyForecast.timeFormat(i);
            case FIFTEEN_MIN -> meteo15MinForecast.timeFormat(i);
        };
    }

    @Override
    public boolean useAltColoring(int i) {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.useAltColoring(i);
            case HOURLY -> meteoHourlyForecast.useAltColoring(i);
            case FIFTEEN_MIN -> meteo15MinForecast.useAltColoring(i);
        };
    }

    @Override
    public boolean isDay(int i) {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.isDay(zoneId, i);
            case HOURLY -> meteoHourlyForecast.isDay(i);
            case FIFTEEN_MIN -> meteo15MinForecast.isDay(i);
        };
    }

    @Override
    public int rightNow() {
        return switch (Config.frequency) {
            case DAILY -> meteoDailyForecast.rightNow(zoneId);
            case HOURLY -> meteoHourlyForecast.rightNow(zoneId);
            case FIFTEEN_MIN -> meteo15MinForecast.rightNow(zoneId);
        };
    }
}
