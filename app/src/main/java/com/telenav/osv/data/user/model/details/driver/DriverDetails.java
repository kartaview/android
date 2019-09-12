package com.telenav.osv.data.user.model.details.driver;

import com.telenav.osv.data.user.model.details.BaseUserDetails;
import androidx.annotation.NonNull;

/**
 * The driver type of account represented by {@link UserDetailsTypes#DRIVER} denoted by {@link #getType()} method.
 * <p>
 * The class hold information such as:
 * <ul>
 * <li>{@link #driverPayment}</li>
 * <li>{@link #rejectedDistance}</li>
 * </ul>
 * This information is besides the one from the {@link BaseUserDetails} parent.
 * @author horatiuf
 * @see BaseUserDetails
 */

public class DriverDetails extends BaseUserDetails {

    /**
     * Details about the total payment of the driver so far.
     */
    @NonNull
    private DriverPayment driverPayment;

    /**
     * The total number of distance rejected by the server.
     */
    private double rejectedDistance;

    /**
     * Default constructor for the current class.
     * @param photosCount {@code int} representing {@link #photosCount}.
     * @param tracksCount {@code int} representing {@link #tracksCount}.
     * @param obdDistance {@code double} representing {@link #obdDistance}.
     * @param distance {@code double} representing {@link #distance}.
     * @param driverPayment {@code DriverPayment} representing {@link #driverPayment}.
     * @param rejectedDistance {@code double} representing {@link #rejectedDistance}.
     */
    public DriverDetails(int photosCount, int tracksCount, double obdDistance, double distance, @NonNull DriverPayment driverPayment, double
            rejectedDistance) {
        super(photosCount, tracksCount, obdDistance, distance);
        this.driverPayment = driverPayment;
        this.rejectedDistance = rejectedDistance;
    }

    @Override
    public int getType() {
        return UserDetailsTypes.DRIVER;
    }

    /**
     * @return {@code double} representing {@link #rejectedDistance}.
     */
    public double getRejectedDistance() {
        return rejectedDistance;
    }

    /**
     * @return {@code DriverPayment} representing {@link #driverPayment}.
     */
    @NonNull
    public DriverPayment getDriverPayment() {
        return driverPayment;
    }
}
