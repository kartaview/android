package com.telenav.spherical.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.telenav.spherical.model.ImageSize;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP connection to device
 */
public class CameraConnector {

  private final static long CHECK_STATUS_PERIOD_MS = 50;

  private static final String TAG = "CameraConnector";

  private String mIpAddress = null;

  private String mSessionId = null;

  private String mContinuationToken = null;

  private String mFingerPrint = null;

  private Timer mCheckStatusTimer = null;

  private HttpEventListener mHttpEventListener = null;

  /**
   * Constructor
   *
   * @param cameraIpAddress IP address of connection destination
   */
  public CameraConnector(String cameraIpAddress) {
    mIpAddress = cameraIpAddress;
  }

  /**
   * Connect to device
   *
   * @return Session ID (null is returned if the connection fails)
   */
  private String connect() {
    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData;
    String sessionId = null;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.startSession");

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");

      if (status.equals("done")) {
        JSONObject results = output.getJSONObject("results");
        sessionId = results.getString("sessionId");
      } else if (status.equals("error")) {
        JSONObject errors = output.getJSONObject("error");
        String errorCode = errors.getString("code");
        if (errorCode.equals("invalidSessionId")) {
          sessionId = null;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return sessionId;
  }

  /**
   * Acquire storage information of device
   *
   * @return Storage information
   */
  public StorageInfo getStorageInfo() {
    mSessionId = connect();

    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData;
    StorageInfo storageInfo = new StorageInfo();
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.getOptions");
      JSONObject parameters = new JSONObject();
      parameters.put("sessionId", mSessionId);
      JSONArray optionNames = new JSONArray();
      optionNames.put("remainingPictures");
      optionNames.put("remainingSpace");
      optionNames.put("totalSpace");
      parameters.put("optionNames", optionNames);
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");

      if (status.equals("done")) {
        JSONObject results = output.getJSONObject("results");
        JSONObject options = results.getJSONObject("options");
        int remainingPictures = options.getInt("remainingPictures");
        storageInfo.setFreeSpaceInImages(remainingPictures);

        long remainingSpace = options.getLong("remainingSpace");
        storageInfo.setFreeSpaceInBytes(remainingSpace);

        long totalSpace = options.getLong("totalSpace");
        storageInfo.setMaxCapacity(totalSpace);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return storageInfo;
  }

  /**
   * Acquire device information
   *
   * @return Device information
   */
  public CameraInfo getDeviceInfo() {
    HttpURLConnection getConnection = createHttpConnection("GET", "/osc/info");
    String responseData;
    CameraInfo cameraInfo = new CameraInfo();
    InputStream is = null;

    try {
      // send HTTP GET
      // this protocol has no input.
      getConnection.connect();

      is = getConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);

      String model = output.getString("model");
      cameraInfo.setModel(model);

      String version = output.getString("firmwareVersion");
      cameraInfo.setDeviceVersion(version);

      String serialNumber = output.getString("serialNumber");
      cameraInfo.setSerialNumber(serialNumber);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return cameraInfo;
  }

  /**
   * Acquire list of media files on device
   *
   * @return Media file list
   */
  public ArrayList<ImageInfo> getList() {
    ArrayList<ImageInfo> imageInfos = new ArrayList<>();

    for (int continuation = 0; continuation < 3; continuation++) {
      ArrayList<ImageInfo> receivedImageInfo = getListInternal(10, mContinuationToken);
      imageInfos.addAll(receivedImageInfo);
      if (mContinuationToken == null) {
        break;
      }
    }

    return imageInfos;
  }

  /**
   * Acquire media file list (limited number of items)
   *
   * @param maxReceiveEntry Maximum number of files that can be acquired at once
   * @param token Set the previously acquired token to continue. Set null if acquiring for the first time.
   *
   * @return List of specified number of media files
   */
  private ArrayList<ImageInfo> getListInternal(int maxReceiveEntry, String token) {
    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData;
    ArrayList<ImageInfo> imageInfos = new ArrayList<>();
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera._listAll");
      JSONObject parameters = new JSONObject();
      parameters.put("entryCount", maxReceiveEntry);
      if (token != null) {
        parameters.put("continuationToken", token);
      }
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");

      if (status.equals("done")) {
        JSONObject results = output.getJSONObject("results");
        JSONArray entries = results.getJSONArray("entries");
        int entrySize = entries.length();
        try {
          mContinuationToken = results.getString("continuationToken");
        } catch (JSONException e) {
          mContinuationToken = null;
        }

        for (int index = 0; index < entrySize; index++) {
          JSONObject entry = entries.getJSONObject(index);
          ImageInfo imageInfo = new ImageInfo();

          String name = entry.getString("name");
          imageInfo.setFileName(name);

          String id = entry.getString("uri");
          imageInfo.setFileId(id);

          long size = Long.parseLong(entry.getString("size"));
          imageInfo.setFileSize(size);

          String date = entry.getString("dateTimeZone");
          imageInfo.setCaptureDate(date);

          int width = entry.getInt("width");
          imageInfo.setWidth(width);

          int height = entry.getInt("height");
          imageInfo.setHeight(height);

          try {
            entry.getInt("recordTime");
            imageInfo.setFileFormat(ImageInfo.FILE_FORMAT_CODE_EXIF_MPEG);
          } catch (JSONException e) {
            imageInfo.setFileFormat(ImageInfo.FILE_FORMAT_CODE_EXIF_JPEG);
          }

          imageInfos.add(imageInfo);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return imageInfos;
  }

  /**
   * Acquire thumbnail image
   *
   * @param fileId File ID
   *
   * @return Thumbnail (null is returned if acquisition fails)
   */
  public Bitmap getThumb(String fileId) {
    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    Bitmap thumbnail = null;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.getImage");
      JSONObject parameters = new JSONObject();
      parameters.put("fileUri", fileId);
      parameters.put("_type", "thumb");
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      BufferedInputStream bis = new BufferedInputStream(is);
      thumbnail = BitmapFactory.decodeStream(bis);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return thumbnail;
  }

  /**
   * Take photo<p>
   * After shooting, the status is checked for each {@link CameraConnector#CHECK_STATUS_PERIOD_MS} and the listener notifies you of the
   * status.
   *
   * @param listener Post-shooting event listener
   *
   * @return Shooting request results
   */
  public ShootResult takePicture(HttpEventListener listener) {
    ShootResult result = ShootResult.FAIL_DEVICE_BUSY;

    mSessionId = connect();
    if (mSessionId == null) {
      listener.onError("cannot get to start session");
      result = ShootResult.FAIL_DEVICE_BUSY;
      return result;
    }

    // set capture mode to image
    String errorMessage = setImageCaptureMode(mSessionId);
    if (errorMessage != null) {
      listener.onError(errorMessage);
      result = ShootResult.FAIL_DEVICE_BUSY;
      return result;
    }

    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData;
    mHttpEventListener = listener;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.takePicture");
      JSONObject parameters = new JSONObject();
      parameters.put("sessionId", mSessionId);
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");
      String commandId = output.getString("id");

      if (status.equals("inProgress")) {
        mCheckStatusTimer = new Timer(true);
        CapturedTimerTask capturedTimerTask = new CapturedTimerTask();
        capturedTimerTask.setCommandId(commandId);
        mCheckStatusTimer.scheduleAtFixedRate(capturedTimerTask, CHECK_STATUS_PERIOD_MS, CHECK_STATUS_PERIOD_MS);
        result = ShootResult.SUCCESS;
      } else if (status.equals("done")) {
        JSONObject results = output.getJSONObject("results");
        String lastFileId = results.getString("fileUri");

        mHttpEventListener.onObjectChanged(lastFileId);
        mHttpEventListener.onCompleted();
        result = ShootResult.SUCCESS;
      }
    } catch (IOException e) {
      e.printStackTrace();
      result = ShootResult.FAIL_DEVICE_BUSY;
    } catch (JSONException e) {
      e.printStackTrace();
      result = ShootResult.FAIL_DEVICE_BUSY;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return result;
  }

  /**
   * Check still image shooting status
   *
   * @param commandId Command ID for shooting still images
   *
   * @return ID of saved file (null is returned if the file is not saved)
   */
  private String checkCaptureStatus(String commandId) {
    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/status");
    JSONObject input = new JSONObject();
    String responseData;
    String capturedFileId = null;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("id", commandId);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");

      if (status.equals("done")) {
        JSONObject results = output.getJSONObject("results");
        capturedFileId = results.getString("fileUri");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return capturedFileId;
  }

  /**
   * Acquire raw data of specified image
   *
   * @param fileId File ID
   * @param listener Listener for receiving received data count
   *
   * @return Image data
   */
  public ImageData getImage(String fileId, DownloadProgressListener listener) {
    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    ImageData imageData = new ImageData();
    int totalSize = 0;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.getImage");
      JSONObject parameters = new JSONObject();
      parameters.put("fileUri", fileId);
      parameters.put("_type", "full");
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      totalSize = postConnection.getContentLength();
      if (listener != null) {
        listener.onTotalSize(totalSize);
      }
      is = postConnection.getInputStream();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buffer = new byte[2048];
      int length;

      while ((length = is.read(buffer)) >= 0) {
        baos.write(buffer, 0, length);
        if (listener != null) {
          listener.onDataReceived(length);
        }
      }
      byte[] rawData = baos.toByteArray();
      imageData.setRawData(rawData);

      XMP xmp = new XMP(rawData);
      imageData.setPitch(xmp.getPosePitchDegrees());
      imageData.setRoll(xmp.getPoseRollDegrees());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return imageData;
  }

  /**
   * Acquire live view stream
   *
   * @return Stream for receiving data
   *
   * @throws IOException
   */
  public InputStream getLivePreview() throws IOException, JSONException {
    mSessionId = connect();

    // set capture mode to image
    setImageCaptureMode(mSessionId);

    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera._getLivePreview");
      JSONObject parameters = new JSONObject();
      parameters.put("sessionId", mSessionId);
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
    } catch (IOException e) {
      e.printStackTrace();
      String errorMessage = null;
      InputStream es = postConnection.getErrorStream();
      try {
        if (es != null) {
          String errorData = InputStreamToString(es);
          Log.d(TAG, "getLivePreview: " + errorData);
          JSONObject output = new JSONObject(errorData);
          JSONObject errors = output.getJSONObject("error");
          errorMessage = errors.getString("message");
        }
      } catch (IOException e1) {
        e1.printStackTrace();
      } catch (JSONException e1) {
        e1.printStackTrace();
      } finally {
        if (es != null) {
          try {
            es.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
      throw e;
    } catch (JSONException e) {
      e.printStackTrace();
      throw e;
    }

    return is;
  }

  /**
   * Delete specified file
   *
   * @param deletedFileId File ID
   * @param listener Listener for receiving deletion results
   */
  public void deleteFile(String deletedFileId, HttpEventListener listener) {
    mSessionId = connect();

    // set capture mode to image
    String errorMessage = setImageCaptureMode(mSessionId);
    if (errorMessage != null) {
      listener.onError(errorMessage);
      return;
    }

    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData;
    mHttpEventListener = listener;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.delete");
      JSONObject parameters = new JSONObject();
      parameters.put("fileUri", deletedFileId);
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");

      if (status.equals("inProgress")) {
        getState();
        mCheckStatusTimer = new Timer(true);
        DeletedTimerTask deletedTimerTask = new DeletedTimerTask();
        deletedTimerTask.setDeletedFileId(deletedFileId);
        mCheckStatusTimer.scheduleAtFixedRate(deletedTimerTask, CHECK_STATUS_PERIOD_MS, CHECK_STATUS_PERIOD_MS);
      } else if (status.equals("done")) {
        mHttpEventListener.onObjectChanged(deletedFileId);
        mHttpEventListener.onCompleted();
        mFingerPrint = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Specify shooting size
   * Shooting size
   */
  public String setImageSize(int width, int height) {
    //        switch (imageSize) {
    //            case IMAGE_SIZE_2048x1024:
    //                width = 2048;
    //                height = 1024;
    //                break;
    //            default:
    //            case IMAGE_SIZE_5376x2688:
    //
    //                break;
    //        }
    mSessionId = connect();

    // set capture mode to image
    setImageCaptureMode(mSessionId);

    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData = "";
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.setOptions");
      JSONObject parameters = new JSONObject();
      parameters.put("sessionId", mSessionId);
      JSONObject options = new JSONObject();
      JSONObject fileFormat = new JSONObject();
      fileFormat.put("type", "jpeg");
      fileFormat.put("width", width);
      fileFormat.put("height", height);
      options.put("fileFormat", fileFormat);
      parameters.put("options", options);
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return responseData;
  }

  /**
   * Acquire currently set shooting size
   *
   * @return Shooting size (null is returned if acquisition fails)
   */
  public ImageSize getImageSize() {
    mSessionId = connect();

    // set capture mode to image
    setImageCaptureMode(mSessionId);

    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData;
    ImageSize imageSize = null;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.getOptions");
      JSONObject parameters = new JSONObject();
      parameters.put("sessionId", mSessionId);
      JSONArray optionNames = new JSONArray();
      optionNames.put("fileFormat");
      parameters.put("optionNames", optionNames);
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");

      if (status.equals("done")) {
        JSONObject results = output.getJSONObject("results");
        JSONObject options = results.getJSONObject("options");
        JSONObject fileFormat = options.getJSONObject("fileFormat");
        int width = fileFormat.getInt("width");

        if (width == 2048) {
          imageSize = ImageSize.IMAGE_SIZE_2048x1024;
        } else {
          imageSize = ImageSize.IMAGE_SIZE_5376x2688;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return imageSize;
  }

  /**
   * Set still image as shooting mode
   *
   * @param sessionId Session ID
   *
   * @return Error message (null is returned if successful)
   */
  private String setImageCaptureMode(String sessionId) {
    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/commands/execute");
    JSONObject input = new JSONObject();
    String responseData;
    String errorMessage = null;
    InputStream is = null;

    try {
      // send HTTP POST
      input.put("name", "camera.setOptions");
      JSONObject parameters = new JSONObject();
      parameters.put("sessionId", sessionId);
      JSONObject options = new JSONObject();
      options.put("captureMode", "image");
      parameters.put("options", options);
      input.put("parameters", parameters);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String status = output.getString("state");

      if (status.equals("error")) {
        JSONObject errors = output.getJSONObject("error");
        errorMessage = errors.getString("message");
      }
    } catch (IOException e) {
      e.printStackTrace();
      errorMessage = e.toString();
      InputStream es = postConnection.getErrorStream();
      try {
        if (es != null) {
          String errorData = InputStreamToString(es);
          JSONObject output = new JSONObject(errorData);
          JSONObject errors = output.getJSONObject("error");
          errorMessage = errors.getString("message");
        }
      } catch (IOException e1) {
        e1.printStackTrace();
      } catch (JSONException e1) {
        e1.printStackTrace();
      } finally {
        if (es != null) {
          try {
            es.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
      errorMessage = e.toString();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return errorMessage;
  }

  /**
   * Acquire device status
   *
   * @return Last saved file
   */
  private String getState() {
    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/state");
    String responseData;
    String lastFile = "";
    InputStream is = null;

    try {
      // send HTTP POST
      postConnection.connect();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      mFingerPrint = output.getString("fingerprint");
      JSONObject status = output.getJSONObject("state");
      lastFile = status.getString("_latestFileUri");
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return lastFile;
  }

  /**
   * Check for updates to device status
   *
   * @return true:Update available, false:No update available
   */
  private boolean isUpdate() {
    boolean update = false;
    InputStream is = null;

    if (mFingerPrint == null) {
      return update;
    }

    HttpURLConnection postConnection = createHttpConnection("POST", "/osc/checkForUpdates");
    JSONObject input = new JSONObject();
    String responseData = null;

    try {
      // send HTTP POST
      input.put("stateFingerprint", mFingerPrint);

      OutputStream os = postConnection.getOutputStream();
      os.write(input.toString().getBytes());
      postConnection.connect();
      os.flush();
      os.close();

      is = postConnection.getInputStream();
      responseData = InputStreamToString(is);

      // parse JSON data
      JSONObject output = new JSONObject(responseData);
      String currentFingerPrint = output.getString("stateFingerprint");
      if (!currentFingerPrint.equals(mFingerPrint)) {
        mFingerPrint = currentFingerPrint;
        update = true;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return update;
  }

  /**
   * Generate connection destination URL
   *
   * @param path Path
   *
   * @return URL
   */
  private String createUrl(String path) {
    String sb = "http://" + mIpAddress + path;

    return sb;
  }

  /**
   * Generate HTTP connection
   *
   * @param method Method
   * @param path Path
   *
   * @return HTTP Connection instance
   */
  private HttpURLConnection createHttpConnection(String method, String path) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(createUrl(path));
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
      connection.setRequestProperty("Accept", "application/json");
      connection.setDoInput(true);

      if (method.equals("POST")) {
        connection.setRequestMethod(method);
        connection.setDoOutput(true);
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return connection;
  }

  /**
   * Convert input stream to string
   *
   * @param is InputStream
   *
   * @return String
   *
   * @throws IOException IO error
   */
  private String InputStreamToString(InputStream is) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    StringBuilder sb = new StringBuilder();
    String lineData;
    while ((lineData = br.readLine()) != null) {
      sb.append(lineData);
    }
    br.close();
    return sb.toString();
  }

  public enum ShootResult {
    SUCCESS,
    FAIL_CAMERA_DISCONNECTED,
    FAIL_STORE_FULL,
    FAIL_DEVICE_BUSY
  }

  private class CapturedTimerTask extends TimerTask {

    private String mCommandId;

    public void setCommandId(String commandId) {
      mCommandId = commandId;
    }

    @Override
    public void run() {
      String capturedFileId = checkCaptureStatus(mCommandId);

      if (capturedFileId != null) {
        mHttpEventListener.onCheckStatus(true);
        mCheckStatusTimer.cancel();
        mHttpEventListener.onObjectChanged(capturedFileId);
        mHttpEventListener.onCompleted();
      } else {
        mHttpEventListener.onCheckStatus(false);
      }
    }
  }

  /**
   * Status check class for file deletion
   */
  private class DeletedTimerTask extends TimerTask {

    private String mDeletedFileId = null;

    public void setDeletedFileId(String deletedFileId) {
      mDeletedFileId = deletedFileId;
    }

    @Override
    public void run() {
      boolean update = isUpdate();
      mHttpEventListener.onCheckStatus(update);
      if (update) {
        mCheckStatusTimer.cancel();
        getState();
        mHttpEventListener.onObjectChanged(mDeletedFileId);
        mHttpEventListener.onCompleted();
        mFingerPrint = null;
      }
    }
  }
}
