package com.bullbytes.mayray.utils;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Helps with files.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum FileUtil {
    ;

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static Try<Path> zipAllFiles(Path dirToZip, Path zipFilePath, Function<String,String> modifyFilePathInZipArchive) {

        DirectoryUtil.createParentDirs(zipFilePath);

        // If we encounter an exception, we'll overwrite the value in the Try
        Try<Path> fileTry = Try.success(zipFilePath);

        try (var zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {

            List<Path> filesToZip = getFilesRecursively(dirToZip);

            for (var file : filesToZip) {
                if (Files.isRegularFile(file)) {
                    // This creates the file entry in the zip file but doesn't write any file contents into the zip file
                    zipOutputStream.putNextEntry(new ZipEntry(modifyFilePathInZipArchive.apply(file.toString())));
                    // Add the file contents to the zip file
                    zipOutputStream.write(Files.readAllBytes(file));
                } else {
                    // If the file is a directory (or a symlink to a directory), readAllBytes throws since it can't read bytes from a directory
                    log.info("Not writing contents of file {} to zip archive since it's not a regular file", file);
                }
            }

        } catch (Exception e) {
            fileTry = Try.failure(e);
        }
        return fileTry;
    }


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
            contentTry = Try.success(java.nio.file.Files.readString(filePath, StandardCharsets.UTF_8));
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
                pathTry = Try.success(file.toPath());
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
        parentDirsToCreate.reverse().forEach(File::mkdir);

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

    public static List<Path> getFilesRecursively(Path startDir) {
        var files = new ArrayList<Path>();
        try {
            // Includes symbolic links in the list but doesn't follow them
            Files.walkFileTree(startDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    files.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Could not walk file tree starting at {}", startDir, e);
        }
        return List.ofAll(files);
    }
}
