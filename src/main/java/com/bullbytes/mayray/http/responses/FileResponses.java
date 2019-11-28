package com.bullbytes.mayray.http.responses;

import com.bullbytes.mayray.fileaccess.DirectoryAccess;
import com.bullbytes.mayray.html.Pages;
import com.bullbytes.mayray.http.headers.HeaderUtil;
import com.bullbytes.mayray.http.headers.HttpHeader;
import com.bullbytes.mayray.http.requests.Request;
import com.bullbytes.mayray.http.requests.Requests;
import com.bullbytes.mayray.utils.FileUtil;
import com.bullbytes.mayray.utils.ParseUtil;
import com.bullbytes.mayray.utils.Strings;
import io.vavr.API;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import j2html.tags.Renderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static com.bullbytes.mayray.http.headers.InlineOrAttachment.ATTACHMENT;
import static com.bullbytes.mayray.http.requests.RequestMethod.GET;
import static com.bullbytes.mayray.http.requests.RequestMethod.POST;
import static io.vavr.API.Tuple;
import static java.lang.String.format;

/**
 * Handles client requests to list or download files on the server.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum FileResponses {
    ;
    // Expected parameter names in the URL
    public static final String DIR_KEY = "dir";
    public static final String PASSWORD_KEY = "pass";
    private static final Logger log = LoggerFactory.getLogger(FileResponses.class);

    public static byte[] listFiles(Request req) {

        var passwordInputName = "passwordInput";
        var queryMap = getQueryMap(req.getResource());
        return switch (req.getMethod()) {
            // The user wants to list the contents of a downloadable directory on the server
            case GET -> {
                Renderable pageToShow = queryMap.get(DIR_KEY)
                        .map(dirToGet -> queryMap.get(PASSWORD_KEY)
                                .map(password -> DirectoryAccess.create(Path.of(dirToGet), password))
                                .map(dirAccess -> getPageListingDirContents(dirAccess, passwordInputName)
                                        // The query contains which directory to get but not the password
                                ).getOrElse(() -> Pages.login(dirToGet, passwordInputName)))
                        // The query doesn't contain which directory to download
                        .getOrElse(() -> Pages.needDirToDownload(DIR_KEY));

                yield Responses.html(pageToShow);
            }
            // The user has entered the password via the password input element → Read the password from the request
            // body, decode it if necessary and show a listing of the directory if the password matches
            case POST -> Requests.getBody(req)
                    .fold(error -> {
                        var msg = "Could not read body of post message";
                        log.warn(msg, error);
                        return Responses.plainText(msg, StatusCode.BAD_REQUEST);
                    }, reqBody -> {
                        var pageToShow = getDecodedPassword(req.getHeaders(), passwordInputName, reqBody)
                                .fold(
                                        () -> Pages.couldNotGetPasswordFromBody(PASSWORD_KEY),
                                        decodedPassword -> queryMap.get(DIR_KEY)
                                                .map(dirToGet -> DirectoryAccess.create(Path.of(dirToGet), decodedPassword))
                                                .map(dirAccess -> getPageListingDirContents(dirAccess, passwordInputName)
                                                        // The query doesn't contain which directory to download
                                                ).getOrElse(() -> Pages.needDirToDownload(DIR_KEY)));
                        return Responses.html(pageToShow);
                    });
            default -> Responses.unsupportedMethod(List.of(GET, POST));
        };
    }

    private static Option<String> getDecodedPassword(Map<String, String> headers,
                                                     String passwordInputName,
                                                     String reqBody) {
        var bodyLines = List.of(reqBody.split("\r\n"));
        // In the request body, the key to the password is the name of the input in the page's form
        var parameterPrefix = passwordInputName + "=";
        Option<String> encodedPassword = bodyLines.filter(bodyLine -> bodyLine.startsWith(parameterPrefix))
                .headOption()
                .map(lineWithParameter -> lineWithParameter.substring(parameterPrefix.length()));

        return encodedPassword.map(password ->
                isUrlEncoded(headers) ?
                        URLDecoder.decode(password, StandardCharsets.UTF_8) :
                        password);
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

    private static Map<String, String> getQueryMap(String uriWithQuery) {

        // If the URI is "/resource?param1=value1&param2=value2" get the query part: "param1=value1&param2=value2"
        String query = Strings.getStringAfter("?", uriWithQuery);

        String[] keyValuePairs = query.split("&");
        return ParseUtil.getKeyValueMap(
                List.of(keyValuePairs),
                "=",
                msg -> log.warn(msg.toString()));
    }

    private static boolean isUrlEncoded(Map<String, String> requestHeaders) {
        return HeaderUtil.getValueOf(HttpHeader.CONTENT_TYPE, requestHeaders)
                .map("application/x-www-form-urlencoded"::equals)
                .getOrElse(false);
    }

    private static Try<URL> getZipFile(String password, Path dirToZip) {

        var access = DirectoryAccess.create(dirToZip, password);
        // Place the zipped files in a directory in the current working directory
        var archiveName = format("zipFiles/%s.zip", dirToZip.getFileName());
        return access.isDownloadAllowed() && access.passwordMatches() ?
                FileUtil.zipAllFiles(
                        access.getNormalizedPathFromRoot(),
                        Path.of(archiveName),
                        FileResponses::stripDownloadDir)
                        // Convert the path to a URL
                        .mapTry(zipArchive -> zipArchive.toUri().toURL()) :
                Try.failure(new RuntimeException(format("Not zipping directory %s: Access denied", dirToZip)));
    }

    private static String stripDownloadDir(String filePath) {
        return Strings.getStringAfter(DirectoryAccess.DOWNLOAD_ROOT_DIR.normalize().toString(), filePath);
    }

    public static byte[] zipDir(Request request) {

        byte[] response;

        if (request.getMethod() == GET) {
            var queryMap = getQueryMap(request.getResource());
            // Get password and the directory to zip from the URL
            Try<URL> zipUrlTry = Tuple(queryMap.get(PASSWORD_KEY), queryMap.get(DIR_KEY).map(Path::of))
                    // Get at the two Options if they are both present
                    .apply(API::For)
                    // TODO: Avoid creating the zip file if we already have done so and there is a file in the zipFiles directory on disk
                    .yield(FileResponses::getZipFile)
                    .getOrElse(Try.failure(new RuntimeException(
                            format("Could not get password (key: '%s') and directory (key: '%s') from request URL", PASSWORD_KEY, DIR_KEY))));

            response = zipUrlTry.fold(error -> {
                String msg = "Could not zip directory";
                log.warn(msg, error);
                return Responses.plainText(msg, StatusCode.SERVER_ERROR);
            }, zipUrl -> Responses.file(zipUrl, ContentType.ZIP, ATTACHMENT));
        } else {
            response = Responses.unsupportedMethod(List.of(GET));
        }
        return response;
    }
}
