package com.telenav.osv.manager.network;

import android.content.Context;
import android.os.Build;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.http.IssueCreationRequest;
import com.telenav.osv.http.IssueUploadRequest;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.item.network.IssueData;
import com.telenav.osv.jarvis.login.utils.LoginUtils;
import com.telenav.osv.listener.network.KVRequestResponseListener;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.IssueCreationParser;
import com.telenav.osv.network.endpoint.UrlIssue;
import com.telenav.osv.utils.Log;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * responsible for uploading issue reports
 * Created by Kalman on 02/05/2017.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class IssueReporter extends NetworkManager implements RequestQueue.RequestFinishedListener<Object> {

    private static final String TAG = "IssueReporter";

    /**
     * The time out for the report an issue request.
     */
    private static final int REQUEST_TIME_OUT = 1800;

    /**
     * The maximum number of retries for the report an issue request.
     */
    private static final int MAX_NO_RETRY = 1;

    //TODO: Clarify the meaning of this constant.
    private static final float BACK_OFF_MULTIPLIER = 1;

    private ConcurrentLinkedQueue<StringRequest> mHolderQueue = new ConcurrentLinkedQueue<>();

    private NetworkResponseDataListener<IssueData> mListener;

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();

    private IssueCreationParser mIsueCreationParser = new IssueCreationParser();

    public IssueReporter(Context context) {
        super(context);
        mQueue.addRequestFinishedListener(this);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void onRequestFinished(Request<Object> request) {
        Log.d(TAG, "onRequestFinished: " + request.toString());
        if (request instanceof IssueUploadRequest) {
            mHolderQueue.remove(request);
            Log.d(TAG, "onRequestFinished: mHolderQueue size is " + mHolderQueue.size());
            if (mHolderQueue.size() == 0) {
                if (mListener != null) {
                    mListener.requestFinished(NetworkResponseDataListener.HTTP_OK, new IssueData());
                }
            }
        }
    }

    public void createIssue(final NetworkResponseDataListener<IssueData> listener, String description) {
        mHolderQueue.clear();
        mListener = listener;
        IssueCreationRequest issueRequest =
                new IssueCreationRequest(factoryServerEndpointUrl.getIssueEndpoint(UrlIssue.ISSUE_CREATE), new KVRequestResponseListener<IssueCreationParser, IssueData>(mIsueCreationParser) {

                    @Override
                    public void onSuccess(int status, final IssueData issueData) {
                        runInBackground(() -> {
                            mQueue.stop();
                            int index = 0;
                            ArrayList<KVFile> files = Log.getLogFiles(mContext);
                            for (KVFile file : files) {
                                uploadIssueFile(issueData.getOnlineID(), file, index);
                                index++;
                            }
                            mQueue.start();
                        });
                    }

                    @Override
                    public void onFailure(final int status, final IssueData issueData) {
                        runInBackground(() -> listener.requestFailed(status, issueData));
                    }
                }, "[Android;" + Build.MODEL + ";" + Build.VERSION.RELEASE + "] " + description, getAccessToken(), LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
        issueRequest.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIME_OUT, MAX_NO_RETRY, BACK_OFF_MULTIPLIER));
        mQueue.add(issueRequest);
    }

    private void uploadIssueFile(final int id, final KVFile file, final int index) {
        if (!file.exists()) {
            Log.w(TAG, "uploadIssueFile: file doesn't exist: " + file.getPath());
            return;
        }
        final IssueUploadRequest request =
                new IssueUploadRequest(factoryServerEndpointUrl.getIssueEndpoint(UrlIssue.ISSUE_UPLOAD_FILE), new KVRequestResponseListener<HttpResponseParser, ApiResponse>(mHttpResponseParser) {

                    @Override
                    public void onSuccess(int status, ApiResponse apiResponse) {
                        Log.d(TAG, "uploadIssueFile: success, entering background to delete file");
                        if (file.exists()) {
                            file.delete();
                        }
                    }

                    @Override
                    public void onFailure(int status, ApiResponse apiResponse) {
                        uploadIssueFile(id, file, index);
                    }
                }, getAccessToken(), file, id, index, LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
        request.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, BACK_OFF_MULTIPLIER));
        mHolderQueue.add(request);
        mQueue.add(request);
    }
}
