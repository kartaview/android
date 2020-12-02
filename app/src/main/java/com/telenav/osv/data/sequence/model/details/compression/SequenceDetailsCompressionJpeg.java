package com.telenav.osv.data.sequence.model.details.compression;

/**
 * @author horatiuf
 */
public class SequenceDetailsCompressionJpeg extends SequenceDetailsCompressionBase {

    /**
     * ToDo: This is UI related information, remove it at UI refactoring.
     * The index of the current used for display.
     */
    private int frameIndex;

    /**
     * Default constructor for the current class.
     */
    public SequenceDetailsCompressionJpeg(int length, String thumbnailLink, int frameIndex) {
        super(length, thumbnailLink);
        this.frameIndex = frameIndex;
    }

    @Override
    public int getCompressionType() {
        return SequenceDetailsCompressionFormat.JPEG;
    }

    @Override
    public int getLocationsCount() {
        return getLength();
    }

    @Override
    public void setLocationsCount(int frameCount) {
        setLength(frameCount);
    }

    @Override
    public String getFileExtensions() {
        return SequenceFilesExtensions.JPEG;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }
}
