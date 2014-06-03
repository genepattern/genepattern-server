package org.genepattern.server.genomespace;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.util.FacesUtil;

/**
 * Servlet used by new GenomeSpace integration components
 * @author tabor
 */
public class GenomeSpaceServlet extends HttpServlet {
    private static final long serialVersionUID = 1632404362013601528L;
    public static Logger log = Logger.getLogger(GenomeSpaceServlet.class);

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
            loadSaveLevel(request, response);
        }
        if (SAVE_FILE.equals(action)) {
            saveFile(request, response);
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

    private void saveFile(HttpServletRequest request, HttpServletResponse response) {
        GenomeSpaceBean bean = getGSBean(request, response);
        String directoryURL = request.getParameter("directory");
        String fileURL = request.getParameter("file");

        try {
            if (directoryURL == null || fileURL == null) {
                log.error("No file or directory provided when saving file to GenomeSpace");
                response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Error: No file or directory provided when saving file to GenomeSpace");
            }

            String statusText = bean.sendFileToGenomeSpace(directoryURL, fileURL);

            if (statusText.contains("Error")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, statusText);
            }
            else {
                this.write(response, statusText);
            }
        }
        catch (IOException e) {
            log.error("IOError sending error in GenomeSpaceServlet");
        }
    }

    private void loadSaveLevel(HttpServletRequest request, HttpServletResponse response) {
        GenomeSpaceBean bean = getGSBean(request, response);
        String url = request.getParameter("dir");

        List<GenomeSpaceFile> tree = null;
        if (url == null) {
            tree = bean.getFileTree();
//            tree = new ArrayList<GenomeSpaceFile>(tree.get(0).getChildFiles());
//            for (GenomeSpaceFile file : tree) {
//                if (file.getName().equals(bean.getUsername())) {
//                    tree = new ArrayList<GenomeSpaceFile>();
//                    tree.add(file);
//                    break;
//                }
//            }
        }
        else {
            tree = new ArrayList<GenomeSpaceFile>(bean.getDirectory(url).getChildFiles());
        }

        TreeJSON json = null;
        if (!tree.isEmpty()) {
            json = new TreeJSON(tree, TreeJSON.SAVE_TREE, bean);
        }
        else {
            json = new TreeJSON(new ArrayList<GenomeSpaceFile>(), TreeJSON.SAVE_TREE, bean);
        }
        this.write(response, json);
    }

    private void loadTreeLevel(HttpServletRequest request, HttpServletResponse response) {
        GenomeSpaceBean bean = getGSBean(request, response);
        String url = request.getParameter("dir");

        List<GenomeSpaceFile> tree = null;
        if (url == null) {
            tree = bean.getFileTree();
            tree = new ArrayList<GenomeSpaceFile>(tree.get(0).getChildFiles());
        }
        else {
            GenomeSpaceFile dir = bean.getDirectory(url);
            tree = new ArrayList<GenomeSpaceFile>(dir.getChildFiles());
        }

        TreeJSON json = null;
        if (!tree.isEmpty()) {
            json = new TreeJSON(tree, bean);
        }
        else {
            json = new TreeJSON(null, TreeJSON.EMPTY, bean);
        }
        this.write(response, json);
    }

    @SuppressWarnings("deprecation")
    private GenomeSpaceBean getGSBean(HttpServletRequest request, HttpServletResponse response) {
        // Get the FacesContext inside HttpServlet.
        FacesContext facesContext = FacesUtil.getFacesContext(request, response);
        return (GenomeSpaceBean) facesContext.getApplication().createValueBinding("#{genomeSpaceBean}").getValue(facesContext);
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
