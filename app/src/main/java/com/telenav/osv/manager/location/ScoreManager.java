package com.telenav.osv.manager.location;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraShutterEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.network.matcher.MatchedSegmentEvent;
import com.telenav.osv.event.network.matcher.ScoreChangedEvent;
import com.telenav.osv.event.ui.GamificationSettingEvent;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Component responsible for points calculation
 * Created by Kalman on 17/11/2016.
 */

public class ScoreManager {

    private static final String TAG = "ScoreManager";

    private Polyline mCurrentSegment;

    private float mPoints;

    private Sequence mSequence;

    private boolean mEnabled = true;

    public ScoreManager(boolean enabled) {
        mEnabled = enabled;
        EventBus.register(this);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPictureTaken(CameraShutterEvent event) {
        if (mSequence != null && mEnabled) {
            final boolean obd = ObdManager.isConnected();
            final int coverage = mCurrentSegment == null ? -1 : mCurrentSegment.coverage;
            int cappedCoverage = Math.min(coverage, 10);
            final int seqId = mSequence.sequenceId;
            if (coverage >= 0) {
                int segmentValue = Utils.getValueOnSegment(cappedCoverage);
                Log.d(TAG, "onPictureTaken: seqId = " + mSequence.sequenceId + " matched segment " + (mCurrentSegment == null ? 0 : mCurrentSegment.getIdentifier()) + " coverage" +
                        " " + coverage + " obd : " + obd);
                SequenceDB.instance.insertScore(seqId, obd, cappedCoverage);
                mPoints = mPoints + ((obd ? 2 : 1) * segmentValue);
                mSequence.score = (int) mPoints;
                Log.d(TAG, "onPictureTaken: score changed called 1");
                EventBus.post(new ScoreChangedEvent(mPoints, obd, segmentValue));
            } else {
                Log.d(TAG, "onPictureTaken: seqId = " + mSequence.sequenceId + " matched segment " + (mCurrentSegment == null ? 0 : mCurrentSegment.getIdentifier()) + " coverage" +
                        " " + coverage + " obd : " + obd);
                SequenceDB.instance.insertScore(seqId, obd, cappedCoverage);
                Log.d(TAG, "onPictureTaken: score changed called 2 no coverage");
                EventBus.post(new ScoreChangedEvent(mPoints, obd, -1));
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onMatchedSegment(MatchedSegmentEvent event) {
        if (mEnabled) {
            mCurrentSegment = event.polyline;
            final int coverage = mCurrentSegment == null ? -1 : mCurrentSegment.coverage;
            int cappedCoverage = Math.min(coverage, 10);
            int segmentValue = Utils.getValueOnSegment(cappedCoverage);
            final boolean obd = ObdManager.isConnected();
            Log.d(TAG, "onMatchedSegment: score changed called");
            EventBus.post(new ScoreChangedEvent(mPoints, obd, segmentValue));
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onRecordingStarted(RecordingEvent event) {
        if (event.started) {
            mPoints = 0;
            mSequence = event.sequence;
        } else {
            mSequence = null;
            mPoints = 0;
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGamificationEnabled(GamificationSettingEvent event) {
        this.mEnabled = event.enabled;
    }
}
