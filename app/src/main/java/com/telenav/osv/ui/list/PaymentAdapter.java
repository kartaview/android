package com.telenav.osv.ui.list;

import java.util.Currency;
import java.util.List;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.databinding.ItemPaymentBinding;
import com.telenav.osv.item.Payment;
import com.telenav.osv.ui.binding.viewmodel.DefaultBindingComponent;
import com.telenav.osv.ui.binding.viewmodel.profile.PaymentItemViewModel;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * the adapter used for displaying the uploaded tracks from the server
 * Created by kalmanbencze on 7/07/17.
 */
public class PaymentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;

    private static final int TYPE_ITEM = 1;

    private static final int TYPE_ITEM_NO_INTERNET = 2;

    private static final String TAG = "PaymentAdapter";

    private final ValueFormatter valueFormatter;

    private List<Payment> mPaymentList;

    private boolean mInternetAvailable;

    private int lastPosition = -1;

    private double mTotalPayments;

    private String mCurrency = "";

    private Context context;

    public PaymentAdapter(List<Payment> results, OSVActivity activity, ValueFormatter valueFormatter) {
        mPaymentList = results;
        mInternetAvailable = Utils.isInternetAvailable(activity);
        context = activity;
        this.valueFormatter = valueFormatter;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_payment_header, parent, false);
            return new HeaderViewHolder(layoutView);
        } else if (viewType == TYPE_ITEM) {
            ItemPaymentBinding binding = DataBindingUtil
                    .inflate(LayoutInflater.from(parent.getContext()), R.layout.item_payment, parent, false,
                            new DefaultBindingComponent());

            binding.setViewModel(new PaymentItemViewModel(valueFormatter));
            return new PaymentHolder(binding);
        } else if (viewType == TYPE_ITEM_NO_INTERNET) {
            CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_no_internet_card, null);
            return new MessageCardHolder(layoutView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {
            if (holder instanceof HeaderViewHolder) {
                String currency = mCurrency;
                try {
                    currency = Currency.getInstance(currency).getSymbol();
                } catch (IllegalArgumentException ignored) {
                    Log.d(TAG, Log.getStackTraceString(ignored));
                }
                ((HeaderViewHolder) holder).totalPaymentTextView.setText(currency + valueFormatter.formatMoney(mTotalPayments));
                ((HeaderViewHolder) holder).valueHeaderTextView.setText(context.getString(R.string.value_label) + " (" + mCurrency + ")");
            } else if (holder instanceof PaymentHolder) {
                PaymentHolder paymentHolder = (PaymentHolder) holder;
                Payment payment = mPaymentList.get(position - 1);
                paymentHolder.binding.getViewModel().setPayment(payment);
            }
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isPositionHeader(position) ? TYPE_HEADER : (mInternetAvailable ? TYPE_ITEM : TYPE_ITEM_NO_INTERNET);
    }

    @Override
    public int getItemCount() {
        if (mInternetAvailable) {
            return mPaymentList.size() + 1;
        } else {
            return mPaymentList.size() + 2;
        }
    }

    public void setOnline(boolean online) {
        mInternetAvailable = online;
    }

    public void setTotalPayment(double payment, String currency) {
        this.mTotalPayments = payment;
        this.mCurrency = currency;
        notifyItemChanged(0);
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    private class PaymentHolder extends ViewHolder {

        private final ItemPaymentBinding binding;

        PaymentHolder(ItemPaymentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private class MessageCardHolder extends ViewHolder {

        View container;

        MessageCardHolder(View v) {
            super(v);
            container = v;
        }
    }

    private abstract class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class HeaderViewHolder extends ViewHolder {

        public TextView totalPaymentTextView;

        public TextView valueHeaderTextView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            totalPaymentTextView = itemView.findViewById(R.id.payment_header_total_payment_text);
            valueHeaderTextView = itemView.findViewById(R.id.payment_value_text);
        }
    }
}