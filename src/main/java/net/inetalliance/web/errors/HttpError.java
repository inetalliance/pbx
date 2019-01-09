package net.inetalliance.web.errors;

import net.inetalliance.web.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static net.inetalliance.funky.StringFun.isEmpty;

public abstract class HttpError extends Error
{
	public HttpError()
	{
		super();
	}

	public HttpError(final String message)
	{
		super(message);
	}

	public HttpError(final String message, final Throwable cause)
	{
		super(message, cause);
	}

	public HttpError(final Throwable cause)
	{
		super(cause);
	}

	public abstract int getCode();

	public void $(final HttpMethod method, final HttpServletRequest req, final HttpServletResponse resp)
			throws IOException
	{
		final String message = getMessage();
		if (isEmpty(message)) {
			resp.sendError(getCode());
		} else
			resp.sendError(getCode(), message);
	}
}
