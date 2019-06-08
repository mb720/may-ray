package com.bullbytes.mayray.utils;


import io.vavr.collection.List;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Helps with file system directories.
 * For example, creating and deleting directories, listing their contents, and searching files.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum DirectoryUtil {
    ;

    /**
     * Creates an empty directory. All parent directories are created if they
     * don't exist.
     *
     * @param dir directory path. If it is null, this method returns {@code false}.
     * @return whether a new directory was created (false if there already
     * existed one with the same name)
     */
    public static boolean makeDir(File dir) {
        // Return false if the path was null and whether the directory is new
        return dir != null && dir.mkdirs();
    }

    /**
     * Gets the directory for temporary files. You can define this directory by calling Java using
     * {@code java -Djava.io.tmpdir=/your/temp/dir} or by setting the environment variable {@code TEMP}.
     *
     * @return the directory for temporary files as a string
     */
    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }


    /**
     * Creates the parent directories for a {@code file}.
     *
     * @param file we create the parent directories for this {@link File}
     * @return whether any directories were created
     */
    public static boolean createParentDirs(Path file) {
        // Make sure the directories leading to the file exist
        File containingDir = file.toFile().getParentFile();
        return makeDir(containingDir);
    }

    /**
     * Gets the list of directories that are the parents of the {@code file} and don't exist yet.
     * <p>
     * The first directory in the returned list is the parent of the {@code file}, the next directory is the file's
     * grandparent and so forth.
     *
     * @param file we get the non-existing parent directories of this {@link File}
     * @return the list of directories that are the parents of the {@code file} and don't exist yet
     */
    public static List<File> getParentDirsToCreate(File file) {
        var parentsToCreate = new ArrayList<File>();
        File parent = file.getParentFile();
        while (parent != null && !parent.exists()) {
            parentsToCreate.add(parent);

            parent = parent.getParentFile();
        }
        return List.ofAll(parentsToCreate);
    }
}
