package io.github.kittheuh.mforecast;


import io.github.kittheuh.mforecast.gui.ForecastWidget;
import io.github.kittheuh.mforecast.gui.LocationPopup;
import io.github.kittheuh.mforecast.gui.SettingsFrame;
import io.github.kittheuh.mforecast.util.AsyncUtil;
import io.github.kittheuh.mforecast.util.UpdateUtil;
import io.github.kittheuh.mforecast.util.Util;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MittensMain {
    private static final Logger LOGGER = Logger.getLogger(MittensMain.class.getName());

    private static TrayIcon TRAY_ICON;
    private static ForecastWidget forecastWidget;
    private static SettingsFrame settingsFrame;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.log(Level.WARNING, "Failed to set the system look and feel", e);
        }

        var lockFile = Paths.get("./mittens.lock");
        if (Files.exists(lockFile)) {
            LOGGER.severe("LOCK FILE ALREADY EXISTS!");
            int option = JOptionPane.showConfirmDialog(null, "The widget appears to already be open! It may still be running but in the background, especially if the \"Hide to Tray\" setting is enabled (it is by default!)\n\nIf you wish to continue anyways, or if this is an error, press OK.", "Mittens Forecast Widget", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (option != JOptionPane.OK_OPTION) return;
        }
        try {
            Files.createFile(lockFile);
            lockFile.toFile().deleteOnExit();
        } catch (IOException e) {
            LOGGER.severe("FAILED TO CREATE LOCK FILE!");
        }

        ImageIO.setUseCache(false);
        Constants.VERSION.length(); // load constants
        Config.loadFromConfig();

        if (Config.checkForUpdates) {
            UpdateUtil.Version newVersion = UpdateUtil.checkForUpdates();
            UpdateUtil.Version currentVersion = Constants.getVersion();

            // skip update check if DEV or fail
            if (newVersion != null && currentVersion != null && currentVersion.isOlder(newVersion)) {
                int option = JOptionPane.showConfirmDialog(settingsFrame(), "An update is available!\n\nCurrent version: v%s\nNew Version: v%s\n\nPress OK to visit the download page, or cancel to ignore.".formatted(currentVersion, newVersion),
                        Constants.APP_NAME, JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (option == JOptionPane.OK_OPTION) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/kittheuh/mittens-forecast-widget/releases"));
                    } catch (IOException | URISyntaxException e) {
                        LOGGER.warning("Failed to open update URL");
                        JOptionPane.showMessageDialog(settingsFrame(), "Failed to open the download page, sorry!\n\nhttps://github.com/kittheuh/mittens-forecast-widget/releases", Constants.APP_NAME, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

        initTrayIcon();

        if (!Config.locationSet) {
            String message = "Welcome to the %s (v%s) by kittheuh!\nThis appears to be the first time you are launching this app, so would you like to set the forecast location first?".formatted(Constants.APP_NAME, Constants.VERSION);

            int option = JOptionPane.showConfirmDialog(settingsFrame(), message, Constants.APP_NAME, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) new LocationPopup(true);
            else {
                // canceling would've skipped the tip on how to open settings
                JOptionPane.showMessageDialog(settingsFrame(), "You can always set the forecast location later by opening the settings.\nThere are two ways to open the settings:\n1. Right click on the Forecast Widget, then click on 'Settings'\n2. Right click on the tray icon, and click on 'Open Settings'.",
                        Constants.APP_NAME,
                        JOptionPane.PLAIN_MESSAGE
                );
                forecastWidget().setVisible(true);
            }
        } else {
//            settingsFrame().setVisible(true);
            forecastWidget().setVisible(true);
        }

        if (Config.autoLaunch && Util.isWindows()) Util.createAutoLaunchFile();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AsyncUtil.shutdown(false);
            Config.saveToConfig();
        }, "MFW/Shutdown Hook"));
    }

    private static void initTrayIcon() {
        TRAY_ICON = new TrayIcon(Constants.ICON, Constants.APP_NAME);
        try {
            TRAY_ICON.setImageAutoSize(true);

            PopupMenu popup = new PopupMenu(Constants.APP_NAME);
            popup.addActionListener(e -> {
                switch (e.getActionCommand()) {
                    case "Open Forecast Widget" -> forecastWidget().setVisible(true);
                    case "Open Settings" -> settingsFrame().setVisible(true);
                    case "Quit" -> System.exit(0);
                    default -> settingsFrame().showAboutDialog();
                }
            });
            popup.add(Constants.APP_NAME);
            popup.add("Version " + Constants.VERSION);
            popup.add("By kittheuh");
            popup.addSeparator();
            popup.add("Open Forecast Widget");
            popup.add("Open Settings");
            popup.addSeparator();
            popup.add("Quit");

            TRAY_ICON.setPopupMenu(popup);
            TRAY_ICON.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) return;
                    forecastWidget().setVisible(true);
                }
            });
            SystemTray.getSystemTray().add(TRAY_ICON);
        } catch (AWTException e) {
            LOGGER.log(Level.WARNING, "Failed to initialize TrayIcon", e);
        }
    }

    public static void updateTrayIcon(WeatherType currType) {
        BufferedImage icon;
        if (currType == null) icon = Constants.ICON;
        else icon = currType.icon();

        TRAY_ICON.setImage(icon);
    }

    public static void sendTrayMessage(String message) {
        TRAY_ICON.displayMessage(Constants.APP_NAME, message, TrayIcon.MessageType.INFO);
    }

    public static ForecastWidget forecastWidget() {
        if (forecastWidget == null) forecastWidget = new ForecastWidget();
        return forecastWidget;
    }

    public static SettingsFrame settingsFrame() {
        if (settingsFrame == null) settingsFrame = new SettingsFrame();
        return settingsFrame;
    }

    public static boolean forecastWidgetVisible() {
        return forecastWidget != null;
    }

    public static boolean settingsFrameVisible() {
        return settingsFrame != null;
    }
}
