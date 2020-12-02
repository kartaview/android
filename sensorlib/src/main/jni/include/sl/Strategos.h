//
//  Strategos.h
//  SensorLib
//
//  Created by Andrei Strugaru on 5/28/15.
//  Copyright (c) 2015 Telenav EU. All rights reserved.
//

#ifndef SensorLib_Strategos
#define SensorLib_Strategos

namespace SL
{

class Strategos
{

  public:
    /// TODO: bitset or not?
    enum eRecordingExternalInputs
    {
        reiNAN = 0x00,
        reiStartRecording = 0x01,
        reiEndRecording = 0x03,
        reiEnablePostRecording = 0x08
    };

    /// TODO: bitset or not?
    enum eRecordingInternalInputs
    {
        riiNAN = 0x00,
        riiRecordingStarted = 0x01,
        riiRecordingStopped = 0x02
    };

    enum eRecordingStates
    {
        rstIdle = 0x00,
        rstStartRecording = 0x01,
        rstRecord = 0x02,
        rstEndRecording = 0x04,
        rstPostRecording = 0x08,
        rstStartandPost = rstStartRecording | rstPostRecording,
        rstRecordandPost = rstRecord | rstPostRecording,
        rstEndandPost = rstEndRecording | rstPostRecording
    };

    /// TODO: bitset or not?
    enum ePlaybackExternalInputs
    {
        peiNAN = 0x00,
        peiStartPlayback = 0x01,
        peiEndPlayback = 0x02
    };

    /// TODO: bitset or not?
    enum ePlaybackInternalInputs
    {
        piiNAN = 0x00,
        piiPlaybackStarted = 0x01,
        piiPlaybackEnded = 0x02,
        piiEOF = 0x03
    };

    /// TODO: bitset or not?
    enum ePlaybackStates
    {
        pstIdle = 0x00,
        pstStartPlayback = 0x01,
        pstPlayback = 0x02,
        pstEndPlayback = 0x03,
        pstPausePlayback = 0x04

    };

    /// TODO: bitset or not?
    enum eProcessingExternalInputs
    {
        preiNAN = 0x00,
        preiColorSeparation = 0x01,
        preiRadialSimetry = 0x02,
        preiMSER = 0x03,
        preiMixed = 0x04
    };

    /// TODO: bitset or not?
    enum eProcessingInternalInputs
    {
        priiNAN = 0x00,
        priiColorSeparation = 0x01,
        priiRadialSimetry = 0x02,
        priiMSER = 0x04
    };

    /// TODO: bitset or not?
    enum eProcessingState
    {
        prstIdle = 0x00,
        prstColorSeparation = 0x01,
        prstRadialSimetry = 0x02,
        prstMSER = 0x04
    };

    template <typename E, typename I> struct StateShift
    {
        E externalInput;
        I internalInput;

        StateShift(E extI)
        {
            externalInput = extI;
            internalInput = static_cast<I>(0x00);
        }

        StateShift(I intI)
        {
            externalInput = static_cast<E>(0x00);
            internalInput = intI;
        }

        StateShift(E extI, I intI)
        {
            externalInput = extI;
            internalInput = intI;
        }
    };

    typedef StateShift<eRecordingExternalInputs, eRecordingInternalInputs> RecordingStateShift;
    typedef StateShift<ePlaybackExternalInputs, ePlaybackInternalInputs> PlaybackStateShift;
    typedef StateShift<eProcessingExternalInputs, eProcessingInternalInputs> ProcessingStateShift;

    Strategos();

    void RecordingFSM(RecordingStateShift stateShift);
    void PlaybackFSM(PlaybackStateShift stateShift);
    void ProcessingFSM(ProcessingStateShift stateShift);

    void externalRecordingEvent(eRecordingExternalInputs newInput);
    bool checkRecordingState(eRecordingStates checkState);
    bool checkProcessingState(eProcessingState checkState);

  protected:
    void startRecording(eRecordingInternalInputs newInput);

    eRecordingStates mRecordingState;
    ePlaybackStates mPlaybackState;
    eProcessingState mProcessingState;

    eRecordingStates getStateWithPost(eRecordingStates newState);
};
}

#endif /* defined(SensorLib_Strategos) */
