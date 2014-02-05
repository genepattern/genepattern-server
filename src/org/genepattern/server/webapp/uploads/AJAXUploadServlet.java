package org.genepattern.server.webapp.uploads;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.webapp.LoginManager;

/**
 * Servlet implementation class AJAXUploadServlet
 */
public class AJAXUploadServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(AJAXUploadServlet.class);
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public AJAXUploadServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
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

            ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);

            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
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
            log.error("Unknown exception occured in UploadReceiver.doPost(): " + e.getMessage());
            returnErrorResponse(responseWriter, new FileUploadException("Unknown error occured: " + e.getMessage()));
        }
        finally {
            responseWriter.close();
        }
    }

    private File getUploadDirectory(ServerConfiguration.Context userContext, String uploadDirPath) throws FileUploadException {
        if (uploadDirPath == null) {
            throw new FileUploadException("server error, missing session attribute 'uploadPath'");
        }
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
        finally {
            is.close();
            os.close();
        }
    }

    private GpFilePath getUploadFile(ServerConfiguration.Context userContext, HttpServletRequest request, String name, String uploadPath, boolean first) throws FileUploadException {
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
            log.error(e.getMessage());
            throw new FileUploadException("Error initializing upload file reference for '"+relativeFile.getPath()+"': "+e.getLocalizedMessage());
        }
    }

    private String writeFile(ServerConfiguration.Context userContext, HttpServletRequest request, int index, int count, String filename, String uploadPath) throws FileUploadException, IOException {
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
                throw new FileUploadException("Error creating entry in DB for '"+file.getName()+"': "+t.getLocalizedMessage());
            }
        }

        try {
            appendPartition(is, file.getServerFile());
        }
        catch (Throwable t) {
            throw new FileUploadException("Error appending partition for '"+file.getName()+"': "+t.getLocalizedMessage());
        }

        try {
            UserUploadManager.updateUploadFile(userContext, file, index + 1, count);
        }
        catch (Throwable t) {
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
