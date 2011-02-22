package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.GPConstants;

public class MultiFileUploadReceiver extends HttpServlet {
    private static final long serialVersionUID = -6720003935924717973L;
    private static Logger log =  Logger.getLogger(MultiFileUploadReceiver.class);

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
        int partitionNumber = Integer.parseInt(getParameter(postParameters, "partitionIndex"));
        String paritionDir = getWriteDirectory(request, postParameters, true);
        Iterator<FileItem> it = postParameters.iterator();
        while (it.hasNext()) {
            FileItem postParameter = it.next();
            if (!postParameter.isFormField()) {
                File file = new File(paritionDir, postParameter.getName() + "." + partitionNumber);
                postParameter.write(file);
                break;
            }
        }

        int partitionCount = Integer.parseInt(getParameter(postParameters, "partitionCount"));
        if (partitionNumber == (partitionCount - 1)) {
            String partitionedFileName = getParameter(postParameters, "fileName");
            
            String wholeDir = getWholeDirectory(request, postParameters);
            File wholeFile = new File(wholeDir, partitionedFileName);
            OutputStream out = new FileOutputStream(wholeFile);
            byte[] buffer = new byte[65536];
            for (int i = 0; i < partitionCount; i++) {
                File portion = new File(paritionDir, partitionedFileName + "." + i);
                FileInputStream in = new FileInputStream(portion);
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                in.close();
                portion.delete();
            }
            out.close();
            long fileLength = Long.parseLong(getParameter(postParameters, "fileLength"));
            if (wholeFile.length() == fileLength) {
                responseWriter.println(paritionDir + ";" + wholeFile.getCanonicalPath());
            }
            else {
                responseWriter.println("Error: Upload validation error");
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
    
    private String getWholeDirectory(HttpServletRequest request, List<FileItem> postParameters) {
        return (String) request.getSession().getAttribute("wholeDirectory");
    }
    
    private String getWriteDirectory(HttpServletRequest request, List<FileItem> postParameters) throws IOException {
        return getWriteDirectory(request, postParameters, false);
    }

    private String getWriteDirectory(HttpServletRequest request, List<FileItem> postParameters, boolean filePart) throws IOException {
        String paramId = getParameter(postParameters, "paramId");
        String directUpload = getParameter(postParameters, "directUpload");
        String writeDirectory = (String) request.getSession().getAttribute(paramId);
        File directory = null;
        if (writeDirectory != null) {
            directory = new File(writeDirectory);

        }
        if (writeDirectory == null || !directory.isDirectory()) {
            String userName = (String) request.getSession().getAttribute(GPConstants.USERID);
            // Create a temporary directory for the uploaded files.
            String prefix = userName + "_run";
            File dir = null;
            if (directUpload != null) {
                Context context = Context.getContextForUser(userName);
                dir = ServerConfiguration.instance().getUserUploadDir(context);
                
                // lazily create uploads directory if need be
                if (!dir.exists()) {
                    dir.mkdir();
                }
                
                // If this is a part of a larger file
                if (filePart) {
                    File partDir = new File(dir.getAbsoluteFile() + "/parts");
                    if (!partDir.exists()) {
                        partDir.mkdir();
                    }
                    directory = File.createTempFile(prefix, null, dir);
                    directory.delete();
                    directory.mkdir();
                    writeDirectory = directory.getCanonicalPath();
                    dir = partDir;
                    request.getSession().setAttribute("wholeDirectory", writeDirectory);
                }
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
