package com.telenav.osv.utils;

import androidx.annotation.NonNull;

/**
 * Class for describing width and height dimensions in pixels.
 */
public class Size implements Comparable<Size> {

    /**
     * The value to divide the product of the width and height values.
     */
    private static final float MEGA_PIXELS_CONVERSION_VALUE = 1000000f;

    /**
     * The width value in pixels for the current size.
     */
    private int width;

    /**
     * The height value in pixels for the current size.
     */
    private int height;

    /**
     * Default constructor for the current class.
     * @param width The width of the size in pixels.
     * @param height The height of the size in pixels.
     */
    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof Size) {
            Size size = (Size) o;
            return width == size.width && height == size.height;
        }
        return false;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

    @Override
    public int compareTo(@NonNull Size another) {
        return width * height - another.width * another.height;
    }

    /**
     * Swaps the width with the height value.
     */
    public void swapValues() {
        width = width + height;
        height = width - height;
        width = width - height;
    }

    /**
     * @return the width dimension given by {@link #width} field.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height dimension given by {@link #height} field.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the mega pixels for current width and height values rounded up to an {@code int}.
     */
    public int getRoundedMegaPixels() {
        return Math.round((width * height) / MEGA_PIXELS_CONVERSION_VALUE);
    }

    /**
     * @return the raw value of mega pixels for the current width and height.
     */
    public double getMegaPixelsWithPrecision() {
        return (width * height) / MEGA_PIXELS_CONVERSION_VALUE;
    }
}