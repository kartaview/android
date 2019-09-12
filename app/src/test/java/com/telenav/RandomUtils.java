package com.telenav;

import java.util.Random;

/**
 * Utility class to generate random numbers for testing purposes.
 */
public class RandomUtils {

    /**
     * @return a random {@code int} number.
     */
    public static int generateInt() {
        Random random = new Random();
        return random.nextInt();
    }

    /**
     * @return a random {@code double} number.
     */
    public static double generateDouble() {
        Random random = new Random();
        return random.nextDouble();
    }
}
