package org.genepattern.server.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for debugging HttpServletRequest contextPath, servletPath, and pathInfo.
 * Example web.xml entry:
 * <pre>
 <servlet>
  <servlet-name>DebugServlet</servlet-name>
  <servlet-class>org.genepattern.server.webapp.DebugServlet</servlet-class>
 </servlet>

 <servlet-mapping>
  <servlet-name>DebugServlet</servlet-name>
  <url-pattern>/debug/*</url-pattern>
 </servlet-mapping>

 <!-- match /* path -->
 <servlet-mapping>
  <servlet-name>DebugServlet</servlet-name>
  <url-pattern>/*</url-pattern>
 </servlet-mapping>

 <!-- match a sub directory -->
 <servlet-mapping>
  <servlet-name>DebugServlet</servlet-name>
  <url-pattern>/test/debug/*</url-pattern>
 </servlet-mapping>

 * </pre>
 */
@SuppressWarnings("serial")
public class DebugServlet extends HttpServlet {
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String rClass=request.getClass().getName();
        final String rurl=request.getRequestURL().toString();
        String contextPath=request.getContextPath();
        final String contextPathOrig=contextPath;
        if (contextPath==null) {
            contextPath="/";
        }
        else if (!contextPath.startsWith("/")) {
            contextPath="/"+contextPath;
        }
        final URI uri=URI.create(rurl);
        final URI contextUrl=uri.resolve(contextPath);
        final String servletPath=request.getServletPath();
        final String pathInfo=request.getPathInfo();
 
        // Set response content type
        response.setContentType("text/html");

        // Actual logic goes here.
        PrintWriter out = response.getWriter();
        out.print("<html><head></head>");
        out.print("<body>");
        out.print("<h3>HttpServletRequest details</h3>");
        out.print("<table>");
        addRow(out, "request.class", rClass);
        addRow(out, "request.requestURL", rurl);
        addRow(out, "request.contextPath", contextPathOrig);
        addRow(out, "adjusted request.contextPath", contextPath);
        addRow(out, "contextUrl", contextUrl.toString());
        addRow(out, "servletPath", servletPath);
        addRow(out, "pathInfo", pathInfo);
        out.print("</table>");
        out.print("</body></html>");
    }

    protected void addRow(final PrintWriter out, String... cols) {
        out.print("<tr>");
        for(final String col : cols) {
            out.print("<td>"+col+"</td>");
        }
        out.print("</tr>");
    }
}
