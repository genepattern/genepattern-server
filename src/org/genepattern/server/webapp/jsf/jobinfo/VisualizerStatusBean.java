package org.genepattern.server.webapp.jsf.jobinfo;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

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
public class VisualizerStatusBean {
    private static Logger log = Logger.getLogger(VisualizerStatusBean.class);

    public static class Obj {
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
    }
    

    private JobInfoWrapper jobInfo = null;
    
    //map from index to job number, e.g. the first visualizer on the page has index 0 and a null job number
    //    once the visualizer is available for rendering assign job number to the correct value
    //    use that value to render the visualizer applet tag once and only once
    //private List<Obj> visObjsUpdate = new ArrayList<Obj>();
    private List<Obj> visObjs = new ArrayList<Obj>();

    public List<Obj> getVisObjs() {
        return visObjs;
    }
    
    public void setJobInfo(JobInfoWrapper jobInfo) {
        this.jobInfo = jobInfo;
        for(int i=0; i<this.jobInfo.getNumVisualizers(); ++i) {
            visObjs.add(new Obj(""+i, null));
        }
        updateVisualizerFlags();
    }
    
    private List<JobInfoWrapper> getAllVisualizers() {
        List<JobInfoWrapper> v = new ArrayList<JobInfoWrapper>();
        if (jobInfo != null && jobInfo.isVisualizer() && "Finished".equals(jobInfo.getStatus()) ) {
            v.add(jobInfo);
        }
        else if (jobInfo != null && jobInfo.isPipeline()) {
            for(JobInfoWrapper step : this.jobInfo.getAllSteps()) {
                if (step.isVisualizer()) {
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
    
    public void updateAjaxPoll(ActionEvent actionEvent) {
        try {
            JobStatusBean jobStatusBean = (JobStatusBean) UIBeanHelper.getManagedBean("#{jobStatusBean}");
            if (jobStatusBean != null) {
                this.jobInfo = jobStatusBean.getJobInfo();
            }
        }
        catch (Exception e) {
            log.error("Error in getManagedBean(#{jobStatusBean}): "+e.getLocalizedMessage(), e);
            return;
        }
        if (this.jobInfo != null) {
            updateVisualizerFlags();
        }
    }
    
}
