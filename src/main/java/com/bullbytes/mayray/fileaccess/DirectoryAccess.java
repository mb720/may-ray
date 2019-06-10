package com.bullbytes.mayray.fileaccess;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Determines whether it's allowed to download a specified directory from the server.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class DirectoryAccess {
    public static final Path DOWNLOAD_ROOT_DIR = Path.of("./downloadable");
    private final Path desiredDirectory;
    private final String password;

    private DirectoryAccess(Path desiredDirectory, String password) {
        this.desiredDirectory = desiredDirectory;
        this.password = password;
    }

    public static DirectoryAccess create(Path desiredDir, String password) {
        return new DirectoryAccess(desiredDir, password);
    }

    /**
     * Checks whether downloading the directory is allowed. It's allowed if the directory exists and is a subdirectory
     * of the download root directory.
     *
     * @return whether the user can download the directory
     */
    public boolean isDownloadAllowed() {
        Path normalizedPath = getNormalizedPathFromRoot();
        boolean isDirectory = normalizedPath.toFile().isDirectory();

        Path normalizedRootDir = DOWNLOAD_ROOT_DIR.normalize();
        boolean desiredDirIsSubdirOfRoot = normalizedPath.startsWith(normalizedRootDir);

        return desiredDirIsSubdirOfRoot && isDirectory;
    }

    /**
     * Gets the normalized path of the directory to download.
     *
     * @return the path of the desired directory inside the root download directory, normalized (no "..", or ".")
     */
    public Path getNormalizedPathFromRoot() {
        return Paths.get(DOWNLOAD_ROOT_DIR.toString(), desiredDirectory.toString()).normalize();
    }

    public Path getDesiredDir() {
        return desiredDirectory;
    }

    public boolean passwordMatches() {
        return AccessFile.readFromDir(DOWNLOAD_ROOT_DIR)
                .map(accessFile -> accessFile.passwordIs(password, desiredDirectory))
                .getOrElse(() -> false);
    }

    public String getPassword() {
        return password;
    }
}
