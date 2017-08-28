package com.telenav.osv.http;

import android.os.Build;
import android.os.Handler;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.listener.network.GenericResponseListener;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

/**
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class SequenceRequest<T> extends StringRequest {

  private static final String PARAM_CURRENT_COORD = "currentCoordinate";

  private static final String PARAM_METADATA_FILE = "metaData";

  private static final String PARAM_OBD_INFO = "obdInfo";

  private static final String PARAM_APP_VERSION = "appVersion";

  private static final String PARAM_OS_VERSION = "platformVersion";

  private static final String PARAM_PLATFORM = "platformName";

  private static final String PARAM_TOKEN = "access_token";

  private static final String PARAM_SCORE = "clientTotal";

  private static final String TAG = "SequenceRequest";

  private static final String PARAM_UPLOAD_SOURCE = "uploadSource";

  private static final String PARAM_SCORE_DETAILS = "clientTotalDetails";

  private final int mLocalSequenceId;

  private final int mScore;

  private String mScoreDetails;

  private String mAppVersion = "";

  private Listener<String> mListener;

  private OSVFile mMetadata;

  private boolean mOBD;

  private String mStartCoord;

  private String mToken;

  private ProgressiveEntity mProgressiveEntity;

  private ProgressiveEntity.DataProgressListener mDataProgressListener;

  private Handler mResponseHandler;

  public SequenceRequest(String url, GenericResponseListener listener, ProgressiveEntity.DataProgressListener dataProgressListener,
                         int localSequenceId, String token, String startCoord, OSVFile file, String appVersion, boolean obd, int score,
                         String scoreDetails, Handler responseHandler) {
    super(Request.Method.POST, url, listener, listener);
    mListener = listener;
    mLocalSequenceId = localSequenceId;
    mToken = token;
    mStartCoord = startCoord;
    mMetadata = file;
    mAppVersion = appVersion;
    mOBD = obd;
    mDataProgressListener = dataProgressListener;
    mScore = score;
    mScoreDetails = scoreDetails;
    mResponseHandler = responseHandler;
    buildMultipartEntity();
  }

  @Override
  public Map<String, String> getHeaders() throws AuthFailureError {
    Map<String, String> headers = super.getHeaders();

    if (headers == null || headers.equals(Collections.emptyMap())) {
      headers = new HashMap<>();
    }

    return headers;
  }

  @Override
  protected Map<String, String> getParams() {
    return new HashMap<>();
  }

  @Override
  public String getBodyContentType() {
    return mProgressiveEntity.getContentType().getValue();
  }

  @Override
  public byte[] getBody() throws AuthFailureError {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      mProgressiveEntity.writeTo(bos);
    } catch (Exception e) {
      VolleyLog.e("IOException writing to ByteArrayOutputStream bos, building the multipart request. " + e.getLocalizedMessage());
    }

    return bos.toByteArray();
  }

  @Override
  public HttpEntity getMultipartEntity() {
    return mProgressiveEntity;
  }

  private void buildMultipartEntity() {
    MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();
    mBuilder.addTextBody(PARAM_TOKEN, mToken);
    mBuilder.addTextBody(PARAM_UPLOAD_SOURCE, "Android");
    mBuilder.addTextBody(PARAM_CURRENT_COORD, mStartCoord);
    mBuilder.addTextBody(PARAM_OBD_INFO, String.valueOf(mOBD ? 1 : 0));
    mBuilder.addTextBody(PARAM_SCORE, "" + mScore);
    mBuilder.addTextBody(PARAM_PLATFORM, "Android");
    mBuilder.addTextBody(PARAM_OS_VERSION, Build.VERSION.RELEASE);
    mBuilder.addTextBody(PARAM_APP_VERSION, mAppVersion);
    mBuilder.addTextBody(PARAM_SCORE_DETAILS, mScoreDetails);
    if (mMetadata != null) {
      if (!mMetadata.exists()) {
        Log.w(TAG, "buildMultipartEntity: meta doesn't exist");
      } else {
        if (mMetadata.getName().contains(".gz")) {
          mBuilder.addBinaryBody(PARAM_METADATA_FILE, mMetadata, ContentType.create("application/x-gzip"), mMetadata.getName());
        } else {
          mBuilder.addBinaryBody(PARAM_METADATA_FILE, mMetadata, ContentType.create("text/plain"), mMetadata.getName());
        }
      }
    }
    //        Log.d(TAG, "buildMultipartEntity: sending request: "
    //                + " " + PARAM_TOKEN + " " + mToken
    //                + " " + PARAM_UPLOAD_SOURCE + " " + "Android"
    //                + " " + PARAM_CURRENT_COORD + " " + mStartCoord
    //                + " " + PARAM_OBD_INFO + " " + String.valueOf(mOBD ? 1 : 0)
    //                + " " + PARAM_SCORE + " " + mScore
    //                + " " + PARAM_PLATFORM + " " + "Android"
    //                + " " + PARAM_OS_VERSION + " " + Build.VERSION.RELEASE
    //                + " " + PARAM_APP_VERSION + " " + mAppVersion
    //                + " " + PARAM_SCORE_DETAILS + " " + mScoreDetails
    //        );
    mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    mBuilder.setLaxMode().setBoundary("xx").setCharset(Charset.forName("UTF-8"));
    HttpEntity mEntity = mBuilder.build();
    mProgressiveEntity = new ProgressiveEntity(mEntity, mDataProgressListener, Utils.fileSize(mMetadata), mResponseHandler);
  }

  @Override
  protected void deliverResponse(String response) {
    mListener.onResponse(response);
  }
}