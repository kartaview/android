package com.telenav.osv.manager.capture;

import android.database.sqlite.SQLiteConstraintException;
import android.location.Location;
import android.os.Handler;
import com.telenav.ffmpeg.FFMPEG;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.FrameSaveError;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.metadata.VideoData;
import com.telenav.osv.manager.location.SensorManager;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 15/02/2017.
 */
public class VideoSaver extends DataSaver {

    public final static String TAG = "VideoSaver";

    private FFMPEG ffmpeg;

    public VideoSaver(LocalSequence sequence, Handler handler) {
        this.mSequence = sequence;
        this.ffmpeg = new FFMPEG(new FFMPEG.ErrorListener() {

            @Override
            public void onError() {

            }
        });
        mBackgroundHandler = handler;

        int ret = -1;
        try {
            ret = ffmpeg.initial(sequence.getFolder().getPath() + "/");
        } catch (Exception ignored) {
        }
        if (ret != 0) {
            Log.e(TAG, "startSequence: could not create video file");
            Exception e = new Exception("Could not create video file. Try again please.");
            EventBus.post(new FrameSaveError(e, "Could not initialize/create mp4 file. Try again please."));
        }
    }

    @Override
    public void saveFrame(final byte[] jpegData, final Location mLocationF, final float mAccuracyF, final int mOrientationF,
                          final long mTimestampF) {
        super.saveFrame(jpegData, mLocationF, mAccuracyF, mOrientationF, mTimestampF);
        final int mIndexF = mIndex;
        mIndex++;
        mBackgroundHandler.post(new Runnable() {

            @Override
            public void run() {
                //mRunDetection = false;
                final long time = System.currentTimeMillis();
                int[] ret;
                ret = ffmpeg.encode(jpegData);
                Log.d(TAG, "saveFrame: encoding done in " + (System.currentTimeMillis() - time) + " ms ,  video file " + ret[0] + " and frame " +
                        ret[1]);
                //mRunDetection = true;

                if (ret[0] < 0 || ret[1] < 0) {
                    mIndex--;
                    EventBus.post(new ImageSavedEvent(mSequence, false));
                    if (ret[0] < 0) {
                        Exception e = new Exception("Could not initialize/create mp4 file. Try again please.");
                        EventBus.post(new FrameSaveError(e, "Could not initialize/create mp4 file. Try again please."));
                    }
                    return;
                }
                int mVideoIndexF = ret[0];
                SensorManager.logVideoData(new VideoData(mIndexF, mVideoIndexF, mTimestampF));
                try {
                    SequenceDB.instance
                            .insertVideoIfNotAdded(mSequence.getId(), mVideoIndexF, mSequence.getFolder().getPath() + "/" + mVideoIndexF + ".mp4");
                } catch (Exception ignored) {
                }

                try {
                    SequenceDB.instance
                            .insertPhoto(mSequence.getId(), mVideoIndexF, mIndexF, mSequence.getFolder().getPath() + "/" + mVideoIndexF + ".mp4",
                                    mLocationF.getLatitude(), mLocationF.getLongitude(), mAccuracyF, mOrientationF);
                } catch (final SQLiteConstraintException e) {
                    EventBus.post(new FrameSaveError(e, "Recording stopped because SQL"));
                }

                mSequence.setFrameCount(mSequence.getFrameCount() + 1);
                EventBus.post(new ImageSavedEvent(mSequence, true));
            }
        });
    }

    @Override
    public void finish() {
        if (ffmpeg != null) {
            ffmpeg.close();
        }
        mSequence = null;
    }
}
