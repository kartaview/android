package com.telenav.osv.manager.network.parser;

import java.util.Currency;
import org.json.JSONException;
import org.json.JSONObject;
import com.telenav.osv.item.network.DriverData;
import com.telenav.osv.utils.Log;

/**
 * JSON parser for driver profile data
 * Created by kalmanb on 8/1/17.
 */
public class DriverDataParser extends ApiResponseParser<DriverData> {

    private static final String TAG = DriverDataParser.class.getSimpleName();

    @Override
    public DriverData getHolder() {
        return new DriverData();
    }

    @Override
    public DriverData parse(String json) {
        DriverData driverData = super.parse(json);

        if (json != null && !json.isEmpty()) {

            String name;
            String currency;
            String paymentModel;
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
                try {
                    paymentModel = osv.getString("payment_model");
                } catch (JSONException jsonException) {
                    paymentModel = null;
                    Log.d(TAG, "Error when reading the payment model. Set model to null.", jsonException);
                }
                currency = osv.getString("currency");

                try {
                    currency = Currency.getInstance(currency).getSymbol();
                } catch (IllegalArgumentException ignored) {
                    Log.d(TAG, "Error when decoding the currency. Ignored.");
                }

                totalAccepted = osv.getDouble("total_accepted_distance");
                rejected = osv.getDouble("total_rejected_distance");
                obdDistance = osv.getDouble("total_obd_distance");
                accepted = osv.getDouble("tbp_distance");
                rate = osv.getDouble("tbp_payrate");
                value = osv.getDouble("tbp_value");
                tracks = osv.getDouble("total_tracks");
                photos = osv.getDouble("total_images");
                totalValue = osv.getDouble("total_paid");

                driverData.setDisplayName(name);
                driverData.setPaymentModelVersion(paymentModel);
                driverData.setCurrency(currency);
                driverData.setTotalAcceptedDistance(totalAccepted);
                driverData.setTotalRejectedDistance(rejected);
                driverData.setTotalObdDistance(obdDistance);
                driverData.setCurrentAcceptedDistance(accepted);
                driverData.setCurrentPayRate(rate);
                driverData.setCurrentPaymentValue(value);
                driverData.setTotalTracks(tracks);
                driverData.setTotalPhotos(photos);
                driverData.setTotalPaidValue(totalValue);

            } catch (Exception e) {
                Log.d(TAG, "Error when parsing the driver data.", e);
            }
        }
        return driverData;
    }
}
