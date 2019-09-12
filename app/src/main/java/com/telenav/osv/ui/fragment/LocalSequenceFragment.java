package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;
import com.telenav.osv.R;
import com.telenav.osv.activity.LoginActivity;
import com.telenav.osv.activity.PlayerActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.application.initialisation.DataConsistency;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.listener.GenericListener;
import com.telenav.osv.common.model.base.OSCBaseFragment;
import com.telenav.osv.common.toolbar.OSCToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.common.ui.loader.LoadingScreen;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.utils.ActivityUtils;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.SortUtils;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.UiUtils;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * fragment displaying local tracks
 * Created by alexandra on 7/14/16.
 */
public class LocalSequenceFragment extends OSCBaseFragment implements GenericListener {

    public final static String TAG = "LocalSequenceFragment";

    /**
     * Default sequence address name.
     */
    private static final String DEFAULT_SEQUENCE_ADDRESS = "-";

    private TextView uploadButton;

    private SequenceListAdapter localSequencesAdapter;

    private ApplicationPreferences appPrefs;

    private SequenceLocalDataSource sequenceLocalDataSource;

    private TextView signatureActionBarText;

    private DataConsistency dataConsistency;

    /**
     * Instance for {@code UserDataSource} which represents the user repository.
     */
    private UserDataSource userRepository;

    /**
     * Disposable which represents the upload process.
     */
    private CompositeDisposable uploadCompositeDisposable;

    private View parentView;

    public static LocalSequenceFragment newInstance() {

        Bundle args = new Bundle();

        LocalSequenceFragment fragment = new LocalSequenceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uploadCompositeDisposable = new CompositeDisposable();
        Context context = getApplicationContext();
        VideoLocalDataSource videoLocalDataSource = Injection.provideVideoDataSource(context);
        FrameLocalDataSource frameLocalDataSource = Injection.provideFrameLocalDataSource(context);
        LocationLocalDataSource locationLocalDataSource = Injection.provideLocationLocalDataSource(context);
        userRepository = Injection.provideUserRepository(context);
        dataConsistency = Injection.provideDataConsistency(
                Injection.provideSequenceLocalDataSource(context,
                        frameLocalDataSource,
                        Injection.provideScoreLocalDataSource(context),
                        locationLocalDataSource,
                        videoLocalDataSource),
                locationLocalDataSource,
                videoLocalDataSource,
                frameLocalDataSource,
                getContext());
        dataConsistency.addListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setupUploadButton(true);
    }

