package com.telenav.osv.network.payrate.model;

/**
 * A certain pay rate applies to a variable number of passes over the same track. This class defines an interval of distinct passes over the same track/region.
 * <p>
 * Instances of this class are immutable.
 * <p>
 * Created by catalinj on 10/18/17.
 */
public class PayRateCoverageInterval {

    /**
     * The minimum number of road passes of this interval.
     */
    public final int minPass;

    /**
     * The maximum number of road passes of this interval.
     * For the last interval the server return null and is mapped automatically to 0.
     * e.g last interval -> minPass = 6; maxPass = 0;
     * new road -> minPass = 0; maxPass = 0;
     */
    public final int maxPass;

    /**
     * Creates a new immutable {@link PayRateCoverageInterval}.
     * @param minPass minimum number of passes of this interval.
     * @param maxPass maximum number of passes of this interval.
     */
    public PayRateCoverageInterval(int minPass, int maxPass) {
        this.minPass = minPass;
        this.maxPass = maxPass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PayRateCoverageInterval that = (PayRateCoverageInterval) o;

        if (minPass != that.minPass) return false;
        return maxPass == that.maxPass;
    }

    @Override
    public int hashCode() {
        int result = minPass;
        result = 31 * result + maxPass;
        return result;
    }
}
