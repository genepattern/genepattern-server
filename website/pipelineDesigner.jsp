<%@ page import="org.genepattern.server.webapp.*,
                 org.genepattern.server.util.IAuthorizationManager,
                 org.genepattern.server.util.AuthorizationManagerFactory,
                 org.genepattern.util.GPConstants,
                 org.genepattern.util.LSID,
                 org.genepattern.util.LSIDUtil,
                 org.genepattern.server.genepattern.LSIDManager,
                 org.genepattern.webservice.TaskInfo"
         session="true" contentType="text/html" language="Java" buffer="100kb" %>
<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    String userID = (String) request.getAttribute("userID"); // will force login if necessary
    if (userID == null) return; // come back after login
    if (request.getParameter(GPConstants.NAME) != null && !request.getParameter(GPConstants.NAME).equals("") && !LSIDManager.getInstance().getAuthorityType(new LSID(request.getParameter(GPConstants.NAME))).equals(LSIDUtil.AUTHORITY_MINE)) {
        response.sendRedirect("viewPipeline.jsp?" + GPConstants.NAME + "=" + request.getParameter(GPConstants.NAME));
    }
    IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();

    if (!(authManager.checkPermission("createPublicPipeline", userID) || authManager.checkPermission("createPrivatePipeline", userID))) {
        response.sendRedirect("pages/notPermitted.jsf");
        return;
    }


%>

