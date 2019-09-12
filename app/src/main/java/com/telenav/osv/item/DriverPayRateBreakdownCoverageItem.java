package com.telenav.osv.item;

/**
 * Created by catalinj on 10/31/17.
 */
public class DriverPayRateBreakdownCoverageItem {

    public final int passes;

    public final float distance;

    public final float receivedMoney;

    public DriverPayRateBreakdownCoverageItem(int passes, float distance, float receivedMoney) {
        this.passes = passes;
        this.distance = distance;
        this.receivedMoney = receivedMoney;
    }
}
