package com.telenav.osv.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.telenav.osv.R;
import androidx.annotation.Nullable;

/**
 * Fragment responsible with displaying the user guide.
 * <p>
 * This contains just a {@link WebView} which loads the user guide page for the current environment.
 * Created by catalinj on 10/27/17.
 */
public class DriverGuideFragment extends OSVFragment {

    public static final String TAG = DriverGuideFragment.class.getSimpleName();

    private static final String EMBED = "embed";

    private static final String TYPE_GUIDE = "guide";

    private static final String TYPE_FAQ = "faq";

    private static final String ARG_SERVER_TYPE = "arg::serverType";

    private static final String COUNTRY_CODE = "us";

    private static final String PLATFORM_CODE = "android";

    private static final String SCHEME_MAILTO = "mailto:";

    /**
     * Links to the environments from which user guide data is fetched. Currently, the staging and beta environments point to the live environment.
     */
    private static final String[] DRIVER_GUIDE_WEB_VIEW_URL_BASE_URL = {
            "https://drivers.openstreetcam.org/",
            "https://drivers.openstreetcam.org/",
            "http://testing-drivers.openstreetview.com/",
            "https://drivers.openstreetcam.org/",
            "https://drivers.openstreetcam.org/"
    };

    /**
     * Computed final endpoint URL, from which the user guide is fetched.
     */
    private String endpointUrl;

    /**
     * WebView in which the user guide will be displayed.
     */
    private WebView webView;

    private int serverType;

    /**
     * Creates a new instance of a {@link DriverGuideFragment}.
     * @param serverType An int representing the server type from which the user guide will be fetched.
     * @return a {@link DriverGuideFragment} which displays a user guide fetched from the server represented by the int parameter.
     */
    public static DriverGuideFragment newInstance(int serverType) {
        DriverGuideFragment driverGuideFragment = new DriverGuideFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_SERVER_TYPE, serverType);
        driverGuideFragment.setArguments(b);
        return driverGuideFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int serverType = getArguments().getInt(ARG_SERVER_TYPE, 0);
        this.endpointUrl = DRIVER_GUIDE_WEB_VIEW_URL_BASE_URL[serverType] + COUNTRY_CODE + "/" + TYPE_GUIDE + "?" + EMBED + "=" + PLATFORM_CODE;
        this.serverType = serverType;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_guide, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView = view.findViewById(R.id.webview_user_guide);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(SCHEME_MAILTO) || !url.contains(DRIVER_GUIDE_WEB_VIEW_URL_BASE_URL[serverType])) {
                    if (url.startsWith(SCHEME_MAILTO)) {
                        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                        startActivity(i);
                    } else if (!url.contains(DRIVER_GUIDE_WEB_VIEW_URL_BASE_URL[serverType])) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        webView.loadUrl(endpointUrl);
    }

    @Override
    public boolean onBackPressed() {
        if (webView.canGoBack()) {
            //if the web view can go back, perform back navigation and consume the event.
            webView.goBack();
            return true;
        }
        return super.onBackPressed();
    }

}
