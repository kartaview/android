package com.telenav.osv.manager.network;

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
   * create issue on backend
   */
  private static String URL_ISSUE_CREATE = "http://" + "&&" + URL_VER + "issue/";

  /**
   * create issue on backend
   */
  private static String URL_ISSUE_UPLOAD = "http://" + "&&" + URL_VER + "upload/issue-file/";

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
    super.setEnvironment();
    URL_ISSUE_CREATE = URL_ISSUE_CREATE.replace("&&", URL_ENV[mCurrentServer]);
    URL_ISSUE_UPLOAD = URL_ISSUE_UPLOAD.replace("&&", URL_ENV[mCurrentServer]);
  }

  @Override
  public void destroy() {
    super.destroy();
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
                uploadIssueFile(listener, issueData.getOnlineID(), file, index);
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
    issueRequest.setRetryPolicy(new DefaultRetryPolicy(18000, 1, 1f));
    mQueue.add(issueRequest);
  }

  private void uploadIssueFile(final NetworkResponseDataListener<IssueData> listener, final int id, final OSVFile file, final int index) {
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
            uploadIssueFile(listener, id, file, index);
          }
        }, getAccessToken(), file, id, index);
    request.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, 1f));
    mHolderQueue.add(request);
    mQueue.add(request);
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
}
