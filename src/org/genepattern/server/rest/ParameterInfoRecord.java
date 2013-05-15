package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for working with ParameterInfo records,
 * because the runtime instance of a parameter info can differ from the 
 * initial version for the TaskInfo, use this to hold onto both the initial aka formal state
 * as well as the actual value to use at runtime.
 * 
 * @author pcarr
 *
 */
public class ParameterInfoRecord {
    final static private Logger log = Logger.getLogger(ParameterInfoRecord.class);

    /**
     * Initialize a map of paramName to ParameterInfo 
     * @param taskInfo
     * @return
     */
    final static public Map<String,ParameterInfoRecord> initParamInfoMap(final TaskInfo taskInfo) {
        Map<String,ParameterInfoRecord> paramInfoMap=new HashMap<String,ParameterInfoRecord>();
        for(ParameterInfo pinfo : taskInfo.getParameterInfoArray()) {
            ParameterInfoRecord record = new ParameterInfoRecord(pinfo);
            paramInfoMap.put(pinfo.getName(), record);
        }
        return paramInfoMap;
    }

    public static List<String> getFileFormats(final ParameterInfo pinfo) {
        if (pinfo.isInputFile()) {
            String fileFormatsString = (String) pinfo.getAttributes().get(GPConstants.FILE_FORMAT);
            if (fileFormatsString == null || fileFormatsString.equals("")) {
                return Collections.emptyList();
            }

            List<String> inputFileTypes=new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(fileFormatsString, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
            while (st.hasMoreTokens()) {
                String type = st.nextToken();
                inputFileTypes.add(type);
            }
            return inputFileTypes;
        }
        else if (pinfo._isDirectory()) {
            List<String> inputFileTypes=new ArrayList<String>();
            inputFileTypes.add("directory");
            return inputFileTypes;
        }
        return Collections.emptyList();
    }

    private ParameterInfo formalParam;
    private ParameterInfo actualParam;

    public ParameterInfoRecord(ParameterInfo formalParam) {
        this.formalParam=formalParam;
        this.actualParam=ParameterInfo._deepCopy(formalParam);
    }

    public ParameterInfo getFormal() {
        return formalParam;
    }
    public ParameterInfo getActual() {
        return actualParam;
    }

    public List<String> getFileFormats() {
        if (formalParam != null) {
            return getFileFormats(formalParam);
        }
        log.error("formalParam == null");
        return Collections.emptyList();
    }
}

