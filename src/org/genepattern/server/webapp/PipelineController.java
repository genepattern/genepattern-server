package org.genepattern.server.webapp;

import java.io.OutputStreamWriter;
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


public class PipelineController {

	IPipelineView viewer = null;

	PipelineModel model = null;

	AbstractPipelineCodeGenerator codeGenerator = null;

	// transient means: don't persist!!
	transient Collection tmTasks = null; // collection of TaskInfo objects

	public PipelineController(IPipelineView viewer, PipelineModel model) {
		this.viewer = viewer;
		this.model = model;
	}

	public PipelineController(AbstractPipelineCodeGenerator codeGenerator,
			PipelineModel model) {
		this.codeGenerator = codeGenerator;
		this.model = model;
	}

	public void addTask(JobSubmission jobSubmission) {
		model.addTask(jobSubmission);
	}

	public String generateUserInstructions() {
		return codeGenerator.emitUserInstructions();
	}

	public String generateCode() throws Exception {
		return codeGenerator.generateCode();
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

	public String invoke() {
		return codeGenerator.invoke();
	}

	public void displayTasks() throws OmnigeneException, RemoteException {
		TaskInfo ti = null;
		for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
			ti = (TaskInfo) itTasks.next();
			displayTask(ti);
		}
	}

   public String generateTask() throws TaskInstallationException {
		String lsid = generateTask(codeGenerator
				.giveParameterInfoArray());
		model.setLsid(lsid);
		return lsid;
	}
   
   
	public String generateTask(ParameterInfo[] params)
			throws TaskInstallationException {
		try {
			TaskInfoAttributes tia = codeGenerator.getTaskInfoAttributes();
			tia.put(codeGenerator.getLanguage() + GPConstants.INVOKE, codeGenerator.invoke()); // save
																   // invocation
																   // string in
																   // TaskInfoAttributes
			tia.put(GPConstants.CPU_TYPE, GPConstants.ANY);
			tia.put(GPConstants.OS, GPConstants.ANY);
			tia.put(GPConstants.LANGUAGE, "Java");
			tia.put(GPConstants.SERIALIZED_MODEL, model.toXML());
			tia.put(GPConstants.USERID, model.getUserID());

			String lsid = (String) tia.get(GPConstants.LSID);
			if (lsid != null && lsid.length() > 0) {
				//System.out.println("AbstractPipelineCodeGenerator.generateTask:
				// updating " + lsid);
				lsid = GenePatternAnalysisTask.updateTask(model.getName() + "."
						+ GPConstants.TASK_TYPE_PIPELINE, ""
						+ model.getDescription(), params, tia, model.getUserID(), model
						.isPrivate() ? GPConstants.ACCESS_PRIVATE
						: GPConstants.ACCESS_PUBLIC);
			} else {
				lsid = GenePatternAnalysisTask.installNewTask(model.getName()
						+ "." + GPConstants.TASK_TYPE_PIPELINE, ""
						+ model.getDescription(), params, tia, model.getUserID(), model
						.isPrivate() ? GPConstants.ACCESS_PRIVATE
						: GPConstants.ACCESS_PUBLIC);
			}
			return lsid;
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
		return codeGenerator.giveParameterInfoArray();
	}

	public TaskInfoAttributes giveTaskInfoAttributes() {
		return codeGenerator.getTaskInfoAttributes();
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
					new OutputStreamWriter(System.out),
					"http://localhost:8080/gp/makePipeline.jsp",
					"Mozilla/4.78", null);
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