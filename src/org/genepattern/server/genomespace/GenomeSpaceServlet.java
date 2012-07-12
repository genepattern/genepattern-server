package org.genepattern.server.genomespace;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getPathInfo();
        
        // Route to the appropriate action, returning an error if unknown
        if (TREE.equals(action)) {
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
    
    private void loadTreeLevel(HttpServletRequest request, HttpServletResponse response) {
        GenomeSpaceBean bean = getGSBean(request, response);
        String url = request.getParameter("dir");
        
        List<GenomeSpaceFile> tree = null;
        if (url == null) {
            tree = bean.getFileTree();
            tree = new ArrayList<GenomeSpaceFile>(tree.get(0).getChildFiles());
        }
        else {
            tree = new ArrayList<GenomeSpaceFile>(bean.getDirectory(url).getChildFiles());            
        }
        
        TreeJSON json = null;
        if (!tree.isEmpty()) {
            json = new TreeJSON(tree);
        }
        else {
            json = new TreeJSON(TreeJSON.EMPTY);
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
