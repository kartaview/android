package com.telenav.osv.data.sequence.model.details;

import com.telenav.osv.item.KVFile;

/**
 * @author horatiuf
 */
public class SequenceDetailsLocal {

    /**
     * The physical path for the sequence on the disk.
     *
     * @see KVFile
     */
    private KVFile folder;

    /**
     * The size on disk of the current sequence.
     */
    private long diskSize;

    /**
     * The status representing the consistency of the sequence.
     * <p> The default value for it is {@link SequenceConsistencyStatus#VALID}.
     */
    @SequenceConsistencyStatus
    private int consistencyStatus;

    /**
     * Default constructor for current class.
     */
    public SequenceDetailsLocal(KVFile folder,
                                long diskSize,
                                int consistencyStatus) {
        this.folder = folder;
        this.diskSize = diskSize;
        this.consistencyStatus = consistencyStatus;
    }

    public KVFile getFolder() {
        return folder;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long size) {
        this.diskSize = size;
    }

    public int getConsistencyStatus() {
        return consistencyStatus;
    }

    public void setConsistencyStatus(int consistencyStatus) {
        this.consistencyStatus = consistencyStatus;
    }

    /**
     * The consistency values which a local sequences posses. These values can be:
     * <ul>
     * <li>{@link #VALID}</li>
     * <li>{@link #EXTERNAL_DATA_MISSING}</li>
     * <li>{@link #DATA_MISSING}</li>
     * <li>{@link #METADATA_MISSING}</li>
     * </ul>
     */
    public @interface SequenceConsistencyStatus {

        /**
         * The valid value where a sequence is considered to have all data and files correct.
         */
        int VALID = 0;

        /**
         * The disabled value where a sequence is retained but not upload/pre-viewed. This will happen when the sdcard was removed and its file paths point to it.
         */
        int DATA_MISSING = 1;

        /**
         * The value where a sequence is missing the metadata file, this makes it to be generated based on database with its locations/creation timestamps only.
         */
        int METADATA_MISSING = 2;

        /**
         * The value where a sequence data is pointing to an external card which is currently removed.
         */
        int EXTERNAL_DATA_MISSING = 3;
    }
}
