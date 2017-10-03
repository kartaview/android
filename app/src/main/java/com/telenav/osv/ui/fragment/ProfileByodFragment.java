package com.telenav.osv.ui.fragment;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.telenav.osv.R;
import com.telenav.osv.databinding.FragmentProfileByodBinding;
import com.telenav.osv.item.network.DriverData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.ui.binding.viewmodel.DefaultBindingComponent;
import com.telenav.osv.ui.binding.viewmodel.profile.newimpl.ProfileByodViewModel;
import javax.inject.Inject;

/**
 * todo only used in new profile fragment impl.
 * the new byod profile fragment
 * Created by kalmanb on 9/21/17.
 */
public class ProfileByodFragment extends OSVFragment implements AppBarLayout.OnOffsetChangedListener {

  public static final String TAG = "ProfileByodFragment";

  protected static final int PERCENTAGE_TO_ANIMATE_AVATAR = 5;

  protected boolean mIsAvatarShown = true;

  @Inject
  ViewModelProvider.Factory mViewModelFactory;

  @Inject
  UserDataManager userDataManager;

  private ProfileByodViewModel viewModel;

  private FragmentProfileByodBinding binding;

  private int mMaxScrollSize;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = ViewModelProviders.of(this, mViewModelFactory).get(ProfileByodViewModel.class);
    viewModel.setOwner(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding =
        DataBindingUtil.inflate(inflater, R.layout.fragment_profile_byod, null, false, new DefaultBindingComponent());
    binding.setViewModel(viewModel);
    binding.toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
    binding.profileAppbar.addOnOffsetChangedListener(this);
    userDataManager.getDriverProfileDetails(new NetworkResponseDataListener<DriverData>() {

      @Override
      public void requestFailed(int status, DriverData details) {

      }

      @Override
      public void requestFinished(int status, DriverData details) {

      }
    });
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onDestroyView() {
    binding.profileAppbar.addOnOffsetChangedListener(this);
    super.onDestroyView();
  }

  @Override
  public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

    if (mMaxScrollSize == 0) {
      mMaxScrollSize = appBarLayout.getTotalScrollRange();
    }

    if (mMaxScrollSize == 0) {
      return;
    }
    int percentage = (Math.abs(verticalOffset)) * 100 / mMaxScrollSize;

    if (percentage >= PERCENTAGE_TO_ANIMATE_AVATAR && mIsAvatarShown) {
      mIsAvatarShown = false;

      binding.profileHeader.profilePictureHolder.profileImage.animate().scaleY(0).scaleX(0).alpha(0.0f)
          //                    .translationYBy(-200)
          .setDuration(200).start();
      binding.profileHeader.infoView.animate().alpha(0.0f)
          //                    .translationYBy(-200)
          .setDuration(200).start();
    }

    if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
      mIsAvatarShown = true;

      binding.profileHeader.profilePictureHolder.profileImage.animate().scaleY(1).scaleX(1).alpha(1.0f).setDuration(200)
          //                    .translationYBy(200)
          .start();
      binding.profileHeader.infoView.animate().alpha(1.0f).setDuration(200)
          //                    .translationYBy(200)
          .start();
    }
  }
}
