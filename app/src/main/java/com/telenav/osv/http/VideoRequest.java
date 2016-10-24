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
import com.android.volley.AuthFailureError;
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
@SuppressWarnings("HardCodedStringLiteral")
public class VideoRequest<T> extends StringRequest {


    private static final String FILE_PART_NAME = "video";

    private static final String PARAM_TOKEN = "access_token";

    private static final String TAG = "VideoRequest";

    private final int mSequenceId;

    private final int mSequenceIndex;

    private final Listener<String> mListener;

    private final OSVFile mVideoFile;

    private final String mToken;

    private ProgressiveEntity mProgressiveEntity;

    private ProgressiveEntity.DataProgressListener mDataProgressListener;

    public VideoRequest(String url, ErrorListener errorListener, Listener<String> listener, ProgressiveEntity.DataProgressListener dataProgressListener, String token, OSVFile
            videoFile, int sequenceID, int sequenceIndex) {
        super(Method.POST, url, listener, errorListener);

        mListener = listener;
        mVideoFile = videoFile;
        mSequenceId = sequenceID;
        mSequenceIndex = sequenceIndex;
        mToken = token;
        mDataProgressListener = dataProgressListener;

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

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = super.getParams();
        if (params == null
                || params.equals(Collections.emptyMap())) {
            params = new HashMap<>();
        }
        return params;
    }

    private void buildMultipartEntity() {
        MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();
        mBuilder.addTextBody(PARAM_TOKEN, mToken);
        mBuilder.addTextBody("sequenceId", "" + mSequenceId);
        mBuilder.addTextBody("sequenceIndex", "" + mSequenceIndex);
        if (!mVideoFile.exists()) {
            Log.d(TAG, "buildMultipartEntity: video doesn't exist");
        }
        mBuilder.addBinaryBody(FILE_PART_NAME, mVideoFile, ContentType.create("video/mp4"), mVideoFile.getName());
        mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        mBuilder.setLaxMode().setBoundary("xx").setCharset(Charset.forName("UTF-8"));
        HttpEntity mEntity = mBuilder.build();
        mProgressiveEntity = new ProgressiveEntity(mEntity, mDataProgressListener, Utils.fileSize(mVideoFile));

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