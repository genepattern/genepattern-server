package org.genepattern.server.webapp.jsf.jobinfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

/**
 * Helper class for managing visualizers on the Job Status page.
 * For automatically opening visualizers on the Job Status page.
 * Maintains state between AJAX requests.
 * 
 * @author pcarr
 */
public class VisualizerStatusBean implements Serializable {
    private static Logger log = Logger.getLogger(VisualizerStatusBean.class);

    public static class Obj implements Serializable {
        private String key = null;
        private JobInfoWrapper jobInfo = null;
        
        private boolean showAppletTag = false;
        
        public Obj(String key, JobInfoWrapper jobInfo) {
            this.key = key;
            this.jobInfo = jobInfo;
            this.showAppletTag = jobInfo != null;            
        }

        public void setJobInfo(JobInfoWrapper jobInfo) {
            this.jobInfo = jobInfo;
        }
        
        public String getKey() {
            return key;
        }
        
        public JobInfoWrapper getJobInfo() {
            return jobInfo;
        }
        
        public boolean getShowAppletTag() {
            return this.showAppletTag;
        }
        
        public void setShowAppletTag(boolean b) {
            this.showAppletTag = b;
        }
        
        public String getAppletTag() {
            if (jobInfo == null) {
                return "";
            }
            String tag = jobInfo.getVisualizerAppletTag();
            return tag;
        }
    }
    
    private JobInfoWrapper jobInfo = null;
    private boolean openVisualizers = false;
    
    public VisualizerStatusBean() {
        //this is here because the openVisualizers parameter may or may not be set
        //    not sure how to configure this properly with JSF in the faces-config.xml file
        String openVisualizersParameter = UIBeanHelper.getRequest().getParameter("openVisualizers");
        if (openVisualizersParameter != null) {
            this.openVisualizers = true;
        }
    }
    
    public void setJobInfo(JobInfoWrapper jobInfo) {
        this.jobInfo = jobInfo;
        if (jobInfo == null) {
            log.error("VisualizerStatusBean.setJobInfo: null");
            return;
        }
        
        this.enablePoll = !jobInfo.isFinished();
        this.dontUpdate = jobInfo.isFinished();
        
        for(int i=0; i<this.jobInfo.getNumVisualizers(); ++i) {
            visObjs.add(new Obj(""+i, null));
        }
        updateVisualizerFlags();
    }
    
    public void setOpenVisualizers(String val) {
        if (val != null && "true".equals(val)) {
            this.openVisualizers = true;   
        }
    }
    
    public boolean isOpenVisualizers() {
        return this.openVisualizers;
    }
    
    //map from index to job number, e.g. the first visualizer on the page has index 0 and a null job number
    //    once the visualizer is available for rendering assign job number to the correct value
    //    use that value to render the visualizer applet tag once and only once
    //private List<Obj> visObjsUpdate = new ArrayList<Obj>();
    private List<Obj> visObjs = new ArrayList<Obj>();

    public List<Obj> getVisObjs() {
        return visObjs;
    }
    
    /**
     * @return a list of visualizers which have not yet been opened.
     */
    public List<Obj> getNewVisObjs() {
        List<Obj> newVisObjs = new ArrayList<Obj>();
        for(Obj obj : visObjs) {
            if (obj.getShowAppletTag()) {
                newVisObjs.add(obj);
            }
        }
        return newVisObjs;
    }
    
    private List<JobInfoWrapper> getAllVisualizers() {
        List<JobInfoWrapper> v = new ArrayList<JobInfoWrapper>();
        if (jobInfo != null && jobInfo.isVisualizer() && "Finished".equals(jobInfo.getStatus()) ) {
            v.add(jobInfo);
        }
        else if (jobInfo != null && jobInfo.isPipeline()) {
            for(JobInfoWrapper step : this.jobInfo.getAllSteps()) {
                if (step != null && step.isVisualizer()) {
                    v.add(step);
                }
            }
        }
        return v;
    }
    
    private void updateVisualizerFlags() {
        List<JobInfoWrapper> current = getAllVisualizers();
        int idx = 0;
        for(JobInfoWrapper jobInfo : current) {
            //if an item previously had a null jobInfo and now has a non-null jobInfo ...
            //   ... automatically display the applet tag
            if (idx<visObjs.size()) {
                Obj prevState = visObjs.get(idx);
                if (prevState.getJobInfo() == null) {
                    prevState.setShowAppletTag(true);
                }
                else {
                    prevState.setShowAppletTag(false);
                }
                prevState.setJobInfo(jobInfo);
            }
            ++idx;
        }
    }
    
    private boolean dontUpdate = false;
    private boolean enablePoll = false;
    public boolean isEnablePoll() {
        return enablePoll;
    }

    public void updateAjaxPoll(ActionEvent actionEvent) {
        try {
            JobStatusBean jobStatusBean = (JobStatusBean) UIBeanHelper.getManagedBean("#{jobStatusBean}");
            if (jobStatusBean != null) {
                this.jobInfo = jobStatusBean.getJobInfo();
                this.enablePoll = !this.jobInfo.isFinished();
            }
        }
        catch (Exception e) {
            log.error("Error in getManagedBean(#{jobStatusBean}): "+e.getLocalizedMessage(), e);
            return;
        }
        if (this.jobInfo != null) {
            updateVisualizerFlags();
        }
        
        //test support for ajaxRendered ...
        PhaseId id = actionEvent.getPhaseId();
        if (PhaseId.RENDER_RESPONSE.equals(id)) {
            log.debug("about to render response ...");
            //TODO: keep track of ajaxKeys here
        }
    }

    //add support for ajax reRendering of other items, not just visualizers
    /**
     * rerender steps which have not yet started (could be null JobInfoWrapper) or that have not yet completed.
     * @return
     */
    public Set<Integer> getAjaxKeys() {
        Set<Integer> keys = new HashSet<Integer>();
        List<JobInfoWrapper> allStepsPlusRoot = jobInfo.getAllStepsIncludingRoot();
        
        int idx = 0;
        for(JobInfoWrapper step : allStepsPlusRoot) {
            keys.add(idx);
//            if (step == null) {
//                keys.add(idx);
//            }
//            else if (!step.isFinished()) {
//                keys.add(idx);
//            }
            ++idx;
        }
        return keys;
    }
    
    private Set<Integer> ajaxKeysPrevUpdate = new HashSet<Integer>();
    private void initAjaxKeys() {
        List<JobInfoWrapper> allStepsPlusRoot = jobInfo.getAllStepsIncludingRoot();
        int idx = 0;
        for(JobInfoWrapper step : allStepsPlusRoot) {
            if (step != null && step.isFinished()) {
                
            }
            else {
                ajaxKeysPrevUpdate.add(idx);
            }
            ++idx;
        }
    }
    private void updateAjaxKeys() {
        
    }
    
    private Set<Integer> stepsToUpdate = new HashSet<Integer>();
    private Set<Integer> finishedDuringLastRound = new HashSet<Integer>();
//    private void updateStepsToUpdate() {
//        if (dontUpdate) {
//            return;
//        }
//        stepsToUpdate //TODO: implement this method
//        //if the initial status of the job was complete, then don't update anything
//        //otherwise, anything that is still running is on the list,
//        //    also, the first time a step is finished it is on the list
//        
//    }

    
    
}
