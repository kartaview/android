package com.telenav.osv.manager.capture;

import java.io.FileOutputStream;
import java.io.IOException;
import android.database.sqlite.SQLiteConstraintException;
import android.location.Location;
import android.os.Handler;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.FrameSaveError;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.metadata.VideoData;
import com.telenav.osv.manager.location.SensorManager;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 15/02/2017.
 */
public class PhotoSaver extends DataSaver {

    public final static String TAG = "PhotoSaver";

    public PhotoSaver(LocalSequence sequence, Handler handler) {
        this.mSequence = sequence;
        this.mBackgroundHandler = handler;
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
                String path = mSequence.getFolder().getPath() + "/" + mIndexF + ".jpg";
                String tmpPath = path + ".tmp";
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(tmpPath);
                    out.write(jpegData);
                    out.close();
                    OSVFile jpg = new OSVFile(path);
                    OSVFile tmpFile = new OSVFile(tmpPath);
                    tmpFile.renameTo(jpg);
                    Log.v(TAG, "Saved JPEG data : " + jpg.getName() + ", size: " + ((float) jpg.length()) / 1024f / 1024f + " mb");

                    try {
                        SequenceDB.instance
                                .insertPhoto(mSequence.getId(), -1, mIndexF, jpg.getPath(), mLocationF.getLatitude(), mLocationF.getLongitude(), mAccuracyF,
                                        mOrientationF);

                        SensorManager.logVideoData(new VideoData(mIndexF, 0, mTimestampF));
                    } catch (SQLiteConstraintException e) {
                        jpg.delete();
                        Log.w(TAG, "saveFrame: " + Log.getStackTraceString(e));
                        mIndex--;
                        EventBus.post(new ImageSavedEvent(mSequence, false));
                        EventBus.post(new FrameSaveError(e, "SQL constraint for jpeg"));
                    }
                } catch (IOException e) {
                    mIndex--;
                    EventBus.post(new ImageSavedEvent(mSequence, false));
                    Log.e(TAG, "Failed to write image", e);
                }
                mSequence.setFrameCount(mSequence.getFrameCount() + 1);
                EventBus.post(new ImageSavedEvent(mSequence, true));
            }
        });
    }

    @Override
    public void finish() {
        mSequence = null;
    }
}
