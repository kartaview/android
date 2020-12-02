//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.ml;

import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;

// C++: class RTrees
//javadoc: RTrees
public class RTrees extends DTrees {

    protected RTrees(long addr) { super(addr); }


    //
    // C++:  Mat getVarImportance()
    //

    //javadoc: RTrees::create()
    public static RTrees create() {

        RTrees retVal = new RTrees(create_0());

        return retVal;
    }


    //
    // C++: static Ptr_RTrees create()
    //

    // C++:  Mat getVarImportance()
    private static native long getVarImportance_0(long nativeObj);


    //
    // C++:  TermCriteria getTermCriteria()
    //

    // C++: static Ptr_RTrees create()
    private static native long create_0();


    //
    // C++:  bool getCalculateVarImportance()
    //

    // C++:  TermCriteria getTermCriteria()
    private static native double[] getTermCriteria_0(long nativeObj);


    //
    // C++:  int getActiveVarCount()
    //

    // C++:  bool getCalculateVarImportance()
    private static native boolean getCalculateVarImportance_0(long nativeObj);


    //
    // C++:  void setActiveVarCount(int val)
    //

    // C++:  int getActiveVarCount()
    private static native int getActiveVarCount_0(long nativeObj);


    //
    // C++:  void setCalculateVarImportance(bool val)
    //

    // C++:  void setActiveVarCount(int val)
    private static native void setActiveVarCount_0(long nativeObj, int val);


    //
    // C++:  void setTermCriteria(TermCriteria val)
    //

    // C++:  void setCalculateVarImportance(bool val)
    private static native void setCalculateVarImportance_0(long nativeObj, boolean val);

    // C++:  void setTermCriteria(TermCriteria val)
    private static native void setTermCriteria_0(long nativeObj, int val_type, int val_maxCount, double val_epsilon);

    // native support for java finalize()
    private static native void delete(long nativeObj);

    //javadoc: RTrees::getVarImportance()
    public Mat getVarImportance() {

        Mat retVal = new Mat(getVarImportance_0(nativeObj));

        return retVal;
    }

    //javadoc: RTrees::getTermCriteria()
    public TermCriteria getTermCriteria() {

        TermCriteria retVal = new TermCriteria(getTermCriteria_0(nativeObj));

        return retVal;
    }

    //javadoc: RTrees::setTermCriteria(val)
    public void setTermCriteria(TermCriteria val) {

        setTermCriteria_0(nativeObj, val.type, val.maxCount, val.epsilon);

        return;
    }

    //javadoc: RTrees::getCalculateVarImportance()
    public boolean getCalculateVarImportance() {

        boolean retVal = getCalculateVarImportance_0(nativeObj);

        return retVal;
    }

    //javadoc: RTrees::setCalculateVarImportance(val)
    public void setCalculateVarImportance(boolean val) {

        setCalculateVarImportance_0(nativeObj, val);

        return;
    }

    //javadoc: RTrees::getActiveVarCount()
    public int getActiveVarCount() {

        int retVal = getActiveVarCount_0(nativeObj);

        return retVal;
    }

    //javadoc: RTrees::setActiveVarCount(val)
    public void setActiveVarCount(int val) {

        setActiveVarCount_0(nativeObj, val);

        return;
    }

    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }

}
