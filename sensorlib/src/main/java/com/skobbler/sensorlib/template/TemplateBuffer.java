package com.skobbler.sensorlib.template;

import java.util.ArrayList;
import org.opencv.core.Mat;

/**
 * The class used to translate the template data, from java to native, in jni
 * Created by Kalman on 7/9/2015.
 */
public class TemplateBuffer {
    public Mat[] matArray;

    public int[] signTypesArray;

    public TemplateBuffer(ArrayList<Template> templates) {
        matArray = new Mat[templates.size()];
        signTypesArray = new int[templates.size()];
        int i = 0;
        for (Template t : templates) {
            matArray[i] = t.getMat();
            signTypesArray[i] = t.getSignType().getValue();
            i++;
        }
//        Log.d("Templates", Arrays.toString(signTypesArray));
    }
}
