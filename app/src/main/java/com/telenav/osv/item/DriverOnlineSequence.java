package com.telenav.osv.item;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by kalmanb on 7/11/17.
 */
public class DriverOnlineSequence extends OnlineSequence {

    private static final String TAG = "DriverOnlineSequence";

    private final List<DriverPayRateBreakdownCoverageItem> driverPayRateBreakdownCoverageItem = new ArrayList<>(0);

    public DriverOnlineSequence(int sequenceId,
                                Date date,
                                int originalImageCount,
                                String address,
                                String thumbLink,
                                boolean obd,
                                String platform,
                                String platformVersion,
                                String appVersion,
                                int distance,
                                double value,
                                String currency,
                                @Nullable List<DriverPayRateBreakdownCoverageItem> driverPayRateBreakdownCoverageItem) {
        super(sequenceId, date, originalImageCount, address, thumbLink, obd, platform, platformVersion, appVersion, distance, value);
        if (driverPayRateBreakdownCoverageItem != null) {
            this.driverPayRateBreakdownCoverageItem.addAll(driverPayRateBreakdownCoverageItem);
        }
        this.mCurrency = currency;
    }

    @NonNull
    public List<DriverPayRateBreakdownCoverageItem> getDriverPayRateBreakdownCoverage() {
        return driverPayRateBreakdownCoverageItem;
    }
}
