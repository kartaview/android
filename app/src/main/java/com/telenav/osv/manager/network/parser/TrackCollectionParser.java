package com.telenav.osv.manager.network.parser;

import java.util.Date;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.item.ScoreHistory;
import com.telenav.osv.item.UserOnlineSequence;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * JSON parser for user tracks list
 * Created by kalmanb on 8/1/17.
 */
public class TrackCollectionParser extends ApiResponseParser<TrackCollection> {
    private static final String TAG = "UserTracksParser";

    @Override
    public TrackCollection getHolder() {
        return new TrackCollection();
    }

    public TrackCollection parse(String json) {
        TrackCollection collection = super.parse(json);
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray array1 = obj.getJSONArray("totalFilteredItems");
            collection.setTotalFilteredItems(array1.getInt(0));
            if (array1.length() > 0 && collection.getTotalFilteredItems() > 0) {
                JSONArray array = obj.getJSONArray("currentPageItems");
                Log.d(TAG, "listSequences: size = " + collection.getTotalFilteredItems());

                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    int id = item.getInt("id");
                    String dateStr = item.getString("date_added");
                    Date date = new Date();
                    try {
                        date = Utils.onlineDateFormat.parse(dateStr);
                    } catch (Exception e) {
                        Log.w(TAG, "listSequences: " + e.getLocalizedMessage());
                    }
                    String imgNum = item.getString("photo_no");
                    String distance = item.getString("distance");
                    double lat = item.getDouble("current_lat");
                    double lon = item.getDouble("current_lng");
                    String processing = item.getString("image_processing_status");
                    boolean obd = false;
                    String platform = "";
                    String platformVersion = "";
                    String appVersion = "";
                    try {
                        platform = item.getString("platform_name");
                        platformVersion = item.getString("platform_version");
                        appVersion = item.getString("app_version");
                        obd = item.getInt("obd_info") > 0;
                    } catch (Exception e) {
                        if (!(e instanceof JSONException)) {
                            Log.w(TAG, "listSequences: " + Log.getStackTraceString(e));
                        }
                    }
                    String partialAddress = "";
                    try {
                        String address = item.getString("location");
                        String[] list = address.split(", ");
                        partialAddress = list[0] + ", " + list[2];
                    } catch (Exception ignored) {}
                    String thumbLink = UserDataManager.URL_DOWNLOAD_PHOTO + item.getString("thumb_name");
                    double distanceNum = 0;
                    try {
                        if (distance != null) {
                            distanceNum = Double.parseDouble(distance);
                        }
                    } catch (NumberFormatException ignored) {}
                    UserOnlineSequence seq = new UserOnlineSequence(id, date, Integer.valueOf(imgNum), partialAddress, thumbLink, obd, platform, platformVersion, appVersion,
                            (int) (distanceNum
                            * 1000d), 0);
                    seq.setServerStatus(processing);
                    seq.setLocation(new SKCoordinate(lat, lon));
                    int totalPoints = 0;
                    try {
                        JSONObject history = item.getJSONObject("upload_history");
                        boolean historyObd = history.getString("has_obd").equals("N");
                        JSONArray coverages = history.getJSONArray("coverage");
                        JSONObject points = history.getJSONObject("points");
                        totalPoints = Integer.parseInt(points.getString("total"));
                        HashMap<Integer, ScoreHistory> scoreHistory = new HashMap<>();
                        for (int j = 0; j < coverages.length(); j++) {
                            JSONObject coverage = coverages.getJSONObject(j);
                            int cov = Integer.parseInt(coverage.getString("coverage_value").replace("+", "").replace("-", "0"));
                            int pts = coverage.getInt("coverage_points");
                            int photosCount = coverage.getInt("coverage_photos_count");
                            ScoreHistory existing = scoreHistory.get(cov);
                            if (existing != null) {
                                existing.obdPhotoCount += obd ? photosCount : 0;
                                existing.photoCount += obd ? 0 : photosCount;
                            } else {
                                scoreHistory.put(cov, new ScoreHistory(cov, obd ? 0 : photosCount, obd ? photosCount : 0));
                            }
                        }
                        seq.setScoreHistory(scoreHistory);
                    } catch (Exception e) {
                        Log.d(TAG, "listSequences: " + Log.getStackTraceString(e));
                    }
                    seq.setScore(totalPoints);
                    collection.getTrackList().add(seq);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return collection;
    }
}
