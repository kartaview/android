package com.telenav.osv.item;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom file class in order to ToDo: wtf does this?
 *
 * @author Kalman
 */
public class KVFile extends File {

    private static final String TAG = "OSVFile";

    public KVFile(File dir, String name) {
        super(dir, name);
    }

    public KVFile(String path) {
        super(path);
    }

    public KVFile(String dirPath, String name) {
        super(dirPath, name);
    }

    public KVFile(URI uri) {
        super(uri);
    }

    @Override
    public KVFile getParentFile() {
        File parent = super.getParentFile();
        if (parent == null) {
            parent = new File(getParent(), getName());
        }
        return new KVFile(parent.getParent(), parent.getName());
    }

    @Override
    public boolean exists() {
        return super.exists();
    }

    @Override
    public boolean delete() {
        if (isDirectory()) {
            String[] children = list();
            if (children != null) {
                for (String child : children) {
                    new KVFile(this, child).delete();
                }
            }
        }
        return super.delete();
    }

    @Override
    public KVFile[] listFiles() {
        return filenamesToFiles(list());
    }

    @Override
    public KVFile[] listFiles(FilenameFilter filter) {
        return filenamesToFiles(list(filter));
    }

    @Override
    public KVFile[] listFiles(FileFilter filter) {
        KVFile[] files = listFiles();
        if (filter == null || files == null) {
            return files;
        }
        List<KVFile> result = new ArrayList<>(files.length);
        for (KVFile file : files) {
            if (filter.accept(file)) {
                result.add(file);
            }
        }
        return result.toArray(new KVFile[result.size()]);
    }

    /**
     * Converts a String[] containing filenames to a File[].
     * Note that the filenames must not contain slashes.
     * This method is to remove duplication in the implementation
     * of File.list's overloads.
     */
    private KVFile[] filenamesToFiles(String[] filenames) {
        if (filenames == null) {
            return new KVFile[0];
        }
        int count = filenames.length;
        KVFile[] result = new KVFile[count];
        for (int i = 0; i < count; ++i) {
            result[i] = new KVFile(this, filenames[i]);
        }
        return result;
    }
}
