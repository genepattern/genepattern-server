/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.api;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class ApiUtil {
    static public void handleSuccess(HttpServletResponse resp) throws IOException {
        handleSucess(resp, "Success!");
    }

    static public void handleSucess(HttpServletResponse resp, String message) throws IOException {
        resp.getWriter().println(message);
    }

    static public void handleError(HttpServletResponse resp) throws IOException {
        handleError(resp, "Failure!");
    }

    static public void handleError(HttpServletResponse resp, String errorMessage) throws IOException {
        handleError(resp, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
    }
    static public void handleError(HttpServletResponse resp, int httpServletResponseCode, String errorMessage) throws IOException {
        resp.setStatus(httpServletResponseCode);
        resp.getWriter().println(errorMessage);
    }
}
