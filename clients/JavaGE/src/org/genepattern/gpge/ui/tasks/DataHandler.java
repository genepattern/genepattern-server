package org.genepattern.gpge.ui.tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.StructureValidationException;
import javax.xml.bind.UnmarshalException;

import org.genepattern.gpge.ui.analysis.jaxb.AnalysisData;
import org.genepattern.gpge.ui.analysis.jaxb.Attribute;
import org.genepattern.gpge.ui.analysis.jaxb.History;
import org.genepattern.gpge.ui.analysis.jaxb.Job;
import org.genepattern.gpge.ui.analysis.jaxb.Parameter;
import org.genepattern.gpge.ui.analysis.jaxb.Result;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

/**
 * <p>
 * Title: DataHandler.java
 * </p>
 * <p>
 * Description: Used to serialize and de-serialize between DataModel and XML
 * file
 * </p>
 * 
 * @author Hui Gong
 * @version 1.0
 */

public class DataHandler {

	public DataHandler() {

	}

	/**
	 * Gets XML string from
	 * <code>DataModel<code>, only history and result will be converted to XML
	 * @param model DataModel for the analysis UI
	 * @return XML string
	 * @throws IOException
	 * @throws StructureValidationException
	 */
	public static String getXML(DataModel model)
			throws StructureValidationException, IOException {
		AnalysisData data = new AnalysisData();
		List historyList = data.getHistory();
		List resultList = data.getResult();

		Vector history = model.getJobs();
		Enumeration enu = history.elements();
		while (enu.hasMoreElements()) {
			AnalysisJob ajob = (AnalysisJob) enu.nextElement();
			History his = new History();
			his.setSiteName(ajob.getServer());
			his.setTaskName(ajob.getTaskName());
			JobInfo jobInfo = ajob.getJobInfo();
			Job job = jobInfoToJob(jobInfo);
			his.setJob(job);
			historyList.add(his);
		}
		Hashtable results = model.getResults();
		Enumeration siteEnu = results.keys();
		while (siteEnu.hasMoreElements()) {
			String siteName = (String) siteEnu.nextElement();
			Hashtable taskResults = (Hashtable) results.get(siteName);
			Enumeration taskEnu = taskResults.keys();
			Result result = new Result();
			List jobList = result.getJob();
			while (taskEnu.hasMoreElements()) {
				String taskName = (String) taskEnu.nextElement();
				Vector jobs = (Vector) taskResults.get(taskName);
				Enumeration jobEnu = jobs.elements();
				while (jobEnu.hasMoreElements()) {
					AnalysisJob ajob = (AnalysisJob) jobEnu.nextElement();
					Job job = jobInfoToJob(ajob.getJobInfo());
					jobList.add(job);
				}
				result.setSiteName(siteName);
				result.setTaskName(taskName);
				resultList.add(result);
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		data.validate();
		data.marshal(out);
		return out.toString();
	}

	/**
	 * Saves the DataModel into a XML file
	 * 
	 * @param model
	 *            DataModel for the analysis UI
	 * @param fileName
	 *            the xml file name
	 * @throws IOException
	 * @throws StructureValidationException
	 */
	public static void saveData(DataModel model, String fileName)
			throws IOException, StructureValidationException {
		final String data = getXML(model);
		FileWriter writer = new FileWriter(fileName);
		writer.write(data);
		writer.flush();
		writer.close();
	}

	/**
	 * Load data from a xml file and restore it to <code>DataModel<code>
	 * @param filename for a XML file containing result and history
	 * @return DataModel for analysis UI
	 * @throws FileNotFoundException
	 * @throws UnmarshalException
	 */
	public static DataModel loadDate(String filename)
			throws FileNotFoundException, UnmarshalException {
		File file = new File(filename);
		return loadData(file);
	}

	/**
	 * Load data from a xml file and restore it to <code>DataModel<code>
	 * @param file XML file containing result and history
	 * @return DataModel for analysis UI
	 * @throws FileNotFoundException
	 * @throws UnmarshalException
	 */
	public static DataModel loadData(File file) throws FileNotFoundException,
			UnmarshalException {
		DataModel dataModel = new DataModel();
		Vector history = new Vector();
		Hashtable results = new Hashtable();

		FileInputStream in = new FileInputStream(file);
		AnalysisData data = AnalysisData.unmarshal(in);
		List historyList = data.getHistory();
		List resultList = data.getResult();

		//restore the history
		Iterator hisIt = historyList.iterator();
		while (hisIt.hasNext()) {
			History jobHistory = (History) hisIt.next();
			JobInfo jobInfo = jobToJobInfo(jobHistory.getJob());
			//System.out.println("Job ID: "+jobInfo.getJobNumber()+" Submitted:
			// "+jobInfo.getDateSubmitted());
			AnalysisJob analysisJob = new AnalysisJob(jobHistory.getSiteName(),
					jobHistory.getTaskName(), jobInfo);
			history.add(analysisJob);
		}

		//restore the result
		Iterator resultIt = resultList.iterator();
		while (resultIt.hasNext()) {
			Hashtable taskResults;
			Result result = (Result) resultIt.next();
			String siteName = result.getSiteName();
			String taskName = result.getTaskName();
			List jobList = result.getJob();
			Iterator jobListIt = jobList.iterator();
			while (jobListIt.hasNext()) {
				Job job = (Job) jobListIt.next();
				JobInfo jobInfo = jobToJobInfo(job);
				AnalysisJob aJob = new AnalysisJob(siteName, taskName, jobInfo);
				if (results.containsKey(siteName)) {
					taskResults = (Hashtable) results.get(siteName);
					if (taskResults.containsKey(taskName)) {
						Vector jobs = (Vector) taskResults.get(taskName);
						jobs.add(aJob);
					} else {
						Vector jobs = new Vector();
						jobs.add(aJob);
						taskResults.put(taskName, jobs);
					}
				} else {
					taskResults = new Hashtable();
					Vector jobs = new Vector();
					jobs.add(aJob);
					taskResults.put(taskName, jobs);
					results.put(siteName, taskResults);
				}
			}
		}
		dataModel.resetData(history, results);
		return dataModel;
	}

	private static Job jobInfoToJob(JobInfo jobInfo) {
		Job job = new Job();
		job.setId(Integer.toString(jobInfo.getJobNumber()));
		Date completed = jobInfo.getDateCompleted();
		if (completed != null)
			job.setDateCompleted(String.valueOf(completed.getTime()));
		else
			job.setDateCompleted("");
		Date submittedTime = jobInfo.getDateSubmitted();
		if (submittedTime != null)
			job.setDateSubmitted(String.valueOf(submittedTime.getTime()));
		else
			job.setDateSubmitted("");
		
		job.setStatus(jobInfo.getStatus());
		job.setTaskId(Integer.toString(jobInfo.getTaskID()));
		ParameterInfo[] params = jobInfo.getParameterInfoArray();
		List paramList = job.getParameter();
		for (int i = 0; i < params.length; i++) {
			ParameterInfo paramInfo = params[i];
			Parameter param = new Parameter();
			param.setName(paramInfo.getName());
			param.setValue(paramInfo.getValue());
			param.setDescription(paramInfo.getDescription());
			HashMap attributeMap = (HashMap) paramInfo.getAttributes();
			List attributeList = param.getAttribute();
			Iterator it = attributeMap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				String value = (String) attributeMap.get(key);
				Attribute attribute = new Attribute();
				attribute.setKey(key);
				attribute.setContent(value);
				attributeList.add(attribute);
			}
			paramList.add(param);
		}
		return job;
	}

	private static JobInfo jobToJobInfo(Job job) {
		JobInfo jobInfo = new JobInfo();
		jobInfo.setJobNumber(Integer.parseInt(job.getId()));
		String completedTime = job.getDateCompleted();
		if (!completedTime.equals(""))
			jobInfo.setDateCompleted(new Date(Long.parseLong(completedTime)));
		String submittedTime = job.getDateSubmitted();
		if (!submittedTime.equals(""))
			jobInfo.setDateSubmitted(new Date(Long.parseLong(submittedTime)));
		
		jobInfo.setStatus(job.getStatus());
		jobInfo.setTaskID(Integer.parseInt(job.getTaskId()));
		List paramList = job.getParameter();
		Iterator paramIt = paramList.iterator();
		while (paramIt.hasNext()) {
			Parameter param = (Parameter) paramIt.next();
			List attributeList = param.getAttribute();

			ParameterInfo paramInfo = new ParameterInfo();
			paramInfo.setName(param.getName());
			paramInfo.setValue(param.getValue());
			paramInfo.setDescription(param.getDescription());
			HashMap attributeMap = new HashMap();

			Iterator attributeIt = attributeList.iterator();
			while (attributeIt.hasNext()) {
				Attribute attribute = (Attribute) attributeIt.next();
				attributeMap.put(attribute.getKey(), attribute.getContent());
			}
			paramInfo.setAttributes(attributeMap);
			jobInfo.addParameterInfo(paramInfo);

		}
		return jobInfo;
	}
}