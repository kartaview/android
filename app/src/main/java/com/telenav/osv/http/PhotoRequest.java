package com.telenav.osv.http;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import android.os.Handler;
import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 10/6/2015.
 */
public class PhotoRequest<T> extends StringRequest {


    private static final String FILE_PART_NAME = "photo";

    private static final String TAG = "PhotoRequest";

    private static final String PARAM_TOKEN = "access_token";

    public final int mSequenceId;

    public final int mSequenceIndex;

    private final Response.Listener<String> mListener;

    private final OSVFile mImageFile;

    private final double mLat;

    private final double mLon;

    private final String mToken;

    protected Map<String, String> headers;

    private MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();

    private float mAccuracy;

    private ProgressiveEntity mProgressiveEntity;

    private ProgressiveEntity.DataProgressListener mDataProgressListener;

    private Handler mResponseHandler;

    public PhotoRequest(String url, ErrorListener errorListener, Listener<String> listener, ProgressiveEntity.DataProgressListener dataProgressListener, String token
            , OSVFile imageFile, int sequenceID, int sequenceIndex, double lat, double lon, float accuracy, Handler responseHandler) {
        super(Method.POST, url, listener, errorListener);

        mListener = listener;
        mImageFile = imageFile;
        mSequenceId = sequenceID;
        mSequenceIndex = sequenceIndex;
        mLat = lat;
        mLon = lon;
        mAccuracy = accuracy;

        mToken = token;
        mDataProgressListener = dataProgressListener;
        mResponseHandler = responseHandler;
        buildMultipartEntity();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = super.getHeaders();

        if (headers == null
                || headers.equals(Collections.emptyMap())) {
            headers = new HashMap<String, String>();
        }

        headers.put("Accept", "application/json");

        return headers;
    }

    private void buildMultipartEntity() {
        mBuilder.addTextBody(PARAM_TOKEN, mToken);
        mBuilder.addTextBody("coordinate", mLat + "," + mLon);
        mBuilder.addTextBody("sequenceId", "" + mSequenceId);
        mBuilder.addTextBody("sequenceIndex", "" + mSequenceIndex);
        mBuilder.addTextBody("gpsAccuracy", "" + mAccuracy);
        if (!mImageFile.exists()) {
            Log.d(TAG, "buildMultipartEntity: image doesn't exist");
        }
        mBuilder.addBinaryBody(FILE_PART_NAME, mImageFile, ContentType.create("image/jpeg"), mImageFile.getName());
        Log.d(TAG, "buildMultipartEntity: sending request: "
                + " " + PARAM_TOKEN + " " + mToken
                + " coordinate " + mLat + "," + mLon
                + " sequenceId " + mSequenceId
                + " sequenceIndex " + mSequenceIndex
                + " gpsAccuracy " + mAccuracy
        );
        mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        mBuilder.setLaxMode().setBoundary("xx").setCharset(Charset.forName("UTF-8"));
        HttpEntity mEntity = mBuilder.build();
        mProgressiveEntity = new ProgressiveEntity(mEntity, mDataProgressListener, Utils.fileSize(mImageFile), mResponseHandler);
    }

    @Override
    public String getBodyContentType() {
        return mProgressiveEntity.getContentType().getValue();
    }

    @Override
    public HttpEntity getMultipartEntity() {
        return mProgressiveEntity;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {//todo this should not be called, but lets leave it to be sure
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            mProgressiveEntity.writeTo(bos);
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream bos, building the multipart request.");
        }

        return bos.toByteArray();
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}