package com.telenav.osv.manager.network.parser;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.telenav.osv.item.network.PayRateCoverageInterval;
import com.telenav.osv.item.network.PayRateData;
import com.telenav.osv.item.network.PayRateItem;
import com.telenav.osv.utils.Log;

/**
 * Parser which converts the json result of a pay rate info request into a {@link PayRateData} object.
 * <p>
 * Created by catalinj on 10/18/17.
 */
public class PayRateDataParser extends ApiResponseParser<PayRateData> {

    public static final String JSON_NULL = "null";

    private static final String TAG = PayRateDataParser.class.getSimpleName();

    /**
     * key of the object which contains the pay rate data in the json returned from the endpoint.
     */
    private static final String PAY_RATES_DATA_CONTAINER = "osv";

    /**
     * key of the json field which represents the array of pay rates
     */
    private static final String KEY_CURRENCY_TYPE = "currency";

    /**
     * key of the json field in which are stored the pay rates when obd is connected.
     */
    private static final String KEY_PAY_RATES_ARRAY = "payrates";

    /**
     * key of the json field holding the value of a certain pay rate item. the value represents the monetary value a certain
     * coverage interval has, in the currency expressed in the current field of the json.
     */
    private static final String KEY_PAY_RATE_OBD_VALUE = "obdValue";

    /**
     * key of the json field holding the value of a certain pay rate item. the value represents the monetary value a certain
     * coverage interval has, in the currency expressed in the current field of the json.
     */
    private static final String KEY_PAY_RATE_NON_OBD_VALUE = "nonObdValue";

    /**
     * key of the json field holding the coverage interval of a specific pay rate item.
     */
    private static final String KEY_PAY_RATE_COVERAGE_INFO = "coverage";

    /**
     * key of the field representing the maximum number of passes for which a certain coverage interval applies
     */
    private static final String KEY_PAY_RATE_COVERAGE_INFO_MAX_PASS = "maxPass";

    /**
     * key of the field representing the minimum number of passes for which a certain coverage interval applies
     */
    private static final String KEY_PAY_RATE_COVERAGE_MIN_PASS = "minPass";

    @Override
    public PayRateData getHolder() {
        return new PayRateData();
    }

    @Override
    public PayRateData parse(String json) {
        PayRateData payRateData = super.parse(json);

        if (json != null && !json.isEmpty()) {

            String currency;

            try {
                JSONObject container = new JSONObject(json).getJSONObject(PAY_RATES_DATA_CONTAINER);
                currency = container.getString(KEY_CURRENCY_TYPE);

                try {
                    currency = Currency.getInstance(currency).getSymbol();
                } catch (IllegalArgumentException ignored) {
                    Log.d(TAG, "An exception occurred when parsing the pay rate currency. Reverting to value in json.");
                }

                JSONArray obdPayRatesListContainer = container.getJSONArray(KEY_PAY_RATES_ARRAY);
                List<PayRateItem> payRateItems = parsePayRatesListWithKey(obdPayRatesListContainer);

                payRateData.setCurrency(currency);
                payRateData.setPayRates(payRateItems);

            } catch (Exception e) {
                Log.d(TAG, "An exception occurred when parsing the pay rates.", e);
            }
        }
        return payRateData;
    }

    /**
     * Given the json array in the parameter, which holds an array of {@link PayRateItem} objects, it parses the json and
     * returns the parsed equivalent {@link List}<{@link PayRateItem}>.
     * @param payRatesListContainer the {@link JSONArray} which holds the array of {@link PayRateItem}s, as json objects
     * @return the parsed list of {@link PayRateItem}
     * @throws JSONException if anything bad occurs when parsing
     */
    private List<PayRateItem> parsePayRatesListWithKey(JSONArray payRatesListContainer) throws JSONException {

        final int payRateItemCount = payRatesListContainer.length();

        final List<PayRateItem> payRatesList = new ArrayList<>(payRateItemCount);

        for (int i = 0; i < payRateItemCount; i++) {

            JSONObject payRateItemJson = payRatesListContainer.getJSONObject(i);
            JSONObject coverageJson = payRateItemJson.getJSONObject(KEY_PAY_RATE_COVERAGE_INFO);

            int minInterval = Integer.parseInt(coverageJson.getString(KEY_PAY_RATE_COVERAGE_MIN_PASS));
            String maxPassString = coverageJson.getString(KEY_PAY_RATE_COVERAGE_INFO_MAX_PASS);
            int maxInterval = isNotJsonStringNull(maxPassString) ? Integer.parseInt(maxPassString) : minInterval;

            PayRateCoverageInterval payRateCoverageInterval = new PayRateCoverageInterval(minInterval, maxInterval);

            float obdCoverageValue = (float) payRateItemJson.getDouble(KEY_PAY_RATE_OBD_VALUE);
            float nonObdCoverageValue = (float) payRateItemJson.getDouble(KEY_PAY_RATE_NON_OBD_VALUE);

            PayRateItem payRateItem = new PayRateItem(payRateCoverageInterval, obdCoverageValue, nonObdCoverageValue);
            payRatesList.add(payRateItem);
        }

        return payRatesList;
    }

    private boolean isNotJsonStringNull(String maxPassString) {
        return maxPassString != null && !maxPassString.equals(JSON_NULL);
    }
}
