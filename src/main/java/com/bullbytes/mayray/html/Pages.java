package com.bullbytes.mayray.html;

import com.bullbytes.mayray.fileaccess.DirectoryAccess;
import com.bullbytes.mayray.utils.FormattingUtil;
import com.bullbytes.mayray.http.responses.FileResponses;
import com.bullbytes.mayray.utils.FileUtil;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;
import j2html.tags.Renderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static j2html.TagCreator.*;
import static java.lang.String.format;

/**
 * Creates HTML pages for this application.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Pages {
    ;

    private static final Logger log = LoggerFactory.getLogger(Pages.class);

    private static EmptyTag passwordInput(String name) {
        return input()
                .withType("password")
                .withName(name)
                .isRequired();
    }

    public static Renderable login(String dirToGet, String passwordInputName) {

        return withHead("Authentication",
                body(
                        h2("🗝"),
                        p(format("You're about to access directory '%s'", dirToGet)),
                        getPasswordForm(passwordInputName)
                ));
    }

    private static ContainerTag getPasswordForm(String passwordInputName) {
        return form(
                label("Password: ").attr("for", passwordInputName),
                passwordInput(passwordInputName),
                button("log in").withType("submit")
        ).withMethod("post");
    }

    private static Renderable withHead(String pageTitle, ContainerTag body) {

        return html(
                head(
                        meta().withCharset("UTF-8"),
                        title(pageTitle)),
                body
        );
    }

    public static Renderable needDirToDownload(String dirKey) {
        return withHead("Which directory?", body(
                p(join(h2("Which directory do you want to download?"),
                        "Specify in the URL with "), i(" ?" + dirKey + "=yourDirectory")))
        );
    }

    public static Renderable dirContents(DirectoryAccess directoryAccess) {
        Renderable page;

        if (directoryAccess.isDownloadAllowed() && directoryAccess.passwordMatches()) {
            Path dirPath = directoryAccess.getNormalizedPathFromRoot();
            var filesInDir = FileUtil.getFilesRecursively(dirPath).toJavaList();

            var fileList = ul(each(filesInDir, f -> li(trimDownloadRootDir(f) + " " + getSize(f))));

            var dirKey = FileResponses.DIR_KEY;
            var passKey = FileResponses.PASSWORD_KEY;
            var downloadLink = p(a("Download files")
                    .withHref(format("get?%s=%s&%s=%s",
                            dirKey,
                            directoryAccess.getDesiredDir(),
                            passKey,
                            directoryAccess.getPassword())));

            page = withHead("Files of " + dirPath,
                    body(join(h1(
                            "Contents of " + trimDownloadRootDir(dirPath)),
                            fileList,
                            downloadLink
                    )));
        } else {
            Path desiredDir = directoryAccess.getDesiredDir();
            log.warn("This method shouldn't be called if the directory to download doesn't exist ('{}') or if the password is incorrect", desiredDir);

            page = withHead("Access denied", body(join(h1("Denied accessing " + desiredDir),
                    p("Either the directory doesn't exist on the server or the provided password is incorrect"))));
        }
        return page;
    }

    private static String getSize(Path f) {
        return FormattingUtil.humanReadableBytes(f.toFile().length());
    }

    private static String trimDownloadRootDir(Path path) {
        return path.subpath(1, path.getNameCount()).toString();
    }

    public static Renderable wrongPassword(String passwordInputName) {
        return withHead("Directory password incorrect",
                body(join("The directory password is incorrect. Try entering the password again in case this was an error.",
                        getPasswordForm(passwordInputName))));
    }

    public static Renderable directoryCannotBeDownloaded(Path dirToGet) {
        return withHead("Can't download directory", body(
                h2(format("Can't download files from directory '%s'", dirToGet)),
                p("Maybe the directory does not exist.")
        ));
    }

    public static Renderable couldNotGetPasswordFromBody(String passwordKey) {
        return withHead("Couldn't get password from body", body(
                h2("Something went wrong"),
                p(format("Couldn't get password from body. It should be something like '%s=directoryPassword'", passwordKey))
        ));
    }
}
