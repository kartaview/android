package com.telenav.osv.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.database.Cursor;
import android.os.Build;
import android.os.Looper;

import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.listener.ProgressListener;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 11/18/15.
 */
public class Sequence {

    public static final String TAG = "Sequence";

    public static final int STATUS_NEW = 0;

    public static final int STATUS_INDEXING = 1;

    public static final int STATUS_UPLOADING = 2;

    public static final int STATUS_INTERRUPTED = 3;

    public static final int STATUS_FINISHED = 4;

    private static final ConcurrentHashMap<Integer, Sequence> sequences = new ConcurrentHashMap<>();

    private static boolean mInitialized = false;

    public final boolean online;

    public int mTotalLength;

    public MapFragment.Polyline polyline;

    public int originalImageCount;

    public boolean mIsExternal = false;

    public int imageCount;

    public long size = 0;

    public String thumblink = "";

    public String address = "";

    public int sequenceId = -1;

    public String title = "";

    public OSVFile folder;

    public int onlineSequenceId;

    public SKCoordinate location = new SKCoordinate();

    public int failedCount = 0;

    public boolean processing = false;

    public boolean obd = false;

    public String platform = "";

    public String platformVersion = "";

    public String appVersion = "";

    private ArrayList<ProgressListener> progressListeners = new ArrayList<>();

    public int numberOfVideos = -1;

    public Sequence(int sequenceId, String date, int originalImageCount, String address, String thumbLink, boolean obd, String platform, String platformVersion, String
            appVersion, int distance) {
        this.sequenceId = sequenceId;
        this.title = date;
        this.online = true;
        this.originalImageCount = originalImageCount;
        this.imageCount = originalImageCount;
        this.address = address;
        this.thumblink = thumbLink;
        this.onlineSequenceId = sequenceId;
        this.obd = obd;
        this.platform = platform;
        this.platformVersion = platformVersion;
        this.appVersion = appVersion;
        this.mTotalLength = distance;
    }

    public Sequence(int sequenceId, String date) {
        this.sequenceId = sequenceId;
        this.originalImageCount = 0;
        this.imageCount = originalImageCount;
        this.title = date;
        this.online = true;
    }

    public Sequence(OSVFile folder) {
        this.folder = folder;
        this.sequenceId = getSequenceId(folder);
        this.title = Utils.numericDateFormat.format(new Date(folder.lastModified()));
        this.online = false;

        refreshStats();
        synchronized (sequences) {
            sequences.put(sequenceId, this);
            orderByValue(sequences, new Comparator<Sequence>() {
                @Override
                public int compare(Sequence lhs, Sequence rhs) {
                    return (rhs.getStatus() * 1000 + rhs.sequenceId) - (lhs.getStatus() * 1000 + lhs.sequenceId);
                }
            });
        }
        Log.d(TAG, "Sequence: " + this.toString());
    }

    public static ConcurrentHashMap<Integer, Sequence> forceRefreshLocalSequences() {
        ConcurrentHashMap<Integer, Sequence> tempSequences = new ConcurrentHashMap<>();
        synchronized (sequences) {
            Cursor cur = SequenceDB.instance.getAllSequences();
            while (cur != null && !cur.isAfterLast()) {
                OSVFile f = new OSVFile(cur.getString(cur.getColumnIndex(SequenceDB.SEQUENCE_PATH)));
                boolean external = cur.getInt(cur.getColumnIndex(SequenceDB.SEQUENCE_EXTERNAL)) > 0;
                Integer id = cur.getInt(cur.getColumnIndex(SequenceDB.SEQUENCE_ID));
                if (!sequences.containsKey(id)) {
                    Sequence seq = new Sequence(f);
                    seq.mIsExternal = external;
                    double lat = cur.getDouble(cur.getColumnIndex(SequenceDB.SEQUENCE_LAT));
                    double lon = cur.getDouble(cur.getColumnIndex(SequenceDB.SEQUENCE_LON));
                    seq.location.setLatitude(lat);
                    seq.location.setLongitude(lon);
                    if (SKReverseGeocoderManager.getInstance() != null && lat != 0 && lon != 0) {
                        SKSearchResult address = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(new SKCoordinate(lon, lat));
                        if (address != null) {
                            seq.address = address.getName();
                        }
                    }
                    Log.d(TAG, "getLocalSequences: reverseGeocode: " + seq.address);
                    tempSequences.put(seq.sequenceId, seq);

                } else {
                    tempSequences.put(id, sequences.get(id));
                }
                cur.moveToNext();
            }
            if (cur != null) {
                cur.close();
            }
            sequences.clear();
            for (Sequence seq : tempSequences.values()) {
                seq.progressListeners.clear();
                sequences.put(seq.sequenceId, seq);
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Log.e(TAG, "getLocalSequences called on main thread ");
            }
            orderByValue(sequences, new Comparator<Sequence>() {
                @Override
                public int compare(Sequence lhs, Sequence rhs) {
                    return (rhs.getStatus() * 1000 + rhs.sequenceId) - (lhs.getStatus() * 1000 + lhs.sequenceId);
                }
            });
//            Log.d(TAG, "getLocalSequences: " + sequences.values().toString());
            mInitialized = true;
            return sequences;
        }
    }

    public static ConcurrentHashMap<Integer, Sequence> getLocalSequences() {
        if (UploadManager.sUploadStatus != UploadManager.STATUS_IDLE) {
            return sequences;
        }
        return forceRefreshLocalSequences();
    }

