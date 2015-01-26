package org.genepattern.server.genomespace;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
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
        String directoryURL = request.getParameter("directory");
        String fileURL = request.getParameter("file");

        try {
            if (directoryURL == null || fileURL == null) {
                log.error("No file or directory provided when saving file to GenomeSpace");
                response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Error: No file or directory provided when saving file to GenomeSpace");
            }

            GenomeSpaceFile directory = GenomeSpaceManager.getDirectory(request.getSession(), directoryURL);
            GpFilePath file = GpFileObjFactory.getRequestedGpFileObj(fileURL);

            HttpSession httpSession = request.getSession();
            Object gsSession = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
            GenomeSpaceClientFactory.instance().saveFileToGenomeSpace(gsSession, file, directory);
            String statusText = "File uploaded to GenomeSpace " + file.getName();

            this.write(response, statusText);
        }
        catch (Exception e) {
            log.error("IOError sending error in GenomeSpaceServlet");
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Error saving file to GenomeSpace");
            } catch (IOException e1) {
                log.error("Error sending error to GenomeSpace");
            }
        }
    }

    private void loadSaveLevel(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String url = request.getParameter("dir");

        List<GenomeSpaceFile> tree = null;
        if (url == null) {
            tree = GenomeSpaceManager.getFileTreeLazy(session);
        }
        else {
            tree = new ArrayList<GenomeSpaceFile>(GenomeSpaceManager.getDirectory(session, url).getChildFiles());
        }

        GenomeSpaceTreeJSON json = null;
        if (!tree.isEmpty()) {
            json = new GenomeSpaceTreeJSON(session, tree, GenomeSpaceTreeJSON.SAVE_TREE);
        }
        else {
            json = new GenomeSpaceTreeJSON(session, new ArrayList<GenomeSpaceFile>(), GenomeSpaceTreeJSON.SAVE_TREE);
        }
        this.write(response, json);
    }

    private void loadTreeLevel(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String url = request.getParameter("dir");

        List<GenomeSpaceFile> tree = null;
        if (url == null) {
            tree = GenomeSpaceManager.getFileTreeLazy(session);
            tree = new ArrayList<GenomeSpaceFile>(tree.get(0).getChildFiles());
        }
        else {
            GenomeSpaceFile dir = GenomeSpaceManager.getDirectory(session, url);
            tree = new ArrayList<GenomeSpaceFile>(dir.getChildFiles());
        }

        GenomeSpaceTreeJSON json = null;
        if (!tree.isEmpty()) {
            json = new GenomeSpaceTreeJSON(session, tree);
        }
        else {
            json = new GenomeSpaceTreeJSON(session, null, GenomeSpaceTreeJSON.EMPTY);
        }
        this.write(response, json);
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
