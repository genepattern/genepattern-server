package org.genepattern.server.genepattern;

import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
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
        // <gpServletContext>
        if (gpConfig != null && !Strings.isNullOrEmpty(gpConfig.getGpPath())) {  
            launchUrl.append(gpConfig.getGpPath());
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("gpConfig == null || gpPath not set, using default value, '/gp'");
            }
            launchUrl.append("/gp");
        } 
        launchUrl.append("/tasklib/");
        launchUrl.append(taskInfo.getLsid());
        // <mainFile>        
        String mainFile = (String)taskInfo.getAttributes().get("commandLine");
        mainFile = mainFile.substring(0, mainFile.indexOf("?")).trim();
        launchUrl.append("/");
        launchUrl.append(mainFile);

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

