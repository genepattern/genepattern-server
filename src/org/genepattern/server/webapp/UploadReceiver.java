package org.genepattern.server.webapp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.uploads.UploadFilesBean;
import org.genepattern.server.webapp.uploads.UploadFilesBean.FileInfoWrapper;

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
     * Get the parent directory on the server file system to which to upload the file
     * @param request
     * @return
     * @throws FileUploadException
     */
    protected File getUploadDirectory(HttpServletRequest request) throws FileUploadException {
        final String userId = LoginManager.instance().getUserIdFromSession(request);
        final Context context = Context.getContextForUser(userId);
        final File dir = ServerConfiguration.instance().getUserUploadDir(context);
        
        // lazily create directory if need be
        if (!dir.exists()) {
            boolean success = dir.mkdir();
            if (!success) {
                log.error("Failed to mkdir for dir="+dir.getAbsolutePath());
                throw new FileUploadException("Could not get the appropriate directory for file upload");
            }
        }
        
        return dir;
    }
    
    /**
     * Get the path on the server file system to which to upload the file.
     * 
     * @param request
     * @param name
     * @return
     * @throws FileUploadException
     */
    protected File getUploadFile(HttpServletRequest request, String name) throws FileUploadException {
        File parentDir = getUploadDirectory(request);
        File file = getUploadFile(parentDir, name);
        return file;
    }
    
    protected File getUploadFile(File uploadDir, String name) throws FileUploadException {
        return new File(uploadDir, name);
    }
    
    // Older implementation of appendPartition
    private void appendPartitionOld(FileItem from, File to) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(to, "rw");
        raf.seek(to.length());
        byte[] bytes = from.get();
        raf.write(bytes);
        raf.close();
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
    
    private void updateDatabase(String userId, File file, int status) throws FileUploadException {
        if (log.isDebugEnabled()) {
            log.debug("Uploaded file to: "+file.getAbsolutePath());
        } 

        try {
            //record the uploaded file into the DB
            final UploadFile uploadFile = new UploadFile();
            uploadFile.initFromFile(file, status);
            uploadFile.setUserId(userId);
            
            //constructor begins a Hibernate Transaction
            UploadFileDAO dao = new UploadFileDAO();
            dao.saveOrUpdate(uploadFile);
            HibernateUtil.commitTransaction();
        }
        catch (IOException e) {
            throw new FileUploadException("Problem uploading file");
        }
        catch (Exception e) {
            log.error(e);
            HibernateUtil.rollbackTransaction();
            throw new FileUploadException("Problem uploading file to database");
        }
    }
    
    private void databaseAddIfNecessary(String userId, File file) throws FileUploadException {
        List<UploadFile> dbFiles = new UploadFileDAO().findByUserId(userId);
        boolean foundMatch = false;
        for (UploadFile i : dbFiles) {
            if (i.getPath().equals(file.getAbsolutePath())) {
                foundMatch = true;
                break;
            }
        }
        if (!foundMatch) {
            updateDatabase(userId, file, UploadFile.COMPLETE);
        }
    }
    
    protected String writeFile(HttpServletRequest request, List<FileItem> postParameters, boolean first, boolean last, String userId) throws FileUploadException { 
        final boolean partial = !(first && last);
        String responeText = "";
        for(FileItem fileItem : postParameters) {
            if (!fileItem.isFormField()) {
                File file = getUploadFile(request, fileItem.getName()); 
                
                // Check if the file exists and throw an error if it does
                if (first && file.exists()) {
                    databaseAddIfNecessary(userId, file);
                    throw new FileUploadException("File already exists");
                }
                
                // If partial file, set file to be .part, removing old file parts first
                if (partial) {
                    updateDatabase(userId, file, UploadFile.PARTIAL);
                }
                
                try {
                    appendPartition(fileItem, file);
                }
                catch (IOException e) {
                    throw new FileUploadException("Problems appending partition onto uploaded file");
                }
                
                // Do final tasks for the last partition
                if (last) {
                    // If last partition, rename .part file to actual file
                    updateDatabase(userId, file, UploadFile.COMPLETE);
                }
                try {
                    responeText += file.getParent() + ";" + file.getCanonicalPath();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return responeText;
    }
    
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
            
            RequestContext reqContext = new ServletRequestContext(request);
            if (FileUploadBase.isMultipartContent(reqContext)) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> postParameters = upload.parseRequest(reqContext);
                final int partitionCount = Integer.parseInt(getParameter(postParameters, "partitionCount"));
                final int partitionIndex = Integer.parseInt(getParameter(postParameters, "partitionIndex"));
                final boolean firstPartition = partitionIndex == 0;
                final boolean lastPartition = (partitionIndex + 1) == partitionCount;
                responseText = writeFile(request, postParameters, firstPartition, lastPartition, userId);
                
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
        finally {
            responseWriter.close();
        } 
    }
}