    private static <K, V> void orderByValue(ConcurrentHashMap<K, V> m, final Comparator<? super V> c) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(m.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> lhs, Map.Entry<K, V> rhs) {
                return c.compare(lhs.getValue(), rhs.getValue());
            }
        });

        m.clear();
        for (Map.Entry<K, V> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
    }

    /**
     * used only for the delete of OSVFile
     *
     * @return
     */
    public static ConcurrentHashMap<Integer, Sequence> getStaticSequences() {
        if (!mInitialized) {
            return forceRefreshLocalSequences();
        }
        return sequences;
    }

    public static Integer getSequenceId(OSVFile f) {
        int result = -1;
        try {
            result = Integer.valueOf(f.getName().split("_")[1]);
        } catch (Exception e) {
            Log.d(TAG, "getSequenceId: " + e.getLocalizedMessage());
        }
        return result;
    }

    public static void removeSequence(int id) {
        if (UploadManager.sUploadStatus != UploadManager.STATUS_IDLE) {
            return;
        }
//        Sequence seq = sequences.get(id);
        SequenceDB.instance.deleteRecords(id);
        synchronized (sequences) {
            Log.d(TAG, "removeSequence: removed seq with id " + id);
            sequences.remove(id);
        }
    }

    public static int getLocalSequencesSize() {
        return sequences.size();
    }

    public void addProgressListener(final ProgressListener pl) {
        progressListeners.add(pl);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int status = getStatus();
        numberOfVideos = (int) SequenceDB.instance.getNumberOfVideos(sequenceId) - failedCount;
        imageCount = (int) SequenceDB.instance.getNumberOfFrames(sequenceId) - failedCount;

//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
        pl.update(originalImageCount != 0 ? ((originalImageCount - imageCount) * 100) / originalImageCount : 0, getStatus());
//                    }
//                });
//            }
//        }).start();
    }

    public ArrayList<ProgressListener> getProgressListeners() {
        return progressListeners;
    }

    public void decreaseVideoCount() {
        numberOfVideos--;
        numberOfVideos = Math.max(0, numberOfVideos);
    }

    public void refreshStats() {
        this.size = Utils.folderSize(folder);
        this.polyline = new MapFragment.Polyline(sequenceId);
        this.platform = "Android";
        this.platformVersion = Build.VERSION.RELEASE;
        this.appVersion = SequenceDB.instance.getSequenceVersion(sequenceId);
        this.originalImageCount = (int) SequenceDB.instance.getOriginalFrameCount(sequenceId);
        this.numberOfVideos = (int) SequenceDB.instance.getNumberOfVideos(sequenceId);
        try {
            Cursor records = SequenceDB.instance.getFrames(sequenceId);

            if (records != null && records.getCount() > 0) {
                try {
                    this.thumblink = "file:///" + records.getString(records.getColumnIndex(SequenceDB.FRAME_FILE_PATH));
                    this.imageCount = records.getCount();
                    this.failedCount = 0;
                } catch (Exception e) {
                    Log.d(TAG, "Sequence: " + e.getLocalizedMessage());
                }
                Log.d(TAG, "refreshStats: sequence " + sequenceId + ", count: " + imageCount);

                while (!records.isAfterLast()) {
                    double lat = records.getDouble(records.getColumnIndex(SequenceDB.FRAME_LAT));
                    double lon = records.getDouble(records.getColumnIndex(SequenceDB.FRAME_LON));
                    int index = records.getInt(records.getColumnIndex(SequenceDB.FRAME_SEQ_INDEX));
                    if (lat != 0.0 && lon != 0.0) {
                        polyline.getNodes().add(new ImageCoordinate(lon, lat, index));
                    }
                    records.moveToNext();
                }
                Collections.sort(polyline.getNodes(), new Comparator<SKCoordinate>() {
                    @Override
                    public int compare(SKCoordinate lhs, SKCoordinate rhs) {
                        return ((ImageCoordinate) lhs).index - ((ImageCoordinate) rhs).index;
                    }
                });
                mTotalLength = 0;
                try {
                    for (int i = 0; i < polyline.getNodes().size() - 1; i++) {
                        mTotalLength = (int) (mTotalLength + ComputingDistance.distanceBetween(polyline.getNodes().get(i), polyline.getNodes().get(i + 1)));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "distanceCalculation: " + Log.getStackTraceString(e));
                }
            } else {
                Log.d(TAG, "Sequence: cursor has 0 elements");
            }
            if (records != null) {
                records.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "Sequence: " + Log.getStackTraceString(e));
        }
        this.obd = SequenceDB.instance.isOBDSequence(sequenceId);
    }

    public static double getTotalSize() {
        double totalSize = 0;
        for (Sequence s : sequences.values()) {
            if (s.getStatus() != STATUS_FINISHED) {
                totalSize = totalSize + s.size;
            }
        }
        return totalSize;
    }

    public static int getTotalDistance() {
        int totalDistance = 0;
        for (Sequence s : sequences.values()) {
            if (s.getStatus() != STATUS_FINISHED) {
                totalDistance = totalDistance + s.mTotalLength;
            }
        }
        return totalDistance;
    }

    public static Sequence getLocalSequence(int id) {
        return sequences.get(id);
    }

    public int getStatus() {
        if (online) {
            return STATUS_NEW;
        }
        return SequenceDB.instance.getStatus(sequenceId);
    }

    public void setStatus(int status) {
        SequenceDB.instance.setStatus(sequenceId, status);
    }

    @Override
    public String toString() {
        return "Sequence (id " + sequenceId + " images " + imageCount + " from " + originalImageCount + " status " + getStatus() + " number of videos " + numberOfVideos + ")";
    }
}