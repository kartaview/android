package com.telenav.osv.manager.network.parser;

import com.telenav.osv.item.Payment;
import com.telenav.osv.item.network.PaymentCollection;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Json parser for the payments list request
 * Created by kalmanb on 8/1/17.
 */
public class PaymentCollectionParser extends ApiResponseParser<PaymentCollection> {

  private static final String TAG = "PaymentCollectionParser";

  @Override
  public PaymentCollection getHolder() {
    return new PaymentCollection();
  }

  public PaymentCollection parse(String json) {
    PaymentCollection collection = super.parse(json);
    try {
      JSONObject obj = new JSONObject(json);
      JSONObject osv = obj.getJSONObject("osv");
      int totalFilteredItems = osv.getInt("totalFilteredItems");
      collection.setTotalFilteredItems(totalFilteredItems);
      String currency = osv.getString("currency");
      collection.setCurrency(currency);
      if (collection.getTotalFilteredItems() > 0) {
        JSONArray array = osv.getJSONArray("list");
        for (int i = 0; i < array.length(); i++) {
          JSONObject item = array.getJSONObject(i);
          int index = item.getInt("index");
          String dateStr = item.getString("date");
          Date date = new Date();
          try {
            date = Utils.paymentServerDateFormat.parse(dateStr);
          } catch (Exception e) {
            Log.w(TAG, "parse: " + e.getLocalizedMessage());
          }
          double distance = item.getDouble("distance");
          double value = item.getDouble("value");

          Payment payment = new Payment(index, date, distance, value, currency);
          collection.getPaymentList().add(payment);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    Log.d(TAG, "parse: successful, data size = " + collection.getTotalFilteredItems());
    return collection;
  }
}
