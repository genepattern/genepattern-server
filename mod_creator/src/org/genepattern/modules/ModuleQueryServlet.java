package org.genepattern.modules;

import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.util.GPConstants;
import org.apache.log4j.Logger;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.io.*;

/**
 * based on PipelineQueryServer class in the org.genepattern.pipelines class
 */
public class ModuleQueryServlet extends HttpServlet 
{
    public static Logger log = Logger.getLogger(ModuleQueryServlet.class);

    public static final String MODULE_CATEGORIES = "/categories";
    public static final String OUTPUT_FILE_FORMATS = "/fileformats";
    public static final String UPLOAD = "/upload";
    public static final String SAVE = "/save";
    

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    {
		String action = request.getPathInfo();

		// Route to the appropriate action, returning an error if unknown
		if (MODULE_CATEGORIES.equals(action)) 
        {
            getModuleCategories(response);
        }
        if (OUTPUT_FILE_FORMATS.equals(action)) 
        {
            getOutputFileFormats(response);
        }
        else if (SAVE.equals(action)) {
		    saveModule(request, response);
		}
        else if (UPLOAD.equals(action))
        {
		    uploadFile(request, response);
		}
        else
        {
		    sendError(response, "Routing error for " + action);
		}
    }

    @Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
    {
		doGet(request, response);
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
    {
	    doGet(request, response);
	}

    private void write(HttpServletResponse response, Object content)
    {
        this.write(response, content.toString());
    }

    private void write(HttpServletResponse response, String content)
    {
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

    public void sendError(HttpServletResponse response, String message)
    {
	    ResponseJSON error = new ResponseJSON();
	    error.addError("ERROR: " + message);
	    this.write(response, error);
	}

    public SortedSet<String> getAllCategories() {
        SortedSet<String> categories = new TreeSet<String>(new Comparator<String>() {
            // sort categories alphabetically, ignoring case
            public int compare(String arg0, String arg1) {
                String arg0tl = arg0.toLowerCase();
                String arg1tl = arg1.toLowerCase();
                int rval = arg0tl.compareTo(arg1tl);
                if (rval == 0) {
                    rval = arg0.compareTo(arg1);
                }
                return rval;
            }
        });
        
        for (TaskInfo ti : TaskInfoCache.instance().getAllTasks()) {
            String taskType = ti.getTaskInfoAttributes().get("taskType");
            if (taskType == null || taskType.trim().length() == 0) {
                //ignore null and blank
            }
            else {
                categories.add(taskType);
            }
        }
        return Collections.unmodifiableSortedSet(categories);
    }
    
    public void getModuleCategories(HttpServletResponse response)
    {
        SortedSet<String> categories = null;
        try
        {
            categories = getAllCategories();
        }
        catch (Throwable t)
        {
            log.error("Error listing categories from TaskInfoCache: "+t.getLocalizedMessage());
        }

        ResponseJSON message = new ResponseJSON();
        if(categories != null)
        {
            message.addChild("categories", categories.toString());
        }
        else
        {
            message.addChild("categories", "");
        }
        this.write(response, message);
    }

    public SortedSet<String> getFileFormats()
    {
        SortedSet<String> fileFormats = new TreeSet<String>(new Comparator<String>() {
            // sort categories alphabetically, ignoring case
            public int compare(String arg0, String arg1) {
                String arg0tl = arg0.toLowerCase();
                String arg1tl = arg1.toLowerCase();
                int rval = arg0tl.compareTo(arg1tl);
                if (rval == 0) {
                    rval = arg0.compareTo(arg1);
                }
                return rval;
            }
        });

        for (TaskInfo ti : TaskInfoCache.instance().getAllTasks()) {
            String fileFormat = ti.getTaskInfoAttributes().get("fileFormat");
            if (fileFormat == null || fileFormat.trim().length() == 0) {
                //ignore null and blank
            }
            else
            {
                if(fileFormat.indexOf(";") != -1)
                {
                    String[] result = fileFormat.split(";");
                    for(String f : result)
                    {
                        fileFormats.add(f);
                    }
                }
                else
                {
                    fileFormats.add(fileFormat);
                }

            }

            ParameterInfo[] pInfoArray = ti.getParameterInfoArray();
            if(pInfoArray == null)
            {
                continue;
            }
            for(ParameterInfo pi : pInfoArray)
            {
                String pFileFormat = (String)pi.getAttributes().get("fileFormat");

                if (!(pFileFormat == null || pFileFormat.trim().length() == 0))
                {
                    if(pFileFormat.indexOf(";") != -1)
                    {
                        String[] result = pFileFormat.split(";");
                        for(String f : result)
                        {
                            fileFormats.add(f);
                        }
                    }
                    else
                        fileFormats.add(pFileFormat);
                }
            }
        }
        return Collections.unmodifiableSortedSet(fileFormats);
    }

    public void getOutputFileFormats(HttpServletResponse response)
    {
        SortedSet<String> fileFormats = null;
        try
        {
            fileFormats = getFileFormats();
        }
        catch (Throwable t)
        {
            log.error("Error listing categories from TaskInfoCache: "+t.getLocalizedMessage());
        }

        ResponseJSON message = new ResponseJSON();

        if(fileFormats != null)
        {
            message.addChild("fileformats", fileFormats.toString());
        }
        else
        {
            message.addChild("fileformats", "");
        }
        this.write(response, message);
    }

    public void uploadFile(HttpServletRequest request, HttpServletResponse response)
    {
        String username = (String) request.getSession().getAttribute("userid");
        if (username == null) {
           sendError(response, "No GenePattern session found.  Please log in.");
           return;
        }

	    RequestContext reqContext = new ServletRequestContext(request);
        if (FileUploadBase.isMultipartContent(reqContext)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> postParameters = upload.parseRequest(reqContext);

                for (FileItem i : postParameters) {
                    // Only read the submitted files
                    if (!i.isFormField()) {
                        // Store in a temp directory until the pipeline is saved
                        String str = System.getProperty("java.io.tmpdir");
                        File tempDir = new File(str);
                        File uploadedFile = new File(tempDir, i.getName());
                        transferUpload(i, uploadedFile);

                        // Return a success response
                        ResponseJSON message = new ResponseJSON();
                        message.addChild("location", uploadedFile.getCanonicalPath());
                        this.write(response, message);
                    }
                    else
                    {
                        ResponseJSON message = new ResponseJSON();
                        message.addChild("formfield", "false");
                        this.write(response, message);
                    }
                }
            }
            catch (Exception e) {
                log.error("error", e);
                sendError(response, "Exception retrieving the uploaded file");
            }
        }
        else {
            sendError(response, "Unable to find uploaded file");
        }
	}

    public void saveModule(HttpServletRequest request, HttpServletResponse response)
    {
	    String username = (String) request.getSession().getAttribute("userid");
	    if (username == null) {
	        sendError(response, "No GenePattern session found.  Please log in.");
	        return;
	    }

        String bundle = request.getParameter("bundle");
	    if (bundle == null) {
	        log.error("Unable to retrieved the saved module");
	        sendError(response, "Unable to save the module");
	        return;
	    }

        try
        {
            JSONObject moduleJSON = ModuleJSON.parseBundle(bundle);
	        ModuleJSON moduleObject = ModuleJSON.extract(moduleJSON);
            String name = moduleObject.getName();
            String description = moduleObject.getDescription();

            TaskInfoAttributes tia = new TaskInfoAttributes();
            Iterator<String> infoKeys = moduleObject.keys();
            while(infoKeys.hasNext())
            {
                String key = infoKeys.next();

                //omit module name and description from taskinfoattributes
                if(!key.equals(ModuleJSON.NAME) && !key.equals(ModuleJSON.DESCRIPTION))
                {
                    tia.put(key, moduleObject.get(key));
                }
            }

            //parse out privacy info
            int privacy = GPConstants.ACCESS_PRIVATE;
            if(moduleObject.get(ModuleJSON.PRIVACY) != null
                    && !((String)moduleObject.get(ModuleJSON.PRIVACY)).equalsIgnoreCase("private"))
            {
                privacy = GPConstants.ACCESS_PUBLIC;
            }

            ParameterInfo[] pInfo = new ParameterInfo[0];
            String newLsid = GenePatternAnalysisTask.installNewTask(name, description, pInfo, tia, username, privacy,
            new Status() {
                    public void beginProgress(String string) {
                    }
                    public void continueProgress(int percent) {
                    }
                    public void endProgress() {
                    }
                    public void statusMessage(String message) {
                    }
            });

            ResponseJSON message = new ResponseJSON();
            message.addMessage("Module Saved");
            message.addChild("lsid", newLsid);
            this.write(response, message);
        }
        catch(Exception e)
        {
            log.error(e);
            sendError(response, "Exception saving the module.");                   
        }
    }

    private void transferUpload(FileItem from, File to) throws IOException {
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
            if(is != null)
            {
                is.close();
            }


            if(os != null)
            {
                os.close();
            }

        }
    }

}
