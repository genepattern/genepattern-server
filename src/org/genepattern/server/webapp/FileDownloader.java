package org.genepattern.server.webapp;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webapp.jsf.UIBeanHelper;


/**
 * File Download utility based on BalusC FileServlet, which is very similar to Tomcat DefaultServlet.
 * This code is a modification of the code posted by BalusC with customizations for GenePattern.
 * See: http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 * See also: Tomcat source code, DefaultServlet.java, (I based my edits on v. 5.5.33).
 * 
 * @author pcarr
 * 
 */
public class FileDownloader {
    private static Logger log = Logger.getLogger(FileDownloader.class);

    // Constants ----------------------------------------------------------------------------------

    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    
    public enum ContentDisposition {
        INLINE,
        ATTACHMENT,
    }
    
    private static int getMaxInlineSize() {
        String userId = UIBeanHelper.getUserId();
        GpContext userContext = GpContext.getContextForUser(userId);
        int i = ServerConfigurationFactory.instance().getGPIntegerProperty(userContext, "max.inline.size", 10000000);
        return i;
    }
    
    /*
     * The headers were set in the JobResultsServlet in GP 3.3.1 and earlier.
     
        httpServletResponse.setHeader("Content-disposition", "inline; filename=\"" + fileObj.getName() + "\"");
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setDateHeader("Last-Modified", fileObj.lastModified());
        httpServletResponse.setHeader("Content-Length", "" + fileObj.length());

        if (lcFileName.endsWith(".html") || lcFileName.endsWith(".htm")){
            httpServletResponse.setHeader("Content-Type", "text/html"); 
        }

     */
    public static void serveFile(ServletContext context, HttpServletRequest request, HttpServletResponse response, boolean content, File file) throws IOException {
        serveFile(context, request, response, content, ContentDisposition.INLINE, file);
    }

