<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.data.pipeline.*,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.util.LSID,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.webservice.TaskInfo"
	session="false" contentType="text/html" language="Java" buffer="100kb" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String userID = GenePatternAnalysisTask.getUserID(request, response); // will force login if necessary
	if (userID == null) return; // come back after login
	if (request.getParameter(GenePatternAnalysisTask.NAME) != null && !request.getParameter(GenePatternAnalysisTask.NAME).equals("") && !LSIDManager.getInstance().getAuthorityType(new LSID(request.getParameter(GenePatternAnalysisTask.NAME))).equals(LSIDUtil.AUTHORITY_MINE)) {
		response.sendRedirect("viewPipeline.jsp?" + GenePatternAnalysisTask.NAME + "=" + request.getParameter(GenePatternAnalysisTask.NAME));
	}
%>
<!doctype HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link href="favicon.ico" rel="shortcut icon">

<style>
.shadow { border-style: none; font-style: italic; font-size: 9pt; background-color: transparent ;}
</style>

<script language="Javascript">
var ie4 = (document.all) ? true : false;
var ns4 = (document.layers) ? true : false;
var ns6 = (document.getElementById && !document.all) ? true : false;
var numTasks = 0;
var NOT_SET = "[task not yet selected]";
var stopLoading = false;
var MAX_TASK_DESCRIPTION_LENGTH = 70;
var myAuthority = '<%= LSIDManager.getInstance().getAuthority() %>';
var broadAuthority = 'broad.mit.edu';

function scriptError(message, url, lineNumber) {
	if (stopLoading) return true;
	alert('Error in pipelineDesigner script at line ' + lineNumber + ': ' + message);
	return true;
}
function hideLayer(lay) {
	if (ie4) {document.all['id'+lay].style.visibility = "hidden";}
	if (ns4) {document.layers[lay].visibility = "hide";}
	if (ns6) {document.getElementById([lay]).style.display = "none";}
}

function showLayer(lay) {
	if (ie4) {document.all['id'+lay].style.visibility = "visible";}
	if (ns4) {document.layers[lay].visibility = "show";}
	if (ns6) {document.getElementById([lay]).style.display = "block";}
}

function writeToLayer(lay,txt) {
	divs[lay] = txt;
	if (ie4) {
		document.all("id" + lay).innerHTML = txt;
	}
	if (ns4) {
		var l = document['id'+lay];
		l.document.write(txt);
		l.document.close();
	}
	if (ns6) {
		over = document.getElementById([lay]);
		range = document.createRange();
		range.setStartBefore(over);
		domfrag = range.createContextualFragment(txt);
		while (over.hasChildNodes()) {
			over.removeChild(over.lastChild);
		}
		over.appendChild(domfrag);
   }
}
function readLayer(lay) {
   return divs[lay];
}

function setTask(taskNum, taskLSID) {
	writeToLayer(taskNum, changeTaskHTML(taskLSID, taskNum, true));
	showLayer(taskNum);
}

// insert a new task at the requested slot, moving all subsequent tasks down by one
function addAnother(taskNum, scrollTo) {

	// if the maximum number of tasks is already in the list, then ask whether the user intends to push the last one off the end
	if (numTasks == MAX_TASKS) {
		if (!confirm("There is a limit of " + numTasks + " tasks per pipeline.  Adding another task will force the " + numTasks +
			     "th to be deleted.  Press OK to delete '" + MAX_TASKS + ". " + 
			     document.forms["pipeline"]['t' + (MAX_TASKS-1) + '_taskName'].value +
			     "' and insert another.")) {
			// user cancelled the add, just return
			return;
		}
		// user confirmed the delete, do it
		deleteTask(MAX_TASKS-1);
	}

	// create task again, at the new location, and copy all of the parameters with their old values
	for (oldTaskNum = numTasks-1; oldTaskNum >= taskNum; oldTaskNum--) {
		var newTaskNum = (oldTaskNum+1);
	   	var taskName = document.forms["pipeline"]['t' + oldTaskNum + '_taskName'].value;
	   	var taskLSID = document.forms["pipeline"]['t' + oldTaskNum + '_taskLSID'].value;
		if (taskName == NOT_SET) {
			writeToLayer(newTaskNum, newTaskHTML(newTaskNum));
			continue;
		}
	   	var task = TaskInfos[taskLSID];
		writeToLayer(newTaskNum, changeTaskHTML(taskLSID, newTaskNum, true));
	   	for (param in task.parameterInfoArray) {
			var pi = task.parameterInfoArray[param];
			if (pi.isInputFile) {
				if (document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value != '') {
					setParameter(newTaskNum, 'shadow' + param, document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value);
				}
				if (oldTaskNum == 0) continue; // first task can't inherit from anywhere
				var inheritsFromTaskNum = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex - 1;
				if (inheritsFromTaskNum < 0) continue;
				if (inheritsFromTaskNum >= taskNum) {
					inheritsFromTaskNum++;
				} else {
				}
				if (oldTaskNum > 0) {
					setFileInheritance(newTaskNum, param, inheritsFromTaskNum, getSelectorValues(document.forms["pipeline"]['t' + oldTaskNum + '_if_' + param]));
				}
				else { 
					//alert('not setting file inheritance');
				} 
	   		} else {
				setParameter(newTaskNum, pi.name, document.forms["pipeline"]['t' + oldTaskNum + '_' + pi.name].value);
	   		} 
	   	}
	}
	var newTask = newTaskHTML(taskNum);
	writeToLayer(taskNum, newTask);
	showLayer(taskNum);

	// adjust inheritance task numbers to delete any tasks which are NOT_SET yet
	for (i = taskNum+1; numTasks > 0 && i <= numTasks; i++) {
		if (taskNum >= numTasks) break;
	   	var taskName = document.forms["pipeline"]['t' + i + '_taskName'].value;
	   	var taskLSID = document.forms["pipeline"]['t' + i + '_taskLSID'].value;
		if (taskName == NOT_SET) {
			continue;
		}
	   	var task = TaskInfos[taskLSID];
	   	for (param in task.parameterInfoArray) {
			var pi = task.parameterInfoArray[param];
			if (pi.isInputFile) {
				var selector = document.forms["pipeline"]['t' + i + '_i_' + param];
				var selected = selector.options[selector.selectedIndex].value;
				selector.length = 0;
				selector.options[0] = new Option('Choose task', NOT_SET);
				selector.options[0].disabled = true;
				for (t = 0; t < i; t++) {
					if (t != taskNum) {
						//alert((t+1) + '.  ' + document.forms["pipeline"]['t' + t + '_taskName'].value + '=' + t + ', selected=' + (t == selected));
						selector.options[selector.options.length] = new Option((t+1) + '.  ' + document.forms["pipeline"]['t' + t + '_taskName'].value, t, (t == selected), (t == selected));
					}
				}
	   		} 
	   	}
	}
	numTasks++;
	if (scrollTo) window.location.hash = taskNum; // scroll to the new task
}

