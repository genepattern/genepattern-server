/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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

    public static String getExtension(File file) {
        String name = null;
        if (file != null) {
            name = file.getName();
        }
        if (name == null) {
            return null;
        }
        if (file.isDirectory()) {
            return null;
        }
        
        String extension = null;
        int idx = name.lastIndexOf(".");
        if (idx > 0 && idx < (name.length() - 1)) {
            extension = name.substring(idx + 1);
        }
        return extension;
    }
    
    public static String getGzKind(File file) {
        String name = file.getName();
        // Get the index of the next to last period, assumes the .gz
        int index = name.substring(0, name.length() - 3).lastIndexOf(".");
        if (index > 0) {
            return name.substring(index + 1);
        }
        else {
            return "gz";
        }
    }

    public static String getKind(File file) {
        String extension = getExtension(file);
        if (extension == null) {
            return null;
        }
        if (extension.equalsIgnoreCase("odf")) {
            return getOdfKind(file);
        }
        if (extension.equalsIgnoreCase("gz")) {
            return getGzKind(file);
        }
        return extension.toLowerCase();
    }

    public static String getKind(final File file, final String extension) {
        if (extension == null) {
            return null;
        }
        if (extension.equalsIgnoreCase("odf")) {
            return getOdfKind(file);
        }
        if (extension.equalsIgnoreCase("gz")) {
            return getGzKind(file);
        }
        return extension.toLowerCase();
    }

    private static String getOdfKind(File file) {
        OdfParser parser = new OdfParser();
        MyOdfHandler handler = new MyOdfHandler();
        FileInputStream fis = null;
        parser.setHandler(handler);
        try {
            fis = new FileInputStream(file);
            parser.parse(fis);
        }
        catch (Exception e) {
        }
        finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch (IOException x) {
            }
        }
        return handler.model;
    }
    
//    public static String getKindOld(File file) {
//        String name = file.getName();
//        int dotIndex = name.lastIndexOf(".");
//        String extension = null;
//        if (dotIndex > 0) {
//            extension = name.substring(dotIndex + 1, name.length());
//        } else {
//            return null;
//        }
//        if (extension.equalsIgnoreCase("odf")) {
//            OdfParser parser = new OdfParser();
//            MyOdfHandler handler = new MyOdfHandler();
//            FileInputStream fis = null;
//            parser.setHandler(handler);
//            try {
//                fis = new FileInputStream(file);
//                parser.parse(fis);
//            } catch (Exception e) {
//            } finally {
//                try {
//                    if (fis != null) {
//                        fis.close();
//                    }
//                } catch (IOException x) {
//                }
//            }
//            return handler.model;
//        } else {
//            return extension.toLowerCase();
//        }
//    }

    private static class MyOdfHandler implements OdfHandler {
        public String model = "odf";

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
     * Helper method for generating the 'send to' pop up menu for each output file.
     * Each output file has a type. Each module accepts a set of zero or more file types.
     * This method creates a new unmodifiable Map<String, Set<TaskInfo>> of file type to the set 
     * of module zero or more modules which accepts that file type. 
     * If no modules accept the file type, there will not be an entry in the map.
     * 
     * @param taskArray, the list of all modules from which to generate the collection, usually the list of all modules that 
     *           the current user can run.
     * @return map of fileType to set of modules which accept the given file type as input.
     */
    public static Map<String, Set<TaskInfo>> getKindToModulesMap(List<TaskInfo> taskArray) {
        Map<String, Set<TaskInfo>> map = new HashMap<String, Set<TaskInfo>>();
        for (TaskInfo taskInfo : taskArray) {
            for(String inputFileType : taskInfo._getInputFileTypes()) {
                Set<TaskInfo> sendTo = map.get(inputFileType);
                if (sendTo == null) {
                    sendTo = new HashSet<TaskInfo>();
                    map.put(inputFileType, sendTo);
                }
                sendTo.add(taskInfo);
            }
        }
        return Collections.unmodifiableMap(map);
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

}
