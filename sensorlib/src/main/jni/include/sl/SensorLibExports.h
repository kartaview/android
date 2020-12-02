//
//  SensorLibExports.h
//  SensorLib
//
//  Created by Andrei Strugaru on 6/18/15.
//  Copyright (c) 2015 Telenav EU. All rights reserved.
//

#ifndef SensorLib_SensorLibExports_h
#define SensorLib_SensorLibExports_h

#include <opencv2/core/types.hpp>
#include <string>

/*! Sensor Library Namespace */
namespace SL
{
/*! Sign Category Enumeration */
/**
 *  Enumeration of known road sign categories.
 */
enum enSignCategory
{
    eSpeedLimitCategory = 0,
    eSpeedLimitUSCategory = 1,
    eTurnRestrictionsCategory = 2
};

/*! Sign Type Enumeration */
/**
 *  Enumeration of known road signs. 5 first bits are for specific sign, the rest are for the sign class
 */
enum enSignType
{
    eUndefinedSign = 0x0000,

    // no defined class for 1 << 5
    eStopSign = 0x0021,
    eGiveWay = 0x0022,
    eHwSign = 0x0024,
    eNoOvertakeCar = 0x0025,
    eNoOvertakeTruck = 0x0026,
    eNoEntry = 0x0027,
    eNoEntryTruck = 0x0028,

    eSpeedLimit = 1 << 6,
    eSpeedLimit5 = 0x0041,
    eSpeedLimit10 = 0x0042,
    eSpeedLimit20 = 0x0043,
    eSpeedLimit30 = 0x0044,
    eSpeedLimit40 = 0x0045,
    eSpeedLimit50 = 0x0046,
    eSpeedLimit60 = 0x0047,
    eSpeedLimit70 = 0x0048,
    eSpeedLimit80 = 0x0049,
    eSpeedLimit90 = 0x004A,
    eSpeedLimit100 = 0x004B,
    eSpeedLimit110 = 0x004C,
    eSpeedLimit120 = 0x004D,
    eSpeedLimit130 = 0x004E,

    eSpeedLimitUSConstruction = 1 << 7,
    eSpeedLimitUSConstruction25 = 0x0081,
    eSpeedLimitUSConstruction35 = 0x0082,
    eSpeedLimitUSConstruction40 = 0x0083,

    eSpeedLimitUS = 1 << 8,
    eSpeedLimit5US = 0x0101,
    eSpeedLimit10US = 0x0102,
    eSpeedLimit15US = 0x0103,
    eSpeedLimit20US = 0x0104,
    eSpeedLimit25US = 0x0105,
    eSpeedLimit30US = 0x0106,
    eSpeedLimit35US = 0x0107,
    eSpeedLimit40US = 0x0108,
    eSpeedLimit45US = 0x0109,
    eSpeedLimit50US = 0x010A,
    eSpeedLimit55US = 0x010B,
    eSpeedLimit60US = 0x010C,
    eSpeedLimit65US = 0x010D,
    eSpeedLimit70US = 0x010E,
    eSpeedLimit75US = 0x010F,
    eSpeedLimit80US = 0x0110,

    eRegulatorySign = 1 << 9,
    eRegLeft = 0x0201,
    eRegLeftNow = 0x0202,
    eRegRight = 0x0203,
    eRegRightNow = 0x0204,
    eRegLeftRight = 0x0205,
    eRegStraight = 0x0206,
    eRegStraightLeft = 0x0207,
    eRegStraightRight = 0x0208,

    eTurnRestriction = 1 << 10,
    eTurnRestrictionLeft = 0x0401,
    eTurnRestrictionRight = 0x0402,
    eTurnRestrictionUTurn = 0x0403,
    eTurnRestrictionLeftUTurn = 0x0404,

    eCanadaRegulatory = 1 << 11,
    eCanadaRegulatoryStraight = 0x0801,
    eCanadaRegulatoryLeft = 0x0802,
    eCanadaRegulatoryRight = 0x0803,
    eCanadaRegulatoryLeftRight = 0x0804,
    eCanadaRegulatoryStraightLeft = 0x0805,
    eCanadaRegulatoryStraightRight = 0x0806,
    eCanadaRegulatoryLast

};

/*! Tracked sign confidence */
struct DetectionConfidence
{
    DetectionConfidence() : confidence_(1.0), sign_type_(eUndefinedSign) {}

