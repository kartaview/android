//
//  VideoTools.h
//  SensorLib
//
//  Created by Andrei Strugaru on 5/28/15.
//  Copyright (c) 2015 Telenav EU. All rights reserved.
//

#ifndef SensorLib_VideoTools
#define SensorLib_VideoTools

#include <opencv2/core/types.hpp>
#include <string>

namespace cv
{
    class VideoCapture;
    class VideoWriter;
}

namespace SL
{
class VideoTools
{
    cv::VideoWriter* mVideoWriter;
    cv::VideoCapture* mVideoCapture;

    std::string mCurrentRecordingPath;
    std::string mCurrentPlaybackPath;
    bool mAppendProcessing;

  public:
    VideoTools(bool bProcessed = false);
    ~VideoTools();

    bool startRecording(cv::Size framesize);
    void stopRecording();
    bool recordFrame(cv::Mat frame);
    bool startPlayback(const std::string& videoPath);
    bool readNextFrame(cv::Mat& frame);
    bool endPlayback();
    static std::string saveMatToFile(time_t creationTime, cv::Mat toSave);
};
}

#endif /* defined(SensorLib_VideoTools) */
