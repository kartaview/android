package com.telenav.osv.manager.network.parser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.location.Location;
import com.telenav.osv.data.sequence.model.OnlineSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPaid;
import com.telenav.osv.item.DriverPayRateBreakdownCoverageItem;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;

/**
 * JSON parser for driver tracks list
 * Created by kalmanb on 8/1/17.
 */
public class DriverSequenceParser extends ApiResponseParser<TrackCollection> {

    /**
     * The multiplier required to transform km to meters.
     */
    private static final double M_MULTIPLIER = 1000d;

    private static final String TAG = "DriverSequenceParser";

    @Override
    public TrackCollection getHolder() {
        return new TrackCollection();
    }

    public TrackCollection parse(String json) {
        TrackCollection collectionData = super.parse(json);
        try {
            JSONObject obj = new JSONObject(json);
            JSONObject osv = obj.getJSONObject("osv");
            int totalFilteredItems = osv.getInt("totalFilteredItems");
            collectionData.setTotalFilteredItems(totalFilteredItems);
            Log.d(TAG, "listDriverSequences: items = " + totalFilteredItems);
            if (collectionData.getTotalFilteredItems() > 0) {
                JSONArray array = osv.getJSONArray("list");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);

                    SequenceDetails sequenceDetails = getSequenceDetails(item);
                    SequenceDetailsCompressionJpeg compressionVideo = getJpegCompression(item);
                    SequenceDetailsRewardPaid rewardLocal = getRewardPaid(item);

                    collectionData.getTrackList().add(new OnlineSequence(
                            UUID.randomUUID().toString(),
                            sequenceDetails,
                            compressionVideo,
                            rewardLocal));
                }
            }
        } catch (Exception exception) {
            Log.d(TAG, String.format("Exception while parsing sequence. Exception: %s", exception.getMessage()));
        }
        return collectionData;
    }

    @Override
    SequenceDetails getSequenceDetails(JSONObject sequenceJsonFormat) throws JSONException {
        boolean obd = false;
        String appVersion = StringUtils.EMPTY_STRING;
        double distanceNumber = 0;
        String partialAddress = StringUtils.EMPTY_STRING;
        int id = sequenceJsonFormat.getInt("id");
        double lat = sequenceJsonFormat.getDouble("current_lat");
        double lon = sequenceJsonFormat.getDouble("current_lng");
        long timestamp = new DateTime().getMillis();
        String processingStatus = sequenceJsonFormat.getString("status");
        String distance = sequenceJsonFormat.getString("distance");
        String dateFormat = sequenceJsonFormat.getString("date_added");
        Location initialLocation = new Location(StringUtils.EMPTY_STRING);
        initialLocation.setLatitude(lat);
        initialLocation.setLongitude(lon);

        try {
            String address = sequenceJsonFormat.getString("address");
            String[] list = address.split(", ");
            partialAddress = list[0] + ", " + list[2];
            if (distance != null) {
                distanceNumber = Double.parseDouble(distance) * M_MULTIPLIER;
            }
            appVersion = sequenceJsonFormat.getString("app_version");
            obd = sequenceJsonFormat.getInt("obd_info") > 0;
            try {
                Date date = Utils.onlineDriverDateFormat.parse(dateFormat);
                timestamp = date.getTime();
            } catch (ParseException parseException) {
                Log.d(TAG, String.format("ParseException for date. Sequence id: %s. Exception: %s", id, parseException.getMessage()));
            }
        } catch (NumberFormatException numberFormatException) {
            Log.d(TAG, String.format("Number format exception. Sequence id: %s. Exception: %s", id, numberFormatException.getMessage()));
        } catch (JSONException jsonException) {
            Log.d(TAG, String.format("Json exception. Sequence id: %s. Exception: %s", id, jsonException.getMessage()));
        }

        SequenceDetails sequenceDetails = new SequenceDetails(initialLocation,
                distanceNumber,
                appVersion,
                new DateTime(timestamp));
        sequenceDetails.setOnlineId(id);
        sequenceDetails.setObd(obd);
        sequenceDetails.setProcessingRemoteStatus(processingStatus);
        sequenceDetails.setAddressName(partialAddress);

        return sequenceDetails;
    }

    /**
     * Transform the data for a sequence in Json Format related to {@code SequenceDetailsRewardPaid}.
     * @return {@code SequenceDetailsRewardPaid} object.
     * @throws JSONException exception thrown by the Json when data in invalid.
     */
    private SequenceDetailsRewardPaid getRewardPaid(JSONObject jsonObject) throws JSONException {
        DriverPayRateBreakdownCoverageItem[] driverPayRateBreakdownCoverageArray = new DriverPayRateBreakdownCoverageItem[0];
        if (!jsonObject.isNull("coverage")) {
            JSONArray coverageItemsArray = jsonObject.getJSONArray("coverage");

            if (coverageItemsArray != null) {
                int coverageItemsLength = coverageItemsArray.length();

                if (coverageItemsLength > 0) {
                    driverPayRateBreakdownCoverageArray = new DriverPayRateBreakdownCoverageItem[coverageItemsLength];

                    for (int coverageItemIndex = 0; coverageItemIndex < coverageItemsLength; coverageItemIndex++) {

                        JSONObject coverageItem = coverageItemsArray.getJSONObject(coverageItemIndex);
                        if (coverageItem != null) {
                            int coverageValue = coverageItem.getInt("value");
                            float coverageDistance = (float) coverageItem.getDouble("distance");
                            float coveragePrice = (float) coverageItem.getDouble("price");
                            driverPayRateBreakdownCoverageArray[coverageItemIndex] = new DriverPayRateBreakdownCoverageItem(coverageValue, coverageDistance,
                                    coveragePrice);
                        } else {
                            driverPayRateBreakdownCoverageArray[coverageItemIndex] = null;
                        }
                    }

                }
            }
        }
        double value = jsonObject.getDouble("value");
        String currency = jsonObject.getString("currency");
        try {
            currency = Currency.getInstance(currency).getSymbol();
        } catch (IllegalArgumentException ignored) {
            Log.d(TAG, "An exception occurred when parsing the pay rate currency. Reverting to value in json.");
        }
        List<DriverPayRateBreakdownCoverageItem> driverPayRateBreakdownCoverageItems = null;
        if (driverPayRateBreakdownCoverageArray.length != 0) {
            driverPayRateBreakdownCoverageItems = new ArrayList<>(Arrays.asList(driverPayRateBreakdownCoverageArray));
        }
        return new SequenceDetailsRewardPaid(value, currency, driverPayRateBreakdownCoverageItems);
    }
}
