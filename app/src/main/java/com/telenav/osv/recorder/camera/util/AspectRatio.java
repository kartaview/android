package com.telenav.osv.recorder.camera.util;

import androidx.annotation.NonNull;

import com.telenav.osv.utils.Size;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class for describing proportional relationship between width and height.
 */
public class AspectRatio implements Comparable<AspectRatio> {

    /**
     * List containing all the supported aspect ratios.
     */
    private final static List<AspectRatio> aspectRatios = new CopyOnWriteArrayList<>();

    /**
     * The aspect ratio value for the width dimension.
     */
    private final int x;

    /**
     * The aspect ratio value for the height dimension.
     */
    private final int y;

    /**
     * Default constructor for the current class.
     *
     * @param x the aspect ratio value for the width dimension.
     * @param y the aspect ratio value for the height dimension.
     */
    private AspectRatio(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns an instance of {@link AspectRatio} specified by {@code x} and {@code y} values representing the width and height.
     * The values {@code x} and {@code y} will be reduced by their greatest common divider.
     *
     * @param x The width
     * @param y The height
     * @return An instance of {@link AspectRatio}
     */
    public static AspectRatio createAspectRatio(int x, int y) {
        int gcd = greatestCommonDivisor(x, y);
        x /= gcd;
        y /= gcd;
        Iterator<AspectRatio> iterator = aspectRatios.iterator();
        AspectRatio aspectRatio = new AspectRatio(x, y);
        while (iterator.hasNext()) {
            AspectRatio ratio = iterator.next();
            if (ratio.equals(aspectRatio)) {
                return ratio;
            }
        }
        aspectRatios.add(aspectRatio);
        return aspectRatio;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof AspectRatio) {
            AspectRatio ratio = (AspectRatio) o;
            return x == ratio.x && y == ratio.y;
        }
        return false;
    }

    @Override
    public String toString() {
        return x + ":" + y;
    }

    @Override
    public int compareTo(@NonNull AspectRatio another) {
        if (equals(another)) {
            return 0;
        } else if ((float) x / y - (float) another.getX() / another.getY() > 0) {
            return 1;
        }
        return -1;
    }

    /**
     * Calculates the greatest common divisor between the given value.
     *
     * @param firstNo  the first number.
     * @param secondNo the second number.
     * @return the greatest common divisor between the given values.
     */
    private static int greatestCommonDivisor(int firstNo, int secondNo) {
        while (secondNo != 0) {
            int c = secondNo;
            secondNo = firstNo % secondNo;
            firstNo = c;
        }
        return firstNo;
    }

    /**
     * @return the value of the ratio for the width dimension.
     */
    public int getX() {
        return x;
    }

    /**
     * @return the value for the ratio for the height dimension.
     */
    public int getY() {
        return y;
    }

    /**
     * Checks if the current ratio matches with the given size.
     *
     * @param size the size that will be checked if matches the current ratio.
     * @return {@code true} if the size matches the current aspect ratio, {@code false} otherwise.
     */
    boolean matches(Size size) {
        int gcd = greatestCommonDivisor(size.getWidth(), size.getHeight());
        int x = size.getWidth() / gcd;
        int y = size.getHeight() / gcd;
        return this.x == x && this.y == y;
    }
}
