package com.telenav.osv.ui.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.item.ScoreItem;
import java.util.ArrayList;

/**
 * adapter for the score list on track preview
 * Created by Kalman on 30/12/2016.
 */
public class ScoreHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int TYPE_HEADER = 0;

  private static final int TYPE_ITEM = 1;

  private ArrayList<ScoreItem> mScoreHistory;

  private OSVActivity activity;

  public ScoreHistoryAdapter(ArrayList<ScoreItem> results, OSVActivity activity) {
    mScoreHistory = results;
    this.activity = activity;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_score_history_header, null);
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
        final ScoreItem score = mScoreHistory.get(Math.min(position - 1, mScoreHistory.size() - 1));
        pointsHolder.multiplierText.setText("" + score.value);
        pointsHolder.distanceText.setText("" + (score.photoCount));
        pointsHolder.pointsText.setText("" + (score.value * (score.photoCount)));

        int clr = activity.getResources().getColor(R.color.white);
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
    return mScoreHistory.size() + 1;
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
      distanceText = v.findViewById(R.id.distance);
      multiplierText = v.findViewById(R.id.multiplier);
      pointsText = v.findViewById(R.id.points);
    }
  }

  private static class HeaderHolder extends RecyclerView.ViewHolder {

    HeaderHolder(View v) {
      super(v);
    }
  }
}
