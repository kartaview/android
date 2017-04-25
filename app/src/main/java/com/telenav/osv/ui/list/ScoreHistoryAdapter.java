package com.telenav.osv.ui.list;

import java.util.List;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.item.ScoreHistory;
import com.telenav.osv.item.UserData;
import com.telenav.osv.utils.Utils;

/**
 *
 * Created by Kalman on 30/12/2016.
 */

public class ScoreHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;

    private static final int TYPE_ITEM = 1;

    private List<ScoreHistory> mScoreHistory;

    private OSVActivity activity;

    public ScoreHistoryAdapter(List<ScoreHistory> results, OSVActivity activity) {
        mScoreHistory = results;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER){
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.header_score_history, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            HeaderHolder pointsHolder = new HeaderHolder(layoutView);
            return pointsHolder;
        } else {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_history, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            PointsHolder pointsHolder = new PointsHolder(layoutView);
            return pointsHolder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {
            if (holder instanceof PointsHolder) {
                PointsHolder pointsHolder = (PointsHolder) holder;
                final ScoreHistory score = mScoreHistory.get(Math.min(position - 1, mScoreHistory.size() - 1));
                pointsHolder.multiplierText.setText("" + Utils.getValueOnSegment(score.coverage));
                pointsHolder.distanceText.setText("" + (score.photoCount + score.obdPhotoCount));
                pointsHolder.pointsText.setText("" + (Utils.getValueOnSegment(score.coverage) * (score.photoCount + score.obdPhotoCount)));

                int clr = activity.getResources().getColor(R.color.white);
                pointsHolder.multiplierText.setTextColor(clr);
                pointsHolder.distanceText.setTextColor(clr);
                pointsHolder.pointsText.setTextColor(clr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    @Override
    public int getItemCount() {
        return mScoreHistory.size() + 1;

    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }


    private static class PointsHolder extends RecyclerView.ViewHolder {

        private final TextView distanceText;

        private final TextView multiplierText;

        private final TextView pointsText;

        PointsHolder(View v) {
            super(v);
            distanceText = (TextView) v.findViewById(R.id.distance);
            multiplierText = (TextView) v.findViewById(R.id.multiplier);
            pointsText = (TextView) v.findViewById(R.id.points);
        }
    }

    private static class HeaderHolder extends RecyclerView.ViewHolder {

        HeaderHolder(View v) {
            super(v);
        }
    }
}
