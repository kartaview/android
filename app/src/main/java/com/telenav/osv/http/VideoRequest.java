package com.telenav.osv.http;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import com.android.volley.AuthFailureError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 10/6/2015.
 */
public class VideoRequest<T> extends StringRequest {


    private static final String FILE_PART_NAME = "video";

    private static final String TAG = "VideoRequest";

    private final int mSequenceId;

    private final int mSequenceIndex;

    private final Listener<String> mListener;

    private final OSVFile mVideoFile;

    private MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();

    public VideoRequest(String url, ErrorListener errorListener, Listener<String> listener, OSVFile videoFile, int sequenceID, int sequenceIndex) {
        super(Method.POST, url, listener, errorListener);

        mListener = listener;
        mVideoFile = videoFile;
        mSequenceId = sequenceID;
        mSequenceIndex = sequenceIndex;

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
        mBuilder.addTextBody("sequenceId", "" + mSequenceId);
        mBuilder.addTextBody("sequenceIndex", "" + mSequenceIndex);
        if (!mVideoFile.exists()) {
            Log.d(TAG, "buildMultipartEntity: video doesn't exist");
        }
        mBuilder.addBinaryBody(FILE_PART_NAME, mVideoFile, ContentType.create("video/mp4"), mVideoFile.getName());
        mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        mBuilder.setLaxMode().setBoundary("xx").setCharset(Charset.forName("UTF-8"));
    }

    @Override
    public String getBodyContentType() {
        String contentTypeHeader = mBuilder.build().getContentType().getValue();
        return contentTypeHeader;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            mBuilder.build().writeTo(bos);
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