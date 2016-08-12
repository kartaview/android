package com.telenav.osv.listener;

import android.content.Context;
import android.view.View;
import com.telenav.osv.R;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.ui.fragment.WaitingFragment;

/**
 * Created by Kalman on 1/19/16.
 */
public class ProgressListener {


    private static final String TAG = "ProgressListener";

    private final Sequence sequence;

    public final WaitingFragment.ViewHolder holder;

    private final Context context;

    public ProgressListener(Context context, Sequence sequence, WaitingFragment.ViewHolder holder) {
        this.sequence = sequence;
        this.holder = holder;
        this.context = context;
    }

    public void update(int percentage, int status) {
        percentage = Math.min(100, percentage);
        percentage = Math.max(0, percentage);
        holder.uploadProgress.setIndeterminate(false);
        holder.uploadProgress.setMax(100);
        holder.uploadProgress.setProgress(percentage);
        holder.fader.setVisibility(View.GONE);
        switch (status){
            case Sequence.STATUS_NEW:
                holder.addressTitle.setText(sequence.address);
                holder.uploadProgress.setVisibility(View.GONE);
                holder.rightIcon.setVisibility(View.GONE);
                break;
            case Sequence.STATUS_INDEXING:
                holder.uploadProgress.setVisibility(View.VISIBLE);
                holder.addressTitle.setText(sequence.address);
                holder.rightIcon.setVisibility(View.GONE);
                break;
            case Sequence.STATUS_INTERRUPTED:
                holder.uploadProgress.setVisibility(View.VISIBLE);
                holder.fader.setVisibility(View.VISIBLE);
                holder.addressTitle.setText("Interrupted: " + sequence.address);
                holder.rightIcon.setVisibility(View.VISIBLE);
                holder.rightIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_pause_black_24dp));
                holder.rightIcon.setImageAlpha(50);
                holder.rightIcon.setClickable(false);
                break;
            case Sequence.STATUS_UPLOADING:
                holder.uploadProgress.setVisibility(View.VISIBLE);
                holder.addressTitle.setText(sequence.address);
                holder.rightIcon.setVisibility(View.GONE);
                break;
            case Sequence.STATUS_FINISHED:
                holder.uploadProgress.setVisibility(View.VISIBLE);
                holder.uploadProgress.setProgress(100);
                holder.fader.setVisibility(View.VISIBLE);
                holder.addressTitle.setText(sequence.address);
                holder.rightIcon.setVisibility(View.VISIBLE);
                holder.rightIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_done_black_24dp));
                holder.rightIcon.setImageAlpha(50);
                holder.rightIcon.setClickable(false);
                break;
        }
    }
}
