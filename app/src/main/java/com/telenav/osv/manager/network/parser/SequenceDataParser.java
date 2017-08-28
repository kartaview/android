package com.telenav.osv.manager.network.parser;

import com.telenav.osv.item.network.SequenceData;
import com.telenav.osv.utils.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON parser for driver profile data
 * Created by kalmanb on 8/1/17.
 */
public class SequenceDataParser extends ApiResponseParser<SequenceData> {

  private static final String TAG = "SequenceDataParser";

  @Override
  public SequenceData getHolder() {
    return new SequenceData();
  }

  public SequenceData parse(String json) {
    SequenceData sequenceData = super.parse(json);
    try {
      JSONObject jsonObject;
      try {
        jsonObject = new JSONObject(json);
        sequenceData.setOnlineID(jsonObject.getJSONObject("osv").getJSONObject("sequence").getInt("id"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      Log.w(TAG, "createSequence: " + e.getLocalizedMessage());
    }
    return sequenceData;
  }
}
