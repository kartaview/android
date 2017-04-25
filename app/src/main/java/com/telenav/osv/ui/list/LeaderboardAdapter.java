package com.telenav.osv.ui.list;

import java.util.List;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.UserData;
import com.telenav.osv.utils.Utils;

/**
 * *
 * Created by Kalman on 22/11/2016.
 */

public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;

    private static final int TYPE_ITEM = 1;

    private static final int TYPE_ITEM_NO_INTERNET = 2;

    private List<UserData> mUserList;

    private MainActivity activity;

    private boolean mInternetAvailable;

    private int mUserPosition;

    public LeaderboardAdapter(List<UserData> results, MainActivity activity) {
        mUserList = results;
        this.activity = activity;
        mInternetAvailable = Utils.isInternetAvailable(this.activity);
    }

    public void setOnline(boolean online) {
        mInternetAvailable = online;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_leaderboard_header, parent, false);
            return new LeaderboardAdapter.HeaderViewHolder(v);
        } else if (viewType == TYPE_ITEM) {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            LeaderboardAdapter.UserHolder userHolder = new LeaderboardAdapter.UserHolder(layoutView);
            return userHolder;

        } else if (viewType == TYPE_ITEM_NO_INTERNET) {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_no_internet, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            LeaderboardAdapter.MessageCardHolder messageCardHolder = new LeaderboardAdapter.MessageCardHolder(layoutView);
            return messageCardHolder;
        }
        return null;

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {
            if (holder instanceof LeaderboardAdapter.HeaderViewHolder) {
                LeaderboardAdapter.HeaderViewHolder headerHolder = (LeaderboardAdapter.HeaderViewHolder) holder;
            } else if (holder instanceof LeaderboardAdapter.UserHolder) {
                LeaderboardAdapter.UserHolder userHolder = (LeaderboardAdapter.UserHolder) holder;
                final UserData userData = mUserList.get(Math.min(position - 1, mUserList.size() - 1));
                userHolder.nameText.setText(userData.getName());
                userHolder.rankText.setText("" + userData.getRank());
                userHolder.pointsText.setText(Utils.formatNumber(userData.getPoints()));
                if (mUserPosition == position){
                    userHolder.itemView.setBackgroundColor(activity.getResources().getColor(R.color.leaderboard_green));
                    userHolder.nameText.setTextColor(Color.WHITE);
                    userHolder.rankText.setTextColor(Color.WHITE);
                    userHolder.pointsText.setTextColor(Color.WHITE);
                    return;
                } else {
                    int clr = activity.getResources().getColor(R.color.leaderboard_text_grey);
                    userHolder.nameText.setTextColor(clr);
                    userHolder.rankText.setTextColor(clr);
                    userHolder.pointsText.setTextColor(clr);
                }
            } else if (holder instanceof LeaderboardAdapter.MessageCardHolder) {
                LeaderboardAdapter.MessageCardHolder messageCardHolder = (LeaderboardAdapter.MessageCardHolder) holder;

            }
            holder.itemView.setBackgroundColor(activity.getResources().getColor(position % 2 == 0 ? R.color.leaderboard_grey : R.color.white));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    @Override
    public int getItemCount() {
        if (mInternetAvailable) {
            return mUserList.size() + 1;
        } else {
            return mUserList.size() + 2;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        if (mInternetAvailable) {
            return TYPE_ITEM;
        } else {
            return TYPE_ITEM_NO_INTERNET;
        }
    }

    public void setUserPosition(int userPosition) {
        this.mUserPosition = userPosition;
        this.notifyItemChanged(mUserPosition);
    }


    private static class UserHolder extends RecyclerView.ViewHolder {

        private final TextView rankText;

        private final TextView nameText;

        private final TextView pointsText;

        UserHolder(View v) {
            super(v);
            rankText = (TextView) v.findViewById(R.id.rank_text);
            nameText = (TextView) v.findViewById(R.id.name_text);
            pointsText = (TextView) v.findViewById(R.id.points_text);
        }
    }

    private static class MessageCardHolder extends RecyclerView.ViewHolder {
        View container;

        MessageCardHolder(View v) {
            super(v);
            container = v;
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }
}