function deleteTask(taskNum) {

	var deletedTaskName = document.forms["pipeline"]['t' + taskNum + '_taskName'].value;
	var warnings = "";

	// pull each task up one
	for (oldTaskNum = taskNum+1; oldTaskNum < numTasks; oldTaskNum++) {
		var newTaskNum = (oldTaskNum-1);
	   	var taskName = document.forms["pipeline"]['t' + oldTaskNum + '_taskName'].value;
	   	var taskLSID = document.forms["pipeline"]['t' + oldTaskNum + '_taskLSID'].value;
		if (taskName == NOT_SET) {
			writeToLayer(newTaskNum, newTaskHTML(newTaskNum));
			continue;
		}
	   	var task = TaskInfos[taskLSID];
		writeToLayer(newTaskNum, changeTaskHTML(taskLSID, newTaskNum, true));

		// adjust inheritance task numbers
	   	for (param in task.parameterInfoArray) {
			var pi = task.parameterInfoArray[param];
			if (pi.isInputFile) {
				if (document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value != '') {
					setParameter(newTaskNum, 'shadow' + param, document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value);
				}

				// if deleting the first task, there won't be an inheritor but the second task may have a loss of inheritance issue
				if (newTaskNum == 0 && document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex > 0) {
					var inheritsFromTaskNum = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].options[document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex].value;
					if (inheritsFromTaskNum == taskNum) {
						warnings = warnings + (newTaskNum+1) + '. ' + taskName + ' has lost its inherited input for ' + 
							   pi.name + '\n';
					}
				}

				if (newTaskNum > 0 && oldTaskNum > 0 && document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex > 0) {
					var selectIndex = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex;
					var inheritsFromTaskNum = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].options[selectIndex].value;

					if (selectIndex == 0 || inheritsFromTaskNum == "" || inheritsFromTaskNum == NOT_SET) {
						continue; // not inheriting
					}
					if (inheritsFromTaskNum > taskNum) {
						inheritsFromTaskNum = inheritsFromTaskNum-1;
					} else if (inheritsFromTaskNum == taskNum) {
						if (document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex > 0) {
							warnings = warnings + (newTaskNum+1) + '. ' + taskName + ' has lost its inherited input for ' + 
								   pi.name + '\n';
							continue; // don't set inheritance if it inherited from the deleted task
						}
					}

					// adjust inheritance task numbers
					var selector = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param];
					var selected = selector.options[selector.selectedIndex].value;
					selector.length = 0;
					selector.options[0] = new Option('Choose task', NOT_SET);
					selector.options[0].disabled = true;
					for (t = 0; t < newTaskNum; t++) {
					   	var tName = document.forms["pipeline"]['t' + t + '_taskName'].value;
						if (tName == NOT_SET) {
							continue;
						}
						if (t != taskNum) {
							selector.options[selector.options.length] = new Option((t+1) + '.  ' + 
								document.forms["pipeline"]['t' + t + '_taskName'].value, t,
								(t == selected), (t == selected));
						} else {
//							alert('deleteTask: ' + (t+1) + '. ' + taskName + ' has lost its inherited input for ' + pi.name + ' from ' + (taskNum+1) + '. ' + deletedTaskName);
						}
					}
					setFileInheritance(newTaskNum, param, inheritsFromTaskNum, getSelectorValues(document.forms["pipeline"]['t' + oldTaskNum + '_if_' + param]));
				}
	   		} else {
				setParameter(newTaskNum, pi.name, document.forms["pipeline"]['t' + oldTaskNum + '_' + pi.name].value);
	   		} 
	   	}
	}
	numTasks--;
	writeToLayer(numTasks, "");

	if (numTasks == 0) {
		addAnother(0);
	}
	if (warnings.length > 0) {
		alert('Warning:\n\n' + warnings);
	}
}

pipelineInstruction = 'Pipeline names are composed of letters, digits, and period, and must start with a letter.';

function isRSafe(varName) {
	var invalidCharacters = '[^a-zA-Z0-9\.]';
	var reservedNames = new Array('if', 'else', 'repeat', 'while', 'function', 'for', 'in', 'next', 'break', 'true', 'false', 'null', 'na', 'inf', 'nan');
	var ret =   varName.length > 0 && 				// the name is not empty
		    varName.search(invalidCharacters) == -1 && 		// it consists of only letters, digits, and periods
		    varName.charAt(0).search('[0-9\.]') == -1 &&	// it doesn't begin with a digit or period
		    reservedNames[varName.toLowerCase()] == null;	// it isn't a reserved name
	return ret;
}

function chooseInheritTask(taskNum, param) {
	var frm = document.forms['pipeline'];
	frm['t' + taskNum + '_shadow' + param].value="";
	var ctl = frm['t' + taskNum + '_i_' + param];
	var inheritFromTaskNum = ctl.options[ctl.options.selectedIndex].value;
	var lsid = frm['t' + inheritFromTaskNum + '_taskLSID'].value;
	var ti = TaskInfos[lsid];
	var fileFormats = ti.fileFormats;

	frm['t' + taskNum + '_prompt_' + param].checked = false;
	
	ctl = frm['t' + taskNum + '_if_' + param];
	// clear any previous entries in the file number selector
	ctl.options.length = 0;

	// semantic knowledge first!
	ctl.options[ctl.options.length]  = new Option('Choose output file', NOT_SET);
	ctl.options[ctl.options.length-1].disabled = true;
	
	for (f = 0; f < fileFormats.length; f++) {
		var ff = fileFormats[f];
		ctl.options[ctl.options.length]  = new Option(ff, ff);
	}
	if (fileFormats.length > 0) {
		ctl.options[ctl.options.length]  = new Option('', NOT_SET);
		ctl.options[ctl.options.length-1].disabled = true;
	}

	ctl.options[ctl.options.length]  = new Option('1st output', '1');
	ctl.options[ctl.options.length]  = new Option('stdout', 'stdout');
	ctl.options[ctl.options.length]  = new Option('stderr', 'stderr');
	ctl.options[ctl.options.length]  = new Option('2nd output', '2');
	ctl.options[ctl.options.length]  = new Option('3rd output', '3');
	ctl.options[ctl.options.length]  = new Option('4th output', '4');
}

