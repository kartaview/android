package com.telenav.osv.upload.status;

public interface UploadStatus {

    void onUploadStarted();

    void onUploadPaused();

    void onUploadStoped();

    void onUploadResume();

    void onUploadComplete();

    void onUploadProgressUpdate(UploadUpdate uploadUpdate);

    void onUploadError();

    void onNoInternetDetected();
}
