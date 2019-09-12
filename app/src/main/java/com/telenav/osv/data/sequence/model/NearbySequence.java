package com.telenav.osv.data.sequence.model;

import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;

/**
 * @author horatiuf
 */
public class NearbySequence extends Sequence {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = NearbySequence.class.getSimpleName();

    /**
     * Default constructor for the base model class.
     */
    public NearbySequence(String ID, SequenceDetails details, SequenceDetailsCompressionBase framesDetails) {
        super(ID, details, framesDetails, null);
    }

    @Override
    public int getType() {
        return SequenceTypes.NEARBY;
    }
}
