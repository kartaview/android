package com.telenav.osv.ui.binding.viewmodel.profile;

import android.annotation.SuppressLint;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.item.Payment;
import com.telenav.osv.utils.Utils;

/**
 * ViewModel used for the sequence cards list in my profile screen
 * Created by kalmanb on 8/29/17.
 */
public class PaymentItemViewModel extends BaseObservable {

  private final ValueFormatter valueFormatter;

  private Payment payment;

  public PaymentItemViewModel(ValueFormatter valueFormatter) {
    this.valueFormatter = valueFormatter;
  }

  public void setPayment(Payment payment) {
    this.payment = payment;
    notifyChange();
  }

  @SuppressLint("DefaultLocale")
  @Bindable
  public String getPaymentId() {
    return String.format("%03d", payment.getId());
  }

  @Bindable
  public String getPaymentDistance() {
    return valueFormatter.formatDistanceFromKiloMeters(payment.getDistance())[0];
  }

  @Bindable
  public String getPaymentValue() {
    return valueFormatter.formatMoney(payment.getValue());
  }

  @Bindable
  public String getPaymentDate() {
    return Utils.numericPaymentDateFormat.format(payment.getDate());
  }
}
