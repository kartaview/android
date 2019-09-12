package com.telenav.osv.network.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.network.request.ProgressRequestBody;
import com.telenav.osv.network.request.ProgressRequestListener;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * @author horatiuf
 */
public class NetworkRequestConverter {

    /**
     * Value for the request of type plain text.
     */
    public static final String REQUEST_MEDIA_TYPE_PLAIN_TEXT = "text/plain";

    /**
     * Value for the request of type zip.
     */
    public static final String REQUEST_MEDIA_TYPE_ZIP = "application/x-gzip";

    /**
     * Value for the request of type zip.
     */
    public static final String REQUEST_MEDIA_TYPE_JSON = "application/json";

    /**
     * Value for the request body of type video in mp4 compression.
     */
    public static final String REQUEST_MEDIA_TYPE_VIDEO = "video/mp4";

    /**
     * Value for the request of type image in jpeg compression.
     */
    public static final String REQUEST_MEDIA_TYPE_IMAGE = "image/jpeg";

    public static MultipartBody.Part generateFilePart(String name, OSVFile file, boolean isZip) {
        MediaType type = getMediaType(REQUEST_MEDIA_TYPE_PLAIN_TEXT);
        if (isZip) {
            type = getMediaType(REQUEST_MEDIA_TYPE_ZIP);
        }
        return MultipartBody.Part.createFormData(name, file.getName(), RequestBody.create(type, file));
    }

    public static FormBody generateFormBody(String name, String value) {
        return new FormBody.Builder()
                .add(name, value)
                .build();
    }

    public static MediaType getMediaType(@MediaTypesDef String type) {
        return MediaType.parse(type);
    }

    /**
     * Generate a MultipartBody.Part object required by the network in order to wrap file specific operations.
     * <p> This will either create a generic body or a progress specific one based on the null or set value of the given {@code progressRequestListener} param.
     * @return {@code MultipartBody.Part} with given params wrapped into specific format.
     */
    public static MultipartBody.Part generateMultipartBodyPart(@MediaTypesDef String type, String name, OSVFile file, @Nullable ProgressRequestListener progressRequestListener) {
        RequestBody requestBody = RequestBody.create(getMediaType(type), file);
        if (progressRequestListener != null) {
            requestBody = generateProgressRequestBody(requestBody, progressRequestListener);
        }
        return MultipartBody.Part.createFormData(name, file.getName(), requestBody);
    }

    public static ProgressRequestBody generateProgressRequestBody(RequestBody requestBody, ProgressRequestListener progressRequestListener) {
        return new ProgressRequestBody(requestBody, progressRequestListener);
    }

    public static RequestBody generateTextRequestBody(String content) {
        return RequestBody.create(getMediaType(REQUEST_MEDIA_TYPE_PLAIN_TEXT), content);
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(value = {REQUEST_MEDIA_TYPE_PLAIN_TEXT, REQUEST_MEDIA_TYPE_IMAGE, REQUEST_MEDIA_TYPE_VIDEO, REQUEST_MEDIA_TYPE_ZIP, REQUEST_MEDIA_TYPE_JSON})
    public @interface MediaTypesDef {
        //empty since we use the value for the constants
    }
}
