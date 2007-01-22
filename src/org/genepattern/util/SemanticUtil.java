/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2006) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.genepattern.io.IOdfHandler;
import org.genepattern.io.OdfParser;
import org.genepattern.io.ParseException;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class SemanticUtil {
    private SemanticUtil() {
    }

    public static String getKind(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        String extension = null;
        if (dotIndex > 0) {
            extension = name.substring(dotIndex + 1, name.length());
        } else {
            return null;
        }
        if (extension.equalsIgnoreCase("odf")) {
            OdfParser parser = new OdfParser();
            OdfHandler handler = new OdfHandler();
            FileInputStream fis = null;
            parser.setHandler(handler);
            try {
                fis = new FileInputStream(file);
                parser.parse(fis);
            } catch (Exception e) {
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException x) {
                }
            }
            return handler.model;
        } else {
            return extension.toLowerCase();
        }
    }

    private static class OdfHandler implements IOdfHandler {
        public String model;

        public void endHeader() throws ParseException {
            throw new ParseException("");
        }

        public void header(String key, String[] values) throws ParseException {
        }

        public void header(String key, String value) throws ParseException {
            if (key.equals("Model")) {
                model = value;
                throw new ParseException("");
            }
        }

        public void data(int row, int column, String s) throws ParseException {
            throw new ParseException("");
        }
    }

    /**
     * 
     * Returns <code>true</code> if the given kind returned from is an
     * acceptable input file
     * 
     * format for the given input parameter
     * 
     */
    public static boolean isCorrectKind(String[] inputTypes, String kind) {
        if (inputTypes == null || inputTypes.length == 0) {
            return false;
        }
        if (kind == null || kind.equals("")) {
            return true;
        }
        return Arrays.asList(inputTypes).contains(kind);
    }

    /**
     * 
     * Gets a map which maps the input type as a string to a list of analysis
     * services that take that input type as an input parameter
     * 
     */
    public static Map<String, List<AnalysisService>> getKindToModulesMap(Collection<AnalysisService> analysisServices) {
        Iterator<AnalysisService> temp = analysisServices.iterator();
        String server = null;
        if (temp.hasNext()) {
            server = temp.next().getServer();
        }
        Map<String, Collection<TaskInfo>> map = new HashMap<String, Collection<TaskInfo>>();
        for (Iterator<AnalysisService> it = analysisServices.iterator(); it.hasNext();) {
            AnalysisService svc = it.next();
            addToInputTypeToModulesMap(map, svc.getTaskInfo());
        }
        Map<String, List<AnalysisService>> kindToServices = new HashMap<String, List<AnalysisService>>();
        for (Iterator<String> it = map.keySet().iterator(); it.hasNext();) {
            String kind = it.next();
            Collection<TaskInfo> tasks = map.get(kind);
            if (tasks != null) {
                List<AnalysisService> modules = new ArrayList<AnalysisService>();
                for (Iterator<TaskInfo> taskIt = tasks.iterator(); taskIt.hasNext();) {
                    modules.add(new AnalysisService(server, taskIt.next()));
                }
                kindToServices.put(kind, modules);
            }
        }
        return kindToServices;
    }

    public static Map<String, Collection<TaskInfo>> getKindToModulesMap(TaskInfo[] tasks) {
        Map<String, Collection<TaskInfo>> map = new HashMap<String, Collection<TaskInfo>>();
        for (TaskInfo task : tasks) {
            addToInputTypeToModulesMap(map, task);
        }
        return map;
    }

    public static List<String> getFileFormats(ParameterInfo p) {
        String fileFormatsString = (String) p.getAttributes().get(GPConstants.FILE_FORMAT);
        if (fileFormatsString == null || fileFormatsString.equals("")) {
            return Collections.emptyList();
        }
        List<String> fileFormats = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(fileFormatsString, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
        while (st.hasMoreTokens()) {
            String type = st.nextToken();
            fileFormats.add(type);

        }
        return fileFormats;
    }

    private static void addToInputTypeToModulesMap(Map<String, Collection<TaskInfo>> map, TaskInfo taskInfo) {
        ParameterInfo[] p = taskInfo.getParameterInfoArray();
        if (p != null) {
            for (int i = 0; i < p.length; i++) {
                if (p[i].isInputFile()) {
                    ParameterInfo info = p[i];
                    String fileFormatsString = (String) info.getAttributes().get(GPConstants.FILE_FORMAT);
                    if (fileFormatsString == null || fileFormatsString.equals("")) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(fileFormatsString, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
                    while (st.hasMoreTokens()) {
                        String type = st.nextToken();
                        Collection<TaskInfo> modules = map.get(type);
                        if (modules == null) {
                            modules = new HashSet<TaskInfo>();
                            map.put(type, modules);
                        }
                        modules.add(taskInfo);
                    }
                }
            }
        }
    }
}
