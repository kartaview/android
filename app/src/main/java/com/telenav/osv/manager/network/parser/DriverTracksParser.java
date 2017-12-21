package com.telenav.osv.manager.network.parser;

import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.item.DriverOnlineSequence;
import com.telenav.osv.item.DriverPayRateBreakdownCoverageItem;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * JSON parser for driver tracks list
 * Created by kalmanb on 8/1/17.
 */
public class DriverTracksParser extends ApiResponseParser<TrackCollection> {

    private static final String TAG = "DriverTracksParser";

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
                    int id = item.getInt("id");
                    String dateStr = item.getString("date_added");
                    Date date = new Date();
                    try {
                        date = Utils.onlineDriverDateFormat.parse(dateStr);
                    } catch (Exception e) {
                        Log.w(TAG, "listDriverSequences: " + e.getLocalizedMessage());
                    }
                    String imgNum = item.getString("photo_no");
                    String distance = item.getString("distance");
                    double lat = item.getDouble("current_lat");
                    double lon = item.getDouble("current_lng");
                    double value = item.getDouble("value");
                    String currency = item.getString("currency");
                    try {
                        currency = Currency.getInstance(currency).getSymbol();
                    } catch (IllegalArgumentException ignored) {
                        Log.d(TAG, "An exception occurred when parsing the pay rate currency. Reverting to value in json.");
                    }
                    String processing = item.getString("image_processing_status");
                    String status = item.getString("status");
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
                            Log.w(TAG, "listDriverSequences: " + Log.getStackTraceString(e));
                        }
                    }
                    String partialAddress = "";
                    try {
                        String address = item.getString("address");
                        String[] list = address.split(", ");
                        partialAddress = list[0] + ", " + list[2];
                    } catch (Exception ignored) {
                    }
                    String thumbLink = UserDataManager.URL_DOWNLOAD_PHOTO + item.getString("thumb_name");
                    double distanceNum = 0;
                    try {
                        if (distance != null) {
                            distanceNum = Double.parseDouble(distance);
                        }
                    } catch (NumberFormatException ignored) {
                    }

                    DriverPayRateBreakdownCoverageItem[] driverPayRateBreakdownCoverageArray;
                    if (item.has("coverage")) {
                        try {
                            JSONArray coverageItemsArray = item.getJSONArray("coverage");

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

                                } else {
                                    driverPayRateBreakdownCoverageArray = null;
                                }
                            } else {
                                driverPayRateBreakdownCoverageArray = null;
                            }

                        } catch (JSONException e) {
                            Log.d(TAG, "Exception occurred when parsing the driver pay rate breakdown at index:" + i + ", exception:", e);
                            driverPayRateBreakdownCoverageArray = null;
                        }
                    } else {
                        driverPayRateBreakdownCoverageArray = null;
                    }

                    DriverOnlineSequence seq = new DriverOnlineSequence(id,
                            date,
                            Integer.valueOf(imgNum),
                            partialAddress,
                            thumbLink,
                            obd,
                            platform,
                            platformVersion,
                            appVersion,
                            (int) (distanceNum * 1000d),
                            value,
                            currency,
                            driverPayRateBreakdownCoverageArray != null ? Arrays.asList(driverPayRateBreakdownCoverageArray) : null);

                    seq.setServerStatus(status);
                    seq.setLocation(new SKCoordinate(lat, lon));
                    collectionData.getTrackList().add(seq);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return collectionData;
    }
}
