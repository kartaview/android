package com.telenav.osv.data.sequence.model.details.reward;

/**
 * Abstract class which holds information of the reward used in sequence. This is shown by {@link #value} field.
 * Furthermore, the {@link #unit} field will keep information related too the currency symbol.
 * @author horatiuf
 */
public abstract class SequenceDetailsRewardBase {

    /**
     * The reward value for the current sequence. This is based on the user type, such as:
     * <ul>
     * <li>normal users - will have points, if enabled</li>
     * <li>byod users - will have pay rate</li>
     * </ul>
     */
    private double value;

    /**
     * The unit symbol used for reward system.
     * <p>
     * This can be null for normal users if the points are not enabled.
     */
    private String unit;

    /**
     * Default constructor for the current class.
     */
    public SequenceDetailsRewardBase(double value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }
}
