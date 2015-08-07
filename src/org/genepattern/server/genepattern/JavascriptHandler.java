package org.genepattern.server.genepattern;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.webservice.TaskInfo;

/**
 * Created by nazaire on 7/16/15.
 */
public class JavascriptHandler {
    private static final Logger log = Logger.getLogger(JavascriptHandler.class);
    public static final String LAUNCH_URL_FILE = ".launchUrl.txt";

    public static String generateLaunchUrl(final GpConfig gpConfig, final TaskInfo taskInfo, final Map<String, List<String>> substitutedValuesMap) throws Exception
    {
        if (gpConfig==null) {
            throw new IllegalArgumentException("gpConfig==null");
        }
        StringBuffer launchUrl = new StringBuffer();

        String mainFile = (String)taskInfo.getAttributes().get("commandLine");
        mainFile = mainFile.substring(0, mainFile.indexOf("?")).trim();
        final String relativeUriStr="tasklib/"+taskInfo.getLsid()+"/"+mainFile;
        launchUrl.append(gpConfig.getGenePatternURL() + relativeUriStr + "?");

        if (substitutedValuesMap != null) {
            for(String paramName: substitutedValuesMap.keySet())
            {
                List<String> paramValues = substitutedValuesMap.get(paramName);
                for(String paramValue: paramValues)
                {
                    launchUrl.append(paramName + "=" + paramValue + "&");
                }
            }
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
