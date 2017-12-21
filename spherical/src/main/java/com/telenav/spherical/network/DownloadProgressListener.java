package com.telenav.spherical.network;

/**
 * HTTP communication download listener class
 */
public interface DownloadProgressListener {

    /**
     * Total byte count
     */
    void onTotalSize(long totalSize);

    /**
     * Received byte count
     */
    void onDataReceived(int size);
}
