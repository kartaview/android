//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.ml;

import org.opencv.core.Mat;

// C++: class DTrees
//javadoc: DTrees
public class DTrees extends StatModel {

    public static final int
            PREDICT_AUTO = 0,
            PREDICT_SUM = (1 << 8),
            PREDICT_MAX_VOTE = (2 << 8),
            PREDICT_MASK = (3 << 8);


    protected DTrees(long addr) { super(addr); }


    //
    // C++:  Mat getPriors()
    //

    //javadoc: DTrees::create()
    public static DTrees create() {

        DTrees retVal = new DTrees(create_0());

        return retVal;
    }


    //
    // C++: static Ptr_DTrees create()
    //

    // C++:  Mat getPriors()
    private static native long getPriors_0(long nativeObj);


    //
    // C++:  bool getTruncatePrunedTree()
    //

    // C++: static Ptr_DTrees create()
    private static native long create_0();


    //
    // C++:  bool getUse1SERule()
    //

    // C++:  bool getTruncatePrunedTree()
    private static native boolean getTruncatePrunedTree_0(long nativeObj);


    //
    // C++:  bool getUseSurrogates()
    //

    // C++:  bool getUse1SERule()
    private static native boolean getUse1SERule_0(long nativeObj);


    //
    // C++:  float getRegressionAccuracy()
    //

    // C++:  bool getUseSurrogates()
    private static native boolean getUseSurrogates_0(long nativeObj);


    //
    // C++:  int getCVFolds()
    //

    // C++:  float getRegressionAccuracy()
    private static native float getRegressionAccuracy_0(long nativeObj);


    //
    // C++:  int getMaxCategories()
    //

    // C++:  int getCVFolds()
    private static native int getCVFolds_0(long nativeObj);


    //
    // C++:  int getMaxDepth()
    //

    // C++:  int getMaxCategories()
    private static native int getMaxCategories_0(long nativeObj);


    //
    // C++:  int getMinSampleCount()
    //

    // C++:  int getMaxDepth()
    private static native int getMaxDepth_0(long nativeObj);


    //
    // C++:  void setCVFolds(int val)
    //

    // C++:  int getMinSampleCount()
    private static native int getMinSampleCount_0(long nativeObj);


    //
    // C++:  void setMaxCategories(int val)
    //

    // C++:  void setCVFolds(int val)
    private static native void setCVFolds_0(long nativeObj, int val);


    //
    // C++:  void setMaxDepth(int val)
    //

    // C++:  void setMaxCategories(int val)
    private static native void setMaxCategories_0(long nativeObj, int val);


    //
    // C++:  void setMinSampleCount(int val)
    //

    // C++:  void setMaxDepth(int val)
    private static native void setMaxDepth_0(long nativeObj, int val);


    //
    // C++:  void setPriors(Mat val)
    //

    // C++:  void setMinSampleCount(int val)
    private static native void setMinSampleCount_0(long nativeObj, int val);


    //
    // C++:  void setRegressionAccuracy(float val)
    //

    // C++:  void setPriors(Mat val)
    private static native void setPriors_0(long nativeObj, long val_nativeObj);


    //
    // C++:  void setTruncatePrunedTree(bool val)
    //

    // C++:  void setRegressionAccuracy(float val)
    private static native void setRegressionAccuracy_0(long nativeObj, float val);


    //
    // C++:  void setUse1SERule(bool val)
    //

    // C++:  void setTruncatePrunedTree(bool val)
    private static native void setTruncatePrunedTree_0(long nativeObj, boolean val);


    //
    // C++:  void setUseSurrogates(bool val)
    //

    // C++:  void setUse1SERule(bool val)
    private static native void setUse1SERule_0(long nativeObj, boolean val);

    // C++:  void setUseSurrogates(bool val)
    private static native void setUseSurrogates_0(long nativeObj, boolean val);

    // native support for java finalize()
    private static native void delete(long nativeObj);

    //javadoc: DTrees::getPriors()
    public Mat getPriors() {

        Mat retVal = new Mat(getPriors_0(nativeObj));

        return retVal;
    }

    //javadoc: DTrees::setPriors(val)
    public void setPriors(Mat val) {

        setPriors_0(nativeObj, val.nativeObj);

        return;
    }

    //javadoc: DTrees::getTruncatePrunedTree()
    public boolean getTruncatePrunedTree() {

        boolean retVal = getTruncatePrunedTree_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setTruncatePrunedTree(val)
    public void setTruncatePrunedTree(boolean val) {

        setTruncatePrunedTree_0(nativeObj, val);

        return;
    }

    //javadoc: DTrees::getUse1SERule()
    public boolean getUse1SERule() {

        boolean retVal = getUse1SERule_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setUse1SERule(val)
    public void setUse1SERule(boolean val) {

        setUse1SERule_0(nativeObj, val);

        return;
    }

    //javadoc: DTrees::getUseSurrogates()
    public boolean getUseSurrogates() {

        boolean retVal = getUseSurrogates_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setUseSurrogates(val)
    public void setUseSurrogates(boolean val) {

        setUseSurrogates_0(nativeObj, val);

        return;
    }

    //javadoc: DTrees::getRegressionAccuracy()
    public float getRegressionAccuracy() {

        float retVal = getRegressionAccuracy_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setRegressionAccuracy(val)
    public void setRegressionAccuracy(float val) {

        setRegressionAccuracy_0(nativeObj, val);

        return;
    }

    //javadoc: DTrees::getCVFolds()
    public int getCVFolds() {

        int retVal = getCVFolds_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setCVFolds(val)
    public void setCVFolds(int val) {

        setCVFolds_0(nativeObj, val);

        return;
    }

    //javadoc: DTrees::getMaxCategories()
    public int getMaxCategories() {

        int retVal = getMaxCategories_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setMaxCategories(val)
    public void setMaxCategories(int val) {

        setMaxCategories_0(nativeObj, val);

        return;
    }

    //javadoc: DTrees::getMaxDepth()
    public int getMaxDepth() {

        int retVal = getMaxDepth_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setMaxDepth(val)
    public void setMaxDepth(int val) {

        setMaxDepth_0(nativeObj, val);

        return;
    }

    //javadoc: DTrees::getMinSampleCount()
    public int getMinSampleCount() {

        int retVal = getMinSampleCount_0(nativeObj);

        return retVal;
    }

    //javadoc: DTrees::setMinSampleCount(val)
    public void setMinSampleCount(int val) {

        setMinSampleCount_0(nativeObj, val);

        return;
    }

    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }

}
