//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.ml;


// C++: class Boost
//javadoc: Boost
public class Boost extends DTrees {

    public static final int
            DISCRETE = 0,
            REAL = 1,
            LOGIT = 2,
            GENTLE = 3;


    protected Boost(long addr) { super(addr); }


    //
    // C++: static Ptr_Boost create()
    //

    //javadoc: Boost::create()
    public static Boost create() {

        Boost retVal = new Boost(create_0());

        return retVal;
    }


    //
    // C++:  double getWeightTrimRate()
    //

    // C++: static Ptr_Boost create()
    private static native long create_0();


    //
    // C++:  int getBoostType()
    //

    // C++:  double getWeightTrimRate()
    private static native double getWeightTrimRate_0(long nativeObj);


    //
    // C++:  int getWeakCount()
    //

    // C++:  int getBoostType()
    private static native int getBoostType_0(long nativeObj);


    //
    // C++:  void setBoostType(int val)
    //

    // C++:  int getWeakCount()
    private static native int getWeakCount_0(long nativeObj);


    //
    // C++:  void setWeakCount(int val)
    //

    // C++:  void setBoostType(int val)
    private static native void setBoostType_0(long nativeObj, int val);


    //
    // C++:  void setWeightTrimRate(double val)
    //

    // C++:  void setWeakCount(int val)
    private static native void setWeakCount_0(long nativeObj, int val);

    // C++:  void setWeightTrimRate(double val)
    private static native void setWeightTrimRate_0(long nativeObj, double val);

    // native support for java finalize()
    private static native void delete(long nativeObj);

    //javadoc: Boost::getWeightTrimRate()
    public double getWeightTrimRate() {

        double retVal = getWeightTrimRate_0(nativeObj);

        return retVal;
    }

    //javadoc: Boost::setWeightTrimRate(val)
    public void setWeightTrimRate(double val) {

        setWeightTrimRate_0(nativeObj, val);

        return;
    }

    //javadoc: Boost::getBoostType()
    public int getBoostType() {

        int retVal = getBoostType_0(nativeObj);

        return retVal;
    }

    //javadoc: Boost::setBoostType(val)
    public void setBoostType(int val) {

        setBoostType_0(nativeObj, val);

        return;
    }

    //javadoc: Boost::getWeakCount()
    public int getWeakCount() {

        int retVal = getWeakCount_0(nativeObj);

        return retVal;
    }

    //javadoc: Boost::setWeakCount(val)
    public void setWeakCount(int val) {

        setWeakCount_0(nativeObj, val);

        return;
    }

    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }

}