function promptOnRunChecked(checkbox, taskNum, param, paramName) {
	var frm = checkbox.form;
	if (checkbox.checked) {
		
		if (paramName != '') {
			ctl = frm['t' + taskNum + '_' + paramName];
			if (ctl.type == 'text') {
				ctl.value = '';
			} else if (ctl.type == 'select-one') {
				for (option in ctl.options) {
					ctl.options[option].selected = ctl.options[option].defaultSelected;
				}
			} else {
				window.alert('promptOnRunChecked: unexpected field type: ' + ctl.type);
			}
		} else {
			// clear the input filename field
			// can't clear the input file because it is illegal under Javascript
			// clear the shadow of the input file
			frm['t' + taskNum + '_shadow' + param].value='';

			if (taskNum > 0) {
	
				// clear the two inheritance selectors
				frm['t' + taskNum + '_i_' + param].selectedIndex=0;
				frm['t' + taskNum + '_if_' + param].selectedIndex=0;
			}
		}
	} else {
		// unchecked the box
		if (paramName != '') {
			ctl = frm['t' + taskNum + '_' + paramName];
			if (ctl.type == 'text') {
				ctl.value = ctl.defaultValue;
			} else if (ctl.type == 'select-one') {
				for (option in ctl.options) {
					ctl.options[option].selected = ctl.options[option].defaultSelected;
				}
			} else {
				window.alert('promptOnRunChecked: unexpected field type: ' + ctl.type);
			}
		} else {
			// nothing to do for file input fields
		}
	}
}

function inverseOrder(a, b) {
	return (a > b ? a : b);
}

function TaskType(taskType) {
	this.taskType = taskType;
}

function TaskInfo(name, description, lsid, taskType, parameterInfoArray, docs, fileFormats, domains) {
	this.name = name;
	this.description = description;
	this.lsid = lsid;
	this.lsidVersion = new LSID(lsid).version
	this.lsidNoVersion = LSIDNoVersion(lsid);
	this.taskType = taskType;
	this.parameterInfoArray = parameterInfoArray;
	this.docs = docs;
	this.fileFormats = fileFormats;
	this.domains = domains;
}

function LSIDNoVersion(lsid) {
	return lsid.split('<%= LSID.DELIMITER %>').slice(0,-1).join('<%= LSID.DELIMITER %>')
}

function ParameterInfo(name, description, value, isInputFile, isOutputFile, defaultValue, isOptional, fileFormats, domains) {
	this.name = name;
	this.description = description;
	this.value = value;
	this.isInputFile = isInputFile;
	this.isOutputFile = isOutputFile;
	this.defaultValue = defaultValue;
	this.isOptional = isOptional;
	this.fileFormats = fileFormats;
	this.domains = domains;
}

function LSID(lsid) {
	this.lsid = lsid;
	var tokens = lsid.split('<%= LSID.DELIMITER %>');
	this.authority = tokens[2];
	this.namespace = tokens[3];
	this.identifier = tokens[4];
	this.version = tokens[5];
	this.authorityType = (this.authority == '<%= LSIDManager.getInstance().getAuthority() %>') ? 'mine' : (this.authority == '<%= LSIDUtil.BROAD_AUTHORITY %>' ? 'broad' : 'foreign');
}

// sort array of LSIDs alphabetically by name, then inverse by LSID (authority/namespace/identifier/version)
function sortTaskTypesByName(lsid1, lsid2) {
		var task1 = TaskInfos[lsid1];
		var task2 = TaskInfos[lsid2];
		if (task1.name.toLowerCase() != task2.name.toLowerCase()) {
			return (task1.name.toLowerCase() < task2.name.toLowerCase() ? -1 : 1);
		}
		return (lsid1 > lsid2 ? -1 : 1);
}

function changeTaskType(selectorTaskType, taskNum) {
	var taskSelector = selectorTaskType.form['t' + taskNum];
	taskSelector.options.length = 0;
	if (selectorTaskType.selectedIndex == 0) return; // user chose the "choose task" heading, item 0
	var type = selectorTaskType.options[selectorTaskType.selectedIndex].value;
	taskSelector.options[0] = new Option("Choose a " + selectorTaskType.options[selectorTaskType.selectedIndex].text + " task", "");
	taskSelector.options[0].style['fontWeight'] = "bold";
	var versionlessLSIDs = new Array();
 	for (i in TaskTypes[type]) {
		taskLSID = new LSID(TaskTypes[type][i]);
		var taskLSIDnoVersion = taskLSID.authority + '<%= LSID.DELIMITER %>' + taskLSID.namespace + '<%= LSID.DELIMITER %>' + taskLSID.identifier;
		if (versionlessLSIDs[taskLSIDnoVersion] != null) {
			continue; // already has it
		}
		versionlessLSIDs[taskLSIDnoVersion] = taskLSIDnoVersion;
		task = TaskInfos[taskLSID.lsid];
		var optionText = task.name;
		if (task.description != "") optionText = optionText + ' - ' + task.description
		if (optionText.length > MAX_TASK_DESCRIPTION_LENGTH) {
			optionText = optionText.substring(0,MAX_TASK_DESCRIPTION_LENGTH) + "..."
		}
		if (taskLSID.authority != broadAuthority && taskLSID.authority != myAuthority) {
			optionText = optionText + " (" + taskLSID.authority + ")";
		}
		taskSelector.options[taskSelector.options.length] = new Option(optionText, task.lsid);
		var t = taskSelector.options.length-1;
		taskSelector.options[t].setAttribute('class', "tasks-" + taskLSID.authorityType);
	}
}

function changeFile(ctl, taskNum, param) {
	var frm = ctl.form;
	frm['t' + taskNum + '_shadow' + param].value = ctl.value;
	if (ctl.value != '') {
		frm['t' + taskNum + '_prompt_' + param].checked = false;
		if (taskNum > 0) {
			frm['t' + taskNum + '_i_' + param].selectedIndex = 0;
			frm['t' + taskNum + '_if_' + param].options.length = 0;
		}
	}
}

function blurFile(ctl, taskNum, param) {
	changeFile(ctl, taskNum, param);
}

function dropFile(ctl, taskNum, param) {
	changeFile(ctl, taskNum, param);
}

function changeTaskInheritFile(ctl, taskNum, param, name) {
	var frm = ctl.form;
	frm['t' + taskNum + '_shadow' + param].value = "";
	frm['t' + taskNum + '_prompt_' + param].checked = false;
}

function changeChoice(ctl, taskNum, param) {
	ctl.form['t' + taskNum + "_prompt_" + param].checked = false;
}