<!doctype HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
    <head>

        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="css/style.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">


        <style>
            .shadow {
                border-style: none;
                font-style: italic;
                font-size: 9pt;
                background-color: transparent;
            }
        </style>

        <script language="Javascript">
            var ie4 = (document.all) ? true : false;
            var ns4 = (document.layers) ? true : false;
            var ns6 = (document.getElementById && !document.all) ? true : false;
            var numTasks = 0;
            var NOT_SET = "[module not yet selected]";
            var stopLoading = false;
            var MAX_TASK_DESCRIPTION_LENGTH = 70;
            var myAuthority = '<%= LSIDManager.getInstance().getAuthority() %>';
            var broadAuthority = '<%= LSIDUtil.BROAD_AUTHORITY %>';
            var suggested = "suggested";

            function scriptError(message, url, lineNumber) {
                if (stopLoading) return true;
                alert('Error in pipelineDesigner script at line ' + lineNumber + ': ' + message);
                return true;
            }
            function hideLayer(lay) {
                if (ie4) {
                    document.all['id' + lay].style.visibility = "hidden";
                }
                if (ns4) {
                    document.layers[lay].visibility = "hide";
                }
                if (ns6) {
                    document.getElementById([lay]).style.display = "none";
                }
            }

            function showLayer(lay) {
                if (ie4) {
                    document.all['id' + lay].style.visibility = "visible";
                }
                if (ns4) {
                    document.layers[lay].visibility = "show";
                }
                if (ns6) {
                    document.getElementById([lay]).style.display = "block";
                }
            }

            function writeToLayer(lay, txt) {
                divs[lay] = txt;
                if (ie4) {
                    document.all("id" + lay).innerHTML = txt;
                }
                if (ns4) {
                    var l = document['id' + lay];
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
                    if (!confirm("There is a limit of " + numTasks + " modules per pipeline.  Adding another module will force the " + numTasks +
                            "th to be deleted.  Press OK to delete '" + MAX_TASKS + ". " +
                            document.forms["pipeline"]['t' + (MAX_TASKS - 1) + '_taskName'].value +
                            "' and insert another.")) {
                        // user cancelled the add, just return
                        return;
                    }
                    // user confirmed the delete, do it
                    deleteTask(MAX_TASKS - 1);
                }

                if (taskNum < numTasks && document.forms["pipeline"]['t' + taskNum + '_taskName'].value == NOT_SET) {
                    // don't enter a blank task twice at the same spot
                    //alert("skipping double new task");
                    return;
                }

                // create task again, at the new location, and copy all of the parameters with their old values
                for (oldTaskNum = numTasks - 1; oldTaskNum >= taskNum; oldTaskNum--) {
                    var newTaskNum = (oldTaskNum + 1);
                    var taskName = document.forms["pipeline"]['t' + oldTaskNum + '_taskName'].value;
                    if (taskName == NOT_SET) {
                        writeToLayer(newTaskNum, newTaskHTML(newTaskNum));
                        continue;
                    }
                    var taskLSID = document.forms["pipeline"]['t' + oldTaskNum + '_taskLSID'].value;
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
                for (i = taskNum + 1; numTasks > 0 && i <= numTasks; i++) {
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
                            selector.options[0] = new Option('Choose module', NOT_SET);
                            selector.options[0].disabled = true;
                            for (t = 0; t < i; t++) {
                                if (t != taskNum) {
                                    //alert((t+1) + '.  ' + document.forms["pipeline"]['t' + t + '_taskName'].value + '=' + t + ', selected=' + (t == selected));
                                    selector.options[selector.options.length] = new Option((t + 1) + '.  ' + document.forms["pipeline"]['t' + t + '_taskName'].value, t, (t == selected), (t == selected));
                                }
                            }
                        }
                    }
                }
                numTasks++;
                if (scrollTo && navigator.userAgent.indexOf("Safari") == -1) window.location.hash = taskNum; // scroll to the new task
            }

            function deleteTask(taskNum) {

                var deletedTaskName = document.forms["pipeline"]['t' + taskNum + '_taskName'].value;
                var warnings = "";

                // pull each task up one
                for (oldTaskNum = taskNum + 1; oldTaskNum < numTasks; oldTaskNum++) {
                    var newTaskNum = (oldTaskNum - 1);
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
                                    warnings = warnings + (newTaskNum + 1) + '. ' + taskName + ' has lost its inherited input for ' +
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
                                    inheritsFromTaskNum = inheritsFromTaskNum - 1;
                                } else if (inheritsFromTaskNum == taskNum) {
                                    if (document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param].selectedIndex > 0) {
                                        warnings = warnings + (newTaskNum + 1) + '. ' + taskName + ' has lost its inherited input for ' +
                                                pi.name + '\n';
                                        continue; // don't set inheritance if it inherited from the deleted task
                                    }
                                }

                                // adjust inheritance task numbers
                                var selector = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + param];
                                var selected = selector.options[selector.selectedIndex].value;
                                selector.length = 0;
                                selector.options[0] = new Option('Choose module', NOT_SET);
                                selector.options[0].disabled = true;
                                for (t = 0; t < newTaskNum; t++) {
                                    var tName = document.forms["pipeline"]['t' + t + '_taskName'].value;
                                    if (tName == NOT_SET) {
                                        continue;
                                    }
                                    if (t != taskNum) {
                                        selector.options[selector.options.length] = new Option((t + 1) + '.  ' +
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
                var invalidCharacters = '[^a-zA-Z0-9\._]';
                var reservedNames = new Array('if', 'else', 'repeat', 'while', 'function', 'for', 'in', 'next', 'break', 'true', 'false', 'null', 'na', 'inf', 'nan');
                var ret = varName.length > 0 && // the name is not empty
                        varName.search(invalidCharacters) == -1 && // it consists of only letters, digits, and periods
                        varName.charAt(0).search('[0-9\.]') == -1 && // it doesn't begin with a digit or period
                        reservedNames[varName.toLowerCase()] == null;	// it isn't a reserved name
                return ret;
            }

            // check if any other tasks already use this name
            function isUniqueName(varName, pipeLsid) {
                var versionlessLsid = LSIDNoVersion(pipeLsid);

                for (task in TaskInfos) {
                    if (TaskInfos[task].name == varName) {
                        // don't error if the LSIDs match
                        var taskLsid = LSIDNoVersion(TaskInfos[task].lsid);
                        if (taskLsid != versionlessLsid) return false;
                    }
                }
                return true;
            }

            function chooseInheritTask(taskNum, param) {
                //alert("chooseInheritTask: taskNum="+taskNum+", param="+param);
                var frm = document.forms['pipeline'];
                frm['t' + taskNum + '_shadow' + param].value = "";
                var ctl = frm['t' + taskNum + '_i_' + param];
                var idx = ctl.options.selectedIndex;

                //alert("index = " + idx + "  " + ctl.options[idx].text);

                if (idx == 0) return;
                ctl.options.selectedIndex = 0;
                ctl.options.selectedIndex = idx;
                ctl.focus();

                var inheritFromTaskNum = ctl.options[idx].value;

                var lsid = frm['t' + inheritFromTaskNum + '_taskLSID'].value;
                var ti = TaskInfos[lsid];
                var fileFormats = ti.fileFormats;

                frm['t' + taskNum + '_prompt_' + param].checked = false;

                ctl = frm['t' + taskNum + '_if_' + param];
                // clear any previous entries in the file number selector
                ctl.options.length = 0;

                // semantic knowledge first!
                ctl.options[ctl.options.length] = new Option('Choose output file', NOT_SET);
                ctl.options[ctl.options.length - 1].disabled = true;

                for (f = 0; f < fileFormats.length; f++) {
                    var ff = fileFormats[f];
                    ctl.options[ctl.options.length] = new Option(ff, ff);
                }
                if (fileFormats.length > 0) {
                    ctl.options[ctl.options.length] = new Option('', NOT_SET);
                    ctl.options[ctl.options.length - 1].disabled = true;
                }

                ctl.options[ctl.options.length] = new Option('1st output', '1');
                ctl.options[ctl.options.length] = new Option('2nd output', '2');
                ctl.options[ctl.options.length] = new Option('3rd output', '3');
                ctl.options[ctl.options.length] = new Option('4th output', '4');
                ctl.options[ctl.options.length] = new Option('stdout', 'stdout');
                ctl.options[ctl.options.length] = new Option('stderr', 'stderr');
                ctl.options[ctl.options.length] = new Option('scatter each output', '?scatter&amp;filter=*');
                ctl.options[ctl.options.length] = new Option('file list of all outputs', '?filelist&amp;filter=*');
            }

            function promptOnRunChecked(checkbox, taskNum, param, paramName) {
                //alert('promptOnRunChecked, checkbox=' + checkbox + ', taskNum=' + taskNum + ', param=' + param + ', paramName='+paramName);
                var frm = checkbox.form;
                if (checkbox.checked) {
                    spanner = document.getElementById('span_' + taskNum + '_' + param);
                    spanner.style.display = "inline";
                    spannerInput = document.getElementById('span_input_' + taskNum + '_' + param);
                    spannerInput.style.display = "none";
                    spannerInput = document.getElementById('span_altinputdisplay_' + taskNum + '_' + param);
                    spannerInput.style.display = "inline";

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
                        frm['t' + taskNum + '_shadow' + param].value = '';

                        if (taskNum > 0) {

                            // clear the two inheritance selectors
                            frm['t' + taskNum + '_i_' + param].selectedIndex = 0;
                            frm['t' + taskNum + '_if_' + param].selectedIndex = 0;
                        }
                    }
                } else {
                    spanner = document.getElementById('span_' + taskNum + '_' + param);
                    spanner.style.display = "none";
                    spannerInput = document.getElementById('span_input_' + taskNum + '_' + param);
                    spannerInput.style.display = "inline";
                    spannerInput = document.getElementById('span_altinputdisplay_' + taskNum + '_' + param);
                    spannerInput.style.display = "none";


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

            var ie5 = document.all && document.getElementById
            var ns6 = document.getElementById && !document.all
            var xpos = "30px"
            var ypos = "30px"


            var pwrCancelCacheName = new Object();
            var pwrCancelCacheDescrip = new Object();


            function openPromptWindow(divid, taskNum, pName) {
                document.getElementById(divid).style.display = ''
                document.getElementById(divid).style.visibility = 'visible'
                document.getElementById(divid).style.width = initialwidth = "270px"
                document.getElementById(divid).style.height = initialheight = "120px"
                document.getElementById(divid).style.left = xpos
                document.getElementById(divid).style.top = ypos

                pwrCancelCacheName[divid] = document.getElementById('t' + taskNum + '_' + pName + '_altName').value;
                pwrCancelCacheDescrip[divid] = document.getElementById('t' + taskNum + '_' + pName + '_altDescription').value;

            }

            function cancelPromptWindow(divid, taskNum, pName) {
                document.getElementById('t' + taskNum + '_' + pName + '_altName').value = pwrCancelCacheName[divid];
                document.getElementById('t' + taskNum + '_' + pName + '_altDescription').value = pwrCancelCacheDescrip[divid];
                document.getElementById(divid).style.display = "none"
            }


            function cumulativeOffset(element) {
                var valueT = 0, valueL = 0;
                do {
                    valueT += element.offsetTop || 0;
                    valueL += element.offsetLeft || 0;
                    element = element.offsetParent;
                } while (element);
                return [valueL, valueT];
            }

            function setPromptPosition(e, divid, anElement) {

                //xpos=(document.body.scrollLeft + (ie5? event.clientX : e.clientX)) + "px";
                //ypos=(document.body.scrollTop - 100  + ((ie5? event.clientY : e.clientY)))+"px";

                ppos = cumulativeOffset(anElement);
                xpos = (ppos[0]) + "px";
                ypos = (ppos[1] - 100) + "px";

                document.getElementById(divid).style.left = xpos;
                document.getElementById(divid).style.top = ypos;

            }

            function closePromptWindow(divid) {
                document.getElementById(divid).style.display = "none"
            }


            function resetDisplay(taskNum, name, descrip) {
                var namestr = 't' + taskNum + '_' + name + '_altName';
                var desstr = 't' + taskNum + '_' + name + '_altDescription';
                document.getElementById(namestr).value = name;
                document.getElementById(desstr).value = descrip;
            }

            function collapseTask(taskNum) {
                document.getElementById('div_collapsable_task_' + taskNum).style.display = "none";
                document.getElementById('hideArrow_' + taskNum).style.display = "none";
                document.getElementById('showArrow_' + taskNum).style.display = "inline";
            }

            function expandTask(taskNum) {
                document.getElementById('div_collapsable_task_' + taskNum).style.display = "inline";
                document.getElementById('hideArrow_' + taskNum).style.display = "inline";
                document.getElementById('showArrow_' + taskNum).style.display = "none";
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
                return lsid.split('<%= LSID.DELIMITER %>').slice(0, -1).join('<%= LSID.DELIMITER %>')
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
                this.authorityType = (this.authority == '<%= LSIDManager.getInstance().getAuthority() %>'.replace(" ", "+")) ? '<%= LSIDUtil.AUTHORITY_MINE %>' : (this.authority == '<%= LSIDUtil.BROAD_AUTHORITY %>' ? '<%= LSIDUtil.AUTHORITY_BROAD %>' : '<%= LSIDUtil.AUTHORITY_FOREIGN %>');
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

            //
            // A task type has been selected for the new module.  Create the list of modules using
            // this type
            //
            function changeTaskType(selectorTaskType, taskNum) {
                var taskSelector = selectorTaskType.form['t' + taskNum];
                taskSelector.options.length = 0;
                if (selectorTaskType.selectedIndex == 0) return; // user chose the "choose task" heading, item 0
                var type = selectorTaskType.options[selectorTaskType.selectedIndex].value;
                taskSelector.options[0] = new Option("- Module -", "");
                taskSelector.options[0].style['fontWeight'] = "bold";
                var versionlessLSIDs = new Array();
                var versionedLSIDs = new Array();

                for (i in TaskTypes[type]) {
                    taskLSID = new LSID(TaskTypes[type][i]);
                    var taskLSIDnoVersion = taskLSID.authority + '<%= LSID.DELIMITER %>' + taskLSID.namespace + '<%= LSID.DELIMITER %>' + taskLSID.identifier;

                    if (versionlessLSIDs[taskLSIDnoVersion] != null) {
                        // already has it, get the latest name though
                        var heldLSID = new LSID(versionedLSIDs[taskLSIDnoVersion]);

                        if (Number(heldLSID.version) <= Number(taskLSID.version)) {
                            // this one is newer, update descrip and name

                            //for debugging:
                            //alert('heldLSID.version='+heldLSID.version+' <= taskLSID.version='+taskLSID.version);
                        } else {
                            continue; // already has latest we've seen yet
                        }
                    }
                    versionlessLSIDs[taskLSIDnoVersion] = taskLSIDnoVersion;
                    versionedLSIDs[taskLSIDnoVersion] = taskLSID.lsid;
                }

                for (lsidKey in versionedLSIDs) {
                    var taskLSID = versionedLSIDs[lsidKey];
                    taskLSID = new LSID(taskLSID);
                    task = TaskInfos[taskLSID.lsid];


                    var optionText = task.name;
                    if (task.description != "") optionText = optionText + ' - ' + task.description
                    if (optionText.length > MAX_TASK_DESCRIPTION_LENGTH) {
                        optionText = optionText.substring(0, MAX_TASK_DESCRIPTION_LENGTH) + "..."
                    }

                    if (taskLSID.authority != broadAuthority && taskLSID.authority != myAuthority) {
                        optionText = optionText + " (" + taskLSID.authority + ")";
                    }

                    taskSelector.options[taskSelector.options.length] = new Option(optionText, task.lsid);
                    var t = taskSelector.options.length - 1;
                    taskSelector.options[t].setAttribute('class', "tasks-" + taskLSID.authorityType);
                    taskSelector.options[t].style['fontWeight'] = "normal";


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
                    var oldParam;
                    for (oldParam in oldTask.parameterInfoArray) {
                        if (oldTask.parameterInfoArray[oldParam].name == pi.name) break;
                    }
                    if (oldTask.parameterInfoArray[oldParam].name != pi.name) {
                        //alert("unable to find " + pi.name + " in old task");
                        continue;
                    }
                    //if (param != oldParam) alert(pi.name + " was in slot " + oldParam + " but now in " + param);

                    // cannot assume that all old variables are present in new task
                    if (document.forms["pipeline"]['t' + oldTaskNum + '_' + pi.name] == null) {
                        alert(pi.name + ' is in the current version but not the previous one');
                        continue;
                    }
                    if (document.forms["pipeline"]['t' + newTaskNum + '_' + pi.name] == null) {
                        alert(pi.name + ' is in the previous version but not the current one');
                        continue;
                    }

                    if (pi.isInputFile) {
                        if (document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + oldParam].value != '') {
                            setParameter(newTaskNum, 'shadow' + param, document.forms["pipeline"]['t' + oldTaskNum + '_shadow' + oldParam].value);
                        }
                        if (oldTaskNum == 0) continue; // first task can't inherit from anywhere
                        var inheritsFromTaskNum = document.forms["pipeline"]['t' + oldTaskNum + '_i_' + oldParam].selectedIndex - 1;
                        if (inheritsFromTaskNum < 0) continue;
                        if (inheritsFromTaskNum >= newTaskNum) {
                            inheritsFromTaskNum++;
                        } else {
                        }
                        if (oldTaskNum > 0) {
                            setFileInheritance(newTaskNum, param, inheritsFromTaskNum, getSelectorValues(document.forms["pipeline"]['t' + oldTaskNum + '_if_' + oldParam]));
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

            function numberSorter(a, b) {
                return a - b;
            }

            function changeTaskHTML(taskLSID, taskNum, bUpdateInheritance) {
                var origTaskLSID = taskLSID;
                var taskFields = "";

                var task = TaskInfos[taskLSID];
                if (task == null) {
                    alert('no such module \"' + taskLSID + '\".  Aborting pipeline loading.');
                    stopLoading = true;
                    window.stop();
                    return ('<hr class="pipelineDesigner">no such module \"' + taskLSID + '\".  Aborting pipeline loading.');
                }

                taskFields = taskFields + "<br/><div class=\"pipeline_item\">";

                taskFields = taskFields + '<input type="hidden" name="t' + taskNum + '_taskName" value="' + task.name + '">';
                taskFields = taskFields + '<table class=\"barhead-task\"><tr><td valign="top">';

                taskFields = taskFields + '<div id="hideArrow_' + taskNum + '" style="display: inline;"><a name="t' + taskNum + '" onClick="collapseTask(' + taskNum + ')"><img src="images/arrow-pipelinetask-down.gif"/></a></div> \n';
                taskFields = taskFields + '<div id="showArrow_' + taskNum + '" style="display: none;"><a name="t' + taskNum + '" onClick="expandTask(' + taskNum + ')"><img src="images/arrow-pipelinetask-right.gif"/></a> </div>\n';


                taskFields = taskFields + (taskNum + 1) + '.&nbsp;' + task.name + ':&nbsp;</td>';


// create selector showing all versions of this task (same authority/namespace/identifier)
                var latestVersion = new LSID(task.lsid).version

                var wildcard = task.lsid.substring(0, task.lsid.length - latestVersion.length);
                var sameTasks = new Array();
                for (t in TaskInfos) {
                    if (TaskInfos[t].lsid.indexOf(wildcard) == 0) {
                        sameTasks = sameTasks.concat(new LSID(TaskInfos[t].lsid).version)
                    }
                }

                sameTasks.sort(numberSorter);
                sameTasks.reverse(numberSorter);

                taskFields = taskFields + '<td align="right">version ';

                if (sameTasks.length > 1) {
                    taskFields = taskFields + '<select name="t' + taskNum + '_taskLSIDv" onchange="chgLSIDVer(' + taskNum + ', this)">\n';

                    for (t in sameTasks) {
                        taskFields = taskFields + '<option value="' + wildcard + sameTasks[t] + '"' +
                                (sameTasks[t] == latestVersion ? ' selected' : '') +
                                '>' + sameTasks[t] + ( t == 0 ? " (latest)" : "") + '</option>\n';
                    }

                    taskFields = taskFields + '</select>';
                } else {
                    taskFields = taskFields + latestVersion;
                }

                taskFields = taskFields + '</td>\n';


// end version selector

                taskFields = taskFields + '</tr></table>';

                taskFields = taskFields + '<div id="div_collapsable_task_' + taskNum + '" >';


                taskFields = taskFields + '<table cols="3" cellspacing="0" valign="top" width="100%"><col align="center" width="10%"><col align="right" width="10%"><col align="left" width="*">';
                taskFields = taskFields + '<tr><td>&nbsp;</td><td></td><td>\n';
                taskFields = taskFields + '<input type="hidden" name="t' + taskNum + '_taskLSID" value="' + task.lsid + '" size="80"><br>';
                taskFields = taskFields + '</td></tr>\n';

                taskFields = taskFields + '<tr><td>&nbsp;</td><td colspan="2" class="description">' + task.description + '</td><td>\n';


                if (task.docs.length > 0 || task.taskType == PIPELINE) {
                    taskFields = taskFields + '<tr><td>&nbsp;</td><td>documentation: </td><td>\n';
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
                    taskFields = taskFields + '<tr class="tableheader-row2"><td class="smalltype2">prompt when run</td><td>parameter name</td><td>value</td><td>description</td></tr>';
                } else {
                    taskFields = taskFields + '<tr><td></td><td colspan="2"><i>' + task.name + ' has no input parameters</i>';
                }

                var pnum = -1;
                for (param in task.parameterInfoArray) {
                    pnum = pnum + 1;
                    var pi = task.parameterInfoArray[param];
                    var choices = ((pi.value != null && pi.value != 'null' && pi.value.length > 0) ? pi.value.split(";") : "");
                    // prompt-when-run
                    taskFields = taskFields + '<tr class=\"taskperameter\"><td valign="top">\n';
                    taskFields = taskFields + '<input type="checkbox" name="t' + taskNum + '_prompt_' + param + '"';
//		taskFields = taskFields + '\n onchange=\"promptOnRunChecked(this, ' + taskNum + ', ' + param + ', \'' + (pi.isInputFile ? "" : pi.name) + '\');\"';
                    taskFields = taskFields + '\n  onclick=\"promptOnRunChecked(this, ' + taskNum + ', ' + param + ', \'' + (pi.isInputFile ? "" : pi.name) + '\');\"';
                    taskFields = taskFields + '>';


                    // XXX add ability to submit an alternate name for prompt when run params
                    taskFields = taskFields + '<span id="span_' + taskNum + '_' + pnum + '" style="display:none">'
                    var namestr = "\'t'+taskNum+'\'_\''+pi.name+'\'_altName\'";
                    //alert(namestr);
                    taskFields = taskFields + '<div id="div_' + taskNum + '_' + pnum + '" class="prompt-window" style="position:absolute;background-color:#FFFFFF;left:0px;top:0px;display:none" onSelectStart="return false">';
                    taskFields = taskFields + '<div id="dwindowcontent" style="height:100%;text-align:center;">';
                    taskFields = taskFields + 'Define alternative name and description to display when prompting for this input.<br>';
                    taskFields = taskFields + '<br><table class="prompt-table" border="0" cellspacing="0" cellpadding="0"><tr border="0" ><td>Display Name:</td><td><input id="t' + taskNum + '_' + pi.name + '_altName" value="' + pi.name + '" name="t' + taskNum + '_' + pi.name + '_altName" value="' + pi.name + '"/></td></tr>';

                    taskFields = taskFields + '<tr><td>Display Description:</td><td><input id="t' + taskNum + '_' + pi.name + '_altDescription" name="t' + taskNum + '_' + pi.name + '_altDescription" value="' + pi.description + '"/></td></tr></table><center>';
                    taskFields = taskFields + '<input type="button" value="Save"   onclick="closePromptWindow(\'div_' + taskNum + '_' + pnum + '\')"/>&#160;&#160;';
                    taskFields = taskFields + '<input type="button" value="Cancel" onclick="cancelPromptWindow(\'div_' + taskNum + '_' + pnum + '\', ' + taskNum + ', \'' + pi.name + '\')"/>&#160;&#160;';
                    taskFields = taskFields + '<input type="button" value="Reset"  onclick="resetDisplay(\'' + taskNum + '\', \'' + pi.name + '\', \'' + pi.description + '\')"/></center>';
                    taskFields = taskFields + '</div></div>';
                    taskFields = taskFields + '  </span>';

                    // XXX END: add ability to submit an alternate name for prompt when run params

                    taskFields = taskFields + '</td>\n';


                    // label for the field
                    taskFields = taskFields + '<td valign="top" style="white-space: nowrap;">';
                    taskFields = taskFields + pi.name.replace(/\./g, ' ').replace(/\_/g, ' ') + ':';
                    taskFields = taskFields + '</td>\n';

                    // input area for the field
                    taskFields = taskFields + '<td> <span id="span_input_' + taskNum + '_' + pnum + '" style="display:inline">';
                    if (pi.isInputFile) {
                        taskFields = taskFields + '<input type="file" name="t' + taskNum + '_' + pi.name +
                                '" size="30" ' +
                                'onchange="changeFile(this, ' + taskNum + ', ' + param + ')" ' +
                                'onblur="blurFile(this, ' + taskNum + ', ' + param + ')" ' +
                                'ondrop="dropFile(this, ' + taskNum + ', ' + param + ')" ' +
                                'class="little">';
                        taskFields = taskFields + '<br><input name="t' + taskNum + '_shadow' + param +
                                '" type="text" readonly size="60" tabindex="-1" class="shadow">';
                        if (taskNum > 0) {
                            taskFields = taskFields + '<br><span style="white-space: nowrap;">or use output from <select name="t' + taskNum + '_i_' + param +
                                    '" onchange="chooseInheritTask(' + taskNum + ', ' + param + ')"><option value=' + NOT_SET + '" >Choose module</option>\n';
                            for (t = 0; t < taskNum; t++) {
                                taskFields = taskFields + '<option value="' + t + '">' + (t + 1) + '.  ' +
                                        document.forms['pipeline']['t' + t + '_taskName'].value + '</option>\n';
                            }
                            taskFields = taskFields + ' </select>\n';

                            taskFields = taskFields + ' &nbsp;';
                            taskFields = taskFields + '<select name="t' + taskNum + '_if_' + param + '"' +
                                    ' onChange="changeTaskInheritFile(this, ' + taskNum + ', ' + param + ', \'' + pi.name + '\')" >\n';
                            // this selector will be filled in dynamically by chooseInheritTask when the task is selected
                            taskFields = taskFields + ' </select></span>\n';
                        }
                    } else if (pi.isOutputFile) {
                        // should never happen
                    } else if (choices.length < 2) {
                        taskFields = taskFields + "<input name='t" + taskNum + "_" + pi.name + "' value='" + pi.defaultValue + "'> ";
                    } else {
                        taskFields = taskFields + "	<select name='t" + taskNum + "_" + pi.name +
                                "' onchange='changeChoice(this, " + taskNum + ", " + param + ")'>\n";
                        for (i in choices) {
                            var c = choices[i].split('=');
                            if (c.length == 1) c = new Array(c, c);
                            taskFields = taskFields + '		<option value="' + c[0] + '"' + (pi.defaultValue == c[0] || pi.defaultValue == c[1] ? ' selected' : '') + '>' + c[1] + '</option>\n';
                        }
                        taskFields = taskFields + "	</select>\n";
                        taskFields = taskFields + ' ';
                    }
                    taskFields = taskFields + "</span>";

                    taskFields = taskFields + '<span id="span_altinputdisplay_' + taskNum + '_' + pnum + '" style="display:none">';

                    taskFields = taskFields + '<a onMouseDown="setPromptPosition(event,\'div_' + taskNum + '_' + pnum + '\', this)"  href="javascript:openPromptWindow(\'div_' + taskNum + '_' + pnum + '\', ' + taskNum + ', \'' + pi.name + '\')">set prompt when run display settings...</a>';

                    taskFields = taskFields + "</span></td><td valign=\"top\">";
                    if (pi.description.length > 0)  taskFields = taskFields + pi.description;
                    taskFields = taskFields + "&nbsp;</td></tr>\n";

                }
                taskFields = taskFields + '</table>\n';
                taskFields = taskFields + '</div>\n';

                taskFields = taskFields + '<br><center>\n';
                if ((taskNum + 1) < MAX_TASKS) {
                    taskFields = taskFields + '<input type="button" value="Add Another Module" onClick="addAnother(' + (taskNum + 1) +
                            ', true)" name="notused" class="little">&nbsp;&nbsp;\n';
                }
                taskFields = taskFields + '<input type="button" value="Delete ' + task.name + '" onClick="deleteTask(' + taskNum +
                        ')" name="notused" class="little">\n</center>';

                if (bUpdateInheritance) {
                    // update the inheritors for any subsequent tasks
                    for (i = taskNum + 1; numTasks > 1 && i < numTasks; i++) {
                        if ((taskNum + 1) == numTasks) break;
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
                                selector.options[0] = new Option('Choose module', NOT_SET);
                                selector.options[0].disabled = true;
                                for (t = 0; t < i; t++) {
                                    if (t != taskNum) {
                                        selector.options[selector.options.length] = new Option((t + 1) + '.  ' + document.forms['pipeline']['t' + t + '_taskName'].value, t, (t == selected), (t == selected));
                                    } else {
                                        selector.options[selector.options.length] = new Option((t + 1) + '.  ' + TaskInfos[taskLSID].name, t, (t == selected), (t == selected));
                                    }
                                }
                            }
                        }
                    }
                }
                taskFields = taskFields + "</div>";
                return taskFields;
            }

            function chgTask(selectorTask, taskNum) {
                if ((numTasks == 0) && (taskNum == 0)) numTasks = 1;

                var taskLSID = selectorTask.options[selectorTask.selectedIndex].value;
                var taskFields = changeTaskHTML(taskLSID, taskNum, true);
                writeToLayer(taskNum, taskFields);
                showLayer(taskNum);
                setDefaultInheritances(taskNum);
            }

            // if task has input files and there is only one possible source, preset the inheritance
            function setDefaultInheritances(taskNum) {
                var task = TaskInfos[document.forms["pipeline"]['t' + taskNum + '_taskLSID'].value];
                for (param in task.parameterInfoArray) {
                    var pi = task.parameterInfoArray[param];
                    if (pi.isInputFile) {
                        var desiredFileFormats = pi.fileFormats;
                        var suggestedTasks = new Array();
                        var suggestedFormats = new Array();
                        for (previousTaskNum = 0; previousTaskNum < taskNum; previousTaskNum++) {
                            var previousTask = TaskInfos[document.forms["pipeline"]['t' + previousTaskNum + '_taskLSID'].value];
                            var profferedFileFormats = previousTask.fileFormats;
                            for (f = 0; f < profferedFileFormats.length; f++) {
                                for (ff = 0; ff < desiredFileFormats.length; ff++) {
                                    if (profferedFileFormats[f] == desiredFileFormats[ff]) {
                                        suggestedTasks[suggestedTasks.length] = previousTaskNum;
                                        suggestedFormats[suggestedFormats.length] = desiredFileFormats[ff];
                                    }
                                }
                            }
                        }
                        if (suggestedTasks.length == 1) {
                            setFileInheritance(taskNum, param, suggestedTasks[0], suggestedFormats[0]);
                        }
                    }
                }
            }

            function setSelector(selector, findValue) {
                if (stopLoading) return;
                var taskNum;
                var taskName;
                if (selector.charAt(0) == 't' && selector.indexOf("_") != -1) {
                    taskNum = selector.substring(1, selector.indexOf("_"));
                    taskName = document.forms['pipeline']['t' + taskNum + '_taskName'].value;
                    if (taskName == NOT_SET) return;
                }
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
                }
                if (numFound == findValues.length) return;

                var i = fld.name.indexOf("_");
                var taskNum = fld.name.substring(1, i);
                var taskName = document.forms['pipeline']['t' + t + '_taskName'].value;
                var userFieldName = fld.name.substring(i + 1);
                var vals = '';
                for (f = 0; f < fld.options.length; f++) {
                    val = vals + fld.options[f].value + "=" + fld.options[f].text + "\n";
                }
                alert('setSelector: could not find ' + findValue + ' among ' + fld.options.length +
                        ' items in ' + taskName + ' (module ' + (parseInt(taskNum) + 1) + ') field ' +
                        userFieldName + '.  Values are:\n' + vals);
            }

            function setOption(selector, findValue) {
                var fld = document.forms['pipeline'][selector];
                if (fld == null) {
                    alert("setOption: " + selector + " does not exist!");
                    return;
                }
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
                if (fld == null) {
                    alert("setField: " + field + " does not exist!");
                    return;
                }
                fld.value = value;
            }

            function setCheckbox(field, value) {
                if (field == null) return;
                var fld = document.forms['pipeline'][field];
                if (fld.type == "select-one") return setSelector(field, value);
                if (fld == null) {
                    alert("setCheckbox: " + field + " does not exist!");
                    return;
                }
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
                        found = window.confirm("Attempt to use latest version (" + new LSID(taskLSID).version + ") of " + taskName + ' for step ' + (taskNum + 1) + ' instead of requested missing version (' + new LSID(requestedLSID).version + ')?')
                    }
                }
                if (!found) {
                    alert("Step " + (taskNum + 1) + ": unable to locate " + taskName + '(' + taskLSID + ')');
                    writeToLayer(taskNum, newTaskHTML(taskNum) +
                            (new LSID(requestedLSID).authority != myAuthority ?
                                    ('<br><a href="<%=request.getContextPath()%>/pages/taskCatalog.jsf?<%= GPConstants.LSID %>=' + requestedLSID + '">load ' + taskName + ' from Broad module catalog</a>') : ""));
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
                    alert("Parameter #" + (thisParamNum + 1) + " in module #" + (thisTaskNum + 1) + ", " +
                            document.forms['pipeline']['t' + thisTaskNum + '_taskName'].value +
                            ", is no longer an input file.  This parameter will not inherit input from module #" +
                            (fromTaskNum + 1) + ", " + document.forms['pipeline']['t' + fromTaskNum + '_taskName'].value +
                            ", file #" + fromFileNum);
                    return;
                }

                if (document.forms['pipeline']['t' + fromTaskNum + '_taskName'].value == NOT_SET) {
                    if (stopLoading) return;

                    var task = TaskInfos[document.forms["pipeline"]['t' + thisTaskNum + '_taskLSID'].value];
                    var pi = task.parameterInfoArray[thisParamNum];

                    alert("Warning: parameter #" + (thisParamNum + 1) + " in module #" + (thisTaskNum + 1) + ", " +
                            document.forms['pipeline']['t' + thisTaskNum + '_taskName'].value +
                            ", inherits " + pi.name + " from module #" +
                            (fromTaskNum + 1) + ", " + document.forms['pipeline']['t' + fromTaskNum + '_taskName'].value +
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
                document.forms['pipeline']['t' + taskNum + '_' + paramName].value = paramValue;
            }

            function checkMatchingRadioValue(radioName, radioValue) {
            }

            // called to validate that the pipeline inputs are acceptable before submitting
            function savePipeline(bMustName, cmd) {
                // delete all stages that aren't defined yet
                for (i = numTasks - 1; i >= 0; i--) {
                    if (document.forms['pipeline']['t' + i + '_taskName'].value == NOT_SET) {
                        var t = i;
                        deleteTask(i);
                    }

                    t--;
                    if (t < 0) break;
                }
                // we start with a single dummy/blank task.  Check for this
                if (numTasks == 1) {
                    var taskOne = document.forms['pipeline']['t' + 0 + '_taskLSID']
                    if (taskOne == null) numTasks = 0;
                }

                var success = true;
                var missingInheritedFileValue = '';

                // validate that the name is present, RSafe and warn them if not unique
                if (bMustName) {
                    var pipeName = document.forms['pipeline'].pipeline_name.value;
                    var pipeLsid = document.forms['pipeline'].LSID.value;

                    success = (pipeName.length > 0);
                    if (!success) {
                        alert('You must enter a pipeline name');
                        return;
                    }
                    success = isRSafe(pipeName);
                    if (!success) {
                        alert(pipelineInstruction);
                        return;
                    }
                    success = isUniqueName(pipeName, pipeLsid);
                    if (!success) {
                        success = confirm('A pipeline named "' + pipeName + '" already exists. Save to this name anyway?');
                        if (!success) return;
                    }

                }


                // Netscape Navigator loses filename selections in file choosers when layers are modified
                // although we have shadowed them into a user-visible field, the user needs to paste them back
                // into the file chooser so that the files will be uploaded to the server upon submission

                var lostFiles = "";
                for (i = 0; i < numTasks; i++) {
                    var taskLSID = document.forms['pipeline']['t' + i + '_taskLSID'].value;
                    var task = TaskInfos[taskLSID];
                    var j = -1;
                    for (param in task.parameterInfoArray) {
                        j++;
                        var pi = task.parameterInfoArray[param];
                        if (pi.isInputFile) {
                            var fileChooserValue = document.forms['pipeline']['t' + i + '_' + pi.name];
                            var shadowValue = document.forms['pipeline']['t' + i + '_shadow' + param].value;

                            if (shadowValue != ''
                                    && fileChooserValue.value != shadowValue
                                    && shadowValue.indexOf('http://') != 0
                                    && shadowValue.indexOf('https://') != 0
                                    && shadowValue.indexOf('ftp://') != 0
                                    && shadowValue.indexOf('file://') != 0
                                    && shadowValue.indexOf('<GenePatternURL>') != 0) {
                                lostFiles = lostFiles + (i + 1) + '. ' + task.name + ': ' + pi.name + ' (was ' + shadowValue + ')\n';
                                success = false;
                            } else if (shadowValue == '') {
                                // make sure inheritence is set
// XXXXX
                                var inheritTask = document.forms['pipeline']['t' + i + '_i_' + j];
                                var inheritFile = document.forms['pipeline']['t' + i + '_if_' + j];
                                var promptWhenRun = document.forms['pipeline']['t' + i + "_prompt_" + j];


                                if (!promptWhenRun.checked && !pi.isOptional) {

                                    if ((inheritTask == null) || (inheritFile == null)) {
                                        missingInheritedFileValue = "module " + i + " inherited value for " + pi.name + " is not fully specified";
                                    } else if ((inheritTask.value != null) && (inheritFile.value == '')) {
                                        missingInheritedFileValue = "module " + i + " inherited value for " + pi.name + " is not fully specified";
                                    }
                                }
                            }
                        }
                    }
                }
                if (!success) {

                    alert("Please copy and paste the input filenames from below each file chooser box into the file chooser above each.\n\n" +
                            lostFiles);
                }
                if (missingInheritedFileValue != '') {
                    success = false;
                    alert(missingInheritedFileValue);
                }

                if (numTasks == 0) {
                    success = false;
                    alert('No modules defined in pipeline');
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
                if (options == null) {
                    selection = sel.value;
                } else {
                    selection = sel.options[sel.selectedIndex].value;
                }
                if (selection == null || selection == "") return;
                if (window.confirm('Really remove ' + selection + ' from support files?\nThis will discard other pipeline changes since the last save.')) {
                    window.location = 'saveTask.jsp?deleteFiles=' + selection +
                            '&deleteSupportFiles=' + selection +
                            '&<%= GPConstants.NAME %>=' + document.forms['pipeline'].pipeline_name.value +
                            '&<%= GPConstants.LSID %>=' + document.forms['pipeline']['LSID'].value +
                            '&forward=pipelineDesigner.jsp' +
                            '&<%= GPConstants.PRIVACY %>=<%= GPConstants.ACCESS_PRIVATE %>';
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
                newTask = newTask + '<hr class="pipelineDesigner">\n';
                newTask = newTask + '<a name="' + (taskNum + 1) + '">\n'; // anchor for each task

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
// alert("considering " + task.name + " which has " + task.parameterInfoArray.length + " parameters");
                    nextTask:
                            for (param in task.parameterInfoArray) {
                                var pi = task.parameterInfoArray[param];
                                if (true || pi.isInputFile) {
                                    var fileFormats = pi.fileFormats;
                                    for (f = 0; f < fileFormats.length; f++) {
                                        var ff = fileFormats[f];
                                        // is this input file format one that might be generated by a previous task?
                                        if (outputFormats[ff] == undefined) {
                                            // no, ignore the task
                                            continue;
                                        }
                                        suggestedTasks[task.lsid] = task;
                                        numSuggested++;

                                        break nextTask;
                                    }
                                }
                            }
                }

                // remove all but latest LSID from each entry
                var LSIDsWithoutVersions = new Array();
                TaskTypes[suggested] = new Array();
                for (t in suggestedTasks) {
                    var task = suggestedTasks[t];
                    if (LSIDsWithoutVersions[task.lsidNoVersion] != undefined) {
                        suggestedTasks[t] = undefined; // delete duplicate item
                        numSuggested--;
                        continue;
                    }
                    LSIDsWithoutVersions[task.lsidNoVersion] = task.lsidNoVersion;
                    TaskTypes[suggested][TaskTypes[suggested].length] = task.lsid;
                }
                // sort by name (case-insensitive, then by LSID descending)!
                TaskTypes[suggested].sort(sortTaskTypesByName);

                newTask = newTask + '<table>\n';
                newTask = newTask + '<tr><td valign="top">\n';
                newTask = newTask + 'Module Category & Name: <select onchange="changeTaskType(this, ' + taskNum + ')" name="notused" >\n';
                newTask = newTask + '<option value="" selected style="font-weight: bold">- Category -</option>\n';

                if (numSuggested > 0) {
                    newTask = newTask + '<option value="' + suggested + '">' + suggested + '</option>\n';
                }

                for (taskType in TaskTypesList) {
                    var name = TaskTypesList[taskType];
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    newTask = newTask + '<option value="' + TaskTypesList[taskType] + '">' + name + '</option>\n';
                }
                newTask = newTask + '</select></td>\n';
                newTask = newTask + '<td valign="top"></td>';
                newTask = newTask + '<td valign="top"><select name="t' + taskNum + '" onchange="chgTask(this, ' + taskNum + ')">\n';
            <
                !--/* newTask = newTask + '<option value="" selected style="font-weight: bold">- Module -</option>'; */ -- >


                        newTask = newTask + '</select></td></tr></table>\n';
                newTask = newTask + '<input type="hidden" name="t' + taskNum + '_taskName" value="' + NOT_SET + '">';
                if (taskNum > 0) {
                    newTask = newTask + '<br><center><input type="button" value="Delete" onClick="deleteTask(' + taskNum +
                            ')" name="notused" class="little"></center>';
                }
                return newTask;
            }

        </script>
        <%
            try {
                HTMLPipelineView viewer = new HTMLPipelineView(out, request.getContextPath(), request.getHeader("User-Agent"), request.getParameter("name"), userID);

                viewer.init();
                viewer.head();
        %>
        <jsp:include page="navbarHead.jsp" />

    </head>
    <body>
        <jsp:include page="navbar.jsp" />
        <div id="messageBox" style="color:red;text-align:center;font-weight:bold;">
            <%
                String message = request.getParameter("message");
                if (message != null) {
                    out.print(message);
                }
            %>
        </div>

        <%
                viewer.writeStartBody();
                // start with a single blank slate
                viewer.generateTask(new TaskInfo());
                viewer.end();
            } catch (Exception e) {
                out.println("<br>" + e.getMessage() + "<br>");
                e.printStackTrace();
            }


            if (request.getParameter("autoSave") != null) {
        %>
        <script language="Javascript">
            savePipeline(true, "save");
        </script>
        <%
            }
        %>

        <jsp:include page="footer.jsp" />

    </body>
</html>