package com.skobbler.sensorlib.template;

import org.opencv.core.Mat;
import com.skobbler.sensorlib.sign.SignType;

/**
 * The template objects which are fed to the native library at init
 * Created by Kalman on 7/9/2015.
 */
public class Template {

    private SignType signType;

    private Mat mat;

    public Template() {
    }

    public Template(SignType type, Mat mat) {
        this.signType = type;
        this.mat = mat;
    }

    /**
     * @return the Mat image of the template
     */
    public Mat getMat() {
        return mat;
    }

    /**
     * sets the mat image of the template
     * @param matTemplate - the image to be set
     */
    public void setMat(Mat matTemplate) {
        this.mat = matTemplate;
    }

    public SignType.enSignType getSignType() {
        return SignType.enSignType.forInt(signType.getSignType());
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }

    @Override
    public String toString() {
        return signType.toString();
    }
}
