package com.telenav.osv.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.di.PlaybackModule;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * fullscreen preview fragment
 * Created by kalmanb on 8/9/17.
 */
public class FullscreenFragment extends OSVFragment implements Displayable<Sequence> {

  public static final String TAG = "FullscreenFragment";

  @Inject
  @Named(PlaybackModule.SCOPE_JPEG_ONLINE)
  PlaybackManager playbackManager;

  private FrameLayout mFrameHolder;

  private OSVActivity activity;

  private Sequence mSequence;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.fullscreentransition));
    setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.fullscreentransition));
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_fullscreen_preview, null);
    activity = (MainActivity) getActivity();
    this.playbackManager.setSource(mSequence);
    mFrameHolder = view.findViewById(R.id.image_holder);
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    ScrollDisabledViewPager mPager = new ScrollDisabledViewPager(activity);
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    mPager.setLayoutParams(lp);
    mFrameHolder.removeAllViews();
    mFrameHolder.addView(mPager);
    playbackManager.setSurface(mPager);
    playbackManager.prepare();
    playbackManager.play();
  }

  @Override
  public void onPause() {
    if (playbackManager != null) {
      playbackManager.pause();
    }
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    if (playbackManager != null) {
      playbackManager.stop();
      playbackManager.destroy();
    }
    super.onDestroyView();
  }

  @Override
  public int getEnterAnimation() {
    return R.anim.alpha_add;
  }

  @Override
  public int getExitAnimation() {
    return R.anim.alpha_remove;
  }

  @Override
  public void setDisplayData(Sequence extra) {
    this.mSequence = extra;
  }
}
