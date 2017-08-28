package com.telenav.spherical.glview;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import com.telenav.spherical.glview.model.UVSphere;
import com.telenav.spherical.model.Constants;
import com.telenav.spherical.model.Photo;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer class for photo display
 */
public class GLRenderer implements Renderer {

  private static final float Z_NEAR = 0.1f;

  private static final float Z_FAR = 100.0f;

  private final String VSHADER_SRC =
      "attribute vec4 aPosition;\n" + "attribute vec2 aUV;\n" + "uniform mat4 uProjection;\n" + "uniform mat4 uView;\n" +
          "uniform mat4 uModel;\n" + "varying vec2 vUV;\n" + "void main() {\n" +
          "  gl_Position = uProjection * uView * uModel * aPosition;\n" + "  vUV = aUV;\n" + "}\n";

  private final String FSHADER_SRC =
      "precision mediump float;\n" + "varying vec2 vUV;\n" + "uniform sampler2D uTex;\n" + "void main() {\n" +
          "  gl_FragColor = texture2D(uTex, vUV);\n" + "}\n";

  private final float[] mProjectionMatrix = new float[16];

  private final float[] mViewMatrix = new float[16];

  private final float[] mModelMatrix = new float[16];

  private UVSphere mEastShell = null;

  private UVSphere mWestShell = null;

  private double mRotationAngleY;

  private double mRotationAngleXZ;

  private Photo mTexture;

  private boolean mTextureUpdate = false;

  private float mScreenAspectRatio;

  private float mCameraPosX = 0.0f;

  private float mCameraPosY = 0.0f;

  private float mCameraPosZ = 0.0f;

  private float mCameraDirectionX = 0.0f;

  private float mCameraDirectionY = 0.0f;

  private float mCameraDirectionZ = 1.0f;

  private float mCameraFovDegree = 45;

  private int[] mTextures = new int[2];

  private int mPositionHandle;

  private int mProjectionMatrixHandle;

  private int mViewMatrixHandle;

  private int mUVHandle;

  private int mTexHandle;

  private int mModelMatrixHandle;

  /**
   * Constructor
   */
  public GLRenderer() {
    mEastShell = new UVSphere(Constants.TEXTURE_SHELL_RADIUS, Constants.SHELL_DIVIDES, true);
    mWestShell = new UVSphere(Constants.TEXTURE_SHELL_RADIUS, Constants.SHELL_DIVIDES, false);

    mRotationAngleY = 0.0f;
    mRotationAngleXZ = 0.0f;
  }

