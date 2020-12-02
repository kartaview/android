package com.telenav.osv.manager.network.parser;

import com.telenav.osv.data.score.model.ScoreHistory;
import com.telenav.osv.data.sequence.model.OnlineSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPoints;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.network.endpoint.FactoryServerEndpointUrl;
import com.telenav.osv.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JSON parser for user tracks list
 * Created by kalmanb on 8/1/17.
 */
public class SequenceParser extends ApiResponseParser<TrackCollection> {

    private static final String TAG = SequenceParser.class.getSimpleName();

    private FactoryServerEndpointUrl factoryServerEndpointUrl;

    public SequenceParser(FactoryServerEndpointUrl factoryServerEndpointUrl){
        this.factoryServerEndpointUrl = factoryServerEndpointUrl;
    }

    @Override
    public TrackCollection getHolder() {
        return new TrackCollection();
    }

    public TrackCollection parse(String json) {
        TrackCollection collection = super.parse(json);
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray filteredItemsJsonArray = obj.getJSONArray("totalFilteredItems");
            collection.setTotalFilteredItems(filteredItemsJsonArray.getInt(0));
            if (filteredItemsJsonArray.length() > 0 && collection.getTotalFilteredItems() > 0) {
                JSONArray array = obj.getJSONArray("currentPageItems");
                Log.d(TAG, "listSequences: size = " + collection.getTotalFilteredItems());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    SequenceDetails sequenceDetails = getSequenceDetails(item);
                    SequenceDetailsCompressionJpeg compressionVideo = getJpegCompression(item, factoryServerEndpointUrl);
                    SequenceDetailsRewardPoints rewardLocal = getRewardLocal(sequenceDetails.isObd(), item);

                    collection.getTrackList().add(new OnlineSequence(
                            UUID.randomUUID().toString(),
                            sequenceDetails,
                            compressionVideo,
                            rewardLocal));
                }
            }
        } catch (Exception exception) {
            Log.d(TAG, String.format("Exception while parsing sequence. Exception: %s", exception.getMessage()));
        }

        return collection;
    }

    /**
     * Transform the data for a sequence in Json Format related to {@code SequenceDetailsRewardPoints}.
     * @param isObd flag which shows the obd status for the sequence.
     * @param sequenceJsonFormat sequence in Json Format.
     * @return {@code SequenceDetailsRewardPoints} object.
     * @throws JSONException exception thrown by the Json when data in invalid.
     */
    private SequenceDetailsRewardPoints getRewardLocal(boolean isObd, JSONObject sequenceJsonFormat) throws JSONException {
        int totalPoints;
        Map<Integer, ScoreHistory> scoreHistory = new HashMap<>();
        String upload_history_argument_name = "upload_history";
        String coverage_argument_name = "coverage";
        String points_argument_name = "points";
        if (sequenceJsonFormat.isNull(upload_history_argument_name)) {
            Log.d(TAG, "No upload history found.");
            return null;
        }
        JSONObject history;
        try {
            history = sequenceJsonFormat.getJSONObject(upload_history_argument_name);
        } catch (JSONException jsonException) {
            Log.d(TAG, String.format("No upload history found. Parsing exception: %s", jsonException.getLocalizedMessage()));
            boolean uploadHistory = sequenceJsonFormat.getBoolean(upload_history_argument_name);
            if (!uploadHistory) {
                Log.d(TAG, "No upload history found.");
                return null;
            }
            return null;
        }
        // gets the history of the object in order to get reward points info
        if (history.isNull(coverage_argument_name)
                || history.isNull(points_argument_name)) {
            Log.d(TAG, "No points/coverage found.");
            return null;
        }
        JSONArray coverages = history.getJSONArray(coverage_argument_name);
        JSONObject points = history.getJSONObject(points_argument_name);
        int multiplier = points.getInt("obd_multiple");
        totalPoints = Integer.parseInt(points.getString("total"));
        for (int j = 0; j < coverages.length(); j++) {
            JSONObject coverage = coverages.getJSONObject(j);
            int coverageValue = Integer.parseInt(coverage.getString("coverage_value").replace("+", "").replace("-", "0"));
            int photosCount = coverage.getInt("coverage_photos_count");
            ScoreHistory existing = scoreHistory.get(coverageValue);
            if (existing != null) {
                existing.setObdPhotoCount(existing.getObdPhotoCount() + (isObd ? photosCount : 0));
                existing.setPhotoCount(existing.getPhotoCount() + (isObd ? 0 : photosCount));
                existing.setObdStatus(multiplier);
            } else {
                scoreHistory.put(coverageValue, new ScoreHistory(coverageValue, isObd ? 0 : photosCount, isObd ? photosCount : 0, multiplier));
            }
        }

        return new SequenceDetailsRewardPoints(totalPoints, "points", scoreHistory);
    }
}
