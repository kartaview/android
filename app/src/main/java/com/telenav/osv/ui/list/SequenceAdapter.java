package com.telenav.osv.ui.list;

import java.util.List;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.Utils;

/**
 * Created by alexandra on 7/12/16.
 */

public class SequenceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;

    private static final int TYPE_ITEM = 1;

    private static final int TYPE_ITEM_NO_INTERNET = 2;

    private final boolean showHeader;

    List<Sequence> mSequenceList;

    MainActivity activity;

    private boolean mInternetAvailable;

    private ProgressWheel experienceProgress;

    private TextView experienceText;

    private TextView levelText;

    private TextView rankText;

    private TextView txtImagesNumber;

    private TextView txtTracksNumber;

    private TextView txtDistance;

    private TextView txtObd;

    private TextView scoreText;

    public SequenceAdapter(List<Sequence> results, MainActivity activity, boolean showHeader) {
        mSequenceList = results;
        this.activity = activity;
        this.showHeader = showHeader;
        mInternetAvailable = Utils.isInternetAvailable(this.activity);
    }

    public void setOnline(boolean online) {
        mInternetAvailable = online;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_profile_header, parent, false);
            if (!showHeader) {
                v.setVisibility(View.GONE);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(0, 0);
                int fiveDips = (int) (5 * activity.getResources().getDisplayMetrics().density);
                lp.setMargins(-fiveDips, 0, -fiveDips, fiveDips);
                v.setLayoutParams(lp);
            }
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_ITEM) {
            CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sequence_card, null);

            layoutView.setUseCompatPadding(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int compatMargin = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? (int) -layoutView.getCardElevation() : 0;

            int topMargin = (int) (5 * activity.getResources().getDisplayMetrics().density);
            int fiveDips = (int) (5 * activity.getResources().getDisplayMetrics().density);
            compatMargin = compatMargin + 3 * fiveDips;
            params.setMargins(fiveDips, topMargin, fiveDips, fiveDips);
            layoutView.setLayoutParams(params);
            SequenceHolder sequenceHolder = new SequenceHolder(layoutView);
            return sequenceHolder;

        } else if (viewType == TYPE_ITEM_NO_INTERNET) {
            CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_no_internet_card, null);

            layoutView.setUseCompatPadding(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int compatMargin = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? (int) -layoutView.getCardElevation() : 0;

            int topMargin = (int) (5 * activity.getResources().getDisplayMetrics().density);
            int fiveDips = (int) (5 * activity.getResources().getDisplayMetrics().density);
            compatMargin = compatMargin + 3 * fiveDips;
            params.setMargins(fiveDips, topMargin, fiveDips, fiveDips);
            layoutView.setLayoutParams(params);
            MessageCardHolder messageCardHolder = new MessageCardHolder(layoutView);
            return messageCardHolder;
        }
        return null;

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {

            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

            } else if (holder instanceof SequenceHolder) {
                SequenceHolder sequenceHolder = (SequenceHolder) holder;
                final Sequence sequence = mSequenceList.get(Math.min(position - (showHeader ? 1 : 0), mSequenceList.size() - 1));

                Glide.with(activity)
                        .load(sequence.thumblink)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .skipMemoryCache(false)
                        .placeholder(R.drawable.custom_image_loading_background)
                        .signature(new StringSignature(sequence.thumblink + " " + sequence.location.getLatitude() + ", " + sequence.location.getLongitude()))
                        .error(R.drawable.custom_image_broken_background)
                        .listener(MainActivity.mGlideRequestListener)
                        .into(sequenceHolder.imageItem);

                if (sequence.address.equals("")) {
                    reverseGeocodeAddress(sequence, true);
                } else {
                    sequenceHolder.addressTextItem.setText(sequence.address.equals("") ? sequence.location.getLatitude() + ", " + sequence.location.getLongitude() : sequence.address);
                }
                sequenceHolder.totalImagesTextItem.setText(sequence.originalImageCount + " IMG");
                sequenceHolder.dateTimeTextItem.setText(sequence.title);
                if (showHeader) {
                    String first;
                    if (sequence.score > 10000){
                        first = sequence.score / 1000 + "K\n";
                    } else {
                        first = sequence.score + "\n";
                    }
                    String second = "pts";
                    SpannableString styledString = new SpannableString(first + second);
                    styledString.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(), 0);
                    styledString.setSpan(new StyleSpan(Typeface.NORMAL), first.length(), second.length() + first.length(), 0);
                    styledString.setSpan(new AbsoluteSizeSpan(16, true), 0, first.length(), 0);
                    styledString.setSpan(new AbsoluteSizeSpan(12, true), first.length(), second.length() + first.length(), 0);
                    sequenceHolder.pointsTextItem.setText(styledString);
                } else {
                    sequenceHolder.pointsBackground.setVisibility(View.GONE);
                    sequenceHolder.pointsTextItem.setVisibility(View.GONE);
                }
                if (sequence.mTotalLength > 0) {
                    sequenceHolder.totalLengthTextItem.setVisibility(View.VISIBLE);
                    String[] distance = Utils.formatDistanceFromMeters(activity, sequence.mTotalLength);
                    sequenceHolder.totalLengthTextItem.setText(distance[0] + distance[1]);
                }
                sequenceHolder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.openScreen(ScreenComposer.SCREEN_PREVIEW, sequence);
                    }
                });

            } else if (holder instanceof MessageCardHolder) {
                MessageCardHolder messageCardHolder = (MessageCardHolder) holder;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void reverseGeocodeAddress(final Sequence sequence, boolean retry) {
        if (sequence.location.getLatitude() == 0 || sequence.location.getLongitude() == 0) {
            return;
        }
        Sequence.reverseGeocodeAddress(sequence,activity);

    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    @Override
    public int getItemCount() {
        if (mInternetAvailable) {
            return mSequenceList.size() + (showHeader ? 1 : 0);
        } else {
            return mSequenceList.size() + (showHeader ? 2 : 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position) && showHeader) {
            return TYPE_HEADER;
        }
        if (mInternetAvailable) {
            return TYPE_ITEM;
        } else {
            return TYPE_ITEM_NO_INTERNET;
        }
    }


    public void refreshDetails(String[] totalDistance, String[] obdDistance, String totalPhotos, String totalTracks, int rank, int score, int level, int xpProgress,
                               int xpTarget) {
        if (scoreText != null) {
            String first = "Score\n";
            String second = Utils.formatNumber(score);
            SpannableString styledString = new SpannableString(first + second);
            styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, first.length(), 0);
            styledString.setSpan(new StyleSpan(Typeface.BOLD), first.length(), second.length() + first.length(), 0);
            styledString.setSpan(new AbsoluteSizeSpan(16, true), 0, first.length(), 0);
            styledString.setSpan(new AbsoluteSizeSpan(20, true), first.length(), second.length() + first.length(), 0);
            styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.white)), 0, first.length(), 0);
            styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.white)), first.length(), second.length() + first.length(), 0);
            scoreText.setText(styledString);
        }
        if (rankText != null) {
            String first = "Rank\n";
            String second = "" + (rank == 0 ? "-" : rank);
            SpannableString styledString = new SpannableString(first + second);
            styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, first.length(), 0);
            styledString.setSpan(new StyleSpan(Typeface.BOLD), first.length(), second.length() + first.length(), 0);
            styledString.setSpan(new AbsoluteSizeSpan(16, true), 0, first.length(), 0);
            styledString.setSpan(new AbsoluteSizeSpan(20, true), first.length(), second.length() + first.length(), 0);
            styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.action_bar_blue)), 0, first.length(), 0);
            styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.action_bar_blue)), first.length(), second.length() + first.length(), 0);
            rankText.setText(styledString);
            rankText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.goToLeaderboard();
                }
            });
        }
        if (levelText != null) {
            levelText.setText("" + level);
        }
        if (experienceText != null) {
            String first = Utils.formatNumber(xpTarget - score) + "";
            String second = " points to next level";
            SpannableString styledString = new SpannableString(first + second);
            styledString.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(), 0);
            styledString.setSpan(new StyleSpan(Typeface.NORMAL), first.length(), second.length() + first.length(), 0);
            experienceText.setText(styledString);
        }
        if (experienceProgress != null) {
            float levelLenght = xpTarget - (score - xpProgress);
            float progressPercentage = ((int) ((float) xpProgress / levelLenght * 100f)) / 100f;
            if (progressPercentage > 0.9f){
                progressPercentage = 0.9f;
            }
            experienceProgress.setProgress(progressPercentage);
        }

        //the lower all-time stats
        if (activity != null) {
            String imagesText = activity.getString(R.string.account_images_label);
            String tracksText = activity.getString(R.string.account_tracks_label);
            String distanceText = activity.getString(R.string.account_distance_label);
            String obdText = activity.getString(R.string.account_obd_label);

            if (txtImagesNumber != null) {
                txtImagesNumber.setText(getSpannableForStats(imagesText, totalPhotos));
            }
            if (txtTracksNumber != null) {
                txtTracksNumber.setText(getSpannableForStats(tracksText, totalTracks));
            }
            if (txtDistance != null) {
                txtDistance.setText(getSpannableForStats(distanceText, totalDistance[0] + " " + totalDistance[1]));
            }
            if (txtObd != null) {
                txtObd.setText(getSpannableForStats(obdText, obdDistance[0] + " " + obdDistance[1]));
            }
        }
    }

    private SpannableString getSpannableForStats(String first, String second) {
        SpannableString styledString = new SpannableString(first + second);
        styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, first.length(), 0);
        styledString.setSpan(new StyleSpan(Typeface.BOLD), first.length(), second.length() + first.length(), 0);
        styledString.setSpan(new AbsoluteSizeSpan(15, true), 0, first.length(), 0);
        styledString.setSpan(new AbsoluteSizeSpan(15, true), first.length(), second.length() + first.length(), 0);
        styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.md_grey_600)), 0, first.length(), 0);
        styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.text_colour_default)), first.length(), second.length() + first.length(), 0);
        return styledString;
    }


    private static class SequenceHolder extends RecyclerView.ViewHolder {

        ImageView pointsBackground;

        TextView pointsTextItem;

        View container;

        ImageView imageItem;

        TextView addressTextItem;

        TextView totalImagesTextItem;

        TextView dateTimeTextItem;

        TextView totalLengthTextItem;


        SequenceHolder(View v) {
            super(v);
            container = v;
            addressTextItem = (TextView) v.findViewById(R.id.sequence_address_label);
            imageItem = (ImageView) v.findViewById(R.id.sequence_image);
            totalImagesTextItem = (TextView) v.findViewById(R.id.total_images_textView);
            dateTimeTextItem = (TextView) v.findViewById(R.id.sequence_datetime_label);
            totalLengthTextItem = (TextView) v.findViewById(R.id.total_length_label);
            pointsTextItem = (TextView) v.findViewById(R.id.points_text);
            pointsBackground = (ImageView) v.findViewById(R.id.points_background);
        }
    }

    private static class MessageCardHolder extends RecyclerView.ViewHolder {
        View container;

        MessageCardHolder(View v) {
            super(v);
            container = v;
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {


        HeaderViewHolder(View itemView) {
            super(itemView);
            scoreText = (TextView) itemView.findViewById(R.id.score_text);
            rankText = (TextView) itemView.findViewById(R.id.rank_text);
            levelText = (TextView) itemView.findViewById(R.id.level_text);
            experienceText = (TextView) itemView.findViewById(R.id.experience_text);
            experienceProgress = (ProgressWheel) itemView.findViewById(R.id.experience_progress);
            txtImagesNumber = (TextView) itemView.findViewById(R.id.images_text_view);
            txtTracksNumber = (TextView) itemView.findViewById(R.id.tracks_text_view);
            txtDistance = (TextView) itemView.findViewById(R.id.distance_text_view);
            txtObd = (TextView) itemView.findViewById(R.id.obd_text_view);
        }
    }
}