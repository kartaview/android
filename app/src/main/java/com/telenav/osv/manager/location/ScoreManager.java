package com.telenav.osv.manager.location;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.location.Location;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.command.BroadcastSegmentsCommand;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.network.matcher.MatchedSegmentEvent;
import com.telenav.osv.event.network.matcher.ScoreChangedEvent;
import com.telenav.osv.event.ui.ByodDriverPayRateUpdatedEvent;
import com.telenav.osv.event.ui.GamificationSettingEvent;
import com.telenav.osv.item.Segment;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.network.PayRateData;
import com.telenav.osv.item.network.PayRateItem;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Component responsible for points calculation, while recording takes place
 * Created by Kalman on 17/11/2016.
 */
public class ScoreManager implements PositionMatcher.SegmentsListener {

    public static final float PAY_RATE_UNKNOWN = -1f;

    private static final String TAG = "ScoreManager";

    private final PositionMatcher mPositionMatcher;

    private float mPoints;

    private Sequence mSequence;

    private boolean mEnabledForNormalUser = true;

    private boolean mMatchLocations = true;

    private boolean mEnabledForByodDriver = false;

    private PayRateData mPayRateData;

    public ScoreManager(Context context) {
        mPositionMatcher = new PositionMatcher(context, this);
        EventBus.register(this);
    }

    @Override
    public void onSegmentsReceived(SKCoordinate location) {
        Log.d(TAG, "ScoreManager#onSegmentsReceived");
        Segment matchedSegment = mPositionMatcher.match(location);
        EventBus.postSticky(new MatchedSegmentEvent(matchedSegment));
        updateScore(matchedSegment, false);
    }

    public void setEnabledForByodDriver(boolean enabledForByodDriver) {
        Log.d(TAG, "ScoreManager#setEnabledForByodDriver: " + enabledForByodDriver);
        mEnabledForByodDriver = enabledForByodDriver;
    }

    public void setEnabledForNormalUser(boolean enabledForNormalUser) {
        Log.d(TAG, "ScoreManager#setEnabledForNormalUser: " + enabledForNormalUser);
        mEnabledForNormalUser = enabledForNormalUser;
    }

    public void onPictureTaken(Location location) {
        Log.d(TAG, "ScoreManager#onPictureTaken");
        if (mSequence != null && (mEnabledForNormalUser || mEnabledForByodDriver)) {
            Segment matchedSegment = mPositionMatcher.onPictureTaken(location);
            EventBus.postSticky(new MatchedSegmentEvent(matchedSegment));
            updateScore(matchedSegment, true);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onRecordingStarted(RecordingEvent event) {
        Log.d(TAG, "ScoreManager#onRecordingStarted");

        if (event.started) {
            mPoints = 0;
            mSequence = event.sequence;
            mPositionMatcher.broadcastSegments();
            mMatchLocations = false;
        } else {
            mSequence = null;
            mPoints = 0;
            mMatchLocations = true;
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGamificationEnabled(GamificationSettingEvent event) {
        Log.d(TAG, "ScoreManager#onGamificationEnabled. event.enabled: " + event.enabled);
        setEnabledForNormalUser(event.enabled);
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "ScoreManager#onLocationChanged");
        if (mMatchLocations) {
            Segment segment = mPositionMatcher.onLocationChanged(location);
            if (segment != null) {
                EventBus.postSticky(new MatchedSegmentEvent(segment));
                if (mEnabledForByodDriver || mEnabledForNormalUser) {
                    updateScore(segment, false);
                }
            }
        }
    }

    @Subscribe
    public void onBroadcastSegmentsCommand(BroadcastSegmentsCommand command) {
        Log.d(TAG, "ScoreManager#onBroadcastSegmentsCommand");

        if (mPositionMatcher != null) {
            mPositionMatcher.broadcastSegments();
        }
    }

    public PayRateData getPayRateData() {
        return mPayRateData;
    }

    public void setPayRateData(PayRateData payRateData) {
        this.mPayRateData = payRateData;
    }

    private void updateScore(Segment matchedSegment, boolean incrementScore) {
        Log.d(TAG, "ScoreManager#updateScore. incrementScore: " + incrementScore);

        final boolean obd = ObdManager.isConnected();
        final int coverage = matchedSegment == null ? -1 : matchedSegment.getPolyline().coverage;
        if (mEnabledForByodDriver) {
            if (mPayRateData != null && mPayRateData.getPayRates() != null) {
                float payRateForCurrentSegment = PAY_RATE_UNKNOWN;
                for (PayRateItem payRateItem : mPayRateData.getPayRates()) {
                    if ((payRateItem.payRateCoverageInterval.minPass <= coverage && payRateItem.payRateCoverageInterval.maxPass >= coverage) ||
                            (payRateItem.payRateCoverageInterval.maxPass < coverage && mPayRateData.getPayRates().indexOf(payRateItem) == mPayRateData.getPayRates().size() - 1)) {
                        if (obd) {
                            payRateForCurrentSegment = payRateItem.obdPayRateValue;
                        } else {
                            payRateForCurrentSegment = payRateItem.nonObdPayRateValue;
                        }
                    }
                }
                Log.d(TAG, "ScoreManager#updateScore. byod pay rate for current segment is: " + payRateForCurrentSegment);

                EventBus.post(new ByodDriverPayRateUpdatedEvent(payRateForCurrentSegment, mPayRateData.getCurrency()));
            }
        } else if (mEnabledForNormalUser) {
            int cappedCoverage = Math.min(coverage, 10);
            final int seqId = mSequence != null ? mSequence.getId() : -1;
            int segmentValue = Utils.getValueOnSegment(cappedCoverage);
            if (incrementScore && mSequence != null) {
                if (coverage >= 0) {
                    Log.d(TAG, "onPictureTaken: seqId = " + mSequence.getId() + " matched segment " + matchedSegment.getPolyline().getIdentifier() +
                            " coverage" + " " + coverage + " obd : " + obd);
                    SequenceDB.instance.insertScore(seqId, obd, cappedCoverage);
                    mPoints = mPoints + ((obd ? 2 : 1) * segmentValue);
                    mSequence.setScore((int) mPoints);
                    Log.d(TAG, "onPictureTaken: score changed called 1");
                } else {
                    Log.d(TAG, "onPictureTaken: seqId = " + mSequence.getId() + " matched segment " +
                            (matchedSegment == null ? 0 : matchedSegment.getPolyline().getIdentifier()) + " coverage" + " " + coverage + " obd : " + obd);
                    SequenceDB.instance.insertScore(seqId, obd, cappedCoverage);
                    Log.d(TAG, "onPictureTaken: score changed called 2 no coverage");
                }
            }
            EventBus.post(new ScoreChangedEvent(mPoints, obd, segmentValue));
        }
    }
}
