package net.inetalliance.web;

import net.inetalliance.web.errors.HttpError;
import net.inetalliance.web.errors.InternalServerError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class SecureProcessor extends Processor
{
	@Override protected void doAll(final HttpMethod method, final HttpServletRequest request,
	                               final HttpServletResponse response) throws IOException
	{
		try
		{
			if (Auth.isAuthenticated(request, response))
				super.doAll(method, request, response);
		}
		catch (HttpError e)
		{
			e.$(method, request, response);
		}
		catch (Throwable t)
		{
			InternalServerError.$(response, t);
		}
	}
}
