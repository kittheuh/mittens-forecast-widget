package io.github.kittheuh.mforecast.gui;

import io.github.kittheuh.mforecast.Config;
import io.github.kittheuh.mforecast.Constants;
import io.github.kittheuh.mforecast.MittensMain;
import io.github.kittheuh.mforecast.api.GeocodingResult;
import io.github.kittheuh.mforecast.util.AsyncUtil;
import io.github.kittheuh.mforecast.util.GeocodingUtil;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LocationPopup extends JDialog {
    private final JComboBox<GeocodingResult> locationCityComboBox;
    private final JLabel loadingLabel = new JLabel();
    private final JButton saveButton = new JButton("Save Changes");
    private final JTextField searchCityField;

    private List<GeocodingResult> results;

    public LocationPopup(boolean openForecastOnClose) {
        setTitle("Update Forecast Location");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (openForecastOnClose) {
                    JOptionPane.showMessageDialog(MittensMain.settingsFrame(), "You can always change the forecast location again by opening the settings!\nThere are two ways to open the settings:\n1. Right click on the Forecast Widget, then click on 'Settings'\n2. Right click on the tray icon, and click on 'Open Settings'.",
                            Constants.APP_NAME,
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    MittensMain.forecastWidget().setVisible(true);
                }
            }
        });

        var cancelButton = new JButton("Close Without Saving");
        cancelButton.addActionListener(e -> {
            dispose();
        });

        saveButton.setEnabled(false);
        searchCityField = new JTextField(Config.locationCityName, 30);
        searchCityField.addActionListener(this::searchForCity);

        locationCityComboBox = new JComboBox<>();
        locationCityComboBox.setMaximumRowCount(10);
        locationCityComboBox.setEnabled(false);
        locationCityComboBox.setPrototypeDisplayValue(GeocodingUtil.DEFAULT_RESULT);
        locationCityComboBox.addItemListener(e -> {
            saveButton.setEnabled(true);
            cancelButton.setText("Close Without Saving");
        });

        var button_searchCity = new JButton("Search");
        button_searchCity.addActionListener(this::searchForCity);

        var jp = new JPanel();
        jp.add(new JLabel("Search for a city:"));
        jp.add(searchCityField);
        jp.add(button_searchCity);

        var lJp = new JPanel();
        lJp.add(loadingLabel);

        var jp2 = new JPanel();
        jp2.add(new JLabel("Select a city:"));
        jp2.add(locationCityComboBox);

        saveButton.addActionListener(e -> {
            GeocodingResult result = results.get(locationCityComboBox.getSelectedIndex());

            Config.locationCityName = result.cityName();
            Config.locationLatitude = result.latitude();
            Config.locationLongitude = result.longitude();
            Config.locationSet = true;
            Config.scheduleSave();
            saveButton.setEnabled(false);
            cancelButton.setText("Close");

            if (MittensMain.settingsFrameVisible()) MittensMain.settingsFrame().updateLocationDisplay();
            if (MittensMain.forecastWidgetVisible()) {
                // TODO: refresh forecast & canvas!
//                MittensMain.forecastWidget().forecastCanvas().clearForecastCache();
                MittensMain.settingsFrame().repaintForecast();
            }
            if (openForecastOnClose) dispose();
        });

        var jp3 = new JPanel();
        jp3.add(cancelButton);
        jp3.add(saveButton);

        var box = Box.createVerticalBox();
        box.add(jp, BorderLayout.NORTH);
        box.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.CENTER);
        box.add(lJp, BorderLayout.WEST);
        box.add(jp2, BorderLayout.SOUTH);
        box.add(jp3);
        add(box);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        toFront();
    }

    private void searchForCity(ActionEvent e) {
        saveButton.setEnabled(false);
        locationCityComboBox.setEnabled(false);
        loadingLabel.setText("Searching...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        AsyncUtil.timeoutRun(() -> {
            long start = System.currentTimeMillis();
            results = GeocodingUtil.geocoding(searchCityField.getText());
            long end = System.currentTimeMillis();
            loadingLabel.setText("Done. %d results (%sms)".formatted(results.size(), end - start));
            locationCityComboBox.setEnabled(true);
            setCursor(null);

            locationCityComboBox.removeAllItems();
            for (GeocodingResult result : results) {
                locationCityComboBox.addItem(result);
            }

            SwingUtilities.invokeLater(() -> {
                pack();
                setLocationRelativeTo(null);
            });
        }, 30, TimeUnit.SECONDS, () -> {
            setCursor(null);
            loadingLabel.setText("Failed. Search timed out!");
            if (results.isEmpty()) locationCityComboBox.setEnabled(false);
        });
    }
}
