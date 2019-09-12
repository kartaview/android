package com.telenav.osv.data.user.model.details.driver;

import androidx.annotation.Nullable;

/**
 * Class which holds information related to payment for a driver type user account.
 * @author horatiuf
 */

public class DriverPayment {

    /**
     * The symbol used for currency.
     */
    private String currencySymbol;

    /**
     * Total paid number for the account.
     */
    private double paidValue;

    /**
     * Payment model for the current driver. The values might be:
     * <ul>
     * <li>2.0 - represents BYOD 2.0 payment model</li>
     * <li>1.0 - represents BYOD 1.0 payment model</li>
     * </ul>
     * This value can be also null which will by default be taken as payment model 1.0.
     */
    @Nullable
    private String paymentModel;

    /**
     * Details about the payment for the driver at the end of the month.
     * @see DriverToBePaid
     */
    private DriverToBePaid driverToBePaid;

    /**
     * Default constructor for the current class.
     * @param currencySymbol {@code String} representing {@link #currencySymbol}.
     * @param paidValue {@code double} representing {@link #paidValue}.
     * @param driverToBePaid {@code DriverToBePaid} representing {@link #driverToBePaid}.
     * @param paymentModel {@code } representing {@link #paymentModel}.
     */
    public DriverPayment(String currencySymbol, double paidValue, DriverToBePaid driverToBePaid, @Nullable String paymentModel) {
        this.currencySymbol = currencySymbol;
        this.driverToBePaid = driverToBePaid;
        this.paidValue = paidValue;
        this.paymentModel = paymentModel;
    }

    /**
     * @return {@code String} representing {@link #currencySymbol}.
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * @return {@code double} representing {@link #paidValue}.
     */
    public double getPaidValue() {
        return paidValue;
    }

    /**
     * @return {@code String} representing {@link #paymentModel}.
     */
    @Nullable
    public String getPaymentModel() {
        return paymentModel;
    }

    /**
     * @return {@code DriverToBePaid} representing {@link #driverToBePaid}.
     */
    public DriverToBePaid getDriverToBePaid() {
        return driverToBePaid;
    }
}
