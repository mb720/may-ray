package com.bullbytes.mayray.utils;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
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

    /**
     * Zips all regular files in a {@code dirToZip} and returns the {@link Path} of the created zip archive.
     *
     * @param dirToZip                   we zip all {@link Files#isRegularFile regular} files in this directory
     * @param zipFilePath                the location where we create the zip archive containing the file of the
     *                                   {@code dirToZip}
     * @param modifyFilePathInZipArchive transforms the file path of a zipped file as it appears in the resulting zip
     *                                   file
     * @return the {@link Path} of the created zip archive wrapped in a {@link Try} in case zipping failed
     */
    public static Try<Path> zipAllFiles(Path dirToZip,
                                        Path zipFilePath,
                                        Function<String, String> modifyFilePathInZipArchive) {

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
