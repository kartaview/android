package com.telenav.osv.recorder.camera.util;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodecInfo;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.utils.ExtensionsKt;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;

/**
 * Helper class for the Camera2 implementation.
 * The available operations:
 * <ul>
 * <li>{@link #calculateCameraFocusRect(int, int, int, int, Rect)}</li>
 * <li>{@link #chooseOptimalPreviewSize(List, int, int)}</li>
 * <li>{@link #checkTapToFocusAvailability(CameraCharacteristics)}</li>
 * <li>{@link #getPreviewSizes(StreamConfigurationMap)}</li>
 * <li>{@link #getPictureSizes(StreamConfigurationMap, int)}</li>
 * </ul>
 * @author cameliao
 */
@SuppressWarnings("deprecation")
public class CameraHelper {

    private static final String TAG = CameraHelper.class.getSimpleName();

    /**
     * The value to determine the device rotation using the maximum rotation degrees.
     * This value if used to normalize the JPEG orientation when sensor orientation is 270.
     */
    private static final int MAXIMUM_ROTATION_DEGREES_360 = 360;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * The width and the height for the focus rectangle.
     */
    private static final int FOCUS_RECTANGLE_DIMENSION = 100;

    /**
     * Value for dividing a number in half.
     */
    private static final int HALF_DIVIDER = 2;

    /**
     * Value for dividing a number to a quarter.
     */
    private static final int QUARTER_DIVIDER = 4;

    /**
     * Value to double a number.
     */
    private static final int DOUBLE_MULTIPLIER = 2;

    /**
     * The number of regions for the auto-focus (AF) or auto-exposure (AE) routine.
     * If the number of regions is equal to this value, then the AF or AE feature is not available on device.
     */
    private static final int REGION_UNAVAILABLE = 0;

    /**
     * The minimum threshold for the camera to support. This is designed to be in Mega Pixels so the value is rounded to int.
     */
    private static final int FIVE_MEGA_PIXEL_THRESHOLD = 5;

    /**
     * Creates a rectangle around the tapped area.
     * @param x the value on the X axis for the tapped area.
     * @param y the value on the Y axis for the tapped area.
     * @return a rectangle with the center being the tapped area.
     */
    public static Rect calculateCameraFocusRect(int x, int y, int previewHeight, int previewWidth, Rect rect) {
        //convert the point form the screen window to the camera active pixel array window.
        int sensorHeight = rect.height();
        int sensorWidth = rect.width();
        int sensorX = x * sensorWidth / previewWidth;
        int sensorY = y * sensorHeight / previewHeight;
        return new Rect((sensorX - FOCUS_RECTANGLE_DIMENSION / HALF_DIVIDER), (sensorY - FOCUS_RECTANGLE_DIMENSION / HALF_DIVIDER),
                (sensorX + FOCUS_RECTANGLE_DIMENSION / HALF_DIVIDER), (sensorY + FOCUS_RECTANGLE_DIMENSION / HALF_DIVIDER));
    }

    /**
     * Returns the focus rectangle around the tapped area.
     */
    //TODO: This legacy method should be refactored properly.
    public static Rect calculateCameraFocusRect(int x, int y) {
        float top, left, right, bottom;

        x = x * 2 - 1000;
        y = y * 2 - 1000;

        int r = 100;

        left = x - r;
        right = x + r;
        top = y - r;
        bottom = y + r;

        int hOffset = (int) ((left < -1000) ? ((left + 1000) * (-1)) : ((right > 1000) ? ((right - 1000) * (-1)) : (0)));
        int vOffset = (int) ((top < -1000) ? ((top + 1000) * (-1)) : ((bottom > 1000) ? ((bottom - 1000) * (-1)) : (0)));

        return new Rect((int) (left + hOffset), (int) (top + vOffset), (int) (right + hOffset), (int) (bottom + vOffset));
    }

