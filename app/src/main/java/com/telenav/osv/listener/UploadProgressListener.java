package com.telenav.osv.listener;

import com.facebook.network.connectionclass.ConnectionClassManager;
import com.telenav.osv.data.sequence.model.Sequence;

/**
 * Created by Kalman on 11/18/15.
 */
public interface UploadProgressListener extends ConnectionClassManager.ConnectionClassStateChangeListener {

    void onUploadStarted(long totalSize, int remainingSequences);

    void onUploadingMetadata();

    void onPreparing(int nrOfFrames);

    void onIndexingFinished();

    void onUploadPaused();

    void onUploadResumed();

    void onUploadCancelled(long total, long remaining);

    void onUploadFinished();

    void onProgressChanged(long total, long remaining);

    void onImageUploaded(Sequence sequence);

    void onSequenceUploaded(Sequence sequence);

    void onIndexingSequence(Sequence sequence, int remainingRecordings);
}
