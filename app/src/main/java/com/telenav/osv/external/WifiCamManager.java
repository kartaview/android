package com.telenav.osv.external;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.telenav.osv.R;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.external.model.ImageSize;
import com.telenav.osv.external.network.CameraConnector;
import com.telenav.osv.external.network.CameraInfo;
import com.telenav.osv.external.network.HttpEventListener;
import com.telenav.osv.external.network.ImageData;
import com.telenav.osv.external.network.ImageInfo;
import com.telenav.osv.external.network.StorageInfo;
import com.telenav.osv.external.view.ImageItem;
import com.telenav.osv.external.view.JpegInputStream;
import com.telenav.osv.external.view.WifiCamSurfaceView;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 2/6/16.
 */
public class WifiCamManager {

    private static final String TAG = "WifiCamManager";

    private Context mContext;

    private String cameraIpAddress;

    private CameraStatusChangedListener mStatusListener;

    private ImageSize currentImageSize;

    private WifiCamSurfaceView mPreviewSurfaceView;

    public WifiCamManager(Context context) {
        mContext = context;
        cameraIpAddress = context.getResources().getString(R.string.theta_ip_address);
    }

    public void setOnCameraStatusChangedListener(CameraStatusChangedListener listener) {
        this.mStatusListener = listener;
    }

    public void takePicture(final HttpEventListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CameraConnector camera = new CameraConnector(mContext.getResources().getString(R.string.theta_ip_address));

                CameraConnector.ShootResult result = camera.takePicture(listener);
                if (result == CameraConnector.ShootResult.FAIL_CAMERA_DISCONNECTED) {
                    Log.d(TAG, "takePicture: FAIL camera disconnected");
                } else if (result == CameraConnector.ShootResult.FAIL_STORE_FULL) {
                    Log.d(TAG, "takePicture: FAIL storage full");
                } else if (result == CameraConnector.ShootResult.FAIL_DEVICE_BUSY) {
                    Log.d(TAG, "takePicture: FAIL device busy");
                } else if (result == CameraConnector.ShootResult.SUCCESS) {
                    Log.d(TAG, "takePicture: SUCCESS");
                }
            }
        }).start();
    }

    public void startPreview(final WifiCamSurfaceView preview) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mPreviewSurfaceView = preview;
                mPreviewSurfaceView.play();
                JpegInputStream mjis = null;
                final int MAX_RETRY_COUNT = 5;

                for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
                    try {
                        Log.d(TAG, "startPreview: start Live view");
                        CameraConnector camera = new CameraConnector(cameraIpAddress);
                        InputStream is = camera.getLivePreview();
                        mjis = new JpegInputStream(is);
                        retryCount = MAX_RETRY_COUNT;
                    } catch (IOException e) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } catch (JSONException e) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                final JpegInputStream jpegInputStream = mjis;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (jpegInputStream != null) {
                            mPreviewSurfaceView.setSource(jpegInputStream);
                            EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_READY));
                        } else {
                            Log.d(TAG, "onPostExecute: failed to start live view");
                            EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_FAILED));
                        }
                    }
                });
            }
        }).start();
    }

    public void disconnectCamera() {
        new WifiCamManager.DisConnectTask().execute();
    }

    public void getImage(final String fileId, final ImageDataCallback imageDataCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CameraConnector camera = new CameraConnector(mContext.getResources().getString(R.string.theta_ip_address));
                ImageData image = camera.getImage(fileId, null);
                if (image != null) {
                    imageDataCallback.onImageDataReceived(image);
                } else {
                    imageDataCallback.onRequestFailed();
                    Log.d(TAG, "getImage: failed to get file data.");
                }
            }
        }).start();
    }

    public void getThumbnail(final String fileId, final ImageDataCallback imageDataCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CameraConnector camera = new CameraConnector(mContext.getResources().getString(R.string.theta_ip_address));
                Bitmap thumbnail = camera.getThumb(fileId);
                if (thumbnail != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] thumbnailImage = baos.toByteArray();
                    ImageData imgData = new ImageData();
                    imgData.setRawData(thumbnailImage);
                    imageDataCallback.onImageDataReceived(imgData);
//                GLPhotoActivity.startActivityForResult(ImageListActivity.this, cameraIpAddress, fileId, thumbnailImage, true);
                } else {
                    Log.d(TAG, "getThumb: failed to get file data.");
                }
            }
        }).start();
    }


    public void deleteImage(final String fileId, final HttpEventListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CameraConnector camera = new CameraConnector(mContext.getResources().getString(R.string.theta_ip_address));
                camera.deleteFile(fileId, listener);
            }
        }).start();
    }

    public void restartPreview() {
        if (mPreviewSurfaceView != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JpegInputStream mjis = null;
                    final int MAX_RETRY_COUNT = 5;

                    for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
                        try {
                            Log.d(TAG, "startPreview: start Live view");
                            CameraConnector camera = new CameraConnector(cameraIpAddress);
                            InputStream is = camera.getLivePreview();
                            mjis = new JpegInputStream(is);
                            retryCount = MAX_RETRY_COUNT;
                        } catch (IOException e) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        } catch (JSONException e) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    final JpegInputStream jpegInputStream = mjis;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (jpegInputStream != null) {
                                mPreviewSurfaceView.setVisibility(View.VISIBLE);
//                                mPreviewSurfaceView.play();
                                mPreviewSurfaceView.setSource(jpegInputStream);
//                                cameraReadyListener.onCameraReady();
                            } else {
                                Log.d(TAG, "onPostExecute: failed to start live view");
//                                cameraReadyListener.onCameraFailed();
                            }
                        }
                    });
                }
            }).start();
        } else {
            Log.w(TAG, "restartPreview: preview surface view is null");
        }
    }

    //def width = 5376;
