/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.gpge.ui.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * 
 * Utility methods for semantic information
 * 
 * 
 * 
 * @author Joshua Gould
 * 
 */

public class SemanticUtil {

    private SemanticUtil() {

    }

    private static Map _getInputTypeToMenuItemsMap(Map inputTypeToModulesMap) {

        Map inputTypeToMenuItemMap = new HashMap();

        for (Iterator it = inputTypeToModulesMap.keySet().iterator(); it

        .hasNext();) {

            String type = (String) it.next();

            List modules = (List) inputTypeToModulesMap.get(type);

            if (modules == null) {

                continue;

            }

            ModuleMenuItemAction[] m = new ModuleMenuItemAction[modules.size()];

            java.util.Collections.sort(modules,

            AnalysisServiceUtil.CASE_INSENSITIVE_TASK_NAME_COMPARATOR);

            for (int i = 0; i < modules.size(); i++) {

                final AnalysisService svc = (AnalysisService) modules.get(i);

                m[i] = new ModuleMenuItemAction(svc);

            }

            inputTypeToMenuItemMap.put(type, m);

        }

        return inputTypeToMenuItemMap;

    }

    /**
     * 
     * Gets a map which maps the input type as a string to an array of
     * 
     * ModuleMenuItemAction instances
     * 
     */

    public static Map getInputTypeToMenuItemsMap(Collection analysisServices) {

        Map inputTypeToModulesMap = org.genepattern.util.SemanticUtil

        .getKindToModulesMap(analysisServices);

        return _getInputTypeToMenuItemsMap(inputTypeToModulesMap);

    }

    private static void addToInputTypeToModulesMap(Map map, AnalysisService svc) {

        TaskInfo taskInfo = svc.getTaskInfo();

        ParameterInfo[] p = taskInfo.getParameterInfoArray();

        if (p != null) {

            for (int i = 0; i < p.length; i++) {

                if (p[i].isInputFile()) {

                    ParameterInfo info = p[i];

                    String fileFormatsString = (String) info.getAttributes()

                    .get(GPConstants.FILE_FORMAT);

                    if (fileFormatsString == null

                    || fileFormatsString.equals("")) {

                        continue;

                    }

                    StringTokenizer st = new StringTokenizer(fileFormatsString,

                    GPConstants.PARAM_INFO_CHOICE_DELIMITER);

                    while (st.hasMoreTokens()) {

                        String type = st.nextToken();

                        List modules = (List) map.get(type);

                        if (modules == null) {

                            modules = new ArrayList();

                            map.put(type, modules);

                        }

                        if (!modules.contains(svc)) {

                            modules.add(svc);

                        }

                    }

                }

            }

        }
    }
}
