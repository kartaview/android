//
//  SensorLib.h
//  SensorLib
//
//  Created by Andrei Strugaru on 10/14/14.
//  Copyright (c) 2014 Telenav EU. All rights reserved.
//

#ifndef SensorLib_SensorLib
#define SensorLib_SensorLib

#include "SensorLibExports.h"

#include <map>
#include <vector>

#include "utils/visibility.h"

#ifdef __GNUC__
#define DEPRECATED __attribute__((deprecated))
#elif defined(_MSC_VER)
#define DEPRECATED __declspec(deprecated)
#else
#pragma message("WARNING: You need to implement DEPRECATED for this compiler")
#define DEPRECATED
#endif

/*! Sensor Library Namespace */
namespace SL
{
class SensorLibImpl;

/*! The main class of the Sensor Library. */
/**
 *  class SensorLib sums up all the APIs for interfacing the Sensor Library.
 */
class ORBB_EXPORTS SensorLib
{
  public:
    /** \addtogroup SensorLib_Management
     *  @{
     */
    //! ctor
    /**
     *  SensorLib constructor.
     */
    explicit SensorLib(const std::string& tessdataPath = "", const enSettingsType new_settings = eLastDefault);

    //! dtor
    /**
     *  SensorLib destructor.
     */
    ~SensorLib();

    //! Initialisation method
    /**
     *  \return a boolean value representing the success of the operation.
     */
    static bool init();

    //! Initialisation method
    /**
     *  \param pathToTemplates - the path to the template files.
     *  \return a boolean value representing the success of the operation.
     */
    static bool init(const std::string& pathToTemplates);

    //! Initialisation method
    /**
     *  \param mapTemplates - a map containing the already loaded templates.
     *  \return a boolean value representing the success of the operation.
     */
    static bool init(const std::map<enSignType, cv::Mat>& mapTemplates);
    /** @} */

    /** \addtogroup SensorLib_Road_Sign_Tracking
     *  @{
     */
    //! Start tracking road signs.
    /**
     *  \param frameProvider - a pointer to a function that will be responsible with providing frames.
     *  \param resultReceiver - a pointer to a function that will receive the processed frame.
     *  \sa stopTracking(), trackSignType() and isTrackingSigns().
     */
    void startTracking(const RequestFrameCallbackType frameProvider, const FrameCallbackType resultReceiver = NULL);

    //! Stop road sign tracking.
    /**
     *  \sa startTracking(), trackSignType() and isTrackingSigns().
     */
    void stopTracking();

    //! Set the types of signs that are to be tracked. !!! UNIMPLEMENTED - all signs are tracked at the moment. !!!
    /**
     *  \param signCategory - type of sign to track/ignore.
     *  \param enable - a boolean value that enables/disables tracking for the specified road sign type.
     *  \sa startTracking(), stopTracking() and isTrackingSigns().
     */
    void trackSignType(const enSignCategory signCategory, const bool enable);

    //! Tracking activity query
    /**
     *  \return a boolean value representing the current tracking activity status.
     *  \sa startTracking(), stopTracking() and trackSignType().
     */
    bool isTrackingSigns() const;
    /** @} */

    /** \addtogroup SensorLib_UNDOCUMENTED
     *  @{
     */
    //! UNDOCUMENTED
    /**
     *  \return UNDOCUMENTED
     */
    static std::map<enSignType, std::string> getFileNames();

    //! UNDOCUMENTED
    /**
     *  \param frame - UNDOCUMENTED.
     *  \param retroFrame - UNDOCUMENTED.
     */
    // Deprecated please use processFrame
    DEPRECATED void applyToPhoto(const cv::Mat& frame, cv::Mat& retroFrame);

    //! New variant of applyToPhoto.
    /**
     *  \param frame - UNDOCUMENTED.
     *  \param retroFramPtr - UNDOCUMENTED.
     *  \param validSignsPtr - UNDOCUMENTED.
     */
    void processFrame(const cv::Mat& frame, cv::Mat* retroFramPtr = NULL,
                      std::vector<TrackedSignEx>* validSignsPtr = NULL);

    //! UNDOCUMENTED
    /**
     *  \param buff - UNDOCUMENTED.
     *  \param width - UNDOCUMENTED.
     *  \param height - UNDOCUMENTED.
     *  \param retroFrame - UNDOCUMENTED
     */
    void applyToYUV420SP(const unsigned char* buff, const int width, const int height, cv::Mat& retroFrame);

    //! UNDOCUMENTED
    /**
     *  \param videoPath - UNDOCUMENTED.
     */
    void playbackVideo(const std::string& videoPath);

    //! UNDOCUMENTED
    /**
     *  \return UNDOCUMENTED
     */
    bool startSession();

    //! UNDOCUMENTED
    /**
     *  \return UNDOCUMENTED
     */
    bool endSession();

    //! UNDOCUMENTED
    /**
     *  \return UNDOCUMENTED
     */
    std::vector<cv::Mat> getROIS();

    //! UNDOCUMENTED
    /**
     *  \return UNDOCUMENTED
     */
    std::vector<cv::Rect> getBBoxes();
    /** @} */

  private:
    SensorLibImpl* mImpl;
};
}

#endif /* defined(SensorLib_SensorLib) */
