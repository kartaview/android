package com.telenav.osv.recorder.camera.util;

/**
 * The available types for teh aspect ratio:
 * <ul>
 * <li>{@link #ASPECT_RATIO_16_9}</li>
 * <li>{@link #ASPECT_RATIO_4_3}</li>
 * </ul>
 */
public @interface AspectRatioTypes {

    /**
     * The 16:9 aspect ratio.
     */
    AspectRatio ASPECT_RATIO_16_9 = AspectRatio.createAspectRatio(16, 9);

    /**
     * The 4:3 aspect ratio.
     */
    AspectRatio ASPECT_RATIO_4_3 = AspectRatio.createAspectRatio(4, 3);
}
