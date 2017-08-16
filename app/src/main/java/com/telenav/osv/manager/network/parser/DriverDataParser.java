package com.telenav.osv.manager.network.parser;

import java.util.Currency;
import org.json.JSONObject;
import com.telenav.osv.item.network.DriverData;

/**
 * JSON parser for driver profile data
 * Created by kalmanb on 8/1/17.
 */
public class DriverDataParser extends ApiResponseParser<DriverData> {
    @Override
    public DriverData getHolder() {
        return new DriverData();
    }

    public DriverData parse(String json) {
        DriverData driverData = super.parse(json);
        if (json != null && !json.isEmpty()) {
            String name;
            String currency;
            double totalAccepted;
            double accepted;
            double rejected;
            double obdDistance;
            double rate;
            double value;
            double totalValue;
            double tracks;
            double photos;
            try {
                JSONObject obj = new JSONObject(json);
                JSONObject osv = obj.getJSONObject("osv");
                name = osv.getString("full_name");
                driverData.setDisplayName(name);
                currency = osv.getString("currency");
                try {
                    currency = Currency.getInstance(currency).getSymbol();
                } catch (IllegalArgumentException ignored) {}
                driverData.setCurrency(currency);
                totalAccepted = osv.getDouble("total_accepted_distance");
                driverData.setTotalAcceptedDistance(totalAccepted);
                rejected = osv.getDouble("total_rejected_distance");
                driverData.setTotalRejectedDistance(rejected);
                obdDistance = osv.getDouble("total_obd_distance");
                driverData.setTotalObdDistance(obdDistance);
                accepted = osv.getDouble("tbp_distance");
                driverData.setCurrentAcceptedDistance(accepted);
                rate = osv.getDouble("tbp_payrate");
                driverData.setCurrentPayRate(rate);
                value = osv.getDouble("tbp_value");
                driverData.setCurrentPaymentValue(value);
                tracks = osv.getDouble("total_tracks");
                driverData.setTotalTracks(tracks);
                photos = osv.getDouble("total_images");
                driverData.setTotalPhotos(photos);
                totalValue = osv.getDouble("total_paid");
                driverData.setTotalPaidValue(totalValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return driverData;
    }
}
