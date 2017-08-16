package com.telenav.spherical.view;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Input stream for motion JPEG data
 */
public class JpegInputStream extends DataInputStream {
    private final static String CONTENT_LENGTH = "Content-Length";

    private final static int HEADER_MAX_LENGTH = 100;

    private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;

    private final byte[] SOI_MARKER = {(byte) 0xFF, (byte) 0xD8};

    private final byte[] EOF_MARKER = {(byte) 0xFF, (byte) 0xD9};

    /**
     * Constructor
     * @param inputStream Input stream for receiving data
     */
    public JpegInputStream(InputStream inputStream) {
        super(new BufferedInputStream(inputStream, FRAME_MAX_LENGTH));
    }

    /**
     * Acquire end position of specified character string
     * @param dataInputStream Input stream for receiving data
     * @param sequence Specified character string
     * @return End position of specified character string
     * @throws IOException
     */
    private int getEndOfSequence(DataInputStream dataInputStream, byte[] sequence) throws IOException {
        int sequenceIndex = 0;
        byte readByteData;
        for (int index = 0; index < FRAME_MAX_LENGTH; index++) {
            readByteData = (byte) dataInputStream.readUnsignedByte();
            if (readByteData == sequence[sequenceIndex]) {
                sequenceIndex++;
                if (sequenceIndex == sequence.length) {
                    return index + 1;
                }
            } else {
                sequenceIndex = 0;
            }
        }
        return -1;
    }

    /**
     * Acquire start position of specified character string
     * @param dataInputStream Input stream for receiving data
     * @param sequence Specified character string
     * @return Start position of specified character string
     * @throws IOException
     */
    private int getStartOfSequence(DataInputStream dataInputStream, byte[] sequence) throws IOException {
        int endIndex = getEndOfSequence(dataInputStream, sequence);
        return (endIndex < 0) ? (-1) : (endIndex - sequence.length);
    }

    /**
     * Acquire data length from header
     * @param headerByteData Header data
     * @return Data length
     * @throws IOException
     * @throws NumberFormatException
     */
    private int parseContentLength(byte[] headerByteData) throws IOException, NumberFormatException {
        ByteArrayInputStream bais = new ByteArrayInputStream(headerByteData);
        Properties properties = new Properties();
        properties.load(bais);
        return Integer.parseInt(properties.getProperty(CONTENT_LENGTH));
    }

    /**
     * Acquire image data for 1 frame
     * @return Image data for 1 frame
     * @throws IOException
     */
    public Bitmap readMJpegFrame() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLength = getStartOfSequence(this, SOI_MARKER);
        int contentLength;
        reset();

        byte[] headerData = new byte[headerLength];
        readFully(headerData);
        try {
            contentLength = parseContentLength(headerData);
        } catch (NumberFormatException e) {
            e.getStackTrace();
            contentLength = getEndOfSequence(this, EOF_MARKER);
        }
        reset();

        byte[] frameData = new byte[contentLength];
        skipBytes(headerLength);
        readFully(frameData);

        return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData));
    }
}
