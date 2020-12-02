package com.telenav.osv.data.sequence.legacy;

import java.util.Map;
import com.telenav.osv.data.frame.legacy.FrameLegacyModelTest;
import com.telenav.osv.data.video.legacy.VideoLegacyTestModel;

/**
 * @author horatiuf
 */
public class LocalSequenceLegacyModelTest extends SequenceLegacyModelTest {

    public static final int STATUS_NEW = 0;

    public static final int STATUS_INDEXING = 1;

    public static final int STATUS_UPLOADING = 2;

    public static final int STATUS_INTERRUPTED = 3;

    private static final String TAG = "LocalSequence";

    private static boolean sInitialized = false;

    private Map<String, VideoLegacyTestModel> videoLegacyTestModels;

    private Map<String, FrameLegacyModelTest> frameLegacyModelTestMap;

    private int mOnlineId;

    private String path;

    private boolean mIsExternal;

    private long mOriginalSize;

    private long mSize;

    private boolean mIsSafe;

    private int mVideoCount;

    public LocalSequenceLegacyModelTest(int mId, int mOnlineId, String path, boolean mIsExternal, long mOriginalSize, long mSize, boolean mIsSafe, int mVideoCount, String
            appVersion) {
        this.mId = mId;
        this.mOnlineId = mOnlineId;
        this.path = path;
        this.mIsExternal = mIsExternal;
        this.mOriginalSize = mOriginalSize;
        this.mSize = mSize;
        this.mIsSafe = mIsSafe;
        this.mVideoCount = mVideoCount;
        this.mAppVersion = appVersion;
    }

    public boolean isSafe() {
        return mIsSafe;
    }

    @Override
    public String toString() {
        return "LocalSequence (id " + mId + " images " + mFrameCount + " from " + mOriginalFrameCount + " number of videos " + mVideoCount + " and " + value + " Points" + ")";
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    public int getScore() {
        return (int) getValue();
    }

    public Map<String, FrameLegacyModelTest> getFrameLegacyModelTestMap() {
        return frameLegacyModelTestMap;
    }

    public void setFrameLegacyModelTestMap(Map<String, FrameLegacyModelTest> frameLegacyModelTestMap) {
        this.frameLegacyModelTestMap = frameLegacyModelTestMap;
    }

    public Map<String, VideoLegacyTestModel> getVideoLegacyTestModels() {
        return videoLegacyTestModels;
    }

    public void setVideoLegacyTestModels(Map<String, VideoLegacyTestModel> videoLegacyTestModels) {
        this.videoLegacyTestModels = videoLegacyTestModels;
    }

    public int getOnlineId() {
        return mOnlineId;
    }

    public void setOnlineId(int mOnlineId) {
        this.mOnlineId = mOnlineId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isExternal() {
        return mIsExternal;
    }

    public void setExternal(boolean external) {
        this.mIsExternal = external;
    }

    public long getOriginalSize() {
        return mOriginalSize;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        this.mSize = size;
    }

    public int getVideoCount() {
        return mVideoCount;
    }

    public void setVideoCount(int count) {
        this.mVideoCount = count;
    }
}

