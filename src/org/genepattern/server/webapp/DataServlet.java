package org.genepattern.server.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;

/**
 * Access to GP data files from IGV with support for HTTP Basic Authentication and partial get.
 * 
 * TODO: implement access control based on current user and server file path
 * 
 * Notes:
 *     http://stackoverflow.com/questions/1478401/wrap-the-default-servlet-but-override-the-default-webapp-path
 *     http://tomcat.apache.org/tomcat-6.0-doc/funcspecs/fs-default.html
 *     http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 *     
 * For debugging, use this from the command line:
 *     curl --basic -u "test:test" -H Range:bytes=0-10 --dump-header - http://127.0.0.1:8080/gp/data//Users/pcarr/tmp/test.txt
 *
 * 
 * @author pcarr
 */
public class DataServlet extends HttpServlet implements Servlet {
    private static Logger log = Logger.getLogger(DataServlet.class);

    public DataServlet() {
        super();
    }
    
    public void init() throws ServletException {
        super.init();
    }
    
    public void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = basicAuth(req, resp);
        if (userId == null || userId.trim().length() == 0) {
            //Not authorized, the basicAuth method sends the response back to the client
            return;
        }
        processRequest(req, resp, false);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = basicAuth(req, resp);
        if (userId == null || userId.trim().length() == 0) {
            //Not authorized, the basicAuth method sends the response back to the client
            return;
        }

