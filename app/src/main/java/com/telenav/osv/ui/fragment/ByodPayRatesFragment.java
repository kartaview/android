package com.telenav.osv.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.item.network.PayRateData;
import com.telenav.osv.item.network.PayRateItem;

/**
 * Dialog fragment used to display the pay rates a byod driver might get, on a particular processed track.
 * <p>
 * Created by catalinj on 10/20/17.
 */
public class ByodPayRatesFragment extends DialogFragment {

    public static final String TAG = ByodPayRatesFragment.class.getSimpleName();

    private PayRateData payRateData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_byod_pay_rates, container, false);

        RecyclerView recyclerView = v.findViewById(R.id.pay_rates_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        recyclerView.setAdapter(new PayRateItemsAdapter(getContext(), payRateData));
        v.findViewById(R.id.button_byod_pay_rate_info_dialog_close_button).setOnClickListener(view -> dismiss());
        return v;
    }

    public void setPayRateData(PayRateData payRateData) {
        this.payRateData = payRateData;
    }

    /**
     * View holder for one pay rate data item displayed within the recycler view.
     */
    private static class PayRateItemViewHolder extends RecyclerView.ViewHolder {

        public final TextView previousPassesTextView;

        public final TextView obdPayRateTextView;

        public final TextView nonObdPayRateTextView;

        public PayRateItemViewHolder(View itemView) {
            super(itemView);
            previousPassesTextView = itemView.findViewById(R.id.text_pay_rate_previous_passes);
            obdPayRateTextView = itemView.findViewById(R.id.text_pay_rate_obd_rate);
            nonObdPayRateTextView = itemView.findViewById(R.id.text_pay_rate_non_obd_rate);
        }
    }

    /**
     * Adapter which creates the views that are displayed in the pay rates list, by the recycler view.
     */
    private static class PayRateItemsAdapter extends RecyclerView.Adapter<PayRateItemViewHolder> {

        private final PayRateData payRateData;

        private final LayoutInflater layoutInflater;

        private final Context context;

        /**
         * Creates a new adapter for pay rates items.
         * @param context the context in which the recycler view is used. Internally used to fetch resources and inflate views.
         * @param payRateData the pay rate data holder, i.e. the data carrier object.
         */
        public PayRateItemsAdapter(Context context, PayRateData payRateData) {
            this.payRateData = payRateData;
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public PayRateItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = layoutInflater.inflate(R.layout.item_pay_rate, parent, false);
            return new PayRateItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(PayRateItemViewHolder holder, int position) {
            PayRateItem payRateItem = payRateData.getPayRates().get(position);
            bindContent(holder, payRateItem.payRateCoverageInterval.maxPass == 0 ? context.getString(R.string.byod_pay_rate_new_road) :
                            Integer.toString(payRateItem.payRateCoverageInterval.maxPass),
                    payRateData.getCurrency() + Float.toString(payRateItem.obdPayRateValue),
                    payRateData.getCurrency() + Float.toString(payRateItem.nonObdPayRateValue));
        }

        @Override
        public int getItemCount() {
            if (payRateData == null || payRateData.getPayRates() == null) {
                return 0;
            }
            return payRateData.getPayRates().size();
        }

        private void bindContent(PayRateItemViewHolder holder, String payRateCoverageInterval, String obdPayRateValue, String nonObdPayRateValue) {
            holder.previousPassesTextView.setText(payRateCoverageInterval);
            holder.obdPayRateTextView.setText(obdPayRateValue);
            holder.nonObdPayRateTextView.setText(nonObdPayRateValue);
        }
    }

}
