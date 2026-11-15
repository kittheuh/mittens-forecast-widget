package io.github.kittheuh.mforecast.util;

import java.awt.image.BufferedImage;

public class Spritesheet {
    private final BufferedImage[][] sprites;
    private final BufferedImage sprite;
    private final int rows, columns;

    public Spritesheet(String filename, int rows, int columns) {
        this(Util.tryLoadImage(filename), rows, columns);
    }

    /**
     * create a spritesheet from an image
     * @param sprite image to parse, or a spritesheet to copy sprites from
     * @param rows number of rows the spritesheet should have, going down
     * @param columns number of columns the spritesheet should have, going right
     */
    public Spritesheet(BufferedImage sprite, int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.sprites = new BufferedImage[rows][columns];
        this.sprite = sprite;

        int widthPerSprite = width();
        int heightPerSprite = height();
        int currX = 0;
        int currY = 0;

        //Create an array, and store the split image files into it
        for (int i = 0; i < rows; i++) {
            this.sprites[i] = new BufferedImage[columns];

            for (int c = 0; c < columns; c++) {
                BufferedImage subSprite = sprite.getSubimage(currX, currY, widthPerSprite, heightPerSprite);
                this.sprites[i][c] = subSprite;
                currX += widthPerSprite;
            }

            currX = 0;
            currY += heightPerSprite;
        }
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    public int width() {
        return sprite.getWidth() / columns;
    }

    public int fullWidth() {
        return sprite.getWidth();
    }

    public int height() {
        return sprite.getHeight() / rows;
    }

    public int fullHeight() {
        return sprite.getHeight();
    }

    /**
     * get a specific frame of the spritesheet
     * @param row the row in question
     * @param column the column in question
     */
    public BufferedImage frame(int row, int column) {
        return this.sprites[row][column];
    }
}