//    height = 2688;
    public void changeImageSize(final int width, final int height, final CameraEventListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CameraConnector camera = new CameraConnector(cameraIpAddress);
                String response = camera.setImageSize(width, height);
                if (!response.equals("")) {
                    listener.onImageSizeChanged(width, height);
                }
            }
        }).start();
    }

    public abstract class CameraTask extends AsyncTask<ImageSize, String, Void> {

        private final CameraEventListener mListener;

        public CameraTask(CameraEventListener listener) {
            super();
            this.mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            mListener.onStarted(this);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
        }

        @Override
        protected void onProgressUpdate(String... values) {
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public class GetImageSizeTask extends AsyncTask<Void, String, ImageSize> {
        @Override
        protected void onPreExecute() {
//            btnImageSize.setEnabled(false);
        }

        @Override
        protected ImageSize doInBackground(Void... params) {
            publishProgress("get current image size");
            CameraConnector camera = new CameraConnector(cameraIpAddress);
            ImageSize imageSize = camera.getImageSize();

            return imageSize;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String log : values) {
                Log.d(TAG, "onProgressUpdate: " + log);
            }
        }

        @Override
        protected void onPostExecute(ImageSize imageSize) {
            if (imageSize != null) {
                Log.d(TAG, "onPostExecute: new image size: " + imageSize.name());
                currentImageSize = imageSize;
//                btnImageSize.setEnabled(true);
            } else {
                Log.d(TAG, "onPostExecute: failed to get image size");
            }
        }
    }

    public class LoadObjectListTask extends AsyncTask<Void, String, List<ImageItem>> {


        @Override
        protected void onPreExecute() {
//            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<ImageItem> doInBackground(Void... params) {
            try {
                publishProgress("------");
                publishProgress("connecting to " + cameraIpAddress + "...");
                CameraConnector camera = new CameraConnector(cameraIpAddress);
                if (mStatusListener != null) {
                    mStatusListener.onCameraStatusChanged(CameraStatusChangedListener.STATUS_IDLE);
                }

                CameraInfo cameraInfo = camera.getDeviceInfo();
                publishProgress("connected.");
                publishProgress(cameraInfo.getClass().getSimpleName() + ":<" + cameraInfo.getModel() + ", " + cameraInfo.getDeviceVersion() + ", " + cameraInfo.getSerialNumber()
                        + ">");

                List<ImageItem> imageItems = new ArrayList<>();

                StorageInfo storage = camera.getStorageInfo();
                ImageItem storageCapacity = new ImageItem();
                int freeSpaceInImages = storage.getFreeSpaceInImages();
                int megaByte = 1024 * 1024;
                long freeSpace = storage.getFreeSpaceInBytes() / megaByte;
                long maxSpace = storage.getMaxCapacity() / megaByte;
                storageCapacity.setFileName("Free space: " + freeSpaceInImages + "[shots] (" + freeSpace + "/" + maxSpace + "[MB])");
                imageItems.add(storageCapacity);

                ArrayList<ImageInfo> objects = camera.getList();
                int objectSize = objects.size();

                for (int i = 0; i < objectSize; i++) {
                    ImageItem imageItem = new ImageItem();
                    ImageInfo object = objects.get(i);
                    imageItem.setFileId(object.getFileId());
                    imageItem.setFileSize(object.getFileSize());
                    imageItem.setFileName(object.getFileName());
                    imageItem.setCaptureDate(object.getCaptureDate());
                    publishProgress("<ImageInfo: File ID=" + object.getFileId() + ", filename=" + object.getFileName() + ", capture_date=" + object.getCaptureDate()
                            + ", image_pix_width=" + object.getWidth() + ", image_pix_height=" + object.getHeight() + ", object_format=" + object.getFileFormat()
                            + ">");

                    if (object.getFileFormat().equals(ImageInfo.FILE_FORMAT_CODE_EXIF_JPEG)) {
                        imageItem.setIsPhoto(true);
                        Bitmap thumbnail = camera.getThumb(object.getFileId());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        final byte[] thumbnailImage = baos.toByteArray();
                        imageItem.setThumbnail(thumbnailImage);
                    } else {
                        imageItem.setIsPhoto(false);
                    }
                    imageItems.add(imageItem);

                    publishProgress("getList: " + (i + 1) + "/" + objectSize);
                }
                return imageItems;

            } catch (Throwable throwable) {
                String errorLog = Log.getStackTraceString(throwable);
                publishProgress(errorLog);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String log : values) {
                Log.d(TAG, "onProgressUpdate: " + log);
            }
        }

        @Override
        protected void onPostExecute(List<ImageItem> imageItems) {
            if (imageItems != null) {
//                TextView storageInfo = (TextView) findViewById(R.id.storage_info);
                String info = imageItems.get(0).getFileName();
                imageItems.remove(0);
//                storageInfo.setText(info);
//
//                ImageListArrayAdapter imageListArrayAdapter = new ImageListArrayAdapter(ImageListActivity.this, R.layout.listlayout_object, imageItems);
//                objectList.setAdapter(imageListArrayAdapter);
//                objectList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                        ImageItem selectedItem = (ImageItem) parent.getItemAtPosition(position);
//                        if (selectedItem.isPhoto()) {
//                            byte[] thumbnail = selectedItem.getThumbnail();
//                            String fileId = selectedItem.getFileId();
//                            GLPhotoActivity.startActivityForResult(ImageListActivity.this, cameraIpAddress, fileId, thumbnail, false);
//                        } else {
//                            Toast.makeText(mContext, "This isn't a photo.", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//                objectList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//                    private String mFileId;
//
//                    @Override
//                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                        ImageItem selectedItem = (ImageItem) parent.getItemAtPosition(position);
//                        mFileId = selectedItem.getFileId();
//                        String fileName = selectedItem.getFileName();
//
//                        new AlertDialog.Builder(mContext)
//                                .setTitle(fileName)
//                                .setMessage(R.string.delete_dialog_message)
//                                .setPositiveButton(R.string.dialog_positive_button, new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        DeleteObjectTask deleteTask = new DeleteObjectTask();
//                                        deleteTask.execute(mFileId);
//                                    }
//                                })
//                                .show();
//                        return true;
//                    }
//                });
            } else {
                Log.d(TAG, "failed to get image list");
            }

//            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onCancelled() {
//            progressBar.setVisibility(View.GONE);
        }

    }

    public class DisConnectTask extends AsyncTask<Void, String, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                publishProgress("disconnected.");
                return true;

            } catch (Throwable throwable) {
                String errorLog = Log.getStackTraceString(throwable);
                publishProgress(errorLog);
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String log : values) {
                Log.d(TAG, "DisConnectTask: " + log);
            }
        }
    }
    public interface ImageDataCallback {
        void onImageDataReceived(ImageData imgData);

        void onRequestFailed();
    }
}