function chgLSIDVer(oldTaskNum, selector) {
	var taskLSID = selector.options[selector.selectedIndex].value;
	var newTaskNum = numTasks;
	addAnother(newTaskNum);

	// preserve old settings within this task, where possible

   	var taskName = document.forms["pipeline"]['t' + oldTaskNum + '_taskName'].value;
   	var oldLSID = document.forms["pipeline"]['t' + oldTaskNum + '_taskLSID'].value;
   	var task = TaskInfos[taskLSID];
	var oldTask = TaskInfos[oldLSID];

	writeToLayer(newTaskNum, changeTaskHTML(taskLSID, newTaskNum, false));

//var whatsInOld = "";
//for (param in TaskInfos[oldLSID].parameterInfoArray) {
//	var pi = task.parameterInfoArray[param];
//	if (pi.isInputFile) {
//		var p = document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param];
//		whatsInOld = whatsInOld + p.name + "=" + p.value + "\n";
//	} else {
//		var p = document.forms["pipeline"]['t' + oldTaskNum + '_' + pi.name];
//		whatsInOld = whatsInOld + p.name + "=" + p.value + "\n";
//	}
//}
//alert('whatsInOld ' + oldTaskNum + ': ' + whatsInOld);

// XXX: copy runtime prompt settings too

   	for (param in task.parameterInfoArray) {
		var pi = task.parameterInfoArray[param];

		// cannot assume that all old variables are present in new task
		if (document.forms["pipeline"]['t' + oldTaskNum + '_' + pi.name]==null) {
			alert(pi.name + ' is in the new task but not the old one');
			continue;
		}

		if (pi.isInputFile) {
			if (document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value != '') {
				setParameter(newTaskNum, 'shadow' + param, document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value);
			}
			if (oldTaskNum == 0) continue; // first task can't inherit from anywhere
			var inheritsFromTaskNum = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex - 1;
			if (inheritsFromTaskNum < 0) continue;
			if (inheritsFromTaskNum >= newTaskNum) {
				inheritsFromTaskNum++;
			} else {
			}
			if (oldTaskNum > 0) {
				setFileInheritance(newTaskNum, param, inheritsFromTaskNum, getSelectorValues(document.forms["pipeline"]['t' + oldTaskNum + '_if_' + param]));
			}
			else { 
				//alert('not setting file inheritance');
			} 
   		} else {
			setParameter(newTaskNum, pi.name, document.forms["pipeline"]['t' + oldTaskNum + '_' + pi.name].value);
   		} 
   	}
	// all of the old task is copied to the new one

	writeToLayer(oldTaskNum, changeTaskHTML(taskLSID, oldTaskNum, false));

	// copy all of the parameters again
	var t = oldTaskNum;
	oldTaskNum = newTaskNum;
	newTaskNum = t;

   	for (param in task.parameterInfoArray) {
		var pi = task.parameterInfoArray[param];
		if (pi.isInputFile) {
			if (document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value != '') {
				setParameter(newTaskNum, 'shadow' + param, document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + param].value);
			}
			if (oldTaskNum == 0) continue; // first task can't inherit from anywhere
			var inheritsFromTaskNum = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex - 1;
			if (inheritsFromTaskNum < 0) continue;
			if (inheritsFromTaskNum >= newTaskNum) {
				inheritsFromTaskNum++;
			} else {
			}
			if (oldTaskNum > 0) {
				setFileInheritance(newTaskNum, param, inheritsFromTaskNum, getSelectorValues(document.forms["pipeline"]['t' + oldTaskNum + '_if_' + param]));
			}
			else { 
				//alert('not setting file inheritance');
			} 
   		} else {
			setParameter(newTaskNum, pi.name, document.forms["pipeline"]['t' + oldTaskNum + '_' + pi.name].value);
   		} 
   	}
	t = oldTaskNum;
	oldTaskNum = newTaskNum;
	newTaskNum = t;

	deleteTask(newTaskNum);
}

