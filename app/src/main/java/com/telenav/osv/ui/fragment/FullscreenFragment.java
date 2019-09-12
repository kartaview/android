package com.telenav.osv.ui.fragment;

import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import androidx.annotation.Nullable;

/**
 * fullscreen preview fragment
 * Created by kalmanb on 8/9/17.
 */
public class FullscreenFragment extends DisplayFragment {

    public static final String TAG = "FullscreenFragment";

    private PlaybackManager playbackManager;

    private FrameLayout mFrameHolder;

    private OSVActivity activity;

    //    private PhotoView mImageView;

    private Sequence mSequence;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.fullscreentransition));
            setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.fullscreentransition));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fullscreen_preview, null);
        activity = (MainActivity) getActivity();
        this.playbackManager = PlaybackManager.get(activity, mSequence,
                Injection.provideFrameLocalDataSource(getContext().getApplicationContext()),
                Injection.provideVideoDataSource(getContext().getApplicationContext()));
        //        mImageView = (PhotoView) inflater.inflate(R.layout.item_image_view_pager, null);
        mFrameHolder = view.findViewById(R.id.image_holder);
        //        mFrameHolder.addView(mImageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams
        // .MATCH_PARENT));
        //        Bitmap bitmap = ((GlideBitmapDrawable)((ImageView)((ScrollDisabledViewPager)playbackManager.getSurface()).getChildAt(0))
        // .getDrawable()).getBitmap();
        //        mImageView.setImageBitmap(bitmap);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //        mImageView = (PhotoView) activity.getLayoutInflater().inflate(R.layout.item_image_view_pager, null);
        //        mFrameHolder.addView(mImageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams
        // .MATCH_PARENT));
        //        Bitmap bitmap = ((GlideBitmapDrawable)((ImageView)((ScrollDisabledViewPager)playbackManager.getSurface()).getChildAt(0))
        // .getDrawable()).getBitmap();
        //        mImageView.setImageBitmap(bitmap);
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        //            setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition
        // .fullscreentransition));
        //            setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition
        // .fullscreentransition));
        //        }

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
    public void setSource(Object extra) {
        this.mSequence = (Sequence) extra;
    }
}
