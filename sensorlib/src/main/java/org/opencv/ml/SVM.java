//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.ml;

import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;

// C++: class SVM
//javadoc: SVM
public class SVM extends StatModel {

    public static final int
            C_SVC = 100,
            NU_SVC = 101,
            ONE_CLASS = 102,
            EPS_SVR = 103,
            NU_SVR = 104,
            CUSTOM = -1,
            LINEAR = 0,
            POLY = 1,
            RBF = 2,
            SIGMOID = 3,
            CHI2 = 4,
            INTER = 5,
            C = 0,
            GAMMA = 1,
            P = 2,
            NU = 3,
            COEF = 4,
            DEGREE = 5;


    protected SVM(long addr) { super(addr); }


    //
    // C++:  Mat getClassWeights()
    //

    //javadoc: SVM::create()
    public static SVM create() {

        SVM retVal = new SVM(create_0());

        return retVal;
    }


    //
    // C++:  Mat getSupportVectors()
    //

    // C++:  Mat getClassWeights()
    private static native long getClassWeights_0(long nativeObj);


    //
    // C++:  Mat getUncompressedSupportVectors()
    //

    // C++:  Mat getSupportVectors()
    private static native long getSupportVectors_0(long nativeObj);


    //
    // C++: static Ptr_SVM create()
    //

    // C++:  Mat getUncompressedSupportVectors()
    private static native long getUncompressedSupportVectors_0(long nativeObj);


    //
    // C++:  TermCriteria getTermCriteria()
    //

    // C++: static Ptr_SVM create()
    private static native long create_0();


    //
    // C++:  double getC()
    //

    // C++:  TermCriteria getTermCriteria()
    private static native double[] getTermCriteria_0(long nativeObj);


    //
    // C++:  double getCoef0()
    //

    // C++:  double getC()
    private static native double getC_0(long nativeObj);


    //
    // C++:  double getDecisionFunction(int i, Mat& alpha, Mat& svidx)
    //

    // C++:  double getCoef0()
    private static native double getCoef0_0(long nativeObj);


    //
    // C++:  double getDegree()
    //

    // C++:  double getDecisionFunction(int i, Mat& alpha, Mat& svidx)
    private static native double getDecisionFunction_0(long nativeObj, int i, long alpha_nativeObj, long svidx_nativeObj);


    //
    // C++:  double getGamma()
    //

    // C++:  double getDegree()
    private static native double getDegree_0(long nativeObj);


    //
    // C++:  double getNu()
    //

    // C++:  double getGamma()
    private static native double getGamma_0(long nativeObj);


    //
    // C++:  double getP()
    //

    // C++:  double getNu()
    private static native double getNu_0(long nativeObj);


    //
    // C++:  int getKernelType()
    //

    // C++:  double getP()
    private static native double getP_0(long nativeObj);


    //
    // C++:  int getType()
    //

    // C++:  int getKernelType()
    private static native int getKernelType_0(long nativeObj);


    //
    // C++:  void setC(double val)
    //

    // C++:  int getType()
    private static native int getType_0(long nativeObj);


    //
    // C++:  void setClassWeights(Mat val)
    //

    // C++:  void setC(double val)
    private static native void setC_0(long nativeObj, double val);


    //
    // C++:  void setCoef0(double val)
    //

    // C++:  void setClassWeights(Mat val)
    private static native void setClassWeights_0(long nativeObj, long val_nativeObj);


    //
    // C++:  void setDegree(double val)
    //

    // C++:  void setCoef0(double val)
    private static native void setCoef0_0(long nativeObj, double val);


    //
    // C++:  void setGamma(double val)
    //

    // C++:  void setDegree(double val)
    private static native void setDegree_0(long nativeObj, double val);


    //
    // C++:  void setKernel(int kernelType)
    //

    // C++:  void setGamma(double val)
    private static native void setGamma_0(long nativeObj, double val);


    //
    // C++:  void setNu(double val)
    //

