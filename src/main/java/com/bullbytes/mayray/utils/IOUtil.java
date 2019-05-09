package com.bullbytes.mayray.utils;


import io.atlassian.fugue.Try;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * Constants and functions for dealing with files.
 * <p>
 * Persons of contact: Martin Gangl, Matthias Braun
 */
public enum IOUtil {
    ;


    /**
     * Reads a {@code file} and returns its contents as a string.
     * <p>
     * The encoding used is {@link StandardCharsets#UTF_8 UTF-8}.
     *
     * @param filePath {@link Path} to read
     * @return the contents of the file or a failed {@link Try} if the {@code file} could not be read
     */
    public static Try<String> read(Path filePath) {
        Try<String> contentTry;
        try {
            contentTry = Try.successful(java.nio.file.Files.readString(filePath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            contentTry = Try.failure(e);
        }
        return contentTry;
    }

    /**
     * Converts a {@code file} to a {@link Path}. If that conversion would throw an {@link InvalidPathException}, we
     * return that exception inside a {@link Try}.
     * <p>
     * Such {@link InvalidPathException}s happen when the {@code file} contains characters that are considered illegal
     * by the operating system.
     *
     * @param file we convert this {@code File} to a {@link Path}
     * @return the {@link Path} created from the {@code file} wrapped in a {@link Try} in case the conversion failed
     */
    public static Try<Path> toPath(File file) {
        Try<Path> pathTry;
        if (file == null) {
            pathTry = Try.failure(new NullPointerException("Can't convert null to a path"));
        } else {
            try {
                pathTry = Try.successful(file.toPath());
            } catch (InvalidPathException ex) {
                pathTry = Try.failure(ex);
            }
        }
        return pathTry;
    }

    /**
     * Determines whether a {@code file} can be created or, if it already exists, can be written to.
     *
     * @param file we want to see if we can create this {@link File} or, if it already exists, write to it
     * @return whether we can create or write to the {@code file}
     */
    public static boolean canCreateOrIsWritable(File file) {
        boolean canCreateOrIsWritable;

        // Determine which parent directories don't exist
        List<File> parentDirsToCreate = DirectoryUtil.getParentDirsToCreate(file);

        // Create the parent directories that don't exist, starting with the one highest up in the file system hierarchy
        Lists.reverse(parentDirsToCreate).forEach(File::mkdir);

        try {
            boolean wasCreated = file.createNewFile();
            if (wasCreated) {
                canCreateOrIsWritable = true;
                // Remove the file and its parent dirs that didn't exist before
                file.delete();
                parentDirsToCreate.forEach(File::delete);
            } else {
                // There was already a file at the path. Let's see if we can write to it
                canCreateOrIsWritable = java.nio.file.Files.isWritable(file.toPath());
            }
        } catch (IOException e) {
            // File creation failed
            canCreateOrIsWritable = false;
        }
        return canCreateOrIsWritable;
    }
}
