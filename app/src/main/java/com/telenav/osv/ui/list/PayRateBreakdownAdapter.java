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
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.item.DriverPayRateBreakdownCoverageItem;
import com.telenav.osv.utils.Utils;

/**
 * Adapter for the Pay Rate breakdown list on track preview, for byod 2.0
 */
public class PayRateBreakdownAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;

    private static final int TYPE_ITEM = 1;

    private static final String DISTANCE_UNIT = " km";

    private final String currency;

    private List<DriverPayRateBreakdownCoverageItem> mPayRateItems;

    private OSVActivity activity;

    public PayRateBreakdownAdapter(List<DriverPayRateBreakdownCoverageItem> results, String currency, OSVActivity activity) {
        this.mPayRateItems = results;
        this.currency = currency;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_score_history_header, null);
            ((TextView) layoutView.findViewById(R.id.text_score_breakdown_header_column1)).setText(R.string.payment_breakdown_passes);
            ((TextView) layoutView.findViewById(R.id.text_score_breakdown_header_column2)).setText(R.string.payment_breakdown_distance);
            ((TextView) layoutView.findViewById(R.id.text_score_breakdown_header_column3)).setText(R.string.payment_breakdown_value);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(params);
            HeaderHolder pointsHolder = new HeaderHolder(layoutView);
            return pointsHolder;
        } else {
            FrameLayout layoutView = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_history, null);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
                final DriverPayRateBreakdownCoverageItem payRateBreakdownCoverageItem = mPayRateItems.get(Math.min(position - 1, mPayRateItems.size() - 1));
                if (payRateBreakdownCoverageItem.passes == 0) {
                    pointsHolder.passesText.setText(R.string.byod_pay_rate_new_road);
                } else {
                    pointsHolder.passesText.setText(Integer.toString(payRateBreakdownCoverageItem.passes));
                }

                pointsHolder.distanceText.setText(payRateBreakdownCoverageItem.distance + DISTANCE_UNIT);
                pointsHolder.moneyReceivedText.setText(this.currency + Utils.formatMoneyConstrained(payRateBreakdownCoverageItem.receivedMoney));

                int clr = activity.getResources().getColor(R.color.white);
                pointsHolder.passesText.setTextColor(clr);
                pointsHolder.distanceText.setTextColor(clr);
                pointsHolder.moneyReceivedText.setTextColor(clr);
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
        return mPayRateItems.size() + 1;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    private static class PointsHolder extends RecyclerView.ViewHolder {

        private final TextView passesText;

        private final TextView distanceText;

        private final TextView moneyReceivedText;

        PointsHolder(View v) {
            super(v);
            passesText = v.findViewById(R.id.text_track_breakdown_column1);
            distanceText = v.findViewById(R.id.text_track_breakdown_column2);
            moneyReceivedText = v.findViewById(R.id.text_track_breakdown_column3);
        }
    }

    private static class HeaderHolder extends RecyclerView.ViewHolder {

        HeaderHolder(View v) {
            super(v);
        }
    }
}
