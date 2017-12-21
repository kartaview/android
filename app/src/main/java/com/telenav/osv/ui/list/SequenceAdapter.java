package com.telenav.osv.ui.list;

import java.util.List;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.matthewtamlin.dotindicator.DotIndicator;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.DriverOnlineSequence;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.UserOnlineSequence;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.custom.HorizontalScrollViewImpl;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * the adapter used for displaying the uploaded tracks from the server
 * Created by kalmanbencze on 7/07/17.
 */
public class SequenceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER_USER = 0;

    private static final int TYPE_HEADER_DRIVER = 1;

    private static final int TYPE_ITEM = 2;

    private static final int TYPE_ITEM_NO_INTERNET = 3;

    private static final String TAG = "SequenceAdapter";

    private final boolean showHeader;

    private List<Sequence> mSequenceList;

    private MainActivity activity;

    private boolean mInternetAvailable;

    private int lastPosition = 0;

    private boolean isDriverHeader;

    private String[] mAcceptedDistance = new String[]{"0", "km"};

    private String[] mRejectedDistance = new String[]{"0", "km"};

    private String[] mObdDistance = new String[]{"0", "km"};

    private String mTotalPhotos = "0";

    private String mTotalTracks = "0";

    private boolean mIsHeaderScrollable;

    private boolean mAnimate = false;

    private boolean showPoints;

    public SequenceAdapter(List<Sequence> results, MainActivity activity, boolean showHeader) {
        mSequenceList = results;
        this.activity = activity;
        this.showHeader = showHeader;
        mInternetAvailable = Utils.isInternetAvailable(this.activity);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Resources resources = activity.getResources();
        if (viewType == TYPE_HEADER_USER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_profile_header_user, parent, false);
            if (!showHeader) {
                v.setVisibility(View.GONE);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(0, 0);
                int fiveDips = (int) (5 * resources.getDisplayMetrics().density);
                lp.setMargins(-fiveDips, 0, -fiveDips, fiveDips);
                v.setLayoutParams(lp);
            }
            UserHeaderViewHolder header = new UserHeaderViewHolder(v);
            return header;
        } else if (viewType == TYPE_HEADER_DRIVER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_profile_header_driver, parent, false);
            if (!showHeader) {
                v.setVisibility(View.GONE);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(0, 0);
                int fiveDips = (int) (5 * resources.getDisplayMetrics().density);
                lp.setMargins(-fiveDips, 0, -fiveDips, fiveDips);
                v.setLayoutParams(lp);
            }
            DriverHeaderViewHolder header = new DriverHeaderViewHolder(v);
            mIsHeaderScrollable = resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            return header;
        } else if (viewType == TYPE_ITEM) {
            CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sequence_card, parent, false);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            int cardMarginSide = (int) resources.getDimension(R.dimen.sequence_list_item_margin_side);
            int cardMarginTop = (int) resources.getDimension(R.dimen.sequence_list_item_margin_top);
            int cardMarginBottom = (int) resources.getDimension(R.dimen.sequence_list_item_margin_bottom);

            params.setMargins(cardMarginSide, cardMarginTop, cardMarginSide, cardMarginBottom);
            layoutView.setLayoutParams(params);
            return new SequenceHolder(layoutView);
        } else if (viewType == TYPE_ITEM_NO_INTERNET) {
            CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_no_internet_card, parent, false);

            layoutView.setUseCompatPadding(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            int cardMarginSide = (int) resources.getDimension(R.dimen.sequence_list_item_margin_side);
            int cardMarginTop = (int) resources.getDimension(R.dimen.sequence_list_item_margin_top);
            int cardMarginBottom = (int) resources.getDimension(R.dimen.sequence_list_item_margin_bottom);

            params.setMargins(cardMarginSide, cardMarginTop, cardMarginSide, cardMarginBottom);
            layoutView.setLayoutParams(params);
            return new MessageCardHolder(layoutView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {

            Resources resources = activity.getResources();
            if (holder instanceof UserHeaderViewHolder) {
                UserHeaderViewHolder header = (UserHeaderViewHolder) holder;
                String imagesText = activity.getString(R.string.account_images_label);
                String distanceText = activity.getString(R.string.account_distance_label);
                String obdText = activity.getString(R.string.account_obd_label);

                if (header.imagesNumberText != null) {
                    header.imagesNumberText.setText(getSpannableForStats(imagesText, mTotalPhotos));
                }
                if (header.distanceText != null) {
                    header.distanceText.setText(getSpannableForStats(distanceText, mAcceptedDistance[0] + " " + mAcceptedDistance[1]));
                }
                if (header.obdDistanceText != null) {
                    header.obdDistanceText.setText(getSpannableForStats(obdText, mObdDistance[0] + " " + mObdDistance[1]));
                }
                if (header.uploadedTracksText != null) {
                    String first = "Uploaded tracks - ";
                    String second = "" + mTotalTracks;
                    SpannableString styledString = new SpannableString(first + second);
                    styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, first.length(), 0);
                    styledString.setSpan(new StyleSpan(Typeface.BOLD), first.length(), second.length() + first.length(), 0);
                    header.uploadedTracksText.setText(styledString);
                }

                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) header.infoHolder.getLayoutParams();
                lp.width = resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? LinearLayout.LayoutParams.MATCH_PARENT :
                        (int) Utils.dpToPx(activity, resources.getConfiguration().smallestScreenWidthDp);
                lp.height = LinearLayout.LayoutParams.MATCH_PARENT;
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                header.infoHolder.setLayoutParams(lp);
            } else if (holder instanceof DriverHeaderViewHolder) {

                DriverHeaderViewHolder header = (DriverHeaderViewHolder) holder;
                String imagesText = activity.getString(R.string.account_images_label);
                String acceptedText = activity.getString(R.string.account_distance_accepted_label);
                String rejectedText = activity.getString(R.string.account_distance_rejected_label);
                String obdText = activity.getString(R.string.account_obd_label);

                if (header.imagesNumberText != null) {
                    header.imagesNumberText.setText(getSpannableForStats(imagesText, mTotalPhotos));
                }
                if (header.rejectedDistanceText != null) {
                    header.rejectedDistanceText.setText(getSpannableForStats(rejectedText, mRejectedDistance[0] + " " + mRejectedDistance[1]));
                }
                if (header.acceptedDistanceText != null) {
                    header.acceptedDistanceText.setText(getSpannableForStats(acceptedText, mAcceptedDistance[0] + " " + mAcceptedDistance[1]));
                }
                if (header.obdDistanceText != null) {
                    header.obdDistanceText.setText(getSpannableForStats(obdText, mObdDistance[0] + " " + mObdDistance[1]));
                }
                if (header.uploadedTracksText != null) {
                    String first = "Uploaded tracks - ";
                    String second = "" + mTotalTracks;
                    SpannableString styledString = new SpannableString(first + second);
                    styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, first.length(), 0);
                    styledString.setSpan(new StyleSpan(Typeface.BOLD), first.length(), second.length() + first.length(), 0);
                    header.uploadedTracksText.setText(styledString);
                }
                boolean portrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                if (mIsHeaderScrollable) {
                    header.dotindicator.setVisibility(View.VISIBLE);
                    header.separator.setVisibility(View.VISIBLE);
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) header.infoLayout.getLayoutParams();
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    lp.gravity = Gravity.START;
                    int width = (int) Utils.dpToPx(activity, activity.getResources().getConfiguration().smallestScreenWidthDp);
                    width = (int) ((width - activity.getResources()
                            .getDimension(portrait ? R.dimen.sequence_list_padding_side_portrait : R.dimen.sequence_list_padding_side_landscape) * 2) /
                            3 * 4);
                    header.infoLayout.setLayoutParams(lp);
                    header.infoLayout.setMinimumWidth(width);
                    header.infoLayout.requestLayout();
                } else {
                    header.dotindicator.setVisibility(View.GONE);
                    header.separator.setVisibility(View.GONE);
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) header.infoLayout.getLayoutParams();
                    int largeWidth = (int) Utils.dpToPx(activity, activity.getResources().getConfiguration().screenWidthDp);
                    int padding = (int) activity.getResources()
                            .getDimension(portrait ? R.dimen.sequence_list_padding_side_portrait : R.dimen.sequence_list_padding_side_landscape);
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.gravity = Gravity.CENTER_HORIZONTAL;
                    header.infoLayout.setLayoutParams(lp);
                    header.infoLayout.setMinimumWidth(largeWidth - padding * 2);
                    header.infoLayout.requestLayout();
                }
            } else if (holder instanceof SequenceHolder) {
                SequenceHolder sequenceHolder = (SequenceHolder) holder;
                final Sequence sequence = mSequenceList.get(Math.min(position - (showHeader ? 1 : 0), mSequenceList.size() - 1));

                Glide.with(activity).load(sequence.getThumbLink()).centerCrop().diskCacheStrategy(DiskCacheStrategy.RESULT).skipMemoryCache(false)
                        //                        .placeholder(R.drawable.custom_image_loading_background)
                        .signature(new StringSignature(
                                sequence.getThumbLink() + " " + sequence.getLocation().getLatitude() + ", " + sequence.getLocation().getLongitude()))
                        .error(R.drawable.vector_picture_placeholder).listener(MainActivity.mGlideRequestListener).into(sequenceHolder.imageItem);

                if (sequence.getAddress().equals("")) {
                    reverseGeocodeAddress(sequence, true);
                } else {
                    sequenceHolder.addressTextItem.setText(
                            sequence.getAddress().equals("") ? sequence.getLocation().getLatitude() + ", " + sequence.getLocation().getLongitude() :
                                    sequence.getAddress());
                }
                sequenceHolder.totalImagesTextItem.setText("" + sequence.getOriginalFrameCount());
                sequenceHolder.totalImagesTextItem
                        .setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.vector_camera_gray), null, null, null);
                sequenceHolder.dateTimeTextItem.setText(Utils.numericCardDateFormat.format(sequence.getDate()));
                if (sequence instanceof UserOnlineSequence) {
                    if (showHeader && showPoints) {
                        String first;
                        if (sequence.getScore() > 10000) {
                            first = sequence.getScore() / 1000 + "K\n";
                        } else {
                            first = sequence.getScore() + "\n";
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
                } else if (sequence instanceof DriverOnlineSequence) {
                    if (sequence.getCurrency() != null && !sequence.getCurrency().equals("") &&
                            !sequence.getServerStatus().equals(UserDataManager.SERVER_STATUS_REJECTED)) {

                        sequenceHolder.pointsBackground.setVisibility(View.VISIBLE);
                        sequenceHolder.pointsTextItem.setVisibility(View.VISIBLE);

                        String first = Utils.formatMoneyConstrained(sequence.getValue()) + "\n";
                        String second = sequence.getCurrency();
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
                } else {
                    sequenceHolder.pointsBackground.setVisibility(View.GONE);
                    sequenceHolder.pointsTextItem.setVisibility(View.GONE);
                }
                if (sequence.getTotalLength() > 0) {
                    sequenceHolder.totalLengthTextItem.setVisibility(View.VISIBLE);
                    String[] distance = Utils.formatDistanceFromMeters(activity, sequence.getDistance());
                    sequenceHolder.totalLengthTextItem.setText(distance[0] + distance[1]);
                    sequenceHolder.totalLengthTextItem
                            .setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.vector_distance_gray), null, null, null);
                }
                switch (sequence.getServerStatus()) {
                    default:
                    case UserDataManager.SERVER_STATUS_PROCESSED:
                        sequenceHolder.statusTextItem.setVisibility(View.GONE);
                        sequenceHolder.statusTextItem.setText("PROCESSED");
                        break;
                    case UserDataManager.SERVER_STATUS_UPLOADING:
                        sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                        sequenceHolder.statusTextItem.setText("UPLOADING");
                        sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.sequence_card_status_text_color_green));
                        break;
                    case UserDataManager.SERVER_STATUS_UPLOADED:
                        sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                        sequenceHolder.statusTextItem.setText("PROCESSING");
                        sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.sequence_card_status_text_color_blue));
                        break;
                    case UserDataManager.SERVER_STATUS_APPROVED:
                        sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                        sequenceHolder.statusTextItem.setText("ACCEPTED");
                        sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.sequence_card_status_text_color_green));
                        break;
                    case UserDataManager.SERVER_STATUS_REJECTED:
                        sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                        sequenceHolder.statusTextItem.setText("REJECTED");
                        sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.sequence_card_status_text_color_red));
                        break;
                    case UserDataManager.SERVER_STATUS_TBD:
                        sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                        sequenceHolder.statusTextItem.setText("IN REVIEW");
                        sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.sequence_card_status_text_color_blue));
                        break;
                }

                sequenceHolder.container.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        activity.openScreen(ScreenComposer.SCREEN_PREVIEW, sequence);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mAnimate) {
            setAnimation(holder.itemView, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position) && showHeader) {
            return isDriverHeader ? TYPE_HEADER_DRIVER : TYPE_HEADER_USER;
        }
        if (mInternetAvailable) {
            return TYPE_ITEM;
        } else {
            return TYPE_ITEM_NO_INTERNET;
        }
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
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        ((ViewHolder) holder).clearAnimation();
    }

    public void setOnline(boolean online) {
        mInternetAvailable = online;
    }

    public void refreshDetails(String[] acceptedDistance, String[] rejectedDistance, String[] obdDistance, String totalPhotos,
                               String totalTracks) {
        Log.d(TAG, "refreshDetails: ");
        this.mAcceptedDistance = acceptedDistance;
        this.mRejectedDistance = rejectedDistance;
        this.mObdDistance = obdDistance;
        this.mTotalPhotos = totalPhotos;
        this.mTotalTracks = totalTracks;
        notifyItemChanged(0);
    }

    public void enableDriverStats(boolean enable) {
        this.isDriverHeader = enable;
    }

    public void resetLastAnimatedItem() {
        lastPosition = 0;
    }

    public void enableAnimation(boolean enable) {
        mAnimate = enable;
        resetLastAnimatedItem();
    }

    public void enablePoints(boolean enable) {
        this.showPoints = enable;
    }

    public void makeHeaderScrollable(boolean scrollable) {
        mIsHeaderScrollable = scrollable;
        notifyItemChanged(0);
    }

    private void reverseGeocodeAddress(final Sequence sequence, boolean retry) {
        if (sequence.getLocation().getLatitude() == 0 || sequence.getLocation().getLongitude() == 0) {
            return;
        }
        if (SKReverseGeocoderManager.getInstance() != null) {
            SKSearchResult addr = SKReverseGeocoderManager.getInstance()
                    .reverseGeocodePosition(new SKCoordinate(sequence.getLocation().getLongitude(), sequence.getLocation().getLatitude()));
            if (addr != null) {
                String address = "" + addr.getName();
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
                sequence.setAddress(address);
            }
            if (sequence.getAddress().equals("") && retry) {
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

    private SpannableString getSpannableForStats(String first, String second) {
        SpannableString styledString = new SpannableString(first + second);
        styledString.setSpan(new TextAppearanceSpan(activity, R.style.profileHeaderInfoTextSmall), 0, first.length(), 0);
        styledString
                .setSpan(new TextAppearanceSpan(activity, R.style.profileHeaderInfoTextLarge), first.length(), second.length() + first.length(), 0);
        return styledString;
    }

    /**
     * method for the item animation
     */
    private void setAnimation(View viewToAnimate, int position) {
        if (position != 0) {//gridlayout instantiates 2 items at the same time which breaks the animation
            int last = lastPosition;
            lastPosition = position;
            if (Math.abs(last - position) > 1) {
                return;
            }
            Animation animation = AnimationUtils.loadAnimation(activity, (position >= last) ? R.anim.item_slide_up : R.anim.item_slide_down);
            viewToAnimate.startAnimation(animation);
        }
    }

    private class SequenceHolder extends ViewHolder {

        ImageView pointsBackground;

        TextView pointsTextItem;

        View container;

        ImageView imageItem;

        TextView addressTextItem;

        TextView statusTextItem;

        TextView totalImagesTextItem;

        TextView dateTimeTextItem;

        TextView totalLengthTextItem;

        SequenceHolder(View v) {
            super(v);
            container = v;
            statusTextItem = v.findViewById(R.id.sequence_status_text);
            addressTextItem = v.findViewById(R.id.sequence_address_label);
            imageItem = v.findViewById(R.id.sequence_image);
            totalImagesTextItem = v.findViewById(R.id.total_images_textView);
            dateTimeTextItem = v.findViewById(R.id.sequence_datetime_label);
            totalLengthTextItem = v.findViewById(R.id.total_length_label);
            pointsTextItem = v.findViewById(R.id.points_text);
            pointsBackground = v.findViewById(R.id.points_background);
        }
    }

    private class MessageCardHolder extends ViewHolder {

        View container;

        MessageCardHolder(View v) {
            super(v);
            container = v;
        }
    }

    private class UserHeaderViewHolder extends ViewHolder {

        private final LinearLayout infoHolder;

        private TextView imagesNumberText;

        private TextView distanceText;

        private TextView obdDistanceText;

        private TextView uploadedTracksText;

        UserHeaderViewHolder(View itemView) {
            super(itemView);
            infoHolder = itemView.findViewById(R.id.user_info_row);
            imagesNumberText = itemView.findViewById(R.id.header_images_text_view);
            distanceText = itemView.findViewById(R.id.header_distance_text_view);
            obdDistanceText = itemView.findViewById(R.id.header_obd_text_view);
            uploadedTracksText = itemView.findViewById(R.id.header_uploaded_tracks_text_view);
        }
    }

    private class DriverHeaderViewHolder extends ViewHolder {

        private final View separator;

        private HorizontalScrollViewImpl scrollView;

        private DotIndicator dotindicator;

        private LinearLayout infoLayout;

        private int scrollViewWidth;

        private TextView imagesNumberText;

        private TextView acceptedDistanceText;

        private TextView rejectedDistanceText;

        private TextView obdDistanceText;

        private TextView uploadedTracksText;

        DriverHeaderViewHolder(View itemView) {
            super(itemView);
            imagesNumberText = itemView.findViewById(R.id.header_images_text_view);
            acceptedDistanceText = itemView.findViewById(R.id.header_accepted_distance_text_view);
            rejectedDistanceText = itemView.findViewById(R.id.header_rejected_distance_text_view);
            obdDistanceText = itemView.findViewById(R.id.header_obd_text_view);
            uploadedTracksText = itemView.findViewById(R.id.header_uploaded_tracks_text_view);
            scrollView = itemView.findViewById(R.id.info_horizontal_scrollview);
            infoLayout = itemView.findViewById(R.id.header_info_layout);
            dotindicator = itemView.findViewById(R.id.info_dot_indicator);
            separator = itemView.findViewById(R.id.info_separator);
            scrollViewWidth = 0;
            scrollView.setScrollViewListener(new HorizontalScrollViewImpl.ScrollViewListener() {

                @Override
                public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
                    if (clampedX) {
                        if (scrollViewWidth == 0) {
                            scrollViewWidth = scrollView.getWidth();
                        }
                        if (scrollX == 0) {
                            dotindicator.setSelectedItem(0, true);
                        } else if (scrollX > scrollViewWidth / 4) {
                            dotindicator.setSelectedItem(1, true);
                        }
                    }
                }
            });
        }
    }

    private abstract class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }

        void clearAnimation() {
            itemView.clearAnimation();
        }
    }
}