package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.ProgressListener;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.service.UploadHandlerService;
import com.telenav.osv.ui.custom.ScrollDisabledListView;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by alexandra on 7/14/16.
 */

public class WaitingFragment extends Fragment implements UploadProgressListener {
    public final static String TAG = "WaitingFragment";

    private UploadManager uploadManager;

    private ScrollDisabledListView localListView;

    private MainActivity activity;

    private Button uploadButton;

    private SequenceListAdapter localSequencesAdapter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waiting, null);
        activity = (MainActivity) getActivity();
        localListView = (ScrollDisabledListView) view.findViewById(R.id.local_list);
        uploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
        localListView.canScroll(true);
        final ConcurrentHashMap<Integer, Sequence> localSequences = Sequence.getStaticSequences();
        localSequencesAdapter = new SequenceListAdapter(activity, new ArrayList<>(localSequences.values()), false);
        localListView.setAdapter(localSequencesAdapter);
        uploadButton = (Button) view.findViewById(R.id.upload_button);
        if (activity.mUploadHandlerService != null) {
            activity.mUploadHandlerService.addUploadProgressListener(this);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        if (activity.mUploadHandlerService != null) {
            activity.mUploadHandlerService.removeUploadProgressListener(this);
        }
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        localSequencesAdapter.notifyDataSetChanged();

    }

    public void onUploadServiceConnected(UploadHandlerService service) {

    }

    @Override
    public void onResume() {
        super.onResume();
        setupUploadButton();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setupUploadButton();
    }

    public void setupUploadButton() {
        Log.d(TAG, "setupUploadButton: ");
        if (uploadButton != null) {
            if (uploadManager != null && uploadManager.isUploading()) {
                if (uploadManager.isPaused()) {
                    uploadButton.setText("Upload Paused");
                } else {
                    uploadButton.setText("Uploading...");
                }
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.openScreen(MainActivity.SCREEN_UPLOAD_PROGRESS);
                    }
                });
            } else if (SequenceDB.instance.getNumberOfFrames() <= 0) {
                uploadButton.setText("Clear History");
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SequenceDB.instance.deleteHistory();
                        Sequence.forceRefreshLocalSequences();
                        localSequencesAdapter.data.clear();
                        localSequencesAdapter.data = new ArrayList<>(Sequence.getLocalSequences().values());
                        localSequencesAdapter.notifyDataSetChanged();
                        uploadButton.setText("Upload all");
                        activity.onBackPressed();
                    }
                });
            } else {
                uploadButton.setText("Upload all");
                uploadButton.setOnClickListener(activity.actionUploadAllListener);
            }

        }
    }

    private void reverseGeocodeAddress(final Sequence sequence, final WaitingFragment.ViewHolder holder, boolean retry) {
        if (sequence.location.getLatitude() == 0 || sequence.location.getLongitude() == 0) {
            Log.e(TAG, "reverseGeocodeAddress: lat or lon 0");
            return;
        }
        if (SKReverseGeocoderManager.getInstance() != null) {
            SKSearchResult addr = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(new SKCoordinate(sequence.location.getLongitude(), sequence.location
                    .getLatitude()));
            if (addr != null) {
                sequence.address = addr.getName();
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
                    sequence.address += ", " + city;
                }
                if (!state.equals("")) {
                    sequence.address += ", " + state;
                }
            }
            if (sequence.address.equals("") && retry) {
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reverseGeocodeAddress(sequence, holder, false);
                    }
                }, 1500);
            }
        } else {
            android.util.Log.d(TAG, "reverseGeocodeAddress: not ready");
            if (retry) {
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reverseGeocodeAddress(sequence, holder, false);
                    }
                }, 1500);
            }
        }

        holder.addressTitle.setText(sequence.address.equals("") ? "<location>" : sequence.address);
    }

    @Override
    public void onUploadStarted(long totalSize) {
        setupUploadButton();
        if (localSequencesAdapter != null) {
            localSequencesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUploadingMetadata() {

    }

    @Override
    public void onPreparing(int nrOfFrames) {

    }

    @Override
    public void onIndexingFinished() {
        if (localSequencesAdapter != null) {
            localSequencesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUploadPaused() {
        setupUploadButton();
        if (localSequencesAdapter != null) {
            localSequencesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUploadResumed() {
        setupUploadButton();
        if (localSequencesAdapter != null) {
            localSequencesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUploadCancelled(int total, int remaining) {
        setupUploadButton();
        if (localSequencesAdapter != null) {
            localSequencesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUploadFinished(int successful, int unsuccessful) {
        setupUploadButton();
        if (localSequencesAdapter != null) {
            localSequencesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onProgressChanged(long total, long remaining) {

    }

    @Override
    public void onImageUploaded(Sequence sequence, boolean success) {
        if (success) {
            int total = sequence.originalImageCount;
            if (total == 0) {
                total = 1;
            }
            int remaining = sequence.imageCount;
            int status = sequence.getStatus();
            for (ProgressListener pb : sequence.getProgressListeners()) {
                pb.update(((total - remaining) * 100) / total, status);
            }
        }
    }

    @Override
    public void onSequenceUploaded(Sequence sequence) {

    }

    @Override
    public void onIndexingSequence(Sequence sequence, int remainingRecordings) {
        if (localSequencesAdapter != null) {
            localSequencesAdapter.notifyDataSetChanged();
        }
        for (ProgressListener pl : sequence.getProgressListeners()) {
            pl.holder.uploadProgress.setIndeterminate(true);
        }
    }

    @Override
    public void onBandwidthStateChange(ConnectionQuality bandwidthState, double bandwidth) {

    }

    public static class ViewHolder {

        public TextView addressTitle;

        public TextView totalImages;

        public TextView totalDistance;

        public TextView dateTimeSequence;

        public ProgressBar uploadProgress;

        public ImageView rightIcon;

        public View fader;
    }

    private class SequenceListAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        private ArrayList<Sequence> data;


        SequenceListAdapter(Context context, ArrayList<Sequence> data, boolean online) {
            this.data = data;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public void notifyDataSetChanged() {
            synchronized (Sequence.getStaticSequences()) {
                data = (new ArrayList<>(Sequence.getStaticSequences().values()));
            }

            super.notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final WaitingFragment.ViewHolder holder;
            View view = convertView;
            final Sequence sequence = data.get(position);
            if (view != null) {
                View t = view.findViewById(R.id.text);
                if (t != null) {
                    if (t.getTag() instanceof Sequence) {
                        View p = view.findViewById(R.id.sequence_progressbar);
                        if (p != null && p.getTag() instanceof ProgressListener) {
                            ((Sequence) t.getTag()).getProgressListeners().remove(p.getTag());
                        }
                    }
                }

            }
            if (view == null) {
                holder = new WaitingFragment.ViewHolder();
                view = inflater.inflate(R.layout.item_local_sequence, parent, false);
                holder.addressTitle = (TextView) view.findViewById(R.id.address_text);
                holder.totalImages = (TextView) view.findViewById(R.id.total_images_text);
                holder.totalDistance = (TextView) view.findViewById(R.id.total_distance_text);
                holder.dateTimeSequence = (TextView) view.findViewById(R.id.date_time_text);
                holder.uploadProgress = (ProgressBar) view.findViewById(R.id.sequence_progressbar);
                holder.uploadProgress.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
                holder.rightIcon = (ImageView) view.findViewById(R.id.right_icon);
                holder.fader = view.findViewById(R.id.fader);
                view.setTag(holder);
            } else {
                holder = (WaitingFragment.ViewHolder) view.getTag();
            }

            if (sequence.address.equals("")) {
                reverseGeocodeAddress(sequence, holder, true);
            } else {
                holder.addressTitle.setText(sequence.address.equals("") ? "<location>" : sequence.address);

            }

            holder.addressTitle.setTag(sequence);
            ProgressListener pl = new ProgressListener(activity, sequence, holder);
            sequence.addProgressListener(pl);
            holder.uploadProgress.setTag(pl);
            int status = sequence.getStatus();
            if (status == Sequence.STATUS_NEW || status == Sequence.STATUS_INTERRUPTED) {
                if (sequence.folder.getPath().contains(Utils.EXTERNAL_STORAGE_PATH)) {
                    sequence.mIsExternal = true;
                }

            }

            String distanceText = "";
            if (sequence.mTotalLength >= 0) {
                String[] distance = Utils.formatDistanceFromMeters(activity, sequence.mTotalLength);
                distanceText = distance[0] + distance[1];
            }

            holder.totalImages.setText(sequence.originalImageCount + " IMG");
            holder.totalDistance.setText(distanceText);
            holder.dateTimeSequence.setText(sequence.title);
            if (sequence.getStatus() == Sequence.STATUS_NEW || sequence.getStatus() == Sequence.STATUS_INDEXING || sequence.getStatus() == Sequence.STATUS_INTERRUPTED) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (sequence.getStatus() != Sequence.STATUS_UPLOADING || sequence.getStatus() != Sequence.STATUS_FINISHED) {
                            if (Utils.checkSDCard(activity) || !sequence.mIsExternal) {
//                                activity.showSnackBar("Under construction - local video preview is not yet fully implemented.", Snackbar.LENGTH_LONG);
                                activity.openScreen(MainActivity.SCREEN_PREVIEW, sequence);
                            } else {
                                activity.showSnackBar("SDcard is missing.", Snackbar.LENGTH_LONG);
                            }
                        } else {
                            activity.showSnackBar("SDcard is missing!", Snackbar.LENGTH_SHORT);
                        }
                    }

                });
            }

            return view;
        }

    }

}

