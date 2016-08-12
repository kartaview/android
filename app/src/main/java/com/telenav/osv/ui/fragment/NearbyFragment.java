package com.telenav.osv.ui.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.ui.list.SequenceAdapter;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by adrianbostan on 11/07/16.
 */

public class NearbyFragment extends Fragment {

    public final static String TAG = "NearbyFragment";

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    private MainActivity activity;

    private RecyclerView mSequencesRecyclerView;

    private SequenceAdapter mOnlineSequencesAdapter;

    private UploadManager mUploadManager;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ArrayList<Sequence> mOnlineSequences = new ArrayList<>();

//    private int mCurrentPageToList = 1;

    private int mLastVisibleItem, mTotalItemCount;

    private boolean mLoading;

    private Timer mTimer = new Timer();

    private TimerTask mCancelTask = new TimerTask() {
        @Override
        public void run() {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private int mMaxNumberOfResults = 10000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, null);

        activity = (MainActivity) getActivity();
        mSequencesRecyclerView = (RecyclerView) view.findViewById(R.id.sequences_recycle_view);

        mUploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.profile_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });
        return view;
    }

    public void refreshContent() {
        Log.d(TAG, "refreshContent ");
        startRefreshing();
        new Thread(new Runnable() {
            @Override
            public void run() {
//                mCurrentPageToList = 1;
//                mOnlineSequences.clear();
                mOnlineSequencesAdapter.notifyDataSetChanged();
                loadMoreResults();
            }
        }).start();
    }

    public void startRefreshing() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setRefreshing(true);
                        if (mCancelTask != null) {
                            mCancelTask.cancel();
                        }
                        mCancelTask = new TimerTask() {
                            @Override
                            public void run() {
                                if (activity != null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mSwipeRefreshLayout != null) {
                                                if (activity.getCurrentFragment().equals(TAG)) {
                                                    activity.showSnackBar(R.string.loading_too_long, Snackbar.LENGTH_LONG);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        };
                        mTimer.schedule(mCancelTask, 10000);
                    }
                }
            });
        }
    }

    public void stopRefreshing() {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mSwipeRefreshLayout != null) {
                        if (mCancelTask != null) {
                            mCancelTask.cancel();
                        }
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mSequencesRecyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));

        } else {
            GridLayoutManager glm = new GridLayoutManager(getContext(), 2);
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return (position == 0) ? 2 : 1;
                }
            });
            mSequencesRecyclerView.setLayoutManager(glm);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOnlineSequencesAdapter = new SequenceAdapter(mOnlineSequences, activity, false);
        mSequencesRecyclerView.setAdapter(mOnlineSequencesAdapter);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mSequencesRecyclerView.setLayoutManager(layoutManager);

        } else {
            GridLayoutManager manager = new GridLayoutManager(activity, 3);
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return (3 - position % 3);
                }
            });
        }

        mSequencesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView,
                                   int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                mTotalItemCount = layoutManager.getItemCount() - 1;
                mLastVisibleItem = layoutManager.findLastVisibleItemPosition();

                if (!mLoading && mTotalItemCount == mLastVisibleItem && mTotalItemCount < mMaxNumberOfResults) {
                    // End has been reached
                    mSwipeRefreshLayout.setRefreshing(true);
                    loadMoreResults();
                    mLoading = true;
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    private void loadMoreResults() {
//        mUploadManager.listSequences(this, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE, null, null, true);
    }

    public void handleNearbyResult(String result) {
        if (result != null && !result.isEmpty()) {
            try {
                JSONObject obj = new JSONObject(result);
                JSONObject osv = obj.getJSONObject("osv");
                JSONArray array1 = osv.getJSONArray("sequences");
//                mCurrentPageToList++;
                if (array1.length() > 0) {
                    for (int i = 0; i < array1.length(); i++) {
                        JSONObject item = array1.getJSONObject(i);
                        int id = item.getInt("sequence_id");
                        String date = item.getString("date");
                        String hour = item.getString("hour");
                        SimpleDateFormat onlineDateFormat = new SimpleDateFormat("MM.dd.yyyy hh:mm a");
                        try {
                            date = Utils.numericDateFormat.format(onlineDateFormat.parse(date + " " + hour));
                        } catch (Exception e) {
                            Log.d(TAG, "handleSequenceListResult: " + e.getLocalizedMessage());
                        }
                        String imgNum = item.getString("photo_no");
                        String distance = item.getString("distance");
                        double lat = item.getDouble("lat");
                        double lon = item.getDouble("lng");
                        boolean obd = false;
                        String platform = "";
                        String platformVersion = "";
                        String appVersion = "";
                        String partialAddress = "";
                        try {
                            String address = item.getString("address");
                            String[] list = address.split(", ");
                            partialAddress = list[0] + ", " + list[2];
                        } catch (Exception e) {
//                            Log.d(TAG, "handleSequenceListResult: exception during adress parsing");
                        }
                        String thumbLink = UploadManager.URL_DOWNLOAD_PHOTO + item.getString("photo");
                        double distanceNum = 0;
                        try {
                            if (distance != null) {
                                distanceNum = Double.parseDouble(distance);
                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "handleSequenceListResult: couldn't parse distance");
                        }
                        Sequence seq = new Sequence(id, date, Integer.valueOf(imgNum), partialAddress, thumbLink, obd, platform, platformVersion, appVersion, (int) (distanceNum
                                * 1000d));
//                        seq.processing = !processing.equals("PROCESSING_FINISHED");
                        seq.location.setLatitude(lat);
                        seq.location.setLongitude(lon);
                        mOnlineSequences.add(seq);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

