package com.telenav.osv.external.glview.model;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import android.opengl.GLES20;

/**
 * UV sphere model class
 */
public class UVSphere {

    private final int COORDS_PER_VERTEX = 3;

    private final int TEXTURE_COORDS_PER_VERTEX = 2;

    private final int vertexStride = COORDS_PER_VERTEX * 4;

    private final int textureStride = TEXTURE_COORDS_PER_VERTEX * 4;

    private int mStrips;

    private int mStripePointsNum;

    private ArrayList<FloatBuffer> mVertices;

    private ArrayList<FloatBuffer> mTextureCoords;

    private UVSphere() {
        mVertices = new ArrayList<FloatBuffer>();
        mTextureCoords = new ArrayList<FloatBuffer>();
    }

    /**
     * Constructor
     * Sphere is displayed according to the number of partitions.
     * The longitude is created from the number of partitions which is half the number of
     * latitude lines 1 and the number of polygons which is double the number of
     * partitions set in the radius specified as the origin coordinates.
     * @param radius Radius
     * @param divide Number of partitions (must be an even number)
     * @param eastSide true is east side of sphere, otherwise is west side
     */
    public UVSphere(float radius, int divide, boolean eastSide) {
        this();

        if (radius <= 0 || divide <= 0 || 0 != (divide % 2)) {
            throw new IllegalArgumentException();
        }

        mStrips = divide / 2;
        mStripePointsNum = (divide + 1) * 2;
        makeSphereVertices(radius, divide, eastSide);
    }


    /**
     * Sphere drawing method
     * @param mPositionHandle Handler value tied to gl_Position in vertex shader
     * @param mUVHandle Handler value tied to the UV coordinates provided to the fragment shader via the varyig variable
     */
    public void draw(int mPositionHandle, int mUVHandle) {

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mUVHandle);

        for (int i = 0; i < this.mStrips; i++) {
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mVertices.get(i));
            GLES20.glVertexAttribPointer(mUVHandle, TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, textureStride, mTextureCoords.get(i));


            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mStripePointsNum);
        }

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mUVHandle);

        return;
    }


    private void makeSphereVertices(float radius, int divide, boolean eastSide) {

        float altitude = 0.0f;
        float altitudeDelta = 0.0f;
        float azimuth = 0.0f;
        float ex = 0.0f;
        float ey = 0.0f;
        float ez = 0.0f;
        Double startPoint;

        if (eastSide) {
            startPoint = 0.0d;
        } else {
            startPoint = Math.PI;
        }

        for (int i = 0; i < divide / 2; ++i) {
            altitude = (float) (Math.PI / 2.0 - i * (Math.PI * 2) / divide);
            altitudeDelta = (float) (Math.PI / 2.0 - (i + 1) * (Math.PI * 2) / divide);


            float[] vertices = new float[divide * 6 + 6];
            float[] texCoords = new float[divide * 4 + 4];

            for (int j = 0; j <= divide / 2; ++j) {
                azimuth = (float) (startPoint - (j * (Math.PI * 2) / divide));

                // first point
                ex = (float) (Math.cos(altitudeDelta) * Math.cos(azimuth));
                ey = (float) Math.sin(altitudeDelta);
                ez = (float) (Math.cos(altitudeDelta) * Math.sin(azimuth));

                vertices[6 * j + 0] = radius * ex;
                vertices[6 * j + 1] = radius * ey;
                vertices[6 * j + 2] = radius * ez;

                texCoords[4 * j + 0] = 1.0f - (2 * j / (float) divide);
                texCoords[4 * j + 1] = 2 * (i + 1) / (float) divide;

                // second point
                ex = (float) (Math.cos(altitude) * Math.cos(azimuth));
                ey = (float) Math.sin(altitude);
                ez = (float) (Math.cos(altitude) * Math.sin(azimuth));

                vertices[6 * j + 3] = radius * ex;
                vertices[6 * j + 4] = radius * ey;
                vertices[6 * j + 5] = radius * ez;

                texCoords[4 * j + 2] = 1.0f - (2 * j / (float) divide);
                texCoords[4 * j + 3] = 2 * i / (float) divide;
            }

            mVertices.add(makeFloatBufferFromArray(vertices));
            mTextureCoords.add(makeFloatBufferFromArray(texCoords));
        }

        return;
    }


    private FloatBuffer makeFloatBufferFromArray(float[] array) {

        FloatBuffer fb = ByteBuffer.allocateDirect(array.length * Float.SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(array);
        fb.position(0);

        return fb;
    }


}
