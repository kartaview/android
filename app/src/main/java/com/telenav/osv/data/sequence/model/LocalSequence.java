package com.telenav.osv.data.sequence.model;

import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;
import androidx.annotation.NonNull;

/**
 * local sequence class, representing sequences stored on device
 * @author horatiuf
 */
public class LocalSequence extends Sequence {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = LocalSequence.class.getSimpleName();

    /**
     * @see SequenceDetailsLocal
     */
    @NonNull
    private SequenceDetailsLocal localDetails;

    /**
     * Default constructor for the current class.
     */
    public LocalSequence(String ID,
                         @NonNull SequenceDetails details,
                         @NonNull SequenceDetailsLocal localDetails,
                         @NonNull SequenceDetailsCompressionBase compressionBase,
                         @NonNull SequenceDetailsRewardBase rewardDetails) {
        super(ID, details, compressionBase, rewardDetails);
        this.localDetails = localDetails;
    }

    /**
     * Default constructor for the current class.
     */
    public LocalSequence(String ID,
                         SequenceDetails details,
                         @NonNull SequenceDetailsLocal localDetails,
                         @NonNull SequenceDetailsCompressionBase compressionBase) {
        super(ID, details, compressionBase, null);
        this.localDetails = localDetails;
    }

    @Override
    public int getType() {
        return SequenceTypes.LOCAL;
    }

    @NonNull
    public SequenceDetailsLocal getLocalDetails() {
        return localDetails;
    }
}