    /**
     * Chooses the smallest from those big enough to be considered candidates. If there is no size big enough picks the largest one.
     * Chooses optimal preview size based on the container size.
     * @return The picked size for camera preview.
     */
    public static Size chooseOptimalPreviewSize(List<Size> previewSizesCandidates, int containerWidth, int containerHeight) {
        Size optimalSize = null;
        double minHeightDiff = Double.MAX_VALUE;
        double minWidthDiff = Double.MAX_VALUE;
        //pick the preview size which is the closest to the container dimensions.
        for (Size size : previewSizesCandidates) {
            //check first for the minimum difference between the preview height and the container height.
            if (Math.abs(size.getHeight() - containerHeight) < minHeightDiff) {
                optimalSize = size;
                minHeightDiff = Math.abs(size.getHeight() - containerHeight);
                minWidthDiff = Math.abs(size.getWidth() - containerWidth);
            }
            //if there are multiple sizes having the same minimum height difference
            //check for the minimum width difference
            else if ((Math.abs(size.getHeight() - containerHeight) == minHeightDiff)
                    && (Math.abs(size.getWidth() - containerWidth) < minWidthDiff)) {
                optimalSize = size;
                minWidthDiff = Math.abs(size.getWidth() - containerWidth);
            }
        }
        if (optimalSize != null) {
            Log.d(TAG, String.format("chooseOptimalPreviewSize. Container size (width x height): %s x %s. Optimal size (width x height): %s x %s. ",
                    containerWidth, containerHeight, optimalSize.getWidth(), optimalSize.getHeight()));
        }
        return optimalSize;
    }

    /**
     * This method should be used for Camera1 API.
     * Choose the optimal preview size based on the container size.
     * @param previewSizesCandidates the list with the available preview sizes.
     * @param containerSize the size of the preview container.
     * @return the optimal size from the available preview sizes list which is compatible with the container size.
     * @see #chooseOptimalPreviewSize(List, int, int)
     */
    public static Size chooseOptimalPreviewSize(List<Camera.Size> previewSizesCandidates, Size containerSize) {
        List<Size> supportedPreviewSizes = new ArrayList<>();
        for (Camera.Size size : previewSizesCandidates) {
            supportedPreviewSizes.add(new Size(size.width, size.height));
        }
        return chooseOptimalPreviewSize(supportedPreviewSizes, containerSize.getWidth(), containerSize.getHeight());
    }

    /**
     * Chooses the optimal picture size taking into account the dimension of the preview container.
     * @param pictureSizes the list containing the picture sizes.
     * @param previewWidth the width of the preview.
     * @param previewHeight the height of the preview.
     * @return the optimal size for a picture that is closest to the preview container size.
     */
    public static Size chooseOptimalPictureSize(SizeMap pictureSizes, int previewWidth, int previewHeight) {
        int surfaceLonger, surfaceShorter;
        if (previewWidth < previewHeight) {
            surfaceLonger = previewHeight;
            surfaceShorter = previewWidth;
        } else {
            surfaceLonger = previewWidth;
            surfaceShorter = previewHeight;
        }
        SortedSet<Size> sizes = pictureSizes.sizes(AspectRatio.createAspectRatio(surfaceLonger, surfaceShorter));
        if (sizes == null) {
            sizes = pictureSizes.sizes(AspectRatioTypes.ASPECT_RATIO_16_9);
        }
        //the 5mp is the default value for the picture size
        //if no size was found with this requirement the next size grater than 5mp will be selected.
        Size pictureSize = sizes.first();
        for (Size size : sizes) {
            if (isResolutionValid(size)) {
                Log.d(TAG, "chooseOptimalPictureSize. New resolution set: " + size);
                return size;
            } else if (pictureSize.getRoundedMegaPixels() > size.getRoundedMegaPixels()) {
                Log.d(TAG, "chooseOptimalPictureSize. New base resolution to check against: " + pictureSize);
                pictureSize = size;
            }
        }
        Size lastSize = sizes.last();
        if (pictureSize.getRoundedMegaPixels() <= lastSize.getRoundedMegaPixels()) {
            Log.d(TAG, "chooseOptimalPictureSize. New resolution set: " + lastSize);
            return lastSize;
        }
        Log.d(TAG, "chooseOptimalPictureSize. New resolution set: " + pictureSize);
        return pictureSize;
    }

