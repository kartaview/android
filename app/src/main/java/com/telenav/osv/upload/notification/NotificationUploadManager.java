package com.telenav.osv.upload.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.telenav.osv.R;
import com.telenav.osv.obd.ObdActivity;
import com.telenav.osv.utils.Log;
import androidx.core.app.NotificationCompat;

/**
 * Processor which handles all upload related functionality with regards to <b>notification</b>.
 */
public class NotificationUploadManager {

    /**
     * Identifier for the current class for logging.
     */
    private static final String TAG = NotificationUploadManager.class.getSimpleName();

    /**
     * Notification identifier which will be used to update/cancel the notification.
     */
    private static final int NOTIFICATION_ID = 113;

    /**
     * The identifier for the notification channel.
     */
    private static final String NOTIFICATION_CHANNEL_UPLOAD = "uploadNotificationChannel";

    /**
     * The name which will be display in the notification for the channel.
     */
    private static final String NOTIFICATION_CHANNEL_UPLOAD_NAME = "Upload Channel";

    /**
     * Max value for the progress displayed in the notification.
     */
    private static final int PROGRESS_MAX_VALUE = 100;

    /**
     * Initial value for the progress displayed in the notification.
     */
    private static final int PROGRESS_INIT_VALUE = 0;

    /**
     * Instance for the upload notification.
     */
    private NotificationCompat.Builder uploadNotificationBuilder;

    /**
     * The notification manager which will be used to display the notification.
     */
    private NotificationManager notificationManager;

    /**
     * Default constructor for the current class.
     */
    public NotificationUploadManager() {

    }

    /**
     * Start method which will initialise the current notification with upload specific information.
     */
    public void start(Context context) {
        initNotificationIfNeeded(context);
    }

    /**
     * Updates the notification progress for the current notification.
     * @param progress the progress to be set.
     * @param context the context required in order to get the UI resources.
     */
    @SuppressLint("DefaultLocale")
    public void update(double progress, Context context) {
        if (uploadNotificationBuilder != null && notificationManager != null) {
            int progressDisplay = (int) (progress * 100);
            uploadNotificationBuilder.setProgress(PROGRESS_MAX_VALUE, progressDisplay, false);
            uploadNotificationBuilder.setContentText(String.format("%s%d%%", context.getString(R.string.uploading_images), progressDisplay));
            notificationManager.notify(NOTIFICATION_ID, uploadNotificationBuilder.build());
        } else {
            Log.d(TAG, "update. Status: null. Message: Update progress not possible since notification and/or manager not set.");
        }
    }

    /**
     * Stop method which will remove the notification from the upload screen.
     */
    public void stop(Context context) {
        if (uploadNotificationBuilder != null && notificationManager != null) {
            uploadNotificationBuilder.setContentText(context.getString(R.string.upload_finished_message));
            uploadNotificationBuilder.setProgress(0, 0, false);
            uploadNotificationBuilder.setOngoing(false);
            uploadNotificationBuilder.setContentIntent(null);
            notificationManager.notify(NOTIFICATION_ID, uploadNotificationBuilder.build());
            uploadNotificationBuilder = null;
        } else {
            Log.d(TAG, "stop. Status: null. Message: Update progress not possible since notification and/or manager not set.");
        }
    }

    public int getNotificationId() {
        return NOTIFICATION_ID;
    }

    public Notification getNotification() {
        return uploadNotificationBuilder.build();
    }

    /**
     * @param context the context required for UI specific information.
     */
    private void initNotificationIfNeeded(Context context) {
        if (uploadNotificationBuilder == null) {
            Log.d(TAG, "initNotificationIfNeeded. Status: init. Message: Initialising the notification and/or the manager.");
            Intent intent = ObdActivity.newIntent(context, ObdActivity.SESSION_UPLOAD);
            PendingIntent pnextIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            initChannels(context);
            uploadNotificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_UPLOAD)
                    .setContentTitle(context.getString(R.string.app_short_name))
                    .setContentText(context.getString(R.string.uploading_images_message))
                    .setSmallIcon(R.drawable.ic_upload_white)
                    .setOngoing(false)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSound(null)
                    .setVibrate(null)
                    .setAutoCancel(false)
                    .setProgress(PROGRESS_MAX_VALUE, PROGRESS_INIT_VALUE, false)
                    .setColor(context.getResources().getColor(R.color.default_purple))
                    .setWhen(0)
                    .setContentIntent(pnextIntent);
            initManagerIfRequired(context);
        }
    }

    /**
     * Initialises the notification manager if already not set.
     * @param context the {@code context} required for getting the notification manager.
     */
    private void initManagerIfRequired(Context context) {
        if (notificationManager == null) {
            notificationManager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        }
    }

    /**
     * Method which setup action for the notification
     */
    private void setupActions() {
        //ToDo: add actions for notification
    }

    /**
     * Initialise the channels for the notification in order to be displayed.
     * @param context the context required in order to initialise the manager.
     */
    private void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        initManagerIfRequired(context);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_UPLOAD,
                NOTIFICATION_CHANNEL_UPLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setSound(null, null);
        channel.setDescription(context.getString(R.string.upload_channel_description));
        notificationManager.createNotificationChannel(channel);
    }
}
