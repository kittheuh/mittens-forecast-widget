package io.github.kittheuh.mforecast.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kittheuh.mforecast.*;
import io.github.kittheuh.mforecast.util.ImageGraphic;
import io.github.kittheuh.mforecast.util.ScaledImageGraphic;
import io.github.kittheuh.mforecast.util.Util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public class ForecastManager {
    /*
    daily - temp min & max, weather code, rain chance, sunrise, sunset
    hourly - rain chance, is day, temp, weather code
    minutely_15 - temp, weather code, is day, wind dir, wind speed
    */
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?" +
            "latitude=%s&longitude=%s" +
            "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_mean,sunrise,sunset" +
            "&hourly=precipitation_probability,is_day,temperature_2m,weather_code" +
            "&minutely_15=temperature_2m,weather_code,is_day,wind_direction_10m,wind_speed_10m" +
            "&timezone=auto" +
            "&forecast_hours=12" +
            "&forecast_minutely_15=24"
    ;

    private static CumulativeMeteoForecast currentForecast;
    private static ForecastStatus currentStatus = ForecastStatus.NO_LOCATION_SET;
    private static long cacheLastUpdated;

    @ImageGraphic.Nullable
    public static CumulativeMeteoForecast currentForecast() {
        return currentForecast;
    }

    @ImageGraphic.NotNull
    public static ForecastStatus currentStatus() {
        return currentStatus;
    }

    public static long cacheLastUpdated() {
        return cacheLastUpdated;
    }

    public static void checkRefresh() {
        // only calls private doRefresh when its due to refetch
        if (currentForecast == null || currentForecast.shouldRefetch()) currentStatus = doRefresh();
    }

    private static ForecastStatus doRefresh() {
        // query api and update cache
        JsonElement element = Util.tryGetPage(API_URL.formatted(Config.locationLatitude, Config.locationLongitude));
        if (element == null || !element.isJsonObject()) return ForecastStatus.API_PROB_DOWN;

        // ensure it has the 3 types
        JsonObject object = element.getAsJsonObject();
        if (!object.has("timezone_abbreviation") || !object.has("minutely_15") ||
                !object.has("hourly") || !object.has("daily"))
            return ForecastStatus.API_FAIL;


        ZoneId zoneId = ZoneId.of(object.get("timezone_abbreviation").getAsString());
        var minForecast = new Meteo15MinForecast(zoneId, object.getAsJsonObject("minutely_15"));
        var hourForecast = new MeteoHourlyForecast(zoneId, object.getAsJsonObject("hourly"));
        var dailyForecast = new MeteoDailyForecast(zoneId, object.getAsJsonObject("daily"));

        currentForecast = new CumulativeMeteoForecast(zoneId, minForecast, hourForecast, dailyForecast);
        cacheLastUpdated = System.currentTimeMillis();
        return ForecastStatus.GOOD;
    }

    public static ImageGraphic renderForecast(double renderScale) {
        return renderForecast(renderScale, currentForecast);
    }

    public static ImageGraphic renderNoForecast(double renderScale) {
        return renderNoForecast(renderScale, currentStatus);
    }

    public static ImageGraphic renderForecast(double renderScale, MeteoForecast entry) {
        if (entry == null) return renderNoForecast(renderScale);
        final int rnIndex = entry.rightNow();
        assert rnIndex >= 0;

        BufferedImage bgImage = (entry.isDay(rnIndex) ? WeatherOverlay.FORECAST_DAY : WeatherOverlay.FORECAST_NIGHT).overlay();

        ImageGraphic g = new ScaledImageGraphic(bgImage, renderScale);

        //Util.toWeatherType(entry.snowfall(rnIndex), entry.rainChance(rnIndex), entry.cloudCover(rnIndex))
        Util.applyWeatherOverlays(g, entry, rnIndex);
        //entry.cloudCover(rnIndex),
//                Util.treatFoggy(entry.visibility(rnIndex))
//        );

        int xpos = 15, ypos = g.height() / 2 - (150);

        g.color(Constants.FORECAST_BACKGROUND)
                .rect(xpos, ypos - 51, 636, 50, true) // col width (90) * 7, plus 6 for 1px off

                .color(Color.white)
                .font(Constants.FONT, 30)
                .alignment(ImageGraphic.Align.BOTTOM)
                .str(entry.forecastPrefix() + " Forecast", xpos + 15, ypos - 52)

                .image(Constants.WEATHER_ICONS.frame(4, 1), 530, (ypos - 51) + 25 - 20)
        ;

        for (int i = 0; i < 7; i++) {
            WeatherType type = Util.weatherCodeToType(entry.weatherCode(rnIndex + i));

            g.color(entry.useAltColoring(rnIndex + i) ? Constants.FORECAST_BACKGROUND_BB : Constants.FORECAST_BACKGROUND)
                    .composite(AlphaComposite.SrcOver.derive(.85f))
                    .rect(xpos, ypos, 90, 350, true)
                    .composite(AlphaComposite.SrcOver)
            ;

            String hourStr = entry.timeFormat(rnIndex + i);

            int nX = xpos + 45, nY = ypos;
            g.alignment(ImageGraphic.Align.BOTTOM)
                    .color(Color.white)
                    .justification(ImageGraphic.Justify.CENTER)
                    .font(Constants.SIGNAGE_FONT, entry.timeFontSize())
                    .str(hourStr, nX, nY + (g.metrics().getHeight() / 2) - 10)
                    .font(Constants.SIGNAGE_FONT, 30);


            float topTemp = (entry.displayTwoTemps() ? entry.maxTemperatures() : entry.temperatures())[rnIndex + i];

            String tempStr;
            if (Config.useFahrenheit) tempStr = Math.round(Util.celsiusesToFahrenheit(topTemp)) + "°F";
            else tempStr = Math.round(topTemp) + "°C";

            g.str(tempStr, nX, nY += 120 + 50);
            if (entry.displayTwoTemps()) {
                float bottomTemp = entry.minTemperatures()[rnIndex + i];

                if (Config.useFahrenheit) tempStr = Math.round(Util.celsiusesToFahrenheit(bottomTemp)) + "°F";
                else tempStr = Math.round(bottomTemp) + "°C";

                g.color(g.color().darker()).str(tempStr, nX, nY += g.metrics().getHeight());
            }

            List<MeteoForecast.ExtraEntry> entries = entry.extraEntries(rnIndex + i);
            nY += 15;
            final int iconSize = 26;
            for (MeteoForecast.ExtraEntry extraEntry : entries) {

                g.color(Color.white)
                        .font(Constants.FONT, 22)
                        .imageCentered(
                                extraEntry.image(),
                                xpos + 5, nY += g.metrics().getHeight(), iconSize, iconSize
                        )
                        .justification(ImageGraphic.Justify.LEFT)
                        .alignment(ImageGraphic.Align.MIDDLE)
                        .scaleToFitStr(extraEntry.text(), 10, 2, 90 - 5 - iconSize - 5)

                        .useOldStrPos(false)
                        .str(extraEntry.text(), xpos + 5 + iconSize + 5, nY + (iconSize / 2))
                        .useOldStrPos(true);

                nY += 5;
            }

            if (type != null)
                g.imageCentered(type.timeRelative(!entry.isDay(rnIndex + i)).icon(), (xpos + 45) - 37, (ypos + 110) - 37, 75, 75);
            else {
                g.alignment(ImageGraphic.Align.TOP)
                        .str("?", xpos + 45, ypos + 120);
            }

            xpos += 91;
        }

        return g;
    }

    public static ImageGraphic renderNoForecast(double renderScale, ForecastStatus status) {
        ImageGraphic g = new ScaledImageGraphic(WeatherOverlay.FORECAST_DAY.overlay(), renderScale)
                .font(Constants.SIGNAGE_FONT, 30)
                .useOldStrPos(false);
        g.vignette(g.width()/2, g.height()/2, g.width(), 128);

        String text = status.message();

        int strW = g.metrics().stringWidth(text);
        int strH = g.metrics().getHeight();

        int rectW = strW + (25*2);
        int rectH = strH + (25*2);

        return g.color(Constants.FORECAST_BACKGROUND)
                .alignment(ImageGraphic.Align.MIDDLE)
                .justification(ImageGraphic.Justify.CENTER)
                .composite(AlphaComposite.SrcOver.derive(.85f))
                .rect(g.width()/2 - (rectW/2), g.height()/2 - (rectH/2), rectW, rectH, true)
                .composite(AlphaComposite.SrcOver)
                .color(Color.white)
                .str(text, g.width()/2, g.height()/2);
    }
}
