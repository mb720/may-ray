package com.bullbytes.mayray.http.responses;

import com.bullbytes.mayray.http.requests.Request;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static com.bullbytes.mayray.http.responses.StatusCode.NOT_FOUND;
import static com.bullbytes.mayray.http.headers.InlineOrAttachment.INLINE;
import static java.lang.String.format;

/**
 * Answers requests of the client regarding famous people.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum PersonResponses {
    ;
    public static final Logger log = LoggerFactory.getLogger(PersonResponses.class);
    private static final String QUOTE = "/quote";
    private static final String NAME = "/name";
    private static final String ROLE = "/role";
    private static final String IMG = "/img.jpg";
    private static final String INFO_ABOUT_PERSON_RESOURCES = "You can request the 'name', an 'img.jpg', a 'quote', or the 'role' of this person.";

    public static byte[] graceHopper(Request req) {
        var name = "Grace Hopper";
        var imgUrl = "http://ww2.kqed.org/mindshift/wp-content/uploads/sites/23/2014/10/grace-hopper_custom-7e094af0ae451cd447568fd03d9c89ba6bf8b352.jpg";
        var quote = "\"A ship in port is safe, but that's not what ships are built for.\"";
        var role = "Computer engineering pioneer";

        return getPersonResponse(name, imgUrl, quote, role, req);
    }

    public static byte[] linus(Request req) {
        var name = "Linus Torvalds";
        var imgUrl = "http://cdn.facesofopensource.com/wp-content/uploads/2017/03/16181944/linustorvalds.faces22106.web_.jpg";
        var quote = "\"Intelligence is the ability to avoid doing work, yet getting the work done.\"";
        var role = "Inventor of Linux";

        return getPersonResponse(name, imgUrl, quote, role, req);
    }

    public static byte[] ada(Request req) {
        var name = "Ada Lovelace";
        var imgUrl = "https://upload.wikimedia.org/wikipedia/commons/a/a4/Ada_Lovelace_portrait.jpg";
        var quote = "\"The Analytical Engine has no pretensions whatever to originate anything. " +
                "It can do whatever we know how to order it to perform.\"";
        var role = "First programmer";

        return getPersonResponse(name, imgUrl, quote, role, req);
    }

    public static byte[] simonPeytonJones(Request req) {
        var name = "Simon Peyton Jones";
        var imgUrl = "https://www.microsoft.com/en-us/research/wp-content/uploads/2016/08/TEDx-Mar14-1.jpg";
        var quote = "\"When the limestone of imperative programming is worn away, the granite of functional programming will be observed.\"";
        var role = "Inventor of the Haskell programming language";

        return getPersonResponse(name, imgUrl, quote, role, req);
    }

    private static String getRequestedResource(Request req) {
        String resource = req.getResource();
        int lastIndexOfSlash = resource.lastIndexOf('/');
        return lastIndexOfSlash == -1 ?
                resource :
                resource.substring(lastIndexOfSlash);
    }

    private static byte[] getPersonResource(Request req, String name, URL imgUrl, String quote, String role) {
        String resource = getRequestedResource(req);
        return switch (resource) {
            case "/" -> Responses.plainText(INFO_ABOUT_PERSON_RESOURCES);
            case IMG -> Responses.file(imgUrl, ContentType.JPEG, INLINE);
            case QUOTE -> Responses.plainText(quote);
            case NAME -> Responses.plainText(name);
            case ROLE -> Responses.plainText(role);
            default -> Responses.plainText(unknownResource(resource), NOT_FOUND);
        };
    }

    private static String unknownResource(String resource) {
        return format("Sorry, never heard of this %s thing before", resource);
    }

    private static byte[] getPersonResponse(String name, String imgUrl, String quote, String role, Request req) {
        return Try.of(() -> new URL(imgUrl))
                .fold(
                        error -> {
                            log.warn("Invalid URL for person image: '{}'", imgUrl, error);
                            return Responses.plainText("Could not get image of person", StatusCode.SERVER_ERROR);
                        },
                        url -> getPersonResource(req, name, url, quote, role));
    }
}
