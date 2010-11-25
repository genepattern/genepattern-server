/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2009) by the
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.genepattern.io.ParseException;
import org.genepattern.io.odf.OdfHandler;
import org.genepattern.io.odf.OdfParser;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class SemanticUtil {
    private static Logger log = Logger.getLogger(SemanticUtil.class);

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
            MyOdfHandler handler = new MyOdfHandler();
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

    private static class MyOdfHandler implements OdfHandler {
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

    private static  HashMap<String, Collection<TaskInfo>> mapOfAllTasks = new HashMap<String, Collection<TaskInfo>>();
  
    public static Map<String, Collection<TaskInfo>> getKindToModulesMap(TaskInfo[] taskArray) {
        Map<String, Collection<TaskInfo>> map = new HashMap<String, Collection<TaskInfo>>();
        Set<TaskInfo> userTasks = new HashSet<TaskInfo>();
        for (TaskInfo ti: taskArray){
            userTasks.add(ti);
        }
        /*
         * first make sure we have this task already in the complete list
         */
        for (TaskInfo task : taskArray) {
            if (task == null || task.getLsid() == null) {
                // unexpected input
            }
            else {
                addToInputTypeToModulesMap(mapOfAllTasks, task);
            }
        }
        
        /*
         * Now filter the list to return just those that this user can see (that were provided in the input collection)
         */
        for (String type: mapOfAllTasks.keySet()){
            Collection<TaskInfo>  allModulesForInputType = mapOfAllTasks.get(type);
            Collection<TaskInfo> modules = new HashSet<TaskInfo>();
            for (TaskInfo task: allModulesForInputType){
                /* if this taskinfo is in the input array we use it */
                try {
                    if (task != null && userTasks != null) {
                        if (userTasks.contains(task)) {
                            modules.add(task);
                        }
                    }
                } 
                catch (Exception e) {
                    log.error("error on task: " + (task != null ? task.getName() : "<null>"), e);
                }
            }
            if (modules.size()> 0){
                map.put(type, modules);
            }
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

    /**
     * Get the set of all file formats that the given task accepts.
     * 
     * @param taskInfo
     * @return
     */
    public static Set<String> getInputFileFormats(TaskInfo taskInfo) {
        Set<String> taskInfoInputFileFormats = new HashSet<String>();
        if (taskInfo == null) {
            log.error("illegal null arg");
            return Collections.emptySet();
        }
        for(ParameterInfo param : taskInfo.getParameterInfoArray()) {
            if (param.isInputFile()) {
                List<String> paramInputFileFormats = getFileFormats(param);
                taskInfoInputFileFormats.addAll( paramInputFileFormats );
            }
        }
        return taskInfoInputFileFormats;
    }
    
    /**
     * @deprecated - this can be refactored so that the taskInfo cache stores the set of all file types accepted by each module, 
     *               and additionally refactored so that output file types for each job are stored in the db.
     * @param map
     * @param taskInfo
     */
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
                        if ((modules == null) && (type != null)) {
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
