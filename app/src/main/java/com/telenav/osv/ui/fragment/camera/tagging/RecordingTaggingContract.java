package com.telenav.osv.ui.fragment.camera.tagging;

import com.telenav.osv.item.OSVFile;
import androidx.annotation.Nullable;
import io.reactivex.functions.Action;

public interface RecordingTaggingContract {

    /**
     * The recording tagging view which holds all the available ui related functionality required by the presenter in order to tag at recording time.
     */
    interface RecordingTaggingView {

        void sendNarrowRoadEvent(String toString);

        void sendRoadClosedEvent(String toString);

        void sendDropNoteEvent(String toString);

        void sendRoadWayEvent(boolean isOneWay);

        void startTaggingLogging();
    }

    /**
     * The logic in order to tag at recording time.
     */
    interface RecordingTaggingPresenter {

        void onRoadClosed(@Nullable String note);

        void onNarrowRoad(@Nullable String note);

        void onRoadWay(boolean isOneWay);

        Action initTagging(OSVFile sequenceFolder);

        void dropNote(@Nullable String note);
    }
}
