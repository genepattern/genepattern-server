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
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.util.GPConstants;
import org.jfree.util.Log;

public class UploadReceiver extends HttpServlet {
    private static final long serialVersionUID = -6720003935924717973L;
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter responseWriter = response.getWriter();
        RequestContext reqContext = new ServletRequestContext(request);
        if (FileUploadBase.isMultipartContent(reqContext)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> postParameters = upload.parseRequest(reqContext);
                int partitionCount = Integer.parseInt(getParameter(postParameters, "partitionCount"));
                if (partitionCount == 1) {
                    loadFile(request, postParameters, responseWriter);
                }
                else {
                    loadPartition(request, postParameters, responseWriter);
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

    private void loadPartition(HttpServletRequest request, List<FileItem> postParameters, PrintWriter responseWriter) throws Exception {
        String writeDirectory = getWriteDirectory(request, postParameters);
        Iterator<FileItem> it = postParameters.iterator();
        while (it.hasNext()) {
            FileItem postParameter = it.next();
            if (!postParameter.isFormField()) {
                File file = new File(writeDirectory, postParameter.getName());
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.seek(file.length());
                raf.write(postParameter.get());
                raf.close();
                responseWriter.println(writeDirectory + ";" + file.getCanonicalPath());
            }
        }
    }

    private void loadFile(HttpServletRequest request, List<FileItem> postParameters, PrintWriter responseWriter) throws Exception {
        String writeDirectory = getWriteDirectory(request, postParameters);
        Iterator<FileItem> it = postParameters.iterator();
        while (it.hasNext()) {
            FileItem postParameter = it.next();
            if (!postParameter.isFormField()) {
                File file = new File(writeDirectory, postParameter.getName());
                postParameter.write(file);
                responseWriter.println(writeDirectory + ";" + file.getCanonicalPath());
            }
        }
    }

    private String getParameter(List<FileItem> parameters, String param) {
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

    private String getWriteDirectory(HttpServletRequest request, List<FileItem> postParameters) throws IOException {
        String paramId = request.getParameter("paramId") != null ? request.getParameter("paramId") : "writeDirectory";
        String writeDirectory = (String) request.getSession().getAttribute(paramId);
        File directory = null;
        if (writeDirectory != null) {
            directory = new File(writeDirectory);

        }
        if (writeDirectory == null || !directory.isDirectory()) {
            String userName = (String) request.getSession().getAttribute(GPConstants.USERID);
            Context context = Context.getContextForUser(userName);
            // Create a temporary directory for the uploaded files.
            String prefix = userName + "_run";
            File dir = ServerConfiguration.instance().getUserUploadDir(context);
            
            // lazily create directory if need be
            if (!dir.exists()) {
                dir.mkdir();
            }
            
            // use createTempFile to guarantee a unique name, but then change it to a directory
            directory = File.createTempFile(prefix, null, dir);
            directory.delete();
            directory.mkdir();
            writeDirectory = directory.getCanonicalPath();
            request.getSession().setAttribute(paramId, writeDirectory);
        }
        return writeDirectory;
    }
}
