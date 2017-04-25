package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.PlayerActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.upload.UploadCancelledEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * fragment displaying local tracks
 * Created by alexandra on 7/14/16.
 */
public class WaitingFragment extends Fragment {
    public final static String TAG = "WaitingFragment";

    private MainActivity activity;

    private Button uploadButton;

    private SequenceListAdapter localSequencesAdapter;

    private ApplicationPreferences appPrefs;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_waiting, null);
        activity = (MainActivity) getActivity();
        appPrefs = activity.getApp().getAppPrefs();
        RecyclerView localRecyclerView = (RecyclerView) view.findViewById(R.id.local_list);
        final ConcurrentHashMap<Integer, Sequence> localSequences = Sequence.getStaticSequences();
        localSequencesAdapter = new SequenceListAdapter(activity, new ArrayList<>(localSequences.values()), false);
        localRecyclerView.setLayoutManager(new LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false));
        localRecyclerView.setAdapter(localSequencesAdapter);
        uploadButton = (Button) view.findViewById(R.id.upload_button);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

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
                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
                    AlertDialog deleteDialog = builder.setTitle(R.string.delete_track_title).setMessage(R.string.delete_local_track).setPositiveButton(R.string.delete_label,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    localSequencesAdapter.onDeleteItem(viewHolder);
                                }
                            }).setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).setNeutralButton(R.string.delete_track_and_dont_remind_me, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            appPrefs.saveBooleanPreference(PreferenceTypes.K_SKIP_DELETE_DIALOG, true);
                            localSequencesAdapter.onDeleteItem(viewHolder);
                        }
                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            localSequencesAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                        }
                    }).create();
                    deleteDialog.show();
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(localRecyclerView);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        localSequencesAdapter.refresh();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.register(this);
    }

    @Override
    public void onStop() {
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        int size = Sequence.checkDeletedSequences();
        if (size <= 0 && activity != null){
            activity.openScreen(ScreenComposer.SCREEN_MAP);
            return;
        }
        if (localSequencesAdapter != null){
            localSequencesAdapter.refresh();
        }
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
            uploadButton.setText(R.string.upload_all_label);
            uploadButton.setOnClickListener(activity.actionUploadAllListener);
        }
    }

    private void reverseGeocodeAddress(final Sequence sequence, final WaitingFragment.ViewHolder holder, boolean retry) {
        if (sequence.location.getLatitude() == 0 || sequence.location.getLongitude() == 0) {
            Log.e(TAG, "reverseGeocodeAddress: lat or lon 0");
            return;
        }
        Sequence.reverseGeocodeAddress(sequence, activity);

        holder.addressTitle.setText(sequence.address);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUploadCancelled(UploadCancelledEvent event) {
        setupUploadButton();
        if (localSequencesAdapter != null) {
            localSequencesAdapter.refresh();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUploadFinished(UploadFinishedEvent event) {
        setupUploadButton();
        if (localSequencesAdapter != null) {
            localSequencesAdapter.refresh();
        }
    }

    @Subscribe
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (!event.online && localSequencesAdapter != null) {
            localSequencesAdapter.refresh();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView addressTitle;

        TextView totalImages;

        TextView dateTimeText;

        private TextView totalDistance;

        ViewHolder(View view) {
            super(view);
            addressTitle = (TextView) view.findViewById(R.id.address_text);
            totalImages = (TextView) view.findViewById(R.id.total_images_text);
            totalDistance = (TextView) view.findViewById(R.id.total_distance_text);
            dateTimeText = (TextView) view.findViewById(R.id.date_time_text);
        }
    }

    private class SequenceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;

        private ArrayList<Sequence> data;

        private Handler mDeleteHandler = new Handler(Looper.getMainLooper());


        SequenceListAdapter(Context context, ArrayList<Sequence> data, boolean online) {
            this.data = data;
            Sequence.order(this.data);
            inflater = LayoutInflater.from(context);
        }

        void refresh() {
            synchronized (Sequence.getStaticSequences()) {
                data = (new ArrayList<>(Sequence.getStaticSequences().values()));
                Sequence.order(data);
            }

            super.notifyDataSetChanged();
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
            if (iholder instanceof WaitingFragment.ViewHolder) {
                WaitingFragment.ViewHolder holder = (WaitingFragment.ViewHolder) iholder;
                final Sequence sequence = data.get(position);

                if (sequence.address.contains("Track ")) {
                    reverseGeocodeAddress(sequence, holder, true);
                } else {
                    holder.addressTitle.setText(sequence.address);
                }
                holder.addressTitle.setTag(sequence);
                int status = sequence.getStatus();
                if (status == Sequence.STATUS_NEW || status == Sequence.STATUS_INTERRUPTED) {
                    if (sequence.folder.getPath().contains(Utils.EXTERNAL_STORAGE_PATH)) {
                        sequence.mIsExternal = true;
                    }

                }
                if (sequence.mIsExternal) {
                    holder.addressTitle.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.ic_sd_storage_black_18dp)), null, null, null);
                }

                String distanceText = "";
                if (sequence.mTotalLength >= 0) {
                    String[] distance = Utils.formatDistanceFromMeters(activity, sequence.mTotalLength);
                    distanceText = distance[0] + distance[1];
                }

                holder.totalImages.setText(sequence.originalImageCount + " IMG");
                holder.totalDistance.setText(distanceText);
                holder.dateTimeText.setText(sequence.title);
                if (sequence.getStatus() == Sequence.STATUS_NEW || sequence.getStatus() == Sequence.STATUS_INDEXING) {
                    int color = activity.getResources().getColor(R.color.md_grey_900);
                    holder.addressTitle.setTextColor(color);
                    holder.totalImages.setTextColor(color);
                    Drawable drawable = activity.getResources().getDrawable(R.drawable.ic_camera_small_grey);
                    holder.totalImages.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    holder.totalDistance.setTextColor(color);
                    drawable = activity.getResources().getDrawable(R.drawable.ic_distance);
                    holder.totalDistance.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    holder.dateTimeText.setTextColor(color);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (sequence.getStatus() == Sequence.STATUS_NEW) {
                                if (Utils.checkSDCard(activity) || !sequence.mIsExternal) {
                                    if (sequence.safe){
                                        activity.openScreen(ScreenComposer.SCREEN_PREVIEW, sequence);
                                    } else {
                                        Intent intent = new Intent(activity, PlayerActivity.class);
                                        intent.putExtra(PlayerActivity.EXTRA_SEQUENCE_ID, sequence.folder.getPath());
                                        activity.startActivity(intent);
                                    }
                                } else {
                                    activity.showSnackBar(getString(R.string.sdcard_missing_message), Snackbar.LENGTH_LONG);
                                }
                            }
                        }

                    });
                } else if (sequence.getStatus() == Sequence.STATUS_INTERRUPTED) {
                    int color = activity.getResources().getColor(R.color.gray_summary_primary_text);
                    holder.addressTitle.setTextColor(color);
                    holder.addressTitle.setText("Interrupted: " + holder.addressTitle.getText());
                    holder.totalImages.setTextColor(color);
                    Drawable orig = holder.totalImages.getCompoundDrawables()[0];
                    Drawable drawable;
                    if (orig != null && holder.totalImages.getCompoundDrawables()[0].getConstantState() != null) {
                        drawable = holder.totalImages.getCompoundDrawables()[0].getConstantState().newDrawable().mutate();
                        DrawableCompat.setTint(drawable, ContextCompat.getColor(activity, R.color.gray_summary_primary_text));
                        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
                        holder.totalImages.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    }
                    holder.totalDistance.setTextColor(color);
                    orig = holder.totalDistance.getCompoundDrawables()[0];
                    if (orig != null && holder.totalDistance.getCompoundDrawables()[0].getConstantState() != null) {//
                        drawable = holder.totalDistance.getCompoundDrawables()[0].getConstantState().newDrawable().mutate();
                        drawable = DrawableCompat.wrap(drawable);
                        DrawableCompat.setTint(drawable, ContextCompat.getColor(activity, R.color.gray_summary_primary_text));
                        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
                        holder.totalDistance.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    }
                    holder.dateTimeText.setTextColor(color);
                }
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

        void onDeleteItem(RecyclerView.ViewHolder viewHolder) {
            final int position = viewHolder.getAdapterPosition();
            final Sequence sequence = data.get(Math.max(0,position));
            final int sequenceId = sequence.sequenceId;
            data.remove(position);
            activity.showSnackBar(R.string.recording_deleted, 1500, "Undo", new Runnable() {
                @Override
                public void run() {
                    mDeleteHandler.removeCallbacksAndMessages(null);
                    data.add(position,sequence);
                    notifyDataSetChanged();
                }
            });
            mDeleteHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Sequence.deleteSequence(sequenceId);
                    EventBus.post(new SequencesChangedEvent(false));
                }
            }, 1600);
            notifyDataSetChanged();
        }
    }

}

