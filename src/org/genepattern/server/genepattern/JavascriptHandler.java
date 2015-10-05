package org.genepattern.server.genepattern;

import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.GpConfig;
import org.genepattern.webservice.TaskInfo;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Created by nazaire on 7/16/15.
 */
public class JavascriptHandler {
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


    public static String generateLaunchUrl(final GpConfig gpConfig, final TaskInfo taskInfo, final Map<String, List<String>> substitutedValuesMap) throws Exception
    {
        if (gpConfig==null) {
            throw new IllegalArgumentException("gpConfig==null");
        }

        Multimap<String,String> queryMap = LinkedHashMultimap.create();

        String mainFile = (String)taskInfo.getAttributes().get("commandLine");
        mainFile = mainFile.substring(0, mainFile.indexOf("?")).trim();
        final String relativeUriStr="tasklib/"+taskInfo.getLsid()+"/"+mainFile;

        StringBuffer launchUrl = new StringBuffer();
        launchUrl.append(gpConfig.getGenePatternURL() + relativeUriStr + "?");

        if (substitutedValuesMap != null) {
            for(String paramName: substitutedValuesMap.keySet())
            {
                List<String> paramValues = substitutedValuesMap.get(paramName);
                queryMap.putAll(paramName, paramValues);
            }
        }

        launchUrl.append(buildQueryString(queryMap));
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

