package net.inetalliance.web;

import net.inetalliance.web.errors.*;

import javax.servlet.http.*;
import java.io.*;

public abstract class SecureProcessor
		extends Processor {
	@Override
	protected void doAll(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		try {
			if (Auth.isAuthenticated(request, response)) {
				super.doAll(method, request, response);
			}
		} catch (HttpError e) {
			e.$(method, request, response);
		} catch (Throwable t) {
			InternalServerError.$(response, t);
		}
	}
}
