package org.sunflow.system;

import java.io.File;

public final class FileUtils {
    /**
     * Extract the file extension from the specified filename.
     * 
     * @param filename filename to get the extension of
     * @return a string representing the file extension, or <code>null</code>
     *         if the filename doesn't have any extension, or is not a file
     */
    public static final String getExtension(String filename) {
        if (filename == null)
            return null;
        File f = new File(filename);
        if (f.isDirectory())
            return null;
        int idx = new File(filename).getName().lastIndexOf('.');
        return idx == -1 ? null : filename.substring(idx + 1);
    }
}