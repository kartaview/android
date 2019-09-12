package com.telenav.osv.upload.progress.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Concrete implementation of {@code UploadUpdateBase} which is used to send all update related to progress of upload.
 * <p> This will include a current unit field which is required for operation which streaming is involved.</p>
 * @see UploadUpdateBase
 */
public class UploadUpdateProgress extends UploadUpdateBase {

    /**
     * The current removed physical spaces.
     */
    private long currentUnit;

    /**
     * Flag which if {@code true} signals that the current update needs to be cancel by the parent.
     */
    private boolean cancel;

    /**
     * Flag which if {@code true} signals that the current update needs to be cancel by the parent.
     */
    private boolean archive;

    /**
     * Collection of children for the current progress update.
     */
    private List<UploadUpdateProgress> childs;

    /**
     * Default constructor for the current class.
     */
    public UploadUpdateProgress(long currentUnit, long totalUnit) {
        super(totalUnit);
        this.currentUnit = currentUnit;
        childs = new ArrayList<>();
    }

    /**
     * @return {@code true} if the current child needs to be canceled, {@code false} otherwise.
     */
    public boolean isCancel() {
        return cancel;
    }

    /**
     * @return {@code true} if the current child needs to be archived, {@code false} otherwise.
     */
    public boolean isArchive() {
        return archive;
    }

    /**
     * Sets the {@link #archive} flag on true for the current child.
     */
    public void archive() {
        this.archive = true;
    }

    /**
     * Sets the {@link #cancel} flag on true for the current child.
     */
    public void cancel() {
        this.cancel = true;
    }

    /**
     * Completes the current progress by adding all the children current units to the parent's current unit and also removing them from memory.
     * <p> This acts as an persistence of the current unit and a memory free and should be called when no future updates are required.
     */
    public void complete() {
        long newCurrentUnit = 0;
        for (Iterator<UploadUpdateProgress> iterator = childs.iterator(); iterator.hasNext(); ) {
            UploadUpdateProgress currentUpdateProgress = iterator.next();
            newCurrentUnit += currentUpdateProgress.getCurrentUnit();
            iterator.remove();
        }
        currentUnit += newCurrentUnit;
    }

    /**
     * Adds a {@code UploadUpdateProgress} as a child to the current progress.
     */
    public void addChild(UploadUpdateProgress uploadUpdateProgress) {
        childs.add(uploadUpdateProgress);
    }

    /**
     * Removes a {@code UploadUpdateProgress} as a child from the current progress.
     */
    public void removeChild(UploadUpdateProgress uploadUpdateProgress) {
        childs.remove(uploadUpdateProgress);
    }

    /**
     * Archives a {@code UploadUpdateProgress} by adding to this current unit the total unit of the child and also removes him from the collection in order to clear memory.
     */
    public void archive(UploadUpdateProgress uploadUpdateProgress) {
        currentUnit += uploadUpdateProgress.getTotalUnit();
        removeChild(uploadUpdateProgress);
    }

    /**
     * @return either the {@link #currentUnit} for the current class or the sum between {@link #currentUnit} and all the children's {@link #currentUnit} if he has any.
     */
    public long getCurrentUnit() {
        if (childs.isEmpty()) {
            return currentUnit;
        } else {
            long childsCurrentUnit = 0;
            for (UploadUpdateProgress uploadUpdateProgress : childs) {
                childsCurrentUnit += uploadUpdateProgress.getCurrentUnit();
            }
            return currentUnit + childsCurrentUnit;
        }
    }

    /**
     * Updates the current unit value for the current class.
     */
    public void setCurrentUnit(long currentUnit) {
        this.currentUnit = currentUnit;
    }
}
