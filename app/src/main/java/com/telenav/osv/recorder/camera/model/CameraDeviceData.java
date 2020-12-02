package com.telenav.osv.recorder.camera.model;

public class CameraDeviceData {
    private String cameraId;
    private double horizontalFOV;
    private double verticalFOV;
    private float focalLength;
    private float lensAperture;

    public CameraDeviceData(String cameraId, double horizontalFOV, double verticalFOV, float focalLength, float lensAperture) {
        this.cameraId = cameraId;
        this.horizontalFOV = horizontalFOV;
        this.verticalFOV = verticalFOV;
        this.focalLength = focalLength;
        this.lensAperture = lensAperture;

    }

    public String getCameraId() {
        return cameraId;
    }

    public double getHorizontalFOV() {
        return horizontalFOV;
    }

    public double getVerticalFOV() {
        return verticalFOV;
    }

    public float getFocalLength() {
        return focalLength;
    }

    public float getLensAperture() {
        return lensAperture;
    }
}
