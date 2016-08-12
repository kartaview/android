package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.ui.list.SequenceAdapter;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by adrianbostan on 11/07/16.
 */

public class ProfileFragment extends Fragment implements RequestResponseListener {

    public final static String TAG = "ProfileFragment";

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    private MainActivity activity;

    private RecyclerView mSequencesRecyclerView;

    private SequenceAdapter mOnlineSequencesAdapter;

    private UploadManager mUploadManager;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ArrayList<Sequence> mOnlineSequences;

    private int mCurrentPageToList = 1;

    private int mLastVisibleItem, mTotalItemCount;

    private boolean mLoading;

    /**
     * preference name
     */
    public static final String PREFS_NAME = "osvMyProfileAppPrefs";

    public static final String K_OVERALL_RANK = "overallRank";

    public static final String K_WEEKLY_RANK = "weeklyRank";

    public static final String K_TOTAL_DISTANCE = "totalDistance";

    public static final String K_OBD_DISTANCE = "obdDistance";

    public static final String K_TOTAL_PHOTOS = "totalPhotos";

    public static final String K_TOTAL_TRACKS = "totalTracks";

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
        mOnlineSequences = new ArrayList<Sequence>();
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
        setProfileDetails();
        startRefreshing();
        mSequencesRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mCurrentPageToList = 1;
                mOnlineSequences.clear();
                mOnlineSequencesAdapter.notifyDataSetChanged();
                loadMoreResults();
            }
        });
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

        mOnlineSequencesAdapter = new SequenceAdapter(mOnlineSequences, activity);
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
        initStatistics();
        refreshContent();
    }

    public void initStatistics(){
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final String overall = prefs.getString(K_OVERALL_RANK, "");
        final String weekly = prefs.getString(K_WEEKLY_RANK, "");
        final String photos = prefs.getString(K_TOTAL_PHOTOS, "");
        final String tracks = prefs.getString(K_TOTAL_TRACKS, "");
        final String distance = prefs.getString(K_TOTAL_DISTANCE, "");
        final String obdDistance = prefs.getString(K_OBD_DISTANCE, "");
        final String[] totalDistanceFormatted, obdDistanceFormatted;
        if (overall.equals("")){
            return;
        }
        double totalDistanceNum = 0;
        double obdDistanceNum = 0;
        try {
            totalDistanceNum = Double.parseDouble(distance);
        } catch (NumberFormatException e) {
            Log.d(TAG, "getProfileDetails: " + Log.getStackTraceString(e));
        }
        try {
            obdDistanceNum = Double.parseDouble(obdDistance);
        } catch (NumberFormatException e) {
            Log.d(TAG, "getProfileDetails: " + Log.getStackTraceString(e));
        }
        totalDistanceFormatted = Utils.formatDistanceFromKiloMeters(getContext(), totalDistanceNum);
        obdDistanceFormatted = Utils.formatDistanceFromKiloMeters(getContext(), obdDistanceNum);
        mSequencesRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mOnlineSequencesAdapter.notifyDataSetChanged();
                mOnlineSequencesAdapter.refreshDetails(totalDistanceFormatted, obdDistanceFormatted, photos, overall, tracks, weekly);
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


    @Override
    public void requestFinished(int status, String result) {
        // mOnlineSequences.clear();
        handleSequenceListResult(result);
        stopRefreshing();

    }

    @Override
    public void requestFinished(int status) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCurrentPageToList--;
//                if (!Utils.isInternetAvailable(activity)){
//                    mOnlineSequences.clear();
//                }
                if (mOnlineSequences.isEmpty()) {
                    mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                }
                mOnlineSequencesAdapter.notifyDataSetChanged();
                stopRefreshing();
            }
        });

    }

    private void loadMoreResults() {
        mUploadManager.listSequences(this, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE, null, null, true);
    }


    private void setProfileDetails() {
        mUploadManager.getProfileDetails(new RequestResponseListener() {
            @Override
            public void requestFinished(int status, String result) {
                Log.d(TAG, "getProfileDetails: " + " status - > " + status + " result - > " + result);
                if (result != null && !result.isEmpty() && status == RequestResponseListener.STATUS_SUCCESS_PROFILE_DETAILS) {
                    final String userName, obdDistance, totalDistance, totalPhotos, overallRank, totalTracks, weeklyRank;
                    final String[] totalDistanceFormatted, obdDistanceFormatted;
                    try {
                        JSONObject obj = new JSONObject(result);
                        JSONObject osv = obj.getJSONObject("osv");
                        userName = osv.getString("username");
                        obdDistance = osv.getString("obdDistance");
                        totalDistance = osv.getString("totalDistance");
                        totalPhotos = osv.getString("totalPhotos");
                        overallRank = osv.getString("overallRank");
                        totalTracks = osv.getString("totalTracks");
                        weeklyRank = osv.getString("weeklyRank");

                        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor prefsEditor = prefs.edit();
                        prefsEditor.putString(K_OVERALL_RANK, overallRank);
                        prefsEditor.putString(K_WEEKLY_RANK, weeklyRank);
                        prefsEditor.putString(K_TOTAL_PHOTOS, totalPhotos);
                        prefsEditor.putString(K_TOTAL_TRACKS, totalTracks);
                        prefsEditor.putString(K_TOTAL_DISTANCE, totalDistance);
                        prefsEditor.putString(K_OBD_DISTANCE, obdDistance);
                        prefsEditor.apply();

                        Log.d(TAG, "getProfileDetails: " + userName + " " + totalDistance + " " + obdDistance + " " + totalPhotos + " " + overallRank + " " + totalTracks + " " + weeklyRank);

                        double totalDistanceNum = 0;
                        double obdDistanceNum = 0;
                        try {
                            if (totalDistance != null) {
                                totalDistanceNum = Double.parseDouble(totalDistance);
                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "getProfileDetails: " + Log.getStackTraceString(e));
                        }
                        try {
                            if (obdDistance != null) {
                                obdDistanceNum = Double.parseDouble(obdDistance);
                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "getProfileDetails: " + Log.getStackTraceString(e));
                        }
                        totalDistanceFormatted = Utils.formatDistanceFromKiloMeters(getContext(), totalDistanceNum);
                        obdDistanceFormatted = Utils.formatDistanceFromKiloMeters(getContext(), obdDistanceNum);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mOnlineSequencesAdapter.refreshDetails(totalDistanceFormatted, obdDistanceFormatted, totalPhotos, overallRank, totalTracks, weeklyRank);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void requestFinished(int status) {
                activity.showSnackBar("No Internet connection detected.", Snackbar.LENGTH_LONG);
                Log.d(TAG, "getProfileDetails: " + " status - > " + status);
            }
        });
    }

    private void handleSequenceListResult(String result) {
        if (result != null && !result.isEmpty()) {
            try {
                JSONObject obj = new JSONObject(result);
                JSONArray array1 = obj.getJSONArray("totalFilteredItems");
                mCurrentPageToList++;
                mMaxNumberOfResults = array1.getInt(0);
                if (array1.length() > 0 && array1.getInt(0) > 0) {
                    JSONArray array = obj.getJSONArray("currentPageItems");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        int id = item.getInt("id");
                        String date = item.getString("date_added");
                        try {
                            date = Utils.numericDateFormat.format(Utils.onlineDateFormat.parse(date));
                        } catch (Exception e) {
                            Log.d(TAG, "handleSequenceListResult: " + e.getLocalizedMessage());
                        }
                        String imgNum = item.getString("photo_no");
                        String distance = item.getString("distance");
                        double lat = item.getDouble("current_lat");
                        double lon = item.getDouble("current_lng");
                        String processing = item.getString("image_processing_status");
                        boolean obd = false;
                        String platform = "";
                        String platformVersion = "";
                        String appVersion = "";
                        try {
                            platform = item.getString("platform_name");
                            platformVersion = item.getString("platform_version");
                            appVersion = item.getString("app_version");
                            obd = item.getInt("obd_info") > 0;
                        } catch (Exception e) {
                            if (!(e instanceof JSONException)) {
                                Log.w(TAG, "handleSequenceListResult: " + Log.getStackTraceString(e));
                            }
                        }

                        String partialAddress = "";
                        try {
                            String address = item.getString("location");
                            String[] list = address.split(", ");
                            partialAddress = list[0] + ", " + list[2];
                        } catch (Exception e) {
//                            Log.d(TAG, "handleSequenceListResult: exception during adress parsing");
                        }
                        String thumbLink = UploadManager.URL_DOWNLOAD_PHOTO + item.getString("thumb_name");
                        double distanceNum = 0;
                        try {
                            if (distance != null) {
                                distanceNum = Double.parseDouble(distance);

                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "handleSequenceListResult: could not parse distance");
                        }
                        Sequence seq = new Sequence(id, date, Integer.valueOf(imgNum), partialAddress, thumbLink, obd, platform, platformVersion, appVersion, (int) (distanceNum * 1000d));
                        seq.processing = !processing.equals("PROCESSING_FINISHED");
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
                    mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                    mOnlineSequencesAdapter.notifyDataSetChanged();
                }
            }
        });

    }
}

