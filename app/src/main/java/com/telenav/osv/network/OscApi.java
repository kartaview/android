package com.telenav.osv.network;

import com.telenav.osv.network.model.complete.ResponseModelUploadSequenceComplete;
import com.telenav.osv.network.model.image.ResponseModelUploadImage;
import com.telenav.osv.network.model.metadata.ResponseModelUploadMetadata;
import com.telenav.osv.network.model.tagging.ResponseModelUploadTagging;
import com.telenav.osv.network.model.video.ResponseModelUploadVideo;
import androidx.annotation.StringDef;
import io.reactivex.Single;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;

/**
 * @author horatiuf
 */
public interface OscApi {

    @Multipart
    @POST("1.0/sequence/")
    Single<ResponseModelUploadMetadata> createSequence(
            @Part(OscApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part("uploadSource") RequestBody uploadSource,
            @Part("currentCoordinate") RequestBody initialCoordinates,
            @Part("obdInfo") RequestBody obdInfo,
            @Part("clientTotal") RequestBody score,
            @Part("platformName") RequestBody platform,
            @Part("platformVersion") RequestBody osVersion,
            @Part("appVersion") RequestBody appVersion,
            @Part("clientTotalDetails") RequestBody scoreDetails,
            @Part MultipartBody.Part metadata);

    @Multipart
    @POST("2.0/sequence-attachment/")
    Single<ResponseModelUploadTagging> uploadTagging(
            @Part(OscApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(OscApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId,
            @Part("dataType") RequestBody dataType,
            @Part("sequenceIndex") RequestBody sequenceIndex,
            @Part MultipartBody.Part file);


    @Multipart
    @POST("1.0/sequence/finished-uploading/")
    Single<ResponseModelUploadSequenceComplete> sequenceComplete(
            @Part(OscApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(OscApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId);

    @Multipart
    @POST("1.0/photo/")
    Single<ResponseModelUploadImage> uploadImage(
            @Part(OscApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(OscApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId,
            @Part(OscApiRequestsFields.SEQUENCE_INDEX) RequestBody frameIndexInSequence,
            @Part("gpsAccuracy") RequestBody gpsAccuracy,
            @Part("coordinate") RequestBody coordinate,
            @Part MultipartBody.Part image);

    @Multipart
    @Streaming
    @POST("1.0/video/")
    Single<ResponseModelUploadVideo> uploadVideo(
            @Part(OscApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(OscApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId,
            @Part(OscApiRequestsFields.SEQUENCE_INDEX) RequestBody videoIndexInSequence,
            @Part MultipartBody.Part videoFile);

    @StringDef
    @interface OscApiRequestsFields {
        String ACCESS_TOKEN = "access_token";

        String SEQUENCE_ID = "sequenceId";

        String SEQUENCE_INDEX = "sequenceIndex";
    }
}