  /**
   * GL error judgment method for debugging
   *
   * @param TAG TAG output character string
   * @param glOperation Message output character string
   */
  public static void checkGlError(String TAG, String glOperation) {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
      Log.e(TAG, glOperation + ": glError " + error);
      throw new RuntimeException(glOperation + ": glError " + error);
    }
    return;
  }

  /**
   * onDrawFrame Method
   *
   * @param gl GLObject (not used)
   */
  @Override
  public void onDrawFrame(final GL10 gl) {

    mCameraDirectionX = (float) (Math.cos(mRotationAngleXZ) * Math.cos(mRotationAngleY));
    mCameraDirectionZ = (float) (Math.sin(mRotationAngleXZ) * Math.cos(mRotationAngleY));
    mCameraDirectionY = (float) Math.sin(mRotationAngleY);

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.setIdentityM(mViewMatrix, 0);
    Matrix.setIdentityM(mProjectionMatrix, 0);

    if (mTextureUpdate && mTexture != null && !mTexture.getPhoto().isRecycled()) {
      Log.d("", "load texture1");
      loadTexture(mTexture.getPhoto());
      mTextureUpdate = false;
    }

    Matrix.setLookAtM(mViewMatrix, 0, mCameraPosX, mCameraPosY, mCameraPosZ, mCameraDirectionX, mCameraDirectionY, mCameraDirectionZ, 0.0f,
                      1.0f, 0.0f);
    Matrix.perspectiveM(mProjectionMatrix, 0, mCameraFovDegree, mScreenAspectRatio, Z_NEAR, Z_FAR);

    if (mTexture != null && mTexture.getElevetionAngle() != null) {
      float elevationAngle = mTexture.getElevetionAngle().floatValue();
      Matrix.rotateM(mModelMatrix, 0, elevationAngle, 0, 0, 1);
    }
    if (mTexture != null && mTexture.getHorizontalAngle() != null) {
      float horizontalAngle = mTexture.getHorizontalAngle().floatValue();
      Matrix.rotateM(mModelMatrix, 0, horizontalAngle, 1, 0, 0);
    }

    GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
    GLES20.glUniformMatrix4fv(mProjectionMatrixHandle, 1, false, mProjectionMatrix, 0);
    GLES20.glUniformMatrix4fv(mViewMatrixHandle, 1, false, mViewMatrix, 0);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
    GLES20.glUniform1i(mTexHandle, 0);

    mEastShell.draw(mPositionHandle, mUVHandle);

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[1]);
    GLES20.glUniform1i(mTexHandle, 0);

    mWestShell.draw(mPositionHandle, mUVHandle);

    return;
  }

  /**
   * onSurfaceChanged Method
   *
   * @param gl GLObject (not used)
   * @param width Screen width
   * @param height Screen height
   */
  @Override
  public void onSurfaceChanged(final GL10 gl, final int width, final int height) {

    int _height = height;
    mScreenAspectRatio = (float) width / (float) (_height == 0 ? 1 : _height);

    GLES20.glViewport(0, 0, width, _height);

    Matrix.setLookAtM(mViewMatrix, 0, mCameraPosX, mCameraPosY, mCameraPosZ, mCameraDirectionX, mCameraDirectionY, mCameraDirectionZ, 0.0f,
                      1.0f, 0.0f);
    Matrix.perspectiveM(mProjectionMatrix, 0, mCameraFovDegree, mScreenAspectRatio, Z_NEAR, Z_FAR);

    return;
  }

  /**
   * onSurfaceCreated Method
   *
   * @param gl GLObject (not used)
   * @param config EGL Setting Object
   */
  @Override
  public void onSurfaceCreated(final GL10 gl, final EGLConfig config) {

    int vShader;
    int fShader;
    int program;

    vShader = loadShader(GLES20.GL_VERTEX_SHADER, VSHADER_SRC);
    fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FSHADER_SRC);

    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vShader);
    GLES20.glAttachShader(program, fShader);
    GLES20.glLinkProgram(program);

    GLES20.glUseProgram(program);

    mPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
    mUVHandle = GLES20.glGetAttribLocation(program, "aUV");
    mProjectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjection");
    mViewMatrixHandle = GLES20.glGetUniformLocation(program, "uView");
    mTexHandle = GLES20.glGetUniformLocation(program, "uTex");
    mModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModel");

    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    return;
  }

  /**
   * Rotation process method
   *
   * @param xz X axis direction rotation value
   * @param y Y axis direction rotation value
   */
  public void rotate(float xz, float y) {
    mRotationAngleXZ += xz;
    mRotationAngleY += y;
    if (mRotationAngleY > (Math.PI / 2)) {
      mRotationAngleY = (Math.PI / 2);
    }
    if (mRotationAngleY < -(Math.PI / 2)) {
      mRotationAngleY = -(Math.PI / 2);
    }
    return;
  }

  /**
   * Zoom in/Zoom out method
   *
   * @param ratio Scale value: Zoom in process performed if the value is 1.0 or more; zoom out process is performed if the value is less
   * than 1.0
   */
  public void scale(float ratio) {

    if (ratio < 1.0) {
      mCameraFovDegree = mCameraFovDegree * (Constants.SCALE_RATIO_TICK_EXPANSION);
      if (mCameraFovDegree > Constants.CAMERA_FOV_DEGREE_MAX) {
        mCameraFovDegree = Constants.CAMERA_FOV_DEGREE_MAX;
      }
    } else {
      mCameraFovDegree = mCameraFovDegree * (Constants.SCALE_RATIO_TICK_REDUCTION);
      if (mCameraFovDegree < Constants.CAMERA_FOV_DEGREE_MIN) {
        mCameraFovDegree = Constants.CAMERA_FOV_DEGREE_MIN;
      }
    }

    return;
  }

  /**
   * Acquires the set texture
   *
   * @return Photo object for texture
   */
  public Photo getTexture() {
    return mTexture;
  }

  /**
   * Sets the texture for the sphere
   *
   * @param texture Photo object for texture
   */
  public void setTexture(Photo texture) {
    mTexture = texture;
    mTextureUpdate = true;
    return;
  }

  /**
   * Texture setting method
   *
   * @param texture Setting texture
   */
  public void loadTexture(final Bitmap texture) {

    final Bitmap bitmap = texture;
    int dividedWidth = bitmap.getWidth() / 2;

    GLES20.glGenTextures(2, mTextures, 0);

    for (int textureIndex = 0; textureIndex < 2; textureIndex++) {
      GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[textureIndex]);

      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

      Bitmap dividedBitmap = Bitmap.createBitmap(bitmap, (dividedWidth * textureIndex), 0, dividedWidth, bitmap.getHeight());

      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, dividedBitmap, 0);
      dividedBitmap.recycle();
    }

    return;
  }

  private int loadShader(int type, String shaderCode) {

    int shader = GLES20.glCreateShader(type);

    GLES20.glShaderSource(shader, shaderCode);
    GLES20.glCompileShader(shader);

    return shader;
  }
}