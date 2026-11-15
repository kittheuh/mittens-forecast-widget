package io.github.kittheuh.mforecast.util;

import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

public final class ScaledImageGraphic extends ImageGraphic {

    private final int targetWidth, targetHeight;
    private final double targetScale;

    public ScaledImageGraphic(BufferedImage source, double targetScale) {
        this(source.getWidth(), source.getHeight(), source.getTransparency() == Transparency.TRANSLUCENT, targetScale);
        image(source, 0, 0);
    }

    public ScaledImageGraphic(Dimension dimension, boolean transparent, double targetScale) {
        this(dimension.width, dimension.height, transparent, targetScale);
    }

    public ScaledImageGraphic(int targetWidth, int targetHeight, boolean transparent, double targetScale) {
        super((int) Math.round(targetWidth * targetScale), (int) Math.round(targetHeight * targetScale), transparent);
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.targetScale = targetScale;
        g2d.scale(targetScale, targetScale);
    }

    @Override
    public int width() {
        return targetWidth;
    }

    @Override
    public int height() {
        return targetHeight;
    }

    public int trueWidth() {
        return image.getWidth();
    }

    public int trueHeight() {
        return image.getHeight();
    }

    public double scale() {
        return targetScale;
    }
}
