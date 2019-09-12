package com.telenav.osv.manager.network;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.content.Context;
import android.os.Build;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.http.IssueCreationRequest;
import com.telenav.osv.http.IssueUploadRequest;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.item.network.IssueData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.listener.network.OsvRequestResponseListener;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.IssueCreationParser;
import com.telenav.osv.network.FactoryServerEndpointUrl;
import com.telenav.osv.utils.Log;

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

    /**
     * create issue on backend
     */
    private static String URL_ISSUE_CREATE = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/issue/";

    /**
     * create issue on backend
     */
    private static String URL_ISSUE_UPLOAD = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/upload/issue-file/";

    private ConcurrentLinkedQueue<StringRequest> mHolderQueue = new ConcurrentLinkedQueue<>();

    private NetworkResponseDataListener<IssueData> mListener;

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();

    private IssueCreationParser mIsueCreationParser = new IssueCreationParser();

    public IssueReporter(Context context) {
        super(context);
        mQueue.addRequestFinishedListener(this);
        setEnvironment();
    }

    @Override
    protected void setEnvironment() {
        String serverEndpointUrl = factoryServerEndpointUrl.getServerEndpoint();
        URL_ISSUE_CREATE = URL_ISSUE_CREATE.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_ISSUE_UPLOAD = URL_ISSUE_UPLOAD.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        Log.d(TAG, String.format("setEnvironment. Status: set urls. Server endpoint: %s.", serverEndpointUrl));
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
                new IssueCreationRequest(URL_ISSUE_CREATE, new OsvRequestResponseListener<IssueCreationParser, IssueData>(mIsueCreationParser) {

                    @Override
                    public void onSuccess(int status, final IssueData issueData) {
                        runInBackground(() -> {
                            mQueue.stop();
                            int index = 0;
                            ArrayList<OSVFile> files = Log.getLogFiles(mContext);
                            for (OSVFile file : files) {
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
                }, "[Android;" + Build.MODEL + ";" + Build.VERSION.RELEASE + "] " + description, getAccessToken());
        issueRequest.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIME_OUT, MAX_NO_RETRY, BACK_OFF_MULTIPLIER));
        mQueue.add(issueRequest);
    }

    private void uploadIssueFile(final int id, final OSVFile file, final int index) {
        if (!file.exists()) {
            Log.w(TAG, "uploadIssueFile: file doesn't exist: " + file.getPath());
            return;
        }
        final IssueUploadRequest request =
                new IssueUploadRequest(URL_ISSUE_UPLOAD, new OsvRequestResponseListener<HttpResponseParser, ApiResponse>(mHttpResponseParser) {

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
                }, getAccessToken(), file, id, index);
        request.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, BACK_OFF_MULTIPLIER));
        mHolderQueue.add(request);
        mQueue.add(request);
    }
}
