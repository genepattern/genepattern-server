package org.genepattern.server.webapp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.UploadFile;
import org.genepattern.server.domain.UploadFileDAO;

public class UploadReceiver extends HttpServlet {
    private static Logger log = Logger.getLogger(UploadReceiver.class);
    private static final long serialVersionUID = -6720003935924717973L;
    
    
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        
        // Handle the case of there not being a current session ID
        final String userId = LoginManager.instance().getUserIdFromSession(request);
        if (userId == null) {
            returnErrorResponse(response, "No user ID attached to session");
            return;
        }

        RequestContext reqContext = new ServletRequestContext(request);
        if (FileUploadBase.isMultipartContent(reqContext)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> postParameters = upload.parseRequest(reqContext);
                final int partitionCount = Integer.parseInt(getParameter(postParameters, "partitionCount"));
                final int partitionIndex = Integer.parseInt(getParameter(postParameters, "partitionIndex"));
                final boolean firstPartition = partitionIndex == 0;
                final boolean lastPartition = (partitionIndex + 1) == partitionCount;
                if (partitionCount == 1) {
                    loadFile(request, response, postParameters, userId);
                }
                else {
                    loadPartition(request, response, postParameters, firstPartition, lastPartition, userId);
                }
            }
            catch (Exception e) {
                returnErrorResponse(response, e.getLocalizedMessage());
            }
        }
        else {
            returnErrorResponse(response, "Servlet wasn't called by a multi-file uploader");
            return;
        }
    }

    public void returnErrorResponse(HttpServletResponse response, String error) throws IOException {
        log.debug("Error handling upload request: "+error);
        response.getWriter().println("Error: " + error);
    }
    
    public void returnUploadResponse(HttpServletResponse response, String message) throws IOException {
        response.getWriter().println(message);
    }
    
    protected String getParameter(final List<FileItem> parameters, final String param) {
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
    
    protected void loadFile(final HttpServletRequest request, final HttpServletResponse response, final List<FileItem> postParameters, final String userId) throws Exception {
        final boolean isFirst = true;
        final boolean isPart = false;

        for(FileItem fileItem : postParameters) {
            if (!fileItem.isFormField()) {
                File uploadedFile = getUploadFile(request, response, fileItem, isFirst, isPart);
                fileItem.write(uploadedFile);
                handleFileUploadCompleted(userId, uploadedFile);
                returnUploadResponse(response, uploadedFile.getParent() + ";" + uploadedFile.getCanonicalPath());
            }
        }
    }
    
    protected void loadPartition(final HttpServletRequest request, final HttpServletResponse response, final List<FileItem> postParameters, final boolean isFirst, final boolean isLast, final String userId) throws Exception { 
        final boolean isPart = true;
        for(FileItem fileItem : postParameters) {
            if (!fileItem.isFormField()) {
                File partialFile = getUploadFile(request, response, fileItem, isFirst, isPart); 
                appendToFile(fileItem, partialFile);
                
                if (isLast) {
                    File uploadedFile = getUploadFile(request, response, fileItem, false, false);
                    boolean success = partialFile.renameTo(uploadedFile);
                    if (!success) {
                        throw new FileUploadException("Error renaming upload file: from="+partialFile.getAbsolutePath() + " to="+uploadedFile.getAbsolutePath());
                    }
                    else {
                        handleFileUploadCompleted(userId, uploadedFile);
                    }
                }
                returnUploadResponse(response, partialFile.getParent() + ";" + partialFile.getCanonicalPath());
            }
        }
    }

    /**
     * Get the parent directory on the server file system to which to upload the file
     * @param request
     * @return
     * @throws IOException
     */
    protected File getUploadDirectory(HttpServletRequest request) throws FileUploadException {
        final String userId = LoginManager.instance().getUserIdFromSession(request);
        final Context context = Context.getContextForUser(userId);
        final File dir = ServerConfiguration.instance().getUserUploadDir(context);
        
        // lazily create directory if need be
        if (!dir.exists()) {
            boolean success = dir.mkdir();
            if (!success) {
                throw new FileUploadException("Failed to mkdir for dir="+dir.getAbsolutePath());
            }
        }
        
        return dir;
    }

    /**
     * Get the path on the server file system to which to upload the file.
     * 
     * @param request
     * @param fileItem
     * @param first, true if this is the first (or only) part of the file
     * @param part, true if the upload is partitioned
     * @return
     * @throws IOException
     */
    protected File getUploadFile(final HttpServletRequest request, final HttpServletResponse response, final FileItem fileItem, final boolean first, final boolean part) throws FileUploadException {
        File parentDir = getUploadDirectory(request);
        File file = getUploadFile(parentDir, fileItem, first, part);
        return file;
    }
    
    protected File getUploadFile(final File uploadDir, final FileItem file, final boolean isFirst, final boolean isPart)
    throws FileUploadException {
        File writeFile = new File(uploadDir, file.getName());

        if (isFirst) {
            // Check if file exists,
            // only if this is the first or only part of the file 
            if (writeFile.exists()) {
                log.debug("File already exists: "+writeFile.getAbsolutePath());
                throw new FileUploadException("File already exists");
            }
        }
        
        if (isPart) {
            //rule for locating partial uploads
            //TODO, consider storing upload status in the DB rather than by naming convention
            writeFile = new File(writeFile.getParent(), writeFile.getName() + ".part");
        }
        
        //special-case
        if (isFirst && isPart && writeFile.exists()) {
            //I interpret this to mean that a user is attempting to redo a failed upload
            log.debug("Partial file already exists, deleting and starting new upload: "+writeFile.getAbsolutePath());
            boolean success = writeFile.delete();
            if (!success) {
                throw new FileUploadException("Server error deleting interrupted partial upload file: "+writeFile.getAbsolutePath());
            }
        }
        return writeFile; 
    }

    /**
     * Append the contents of the fileItem to the given file.
     * 
     * @param from, a FileItem from the POST
     * @param to, the partial file to which to append the bytes
     * @throws IOException
     */
    private void appendToFile(FileItem from, File to) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        
        final boolean append = true;
        try {
            is = from.getInputStream();
            os = new BufferedOutputStream(new FileOutputStream(to, append));
            final int BUFSIZE = 2048;
            final byte buf[] = new byte[BUFSIZE];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        finally {
            try {
                is.close();
            }
            catch (IOException e) {
                
            }
            try {
                os.close();
            }
            catch (IOException e) {
                
            }
        }
    }

    private void handleFileUploadCompleted(final String userId, final File file) throws FileUploadException {
        if (log.isDebugEnabled()) {
            try {
                log.debug("Uploaded file to: "+file.getAbsolutePath());
            }
            catch (Throwable t) {
                log.error("Error writing log!", t);
            }
        } 
        
        //record the uploaded file into the DB
        final UploadFile uploadFile;
        try {
            uploadFile = new UploadFile();
            uploadFile.initFromFile(file);
            uploadFile.setUserId(userId);
        }
        catch (Throwable t) {
            log.error(t);
            throw new FileUploadException("Error preparing record for database, uploaded_file="+file.getAbsolutePath());
        }
        try {
            //constructor begins a Hibernate Transaction
            UploadFileDAO dao = new UploadFileDAO();
            dao.saveOrUpdate(uploadFile);
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            log.error(e);
            HibernateUtil.rollbackTransaction();
            throw new FileUploadException("Error recording to datebase, uploaded_file="+file.getAbsolutePath());
        }
    }
}
