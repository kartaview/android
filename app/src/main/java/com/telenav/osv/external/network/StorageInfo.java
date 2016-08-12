package com.telenav.osv.external.network;

/**
 * Information class of device storage
 */
public class StorageInfo {
    int mRemainingPictures = 0;

    long mRemainingSpace = 0;

    long mTotalSpace = 0;

    /**
     * Acquire remaining number of images that can be shot
     * @return Remaining number of images that can be shot
     */
    public int getFreeSpaceInImages() {
        return mRemainingPictures;
    }

    /**
     * Set remaining number of images that can be shot
     * @param remainingPictures Remaining number of images that can be shot
     */
    public void setFreeSpaceInImages(int remainingPictures) {
        mRemainingPictures = remainingPictures;
    }

    /**
     * Acquire remaining capacity
     * @return Remaining capacity (unit: bytes)
     */
    public long getFreeSpaceInBytes() {
        return mRemainingSpace;
    }

    /**
     * Set remaining capacity
     * @param remainingSpace Remaining capacity (unit: bytes)
     */
    public void setFreeSpaceInBytes(long remainingSpace) {
        mRemainingSpace = remainingSpace;
    }

    /**
     * Acquire total capacity of device
     * @return Total capacity of device (unit: bytes)
     */
    public long getMaxCapacity() {
        return mTotalSpace;
    }

    /**
     * Set total capacity of device
     * @param totalSpace Total capacity of device (unit: bytes)
     */
    public void setMaxCapacity(long totalSpace) {
        mTotalSpace = totalSpace;
    }
}
