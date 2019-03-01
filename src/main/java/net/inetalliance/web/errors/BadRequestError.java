package net.inetalliance.web.errors;

import static javax.servlet.http.HttpServletResponse.*;

public class BadRequestError
		extends HttpError {
	public BadRequestError() {
		super();
	}

	public BadRequestError(final String message) {
		super(message);
	}

	public BadRequestError(final String message, final Throwable cause) {
		super(message, cause);
	}

	public BadRequestError(final Throwable cause) {
		super(cause);
	}

	@Override
	public int getCode() {
		return SC_BAD_REQUEST;
	}
}
