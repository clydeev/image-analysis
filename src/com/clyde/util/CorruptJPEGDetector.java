package com.clyde.util;

import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A simple utility class for detecting corrupt JPEG/JPG images.
 *
 * @author Clyde M. Velasquez
 * @version 0.1
 * @since 12/03/2016
 */
public class CorruptJPEGDetector {
    private File jpegFile;
    private boolean isJpeg = false;
    private boolean isCorrupt = false;
    private boolean isFileComplete = false;

    /**
     * Default constructor that accepts a JPEG file
     *
     * @param jpegFile The JPEG image file
     * @throws IOException If IOException occurs
     * @since 0.1
     */
    public CorruptJPEGDetector(@NotNull File jpegFile) throws IOException {
        this(jpegFile, false);
    }

    /**
     * Constructor that accepts a JPEG file and check
     * file extension is jpg or jpeg
     *
     * @param jpegFile        The JPEG image file
     * @param ignoreExtension If file extension is checked
     * @throws IOException If IOException occurs
     * @since 0.1
     */
    public CorruptJPEGDetector(@NotNull File jpegFile, boolean ignoreExtension) throws IOException {
        // File must not be a directory
        if (jpegFile.isDirectory())
            throw new IOException("File " + jpegFile.getCanonicalPath() + " is a directory!");

        if (!ignoreExtension) {
            if (jpegFile.getName().contains(".")) {
                String ext = jpegFile.getName().substring(jpegFile.getName().lastIndexOf("."));

                if (!(ext.equals(".jpeg") || ext.equals(".jpg")))
                    throw new IOException("Not a jpeg extension");
            }
        }

        if (!jpegFile.exists())
            throw new FileNotFoundException("File " + jpegFile.getCanonicalPath() + " is not found!");

        this.jpegFile = jpegFile;

        initialize();
    }

    /**
     * Initialize values
     *
     * @throws IOException If I/O occurs
     * @since 0.1
     */
    private void initialize() throws IOException {
        setIsJpg();
        setIsFileComplete();
        setIsCorrupt();
    }

    /**
     * Set isJpeg either true or false when
     * Jpeg SOI marker is present
     *
     * @throws IOException If I/O error occurs
     * @since 0.1
     */
    private void setIsJpg() throws IOException {
        byte[] buffer = new byte[20];

        try (RandomAccessFile file = new RandomAccessFile(this.jpegFile, "r")) {
            if (file.length() > 20)
                file.read(buffer, 0, 20);
            else
                file.read(buffer, 0, (int) file.length());
        }
        this.isJpeg = matchBytes(buffer, JPEGMarker.START_OF_IMAGE);
    }

    /**
     * Set isFileComplete either true or false when
     * Jpeg EOI marker is present
     *
     * @throws IOException If I/O error occurs
     * @since 0.1
     */
    private void setIsFileComplete() throws IOException {
        byte[] buffer = new byte[JPEGMarker.END_OF_IMAGE.length];
        try (RandomAccessFile file = new RandomAccessFile(this.jpegFile, "r")) {
            if (file.length() > JPEGMarker.END_OF_IMAGE.length) {
                // Set the file pointer to the last value position minus the length of endBits
                file.seek((int) file.length() - JPEGMarker.END_OF_IMAGE.length);
                file.read(buffer, 0, JPEGMarker.END_OF_IMAGE.length);
            } else
                file.read(buffer, 0, (int) file.length());
        }
        this.isFileComplete = matchEndBytes(buffer, JPEGMarker.END_OF_IMAGE);
    }

    /**
     * Set isCorrupt if distorted pattern matches with end bytes
     *
     * @throws IOException If I/O error occurs
     * @since 0.1
     */
    private void setIsCorrupt() throws IOException {
        if (this.isCorrupt)
            return;

        byte[] buffer = new byte[JPEGMarker.DISTORTED_PATTERN.length];
        try (RandomAccessFile file = new RandomAccessFile(this.jpegFile, "r")) {
            if (file.length() > (JPEGMarker.END_OF_IMAGE.length + JPEGMarker.DISTORTED_PATTERN.length)) {
                file.seek(file.length() - (JPEGMarker.END_OF_IMAGE.length + JPEGMarker.DISTORTED_PATTERN.length));
                file.read(buffer, 0, buffer.length);
            } else
                file.read(buffer, 0, (int) file.length());
        }

        this.isCorrupt = matchBytes(buffer, JPEGMarker.DISTORTED_PATTERN);
    }

    /**
     * Compare bytes
     *
     * @param buffer Byte array to check
     * @param comp   Byte array to compare
     * @return true if match otherwise false
     * @since 0.1
     */
    private boolean matchBytes(byte[] buffer, byte[] comp) {
        for (int i = 0; i < comp.length; i++) {
            if (buffer[i] != comp[i])
                return false;
        }
        return true;
    }

    /**
     * Compare end bytes
     *
     * @param buffer Byte array to check
     * @param comp   Byte array to compare
     * @return true if match otherwise false
     * @since 0.1
     */
    private boolean matchEndBytes(byte[] buffer, byte[] comp) {
        for (int i = 1; i < comp.length; i++) {
            if (buffer[buffer.length - i] != comp[comp.length - i])
                return false;
        }
        return true;
    }

    /**
     * Displays the Hex dump of the Jpeg image
     */
    public void printHexDump() {
        try (RandomAccessFile file = new RandomAccessFile(jpegFile, "r")) {
            int i = 0;

            for (int pos = 0; pos < file.length(); pos++) {
                String hex = Integer.toHexString(file.read()).toUpperCase();
                System.out.print((hex.length() == 1 ? ("0" + hex) : hex) + " ");
                i++;
                if (i == 8)
                    System.out.print(" ");

                if (i == 16) {
                    i = 0;
                    System.out.println();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Getter for isJpeg
     *
     * @return true if Jpeg otherwise false
     * @since 0.1
     */
    public boolean isJpeg() {
        return isJpeg;
    }

    /**
     * Getter for isFileComplete
     *
     * @return true if EOI marker is found
     * @since 0.1
     */
    public boolean isFileComplete() {
        return isFileComplete;
    }

    /**
     * Getter for isCorrupt
     *
     * @return true if image is corrupt otherwise false
     * @since 0.1
     */
    public boolean isCorrupt() {
        return isCorrupt;
    }
}