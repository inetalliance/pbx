package net.inetalliance.web.errors;


import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;

public class NotFoundError
        extends HttpError {

    public NotFoundError() {
        super();
    }

    public NotFoundError(final String message) {
        super(message);
    }

    public NotFoundError(final String message, final Object... parameters) {
        super(String.format(message, parameters));
    }

    public NotFoundError(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NotFoundError(final Throwable cause) {
        super(cause);
    }

    @Override
    public int getCode() {
        return SC_NOT_FOUND;
    }

}
