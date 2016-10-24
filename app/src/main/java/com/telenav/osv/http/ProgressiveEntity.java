package com.telenav.osv.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;

/**
 * Created by Kalman on 06/09/16.
 */

public class ProgressiveEntity implements HttpEntity {
    private static final String TAG = "ProgressiveEntity";

    private final DataProgressListener mListener;

    private final long mTotalSize;

    private HttpEntity mEntity;

    public ProgressiveEntity(HttpEntity entity, DataProgressListener listener, long size) {
        mEntity = entity;
        mListener = listener;
        mTotalSize = size;
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
        private static final long MB = 1024 * 1024;

        private final DataProgressListener mListener;

        private long totalSent;

        private long afterReport;

        private HandlerThread mHandlerThread = new HandlerThread("ProgressiveReport", Process.THREAD_PRIORITY_BACKGROUND);

        private Handler mHandler;

        private Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                mListener.onProgressChanged(totalSent, mTotalSize);
            }
        };

        private long mLastTime = 0;

        private ProgressiveOutputStream(OutputStream proxy, DataProgressListener listener) {
            super(proxy);
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            totalSent = 0;
            afterReport = MB;
            mListener = listener;
        }

        public void write(@NonNull byte[] bts, int st, int end) throws IOException {
            totalSent += end;
            afterReport += end;
//            Log.d(TAG, "write: totalSent: " + totalSent + ", totalSize: " + mTotalSize);
            if (afterReport >= MB && System.currentTimeMillis() - mLastTime > 1000) {
                mLastTime = System.currentTimeMillis();
                mHandler.post(updateRunnable);
            }
            out.write(bts, st, end);
        }
    }
}