function changeTaskHTML(taskLSID, taskNum, bUpdateInheritance) {
	var origTaskLSID = taskLSID;
	var taskFields = "";

	var task = TaskInfos[taskLSID];
	if (task == null) {
		alert('no such task \"' + taskLSID + '\".  Aborting pipeline loading.');
		stopLoading = true;
		window.stop();	
		return ('<hr>no such task \"' + taskLSID + '\".  Aborting pipeline loading.');
	}
	taskFields = taskFields + '<a name="t' + taskNum + '">\n';
	taskFields = taskFields + '<input type="hidden" name="t' + taskNum + '_taskName" value="' + task.name + '">';
	taskFields = taskFields + '<hr>\n';
	taskFields = taskFields + '<table><tr><td valign="top"><font size="+1"><b>' + (taskNum+1) + '.&nbsp;<a name="' + task.name + '" href="addTask.jsp?name=' + task.lsid + '" target="_new">' + task.name + '</a>:</b></font>&nbsp;</td><td valign="top">' + task.description + "</td></tr></table>";
	taskFields = taskFields + '<table cols="3" valign="top" width="100%"><col align="center" width="10%"><col align="right" width="10%"><col align="left" width="*">';
	taskFields = taskFields + '<tr><td>&nbsp;</td><td align="right"><b>LSID:</b></td><td>\n';
	taskFields = taskFields + '<input type="hidden" name="t' + taskNum + '_taskLSID" value="' + task.lsid + '" size="80">' + task.lsid + '<br>';
	taskFields = taskFields + '</td></tr>\n';

	// create selector showing all versions of this task (same authority/namespace/identifier)
	var latestVersion = new LSID(task.lsid).version
	taskFields = taskFields + '<tr><td>&nbsp;</td><td align="right"><b>LSID version:</b></td><td>\n';
	taskFields = taskFields + '<select name="t' + taskNum + '_taskLSIDv" onchange="chgLSIDVer(' + taskNum + ', this)">\n';
	var wildcard = task.lsid.substring(0, task.lsid.length - latestVersion.length);
	var sameTasks = new Array();
	for (t in TaskInfos) {
		if (TaskInfos[t].lsid.indexOf(wildcard) == 0) {
			sameTasks = sameTasks.concat(new LSID(TaskInfos[t].lsid).version)
		}
	}
	for (t in sameTasks) {
		taskFields = taskFields + '<option value="' + wildcard + sameTasks[t] + '"' + 
			     (sameTasks[t] == latestVersion ? ' selected' : '') +
			     '>' + sameTasks[t] + (t == 0 ? " (latest)" : "") + '</option>\n';
	}
	taskFields = taskFields + '</select>\n';
	taskFields = taskFields + '</td></tr>\n';

	if (task.docs.length > 0 || task.taskType == PIPELINE) {
		taskFields = taskFields + '<tr><td>&nbsp;</td><td align="right"><b>documentation:</b> </td><td>\n';
		for (doc in task.docs) {
			if (task.docs[doc] == "version.txt") continue;
			taskFields = taskFields + '<a href="getTaskDoc.jsp?name=' + task.lsid + '&file=' + task.docs[doc] + '" target="_new">' + task.docs[doc] + '</a> ';
		}
  		if (task.taskType == PIPELINE) {
  			taskFields = taskFields + '<a href="runPipeline.jsp?name=' + task.lsid + '&description=' + task.description + '&cmd=edit" target="_new">edit pipeline code</a>';
	  	}
		taskFields = taskFields + '</td></tr>\n';
	}
	if (task.parameterInfoArray.length > 0) {
		taskFields = taskFields + '<tr><td align="center" valign="bottom"><b>prompt<br>when run</b></td><td align="right" valign="bottom" width="10%"><b>parameter name</b></td><td valign="bottom"><b>value</b></td></tr>';
	} else {
		taskFields = taskFields + '<tr><td></td><td colspan="2"><i>' + task.name + ' has no input parameters</i>';
	}
	for (param in task.parameterInfoArray) {
		var pi = task.parameterInfoArray[param];
		var choices = ((pi.value != null && pi.value != 'null' && pi.value.length > 0) ? pi.value.split(";") : "");

		// prompt-when-run
		taskFields = taskFields + '<tr><td align="center" valign="top">\n';
		taskFields = taskFields + '<input type="checkbox" name="t' + taskNum + '_prompt_' + param + '"';
//		taskFields = taskFields + '\n onchange=\"promptOnRunChecked(this, ' + taskNum + ', ' + param + ', \'' + (pi.isInputFile ? "" : pi.name) + '\');\"';
		taskFields = taskFields + '\n  onclick=\"promptOnRunChecked(this, ' + taskNum + ', ' + param + ', \'' + (pi.isInputFile ? "" : pi.name) + '\');\"';
		taskFields = taskFields + '></td>\n';

		// label for the field
		taskFields = taskFields + '<td align="right" width="10%" valign="top"><nobr>' + pi.name.replace(/\./g,' ').replace(/\_/g,' ') + ':</nobr></td>\n';

		// input area for the field
		taskFields = taskFields + '	<td valign="top">';
		if (pi.isInputFile) {
			taskFields = taskFields + '<input type="file" name="t' + taskNum + '_' + pi.name + 
						  '" size="60" ' +
						  'onchange="changeFile(this, ' + taskNum + ', ' + param + ')" ' + 
						  'onblur="blurFile(this, ' + taskNum + ', ' + param + ')" ' +
						  'ondrop="dropFile(this, ' + taskNum + ', ' + param + ')" ' +
						  'class="little">';
			taskFields = taskFields + '<br><input name="t' + taskNum + '_shadow' + param + 
				     '" type="text" readonly size="130" tabindex="-1" class="shadow">';
			if (taskNum > 0) {
				taskFields = taskFields + '<br><table><tr><td valign="top"><nobr>or use output from <select name="t' + taskNum + '_i_' + param + 
							  '" onchange="chooseInheritTask(' + taskNum + ', ' + param + ')"><option value=' + NOT_SET + '" disabled>Choose task</option>\n';
				for (t = 0; t < taskNum; t++) {
					taskFields = taskFields + '<option value="' + t + '">' + (t+1) + '.  ' + 
						     document.forms['pipeline']['t' + t + '_taskName'].value + '</option>\n';
				}
				taskFields = taskFields + ' </select>\n';
				if (pi.description.length > 0 && pi.description != pi.name.replace(/\./g,' ').replace(/\_/g,' ')) taskFields = taskFields + '<br><br>' + pi.description;
				taskFields = taskFields + ' </td><td valign="top">';
				taskFields = taskFields + '<select name="t' + taskNum + '_if_' + param + '"' +
					' onChange="changeTaskInheritFile(this, ' + taskNum + ', ' + param + ', \'' + pi.name + '\')" multiple size="6">\n';
				// this selector will be filled in dynamically by chooseInheritTask when the task is selected
				taskFields = taskFields + ' </select></nobr></td></td></table>\n';
			}
		} else if (pi.isOutputFile) {
			// should never happen
		} else if (choices.length < 2) {
			taskFields = taskFields + "<table><tr><td valign='top'><input name='t" + taskNum + "_" + pi.name + "' value='" + pi.defaultValue + "'> &nbsp;</td>";
			if (pi.description.length > 0) taskFields = taskFields + '<td valign="top">' + pi.description + '</td>';
			taskFields = taskFields + '</tr></table>';
		} else {
			taskFields = taskFields + "	<select name='t" + taskNum + "_" + pi.name + 
					"' onchange=\"changeChoice(this, taskNum, param)\">\n";
			for (i in choices) {
				var c = choices[i].split('=');
				if (c.length == 1) c = new Array(c, c);
				taskFields = taskFields + '		<option value="' + c[0] + '"' + (pi.defaultValue == c[0] || pi.defaultValue == c[1] ? ' selected' : '') + '>' + c[1] + '</option>\n';
			}
			taskFields = taskFields + "	</select>\n";
			if (pi.description.length > 0) {
				taskFields = taskFields + ' &nbsp;' + pi.description;
			}
		}
		taskFields = taskFields + "</td></tr>\n";
	}
	taskFields = taskFields + '</table>\n';
	taskFields = taskFields + '<br><center>\n';
	if ((taskNum+1) < MAX_TASKS) {
		taskFields = taskFields + '<input type="button" value="add another task" onClick="addAnother(' + (taskNum+1) + 
				          ', true)" name="notused" class="little">&nbsp;&nbsp;\n';
	}
	taskFields = taskFields + '<input type="button" value="delete ' + task.name + '" onClick="deleteTask(' + taskNum + 
				  ')" name="notused" class="little">\n</center>';

	if (bUpdateInheritance) {
		// update the inheritors for any subsequent tasks
		for (i = taskNum+1; numTasks > 1 && i < numTasks; i++) {
			if ((taskNum+1) == numTasks) break;
			var tName = document.forms['pipeline']['t' + i + '_taskName'].value;
			if (tName == NOT_SET) continue;
			var tLSID = document.forms['pipeline']['t' + i + '_taskLSID'].value;
		   	var task = TaskInfos[tLSID];
		   	for (param in task.parameterInfoArray) {
				var pi = task.parameterInfoArray[param];
				if (pi.isInputFile) {
					var selector = document.forms['pipeline']['t' + i + '_i_' + param];
					var selected = selector.options[selector.selectedIndex].value;
					if (selector.selectedIndex == 0) selected = -1; // ensure no match against task 0
					selector.length = 0;
					selector.options[0] = new Option('Choose task', NOT_SET);
					selector.options[0].disabled = true;
					for (t = 0; t < i; t++) {
						if (t != taskNum) {
							selector.options[selector.options.length] = new Option((t+1) + '.  ' + document.forms['pipeline']['t' + t + '_taskName'].value, t, (t == selected), (t == selected));
						} else {
							selector.options[selector.options.length] = new Option((t+1) + '.  ' + TaskInfos[taskLSID].name, t, (t == selected), (t == selected));
						}
					}
		   		} 
		   	}
		}
	}
	return taskFields;
}

