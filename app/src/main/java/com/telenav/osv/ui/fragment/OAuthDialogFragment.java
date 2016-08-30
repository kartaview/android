package com.telenav.osv.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import com.telenav.osv.R;
import com.telenav.osv.listener.OAuthResultListener;

/**
 * Created by Kalman on 10/15/2015.
 */
public class OAuthDialogFragment extends DialogFragment {


    public static final String TAG = "OAuthDialogFragment";

    private WebView webViewOauth;

    private String URL;

    private OAuthResultListener resultListener;

    private ProgressBar progressBar;

    private OnDetachListener mListener;

    private FragmentActivity activity;

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
        activity = getActivity();
        webViewOauth
                .loadUrl(URL);
        progressBar.setVisibility(View.VISIBLE);
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
        View v = inflater.inflate(R.layout.oauth, container, false);
        webViewOauth = (WebView) v.findViewById(R.id.web_oauth);
        progressBar = (ProgressBar) v.findViewById(R.id.progressbar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
        getDialog().setTitle(R.string.osm_login_label);
//        progressbar = (ProgressBar) v.findViewById(R.id.progressbar);
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
//            BaseActivity.logEvent(Consts.EVENT_CALLBACK + "Login Failed", true);
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            super.onPageFinished(view, url);
        }
    }
}