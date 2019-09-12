package com.telenav.osv.manager.network.parser;

import java.text.ParseException;
import java.util.Date;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import android.location.Location;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;
import static com.telenav.osv.listener.network.NetworkResponseDataListener.API_EXCEPTIONAL_FAILURE;

/**
 * Created by kalmanb on 8/3/17.
 */
public abstract class ApiResponseParser<T extends ApiResponse> {

    private static final String TAG = "ApiResponseParser";

    /**
     * The multiplier required to transform km to meters.
     */
    private static final double M_MULTIPLIER = 1000d;

    private static final int HTTP_TIMEOUT = 408;

    private static final int HTTP_FORBIDDEN = 403;

    public abstract T getHolder();

    public T parse(VolleyError error) {
        T response = getHolder();
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                //                Log.w(TAG, "parse: " + new String(error.networkResponse.data));
                String result = new String(error.networkResponse.data);
                JSONObject ob = new JSONObject(result);
                int httpCode = ob.getJSONObject("status").getInt("httpCode");
                String httpMessage = ob.getJSONObject("status").getString("httpMessage");
                int apiCode = ob.getJSONObject("status").getInt("apiCode");
                String apiMessage = ob.getJSONObject("status").getString("apiMessage");
                response.setApiCode(apiCode);
                response.setApiMessage(apiMessage);
                response.setHttpCode(httpCode);
                response.setHttpMessage(httpMessage);
            } catch (Exception e) {
                Log.d(TAG, "Error when parsing volley error. Volley error: " + Log.getStackTraceString(error) + "\nParsing error:" + Log.getStackTraceString(e));
                response.setApiCode(API_EXCEPTIONAL_FAILURE);
                response.setApiMessage(new String(error.networkResponse.data));
                response.setHttpCode(error.networkResponse.statusCode);
                if (error.networkResponse.headers != null) {
                    response.setHttpMessage(Utils.mapToString(error.networkResponse.headers));
                }
                e.printStackTrace();
            }
        } else if (error instanceof TimeoutError) {
            response.setHttpCode(HTTP_TIMEOUT);
            response.setHttpMessage("Timeout error");
        } else {
            response.setHttpMessage(error.getMessage());
            response.setHttpCode(HTTP_FORBIDDEN);
        }
        return response;
    }

    public T parse(String json) {

        T response = getHolder();
        try {
            JSONObject ob = new JSONObject(json);
            int httpCode = ob.getJSONObject("status").getInt("httpCode");
            String httpMessage = ob.getJSONObject("status").getString("httpMessage");
            int apiCode = ob.getJSONObject("status").getInt("apiCode");
            String apiMessage = ob.getJSONObject("status").getString("apiMessage");
            response.setApiCode(apiCode);
            response.setApiMessage(apiMessage);
            response.setHttpCode(httpCode);
            response.setHttpMessage(httpMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Transform the data for a sequence in Json Format related to {@code SequenceDetailsCompressionJpeg}.
     * @param sequenceJsonFormat sequence in Json Format.
     * @return {@code SequenceDetailsCompressionJpeg} object.
     * @throws JSONException exception thrown by the Json when data in invalid.
     */
    SequenceDetailsCompressionJpeg getJpegCompression(JSONObject sequenceJsonFormat) throws JSONException {
        String frameLength = sequenceJsonFormat.getString("photo_no");
        String thumbLink = UserDataManager.URL_DOWNLOAD_PHOTO + sequenceJsonFormat.getString("thumb_name");

        return new SequenceDetailsCompressionJpeg(Integer.valueOf(frameLength), thumbLink, 0);
    }

    /**
     * Transform the data for a sequence in Json Format related to {@code SequenceDetails}.
     * @param sequenceJsonFormat sequence in Json Format.
     * @return {@code SequenceDetails} object.
     * @throws JSONException exception thrown by the Json when data in invalid.
     */
    SequenceDetails getSequenceDetails(JSONObject sequenceJsonFormat) throws JSONException {
        boolean obd = false;
        String appVersion = StringUtils.EMPTY_STRING;
        double distanceNumber = 0;
        String partialAddress = StringUtils.EMPTY_STRING;
        int id = sequenceJsonFormat.getInt("id");
        double lat = sequenceJsonFormat.getDouble("current_lat");
        double lon = sequenceJsonFormat.getDouble("current_lng");
        long timestamp = new DateTime().getMillis();
        String processingStatus = sequenceJsonFormat.getString("image_processing_status");
        String distance = sequenceJsonFormat.getString("distance");
        String dateFormat = sequenceJsonFormat.getString("date_added");
        Location initialLocation = new Location(StringUtils.EMPTY_STRING);
        initialLocation.setLatitude(lat);
        initialLocation.setLongitude(lon);

        try {
            String address = sequenceJsonFormat.getString("location");
            String[] list = address.split(", ");
            partialAddress = list[0] + ", " + list[2];
            if (distance != null) {
                distanceNumber = Double.parseDouble(distance) * M_MULTIPLIER;
            }
            appVersion = sequenceJsonFormat.getString("app_version");
            obd = sequenceJsonFormat.getInt("obd_info") > 0;
            try {
                Date date = Utils.onlineDateFormat.parse(dateFormat);
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
}
