package com.telenav.osv.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.dialog.KVDialog;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Activity with login choices
 * Created by Kalman on 01/03/2017.
 */
public class LoginActivity extends OSVActivity {

    private final static String TAG = "LoginActivity";

    private ProgressBar progressBar;

    private LoginManager mLoginManager;

    private KVDialog partnerLoginDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPrefs = getApp().getAppPrefs();
        setContentView(R.layout.activity_login);
        progressBar = findViewById(R.id.progressbar);
        AppCompatButton facebookButton = findViewById(R.id.facebook_login_button);
        AppCompatButton googleButton = findViewById(R.id.google_login_button);

        Drawable face = ResourcesCompat.getDrawable(getResources(), R.drawable.vector_facebook, null);
        Drawable goo = ResourcesCompat.getDrawable(getResources(), R.drawable.vector_google, null);
        facebookButton.setCompoundDrawablesWithIntrinsicBounds(face, null, null, null);
        googleButton.setCompoundDrawablesWithIntrinsicBounds(goo, null, null, null);

        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.default_purple), PorterDuff.Mode.SRC_IN);
        Toolbar toolbar = findViewById(R.id.app_toolbar);
        View.OnClickListener mMenuListener = v -> onBackPressed();
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
    public KVApplication getApp() {
        return (KVApplication) getApplication();
    }

    @Override
    public int getCurrentScreen() {
        return 0;
    }

    @Override
    public void resolveLocationProblem() {

    }

    @Override
    public void resolveRecordingProblem() {

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
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
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
            mLoginManager.onActivityResult(requestCode, resultCode, data, this);
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
            switch (view.getId()) {
                case R.id.facebook_login_button:
                    enableProgressBar(true);
                    mLoginManager.login(this, LoginManager.LOGIN_TYPE_FACEBOOK);
                    break;
                case R.id.google_login_button:
                    enableProgressBar(true);
                    mLoginManager.login(this, LoginManager.LOGIN_TYPE_GOOGLE);
                    break;
                case R.id.osm_login_button:
                    enableProgressBar(true);
                    mLoginManager.login(this, LoginManager.LOGIN_TYPE_OSM);
                    break;
                case R.id.tv_partner_login:
                    showPartnerLoginDialog(this);
                    break;
            }
        }
    }

    private void showPartnerLoginDialog(final OSVActivity activity) {
        if (partnerLoginDialog == null) {
            partnerLoginDialog = new KVDialog.Builder(activity)
                    .setTitleResId(R.string.partner_login)
                    .setInfoResId(R.string.partner_login_dialog_message)
                    .setPositiveButton(R.string.continue_caps_label, v -> {
                        enableProgressBar(true);
                        mLoginManager.login(activity, LoginManager.LOGIN_TYPE_PARTNER);
                        partnerLoginDialog.dismiss();
                    })
                    .setNegativeButton(R.string.go_back, v -> partnerLoginDialog.dismiss())
                    .setNegativeButtonTextColor(R.color.default_grab_grey)
                    .setIconLayoutVisibility(false)
                    .build();
        }
        partnerLoginDialog.show();
    }
}
