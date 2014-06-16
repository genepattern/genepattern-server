package org.genepattern.server.webapp.uploads;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.util.FacesUtil;
import org.genepattern.server.webapp.uploads.UploadFilesBean.DirectoryInfoWrapper;
import org.genepattern.server.webapp.uploads.UploadFilesBean.FileInfoWrapper;
import org.genepattern.webservice.TaskInfo;
import org.richfaces.model.TreeNode;

/**
 * Servlet to back the new upload files tree
 * @author tabor
 *
 */
public class UploadFileServlet extends HttpServlet {
    private static final long serialVersionUID = -7008649202244924080L;
    public static Logger log = Logger.getLogger(UploadFileServlet.class);
    
    /**
     * Sort a list of GpFilePath by filename, case insensitive. Child directories are listed before child files.
     * This assumes that all items in the list to be sorted are at the same level.
     */
    public static final Comparator<GpFilePath> dirFirstComparator=new Comparator<GpFilePath>() {

        @Override
        public int compare(final GpFilePath arg0, final GpFilePath arg1) {
            if (arg0==null) {
                if (arg1==null) {
                    return 0;
                }
                return -1;
            }
            if (arg1==null) {
                return 1;
            }
            //directories first
            if (arg0.isDirectory()) {
                if (!arg1.isDirectory()) {
                    return -1;
                }
            }
            else if (arg1.isDirectory()) {
                return 1;
            }
            return arg0.getName().compareToIgnoreCase(arg1.getName());
        }
    };

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
        else if (SAVE_TREE.equals(action)) {
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
    
    private void loadTreeLevel(final HttpServletRequest request, final HttpServletResponse response) {
        final UploadTreeJSON json=initUploadTreeFromBean(request, response);
        this.write(response, json);
    }
    
    /**
     * @deprecated, should remove dependency on JSF stack
     * @param request
     * @param response
     */
    private static UploadTreeJSON initUploadTreeFromBean(HttpServletRequest request, HttpServletResponse response) {
        final String url = request.getParameter("dir");
        final UploadFilesBean bean = getUploadsBean(request, response);
        final List<GpFilePath> tree=initTreeLevelFromBean(bean, url);
        final Map<String, SortedSet<TaskInfo>> kindToTaskInfo=bean.getKindToTaskInfo();
        final UploadTreeJSON treeJson=treeToJson(tree, kindToTaskInfo);
        return treeJson;
    }

    private static UploadFilesBean getUploadsBean(HttpServletRequest request, HttpServletResponse response) {
        // Get the FacesContext inside HttpServlet.
        FacesContext facesContext = FacesUtil.getFacesContext(request, response);   
        return (UploadFilesBean) facesContext.getApplication().createValueBinding("#{uploadFilesBean}").getValue(facesContext);
    }
    
    private static List<GpFilePath> initTreeLevelFromBean(final UploadFilesBean bean, final String url) {
        List<GpFilePath> tree = null;
        if (url == null) {
            try {
                final TreeNode<FileInfoWrapper> fileTree= bean.getFileTree();
                final List<FileInfoWrapper> wrappedFiles = ((DirectoryInfoWrapper) fileTree.getData()).getFiles();
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
        return tree;
    }

    private static List<GpFilePath> unwrapFiles(List<FileInfoWrapper> wrappedFiles) {
        //List<GpFilePath> files = new ArrayList<GpFilePath>();
        SortedSet<GpFilePath> files = new TreeSet<GpFilePath>(dirFirstComparator);
        for (FileInfoWrapper wrapper : wrappedFiles) {
            files.add(wrapper.getFile());
        }
        return new ArrayList<GpFilePath>(files);
    }
    
    private static UploadTreeJSON treeToJson(final List<GpFilePath> tree, Map<String, SortedSet<TaskInfo>> kindToTaskInfo) {
        UploadTreeJSON json = null;
        if (tree != null && !tree.isEmpty()) {
            json = new UploadTreeJSON(tree, "");
        }
        else {
            json = new UploadTreeJSON(null, UploadTreeJSON.EMPTY);
        }
        return json;
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
            log.error("Error writing to the response in UploadFileServlet: " + content);
            e.printStackTrace();
        }
        finally {
            if (writer != null) writer.close();
        }
    }
}
