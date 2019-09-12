package com.telenav.osv.data.sequence.model;

import com.telenav.osv.data.OSVBaseModel;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Base class for the sequence in the application. This uses composition to hold all the information related to the sequence such as:
 * <ul>
 * <li>{@link #details}</li>
 * <li>{@link #framesDetails}</li>
 * <li>{@link #rewardDetails}</li>
 * </ul>
 * @author horatiuf
 */
public abstract class Sequence extends OSVBaseModel {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = Sequence.class.getSimpleName();

    /**
     * The generic details of the sequence.
     * @see SequenceDetails
     */
    protected SequenceDetails details;

    /**
     * The details of the sequence related to frames.
     * @see SequenceDetailsCompressionBase
     */
    private SequenceDetailsCompressionBase framesDetails;

    /**
     * The details of the sequence related to reward.
     * @see SequenceDetailsRewardBase
     */
    @Nullable
    private SequenceDetailsRewardBase rewardDetails;

    /**
     * Default constructor for the base model class.
     * @param ID {@code String} representing {@link #ID}.
     */
    public Sequence(String ID, @NonNull SequenceDetails details, @NonNull SequenceDetailsCompressionBase framesDetails, @Nullable SequenceDetailsRewardBase rewardDetails) {
        super(ID);
        this.details = details;
        this.framesDetails = framesDetails;
        this.rewardDetails = rewardDetails;
    }

    public SequenceDetails getDetails() {
        return details;
    }

    public SequenceDetailsCompressionBase getCompressionDetails() {
        return framesDetails;
    }

    @Nullable
    public SequenceDetailsRewardBase getRewardDetails() {
        return rewardDetails;
    }

    public void setRewardDetails(@Nullable SequenceDetailsRewardBase rewardDetails) {
        this.rewardDetails = rewardDetails;
    }

    @SequenceTypes
    public abstract int getType();

    /**
     * This represents all available sequence types such as:
     * <ul>
     * <li>{@link #LOCAL}</li>
     * <li>{@link #ONLINE}</li>
     * <li>{@link #NEARBY}</li>
     * </ul>
     * The last two mentioned types are remote based.
     */
    public @interface SequenceTypes {

        /**
         * The flag which denotes that the sequence concrete implementation is local.
         */
        int LOCAL = 0;

        /**
         * The flag which denotes that the sequence concrete implementation is online.
         */
        int ONLINE = 1;

        /**
         * The flag which denotes that the sequence concrete implementation is nearby.
         */
        int NEARBY = 2;
    }
}
