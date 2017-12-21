package com.telenav.osv.activity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Activity with login choices
 * Created by Kalman on 01/03/2017.
 */
public class LoginActivity extends OSVActivity {

    private final static String TAG = "LoginActivity";

    private ProgressBar progressBar;

    private LoginManager mLoginManager;

    private ImageView mLogo;

    private LinearLayout mButtonsHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPrefs = getApp().getAppPrefs();
        setContentView(R.layout.activity_login);
        progressBar = findViewById(R.id.progressbar);
        //noinspection deprecation
        mLogo = findViewById(R.id.osc_logo_view);
        mButtonsHolder = findViewById(R.id.login_buttons_holder);
        AppCompatButton facebookButton = findViewById(R.id.facebook_login_button);
        AppCompatButton googleButton = findViewById(R.id.google_login_button);

        Drawable face = ResourcesCompat.getDrawable(getResources(), R.drawable.vector_facebook, null);
        Drawable goo = ResourcesCompat.getDrawable(getResources(), R.drawable.vector_google, null);
        facebookButton.setCompoundDrawablesWithIntrinsicBounds(face, null, null, null);
        googleButton.setCompoundDrawablesWithIntrinsicBounds(goo, null, null, null);

        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
        Toolbar toolbar = findViewById(R.id.app_toolbar);
        View.OnClickListener mMenuListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        };
        toolbar.setNavigationOnClickListener(mMenuListener);
        setSupportActionBar(toolbar);
        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.vector_back_black, null);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(upArrow);
        }
        toolbar.setNavigationOnClickListener(mMenuListener);
        mLoginManager = getApp().getLoginManager();
        setupBound(isPortrait());
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupBound(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public OSVApplication getApp() {
        return (OSVApplication) getApplication();
    }

    @Override
    public int getCurrentScreen() {
        return 0;
    }

    @Override
    public void resolveLocationProblem(boolean b) {

    }

    @Override
    public void hideSnackBar() {

    }

    @Override
    public void showSnackBar(int tip_map_screen, int lengthLong, int got_it_label, Runnable runnable) {

    }

    @Override
    public void showSnackBar(CharSequence text, int duration, CharSequence button, Runnable onClick) {

    }

    @Override
    public void enableProgressBar(final boolean show) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    public boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public void openScreen(int screenRecording) {

    }

    @Override
    public void openScreen(int screenNearby, Object extra) {

    }

    @Override
    public boolean hasPosition() {
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mLoginManager != null) {
            mLoginManager.onActivityResult(requestCode, resultCode, data);
        }
        if (resultCode != RESULT_OK) {
            enableProgressBar(false);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onPause() {
        EventBus.unregister(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginChanged(LoginChangedEvent event) {
        Log.d(TAG, "onLoginChanged: logged=" + event.logged);
        enableProgressBar(false);
        if (event.logged) {
            finish();
        }
    }

    public void onClick(View view) {
        if (mLoginManager != null) {
            if (!Utils.isInternetAvailable(this)) {
                showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
                return;
            }
            enableProgressBar(true);
            switch (view.getId()) {
                case R.id.facebook_login_button:
                    mLoginManager.login(this, LoginManager.LOGIN_TYPE_FACEBOOK);
                    break;
                case R.id.google_login_button:
                    mLoginManager.login(this, LoginManager.LOGIN_TYPE_GOOGLE);
                    break;
                case R.id.osm_login_button:
                    mLoginManager.login(this, LoginManager.LOGIN_TYPE_OSM);
                    break;
            }
        }
    }

    private void setupBound(boolean portrait) {
        if (portrait) {
            ((FrameLayout.MarginLayoutParams) mLogo.getLayoutParams()).topMargin = (int) Utils.dpToPx(this, 92);
            mLogo.requestLayout();
            ((FrameLayout.MarginLayoutParams) mButtonsHolder.getLayoutParams()).width = FrameLayout.MarginLayoutParams.MATCH_PARENT;
            int thirty = (int) Utils.dpToPx(this, 30);
            mButtonsHolder.setPadding(thirty, thirty, thirty, thirty);
            mButtonsHolder.requestLayout();
        } else {
            ((FrameLayout.MarginLayoutParams) mLogo.getLayoutParams()).topMargin = (int) Utils.dpToPx(this, 48);
            mLogo.requestLayout();
            ((FrameLayout.LayoutParams) mButtonsHolder.getLayoutParams()).width = (int) Utils.dpToPx(this, 400);
            int twenty = (int) Utils.dpToPx(this, 20);
            mButtonsHolder.setPadding(twenty, twenty, twenty, twenty / 2);
            mButtonsHolder.requestLayout();
        }
    }
}
