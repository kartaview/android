package com.telenav;

import java.util.Random;
import java.util.UUID;

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

    /**
     * @return a random {@code float} number.
     */
    public static float generateFloat() {
        Random random = new Random();
        return random.nextFloat();
    }

    /**
     * @return a random unique {@code String}.
     */
    public static String generateString() {
        return UUID.randomUUID().toString();
    }
}
