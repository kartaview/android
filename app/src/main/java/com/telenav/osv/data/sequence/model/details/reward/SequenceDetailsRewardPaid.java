package com.telenav.osv.data.sequence.model.details.reward;

import java.util.List;
import com.telenav.osv.data.sequence.model.Sequence.SequenceTypes;
import com.telenav.osv.item.DriverPayRateBreakdownCoverageItem;

/**
 * Class used to hold information for the paid system in a sequence of type {@link SequenceTypes#PAID}.
 * <p>There is required a pay rate distribution based on coverage on a road segment.
 * @author horatiuf
 */
public class SequenceDetailsRewardPaid extends SequenceDetailsRewardBase {

    /**
     * List of {@link DriverPayRateBreakdownCoverageItem} used for a sequence of type {@link SequenceTypes#PAID}.
     */
    private List<DriverPayRateBreakdownCoverageItem> payRateBreakdownCoverageItems;

    /**
     * Default constructor for the current class.
     */
    public SequenceDetailsRewardPaid(double value, String unit, List<DriverPayRateBreakdownCoverageItem> payRateBreakdownCoverageItems) {
        super(value, unit);
        this.payRateBreakdownCoverageItems = payRateBreakdownCoverageItems;
    }

    public List<DriverPayRateBreakdownCoverageItem> getPayRateBreakdownCoverageItems() {
        return payRateBreakdownCoverageItems;
    }
}