function chgTask(selectorTask, taskNum) {
	var taskLSID = selectorTask.options[selectorTask.selectedIndex].value;
	var taskFields = changeTaskHTML(taskLSID, taskNum, true);
	writeToLayer(taskNum, taskFields);
	showLayer(taskNum);
}

function setSelector(selector, findValue) {
	if (stopLoading) return;
	var taskNum;
	var taskName;
	if ( selector.charAt(0) == 't' && selector.indexOf("_") != -1) {
		taskNum = selector.substring(1, selector.indexOf("_"));
		taskName = document.forms['pipeline']['t' + taskNum + '_taskName'].value;
		if (taskName == NOT_SET) return;
	}
	var vals = '';
	var fld = document.forms['pipeline'][selector];
	if (fld == null) { 
		var paramName = selector.substring(selector.indexOf("_") + 1);
		alert(taskName + "'s " + paramName + " parameter does not exist in the current task definition!");
		return;
	}
	var numFound = 0;
	var multiple = fld.multiple;
	var findValues = new Array();
	if (multiple) {
		findValues = findValue.split("<%= GPConstants.PARAM_INFO_CHOICE_DELIMITER %>");
	} else {
		findValues[0] = findValue;
	}

	for (fv = 0; fv < findValues.length; fv++) {
		findValue = findValues[fv];
		var found = false;
		for (f = 0; f < fld.options.length; f++) {
			if (fld.options[f].value != "" && fld.options[f].value == findValue) {
				fld.options[f].selected = true;
				numFound++;
				found = true;
				break;
			}
		}
		if (!found) vals = vals + "'" + fld.options[f].value + "'" + '=' + fld.options[f].text + '\n';
	}
	if (numFound == findValues.length) return;

	var i = fld.name.indexOf("_");
	var taskNum = fld.name.substring(1, i);
	var taskName = document.forms['pipeline']['t' + t + '_taskName'].value;
	var userFieldName = fld.name.substring(i+1);
	alert('setSelector: could not find ' + findValue + ' among ' + fld.options.length +
	      ' items in ' + taskName + ' (task ' + (parseInt(taskNum)+1) + ') field ' + 
	      userFieldName + '.  Values are:\n' + vals);
}

function setOption(selector, findValue) {
	var fld = document.forms['pipeline'][selector];
	if (fld == null) { alert("setOption: " + selector + " does not exist!");  return; }
	for (f = 0; f < fld.length; f++) {
		if (fld.options[f].value == findValue) {
			fld.options[f].selected = true;
			break;
		}
	}
}

function getSelectorValues(selector) {
	var delimiter = "<%= GPConstants.PARAM_INFO_CHOICE_DELIMITER %>";
	var vals = "";
	for (f = 0; f < selector.options.length; f++) {
		if (selector.options[f].selected) {
			if (vals.length > 0) {
				val = vals + delimiter;
			}
			vals = vals + selector.options[f].value
		}
	}
	return vals;
}

function setField(field, value) {
	if (field == null) return;
	var fld = document.forms['pipeline'][field];
	if (fld == null) { alert("setField: " + field + " does not exist!");  return; }
	fld.value=value;
}

function setCheckbox(field, value) {
	if (field == null) return;
	var fld = document.forms['pipeline'][field];
	if (fld.type == "select-one") return setSelector(field, value);
	if (fld == null) { alert("setCheckbox: " + field + " does not exist!");  return; }
	fld.checked = value;
}

function setTaskName(taskNum, taskName, taskLSID) {
	var task = null;
	if (taskLSID != null) task = TaskInfos[taskLSID];
	var found = (task != null);
	if (task == null) {
		// not found by LSID, search by name
		var requestedLSID = taskLSID;
		for (task in TaskInfos) {
			if (TaskInfos[task].name == taskLSID) {
				taskLSID = TaskInfos[task].lsid;
				found = true;
				break;
			}
		}
		if (!found) {
			// search for same LSID but different version number
  			var noVersion = LSIDNoVersion(requestedLSID);
			for (task in TaskInfos) {
				if (TaskInfos[task].lsidNoVersion == noVersion) {
					taskLSID = TaskInfos[task].lsid;
					found = true;
					break;
				}
			}
		}
		if (found) {
			//alert(taskName + " was not found with requested LSID " + taskLSID + "\nconsidering " + requestedLSID);
			found = window.confirm("Attempt to use latest version (" + new LSID(taskLSID).version + ") of " + taskName + ' for step ' + (taskNum+1) + ' instead of requested missing version (' + new LSID(requestedLSID).version + ')?')
		}
	}
	if (!found) {
		alert("Step " + (taskNum+1) + ": unable to locate " + taskName + '(' + taskLSID + ')');
		writeToLayer(taskNum, newTaskHTML(taskNum) + 
			(new LSID(requestedLSID).authority != myAuthority ?
			 ('<br><a href="taskCatalog.jsp?<%= GPConstants.LSID %>=' + requestedLSID + '">load ' + taskName + ' from Broad task catalog</a>') : ""));
		showLayer(taskNum);
		// XXX: there will be problems in subsequent stages that inherit files from this stage
		//stopLoading = true;
		//window.stop();
	} else {
		writeToLayer(taskNum, changeTaskHTML(taskLSID, taskNum, true));
		showLayer(taskNum);
		if (numTasks != 1 || taskNum != 0) numTasks++;
	}
}

