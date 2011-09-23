package org.genepattern.server.webapp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;

public class UploadReceiver extends HttpServlet {
    private static Logger log = Logger.getLogger(UploadReceiver.class);
    private static final long serialVersionUID = -6720003935924717973L;
    
    public void returnErrorResponse(PrintWriter responseWriter, FileUploadException error) {
        responseWriter.println("Error: " + error.getMessage());
    }
    
    public void returnUploadResponse(PrintWriter responseWriter, String message) {
        responseWriter.println(message);
    }
    
    protected String getParameter(List<FileItem> parameters, String param) {
        Iterator<FileItem> it = parameters.iterator();
        while (it.hasNext()) {
            FileItem postParameter = it.next();
            if (postParameter.isFormField()) {
                if (param.compareTo(postParameter.getFieldName()) == 0) {
                    return postParameter.getString();
                }
            }
        }
        return null;
    }
    
    /**
     * Get the GenePattern file path to which to upload the file
     * @param request
     * @return
     * @throws FileUploadException
     */
    private File getUploadDirectory(Context userContext, HttpServletRequest request) throws FileUploadException {
        String uploadDirPath = (String) request.getSession().getAttribute("uploadPath");
        if (!uploadDirPath.startsWith("./")) {
            uploadDirPath = "./" + uploadDirPath;
        }
        GpFilePath dir;
        try {
            dir = GpFileObjFactory.getUserUploadFile(userContext, new File(uploadDirPath));
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
     * Get the GenePattern file path to which to upload the file.
     * 
     * @param request
     * @param name
     * @return
     * @throws FileUploadException
     */
    private GpFilePath getUploadFile(Context userContext, HttpServletRequest request, String name) throws FileUploadException {
        File parentDir = getUploadDirectory(userContext, request);
        GpFilePath file = getUploadFile(userContext, parentDir, name);
        return file;
    }
    
    protected GpFilePath getUploadFile(Context userContext, File uploadDir, String name) throws FileUploadException {
        File file = new File(uploadDir, name);
        try {
            return UserUploadManager.getUploadFileObj(userContext, file);
        }
        catch (Exception e) {
            log.error(e.getMessage());
            throw new FileUploadException("Unable to retrieve the uploaded file");
        }
    }
    
    /**
     * Append the contents of the fileItem to the given file.
     * 
     * @param from, a FileItem from the POST
     * @param to, the partial file to which to append the bytes
     * @throws IOException
     */
    private void appendPartition(FileItem from, File to) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        
        try {
            is = from.getInputStream();
            os = new BufferedOutputStream(new FileOutputStream(to, true));
            final int BUFSIZE = 2048;
            final byte buf[] = new byte[BUFSIZE];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        finally {
            is.close();
            os.close();
        }
    }
    
    /**
     * Write the file to disk and to the database
     * @param request
     * @param postParameters
     * @param index
     * @param count
     * @param userId
     * @return
     * @throws FileUploadException
     */
    private String writeFile(Context userContext, HttpServletRequest request, List<FileItem> postParameters, int index, int count, String userId) throws FileUploadException { 
        // final boolean partial = !(count == 1);
        final boolean first = index == 0;
        // final boolean last = (index + 1) == count;
        String responeText = "";
        for(FileItem fileItem : postParameters) {
            if (!fileItem.isFormField()) {
                GpFilePath file = getUploadFile(userContext, request, fileItem.getName()); 
                
                if (first) {
                    try { 
                        UserUploadManager.createUploadFile(userContext, file, count); 
                    } 
                    catch (Exception e) { 
                        throw new FileUploadException("File already exists in system"); 
                    }
                }
                
                // Check if the file exists and throw an error if it does
                if (first && file.getServerFile().exists()) {
                    throw new FileUploadException("File already exists");
                }
                
                try {
                    appendPartition(fileItem, file.getServerFile());
                }
                catch (IOException e) {
                    throw new FileUploadException("Problems appending partition onto uploaded file");
                }
                
                try {
                    UserUploadManager.updateUploadFile(userContext, file, index + 1, count);
                }
                catch (Exception e) {
                    throw new FileUploadException("File part received out of order for " + file.getServerFile().getName());
                }
                
                try {
                    responeText += file.getServerFile().getParent() + ";" + file.getServerFile().getCanonicalPath();
                }
                catch (IOException e) {
                    log.error("Error generating response text for canonical paths");
                }
            }
        }
        return responeText;
    }
    
    /**
     * Handle post requests to the servlet
     * 
     * All exceptions in the servlet are bubbled up to this level for 
     * unified exception handling used by JumpLoader's error messaging system
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
            Context userContext = ServerConfiguration.Context.getContextForUser(userId);
            
            RequestContext reqContext = new ServletRequestContext(request);
            if (FileUploadBase.isMultipartContent(reqContext)) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> postParameters = upload.parseRequest(reqContext);
                final int partitionCount = Integer.parseInt(getParameter(postParameters, "partitionCount"));
                final int partitionIndex = Integer.parseInt(getParameter(postParameters, "partitionIndex"));
                responseText = writeFile(userContext, request, postParameters, partitionIndex, partitionCount, userId); 
            }
            else {
                // This servlet wasn't called by a multi-file uploader. Return an error page.
                response.sendRedirect(request.getContextPath() + "/pages/internalError.jsf");
            } 
            
            returnUploadResponse(responseWriter, responseText);
        }
        catch (FileUploadException e) {
            returnErrorResponse(responseWriter, e);
        } 
        catch (Exception e) {
            log.error("Unknown exception occured in UploadReceiver.doPost(): " + e.getMessage());
            returnErrorResponse(responseWriter, new FileUploadException("Unknown error occured: " + e.getMessage()));
        }
        finally {
            responseWriter.close();
        } 
    }
}
