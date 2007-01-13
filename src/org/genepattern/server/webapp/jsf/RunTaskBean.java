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
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
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

    private List<String> versions;

    // This is an odd class for this variable, but RunTaskBean is the backing
    // bean
    // for the home page.
    private String splashMessage;

    private boolean showParameterDescriptions;

    /**
     * Initialize the task lsid. This page needs to support redirects from older
     * .jsp pages as well as jsf navigation. JSP pages will pass the lsid in as
     * a request parameter. Look for it there first, if the paramter is null get
     * it from the moduleChooserBean.
     * 
     */
    public RunTaskBean() {
        String taskToRun = UIBeanHelper.getRequest().getParameter("lsid");
        if (taskToRun == null || taskToRun.length() == 0) {
            ModuleChooserBean chooser = (ModuleChooserBean) UIBeanHelper.getManagedBean("#{moduleChooserBean}");
            assert chooser != null;
            taskToRun = chooser.getSelectedModule();
        }
        setTask(taskToRun);
        this.showParameterDescriptions = Boolean.parseBoolean(new UserDAO().getPropertyValue(UIBeanHelper.getUserId(),
                "show.parameter.descriptions", "true"));
    }

    public boolean isShowParameterDescriptions() {
        return showParameterDescriptions;
    }

    /**
     * Update the show parameter descriptions property using AJAX
     * 
     * @return
     */
    public String getUpdateShowParameterDescriptions() {
        setShowParameterDescriptions(Boolean.parseBoolean(UIBeanHelper.getRequest().getParameter("value")));
        return "";
    }

    public void setShowParameterDescriptions(boolean b) {
        showParameterDescriptions = b;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "show.parameter.descriptions", String.valueOf(b));
    }

    public String getFormAction() {
        if (visualizer) {
            return "preRunVisualizer.jsp";

        } else if (pipeline) {
            return "runPromptingPipeline.jsp";
        }
        return "runTaskPipeline.jsp";
    }

    public String[] getDocumentationFilenames() {
        return documentationFilenames;
    }

    public String getLsid() {
        return lsid;
    }

    public String getLsidNoVersion() {
        try {
            return new LSID(lsid).toStringNoVersion();
        } catch (MalformedURLException e) {
            log.error(e);
            return null;
        }
    }

    public String getEncodedLsid() {
        return UIBeanHelper.encode(lsid);
    }

    public String getName() {
        return name;
    }

    public boolean isInputParametersExist() {
        return parameters != null && parameters.length > 0;
    }

    public boolean isPipeline() {
        return pipeline;
    }

    public boolean isVisualizer() {
        return visualizer;
    }

    public static class DefaultValueSelectItem extends SelectItem {
        private boolean defaultOption;

        public DefaultValueSelectItem(String value, String label, boolean defaultOption) {
            super(value, label);
            this.defaultOption = defaultOption;
        }

        public boolean isSelected() {
            return defaultOption;
        }
    }

    public static class Parameter {

        private DefaultValueSelectItem[] choices;

        private boolean optional;

        private String displayDesc;

        private String displayName;

        private String defaultValue;

        private String inputType;

        private String name;

        private Parameter(ParameterInfo pi, String passedDefaultValue) {
            HashMap pia = pi.getAttributes();
            this.optional = ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
                    .length() > 0;

            this.defaultValue = passedDefaultValue != null ? passedDefaultValue : (String) pia
                    .get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

            if (defaultValue == null) {
                defaultValue = "";
            }
            defaultValue = defaultValue.trim();

            String[] choicesArray = pi.hasChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER) ? pi
                    .getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER) : null;
            if (choicesArray != null) {
                choices = new DefaultValueSelectItem[choicesArray.length];
                for (int i = 0; i < choicesArray.length; i++) {
                    String choice = choicesArray[i];
                    String display, option;

                    int equalsCharIndex = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
                    if (equalsCharIndex == -1) {
                        display = choice;
                        option = choice;
                    } else {
                        option = choice.substring(0, equalsCharIndex);
                        display = choice.substring(equalsCharIndex + 1);
                    }
                    display = display.trim();
                    option = option.trim();
                    boolean defaultOption = defaultValue.equals(display) || defaultValue.equals(option);
                    choices[i] = new DefaultValueSelectItem(option, display, defaultOption);
                }
            }
            if (pi.isPassword()) {
                inputType = "password";
            } else if (pi.isInputFile()) {
                inputType = "file";
            } else if (choices != null && choices.length > 0) {
                inputType = "select";
            } else {
                inputType = "text";
            }

            this.displayDesc = (String) pia.get("altDescription");
            if (displayDesc == null) {
                displayDesc = pi.getDescription();
            }
            this.displayName = (String) pia.get("altName");
            if (displayName == null) {
                displayName = pi.getName();
            }
            displayName = displayName.replaceAll("\\.", " ");
            this.name = pi.getName();
        }

        public DefaultValueSelectItem[] getChoices() {
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

    public void setTask(String taskNameOrLsid) {
        TaskInfo taskInfo = null;
        try {
            taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(taskNameOrLsid);
        } catch (WebServiceException e) {
            log.error(e);
        }
        if (taskInfo == null) {
            lsid = null;
            return;
        }
        ParameterInfo[] pi = taskInfo.getParameterInfoArray();

        String matchJob = (String) UIBeanHelper.getRequest().getAttribute("matchJob");
        Map outputFilesToKind = new HashMap();
        if (matchJob != null) {
            if (pi != null) {
                for (ParameterInfo p : pi) {
                    if (p.isInputFile()) {

                    }
                }
            }
            try {
                int reloadJobNumber = Integer.parseInt(matchJob);
                LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper.getUserId());
                JobInfo matchJobInfo = ac.getJob(reloadJobNumber);
                // can only reload own jobs
                File outputDir = new File(GenePatternAnalysisTask.getJobDir("" + matchJobInfo.getJobNumber()));
                if (UIBeanHelper.getUserId().equals(matchJobInfo.getUserId())) {
                    ParameterInfo[] params = matchJobInfo.getParameterInfoArray();
                    if (params != null) {
                        for (ParameterInfo p : params) {
                            if (p.isOutputFile()) {
                                File file = new File(outputDir, p.getName());
                                String kind = SemanticUtil.getKind(file);
                                // SemanticUtil.isCorrectKind(inputTypes, kind);
                                // outputFilesToKind.put(key, value);
                            }

                        }
                    }
                }
            } catch (NumberFormatException nfe) {
                log.error(nfe);
            } catch (WebServiceException e) {
                log.error(e);
            }
        }

        Map<String, String> reloadValues = new HashMap<String, String>();
        String reloadJobNumberString = UIBeanHelper.getRequest().getParameter("reloadJob");
        if (reloadJobNumberString == null) {
            reloadJobNumberString = (String) UIBeanHelper.getRequest().getAttribute("reloadJob");
        }
        if (reloadJobNumberString != null) {
            try {
                int reloadJobNumber = Integer.parseInt(reloadJobNumberString);
                LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper.getUserId());
                JobInfo reloadJob = ac.getJob(reloadJobNumber);
                // can only reload own jobs
                if (UIBeanHelper.getUserId().equals(reloadJob.getUserId())) {
                    ParameterInfo[] reloadParams = reloadJob.getParameterInfoArray();
                    if (reloadParams != null) {
                        for (int i = 0; i < reloadParams.length; i++) {
                            String value = reloadParams[i].getValue();
                            reloadValues.put(reloadParams[i].getName(), value);
                        }
                    }
                }
            } catch (NumberFormatException nfe) {
                log.error(nfe);
            } catch (WebServiceException e) {
                log.error(e);
            }
        }

        this.parameters = new Parameter[pi != null ? pi.length : 0];
        if (pi != null) {
            for (int i = 0; i < pi.length; i++) {
                String defaultValue = reloadValues.get(pi[i].getName());
                if (defaultValue == null) {
                    defaultValue = UIBeanHelper.getRequest().getParameter(pi[i].getName());
                }
                // if defaultValue is null default value will be set in
                // Parameter constructor
                parameters[i] = new Parameter(pi[i], defaultValue);
            }
        }

        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();

        String taskType = tia.get("taskType");
        this.visualizer = "visualizer".equalsIgnoreCase(taskType);
        this.pipeline = "pipeline".equalsIgnoreCase(taskType);
        this.name = taskInfo.getName();
        this.lsid = taskInfo.getLsid();
        try {
            LSID l = new LSID(lsid);
            this.version = l.getVersion();
            versions = new LocalAdminClient(UIBeanHelper.getUserId()).getVersions(l);
            versions.remove(version);

        } catch (MalformedURLException e) {
            log.error("LSID:" + lsid, e);
            versions = null;
            this.version = null;
        }
        File[] docFiles = null;
        try {
            LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
            docFiles = taskIntegratorClient.getDocFiles(taskInfo);
        } catch (WebServiceException e) {
            log.error(e);
        }
        this.documentationFilenames = new String[docFiles != null ? docFiles.length : 0];
        if (docFiles != null) {
            for (int i = 0; i < docFiles.length; i++) {
                documentationFilenames[i] = docFiles[i].getName();
            }
        }

    }

    public void changeVersion(ActionEvent event) {
        System.out.println("changeVersion");
        ModuleChooserBean chooser = (ModuleChooserBean) UIBeanHelper.getManagedBean("#{moduleChooserBean}");
        assert chooser != null;
        String version = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("version"));

        try {
            chooser.setSelectedModule(new LSID(lsid).toStringNoVersion() + ":" + version);
            setTask(chooser.getSelectedModule());
        } catch (MalformedURLException e) {
            log.error(e);
        }
    }

    public String getSplashMessage() {
        return splashMessage;
    }

    public void setSplashMessage(String splashMessage) {
        this.splashMessage = splashMessage;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

}
