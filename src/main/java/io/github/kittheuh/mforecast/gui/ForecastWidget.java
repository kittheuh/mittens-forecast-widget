package io.github.kittheuh.mforecast.gui;

import io.github.kittheuh.mforecast.*;
import io.github.kittheuh.mforecast.util.AsyncUtil;
import io.github.kittheuh.mforecast.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.TimeUnit;

public class ForecastWidget extends JFrame {

    private final ForecastCanvas canvas;

    public ForecastWidget() {
        super(Constants.APP_NAME);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setIconImage(Constants.ICON);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (!Config.useLastLocation) return;
                updateLastLocation(e.getComponent());
            }

            @Override
            public void componentResized(ComponentEvent e) {
                AsyncUtil.scheduleDelayable("mfw:resize", ForecastWidget.this::resizeRelative, 1, TimeUnit.SECONDS);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (Config.closeNoticeShown) return;
                if (!Config.hideToTray) return;
                Config.closeNoticeShown = true;

                JOptionPane.showMessageDialog(canvas,
                        Constants.APP_NAME + " will now run in the background.\n\nTo quit the app entirely, use the \"Quit\" buttons on the tray icon or on the forecast widget right click menu.\nAlternatively, disable the \"Hide to Tray\" option to disable this behavior.",
                        Constants.APP_NAME,
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        addKeyListener(new KeyAdapter() {
            // 0-3 = arrow keys
            private final boolean[] pressedKeys = new boolean[4];

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    GraphicsDevice device = getGraphicsConfiguration().getDevice();
                    if (!device.isFullScreenSupported()) return;


                    if (device.getFullScreenWindow() == ForecastWidget.this) {
                        device.setFullScreenWindow(null);
                        toggleUndecorated();
                    } else {
                        dispose();
                        setUndecorated(true);
                        setVisible(true);
                        device.setFullScreenWindow(ForecastWidget.this);
                    }
                    return;
                }

                if (!isUndecorated()) return;
                if (getGraphicsConfiguration().getDevice().getFullScreenWindow() == ForecastWidget.this) return;

                int index = e.getKeyCode() - 37;
                if (index < 0 || index >= 4) return;

                pressedKeys[index] = true;
                int xChange = 0, yChange = 0;

                if (pressedKeys[KeyEvent.VK_LEFT - 37]) xChange = -1;
                if (pressedKeys[KeyEvent.VK_RIGHT - 37]) xChange = 1;
                if (pressedKeys[KeyEvent.VK_UP - 37]) yChange = -1;
                if (pressedKeys[KeyEvent.VK_DOWN - 37]) yChange = 1;
                if (xChange == 0 && yChange == 0) return;

                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK) {
                    xChange *= 8;
                    yChange *= 8;
                }

                Config.lastLocX = getX()+xChange;
                Config.lastLocY = getY()+yChange;
                if (Config.useLastLocation) Config.scheduleSave();
                setLocation(Config.lastLocX, Config.lastLocY);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int index = e.getKeyCode() - 37;
                if (index < 0 || index >= 4) return;

                pressedKeys[index] = false;
            }
        });

        canvas = new ForecastCanvas(this);
        add(canvas);
        pack();

        boolean locationSet = false;
        if (Config.useLastLocation) {
            Rectangle screenSafeBounds = Util.getScreenSafeBounds();
            locationSet = screenSafeBounds.contains(Config.lastLocX, Config.lastLocY);
        }
        if (locationSet) setLocation(Config.lastLocX, Config.lastLocY);
        else setLocationRelativeTo(null);

        setAlwaysOnTop(Config.alwaysOnTop);
        toggleUndecorated();
    }

    public void triggerRefresh() {
        canvas.refreshForecast();
        canvas.repaint();
    }

    public ForecastCanvas forecastCanvas() {
        return canvas;
    }

    public void updateLastLocation(Component c) {
        int x = c.getX();
        int y = c.getY();
        if (Config.lastLocX == x && Config.lastLocY == y)
            return;

        Config.lastLocX = x;
        Config.lastLocY = y;
        Config.scheduleSave();
    }

    public void toggleUndecorated() {
        if (isDisplayable()) dispose();

        if (Config.undecorated) {
            setUndecorated(true);
            setWindowTransparency();
            setBackground(Constants.TRANSPARENT);
            canvas.setBackground(Constants.TRANSPARENT);
        } else {
            setOpacity(1);
            setBackground(Constants.FORECAST_BACKGROUND);
            canvas.setBackground(Constants.FORECAST_BACKGROUND);
            setUndecorated(false);
        }

        setVisible(true);
        toFront();
    }

    public void setWindowTransparency() {
        if (!isUndecorated()) return;
        setOpacity(Config.windowTransparency / 100f);
    }

    public void resizeRelative() {
        if (getExtendedState() == JFrame.MAXIMIZED_BOTH) return;
        if (getGraphicsConfiguration().getDevice().getFullScreenWindow() == ForecastWidget.this) return;

        SwingUtilities.invokeLater(() -> {
            int preserve = Math.min(canvas.getWidth(), canvas.getHeight());
            if (preserve == canvas.getWidth()) {
                // shrink height to fit the og size

                double scaling = canvas.getWidth() / 810d;
                canvas.setPreferredSize(new Dimension(preserve, (int) Math.round(496 * scaling)));
            } else {
                // shrink width to fit the og size

                double scaling = canvas.getHeight() / 496d;
                canvas.setPreferredSize(new Dimension((int) Math.round(810 * scaling), preserve));
            }
            pack();
            if (!Config.useLastLocation) return;
            Config.lastLocW = canvas.getWidth();
            Config.lastLocH = canvas.getHeight();
            Config.scheduleSave();
        });
    }
}
