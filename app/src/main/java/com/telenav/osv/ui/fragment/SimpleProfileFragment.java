package com.telenav.osv.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.item.view.profile.UserProfileData;
import com.telenav.osv.item.view.tracklist.StatsData;
import com.telenav.osv.item.view.tracklist.StatsDataFactory;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;

/**
 * fragment holding the ui for the user's data
 * Created by kalmanbencze on 7/07/17.
 */
public class SimpleProfileFragment extends ProfileFragment {

  public static final String TAG = "SimpleProfileFragment";

  @Inject
  UserDataManager mUserDataManager;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (view != null) {
      collapsingToolbar
          .setExpandedTitleMarginBottom((int) activity.getResources().getDimension(R.dimen.profile_user_header_title_margin_bottom));
    }
    mOnlineSequencesAdapter.enableDriverStats(false);
    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mSequencesRecyclerView.setAdapter(mOnlineSequencesAdapter);
  }

  @Override
  protected RecyclerView.LayoutManager getLayoutManager(boolean portrait) {
    return portrait ? mPortraitLayoutManager : mLandscapeLayoutManager;
  }

  @Override
  protected void requestDetails() {
    mUserDataManager.getUserProfileDetails(new NetworkResponseDataListener<UserData>() {

      @Override
      public void requestFailed(int status, UserData details) {
        activity.showSnackBar(getString(R.string.warning_server_comunication_failiure), Snackbar.LENGTH_LONG);
        displayCachedStats();
        Log.d(TAG, "requestUserDetails: " + " status - > " + status + " details - > " + details);
      }

      @Override
      public void requestFinished(int status, final UserData userData) {
        Log.d(TAG, "requestUserDetails: " + " status - > " + status + " result - > " + userData);
        if (userData != null) {
          UserProfileData profileData = new UserProfileData();
          profileData.setName(userData.getDisplayName());
          profileData.setUsername(userData.getUserName());
          profileData.setPhotoUrl(appPrefs.getUserPhotoUrl());

          StatsData stats = StatsDataFactory.create(activity, valueFormatter
              , userData.getTotalDistance()
              , 0
              , userData.getTotalObdDistance()
              , (int) userData.getTotalTracks()
              , (int) userData.getTotalPhotos());
          fillUserInformation(profileData, stats);
        }
      }
    });
  }

  protected void displayCachedStats() {
    UserProfileData profileData = new UserProfileData();
    profileData.setName(appPrefs.getUserDisplayName());
    profileData.setUsername(appPrefs.getUserName());
    profileData.setPhotoUrl(appPrefs.getUserPhotoUrl());

    SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    StatsData stats = StatsDataFactory.create(activity, valueFormatter
        , prefs.getFloat(K_TOTAL_DISTANCE, 0.0f)
        , 0
        , prefs.getFloat(K_OBD_DISTANCE, 0.0f)
        , prefs.getInt(K_TOTAL_TRACKS, 0)
        , prefs.getInt(K_TOTAL_PHOTOS, 0));
    fillUserInformation(profileData, stats);

  }

  protected void loadMoreResults() {
    BackgroundThreadPool.post(() -> mUserDataManager.listSequences(new NetworkResponseDataListener<TrackCollection>() {

      @Override
      public void requestFailed(int status, TrackCollection details) {
        mHandler.post(() -> {
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
              Log.d(TAG, Log.getStackTraceString(e));
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
    }, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE));
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

  private void fillUserInformation(UserProfileData profileData, StatsData stats) {
    mHandler.post(() -> {
      collapsingToolbar.setTitle(profileData.getName());
      if (!"".equals(profileData.getPhotoUrl())) {
        Glide.with(activity).load(profileData.getPhotoUrl())
            .centerCrop()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .signature(new StringSignature("profile " + profileData.getUsername() + "-" + profileData.getPhotoUrl()))
            .priority(Priority.IMMEDIATE)
            .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
            .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
            .listener(MainActivity.mGlideRequestListener)
            .into(mProfileImage);
      }
      mProfileImage.setInstantProgress(0.01f);
      mOnlineSequencesAdapter.refreshDetails(stats);
    });
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

      mProfileImage.animate().scaleY(0).scaleX(0).alpha(0.0f).setDuration(200).start();
    }

    if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
      mIsAvatarShown = true;

      mProfileImage.animate().scaleY(1).scaleX(1).alpha(1.0f).setDuration(200).start();
    }
  }
}

