package com.telenav.osv.data.sequence.model;

import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;

/**
 * @author horatiuf
 */
public class OnlineSequence extends Sequence {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = OnlineSequence.class.getSimpleName();

    public OnlineSequence(String ID, SequenceDetails details, SequenceDetailsCompressionBase framesDetails, SequenceDetailsRewardBase rewardDetails) {
        super(ID, details, framesDetails, rewardDetails);
    }

    @Override
    public int getType() {
        return SequenceTypes.ONLINE;
    }
}