        processRequest(req, resp, true);
    }

    /**
     * Map the request to a local file path, authorize the current user, and stream the file back in response.
     * 
     * Note: with Basic Authentication it is not common for clients to logout from a session
     *     This can be a problem when different GP user accounts are requesting content from the 
     *     data servlet
     *     To support this scenario, this method must validate that the current GP user has
     *     access to the requested file, and then send a 401 SC_UNAUTHORIZED if it doesn't
     * 
     * @param request
     * @param response
     * @param serveContent, if false, only respond with the header
     * @throws IOException
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean serveContent) throws IOException {
        String path = request.getPathInfo();
        if (path == null) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String serverFilepath = path;
        
        //TODO, implement authorization

        // Accept ranges header
        response.setHeader("Accept-Ranges", "bytes");
        
        File fileObj = new File(serverFilepath);
        serveFile(request, response, serveContent, fileObj);
    }
    
    private void serveFile(HttpServletRequest request, HttpServletResponse response, boolean serveContent, File fileObj) 
    throws IOException
    {
        //make sure the file exists and can be read
        if (!fileObj.canRead()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: "+fileObj.getPath());
            return;
        }
        
        long fileLength = fileObj.length();
        List<Range> ranges = parseRange(request, response, fileLength);
        if (ranges == null) {
            return;
        }
        Range range = null;
        if (ranges.size() == 0) {
            //stream the entire file
        }
        else if (ranges.size() == 1) {
            //single range specified in set
            range = ranges.get(0);
            
            //special case when requested range.end is > actual file size
            range.end = Math.min(range.end, fileObj.length()-1);
        }
        else {
            //TODO: implement multipart/byteranges
            //server error, byte-range-set not yet implemented
            log.error("multipart/byteranges not yet implemented");
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "multipart/byteranges not yet implemented");
            return;
        }
        
        streamByteRange(request, response, serveContent, fileObj, range);
    }

    private void streamByteRange(HttpServletRequest request, HttpServletResponse response, boolean serveContent, File fileObj, Range range) 
    throws IOException
    {
        
        BufferedInputStream is = null;
        try {
            FileInputStream fis = new FileInputStream(fileObj);
            is = new BufferedInputStream(fis);
            
            long contentLength = fileObj.length();
            if (range != null) {
                contentLength = range.end - range.start + 1;
            }

            String filename = fileObj.getName().toLowerCase();
            response.setHeader("Content-disposition", "inline; filename=\"" + filename + "\"");
            //TODO: need support for ETags, Expires, 
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            //response.setDateHeader("Expires", 0);
            response.setDateHeader("Last-Modified", fileObj.lastModified());
            response.setHeader("Content-Length", "" + contentLength);
            if (filename.endsWith(".html") || filename.endsWith(".htm")){
                response.setHeader("Content-Type", "text/html"); 
            }
            if (range != null) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.addHeader("Content-Range", "bytes "
                        + range.start
                        + "-" + range.end + "/"
                        + range.length); 
            }
            
            if (serveContent) {
                ServletOutputStream os = response.getOutputStream();
                long start = 0;
                long end = fileObj.length() - 1L;
                if (range != null) {
                    start = range.start;
                    end = range.end;
                }
                copyRange(is, os, start, end);
            }
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException x) {
                }
            }
        }
    }

    /**
     * Authenticate the username:password pair from the request header.
     * 
     * @param request
     * @return the username or null if the client is not (yet) authorized.
     */
    private String basicAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //bypass basicauth if the current session already has an authorized user
        String userId = LoginManager.instance().getUserIdFromSession(request);
        if (userId != null) {
            return userId;
        }
        
        boolean allow = false;
        // Get Authorization header
        String auth = request.getHeader("Authorization");
        String[] up = getUsernamePassword(auth);
        userId = up[0];
        String passwordStr = up[1];
        byte[] password = passwordStr != null ? passwordStr.getBytes() : null;
        try {
            allow = UserAccountManager.instance().authenticateUser(userId, password);
        }
        catch (AuthenticationException e) {
        }
        
        // If the user was not validated,
        // fail with a 401 status code (UNAUTHORIZED) and pass back a WWW-Authenticate header for this servlet.
        //
        // Note that this is the normal situation the first time you access the page.  
        // The client web browser will prompt for userID and password and cache them 
        // so that it doesn't have to prompt you again.
        if (!allow) {
            final String realm = "GenePattern";
            response.setHeader("WWW-Authenticate", "BASIC realm=\""+realm+"\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }

        // Otherwise, proceed
        return userId;
    }

    /**
     * Parse out the username:password pair from the authorization header.
     * 
     * @param auth
     * @return <pre>new String[] {<username>, <password>};</pre>
     */
    private String[] getUsernamePassword(String auth) {
        String[] up = new String[2];
        up[0] = null;
        up[1] = null;
        if (auth == null) {
            return up;
        }

        if (!auth.toUpperCase().startsWith("BASIC "))  {
            return up;
        }

        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);

        // Decode it, using any base 64 decoder
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        String userpassDecoded = null;
        try {
            userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));
        }
        catch (IOException e) {
            log.error("Error decoding username and password from HTTP request header", e);
            return up;
        }
        String username = "";
        String passwordStr = null;
        int idx = userpassDecoded.indexOf(":");
        if (idx >= 0) {
            username = userpassDecoded.substring(0, idx);
            passwordStr = userpassDecoded.substring(idx+1);
        }
        up[0] = username;
        up[1] = passwordStr;
        return up;
    }
    
    
    //------ based on the Tomcat 5.5.33 source code, DefaultServlet.java ----
    /* Licensed to the Apache Software Foundation (ASF) under one or more 
     * contributor license agreements under the Apache License, Version 2.0
     * 
     *     http://www.apache.org/licenses/LICENSE-2.0)
     *  
     *  Notice: this code has been altered from the original source.
     */
    /**
     * Parse the range header, based on code in Tomcat 5.5.33 DefaultServlet.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @return List<Range>, an empty list means use the FULL range, a null means an error occurred and a ERROR response was sent
     */
    protected List<Range> parseRange(HttpServletRequest request, HttpServletResponse response, long fileLength)
    throws IOException 
    {
        // Retrieving the range header (if any is specified)
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null) {
            return Collections.emptyList();
        }

        if (!rangeHeader.startsWith("bytes")) {
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return null;
        }

        rangeHeader = rangeHeader.substring(6);

        // list of all the ranges which are successfully parsed.
        List<Range> result = new ArrayList<Range>();
        StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

        // Parsing the range list
        while (commaTokenizer.hasMoreTokens()) {
            String rangeDefinition = commaTokenizer.nextToken().trim();

            Range currentRange = new Range();
            currentRange.length = fileLength;

            int dashPos = rangeDefinition.indexOf('-');

            if (dashPos == -1) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            if (dashPos == 0) {
                try {
                    long offset = Long.parseLong(rangeDefinition);
                    currentRange.start = fileLength + offset;
                    currentRange.start = Math.max(0, currentRange.start);
                    currentRange.end = fileLength - 1;
                } 
                catch (NumberFormatException e) {
                    response.addHeader("Content-Range", "bytes */" + fileLength);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return null;
                }
            } 
            else {
                try {
                    currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1) {
                        currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1, rangeDefinition.length()));
                    }
                    else {
                        currentRange.end = fileLength - 1;
                    }
                } 
                catch (NumberFormatException e) {
                    response.addHeader("Content-Range", "bytes */" + fileLength);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return null;
                }
            }

            if (!currentRange.validate()) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            result.add(currentRange);
        }

        return result;
    }
    
    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param istream The input stream to read from
     * @param ostream The output stream to write to
     * @param start Start of the range which will be copied
     * @param end End of the range which will be copied
     * @return Exception which occurred during processing
     */
    protected IOException copyRange(InputStream istream, ServletOutputStream ostream, long start, long end) {
        try {
            istream.skip(start);
        } 
        catch (IOException e) {
            return e;
        }

        IOException exception = null;
        long bytesToRead = end - start + 1;
        final int BUFSIZE = 2048;
        final byte buffer[] = new byte[BUFSIZE];
        int len = buffer.length;
        while ( (bytesToRead > 0) && (len >= buffer.length)) {
            try {
                len = istream.read(buffer);
                if (bytesToRead >= len) {
                    ostream.write(buffer, 0, len);
                    bytesToRead -= len;
                } 
                else {
                    ostream.write(buffer, 0, (int) bytesToRead);
                    bytesToRead = 0;
                }
            } 
            catch (IOException e) {
                exception = e;
                len = -1;
            }
            if (len < buffer.length) {
                break;
            }
        }
        return exception;
    }

    // ------------------------------------------------------ Range Inner Class


    protected class Range {
        public long start;
        public long end;
        public long length;

        public boolean validate() {
            if (end >= length) {
                end = length - 1;
            }
            return ( (start >= 0) && (end >= 0) && (start <= end) && (length > 0) );
        }

        public void recycle() {
            start = 0;
            end = 0;
            length = 0;
        }
    }

}
