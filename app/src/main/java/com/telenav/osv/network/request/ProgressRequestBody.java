package com.telenav.osv.network.request;

import java.io.IOException;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import com.telenav.osv.network.response.ProgressResponseBody;
import com.telenav.osv.utils.Log;
import androidx.annotation.NonNull;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class ProgressRequestBody extends RequestBody {

    private static final String TAG = ProgressResponseBody.class.getSimpleName();

    /**
     * Unknown bytes size for the source.
     */
    private static final int UNKOWN_BYTES_SIZE = -1;

    private final RequestBody delegate;

    private final ProgressRequestListener progressRequestListener;

    public ProgressRequestBody(RequestBody delegate, ProgressRequestListener progressRequestListener) {
        this.delegate = delegate;
        this.progressRequestListener = progressRequestListener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() {
        try {
            return delegate.contentLength();
        } catch (IOException e) {
            Log.d(TAG, String.format("contentLength. Status: error. Message: %s", e.getLocalizedMessage()));
        }
        return UNKOWN_BYTES_SIZE;
    }


    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);

        delegate.writeTo(bufferedSink);

        bufferedSink.flush();
    }

    final class CountingSink extends ForwardingSink {

        private long bytesWritten = 0;

        CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            if (progressRequestListener != null) {
                progressRequestListener.update(bytesWritten, contentLength());
            }
        }
    }
}
