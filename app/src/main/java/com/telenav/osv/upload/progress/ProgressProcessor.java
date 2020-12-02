package com.telenav.osv.upload.progress;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.telenav.osv.common.structure.CircularQueue;
import com.telenav.osv.upload.progress.model.UploadUpdateProgress;
import com.telenav.osv.upload.status.UploadUpdate;
import com.telenav.osv.utils.Log;
import androidx.core.util.Consumer;

public class ProgressProcessor {

    private static final int DEFAULT_QUEUE_CAPACITY = 25;

    private static final int CORE_POOL_SIZE = 1;

    /**
     * The timer duration.
     */
    private static final int TIMER_DELAY_DURATION = 1;

    private static final double PERCENTAGE_MAX_VALUE_DOUBLE = 1.0D;

    private final String TAG = ProgressProcessor.class.getSimpleName();

    /**
     * Collection representing progress updates from all sequences.
     */
    private UploadUpdateProgress uploadUpdateProgress;

    private CircularQueue<Long> currentUnitQueue;

    private long previousCurrentUnit;

    private Consumer<UploadUpdate> uploadUpdateConsumer;

    private ScheduledThreadPoolExecutor progressExecutor;

    public ProgressProcessor(Consumer<UploadUpdate> uploadUpdateConsumer) {
        this.uploadUpdateConsumer = uploadUpdateConsumer;
        currentUnitQueue = new CircularQueue<>(DEFAULT_QUEUE_CAPACITY);
        progressExecutor = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE);
    }

    public void addChild(UploadUpdateProgress uploadUpdateProgress) {
        if (this.uploadUpdateProgress == null) {
            this.uploadUpdateProgress = uploadUpdateProgress;
        } else {
            uploadUpdateProgress.addChild(uploadUpdateProgress);
        }
    }

    public void start() {
        calculateProgress();
    }

    public void stop() {
        dispose();
    }

    private void calculateProgress() {
        progressExecutor.scheduleAtFixedRate(
                () -> {
                    Log.d(TAG, "calculateProgress. Status: progress thick.");
                    if (uploadUpdateConsumer != null && uploadUpdateProgress.getTotalUnit() != 0) {
                        long currentUnit = uploadUpdateProgress.getCurrentUnit();
                        long totalUnit = uploadUpdateProgress.getTotalUnit();
                        long eta = 0;
                        long bandwidth = 0;
                        long totalCurrentUnit = 0;
                        double percentage = PERCENTAGE_MAX_VALUE_DOUBLE;
                        //fail-safe if the percentage is larger than 1.0 to not display problematic values
                        if (currentUnit < totalUnit) {
                            percentage = ((double) currentUnit / totalUnit);
                        }
                        long newCurrentUnit = currentUnit - previousCurrentUnit;
                        currentUnitQueue.add(newCurrentUnit <= 0 ? 0 : newCurrentUnit);
                        for (int i = 0; i < currentUnitQueue.size(); i++) {
                            totalCurrentUnit += currentUnitQueue.get(i);
                        }
                        if (currentUnitQueue.size() > 0 && totalCurrentUnit != 0) {
                            bandwidth = totalCurrentUnit / currentUnitQueue.size();
                        }
                        if (bandwidth != 0) {
                            long remainingUnit = totalUnit - currentUnit;
                            if (remainingUnit > 0) {
                                eta = remainingUnit / bandwidth;
                            }
                        }
                        Log.d(TAG,
                                String.format("calculateProgress. Percentage: %s. Current unit: %s. Total unit: %s. Bandwidth: %s. Eta: %s.",
                                        percentage,
                                        currentUnit,
                                        totalUnit,
                                        bandwidth,
                                        eta));
                        //post the updates by calling the update consumer callback
                        uploadUpdateConsumer.accept(new UploadUpdate(percentage, eta, bandwidth, currentUnit, totalUnit));

                        this.previousCurrentUnit = currentUnit;
                    }
                },
                TIMER_DELAY_DURATION,
                TIMER_DELAY_DURATION,
                TimeUnit.SECONDS);
    }

    private void dispose() {
        if (progressExecutor != null) {
            progressExecutor.shutdown();
        }
    }
}
