package com.telenav.osv.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.manager.ShutterManager;


public class ShutterButton extends ImageView implements View.OnClickListener {

    private final Context mContext;

    private ShutterManager mShutterManager;

    private boolean isSet;

    public ShutterButton(Context context) {
        super(context);
        mContext = context;
        setOnClickListener(this);
    }

    public ShutterButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setOnClickListener(this);
    }

    public ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnClickListener(this);
    }

    public void setShutterManager(ShutterManager sm) {
        this.mShutterManager = sm;
        if (mShutterManager.isRecording()) {
            started();
            isSet = true;
        }
    }

    public void started() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ShutterButton.this.setImageDrawable(mContext.getDrawable(R.drawable.stop_recording));
        } else {
            ShutterButton.this.setImageDrawable(mContext.getResources().getDrawable(R.drawable.stop_recording));
        }
    }

    public void stopped() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ShutterButton.this.setImageDrawable(mContext.getDrawable(R.drawable.button_record_inactive));
        } else {
            ShutterButton.this.setImageDrawable(mContext.getResources().getDrawable(R.drawable.button_record_inactive));
        }
    }


    @Override
    public void onClick(View v) {
        boolean ok = true;
        if (mContext instanceof OSVActivity){
            ok = ((MainActivity) mContext).checkPermissionsForRecording();
        }
        if (!ok) {
            return;
        }
        if (mShutterManager == null) return;

        if (!mShutterManager.isRecording()) {
            if (!((OSVApplication) mContext.getApplicationContext()).getLocationManager().isGPSEnabled()) {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).resolveLocationProblem(true);
                }
                return;
            }
            if (!((OSVApplication) mContext.getApplicationContext()).getLocationManager().hasPosition()) {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).showSnackBar(R.string.no_gps_message, Snackbar.LENGTH_SHORT);
                }
                return;
            }
            started();
            mShutterManager.startSequence();
        } else {
            mShutterManager.stopSequence();
            stopped();
        }
    }
}
