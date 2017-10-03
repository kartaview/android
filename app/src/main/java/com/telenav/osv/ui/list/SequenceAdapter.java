package com.telenav.osv.ui.list;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.telenav.osv.R;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.databinding.ItemSequenceCardBinding;
import com.telenav.osv.databinding.PartialTrackListHeaderDriverBinding;
import com.telenav.osv.databinding.PartialTrackListHeaderUserBinding;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.view.tracklist.StatsData;
import com.telenav.osv.item.view.tracklist.TrackDataFactory;
import com.telenav.osv.ui.Navigator;
import com.telenav.osv.ui.binding.viewmodel.DefaultBindingComponent;
import com.telenav.osv.ui.binding.viewmodel.profile.TracksHeaderViewModel;
import com.telenav.osv.ui.binding.viewmodel.profile.TracksItemViewModel;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.util.List;

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

  private final ValueFormatter valueFormatter;

  private List<Sequence> mSequenceList;

  private Context context;

  private boolean mInternetAvailable;

  private int lastPosition = 0;

  private boolean isDriverHeader;

  private StatsData stats;

  private boolean mIsHeaderScrollable;

  private boolean mAnimate = false;

  private boolean showValue;

  private Navigator navigator;

  public SequenceAdapter(List<Sequence> results, Context context, Navigator navigator, ValueFormatter valueFormatter, boolean showHeader) {
    mSequenceList = results;
    this.context = context;
    this.navigator = navigator;
    this.valueFormatter = valueFormatter;
    this.showHeader = showHeader;
    mInternetAvailable = Utils.isInternetAvailable(this.context);
  }

  public void setOnline(boolean online) {
    mInternetAvailable = online;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    Resources resources = context.getResources();
    if (viewType == TYPE_HEADER_USER) {
      PartialTrackListHeaderUserBinding binding = DataBindingUtil
          .inflate(LayoutInflater.from(parent.getContext()), R.layout.partial_track_list_header_user, parent, false,
                   new DefaultBindingComponent());

      binding.setViewModel(new TracksHeaderViewModel(context, valueFormatter, showHeader));
      return new UserHeaderViewHolder(binding);
    } else if (viewType == TYPE_HEADER_DRIVER) {

      PartialTrackListHeaderDriverBinding binding = DataBindingUtil
          .inflate(LayoutInflater.from(parent.getContext()), R.layout.partial_track_list_header_driver, parent, false,
                   new DefaultBindingComponent());

      binding.setViewModel(new TracksHeaderViewModel(context, valueFormatter, showHeader));
      mIsHeaderScrollable = resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
      return new DriverHeaderViewHolder(binding);
    } else if (viewType == TYPE_ITEM) {

      ItemSequenceCardBinding binding =
          DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_sequence_card, parent, false,
                                  new DefaultBindingComponent());
      binding.setViewModel(new TracksItemViewModel(navigator, showHeader && showValue));
      return new SequenceHolder(binding);
    } else if (viewType == TYPE_ITEM_NO_INTERNET) {
      CardView layoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_no_internet_card, parent, false);
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
        header.binding.getViewModel().setStats(stats);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) header.binding.userInfoRow.getLayoutParams();
        lp.width = resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? LinearLayout.LayoutParams.MATCH_PARENT :
            (int) Utils.dpToPx(context, resources.getConfiguration().smallestScreenWidthDp);
        lp.height = LinearLayout.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        header.binding.userInfoRow.setLayoutParams(lp);
      } else if (holder instanceof DriverHeaderViewHolder) {

        DriverHeaderViewHolder header = (DriverHeaderViewHolder) holder;
        header.binding.getViewModel().setStats(stats);
        boolean portrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (mIsHeaderScrollable) {
          header.binding.infoDotIndicator.setVisibility(View.VISIBLE);
          header.binding.infoSeparator.setVisibility(View.VISIBLE);
          FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) header.binding.headerInfoLayout.getLayoutParams();
          lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
          lp.gravity = Gravity.START;
          int width = (int) Utils.dpToPx(context, context.getResources().getConfiguration().smallestScreenWidthDp);
          width = (int) ((width - context.getResources()
              .getDimension(portrait ? R.dimen.sequence_list_padding_side_portrait : R.dimen.sequence_list_padding_side_landscape) * 2) /
                             3 * 4);
          header.binding.headerInfoLayout.setLayoutParams(lp);
          header.binding.headerInfoLayout.setMinimumWidth(width);
          header.binding.headerInfoLayout.requestLayout();
        } else {
          header.binding.infoDotIndicator.setVisibility(View.GONE);
          header.binding.infoSeparator.setVisibility(View.GONE);
          FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) header.binding.headerInfoLayout.getLayoutParams();
          int largeWidth = (int) Utils.dpToPx(context, context.getResources().getConfiguration().screenWidthDp);
          int padding = (int) context.getResources()
              .getDimension(portrait ? R.dimen.sequence_list_padding_side_portrait : R.dimen.sequence_list_padding_side_landscape);
          lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
          lp.gravity = Gravity.CENTER_HORIZONTAL;
          header.binding.headerInfoLayout.setLayoutParams(lp);
          header.binding.headerInfoLayout.setMinimumWidth(largeWidth - padding * 2);
          header.binding.headerInfoLayout.requestLayout();
        }
      } else if (holder instanceof SequenceHolder) {
        SequenceHolder sequenceHolder = (SequenceHolder) holder;
        final Sequence sequence = mSequenceList.get(Math.min(position - (showHeader ? 1 : 0), mSequenceList.size() - 1));
        sequenceHolder.binding.getViewModel().setTrackData(TrackDataFactory.create(sequence, valueFormatter));
      }
    } catch (Exception e) {
      Log.d(TAG, Log.getStackTraceString(e));
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

  private boolean isPositionHeader(int position) {
    return position == 0;
  }

  public void refreshDetails(StatsData trackCollectionStats) {
    Log.d(TAG, "refreshDetails: " + trackCollectionStats);
    this.stats = trackCollectionStats;
    notifyItemChanged(0);
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

  public void showValue(boolean show) {
    this.showValue = show;
  }

  public void makeHeaderScrollable(boolean scrollable) {
    mIsHeaderScrollable = scrollable;
    notifyItemChanged(0);
  }

  private class SequenceHolder extends ViewHolder {

    private final ItemSequenceCardBinding binding;

    SequenceHolder(ItemSequenceCardBinding v) {
      super(v.cardView);
      this.binding = v;
    }
  }

  private class MessageCardHolder extends ViewHolder {

    MessageCardHolder(View v) {
      super(v);
    }
  }

  private class UserHeaderViewHolder extends ViewHolder {

    private final PartialTrackListHeaderUserBinding binding;

    UserHeaderViewHolder(PartialTrackListHeaderUserBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  private class DriverHeaderViewHolder extends ViewHolder {

    private final PartialTrackListHeaderDriverBinding binding;

    private int scrollViewWidth;

    DriverHeaderViewHolder(PartialTrackListHeaderDriverBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      scrollViewWidth = 0;
      binding.infoHorizontalScrollview.setScrollViewListener((scrollX, scrollY, clampedX, clampedY) -> {
        if (clampedX) {
          if (scrollViewWidth == 0) {
            scrollViewWidth = binding.infoHorizontalScrollview.getWidth();
          }
          if (scrollX == 0) {
            binding.infoDotIndicator.setSelectedItem(0, true);
          } else if (scrollX > scrollViewWidth / 4) {
            binding.infoDotIndicator.setSelectedItem(1, true);
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