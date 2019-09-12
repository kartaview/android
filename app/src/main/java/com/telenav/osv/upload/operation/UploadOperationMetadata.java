package com.telenav.osv.upload.operation;

import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Build;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.data.score.model.ScoreHistory;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPoints;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.network.OscApi;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;
import com.telenav.osv.network.model.metadata.ResponseModelUploadMetadata;
import com.telenav.osv.network.util.NetworkRequestConverter;
import com.telenav.osv.upload.progress.model.UploadUpdateDisk;
import com.telenav.osv.upload.progress.model.UploadUpdateProgress;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import io.reactivex.Completable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * The operation which will upload a metadata file to the network.
 * <p> This will generate a stream via {@link #getStream()} method which is the only public entry point to this operation.
 * @author horatiuf
 * @see UploadOperationBase
 * @see #getStream()
 */
public class UploadOperationMetadata extends UploadOperationBase {

    /**
     * Metadata throwable message for when the file was not found when creating the stream before network call.
     */
    public static final String THROWABLE_MESSAGE_METADATA_FILE_NOT_FOUND = "Metadata file not found";

    /**
     * Identifier for the current class.
     */
    private static final String TAG = UploadOperationMetadata.class.getSimpleName();

    /**
     * The name used for encoding a {@code ScoreHistory} coverage.
     */
    private static final String SCORE_HISTORY_NAME_COVERAGE = "coverage";

    /**
     * The name used for encoding a {@code ScoreHistory} photo count.
     */
    private static final String SCORE_HISTORY_NAME_PHOTO_COUNT = "photo";

    /**
     * The name used for encoding a {@code ScoreHistory} obd photo count.
     */
    private static final String SCORE_HISTORY_NAME_OBD_PHOTO_COUNT = "obdPhoto";

    /**
     * The name used for encoding a {@code Sequence} metadata file compressed by zip.
     */
    private static final String METADATA_NAME_ZIP = "track.txt.gz";

    /**
     * The name used for encoding a {@code Sequence} metadata file compressed by txt.
     */
    private static final String METADATA_NAME_TXT_FILE = "track.txt";

    /**
     * The name used for encoding a {@code Sequence} metadata file in a request body of type multipart body.
     */
    private static final String METADATA_NAME_MULTI_PART_BODY = "metaData";

    /**
     * The value for when the sequence was recorded with obd required by the network in this format.
     */
    private static final int VALUE_OBD_TRUE = 1;

    /**
     * The value for when the obd was <i>not</i> recorded required by the network in this format.
     */
    private static final int VALUE_OBD_FALSE = 0;

    /**
     * The source representing from where the network operation is made for identification purposes.
     */
    private final String SOURCE = "Android";

    /**
     * The OS version required for the network operation.
     */
    private final String OS_VERSION = Build.VERSION.RELEASE;

    /**
     * Reference to the metadata file used in both pre and post request processing.
     */
    private OSVFile metadataFile;

    /**
     * Action which represent specific handling cases to be performed on success behaviour.
     */
    private Consumer<Long> consumerSuccess;

    /**
     * The reward information of the sequence, used for networking information passing.
     * @see SequenceDetailsRewardBase
     */
    @Nullable
    private SequenceDetailsRewardBase rewardBase;

    /**
     * The details of the sequence, used for networking information processing.
     * @see SequenceDetails
     */
    private SequenceDetails details;

    /**
     * The sequence folder which will be used for metadata file search.
     */
    private OSVFile sequenceFolder;

    /**
     * Default constructor for the current class.
     */
    UploadOperationMetadata(@NonNull String accessToken,
                            @NonNull OscApi api,
                            @NonNull OSVFile sequenceFolder,
                            @NonNull SequenceDetails details,
                            @NonNull SimpleEventBus eventBus,
                            @Nullable SequenceDetailsRewardBase rewardBase,
                            @Nullable Consumer<Long> consumerSuccess,
                            @Nullable Consumer<Throwable> consumerError) {
        super(accessToken, api, eventBus, consumerError);
        this.details = details;
        this.rewardBase = rewardBase;
        this.consumerSuccess = consumerSuccess;
        this.sequenceFolder = sequenceFolder;
        this.consumerError = consumerError;
    }

    /**
     * @return {@code Completable} composed of:
     * <ul>
     * <li> network call by using internally {@link #uploadMetadataStream()}</li>
     * <li> error logging and handling </li>
     * </ul>
     */
    public Completable getStream() {
        return Completable.defer(this::uploadMetadataStream)
                .doOnError(throwable -> Log.d(TAG, String.format("getStream. Status: error. Message: %s.", throwable.getLocalizedMessage())));
    }

    @Override
    public void dispose() {

    }

    /**
     * @return Completable which will process for complete either:
     * <ul>
     * <li> nothing since the id is already set</li>
     * <li> process of data for the upload metadata network request
     * {@link OscApi#createSequence(RequestBody, RequestBody, RequestBody, RequestBody, RequestBody, RequestBody, RequestBody, RequestBody, RequestBody, MultipartBody.Part)}.
     * <p>On success the response will be processed internally via {@link #requestOnlineIdAndMetadataUploadNetworkCompletable(String, String, MultipartBody.Part)} method.
     * </li>
     * </ul>
     */
    private Completable uploadMetadataStream() {
        long onlineSequenceId = details.getOnlineId();
        //checks if the online id is set or not, in case it is it will return a complete without any processing
        if (SequenceDetails.ONLINE_ID_NOT_SET != onlineSequenceId) {
            Log.d(TAG, "uploadMetadataStream. Status: found online id. Message: Returning on complete. Nothing will be performed.");
            removeMetadataFileIfExists();
            return Completable.complete();
        }
        //get the metadata part
        MultipartBody.Part metadataPart = getMetadataPart();
        //if metadata file does not exist there is no point in calling the network, therefore there will be signaled an error.
        if (metadataPart == null) {
            Throwable throwable = new Throwable(THROWABLE_MESSAGE_METADATA_FILE_NOT_FOUND);
            if (consumerError != null) {
                consumerError.accept(throwable);
            }
            return Completable.error(throwable);
        }
        Pair<String, String> scoreData = getScoreData(rewardBase);
        return requestOnlineIdAndMetadataUploadNetworkCompletable(scoreData.first, scoreData.second, metadataPart);
    }

    /**
     * @param score the score in {@code String} format.
     * @param encodedScore the encoded score which is required by the network request.
     * @param metadataPart the metadata part of the request which will be uploaded to the server.
     * @return the {@code Completable} which will process the api request and the response for online id and upload metadata.
     * <p> This will in turn also implement a retry mechanism for when the request will signal an error by having a finite retry given by the {@link #RETRIES_LIMIT} field from
     * the base class.</p>
     */
    private Completable requestOnlineIdAndMetadataUploadNetworkCompletable(String score, String encodedScore, MultipartBody.Part metadataPart) {
        return api.createSequence(
                NetworkRequestConverter.generateTextRequestBody(accessToken),
                NetworkRequestConverter.generateTextRequestBody(SOURCE),
                NetworkRequestConverter.generateTextRequestBody(getLocationAsString(details.getInitialLocation())),
                NetworkRequestConverter.generateTextRequestBody(String.valueOf(details.isObd() ? VALUE_OBD_TRUE : VALUE_OBD_FALSE)),
                NetworkRequestConverter.generateTextRequestBody(score),
                NetworkRequestConverter.generateTextRequestBody(SOURCE),
                NetworkRequestConverter.generateTextRequestBody(OS_VERSION),
                NetworkRequestConverter.generateTextRequestBody(details.getAppVersion()),
                NetworkRequestConverter.generateTextRequestBody(encodedScore),
                metadataPart)
                .flatMapCompletable(response -> handleDefaultResponse(response, handleUploadMetadataSuccessResponse(), null))
                .retryWhen(this::handleDefaultRetryFlowableWithTimer);
    }

    /**
     * Process the success response by calling {@link #consumerSuccess} if set and {@link #removeMetadataFileIfExists()} internally.
     */
    private Consumer<ResponseNetworkBase> handleUploadMetadataSuccessResponse() {
        return (response) -> {
            if (consumerSuccess != null) {
                ResponseModelUploadMetadata responseModelUploadMetadata = (ResponseModelUploadMetadata) response;
                consumerSuccess.accept(responseModelUploadMetadata.osv.sequence.id);
            }
            removeMetadataFileIfExists();
        };
    }

    /**
     * Remove the metadata file in exists. This will be performed before a {@link Completable#complete()} manual callback.
     * <p> The method will internally call progress updates with the size removed from the disk in case the remove of the file is successful.</p>
     */
    private void removeMetadataFileIfExists() {
        //loads the file for metadata which is in zip format
        loadMetadataFileIntoMemory();
        //if metadata file exists remove and send a progress update
        if (metadataFile.exists()) {
            long metadataSize = Utils.fileSize(metadataFile);
            boolean metadataFileRemove = metadataFile.delete();
            Log.d(TAG,
                    String.format("removeMetadataFileIfExists. Status : %s. Message: Remove physical metadata file from the device. Size: %s.", metadataFileRemove, metadataSize));
            updateEventBus.post(new UploadUpdateDisk(metadataSize));
            updateEventBus.post(new UploadUpdateProgress(metadataSize, metadataSize));
        }
    }

    /**
     * Loads either the zip or the txt file of the metadata into memory in case it is not already
     */
    private void loadMetadataFileIntoMemory() {
        if (metadataFile != null && metadataFile.exists()) {
            return;
        }
        metadataFile = new OSVFile(sequenceFolder, METADATA_NAME_ZIP);
        if (!metadataFile.exists()) {
            metadataFile = new OSVFile(sequenceFolder, METADATA_NAME_TXT_FILE);
        }
    }

    /**
     * @param rewardBase the sequence reward information from which the reward will be extracted.
     * @return the score data in both normal (simple String) and encoded format.
     */
    private Pair<String, String> getScoreData(SequenceDetailsRewardBase rewardBase) {
        int score = 0;
        String encodedScore = StringUtils.EMPTY_STRING;
        if (rewardBase != null) {
            score = (int) rewardBase.getValue();
            if (rewardBase instanceof SequenceDetailsRewardPoints) {
                SequenceDetailsRewardPoints SequenceDetailsRewardPoints = (SequenceDetailsRewardPoints) rewardBase;
                Map<Integer, ScoreHistory> scoreHistory = SequenceDetailsRewardPoints.getScoreHistory();
                if (scoreHistory != null) {
                    encodedScore = getEncodedScoreHistory(scoreHistory);
                }
            }
        }
        return new Pair<>(String.valueOf(score), encodedScore);
    }

    /**
     * @return {@code MultipartBody.Part} representing the part of the request which create for the metadata file a request body or null if the metadata file does not exist.
     */
    @Nullable
    private MultipartBody.Part getMetadataPart() {
        //loads the file for metadata which is in zip format
        loadMetadataFileIntoMemory();
        //creates the media type network body which process a zip file
        if (metadataFile.exists()) {
            @NetworkRequestConverter.MediaTypesDef String mediaType =
                    metadataFile.getName().equals(METADATA_NAME_ZIP) ?
                            NetworkRequestConverter.REQUEST_MEDIA_TYPE_ZIP :
                            NetworkRequestConverter.REQUEST_MEDIA_TYPE_PLAIN_TEXT;
            //creates the media type network body which process a txt file
            return NetworkRequestConverter.generateMultipartBodyPart(mediaType, METADATA_NAME_MULTI_PART_BODY, metadataFile, null);
        }
        return null;
    }

    /**
     * @param histories the score histories which will be encoded via a json array into a string. Each history will represent on entry in the array.
     * @return {@code String} which is internally a Json array object with score histories mapped by network specific required fields.
     */
    private String getEncodedScoreHistory(Map<Integer, ScoreHistory> histories) {
        JSONArray array = new JSONArray();
        for (ScoreHistory history : histories.values()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put(SCORE_HISTORY_NAME_COVERAGE, String.valueOf(history.getCoverage()));
                obj.put(SCORE_HISTORY_NAME_PHOTO_COUNT, String.valueOf(history.getPhotoCount()));
                obj.put(SCORE_HISTORY_NAME_OBD_PHOTO_COUNT, String.valueOf(history.getObdPhotoCount()));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, String.format("getEncodedScoreHistory. Status: error. Message: %s.", e.getLocalizedMessage()));
            }
            array.put(obj);
        }
        return array.toString();
    }
}
