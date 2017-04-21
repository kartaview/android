package com.telenav.osv.ui.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.listener.OAuthResultListener;

/**
 * oauth fragment
 * Created by Kalman on 10/15/2015.
 */
public class OAuthDialogFragment extends DialogFragment {

    public static final String TAG = "OAuthDialogFragment";

    private WebView webViewOauth;

    private String URL;

    private OAuthResultListener resultListener;

    private OnDetachListener mListener;

    private OSVActivity activity;

//    private ProgressBar progressbar;

    public void setOnDetachListener(OnDetachListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public void setResultListener(OAuthResultListener resultListener) {
        this.resultListener = resultListener;
    }

    @Override
    public void onViewCreated(View arg0, Bundle arg1) {
        super.onViewCreated(arg0, arg1);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        //load the url of the oAuth login page
//        progressbar.setVisibility(View.VISIBLE);
        activity = (OSVActivity) getActivity();
        webViewOauth
                .loadUrl(URL);
        activity.enableProgressBar(true);
        //set the web client
        webViewOauth.setWebViewClient(new MyWebViewClient());
        //activates JavaScript (just in case)
        WebSettings webSettings = webViewOauth.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Retrieve the webview
        View v = inflater.inflate(R.layout.fragment_oauth, container, false);
        webViewOauth = (WebView) v.findViewById(R.id.web_oauth);
        getDialog().setTitle(R.string.osm_login_label);
        return v;
    }

    @Override
    public void onDetach() {
        if (mListener != null) {
            mListener.onDetach();
        }
        super.onDetach();
    }

    public interface OnDetachListener {
        void onDetach();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //check if the login was successful and the access token returned
            //this test depend of your API
            if (url.contains("osmlogin") && url.contains("telenav")) {
                //save your token
                resultListener.onResult(url);
                activity.getSupportFragmentManager().popBackStackImmediate();
                return true;
            }
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            activity.enableProgressBar(true);
            super.onPageFinished(view, url);
        }
    }
}