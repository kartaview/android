package com.telenav.osv.recorder.camera.model;

public class CameraSensorData {
    private Float focalLength;

    private Float focalNumber;

    private Float fieldOfView;

    private Float zoomFactor;

    private Float lensAperture;

    CameraSensorData(Float focalLength, Float focalNumber, Float fieldOfView, Float zoomFactor, Float lensAperture){
        this.focalLength = focalLength;
        this.focalNumber = focalNumber;
        this.fieldOfView = fieldOfView;
        this.zoomFactor = zoomFactor;
        this.lensAperture = lensAperture;
    }

    public Float getFocalLength() {
        return focalLength;
    }

    public void setFocalLength(Float focalLength) {
        this.focalLength = focalLength;
    }

    public Float getFocalNumber() {
        return focalNumber;
    }

    public void setFocalNumber(Float focalNumber) {
        this.focalNumber = focalNumber;
    }

    public Float getFieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(Float fieldOfView) {
        this.fieldOfView = fieldOfView;
    }

    public Float getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(Float zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    public Float getLensAperture() {
        return lensAperture;
    }

    public void setLensAperture(Float lensAperture) {
        this.lensAperture = lensAperture;
    }
}
