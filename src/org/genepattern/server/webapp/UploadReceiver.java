package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.domain.UploadFile;
import org.genepattern.server.domain.UploadFileDAO;

public class UploadReceiver extends HttpServlet {
    private static Logger log = Logger.getLogger(UploadReceiver.class);
    private static final long serialVersionUID = -6720003935924717973L;
    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        // Handle the case of there not being a current session ID
        String userId = LoginManager.instance().getUserIdFromSession(request);
        if (userId == null) {
            // Return error to the applet; this happens if a user logged out during an upload
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No user ID attached to session");
            return;
        }
        
        PrintWriter responseWriter = response.getWriter();
        RequestContext reqContext = new ServletRequestContext(request);
        if (FileUploadBase.isMultipartContent(reqContext)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> postParameters = upload.parseRequest(reqContext);
                int partitionCount = Integer.parseInt(getParameter(postParameters, "partitionCount"));
                int partitionIndex = Integer.parseInt(getParameter(postParameters, "partitionIndex"));
                boolean firstPartition = partitionIndex == 0;
                boolean lastPartition = (partitionIndex + 1) == partitionCount;
                if (partitionCount == 1) {
                    loadFile(request, postParameters, responseWriter, userId);
                }
                else {
                    loadPartition(request, postParameters, responseWriter, firstPartition, lastPartition, userId);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            responseWriter.close();
        }
        else {
            // This servlet wasn't called by a multi-file uploader. Return an error page.
            response.sendRedirect(request.getContextPath() + "/pages/internalError.jsf");
        }
    }

    public void returnErrorResponse(PrintWriter responseWriter, String error) {
        responseWriter.println("Error: " + error);
    }
    
    public void returnUploadResponse(PrintWriter responseWriter, String message) {
        responseWriter.println("Error: " + message);
    }
    
    protected File getWriteDirectory(HttpServletRequest request) throws IOException {
        String userId = LoginManager.instance().getUserIdFromSession(request);
        Context context = Context.getContextForUser(userId);
        File dir = ServerConfiguration.instance().getUserUploadDir(context);
        
        // lazily create directory if need be
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        return dir;
    }

    /**
     * 
     * @param request
     * @param fileItem
     * @param first, true if this is the first (or only) part of the file
     * @param part, true if the upload is partitioned
     * @return
     * @throws IOException
     */
    protected File getWriteFile(HttpServletRequest request, FileItem fileItem, boolean first, boolean part) throws IOException {
        File parentDir = getWriteDirectory(request);
        File file = getWriteFile(parentDir, fileItem, first, part);
        return file;
    }
    
    protected File getWriteFile(File uploadDir, FileItem file, boolean isFirst, boolean isPart) throws IOException {
        File writeFile = new File(uploadDir, file.getName());

        if (isFirst) {
            // Check if file exists,
            // only if this is the first or only part of the file 
            if (writeFile.exists()) {
                log.debug("File already exists: "+writeFile.getAbsolutePath());
                throw new IOException("File already exists");
            }
        }
        
        if (isPart) {
            //rule for locating partial uploads
            //TODO, consider storing upload status in the DB rather than by naming convention
            writeFile = new File(writeFile.getParent(), writeFile.getName() + ".part");
        }
        return writeFile;        
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
    
    protected void loadFile(HttpServletRequest request, List<FileItem> postParameters, PrintWriter responseWriter, String userId) throws Exception {
        boolean isFirst = true;
        boolean isPart = false;

        for(FileItem fileItem : postParameters) {
            if (!fileItem.isFormField()) {
                File uploadedFile = getWriteFile(request, fileItem, isFirst, isPart);
                fileItem.write(uploadedFile);
                handleFileCompleted(userId, uploadedFile);
                responseWriter.println(uploadedFile.getParent() + ";" + uploadedFile.getCanonicalPath());
            }
        }
    }
    
    protected void loadPartition(HttpServletRequest request, List<FileItem> postParameters, PrintWriter responseWriter, boolean isFirst, boolean isLast, String userId) throws Exception { 
        boolean isPart = true;
        for(FileItem fileItem : postParameters) {
            if (!fileItem.isFormField()) {
                File partialFile = getWriteFile(request, fileItem, isFirst, isPart);
                RandomAccessFile raf = new RandomAccessFile(partialFile, "rw");
                raf.seek(partialFile.length());
                //TODO: not sure if this is the most effective way to stream the data
                byte[] bytes = fileItem.get();
                raf.write(bytes);
                raf.close();
                if (isLast) {
                    File uploadedFile = getWriteFile(request, fileItem, false, false);
                    boolean success = partialFile.renameTo(uploadedFile);
                    if (!success) {
                        //TODO: throw exception
                    }
                    else {
                        handleFileCompleted(userId, uploadedFile);
                    }
                }
                responseWriter.println(partialFile.getParent() + ";" + partialFile.getCanonicalPath());
            }
        }
    }

    private void handleFileCompleted(String userId, File file) throws Exception {
        if (log.isDebugEnabled()) {
            try {
                log.debug("Uploaded file to: "+file.getAbsolutePath());
            }
            catch (Throwable t) {
                log.error("Error writing log!", t);
            }
        } 
        
        //record the uploaded file into the DB
        UploadFileDAO dao = new UploadFileDAO();
        UploadFile uploadFile = new UploadFile();
        uploadFile.initFromFile(file);
        uploadFile.setUserId(userId);
                
        dao.save(uploadFile);
    }
    
}
