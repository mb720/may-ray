package com.bullbytes.mayray.http.requesthandlers;

import com.bullbytes.mayray.fileaccess.DirectoryAccess;
import com.bullbytes.mayray.html.Pages;
import com.bullbytes.mayray.http.ContentType;
import com.bullbytes.mayray.http.HeaderUtil;
import com.bullbytes.mayray.http.Requests;
import com.bullbytes.mayray.http.Responses;
import com.bullbytes.mayray.utils.FileUtil;
import com.bullbytes.mayray.utils.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.vavr.API;
import io.vavr.collection.List;
import io.vavr.control.Try;
import j2html.tags.Renderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.bullbytes.mayray.http.RequestMethod.GET;
import static com.bullbytes.mayray.http.RequestMethod.POST;
import static io.vavr.API.Tuple;
import static java.lang.String.format;

/**
 * Handles client requests to list or download files on the server.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum FileRequestHandlers {
    ;
    // Expected parameter names in the URL
    public static final String DIR_KEY = "dir";
    public static final String PASSWORD_KEY = "pass";
    private static final Logger log = LoggerFactory.getLogger(FileRequestHandlers.class);

    static HttpHandler getListFilesHandler() {
        return HttpHandlers.checked(exchange -> {

            var passwordInputName = "passwordInput";
            var queryMap = getQueryMap(exchange.getRequestURI());
            switch (Requests.getMethod(exchange)) {
                // The user wants to list the contents of a downloadable directory on the server
                case GET:

                    Renderable pageToShowForGet = queryMap.get(DIR_KEY)
                            .map(dirToGet -> queryMap.get(PASSWORD_KEY)
                                    .map(password -> DirectoryAccess.create(Path.of(dirToGet), password))
                                    .map(dirAccess -> getPageListingDirContents(dirAccess, passwordInputName)
                                            // The query contains which directory to get but not the password
                                    ).getOrElse(() -> Pages.login(dirToGet, passwordInputName)))
                            // The query doesn't contain which directory to download
                            .getOrElse(() -> Pages.needDirToDownload(DIR_KEY));

                    Responses.sendHtml(pageToShowForGet, exchange);
                    break;
                // The user has entered the password via the password input element
                case POST:
                    try (var reqBody = exchange.getRequestBody()) {
                        String decodedPassword = getDecodedPassword(exchange, passwordInputName, reqBody);

                        Renderable pageToShowForPost = queryMap.get(DIR_KEY)
                                .map(dirToGet -> DirectoryAccess.create(Path.of(dirToGet), decodedPassword))
                                .map(dirAccess -> getPageListingDirContents(dirAccess, passwordInputName)
                                        // The query doesn't contain which directory to download
                                ).getOrElse(() -> Pages.needDirToDownload(DIR_KEY));

                        Responses.sendHtml(pageToShowForPost, exchange);
                    }
                    break;
                default:
                    Responses.unsupportedMethod(exchange, List.of(GET, POST));
            }
        });
    }

    private static String getDecodedPassword(HttpExchange exchange, String passwordInputName, InputStream reqBody) throws IOException {
        // The key to the password is the name of the input in the page's form
        String password = getParameterValue(passwordInputName, reqBody);

        return isUrlEncoded(exchange.getRequestHeaders()) ?
                URLDecoder.decode(password, StandardCharsets.UTF_8) :
                password;
    }

    private static Renderable getPageListingDirContents(DirectoryAccess dirAccess, String passwordInputName) {
        Renderable page;
        if (dirAccess.isDownloadAllowed()) {
            page = dirAccess.passwordMatches() ?
                    Pages.dirContents(dirAccess) :
                    Pages.wrongPassword(passwordInputName);
        } else {
            page = Pages.directoryCannotBeDownloaded(dirAccess.getDesiredDir());
        }
        return page;
    }

    private static io.vavr.collection.HashMap<String, String> getQueryMap(URI requestURI) {
        Map<String, String> queryMap = new HashMap<>();

        String query = requestURI.getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int indexOfEqualSign = pair.indexOf('=');
                if (indexOfEqualSign != -1) {
                    String key = pair.substring(0, indexOfEqualSign);
                    String value = pair.substring(indexOfEqualSign + 1);
                    queryMap.put(key, value);
                } else {
                    log.warn("No equal sign in string that should contain parameter name and value: {}", pair);
                }
            }
        }
        return io.vavr.collection.HashMap.ofAll(queryMap);
    }

    private static boolean isUrlEncoded(Headers requestHeaders) {
        List<String> contentTypeValues = HeaderUtil.getValuesOf("Content-type", requestHeaders);
        return contentTypeValues.headOption()
                .map("application/x-www-form-urlencoded"::equals)
                .getOrElse(false);
    }

    private static String getParameterValue(String parameter, InputStream reqBody) throws IOException {
        var reqBytes = reqBody.readAllBytes();
        var reqContent = new String(reqBytes, StandardCharsets.UTF_8);
        var parameterPrefix = (parameter + "=");
        return reqContent.substring(parameterPrefix.length());
    }

    static HttpHandler getDownloadHandler() {
        return HttpHandlers.forMethod(GET, exchange -> {

            var queryMap = getQueryMap(exchange.getRequestURI());

            // Get password and the directory to zip from the URL
            Try<URL> zipUrlTry = Tuple(queryMap.get(PASSWORD_KEY), queryMap.get(DIR_KEY).map(Path::of))
                    // Get at the two Options if they are both present
                    .apply(API::For)
                    // TODO: Avoid creating the zip file if we already have done so and there is a file in the zipFiles directory on disk
                    .yield(FileRequestHandlers::getZipFile)
                    .getOrElse(Try.failure(new RuntimeException(
                            format("Could not get password (key: '%s') and directory (key: '%s') from request URL", PASSWORD_KEY, DIR_KEY))));

            zipUrlTry.fold(error -> {
                Responses.sendError("Could not zip directory", error, exchange);
                return false;
            }, zipUrl -> {
                Responses.sendFile(zipUrl, ContentType.ZIP, exchange);
                return true;
            });
        });
    }

    private static Try<URL> getZipFile(String password, Path dirToZip) {

        var access = DirectoryAccess.create(dirToZip, password);
        // Place the zipped files in a directory in the current working directory
        var archiveName = format("zipFiles/%s.zip", dirToZip.getFileName());
        return access.isDownloadAllowed() && access.passwordMatches() ?
                FileUtil.zipAllFiles(
                        access.getNormalizedPathFromRoot(),
                        Path.of(archiveName),
                        FileRequestHandlers::stripDownloadDir)
                        // Convert the path to a URL
                        .mapTry(zipArchive -> zipArchive.toUri().toURL()) :
                Try.failure(new RuntimeException(format("Not zipping directory %s: Access denied", dirToZip)));
    }

    private static String stripDownloadDir(String filePath) {
        return Strings.removeFirst(DirectoryAccess.DOWNLOAD_ROOT_DIR.normalize().toString(), filePath);
    }
}
