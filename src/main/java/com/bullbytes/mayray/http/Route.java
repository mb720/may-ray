package com.bullbytes.mayray.http;

import com.bullbytes.mayray.http.requests.Request;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Matches a resource that the client has requested and turns the client's {@link Request} into a response.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class Route {
    private final String routeName;
    private final Function<Request, byte[]> handleRequest;
    private final Pattern resourcePath;

    private Route(Pattern resourcePath, String routeName, Function<Request, byte[]> handleRequest) {
        this.resourcePath = resourcePath;
        this.routeName = routeName;
        this.handleRequest = handleRequest;
    }

    /**
     * Creates a new {@link Route}.
     *
     * @param resourcePath  if this {@link Pattern} matches the resource the client has requested, this {@link Route}
     *                      will create a response to the request
     * @param routeName     the name of this {@link Route}
     * @param handleRequest a {@link Function} that turns the client's {@link Request} into a response
     * @return an initialized {@link Route}
     */
    public static Route create(Pattern resourcePath,
                               String routeName,
                               Function<Request, byte[]> handleRequest) {
        return new Route(resourcePath, routeName, handleRequest);
    }

    public boolean matches(String resource) {
        return resourcePath.asMatchPredicate().test(resource);
    }

    public byte[] getResponse(Request request) {
        return handleRequest.apply(request);
    }

    public String getName() {
        return routeName;
    }
}
