package com.telenav.osv.network.response;

import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class ProgressResponseBody extends ResponseBody {

    /**
     * Unknown bytes size for the source.
     */
    private static final int UNKOWN_BYTES_SIZE = -1;

    private final ResponseBody responseBody;

    private final ProgressResponseListener progressResponseListener;

    private BufferedSource bufferedSource;

    private String progressIdentifier;

    public ProgressResponseBody(String progressIdentifier, ResponseBody responseBody, ProgressResponseListener progressResponseListener) {
        this.responseBody = responseBody;
        this.progressResponseListener = progressResponseListener;
        this.progressIdentifier = progressIdentifier;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;

            @Override
            public long read(@NotNull Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += bytesRead != UNKOWN_BYTES_SIZE ? bytesRead : 0;
                if (progressResponseListener != null) {
                    progressResponseListener.update(progressIdentifier, totalBytesRead, bytesRead == UNKOWN_BYTES_SIZE);
                }
                return bytesRead;
            }
        };
    }
}
