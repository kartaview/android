package com.telenav.osv.item;

import android.database.Cursor;
import android.os.Build;
import android.os.Looper;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * local sequence class, representing sequences stored on device
 * Created by kalmanb on 7/11/17.
 */
public class LocalSequence extends Sequence {

  public static final int STATUS_NEW = 0;

  public static final int STATUS_INDEXING = 1;

  public static final int STATUS_UPLOADING = 2;

  public static final int STATUS_INTERRUPTED = 3;

  private static final String TAG = "LocalSequence";

  private static final ConcurrentHashMap<Integer, LocalSequence> sequences = new ConcurrentHashMap<>();

  private static boolean sInitialized = false;

  private int mOnlineId;

  private OSVFile mFolder;

  private boolean mIsExternal = false;

  private long mOriginalSize = 0;

  private long mSize = 0;

  private boolean mIsSafe;

  private int mVideoCount = -1;

  public LocalSequence(OSVFile folder) {
    this.mFolder = folder;
    this.mId = getSequenceId(folder);
    this.mDate = new Date(folder.lastModified());

    refreshStats();
    synchronized (sequences) {
      sequences.put(mId, this);
    }
    EventBus.post(new SequencesChangedEvent(false));
    this.mAddress = "Track " + (mId + 1);
    Log.d(TAG, "LocalSequence: " + this.toString());
  }

