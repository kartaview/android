package com.telenav.osv.ui.fragment.camera.tagging.viewmodel;

import android.app.Application;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.recorder.tagging.logger.RecordingTaggingLogger;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class RecordingTaggingViewModelImpl extends AndroidViewModel implements RecordingTaggingViewModel {

    /**
     * Instance to the taggingLogger which handles the business logic.
     */
    private RecordingTaggingLogger taggingLogger;

    private RecorderManager recorderManager;

    /**
     * Default constructor for the current class.
     * @param application
     */
    public RecordingTaggingViewModelImpl(@NonNull Application application) {
        super(application);
        OSVApplication app = (OSVApplication) application;
        this.recorderManager = app.getRecorder();
        this.taggingLogger = recorderManager.getRecordingTaggingLogger();
    }

    @Override
    public void sendDropNoteEvent(String toString) {
        taggingLogger.dropNote(toString);
    }

    @Override
    public void sendNarrowRoadEvent(String toString) {
        taggingLogger.onNarrowRoad(toString);
    }

    @Override
    public void sendRoadClosedEvent(String toString) {
        taggingLogger.onRoadClosed(toString);
    }

    @Override
    public void sendRoadWayEvent(boolean isOneWay) {
        taggingLogger.onRoadWay(isOneWay);
    }

    @Override
    public void startTaggingLogging() {
        taggingLogger.initTagging(recorderManager.getFolder());
    }

    @Override
    public boolean isOneWay() {
        return taggingLogger.isOneWay();
    }
}