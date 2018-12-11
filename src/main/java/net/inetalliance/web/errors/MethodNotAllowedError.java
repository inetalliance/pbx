package net.inetalliance.web.errors;

import static javax.servlet.http.HttpServletResponse.*;

public class MethodNotAllowedError extends HttpError
{
	@Override public int getCode()
	{
		return SC_METHOD_NOT_ALLOWED;
	}
}
