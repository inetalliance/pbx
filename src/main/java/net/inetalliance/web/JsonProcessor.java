package net.inetalliance.web;

import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.inetalliance.types.json.Json;
import net.inetalliance.types.json.Pretty;
import net.inetalliance.util.security.auth.Authorized;

public abstract class JsonProcessor
    extends SecureProcessor {

  @Override
  public void $(final HttpMethod method, final HttpServletRequest request,
      final HttpServletResponse response)
      throws Throwable {
    final Authorized authorized = Auth.getAuthorized(request);
    final Json json = $(method, request, response, authorized);
    response.setContentType(jsonContentType);
    final PrintWriter writer = response.getWriter();
    Pretty.$(json, writer);
    writer.flush();

  }

  protected abstract Json $(final HttpMethod method, final HttpServletRequest request,
      final HttpServletResponse response, final Authorized authorized)
      throws Throwable;
}
