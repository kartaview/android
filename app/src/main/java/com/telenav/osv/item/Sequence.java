package com.telenav.osv.item;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Looper;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.manager.network.UploadManager;
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

    private static final ConcurrentHashMap<Integer, Sequence> sequences = new ConcurrentHashMap<>();

    private static boolean mInitialized = false;

    public final boolean online;

    public int mTotalLength;

    public Polyline polyline;

    public int originalImageCount;

    public boolean mIsExternal = false;

    public int imageCount;

    public long originalSize = 0;

    public long size = 0;

    public String thumblink = "";

    public String address = "";

    public int sequenceId = -1;

    public String title = "";

    public OSVFile folder;

    public int onlineSequenceId;

    public SKCoordinate location = new SKCoordinate();

    public boolean processing = false;

    public boolean obd = false;

    public String platform = "";

    public String platformVersion = "";

    public String appVersion = "";

    public int numberOfVideos = -1;

    public boolean isPublic;

    public int skipToValue = 0;

    public int score;

    public HashMap<Integer, ScoreHistory> scoreHistories = new HashMap<>();

    public boolean safe;

    public Sequence(int sequenceId, String date, int originalImageCount, String address, String thumbLink, boolean obd, String platform, String platformVersion, String
            appVersion, int distance, int score) {
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
        this.score = score;
    }

    public Sequence(OSVFile folder) {
        this.folder = folder;
        this.sequenceId = getSequenceId(folder);
        this.title = Utils.numericDateFormat.format(new Date(folder.lastModified()));
        this.online = false;

        refreshStats();
        synchronized (sequences) {
            sequences.put(sequenceId, this);
        }
        EventBus.post(new SequencesChangedEvent(false));
        this.address = "Track " + (sequenceId + 1);
        Log.d(TAG, "Sequence: " + this.toString());
    }

    private static ConcurrentHashMap<Integer, Sequence> forceRefreshLocalSequences() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "getLocalSequences called on main thread ");
            throw new IllegalStateException("GetLocalSequences called on main thread.");
        }
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

//                    if (Geocoder.isPresent() && lat != 0 && lon != 0) {
//                        Geocoder geocoder = new Geocoder(/*context*/, Locale.ENGLISH);
//                        List<Address> addresses = null;
//                        try {
//                            addresses = geocoder.getFromLocation(lat, lon, 1);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        if (addresses != null) {
//                            Address fetchedAddress = addresses.get(0);
//                            StringBuilder strAddress = new StringBuilder();
//
//                            for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
//                                strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
//                            }
//                            seq.address = strAddress.toString();
//                        }
//                    }
                    seq.safe = cur.getInt(cur.getColumnIndex(SequenceDB.SEQUENCE_SAFE)) > 0;
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
                sequences.put(seq.sequenceId, seq);
            }
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

    public static void order(List<Sequence> m) {

        Collections.sort(m, new Comparator<Sequence>() {
            @Override
            public int compare(Sequence rhs, Sequence lhs) {
                return (int) ((rhs.getStatus() * 1000 + rhs.folder.lastModified()) - (lhs.getStatus() * 1000 + lhs.folder.lastModified()));
            }
        });
    }

    /**
     * used only for the delete of OSVFile
     *
     * @return
     */
    public static ConcurrentHashMap<Integer, Sequence> getStaticSequences() {
        if (!mInitialized && Looper.myLooper() != Looper.getMainLooper()) {
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
        for (Sequence s : sequences.values()) {
            totalSize = totalSize + s.size;

        }
        return totalSize;
    }

    public static int checkDeletedSequences() {
        if (sequences != null && sequences.size() > 0) {
            for (Sequence s : sequences.values()) {
                if (!SequenceDB.instance.checkSequenceExists(s.sequenceId)) {
                    deleteSequence(s.sequenceId);
                }
            }
            return sequences.size();
        }
        return 0;
    }

    public void decreaseVideoCount() {
        numberOfVideos--;
        numberOfVideos = Math.max(0, numberOfVideos);
    }

    public void refreshStats() {
        this.size = Utils.folderSize(folder);
        this.originalSize = size;
        this.polyline = new Polyline(sequenceId);
        this.polyline.isLocal = true;
        this.platform = "Android";
        this.platformVersion = Build.VERSION.RELEASE;
        this.appVersion = SequenceDB.instance.getSequenceVersion(sequenceId);
        this.score = SequenceDB.instance.getSequenceScore(sequenceId);
        this.originalImageCount = SequenceDB.instance.getOriginalFrameCount(sequenceId);
        this.numberOfVideos = (int) SequenceDB.instance.getNumberOfVideos(sequenceId);

        this.safe = SequenceDB.instance.isSequenceSafe(sequenceId);
        Cursor scores = null;
        try {
            scores = SequenceDB.instance.getScores(sequenceId);
            scoreHistories.clear();
            while (scores != null && !scores.isAfterLast()){
                int coverage = Math.min(scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_COVERAGE)), 10);
                int obdCount = scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_OBD_COUNT));
                int count = scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_COUNT));
                scoreHistories.put(coverage, new ScoreHistory(coverage,count,obdCount));
                scores.moveToNext();
            }
            if (scores != null){
                scores.close();
            }
        } catch (Exception e){
            Log.w(TAG, "refreshStats: " + Log.getStackTraceString(e));
            if (scores != null){
                scores.close();
            }
        }
        try {
            Cursor records = SequenceDB.instance.getFrames(sequenceId);

            if (records != null && records.getCount() > 0) {
                try {
                    this.thumblink = "file:///" + records.getString(records.getColumnIndex(SequenceDB.FRAME_FILE_PATH));
                    this.imageCount = records.getCount();
                } catch (Exception e) {
                    Log.w(TAG, "Sequence: " + e.getLocalizedMessage());
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
                    Log.w(TAG, "distanceCalculation: " + Log.getStackTraceString(e));
                }
            } else {
                Log.w(TAG, "Sequence: cursor has 0 elements");
            }
            if (records != null) {
                records.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Sequence: " + Log.getStackTraceString(e));
        }
        this.obd = SequenceDB.instance.isOBDSequence(sequenceId);
    }

    public int getDistance(){
        return mTotalLength;
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
        return "Sequence (id " + sequenceId + " images " + imageCount + " from " + originalImageCount + " number of videos " + numberOfVideos + " and " + score + " Points" + ")";
//        return "Sequence (id " + sequenceId + " images " + imageCount + " from " + originalImageCount + " status " + getStatus() + " number of videos " + numberOfVideos + "
// and " + score + " Points" + ")";
    }

    public String getFlatScoreDetails() {
        JSONArray array = new JSONArray();
        for (ScoreHistory history : scoreHistories.values()){
            JSONObject obj = new JSONObject();
            try {
                obj.put("coverage","" + history.coverage);
                obj.put("photo","" + history.photoCount);
                obj.put("obdPhoto","" + history.obdPhotoCount);
                obj.put("detectedSigns","" + history.detectedSigns);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(obj);
        }
        return array.toString();
    }

    public static void reverseGeocodeAddress(Sequence sequence, Context context){
        try {
            if (Geocoder.isPresent() && sequence.location.getLatitude() != 0 && sequence.location.getLongitude() != 0) {
                Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(sequence.location.getLatitude(), sequence.location.getLongitude(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (addresses != null) {
                    Address fetchedAddress = addresses.get(0);
                    StringBuilder strAddress = new StringBuilder();

                    for (int i = 0; i < fetchedAddress.getMaxAddressLineIndex(); i++) {
                        strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
                    }
                    sequence.address = strAddress.toString();
                }
            }
        } catch (Exception e){

        }
    }
}