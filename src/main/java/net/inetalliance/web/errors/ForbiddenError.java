package net.inetalliance.web.errors;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

public class ForbiddenError
    extends HttpError {

  public ForbiddenError() {
    super();
  }

  public ForbiddenError(final String message) {
    super(message);
  }

  public ForbiddenError(final String message, final Object... parameters) {
    super(String.format(message, parameters));
  }

  public ForbiddenError(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ForbiddenError(final Throwable cause) {
    super(cause);
  }

  @Override
  public int getCode() {
    return SC_FORBIDDEN;
  }
}
