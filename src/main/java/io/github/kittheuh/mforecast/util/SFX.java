package io.github.kittheuh.mforecast.util;

import io.github.kittheuh.mforecast.Constants;

import javax.imageio.stream.IIOByteBuffer;
import javax.sound.sampled.*;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SFX {
    private static final Logger LOGGER = Logger.getLogger(SFX.class.getName());
    private final Clip clip;

    public SFX(String filename)  {
        InputStream is;

        if (Constants.VERSION.equals("DEV")) {
            try {
                is = Files.newInputStream(Paths.get("./src/main/resources/" + filename));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read bytes for SFX \"%s\"".formatted(filename), e);
                clip = null;
                return;
            }
        } else {
            is = ClassLoader.getSystemResourceAsStream(filename);
            if (is == null) {
                LOGGER.warning("Failed to find SFX: " + filename);
                clip = null;
                return;
            }
        }

        ByteArrayInputStream inputStream;
        try (is) {
            inputStream = new ByteArrayInputStream(is.readAllBytes());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read bytes for SFX \"%s\"".formatted(filename), e);
            clip = null;
            return;
        }

        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(inputStream));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load SFX \"%s\"".formatted(filename), e);
        } catch (LineUnavailableException e) {
            LOGGER.log(Level.WARNING, "Failed to load SFX \"%s\", line unavailable".formatted(filename), e);
        } catch (UnsupportedAudioFileException e) {
            LOGGER.log(Level.WARNING, "Failed to load SFX \"%s\", unsupported file type".formatted(filename), e);
        }
        this.clip = clip;
    }

    public void play() {
        if (clip == null) return; // initialized if good

        clip.stop();
        clip.setMicrosecondPosition(0);
        clip.start();
    }
}
