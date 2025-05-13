package net.inetalliance.web.errors;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.web.HttpMethod;

import java.io.IOException;

import static com.ameriglide.phenix.core.Strings.isEmpty;

public abstract class HttpError
        extends Error {

    public HttpError() {
        super();
    }

    public HttpError(final String message) {
        super(message);
    }

    public HttpError(final String message, final Throwable cause) {
        super(message, cause);
    }

    public HttpError(final Throwable cause) {
        super(cause);
    }

    public void $(final HttpMethod method, final HttpServletRequest req,
                  final HttpServletResponse resp)
            throws IOException {
        val message = getMessage();
        if (isEmpty(message)) {
            resp.sendError(getCode());
        } else {
            resp.sendError(getCode(), message);
        }
    }

    public abstract int getCode();
}