    @SuppressLint("CheckResult")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_waiting, null);
        parentView = view;
        appPrefs = ((OSVApplication) getActivity().getApplication()).getAppPrefs();
        RecyclerView localRecyclerView = view.findViewById(R.id.recycler_view_fragment_waiting_sequences);
        Context context = getApplicationContext();
        this.sequenceLocalDataSource = Injection.provideSequenceLocalDataSource(context,
                Injection.provideFrameLocalDataSource(context),
                Injection.provideScoreLocalDataSource(context),
                Injection.provideLocationLocalDataSource(context),
                Injection.provideVideoDataSource(context));

        //ToDo: addChild signature to the toolbar for signature, i.e the size of the current sequences
        //signatureActionBarText = toolbar.findViewById(R.id.signature_action_bar_text);
        showLoadingIndicator();
        localSequencesAdapter = new SequenceListAdapter(getContext(), new ArrayList<>());
        localRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        localRecyclerView.setAdapter(localSequencesAdapter);
        uploadButton = view.findViewById(R.id.text_view_fragment_waiting_upload);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        //Remove swiped item from list and notify the RecyclerView
                        boolean doNotShow = appPrefs.getBooleanPreference(PreferenceTypes.K_SKIP_DELETE_DIALOG);
                        if (doNotShow) {
                            localSequencesAdapter.onDeleteItem(viewHolder);
                        } else {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialog);
                            AlertDialog deleteDialog = builder.setTitle(R.string.delete_track_title).setMessage(R.string.delete_local_track)
                                    .setPositiveButton(R.string.delete_label, (dialog, which) -> localSequencesAdapter.onDeleteItem(viewHolder)).setNegativeButton(R.string
                                            .cancel_label, (dialog, which) -> {
                                    }).setNeutralButton(R.string.delete_track_and_dont_remind_me, (dialogInterface, i) -> {
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SKIP_DELETE_DIALOG, true);
                                        localSequencesAdapter.onDeleteItem(viewHolder);
                                    }).setOnDismissListener(dialogInterface -> localSequencesAdapter.notifyItemChanged(viewHolder.getAdapterPosition())).create();
                            deleteDialog.show();
                        }
                    }
                };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(localRecyclerView);
        setupUploadButton(true);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        localSequencesAdapter.refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        localSequencesAdapter.clear();
    }

    @Override
    public void onStop() {
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideLoadingIndicator();
    }

    @Nullable
    @Override
    public LoadingScreen setupLoadingScreen() {
        return new LoadingScreen(R.layout.generic_loader, R.id.text_view_generic_loader_message, R.string.loading_sequences);
    }

    @Override
    public ToolbarSettings getToolbarSettings(OSCToolbar oscToolbar) {
        //noinspection ConstantConditions since the method is called if the getActivity is not null in the base class
        return new ToolbarSettings.Builder()
                .setTitle(R.string.upload_label)
                .setTextColor(getResources().getColor(R.color.default_black_lighter))
                .setNavigationIcon(R.drawable.vector_back_black, (v) -> getActivity().onBackPressed())
                .setBackgroundColor(getResources().getColor(R.color.default_white))
                .build();
    }

    @Override
    public void onSuccess() {
        Log.d(TAG, "onSuccess. Status: success. Message: Data consistency successful.");
        dataConsistency.dispose();
        dataConsistency = null;
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> localSequencesAdapter.refresh());
        }
    }

    @Override
    public void onError() {
        Log.d(TAG, "onError. Status: error. Message: Data consistency failed.");
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                Toast.makeText(getContext(), R.string.something_wrong_try_again, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    /**
     * Closes the current fragment.
     */
    private void closeFragment() {
        Log.d(TAG, "closeFragment. Status: success. Message: Closing the fragment.");
        Activity activity = getActivity();
        if (activity != null) {
            activity.onBackPressed();
        }
    }

    private void setupUploadButton(boolean enable) {
        Log.d(TAG, "setupUploadButton: ");
        if (uploadButton != null) {
            uploadButton.setText(R.string.upload_all_label);
            if (enable) {
                uploadButton.setBackgroundColor(getResources().getColor(R.color.default_purple));
                uploadButton.setOnClickListener(click -> startUploadIfPossible());
                return;
            }
            uploadButton.setBackgroundColor(getResources().getColor(R.color.default_gray));
            uploadButton.setOnClickListener(click -> Toast.makeText(getContext(), R.string.insert_sd_card_to_upload, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * This will check all upload requirements for start and issue the upload start if the upload
     */
    private void startUploadIfPossible() {
        if (getContext() == null) {
            UiUtils.showSnackBar(getContext(), parentView, getString(R.string.something_wrong_try_again), Snackbar.LENGTH_SHORT, null, null);
            return;
        }
        uploadCompositeDisposable.add(Completable.create(emitter -> {
            boolean isInternetAvailable = NetworkUtils.isInternetConnectionAvailable(getContext(), appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED));
            Log.d(TAG, String.format("actionUploadAllListener internet. Status: %s.", isInternetAvailable));
            if (isInternetAvailable) {
                emitter.onComplete();
            } else {
                emitter.onError(new Throwable(getString(R.string.no_internet_connection_label)));
            }
        }).andThen(Completable.create(emitter -> {
            boolean sequencePopulated = sequenceLocalDataSource.isPopulated();
            if (sequencePopulated) {
                emitter.onComplete();
            } else {
                emitter.onError(new Throwable(getString(R.string.no_local_recordings_message)));
            }
        })).andThen(userRepository.getUser())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //onSuccess
                        user -> handleUploadSuccess(),
                        //onError
                        throwable -> {
                            Log.d(TAG, String.format("startUploadIfPossible. Status: error. Message :%s.", throwable.getMessage()));
                            UiUtils.showSnackBar(getContext(), parentView, throwable.getMessage(), Snackbar.LENGTH_SHORT, null, null);
                        },
                        //onComplete
                        () -> {
                            Log.d(TAG, "startUploadIfPossible. Status: complete. Message: User not found.");
                            UiUtils.showSnackBar(getContext(), parentView, getString(R.string.login_to_upload_warning), Snackbar.LENGTH_LONG, getString(R.string.login_label),
                                    () -> {
                                        if (Utils.isInternetAvailable(getContext())) {
                                            startActivity(new Intent(getContext(), LoginActivity.class));
                                        } else {
                                            UiUtils.showSnackBar(getContext(), parentView, getString(R.string.check_internet_connection), Snackbar.LENGTH_LONG, null, null);
                                        }
                                    }
                            );
                        }));
    }

    /**
     * Handles the success case for upload action by creating an alert dialog for the user which will show the info and actions for upload confirmation.
     */
    private void handleUploadSuccess() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialog);
        builder.setMessage(getString(R.string.upload_all_warning))
                .setTitle(getString(R.string.upload_all_warning_title))
                .setNegativeButton(R.string.cancel_label, (dialog, which) -> {
                    //no action required on cancel
                })
                .setPositiveButton(R.string.upload_all_label, (dialog, which) -> openUploadFragment()).create().show();
    }

    /**
     * Opens the hints screen.
     */
    private void openUploadFragment() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        ActivityUtils.replaceFragment(activity.getSupportFragmentManager(),
                UploadFragment.newInstance(),
                R.id.layout_activity_obd_fragment_container,
                true,
                UploadFragment.TAG);
    }

    private void reverseGeocodeAddress(final LocalSequence sequence, final LocalSequenceFragment.ViewHolder holder) {
        Location initialLocation = sequence.getDetails().getInitialLocation();
        if (initialLocation.getLatitude() == 0 || initialLocation.getLongitude() == 0) {
            Log.e(TAG, "reverseGeocodeAddress: lat or lon 0");
            return;
        }
        String address = DEFAULT_SEQUENCE_ADDRESS;
        if (SKReverseGeocoderManager.getInstance() != null) {
            SKCoordinate skCoordinate = new SKCoordinate();
            skCoordinate.setLatitude(initialLocation.getLatitude());
            skCoordinate.setLongitude(initialLocation.getLongitude());
            SKSearchResult addr = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(skCoordinate);
            if (addr != null) {
                address = "" + addr.getName();
                String city = "", state = "";
                for (SKSearchResultParent p : addr.getParentsList()) {
                    switch (p.getParentType()) {
                        case CITY:
                            city = p.getParentName();
                            break;
                        case CITY_SECTOR:
                            if (city.equals("")) {
                                city = p.getParentName();
                            }
                            break;
                        case STATE:
                            state = p.getParentName();
                            break;
                    }
                }
                if (!city.equals("")) {
                    address += ", " + city;
                }
                if (!state.equals("")) {
                    address += ", " + state;
                }
                sequenceLocalDataSource.updateAddressName(sequence.getID(), address);
            }
        }
        sequence.getDetails().setAddressName(address);
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> holder.addressTitle.setText(sequence.getDetails().getAddressName()));
        }
    }

    private void refreshSignatureValue(List<LocalSequence> sequences) {
        if (signatureActionBarText != null) {
            long diskSize = 0;
            for (LocalSequence localSequence : sequences) {
                diskSize += localSequence.getLocalDetails().getDiskSize();
            }
            String diskSizeStr = String.valueOf(diskSize);
            Log.d(TAG, "refreshSignatureValue: signature " + diskSizeStr);
            signatureActionBarText.setText(FormatUtils.formatSize(diskSize));
            signatureActionBarText.setVisibility(View.VISIBLE);
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView addressTitle;

        private TextView totalImages;

        private TextView dateTimeText;

        private TextView totalDistance;

        private TextView error;

        ViewHolder(View view) {
            super(view);
            addressTitle = view.findViewById(R.id.text_view_item_local_sequence_address);
            totalImages = view.findViewById(R.id.text_view_item_local_sequence_total_images);
            totalDistance = view.findViewById(R.id.text_view_item_local_sequence_total_distance);
            dateTimeText = view.findViewById(R.id.text_view_item_local_sequence_date_time);
            error = view.findViewById(R.id.text_view_item_local_sequence_error);
        }
    }

    private class SequenceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;

        private List<LocalSequence> data;

        private DateTimeFormatter dateTimeFormatter;

        private long diskSize = 0;

        private CompositeDisposable compositeDisposable;

        private String internalPath;

        SequenceListAdapter(Context context, List<LocalSequence> data) {
            this.data = data;
            inflater = LayoutInflater.from(context);
            dateTimeFormatter = DateTimeFormat.forPattern(Utils.numericDateFormatPattern);
            internalPath = Utils.getInternalStoragePath(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_local_sequence, parent, false);
            ViewHolder holder = new ViewHolder(view);
            view.setTag(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder iholder, int position) {
            if (iholder instanceof LocalSequenceFragment.ViewHolder) {
                LocalSequenceFragment.ViewHolder holder = (LocalSequenceFragment.ViewHolder) iholder;
                final LocalSequence sequence = data.get(position);
                SequenceDetailsLocal sequenceDetailsLocal = sequence.getLocalDetails();
                SequenceDetails sequenceDetails = sequence.getDetails();
                String addressName = sequenceDetails.getAddressName();
                if (StringUtils.isEmpty(addressName)) {
                    new Thread(() -> reverseGeocodeAddress(sequence, holder)).start();
                } else {
                    holder.addressTitle.setText(addressName);
                }
                holder.addressTitle.setTag(sequence);
                if (!sequenceDetailsLocal.getFolder().getPath().contains(internalPath)) {
                    holder.addressTitle
                            .setCompoundDrawablesWithIntrinsicBounds(
                                    null,
                                    null,
                                    getResources().getDrawable((R.drawable.ic_sd_storage_black_18dp)),
                                    null);
                }

                String distanceText = "";
                if (sequenceDetails.getDistance() >= 0) {
                    String[] distance = FormatUtils.formatDistanceFromMeters(getContext(), (int) sequenceDetails.getDistance(), FormatUtils.SEPARATOR_SPACE);
                    distanceText = distance[0] + distance[1];
                }

                holder.totalImages.setText(String.format("%s IMG", sequence.getCompressionDetails().getLocationsCount()));
                holder.totalDistance.setText(distanceText);
                holder.dateTimeText.setText(new DateTime(sequence.getDetails().getDateTime()).toString(dateTimeFormatter));
                int color = getResources().getColor(R.color.md_grey_900);
                holder.addressTitle.setTextColor(color);
                holder.totalImages.setTextColor(color);
                holder.totalDistance.setTextColor(color);
                holder.dateTimeText.setTextColor(color);
                int colorId = R.color.default_white;
                int consistencyCheck = sequence.getLocalDetails().getConsistencyStatus();
                if (consistencyCheck == SequenceDetailsLocal.SequenceConsistencyStatus.VALID) {
                    bindValidSequenceView(holder, sequence.getID());
                } else {
                    colorId = R.color.sequence_inconsistent;
                    @StringRes int textMessage;
                    if (consistencyCheck == SequenceDetailsLocal.SequenceConsistencyStatus.EXTERNAL_DATA_MISSING) {
                        textMessage = R.string.invalid_sequence_missing_sdcard_physical_data;
                    } else {
                        textMessage = R.string.invalid_sequence_missing_physical_data;
                    }
                    holder.error.setText(textMessage);
                    holder.error.setVisibility(View.VISIBLE);
                }
                holder.itemView.setBackgroundColor(getContext().getResources().getColor(colorId));
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        void refresh() {
            showLoadingIndicator();
            if (dataConsistency == null || dataConsistency.getStatus() != DataConsistency.DataConsistencyStatus.PROCESSING) {
                getCompositeDisposable().add(sequenceLocalDataSource
                        .getSequences()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                //onSuccess
                                sequences -> {
                                    hideLoadingIndicator();
                                    refreshSignatureValue(sequences);
                                    if (sequences.isEmpty()) {
                                        Log.d(TAG, "sequenceListAdapter refresh. Status: complete. Message: Sequences are empty.");
                                        Toast.makeText(getContext(), R.string.empty_sequences, Toast.LENGTH_SHORT).show();
                                        closeFragment();
                                        return;
                                    }

                                    Log.d(TAG, "sequenceListAdapter refresh. Status: success. Message: Sequences loaded successful.");
                                    SortUtils.sortLocalSequences(sequences);
                                    data = sequences;
                                    boolean sdCardMissingAll = true;
                                    for (LocalSequence localSequence : data) {
                                        diskSize += localSequence.getLocalDetails().getDiskSize();
                                        sdCardMissingAll &= localSequence.getLocalDetails().getConsistencyStatus() == SequenceDetailsLocal.SequenceConsistencyStatus.EXTERNAL_DATA_MISSING;
                                    }
                                    setupUploadButton(!sdCardMissingAll);
                                    super.notifyDataSetChanged();
                                },
                                //throwable
                                throwable -> {
                                    hideLoadingIndicator();
                                    Log.d(TAG, String.format("sequenceListAdapter refresh. Status: error. Message: %s.", throwable.getLocalizedMessage()));
                                    Toast.makeText(getContext(), R.string.something_wrong_try_again, Toast.LENGTH_SHORT).show();
                                    closeFragment();
                                }));
            }
        }

        void clear() {
            if (compositeDisposable != null) {
                compositeDisposable.dispose();
            }
        }

        void onDeleteItem(RecyclerView.ViewHolder viewHolder) {
            if (data == null || data.isEmpty()) {
                Toast.makeText(getContext(), R.string.delete_local_sequence_error, Toast.LENGTH_SHORT).show();
                return;
            }
            final int position = viewHolder.getAdapterPosition();
            final LocalSequence sequence = data.get(Math.max(0, position));
            final String sequenceId = sequence.getID();
            data.remove(position);
            notifyItemRemoved(position);
            getCompositeDisposable().add(
                    (Completable.create(emitter -> {
                        boolean delete = sequenceLocalDataSource.deleteSequence(sequenceId);
                        if (delete) {
                            boolean deleteFolder = sequence.getLocalDetails().getFolder().delete();
                            Log.d(TAG, String.format("onDeleteItem. Status: %s. Message: Deleting sequence folder from the device.", deleteFolder));
                            emitter.onComplete();
                        } else {
                            emitter.onError(new Throwable("Delete failed."));
                        }
                    }))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    //onSuccess
                                    () -> {
                                        Log.d(TAG, String.format("onDeleteItem. Status: successful. Sequence id: %s. Message: Delete was successful.", sequenceId));
                                        long deleteSequenceSize = sequence.getLocalDetails().getDiskSize();
                                        diskSize -= deleteSequenceSize;
                                        refresh();
                                        EventBus.post(new SequencesChangedEvent(false, true, diskSize));
                                    },
                                    //onError
                                    throwable -> Log.d(TAG, String.format("onDeleteItem. Status: error. Sequence id: %s. Message: %s.", sequenceId,
                                            throwable.getLocalizedMessage()))
                            ));
        }

        /**
         * @return {@code CompositeDisposable} which is:
         * <ul>
         * <li>new instance if the composite object has been already disposed</li>
         * <li>existing instance if the composite object is set and has not been already disposed</li>
         * </ul>
         */
        private CompositeDisposable getCompositeDisposable() {
            if (compositeDisposable == null || compositeDisposable.isDisposed()) {
                compositeDisposable = new CompositeDisposable();
            }
            return compositeDisposable;
        }

        /**
         * Binds the view of the {@code holder} for a sequence which is data consistent.
         * @param holder the holder which references the UI views.
         * @param sequenceId the identifier for the sequence.
         */
        private void bindValidSequenceView(ViewHolder holder, String sequenceId) {
            holder.error.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> sequenceLocalDataSource
                    .getSequenceWithAll(sequenceId)
                    .subscribeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                        Log.d(TAG, String.format("onBindViewHolder tap. Status: dispose. Id: %s. Message: Cleaning resources.", sequenceId));
                        hideLoadingIndicator();
                    })
                    .subscribe(
                            //onSuccess
                            item -> {
                                Log.d(TAG, String.format("onBindViewHolder tap. Status: success. Id: %s. Message: Sequence loaded successful.", sequenceId));
                                Context context = getContext();
                                if (context != null) {
                                    Intent intent = new Intent(getContext(), PlayerActivity.class);
                                    intent.putExtra(PlayerActivity.EXTRA_SEQUENCE_ID, sequenceId);
                                    context.startActivity(intent);
                                }
                            },
                            //onError
                            throwable -> {
                                Log.d(TAG, String.format("onBindViewHolder tap. Status: error. Id: %s. Message: %s.", sequenceId, throwable.getLocalizedMessage()));
                                Toast.makeText(getContext(), R.string.something_wrong_try_again, Toast.LENGTH_SHORT).show();
                            },
                            //onComplete
                            () -> {
                                Log.d(TAG, String.format("onBindViewHolder tap. Status: complete. Id: %s. Message: Sequence not found.", sequenceId));
                                Toast.makeText(getContext(), R.string.something_wrong_try_again, Toast.LENGTH_SHORT).show();
                            }
                    ));
        }
    }
}

