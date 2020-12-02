package com.skobbler.sensorlib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.skobbler.sensorlib.listener.SignDetectedListener;
import com.skobbler.sensorlib.sign.SignType;
import com.skobbler.sensorlib.template.Template;
import com.skobbler.sensorlib.template.TemplateBuffer;

/**
 * Created by Kalman on 7/14/2015.
 */
public class SensorLib {
    /**
     * the TAG used for logging events from the library
     */
    private static final String TAG = "SensorLib";

    static { // load the native library "libsensor.so" and "libopencv_java"
        if (!OpenCVLoader.initDebug()) {
            Log.d("OPENCV", "OpenCV could not be loaded");
        }
        System.loadLibrary("sensor");
        Log.d("SensorLib", "static initializer: loaded library");
    }

    /**
     * The context used for getting assets from the device
     * it needs to be initialized in the constructor
     */
    private final Context mContext;

    private SignType.enSignType detectedSignType;

    /**
     * the mat used as a reference to the untouched camera frame
     */
    private Mat mRgba = new Mat();

    /**
     * the camera view that provides the frames from the android camera
     * needs to be set from the exterior
     */
    private CameraBridgeViewBase mCameraView;

    /**
     * the mat used as a reference to the already processed camera frame
     */
    private Mat processedMat = new Mat();

    private ByteArrayOutputStream outputstream;

    /**
     * the listener for the sign detection callback
     * implements SignDetectedListener interface
     */
    private SignDetectedListener mSignDetectedListener;

    /**
     * the handler that attaches to the main looper, to forward the native callbacks
     */
    private Handler mainHandler;

    /**
     * the initialized flag
     * true if the initialization of the library has completed successfully, false otherwise
     */
    private boolean mInitialized;

    /**
     * the path initialized flag for recording
     * true if the path was set for the creation of the video files, false otherwise
     */
    private boolean mStoragePathWasSet;

    private Bitmap mBitmap;

    private Runnable notifRunnable;

    private Mat mYuv;

    public SensorLib(Context context) {
        this.mContext = context;
        initializeFiles();
        outputstream = new ByteArrayOutputStream();
    }

    /**
     * the method responsible for destroying all the resources allocated in native
     * Includes OpenCV Mats and native objects used by the SensorLib jni bridge
     */
    public void destroy() {
        mInitialized = false;
        Log.d(TAG, "Destroy called");
        destroynative();
        if (mRgba != null)
            mRgba.release();
        if (processedMat != null)
            processedMat.release();
    }

    /**
     * Sets the proprietary settings for the processing of the frames
     * @param settings - @see com.skobbler.sensorlib.SensorLibSettings
     */
    public void setSensorLibSettings(SensorLibSettings settings) {
        setprocessingsettings(settings.minFramesForDetection, settings.maxLostTrackFrames);
    }

    /**
     * Returns the listener that has been set with setOnSignDetectedListener
     * @return - the sign detected listener
     */
    public SignDetectedListener getOnSignDetectedListener() {
        return mSignDetectedListener;
    }

    /**
     * Sets the listener for the sign detection callback
     * @param mSignDetectedListener - see com.skobbler.sensorlib.listener.SignDetectedListener
     */
    public void setOnSignDetectedListener(SignDetectedListener mSignDetectedListener) {
        this.mSignDetectedListener = mSignDetectedListener;
    }

    /**
     * yuv420sp Mat input and output
     */
    public void processFrame(byte[] bytes, int w, int h, int rotation) {
        if (mInitialized) {
            if (mYuv == null) {
                mYuv = new Mat(h + h / 2, w, CvType.CV_8UC1);
            }
            mYuv.put(0, 0, bytes);
            if (rotation == 1) {
                Core.transpose(mYuv, mYuv);
                Core.flip(mYuv, mYuv, 1); //transpose+flip(1)=CW
            } else if (rotation == 2) {
                Core.transpose(mYuv, mYuv);
                Core.flip(mYuv, mYuv, 0); //transpose+flip(0)=CCW
            } else if (rotation == 3) {
                Core.flip(mYuv, mYuv, -1);    //flip(-1)=180
            }
            processFrame(mYuv);

        }
    }

    /**
     * the native callback that notified the java wrapper of a signDetected event
     * @param type - the type of the sign that has been recognized, @see com.skobbler.sensorlib.sign.SignType
     */
    protected void signdetectedcallback(int type) {
        detectedSignType = SignType.enSignType.forInt(type);
        Log.d(TAG, "Sign detected: " + detectedSignType.toString());
        if (mSignDetectedListener != null) {
            if (mainHandler == null) {
                mainHandler = new Handler(Looper.getMainLooper());
            }
            if (notifRunnable == null) {
                notifRunnable = new Runnable() {

                    @Override
                    public void run() {
                        mSignDetectedListener.onSignDetected(detectedSignType);
                    }
                };
            }

            mainHandler.post(notifRunnable);

        } else {
            Log.d(TAG, "No SignDetectedListener was set.");
        }
    }

    protected native SignType[] getfilenames();

    protected native boolean init(TemplateBuffer templates, SensorLib SensorLib, String pathToTessData);

    protected native void processFrame(Mat mat);

    protected native void destroynative();

    protected native void setprocessingsettings(int minFramesNrForDetection, int maxLostTrackFrames);

    /**
     * the method that initializes the template files used for sign recognition
     * runs on a background thread
     */
    private void initializeFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<SignType> fileList = new ArrayList<>(Arrays.asList(getfilenames()));
                ArrayList<Template> templateList = new ArrayList<>();
                InputStream bitmap = null;
                for (SignType st : fileList) {
                    try {
                        bitmap = mContext.getAssets().open(st.getFileName());
                        Bitmap bit = BitmapFactory.decodeStream(bitmap);
                        Mat mat = new Mat(bit.getWidth(), bit.getHeight(), CvType.CV_8UC4);
                        Utils.bitmapToMat(bit, mat);
                        templateList.add(new Template(st, mat));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (bitmap != null)
                            try {
                                bitmap.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
//                Log.d(TAG, "templates: ");
//                for (Template t : templateList) {
//                    Log.d(TAG, "template " + t);
//                }
                mInitialized = init(new TemplateBuffer(templateList), SensorLib.this, "");
                Log.d(TAG, "SensorLib initialization succeded: " + mInitialized);
            }
        }).start();
    }
}
