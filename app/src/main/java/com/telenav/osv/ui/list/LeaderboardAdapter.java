package com.telenav.osv.ui.list;

import java.util.List;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.item.LeaderboardData;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import androidx.recyclerview.widget.RecyclerView;

/**
 * *
 * Created by Kalman on 22/11/2016.
 */

public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;

    private static final int TYPE_ITEM = 1;

    private static final int TYPE_ITEM_NO_INTERNET = 2;

    private static final String TAG = "LeaderboardAdapter";

    private List<LeaderboardData> mUserList;

    private Context context;

    private boolean mInternetAvailable;

    private int mUserPosition;

    private int lastPosition = 0;

    public LeaderboardAdapter(List<LeaderboardData> results, Context context) {
        mUserList = results;
        this.context = context;
        mInternetAvailable = Utils.isInternetAvailable(this.context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_leaderboard_header, parent, false);
            return new LeaderboardAdapter.HeaderViewHolder(v);
        } else if (viewType == TYPE_ITEM) {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, null);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            return new UserHolder(layoutView);
        } else if (viewType == TYPE_ITEM_NO_INTERNET) {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_no_internet, null);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            Log.d(TAG, "onCreateViewHolder: No internet card created.");
            return new MessageCardHolder(layoutView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {
            if (holder instanceof LeaderboardAdapter.UserHolder) {
                LeaderboardAdapter.UserHolder userHolder = (LeaderboardAdapter.UserHolder) holder;
                final LeaderboardData leaderboardData = mUserList.get(Math.min(position - 1, mUserList.size() - 1));
                userHolder.nameText.setText(leaderboardData.getName());
                userHolder.rankText.setText(String.format("%d", leaderboardData.getRank()));
                userHolder.pointsText.setText(FormatUtils.formatNumber(leaderboardData.getPoints()));
                if (mUserPosition == position) {
                    userHolder.itemView.setBackgroundColor(context.getResources().getColor(R.color.default_green));
                    userHolder.nameText.setTextColor(Color.WHITE);
                    userHolder.rankText.setTextColor(Color.WHITE);
                    userHolder.pointsText.setTextColor(Color.WHITE);
                    return;
                } else {
                    int clr = context.getResources().getColor(R.color.default_black_lighter);
                    userHolder.nameText.setTextColor(clr);
                    userHolder.rankText.setTextColor(clr);
                    userHolder.pointsText.setTextColor(clr);
                }
            }
            holder.itemView.setBackgroundColor(context.getResources().getColor(position % 2 == 0 ? R.color.settings_default_color : R.color.default_white));
        } catch (Exception e) {
            e.printStackTrace();
        }

        setAnimation(holder.itemView, position);
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

    @Override
    public int getItemCount() {
        if (mInternetAvailable) {
            return mUserList.size() + 1;
        } else {
            return 2;
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        ((ViewHolder) holder).clearAnimation();
    }

    public void setOnline(boolean online) {
        mInternetAvailable = online;
    }

    public void setUserPosition(int userPosition) {
        this.mUserPosition = userPosition;
        this.notifyItemChanged(mUserPosition);
    }

    public void resetLastAnimatedItem() {
        lastPosition = 0;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    /**
     * method for the item animation
     */
    private void setAnimation(View viewToAnimate, int position) {
        if (position != 0) {
            int last = lastPosition;
            lastPosition = position;
            if (Math.abs(last - position) > 1) {
                return;
            }
            Animation animation = AnimationUtils.loadAnimation(context, (position >= last) ? R.anim.item_slide_up : R.anim.item_slide_down);
            viewToAnimate.startAnimation(animation);
        }
    }

    private class UserHolder extends ViewHolder {

        private final TextView rankText;

        private final TextView nameText;

        private final TextView pointsText;

        UserHolder(View v) {
            super(v);
            rankText = v.findViewById(R.id.rank_text);
            nameText = v.findViewById(R.id.name_text);
            pointsText = v.findViewById(R.id.points_text);
        }
    }

    private class MessageCardHolder extends ViewHolder {

        View container;

        MessageCardHolder(View v) {
            super(v);
            container = v;
        }
    }

    private class HeaderViewHolder extends ViewHolder {

        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    private abstract class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }

        void clearAnimation() {
            itemView.clearAnimation();
        }
    }
}
