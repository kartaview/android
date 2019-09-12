package com.telenav.osv.obd.connected;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.utils.AnimationUtils;
import com.telenav.osv.utils.FormatUtils;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ObdConnectedFragment#newInstance(Bundle)} factory method to
 * create an instance of this fragment.
 */
public class ObdConnectedFragment extends ObdBaseFragment implements ObdConnectedContract.ObdConnectedView, ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdConnectedFragment.class.getSimpleName();

    /**
     * Instance to the presenter from the {@code ObdConnectedContract}.
     */
    private ObdConnectedContract.ObdConnectedPresenter presenter;

    /**
     * Instance to view presenting the car icon.
     */
    private ImageView carIcon;

    /**
     * Instance to view presenting the phone icon.
     */
    private ImageView phoneIcon;

    /**
     * Instance to view presenting the data icon.
     */
    private ImageView dataIcon;

    /**
     * The animator set for obd data icon.
     */
    private AnimatorSet obdDataIconAnimatorSet;

    /**
     * Instance to the parent view.
     */
    private View view;

    /**
     * The {@code TextView} representing the duration value.
     */
    private TextView duration;

    /**
     * The {@code TextView} representing the distance value.
     */
    private TextView distance;

    /**
     * The {@code TextView} representing the pics value.
     */
    private TextView pics;

    /**
     * The {@code TextView} representing the points value.
     */
    private TextView points;

    /**
     * The {@code TextView} representing the speed value.
     */
    private TextView speed;

    /**
     * The {@code TextView} representing the speed metrics.
     */
    private TextView speedMetrics;

    /**
     * Default constructor for the current class.
     */
    public ObdConnectedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param bundle the Bundle used in order too persist data.
     * @return A new instance of fragment ObdConnectedFragment.
     */
    public static ObdConnectedFragment newInstance(Bundle bundle) {
        ObdConnectedFragment obdConnectedFragment = new ObdConnectedFragment();
        obdConnectedFragment.setArguments(bundle);
        return obdConnectedFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OSVApplication osvApplication = (OSVApplication) getActivity().getApplication();
        new ObdConnectedPresenterImpl(this,
                osvApplication.getAppPrefs(),
                Injection.provideObdManager(osvApplication.getApplicationContext(), osvApplication.getAppPrefs()),
                osvApplication.getScore(),
                osvApplication.getRecorder()
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_obd_connected, container, false);
        view.getViewTreeObserver().addOnGlobalLayoutListener(this);
        initViews(view);
        return view;
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return new ToolbarSettings.Builder()
                .setTitle(R.string.obd_connected);
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void setPresenter(ObdConnectedContract.ObdConnectedPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.initDetails(getArguments());
        view.postDelayed(this::startAnimation, AnimationUtils.AnimationDurations.ANIMATION_DURATION_500_MS);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (obdDataIconAnimatorSet != null) {
            obdDataIconAnimatorSet.cancel();
        }
    }

    @Override
    public void onPause() {
        EventBus.unregister(this);
        presenter.dispose();
        super.onPause();
    }

    @Override
    public void onResume() {
        EventBus.register(this);
        presenter.start();
        super.onResume();
    }

    @Override
    public void onGlobalLayout() {
        createObdDataAnimation();
        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void updateDuration(int hours, int minutes) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> duration.setText(String.format(FormatUtils.OBD_CONNECTED_HOUR_MIN_FORMAT, hours, minutes)));
        }
    }

    @Override
    public void updateSpeed(String speed, String metric) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                this.speed.setText(speed);
                this.speedMetrics.setText(metric);
            });
        }
    }

    @Override
    public void goToObdConnect() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(activity::onBackPressed);
        }
    }

    /**
     * <p>
     * The formats used:
     * <ul>
     * <li>pics - {@link FormatUtils#formatNumber(double)}</li>
     * <li>distance - {@link FormatUtils#formatDistanceFromMeters(boolean, int)}</li>
     * </ul>
     */
    @Override
    public void updateImageDetails(int pics, int distance) {
        this.pics.setText(FormatUtils.formatNumber(pics));
        String[] distanceArray = FormatUtils.formatDistanceFromMeters(presenter.isImperial(), distance);
        this.distance.setText(String.format("%s %s", distanceArray[0], distanceArray[1]));
    }

    @Override
    public void updateScore(long score) {
        points.setText(FormatUtils.formatNumber(score));
    }

    /**
     * Initialise the views.
     * @param view the parent view.
     */
    private void initViews(View view) {
        String initValue = String.valueOf(0);

        carIcon = view.findViewById(R.id.image_view_obd_connected_car);
        phoneIcon = view.findViewById(R.id.image_view_obd_connected_phone);
        dataIcon = view.findViewById(R.id.image_view_obd_connected_data);
        pics = view.findViewById(R.id.text_view_obd_connected_pics);
        pics.setText(initValue);
        duration = view.findViewById(R.id.text_view_obd_connected_duration);
        duration.setText(initValue);
        distance = view.findViewById(R.id.text_view_obd_connected_distance);
        distance.setText(initValue);
        points = view.findViewById(R.id.text_view_obd_connected_points);
        if (presenter.isScoreEnabled()) {
            points.setText(initValue);
        } else {
            points.setVisibility(View.GONE);
            view.findViewById(R.id.text_view_obd_connected_points_label).setVisibility(View.GONE);
        }
        speed = view.findViewById(R.id.text_view_obd_connected_speed);
        speed.setText(initValue);
        speedMetrics = view.findViewById(R.id.text_view_obd_connected_speed_metrics);
        speedMetrics.setText(FormatUtils.FORMAT_SPEED_KM);

        View disconnectButton = view.findViewById(R.id.text_view_obd_connected_disconnect_button);
        disconnectButton.setOnClickListener(click -> {
            presenter.stopCollecting();
            goToObdConnect();
        });
    }

    /**
     * Creates all the required animations and setups an {@code obdDataIconAnimatorSet} to play them sequentially.
     * <p>
     * Since current android version is <24 the infinite replay needs to be done manually by calling {@link AnimatorSet#start()} on animation end of the animation listener.
     */
    private void createObdDataAnimation() {
        obdDataIconAnimatorSet = AnimationUtils.createAnimatorSet(true,
                AnimationUtils.AnimationDurations.ANIMATION_DURATION_1250_MS,
                0,
                new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        //not required
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dataIcon.clearAnimation();
                        obdDataIconAnimatorSet.start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //not required
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        //not required
                    }
                },
                getLeftRotationAnimation(), getRightRotationAnimation());
    }

    /**
     * @return {@code ObjectAnimator} representing the left translation for the data icon. At the end of the animation the view will rotate by
     * {@link AnimationUtils#DEGREES_180_VALUE}.
     */
    private ObjectAnimator getLeftRotationAnimation() {
        return AnimationUtils.getTranslationObjectAnimator(dataIcon,
                new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        //not required
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dataIcon.setRotation(AnimationUtils.DEGREES_180_VALUE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //not required
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        //not required
                    }
                },
                AnimationUtils.getViewAbsoluteX(carIcon));
    }

    /**
     * @return {@code ObjectAnimator} representing the left translation for the data icon. At the end of the animation the view will rotate by
     * {@link AnimationUtils#DEGREES_360_VALUE}.
     */
    private ObjectAnimator getRightRotationAnimation() {
        return AnimationUtils.getTranslationObjectAnimator(dataIcon,
                new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        //not required
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dataIcon.setRotation(AnimationUtils.DEGREES_360_VALUE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //not required
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        //not required
                    }
                }, AnimationUtils.getViewAbsoluteX(phoneIcon));
    }

    /**
     * Start the animation for obd car data. If the {@link #obdDataIconAnimatorSet} is not created it will be created with all the required animations.
     */
    private void startAnimation() {
        if (obdDataIconAnimatorSet != null) {
            obdDataIconAnimatorSet.start();
        }
    }
}
