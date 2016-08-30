package com.telenav.osv.ui.list;

import android.content.Context;
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
import android.text.style.TabStopSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.utils.Utils;

import java.util.List;

/**
 * Created by alexandra on 7/12/16.
 */

public class SequenceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_ITEM_NO_INTERNET = 2;

    private final boolean showHeader;
    private boolean mInternetAvailable;

    List<Sequence> mSequenceList;

    MainActivity activity;

    private TextView txtWeekPositionValue;
    private TextView txtImagesNumber;
    private TextView txtTracksNumber;
    private TextView txtDistance;
    private TextView txtObd;
    private TextView txtTitleHeader;

    public SequenceAdapter(List<Sequence> results, MainActivity activity) {
        this(results, activity, true);

    }

    public SequenceAdapter(List<Sequence> results, MainActivity activity, boolean showHeader) {
        mSequenceList = results;
        this.activity = activity;
        this.showHeader = showHeader;
        mInternetAvailable = Utils.isInternetAvailable(this.activity);
    }

    public void setOnline(boolean online){
        mInternetAvailable = online;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_profile_header, parent, false);
            if (!showHeader) {
                v.setVisibility(View.GONE);
                v.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            }
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_ITEM) {
            CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.sequence_card, null);

            layoutView.setUseCompatPadding(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int compatMargin = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? (int) -layoutView.getCardElevation() : 0;

            int topMargin = (int) (5 * activity.getResources().getDisplayMetrics().density);
            int fiveDips = (int) (5 * activity.getResources().getDisplayMetrics().density);
            compatMargin = compatMargin + 3 * fiveDips;
            params.setMargins(compatMargin, topMargin, compatMargin, fiveDips);
            layoutView.setLayoutParams(params);
            SequenceHolder sequenceHolder = new SequenceHolder(layoutView);
            return sequenceHolder;

        } else if (viewType == TYPE_ITEM_NO_INTERNET) {
            CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.sequence_card_no_internet, null);

            layoutView.setUseCompatPadding(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int compatMargin = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? (int) -layoutView.getCardElevation() : 0;

            int topMargin = (int) (5 * activity.getResources().getDisplayMetrics().density);
            int fiveDips = (int) (5 * activity.getResources().getDisplayMetrics().density);
            compatMargin = compatMargin + 3 * fiveDips;
            params.setMargins(compatMargin, topMargin, compatMargin, fiveDips);
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
                final Sequence sequence = mSequenceList.get(position);

                Glide.with(activity)
                        .load(sequence.thumblink)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .skipMemoryCache(false)
                        .placeholder(R.drawable.image_loading_background)
                        .signature(new StringSignature(sequence.thumblink + " " + sequence.location.getLatitude() + ", " + sequence.location.getLongitude()))
                        .error(R.drawable.image_broken_background)
                        .listener(MainActivity.mGlideRequestListener)
                        .into(sequenceHolder.imageItem);

                if (sequence.address.equals("")) {
                    reverseGeocodeAddress(sequence, true);
                } else {
                    ((SequenceHolder) holder).addressTextItem.setText(sequence.address.equals("") ? "<location>" : sequence.address);
                }
                ((SequenceHolder) holder).totalImagesTextItem.setText(sequence.originalImageCount + " IMG");
                ((SequenceHolder) holder).dateTimeTextItem.setText(sequence.title);
                if (sequence.mTotalLength > 0) {
                    ((SequenceHolder) holder).totalLengthTextItem.setVisibility(View.VISIBLE);
                    String[] distance = Utils.formatDistanceFromMeters(activity, sequence.mTotalLength);
                    ((SequenceHolder) holder).totalLengthTextItem.setText(distance[0] + distance[1]);
                }
                ((SequenceHolder) holder).container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.openScreen(MainActivity.SCREEN_PREVIEW, sequence);
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
                        reverseGeocodeAddress(sequence, false);
                    }
                }, 1500);
            }
        } else {
            if (retry) {
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reverseGeocodeAddress(sequence, false);
                    }
                }, 1500);
            }
        }

    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    @Override
    public int getItemCount() {
        if (mInternetAvailable) {
            return mSequenceList.size() + 1;
        } else {
            return mSequenceList.size() + 2;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        if (mInternetAvailable) {
            return TYPE_ITEM;
        } else {
            return TYPE_ITEM_NO_INTERNET;
        }
    }


    public static class SequenceHolder extends RecyclerView.ViewHolder {

        View container;
        ImageView imageItem;
        TextView addressTextItem;
        TextView totalImagesTextItem;
        TextView dateTimeTextItem;
        TextView totalLengthTextItem;


        public SequenceHolder(View v) {
            super(v);
            container = v;
            addressTextItem = (TextView) v.findViewById(R.id.sequence_address_label);
            imageItem = (ImageView) v.findViewById(R.id.sequence_image);
            totalImagesTextItem = (TextView) v.findViewById(R.id.total_images_textView);
            dateTimeTextItem = (TextView) v.findViewById(R.id.sequence_datetime_label);
            totalLengthTextItem = (TextView) v.findViewById(R.id.total_length_label);

        }
    }

    public static class MessageCardHolder extends RecyclerView.ViewHolder {
        View container;

        public MessageCardHolder(View v) {
            super(v);
            container = v;
        }
    }

    public void refreshDetails(String[] totalDistanceFormatted, String[] obdDistanceFormatted, String totalPhotos, String overallRank, String totalTracks, String weeklyRank) {
        txtTitleHeader.setText(overallRank);
        txtWeekPositionValue.setText(weeklyRank);

        String imagesText = activity.getString(R.string.account_images_label);
        String tracksText = activity.getString(R.string.account_tracks_label);
        String distanceText = activity.getString(R.string.account_distance_label);
        String obdText = activity.getString(R.string.account_obd_label);

        txtImagesNumber.setText(getSpannable(imagesText, totalPhotos));
        txtTracksNumber.setText(getSpannable(tracksText, totalTracks));
        txtDistance.setText(getSpannable(distanceText, totalDistanceFormatted[0] + " " + totalDistanceFormatted[1]));
        txtObd.setText(getSpannable(obdText, obdDistanceFormatted[0] + " " + obdDistanceFormatted[1]));
    }

    public SpannableString getSpannable(String first, String second) {
        SpannableString styledString = new SpannableString(first + second);
        styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, first.length(), 0);
        styledString.setSpan(new StyleSpan(Typeface.BOLD), first.length(), second.length() + first.length(), 0);
        styledString.setSpan(new AbsoluteSizeSpan(15, true), 0, first.length(), 0);
        styledString.setSpan(new AbsoluteSizeSpan(15, true), first.length(), second.length() + first.length(), 0);
        styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.md_grey_600)), 0, first.length(), 0);
        styledString.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.text_colour_default)), first.length(), second.length() + first.length(), 0);
        return styledString;
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {


        public HeaderViewHolder(View itemView) {
            super(itemView);
            txtTitleHeader = (TextView) itemView.findViewById(R.id.global_rank_value);
            txtWeekPositionValue = (TextView) itemView.findViewById(R.id.this_week_position_value);
            txtImagesNumber = (TextView) itemView.findViewById(R.id.images_text_view);
            txtTracksNumber = (TextView) itemView.findViewById(R.id.tracks_text_view);
            txtDistance = (TextView) itemView.findViewById(R.id.distance_text_view);
            txtObd = (TextView) itemView.findViewById(R.id.obd_text_view);
        }
    }
}