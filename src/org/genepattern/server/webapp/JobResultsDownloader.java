/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
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

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Helper class for downloading job results files.
 * 
 * @deprecated - Use FileDownloader instead.
 * 
 * @author pcarr
 * 
 * References:
 *     The FileServlet developed by Balusc implements ETag and gzip.
 *     http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 */
public class JobResultsDownloader {
    private static Logger log = Logger.getLogger(JobResultsDownloader.class);

    public static void serveFile(ServletContext context, HttpServletRequest request, HttpServletResponse response, boolean serveContent, File fileObj) 
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
        
        streamByteRange(context, request, response, serveContent, fileObj, range);
    }
    
    private static void streamByteRange(ServletContext context, HttpServletRequest request, HttpServletResponse response, boolean serveContent, File fileObj, Range range) 
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
            
            String filename = fileObj.getName();
            //handle special case for Axis (copied from getFile.jsp)
            if (filename.startsWith("Axis")) {
                int idx = filename.indexOf(".att_");
                if (idx >= 0) {
                    idx += ".att_".length();
                    filename = filename.substring(idx);
                }
            }

            response.reset();
            response.setHeader("Content-disposition", "inline; filename=\"" + filename + "\"");
            //TODO: need support for ETags, Expires, 
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            //response.setDateHeader("Expires", 0);
            response.setDateHeader("Last-Modified", fileObj.lastModified());
            String contentType = context.getMimeType(filename);
            if (contentType != null) {
                response.setHeader("Content-Type", contentType);
            }

            //HACK: rich faces servlet alters the content (consequently the size) of html files
            //    This is a consequence of using the same server for downloading data files and rendering the job status page
            //    Data files should not be altered by Rich Faces, however, the job status page uses ajax managed by Rich Faces 
            if (contentType != null && contentType.endsWith("html")) {
                //don't set content length for html files
            }
            else {
                response.setHeader("Content-Length", "" + contentLength);
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
                os.flush();
                os.close();
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
    protected static List<Range> parseRange(HttpServletRequest request, HttpServletResponse response, long fileLength)
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
    protected static IOException copyRange(InputStream istream, ServletOutputStream ostream, long start, long end) {
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


    static class Range {
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
