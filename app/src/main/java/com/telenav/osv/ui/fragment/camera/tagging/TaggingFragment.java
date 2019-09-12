package com.telenav.osv.ui.fragment.camera.tagging;

import org.joda.time.LocalDateTime;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.common.model.base.OSCBaseFragment;
import com.telenav.osv.common.toolbar.OSCToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.ui.fragment.camera.RecordingStoppedDialogFragment;
import com.telenav.osv.ui.fragment.camera.controls.CameraControlsFragment;
import com.telenav.osv.ui.fragment.camera.controls.event.RecordingDetails;
import com.telenav.osv.ui.fragment.camera.controls.viewmodel.RecordingViewModel;
import com.telenav.osv.ui.fragment.camera.tagging.viewmodel.RecordingTaggingViewModel;
import com.telenav.osv.ui.fragment.camera.tagging.viewmodel.RecordingTaggingViewModelImpl;
import com.telenav.osv.utils.AnimationUtils;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import static com.telenav.osv.ui.fragment.camera.controls.CameraControlsFragment.FORMAT_FROM_HH_MM_SS_TO_HH_MM_SS;
import static com.telenav.osv.ui.fragment.camera.controls.CameraControlsFragment.FORMAT_FROM_H_MM_SS_TO_HH_MM_SS;
import static com.telenav.osv.ui.fragment.camera.controls.CameraControlsFragment.FORMAT_HOUR_FINISHED;
import static com.telenav.osv.ui.fragment.camera.controls.CameraControlsFragment.FORMAT_MINUTES_FINISHED;

public class TaggingFragment extends OSCBaseFragment {

    public static final String TAG = TaggingFragment.class.getSimpleName();

    private View fragmentLayout;

    private ConstraintLayout taggingOneWay;

    private ConstraintLayout taggingTwoWay;

    private TextView textViewTaggingOneWay;

    private TextView textViewTaggingTwoWay;

    private ImageView imageVideoGps;

    private Chronometer chronometer;

    private View chronometerLayout;

    /**
     * The {@code TextView} representing the images value.
     */
    private TextView textViewImagesValue;

    /**
     * The {@code TextView} representing the images label.
     */
    private TextView textViewImagesLabel;

    /**
     * The {@code TextView} representing the track size value.
     */
    private TextView textViewSizeValue;

    /**
     * The {@code TextView} representing the size's unit label.
     */
    private TextView textViewSizeLabel;

    /**
     * The {@code TextView} representing the distance value.
     */
    private TextView textViewDistanceValue;

    /**
     * The {@code TextView} representing the distance's unit label.
     */
    private TextView textViewDistanceLabel;

    private RecordingTaggingViewModel taggingViewModel;

    /**
     * The {@code ViewModel} which handles the logic for recording details and recording operations.
     */
    private RecordingViewModel recordingViewModel;

    private RecordingStoppedDialogFragment dialogFragment;