    public static void serveFile(ServletContext context, HttpServletRequest request, HttpServletResponse response, boolean content, ContentDisposition contentDisposition, File file) 
    throws IOException
    {
        //TODO: Hack, based on comments in http://seamframework.org/Community/LargeFileDownload
        if (response instanceof HttpServletResponseWrapper) {
            response = (HttpServletResponse) ((HttpServletResponseWrapper) response).getResponse();
        } 
        
        // Check if file actually exists in filesystem.
        if (file == null || !file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Prepare some variables. 
        String fileName = file.getName();
 
        //TODO: GP specific hack
        //handle special case for Axis
        if (fileName.startsWith("Axis")) {
            int idx = fileName.indexOf(".att_");
            if (idx >= 0) {
                idx += ".att_".length();
                fileName = fileName.substring(idx);
            }
        }
        
        // The ETag is an unique identifier of the file.
        long length = file.length();
        long lastModified = file.lastModified();
        String eTag = fileName + "_" + length + "_" + lastModified;

        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            response.setHeader("ETag", eTag); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setHeader("ETag", eTag); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }


        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }


        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<Range>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            // If-Range header should either match ETag or be greater then LastModified. If not,
            // then return full file.
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }


        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set default GZIP support and content disposition.
        String contentType = context.getMimeType(fileName);
        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        // TODO: commented out to be compatible with GP 3.3.1 and earlier
        //    The correct thing to do is to include mime-types for all of the known data types such as gct, res, odf, et cetera
        //    Setting to 'application/octet-stream' for the unknowns, such as gct, is causing some web clients to download the gct
        //    file rather than display inline.
        //if (contentType == null) {
        //    contentType = "application/octet-stream";
        //}

        // If content type is text, then determine whether GZIP content encoding is supported by
        // the client and expand content type with the one and right character encoding.
        boolean acceptsGzip = false;
        if (contentType != null && contentType.startsWith("text")) {
            String acceptEncoding = request.getHeader("Accept-Encoding");
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
            contentType += ";charset=UTF-8"; 
        }
        
        // determine content disposition
        // use a rule, unless explicitly specified by the contentDisposition arg, 
        String disposition = "inline";
        if (contentDisposition != null) {
            disposition = contentDisposition.toString().toLowerCase();
        } 
        if (!"attachment".equals(disposition)) {
            // Except for images, determine content disposition. 
            //If content type is supported by the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
            if (contentType != null && !contentType.startsWith("image")) {
                String accept = request.getHeader("Accept");
                disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
            }
        }
        
        //special-case for large input files
        int maxInlineSize = getMaxInlineSize();
        if (file.length() > maxInlineSize) {
            disposition = "attachment";
        }
        
        if (contentType == null && "attachment".equals(disposition)) {
            contentType = "application/octet-stream";
        }

        // Initialize response.
        response.reset();
        CorsFilter.applyCorsHeaders(request, response);
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setDateHeader("Last-Modified", lastModified);
        response.setHeader("ETag", eTag);
        response.setHeader("Accept-Ranges", "bytes");
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);
        response.setHeader("Content-Disposition", disposition + "; filename=\"" + fileName + "\"");
        if (contentType != null) {
            response.setContentType(contentType);
        }

        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        BufferedInputStream is = null;
        OutputStream output = null;

        try {
            // Open streams.
            is = new BufferedInputStream(new FileInputStream(file));
            output = response.getOutputStream();

            if (ranges.isEmpty() || ranges.get(0) == full) {
                // Return full file.
                Range r = full;
                if (contentType != null) {
                    response.setContentType(contentType);
                }
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                
                if (acceptsGzip) {
                    // The browser accepts GZIP, so GZIP the content.
                    response.setHeader("Content-Encoding", "gzip");
                }
                else {
                    // Content length is not directly predictable in case of GZIP.
                    // So only add it if there is no means of GZIP, else browser will hang.
                    response.setHeader("Content-Length", String.valueOf(r.length));
                }

                if (content) {
                    if (acceptsGzip) {
                        // The browser accepts GZIP, so GZIP the content.
                        output = new GZIPOutputStream(output, DEFAULT_BUFFER_SIZE);
                    } 
                    // Copy full range.
                    //copyRange(is, output, r.start, r.length);
                    copyRange(is, output, r.start, r.end);
                    response.setStatus(HttpServletResponse.SC_OK); // 200.
                }
            } 
            else if (ranges.size() == 1) {
                // Return single part of file.
                Range r = ranges.get(0);
                if (contentType != null) {
                    response.setContentType(contentType);
                }
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Copy single part range.
                    //copyRange(is, output, r.start, r.length);
                    copyRange(is, output, r.start, r.end);
                }
            } 
            else {
                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Cast back to ServletOutputStream to get the easy println methods.
                    ServletOutputStream sos = (ServletOutputStream) output;

                    // Copy multi part range.
                    for (Range r : ranges) {
                        // Add multipart boundary and header fields for every range.
                        sos.println();
                        sos.println("--" + MULTIPART_BOUNDARY);
                        if (contentType != null) {
                            sos.println("Content-Type: " + contentType);
                        }
                        sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                        // Copy single part range of multi part range.
                        //copyRange(is, output, r.start, r.length);
                        copyRange(is, output, r.start, r.end);
                    }

                    // End with multipart boundary.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY + "--");
                }
            }
        } 
        finally {
            // Gently close streams.
            close(output);
            close(is);
        }
    }


    // Helpers (can be refactored to public utility class) ----------------------------------------

    /**
     * Returns true if the given accept header accepts the given value.
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
            || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
            || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     * @param matchHeader The match header.
     * @param toMatch The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
            || Arrays.binarySearch(matchValues, "*") > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    protected static void skipper(InputStream istream, long start) throws IOException {
        long remainder = start;
        while(remainder > 0) {
            long numSkipped = istream.skip(remainder);
            if (numSkipped < 0) {
                numSkipped = 0;
            }
            remainder = remainder - numSkipped;
        }
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
    protected static IOException copyRange(InputStream istream, OutputStream ostream, long start, long end) {
        try {
            skipper(istream, start);
        } 
        catch (IOException e) {
            log.error("Error skipping bytes from inputstream: "+e.getLocalizedMessage(), e);
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

    /**
     * Close the given resource.
     * @param resource The resource to be closed.
     */
    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignore) {
                // Ignore IOException. If you want to handle this anyway, it might be useful to know
                // that this will generally only be thrown when the client aborted the request.
            }
        }
    }

    // Inner classes ------------------------------------------------------------------------------

    /**
     * This class represents a byte range.
     */
    static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

    }

}
