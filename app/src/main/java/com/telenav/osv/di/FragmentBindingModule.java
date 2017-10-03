package com.telenav.osv.di;

import com.telenav.osv.di.viewmodel.ViewModelModule;
import com.telenav.osv.ui.fragment.BTDialogFragment;
import com.telenav.osv.ui.fragment.ByodProfileFragment;
import com.telenav.osv.ui.fragment.CameraControlsFragment;
import com.telenav.osv.ui.fragment.CameraPreviewFragment;
import com.telenav.osv.ui.fragment.FullscreenFragment;
import com.telenav.osv.ui.fragment.HintsFragment;
import com.telenav.osv.ui.fragment.IssueReportFragment;
import com.telenav.osv.ui.fragment.LeaderboardFragment;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.OBDDialogFragment;
import com.telenav.osv.ui.fragment.PictureSizeDialogFragment;
import com.telenav.osv.ui.fragment.ProfileByodFragment;
import com.telenav.osv.ui.fragment.RecordingSummaryFragment;
import com.telenav.osv.ui.fragment.SettingsFragment;
import com.telenav.osv.ui.fragment.SimpleProfileFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.ui.fragment.UploadProgressFragment;
import com.telenav.osv.ui.fragment.UserProfileFragment;
import com.telenav.osv.ui.fragment.WaitingFragment;
import com.telenav.osv.ui.fragment.WalkthroughSlideFragment;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Utility class for generating subcomponents for binding the fragments' fields
 * Created by kalmanb on 9/22/17.
 */
@Module
public abstract class FragmentBindingModule {

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract SettingsFragment contributeSettingsFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract HintsFragment contributeHintsFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract IssueReportFragment contributeIssueReportFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract LeaderboardFragment contributeLeaderboardFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract UploadProgressFragment contributeUploadProgressFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract WaitingFragment contributeWaitingFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract WalkthroughSlideFragment contributeWalkthroughSlideFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract CameraControlsFragment contributeCameraControlsFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract CameraPreviewFragment contributeCameraPreviewFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract MapFragment contributeMapFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract ProfileByodFragment contributeProfileByodFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract ByodProfileFragment contributeByodProfileFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract UserProfileFragment contributeUserProfileFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract SimpleProfileFragment contributeSimpleProfileFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract RecordingSummaryFragment contributeRecordingSummaryFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract TrackPreviewFragment contributeTrackPreviewFragment();

  @ContributesAndroidInjector(modules = ViewModelModule.class)
  abstract FullscreenFragment contributeFullscreenFragment();

  @ContributesAndroidInjector()
  abstract OBDDialogFragment contributeOdbDialogFragment();

  @ContributesAndroidInjector()
  abstract PictureSizeDialogFragment contributePictureSizeDialogFragment();

  @ContributesAndroidInjector()
  abstract BTDialogFragment contributeBTDialogFragment();
}
