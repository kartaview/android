package com.telenav.osv.manager.network.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.location.Location;
import com.telenav.osv.data.sequence.model.NearbySequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.network.endpoint.FactoryServerEndpointUrl;
import com.telenav.osv.network.endpoint.UrlProfile;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;

/**
 * JSON parser for user tracks list
 * Created by kalmanb on 8/1/17.
 */
public class NearbyParser extends ApiResponseParser<TrackCollection> {

    private static final String TAG = "NearbyParser";

    private FactoryServerEndpointUrl factoryServerEndpointUrl;

    public NearbyParser(FactoryServerEndpointUrl factoryServerEndpointUrl){
        this.factoryServerEndpointUrl = factoryServerEndpointUrl;
    }

    /**
     * Format for the date.
     */
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy hh:mm a");

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
                        SequenceDetails sequenceDetails = getSequenceDetails(item);
                        SequenceDetailsCompressionJpeg compressionJpeg = getJpegCompression(item, factoryServerEndpointUrl);

                        collectionData.getTrackList().add(new NearbySequence(
                                UUID.randomUUID().toString(),
                                sequenceDetails,
                                compressionJpeg));
                    }
                }
            } catch (Exception exception) {
                Log.d(TAG, String.format("Exception while parsing sequence. Exception: %s", exception.getMessage()));
            }
        }
        return collectionData;
    }

    @Override
    SequenceDetailsCompressionJpeg getJpegCompression(JSONObject sequenceJsonFormat, FactoryServerEndpointUrl factoryServerEndpointUrl) throws JSONException {
        String frameLength = sequenceJsonFormat.getString("photo_no");
        String thumbLink = factoryServerEndpointUrl.getProfileEndpoint(UrlProfile.DOWNLOAD_PHOTO) + sequenceJsonFormat.getString("photo");
        String frameIndex = sequenceJsonFormat.getString("sequence_index");

        return new SequenceDetailsCompressionJpeg(Integer.valueOf(frameLength), thumbLink, Integer.valueOf(frameIndex));
    }

    /**
     * Overridden method due to changes in the API for values.
     */
    @Override
    SequenceDetails getSequenceDetails(JSONObject jsonObject) throws JSONException {
        int id = jsonObject.getInt("sequence_id");
        double lat = jsonObject.getDouble("lat");
        double lon = jsonObject.getDouble("lng");
        String hour = jsonObject.getString("hour");
        String dateStr = jsonObject.getString("date") + " " + hour;
        String distance = jsonObject.getString("distance");
        String appVersion = "";
        String partialAddress = "";
        int distanceNumber = 0;
        long timestamp = 0;
        try {
            String address = jsonObject.getString("address");
            String[] list = address.split(", ");
            partialAddress = list[0] + ", " + list[2];
            if (distance != null) {
                distanceNumber = (int) (Double.parseDouble(distance) * 1000d);
            }
            try {
                Date date = dateFormat.parse(dateStr);
                timestamp = date.getTime();
            } catch (ParseException parseException) {
                Log.d(TAG, String.format("ParseException for date. Sequence id: %s. Exception: %s", id, parseException.getMessage()));
            }
            appVersion = jsonObject.getString("app_version");
        } catch (NumberFormatException numberFormatException) {
            Log.d(TAG, String.format("Number format exception. Sequence id: %s. Exception: %s", id, numberFormatException.getMessage()));
        } catch (JSONException jsonException) {
            Log.d(TAG, String.format("Json exception. Sequence id: %s. Exception: %s", id, jsonException.getMessage()));
        }

        Location location = new Location(StringUtils.EMPTY_STRING);
        location.setLatitude(lat);
        location.setLongitude(lon);
        SequenceDetails sequenceDetails = new SequenceDetails(
                location,
                distanceNumber,
                appVersion,
                new DateTime(timestamp));
        sequenceDetails.setOnlineId(id);
        sequenceDetails.setAddressName(partialAddress);

        return sequenceDetails;
    }
}