  public static ConcurrentHashMap<Integer, LocalSequence> forceRefreshLocalSequences() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      Log.e(TAG, "getLocalSequences called on main thread ");
      throw new IllegalStateException("GetLocalSequences called on main thread.");
    }
    ConcurrentHashMap<Integer, LocalSequence> tempSequences = new ConcurrentHashMap<>();
    synchronized (sequences) {
      Cursor cur = SequenceDB.instance.getAllSequences();
      while (cur != null && !cur.isAfterLast()) {
        OSVFile f = new OSVFile(cur.getString(cur.getColumnIndex(SequenceDB.SEQUENCE_PATH)));
        boolean external = cur.getInt(cur.getColumnIndex(SequenceDB.SEQUENCE_EXTERNAL)) > 0;
        Integer id = cur.getInt(cur.getColumnIndex(SequenceDB.SEQUENCE_ID));
        if (!sequences.containsKey(id)) {
          LocalSequence seq = new LocalSequence(f);
          seq.mIsExternal = external;
          double lat = cur.getDouble(cur.getColumnIndex(SequenceDB.SEQUENCE_LAT));
          double lon = cur.getDouble(cur.getColumnIndex(SequenceDB.SEQUENCE_LON));
          seq.mLocation.setLatitude(lat);
          seq.mLocation.setLongitude(lon);
          if (SKReverseGeocoderManager.getInstance() != null && lat != 0 && lon != 0) {
            SKSearchResult address = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(new SKCoordinate(lat, lon));
            if (address != null) {
              seq.mAddress = address.getName();
            }
          }
          seq.mIsSafe = cur.getInt(cur.getColumnIndex(SequenceDB.SEQUENCE_SAFE)) > 0;
          Log.d(TAG, "getLocalSequences: reverseGeocode: " + seq.mAddress);
          tempSequences.put(seq.mId, seq);
        } else {
          tempSequences.put(id, sequences.get(id));
        }
        cur.moveToNext();
      }
      if (cur != null) {
        cur.close();
      }
      sequences.clear();
      for (LocalSequence seq : tempSequences.values()) {
        sequences.put(seq.mId, seq);
      }
      sInitialized = true;
      return sequences;
    }
  }

  public static void order(List<LocalSequence> m) {

    Collections.sort(m, (rhs, lhs) -> (int) ((rhs.getStatus() * 1000 + rhs.mFolder.lastModified()) -
                                                 (lhs.getStatus() * 1000 + lhs.mFolder.lastModified())));
  }

  /**
   * used only for the delete of OSVFile
   *
   * @return the local sequence map
   */
  public static ConcurrentHashMap<Integer, LocalSequence> getStaticSequences() {
    if (!sInitialized && Looper.myLooper() != Looper.getMainLooper()) {
      return forceRefreshLocalSequences();
    }
    return sequences;
  }

  public static Integer getSequenceId(OSVFile f) {
    int result = -1;
    try {
      result = Integer.valueOf(f.getName().split("_")[1]);
    } catch (Exception e) {
      Log.w(TAG, "getSequenceId: " + e.getLocalizedMessage());
    }
    return result;
  }

  public static void deleteSequence(int id) {
    SequenceDB.instance.deleteRecords(id);
    synchronized (sequences) {
      Log.d(TAG, "deleteSequence: removed seq with id " + id);
      sequences.remove(id);
    }
  }

  public static int getLocalSequencesNumber() {
    return sequences.size();
  }

  public static double getTotalSize() {
    double totalSize = 0;
    for (LocalSequence s : sequences.values()) {
      totalSize = totalSize + s.mSize;
    }
    return totalSize;
  }

  public static int checkDeletedSequences() {
    if (sequences != null && sequences.size() > 0) {
      for (LocalSequence s : sequences.values()) {
        if (!SequenceDB.instance.checkSequenceExists(s.mId)) {
          deleteSequence(s.mId);
        }
      }
      return sequences.size();
    }
    return 0;
  }

  public int getOnlineId() {
    return mOnlineId;
  }

  public void setOnlineId(int mOnlineId) {
    this.mOnlineId = mOnlineId;
  }

  public OSVFile getFolder() {
    return mFolder;
  }

  public void setFolder(OSVFile mFolder) {
    this.mFolder = mFolder;
  }

  public boolean isExternal() {
    return mIsExternal;
  }

  public void setExternal(boolean external) {
    this.mIsExternal = external;
  }

  public long getOriginalSize() {
    return mOriginalSize;
  }

  public long getSize() {
    return mSize;
  }

  public void setSize(long size) {
    this.mSize = size;
  }

  public int getVideoCount() {
    return mVideoCount;
  }

  public void setVideoCount(int count) {
    this.mVideoCount = count;
  }

  public void decreaseVideoCount() {
    mVideoCount--;
    mVideoCount = Math.max(0, mVideoCount);
  }

  public void refreshStats() {
    this.mSize = Utils.folderSize(mFolder);
    this.mOriginalSize = mSize;
    this.mPolyline = new Polyline(mId);
    this.mPolyline.isLocal = true;
    this.mPlatform = "Android";
    this.mPlatformVersion = Build.VERSION.RELEASE;
    this.mAppVersion = SequenceDB.instance.getSequenceVersion(mId);
    this.mOriginalFrameCount = SequenceDB.instance.getOriginalFrameCount(mId);
    this.mVideoCount = (int) SequenceDB.instance.getNumberOfVideos(mId);

    this.mIsSafe = SequenceDB.instance.isSequenceSafe(mId);
    Cursor scores = null;
    try {
      scores = SequenceDB.instance.getScores(mId);
      mScoreHistory.clear();
      while (scores != null && !scores.isAfterLast()) {
        int coverage = Math.min(scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_COVERAGE)), 10);
        int obdCount = scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_OBD_COUNT));
        int count = scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_COUNT));
        mScoreHistory.put(coverage, new ScoreHistory(coverage, count, obdCount));
        scores.moveToNext();
      }
      if (scores != null) {
        scores.close();
      }
    } catch (Exception e) {
      Log.w(TAG, "refreshStats: " + Log.getStackTraceString(e));
      if (scores != null) {
        scores.close();
      }
    }
    this.value = calculateScore();
    try {
      Cursor records = SequenceDB.instance.getFrames(mId);

      if (records != null && records.getCount() > 0) {
        try {
          this.mThumbLink = "file:///" + records.getString(records.getColumnIndex(SequenceDB.FRAME_FILE_PATH));
          this.mFrameCount = records.getCount();
        } catch (Exception e) {
          Log.w(TAG, "LocalSequence: " + e.getLocalizedMessage());
        }
        Log.d(TAG, "refreshStats: sequence " + mId + ", count: " + mFrameCount);

        while (!records.isAfterLast()) {
          double lat = records.getDouble(records.getColumnIndex(SequenceDB.FRAME_LAT));
          double lon = records.getDouble(records.getColumnIndex(SequenceDB.FRAME_LON));
          int index = records.getInt(records.getColumnIndex(SequenceDB.FRAME_SEQ_INDEX));
          if (lat != 0.0 && lon != 0.0) {
            mPolyline.getNodes().add(new ImageCoordinate(lat, lon, index));
          }
          records.moveToNext();
        }
        Collections.sort(mPolyline.getNodes(), (lhs, rhs) -> ((ImageCoordinate) lhs).index - ((ImageCoordinate) rhs).index);
        mTotalLength = 0;
        try {
          for (int i = 0; i < mPolyline.getNodes().size() - 1; i++) {
            mTotalLength =
                (int) (mTotalLength + ComputingDistance.distanceBetween(mPolyline.getNodes().get(i), mPolyline.getNodes().get(i + 1)));
          }
        } catch (Exception e) {
          Log.w(TAG, "distanceCalculation: " + Log.getStackTraceString(e));
        }
      } else {
        Log.w(TAG, "LocalSequence: cursor has 0 elements");
      }
      if (records != null) {
        records.close();
      }
    } catch (Exception e) {
      Log.w(TAG, "LocalSequence: " + Log.getStackTraceString(e));
    }
    this.mHasObd = SequenceDB.instance.isOBDSequence(mId);
  }

  private double calculateScore() {
    ArrayList<ScoreHistory> array = new ArrayList<>(mScoreHistory.values());
    Iterator<ScoreHistory> iter = array.iterator();
    double score = 0;
    while (iter.hasNext()) {
      ScoreHistory sch = iter.next();
      if (sch.coverage == -1) {
        iter.remove();
        continue;
      }
      score = score + sch.photoCount * Utils.getValueOnSegment(sch.coverage) + sch.obdPhotoCount * sch.obdPhotoCount;
    }
    return score;
  }

  public int getStatus() {
    return SequenceDB.instance.getStatus(mId);
  }

  public void setStatus(int status) {
    SequenceDB.instance.setStatus(mId, status);
  }

  @Override
  public String toString() {
    return "LocalSequence (id " + mId + " images " + mFrameCount + " from " + mOriginalFrameCount + " number of videos " + mVideoCount +
        " and " + value + " Points" + ")";
  }

  @Override
  public boolean isOnline() {
    return false;
  }

  public int getScore() {
    return (int) getValue();
  }

  @Override
  public boolean isSafe() {
    return mIsSafe;
  }
}
