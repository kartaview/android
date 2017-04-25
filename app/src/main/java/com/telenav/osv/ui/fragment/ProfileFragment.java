package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
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
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.ScoreHistory;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.list.SequenceAdapter;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by adrianbostan on 11/07/16.
 */

public class ProfileFragment extends Fragment implements RequestResponseListener {

    public final static String TAG = "ProfileFragment";

    /**
     * preference name
     */
    public static final String PREFS_NAME = "osvMyProfileAppPrefs";

    public static final String K_RANK = "rank";

    public static final String K_LEVEL = "level";

    public static final String K_SCORE = "score";

    public static final String K_XP_PROGRESS = "xpProgress";

    public static final String K_XP_TARGET = "xpTarget";

    public static final String K_TOTAL_DISTANCE = "totalDistance";

    public static final String K_OBD_DISTANCE = "obdDistance";

    public static final String K_TOTAL_PHOTOS = "totalPhotos";

    public static final String K_TOTAL_TRACKS = "totalTracks";

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

    private LinearLayoutManager mPortraitLayoutManager;

    private GridLayoutManager mLandscapeLayoutManager;

    private Handler mBackgroundHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, null);

        activity = (MainActivity) getActivity();
        mPortraitLayoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        mLandscapeLayoutManager = new GridLayoutManager(activity, 2);
        mLandscapeLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? 2 : 1;
            }
        });
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
        HandlerThread thread = new HandlerThread("profileLoader", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            mBackgroundHandler.getLooper().getThread().interrupt();
        } catch (Exception ignored){}
        super.onDestroyView();
    }

    public void refreshContent() {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "refreshContent ");
                requestProfileDetails();
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
                                                if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MY_PROFILE) {
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
        if (activity != null && mSequencesRecyclerView != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                mSequencesRecyclerView.setLayoutManager(mPortraitLayoutManager);
            } else {
                mSequencesRecyclerView.setLayoutManager(mLandscapeLayoutManager);
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean showHeader = activity.getApp().getAppPrefs().getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
        mOnlineSequencesAdapter = new SequenceAdapter(mOnlineSequences, activity, showHeader);
        mSequencesRecyclerView.setAdapter(mOnlineSequencesAdapter);
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mSequencesRecyclerView.setLayoutManager(mPortraitLayoutManager);
        } else {
            mSequencesRecyclerView.setLayoutManager(mLandscapeLayoutManager);
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {

                mSequencesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView,
                                           int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);

                        RecyclerView.LayoutManager lm = mSequencesRecyclerView.getLayoutManager();
                        if (lm instanceof LinearLayoutManager) {
                            mTotalItemCount = lm.getItemCount() - 1;
                            mLastVisibleItem = ((LinearLayoutManager) lm).findLastCompletelyVisibleItemPosition();

                            if (!mLoading && mTotalItemCount == mLastVisibleItem && mTotalItemCount < mMaxNumberOfResults) {
                                // End has been reached
                                mSwipeRefreshLayout.setRefreshing(true);
                                loadMoreResults();
                                mLoading = true;
                            }
                        }
                    }
                });
                displayCachedStats();
                refreshContent();
            }
        });
    }

    public void displayCachedStats() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final int rank = prefs.getInt(K_RANK, 0);
        final int score = prefs.getInt(K_SCORE, 0);
        final int level = prefs.getInt(K_LEVEL, 0);
        final int xpProgress = prefs.getInt(K_XP_PROGRESS, 0);
        final int xpTarget = prefs.getInt(K_XP_TARGET, 1);
        final String photos = prefs.getString(K_TOTAL_PHOTOS, "");
        final String tracks = prefs.getString(K_TOTAL_TRACKS, "");
        final String distance = prefs.getString(K_TOTAL_DISTANCE, "");
        final String obdDistance = prefs.getString(K_OBD_DISTANCE, "");
        final String[] totalDistanceFormatted, obdDistanceFormatted;
        if (distance.equals("")) {
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
        totalDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, totalDistanceNum);
        obdDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, obdDistanceNum);
        mSequencesRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mOnlineSequencesAdapter.notifyDataSetChanged();
                mOnlineSequencesAdapter.refreshDetails(totalDistanceFormatted, obdDistanceFormatted, photos, tracks, rank, score, level, xpProgress, xpTarget);
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
    public void onStart() {
        super.onStart();
        EventBus.register(this);
    }

    @Override
    public void onStop() {
        EventBus.unregister(this);
        super.onStop();
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
    public void requestFinished(int status, final String result) {
        // mOnlineSequences.clear();
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                handleSequenceListResult(result);
                stopRefreshing();
            }
        });

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
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                mUploadManager.listSequences(ProfileFragment.this, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE);
            }
        });
    }


    private void requestProfileDetails() {
        mUploadManager.getProfileDetails(new RequestResponseListener() {
            @Override
            public void requestFinished(int status, String result) {
                Log.d(TAG, "getProfileDetails: " + " status - > " + status + " result - > " + result);
                if (result != null && !result.isEmpty() && status == RequestResponseListener.STATUS_SUCCESS_PROFILE_DETAILS) {
                    final String userName, obdDistance, totalDistance, totalPhotos, totalTracks, levelName;
                    final String[] totalDistanceFormatted, obdDistanceFormatted;
                    int rank = 0, score = 0, level = 0, xpProgress = 0, xpTarget = 0;
                    try {
                        JSONObject obj = new JSONObject(result);
                        JSONObject osv = obj.getJSONObject("osv");
                        userName = osv.getString("username");
                        String userType = osv.getString("type");
                        activity.getApp().getAppPrefs().saveStringPreference(PreferenceTypes.K_USER_TYPE, userType);
                        obdDistance = osv.getString("obdDistance");
                        totalDistance = osv.getString("totalDistance");
                        totalPhotos = Utils.formatNumber(osv.getDouble("totalPhotos"));
                        totalTracks = Utils.formatNumber(osv.getDouble("totalTracks"));
                        try {
                            JSONObject gamification = osv.getJSONObject("gamification");

                            score = gamification.getInt("total_user_points");
                            level = gamification.getInt("level");
                            levelName = gamification.getString("level_name");
                            xpProgress = gamification.getInt("level_progress");
                            try {
                                xpTarget = gamification.getInt("level_target");
                            } catch (Exception e) {
                                Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
                            }
                            rank = gamification.getInt("rank");
                        } catch (Exception e) {
                            Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
                        }
                        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor prefsEditor = prefs.edit();
                        prefsEditor.putInt(K_RANK, rank);
                        prefsEditor.putInt(K_SCORE, score);
                        prefsEditor.putInt(K_LEVEL, level);
                        prefsEditor.putInt(K_XP_PROGRESS, xpProgress);
                        prefsEditor.putInt(K_XP_TARGET, xpTarget);
                        prefsEditor.putString(K_TOTAL_PHOTOS, totalPhotos);
                        prefsEditor.putString(K_TOTAL_TRACKS, totalTracks);
                        prefsEditor.putString(K_TOTAL_DISTANCE, totalDistance);
                        prefsEditor.putString(K_OBD_DISTANCE, obdDistance);
                        prefsEditor.apply();

                        Log.d(TAG, "getProfileDetails: " + userName + " " + totalDistance + " " + obdDistance + " " + totalPhotos + " " + totalTracks);

                        double totalDistanceNum = 0;
                        double obdDistanceNum = 0;
                        try {
                            if (totalDistance != null) {
                                totalDistanceNum = Double.parseDouble(totalDistance);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "getProfileDetails: " + Log.getStackTraceString(e));
                        }
                        try {
                            if (obdDistance != null) {
                                obdDistanceNum = Double.parseDouble(obdDistance);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "getProfileDetails: " + Log.getStackTraceString(e));
                        }
                        totalDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, totalDistanceNum);
                        obdDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, obdDistanceNum);
                        final int finalRank = rank;
                        final int finalScore = score;
                        final int finalLevel = level;
                        final int finalXpProgress = xpProgress;
                        final int finalXpTarget = xpTarget;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mOnlineSequencesAdapter.refreshDetails(totalDistanceFormatted, obdDistanceFormatted, totalPhotos, totalTracks, finalRank, finalScore, finalLevel,
                                        finalXpProgress, finalXpTarget);
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

    @Subscribe
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (event.online) {
            refreshContent();
        }
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
                            Log.w(TAG, "handleSequenceListResult: " + e.getLocalizedMessage());
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
//                            Log.w(TAG, "handleSequenceListResult: could not parse distance");
                        }


                        Sequence seq = new Sequence(id, date, Integer.valueOf(imgNum), partialAddress, thumbLink, obd, platform, platformVersion, appVersion, (int) (distanceNum
                                * 1000d), 0);
                        seq.processing = !processing.equals("PROCESSING_FINISHED");
                        seq.location.setLatitude(lat);
                        seq.location.setLongitude(lon);
                        int totalPoints = 0;
                        try {
                            JSONObject history = item.getJSONObject("upload_history");
                            boolean historyObd = history.getString("has_obd").equals("N");
                            JSONArray coverages = history.getJSONArray("coverage");
                            totalPoints = 0;
                            seq.scoreHistories.clear();
                            for (int j = 0; j < coverages.length(); j++) {
                                JSONObject coverage = coverages.getJSONObject(j);
                                int cov = Utils.getValueOnSegment(Integer.parseInt(coverage.getString("coverage_value").replace("+", "")));
                                int pts = coverage.getInt("coverage_points");
                                totalPoints += pts;
                                int photosCount = coverage.getInt("coverage_photos_count");
                                ScoreHistory existing = seq.scoreHistories.get(cov);
                                if (existing != null) {
                                    existing.obdPhotoCount += obd ? photosCount : 0;
                                    existing.photoCount += obd ? 0 : photosCount;
                                } else {
                                    seq.scoreHistories.put(cov, new ScoreHistory(cov, obd ? 0 : photosCount, obd ? photosCount : 0));
                                }
                            }
                        } catch (Exception e) {
//                            Log.w(TAG, "handleSequenceListResult: " + e.getLocalizedMessage());
                        }
                        seq.score = totalPoints;
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

