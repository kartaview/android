package com.telenav.osv.ui.list;

import java.util.List;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPoints;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * the adapter used for displaying the uploaded tracks from the server
 * Created by kalmanbencze on 7/07/17.
 */
public class SequenceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String ADRESS_FORMAT = "%s ,%s";

    private static final int TYPE_HEADER_USER = 0;

    private static final int TYPE_ITEM = 2;

    private static final int TYPE_ITEM_NO_INTERNET = 3;

    private static final String TAG = "SequenceAdapter";

    private static final String DEFAULT_VALUE = "0";

    private final boolean showHeader;

    private List<Sequence> mSequenceList;

    private Context context;

    private boolean mInternetAvailable;

    private int lastPosition = 0;

    private String[] mAcceptedDistance;

    private String[] mRejectedDistance;

    private String[] mObdDistance;

    private String mTotalPhotos = DEFAULT_VALUE;

    private String mTotalTracks = DEFAULT_VALUE;

    private boolean mIsHeaderScrollable;

    private boolean mAnimate = false;

    private boolean showPoints;

    private ApplicationPreferences applicationPreferences;

    /**
     * A flag representing if the track id should be visible for a sequence. The track id should be visible only in the "My profile" section.
     * The "Nearby" screen shouldn't contain the track id.
     */
    private boolean isTrackIdVisible = false;

    public SequenceAdapter(List<Sequence> results, Context context, boolean showHeader, ApplicationPreferences applicationPreferences) {
        mSequenceList = results;
        this.context = context;
        this.showHeader = showHeader;
        this.applicationPreferences = applicationPreferences;
        mInternetAvailable = Utils.isInternetAvailable(this.context);
        initDistances();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Resources resources = context.getResources();
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

            Resources resources = context.getResources();
            if (holder instanceof UserHeaderViewHolder) {
                UserHeaderViewHolder header = (UserHeaderViewHolder) holder;
                String imagesText = context.getString(R.string.account_images_label);
                String distanceText = context.getString(R.string.account_distance_label);
                String obdText = context.getString(R.string.account_obd_label);

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
                        (int) Utils.dpToPx(context, resources.getConfiguration().smallestScreenWidthDp);
                lp.height = LinearLayout.LayoutParams.MATCH_PARENT;
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                header.infoHolder.setLayoutParams(lp);
            } else if (holder instanceof HeaderViewHolder) {

                HeaderViewHolder header = (HeaderViewHolder) holder;
                String imagesText = context.getString(R.string.account_images_label);
                String obdText = context.getString(R.string.account_obd_label);

                if (header.imagesNumberText != null) {
                    header.imagesNumberText.setText(getSpannableForStats(imagesText, mTotalPhotos));
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
            } else if (holder instanceof SequenceHolder) {
                SequenceHolder sequenceHolder = (SequenceHolder) holder;
                final Sequence sequence = mSequenceList.get(Math.min(position - (showHeader ? 1 : 0), mSequenceList.size() - 1));
                SequenceDetails sequenceDetails = sequence.getDetails();
                SequenceDetailsCompressionBase compressionBase = sequence.getCompressionDetails();
                String thumbnailLink = compressionBase.getThumbnailLink();
                Location initialLocation = sequenceDetails.getInitialLocation();
                Glide.with(context).load(NetworkUtils.provideGlideUrlWithAuthorizationIfRequired(applicationPreferences, thumbnailLink)).centerCrop().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).skipMemoryCache(false)
                        .error(R.drawable.vector_picture_placeholder).listener(MainActivity.mGlideRequestListener).into(sequenceHolder.imageItem);
                if (isTrackIdVisible) {
                    sequenceHolder.idTextItem.setText(FormatUtils.formatSequenceId(sequence.getDetails().getOnlineId()));
                    sequenceHolder.idTextItem.setVisibility(View.VISIBLE);
                }
                String addressName = sequenceDetails.getAddressName();
                if (StringUtils.isEmpty(addressName)) {
                    addressName = String.format(ADRESS_FORMAT, initialLocation.getLatitude(), initialLocation.getLongitude());
                }
                sequenceHolder.addressTextItem.setText(addressName);
                sequenceHolder.totalImagesTextItem.setText(String.valueOf(compressionBase.getLocationsCount()));
                sequenceHolder.totalImagesTextItem
                        .setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.vector_camera_gray), null, null, null);
                sequenceHolder.dateTimeTextItem.setText(Utils.numericCardDateFormat.format(sequenceDetails.getDateTime().getMillis()));
                SequenceDetailsRewardBase rewardBase = sequence.getRewardDetails();
                String remoteStatus = sequenceDetails.getProcessingRemoteStatus();
                if (rewardBase != null) {
                    if (rewardBase instanceof SequenceDetailsRewardPoints) {
                        if (showHeader && showPoints) {
                            String first;
                            int pointsValue = (int) rewardBase.getValue();
                            ;
                            if (pointsValue > 10000) {
                                first = pointsValue / 1000 + "K\n";
                            } else {
                                first = pointsValue + "\n";
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
                    } else {
                        String unit = rewardBase.getUnit();
                        if (!StringUtils.isEmpty(unit)
                                && !StringUtils.isEmpty(remoteStatus)
                                && !remoteStatus.equals(UserDataManager.SERVER_STATUS_REJECTED)) {
                            sequenceHolder.pointsBackground.setVisibility(View.VISIBLE);
                            sequenceHolder.pointsTextItem.setVisibility(View.VISIBLE);
                            String first = FormatUtils.formatMoneyConstrained(rewardBase.getValue()) + "\n";
                            SpannableString styledString = new SpannableString(first + unit);
                            styledString.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(), 0);
                            styledString.setSpan(new StyleSpan(Typeface.NORMAL), first.length(), unit.length() + first.length(), 0);
                            styledString.setSpan(new AbsoluteSizeSpan(16, true), 0, first.length(), 0);
                            styledString.setSpan(new AbsoluteSizeSpan(12, true), first.length(), unit.length() + first.length(), 0);
                            sequenceHolder.pointsTextItem.setText(styledString);
                        } else {
                            sequenceHolder.pointsBackground.setVisibility(View.GONE);
                            sequenceHolder.pointsTextItem.setVisibility(View.GONE);
                        }
                    }
                } else {
                    sequenceHolder.pointsBackground.setVisibility(View.GONE);
                    sequenceHolder.pointsTextItem.setVisibility(View.GONE);
                }
                sequenceHolder.totalLengthTextItem.setVisibility(View.VISIBLE);
                String[] distance = FormatUtils.formatDistanceFromMeters(context, (int) sequenceDetails.getDistance(), FormatUtils.SEPARATOR_SPACE);
                sequenceHolder.totalLengthTextItem.setText(distance[0] + distance[1]);
                sequenceHolder.totalLengthTextItem
                        .setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.vector_distance_gray), null, null, null);

                if (StringUtils.isEmpty(remoteStatus)) {
                    sequenceHolder.statusTextItem.setVisibility(View.GONE);
                } else {
                    switch (remoteStatus) {
                        default:
                        case UserDataManager.SERVER_STATUS_PROCESSED:
                            sequenceHolder.statusTextItem.setVisibility(View.GONE);
                            sequenceHolder.statusTextItem.setText("PROCESSED");
                            break;
                        case UserDataManager.SERVER_STATUS_UPLOADING:
                            sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                            sequenceHolder.statusTextItem.setText("UPLOADING");
                            sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.default_green));
                            break;
                        case UserDataManager.SERVER_STATUS_UPLOADED:
                            sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                            sequenceHolder.statusTextItem.setText("PROCESSING");
                            sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.default_purple));
                            break;
                        case UserDataManager.SERVER_STATUS_APPROVED:
                            sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                            sequenceHolder.statusTextItem.setText("ACCEPTED");
                            sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.default_green));
                            break;
                        case UserDataManager.SERVER_STATUS_REJECTED:
                            sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                            sequenceHolder.statusTextItem.setText("REJECTED");
                            sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.default_red));
                            break;
                        case UserDataManager.SERVER_STATUS_TBD:
                            sequenceHolder.statusTextItem.setVisibility(View.VISIBLE);
                            sequenceHolder.statusTextItem.setText("IN REVIEW");
                            sequenceHolder.statusTextItem.setTextColor(resources.getColor(R.color.default_purple));
                            break;
                    }
                }

                sequenceHolder.container.setOnClickListener(v -> ((MainActivity) context).openScreen(ScreenComposer.SCREEN_PREVIEW, sequence));
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
            return TYPE_HEADER_USER;
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

    /**
     * Sets the track id visibility from the view. The track id should be visible only in the "My profile" section.
     * The "Nearby" view shouldn't contain the track id.
     * @param isVisible true, if the track is should be visible, false otherwise.
     */
    public void setTrackIdVisibility(boolean isVisible) {
        this.isTrackIdVisible = isVisible;
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

    /**
     * Initializes the values for the accepted uploaded distance, rejected uploaded distance and OBD distance.
     */
    private void initDistances() {
        mAcceptedDistance = new String[]{DEFAULT_VALUE, FormatUtils.getDistanceUnitLabel(context, FormatUtils.SEPARATOR_SPACE, false)};
        mRejectedDistance = new String[]{DEFAULT_VALUE, FormatUtils.getDistanceUnitLabel(context, FormatUtils.SEPARATOR_SPACE, false)};
        mObdDistance = new String[]{DEFAULT_VALUE, FormatUtils.getDistanceUnitLabel(context, FormatUtils.SEPARATOR_SPACE, false)};
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    private SpannableString getSpannableForStats(String first, String second) {
        SpannableString styledString = new SpannableString(first + second);
        styledString.setSpan(new TextAppearanceSpan(context, R.style.profileHeaderInfoTextSmall), 0, first.length(), 0);
        styledString
                .setSpan(new TextAppearanceSpan(context, R.style.profileHeaderInfoTextLarge), first.length(), second.length() + first.length(), 0);
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
            Animation animation = AnimationUtils.loadAnimation(context, (position >= last) ? R.anim.item_slide_up : R.anim.item_slide_down);
            viewToAnimate.startAnimation(animation);
        }
    }

    private class SequenceHolder extends ViewHolder {

        ImageView pointsBackground;

        TextView pointsTextItem;

        View container;

        ImageView imageItem;

        TextView addressTextItem;

        TextView idTextItem;

        TextView statusTextItem;

        TextView totalImagesTextItem;

        TextView dateTimeTextItem;

        TextView totalLengthTextItem;

        SequenceHolder(View v) {
            super(v);
            container = v;
            idTextItem = v.findViewById(R.id.sequence_id_label);
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

    private class HeaderViewHolder extends ViewHolder {

        private TextView imagesNumberText;

        private TextView obdDistanceText;

        private TextView uploadedTracksText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            imagesNumberText = itemView.findViewById(R.id.header_images_text_view);
            obdDistanceText = itemView.findViewById(R.id.header_obd_text_view);
            uploadedTracksText = itemView.findViewById(R.id.header_uploaded_tracks_text_view);
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