    operator bool() const { return sign_type_ != eUndefinedSign; }

    bool operator<(const DetectionConfidence& rhs) const { return confidence_ < rhs.confidence_; }

    /// 0 - perfect match, 1 - no confidence
    double confidence_;
    enSignType sign_type_;

    //!@brief Returns the maximum confidence level to be accepeted. Note: the best confidence level is 0.
    static double max_confidence_level() { return 0.5; }
};

/*! Tracked Sign structure */
/**
 *  Struct containing information about a recognised road sign.
 */
/// TODO: optimize layout
struct TrackedSignEx
{
    TrackedSignEx() : mPathToRoi(), mBBox(), detections_(), mStillActive(false) {}

    std::string mPathToRoi;                       /**< Path to the image containing the region of interest. */
    cv::Rect mBBox;                               /**< Sign bounding box. */
    std::vector<DetectionConfidence> detections_; /**< Sign type/confidence level present in the region of interest. */
    bool mStillActive;                            /**< Sign is active? */
};

/*! Settings structure */
/**
 *  Struct containing information about a recognised road sign.
 */
struct Settings
{
    Settings()
    {
        enabledSignCategories[0] = false;
        enabledSignCategories[1] = false;
        enabledSignCategories[2] = false;
    }

    bool enabledSignCategories[eTurnRestrictionsCategory + 1]; /**< Tracking enable flag for each sign category. */
};

enum enSettingsType
{
    eLastDefault = 0xFFFFF,
    eClientSetting = 0x0000,
    eServerSetting = 0x0001,
    eSLEUClient = 0x0010,
    eSLEUServer = 0x0011,
    eSLUSClient = 0x0020,
    eSLUSServer = 0x0021,
    eTurnUSClient = 0x0030,
    eTurnUSServer = 0x0031,
    eSVMEUClient = 0x0040,
    eSVMUSClient = 0x0041,
    eSVMRussianClient = 0x0042,
    eNNRussianClient = 0x0043,
    eSVM_MSER_EU_Client = 0x0044,
    eStopSignClient = 0x0050,
    eStopSignServer = 0x0051,
    eRegulatoryEUClient = 0x0060,
    eRegulatoryEUServer = 0x0061,
    eRegulatoryCanadaClient = 0x0070,
    eRegulatoryCanadaServer = 0x0071,
};

//! Color Space Transformation
enum enColorSpace
{
    eUndefinedColorSpace = 0x0000,
    eRGBHSV = 0x0002,
    eBGRHSV = 0x0003,
    eYUV = 0x0400,
    eYUV420spGRAY = 0x0800, // eYUVGRAY and eYUV420spGRAY should colide into YUV space
    eYUVGRAY = 0x0900,
    eBGRGRAY = 0x1000
};

//! Filter Color Types
enum enDetectionFilterType
{
    eRed = 0x0001,
    eBlue = 0x0002,
    eGreen = 0x0003,
    eWhite = 0x0004,
    eBlueWhite = 0x0005,
    eOrange = 0x0006,
    eRadialSymmetry = 0x0007,
    eReversedRadialSymmetry = 0x0008,
    eMSER = 0x0009,
    eUndefinedColor = 0xFFFF
};

//! Classifier Types
enum enClassifierType
{
    eUndefinedClassifier,
    eGiveWayClassifier,
    eHighwaySignClassifier,
    eRegulatoryDirectionClassifier,
    eCanadaRegulatoryDirectionClassifier,
    eSpeedLimitClassifierEU,
    eSpeedLimitConstructionClassifierUS,
    eSpeedLimitClassifierUS,
    eStopSignClassifier,
    eTurnRestrictionClassifier,
    eSVMClassifierEU,
    eSVMClassifierUS,
    eCaffeEUClassifier
};

//! Callback for requesting a raw frame.
typedef void (*RequestFrameCallbackType)(cv::Mat&);

//! Callback for returning a processed frame.
typedef void (*FrameCallbackType)(const cv::Mat&);

//! Callback for returning a detected sign.
typedef void (*SignDetectedCallbackType)(const TrackedSignEx);
}

#endif
