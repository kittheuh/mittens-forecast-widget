package io.github.kittheuh.mforecast.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.kittheuh.mforecast.*;
import io.github.kittheuh.mforecast.api.MeteoForecast;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
    public static final Logger LOGGER = Logger.getLogger(Util.class.getName());
    public static final BufferedImage NULL_IMAGE = ImageGraphic.createFallback();

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Creates the file needed to make this app launch automatically on login.
     * This only works on Windows cuz idk how to make it work elsewhere
     * @return true if the file was created
     */
    public static boolean createAutoLaunchFile() {
        if (!writeAutoLaunchFile()) return false;

        var startupDir = getStartupDirectory();
        try {
            Path startupBatFile = startupDir.resolve("MittensForecastWidget.bat");
            Files.copy(Paths.get("./autostart.bat"), startupBatFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not create auto launch file", e);
            return false;
        }
        return true;
    }

    /**
     * Deletes the file that would make this app launch automatically on login.
     * Same issue as above, windows only.
     * @return true if the file was deleted
     */
    public static boolean deleteAutoLaunchFile() {
        var startupDir = getStartupDirectory();

        try {
            Files.deleteIfExists(startupDir.resolve("MittensForecastWidget.bat"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not delete auto launch file", e);
            return false;
        }
        return true;
    }

    /**
     * @return Windows startup directory, probably only works for Windows 10/11
     */
    public static Path getStartupDirectory() {
        return Paths.get("%s\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup".formatted(
                System.getProperty("user.home")
        ));
    }

    /**
     * Actually writes the auto launch file, still windows only.
     * @return true if the file was written
     */
    public static boolean writeAutoLaunchFile() {
        Path path = Paths.get("./autostart.bat");

        try {
            // https://mkyong.com/java/java-get-the-name-or-path-of-a-running-jar-file/
            String jarPath = MittensMain.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            if (jarPath.startsWith("/")) jarPath = jarPath.substring(1);

            Files.writeString(path, "@runmode:hide%n@echo off%nstart /min /d %s javaw -jar %s".formatted(
                    System.getProperty("user.dir"), jarPath)
            );
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Could not write autostart.bat", e);
            return false;
        }
        return true;
    }

    public static Rectangle getScreenSafeBounds() {
        Rectangle rect = new Rectangle();

        var devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice device : devices) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();

            rect = rect.union(bounds);
        }

        return rect;
    }

    public static Font tryLoadFont(String filepath) {
        if (filepath == null) return new Font(Font.MONOSPACED, Font.PLAIN, 12);

        InputStream is = ClassLoader.getSystemResourceAsStream("assets/" + filepath);
        if (is == null) {
            LOGGER.warning("Failed to load \"assets/%s\", file not found".formatted(filepath));
            return new Font(Font.MONOSPACED, Font.PLAIN, 12);
        }

        try (is) {
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 12);
        } catch (IOException | FontFormatException e) {
            LOGGER.log(Level.WARNING, "Failed to load \"assets/%s\"".formatted(filepath), e);
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 12);
    }

    public static BufferedImage tryLoadImage(String filepath) {
        if (filepath == null) return NULL_IMAGE;

        InputStream is = ClassLoader.getSystemResourceAsStream("assets/" + filepath);
        if (is == null) {
            LOGGER.warning("Failed to load \"assets/%s\", file not found".formatted(filepath));
            return NULL_IMAGE;
        }

        try (is) {
            return ImageIO.read(is);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load \"assets/%s\"".formatted(filepath), e);
        }

        return NULL_IMAGE;
    }

    public static JsonElement tryGetPage(String link) {
        try {
            URLConnection connection = URI.create(link).toURL().openConnection();
            connection.addRequestProperty("User-Agent", "MittensForecastWidget/" + Constants.VERSION);
            connection.connect();

            InputStreamReader r = new InputStreamReader(connection.getInputStream());
            return JsonParser.parseReader(r);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve page url \"%s\"".formatted(link), e);
            return null;
        }
    }

    public static String[] stringMapper(JsonArray array) {
        String[] values = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            values[i] = array.get(i).getAsString();
        }
        return values;
    }

    public static float[] floatMapper(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    public static int[] intMapper(JsonArray array) {
        int[] values = new int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            values[i] = array.get(i).getAsInt();
        }
        return values;
    }

    public static boolean[] boolMapper(JsonArray array) {
        boolean[] values = new boolean[array.size()];
        for (int i = 0; i < array.size(); i++) {
            values[i] = array.get(i).getAsInt() == 1;
        }
        return values;
    }

    public static ZonedDateTime[] dateMapper(ZoneId zoneId, JsonArray array) {
        return array.asList()
                .stream()
                .map(JsonElement::getAsString)
                .map(LocalDate::parse)
                .map(date -> date.atStartOfDay(zoneId))
                .toArray(ZonedDateTime[]::new);
    }

    public static ZonedDateTime[] dateTimeMapper(ZoneId zoneId, JsonArray array) {
        return array.asList()
                .stream()
                .map(JsonElement::getAsString)
                .map(LocalDateTime::parse)
                .map(t -> t.atZone(zoneId))
                .toArray(ZonedDateTime[]::new);
    }

    public static String hourToString(int hour, boolean use24HrFmt) {
        hour = hour % 24;
        if (use24HrFmt) return "%02d:00".formatted(hour);

        if (hour == 0) return "12 AM";
        else if (hour < 12) return hour + " AM";
        else if (hour == 12) return "12 PM";
        else return (hour % 12) + " PM";
    }

    public static String hourMinToString(int hour, int min, boolean use24HrFmt) {
        hour = hour % 24;
        if (use24HrFmt) return "%02d:%02d".formatted(hour, min);

        if (hour == 0) return "12:%02d AM".formatted(min);
        else if (hour < 12) return "%d:%02d AM".formatted(hour, min);
        else if (hour == 12) return "12:%02d PM".formatted(min);
        else return "%d:%02d PM".formatted(hour % 12, min);
    }

    public static double celsiusesToFahrenheit(double celsius) {
        return celsius * 9 / 5 + 32;
    }

    public static String weatherCodeToFriendly(int code) {
        return switch (code) {
            case 99 -> "Thunderstorm with heavy hail";
            case 97 -> "Heavy thunderstorm";
            case 96 -> "Thunderstorm with slight hail";
            case 95 -> "Thunderstorm";
            case 86 -> "Heavy snow showers";
            case 85 -> "Slight snow showers";
            case 82 -> "Violent rain showers";
            case 81 -> "Moderate rain showers";
            case 80 -> "Slight rain showers";
            case 77 -> "Snow grains";
            case 75 -> "Heavy snowfall";
            case 73 -> "Moderate snowfall";
            case 71 -> "Slight snowfall";
            case 67 -> "Heavy freezing rain";
            case 66 -> "Light freezing rain";
            case 65 -> "Heavy rain";
            case 63 -> "Moderate rain";
            case 61 -> "Slight rain";
            case 57 -> "Dense freezing drizzle";
            case 56 -> "Light freezing drizzle";
            case 55 -> "Dense drizzle";
            case 53 -> "Moderate drizzle";
            case 51 -> "Light drizzle";
            case 48 -> "Depositing rime fog";
            case 45 -> "Fog";
            case 3 -> "Overcast";
            case 2 -> "Partly cloudy";
            case 1 -> "Mainly clear";
            case 0 -> "Clear sky";
            default -> "Unknown";
        };
    }

    public static WeatherType weatherCodeToType(int code) {
        return switch (code) {
            case 99, 97, 96, 95 -> WeatherType.THUNDERSTORM;
            case 86 -> WeatherType.BLIZZARD;
            case 85, 75, 73 -> WeatherType.SNOWSTORM;
            case 82 -> WeatherType.RAIN;
            case 81, 65, 63, 61 -> WeatherType.SHOWERS;
            case 80, 55, 53, 51 -> WeatherType.DRIZZLE;
            case 77, 71 -> WeatherType.SNOW;
            case 67, 66 -> WeatherType.FREEZING_DRIZZLE;
            case 57, 56 -> WeatherType.COLD_DRIZZLE;
            case 48, 45 -> WeatherType.FOGGY;
            case 3 -> WeatherType.CLOUDY;
            case 2 -> WeatherType.DAY_MOSTLY_CLOUDY;
            case 1 -> WeatherType.DAY_PARTLY_CLOUDY;
            case 0 -> WeatherType.DAY_CLEAR;
            default -> null;
        };
    }

    public static void applyWeatherOverlays(ImageGraphic g, MeteoForecast forecast, int rightNow) {
        WeatherType type = weatherCodeToType(forecast.weatherCode(rightNow));
        int rainChance = forecast.rainChance(rightNow);

        WeatherOverlay overlay = null;
        if (type != null) {
            overlay = switch (type) {
                case BLIZZARD, SNOWSTORM, SNOW, COLD_DRIZZLE, ICE_COLD_DRIZZLE, FREEZING_DRIZZLE -> WeatherOverlay.SNOW;
                case HURRICANE, THUNDERSTORM, FLOOD -> WeatherOverlay.RAIN;
                case FOGGY -> WeatherOverlay.FOG;
                default -> {
                    if (rainChance > 0) yield WeatherOverlay.RAIN;
                    yield null;
                }
            };
        }

        // i don't think anything can trigger a hurricane type?
        if (type == WeatherType.HURRICANE) {
            g.translate(-g.width(), -g.height());
            g.rotate(g.width()/2, g.height()/2, Constants.HURRICANE_ROT);
        }

        if (overlay != null) {
            if (overlay == WeatherOverlay.RAIN && rainChance > 0)
                g.composite(AlphaComposite.SrcAtop.derive(rainChance / 100f));

            if (type == WeatherType.HURRICANE) {
                g.tile(overlay.overlay(), 0, 0, g.width()*3, g.height()*3, 1)
                        .tile(overlay.overlay(), -g.width()/2, -g.height()/2, g.width()*3, g.height()*3, 1);
            } else g.image(overlay.overlay(), 0, 0);

            // these drizzles are hybrids
            if (type == WeatherType.COLD_DRIZZLE || type == WeatherType.ICE_COLD_DRIZZLE || type == WeatherType.FREEZING_DRIZZLE)
                g.image(WeatherOverlay.RAIN.overlay(), 0, 0);

            g.composite(AlphaComposite.SrcOver);
        }
        // foggy would already draw the overlay above, this would double it
//        if (fog) g.image(WeatherOverlay.FOG.overlay(), 0, 0);

        // again, don't think a hurricane can happen, neither a flood
        if (type == WeatherType.HURRICANE) {
            g.rotate(g.width()/2, g.height()/2, -Constants.HURRICANE_ROT)
                    .translate(g.width(), g.height())
            ;
        }
        if (type == WeatherType.FLOOD) applyFloodEffect(g);
    }

    public static void applyFloodEffect(ImageGraphic g) {
        g.composite(AlphaComposite.SrcOver.derive(.8f))
                .image(Constants.WEATHER_BG_FG.frame(1, 1), 0, Math.round(ThreadLocalRandom.current().nextFloat() * 200))
                .debugCross(0, g.height()/2, g.width(), g.height()/2)
                .composite(AlphaComposite.SrcOver);
    }

    public static String currentTimeClock(ZonedDateTime now) {
        String clock;
        if (Config.use24HrFmt) {
            clock = "%02d:%02d:%02d".formatted(now.getHour(), now.getMinute(), now.getSecond());
        } else {
            String suffix;
            int displayHour;

            int hour = now.getHour();
            if (hour < 12) {
                displayHour = hour == 0 ? 12 : hour;
                suffix = "AM";
            } else {
                displayHour = hour % 12;
                suffix = "PM";
            }
            clock = "%02d:%02d:%02d %s".formatted(displayHour, now.getMinute(), now.getSecond(), suffix);
        }
        return clock;
    }
}
