package com.telenav.osv.manager.network.encoder;

import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.telenav.osv.data.score.model.ScoreHistory;

/**
 * The encoder of the local object {@link ScoreHistory} which will be sent to the server as details for a sequence.
 * The details are stored as sequence's details on the server side and are not taken into consideration for remote score calculation.
 * Created by kalmanb on 8/3/17.
 */
public class ScoreJsonEncoder {

    public static String encode(Map<Integer, ScoreHistory> histories) {

        JSONArray array = new JSONArray();
        for (ScoreHistory history : histories.values()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("coverage", "" + history.getCoverage());
                obj.put("photo", "" + history.getPhotoCount());
                obj.put("obdPhoto", "" + history.getObdPhotoCount());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(obj);
        }
        return array.toString();
    }
}
