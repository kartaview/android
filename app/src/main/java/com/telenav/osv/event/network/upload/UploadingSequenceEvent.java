package com.telenav.osv.event.network.upload;

import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.event.OSVEvent;

/**
 * Created by Bencze Kalman on 2/12/2017.
 */
public class UploadingSequenceEvent extends OSVEvent {

    public int remainingSequences;

    private Sequence sequence;

    public UploadingSequenceEvent(Sequence sequence, int remainingRecordings) {
        this.remainingSequences = remainingRecordings;
        this.sequence = sequence;
    }
}
