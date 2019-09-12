package com.telenav.osv.ui.fragment.camera.tagging.viewmodel;


/**
 * The recording tagging view model which holds all the available functionality for the tagging session.
 */
public interface RecordingTaggingViewModel {

    void sendNarrowRoadEvent(String toString);

    void sendRoadClosedEvent(String toString);

    void sendDropNoteEvent(String toString);

    void sendRoadWayEvent(boolean isOneWay);

    void startTaggingLogging();

    boolean isOneWay();
}
