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


package org.genepattern.server.webapp;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import org.genepattern.codegenerator.AbstractPipelineCodeGenerator;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.util.LSID;

public class PipelineController {

	IPipelineView viewer = null;

	PipelineModel model = null;

	boolean isLsidSet = false;

	// transient means: don't persist!!
	transient Collection tmTasks = null; // collection of TaskInfo objects

	public PipelineController(IPipelineView viewer, PipelineModel model) {
		this.viewer = viewer;
		this.model = model;
	}

	public PipelineController(PipelineModel model) {
		this.model = model;
	}

	public void init() throws OmnigeneException, RemoteException {
		model.init();
		tmTasks = getCatalog();
		viewer.init(tmTasks, model.getUserID());
	}

	public void begin() {
		viewer.begin();
	}

	public void end() {
		viewer.end();
	}

	public void displayTask(TaskInfo ti) {
		viewer.generateTask(ti);
	}

	public void displayTasks() throws OmnigeneException, RemoteException {
		TaskInfo ti = null;
		for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
			ti = (TaskInfo) itTasks.next();
			displayTask(ti);
		}
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

			Vector probs = GenePatternAnalysisTask.installTask(model.getName() + "."+ GPConstants.TASK_TYPE_PIPELINE, 
					""+model.getDescription(), 
					params, 
					tia, 
					model.getUserID(), 
					model.isPrivate() ? GPConstants.ACCESS_PRIVATE
						: GPConstants.ACCESS_PUBLIC, 
					null);
        		if ((probs != null) && (probs.size() > 0)) {
            		throw new TaskInstallationException(probs);
        		}
   
			/** if (lsid != null && lsid.length() > 0) {
				//System.out.println("AbstractPipelineCodeGenerator.generateTask:
				// updating " + lsid);
				lsid = GenePatternAnalysisTask.updateTask(, ""
						+ model.getDescription(), params, tia, model.getUserID(), model
						.isPrivate() ? GPConstants.ACCESS_PRIVATE
						: GPConstants.ACCESS_PUBLIC);
			} else {
				lsid = GenePatternAnalysisTask.installNewTask(model.getName()
						+ "." + GPConstants.TASK_TYPE_PIPELINE, ""
						+ model.getDescription(), params, tia, model.getUserID(), model
						.isPrivate() ? GPConstants.ACCESS_PRIVATE
						: GPConstants.ACCESS_PUBLIC, null);
			} **/

			return model.getLsid();
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

	public Collection getCatalog() throws OmnigeneException, RemoteException {
		if (tmTasks == null) {
			tmTasks = loadTaskCatalog();
		}
		return tmTasks;
	}

	public Collection loadTaskCatalog() throws OmnigeneException,
			RemoteException {
		try {
			return new LocalAdminClient(model.getUserID()).getTaskCatalog();
		} catch (WebServiceException wse) {
			throw new OmnigeneException(wse.getMessage());
		}
	}

	public void setModel(PipelineModel model) {
		this.model = model;
	}

	public static void main(String args[]) {
		try {
			HTMLPipelineView viewer = new HTMLPipelineView(
					new OutputStreamWriter(System.out), "http:", "localhost", "8080", "gp", "Mozilla/4.78", null);
			PipelineModel model = new PipelineModel();
			PipelineController controller = new PipelineController(viewer,
					model);

			controller.init();
			controller.begin();
			//controller.displayTasks();
			controller.displayTask(new TaskInfo());
			controller.end();

		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
		System.out.println("done");
		System.exit(1);
	}
}