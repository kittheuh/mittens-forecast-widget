package io.github.kittheuh.mforecast;

import io.github.kittheuh.mforecast.api.ForecastFrequency;
import io.github.kittheuh.mforecast.util.AsyncUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());
    private static final Path CONFIG_PATH = Paths.get("./mittens_forecast_widget.cfg");

    public static float locationLatitude, locationLongitude;
    public static String locationCityName;
    public static boolean locationSet, closeNoticeShown, use24HrFmt, useFahrenheit, alwaysOnTop, undecorated, autoLaunch,
            slowerAnim;
    public static boolean useLastLocation = true, drawMittens = true, playSfx = true, drawLocalClock = true, hideToTray = true,
            resizeToFit = true, checkForUpdates = true;
    public static int lastLocX, lastLocY, lastLocW = 810, lastLocH = 496, quipFrequency = 2, windowTransparency = 100;
    public static double lastLocScale = 1;
    public static ForecastFrequency frequency = ForecastFrequency.FIFTEEN_MIN;

    public static void loadFromConfig() {
        if (Files.notExists(CONFIG_PATH)) return;

        Properties prop = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            prop.load(in);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load config file!", e);
            return;
        }

        try {
            if (prop.containsKey("location.latitude")) locationLatitude = Float.parseFloat(prop.getProperty("location.latitude"));
            if (prop.containsKey("location.longitude")) locationLongitude = Float.parseFloat(prop.getProperty("location.longitude"));
            if (prop.containsKey("location.city")) locationCityName = prop.getProperty("location.city");
            if (prop.containsKey("location.set")) locationSet = Boolean.parseBoolean(prop.getProperty("location.set"));

            if (prop.containsKey("tip.close-notice-shown")) closeNoticeShown = Boolean.parseBoolean(prop.getProperty("tip.close-notice-shown"));

            if (prop.containsKey("pref.quip-frequency")) quipFrequency = Math.max(0, Integer.parseInt(prop.getProperty("pref.quip-frequency")));
            if (prop.containsKey("pref.window-transparency")) windowTransparency = Math.clamp(Integer.parseInt(prop.getProperty("pref.window-transparency")), 10, 100);
            if (prop.containsKey("pref.draw-clock")) drawLocalClock = Boolean.parseBoolean(prop.getProperty("pref.draw-clock"));
            if (prop.containsKey("pref.hide-to-tray")) hideToTray = Boolean.parseBoolean(prop.getProperty("pref.hide-to-tray"));
            if (prop.containsKey("pref.auto-launch")) autoLaunch = Boolean.parseBoolean(prop.getProperty("pref.auto-launch"));
            if (prop.containsKey("pref.resize-to-fit")) resizeToFit = Boolean.parseBoolean(prop.getProperty("pref.resize-to-fit"));
            if (prop.containsKey("do-update-checks")) checkForUpdates = Boolean.parseBoolean(prop.getProperty("do-update-checks"));

            if (prop.containsKey("pref.always-on-top")) alwaysOnTop = Boolean.parseBoolean(prop.getProperty("pref.always-on-top"));
            if (prop.containsKey("pref.draw-mittens")) drawMittens = Boolean.parseBoolean(prop.getProperty("pref.draw-mittens"));
            if (prop.containsKey("pref.slower-anim")) slowerAnim = Boolean.parseBoolean(prop.getProperty("pref.slower-anim"));
            if (prop.containsKey("pref.use-24-hr-fmt")) use24HrFmt = Boolean.parseBoolean(prop.getProperty("pref.use-24-hr-fmt"));
            if (prop.containsKey("pref.use-fahrenheit")) useFahrenheit = Boolean.parseBoolean(prop.getProperty("pref.use-fahrenheit"));
            if (prop.containsKey("pref.undecorated")) undecorated = Boolean.parseBoolean(prop.getProperty("pref.undecorated"));
            if (prop.containsKey("pref.frequency")) frequency = ForecastFrequency.valueOf(prop.getProperty("pref.frequency"));
            else frequency = ForecastFrequency.FIFTEEN_MIN;


            if (prop.containsKey("pref.use-last-location")) useLastLocation = Boolean.parseBoolean(prop.getProperty("pref.use-last-location"));
            if (useLastLocation) {
                if (prop.containsKey("pref.use-last-location.x")) lastLocX = Integer.parseInt(prop.getProperty("pref.use-last-location.x"));
                if (prop.containsKey("pref.use-last-location.y")) lastLocY = Integer.parseInt(prop.getProperty("pref.use-last-location.y"));
                if (prop.containsKey("pref.use-last-location.w")) lastLocW = Integer.parseInt(prop.getProperty("pref.use-last-location.w"));
                if (prop.containsKey("pref.use-last-location.h")) lastLocH = Integer.parseInt(prop.getProperty("pref.use-last-location.h"));
                if (prop.containsKey("pref.use-last-location.scale")) lastLocScale = Double.parseDouble(prop.getProperty("pref.use-last-location.scale"));
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Unable to load configuration, invalid number!", e);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unable to load configuration, invalid enum name!", e);
        }
    }


    public static void saveToConfig() {
        Properties prop = new Properties();
        prop.setProperty("version", Constants.VERSION);
        prop.setProperty("do-update-checks", Boolean.toString(checkForUpdates));

        prop.setProperty("location.latitude", Float.toString(locationLatitude));
        prop.setProperty("location.longitude", Float.toString(locationLongitude));
        prop.setProperty("location.city", locationCityName);
        prop.setProperty("location.set", Boolean.toString(locationSet));
        prop.setProperty("tip.close-notice-shown", Boolean.toString(closeNoticeShown));
        prop.setProperty("pref.use-24-hr-fmt", Boolean.toString(use24HrFmt));
        prop.setProperty("pref.use-fahrenheit", Boolean.toString(useFahrenheit));
        prop.setProperty("pref.draw-mittens", Boolean.toString(drawMittens));
        prop.setProperty("pref.slower-anim", Boolean.toString(slowerAnim));
        prop.setProperty("pref.always-on-top", Boolean.toString(alwaysOnTop));
        prop.setProperty("pref.undecorated", Boolean.toString(undecorated));
        prop.setProperty("pref.draw-clock", Boolean.toString(drawLocalClock));
        prop.setProperty("pref.hide-to-tray", Boolean.toString(hideToTray));
        prop.setProperty("pref.auto-launch", Boolean.toString(autoLaunch));
        prop.setProperty("pref.resize-to-fit", Boolean.toString(resizeToFit));
        prop.setProperty("pref.frequency", frequency.name());
        prop.setProperty("pref.quip-frequency", Integer.toString(quipFrequency));

        prop.setProperty("pref.use-last-location", Boolean.toString(useLastLocation));
        if (useLastLocation) {
            prop.setProperty("pref.use-last-location.x", Integer.toString(lastLocX));
            prop.setProperty("pref.use-last-location.y", Integer.toString(lastLocY));
            prop.setProperty("pref.use-last-location.w", Integer.toString(lastLocW));
            prop.setProperty("pref.use-last-location.h", Integer.toString(lastLocH));
            prop.setProperty("pref.use-last-location.scale", Double.toString(lastLocScale));
        }

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            prop.store(out, "%s by kittheuh (v%s)".formatted(Constants.APP_NAME, Constants.VERSION));
            LOGGER.info("Saved configuration!");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to save configuration", e);
        }
    }

    public static void scheduleSave() {
        AsyncUtil.scheduleDelayable("m4cst:save-config", Config::saveToConfig, 30, TimeUnit.SECONDS);
    }
}