    private ProgressBar progressBar;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param bundle the Bundle used in order too persist data.
     * @return A new instance of fragment TaggingFragment.
     */
    public static TaggingFragment newInstance(Bundle bundle) {
        TaggingFragment taggingFragment = new TaggingFragment();
        taggingFragment.setArguments(bundle);
        return taggingFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        fragmentLayout = inflater.inflate(R.layout.fragment_tagging, container, false);
        progressBar = fragmentLayout.findViewById(R.id.progress_bar);
        if (getActivity() != null) {
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.default_black));
        }
        this.taggingViewModel = ViewModelProviders.of(this).get(RecordingTaggingViewModelImpl.class);
        this.recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel.class);
        if (recordingViewModel.isTaggingMode()) {
            AnimationUtils.resizeCameraPreview(true, getActivity(), fragmentLayout);
        } else {
            recordingViewModel.setTaggingMode(true);
        }
        taggingViewModel.startTaggingLogging();
        initTaggingWay();
        initGpsView();
        initChronometer();
        initRecordingDetails();
        setRecordingDetails();
        initRecordingTaggingViews();
        observeOnRecordingEvent();
        observeOnRecordingDetailsEvent();
        observeOnLocationEvent();
        observeOnRecordingErrors();
        Bundle bundle = getArguments();
        if (bundle != null) {
            textViewImagesValue.setText(bundle.getString(Constants.ARG_PICS, "0"));
            textViewDistanceValue.setText(bundle.getString(Constants.ARG_DISTANCE, "0"));
            textViewSizeValue.setText(bundle.getString(Constants.ARG_SIZE, "0"));
            textViewImagesLabel.setText(R.string.partial_img_label);
            textViewDistanceLabel.setText(bundle.getString(Constants.ARG_DISTANCE_LABEL));
            textViewSizeLabel.setText(bundle.getString(Constants.ARG_SIZE_LABEL));
            if (bundle.containsKey(Constants.ARG_GPS_ICON)) {
                imageVideoGps.setImageDrawable(ContextCompat.getDrawable(getContext(), bundle.getInt(Constants.ARG_GPS_ICON)));
            }
        }
        return fragmentLayout;
    }

    @Nullable
    @Override
    public ToolbarSettings getToolbarSettings(OSCToolbar oscToolbar) {
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        recordingViewModel.onStartTagging();
    }

    @Override
    public void onStop() {
        super.onStop();
        recordingViewModel.onStop();
    }

    @Override
    public boolean handleBackPressed() {
        AnimationUtils.resizeCameraPreview(false, getActivity(), fragmentLayout);
        if (getActivity() != null) {
            getActivity().findViewById(R.id.layout_activity_obd_fragment_container).bringToFront();
        }
        return false;
    }

    private void observeOnRecordingDetailsEvent() {
        recordingViewModel.getCapturedImagesObservable().observe(this, image -> {
            if (image != null) {
                textViewImagesValue.setText(image.getValue());
                textViewImagesLabel.setText(image.getLabel());
            }
        });
        recordingViewModel.getSizeObservable().observe(this, size -> {
            if (size != null) {
                textViewSizeValue.setText(size.getValue());
                textViewSizeLabel.setText(size.getLabel());
            }
        });
        recordingViewModel.getDistanceObservable().observe(this, distance -> {
            if (distance != null) {
                textViewDistanceValue.setText(distance.getValue());
                textViewDistanceLabel.setText(distance.getLabel());
            }
        });
    }

    /**
     * Registers to observe on recording error in order to display a dialog.
     */
    private void observeOnRecordingErrors() {
        recordingViewModel.getRecordingErrorObservable().observe(this, wasError -> {
            if (wasError != null && wasError &&
                    getFragmentManager() != null && getFragmentManager().findFragmentByTag(RecordingStoppedDialogFragment.TAG) == null) {
                dialogFragment = new RecordingStoppedDialogFragment();
                dialogFragment.show(getFragmentManager(), RecordingStoppedDialogFragment.TAG);
            }
        });
    }

    /**
     * Registers an observer to listen for recording events.
     * The event received contains a flag which is {@code true} if the recording was started, {@code false} otherwise.
     */
    private void observeOnRecordingEvent() {
        recordingViewModel.getRecordingObservable().observe(this, isRecording -> {
            if (isRecording == null) {
                return;
            }
            if (!isRecording) {
                progressBar.setVisibility(View.GONE);
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
    }

    private void initRecordingDetails() {
        View layoutRecordingDetails = fragmentLayout.findViewById(R.id.layout_fragment_tagging_recording_details);
        View layoutImagesDetails = layoutRecordingDetails.findViewById(R.id.layout_fragment_tagging_images);
        View layoutDistanceDetails = layoutRecordingDetails.findViewById(R.id.layout_fragment_tagging_distance);
        View layoutSizeDetails = layoutRecordingDetails.findViewById(R.id.layout_fragment_tagging_size);
        textViewImagesValue = layoutImagesDetails.findViewById(R.id.text_view_recording_details_value);
        textViewImagesLabel = layoutImagesDetails.findViewById(R.id.text_view_recording_details_label);
        textViewSizeValue = layoutSizeDetails.findViewById(R.id.text_view_recording_details_value);
        textViewSizeLabel = layoutSizeDetails.findViewById(R.id.text_view_recording_details_label);
        textViewDistanceValue = layoutDistanceDetails.findViewById(R.id.text_view_recording_details_value);
        textViewDistanceLabel = layoutDistanceDetails.findViewById(R.id.text_view_recording_details_label);
    }

    /**
     * Sets the values to the recording details when navigating back to the screen when recording is started.
     */
    private void setRecordingDetails() {
        RecordingDetails recordingDetails = recordingViewModel.getCapturedImagesObservable().getValue();
        if (recordingDetails != null) {
            textViewImagesValue.setText(recordingDetails.getValue());
            textViewImagesLabel.setText(recordingDetails.getLabel());
        }
        recordingDetails = recordingViewModel.getSizeObservable().getValue();
        if (recordingDetails != null) {
            textViewSizeValue.setText(recordingDetails.getValue());
            textViewSizeLabel.setText(recordingDetails.getLabel());
        }
        recordingDetails = recordingViewModel.getDistanceObservable().getValue();
        if (recordingDetails != null) {
            textViewDistanceValue.setText(recordingDetails.getValue());
            textViewDistanceLabel.setText(recordingDetails.getLabel());
        }
    }

    private void initChronometer() {
        chronometer = fragmentLayout.findViewById(R.id.chronometer_fragment_camera_controls_stop_recording);
        chronometerLayout = fragmentLayout.findViewById(R.id.layout_chronometer_fragment_camera_controls_stop_recording);
        if (getActivity() != null) {
            long baseTime = recordingViewModel.getRecordingStartTime() -
                    LocalDateTime.now().toDateTime().getMillis();
            Log.d(TAG, "startRecording baseTime - " + baseTime);
            chronometer.setBase(SystemClock.elapsedRealtime() - Math.abs(baseTime));
        }
        chronometer.setOnChronometerTickListener(chronometer -> {
            long milliseconds = SystemClock.elapsedRealtime() - chronometer.getBase();
            if (milliseconds >= FORMAT_HOUR_FINISHED && !chronometer.getFormat().equals(FORMAT_FROM_HH_MM_SS_TO_HH_MM_SS)) {
                chronometer.setFormat(FORMAT_FROM_HH_MM_SS_TO_HH_MM_SS);
            } else if (milliseconds >= FORMAT_MINUTES_FINISHED && milliseconds < FORMAT_HOUR_FINISHED
                    && !chronometer.getFormat().equals(FORMAT_FROM_H_MM_SS_TO_HH_MM_SS)) {
                chronometer.setFormat(FORMAT_FROM_H_MM_SS_TO_HH_MM_SS);
            }
            chronometer.setText(FormatUtils.formatChronometerText((String) chronometer.getText()));
        });
        chronometer.setFormat(CameraControlsFragment.FORMAT_FROM_MM_SS_TO_HH_MM_SS);
        chronometer.start();
        chronometerLayout.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            chronometerLayout.setVisibility(View.INVISIBLE);
            recordingViewModel.stopRecording();
        });
    }

    private void initGpsView() {
        imageVideoGps = fragmentLayout.findViewById(R.id.image_view_fragment_camera_controls_gps_icon);
    }

    private void initTaggingWay() {
        taggingOneWay = fragmentLayout.findViewById(R.id.fragment_tagging_one_way);
        taggingTwoWay = fragmentLayout.findViewById(R.id.fragment_tagging_two_way);
        textViewTaggingOneWay = fragmentLayout.findViewById(R.id.text_view_fragment_tagging_one_way);
        textViewTaggingTwoWay = fragmentLayout.findViewById(R.id.text_view_fragment_tagging_two_way);
        selectTaggingWay();
        taggingOneWay.setOnClickListener(v -> {
            taggingOneWay.setSelected(true);
            taggingTwoWay.setSelected(false);
            textViewTaggingTwoWay.setSelected(false);
            textViewTaggingOneWay.setSelected(true);
            taggingViewModel.sendRoadWayEvent(true);
        });
        taggingTwoWay.setOnClickListener(v -> {
            taggingOneWay.setSelected(false);
            taggingTwoWay.setSelected(true);
            textViewTaggingTwoWay.setSelected(true);
            textViewTaggingOneWay.setSelected(false);
            taggingViewModel.sendRoadWayEvent(false);
        });
    }

    private void selectTaggingWay() {
        if (taggingViewModel.isOneWay()) {
            taggingOneWay.setSelected(true);
            textViewTaggingOneWay.setSelected(true);
            taggingViewModel.sendRoadWayEvent(true);
        } else {
            taggingTwoWay.setSelected(true);
            textViewTaggingTwoWay.setSelected(true);
            taggingViewModel.sendRoadWayEvent(false);
        }
    }

    /**
     * Registers an observer for the location accuracy type.
     */
    private void observeOnLocationEvent() {
        recordingViewModel.getLocationAccuracyObservable().observe(this, imageResource -> {
            int accuracyTypeIcon = R.drawable.ic_gps_on_big;
            if (imageResource != null) {
                accuracyTypeIcon = imageResource;
            }
            imageVideoGps.setImageDrawable(ContextCompat.getDrawable(getContext(), accuracyTypeIcon));
        });
    }

    /**
     * Initializes the recording tagging layout, which will be hidden when entering the screen until the tagging button is tapped.
     */
    private void initRecordingTaggingViews() {
        View cardViewRoadClosed = fragmentLayout.findViewById(R.id.card_view_fragment_tagging_road_closed);
        View cardViewNarrowRoad = fragmentLayout.findViewById(R.id.card_view_fragment_tagging_narrow_road);
        View cardViewDropNote = fragmentLayout.findViewById(R.id.card_view_fragment_tagging_drop_note);
        cardViewRoadClosed.setOnClickListener(v -> displayDropNoteDialog(TaggingDialogFullscreen.ROAD_CLOSED, (note) -> taggingViewModel.sendRoadClosedEvent(note)));
        cardViewNarrowRoad.setOnClickListener(v -> displayDropNoteDialog(TaggingDialogFullscreen.ROAD_NARROW, (note) -> taggingViewModel.sendNarrowRoadEvent(note)));
        cardViewDropNote.setOnClickListener(v -> displayDropNoteDialog(TaggingDialogFullscreen.DROP_NOTE, (note) -> taggingViewModel.sendDropNoteEvent(note)));
    }

    /**
     * Displays event dialog with consumer for button tap.
     */
    private void displayDropNoteDialog(@TaggingDialogFullscreen.TaggingDialogIdentifier int dialogIdentifier, Consumer<String> onButtonPressConsumer) {
        if (getActivity() == null) {
            return;
        }
        TaggingDialogFullscreen dialog = new TaggingDialogFullscreen(dialogIdentifier, onButtonPressConsumer);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        dialog.show(ft, TaggingDialogFullscreen.TAG);
    }
}