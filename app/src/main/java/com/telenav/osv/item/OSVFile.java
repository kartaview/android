package com.telenav.osv.item;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kalman on 1/13/16.
 */
public class OSVFile extends File {
    private static final String TAG = "OSVFile";

    public OSVFile(File dir, String name) {
        super(dir, name);
    }

    public OSVFile(String path) {
        super(path);
    }

    public OSVFile(String dirPath, String name) {
        super(dirPath, name);
    }

    public OSVFile(URI uri) {
        super(uri);
    }

    @Override
    public OSVFile[] listFiles() {
        return filenamesToFiles(list());
    }

    /**
     * Converts a String[] containing filenames to a File[].
     * Note that the filenames must not contain slashes.
     * This method is to remove duplication in the implementation
     * of File.list's overloads.
     */
    private OSVFile[] filenamesToFiles(String[] filenames) {
        if (filenames == null) {
            return new OSVFile[0];
        }
        int count = filenames.length;
        OSVFile[] result = new OSVFile[count];
        for (int i = 0; i < count; ++i) {
            result[i] = new OSVFile(this, filenames[i]);
        }
        return result;
    }

    @Override
    public OSVFile[] listFiles(FilenameFilter filter) {
        return filenamesToFiles(list(filter));
    }

    @Override
    public OSVFile[] listFiles(FileFilter filter) {
        OSVFile[] files = listFiles();
        if (filter == null || files == null) {
            return files;
        }
        List<OSVFile> result = new ArrayList<>(files.length);
        for (OSVFile file : files) {
            if (filter.accept(file)) {
                result.add(file);
            }
        }
        return result.toArray(new OSVFile[result.size()]);
    }

    @Override
    public OSVFile getParentFile() {
        File parent = super.getParentFile();
        if (parent == null) {
            parent = new File(getParent(), getName());
        }
        return new OSVFile(parent.getParent(), parent.getName());
    }

    @Override
    public boolean delete() {
        if (isDirectory()) {
            String[] children = list();
            if (children != null) {
                for (String child : children) {
                    new OSVFile(this, child).delete();
                }
            }
        }
        return super.delete();
    }

    @Override
    public boolean exists() {
        return super.exists();
    }
}
