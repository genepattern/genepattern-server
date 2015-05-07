/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.uploads;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.webapp.LoginManager;

import java.io.*;

/**
 * Servlet implementation class AJAXUploadServlet
 * @author Thorin Tabor
 */
public class AJAXUploadServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(AJAXUploadServlet.class);
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public AJAXUploadServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * Handle post requests to the servlet
     *
     * All exceptions in the servlet are bubbled up to this level for
     * unified exception handling with our error messaging system
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter responseWriter = response.getWriter();
        String responseText = null;

        try {
            // Handle the case of there not being a current session ID
            final String userId = LoginManager.instance().getUserIdFromSession(request);
            if (userId == null) {
                // Return error to the applet; this happens if a user logged out during an upload
                throw new FileUploadException("No user ID attached to session");
            }

            GpContext userContext = GpContext.getContextForUser(userId);

            final int partitionCount = Integer.parseInt(request.getHeader("partitionCount"));
            final int partitionIndex = Integer.parseInt(request.getHeader("partitionIndex"));
            final String filename = request.getHeader("filename");
            final String uploadPath = request.getHeader("uploadPath");

            responseText = writeFile(userContext, request, partitionIndex, partitionCount, filename, uploadPath);

            returnUploadResponse(responseWriter, responseText);
        }
        catch (FileUploadException e) {
            returnErrorResponse(responseWriter, e);
        }
        catch (Exception e) {
            log.error("Unknown exception occurred in UploadReceiver.doPost(): " + e.getMessage());
            returnErrorResponse(responseWriter, new FileUploadException("Unknown error occurred: " + e.getMessage()));
        }
        finally {
            responseWriter.close();
        }
    }

    /**
     * Get the GenePattern file path to which to upload the file
     * @param userContext
     * @param uploadDirPath
     * @return
     * @throws FileUploadException
     */
    private File getUploadDirectory(GpContext userContext, String uploadDirPath) throws FileUploadException {
        if (uploadDirPath == null) {
            throw new FileUploadException("server error, missing session attribute 'uploadPath'");
        }

        GpFilePath dir;
        try {
            dir = GpFileObjFactory.getRequestedGpFileObj(uploadDirPath);


            // Handle special case for root uploads directory
            if (dir.getRelativeFile().getPath().equals("")) {
                dir = GpFileObjFactory.getUserUploadFile(userContext, new File("./"));
            }
        }
        catch (Exception e) {
            throw new FileUploadException("Could not get the appropriate directory path for file upload");
        }

        // lazily create directory if need be
        if (!dir.getServerFile().exists()) {
            boolean success = dir.getServerFile().mkdir();
            if (!success) {
                log.error("Failed to mkdir for dir=" + dir.getServerFile().getAbsolutePath());
                throw new FileUploadException("Could not get the appropriate directory for file upload");
            }
        }

        return dir.getRelativeFile();
    }

    /**
     * Append the contents of the fileItem to the given file.
     *
     * @param is - an InputStream with the file's binary data
     * @param to - the partial file to which to append the bytes
     * @throws IOException
     */
    private void appendPartition(InputStream is, File to) throws IOException {
        OutputStream os = null;

        try {
            os = new BufferedOutputStream(new FileOutputStream(to, true));
            final int BUFSIZE = 2048;
            final byte buf[] = new byte[BUFSIZE];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        catch (Throwable t) {
            log.error("Something thrown while writing file chunk: " + t.getLocalizedMessage(), t);
        }
        finally {
            is.close();
            os.close();
        }
    }

    /**
     * Get the GenePattern file path to which to upload the file.
     * @param userContext
     * @param request
     * @param name
     * @param uploadPath
     * @param first - if it's the first chunk of data, it means the file or directory should not be on the file system
     * @return
     * @throws FileUploadException - if there is a server error, or if there is a naming conflict with the hidden tmp dir.
     */
    private GpFilePath getUploadFile(GpContext userContext, HttpServletRequest request, String name, String uploadPath, boolean first) throws FileUploadException {
        final File uploadDir = getUploadDirectory(userContext, uploadPath);
        final File relativeFile = new File(uploadDir, name);

        try {
            boolean initMetaData = !first;
            //special-case, block 'tmp'
            final GpFilePath uploadFilePath = GpFileObjFactory.getUserUploadFile(userContext, relativeFile);
            if (DataManager.isTmpDir(uploadFilePath)) {
                throw new FileUploadException("Can't save file with reserved filename: "+relativeFile.getPath());
            }

            //final GpFilePath uploadFile = UserUploadManager.getUploadFileObj(userContext, uploadFilePath, initMetaData);
            return uploadFilePath;
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new FileUploadException("Error initializing upload file reference for '"+relativeFile.getPath()+"': "+e.getLocalizedMessage());
        }
    }

    /**
     * Write the file to disk and to the database
     * @param userContext
     * @param request
     * @param index
     * @param count
     * @param filename
     * @param uploadPath
     * @return
     * @throws FileUploadException
     * @throws IOException
     */
    private String writeFile(GpContext userContext, HttpServletRequest request, int index, int count, String filename, String uploadPath) throws FileUploadException, IOException {
        final boolean first = index == 0;
        String responseText = "";

        GpFilePath file = getUploadFile(userContext, request, filename, uploadPath, first);

        // Check if the file exists and throw an error if it does
        if (first && file.getServerFile().exists() && file.getServerFile().length() > 0) {
            throw new FileUploadException("File already exists");
        }

        InputStream is = request.getInputStream();

        if (first) {
            try {
                UserUploadManager.createUploadFile(userContext, file, count);
            }
            catch (Throwable t) {
                log.error("Error creating entry in DB for '" + file.getName() + "': " + t.getLocalizedMessage(), t);
                throw new FileUploadException("Error creating entry in DB for '" + file.getName() + "': " + t.getLocalizedMessage());
            }
        }

        try {
            appendPartition(is, file.getServerFile());
        }
        catch (Throwable t) {
            log.error("Error appending partition for '" + file.getName() + "': " + t.getLocalizedMessage(), t);
            throw new FileUploadException("Error appending partition for '"+file.getName()+"': "+t.getLocalizedMessage());
        }

        try {
            UserUploadManager.updateUploadFile(userContext, file, index + 1, count);
        }
        catch (Throwable t) {
            log.error("Error updating database after file chunk appended '" + file.getName() + "': " + t.getLocalizedMessage(), t);
            throw new FileUploadException(t.getLocalizedMessage());
        }

        responseText += file.getRelativePath();

        return responseText;
    }

    public void returnErrorResponse(PrintWriter responseWriter, FileUploadException error) {
        responseWriter.println("Error: " + error.getMessage());
    }

    public void returnUploadResponse(PrintWriter responseWriter, String message) {
        responseWriter.println(message);
    }
}