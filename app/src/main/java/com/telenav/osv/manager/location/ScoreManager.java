package com.telenav.osv.manager.location;

import android.location.Location;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.command.BroadcastSegmentsCommand;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.network.matcher.MatchedSegmentEvent;
import com.telenav.osv.event.network.matcher.ScoreChangedEvent;
import com.telenav.osv.item.Segment;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Component responsible for points calculation
 * Created by Kalman on 17/11/2016.
 */
public class ScoreManager implements PositionMatcher.SegmentsListener {

  private static final String TAG = "ScoreManager";

  private final PositionMatcher mPositionMatcher;

  private final SequenceDB mSequenceDB;

  private float mPoints;

  private Sequence mSequence;

  private boolean mEnabled = true;

  private boolean mMatchLocations = true;

  private boolean obdConnected;

  @Inject
  public ScoreManager(PositionMatcher matcher, SequenceDB db) {
    mPositionMatcher = matcher;
    mSequenceDB = db;
    mPositionMatcher.setListener(this);
    EventBus.register(this);
  }

  public void setEnabled(boolean enabled) {
    this.mEnabled = enabled;
  }

  public boolean isEnabled() {
    return mEnabled;
  }

  public void setObdConnected(boolean obdConnected) {
    this.obdConnected = obdConnected;
  }

  public void onPictureTaken(Location location) {
    if (mSequence != null && mEnabled) {
      Segment matchedSegment = mPositionMatcher.onPictureTaken(location);
      EventBus.postSticky(new MatchedSegmentEvent(matchedSegment));
      updateScore(matchedSegment, true);
    }
  }

  private void updateScore(Segment matchedSegment, boolean incrementScore) {
    final boolean obd = obdConnected;
    final int coverage = matchedSegment == null ? -1 : matchedSegment.getPolyline().coverage;
    int cappedCoverage = Math.min(coverage, 10);
    final int seqId = mSequence != null ? mSequence.getId() : -1;
    int segmentValue = Utils.getValueOnSegment(cappedCoverage);
    if (incrementScore && mSequence != null) {
      if (coverage >= 0) {
        Log.d(TAG, "onPictureTaken: seqId = " + mSequence.getId() + " matched segment " + matchedSegment +
            " coverage" + " " + coverage + " obd : " + obd);
        mSequenceDB.insertScore(seqId, obd, cappedCoverage);
        mPoints = mPoints + ((obd ? 2 : 1) * segmentValue);
        mSequence.setScore((int) mPoints);
        Log.d(TAG, "onPictureTaken: score changed called 1");
      } else {
        Log.d(TAG, "onPictureTaken: seqId = " + mSequence.getId() + " matched segment " +
            (matchedSegment == null ? 0 : matchedSegment.getPolyline().getIdentifier()) + " coverage" + " " + coverage + " obd : " + obd);
        mSequenceDB.insertScore(seqId, obd, cappedCoverage);
        Log.d(TAG, "onPictureTaken: score changed called 2 no coverage");
      }
    }
    EventBus.post(new ScoreChangedEvent(mPoints, obd, segmentValue));
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
  public void onRecordingStarted(RecordingEvent event) {
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

  public void onLocationChanged(Location location) {
    if (mMatchLocations) {
      Segment segment = mPositionMatcher.onLocationChanged(location);
      if (segment != null) {
        EventBus.postSticky(new MatchedSegmentEvent(segment));
        updateScore(segment, false);
      }
    }
  }

  @Subscribe
  public void onBroadcastSegmentsCommand(BroadcastSegmentsCommand command) {
    if (mPositionMatcher != null) {
      mPositionMatcher.broadcastSegments();
    }
  }

  @Override
  public void onSegmentsReceived(SKCoordinate location) {
    Segment matchedSegment = mPositionMatcher.match(location);
    EventBus.postSticky(new MatchedSegmentEvent(matchedSegment));
    updateScore(matchedSegment, false);
  }
}
