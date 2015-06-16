/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp;

import java.util.Vector;

import org.genepattern.codegenerator.AbstractPipelineCodeGenerator;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class PipelineCreationHelper {
    PipelineModel model = null;
    boolean isLsidSet = false;

    public PipelineCreationHelper(PipelineModel model) {
        this.model = model;
    }

    

   public String generateTask() throws TaskInstallationException {
        String lsid = generateTask(AbstractPipelineCodeGenerator
                .giveParameterInfoArray(model));
        model.setLsid(lsid);
        return lsid;
    }
   
    public String generateLSID(){
        try {
            LSID taskLSID = GenePatternAnalysisTask.getNextTaskLsid(model.getLsid());
            model.setLsid( taskLSID.toString());        
            isLsidSet = true;
            return taskLSID.toString();
        } catch (Exception e){
            return null;
        }
    }


    public String generateTask(ParameterInfo[] params)
            throws TaskInstallationException {
        try {
            // set the LSID before using the code generator
            if (!isLsidSet){
                generateLSID(); 
            }

            TaskInfoAttributes tia = AbstractPipelineCodeGenerator.getTaskInfoAttributes(model);
            
            tia.put(GPConstants.CPU_TYPE, GPConstants.ANY);
            tia.put(GPConstants.OS, GPConstants.ANY);
            tia.put(GPConstants.LANGUAGE, "Java");
            tia.put(GPConstants.SERIALIZED_MODEL, model.toXML());
            tia.put(GPConstants.USERID, model.getUserID());

            // bug 1555 // Vector probs = GenePatternAnalysisTask.installTask(model.getName() + "."+ GPConstants.TASK_TYPE_PIPELINE, 
             Vector probs = GenePatternAnalysisTask.installTask(model.getName(), 
                    ""+model.getDescription(), 
                    params, 
                    tia, 
                    model.getUserID(), 
                    model.isPrivate() ? GPConstants.ACCESS_PRIVATE
                        : GPConstants.ACCESS_PUBLIC, 
                    null,
                    new InstallInfo(InstallInfo.Type.CREATE));
                if ((probs != null) && (probs.size() > 0)) {
                    throw new TaskInstallationException(probs);
                }
   
            String newLsid = tia.get("LSID");   

            return newLsid;
        } catch (TaskInstallationException tie) {
            throw tie;
        } catch (Exception e) {
            Vector vProblems = new Vector();
            vProblems.add(e.getMessage() + " while generating task "
                    + model.getName());
            throw new TaskInstallationException(vProblems);
        }
    } 

    public ParameterInfo[] giveParameterInfoArray() {
        return AbstractPipelineCodeGenerator.giveParameterInfoArray(model);
    }

    public TaskInfoAttributes giveTaskInfoAttributes() {
        return AbstractPipelineCodeGenerator.getTaskInfoAttributes(model);
    }


    
}
