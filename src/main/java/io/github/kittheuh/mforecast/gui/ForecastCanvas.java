package io.github.kittheuh.mforecast.gui;

import io.github.kittheuh.mforecast.*;
import io.github.kittheuh.mforecast.api.CumulativeMeteoForecast;
import io.github.kittheuh.mforecast.api.ForecastFrequency;
import io.github.kittheuh.mforecast.api.ForecastManager;
import io.github.kittheuh.mforecast.api.MeteoForecast;
import io.github.kittheuh.mforecast.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForecastCanvas extends JPanel {
    private static final Dimension PREFERRED_SIZE = new Dimension(810, 496);
    public static final Color TRANSPARENT_BLACK = new Color(0x66000000, true);

    private final EnumMap<ForecastFrequency, JRadioButtonMenuItem> cmButtons = new EnumMap<>(ForecastFrequency.class);
    private final ForecastWidget parent;

    // Forecast
    private final AtomicBoolean refreshingForecast = new AtomicBoolean(false);
    private boolean displayLoadingMessage;
    private ImageGraphic forecast;
    private double renderScale;

    // Animation
    private final Thread tickThread;
    private int ticks;

    // Quips
    private QuipBubble lastQuip;
    private long lastQuipTimestamp = -1;

    public ForecastCanvas(ForecastWidget parent) {
        this.parent = parent;
        Dimension size = Config.useLastLocation ? new Dimension(Config.lastLocW, Config.lastLocH) : PREFERRED_SIZE;
        renderScale = Config.useLastLocation ? Config.lastLocScale : 1.0d;
        setPreferredSize(size);

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        initPopup();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    lastQuip = null;
                    repaint();
                }
            }
        });

        tickThread = new Thread(this::tick);
        tickThread.setName("MFW/Tick Thread");
        tickThread.setDaemon(true);

        tickThread.start();
        AsyncUtil.SCHEDULED_SERVICE.scheduleAtFixedRate(this::refreshForecast, 0, 5, TimeUnit.MINUTES);
    }

    private void initPopup() {
        JPopupMenu popupMenu = new JPopupMenu(parent.getName());


        var popup_buttonGroup = new ButtonGroup();
        for (ForecastFrequency frequency : ForecastFrequency.values()) {
            var radioButton = new JRadioButtonMenuItem(frequency.displayName(), Config.frequency == frequency);
            radioButton.setActionCommand(frequency.name());
            radioButton.addActionListener(e -> updateForecastType(e.getActionCommand()));

            popupMenu.add(radioButton);
            popup_buttonGroup.add(radioButton);
            cmButtons.put(frequency, radioButton);
        }
        popupMenu.addSeparator();

        popupMenu.add(new AbstractAction("Refresh") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (refreshingForecast.get()) return;
                displayLoadingMessage = true;
                repaint();
                AsyncUtil.run(() -> refreshForecast());
            }
        });

        popupMenu.addSeparator();
        popupMenu.add(new AbstractAction("Settings") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MittensMain.settingsFrame().setVisible(true);
            }
        });
        popupMenu.add(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        popupMenu.setLightWeightPopupEnabled(true);

        setComponentPopupMenu(popupMenu);
    }

    public boolean isRefreshing() {
        return refreshingForecast.get();
    }

    public void updateForecastType(String actionCommand) {
        ForecastFrequency frequency = EnumUtil.getEnum(ForecastFrequency.class, actionCommand);
        updateForecastType(frequency);
    }

    public void updateForecastType(ForecastFrequency frequency) {
        if (frequency == null) return;
        if (Config.frequency == frequency) return;

        Config.frequency = frequency;
        Config.scheduleSave();
        MittensMain.settingsFrame().updateForecastFrequency();

        JRadioButtonMenuItem item = cmButtons.get(frequency);
        if (item != null) item.setSelected(true);

        displayLoadingMessage = true;
        repaint();
        AsyncUtil.run(() -> {
            refreshForecast(); // refresh api data for new freq. if not in cache or expired
            repaint();
        });
    }

    /**
     * re-render the forecast image, useful if the forecast frequency has been changed or the forecast data was refetched to keep updated
     */
    public void refreshForecast() {
        refreshingForecast.set(true);
        ForecastManager.checkRefresh();

        CumulativeMeteoForecast currentForecast = ForecastManager.currentForecast();
        // TODO: account for no right now or other errors
        forecast = ForecastManager.renderForecast(renderScale, currentForecast);
        displayLoadingMessage = false;


        WeatherType rightNow = null;
        if (currentForecast != null) {
            int code = currentForecast.weatherCode(currentForecast.rightNow());
            rightNow = Util.weatherCodeToType(code);
        }
        if (rightNow != null) rightNow = rightNow.timeRelative(!currentForecast.isDay(currentForecast.rightNow()));
        MittensMain.updateTrayIcon(rightNow);
        parent.setIconImage(rightNow == null ? Constants.ICON : rightNow.icon());
        refreshingForecast.set(false);
    }

    private void drawSpinner(Graphics2D g2d, String message) {
        g2d.scale(renderScale, renderScale);
        g2d.drawImage(WeatherOverlay.FORECAST_DAY.overlay(), 0, 0, null);
        g2d.setColor(TRANSPARENT_BLACK);
        g2d.fillRect(0, 0, 810, 496);

        g2d.setColor(Color.white);
        g2d.setFont(Constants.SIGNAGE_FONT.deriveFont(50f));
        g2d.drawString(message, 810/2 - (g2d.getFontMetrics().stringWidth(message)/2), 496 - 25);

        g2d.rotate((2d * Math.PI) * (ticks / 10d), 810d/2, 496d/2);
        g2d.drawImage(Constants.WEATHER_ICONS.frame(1, 2), 810/2 - 128, 496/2 - 128, null);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        if (forecast == null) {
            drawSpinner(g2d, "Initializing...");
            return;
        }

        if (getWidth() != forecast.width() || getHeight() != forecast.height()) {
            renderScale = Math.min(getWidth() / (double) forecast.width(), getHeight() / (double) forecast.height());

            boolean reRender;
            if (forecast instanceof ScaledImageGraphic sg) reRender = sg.scale() != renderScale;
            else reRender = true;
            if (reRender) forecast = ForecastManager.renderForecast(renderScale);

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            g2d.translate(getWidth()/2d - (forecast.width()*renderScale/2d), getHeight()/2d - (forecast.height()*renderScale/2d));
            g2d.drawRenderedImage(forecast.getImage(), null);

            g2d.clip(new Rectangle(0, 0, (int) (forecast.width()*renderScale), (int) (forecast.height()*renderScale)));
            g2d.scale(renderScale, renderScale);
        } else {
            renderScale = 1;
            if (forecast instanceof ScaledImageGraphic) forecast = ForecastManager.renderForecast(renderScale);
            g2d.drawRenderedImage(forecast.getImage(), null);
        }
        if (Config.lastLocScale != renderScale) {
            Config.lastLocScale = renderScale;
            if (Config.useLastLocation) Config.scheduleSave();
        }

        CumulativeMeteoForecast currentForecast = ForecastManager.currentForecast();
        if (Config.drawLocalClock && currentForecast != null) {
            ZoneId zoneId = currentForecast.zoneId();
            ZonedDateTime now = ZonedDateTime.now(zoneId);

            String clock = Util.currentTimeClock(now);

            g2d.setFont(Constants.FONT.deriveFont(28f));
            int boxWidth = 181;
            int boxHeight = 40;
            int strWidth = g2d.getFontMetrics().stringWidth(clock);

            g2d.setColor(Constants.FORECAST_BACKGROUND);
            g2d.fillRect(15, 441 + 10, boxWidth, boxHeight);

            g2d.setColor(Color.white);
            g2d.drawString(clock, 15 + (boxWidth/2) - (strWidth/2), 441 + g2d.getFontMetrics().getHeight() + 5);
        }

        if (Config.drawMittens && currentForecast != null) {
            int yOffset = ticks % 2 == 0 ? -10 : 0;

            float ms = .65f;
            BufferedImage mSprite = Constants.M_TALK;

            double mx = forecast.width() - (mSprite.getWidth() * ms) + 50;
            double my = forecast.height() - (mSprite.getHeight() * ms) + 80 - yOffset;
            var t = g2d.getTransform();
            g2d.translate(mx, my);
            g2d.scale(ms, ms);
            g2d.drawRenderedImage(mSprite, null);
            g2d.setTransform(t); // less efficient

            if (Config.quipFrequency > 0 && !quipExpired()) {
                int xpos = forecast.width() - 140 - lastQuip.buffer().getWidth();
                int ypos = forecast.height() - 190 - lastQuip.buffer().getHeight() - yOffset;
                g2d.translate(xpos, ypos);
                g2d.drawRenderedImage(lastQuip.buffer(), null);
                g2d.translate(-xpos, -ypos);
            }
        }

        if (displayLoadingMessage) drawSpinner(g2d, "Retrieving Forecast...");
    }

    public BufferedImage createSpeechBubble(String text) {
        if (text == null) return null;
        final FontMetrics metrics = getFontMetrics(Constants.SIGNAGE_FONT.deriveFont(20f));

        List<String> words;
        if (metrics.stringWidth(text) < Constants.SPEECH_BUBBLE_TXT_MAX_WIDTH) {
            // avoid wrap if not needed
            words = new ArrayList<>();
            words.add(text);
        } else {
            words = new ArrayList<>(List.of(text.split(" ")));

            int w = 0;
            while (w < words.size()-1) {
                if (!lineLimit(metrics, words.get(w), words.get(w+1))) {

                    words.add(w, words.remove(w) + ' ' + words.remove(w));
                } else w++;
            }
        }

        final int height = metrics.getHeight() * words.size();
        final int padding = 20;
        int bubbleWidth = padding + Math.min(metrics.stringWidth(text), Constants.SPEECH_BUBBLE_TXT_MAX_WIDTH) + padding;
        int bubbleHeight = padding + height + padding;

        // 62 x 11 = additional offset needed
        // 55 x 86 = inset of the trail so it looks clean
        BufferedImage sbTrail = Constants.WEATHER_ICONS.frame(5, 0);

        ImageGraphic g = new ImageGraphic(bubbleWidth + 55, bubbleHeight + 86, true)
                .nearestNeighbor()
                .color(new Color(0xf9c26b))
                .useOldStrPos(true)
                .image(sbTrail, bubbleWidth - 62, bubbleHeight - 11)
                .roundRect(0, 0, bubbleWidth, bubbleHeight, 15, true)
                .font(metrics.getFont())
                .color(Color.black);

        int xOff = 25;
        int yOff = 25 + (metrics.getHeight()/2) + 2;

        int yOffset = 0;
        while (!words.isEmpty()) {
            String str = words.removeFirst();

            g.str(str, xOff, yOff + yOffset);
            yOffset += metrics.getHeight();
        }
        return g.cleanup().getImage();
    }

    private static boolean lineLimit(FontMetrics metrics, String str, String append) {
        int f1 = metrics.stringWidth(str) + metrics.charWidth(' ');
        return f1 > Constants.SPEECH_BUBBLE_TXT_MAX_WIDTH || f1 + metrics.stringWidth(append) > Constants.SPEECH_BUBBLE_TXT_MAX_WIDTH;
    }

    private boolean quipExpired() {
        // show for 1 min max
        return lastQuip == null || lastQuipTimestamp + 60000 < System.currentTimeMillis();
    }

    private void createNewQuip(MeteoForecast forecast) {
        WeatherType type = Util.weatherCodeToType(forecast.weatherCode(forecast.rightNow()));
        if (type != null) type = type.timeRelative(!forecast.isDay(forecast.rightNow()));

        String text = WeatherTypeLines.randomLine(type);
        lastQuip = new QuipBubble(text, createSpeechBubble(text));
        if (Config.playSfx && parent.isVisible()) Constants.SFX_QUIP.play();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // animation loop
    public void tick() {
        while (true) {
            long start = System.currentTimeMillis();

            repaint();

            CumulativeMeteoForecast currentForecast = ForecastManager.currentForecast();
            if (Config.drawMittens && Config.quipFrequency > 0 && forecast != null && currentForecast != null) {
                // quips enabled!

                if (lastQuipTimestamp == -1) lastQuipTimestamp = System.currentTimeMillis();

                if (lastQuipTimestamp + TimeUnit.MINUTES.toMillis(1) < System.currentTimeMillis() &&
                        getCursor() == Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                }

                if (lastQuipTimestamp + TimeUnit.MINUTES.toMillis(Config.quipFrequency) < System.currentTimeMillis()) {
                    // time for a new quip!
                    lastQuip = null;
                    lastQuipTimestamp = System.currentTimeMillis();
                    createNewQuip(currentForecast);
                }
            }


            ticks = (ticks + 1) % 10;

            long end = System.currentTimeMillis();
            long sleep = (Config.slowerAnim ? 500 : 250) - (end - start);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