    // C++:  void setKernel(int kernelType)
    private static native void setKernel_0(long nativeObj, int kernelType);


    //
    // C++:  void setP(double val)
    //

    // C++:  void setNu(double val)
    private static native void setNu_0(long nativeObj, double val);


    //
    // C++:  void setTermCriteria(TermCriteria val)
    //

    // C++:  void setP(double val)
    private static native void setP_0(long nativeObj, double val);


    //
    // C++:  void setType(int val)
    //

    // C++:  void setTermCriteria(TermCriteria val)
    private static native void setTermCriteria_0(long nativeObj, int val_type, int val_maxCount, double val_epsilon);

    // C++:  void setType(int val)
    private static native void setType_0(long nativeObj, int val);

    // native support for java finalize()
    private static native void delete(long nativeObj);

    //javadoc: SVM::getClassWeights()
    public Mat getClassWeights() {

        Mat retVal = new Mat(getClassWeights_0(nativeObj));

        return retVal;
    }

    //javadoc: SVM::setClassWeights(val)
    public void setClassWeights(Mat val) {

        setClassWeights_0(nativeObj, val.nativeObj);

        return;
    }

    //javadoc: SVM::getSupportVectors()
    public Mat getSupportVectors() {

        Mat retVal = new Mat(getSupportVectors_0(nativeObj));

        return retVal;
    }

    //javadoc: SVM::getUncompressedSupportVectors()
    public Mat getUncompressedSupportVectors() {

        Mat retVal = new Mat(getUncompressedSupportVectors_0(nativeObj));

        return retVal;
    }

    //javadoc: SVM::getTermCriteria()
    public TermCriteria getTermCriteria() {

        TermCriteria retVal = new TermCriteria(getTermCriteria_0(nativeObj));

        return retVal;
    }

    //javadoc: SVM::setTermCriteria(val)
    public void setTermCriteria(TermCriteria val) {

        setTermCriteria_0(nativeObj, val.type, val.maxCount, val.epsilon);

        return;
    }

    //javadoc: SVM::getC()
    public double getC() {

        double retVal = getC_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::setC(val)
    public void setC(double val) {

        setC_0(nativeObj, val);

        return;
    }

    //javadoc: SVM::getCoef0()
    public double getCoef0() {

        double retVal = getCoef0_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::setCoef0(val)
    public void setCoef0(double val) {

        setCoef0_0(nativeObj, val);

        return;
    }

    //javadoc: SVM::getDecisionFunction(i, alpha, svidx)
    public double getDecisionFunction(int i, Mat alpha, Mat svidx) {

        double retVal = getDecisionFunction_0(nativeObj, i, alpha.nativeObj, svidx.nativeObj);

        return retVal;
    }

    //javadoc: SVM::getDegree()
    public double getDegree() {

        double retVal = getDegree_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::setDegree(val)
    public void setDegree(double val) {

        setDegree_0(nativeObj, val);

        return;
    }

    //javadoc: SVM::getGamma()
    public double getGamma() {

        double retVal = getGamma_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::setGamma(val)
    public void setGamma(double val) {

        setGamma_0(nativeObj, val);

        return;
    }

    //javadoc: SVM::getNu()
    public double getNu() {

        double retVal = getNu_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::setNu(val)
    public void setNu(double val) {

        setNu_0(nativeObj, val);

        return;
    }

    //javadoc: SVM::getP()
    public double getP() {

        double retVal = getP_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::setP(val)
    public void setP(double val) {

        setP_0(nativeObj, val);

        return;
    }

    //javadoc: SVM::getKernelType()
    public int getKernelType() {

        int retVal = getKernelType_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::getType()
    public int getType() {

        int retVal = getType_0(nativeObj);

        return retVal;
    }

    //javadoc: SVM::setType(val)
    public void setType(int val) {

        setType_0(nativeObj, val);

        return;
    }

    //javadoc: SVM::setKernel(kernelType)
    public void setKernel(int kernelType) {

        setKernel_0(nativeObj, kernelType);

        return;
    }

    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }

}
