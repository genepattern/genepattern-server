package org.genepattern.server.genepattern;

import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.webservice.TaskInfo;

import com.google.common.base.Strings;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Created by nazaire on 7/16/15.
 */
public class JavascriptHandler {
    private static Logger log = Logger.getLogger(JavascriptHandler.class);

    public static final String LAUNCH_URL_FILE = ".launchUrl.txt";

    protected static String buildQueryString(final Multimap<String,String> queryMap) throws UnsupportedEncodingException
    {
        QueryStringBuilder b=new QueryStringBuilder();

        for (Map.Entry<String, String> entry : queryMap.entries())
        {
            String key = entry.getKey();
            String value = entry.getValue();

            b.param(key, value);
        }

        return b.build();
    }

    /**
     * Generate the relative launchUrl for a JsViewer; call this after substituting input values.
     * Template:
     * <pre>
/<gpServletContext>/tasklib/<taskLsid>/<mainFile>
    ?<p01_0>=<value>
    &<p01_1>=<value>
     ...
    &<p02_0>=<value>
     ...
    &<pN_M>=<value>
     * </pre>
     *
     * The relative path, includes the servlet context path, e.g.
     *     /gp/tasklib/...
     * Multi-valued params are passed, in order, one at a time, e.g.
     *     input.param=first_value&input.param=second_value
     * 
     * @param gpConfig, to access GpConfig#gpPath, defaults to "/gp".
     * @param taskInfo, the JsViewer taskInfo
     * @param substitutedValuesMap, a map of param name to substituted value, as computed in GPAT#onJob.
     * @return a relative URL path for launching the JsViewer
     * 
     * @throws Exception
     */
    public static String generateLaunchUrl(final GpConfig gpConfig, final TaskInfo taskInfo, final Map<String, List<String>> substitutedValuesMap) throws Exception
    {
        final StringBuffer launchUrl = new StringBuffer();
        // For servlets in the default (root) context, HttpServletRequest.getContextPath returns ""
        if (gpConfig != null && gpConfig.getGpPath() != null) {  
            launchUrl.append(Strings.nullToEmpty(gpConfig.getGpPath()));
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("gpConfig == null || gpConfig.gpPath==null, using default value, '/gp'");
            }
            launchUrl.append("/gp");
        } 
        launchUrl.append("/tasklib/");
        launchUrl.append(UrlUtil.encodeURIcomponent(taskInfo.getLsid()));
        // <mainFile>        
        final String mainFile = getMainFile(taskInfo);
        launchUrl.append("/");
        launchUrl.append(UrlUtil.encodeURIcomponent(mainFile));

        //?<param>=<value>&<param>=<value>...
        final Multimap<String,String> queryMap = LinkedHashMultimap.create();
        if (substitutedValuesMap != null) {
            for(String paramName: substitutedValuesMap.keySet())
            {
                List<String> paramValues = substitutedValuesMap.get(paramName);
                queryMap.putAll(paramName, paramValues);
            }
        }

        final String queryString=buildQueryString(queryMap);
        if (!Strings.isNullOrEmpty(queryString)) {
            launchUrl.append("?");
            launchUrl.append(queryString);
        }
        if (log.isDebugEnabled()) {
            log.debug("launchUrl="+launchUrl.toString());
        }
        return launchUrl.toString();
    }

    /**
     * Get the mainFile for a JsViewer, passing in hard-coded mainFile_default='index.html'.
     */
    protected static String getMainFile(final TaskInfo taskInfo) {
        final String mainFile_default="index.html";
        return getMainFile(taskInfo, mainFile_default);
    }
    
    /**
     * Get the mainFile for a JsViewer, or the default if not set on the command line.
     * Note: possibility to declare the mainFile= in the manifest file.
     * 
     * @param taskInfo
     * @param mainFile_default
     * @return the value from the commandLine=<mainFile> ? ..., 
     *    or return hard-coded mainFile_default="index.html" if commandLine is not formatted properly.
     */
    protected static String getMainFile(final TaskInfo taskInfo, final String mainFile_default) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return mainFile_default;
        }
        final String commandLine=(String)taskInfo.giveTaskInfoAttributes().get("commandLine");
        if (Strings.isNullOrEmpty(commandLine)) {
            if (log.isDebugEnabled()) {
                log.debug("commandLine not set, mainFile_default="+mainFile_default);
            }
            return mainFile_default;
        }
        int idx=commandLine.indexOf("?");
        // missing '?' delimiter, match first '<' or end of string
        if (idx<0) {
            idx=commandLine.indexOf("<");
        }
        if (idx<0) {
            idx=commandLine.length();
        }
        String mainFile = commandLine.substring(0, idx).trim();
        if (Strings.isNullOrEmpty(mainFile)) {
            if (log.isDebugEnabled()) {
                log.debug("mainFile not set in commandLine="+commandLine+", mainFile_default="+mainFile_default);
            }
            return mainFile_default;
        }
        return mainFile;
    }

    public static String saveLaunchUrl(GpConfig gpConfig, TaskInfo taskInfo, File outputDir,  Map<String, List<String>> substitutedValuesMap) throws Exception
    {
        final String launchUrl=generateLaunchUrl(gpConfig, taskInfo, substitutedValuesMap);

        PrintWriter writer = null;
        try {
            File launchUrlFile = new File(outputDir, LAUNCH_URL_FILE);
            writer = new PrintWriter(launchUrlFile);
            writer.print(launchUrl.toString());
        }
        finally {
            if(writer != null)
            {
                writer.close();
            }
        }

        return launchUrl.toString();
    }

}

