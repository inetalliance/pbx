package net.inetalliance.web.errors;


import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;

public class MethodNotAllowedError
        extends HttpError {

    @Override
    public int getCode() {
        return SC_METHOD_NOT_ALLOWED;
    }
}
