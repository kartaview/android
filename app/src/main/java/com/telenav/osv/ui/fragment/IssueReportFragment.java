package com.telenav.osv.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.common.model.base.OSCBaseFragment;
import com.telenav.osv.common.model.data.SnackBarItem;
import com.telenav.osv.common.toolbar.MenuAction;
import com.telenav.osv.common.toolbar.OSCToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.common.ui.loader.LoadingScreen;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.item.network.IssueData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.IssueReporter;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.UiUtils;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * Fragment holding the issue reporting ui
 * Created by Kalman on 02/05/2017.
 */
public class IssueReportFragment extends OSCBaseFragment {

    public final static String TAG = IssueReportFragment.class.getSimpleName();

    private View view;

    private AppCompatEditText mTextEdit;

    private TextView mExternalHint;

    private IssueReporter mIssueReporter;

    private ProgressBar progressBar;

    /**
     * Method used to create an instance of {@link IssueReportFragment}.
     * @return a new instance of the {@code IssueReportFragment}.
     */
    public static IssueReportFragment newInstance() {
        return new IssueReportFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_issue_report, container, false);
        progressBar = view.findViewById(R.id.progress_bar);
        mIssueReporter = new IssueReporter(getContext());
        setupStatusBarColor(R.color.default_blue);
        return view;
    }

    @Override
    public ToolbarSettings getToolbarSettings(OSCToolbar toolbar) {
        //noinspection ConstantConditions since the method is called if the getActivity is not null in the base class
        return new ToolbarSettings.Builder()
                .setTitle(R.string.settings_report_a_problem_title)
                .setTextColor(getResources().getColor(R.color.default_white))
                .setNavigationIcon(R.drawable.vector_back_white, (v) -> getActivity().onBackPressed())
                .setBackgroundColor(getResources().getColor(R.color.default_blue))
                .setMenuResources(R.menu.menu_report_issue, setUpToolbarMenuActions())
                .build();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @Nullable
    @Override
    public LoadingScreen setupLoadingScreen() {
        //return null since a loading screen is not needed
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.register(this);
        if (getActivity() != null && mTextEdit != null) {
            mTextEdit.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mTextEdit, 0);
            }
        }
    }

    @Override
    public void onPause() {
        closeKeyboard();
        EventBus.unregister(this);
        super.onPause();
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    public void sendIssue() {
        if (mTextEdit != null && mTextEdit.getText().toString().length() > 0 && mTextEdit.isEnabled()) {
            progressBar.setVisibility(View.VISIBLE);
            mTextEdit.setEnabled(false);
            closeKeyboard();
            mIssueReporter.createIssue(new NetworkResponseDataListener<IssueData>() {

                @Override
                public void requestFailed(int status, IssueData details) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            showDialog(getString(R.string.settings_report_issue_failed_title),
                                    getString(R.string.settings_report_issue_failed_message), null);
                            mTextEdit.setEnabled(true);
                        });
                    }
                }

                @Override
                public void requestFinished(int status, IssueData details) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            showDialog(getString(R.string.settings_report_issue_success_title),
                                    getString(R.string.settings_report_issue_success_message),
                                    activity::onBackPressed);
                            mTextEdit.setEnabled(true);
                            mTextEdit.setText(StringUtils.EMPTY_STRING);
                        });
                    }
                }
            }, mTextEdit.getText().toString());
        } else {
            UiUtils.showSnackBar(getContext(), getView(), new SnackBarItem(getString(R.string.issue_reporting_hint), Snackbar.LENGTH_SHORT, null, null));
        }
    }

    private SparseArray<MenuAction> setUpToolbarMenuActions() {
        SparseArray<MenuAction> menuItems = new SparseArray<>();
        menuItems.append(R.id.send_button, this::sendIssue);
        return menuItems;
    }

    private void init() {
        try {
            mExternalHint = view.findViewById(R.id.issue_report_hint_external);
            mTextEdit = view.findViewById(R.id.report_edit_text);
            mTextEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendIssue();
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
        Context context = getContext();
        if (context == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogIssueReport);
        builder.setMessage(message).setTitle(title).setPositiveButton(R.string.continue_caps_label, (dialog, which) -> {
            if (action != null) {
                action.run();
            }
        }).create().show();
    }

    private void closeKeyboard() {
        Activity activity = getActivity();
        if (activity != null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
    }
}
