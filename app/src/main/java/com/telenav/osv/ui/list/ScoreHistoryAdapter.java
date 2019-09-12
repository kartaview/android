package com.telenav.osv.ui.list;

import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.data.score.model.ScoreHistory;
import com.telenav.osv.utils.Utils;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The adapter for the breakdown view in the track preview, used to display the score details.
 * Created by Kalman on 30/12/2016.
 */
public class ScoreHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;

    private static final int TYPE_ITEM = 1;

    private ArrayList<ScoreHistory> scoreHistoryList;

    private Context context;

    public ScoreHistoryAdapter(ArrayList<ScoreHistory> results, Context context) {
        this.scoreHistoryList = results;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_score_history_header, null);
            ((TextView) layoutView.findViewById(R.id.text_score_breakdown_header_column1)).setText(R.string.points_history_pictures);
            ((TextView) layoutView.findViewById(R.id.text_score_breakdown_header_column2)).setText(R.string.points_history_multiplier);
            ((TextView) layoutView.findViewById(R.id.text_score_breakdown_header_column3)).setText(R.string.points_history_points);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            return new HeaderHolder(layoutView);
        } else {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_history, null);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            return new PointsHolder(layoutView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {
            if (holder instanceof PointsHolder) {
                PointsHolder pointsHolder = (PointsHolder) holder;
                final ScoreHistory score = scoreHistoryList.get(Math.min(position - 1, scoreHistoryList.size() - 1));
                int multiplier = Utils.getValueOnSegment(score.getCoverage()) * score.getObdStatus();
                int photos = score.getPhotoCount() + score.getObdPhotoCount();
                pointsHolder.multiplierText.setText(String.valueOf(multiplier));
                pointsHolder.distanceText.setText(String.valueOf(photos));
                pointsHolder.pointsText.setText(String.valueOf(multiplier * photos));

                int clr = context.getResources().getColor(R.color.default_white);
                pointsHolder.multiplierText.setTextColor(clr);
                pointsHolder.distanceText.setTextColor(clr);
                pointsHolder.pointsText.setTextColor(clr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return scoreHistoryList.size() + 1;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    private static class PointsHolder extends RecyclerView.ViewHolder {

        private final TextView distanceText;

        private final TextView multiplierText;

        private final TextView pointsText;

        PointsHolder(View v) {
            super(v);
            distanceText = v.findViewById(R.id.text_track_breakdown_column1);
            multiplierText = v.findViewById(R.id.text_track_breakdown_column2);
            pointsText = v.findViewById(R.id.text_track_breakdown_column3);
        }
    }

    private static class HeaderHolder extends RecyclerView.ViewHolder {

        HeaderHolder(View v) {
            super(v);
        }
    }
}
