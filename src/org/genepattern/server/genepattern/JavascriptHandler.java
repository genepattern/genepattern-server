package org.genepattern.server.genepattern;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.executor.drm.HashMapLookup;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.genepattern.webservice.TaskInfo;

/**
 * Created by nazaire on 7/16/15.
 */
public class JavascriptHandler
{
    public static String LAUNCH_URL_FILE = ".launchUrl.txt";

    private static final Logger log = Logger.getLogger(JavascriptHandler.class);

    public static String generatelaunchUrl(GpConfig gpConfig, TaskInfo taskInfo, File outputDir,  Map<String, List> substitutedValuesMap)throws Exception
    {
        StringBuffer launchUrl = new StringBuffer();

        String mainFile = (String)taskInfo.getAttributes().get("commandLine");
        mainFile = mainFile.substring(0, mainFile.indexOf("?")).trim();
        final String relativeUriStr="tasklib/"+taskInfo.getLsid()+"/"+mainFile;
        launchUrl.append(gpConfig.getGenePatternURL() + relativeUriStr + "?");

        for(String paramName: substitutedValuesMap.keySet())
        {
            List<String> paramValues = substitutedValuesMap.get(paramName);
            for(String paramValue: paramValues)
            {
                launchUrl.append(paramName + "=" + paramValue + "&");
            }
        }

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
