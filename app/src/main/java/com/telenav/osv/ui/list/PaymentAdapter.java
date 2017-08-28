package com.telenav.osv.ui.list;

import android.content.res.Resources;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.Payment;
import com.telenav.osv.utils.Utils;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * the adapter used for displaying the uploaded tracks from the server
 * Created by kalmanbencze on 7/07/17.
 */
public class PaymentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int TYPE_HEADER = 0;

  private static final int TYPE_ITEM = 1;

  private static final int TYPE_ITEM_NO_INTERNET = 2;

  private static final String TAG = "PaymentAdapter";

  private List<Payment> mPaymentList;

  private MainActivity activity;

  private boolean mInternetAvailable;

  private int lastPosition = -1;

  private double mTotalPayments;

  private String mCurrency = "";

  public PaymentAdapter(List<Payment> results, MainActivity activity) {
    mPaymentList = results;
    this.activity = activity;
    mInternetAvailable = Utils.isInternetAvailable(this.activity);
  }

  public void setOnline(boolean online) {
    mInternetAvailable = online;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    Resources resources = activity.getResources();
    if (viewType == TYPE_HEADER) {
      View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_payment_header, parent, false);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      int cardMarginSide = (int) resources.getDimension(R.dimen.sequence_list_item_margin_side);
      params.setMargins(cardMarginSide, 0, cardMarginSide, 0);
      layoutView.setLayoutParams(params);
      return new HeaderViewHolder(layoutView);
    } else if (viewType == TYPE_ITEM) {
      LinearLayout layoutView = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, null);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      int cardMarginSide = (int) resources.getDimension(R.dimen.sequence_list_item_margin_side);

      params.setMargins(cardMarginSide, 0, cardMarginSide, 0);
      layoutView.setLayoutParams(params);
      return new PaymentHolder(layoutView);
    } else if (viewType == TYPE_ITEM_NO_INTERNET) {
      CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_no_internet_card, null);

      layoutView.setUseCompatPadding(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      int topMargin = (int) (15f * activity.getResources().getDisplayMetrics().density);
      int bottomMargin = (int) (10f * activity.getResources().getDisplayMetrics().density);
      int fiveDips = (int) (16f * activity.getResources().getDisplayMetrics().density);
      params.setMargins(fiveDips, topMargin, fiveDips, bottomMargin);
      layoutView.setLayoutParams(params);
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
          currency = Currency.getInstance(mCurrency).getSymbol();
        } catch (IllegalArgumentException ignored) {
        }
        ((HeaderViewHolder) holder).totalPaymentTextView.setText(currency + Utils.formatMoney(mTotalPayments));
        ((HeaderViewHolder) holder).valueHeaderTextView.setText("Value (" + mCurrency + ")");
      } else if (holder instanceof PaymentHolder) {
        PaymentHolder paymentHolder = (PaymentHolder) holder;

        Payment payment = mPaymentList.get(position - 1);
        paymentHolder.idText.setText(String.format(Locale.US, "%03d", payment.getId()));
        paymentHolder.dateText.setText(Utils.numericPaymentDateFormat.format(payment.getDate()));
        String[] dist = Utils.formatDistanceFromKiloMeters(activity, payment.getDistance());
        paymentHolder.distanceText.setText(dist[0]);
        paymentHolder.valueText.setText(Utils.formatMoney(payment.getValue()));
      } else if (holder instanceof MessageCardHolder) {
        //                MessageCardHolder messageCardHolder = (MessageCardHolder) holder;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int getItemViewType(int position) {
    if (mInternetAvailable) {
      return isPositionHeader(position) ? TYPE_HEADER : TYPE_ITEM;
    } else {
      return TYPE_ITEM_NO_INTERNET;
    }
  }

  @Override
  public int getItemCount() {
    if (mInternetAvailable) {
      return mPaymentList.size() + 1;
    } else {
      return mPaymentList.size() + 2;
    }
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

    private final View container;

    TextView idText;

    TextView dateText;

    TextView distanceText;

    TextView valueText;

    PaymentHolder(View v) {
      super(v);
      container = v;
      idText = v.findViewById(R.id.payment_id_text);
      dateText = v.findViewById(R.id.payment_date_text);
      distanceText = v.findViewById(R.id.payment_distance_text);
      valueText = v.findViewById(R.id.payment_value_text);
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