package io.github.kittheuh.mforecast.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

// ported from PolyBot#util/graphic/ImageGraphic.java
public sealed class ImageGraphic permits ScaledImageGraphic {
    public static final float[] ZERO_TO_ONE = new float[]{0, 1};

    public static BufferedImage createFallback() {
        return new ImageGraphic(64, 64, false)
                .color(Color.magenta)
                .rect(0, 0, 32, 32, true)
                .rect(32, 32, 32, 32, true)
                .cleanup()
                .image;
    }

    public static String randomString(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) ThreadLocalRandom.current().nextInt(' ', '~'+1);
        }
        return new String(chars);
    }

    /**
     * @param color color to derive from
     * @param aPercent a percent between 0-100
     * @return provided color with given transparency
     */
    public static Color transparentPercent(Color color, int aPercent) {
        float val = Math.clamp(aPercent / 100f, 0, 1f);
        aPercent = Math.round(255 * val) & 0xFF;
        return new Color(transparentRGB(color.getRGB(), aPercent), true);
    }

    /**
     * convenience method
     * @param color color to derive from
     * @param alpha an alpha value between 0-255
     * @return provided color with given transparency
     */
    public static Color transparent(Color color, int alpha) {
        return new Color(transparentRGB(color.getRGB(), alpha), true);
    }

    /**
     * @param rgb rgb color value to derive from
     * @param alpha an alpha value between 0-255
     * @return argb value with given alpha
     */
    public static int transparentRGB(int rgb, int alpha) {
        alpha = Math.clamp(alpha, 0, 255) & 0xFF;
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface NotNull {}
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface Nullable {}

    public enum Sampling {
        BILINEAR(RenderingHints.VALUE_INTERPOLATION_BILINEAR),
        BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC),
        NEAREST_NEIGHBOR(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR),
        ;

        public final Object hintValue;

        Sampling(Object hintValue) {
            this.hintValue = hintValue;
        }
    }
    public enum Justify { LEFT, CENTER, RIGHT }
    public enum Align { TOP, MIDDLE, BOTTOM }
    public enum Direction { NORTH, SOUTH, WEST, EAST }
    public enum TriState {
        TRUE(true),
        NOT_SET(null),
        FALSE(false);

        private final Boolean value;
        TriState(Boolean b) {
            value = b;
        }

        public static TriState fromBoolean(boolean b) {
            return b ? TRUE : FALSE;
        }

        public boolean getValueBool() throws UnsupportedOperationException {
            if (value == null) throw new UnsupportedOperationException("NOT_SET not mappable");
            return value;
        }

        public boolean getOrElse(boolean b) {
            if (value == null) return b;
            return value;
        }

        @Nullable
        public Boolean getValue() {
            return value;
        }
    }
    private static final RenderingHints hints = new RenderingHints(null);

    private static DebugLogger LOGGER;
    private static TriState defaultDebugMode = TriState.NOT_SET;

    static {
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        LOGGER = new DefaultJavaLogger();
    }

    public static void defaultDebugMode(@NotNull TriState state) {
        defaultDebugMode = Objects.requireNonNull(state);
    }

    public static void defaultDebugLogger(@NotNull DebugLogger logger) {
        LOGGER = Objects.requireNonNull(logger);
    }

    protected @NotNull TriState debugMode = defaultDebugMode;
    protected boolean cleanedUp = false, useOldStrPos = true;
    protected @NotNull final BufferedImage image;
    protected @NotNull final Graphics2D g2d;

    protected @NotNull Sampling sampling = Sampling.BICUBIC;
    protected @NotNull Justify justification = Justify.LEFT;
    protected @NotNull Align alignment = Align.TOP;

    public ImageGraphic(BufferedImage source) {
        image = new BufferedImage(source.getWidth(), source.getHeight(),
                source.getTransparency() == Transparency.TRANSLUCENT ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        g2d = image.createGraphics();
        g2d.setRenderingHints(hints);
        image(source, 0, 0);
    }

    /**
     * this will copy the image from the source and its text variables as well
     * this leaves the source untouched
     * @param source source to copy from
     */
    public ImageGraphic(ImageGraphic source) {
        image = new BufferedImage(source.width(), source.height(),
                source.image.getTransparency() != Transparency.OPAQUE ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        g2d = image.createGraphics();
        g2d.setRenderingHints(hints);
        image(source.image, 0, 0);
        sampling(source.sampling);
        justification = source.justification;
        alignment = source.alignment;
        debugMode = source.debugMode;
        useOldStrPos = source.useOldStrPos;
        cleanedUp = false;
    }

    public ImageGraphic(Dimension dimension, boolean transparent) {
        this(dimension.width, dimension.height, transparent);
    }

    public ImageGraphic(int width, int height, boolean transparent) {
        image = new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        g2d = image.createGraphics();
        g2d.setRenderingHints(hints);
    }

    public ImageGraphic nearestNeighbor() {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return sampling(Sampling.NEAREST_NEIGHBOR);
    }

    public ImageGraphic bilinear() {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return sampling(Sampling.BILINEAR);
    }

    public ImageGraphic bicubic() {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return sampling(Sampling.BICUBIC);
    }

    /**
     * true -> pure
     * not_set -> default
     * false -> normalize
     * @param state hint state to set
     * @return this instance for chaining
     */
    public ImageGraphic strokeHint(TriState state) {
        Object value = switch (state) {
            case TRUE -> RenderingHints.VALUE_STROKE_PURE;
            case NOT_SET -> RenderingHints.VALUE_STROKE_DEFAULT;
            case FALSE -> RenderingHints.VALUE_STROKE_NORMALIZE;
        };
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, value);
        return this;
    }

    /**
     * @return true if this ImageGraphic instance is using the old logic for calculating text positions
     */
    public boolean useOldStrPos() {
        return useOldStrPos;
    }

    /**
     * @param useOldStrPos toggle using
     * @return this instance for chaining
     */
    public ImageGraphic useOldStrPos(boolean useOldStrPos) {
        this.useOldStrPos = useOldStrPos;
        return this;
    }

    public ImageGraphic rect(int xpos, int ypos, int width, int height, boolean fill) {
        if (fill) g2d.fillRect(xpos, ypos, width, height);
        else g2d.drawRect(xpos, ypos, width, height);
        return this;
    }

    public ImageGraphic circle(int xpos, int ypos, int width, int height, boolean fill) {
        if (fill) g2d.fillOval(xpos, ypos, width, height);
        else g2d.drawOval(xpos, ypos, width, height);
        return this;
    }

    public ImageGraphic triangle(int xpos, int ypos, int width, int height, @NotNull Direction dir, boolean fill) {
        int[] xp, yp;

        switch (dir) {
            case NORTH -> {
                xp = new int[]{xpos, xpos + (width /2), xpos + width};
                yp = new int[]{ypos + height, ypos, ypos + height};
            } case SOUTH -> {
                xp = new int[]{xpos, xpos + (width /2), xpos + width};
                yp = new int[]{ypos, ypos + height, ypos};
            } case EAST -> {
                xp = new int[]{xpos, xpos + width, xpos};
                yp = new int[]{ypos, ypos + (height /2), ypos + height};
            } case WEST -> {
                xp = new int[]{xpos + width, xpos, xpos + width};
                yp = new int[]{ypos, ypos + (height /2), ypos + height};
            } default -> throw new IllegalStateException("Unexpected value: " + dir);
        }

        if (fill) g2d.fillPolygon(xp, yp, xp.length);
        else g2d.drawPolygon(xp, yp, xp.length);

        debugPoly(xp, yp).debug(xpos, ypos, width, height);
        return this;
    }

    public ImageGraphic roundRect(int xpos, int ypos, int width, int height, int arc, boolean fill) {
        if (fill) g2d.fillRoundRect(xpos, ypos, width, height, arc, arc);
        else g2d.drawRoundRect(xpos, ypos, width, height, arc, arc);
        return this;
    }

    public ImageGraphic roundRect(int xpos, int ypos, int size, int arc, boolean fill) {
        if (fill) g2d.fillRoundRect(xpos, ypos, size, size, arc, arc);
        else g2d.drawRoundRect(xpos, ypos, size, size, arc, arc);
        return this;
    }

    public ImageGraphic rotate(int xO, int yO, float degrees) {
        debugDot(xO, yO, 5, Color.blue);
        g2d.rotate(Math.toRadians(degrees), xO, yO);
        return this;
    }

    public ImageGraphic composite(Composite composite) {
        g2d.setComposite(composite);
        return this;
    }

    public Composite composite() {
        return g2d.getComposite();
    }

    public ImageGraphic circleClip(int xpos, int ypos, int width, int height) {
        Ellipse2D circle = new Ellipse2D.Float(xpos, ypos, width, height);
        g2d.setClip(circle);
        return this;
    }

    public ImageGraphic rectClip(int xpos, int ypos, int width, int height) {
        Rectangle r = new Rectangle(xpos, ypos, width, height);
        g2d.setClip(r);
        return this;
    }

    public ImageGraphic noClip() {
        g2d.setClip(null);
        return this;
    }

    public ImageGraphic translate(int x, int y) {
        g2d.translate(x, y);
        return this;
    }

    public ImageGraphic translate(float x, float y) {
        g2d.translate(x, y);
        return this;
    }

    public ImageGraphic colorFilter(Color color, float opacity, Consumer<ImageGraphic> consumer) {
        if (color == null) throw new IllegalArgumentException("color null");
        if (opacity > 1 || opacity < 0) throw new IllegalArgumentException("opacity out of range");

        if (consumer != null) consumer.accept(this);
        if (opacity == 0) return this;

        Composite composite = g2d.getComposite();
        Color g2dColor = g2d.getColor();

        g2d.setComposite(AlphaComposite.SrcAtop.derive(opacity));
        g2d.setColor(color);

        rect(0, 0, width(), height(), true);

        g2d.setComposite(composite);
        g2d.setColor(g2dColor);
        return this;
    }

    public ImageGraphic vignette(int xpos, int ypos, int size, int intensity) {
        intensity = Math.clamp(intensity, 0, 255);

        RadialGradientPaint radial = new RadialGradientPaint(xpos, ypos, size / 2f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(0, true), transparent(Color.black, intensity)}
        );

        g2d.setPaint(radial);
        fill();
        return this;
    }


    public ImageGraphic image(BufferedImage image, int xpos, int ypos) {
        g2d.drawImage(image, xpos, ypos, null);
        debug(xpos, ypos, image.getWidth(), image.getHeight());
        return this;
    }

    public ImageGraphic image(BufferedImage image, int xpos, int ypos, int width) {
        float fWidth = (float) width;
        float scale = Math.min(fWidth / image.getWidth(), fWidth / image.getHeight());

        imageScaled(image, xpos, ypos, scale);
        return this;
    }

    public ImageGraphic image(BufferedImage image, int xpos, int ypos, int width, int height) {
        g2d.drawImage(image, xpos, ypos, width, height, null);
        debug(xpos, ypos, width, height);
        return this;
    }

    public ImageGraphic imageRotated(BufferedImage image, int xpos, int ypos, float rot) {
        int wDiv = image.getWidth()/2, hDiv = image.getHeight()/2;

        xpos += wDiv;
        ypos += hDiv;

        AffineTransform transform = g2d.getTransform();
        g2d.rotate(Math.toRadians(rot), xpos, ypos);
        image(image, xpos-wDiv, ypos-hDiv);
        g2d.setTransform(transform);

        return this;
    }

    public ImageGraphic imageScaled(BufferedImage image, int xpos, int ypos, float scale) {
        g2d.drawImage(image, xpos, ypos, Math.round(image.getWidth()*scale), Math.round(image.getHeight()*scale), null);
        debug(xpos, ypos, Math.round(image.getWidth()*scale), Math.round(image.getHeight()*scale));
        return this;
    }

    public ImageGraphic imageCentered(BufferedImage image, int x, int y, int width, int height) {
        if (image == null) {
            debug(x, y, width, height);
            return this;
        }

        float scale = Math.min((float) width / image.getWidth(), (float) height / image.getHeight());
        float xOffset = (width/2f) - ((image.getWidth() * scale) / 2);
        float yOffset = (height/2f) - ((image.getHeight() * scale) / 2);

        this.imageScaled(image, x + Math.round(xOffset), y + Math.round(yOffset), scale).
                debugCross(x, y, width, height);
        return this;
    }

    public ImageGraphic tile(BufferedImage image, int xpos, int ypos, int width, int height, float scale) {
        if (width == 0 || height == 0) return this;
        int scaledWidth = Math.round(image.getWidth() * scale), scaledHeight = Math.round(image.getHeight() * scale);

        int hCount = width / scaledWidth;
        int vCount = height / scaledHeight;
        // using img width we calc the amount needed to fit within this width

        /*
        set clip area to the bounds

        for loop based off number of iterations needed
        0 = we run thru it once
         */
        Rectangle clip = new Rectangle(xpos, ypos, width, height);
        g2d.setClip(clip);

        for (int i = 0; i < vCount+1; i++) {
            for (int j = 0; j < hCount+1; j++) {
                int x = xpos + (j * scaledWidth);
                int y = ypos + (i * scaledHeight);

                g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null);
            }
        }

        g2d.setClip(null);
        return this;
    }

    public ImageGraphic str(String str, int xpos, int ypos) {
        TextLayout layout = new TextLayout(str, font(), g2d.getFontRenderContext());
        Rectangle bounds = layout.getBounds().getBounds();

        int xOff = switch (justification) {
            case LEFT -> 0;
            case CENTER -> useOldStrPos ? metrics().stringWidth(str)/2 : bounds.width/2;
            case RIGHT -> useOldStrPos ? metrics().stringWidth(str) : bounds.width;
        };

        int yOff = switch (alignment) {
            case TOP -> 0;
            case MIDDLE -> useOldStrPos ? metrics().getHeight()/2 : bounds.height/2;
            case BOTTOM -> useOldStrPos ? metrics().getHeight() : bounds.height;
        };

        if (isDebug()) { // legacy bounding box
            int obw = metrics().stringWidth(str), obh = metrics().getHeight();

            int oxOff = switch (justification) {
                case LEFT -> 0;
                case CENTER -> obw/2;
                case RIGHT -> obw;
            };

            int oyOff = switch (alignment) {
                case TOP -> 0;
                case MIDDLE -> obh/2;
                case BOTTOM -> obh;
            };

            Paint paint = g2d.getPaint();
            Stroke s = stroke();
            Color c = color();
            color(Color.blue);
            stroke(1);

            if (useOldStrPos) debug(xpos - xOff, ypos-metrics().getHeight() + yOff, metrics().stringWidth(str), metrics().getHeight());
            else rect(xpos - oxOff, ypos - obh + oyOff, obw, obh, false);

            g2d.setColor(c);
            g2d.setPaint(paint);
            g2d.setStroke(s);
        }

        int strX = xpos - xOff, strY = ypos + yOff;

        layout.draw(g2d, strX, strY);
        translate(strX, strY)
                .debugDot(-xOff, yOff, 7)
                .debugDot(0, 0, 5, Color.cyan);

        if (isDebug()) debug(bounds);

        translate(-strX, -strY);
        return this;
    }

    public ImageGraphic scaleToFitStr(String str, int minSize, int adjRate, int maxWidth) {
        while (g2d.getFontMetrics().stringWidth(str) > maxWidth) {
            int size = g2d.getFont().getSize() - adjRate;
            if (size < minSize) break;

            g2d.setFont(g2d.getFont().deriveFont((float) size));
        }
        return this;
    }

    public ImageGraphic line(int sx, int sy, int ex, int ey) {
        g2d.drawLine(sx, sy, ex, ey);
        return this;
    }

    public ImageGraphic fill() {
        return rect(0, 0, width(), height(), true);
    }

    public ImageGraphic clear() {
        WritableRaster raster = image.getRaster();

        int[] pixels = null;
        for (int y = 0; y < height(); y++) {
            pixels = raster.getPixels(0, y, width(), 1, pixels);

            Arrays.fill(pixels, 0);
            raster.setPixels(0, y, width(), 1, pixels);
        }
        return this;
    }

    public Stroke stroke() {
        return g2d.getStroke();
    }

    public ImageGraphic stroke(float w) {
        g2d.setStroke(new BasicStroke(w));
        return this;
    }

    public ImageGraphic strokeRound(float w) {
        g2d.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        return this;
    }

    public Color color() {
        return g2d.getColor();
    }

    public ImageGraphic color(Color color) {
        g2d.setColor(color);
        return this;
    }

    public ImageGraphic color(int color) {
        g2d.setColor(new Color(color, true));
        return this;
    }

    public Font font() {
        return g2d.getFont();
    }

    public ImageGraphic font(Font font) {
        g2d.setFont(font);
        return this;
    }

    public ImageGraphic font(Font font, float size) {
        g2d.setFont(font.deriveFont(size));
        return this;
    }

    public FontMetrics metrics() {
        return g2d.getFontMetrics();
    }

    public FontMetrics metrics(Font font) {
        return g2d.getFontMetrics(font);
    }

    public FontMetrics metrics(Font font, int size) {
        return g2d.getFontMetrics(font.deriveFont((float) size));
    }

    public Sampling sampling() {
        return sampling;
    }

    public ImageGraphic sampling(Sampling sampling) {
        this.sampling = sampling;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, sampling.hintValue);
        return this;
    }

    public Justify justification() {
        return justification;
    }

    public ImageGraphic justification(Justify justification) {
        this.justification = justification;
        return this;
    }

    public Align alignment() {
        return alignment;
    }

    public ImageGraphic alignment(Align alignment) {
        this.alignment = alignment;
        return this;
    }

    public ImageGraphic gradientLine(Color c1, Color c2, int sx, int sy, int ex, int ey) {
        if (sx == ex && sy == ey) return this;

        g2d.setPaint(new LinearGradientPaint(new Point(sx, sy), new Point(ex, ey),
                ZERO_TO_ONE, new Color[]{c1,c2}));
        return this;
    }

    public ImageGraphic paint(Paint paint) {
        g2d.setPaint(paint);
        return this;
    }

    public ImageGraphic cleanup() {
        cleanedUp = true;
        g2d.dispose();
        return this;
    }

    public int width() {
        return image.getWidth();
    }

    public int height() {
        return image.getHeight();
    }

    public BufferedImage getImage() {
        return image;
    }

    public Graphics2D getGraphics2D() {
        return g2d;
    }

    public Dimension getDimensions() {
        return new Dimension(width(), height());
    }

    private void logNoCleanup(String name) {
        if (cleanedUp) return;
        LOGGER.warn("Cleanup was not called before upload or file call ({})", name);
    }

    public boolean createAndOpen(String prefix) {
        logNoCleanup(prefix);

        Path path = createTempFile(prefix);

        try {
            Desktop.getDesktop().open(path.toFile());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Path createTempFile(String prefix) {
        logNoCleanup(prefix);

        Path tempFile;
        try {
            tempFile = Files.createTempFile(prefix, ".png");
        } catch (IOException e) {
            tempFile = Paths.get("./%s-%s.png".formatted(prefix, randomString(15)));
        }

        createFile(tempFile);
        return tempFile;
    }

    public boolean createFile(Path path) {
        logNoCleanup(path.toString());

        try (OutputStream out = Files.newOutputStream(path)) {
            ImageIO.write(image, "png", out);

            if (LOGGER.isDebugEnabled()) {
                try {
                    long size = Files.size(path);
                    LOGGER.debug("ImageGraphic-FILE, \"{}\" w/size={} ({}kb)", path.getFileName(), size, size / 1024L);
                } catch (IOException e) {
                    LOGGER.debug("Failed to read file size for file image graphic logging");
                }
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to write to file", e);
        }

        return false;
    }

    public ImageGraphic debug(@NotNull TriState state) {
        debugMode = state;
        return this;
    }

    @NotNull
    public TriState debug() {
        return debugMode;
    }

    public boolean isDebug() {
        return debugMode.getOrElse(false);
    }

    public ImageGraphic debug(@NotNull Rectangle rectangle) {
        return debug(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public ImageGraphic debug(int xpos, int ypos, int width, int height) {
        if (!isDebug()) return this;

        Paint paint = g2d.getPaint();
        Stroke s = stroke();
        color(Color.red);
        stroke(1);
        rect(xpos, ypos, width, height, false);

        color(Color.green);
        line(xpos + width/2, ypos-5, xpos + width/2, ypos + 5);
        line(xpos + width/2, ypos+height-5, xpos + width/2, ypos+height+5);

        line(xpos-5, ypos+height/2, xpos+5, ypos+height/2);
        line(xpos+width-5, ypos+height/2, xpos+width+5, ypos+height/2);

        g2d.setPaint(paint);
        g2d.setStroke(s);
        return this;
    }

    public ImageGraphic debugCross(int sx, int sy, int w, int h) {
        if (!isDebug()) return this;
        Color c = color();
        Stroke s = stroke();

        color(Color.yellow)
                .stroke(1)
                .line(sx, sy, sx+w, sy+h)
                .line(sx, sy+h, sx+w, sy)
                .color(c);

        g2d.setStroke(s);
        return this;
    }

    public ImageGraphic debugLine(int sx, int sy, int ex, int ey) {
        if (!isDebug()) return this;
        Color c = color();
        Stroke s = stroke();

        color(Color.yellow)
                .stroke(1)
                .line(sx, sy, ex, ey)
                .color(c);

        g2d.setStroke(s);
        return this;
    }

    public ImageGraphic debugPoly(int[] xp, int[] yp) {
        if (!isDebug()) return this;
        Color c = color();
        Stroke s = stroke();

        color(Color.yellow)
                .stroke(1);

        g2d.drawPolyline(xp, yp, xp.length);
        color(c);

        g2d.setStroke(s);
        return this;
    }

    public ImageGraphic debugDot(int x, int y, int size) {
        return debugDot(x, y, size, null);
    }


    public ImageGraphic debugDot(int x, int y, int size, @Nullable Color d) {
        if (!isDebug()) return this;
        Color c = color();

        color(d == null ? Color.magenta : d)
                .circle(x - (size/2), y - (size/2), size, size, true)
                .color(c);
        return this;
    }

    public interface DebugLogger {
        boolean isDebugEnabled();

        void error(String msg);
        void error(String msg, Throwable e);
        void error(String msg, Object o1);
        void error(String msg, Object o1, Object o2);
        void error(String msg, Object... objects);

        void warn(String msg);
        void warn(String msg, Throwable e);
        void warn(String msg, Object o1);
        void warn(String msg, Object o1, Object o2);
        void warn(String msg, Object... objects);

        void info(String msg);
        void info(String msg, Throwable e);
        void info(String msg, Object o1);
        void info(String msg, Object o1, Object o2);
        void info(String msg, Object... objects);

        void debug(String msg);
        void debug(String msg, Throwable e);
        void debug(String msg, Object o1);
        void debug(String msg, Object o1, Object o2);
        void debug(String msg, Object... objects);
    }

    public static class DefaultJavaLogger implements DebugLogger {
        private static final Logger LOGGER = Logger.getLogger(ImageGraphic.class.getName());

        @Override
        public boolean isDebugEnabled() {
            return Boolean.getBoolean("polybot.imageGraphic.devInst");
        }

        private String convert(String s) {
            return s.replace("{}", "%s");
        }

        @Override
        public void error(String msg) {
            LOGGER.severe(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            LOGGER.log(Level.SEVERE, msg, e);
        }

        @Override
        public void error(String msg, Object o1) {
            LOGGER.severe(convert(msg).formatted(o1));
        }

        @Override
        public void error(String msg, Object o1, Object o2) {
            LOGGER.severe(convert(msg).formatted(o1, o2));
        }

        @Override
        public void error(String msg, Object... objects) {
            LOGGER.severe(convert(msg).formatted(objects));
        }

        @Override
        public void warn(String msg) {
            LOGGER.warning(msg);
        }

        @Override
        public void warn(String msg, Throwable e) {
            LOGGER.log(Level.WARNING, msg, e);
        }

        @Override
        public void warn(String msg, Object o1) {
            LOGGER.warning(convert(msg).formatted(o1));
        }

        @Override
        public void warn(String msg, Object o1, Object o2) {
            LOGGER.warning(convert(msg).formatted(o1, o2));
        }

        @Override
        public void warn(String msg, Object... objects) {
            LOGGER.warning(convert(msg).formatted(objects));
        }

        @Override
        public void info(String msg) {
            LOGGER.info(msg);
        }

        @Override
        public void info(String msg, Throwable e) {
            LOGGER.log(Level.INFO, msg, e);
        }

        @Override
        public void info(String msg, Object o1) {
            LOGGER.info(convert(msg).formatted(o1));
        }

        @Override
        public void info(String msg, Object o1, Object o2) {
            LOGGER.info(convert(msg).formatted(o1, o2));
        }

        @Override
        public void info(String msg, Object... objects) {
            LOGGER.info(convert(msg).formatted(objects));
        }

        @Override
        public void debug(String msg) {
            LOGGER.fine(msg);
        }

        @Override
        public void debug(String msg, Throwable e) {
            LOGGER.log(Level.FINE, msg, e);
        }

        @Override
        public void debug(String msg, Object o1) {
            LOGGER.fine(convert(msg).formatted(o1));
        }

        @Override
        public void debug(String msg, Object o1, Object o2) {
            LOGGER.fine(convert(msg).formatted(o1, o2));
        }

        @Override
        public void debug(String msg, Object... objects) {
            LOGGER.fine(convert(msg).formatted(objects));
        }
    }
}
