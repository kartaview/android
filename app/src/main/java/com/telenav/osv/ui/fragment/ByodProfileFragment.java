package com.telenav.osv.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.network.DriverData;
import com.telenav.osv.item.network.PaymentCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.ui.list.PaymentAdapter;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import org.greenrobot.eventbus.Subscribe;

/**
 * fragment holding the ui for the user's data
 * Created by kalmanbencze on 7/07/17.
 */
public class ByodProfileFragment extends ProfileFragment implements TabLayout.OnTabSelectedListener {

  public final static String TAG = "ByodProfileFragment";

  private static final int DATA_SOURCE_TRACKS = 0;

  private static final int DATA_SOURCE_PAYMENTS = 1;

  private View mInfoView;

  private TextView mDistanceTextView;

  private TextView mRateTextView;

  private TextView mValueTextView;

  private int mCurrentDataSource = DATA_SOURCE_TRACKS;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (view != null) {
      collapsingToolbar
          .setExpandedTitleMarginBottom((int) activity.getResources().getDimension(R.dimen.profile_driver_header_title_margin_bottom));
      mTabLayout.addOnTabSelectedListener(this);
      mInfoView = view.findViewById(R.id.info_view);
      mDistanceTextView = view.findViewById(R.id.profile_info_distance);
      mRateTextView = view.findViewById(R.id.profile_info_rate);
      mValueTextView = view.findViewById(R.id.profile_info_value);
      mInfoView.setVisibility(View.VISIBLE);
      mTabLayout.setVisibility(View.VISIBLE);
      TabLayout.Tab mTracksTab = mTabLayout.getTabAt(0);
      TabLayout.Tab mPaymentsTab = mTabLayout.getTabAt(1);
      if (mCurrentDataSource == DATA_SOURCE_TRACKS) {
        if (mTracksTab != null) {
          mTracksTab.select();
        }
      } else {
        if (mPaymentsTab != null) {
          mPaymentsTab.select();
        }
      }
      mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
          mMaxNumberOfResults = 0;
          mCurrentDataSource = tab.getPosition();
          boolean portrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
          mOnlineSequencesAdapter.makeHeaderScrollable(portrait);
          mSequencesRecyclerView.setLayoutManager(portrait ? mPortraitLayoutManager :
                                                      (mCurrentDataSource == DATA_SOURCE_TRACKS ? mLandscapeLayoutManager :
                                                           mPortraitLayoutManager));
          mSequencesRecyclerView.setAdapter(mCurrentDataSource == DATA_SOURCE_TRACKS ? mOnlineSequencesAdapter : mPaymentAdapter);
          if ((mCurrentDataSource == DATA_SOURCE_PAYMENTS && mPaymentCurrentPageToList == 1) ||
              (mCurrentDataSource == DATA_SOURCE_TRACKS && mCurrentPageToList == 1)) {
            refreshContent();
          }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
      });
      mPaymentAdapter = new PaymentAdapter(mPaymentsList, activity);
    }
    mOnlineSequencesAdapter.enableDriverStats(true);
    displayCachedStats();
    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mSequencesRecyclerView.setAdapter(mCurrentDataSource == DATA_SOURCE_TRACKS ? mOnlineSequencesAdapter : mPaymentAdapter);
  }

  @Override
  protected void setupViews(boolean portrait) {
    super.setupViews(portrait);
    mOnlineSequencesAdapter.makeHeaderScrollable(portrait);
  }

  @Override
  protected RecyclerView.LayoutManager getLayoutManager(boolean portrait) {
    return portrait ? mPortraitLayoutManager :
        (mCurrentDataSource == DATA_SOURCE_PAYMENTS ? mPortraitLayoutManager : mLandscapeLayoutManager);
  }

  @Override
  protected void requestDetails() {
    activity.getUserDataManager().getDriverProfileDetails(new NetworkResponseDataListener<DriverData>() {

      @Override
      public void requestFailed(int status, DriverData details) {
        activity.showSnackBar("No Internet connection detected.", Snackbar.LENGTH_LONG);
        Log.d(TAG, "requestDriverDetails: " + details);
      }

      @Override
      public void requestFinished(int status, final DriverData driverData) {
        Log.d(TAG, "requestDriverDetails: " + " status - > " + status + " result - > " + driverData);
        if (driverData != null) {
          mHandler.post(() -> {
            String name = driverData.getDisplayName();
            String username = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
            String photoUrl = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
            String currency = driverData.getCurrency();
            final String[] acceptedDistance = Utils.formatDistanceFromKiloMeters(activity, driverData.getTotalAcceptedDistance());
            final String[] rejectedDistance = Utils.formatDistanceFromKiloMeters(activity, driverData.getTotalRejectedDistance());
            final String[] obdDistance = Utils.formatDistanceFromKiloMeters(activity, driverData.getTotalObdDistance());
            String totalPhotos = Utils.formatNumber(driverData.getTotalPhotos());
            String totalTracks = Utils.formatNumber(driverData.getTotalTracks());
            final String[] currentAccepted = Utils.formatDistanceFromKiloMeters(activity, driverData.getCurrentAcceptedDistance());
            String value = currency + Utils.formatMoney(driverData.getCurrentPaymentValue());
            String rate = Utils.formatMoney(driverData.getCurrentPayRate()) + " " + currency + "/km";
            fillUserInformation(name, username, photoUrl, currentAccepted, rate, value, acceptedDistance, rejectedDistance, obdDistance,
                                totalPhotos, totalTracks, driverData.getTotalPaidValue(), currency);
          });
        }
      }
    });
  }

  protected void displayCachedStats() {

    final String name = appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME);
    final String username = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
    final String photoUrl = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
    SharedPreferences prefs = activity.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
    final String currency = prefs.getString(ProfileFragment.K_DRIVER_CURRENCY, "$");
    final String[] currentAccepted =
        Utils.formatDistanceFromKiloMeters(activity, prefs.getFloat(ProfileFragment.K_DRIVER_CURRENT_ACCEPTED_DISTANCE, 0));
    final String rate = Utils.formatMoney(prefs.getFloat(ProfileFragment.K_DRIVER_CURRENT_PAYRATE, 0)) + " " + currency + "/km";
    final String value = currency + Utils.formatMoney(prefs.getFloat(ProfileFragment.K_DRIVER_CURRENT_VALUE, 0));
    final double totalValue = prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_VALUE, 0);
    final String[] accepted =
        Utils.formatDistanceFromKiloMeters(activity, prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_ACCEPTED_DISTANCE, 0));
    final String[] rejected =
        Utils.formatDistanceFromKiloMeters(activity, prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_REJECTED_DISTANCE, 0));
    final String[] obdDistance =
        Utils.formatDistanceFromKiloMeters(activity, prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_OBD_DISTANCE, 0));
    final String tracks = Utils.formatNumber(prefs.getFloat(ProfileFragment.K_DRIVER_TRACKS_COUNT, 0));
    final String photos = Utils.formatNumber(prefs.getFloat(ProfileFragment.K_DRIVER_PHOTOS_COUNT, 0));

    mHandler.post(
        () -> fillUserInformation(name, username, photoUrl, currentAccepted, rate, value, accepted, rejected, obdDistance, photos, tracks,
                                  totalValue, currency));
  }

  protected void loadMoreResults() {
    Log.d(TAG, "loadMoreResults: ");
    BackgroundThreadPool.post(() -> {
      if (mCurrentDataSource == DATA_SOURCE_PAYMENTS) {
        activity.getUserDataManager().listDriverPayments(new NetworkResponseDataListener<PaymentCollection>() {

          @Override
          public void requestFailed(int status, PaymentCollection details) {
            mHandler.post(() -> {
              if (mPaymentsList.isEmpty()) {
                mPaymentAdapter.setOnline(Utils.isInternetAvailable(activity));
              }
              mPaymentAdapter.notifyDataSetChanged();
              stopRefreshing();
            });
          }

          @Override
          public void requestFinished(int status, final PaymentCollection collectionData) {
            BackgroundThreadPool.post(() -> {
              if (collectionData != null) {
                try {
                  mPaymentCurrentPageToList++;
                  mPaymentMaxNumberOfResults = collectionData.getTotalFilteredItems();
                  mPaymentsList.addAll(collectionData.getPaymentList());
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              mLoading = false;

              mHandler.post(() -> {
                //change adapter contents
                if (mPaymentAdapter != null && collectionData != null) {
                  mPaymentAdapter.setOnline(Utils.isInternetAvailable(activity));
                  mPaymentAdapter.notifyDataSetChanged();
                }
              });
              stopRefreshing();
            });
          }
        }, mPaymentCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE);
      } else if (mCurrentDataSource == DATA_SOURCE_TRACKS) {
        activity.getUserDataManager().listDriverSequences(new NetworkResponseDataListener<TrackCollection>() {

          @Override
          public void requestFailed(int status, TrackCollection details) {
            mHandler.post(() -> {
              //                                mCurrentPageToList--;
              if (mOnlineSequences.isEmpty()) {
                mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
              }
              mOnlineSequencesAdapter.notifyDataSetChanged();
              stopRefreshing();
            });
          }

          @Override
          public void requestFinished(int status, final TrackCollection collectionData) {
            BackgroundThreadPool.post(() -> {
              if (collectionData != null) {
                try {
                  mCurrentPageToList++;
                  mMaxNumberOfResults = collectionData.getTotalFilteredItems();
                  mOnlineSequences.addAll(collectionData.getTrackList());
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              mLoading = false;

              mHandler.post(() -> {
                //change adapter contents
                if (mOnlineSequencesAdapter != null) {
                  mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                  mOnlineSequencesAdapter.notifyDataSetChanged();
                }
              });
              stopRefreshing();
            });
          }
        }, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE);
      }
    });
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

  private void fillUserInformation(String name, String username, String photoUrl, String[] currentAccepted, String rate, String value,
                                   String[] acceptedDistance, String[] rejectedDistance, String[] obdDistance, String totalPhotos,
                                   String totalTracks, double paymentValue, String currency) {
    collapsingToolbar.setTitle(name);
    if (!"".equals(photoUrl)) {
      Glide.with(activity).load(photoUrl).centerCrop().dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
          .signature(new StringSignature("profile " + username + "-" + photoUrl)).priority(Priority.IMMEDIATE)
          .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
          .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
          .listener(MainActivity.mGlideRequestListener).into(mProfileImage);
    }
    mProfileImage.setInstantProgress(0.01f);
    mDistanceTextView.setText(currentAccepted[0] + " " + currentAccepted[1]);
    mValueTextView.setText(value);
    mRateTextView.setText(rate);
    //        appBar.setExpanded(true, true);
    mOnlineSequencesAdapter.refreshDetails(acceptedDistance, rejectedDistance, obdDistance, totalPhotos, totalTracks);
    mPaymentAdapter.setTotalPayment(paymentValue, currency);
  }

  @Subscribe
  public void onRefreshNeeded(SequencesChangedEvent event) {
    if (event.online) {
      refreshContent();
    }
  }

  @Override
  public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
    if (mMaxScrollSize == 0) {
      mMaxScrollSize = appBarLayout.getTotalScrollRange();
    }

    if (mMaxScrollSize == 0) {
      return;
    }
    int percentage = (Math.abs(i)) * 100 / mMaxScrollSize;

    if (percentage >= PERCENTAGE_TO_ANIMATE_AVATAR && mIsAvatarShown) {
      mIsAvatarShown = false;

      mProfileImage.animate().scaleY(0).scaleX(0).alpha(0.0f)
          //                    .translationYBy(-200)
          .setDuration(200).start();
      mInfoView.animate().alpha(0.0f)
          //                    .translationYBy(-200)
          .setDuration(200).start();
    }

    if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
      mIsAvatarShown = true;

      mProfileImage.animate().scaleY(1).scaleX(1).alpha(1.0f).setDuration(200)
          //                    .translationYBy(200)
          .start();
      mInfoView.animate().alpha(1.0f).setDuration(200)
          //                    .translationYBy(200)
          .start();
    }
  }

  @Override
  public void onTabSelected(TabLayout.Tab tab) {

  }

  @Override
  public void onTabUnselected(TabLayout.Tab tab) {

  }

  @Override
  public void onTabReselected(TabLayout.Tab tab) {

  }
}

