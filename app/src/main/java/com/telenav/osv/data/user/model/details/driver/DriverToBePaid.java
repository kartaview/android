package com.telenav.osv.data.user.model.details.driver;

/**
 * To be paid model for driver details. This represents payment info which will be met for the user at the end of the month.
 * @author horatiuf
 */

public class DriverToBePaid {

    /**
     * The distance which was marked as accepted by the server.
     */
    private double acceptedDistance;

    /**
     * The pay rate for {@code acceptedDistance}.
     */
    private double payRate;

    /**
     * The paid value for the {@code acceptedDistance} based on the set {@code payRate}.
     */
    private double paidValue;

    /**
     * Default constructor for the current class.
     * @param acceptedDistance {@code double} representing {@link #acceptedDistance}.
     * @param payRate {@code double} representing {@link #payRate}.
     * @param paidValue {@code double} representing {@link #paidValue}.
     */
    public DriverToBePaid(double acceptedDistance, double payRate, double paidValue) {
        this.acceptedDistance = acceptedDistance;
        this.payRate = payRate;
        this.paidValue = paidValue;
    }

    /**
     * @return {@code double} representing {@link #acceptedDistance}.
     */
    public double getAcceptedDistance() {
        return acceptedDistance;
    }

    /**
     * @return {@code double} representing {@link #payRate}.
     */
    public double getPayRate() {
        return payRate;
    }

    /**
     * @return {@code double} representing {@link #paidValue}.
     */
    public double getPaidValue() {
        return paidValue;
    }
}
