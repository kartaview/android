package com.telenav.osv.ui.fragment;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.telenav.osv.R;
import com.telenav.osv.item.network.TrackCollection;

/**
 * Fragment displaying nearby recordings
 * Created by adrianbostan on 11/07/16.
 */
public class NearbyFragment extends SimpleProfileFragment {

    public final static String TAG = "NearbyFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        appBar.setActivated(false);
        //you will need to hide also all content inside CollapsingToolbarLayout
        //plus you will need to hide title of it
        mProfileImage.setVisibility(View.GONE);
        collapsingToolbar.setTitleEnabled(false);

        AppBarLayout.LayoutParams p = (AppBarLayout.LayoutParams) collapsingToolbar.getLayoutParams();
        p.setScrollFlags(0);
        collapsingToolbar.setLayoutParams(p);
        collapsingToolbar.setActivated(false);

        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();
        lp.height = getResources().getDimensionPixelSize(R.dimen.action_bar_size);
        appBar.requestLayout();
        mSwipeRefreshLayout.setEnabled(false);
        toolbar.setTitle("Nearby");
        return view;
    }

    @Override
    protected void loadMoreResults() {
    }

    @Override
    protected void requestDetails() {
    }

    @Override
    protected void displayCachedStats() {

    }

    @Override
    protected void refreshContent() {
    }

    @Override
    public void setSource(Object collection) {
        mOnlineSequences.clear();
        mOnlineSequences.addAll(((TrackCollection)collection).getTrackList());
        mLoading = false;

        mHandler.post(new Runnable() {
            public void run() {
                //change adapter contents
                if (mOnlineSequencesAdapter != null) {
                    mOnlineSequencesAdapter.notifyDataSetChanged();
                    stopRefreshing();
                }
            }
        });

    }


}