// set an input file's inheritance (which task number, which file number) from a previous task
function setFileInheritance(thisTaskNum, thisParamNum, fromTaskNum, fromFileNum) {
	if (document.forms['pipeline']['t' + thisTaskNum + '_taskName'].value == NOT_SET) return;

	if (document.forms['pipeline']['t' + thisTaskNum + '_if_' + thisParamNum] == null) {
		if (stopLoading) return;
		alert("Parameter #" + (thisParamNum+1) + " in task #" + (thisTaskNum+1) + ", " + 
			document.forms['pipeline']['t' + thisTaskNum + '_taskName'].value + 
			", is no longer an input file.  This parameter will not inherit input from task #" + 
			(fromTaskNum+1) + ", " + document.forms['pipeline']['t' + fromTaskNum + '_taskName'].value + 
			", file #" + fromFileNum);
		return;
	}

	if (document.forms['pipeline']['t' + fromTaskNum + '_taskName'].value == NOT_SET) {
		if (stopLoading) return;

	   	var task = TaskInfos[document.forms["pipeline"]['t' + thisTaskNum + '_taskLSID'].value];
		var pi = task.parameterInfoArray[thisParamNum];

		alert("Warning: parameter #" + (thisParamNum+1) + " in task #" + (thisTaskNum+1) + ", " + 
			document.forms['pipeline']['t' + thisTaskNum + '_taskName'].value + 
			", inherits " + pi.name + " from task #" + 
			(fromTaskNum+1) + ", " + document.forms['pipeline']['t' + fromTaskNum + '_taskName'].value + 
			", file #" + fromFileNum + ", which is currently not defined.");
	}

	setSelector('t' + thisTaskNum + '_i_' + thisParamNum, fromTaskNum);
	chooseInheritTask(thisTaskNum, thisParamNum);
	setSelector('t' + thisTaskNum + '_if_' + thisParamNum, fromFileNum);
}

// set an input file's settings (currently just the inheritance information due to inability to restore file selection choice in HTML or Javascript)
function setFileSelection(thisTaskNum, thisParamNum, fromTaskNum, fromFileNum) {
	setFileInheritance(thisTaskNum, thisParamNum, fromTaskNum, fromFileNum);
}

// for a given task number, set a named parameter to a particular value
function setParameter(taskNum, paramName, paramValue) {
	if (paramName == null) return;
	if (document.forms['pipeline']['t' + taskNum + '_taskName'].value == NOT_SET) return;
	document.forms['pipeline']['t' + taskNum + '_' + paramName].value=paramValue;
}

function checkMatchingRadioValue(radioName, radioValue) {
}

// called to validate that the pipeline inputs are acceptable before submitting		
function savePipeline(bMustName, cmd) {
	// delete all stages that aren't defined yet
	for (i = numTasks-1; i >= 0; i--) {
		if (document.forms['pipeline']['t' + i + '_taskName'].value == NOT_SET) {
			var t = i;
			deleteTask(i);
		}
		t--;
		if (t < 0) break;
	}
	var success = true;
	if (bMustName) {
		success = (document.forms['pipeline'].pipeline_name.value.length > 0);
		if (!success) alert('You must enter a pipeline name');
		else {
			success = isRSafe(document.forms['pipeline'].pipeline_name.value);
			if (!success) alert(pipelineInstruction);
		}
	}

	// Netscape Navigator loses filename selections in file choosers when layers are modified
	// although we have shadowed them into a user-visible field, the user needs to paste them back
	// into the file chooser so that the files will be uploaded to the server upon submission

	var lostFiles = "";
	for (i=0; i < numTasks; i++) {
	   var taskLSID = document.forms['pipeline']['t' + i + '_taskLSID'].value;
	   var task = TaskInfos[taskLSID];
	   for (param in task.parameterInfoArray) {
		var pi = task.parameterInfoArray[param];
		if (pi.isInputFile) {
		   var fileChooserValue = document.forms['pipeline']['t' + i + '_' + pi.name];
		   var shadowValue = document.forms['pipeline']['t' + i + '_shadow' + param].value;
		   if (shadowValue != '' && fileChooserValue.value != shadowValue && shadowValue.indexOf('http://') != 0
		       && shadowValue.indexOf('ftp://') != 0 && shadowValue.indexOf('<GenePatternURL>') != 0) {
			lostFiles = lostFiles + (i+1) + '. ' + task.name + ': ' + pi.name + ' (was ' + shadowValue + ')\n';
			success = false;
		   }
	   	}
	   }
	}
	if (!success) {
/*
		// BUG: Netscape 7 doesn't seem to return a Window object from window.open!!!

		var w = window.open("",document.forms['pipeline']['pipeline_name'].value + '_error',"height=350,width=600,menuBar=no,resizable=yes,scrollbars=yes,status=no,directories=no", true);
		w.focus();
		w.document.writeln("<html><head><title>Lost names of input files</title></head><body>Please copy and paste the input filenames from below each file chooser box into the file chooser above each.<br><pre>" +
				   lostFiles + "</pre><br><a href=\"javascript:window.close()\">close window</a></body>");
*/
		alert("Please copy and paste the input filenames from below each file chooser box into the file chooser above each.\n\n" +
				   lostFiles);
	}
	if (numTasks == 0) {
		success = false;
		alert('No tasks defined in pipeline');
	}
	if (success) {
		document.forms['pipeline']['cmd'].value = cmd;
		<% if (request.getParameter("autoSave") != null) { %>
		document.forms['pipeline'].target = "";
		document.forms['pipeline']['autoSave'].value = "true";
		<% } %>
		document.forms['pipeline'].submit();
	}
	return success;
}

function deleteDocFiles() {
	var sel = document.forms['pipeline'].deleteFiles;
	var options = sel.options;
	var selection = "";
	if (options == null){
		selection = sel.value;
	} else {
		selection = sel.options[sel.selectedIndex].value;
	}
	if (selection == null || selection == "") return;
	if (window.confirm('Really delete ' + selection + ' from support files?\nThis will discard other pipeline changes since the last save.')) { 
		window.location='saveTask.jsp?deleteFiles=' + selection + '&<%= GPConstants.NAME %>=' + document.forms['pipeline'].pipeline_name.value + '&<%= GPConstants.LSID %>=' + document.forms['pipeline']['LSID'].value +'&forward=pipelineDesigner.jsp';
	}
}


function clonePipeline() {
	while (true) {
		var cloneName = window.prompt("Name for cloned pipeline", "copyOf" + document.forms['pipeline'].pipeline_name.value);
		
		// user cancelled?
		if (cloneName == null || cloneName.length == 0) {
			return false;
		}

		document.forms['pipeline']['cmd'].value = "clone";
		document.forms['pipeline']['cloneName'].value = cloneName;
		document.forms['pipeline'].target = "";
		document.forms['pipeline'].submit();
		return false;
	}
}

function deletePipeline() {
	var version = new LSID(document.forms['pipeline']['LSID'].value).version;
	if (!confirm('Really delete the ' + document.forms['pipeline'].pipeline_name.value + ' pipeline (version ' + version + ')?')) {
		return false;
	}
	document.forms['pipeline']['cmd'].value = "delete";
	document.forms['pipeline'].target = "";
	return true;
}

