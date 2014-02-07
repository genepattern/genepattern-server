package org.genepattern.server.webapp.uploads;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.util.FacesUtil;
import org.genepattern.server.webapp.uploads.UploadFilesBean.DirectoryInfoWrapper;
import org.genepattern.server.webapp.uploads.UploadFilesBean.FileInfoWrapper;

/**
 * Servlet to back the new upload files tree
 * @author tabor
 *
 */
public class UploadFileServlet extends HttpServlet {
    private static final long serialVersionUID = -7008649202244924080L;
    public static Logger log = Logger.getLogger(UploadFileServlet.class);
    
    public static final String TREE = "/tree";
    public static final String SAVE_TREE = "/saveTree";
    public static final String SAVE_FILE = "/saveFile";
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getPathInfo();
        
        // Route to the appropriate action, returning an error if unknown
        if (TREE.equals(action)) {
            loadTreeLevel(request, response);
        }
        if (SAVE_TREE.equals(action)) {
            loadTreeLevel(request, response);
        }
        else {
            // Default to tree if unknown
            loadTreeLevel(request, response);
        }
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
    
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
    
    private List<GpFilePath> unwrapFiles(List<FileInfoWrapper> wrappedFiles) {
        List<GpFilePath> files = new ArrayList<GpFilePath>();
        for (FileInfoWrapper wrapper : wrappedFiles) {
            files.add(wrapper.getFile());
        }
        return files;
    }
    
    private void loadTreeLevel(HttpServletRequest request, HttpServletResponse response) {
        UploadFilesBean bean = getUploadsBean(request, response);
        String url = request.getParameter("dir");
        
        List<GpFilePath> tree = null;
        if (url == null) {
            try {
                List<FileInfoWrapper> wrappedFiles = ((DirectoryInfoWrapper) bean.getFileTree().getData()).getFiles();
                tree = unwrapFiles(wrappedFiles);
            }
            catch (Exception e) {
                log.error("Error getting root of upload file tree");
            }
        }
        else {
            DirectoryInfoWrapper dir = bean.getDirectory(url);
            if (dir != null) {
                tree = unwrapFiles(dir.getFiles());
            }
        }
        
        UploadTreeJSON json = null;
        if (tree != null && !tree.isEmpty()) {
            json = new UploadTreeJSON(tree);
        }
        else {
            json = new UploadTreeJSON(null, UploadTreeJSON.EMPTY);
        }
        this.write(response, json);
    }
    
    @SuppressWarnings("deprecation")
    private UploadFilesBean getUploadsBean(HttpServletRequest request, HttpServletResponse response) {
        // Get the FacesContext inside HttpServlet.
        FacesContext facesContext = FacesUtil.getFacesContext(request, response);   
        return (UploadFilesBean) facesContext.getApplication().createValueBinding("#{uploadFilesBean}").getValue(facesContext);
    }
    
    private void write(HttpServletResponse response, Object content) {
        this.write(response, content.toString());
    }
    
    private void write(HttpServletResponse response, String content) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println(content);
            writer.flush();
        }
        catch (IOException e) {
            log.error("Error writing to the response in PipelineQueryServlet: " + content);
            e.printStackTrace();
        }
        finally {
            if (writer != null) writer.close();
        }
    }
}
