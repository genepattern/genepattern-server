package org.genepattern.data.pipeline;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PipelineModel implements Serializable {
	public static final String INHERIT_TASKNAME = "inheritTaskname"; // should
																	 // be in
																	 // CONSTANTS

	public static final String INHERIT_FILENAME = "inheritFilename"; // should
																	 // be in
																	 // CONSTANTS

	public static final String RUNTIME_PARAM = "runTimePrompt"; // should be in
																// CONSTANTS

	// o:XML tags for tasks and visualizers
	protected static final String TAG_TASK = "task";

	protected static final String TAG_VISUALIZER = "visualizer";

	protected static final String TAG_PARAM = "param";

	protected static final String TAG_VARIABLE = "variable";

	public static final String DESC = "_description";

	protected String name = "";

	protected String description = "";

	protected String author = "";

	protected boolean privacy = true;

	protected String display = "";

	protected String userID = "";

	protected String version = null;

	protected String lsid = "";

	public static final String PIPELINE_MODEL = GPConstants.SERIALIZED_MODEL;

	TreeMap hmParameters = new TreeMap(); // run-time prompt for these

	Vector vTasks = new Vector(); // Vector of JobSubmission objects

	public PipelineModel() {
	}

	public void init() {
	}

	public Vector getTasks() {
		return vTasks;
	}

	public TreeMap getInputParameters() {
		return hmParameters;
	}

	public void addInputParameter(String name, ParameterInfo p) {
		hmParameters.put(name, p);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name != null)
			this.name = name;
	}

	public boolean isPrivate() {
		return privacy;
	}

	public void setPrivacy(boolean privacy) {
		this.privacy = privacy;
	}

	public void setPrivacy(String privacy) {
		if (privacy.equalsIgnoreCase(GPConstants.PRIVATE)) {
			setPrivacy(true);
		} else if (privacy.equalsIgnoreCase(GPConstants.PUBLIC)) {
			setPrivacy(false);
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if (description != null)
			this.description = description;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		if (author != null)
			this.author = author;
	}

	public String getUserID() {
		return userID;
	}

	public String getUserid() {
		return getUserID();
	}

	public String getVersion() {
		return version;
	}

	public void setUserID(String userID) {
		if (userID != null)
			this.userID = userID;
	}

	public void setUserid(String userID) {
		setUserID(userID);
	}

	public void setVersion(String version) {
		if (version != null)
			this.version = version;
	}

	public String getLsid() {
		return lsid;
	}

	public void setLsid(String lsid) {
		if (lsid == null)
			lsid = "";
		this.lsid = lsid;
	}

	public void addTask(JobSubmission jobSubmission) {
		vTasks.add(jobSubmission);
	}

	// set the task at a given position in the list (zero-based)
	public void addTask(JobSubmission jobSubmission, int slot) {
		if (vTasks.size() <= slot) {
			vTasks.setSize(slot + 1);
		}
		vTasks.set(slot, jobSubmission);
	}

	// serialize the PipelineModel to a String
	public String serialize() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this);
		oos.close();
		return baos.toString();
	}

	// deserialize the PipelineModel from a String
	public static PipelineModel deserialize(String serializedModel)
			throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(serializedModel
				.getBytes());
		ObjectInputStream ois = new ObjectInputStream(bais);
		PipelineModel model = (PipelineModel) ois.readObject();
		ois.close();
		return model;
	}

	public String xmlEncode(String what) {
		if (what == null)
			what = "";
		what = what.replaceAll("&", "&amp;");
		what = what.replaceAll("'", "&apos;");
		what = what.replaceAll("\"", "&quot;");
		what = what.replaceAll("<", "&lt;");
		what = what.replaceAll(">", "&gt;");
		what = what.replaceAll("%", "%25");
		what = what.replaceAll("=", "%3D");
		what = what.replaceAll("\\\\", "\\\\\\\\"); // replace backslash with
													// double backslash
		what = "'" + what + "'";
		return what;
	}

	public static String xmlDecode(String what) {
		if (what.startsWith("'") && what.endsWith("'")) {
			what = what.substring(1, what.length() - 1);
		}
		what = what.replaceAll("&apos;", "'");
		what = what.replaceAll("&quot;", "\"");
		what = what.replaceAll("&amp;", "&");
		what = what.replaceAll("&lt;", "<");
		what = what.replaceAll("&gt;", ">");
		what = what.replaceAll("%3D", "=");
		what = what.replaceAll("%25", "%");
		what = what.replaceAll("\\\\\\\\", "\\\\"); // replace double backslash
													// with single backslash
		return what;
	}

	public String toXML() {
		StringWriter outputWriter = new StringWriter();
		JobSubmission task = null;
		Enumeration eParams = null;
		ParameterInfo p = null;
		Map attributes = null;
		HashMap empty = new HashMap();
		Vector vTasks = getTasks();
		String paramName = null;

		outputWriter.write("<?xml version=\"1.0\" ?>\n");
		outputWriter
				.write("<program comments=\"preserve\" space=\"ignore\">\n\n");

		// input parameters (runtime prompt)
		if (getInputParameters().size() > 0) {
			// <param name="foo"/> for each pipeline input parameter that isn't
			// hardcoded
			outputWriter.write("\t<!-- pipeline input parameters -->\n");
			for (Iterator itInputs = getInputParameters().keySet().iterator(); itInputs
					.hasNext();) {
				paramName = (String) itInputs.next();
				p = (ParameterInfo) getInputParameters().get(paramName);
				outputWriter.write("\t<" + TAG_PARAM + " name=\"" + paramName
						+ "\" type=\"" + (p.isInputFile() ? "File" : "String")
						+ "\"/>\n");
			}
			outputWriter.write("\n");
		}

		outputWriter.write("\t<!-- pipeline metadata -->\n");
		outputWriter.write("\t<" + TAG_VARIABLE + " name=\"" + GPConstants.NAME
				+ "\" select=\"" + xmlEncode(getName()) + "\"/>\n");
		outputWriter.write("\t<" + TAG_VARIABLE + " name=\""
				+ GPConstants.DESCRIPTION + "\" select=\""
				+ xmlEncode(getDescription()) + "\"/>\n");
		outputWriter.write("\t<" + TAG_VARIABLE + " name=\""
				+ GPConstants.AUTHOR + "\" select=\"" + xmlEncode(getAuthor())
				+ "\"/>\n");
		outputWriter.write("\t<"
				+ TAG_VARIABLE
				+ " name=\""
				+ GPConstants.PRIVACY
				+ "\" select=\""
				+ xmlEncode(isPrivate() ? GPConstants.PRIVATE
						: GPConstants.PUBLIC) + "\"/>\n");
		outputWriter.write("\t<" + TAG_VARIABLE + " name=\""
				+ GPConstants.USERID + "\" select=\"" + xmlEncode(getUserID())
				+ "\"/>\n");
		outputWriter.write("\t<" + TAG_VARIABLE + " name=\""
				+ GPConstants.VERSION + "\" select=\""
				+ xmlEncode(getVersion()) + "\"/>\n");

		// LSID isn't really part of the persistent model. It comes from the
		// enveloping task.
		//outputWriter.write("\t<" + TAG_VARIABLE + " name=\"" +
		// GPConstants.LSID.toLowerCase() + "\" select=\"" +
		// xmlEncode(getLsid()) + "\"/>\n");

		outputWriter.write("\n");

		// now write out all of the task invocations
		outputWriter.write("\t<!-- task invocations -->\n");
		int taskNum = 1;
		String lsid = null;
		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
			task = (JobSubmission) eTasks.nextElement();
			String tag = task.isVisualizer() ? TAG_VISUALIZER : TAG_TASK;
			outputWriter.write("\t<" + tag + "\tname=\""
					+ xmlEncode(task.getName()) + "\"\n");
			outputWriter.write("\t\tid=\"" + taskNum + "\"\n");
			outputWriter.write("\t\t" + GPConstants.LSID + "=\""
					+ task.getLSID() + "\"\n");
			outputWriter.write("\t\t" + DESC + "=\""
					+ xmlEncode(task.getDescription()) + "\"");
			for (eParams = task.getParameters().elements(); eParams
					.hasMoreElements();) {
				p = (ParameterInfo) eParams.nextElement();
				attributes = p.getAttributes();
				if (attributes == null) {
					attributes = empty;
				}
				HashMap pAttributes = p.getAttributes();
				if (pAttributes == null)
					pAttributes = new HashMap();
				String inheritedFilename = (String) pAttributes
						.get(INHERIT_FILENAME);
				String inheritedTaskNum = (String) pAttributes
						.get(INHERIT_TASKNAME);
				if (inheritedTaskNum != null
						&& !inheritedTaskNum.equals("NOT SET")
						&& inheritedFilename != null) {
					int t = Integer.parseInt(inheritedTaskNum);
					outputWriter.write("\n\t\t" + p.getName()
							+ "=\"gpUseResult(" + (t + 1) + ", '"
							+ inheritedFilename + "')\"");
				} else if (attributes.get("runTimePrompt") == null) {
					outputWriter.write("\n\t\t" + p.getName() + "=\""
							+ xmlEncode(p.getValue()) + "\"");
				} else {
					outputWriter.write("\n\t\t" + p.getName() + "=\"$"
							+ task.getName() + taskNum + "." + p.getName()
							+ "\"");
				}
			}
			outputWriter.write("/>\n");
		}

		outputWriter.write("\n</program>");
		return outputWriter.toString();
	}

	public static PipelineModel toPipelineModel(String inputXML)
			throws IOException, SAXException, ParserConfigurationException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, OmnigeneException, Exception {
		return toPipelineModel(inputXML, false);
	}

   public static PipelineModel toPipelineModel(String inputXML, boolean verify) throws IOException, SAXException, ParserConfigurationException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, OmnigeneException, Exception {
      return toPipelineModel(new InputSource(new StringReader(inputXML)), verify);
   }
   
	public static PipelineModel toPipelineModel(InputSource inputXMLSource, boolean verify)
			throws IOException, SAXException, ParserConfigurationException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, OmnigeneException, Exception {
		//String DBF = "javax.xml.parsers.DocumentBuilderFactory";
		//String oldDocumentBuilderFactory = System.getProperty(DBF);
		//System.setProperty(DBF,
		// "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().parse(inputXMLSource);
		//if (oldDocumentBuilderFactory != null)
		//	System.setProperty(DBF, oldDocumentBuilderFactory);
		Element root = doc.getDocumentElement();

		//System.out.println("XML=" + inputXML);
		//System.out.println(dumpDOM(root, 0));

		PipelineModel model = new PipelineModel();
		model.init();
		String name = null;
		String value = null;
		ParameterInfo pi = null;

		// process "variable" elements(metadata)
		NodeList variables = doc.getElementsByTagName(TAG_VARIABLE);
		Class clsModel = model.getClass();
		Class[] arrayOfString = new Class[] { String.class };
		for (int v = 0; v < variables.getLength(); v++) {
			NamedNodeMap variable = variables.item(v).getAttributes();
			name = ((Attr) variable.getNamedItem("name")).getValue();
			value = xmlDecode(((Attr) variable.getNamedItem("select"))
					.getValue());
			// invoke the setter methods in PipelineModel, once for each
			// <variable> tag
			String methodName = "set" + name.substring(0, 1).toUpperCase()
					+ name.substring(1);
			clsModel.getMethod(methodName, arrayOfString).invoke(model,
					new String[] { value });
		}

		// process "task" elements
		NodeList tasks = doc.getElementsByTagName(TAG_TASK);
		for (int t = 0; t < tasks.getLength(); t++) {
			NamedNodeMap task = tasks.item(t).getAttributes();
			model.addTask(task, verify);
		}

		// process visualizer(s)
		NodeList visualizers = doc.getElementsByTagName(TAG_VISUALIZER);
		for (int v = 0; v < visualizers.getLength(); v++) {
			NamedNodeMap task = visualizers.item(v).getAttributes();
			model.addVisualizer(task, verify);
		}

		// process runtime prompt input parameters
		NodeList params = doc.getElementsByTagName(TAG_PARAM);
		for (int p = 0; p < params.getLength(); p++) {
			NamedNodeMap param = params.item(p).getAttributes();
			model
					.addRTParameter(((Attr) param.getNamedItem("name"))
							.getValue());
		}

		return model;
	}

	// add a runtime prompt parameter to the model
	protected void addRTParameter(String name) {
		String taskName = name.substring(0, name.indexOf("."));
		int taskNum = 1;
		ParameterInfo pi = null;
		findTask: for (Enumeration eTasks = getTasks().elements(); eTasks
				.hasMoreElements(); taskNum++) {
			JobSubmission job = (JobSubmission) eTasks.nextElement();
			if ((job.getName() + taskNum).equals(taskName)) {
				String unprefixedName = name.substring(name.indexOf(".") + 1);
				int paramNum = 0;
				for (Enumeration eParams = job.getParameters().elements(); eParams
						.hasMoreElements(); paramNum++) {
					pi = (ParameterInfo) eParams.nextElement();
					if (pi.getName().equals(unprefixedName)) {
						pi.setValue("");
						HashMap attributes = pi.getAttributes();
						if (attributes == null) {
							attributes = new HashMap();
						}
						attributes.put("runTimePrompt", "1");
						pi.setAttributes(attributes);
						boolean runTimePrompt[] = job.getRuntimePrompt();
						runTimePrompt[paramNum] = true;
						job.setRuntimePrompt(runTimePrompt);
						addInputParameter(name, pi);
						break findTask;
					}
				}
			}
		}
	}

	protected void addTask(NamedNodeMap task, boolean verify) throws Exception,
			OmnigeneException {
		addTaskToModel(task, verify, TAG_TASK);
	}

	protected void addVisualizer(NamedNodeMap task, boolean verify)
			throws Exception, OmnigeneException {
		addTaskToModel(task, verify, TAG_VISUALIZER);
	}

	// throws Exception if verification is requested and it fails
	protected void addTaskToModel(NamedNodeMap task, boolean verify, String tag)
			throws Exception, OmnigeneException {
		String taskNumber = null;
		String lsid = null;
		String value = null;
		ParameterInfo pi = null;
		JobSubmission job = new JobSubmission();
		String paramName = null;

		String taskName = xmlDecode(((Attr) task.removeNamedItem("name"))
				.getValue());
		job.setName(taskName);
		String description = xmlDecode(((Attr) task
				.removeNamedItem("_description")).getValue());
		job.setDescription(description);
		taskNumber = ((Attr) task.removeNamedItem("id")).getValue();
		try {
			lsid = ((Attr) task.removeNamedItem(GPConstants.LSID)).getValue();
			job.setLSID(lsid);
		} catch (org.w3c.dom.DOMException de) {
			lsid = "";
		}

		TaskInfo taskInfo = null;
		if (verify) {
			HashMap hmParams = new HashMap(task.getLength());
			for (int i = 0; i < task.getLength(); i++) {
				paramName = ((Attr) task.item(i)).getName();
				value = xmlDecode(((Attr) task.item(i)).getValue());
				hmParams.put(paramName, value);
			}

			taskInfo = GenePatternAnalysisTask.getTaskInfo(
					(lsid.length() > 0 ? lsid : taskName), getUserID());
			if (taskInfo == null) {
				throw new Exception("No such task: " + taskName);
			}
			ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
			for (int p = 0; formalParameters != null
					&& p < formalParameters.length; p++) {
				pi = formalParameters[p];
				paramName = pi.getName();
				String found = (String) hmParams.remove(paramName);
				if (found == null) {
					throw new Exception(getName()
							+ ": missing pipeline parameter " + paramName
							+ " for task " + job.getName());
				}
			}

			if (hmParams.size() > 0) {
				System.err.println(getName()
						+ ": extra unused pipeline parameters for task "
						+ job.getName() + ": ");
				for (Iterator itParams = hmParams.keySet().iterator(); itParams
						.hasNext();) {
					paramName = (String) itParams.next();
					value = (String) hmParams.get(paramName);
					throw new Exception(paramName + "=" + value);
				}
			}
		} // end of verification testing

		ParameterInfo[] formalParameters = null;
		if (taskInfo == null) {
			try {
				taskInfo = GenePatternAnalysisTask.getTaskInfo(
						(lsid.length() > 0 ? lsid : taskName), getUserID());
			} catch (Throwable t) {

			} // old pipelines don't have hsqldb.jar on classpath
			if (taskInfo == null) {
				//System.out.println("No such task: " + (lsid.length() > 0 ?
				// lsid : taskName));
				//throw new Exception("No such task: " + (lsid.length() > 0 ?
				// lsid : taskName));

			}
		}
		job.setTaskInfo(taskInfo);

		//for (int p = 0; formalParameters != null && p <
		// formalParameters.length; p++) {
		if (taskInfo == null) {
			formalParameters = null;
		} else {
			formalParameters = taskInfo.getParameterInfoArray();
		}
		ArrayList orderedParams = new ArrayList(); // to preserve order of
												   // formal params
		for (int i = 0; i < task.getLength(); i++)
			orderedParams.add(null);


		int idx = -1;

		while (task.getLength() > 0) {
				
			paramName = ((Attr) task.item(0)).getName();

			if (taskInfo == null) {
				pi = new ParameterInfo(paramName, "", "");
				pi.setAttributes(new HashMap());
				//idx = 0;
				idx++;
			} else {
				for (int p = 0; formalParameters != null
						&& p < formalParameters.length; p++) {
					pi = formalParameters[p];
					idx = p;
					if (pi.getName().equals(paramName))
						break;
				}
			}

			try {
				value = ((Attr) task.removeNamedItem(paramName)).getValue();
				if (value.startsWith("'")) {
					value = xmlDecode(value);
					try {
						URL u = new URL(value);
						//pi.getAttributes().put(ParameterInfo.MODE,
						// ParameterInfo.URL_INPUT_MODE);
						pi.getAttributes().remove(ParameterInfo.MODE);
						pi.getAttributes().remove(ParameterInfo.TYPE);
					} catch (MalformedURLException mue) {
						if (value.startsWith(GPConstants.LEFT_DELIMITER
								+ "GenePatternURL"
								+ GPConstants.RIGHT_DELIMITER)) {
							//pi.getAttributes().put(ParameterInfo.MODE,
							// ParameterInfo.URL_INPUT_MODE);
							pi.getAttributes().remove(ParameterInfo.MODE);
							pi.getAttributes().remove(ParameterInfo.TYPE);
						}
					}
					pi.setValue(value);
					//System.out.println("" + idx + ". " + pi.getName() + "=" + pi.toString());
					//job.addParameter(pi);
					orderedParams.set(idx, pi);

				} else {
					// it's either an inherited input file or an input parameter
					// substitution
					if (value.startsWith("$")) {
						//System.err.println("handle pipeline input parameter
						// value for " + job.getName() + taskNumber + "'s " +
						// paramName);
						pi.setValue(value);
						//job.addParameter(pi);
						orderedParams.set(idx, pi);

					} else {
						// it's an inherited input file

						// fix up the ParameterInfo entry
						pi.setValue("");

						// parse something like gpUseResult(taskNum,
						// inheritedFilename);
						StringTokenizer stInherit = new StringTokenizer(value,
								"(,')");
						stInherit.nextToken(); // skip "gpUseResult" and the
											   // open paren delimiter
						int inheritTaskNum = Integer.parseInt(stInherit
								.nextToken()) - 1;
						pi.getAttributes().put(INHERIT_TASKNAME,
								Integer.toString(inheritTaskNum));
						stInherit.nextToken(); // skip space after the comma
						pi.getAttributes().put(INHERIT_FILENAME,
								stInherit.nextToken());
						//System.out.println("inherited parameter: " + pi);
						//job.addParameter(pi);
						orderedParams.set(idx, pi);

					}
				}
			} catch (DOMException de) {
				throw new Exception("Parameter " + paramName
						+ " does not exist in pipeline definition for task "
						+ taskName);
			}

		}
		// add in the order of the formal params
		for (int p = 0; p < orderedParams.size(); p++) {
			pi = (ParameterInfo) orderedParams.get(p);
			if (pi != null)
				job.addParameter(pi);
		}

		int numParams = job.getParameters().size();
		boolean runTimePrompt[] = new boolean[numParams];
		for (int i = 0; i < numParams; i++) {
			runTimePrompt[i] = false;
		}
		job.setRuntimePrompt(runTimePrompt);
		job.setVisualizer(tag.equals(TAG_VISUALIZER));

		// TODO: use ID to ensure that this JobSubmission occupies the
		// appropriate position in the
		// Vector of JobSubmissions within the PipelineModel
		addTask(job, Integer.parseInt(taskNumber) - 1);
	}

	protected static String dumpDOM(Node node, int indent) {
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			// most likely a comment or text node
			System.out.println(node.getNodeName() + ", type="
					+ node.getNodeType() + ", value=" + node.getNodeValue());
			return "";
		}
		Element element = (Element) node;
		String indentString = "\t\t\t\t\t\t\t\t\t".substring(0, indent);
		StringWriter outputWriter = new StringWriter();
		NamedNodeMap attributes = element.getAttributes();
		outputWriter.write(indentString);
		outputWriter.write("<" + element.getTagName());
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				outputWriter.write(" " + ((Attr) attributes.item(i)).getName()
						+ "=" + ((Attr) attributes.item(i)).getValue());
			}
		}
		outputWriter.write("/>\n");
		NodeList children = element.getChildNodes();
		for (int child = 0; child < children.getLength(); child++) {
			outputWriter.write(dumpDOM(children.item(child), indent + 1));
		}
		return outputWriter.toString();
	}

	// return a Map of LSID/name dependencies for this model
	public Map getLsidDependencies() {
		HashMap hmDependencies = new HashMap();
		for (Iterator itSubTasks = getTasks().iterator(); itSubTasks.hasNext();) {
			JobSubmission js = (JobSubmission) itSubTasks.next();
			hmDependencies.put(js.getLSID(), js.getName());
		}
		return hmDependencies;
	}

	public String toString() {
		JobSubmission task;
		ParameterInfo p;
		HashMap attributes;
		StringBuffer out = new StringBuffer();
		int taskNum = 0;
		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
			task = (JobSubmission) eTasks.nextElement();
			out.append("name=" + task.getName() + "\n");
			out.append("taskNum=" + taskNum + "\n");
			out.append("LSID=" + task.getLSID() + "\n");
			out.append("description=" + task.getDescription() + "\n");
			for (Enumeration eParams = task.getParameters().elements(); eParams
					.hasMoreElements();) {
				p = (ParameterInfo) eParams.nextElement();
				out.append(p.toString() + "\n");
			}
		}
		return out.toString();
	}
}

/*
 * class PrivateClassLoader extends ClassLoader { }
 */
