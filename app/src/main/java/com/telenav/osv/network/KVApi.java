package com.telenav.osv.network;

import androidx.annotation.StringDef;

import com.telenav.osv.network.model.complete.ResponseModelUploadSequenceComplete;
import com.telenav.osv.network.model.image.ResponseModelUploadImage;
import com.telenav.osv.network.model.metadata.ResponseModelUploadMetadata;
import com.telenav.osv.network.model.tagging.ResponseModelUploadTagging;
import com.telenav.osv.network.model.video.ResponseModelUploadVideo;

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
public interface KVApi {

    @Multipart
    @POST("1.0/sequence/")
    Single<ResponseModelUploadMetadata> createSequence(
            @Part(KVApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
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
            @Part(KVApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(KVApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId,
            @Part("dataType") RequestBody dataType,
            @Part("sequenceIndex") RequestBody sequenceIndex,
            @Part MultipartBody.Part file);


    @Multipart
    @POST("1.0/sequence/finished-uploading/")
    Single<ResponseModelUploadSequenceComplete> sequenceComplete(
            @Part(KVApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(KVApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId);

    @Multipart
    @POST("1.0/photo/")
    Single<ResponseModelUploadImage> uploadImage(
            @Part(KVApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(KVApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId,
            @Part(KVApiRequestsFields.SEQUENCE_INDEX) RequestBody frameIndexInSequence,
            @Part("gpsAccuracy") RequestBody gpsAccuracy,
            @Part("coordinate") RequestBody coordinate,
            @Part MultipartBody.Part image);

    @Multipart
    @Streaming
    @POST("1.0/video/")
    Single<ResponseModelUploadVideo> uploadVideo(
            @Part(KVApiRequestsFields.ACCESS_TOKEN) RequestBody accessToken,
            @Part(KVApiRequestsFields.SEQUENCE_ID) RequestBody onlineSequenceId,
            @Part(KVApiRequestsFields.SEQUENCE_INDEX) RequestBody videoIndexInSequence,
            @Part MultipartBody.Part videoFile);

    @StringDef
    @interface KVApiRequestsFields {
        String ACCESS_TOKEN = "access_token";

        String SEQUENCE_ID = "sequenceId";

        String SEQUENCE_INDEX = "sequenceIndex";
    }
}
