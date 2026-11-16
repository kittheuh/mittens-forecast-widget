package io.github.kittheuh.mforecast;

import io.github.kittheuh.mforecast.util.SFX;
import io.github.kittheuh.mforecast.util.Spritesheet;
import io.github.kittheuh.mforecast.util.UpdateUtil;
import io.github.kittheuh.mforecast.util.Util;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public class Constants {
    private static final Logger LOGGER = Logger.getLogger(Constants.class.getName());

    public static final Spritesheet WEATHER_BG_FG = new Spritesheet(
            "weather_bg_fg.png",
            3, 2
    ), WEATHER_ICONS = new Spritesheet(
            "weather_icons.png",
            6, 5
    );

    public static final BufferedImage
            ICON = WEATHER_ICONS.frame(5, 1),
            M_TALK = Util.tryLoadImage("mittens_talk.png")
    ;

    public static final Font
            FONT = Util.tryLoadFont("sub_font.ttf"),
            SIGNAGE_FONT = Util.tryLoadFont("main_font.ttf");

    public static final SFX SFX_QUIP;

    public static final Color TRANSPARENT = new Color(0, true);
    public static final Color FORECAST_BACKGROUND = new Color(0x14345A);
    public static final Color FORECAST_BACKGROUND_B = FORECAST_BACKGROUND.brighter();
    public static final Color FORECAST_BACKGROUND_BB = FORECAST_BACKGROUND.brighter().brighter();

    public static final String APP_NAME = "Mittens Forecast Widget";

    public static final String VERSION;
    public static final int SPEECH_BUBBLE_TXT_MAX_WIDTH = 250;
    public static final float HURRICANE_ROT = 23f;

    static {
        String version = null;
        try {
            InputStream stream = ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF");
            if (stream != null) {
                Manifest manifest = new Manifest(stream);

                version = manifest.getMainAttributes().getValue("Version");
            } else LOGGER.warning("META-INF/MANIFEST.MF not found");
        } catch (IOException e) {
            LOGGER.warning("Failed to find the version number from the manifest");
        }
        if (version == null) {
            LOGGER.info("Using DEV version number");
            version = "DEV";
        }

        VERSION = version;
        // SFXs must init after version as they rely on that field to determine loading as a file or from ClassLoader
        SFX_QUIP = new SFX("assets/quip.wav");
    }

    public static UpdateUtil.Version getVersion() {
        return UpdateUtil.toVersion(VERSION);
    }
}
