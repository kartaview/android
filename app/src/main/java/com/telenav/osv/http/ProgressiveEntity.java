package com.telenav.osv.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import android.support.annotation.NonNull;

/**
 * Created by Kalman on 06/09/16.
 */

public class ProgressiveEntity implements HttpEntity {
    private final DataProgressListener mListener;

    private final long mTotalSize;

    private HttpEntity mEntity;

    public ProgressiveEntity(HttpEntity entity, DataProgressListener listener) {
        mEntity = entity;
        mListener = listener;
        mTotalSize = mEntity.getContentLength();
    }

    @Override
    public void consumeContent() throws IOException {
        mEntity.consumeContent();
    }

    @Override
    public InputStream getContent() throws IOException,
            IllegalStateException {
        return mEntity.getContent();
    }

    @Override
    public Header getContentEncoding() {
        return mEntity.getContentEncoding();
    }

    @Override
    public long getContentLength() {
        return mEntity.getContentLength();
    }

    @Override
    public Header getContentType() {
        return mEntity.getContentType();
    }

    @Override
    public boolean isChunked() {
        return mEntity.isChunked();
    }

    @Override
    public boolean isRepeatable() {
        return mEntity.isRepeatable();
    }

    @Override
    public boolean isStreaming() {
        return mEntity.isStreaming();
    } // CONSIDER put a _real_ delegator into here!

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        mEntity.writeTo(new ProgressiveOutputStream(outstream, mListener));
    }

    public interface DataProgressListener {
        void onProgressChanged(long totalSent, long totalSize);
    }

    class ProxyOutputStream extends FilterOutputStream {
        /**
         * @author Stephen Colebourne
         */

        ProxyOutputStream(OutputStream proxy) {
            super(proxy);
        }

        public void write(int idx) throws IOException {
            out.write(idx);
        }

        public void write(@NonNull byte[] bts) throws IOException {
            out.write(bts);
        }

        public void write(@NonNull byte[] bts, int st, int end) throws IOException {
            out.write(bts, st, end);
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            out.close();
        }
    } // CONSIDER import this class (and risk more Jar File Hell)

    private class ProgressiveOutputStream extends ProxyOutputStream {
        private final DataProgressListener mListener;

        private long totalSent;

        private ProgressiveOutputStream(OutputStream proxy, DataProgressListener listener) {
            super(proxy);
            totalSent = 0;
            mListener = listener;
        }

        public void write(@NonNull byte[] bts, int st, int end) throws IOException {
            totalSent += end;
            mListener.onProgressChanged(totalSent, mTotalSize);
            out.write(bts, st, end);
        }
    }
}