    /**
     * @param pictureSizes the list containing the picture sizes.
     * @param previewWidth the width of the preview.
     * @param previewHeight the height of the preview.
     * @return the {@code SortedSet<Size>} represented the picture available with respect to preview aspect ratio if there are any otherwise with respect to 16:9 aspect ratio.
     */
    public static SortedSet<Size> getPictureSize(SizeMap pictureSizes, int previewWidth, int previewHeight) {
        int surfaceLonger, surfaceShorter;
        if (previewWidth < previewHeight) {
            surfaceLonger = previewHeight;
            surfaceShorter = previewWidth;
        } else {
            surfaceLonger = previewWidth;
            surfaceShorter = previewHeight;
        }
        SortedSet<Size> sizes = pictureSizes.sizes(AspectRatio.createAspectRatio(surfaceLonger, surfaceShorter));
        if (sizes == null) {
            sizes = pictureSizes.sizes(AspectRatioTypes.ASPECT_RATIO_16_9);
        }

        return sizes;
    }

    /**
     * @param resolution the resolution to be checked against.
     * @return {@code true} if the resolution is greater or equal with the {@link #FIVE_MEGA_PIXEL_THRESHOLD} threshold.
     */
    public static boolean isResolutionValid(Size resolution) {
        return resolution.getRoundedMegaPixels() >= FIVE_MEGA_PIXEL_THRESHOLD;
    }

