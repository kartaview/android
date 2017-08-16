package com.telenav.osv.manager.network.parser;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.item.NearbySequence;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.utils.Log;

/**
 * JSON parser for user tracks list
 * Created by kalmanb on 8/1/17.
 */
public class NearbyTracksParser extends ApiResponseParser<TrackCollection> {
    private static final String TAG = "NearbyTracksParser";

    @Override
    public TrackCollection getHolder() {
        return new TrackCollection();
    }

    public TrackCollection parse(String json) {
        TrackCollection collectionData = super.parse(json);
        if (json != null && !json.isEmpty()) {
            try {
                JSONObject obj = new JSONObject(json);
                JSONObject osv = obj.getJSONObject("osv");
                JSONArray array1 = osv.getJSONArray("sequences");
                if (array1.length() > 0) {
                    for (int i = 0; i < array1.length(); i++) {
                        JSONObject item = array1.getJSONObject(i);
                        int id = item.getInt("sequence_id");
                        String dateStr = item.getString("date");
                        String hour = item.getString("hour");
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat onlineDateFormat = new SimpleDateFormat("MM.dd.yyyy hh:mm a");
                        Date date = new Date();
                        try {
                            date = onlineDateFormat.parse(dateStr + " " + hour);
                        } catch (Exception e) {
                            Log.w(TAG, "handleSequenceListResult: " + e.getLocalizedMessage());
                        }
                        String imgNum = item.getString("photo_no");
                        String sequenceIndex = item.getString("sequence_index");
                        String distance = item.getString("distance");
                        double lat = item.getDouble("lat");
                        double lon = item.getDouble("lng");
                        String platform = "";
                        String platformVersion = "";
                        String appVersion = "";
                        String partialAddress = "";
                        try {
                            String address = item.getString("address");
                            String[] list = address.split(", ");
                            partialAddress = list[0] + ", " + list[2];
                        } catch (Exception e) {
                        }
                        String thumbLink = UserDataManager.URL_DOWNLOAD_PHOTO + item.getString("photo");
                        double distanceNum = 0;
                        try {
                            if (distance != null) {
                                distanceNum = Double.parseDouble(distance);
                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "handleSequenceListResult: couldn't parse distance");
                        }
                        NearbySequence seq = new NearbySequence(id, date, Integer.valueOf(imgNum), partialAddress, thumbLink, platform, platformVersion,
                                appVersion, (int) (distanceNum
                                * 1000d));
                        seq.setPublic(true);
                        seq.setLocation(new SKCoordinate(lat, lon));
                        seq.setRequestedFrameIndex(Integer.valueOf(sequenceIndex));
                        collectionData.getTrackList().add(seq);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return collectionData;
    }
}
