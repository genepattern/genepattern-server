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

package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class RunTaskBean {

    private boolean visualizer;
    private boolean pipeline;
    private String name;
    private String lsid;
    private String[] documentationFilenames;
    private Parameter[] parameters;
    private String version;
    private static Logger log = Logger.getLogger(RunTaskBean.class);

    public RunTaskBean() {
        setTask("ConvertLineEndings"); // FIXME

    }

    public void setTaskInfo(TaskInfo taskInfo) {
        ParameterInfo[] pi = taskInfo.getParameterInfoArray();
        this.parameters = new Parameter[pi != null ? pi.length : 0];
        if (pi != null) {

            for (int i = 0; i < pi.length; i++) {
                parameters[i] = new Parameter(pi[i]);
            }
        }
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();

        String taskType = tia.get("taskType");
        this.visualizer = "visualizer".equalsIgnoreCase(taskType);
        this.pipeline = "pipeline".equalsIgnoreCase(taskType);
        this.name = taskInfo.getName();
        this.lsid = taskInfo.getLsid();
        try {
            this.version = new LSID(lsid).getVersion();
        }
        catch (MalformedURLException e) {
            log.error(e);
        }
        File[] docFiles = null;
        try {
            LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
            docFiles = taskIntegratorClient.getDocFiles(taskInfo);
        }
        catch (WebServiceException e) {
            e.printStackTrace();
        }
        this.documentationFilenames = new String[docFiles != null ? docFiles.length : 0];
        if (docFiles != null) {
            for (int i = 0; i < docFiles.length; i++) {
                documentationFilenames[i] = docFiles[i].getName();
            }
        }

    }

    public String[] getDocumentationFilenames() {
        return documentationFilenames;
    }

    public String getLsid() {
        return lsid;
    }

    public String getName() {
        return name;
    }

    public boolean isPipeline() {
        return pipeline;
    }

    public boolean isVisualizer() {
        return visualizer;
    }

    public static class Parameter {

        private SelectItem[] choices;
        private boolean optional;
        private String displayDesc;
        private String displayName;
        private String defaultValue;
        private String inputType;
        private String name;

        private Parameter(ParameterInfo pi) {
            HashMap pia = pi.getAttributes();

            String[] choicesArray = pi.getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
            if (choices != null) {
                for (int i = 0; i < choicesArray.length; i++) {
                    String choice = choicesArray[i];
                    String display, option;
                    int c = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
                    if (c == -1) {
                        display = choice;
                        option = choice;
                    }
                    else {
                        option = choice.substring(0, c);
                        display = choice.substring(c + 1);
                    }
                    display = display.trim();
                    option = option.trim();
                    choices[i] = new SelectItem(option, display);
                }
            }
            if (pi.isPassword()) {
                inputType = "password";
            }
            else if (pi.isInputFile()) {
                inputType = "file";
            }
            else if (choices != null && choices.length > 0) {
                inputType = "select";
            }
            else {
                inputType = "text";
            }

            this.optional = ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
                    .length() > 0;
            this.defaultValue = (String) pia.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

            if (defaultValue != null) {
                defaultValue = defaultValue.trim();
            }

            this.displayDesc = (String) pia.get("altDescription");
            if (displayDesc == null) {
                displayDesc = pi.getDescription();
            }
            this.displayName = (String) pia.get("altName");
            if (displayName == null) {
                displayName = pi.getName();
            }
            this.name = pi.getName();
        }

        public SelectItem[] getChoices() {
            return choices;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDisplayDescription() {
            return displayDesc;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isOptional() {
            return optional;
        }

        public String getInputType() {
            return inputType;
        }

    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public void setParameters(Parameter[] parameters) {
        this.parameters = parameters;
    }

    public String getVersion() {
        return version;
    }

    public void setTask(String lsid) {
        try {
            TaskInfo taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(lsid);
            setTaskInfo(taskInfo);
        }
        catch (WebServiceException e) {
            e.printStackTrace();
        }
    }

}
