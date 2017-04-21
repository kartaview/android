package com.telenav.osv.ui.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 17/01/2017.
 */
public class RecordingSummaryFragment extends Fragment {
    
    public final static String TAG = "RecordingSummaryFragment";
    
    private Sequence mSequence;

    private MainActivity activity;

    private FrameLayout view;

    private LayoutInflater mInflater;
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        view = (FrameLayout) inflater.inflate(R.layout.fragment_recording_summary, null);
        mInflater = inflater;
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(activity.isPortrait());
    }

    private void init(boolean portrait) {
        View view = mInflater.inflate(portrait ? R.layout.partial_recording_summary : R.layout.partial_recording_summary_landscape, null);
        this.view.addView(view);
        this.view.requestLayout();
        try {
            View okButton = view.findViewById(R.id.ok_button);
            TextView sizeText = (TextView) view.findViewById(R.id.summary_size_text);
            TextView distanceText = (TextView) view.findViewById(R.id.summary_distance_text);
            TextView imagesText = (TextView) view.findViewById(R.id.summary_images_text);
            TextView summaryText = (TextView) view.findViewById(R.id.summary_points_text);

            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onBackPressed();
                }
            });
            if (mSequence != null) {
                String first = "Disk size ";
                String[] items = Utils.formatSizeDetailed(mSequence.size);
                String second = items[0];
                String third = items[1];
                third.replace("MB","mb");
                third.replace("GB","gb");
                SpannableString styledString = new SpannableString(first + second + third);
                styledString.setSpan(new AbsoluteSizeSpan(20, true), 0, first.length(), 0);
                styledString.setSpan(new AbsoluteSizeSpan(24, true), first.length(), second.length() + first.length(), 0);
//                styledString.setSpan(new CenteredSpan(), 0, first.length(), 0);
//                styledString.setSpan(new CenteredSpan(), first.length() + second.length(), second.length() + first.length() + third.length(), 0);
                styledString.setSpan(new AbsoluteSizeSpan(18, true), first.length() + second.length(), first.length() + second.length() + third.length(), 0);
                styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_secondary_text)), 0, first.length(), 0);
                styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_primary_text)), first.length(), first.length() + second.length(), 0);

                styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_secondary_text)), first.length() + second.length(), first
                        .length()
                        + second.length() + third.length(), 0);
                sizeText.setText(styledString);

                first = "Distance ";
                items = Utils.formatDistanceFromMeters(activity, mSequence.getDistance());
                second = items[0];
                third = items[1];
                SpannableString styledString2 = new SpannableString(first + second + third);
                styledString2.setSpan(new AbsoluteSizeSpan(20, true), 0, first.length(), 0);
                styledString2.setSpan(new AbsoluteSizeSpan(24, true), first.length(), second.length() + first.length(), 0);
//                styledString2.setSpan(new CenteredSpan(), 0, first.length(), 0);
//                styledString2.setSpan(new CenteredSpan(), first.length() + second.length(), second.length() + first.length() + third.length(), 0);
                styledString2.setSpan(new AbsoluteSizeSpan(18, true), first.length() + second.length(), first.length() + second.length() + third.length(), 0);
                styledString2.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_secondary_text)), 0, first.length(), 0);
                styledString2.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_primary_text)), first.length(), first.length() + second.length(), 0);
                styledString2.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_secondary_text)), first.length() + second.length(), first.length()
                        + second.length() + third.length(), 0);
                distanceText.setText(styledString2);

                first = "Photos ";
                second = "" + mSequence.originalImageCount;
                SpannableString styledString3 = new SpannableString(first + second);
                styledString3.setSpan(new AbsoluteSizeSpan(20, true), 0, first.length(), 0);
                styledString3.setSpan(new AbsoluteSizeSpan(24, true), first.length(), second.length() + first.length(), 0);
//                styledString3.setSpan(new CenteredSpan(), 0, first.length(), 0);
//                styledString3.setSpan(new CenteredSpan(), first.length() + second.length(), second.length() + first.length(), 0);
                styledString3.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_secondary_text)), 0, first.length(), 0);
                styledString3.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.gray_summary_primary_text)), first.length(), first.length() + second.length(), 0);
                imagesText.setText(styledString3);

                summaryText.setText("" + mSequence.score);
            }
        } catch (Exception e){
            Log.d(TAG, "onViewCreated: " + Log.getStackTraceString(e));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int orientation = newConfig.orientation;
        final boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        init(portrait);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void setSource(Sequence sequence) {
        this.mSequence = sequence;
    }
}
