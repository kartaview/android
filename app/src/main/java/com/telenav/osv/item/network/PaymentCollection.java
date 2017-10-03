package com.telenav.osv.item.network;

import java.util.ArrayList;
import com.telenav.osv.item.Payment;

/**
 * Class holding payment objects
 * Created by kalmanb on 7/12/17.
 */
public class PaymentCollection extends ApiResponse {

    /**
     * total number of results
     */
    private int totalFilteredItems;

    private ArrayList<Payment> paymentList = new ArrayList<>();

    private String mCurrency;

    public int getTotalFilteredItems() {
        return totalFilteredItems;
    }

    public void setTotalFilteredItems(int totalFilteredItems) {
        this.totalFilteredItems = totalFilteredItems;
    }

    public ArrayList<Payment> getPaymentList() {
        return paymentList;
    }

    public String getCurrency() {
        return mCurrency;
    }

    public void setCurrency(String currency) {
        this.mCurrency = currency;
    }
}