// sort array of suggested tasks for next stage in descending order by "natural" position in a microarray /*

function sortSuggested(task1, task2) {
	var taskTypes = new Array(
				"Preprocess & Utilities",
				"Sequence Analysis",
				"Annotation",
				"pipeline",
				"Clustering",
				"Gene List Selection",
				"Missing Value Imputation",
				"Prediction",
				"Projection",
				"Statistical Methods",
				"Image Creators",
				"Visualizer"
				).toString();

	// alert("comparing " + task1.name + " to " + task2.name);

	var t1 = taskTypes.indexOf(task1.taskType);
	var t2 = taskTypes.indexOf(task2.taskType);
	var ret = 0;
	if (t1 > t2) {
		ret = -1;
	} else if (t1 < t2) {
		ret = 1;
	} else if (task1.name < task2.name) {
		ret = 1;
	} else if (task1.name > task2.name) {
		ret = -1;
	} else {
		ret = 0;
	}
	return ret;
}

// when a new task is requested, generate the inital task type selection HTML
function newTaskHTML(taskNum) {
	var newTask = '';
	newTask = newTask + '<hr>\n';
	newTask = newTask + '<a name="' + (taskNum+1) + '">\n'; // anchor for each task
	newTask = newTask + '<table cols="3" width="1">\n';

	// build a list of tasks whose input file formats potentially match the output formats of tasks already in the pipeline

	// make a list of all possible output formats for the current stages of the pipeline
	var outputFormats = new Array();
	for (t = 0; t < taskNum; t++) {
	   	var lsid = document.forms["pipeline"]['t' + t + '_taskLSID'].value;
	   	var task = TaskInfos[lsid];
		var fileFormats = task.fileFormats;
		for (f = 0; f < fileFormats.length; f++) {
			var ff = fileFormats[f];
			if (outputFormats[ff] == undefined) {
				outputFormats[ff] = ff;
			}
		}
	}

	// now find all tasks which have input parameters that accept file formats from among those possibly output at this stage
	var suggestedTasks = new Array();
	var numSuggested = 0;
	for (t in TaskInfos) {
	   	var task = TaskInfos[t];
//if (task.name.indexOf("copyOf") == 0) alert("considering " + task.name + " which has " + task.parameterInfoArray.length + " parameters");
nextTask:
	   	for (param in task.parameterInfoArray) {
			var pi = task.parameterInfoArray[param];
			if (true || pi.isInputFile) {
				var fileFormats = pi.fileFormats;
//if (fileFormats.length > 0 || task.name.indexOf("copyOf") == 0) alert(task.name + "." + pi.name + " file formats=" + fileFormats);
				for (f = 0; f < fileFormats.length; f++) {
					var ff = fileFormats[f];
					// is this input file format one that might be generated by a previous task?
					if (outputFormats[ff] == undefined) {
						// no, ignore the task
						continue;
					}
//alert("adding " + task.name);
					suggestedTasks[task.lsid] = task;
					numSuggested++;

					break nextTask;
				}
			}
		}
	}

	// TODO: sort by something useful!
	//suggestedTasks.sort(sortSuggested);

	if (numSuggested > 0) {
		newTask = newTask + '<tr><td valign="top" colspan="3">';
		newTask = newTask + '<select onchange="chgTask(this, ' + taskNum + ')" size="' + (numSuggested+1) + '">\n';
		newTask = newTask + '<option value="' + NOT_SET + 
				    '" selected style="font-weight: bold">proto-semantic suggestions</option>\n';
	}
	for (t in suggestedTasks) {
		var task = suggestedTasks[t];
		newTask = newTask + '<option value="' + task.lsid + '" title="' + task.taskType + '">' + 
			  task.name + ' - ' + task.description + '</option>\n';
	}
	if (numSuggested > 0) newTask = newTask + '</select></td></tr>\n';

	newTask = newTask + '<tr><td valign="top">\n';
	newTask = newTask + '<select onchange="changeTaskType(this, ' + taskNum + ')" name="notused" size="' + (TaskTypesList.length+1) + '">\n';
	newTask = newTask + '<option value="" selected style="font-weight: bold">task types</option>\n';
	for (taskType in TaskTypesList) {
		var name = TaskTypesList[taskType];
		name = name.substring(0,1).toUpperCase() + name.substring(1);
		newTask = newTask + '<option value="' + TaskTypesList[taskType] + '">' + name + '</option>\n';
	}
	newTask = newTask + '</select></td>\n';
	newTask = newTask + '<td valign="top"><font size="+3">&#8594;</font></td>';
	newTask = newTask + '<td align="left" valign="top"><select name="t' + taskNum + '" onchange="chgTask(this, ' + taskNum + ')">\n';
	newTask = newTask + '</select></td></tr></table>\n';
	newTask = newTask + '<input type="hidden" name="t' + taskNum + '_taskName" value="' + NOT_SET + '">';
	if (taskNum > 0) {
		newTask = newTask + '<br><center><input type="button" value="delete" onClick="deleteTask(' + taskNum + 
				    ')" name="notused" class="little"></center>';
	}
	return newTask;
}

</script>
<%
      String userAgent = request.getHeader("User-Agent");
      if (userAgent.indexOf("Safari/") != -1 ||
          userAgent.indexOf("MSIE") != -1 && userAgent.indexOf("Mac_PowerPC") != -1) {
%>
              </head>
              <body>
              <jsp:include page="navbar.jsp"></jsp:include>
              Sorry, Safari and Internet Explorer for Mac don't work right on this page.
              We recommend Netscape Navigator 7.1 or later for Macs, which you 
	      can download from the
              <a href="http://channels.netscape.com/ns/browsers/download.jsp">Netscape.com download page</a>.
<%
      } else {
		try {
			PipelineModel model = new PipelineModel();
			model.setUserID(userID);
			HTMLPipelineView viewer = new HTMLPipelineView(out, "http://" + request.getServerName() + ":" + request.getServerPort() + "/gp/makePipeline.jsp", request.getHeader("User-Agent"), request.getParameter("name"));
			PipelineController controller = new PipelineController(viewer, model);
			
			controller.init();
			controller.begin();

			// start with a single blank slate
			controller.displayTask(new TaskInfo());
			controller.end();
		} catch (Exception e) {
			out.println("<br>" + e.getMessage()+"<br>");
			e.printStackTrace();
		}
	}

	if (request.getParameter("autoSave") != null) {
%>
		<script language="Javascript">
			savePipeline(true, "save");
		</script>
<%
	}
%>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>