    public static com.telenav.osv.recorder.camera.Camera initCamera(ApplicationPreferences appPrefs, Context context) {
        int pictureWidth = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
        int pictureHeight = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
        boolean videoMode = appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED);
        Log.d(TAG,
                "initCamera. Status: Initialising camera. Width: " + pictureWidth + ". Height: " + pictureHeight + ". Video mode: " + videoMode);
        com.telenav.osv.recorder.camera.Camera camera = Injection.provideCamera(context,
                new Size(pictureWidth, pictureHeight),
                Utils.getLandscapeScreenSize(context),
                !videoMode);
        if (pictureWidth == 0 || pictureHeight == 0) {
            Size videoResolution = ExtensionsKt.getResolution(camera);
            // Use case 1 when camera is in video already and the resolution is greater than 5MP
            if (videoMode && CameraHelper.isResolutionValid(videoResolution)) {
                saveResolutionInPrefs(videoResolution, camera, appPrefs);
                return camera;
            }
            //check to not initialise camera again if it is already in jpeg
            if (videoMode) {
                camera.closeCamera();
                appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, false);
                camera = Injection.provideCamera(context,
                        new Size(0, 0),
                        Utils.getLandscapeScreenSize(context),
                        true);
            }
            Size jpegResolution = ExtensionsKt.getResolution(camera);
            if (CameraHelper.isResolutionValid(jpegResolution) || jpegResolution.getRoundedMegaPixels() > videoResolution.getRoundedMegaPixels()) {
                saveResolutionInPrefs(jpegResolution, camera, appPrefs);
            } else {
                camera.closeCamera();
                appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
                camera = Injection.provideCamera(context,
                        new Size(0, 0),
                        Utils.getLandscapeScreenSize(context),
                        false);
                videoResolution = ExtensionsKt.getResolution(camera);
                saveResolutionInPrefs(videoResolution, camera, appPrefs);
            }
            return camera;
        }
        return camera;
    }

    /**
     * Persist the camera resolution in the app prefs.
     * @param resolution the resolution to be persisted.
     * @param camera the camera for which the new resolution to be set
     * @param appPrefs the application preferences where the new settings will be persisted.
     */
    public static void saveResolutionInPrefs(Size resolution, com.telenav.osv.recorder.camera.Camera camera, ApplicationPreferences appPrefs) {
        appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, camera.isVideoMode());
        Log.d(TAG,
                "initCamera. Status: Resolution reset. Width: " + resolution.getWidth() +
                        ". Height: " + resolution.getHeight() +
                        ". Video mode: " + camera.isVideoMode() +
                        ". Camera v2: " + camera.isCamera2Api());
        appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, resolution.getWidth());
        appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, resolution.getHeight());
        camera.setPictureSize(resolution);
    }

    /**
     * Checks if tap to focus is available for the current camera.
     * In order to focus on tap the auto focus region and the auto exposure region must be available on the camera characteristics.
     * @param characteristics the properties of the camera.
     * @return {@code true} if the regions are supported, {@code false} otherwise.
     */
    public static boolean checkTapToFocusAvailability(CameraCharacteristics characteristics) {
        Integer afRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        Integer aeRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return afRegions != null && aeRegions != null && (afRegions > REGION_UNAVAILABLE) && (aeRegions > REGION_UNAVAILABLE);
    }

    /**
     * @param map the object containing all the output formats supported by the camera.
     * @return a map containing all the preview sizes grouped by their aspect ratios.
     */
    public static SizeMap getPreviewSizes(StreamConfigurationMap map) {
        SizeMap previewSizes = new SizeMap();
        for (android.util.Size size : map.getOutputSizes(SurfaceTexture.class)) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                previewSizes.add(new Size(width, height));
            }
        }
        return previewSizes;
    }

    /**
     * @param map the object containing all the output formats supported by the camera.
     * @return a map containing all the picture sizes grouped by their aspect ratios.
     */
    public static SizeMap getPictureSizes(StreamConfigurationMap map, int imageFormat) {
        SizeMap pictureSizes = new SizeMap();
        MediaCodecInfo.VideoCapabilities videoCapabilities = Utils.getEncoderVideoCapabilities();
        //check if the current mode is video,
        //in this case we should keep only the sizes that are available for both, encoder and camera.
        if (imageFormat != ImageFormat.JPEG && videoCapabilities != null) {
            for (android.util.Size size : map.getOutputSizes(imageFormat)) {
                if (videoCapabilities.isSizeSupported(size.getWidth(), size.getHeight())) {
                    pictureSizes.add(new Size(size.getWidth(), size.getHeight()));
                }
            }
        } else {
            for (android.util.Size size : map.getOutputSizes(imageFormat)) {
                pictureSizes.add(new Size(size.getWidth(), size.getHeight()));
            }
        }
        return pictureSizes;
    }

    /**
     * This method should be used for Camera1 API.
     * @param supportedPictureSizes the list containing all the supported resolutions for a picture.
     * @return a map containing all the picture sizes grouped by their aspect ratios.
     */
    public static SizeMap getPictureSizes(List<Camera.Size> supportedPictureSizes) {
        SizeMap pictureSizes = new SizeMap();
        for (Camera.Size size : supportedPictureSizes) {
            pictureSizes.add(new Size(size.width, size.height));
        }
        return pictureSizes;
    }

    /**
     * @param containerSize the container size for the camera preview.
     * @param previewSize the actual size of the preview.
     * @return the layout params for the camera preview which is centered in parent.
     */
    public static ViewGroup.LayoutParams getPreviewLayoutParams(Size containerSize, Size previewSize) {
        RelativeLayout.LayoutParams previewLayoutParams;
        float width = (float) containerSize.getWidth() / (float) previewSize.getWidth();
        float height = (float) containerSize.getHeight() / (float) previewSize.getHeight();
        if (width == height) {
            previewLayoutParams = new RelativeLayout.LayoutParams(containerSize.getWidth(), containerSize.getHeight());
        } else {
            float ratio = Math.max(width, height);
            int previewWidth = (int) (previewSize.getWidth() * ratio);
            int previewHeight = (int) (previewSize.getHeight() * ratio);
            previewLayoutParams = new RelativeLayout.LayoutParams(previewWidth, previewHeight);
        }
        previewLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        return previewLayoutParams;
    }

    /**
     * @param rotation the device rotation in degrees.
     * @param sensorOrientation the orientation received from sensors.
     * @return the device orientation in degrees.
     */
    public static int getOrientation(int rotation, int sensorOrientation) {
        return (sensorOrientation + rotation) % MAXIMUM_ROTATION_DEGREES_360;
    }

    /**
     * Convert the camera output NV21 format to YUV420 Planar.
     * This YUV format is used for frame encoding.
     * <ul>The format representation:
     * <li>NV21: Y0Y1Y2...V0U0V2U2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * <li>YUV420Planar: Y0Y1Y2...U0U2...V0V2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * </ul>
     * In order to achieve the conversion between NV21 to YUV420Planar
     * the U component must be the next after the Y component, followed by V component.
     * @param frameData the buffer containing the raw frame data and also a padding which will be filled with the converted frame data.
     * @param width the frame width.
     * @param height the frame height.
     * @return the frame data having the YUV420 planar format.
     */
    public static byte[] convertFromNV21ToYUV420Planar(byte[] frameData, int width, int height) {
        int yLength = width * height;
        int index = 0;
        int yConvertedStartIndex = yLength + (yLength / DOUBLE_MULTIPLIER);
        int uConvertedStartIndex = yConvertedStartIndex + yLength;
        for (int i = yConvertedStartIndex; i < uConvertedStartIndex; i++) {
            frameData[i] = frameData[index];
            index++;
        }
        int vIndex = index;
        int uIndex = index + 1;
        for (int i = uConvertedStartIndex; i < uConvertedStartIndex + yLength / QUARTER_DIVIDER; i++) {
            frameData[i] = frameData[uIndex];
            frameData[i + yLength / QUARTER_DIVIDER] = frameData[vIndex];
            uIndex += 2;
            vIndex += 2;
        }
        return frameData;
    }


    /**
     * Convert the camera output NV21 format to YUV420 Semi-Planar.
     * This YUV format is used for frame encoding.
     * <ul>The format representation:
     * <li>NV21: Y0Y1Y2...V0U0V2U2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * <li>YUV420SemiPlanar: Y0Y1Y2...U0V0U2V2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * </ul>
     * In order to achieve the conversion between NV21 to YUV420Planar
     * the U component must be swapped with the V component.
     * @param frameData the buffer containing the raw frame data and also a padding which will be filled with the converted frame data.
     * @param width the frame width.
     * @param height the frame height.
     * @return the frame data having the YUV420 semi-planar format.
     */
    public static byte[] convertFromNV21ToYUV420SemiPlanar(byte[] frameData, int width, int height) {
        int yLength = width * height;
        int index = 0;
        int yConvertedStartIndex = yLength + (yLength / DOUBLE_MULTIPLIER);
        int uConvertedStartIndex = yConvertedStartIndex + yLength;
        for (int i = yConvertedStartIndex; i < uConvertedStartIndex; i++) {
            frameData[i] = frameData[index];
            index++;
        }
        int vIndex = index;
        int uIndex = index + 1;
        for (int i = uConvertedStartIndex; i < uConvertedStartIndex + yLength / HALF_DIVIDER; i += 2) {
            frameData[i] = frameData[uIndex];
            frameData[i + 1] = frameData[vIndex];
            uIndex += 2;
            vIndex += 2;
        }
        return frameData;
    }

    /**
     * Convert the camera output YUV420 format to YUV420 Planar.
     * <ul>
     * <li>YUV420: Y0Y1Y2...U0U2...V0V2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * <li>YUV420Planar: Y0Y1Y2...U0U2...V0V2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * </ul>
     * @param frameData the buffer containing the raw frame data and also a padding which will be filled with the converted frame data.
     * @param width the frame width.
     * @param height the frame height.
     * @return the frame data having the YUV420 planar format.
     */
    public static byte[] convertCameraYUVtoYUV420Planar(byte[] frameData, int width, int height) {
        int yLength = width * height;
        int index = yLength + yLength / HALF_DIVIDER;
        for (int i = 0; i < yLength + yLength / HALF_DIVIDER; i++) {
            frameData[index] = frameData[i];
            index++;
        }
        return frameData;
    }

    /**
     * Convert the camera output YUV420 format to YUV420 Semi-Planar.
     * <ul>
     * <li>YUV420: Y0Y1Y2...U0U2...V0V2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * <li>YUV420SemiPlanar: Y0Y1Y2...U0V0U2V2...</li>
     * <p>The Y component length: frameWidth * frameHeight.</p>
     * <p>The U component length: (frameWidth * frameHeight)/4.</p>
     * <p>The V component length: (frameWidth * frameHeight)/4.</p>
     * </ul>
     * @param frameData the buffer containing the raw frame data and also a padding which will be filled with the converted frame data.
     * @param width the frame width.
     * @param height the frame height.
     * @return the frame data having the YUV420 semi-planar format.
     */
    public static byte[] convertCameraYUVtoYUV420SemiPlanar(byte[] frameData, int width, int height) {
        int yLength = width * height;
        int index = 0;
        int yConvertedStartIndex = yLength + (yLength / HALF_DIVIDER);
        int uConvertedStartIndex = yConvertedStartIndex + yLength;
        for (int i = yConvertedStartIndex; i < uConvertedStartIndex; i++) {
            frameData[i] = frameData[index];
            index++;
        }
        int indexU = yLength;
        int indexV = yLength + yLength / QUARTER_DIVIDER;
        for (int i = uConvertedStartIndex; i < uConvertedStartIndex + yLength / HALF_DIVIDER; i += 2) {
            frameData[i] = frameData[indexU];
            frameData[i + 1] = frameData[indexV];
            indexU++;
            indexV++;
        }
        return frameData;
    }
}
