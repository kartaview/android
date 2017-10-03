package com.telenav.osv.ui.fragment;

import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.command.SendReportCommand;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.item.network.IssueData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.IssueReporter;
import com.telenav.osv.utils.Log;

/**
 * Fragment holding the issue reporting ui
 * Created by Kalman on 02/05/2017.
 */
public class IssueReportFragment extends OSVFragment {

    public static final String TAG = "IssueReportFragment";

    @Inject
    Preferences prefs;

    @Inject
    IssueReporter mIssueReporter;

    private MainActivity activity;

    private ScrollView view;

    private AppCompatEditText mTextEdit;

    private TextView mExternalHint;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        view = (ScrollView) inflater.inflate(R.layout.fragment_issue_report, null);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.register(this);
        if (activity != null && mTextEdit != null) {
            mTextEdit.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mTextEdit, 0);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        init();
    }

    @Override
    public void onPause() {
        closeKeyboard();
        EventBus.unregister(this);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mIssueReporter.destroy();
        mIssueReporter = null;
        super.onDestroyView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void sendIssue(SendReportCommand command) {
        if (mTextEdit != null && mTextEdit.getText().toString().length() > 0 && mTextEdit.isEnabled()) {
            activity.enableProgressBar(true);
            mTextEdit.setEnabled(false);
            closeKeyboard();
            mIssueReporter.createIssue(new NetworkResponseDataListener<IssueData>() {

                @Override
                public void requestFailed(int status, IssueData details) {
                    activity.runOnUiThread(() -> {
                        activity.enableProgressBar(false);
                        showDialog(getString(R.string.issue_report_error_title), getString(R.string.issue_report_error_message), null);
                        mTextEdit.setEnabled(true);
                    });
                }

                @Override
                public void requestFinished(int status, IssueData details) {
                    activity.runOnUiThread(() -> {
                        activity.enableProgressBar(false);
                        showDialog(getString(R.string.issue_report_success_title), getString(R.string.issue_report_success_message),
                                () -> activity.onBackPressed());
                        mTextEdit.setEnabled(true);
                        mTextEdit.setText("");
                    });
                }
            }, mTextEdit.getText().toString());
        } else {
            activity.showSnackBar(R.string.issue_reporting_hint, Snackbar.LENGTH_LONG);
        }
    }

    private void init() {
        try {
            mExternalHint = view.findViewById(R.id.issue_report_hint_external);
            mTextEdit = view.findViewById(R.id.report_edit_text);
            mTextEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    EventBus.post(new SendReportCommand());
                    return true;
                }
                return false;
            });
            mTextEdit.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (mTextEdit.getText().length() == 0) {
                        mExternalHint.setVisibility(View.INVISIBLE);
                    } else {
                        mExternalHint.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "onViewCreated: " + Log.getStackTraceString(e));
        }
    }

    private void showDialog(String title, String message, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogIssueReport);
        builder.setMessage(message).setTitle(title).setPositiveButton(R.string.continue_caps_label, (dialog, which) -> {
            if (action != null) {
                action.run();
            }
        }).create().show();
    }

    private void closeKeyboard() {
        if (activity != null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
