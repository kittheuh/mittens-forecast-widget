package io.github.kittheuh.mforecast.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import io.github.kittheuh.mforecast.WeatherType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeatherTypeLines {
    private static final Logger LOGGER = Logger.getLogger(WeatherTypeLines.class.getName());
    private static final EnumMap<WeatherType, String[]> lineMap = new EnumMap<>(WeatherType.class);

    static {
        InputStream is = ClassLoader.getSystemResourceAsStream("forecast_lines.json");
        if (is != null) {
            try (var isr = new InputStreamReader(is)) {
                JsonElement element = JsonParser.parseReader(isr);
                if (element.isJsonObject()) {
                    var obj = element.getAsJsonObject();

                    for (WeatherType type : WeatherType.values()) {
                        if (!obj.has(type.name()) || !obj.get(type.name()).isJsonArray()) continue;

                        var arr = Util.stringMapper(obj.getAsJsonArray(type.name()));
                        lineMap.put(type, arr);
                    }
                }
            } catch (IOException | JsonParseException e) {
                LOGGER.log(Level.WARNING, "Failed to load the forecast lines!", e);
            }
        } else LOGGER.warning("Failed to find the forecast lines file!");
    }

    public static String randomLine(WeatherType type) {
        String[] strings = lineMap.get(type);
        if (strings == null) return null;
        return strings[ThreadLocalRandom.current().nextInt(strings.length)];
    }
}
