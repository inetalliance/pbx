package net.inetalliance.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.types.json.Json;
import net.inetalliance.types.json.Pretty;
import net.inetalliance.util.security.auth.Authorized;

public abstract class JsonProcessor
        extends SecureProcessor {

    @Override
    public void $(final HttpMethod method, final HttpServletRequest request,
                  final HttpServletResponse response)
            throws Throwable {
        val authorized = Auth.getAuthorized(request);
        val json = $(method, request, response, authorized);
        response.setContentType(jsonContentType);
        val writer = response.getWriter();
        Pretty.$(json, writer);
        writer.flush();

    }

    protected abstract Json $(final HttpMethod method, final HttpServletRequest request,
                              final HttpServletResponse response, final Authorized authorized)
            throws Throwable;
}
