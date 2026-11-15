package io.github.kittheuh.mforecast.gui;

import io.github.kittheuh.mforecast.Config;
import io.github.kittheuh.mforecast.Constants;
import io.github.kittheuh.mforecast.MittensMain;
import io.github.kittheuh.mforecast.api.ForecastFrequency;
import io.github.kittheuh.mforecast.util.AsyncUtil;
import io.github.kittheuh.mforecast.util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class SettingsFrame extends JFrame {
    private final JLabel
            cityDisplayer = new JLabel(),
            latLonDisplayer = new JLabel();
    private final JComboBox<ForecastFrequency> comboBox_forecastFrequency = new JComboBox<>(ForecastFrequency.values());

    public void updateLocationDisplay() {
        cityDisplayer.setText("City: %s".formatted(Config.locationCityName));
        latLonDisplayer.setText("Latitude: %s | Longitude: %s".formatted(Config.locationLatitude, Config.locationLongitude));
    }

    public void updateForecastFrequency() {
        comboBox_forecastFrequency.setSelectedIndex(Config.frequency.ordinal());
    }

    public SettingsFrame() throws HeadlessException {
        super(Constants.APP_NAME + " Settings");
        final Box mainBox = Box.createVerticalBox();

        mainBox.setBorder(new EmptyBorder(10, 15, 10, 15));
        mainBox.setAlignmentX(Component.CENTER_ALIGNMENT);

//        mainBox.add(Box.createHorizontalGlue());
        mainBox.add(createForecastBox());
        mainBox.add(Box.createVerticalGlue());
        mainBox.add(createDivider());
        mainBox.add(createGeneralBox());

        mainBox.add(createDivider());

        var button_about = new JButton("About / Credits");
        button_about.setToolTipText("Open the about page. What else do you expect?");
        button_about.addActionListener(e -> showAboutDialog());
        mainBox.add(button_about);

        var label_hoverNudge = new JLabel("Tip: Hover over settings to view a tooltip description.");
        label_hoverNudge.setToolTipText(":3");
        label_hoverNudge.setFont(label_hoverNudge.getFont().deriveFont(Font.ITALIC));
        mainBox.add(label_hoverNudge);

        add(mainBox);
        setIconImage(Constants.ICON);
        pack();
        setLocationRelativeTo(null);
    }

    private Box createDivider() {
        Box box = Box.createVerticalBox();
        box.setBorder(new EmptyBorder(5, 0, 5, 0));
        box.add(new JSeparator(JSeparator.HORIZONTAL));
        return box;
    }

    private JLabel createHeaderLabel(String label) {
        JLabel header = new JLabel(label);
        Font font = header.getFont();
        font = font.deriveFont(Font.BOLD, font.getSize() * 1.2f);
        header.setFont(font);
        header.updateUI();
        return header;
    }

    private Box createForecastBox() {
        updateLocationDisplay();
        var button_updateLatLon = new JButton("Update Location");
        button_updateLatLon.addActionListener(e -> {
            new LocationPopup(false);
        });

        var label_attribution = new JLabel("Weather data by Open-Meteo.com");
        label_attribution.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        {
            Font font = label_attribution.getFont();

            Map<TextAttribute, Object> map = new HashMap<>(font.getAttributes());
            map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            map.put(TextAttribute.FOREGROUND, new Color(0x3371E4));
            label_attribution.setFont(font.deriveFont(map));
        }
        label_attribution.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(URI.create("https://open-meteo.com/"));
                } catch (IOException ignored) {}
            }
        });

        var toggle_use24HrFmt = new JCheckBox("Use 24 Hour Format");
        toggle_use24HrFmt.setToolTipText("Display times in the 24-hour format (or military time).");
        toggle_use24HrFmt.setSelected(Config.use24HrFmt);
        toggle_use24HrFmt.addActionListener(e -> {
            Config.use24HrFmt = toggle_use24HrFmt.isSelected();
            repaintForecast();
            Config.scheduleSave();
        });

        var toggle_useFahrenheit = new JCheckBox("Temperature Uses Fahrenheit");
        toggle_useFahrenheit.setToolTipText("Display temperature in Fahrenheit instead of Celsius.");
        toggle_useFahrenheit.setSelected(Config.useFahrenheit);
        toggle_useFahrenheit.addActionListener(e -> {
            Config.useFahrenheit = toggle_useFahrenheit.isSelected();
            repaintForecast();
            Config.scheduleSave();
        });

        var label_forecastFrequency = new JLabel("Forecast Frequency: ");
        label_forecastFrequency.setToolTipText("Handles how spread out the forecast entries are.");
        updateForecastFrequency();
        comboBox_forecastFrequency.setToolTipText("Handles how spread out the forecast entries are.");
        comboBox_forecastFrequency.addActionListener(e -> {
            ForecastFrequency selectedItem = (ForecastFrequency) comboBox_forecastFrequency.getSelectedItem();
            MittensMain.forecastWidget().forecastCanvas().updateForecastType(selectedItem);
        });

        Box box = Box.createVerticalBox();
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(createHeaderLabel("Forecast Settings"));
        box.add(cityDisplayer);
        box.add(latLonDisplayer);
        box.add(button_updateLatLon);
        box.add(label_attribution);
        box.add(toggle_use24HrFmt);
        box.add(toggle_useFahrenheit);
        {
            Box subBox = Box.createHorizontalBox();
            subBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            subBox.add(label_forecastFrequency);
            subBox.add(comboBox_forecastFrequency);
            box.add(subBox);
        }
        return box;
    }

    private Box createGeneralBox() {
        var label_quipFrequency = new JLabel("Quip Frequency (Minutes): ");
        label_quipFrequency.setToolTipText("How often Mittens will say a line regarding the weather. Each line lasts for 1 minute. Set to 0 to disable.");
        var spinner_quipFrequency = new JSpinner();
        spinner_quipFrequency.setModel(new SpinnerNumberModel(Config.quipFrequency, 0, Integer.MAX_VALUE, 1));
        spinner_quipFrequency.setToolTipText("How often Mittens will say a line regarding the weather. Each line lasts for 1 minute. Set to 0 to disable.");
        spinner_quipFrequency.addChangeListener(e -> {
            Config.quipFrequency = (int) spinner_quipFrequency.getValue();
            Config.scheduleSave();
        });

        var toggle_drawMittens = new JCheckBox("Draw Mittens");
        toggle_drawMittens.setToolTipText("Toggle showing Mittens on the forecast widget.");
        toggle_drawMittens.setSelected(Config.drawMittens);
        toggle_drawMittens.addActionListener(e -> {
            Config.drawMittens = toggle_drawMittens.isSelected();
            spinner_quipFrequency.setEnabled(Config.drawMittens);
            repaintForecast();
            Config.scheduleSave();
        });

        var toggle_playSfx = new JCheckBox("Play Sound Effects");
        toggle_playSfx.setToolTipText("Whether to play sound effects for specific actions.");
        toggle_playSfx.setSelected(Config.playSfx);
        toggle_playSfx.addActionListener(e -> {
            Config.playSfx = toggle_playSfx.isSelected();
            Config.scheduleSave();
        });

        var toggle_slowerAnim = new JCheckBox("Slower Animation");
        toggle_slowerAnim.setToolTipText("Animation runs at 4 FPS by default. Enable to run at 2 FPS instead.");
        toggle_slowerAnim.setSelected(Config.slowerAnim);
        toggle_slowerAnim.addActionListener(e -> {
            Config.slowerAnim = toggle_slowerAnim.isSelected();
            Config.scheduleSave();
        });

        var toggle_hideToTray = new JCheckBox("Hide to System Tray");
        toggle_hideToTray.setToolTipText("When enabled, closing the widget will make it run in the background. Disable this to make closing the widget instead exit entirely. You can always exit directly via the \"Quit\" buttons on the tray icon or the forecast widget right click menu.");
        toggle_hideToTray.setSelected(Config.hideToTray);
        toggle_hideToTray.addActionListener(e -> {
            Config.hideToTray = toggle_hideToTray.isSelected();
            MittensMain.forecastWidget().setDefaultCloseOperation(Config.hideToTray ? HIDE_ON_CLOSE : EXIT_ON_CLOSE);
            Config.scheduleSave();
        });

        var toggle_useLastLocation = new JCheckBox("Persistent Location");
        toggle_useLastLocation.setToolTipText("If enabled, the widget will save its current position and size, so it persist throughout restarts.");
        toggle_useLastLocation.setSelected(Config.useLastLocation);
        toggle_useLastLocation.addActionListener(e -> {
            Config.useLastLocation = toggle_useLastLocation.isSelected();
            repaintForecast();

            // updateLastLocation will call save for us
            if (Config.useLastLocation) MittensMain.forecastWidget().updateLastLocation(MittensMain.forecastWidget());
            else Config.scheduleSave();
        });

        var toggle_resizeToFit = new JCheckBox("Auto Resize to Fit");
        toggle_resizeToFit.setToolTipText("If enabled, the widget will automatically resize itself to remove empty space. This won't apply when maximized or in fullscreen mode.");
        toggle_resizeToFit.setSelected(Config.resizeToFit);
        toggle_resizeToFit.addActionListener(e -> {
            Config.resizeToFit = toggle_resizeToFit.isSelected();
            Config.scheduleSave();
        });

        var slider_windowTransparency = new JSlider(10, 100, Config.windowTransparency);
        slider_windowTransparency.setEnabled(Config.undecorated);
        slider_windowTransparency.setMajorTickSpacing(10);
        slider_windowTransparency.setMinorTickSpacing(5);
        slider_windowTransparency.createStandardLabels(10, 10);
        slider_windowTransparency.setPaintTicks(true);
        slider_windowTransparency.setSnapToTicks(false);
        slider_windowTransparency.addChangeListener(e -> {
            Config.windowTransparency = Math.clamp(slider_windowTransparency.getValue(), 10, 100);
            MittensMain.forecastWidget().setWindowTransparency();
            Config.scheduleSave();
        });

        var toggle_undecorated = new JCheckBox("Enable Borderless Forecast");
        toggle_undecorated.setToolTipText("Remove the window borders from the forecast widget. Use the arrow keys to move the window around in this state.");
        toggle_undecorated.setSelected(Config.undecorated);
        toggle_undecorated.addActionListener(e -> {
            Config.undecorated = toggle_undecorated.isSelected();
            slider_windowTransparency.setEnabled(Config.undecorated);
            MittensMain.forecastWidget().toggleUndecorated();
            Config.scheduleSave();

        });

        var toggle_alwaysOnTop = new JCheckBox("Always On Top");
        toggle_alwaysOnTop.setToolTipText("Toggle the forecast widget staying on top of every other window.");
        toggle_alwaysOnTop.setSelected(Config.alwaysOnTop);
        toggle_alwaysOnTop.addActionListener(e -> {
            Config.alwaysOnTop = toggle_alwaysOnTop.isSelected();
            MittensMain.forecastWidget().setAlwaysOnTop(Config.alwaysOnTop);
            Config.scheduleSave();
        });

        Box box = Box.createVerticalBox();
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(createHeaderLabel("Mittens"));

        {
            Box subBox = Box.createHorizontalBox();
            subBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            subBox.add(label_quipFrequency);
            subBox.add(spinner_quipFrequency);
            box.add(subBox);
        }
        box.add(toggle_drawMittens);
        box.add(toggle_playSfx);
        box.add(toggle_slowerAnim);

        box.add(new JSeparator(JSeparator.HORIZONTAL));
        box.add(createHeaderLabel("General"));

        box.add(toggle_hideToTray);
        box.add(toggle_useLastLocation);
        box.add(toggle_resizeToFit);
        box.add(toggle_alwaysOnTop);
        box.add(toggle_undecorated);

        {
            Box subBox = Box.createHorizontalBox();
            subBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            subBox.add(new JLabel("Window Transparency: "));
            subBox.add(slider_windowTransparency);
            box.add(subBox);
        }

        // only know a windows solution to this - only add if windows?
        if (Util.isWindows()) {
            var toggle_launchOnStartUp = new JCheckBox("Launch On Start Up", Config.autoLaunch);
            toggle_launchOnStartUp.setToolTipText("If enabled, the widget will attempt to automatically start when Windows starts.");
            toggle_launchOnStartUp.addActionListener(e -> {
                if (toggle_launchOnStartUp.isSelected()) {
                    if (!Util.createAutoLaunchFile()) {
                        toggle_launchOnStartUp.setSelected(false);
                        showErrorDialog("Failed to make auto start up file");
                    } else {
                        Config.autoLaunch = true;
                        Config.scheduleSave();
                    }
                } else {
                    Config.autoLaunch = false;
                    Config.scheduleSave();
                    if (!Util.deleteAutoLaunchFile()) {
                        toggle_launchOnStartUp.setSelected(false);
                        showErrorDialog("Failed to delete auto start up file");
                    }
                }
            });
            box.add(toggle_launchOnStartUp);
        }

        return box;
    }

    void repaintForecast() {
        AsyncUtil.run(() -> MittensMain.forecastWidget().triggerRefresh());
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error - " + Constants.APP_NAME, JOptionPane.ERROR_MESSAGE);
    }

    public void showAboutDialog() {
        String message = """
                %s v%s
                By kittheuh
                
                Credits:
                - Open-Meteo - Weather data (https://open-meteo.com/)
                - olliedid_that - Mittens sprite & Weather icons
                - tallcakes - Forecast backgrounds & Weather overlays
                - lorelaidawitch - Various Mittens quips
                
                https://github.com/kittheuh/mittens-forecast-widget/""".formatted(Constants.APP_NAME, Constants.VERSION);

        JOptionPane.showMessageDialog(this, message, "About - " + Constants.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
    }
}

