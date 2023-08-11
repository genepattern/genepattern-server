/*
 Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 */

var mainLayout;
var run = false;
var dirty = false;
var saving = false;
var invalidDefaultValueFound = false;

var module_editor = {
    lsid: "",
    uploadedfiles: [],
    filestoupload: [],
    filesToDelete: [],
    currentUploadedFiles: [],
    licensefile: "",
    documentationfile: "",
    documentationUrl: "",
    moduleCategories: [],
    otherModAttrs: {},
    promptForTaskDoc: false
};

var Request = {
    parameter: function(name) {
        return this.parameters()[name];
    },

    parameters: function() {
        var result = {};
        var url = window.location.href;
        var parameters = url.slice(url.indexOf('?') + 1).split('&');

        for(var i = 0;  i < parameters.length; i++) {
            var parameter = parameters[i].split('=');
            result[parameter[0]] = parameter[1];
        }
        return result;
    }
};

//For those browsers that dont have it so at least they won't crash.
if (!window.console)
{
    window.console = { time:function(){}, timeEnd:function(){}, group:function(){}, groupEnd:function(){}, log:function(){} };
}

function forceLowerCategory(strInput) 
{
	if (strInput.value != strInput.value.toLowerCase()){
		$('#forceLowerCategoryWarning').show();
	}
	
    strInput.value=strInput.value.toLowerCase();
}

function trim(s)
{
    var l=0; var r=s.length -1;
    while(l < s.length && s[l] == ' ')
    {	l++; }
    while(r > l && s[r] == ' ')
    {	r-=1;	}
    return s.substring(l, r+1);
}

function setDirty(value)
{
    dirty = value;

    //if page is not already marked as dirty
    if(dirty)
    {
        $(window).bind('beforeunload', function()
        {
            return 'If you leave this page all module changes will be lost.';
        });
    }
    else
    {
        $(window).unbind('beforeunload');
    }
}

function isDirty()
{
    return dirty;
}

function bytesToSize(bytes)
{
    var kilobyte = 1024;
    var megabyte = kilobyte * 1024;
    var gigabyte = megabyte * 1024;
    var terabyte = gigabyte * 1024;

    if ((bytes >= 0) && (bytes < kilobyte)) {
        return bytes + ' B';

    } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
        return (bytes / kilobyte).toFixed() + ' KB';

    } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
        return (bytes / megabyte).toFixed() + ' MB';

    } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
        return (bytes / gigabyte).toFixed() + ' GB';

    } else if (bytes >= terabyte) {
        return (bytes / terabyte).toFixed() + ' TB';

    } else {
        return bytes + ' B';
    }
}


function createErrorMsg(title, message)
{
    var errorDiv = $("<div/>");
    var errorContents = $('<p> <span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span>'
        + '<strong>Error: </strong></p>');
    errorContents.append(message);
    errorDiv.append(errorContents);

    errorDiv.dialog({
        title: title,
        width: 480,
        buttons: {
            "OK": function()
            {
                $(this).dialog("close");
            }
        }
    });
}

function saveError(errorMessage)
{
    $("#savingDialog").empty();
    $("#savingDialog").append(errorMessage).dialog({
        resizable: true,
        width: 400,
        height:200,
        modal: true,
        title: "Module Save Error",
        buttons: {
            OK: function() {
                $(this).dialog("destroy");
                throw(errorMessage);
            }
        }
    });
    saving = false;
    throw new Error(errorMessage);
}

/**
 * Update the Version Increment menu.
 * @param lsidMenu, a gpUtil.LsidMenu object
 */
function updateVersionIncrement(lsidMenu) {
	var theLsid
    if (adminServerAllowed){
    	theLsid = "urn:lsid:"+ $('#lsidAuthority').val()+":" + $('#lsidNamespace').val()+":"+ $('#lsidId').val()+":"+ $('#lsidVersion').val();
        	
    } else {
    	theLsid= module_editor.lsid;
    }
	
	
    if (!lsidMenu) {
        // default menu
        lsidMenu=new gpUtil.LsidMenu(theLsid);
    }
    var next  = lsidMenu.getNextVersionValue() ;
    var menu=$('select[name="versionIncrement"]');
    menu.children().remove();
    var opts=lsidMenu.getNextVersionOptions();
    if (opts && opts.length > 0) {
        for(var i=0; i<opts.length; i++) {
        	var option = $("<option value=\""+opts[i].value+"\">"+opts[i].name+"</option>");
            menu.append(option );
            if (next == opts[i].value){
            	option.attr("selected",true);
            }
        }
    }
    
   
    
    
//    menu.multiselect({
//        multiple: false,
//        header: false,
//        selectedList: 1
//    })
//    .val( lsidMenu.getNextVersionValue() )
//    .multiselect( lsidMenu.isNextVersionEnabled() ? 'enable' : 'disable');
}

/**
 * Update the version menu.
 * @param lsidMenu, a gpUtil.LsidMenu object
 */
function updateModuleVersions(lsidMenu) {
    if (lsidMenu === undefined) {
        return;
    }
    var all=lsidMenu.getAllVersions();
    if (all === undefined || !all.length === 0) {
        return;
    }

    // select 
    var menu=$('select[name="modversion"]');
    var currentVersion=lsidMenu.getCurrentLsid().getLsid();
    if (!currentVersion) {
        console.error("Error getting currentVersion from lsidMenu object");
        currentVersion = menu.val();
    }
    menu.children().remove();
    var all=lsidMenu.getAllVersions();
    if (all && all.length>0) {
        // reverse-sort
        for(var i=all.length-1; i>=0; --i) {
            menu.append(
                    "<option value=\""+all[i].getLsid()+"\">"+all[i].getVersion()+"</option>");
        }
    }
    menu.change(function() {
        var editLocation = "creator.jsf?lsid=" + $(this).val();
        window.open(editLocation, '_self');
    });
    menu.val(currentVersion);
    menu.multiselect("refresh");
}

function runModule(lsid)
{
    window.open("/gp/pages/index.jsf?lsid=" + lsid, '_self');
}

function saveModule()
{
    var modname = $('#modtitle').val();
    if(modname == undefined || modname == null || modname.length < 1)
    {
        saveError("A module name must be specified");
        return;
    }

    var description = $('textarea[name="description"]').val();

    var documentationFile = module_editor.documentationfile;
    if(documentationFile == null || documentationFile == undefined)
    {
        documentationFile = "";
    }
    

    var author = $('input[name="author"]').val();
    var organization = $('input[name="organization"]').val();

    if(organization !== undefined && organization !== "")
    {
        author = author + ";" + organization;
    }
    var src_repo = $('input[name="src.repo"]').val();
    var documentationUrl = $('input[name="documentationUrl"]').val();
    
    
    var privacy = $('select[name="privacy"] option:selected').val();
    var quality = $('select[name="quality"] option:selected').val();
    var language = $('select[name="language"] option:selected').val();
    var lang_version = $('input[name="lang_version"]').val();
    var os = $('input[name=os]:checked').val();

    var taskType = "";

    var categories = [];
    //check if this is a visualizer
    if($("select[name='category']").val() != null && $("select[name='category']").val().length > 0)
    {
        categories = $("select[name='category']").val();

        if($.inArray("visualizer",categories) !== -1)
        {
            taskType = "visualizer";
        }
        else
        {
            //do not allow pipeline to be added as a taskType for a module
            if(categories[0] !== undefined && categories[0] !== null
                && categories.length > 0 && categories[0] !== "pipeline")
            {
                taskType = categories[0];
            }
        }
    }


    var cpu = $("select[name='cpu'] option:selected").val();
    var commandLine = $('textarea[name="cmdtext"]').val();
    var fileFormats = $('select[name="mod_fileformat"]').val();

    if(fileFormats == null)
    {
        fileFormats = [];
    }

    if(commandLine == undefined || commandLine == null || commandLine.length < 1)
    {
        saveError("A command line must be specified");
        return("A command line must be specified");
    }

    var licenseFile = module_editor.licensefile;
    if(licenseFile == null || licenseFile == undefined)
    {
        licenseFile = "";
    }

    var theLsid
    //if (adminServerAllowed){
    // Now any user can explicitly set the version number 
    	theLsid = "urn:lsid:"+ $('#lsidAuthority').val()+":" + $('#lsidNamespace').val()+":"+ $('#lsidId').val()+":"+ $('#lsidVersion').val();
        	
    //} else {
    //	theLsid= module_editor.lsid;
    //}
    
    var lsid = theLsid;
    var supportFiles = module_editor.uploadedfiles;
    var version = $('input[name="comment"]').val();
    var versionIncrement = $('select[name="versionIncrement"] option:selected').val();
    var dockerImage = $('input[name="dockerImage"]').val();
    var jobMemory = $('input[name="jobMemory"]').val();
    var jobCpuCount = $('input[name="jobCpuCount"]').val();
    var jobWalltime = $('input[name="jobWalltime"]').val();
    
    
    var filesToDelete = module_editor.filesToDelete;

    var json = {};
    json["versionIncrement"] =  versionIncrement;
    json["module"] = {"name": modname, "description": description,
        "author": author, "privacy": privacy, "quality": quality,
        "language": language, "JVMLevel": lang_version, "cpuType": cpu, "taskType": taskType, "version": version,
        "job.docker.image": dockerImage,
        "job.memory": jobMemory,"job.cpuCount": jobCpuCount,"job.walltime": jobWalltime, 
        "os": os, "commandLine": commandLine, "LSID": lsid, "supportFiles": supportFiles,
        "filesToDelete": filesToDelete, "fileFormat": fileFormats, "license":licenseFile, "taskDoc":documentationFile,"documentationUrl":documentationUrl ,"src.repo":src_repo};

    var useEditor = $("input[name='param_groups_editor_ok']:checked").val();
    if (useEditor && ($("#param_groups_editor").val().length > 0)){
    	var isValid = validateParamGroupsEditor();
    	json["paramGroupsJson"] = $("#param_groups_editor").val();
    	// do not fail since we want to be able to save in-process modules
    	// even if they may not be runnable
    	//if (!isValid) saveError("There is a problem with your paramGroups.json file");
    }
    
    
    if(categories.length > 0)
    {
        json.module["categories"] = categories;
    }
    //add other remaining attributes
    $.each(module_editor.otherModAttrs, function(keyName, value) {
        console.log("\nsaving other module attributes: " + keyName + "=" + module_editor.otherModAttrs[keyName]);
        json.module[keyName] = module_editor.otherModAttrs[keyName];
    });

    json["parameters"] = getParametersJSON();

    //check if the doc was implicit and so know whether now we should prompt for the correct doc
    if(module_editor.promptForTaskDoc && (json["module"].taskDoc == undefined || json["module"].taskDoc == null
        || json["module"].taskDoc.length < 1) &&  (module_editor.docFileNames != undefined && module_editor.docFileNames != null
        && module_editor.docFileNames.length > 0))
    {
        var docPromptDialog = $("<div/>");
        //get list of all current support files that could be interpreted as doc

        for(var s=0;s<module_editor.docFileNames.length+1;s++)
        {
            var docCheckBox = null;

            if(s != module_editor.docFileNames.length)
            {
                docCheckBox = $("<input type='radio' name='docGroup' value='" + module_editor.docFileNames[s]+ "'>");
                docCheckBox.after(module_editor.docFileNames[s]);
                docPromptDialog.append(docCheckBox);
                docPromptDialog.append("<br/>");
            }
            else
            {
                docCheckBox = $("<input type='radio' name='docGroup' value='' checked>");
                docCheckBox.after("No documentation");
                docPromptDialog.prepend("<br/>");
                docPromptDialog.prepend(docCheckBox);
            }
            docCheckBox.click(function()
            {
                json["module"].taskDoc = $(this).val();
            });
        }

        docPromptDialog.prepend("<p>Please indicate if any of the following support files is the documentation for this module.</p>");

        docPromptDialog.dialog(
        {
            title: "Please select documentation",
            maxWidth: 380,
            maxHeight: 320,
            modal: true,
            buttons:
            {
                OK: function(event, ui)
                {
                    $(this).dialog("close");
                }
            },
            close: function( event, ui )
            {
                saveModulePost(json);
            }
        });
    }
    else
    {
        saveModulePost(json);
    }
}

function saveModulePost(moduleJson)
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/save",
        data: { "bundle" : JSON.stringify(moduleJson) },
        success: function(response) {
            $("#savingDialog").dialog("destroy");

            saving = false;

            var error = response["ERROR"];
            var newLsid = response["lsid"];
            //get updated module versions
            var versions = response["lsidVersions"];

            if (error !== undefined && error !== null) {
                saveError(error);
                return;
            }

            setDirty(false);
            var lsidMenu=new gpUtil.LsidMenu(newLsid, versions);
            updateModuleVersions(lsidMenu);

            // Update the LSID upon successful save
            if (newLsid !== undefined && newLsid !== null)
            {
                //$("#lsid").empty().append("LSID: " + newLsid);
                setLsidDisplay(newLsid)
                
                var vindex = newLsid.lastIndexOf(":");
                if(vindex != -1)
                {
                    var version = newLsid.substring(vindex+1, newLsid.length);
                    var modtitle = $("#modtitle").val();

                    $("#savedDialog").append(modtitle + " version " + version + " saved");
                    $("#savedDialog").dialog({
                        resizable: false,
                        width: 400,
                        height:130,
                        modal: true,
                        title: "Module Saved",
                        open: function () {
                            $(this).parent().find('button:nth-child(2)').focus();
                        },
                        buttons: {
                            "View Manifest": function(){
                            	var url = "/gp/rest/v1/tasks/"+newLsid+"/manifest";
                            	window.open(url, "_blank");
                            },	
                        
                            OK: function() {
                                $( this ).dialog( "close" );
                                module_editor.lsid = newLsid;
                                module_editor.uploadedfiles = [];

                                var unversioned = $(' select[name="modversion"] option[value="unversioned"]');
                                if(unversioned != undefined && unversioned != null)
                                {
                                    unversioned.remove();
                                }

                                $('select[name="modversion"]').val(newLsid);
                                if($('select[name="modversion"]').val() != newLsid)
                                {
                                    var modversion = "<option value='" + newLsid + "'>" + version + "</option>";
                                    $('select[name="modversion"]').append(modversion);
                                    $('select[name="modversion"]').val(version);
                                }

                                $('select[name="modversion"]').multiselect("refresh");

                                if(run)
                                {
                                    runModule(newLsid);
                                }
                                else
                                {
                                    //reload the editor page using the new LSID
                                    var editLocation = "creator.jsf?lsid=" + newLsid;
                                    window.open(editLocation, '_self');
                                }
                            }
                        }
                    });
                }
            }
        },
        dataType: "json"
    });

    module_editor.filesToDelete = [];
}

function editModule()
{
    var lsid = Request.parameter('lsid');

    if(lsid !== undefined && lsid !== null && lsid.length > 0)
    {
        lsid = lsid.replace(/#/g, "");
        loadModule(lsid);
    }
    else {
        //updateVersionIncrement();
    }
}

function reorderParametersToMatchParamGroupsJson(collapse){
	var jsonString = $("#param_groups_editor").val();
	var jsonArr = JSON.parse(jsonString);
	
	//if (true) return;
	
	// get the parameter div objects we will reorder
	var paramDivs =  $('#parameters').find("div.parameter");
	
	// collapse them all
	if (collapse) $("#collapse_all_params").trigger("click");
	
	// collect and remove them
	pdivDict = {};
	for (var i=0; i < paramDivs.length; i++){
		var nameControl = $(paramDivs[i]).find("input[name='p_name']");
		var name = nameControl.val();
		pdivDict[name] = paramDivs[i];
	//	$(paramDivs[i]).remove();
	} 
	 
	var lastParam = null;
	// now put them back in order
	var initialized = false;
	for (var i=0; i < jsonArr.length; i++){
		var group = jsonArr[i];
		var groupParams = group["parameters"];
		
		if ((groupParams != null) &&  (groupParams.length > 0)){
			if (!initialized) {
				lastParam = pdivDict[groupParams[0]]; // first param in first group
				var nameControl = $(lastParam).find("input[name='p_name']");
				$(nameControl).parent().find(".pgroupLabel").remove();
				$(nameControl).parent().append("<span class='pgroupLabel'> <i>Param Group: "+group["name"]+"</i></span>");
				initialized = true;
			} else {
				var firstInGroup = pdivDict[groupParams[0]];
				$(firstInGroup).insertAfter($(lastParam));
				lastParam=pDiv;
				var nameControl = $(firstInGroup).find("input[name='p_name']");
				$(nameControl).parent().find(".pgroupLabel").remove();
				$(nameControl).parent().append("<span class='pgroupLabel'> <i>Param Group: "+group["name"]+"</i></span>");
	
			}
			
			for (var j=1; j < groupParams.length; j++){
				var pName = groupParams[j];
				
				var pDiv = pdivDict[pName];
				var prevDiv = pdivDict[groupParams[j-1]];
				$(pDiv).insertAfter($(prevDiv));
				lastParam = pDiv;
				
				var nameControl = $(pDiv).find("input[name='p_name']");
				$(nameControl).parent().find(".pgroupLabel").remove();
				$(nameControl).parent().append("<span class='pgroupLabel'> <i>Param Group: "+group["name"]+"</i></span>");
				
			}
		}
	}
	$('#parameters').sortable();
	
	
}



var addParamLiveEventsInstalled = false;
function addparameter()
{
	 var paramDiv = $("<div class='parameter'>  \
		        \
		        <table class='param_outer_table'>  \
		        <tr> <td class='dragIndicator'><div class='dragSquare'></div></div></td> \
		        <td><table class='deloptions'> \
		         <tr > <td colspan=5> <span class='param_name'>Name*: <input type='text' name='p_name' size='28'/> </span> <span class='parameter_minimized'>-</span> </td></tr>		\
		        <tr class='delOptionsCollapsible'> \
		        <td class='btntd'>\
		        <button class='delparam'>x Delete</button></td><td>\
		        <p>\
		        <input type='checkbox' name='p_optional' size='25'/>Make this parameter optional.</p>\
		        <p>Description:<br/>\
		        <textarea cols='60' name='p_description' rows='2'></textarea></p>\
		        </td><td >\
		        <table class='pmoptions'>\
		        <tr><td>Flag:<br/><input type='text' name='p_flag' size='7'/>\
		        <input type='checkbox' name='p_prefix' size='7' disabled='disabled'/> prefix when specified \
		        </td> \
		        </tr>\
		        <tr><td>Type of field to display*:<br/>\
		                <select name='p_type' class='m_select'>\
		                        <option value='text'>Text Field</option> \
		                        <option value='Input File'>File Field</option>\
		                </select>\
		        </td></tr></table>\
		        </td>   \
		        </td></tr></table> \
		        </tr>\
		        </table>\
		        <div class='editChoicesDialog'/> \
		        <div class='editFileGroupDialog'/> \
		    </div>");

    paramDiv.find("select[name='p_type']").multiselect({
        multiple: false,
        header: false,
        noneSelectedText: "Select type",
        selectedList: 1,
        position: {
            my: 'left bottom',
            at: 'left top'
        }
    });

    if (!addParamLiveEventsInstalled){
	    $("select[name='p_type']").live("change", function(event)
	    {
	        var tSelect = $(this);
	
	        var choicelist = tSelect.parents(".parameter").find("input[name='choicelist']").val();
	        if(choicelist != undefined && choicelist != null && choicelist.length != 0)
	        {
	            var numItems = choicelist.split(";");
	            var confirmed = confirm("You have a drop down list containing " +
	                numItems.length + " items. Changing parameter types" +
	                " will cause this drop down list to be lost. Do you want to continue?");
	            if(!confirmed)
	            {
	                element.find("option:not(:selected)").click();
	                return;
	            }
	        }
	
	        changeParameterType(tSelect);
	    });

	    $("input[name='p_optional']").live("click", function()
	    {
	        //if parameter is not optional then minimum number of files should be at least 1
	        var minNumValues = $(this).parents(".parameter").find("input[name='minNumValues']");
	        if(minNumValues.length > 0) {
	            if ($(this).is(":checked"))
	            {
	                minNumValues.spinner("value", 0);
	            }
	            else
	            {
	                if (minNumValues.val() < 1)
	                {
	                    minNumValues.spinner("value", 1);
	                }
	            }
	        }
	    });
	    addParamLiveEventsInstalled = true;
    }

    $('#parameters').append(paramDiv);
    
    paramDiv.find(".parameter_minimized").button().click(function() {
    	var oldIcon= paramDiv.find(".parameter_minimized").find(".ui-button-text").text();
    	if (oldIcon.indexOf("-") >= 0){
    		paramDiv.find(".delOptionsCollapsible").hide();
        	paramDiv.find(".parameter_minimized").find(".ui-button-text").text(" + ")
    	} else {
    		paramDiv.find(".delOptionsCollapsible").show();
        	paramDiv.find(".parameter_minimized").find(".ui-button-text").text(" - ")
    	}
    	
    	
    });
    
    paramDiv.find(".delparam").button().click(function()
    {
        //first remove the parameter from the commandline
        var pelement = $(this).parent().parent().parent().find("input[name='p_name']");

        if(!confirm("Are you sure you want to delete the parameter '"+  pelement.val() + "' ?"))
        {
            return;
        }

        var felement = $(this).parent().parent().parent().find("input[name='p_flag']");
        pelement.val("");
        felement.val("");

        updateparameter($(this).parent().parent().parent());

        $(this).parents("div:first").remove();

        setDirty(true);
    });

    // set in the default initial param name
    var paramCount = $("div.parameter").length;
    var nameAvailable = false;
    
    
    while (!nameAvailable){
    	var paramExists = false;
	    pname_newval = "p_"+ paramCount;
	    $('#parameters').find("input[name='p_name']").each(function() {
    	        if(!paramExists && $(this).val() != "" && $(this).val() == pname_newval) {
    	            paramExists = true;
    	        } 
    	});
	    if (paramExists){
	    	paramCount++;
	    
	    } else {
	    	nameAvailable = true;
	    }
	    
    }
    
    
    
    
    paramDiv.find("input[name='p_name']").val(pname_newval);
   
    paramDiv.find("select[name='p_type']").trigger("change");

    return paramDiv;
}

function addGroup()
{
	 var groupDiv = $("<div class='parameterGroup'>  \
		        \
		        <table class='param_outer_table'>  \
		        <tr> <td class='dragIndicator'><div class='dragSquare'></div></div></td> \
		        <td><table class='deloptions'> \
		         <tr > <td colspan=5> </td></tr>		\
		        <tr class='delOptionsCollapsible'> \
		        <td class='btntd'>\
		        <button class='delparam'>x Delete Group</button></td><td>\
		        <span class='paramGroup_name'>Group Name*: <br/><input type='text' name='g_name' size='28'/> </span>    \
		        </td><td>Group Description: <br/> \
		        <textarea cols='60' name='g_description' rows='2'></textarea>\
		        </td><td>Initially Closed:  <input type='checkbox' name='g_hidden'/>  \
		        </td> \
		        </td></tr></table> \
		        </tr>\
		        </table>\
		    </div>");

    $('#parameters').prepend(groupDiv);
    
    
    groupDiv.find(".delparam").button().click(function()
    {
        //first remove the parameter from the commandline
        var pelement = $(this).parent().parent().find("input[name='g_name']");

        if(!confirm("Are you sure you want to delete this group?"))
        {
            return;
        }

        var felement = $(this).parent().parent().find("input[name='g_flag']");
        pelement.val("");
        felement.val("");

        updateparameter($(this).parent().parent());

        $(this).parents("div:first").remove();

        setDirty(true);
    });


    return groupDiv;
}




function addtocommandline(flag, name, prevflag, prevname)
{
    var text = "";
    var nameBracketed = "&lt;" + name + "&gt;";
    var nameBracketedAlt = "<" + name + ">";
    // special case for erasing a deleted param so we don't just get empty brackets.
    if (name.length == 0){
    	nameBracketed = "";
    	nameBracketedAlt = "";
    }
    
    if (flag == "" && name == "" && prevflag ==undefined && prevname == undefined)
    {
        return;
    }
    
    if (flag == prevflag && name == prevname){
    	return;
    }
    
    //construct new parameter value
    if(name !== "")
    {
        text = "&lt;" + name + "&gt;";
    }

    if(flag !== "")
    {
        text = flag + text;
    }

    var item = $("<li class='commanditem'>" + text + "</li>");

    //construct prev parameter value
    var  prevtext = "";
    var prevNameBracketed = "&lt;" + prevname + "&gt;";
    var prevNameBracketedAlt = "<" + prevname + ">";
    var prevNameAndFlag = prevflag + prevNameBracketed;
    var prevNameSpaceAndFlag = prevflag + " " + prevtext;
    	    	
    if(prevname !== "")
    {
        prevtext = prevNameBracketed;
    }

    var paramMatchStrings = new Array();
    if(prevflag !== "")
    {
        prevtext = prevNameAndFlag;
    }
    
    //if no change (exact) in value do nothing
    if(prevtext == text) return;

    // JTL 083120
    // the previous check only works if the keyup is how the flag was added.  Frequently you see a few other conditions
    // where we do not want to edit the command line or want to change it to match what is there for this param
    //
    // 1. The user inserted a space between the flag and the name in the command line editor and not in the param editor
    // 2. The flag is not in the command line at all  so it was added as a flag that would be inserted
    //
    if (text == prevNameAndFlag) return;
    if (text == prevNameSpaceAndFlag) return;
    
    
    //look for child with matching old parameter value and replace with new parameter value
    var cmdline= $("#commandtextarea textarea").val();

    // look for a match on the current command line
    var cmdTokens = cmdline.split(" ");
    for (i=0; i < cmdTokens.length; i++){
    	var tok = cmdTokens[i];
    	console.log((cmdTokens[i] == prevflag));
    	
    	if ((cmdTokens[i] == prevNameBracketed) || (cmdTokens[i] == prevNameBracketedAlt)) {
    		// we found the param name token .  Now look at the previous token to see if its the old flag
    		// if it is, update both tokens
    		// if not, just update the param name in case its changed
    		if (i > 0) {
    			if (cmdTokens[i -1] == prevflag){
    				cmdTokens[i -1] = flag;
    				cmdTokens[i] = nameBracketedAlt;
    				
    			} else {
    				// it was on the command line without the flag there explicitly.  This is allowed
    				// so just update this name tag
    				cmdTokens[i] = nameBracketedAlt;
    			}
    			
    		}
    		
    	} else if (cmdTokens[i] == prevNameAndFlag) {
    		// replace prevNameAndFlag with new name and flag with no space
    		cmdTokens[i] = flag + nameBracketed;
    	} else if ((cmdTokens[i] == prevflag) && (prevflag !== "")) {
    		cmdTokens[i] = flag;
    	} else if ((cmdTokens[i].trim() == prevflag.trim()) && (prevflag !== "")) {
    		cmdTokens[i] = flag;
    	}
    	
    }
    var newCommandLine = cmdTokens.join(" ");
    $("#commandtextarea textarea").val(newCommandLine);
    
    
    // if old parameter value was not found then this must be a new parameter so
    // insert it into parameter list
    if(nameBracketedAlt !== "")
    {
        //if argument is already in command which will occur if this is
        // a module edit
        if(newCommandLine.indexOf(nameBracketedAlt) == -1)
        {
        	newCommandLine += " " + nameBracketedAlt;
            $("#commandtextarea textarea").val(newCommandLine);
        }
    }
    
    
    if (true) return;
    
    
    // below is from the commandList dialog - may want to delete it...
    var found = false;


    var decodedPrevText = $('<div/>').html(prevtext).text();

    var decodedText = $('<div/>').html(text).text();

    $('#commandlist').children().each(function()
    {
        //decode the prevtext string first and compare it
        if($(this).text() ==  decodedPrevText)
        {
            if(text !== "")
            {
                $(this).replaceWith(item);
            }
            else
            {
                $(this).remove();
            }
            found = true;
            cmdline = cmdline.replace(decodedPrevText, decodedText);
            $("#commandtextarea textarea").val(cmdline);

        }
    });

    
  
}

//update the specific parameter div
function updateparameter(parameter, updateCmdLine)
{
    if(typeof(updateCmdLine)==='undefined') updateCmdLine = true;
    var pelement = parameter.find("input[name='p_name']");
    var felement = parameter.find("input[name='p_flag']");
    
    
    var pelementval = pelement.val().replace(/ /g, ".");
    pelement.val(pelementval);
   
    var pname_newval = pelement.val();
    var pflag_newval = felement.val();
    if(parameter.find("input[name='p_prefix']").attr('checked'))
    {
        pflag_newval = "";
    }

    var pname_oldval = pelement.data('oldVal');
    var pflag_oldval = felement.data('oldVal');
    if (pname_oldval == null) pname_oldval = "";
    if (pflag_oldval == null) pflag_oldval = "";
    
    
    
    pelement.data('oldVal',  pname_newval );
    felement.data('oldVal',  pflag_newval );

    var paramExists = false;
    //check if parameter exists
    parameter.siblings().find("input[name='p_name']").each(function()
    {
        if(!paramExists && $(this).val() != "" && $(this).val() == pname_newval)
        {
            paramExists = true;
        }
    });

    if(paramExists)
    {
        alert("The parameter: " + pname_newval + " already exists.");
        pelement.val("");
        return;
    }

    //do not update the command line
    if(updateCmdLine)
    {
        addtocommandline(pflag_newval, pname_newval, pflag_oldval, pname_oldval);
    }
    else
    {
        //add any new parameter to the command line argument listing
        if(pname_oldval == undefined && pflag_oldval == undefined)
        {
            var text = "&lt;" + pname_newval + "&gt;";
            text = pflag_newval + text;

            var decodedText = $('<div/>').html(text).text();
            var cmdline= $("#commandtextarea textarea").val();


            if(cmdline.indexOf(decodedText) != -1)
            {
                var item = $("<li class='commanditem'>" + text + "</li>");

                $('#commandlist').append(item);
            }
        }
    }
}

function changeParameterType(element) {
    element.multiselect("refresh");

    if (!element.parent().next().children().is("input[name='p_prefix']")) {
        element.parent().next().remove();
    }

    var value = element.val(); //type of the parameter
    element.parents(".pmoptions").parent().find(".textFieldData").remove();

    var fieldDetailsTd = $("<td/>");
    if (value == "Input File") {
        fieldDetailsTd.append("File format: <br/>");

        var fileFormatList = $('<select multiple="multiple" name="fileformat"></select>');
        var fileFormatButton = $('<button id="addinputfileformat">New</button>');

        fileFormatButton.button().click(function () {
            $("#addfileformatdialog").dialog("open");
        });

        //copy option values from the modules output file format list that was generated earlier
        $('select[name="mod_fileformat"]').children("option").each(function () {
            fileFormatList.append("<option>" + $(this).val() + "</option>");
        });

        fieldDetailsTd.append(fileFormatList);
        fieldDetailsTd.append(fileFormatButton);

        fileFormatList.multiselect({
            header: true,
            noneSelectedText: "Specify input file formats",
            selectedList: 4, // 0-based index
            position: {
                my: 'left bottom',
                at: 'left top'
            }
        }).multiselectfilter();
    }
    else
    {
        var format = element.parents(".parameter").find("select[name='p_format']");

        var dataTypeTd = $("<td class='textFieldData'/>");
        dataTypeTd.append("Type of data to enter*: <br/>");
        var formatList = $("<select name='p_format'>\
                <option value='String'>Text</option>\
                <option value='Integer'>Integer</option>\
                <option value='Floating Point'>Floating Point</option>\
                <option value='Directory'>Directory</option>\
                <option value='Password'>Password</option>\
            </select> ");
        formatList.change(function () {
            //hide choices info if this is a directory or password entry
            $(this).parents(".parameter:first").find(".minValues").show();
            $(this).parents(".parameter:first").find(".maxValues").show();
            $(this).parents(".parameter:first").find(".choices").show();

            $(this).parents(".parameter:first").find(".range").hide();
            $(this).parents(".parameter:first").find(".range").hide();

            if ($(this).val() === "Directory")
            {
                $(this).parents(".parameter:first").find(".choices").hide();
            }
            else if ($(this).val() === "Password")
            {
                $(this).parents(".parameter:first").find(".choices").hide();
                $(this).parents(".parameter:first").find(".minValues").hide();
                $(this).parents(".parameter:first").find(".maxValues").hide();
            }
            else if ($(this).val() === "Integer" || $(this).val() === "Floating Point")
            {
                $(this).parents(".parameter:first").find(".range").show();
                $(this).parents(".parameter:first").find(".range").show();
            }
        });

        dataTypeTd.append(formatList);
        formatList.multiselect({
            header: false,
            multiple: false,
            noneSelectedText: "Specify text format",
            selectedList: 1, // 0-based index
            position: {
                my: 'left bottom',
                at: 'left top'
            }
        });

        $("<tr/>").append(dataTypeTd).appendTo(element.parents(".pmoptions"));
    }

    var typeDetailsTable = $("<table class='ptypeDetails pmoptions'/>");

    element.parents(".pmoptions").parent().next(".lasttd").remove();

    $("<td class='lasttd'/>").append(typeDetailsTable).insertAfter(element.parents(".pmoptions").parent());
    $("<tr/>").append(fieldDetailsTd).appendTo(typeDetailsTable);

    var helpImgSrc = $(".helpbutton").first().attr("src");
    var defaultValueRow = $("<tr/>");
    var defaultValue = $("<input type='text' name='p_defaultvalue' class='defaultValue' size='40'/>");
    $("<td/>").append("Default value: ").append(defaultValue).append("<a href='createhelp.jsp#paramDefault' target='help'> " +
        " <img src='" + helpImgSrc + "' width='12' height='12' alt='help' class='buttonIcon' />"
        + "</a>").appendTo(defaultValueRow);
    typeDetailsTable.append(defaultValueRow);

    var specifyChoicesRow = $("<tr class='choices'/>");
    var editChoicesLink = $("<a href='#' class='choicelink'>add a drop-down list</a>");
    editChoicesLink.click(function (event) {
        event.preventDefault();

        var isFile = !($(this).parents(".parameter").find("select[name='p_type']").val() != "Input File");
        var choices = $(this).parents(".parameter").find("input[name='choicelist']").val();
        var pName = $(this).parents(".parameter").find("input[name='p_name']").val();
        var title = "Create drop-down list";
        if (pName != null && pName != undefined && pName.length > 0) {
            pName = pName.replace(/\./g, " ");

            title = "Edit Choices for " + pName;
        }

        var editChoicesDialog = $(this).parents(".parameter").find(".editChoicesDialog");
        editChoicesDialog.empty();

        editChoicesDialog.dialog({
            autoOpen: true,
            height: 620,
            width: 630,
            title: title,
            create: function () {
                var enterValuesDiv = $("<div class='hcontent'/>");
                $(this).prepend(enterValuesDiv);

                var staticChoiceDiv = $("<div class='staticChoicesDiv'/>");
                enterValuesDiv.append(staticChoiceDiv);
                var choiceButton = $("<button class='choiceadd'>Add Menu Item</button>");
                choiceButton.button().click(function () {
                    var choicerow = $("<tr> <td> <div class='sortHandle'><div class='frictionBox'/><div class='frictionBox'/></div></td><td class='defaultChoiceCol'> <input type='radio' name='cradio'/></td>" +
                        "<td> <input type='text' name='choicev' class='choiceFields'/> </td>" +
                        "<td> <input type='text' name='choicen' class='choiceFields'/> </td>" +
                        "<td> <button> X </button></td></tr>");
                    choicerow.find("button").button().click(function () {
                        $(this).parent().parent().remove();
                    });

                    choicerow.find("input[name='cradio']").click(function () {
                        //check if this is the first item in the list
                        var firstListItem = $(this).parents("tr :first").index();
                        if (firstListItem == 1) {
                            //this is the first data item in the list so allow it to be set as the default
                            return;
                        }

                        //check if the actual value is empty
                        var actualValue = $(this).parents("tr:first").find("input[name='choicev']").val();
                        if (actualValue == undefined || actualValue == null || actualValue.length < 1) {
                            alert("Please either specify a value to pass on the command line or make this item the first selection" +
                                " in the list in order to make it the default");
                            $(this).removeAttr("checked");
                            $(this).parents(".editChoicesDialog").find("input[name='cradio']").first().click();
                        }
                    });

                    choicerow.find("input[name='choicev']").focusout(function () {
                        //set the display value if it is empty
                        if ($(this).val() != "") {
                            if (choicerow.find("input[name='choicen']").val() == "") {
                                var displayVal = $(this).val();
                                if (isFile) {
                                    displayVal = displayVal.replace(/\/\//g, '');
                                    var url_split = displayVal.split("/");
                                    if (url_split.length > 1) {
                                        //get last item in parsed file url
                                        displayVal = url_split[url_split.length - 1];
                                    }
                                }

                                choicerow.find("input[name='choicen']").val(displayVal);
                            }
                        }
                        else {
                            //check if this was marked as the default and do not allow since
                            //the actual value is blank
                            if ($(this).parents("tr :first").index() > 1 && choicerow.find("input[name='cradio']").is(":checked")) {
                                alert("Please either specify a value to pass on the command line or make this item the first selection" +
                                    " in the list in order to make it the default");
                                $(this).parents(".editChoicesDialog").find("input[name='cradio']").first().click();
                            }
                        }
                    });

                    //if this is the only item in the list set it as the default
                    if ($(this).parents(".editChoicesDialog").find(".staticChoiceTable").find("input[name='cradio']:checked").length == 0) {
                        $(this).parents(".editChoicesDialog").find(".staticChoiceTable").find("input[name='cradio']:first").click();
                    }

                    $(this).parent().find("table").find("tbody").append(choicerow);

                    var choiceEntries = document.getElementsByClassName("choiceFields");

                    for (var y = 0; y < choiceEntries.length; ++y) {
                        var choiceEntry = choiceEntries[y];
                        choiceEntry.addEventListener("dragenter", function (evt) {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, true);
                        choiceEntry.addEventListener("dragleave", function (evt) {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, true);
                        choiceEntry.addEventListener("dragexit", function (evt) {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, false);
                        choiceEntry.addEventListener("dragover", function (evt) {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, false);
                        choiceEntry.addEventListener("drop", function (evt) {
                            evt.stopPropagation();
                            evt.preventDefault();

                            console.log("choice entry drop");
                            if (evt.dataTransfer.getData('Text') != null
                                && evt.dataTransfer.getData('Text') !== undefined
                                && evt.dataTransfer.getData('Text') != "") {
                                $(this).val(evt.dataTransfer.getData('Text'));
                            }
                        }, false);
                    }

                });
                staticChoiceDiv.append(choiceButton);

                var valueColHeader = "Value";
                var dValueColHeader = "Display Value";
                var valueColHeaderDescription = "The value to pass on the command line";
                var dValueColHeaderDescription = "The value to display in the drop-down list";

                if (isFile) {
                    //change header text for file choices
                    valueColHeader = "URL";
                    valueColHeaderDescription = "Enter URLs (ftp, http, https)";
                }

                var table = $("<table class='staticChoiceTable'>" +
                    "<thead><tr class='choiceHeaderRow'><td></td><td><span class='staticTableHeader'> Default </span> " +
                    "<br/><span class='shortDescription'> The default selection </span></td>" +
                    "<td> <span class='staticTableHeader'>" + valueColHeader + "</span>" +
                    "<br/>" +
                    "<span class='shortDescription'>" + valueColHeaderDescription + "</span>" +
                    "</td> <td>" +
                    "<span class='staticTableHeader'>" + dValueColHeader + "</span>" +
                    "<br/>" +
                    "<span class='shortDescription'>" + dValueColHeaderDescription + "</span>" +
                    " </td> </tr> </thead><tbody></tbody></table>");

                staticChoiceDiv.prepend(table);

                table.find("tbody").sortable(
                    {
                        placeholder: "ui-sort-placeholder",
                        forcePlaceholderSize: true,
                        start: function (event, ui) {
                            ui.item.addClass("highlight");
                        },
                        stop: function (event, ui) {
                            var element = ui.item;
                            element.removeClass("highlight");

                            //check if this item is set at as the default and its actual value is blank
                            if (element.find("input[name='cradio']").is(":checked")
                                && (element.find("input[name='choicev']").val() == undefined
                                    || element.find("input[name='choicev']").val() == null
                                    || element.find("input[name='choicev']").val().length < 1)) {
                                alert("You are not allowed to move a default selection with no command line " +
                                    "value to any position except the first");
                                element.parents("tbody").sortable("cancel");
                            }
                        },
                        handle: ".sortHandle"
                    });

                var result = choices.split(';');
                if (choices == "" || result == null || result.length < 1) {
                    //start with two rows of data
                    choiceButton.click();
                    choiceButton.click();
                }
                else {
                    for (var i = 0; i < result.length; i++) {
                        var rowdata = result[i].split("=");

                        var displayValue = "";
                        var value = "";
                        if (rowdata.length > 1) {
                            displayValue = rowdata[1];
                            value = rowdata[0];
                        }
                        else {
                            value = rowdata[0];
                        }

                        choiceButton.click();

                        table.find("input[name='cradio']").last().removeAttr("disabled");

                        //check if this should be set as the default
                        if (value != "" && element.parents(".parameter").find(".defaultValue").val() == value) {
                            table.find("input[name='cradio']:checked").removeAttr("checked");
                            table.find("input[name='cradio']").last().attr("checked", "checked");
                        }

                        table.find("input[name='choicev']").last().val(value);
                        table.find("input[name='choicen']").last().val(displayValue);
                    }
                }

                if (isFile) {
                    //type is file then display field to input url to retrieve
                    //files from
                    var choiceURLDiv = $("<div class='choicesURLDiv'/>");

                    var choiceURLTable = $("<table/>");
                    choiceURLDiv.append(choiceURLTable);
                    var choiceURLTableTR = $("<tr/>");
                    choiceURLTableTR.append("<td width=\"130px\">Ftp directory URL<br/>or S3 directory URI:</td>");
                    var choiceURL = $("<input name='choiceURL' type='text' size='45'/>");
                    choiceURL.val(element.parents(".parameter").find("input[name='choiceDir']").val());
                    $("<td/>").append(choiceURL).append("<div class='shortDescription'>Enter the ftp/S3 directory " +
                        "containing the files to use to populate the drop-down list</div>").appendTo(choiceURLTableTR);
                    choiceURLTable.append(choiceURLTableTR);

                    //add filter box
                    var choiceURLTableFilterTR = $("<tr/>");
                    choiceURLTableFilterTR.append("<td>File filter:</td>");
                    choiceURLTable.append(choiceURLTableFilterTR);
                    var globDoc = "Enter comma-separated list if one or more glob patterns (e.g. '*.gct') or anti-patterns (e.g. '!*.cls'). By default, 'readme.*' and '*.md5' files are ignored. " +
                        "By default, sub-directories are ignored. To include sub-directories instead of files set 'type=dir'. " +
                        "To include both files and directories set 'type=all'. The two can be combined (e.g. 'type=dir&hg*').";
                    var fileFilter = $("<input name='choiceURLFilter' type='text'/>");
                    fileFilter.val(element.parents(".parameter").find("input[name='choiceDirFilter']").val());
                    $("<td/>").append(fileFilter).append("<div class='shortDescription'>" + globDoc + "</div>").appendTo(choiceURLTableFilterTR);

                    var altStaticChoiceToggle = $("<input type='checkbox' class='staticChoiceLink'/>");
                    altStaticChoiceToggle.click(function (event) {
                        $(this).parents(".editChoicesDialog").find(".staticChoicesDiv").toggle();

                        //hide the default value column
                        $(this).parents(".editChoicesDialog").find(".staticChoiceTable").find("tr").each(function () {
                            var numElements = $(this).find("td").length;
                            if (numElements > 2) {
                                $(this).find("td:nth-child(2)").hide();
                            }
                        });
                    });
                    $("<span>Specify alternative static drop-down list</span>").prepend(altStaticChoiceToggle).appendTo(choiceURLDiv);

                    var altStaticChoiceDescription = $("<span class='altStaticDesc shortDescription'> Static values will be " +
                        "displayed in the event the dynamic choices cannot be loaded</span>");
                    altStaticChoiceToggle.parent().append("<br/>").append(altStaticChoiceDescription);

                    enterValuesDiv.prepend(choiceURLDiv);

                    $(this).prepend("<p class='heading editChoicesHeading'>Step 2: Enter URL(s)</p>");

                    var dynamicChoiceButton = $('<input type="radio" name="radio" class="dynamicChoice"/><label for="radio1">Dynamic drop-down list</label>');
                    var staticChoiceButton = $('<input type="radio" name="radio" class="staticChoice"/><label for="radio1" >Static drop-down list</label>');

                    if (choiceURL.val() != undefined && choiceURL.val() != null && choiceURL.val() != "") {
                        $(this).find(".choicesURLDiv").show();
                        $(this).find(".staticChoicesDiv").hide();
                        if (choices != undefined && choices != null && choices.length > 1) {
                            altStaticChoiceToggle.click();
                        }
                        dynamicChoiceButton.attr("checked", "checked");
                    }
                    else {
                        $(this).find(".choicesURLDiv").hide();
                        $(this).find(".staticChoicesDiv").show();
                        staticChoiceButton.attr("checked", "checked");
                    }

                    dynamicChoiceButton.click(function () {
                        if ($(this).data("prevSelection") == "dynamic") {
                            //do nothing since no change from previous selection
                            return;
                        }

                        //remove any values specified for static or dynamic drop-down lists
                        $(this).parents(".editChoicesDialog").find("input[name='choiceURL']").val("");
                        $(this).parents(".editChoicesDialog").find("input[name='choiceURLFilter']").val("");

                        $(this).parents(".editChoicesDialog").find(".choicesURLDiv").show();
                        $(this).parents(".editChoicesDialog").find(".staticChoicesDiv").hide();

                        $(this).parents(".editChoicesDialog").find(".staticChoiceLink").removeAttr("checked");

                        //keep track of previously selected drop-down list types
                        $(this).parents(".selectChoiceTypeDiv").find(".staticChoice").data("prevSelection", "dynamic");
                        $(this).data("prevSelection", "dynamic");
                    });

                    staticChoiceButton.click(function () {
                        if ($(this).data("prevSelection") == "static") {
                            //do nothing since no change from previous selection
                            return;
                        }

                        //remove any values specified for static or dynamic drop-down lists
                        $(this).parents(".editChoicesDialog").find("input[name='choiceURL']").val("");
                        $(this).parents(".editChoicesDialog").find("input[name='choiceURLFilter']").val("");

                        $(this).parents(".editChoicesDialog").find(".choicesURLDiv").hide();
                        $(this).parents(".editChoicesDialog").find(".staticChoicesDiv").show();
                        $(this).parents(".editChoicesDialog").find(".staticChoicesDiv").find(".staticChoiceTable").find("td").show();
                        $(this).parents(".editChoicesDialog").find(".staticChoiceLink").removeAttr("checked");

                        //keep track of previously selected choices
                        $(this).parents(".selectChoiceTypeDiv").find(".dynamicChoice").data("prevSelection", "static");
                        $(this).data("prevSelection", "static");
                    });

                    var selectChoiceTypeDiv = $("<div class='selectChoiceTypeDiv hcontent'/>");
                    $("<div/>").append("<p class='editChoiceEntry'>Select this option to manually enter a list of files to populate the drop-down list</p>").prepend(staticChoiceButton).appendTo(selectChoiceTypeDiv);
                    $("<div/>").append("<p class='editChoiceEntry'>Select this option to create a drop-down list that will be dynamically populated <br/> with a list of files found at a remote location</p>").prepend(dynamicChoiceButton).appendTo(selectChoiceTypeDiv);
                    $(this).prepend(selectChoiceTypeDiv);

                    var selectDropDownType = $("<p class='heading editChoicesHeading'>Step 1: Select drop-down type</p>");
                    $(this).prepend(selectDropDownType);

                    addsectioncollapseimages();
                }
            },
            close: function () {
                $(this).dialog("destroy");
            },
            buttons: {
                "OK": function () {
                    var choicelist = "";
                    var newDefault = "";

                    if (!isFile || $(this).find(".staticChoice").is(":checked")
                        || ($(this).find(".dynamicChoice").is(":checked") && $(this).find(".staticChoiceLink").is(":checked"))) {
                        $(this).find(".staticChoiceTable").find("tr").each(function () {
                            var dvalue = $(this).find("td input[name='choicen']").val();
                            var value = $(this).find("td input[name='choicev']").val();

                            if ((dvalue == undefined && value == undefined)
                                || (dvalue == "" && value == "")) {
                                return;
                            }

                            if (choicelist !== "") {
                                choicelist += ";";
                            }

                            if (dvalue == undefined || dvalue == null || dvalue == "") {
                                choicelist += value;
                            }
                            else {
                                choicelist += value + "=" + dvalue;
                            }

                            //set default value
                            if ($(this).find("input[name='cradio']").is(":checked")) {
                                //set the default value
                                newDefault = $(this).find("input[name='choicev']").val();
                                newDefault = newDefault.trim();
                            }
                        });
                    }

                    var choiceURL = $(this).find("input[name='choiceURL']").val();
                    //set the dynamic url if there is any
                    if (choiceURL == undefined && choiceURL == null) {
                        choiceURL = "";
                    }

                    if ($(this).find(".dynamicChoice").is(":checked") && choiceURL.length < 1) {
                        alert("Please enter an ftp directory or switch to a static drop-down list");
                        return;
                    }

                    element.parents(".parameter").find("input[name='choiceDir']").val(choiceURL);
                    element.parents(".parameter").find("input[name='choiceDir']").trigger("change");

                    //set the dynamic url filter if there is any
                    var choiceURLFilter = $(this).find("input[name='choiceURLFilter']").val();
                    //set the dynamic url if there is any
                    if (choiceURLFilter == undefined && choiceURLFilter == null) {
                        choiceURLFilter = "";
                    }

                    element.parents(".parameter").find("input[name='choiceDirFilter']").val(choiceURLFilter);
                    element.parents(".parameter").find("input[name='choiceDirFilter']").trigger("change");


                    element.parents(".parameter").find("input[name='choicelist']").val(choicelist);
                    element.parents(".parameter").find("input[name='choicelist']").trigger("change");

                    //set default value
                    if (choiceURL == "" && choicelist.length > 0) {
                        //element.parents(".parameter").find(".defaultValue").find("option:selected").removeAttr("selected");
                        element.parents(".parameter").find(".defaultValue").val(newDefault);

                        if (newDefault != "" && element.parents(".parameter").find(".defaultValue").val() != newDefault) {
                            element.parents(".parameter").find(".defaultValue").append("<option value='" + newDefault + "'>" +
                                newDefault + "</option>");
                            element.parents(".parameter").find(".defaultValue").val(newDefault);
                        }

                        element.parents(".parameter").find(".defaultValue").multiselect("refresh");
                    }

                    $(this).dialog("destroy");
                },
                "Cancel": function () {
                    $(this).dialog("destroy");
                }
            },
            resizable: true
        });
    });

    $("<td/>").append(editChoicesLink).appendTo(specifyChoicesRow);

    var helpImgSrc = $(".helpbutton").first().attr("src");
    editChoicesLink.parent().append("<a href='createhelp.jsp#paramType' target='help'> " +
        " <img src='" + helpImgSrc + "' width='12' height='12' alt='help' class='buttonIcon' />"
        + "</a>");

    editChoicesLink.parent().append("<div class='staticChoicesInfo'/>");
    editChoicesLink.parent().append("<div class='dynamicChoicesInfo'/>");

    //create hidden link for list of choices
    editChoicesLink.parent().append("<input type='hidden' name='choicelist'/>");

    //also create hidden fields for the ftp directory and file filter
    editChoicesLink.parent().append("<input type='hidden' name='choiceDir'/>");
    editChoicesLink.parent().append("<input type='hidden' name='choiceDirFilter'/>");

    editChoicesLink.parent().find("input[name='choicelist']").change(function () {
        var choicelist = $(this).parents(".parameter").find("input[name='choicelist']").val();
        $(this).parents(".parameter").find(".staticChoicesInfo").text("");

        var prevDefaultField = $(this).parents(".parameter").find(".defaultValue");
        var choiceDir = $(this).parents(".parameter").find("input[name='choiceDir']").val();

        if (choicelist != null && choicelist != undefined && choicelist.length > 0) {
            //change text of the create drop down link to edit
            $(this).parents(".parameter").find(".choicelink").text("edit drop down list");

            var choicelistArray = choicelist.split(";");
            if (choicelistArray.length > 0) {
                $(this).parents(".parameter").find(".staticChoicesInfo").append("Static list: " + choicelistArray.length + " items");
            }

            //change the default value field to a combo box if this is not a dynamic file choice
            if (choiceDir == undefined || choiceDir == null || choiceDir.length < 1) {
                var currentDefaultValue = $(this).parents(".parameter").find(".defaultValue").val();

                var defaultValueSelect = $("<select name='p_defaultvalue' class='defaultValue'/>");
                defaultValueSelect.append("<option value=''></option>");
                var defaultValueFound = false;
                for (var t = 0; t < choicelistArray.length; t++) {
                    var result = choicelistArray[t].split("=");
                    if (result[0] == "") {
                        continue;
                    }

                    defaultValueSelect.append("<option value='" + result[0] + "'>" + result[0] + "</option>");

                    if (result[0] == currentDefaultValue) {
                        defaultValueSelect.val(result[0]);
                        defaultValueFound = true;
                    }
                }

                if (!defaultValueFound && currentDefaultValue != undefined
                    && currentDefaultValue != null && currentDefaultValue != "") {
                    invalidDefaultValueFound = true;
                }

                $(this).parents(".parameter").find(".defaultValue").after(defaultValueSelect);
                prevDefaultField.remove();

                $(this).parents(".parameter").find(".defaultValue").multiselect(
                    {
                        header: false,
                        multiple: false,
                        selectedList: 1,
                        position: {
                            my: 'left bottom',
                            at: 'left top'
                        }
                    }
                );
            }
        }
        else {
            if (choiceDir == undefined || choiceDir == null || choiceDir.length < 1) {
                $(this).parents(".parameter").find(".choicelink").text("add a drop down list");
            }

            prevDefaultField.after("<input name='p_defaultvalue' class='defaultValue' size='40'/>");
            prevDefaultField.remove();
        }
    });

    specifyChoicesRow.find("input[name='choiceDir']").change(function () {
        $(this).parents(".parameter").find(".dynamicChoicesInfo").text("");

        var ftpDir = $(this).parents(".parameter").find("input[name='choiceDir']").val();
        if (ftpDir != null && ftpDir != undefined && ftpDir.length > 0) {
            //change text of the create drop down link to edit
            $(this).parents(".parameter").find(".choicelink").text("edit drop down list");

            var newFtpDir = ftpDir;
            if (newFtpDir.length > 60) {
                newFtpDir = newFtpDir.substring(0, 20) + "..." + newFtpDir.substring(newFtpDir.length - 20, newFtpDir.length);
            }
            $(this).parents(".parameter").find(".dynamicChoicesInfo").append("Dynamic directory URL: "
                + newFtpDir);
        }
        else {
            if ($(this).parents(".parameter").find("input[name='choicelist']").val() == undefined
                || $(this).parents(".parameter").find("input[name='choicelist']").val() == null
                || $(this).parents(".parameter").find("input[name='choicelist']").val().length < 1) {
                $(this).parents(".parameter").find(".choicelink").text("add a drop down list");
            }
        }
    });

    typeDetailsTable.append(specifyChoicesRow);

    //add row to allow setting of number of allowed values
    var specifyMinValuesRow = $("<tr class='minValues'/>");
    var specifyMinValuesTd = $("<td/>");
    specifyMinValuesTd.append('Minimum # of values: ');
    var minNumValues = $('<input name="minNumValues" value="0"/>');
    specifyMinValuesTd.append(minNumValues);
    minNumValues.spinner({
        min: 0,
        incremental: true,
        change: function (event, ui) {
            setDirty(true);
        },
        stop: function (event, ui) {
            var value = $(this).val();
            //check if this value is a number
            if(!$.isNumeric($(this).val()))
            {
                alert("Invalid value: " + value + ". Value must be numeric.");
                $(this).val("");
                throw new Error("Invalid value: " + value + ". Value must be numeric.");
            }
        }

    });
    specifyMinValuesRow.append(specifyMinValuesTd);
    typeDetailsTable.append(specifyMinValuesRow);

    var helpImgSrc = $(".helpbutton").first().attr("src");
    specifyMinValuesTd.append("<a href='createhelp.jsp#multipleInputs' target='help'> " +
        " <img src='" + helpImgSrc + "' width='12' height='12' alt='help' class='buttonIcon' />"
        + "</a>");

    var specifyMaxValuesRow = $("<tr class='maxValues'/>");
    var specifyMaValuesTd = $("<td/>");
    specifyMaValuesTd.append('Maximum # of values: ');
    var maxValues = $('<input name="maxNumValues" value="1"/>');

   var spinchange = function maxValChange(element)
   {
       var value = $(element).val();
       //check if this value is a number
       if(value !== "" && !$.isNumeric(value))
       {
           alert("Invalid value: " + value + ". Value must be numeric.");
           $(element).val("");
           throw new Error("Invalid value: " + value + ". Value must be numeric.");
       }

       //display the list mode row if max values is greater than 1
       if(value !== "" && value > 1)
       {
           $(element).parents("table").first().find("select[name='p_list_mode']").multiselect('enable');
       }
       else
       {
           $(element).parents("table").first().find("select[name='p_list_mode']").multiselect('disable');
       }
   };

    specifyMaValuesTd.append(maxValues);
    maxValues.spinner({
        min: 1,
        incremental: true,
        change: function (event, ui) {
            setDirty(true);

            spinchange(this);
        },
        stop: function (event, ui) {
            var value = $(this).val();
           spinchange(this);
        }
    });


    var unlimitedValues = $('<input name="unlimitedNumValues" type="checkbox" />');
    unlimitedValues.click(function () {
        if ($(this).is(":checked")) {
            $(this).parent("td").find("input[name='maxNumValues']").spinner("disable");
            $(this).parents("table").first().find("select[name='p_list_mode']").multiselect('enable');

        }
        else {
            $(this).parent("td").find("input[name='maxNumValues']").spinner("enable");
            $(this).parents("table").first().find("select[name='p_list_mode']").multiselect('disable');
        }
    });
    specifyMaValuesTd.append(unlimitedValues);
    specifyMaValuesTd.append("unlimited");
    specifyMaxValuesRow.append(specifyMaValuesTd);
    typeDetailsTable.append(specifyMaxValuesRow);

    if(value == "Input File") {
        //specify file grouping
        var specifyGroupsRow = $("<tr class='fileGroups'/>");

        var editFileGroupLink = $("<a href='#' class='fileGroupsLink'>add file groups</a>");

        editFileGroupLink.click(function (event) {
            event.preventDefault();

            var editFileGroupDialog = $(this).parents(".parameter").find(".editFileGroupDialog");

            editFileGroupDialog.empty();

            editFileGroupDialog.dialog({
                autoOpen: true,
                height: 330,
                width: 600,
                title: "Specify File Groups",
                create: function(event){
                    var table = $("<table>" +
                        "<tr><td>Minimum number of groups: </td>" +
                        "<td> <input type='text' name='minNumFileGroups' value='1'/> </td></tr>" +
                        "<tr><td>Maximum number of groups: </td>" +
                        "<td><input type='text' name='maxNumFileGroups' value='1'/>"+
                        "<label><input type='checkbox' name='unlimitedFileGroups'/>unlimited</label></td></tr>" +
                        "<tr><td>Group label: </td>" +
                        "<td> <input type='text' name='groupColumnLabel'/> </td></tr>" +
                        "<tr><td>Number of files per group must match: </td>" +
                        "<td> <select type='checkbox' name='fileGroupsMatch'> " +
                        "<option value='true'>yes</option>" +
                        "<option value='false'>no</option> </select></td></tr>" +
                        "<tr><td>File label (optional):</td>" +
                        "<td> <input type='text' name='fileColumnLabel'/> </td></tr>" +
                        "</table>");

                    table.find("input[name='minNumFileGroups']").spinner({
                        min: 0,
                        spin: function(event, ui)
                        {
                            if(ui.value == 0)
                            {
                                $(this).parents("table").find("input[name='maxNumFileGroups']").spinner("disable");
                                $(this).parents("table").find("input[name='groupColumnLabel']").prop("disabled", true);
                                $(this).parents("table").find("input[name='fileColumnLabel']").prop("disabled", true);
                                $(this).parents("table").find("select[name='fileGroupsMatch']").prop("disabled", true);
                            }
                            else
                            {
                                $(this).parents("table").find("input[name='maxNumFileGroups']").spinner("enable");
                                $(this).parents("table").find("input[name='groupColumnLabel']").prop("disabled", false);
                                $(this).parents("table").find("input[name='fileColumnLabel']").prop("disabled", false);
                                $(this).parents("table").find("select[name='fileGroupsMatch']").prop("disabled", false);
                            }

                            var maxNumFileGroups = $(this).parents("table").find("input[name='maxNumFileGroups']").spinner("value");
                            if(ui.value > maxNumFileGroups)
                            {
                                $(this).parents("table").find("input[name='maxNumFileGroups']").spinner("value", ui.value);
                            }
                        }
                    });

                    table.find("input[name='maxNumFileGroups']").spinner({
                        min: 1
                    });

                    table.find("input[name='unlimitedFileGroups']").click(function()
                    {
                        //disable the maximum file group
                        if($(this).is(":checked"))
                        {
                            $(this).parents("table").find("input[name='maxNumFileGroups']").spinner("disable");
                        }
                        else
                        {
                            $(this).parents("table").find("input[name='maxNumFileGroups']").spinner("enable");
                        }
                    });

                    table.find("select[name='fileGroupsMatch']").multiselect(
                    {
                        header: false,
                        multiple: false,
                        selectedList: 1,
                        minWidth: 145
                    });

                    var minNumGroups = element.parents(".parameter").data("minNumGroups");
                    if(parseInt(minNumGroups) !== 0 && minNumGroups !== undefined && minNumGroups !== null)
                    {
                        table.find("input[name='minNumFileGroups']").spinner("value", minNumGroups);

                        var maxNumGroups = element.parents(".parameter").data("maxNumGroups");
                        var groupColumnLabel = element.parents(".parameter").data("groupColumnLabel");
                        var fileColumnLabel = element.parents(".parameter").data("fileColumnLabel");
                        var fileGroupsMatch = element.parents(".parameter").data("fileGroupsMatch");

                        if(maxNumGroups !== undefined && maxNumGroups !== null) {
                            table.find("input[name='maxNumFileGroups']").spinner("value", maxNumGroups);
                        }
                        else
                        {
                            table.find("input[name='unlimitedFileGroups']").prop('checked', true);
                            table.find("input[name='maxNumFileGroups']").spinner("disable");
                        }

                        if(groupColumnLabel !== undefined && groupColumnLabel !== null) {
                            table.find("input[name='groupColumnLabel']").val(groupColumnLabel);
                        }

                        if(fileColumnLabel !== undefined && fileColumnLabel !== null) {
                            table.find("input[name='fileColumnLabel']").val(fileColumnLabel);
                        }
                        if(fileGroupsMatch !== undefined && fileGroupsMatch !== null && fileGroupsMatch == true) {
                            table.find("select[name='fileGroupsMatch']").val("true");
                        }
                    }

                    $(this).append(table);
                },
                buttons: {
                    "OK": function ()
                    {
                        var minNumGroups = $(this).find("input[name='minNumFileGroups']").val();
                        var maxNumGroups = $(this).find("input[name='maxNumFileGroups']").val();
                        var groupColumnLabel = $(this).find("input[name='groupColumnLabel']").val();
                        var fileColumnLabel = $(this).find("input[name='fileColumnLabel']").val();
                        var fileGroupsMatch = $(this).find("select[name='fileGroupsMatch']").val();

                        if(groupColumnLabel === "")
                        {
                            createErrorMsg("File Group Error", "A group column label must be specified");
                        }
                        else
                        {
                            //check if number of groups is unlimited
                            if($(this).find("input[name='unlimitedFileGroups']").is(":checked"))
                            {
                                maxNumGroups = -1;
                            }

                            element.parents(".parameter").data("minNumGroups", minNumGroups);
                            element.parents(".parameter").data("maxNumGroups", maxNumGroups);
                            element.parents(".parameter").data("groupColumnLabel", groupColumnLabel);
                            element.parents(".parameter").data("fileColumnLabel", fileColumnLabel);
                            element.parents(".parameter").data("fileGroupsMatch", fileGroupsMatch);

                            if(parseInt(minNumGroups) === 0)
                            {
                                element.parents(".parameter").find(".fileGroupsLink").text("add file groups");
                                element.parents(".parameter").removeData("maxNumGroups");
                                element.parents(".parameter").removeData("groupColumnLabel");
                                element.parents(".parameter").removeData("fileColumnLabel");
                                element.parents(".parameter").removeData("fileGroupsMatch");
                            }
                            else
                            {
                                element.parents(".parameter").find(".fileGroupsLink").text("edit file groups");
                            }

                            $(this).dialog("destroy");
                        }
                    },
                    "Cancel": function () {
                        $(this).dialog("destroy");
                    }
                },
                close: function()
                {
                    $(this).dialog("destroy");
                }

            });
        });

        $("<td/>").append(editFileGroupLink).appendTo(specifyGroupsRow);
        typeDetailsTable.append(specifyGroupsRow);

        var helpImgSrc = $(".helpbutton").first().attr("src");
        editFileGroupLink.parent().append("<a href='createhelp.jsp#fileGroup' target='help'> " +
            " <img src='" + helpImgSrc + "' width='12' height='12' alt='help' class='buttonIcon' />"
            + "</a>");

    }
    else
    {
        var listModeRow = $("<tr class='listMode'/>");
        var listModeTd = $("<td/>");
        listModeRow.append(listModeTd);
        listModeTd.append("List mode: ");
        var listMode = $("<select name='p_list_mode'>\
                <option value='cmd'>List</option>\
                <option value='cmd_opt'>Get-opt style list</option>\
            </select> ");

        listModeTd.append(listMode);
        listMode.multiselect({
            header: false,
            multiple: false,
            noneSelectedText: "Specify list mode",
            selectedList: 1, // 0-based index
            position: {
                my: 'left bottom',
                at: 'left top'
            }
        });
        listMode.multiselect('disable');

        listModeTd.append('<a href="createhelp.jsp#listMode" target="help">'
           + '<img src="/gp/css/frozen/modules/styles/images/help_small.gif" width="12" height="12" alt="help" class="helpbutton" /></a>');
        typeDetailsTable.append(listModeRow);

        //add a row for specifying the range if this is numeric parameter
        var  numRangeRow= $("<tr class='range'/>");
        var rangeTd = $("<td/>");
        numRangeRow.append(rangeTd);
        rangeTd.append("Range: <br/>");
        var minRangeField = $("<input class='minRange' name='p_minRange' type='text' style='width: 90px;'>");
        minRangeField.change(function () {
            //check whether needs to be an integer
            var format = $(this).parents(".parameter"). find("select[name='p_format'] option:selected").val();
            var maxRange = parseFloat($(this).parents(".parameter"). find("input[name='p_maxRange']").val());
            var minRange = parseFloat($(this).val());

            if($(this).val().length > 0)
            {
                if($.isNumeric(minRange))
                {
                    if(format == "Integer" && Math.floor(minRange) != minRange)
                    {
                        $(this).val("");
                        alert("The parameter type is an integer but the value " + minRange + " is not");
                        return;
                    }

                    //check that min is less than max
                    if(maxRange != undefined && maxRange != null && minRange > maxRange)
                    {
                        $(this).val("");
                        alert("Min range must be less than max range");
                    }
                }
                else
                {
                    $(this).val("");
                    alert("The range value must be a number");
                }
            }
        });

        $("<label>Min: </label>").append(minRangeField).appendTo(rangeTd);

        var maxRangeField = $("<input class='maxRange' name='p_maxRange' type='text' style='width: 90px;'>");
        maxRangeField.change(function () {
            //check whether needs to be an integer
            var format = $(this).parents(".parameter"). find("select[name='p_format'] option:selected").val();
            var minRange = parseFloat($(this).parents(".parameter"). find("input[name='p_minRange']").val());
            var maxRange = parseFloat($(this).val());

            if($(this).val())
            {
                if($.isNumeric(maxRange))
                {
                    if(format == "Integer" && Math.floor(maxRange) != maxRange)
                    {
                        $(this).val("");
                        alert("The parameter type is an integer but the value " + maxRange + " is not");
                        return;
                    }

                    //check that min is less than max
                    if(minRange != undefined && minRange != null && minRange > maxRange)
                    {
                        $(this).val("");
                        alert("Max range must be greater than min range");
                    }
                }
                else
                {
                    $(this).val("");
                    alert("The range value must be a number");
                }
            }
        });

        $("<label style='margin-left: 10px;'>Max: </label>").append(maxRangeField).appendTo(rangeTd);

        rangeTd.append('<a href="createhelp.jsp#Range" target="help">'
            + '<img src="/gp/css/frozen/modules/styles/images/help_small.gif" width="12" height="12" alt="help" class="helpbutton" /></a>');

        typeDetailsTable.append(numRangeRow);

        //hide by default
        numRangeRow.hide();

    }
}

function validateParamGroupsEditor(silent){
	var valid = true;
	try {
		var errorString = "";
		
		var jsonString = $("#param_groups_editor").val();
		if (jsonString.length == 0) return true;
		// first make sure its valid json
		var jsonArr = JSON.parse(jsonString);
		var allMentionedParams = new Array();
		// next make sure each group has a name and parameters appear only once
		for (var i=0; i < jsonArr.length; i++){
			var aGroup = jsonArr[i];
			var aGroupName = aGroup["name"];
			if (aGroupName == null) errorString += "Group " + i + " does not define a group name (empty string is allowed).\r\n ";
			
			var paramList = aGroup["parameters"];
			if (paramList != null){
				for  (var j=0; j < paramList.length; j++){
					var duplicate = $.inArray(paramList[j], allMentionedParams) > -1;
					if (duplicate) {
						errorString += "<br/> " + paramList[j] +" appears in more than 1 group. ";
					} else {
						allMentionedParams.push(paramList[j]);
					}
				}
			} else {
				// paramList is null so the group has no parameters
				//errorString += "Parameter group \"" + aGroupName +"\" does not contain any parameters. ";
				// we allow empty parameter groups as they can server as headings for 
				// the following parameter groups
			}
		} 
		// finally make sure there are no parameters mentioned that are missing
		// from the module or in the module but missing from the paramGroups file.
		var missingFromModuleParams = allMentionedParams;
		$(".parameter").each(function()   {
			var pname = $(this).find("input[name='p_name']").val();
			var present = $.inArray(pname, allMentionedParams) > -1;
			if (present) {
				// remove it so we have unmentioned left at the end
				missingFromModuleParams = missingFromModuleParams.filter(e => e !== pname);
				
			} else {
				errorString += "Parameter " + pname + " is not included in any group.\r\n ";
			}
		});
		if (missingFromModuleParams.length > 0){
			for (var i=0; i < missingFromModuleParams.length; i++){
				errorString += "Parameter " + missingFromModuleParams[i] + " is not defined in the module.\r\n ";
			}
		}
		
		
		if (errorString.length > 1) {
			if (!(silent == true)){
				alert(errorString);
			}
			return false;
		}
		
		return true;
		
	} catch (err) {
		alert(err);
		valid = false;
	}
	return valid;
}

function updatedefaultcontainers()
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/containersAndJobOptions",
        success: function(response) {
            var error = response["ERROR"];
            if (error !== undefined) {
                alert(error);
            }  else {
            	var containers =  $.parseJSON( response["containers"]);
            	
            	$("#dockerImageDefaults").empty();
            	for (var i in containers) {
            		if (containers[i].length > 0){
            			$("<option/>").html(containers[i]).appendTo("#dockerImageDefaults");
            		}
                    
                 }
            	
            	var memory =  $.parseJSON( response["job.memory"]);
            	
            	$("#jobMemoryDefaults").empty();
            	for (var i in memory) {
            		if (memory[i].length > 0){
            			$("<option value='"+ memory[i] + "'/>").appendTo("#jobMemoryDefaults");
            		}
                    
                 }
            	
            	var cpu =  $.parseJSON( response["job.cpuCount"]);
            	
            	$("#jobCpuDefaults").empty();
            	for (var i in cpu) {
            		if (cpu[i].length > 0){
            			$("<option value='"+ cpu[i] + "'/>").appendTo("#jobCpuDefaults");
            		}
                    
                 }
            	
            }
        },
        dataType: "json"});
    
}
    
function creatorValidateModuleCPU(){
	
    var jobCpuCount = $('input[name="jobCpuCount"]').val();
    if (jobCpuCount.trim().length == 0)  return;
	var x = document.getElementById("jobCpuDefaults");
	for (i = 0; i < x.options.length; i++) {
		 if (jobCpuCount == x.options[i].value) return;
    }
	alert("The default CPU selected, "+jobCpuCount+", is not one of the configured options for this GenePattern server.  It will work as intended but you should contact this server's administrator to see if adjusting the default CPU configuration is warranted.")
}    

function creatorValidateModuleMemory(){
	var jobMemory = $('input[name="jobMemory"]').val();
	if (jobMemory.trim().length == 0)  return;
	var x = document.getElementById("jobMemoryDefaults");
	for (i = 0; i < x.options.length; i++) {
        if (jobMemory == x.options[i].value) return;
    }
	alert("The default memory selected, "+jobMemory+", is not one of the configured options for this GenePattern server.  It will work as intended but you should contact this server's administrator to see if adjusting the default memory configuration is warranted.")
}

function updatemodulecategories()
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/categories",
        success: function(response) {
            var error = response["ERROR"];
            if (error !== undefined) {
                alert(error);
            }
            else {
                var categories = response["categories"].toLowerCase();
                categories = categories.substring(1, categories.length-1);

                var result = categories.split(", ");
                var catNames = new Set();
                
                
                var mcat = $("select[name='category']");

                for(var i=0;i < result.length;i++)
                {
                	var cat = result[i];
                	
                    if(cat !== "" && cat !== "Uncategorized"  && !(catNames.has(cat)))
                    {
                        mcat.append($("<option value='"  + result[i] + "'>" + escapeHTML(result[i]) + "</option>"));
                    }
                    catNames.add(cat);
                }
                mcat.multiselect("refresh");

                if(module_editor.moduleCategories.length > 0)
                {
                    mcat.val(module_editor.moduleCategories);
                    mcat.multiselect("refresh");
                }
            }
        },
        error: function(e){
        	console.log("Error getting categories");
        },
        
        dataType: "json"
    });
}

function escapeHTML(str)
{
    return str.replace(/[&"'<>]/g, function(c)
    {
        return {
            "&": "&amp;",
            '"': "&quot;",
            "'": "&apos;",
            "<": "&lt;",
            ">": "&gt;"
        }[c];
    });
}

function updatefileformats()
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/fileformats",
        success: function(response) {
            var error = response["ERROR"];
            if (error !== undefined) {
                alert(error);
            }
            else {
                var fileformats = response["fileformats"];

                var unique_fileformatlist = [];

                for(m=0;m < fileformats.length;m++)
                {
                    var index = unique_fileformatlist.indexOf(fileformats[m]);

                    if(index == -1)
                    {
                        unique_fileformatlist.push(fileformats[m]);
                    }
                }

                var result = unique_fileformatlist;

                var mcat = $("select[name='mod_fileformat']");

                for(i=0;i < result.length;i++)
                {
                    mcat.append($("<option>" + result[i] + "</option>"));
                }

                if(mcat.data("values") != undefined && mcat.data("values") != null)
                {
                    mcat.val(mcat.data("values"));
                }
                mcat.multiselect('refresh');
                $('#mod_fileformat').multiselectfilter();
                
                $("select[name='fileformat']").each(function()
                {
                    console.log("Adding loaded file formats to parameters");
                    var fileformat = $(this);
                    $('select[name="mod_fileformat"]').children("option").each(function()
                    {
                        fileformat.append("<option>" + $(this).val() + "</option>");
                    });

                    if(fileformat.data("fileformats") != null && fileformat.data("fileformats") != "")
                    {
                        fileformat.val(fileformat.data("fileformats"));
                    }

                    // extra multiselect() call due to race condition
                    fileformat.multiselect().multiselect("refresh").multiselectfilter();
                });
            }
        },
        dataType: "json"
    });
}

function addsectioncollapseimages()
{
    $(".heading").each(function()
    {
        if($(this).find(".imgcollapse").length == 0 || $(this).find(".imgexpand").length == 0)
        {
            var imagecollapse = $("<img class='imgcollapse' src='/gp/css/frozen/modules/styles/images/section_collapsearrow.png' alt='some_text' width='11' height='11'/>");
            var imageexpand = $("<img class='imgexpand' src='/gp/css/frozen/modules/styles/images/section_expandarrow.png' alt='some_text' width='11' height='11'/>");

            $(this).prepend(imageexpand);
            $(this).prepend(imagecollapse);

            $(this).children(".imgcollapse").toggle();

            $(this).next(".hcontent").data("visible", true);
        }
    });
}


function htmlEncode(value)
{
    return $('<div/>').text(value).html();
}


//permissions.put("isAdmin", taskContext.isAdmin());
//permissions.put("isLocalLSID", LSIDUtil.isAuthorityMine(lsid));
//permissions.put("allowUserEditNonLocalModules", gpConfig.getGPBooleanProperty(taskContext, "allowUserEditNonLocalModules", false));
//permissions.put("allowAdminEditNonLocalModules", gpConfig.getGPBooleanProperty(taskContext, "allowAdminEditNonLocalModules", false));


function setLsidDisplay(lsid){
	// all disabled by default
	$('#lsidAuthority').prop('disabled', true).addClass("disabledLsid");
	$('#lsidNamespace').prop('disabled', true).addClass("disabledLsid");
	$('#lsidId').prop('disabled', true).addClass("disabledLsid");
	$('#lsidVersion').prop('disabled', true).addClass("disabledLsid");
	
	// fill in the values
	lsidParts = lsid.split(":");
    $('#lsidAuthority').val(lsidParts[2]);
    $('#lsidNamespace').val(lsidParts[3]);
    $('#lsidId').val(lsidParts[4]);
    $('#lsidVersion').val(lsidParts[5]);
    
    // enable the full LSID in the save dialog to be editable if the user is an admin and (allowAdminEditNonLocalModules=true or isLocalLSID)
    // otherwise only enable the version
    
    if (moduleCreatorPermissions != null){
	    if ((moduleCreatorPermissions.isAdmin) && ((moduleCreatorPermissions.isLocalLSID) || (moduleCreatorPermissions.allowAdminEditNonLocalModules))){
	    	$('#lsidAuthority').prop('disabled', false).removeClass("disabledLsid");
	    	$('#lsidNamespace').prop('disabled', false).removeClass("disabledLsid");
	    	$('#lsidId').prop('disabled', false).removeClass("disabledLsid");
	    	$('#lsidVersion').prop('disabled', false).removeClass("disabledLsid");
	    } else if (moduleCreatorPermissions.allowUserEditNonLocalModules){
	    	// do we do anything different here?
	    }
    }
    // always allow an editor to override the module version
    $('#lsidVersion').prop('disabled', false).removeClass("disabledLsid");
    
    // now set the behavior so that if the LSID text fields are manually edited it changes
    // the LSID indicator to "no increment" since its unlikely a user would explicitly set the LSID
    // to a pre-incremental value and then save it
    function lsidKeyupFunction(){ 
    	$('select[name ="versionIncrement"]').val("noincrement"); 
    //	$("#versionIncrement").val("noincrement"); 
    }
    
    $('#lsidVersion').keyup(lsidKeyupFunction);
    
  
    
}

function loadModuleInfo(module)
{
    module_editor.lsid = module["LSID"];
    //$("#lsid").empty().append("LSID: " + module_editor.lsid);
    setLsidDisplay(module_editor.lsid)
    
    
    if(module["name"] !== undefined)
    {
        $('#modtitle').val(module["name"]);
    }

    var lsidMenu=new gpUtil.LsidMenu(module["LSID"], module["lsidVersions"]);
    if(module["lsidVersions"] !== undefined)
    {
        updateModuleVersions(lsidMenu);
    }
    //updateVersionIncrement(lsidMenu);
    
    if(module["description"] !== undefined)
    {
        $('textarea[name="description"]').val(module["description"]);
    }
    var hasDocUrl = ((module["documentationUrl"] !== undefined) && (module["documentationUrl"] !== ""));
    if(hasDocUrl)
    {
        $('documentationUrl').val(module["documentationUrl"]);
    }
    
    var hasDocFile = ((module["taskDoc"] !== undefined) && (module["taskDoc"] !== ""));
    if(hasDocFile)
    {
        var documentation = module["taskDoc"];
        module_editor.documentationfile = documentation;

        //keep track of files that are already part of this module
        module_editor.currentUploadedFiles.push(documentation);

        var currentDocumentationSpan = $("<span class='clear' id='currentDocumentationSpan'></span>");

        var delbutton = $('<button value="' + documentation + '">x</button>&nbsp;');
        delbutton.button().click(function()
        {
            //set this so that module will update version when save button is clicked
            setDirty(true);

            var fileName = $(this).val();

            var confirmed = confirm("Are you sure you want to delete the documentation file: " + fileName);
            if(confirmed)
            {
                module_editor.documentationfile = "";

                module_editor.filesToDelete.push(fileName);

                //remove display of uploaded license file
                $("#currentDocumentationSpan").remove();

                $("#documentationSpan").show();
            }
        });

        currentDocumentationSpan.append(delbutton);
        var documentationFileURL = "<a href=\"/gp/getTaskDoc.jsp?name=" + module_editor.lsid + "\" target=\"new\">" + htmlEncode(documentation) + "</a> ";
        currentDocumentationSpan.append(documentationFileURL);

        $("#documentationSpan").hide();
        $("#mainDocumentationDiv").prepend(currentDocumentationSpan);
        $("#deprecatedDocFileRow").show();
        $("#showDeprecatedDocFileRow").html("hide documentation file")
    }
    else
    {
        if (!hasDocUrl) module_editor.promptForTaskDoc = true;
        $("#deprecatedDocFileRow").hide();
    }

    if(module["author"] !== undefined)
    {
        var author = module["author"];
        if(author.indexOf(";") != -1)
        {
            var results = author.split(";");
            author = results[0];
            var organization = results[1];

            $('input[name="author"]').val(author);
            $('input[name="organization"]').val(organization);
        }
        else
        {
            $('input[name="author"]').val(module["author"]);
        }
    }

    if(module["privacy"] !== undefined)
    {
        $('select[name="privacy"]').val(module["privacy"]);
        $('select[name="privacy"]').multiselect("refresh");
    }

    if(module["quality"] !== undefined)
    {
        $('select[name="quality"]').val(module["quality"]);
        $('select[name="quality"]').multiselect("refresh");
    }

    if(module["version"] !== undefined)
    {
        $('input[name="comment"]').val(module["version"]);
    }
    
    if(module["job.docker.image"] !== undefined)
    {
        $('input[name="dockerImage"]').val(module["job.docker.image"]);
    }
    if(module["job.memory"] !== undefined)
    {
        $('input[name="jobMemory"]').val(module["job.memory"]);
        
    	if (module["job.memory"].length > 0){
    		$("<option value='"+ module["job.memory"] + "'/>").appendTo("#jobMemoryDefaults");
    	}
    }
    if(module["job.cpuCount"] !== undefined)
    {
        $('input[name="jobCpuCount"]').val(module["job.cpuCount"]);
        if (module["job.cpuCount"].length > 0){
    		$("<option value='"+ module["job.cpuCount"] + "'/>").appendTo("#jobCpuDefaults");
    	}
    }
    if(module["job.walltime"] !== undefined)
    {
        $('input[name="jobWalltime"]').val(module["job.walltime"]);
    }

    
    if(module["src.repo"] !== undefined)
    {
        $('input[name="src.repo"]').val(module["src.repo"]);
    }
    if(module["documentationUrl"] !== undefined)
    {
        $('input[name="documentationUrl"]').val(module["documentationUrl"]);
    }
    
    if(module["language"] !== undefined)
    {
        $('select[name="language"]').val(module["language"]);
        $('select[name="language"]').multiselect("refresh");

        if($('select[name="language"]').val() == null)
        {
            $('select[name="language"]').val("any");
            $('select[name="language"]').multiselect("refresh");
        }

        if(module["language"] == "Java")
        {
            $("select[name='c_type']").val(module["language"]);
            $("select[name='c_type']").multiselect("refresh");
            $("#commandtextarea textarea").data("type", "<java>");
        }
        if(module["language"] == "Perl")
        {
            $("select[name='c_type']").val(module["language"]);
            $("select[name='c_type']").multiselect("refresh");
            $("#commandtextarea textarea").data("type", "<perl>");
        }
    }

    if(module["os"] !== undefined)
    {
        $('input[name=os]').val(module["os"]);
    }

    if(module["categories"] !== undefined && module["categories"].length > 0)
    {
        module_editor.moduleCategories = module["categories"].toLowerCase().split(";");

        $("select[name='category']").val(module_editor.moduleCategories);
        $("select[name='category']").multiselect("refresh");
    }
    else if(module["taskType"] !== undefined)
    {
        module_editor.moduleCategories = module["taskType"].toLowerCase();

        $("select[name='category']").val(module["taskType"]);
        $("select[name='category']").multiselect("refresh");
    }

    if(module["cpuType"] !== undefined)
    {
        $("select[name='cpu']").val(module["cpuType"]);
        $("select[name='cpu']").multiselect("refresh");
    }

    if(module["commandLine"] !== undefined)
    {
        $('textarea[name="cmdtext"]').val(module["commandLine"]);

        var cmdtype = $("select[name='c_type']").val();
        if(cmdtype === "Custom" || cmdtype == null)
        {
            if(module["commandLine"].indexOf("<java>") != -1 &&
                module["commandLine"].indexOf("<java>") < 1)
            {
                $("select[name='c_type']").val("Java");
                $("select[name='c_type']").multiselect("refresh");
                $("#commandtextarea textarea").data("type", "<java>");
            }
            if(module["commandLine"].indexOf("<perl>") != -1 &&
                module["commandLine"].indexOf("<perl>") < 1)
            {
                $("select[name='c_type']").val("Perl");
                $("select[name='c_type']").multiselect("refresh");
                $("#commandtextarea textarea").data("type", "<perl>");
            }
        }
    }

    if(module["fileFormat"] !== undefined)
    {
        var fileformats = module["fileFormat"];
        fileformats = fileformats.split(";");
        $("select[name='mod_fileformat']").data("values", fileformats);
        $("select[name='mod_fileformat']").val(fileformats);
        $("select[name='mod_fileformat']").multiselect("refresh");
        $("#mod_fileformat").multiselectfilter();
    }

    if(module["license"] !== undefined && module["license"] !== "")
    {
        var license = module["license"];
        module_editor.licensefile = license;

        //keep track of files that are already part of this module
        module_editor.currentUploadedFiles.push(license);

        var currentLicenseDiv = $("<div class='clear' id='currentLicenseDiv'></div>");

        var delbutton = $('<button value="' + license + '">x</button>&nbsp;');
        delbutton.button().click(function()
        {
            //set this so that module will update version when save button is clicked
            setDirty(true);

            var fileName = $(this).val();

            var confirmed = confirm("Are you sure you want to delete the license file: " + fileName);
            if(confirmed)
            {
                module_editor.licensefile = "";

                module_editor.filesToDelete.push(fileName);

                //remove display of uploaded license file
                $("#currentLicenseDiv").remove();

                $("#licenseDiv").show();
            }
        });

        currentLicenseDiv.append(delbutton);
        var licenseFileURL = "<a href=\"/gp/getFile.jsp?task=" + module_editor.lsid + "&file=" + encodeURI(license) + "\" target=\"new\">" + htmlEncode(license) + "</a> ";
        currentLicenseDiv.append(licenseFileURL);

        $("#licenseDiv").hide();
        $("#mainLicenseDiv").append(currentLicenseDiv);

    }

    //store remaining task info attributes
    $.each(module, function(keyName, value) {
        //console.log("\nkeys: " + keyName);
        if(keyName != "fileFormat" && keyName != "commandLine" && keyName != "description"
            && keyName != "os" && keyName != "name" && keyName != "author" && keyName != "JVMLevel"
            && keyName != "LSID" && keyName != "lsidVersions" && keyName != "cpuType"
            && keyName != "privacy" && keyName != "language" && keyName != "version"
            && keyName != "job.docker.image"  
            && keyName != "job.cpuCount" && keyName != "job.memory"&& keyName != "job.walltime"
            && keyName != "src.repo" && keyName != "documentationUrl"
            && keyName != "supportFiles" && keyName != "categories" && keyName != "taskType"
            && keyName != "quality" && keyName != "license" && keyName != "taskDoc")
        {
            module_editor.otherModAttrs[keyName] = module[keyName];
        }
    });

    var supportFilesList = module["supportFiles"];
    if(supportFilesList !== undefined && supportFilesList != null &&  supportFilesList != "")
    {
        var currentFilesDiv = $("<div id='currentfiles'><div>");
        currentFilesDiv.append("Current Files (Check to delete): ");

        $("#supportfilecontent").prepend(currentFilesDiv);

        // var currentFilesSelect = $("<select name='currentfiles' multiple='multiple'></select>");
        supportFilesList = supportFilesList.split(";");
        for(s=0;s<supportFilesList.length;s++)
        {
            //do not show the module manifest in the list of support files
            if(supportFilesList[s] == "manifest")
            {
                continue;
            }

            module_editor.currentUploadedFiles.push(supportFilesList[s]);

            var checkbox = $('<input type="checkbox" name="currentfiles" value="' +
                supportFilesList[s] + '" />').click(function()
                {
                    var selectedVal = $(this).val();

                    if($(this).is(':checked'))
                    {
                        module_editor.filesToDelete.push(selectedVal);
                    }
                    else
                    {
                        //check the attribute in case deleting from the file list fails
                        this.setAttribute("checked", "checked");
                        this.checked = true;
                        //remove from delete file list
                        removeFileToDelete(selectedVal);

                        this.setAttribute("checked", ""); // For IE
                        this.removeAttribute("checked");
                        this.checked = false;
                    }
                });

            currentFilesDiv.append(checkbox);

            var currentFileURL = "<a href=\"/gp/getFile.jsp?task=" + module_editor.lsid + "&file=" + encodeURI(supportFilesList[s]) + "\" target=\"new\">" + htmlEncode(supportFilesList[s]) + "</a> ";
            currentFilesDiv.append(currentFileURL);
        }

        currentFilesDiv.append("<br>");

        currentFilesDiv.append("<br><br>");
    }
}

function loadParameterInfo(parameters)
{
    for(i=0; i < parameters.length;i++)
    {
        var newParameter = addparameter();
        newParameter.find("input[name='p_name']").val(parameters[i].name);
        newParameter.find("textarea[name='p_description']").val(parameters[i].description);

        var optional = parameters[i].optional;
        var prefix = parameters[i].prefix;

        if(parameters[i].flag !== undefined && parameters[i].flag !== null)
        {
            newParameter.find("input[name='p_flag']").val(parameters[i].flag);
            if(newParameter.find("input[name='p_flag']").val() != "")
            {
                newParameter.find("input[name='p_prefix']").removeAttr("disabled");
            }
        }

        if(optional.length > 0)
        {
            newParameter.find("input[name='p_optional']").attr('checked', true);
        }

        if(prefix !== undefined && prefix !== null && prefix.length > 0)
        {
            newParameter.find("input[name='p_prefix']").attr('checked', true);
            newParameter.find("input[name='p_flag']").val(prefix);
        }

        var pfileformat = parameters[i].fileFormat;

        var type = parameters[i].type;

        newParameter.find("select[name='p_type']").val("text");
        newParameter.find("select[name='p_type']").trigger("change");

        if(parameters[i].TYPE == "FILE" && parameters[i].MODE == "IN")
        {
            newParameter.find("select[name='p_type']").val("Input File");
            newParameter.find("select[name='p_type']").trigger("change");
        }

        if(type == "java.lang.Integer")
        {
            newParameter.find("select[name='p_format']").val("Integer");
            newParameter.find("select[name='p_format']").multiselect("refresh");
            newParameter.find("select[name='p_format']").trigger('change');
        }
        else if(type == "java.lang.Float")
        {
            newParameter.find("select[name='p_format']").val("Floating Point");
            newParameter.find("select[name='p_format']").multiselect("refresh");
            newParameter.find("select[name='p_format']").trigger('change');
        }
        else if(type == "PASSWORD")
        {
            newParameter.find("select[name='p_format']").val("Password");
            newParameter.find("select[name='p_format']").multiselect("refresh");
            newParameter.find("select[name='p_format']").trigger('change');
        }
        else if(type == "DIRECTORY")
        {
            newParameter.find("select[name='p_format']").val("Directory");
            newParameter.find("select[name='p_format']").multiselect("refresh");
            newParameter.find("select[name='p_format']").trigger('change');
        }
        else
        {
            newParameter.find("select[name='p_format']").val("String");
            newParameter.find("select[name='p_format']").multiselect("refresh");
            newParameter.find("select[name='p_format']").trigger('change');
        }

        newParameter.find(".defaultValue").val(parameters[i].default_value);

        if(pfileformat !== undefined && pfileformat != null && pfileformat.length > 0)
        {
            var pfileformatlist = pfileformat.split(";");
            newParameter.find("select[name='fileformat']").val(pfileformatlist);
            newParameter.find("select[name='fileformat']").data("fileformats", pfileformatlist);
            newParameter.find("select[name='fileformat']").multiselect('refresh');
        }

        var values = parameters[i].value;
        if(values !== undefined && values !== null && values.length > 0)
        {
            newParameter.find('input[name="choicelist"]').val(values);
            newParameter.find('input[name="choicelist"]').trigger("change");
        }

        var choices = parameters[i].choices;
        if(choices !== undefined && choices !== null && choices.length > 0)
        {

            newParameter.find('input[name="choicelist"]').val(choices);
        }

        var choiceDir = parameters[i].choiceDir;
        if(choiceDir !== undefined && choiceDir !== null && choiceDir.length > 0)
        {
            newParameter.find('input[name="choiceDir"]').val(choiceDir);
            newParameter.find('input[name="choiceDir"]').trigger("change");
            if(choices !== undefined && choices !== null && choices.length > 0)
            {
                newParameter.find('input[name="choicelist"]').trigger("change");
            }
        }

        var choiceDirFilter = parameters[i].choiceDirFilter;
        if(choiceDirFilter !== undefined && choiceDirFilter !== null && choiceDirFilter.length > 0)
        {
            newParameter.find('input[name="choiceDirFilter"]').val(choiceDirFilter);
            newParameter.find('input[name="choiceDirFilter"]').trigger("change");

            if(choices !== undefined && choices !== null && choices.length > 0)
            {
                newParameter.find('input[name="choicelist"]').trigger("change");
            }
        }

        if(parameters[i].minValue != undefined && parameters[i].minValue != null)
        {
            newParameter.find('input[name="minNumValues"]').spinner( "value", parameters[i].minValue);
        }

        if(parameters[i].maxValue != undefined && parameters[i].maxValue != null && parameters[i].maxValue != -1)
        {
            newParameter.find('input[name="maxNumValues"]').spinner("value", parameters[i].maxValue);
        }
        else
        {
            newParameter.find('input[name="maxNumValues"]').spinner( "disable" );
            newParameter.find('input[name="unlimitedNumValues"]').prop('checked', true);

            //enable the list mode since numValues is unlimited
            newParameter.find("select[name='p_list_mode']").multiselect('enable');
        }

        if(parameters[i].listMode !== undefined && parameters[i].listMode != null
            && parameters[i].listMode.length > 0 && newParameter.find("select[name='p_list_mode']").length > 0)
        {
            newParameter.find("select[name='p_list_mode']").val(parameters[i].listMode);
            newParameter.find("select[name='p_list_mode']").multiselect('refresh');
        }

        if(parameters[i].groupInfo != undefined && parameters[i].groupInfo !== null)
        {
            var groupInfo = parameters[i].groupInfo;
            if (groupInfo.minNumGroups != undefined && groupInfo.minNumGroups != null) {
                newParameter.data("minNumGroups", groupInfo.minNumGroups);
            }
            if (groupInfo.maxNumGroups != undefined && groupInfo.maxNumGroups != null) {
                newParameter.data("maxNumGroups", groupInfo.maxNumGroups);
            }
            if (groupInfo.groupColumnLabel != undefined && groupInfo.groupColumnLabel != null) {
                newParameter.data("groupColumnLabel", groupInfo.groupColumnLabel);
            }
            if (groupInfo.fileColumnLabel != undefined && groupInfo.fileColumnLabel != null) {
                newParameter.data("fileColumnLabel", groupInfo.fileColumnLabel);
            }

            if (groupInfo.numValuesMustMatch != undefined && groupInfo.numValuesMustMatch != null) {
                newParameter.data("fileGroupsMatch", groupInfo.numValuesMustMatch);
            }

            newParameter.find(".fileGroupsLink").text("edit file groups");
        }


        if(parameters[i].minRange != undefined && parameters[i].minRange != null && $.isNumeric(parameters[i].minRange))
        {
            newParameter.find('input[name="p_minRange"]').val(parameters[i].minRange);
        }

        if(parameters[i].maxRange != undefined && parameters[i].maxRange != null && $.isNumeric(parameters[i].maxRange))
        {
            newParameter.find('input[name="p_maxRange"]').val(parameters[i].maxRange);
        }

        var allAttrs = {};
        $.each(parameters[i], function(keyName, value) {
            allAttrs[keyName] = parameters[i][keyName];
        });

        newParameter.data("allAttrs",  allAttrs);
        updateparameter(newParameter, false);
    }

    if(invalidDefaultValueFound)
    {
        alert("Warning: Some parameters with invalid default drop-down parameters were found. " +
            "The default value of these parameters have been removed.");
    }
}

function loadModule(taskId)
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/load",
        data: { "lsid" : taskId },
        success: function(response) {
            var message = response["MESSAGE"];
            var error = response["ERROR"];
            var module = response["module"];

            if (error !== undefined && error !== null)
            {
                alert(error);

                if(error.indexOf("not editable") != -1)
                {
                    window.open("/gp/modules/creator.jsf", '_self');
                }
            }
            if (message !== undefined && message !== null) {
                alert(message);
            }

            if(response["docFileNames"] != null)
            {
                module_editor.docFileNames = response["docFileNames"];
            }
            // loadPermissions before loadModuleInfo !
            loadPermissions(response["Permissions"]);
            loadModuleInfo(response["module"]);
            loadParameterInfo(response["parameters"]);
            loadParameterGroups(response["ParamGroupsJson"]);
           
            // collapse the jobOptions to its initially closed
            $($("#joboptionheading").find(".imgexpand")[0]).trigger("click")
            
            setDirty(false);
            $(this).resize();
        },
        dataType: "json"
    });
}


/**
 * Cache user permissions and server configuration to address UI features to enable/disable
 */
var moduleCreatorPermissions = null;
function loadPermissions(permissionsObject){
	moduleCreatorPermissions = permissionsObject;
	
}



function loadParameterGroups(jsonArray, skipValidation){
	
	if (jsonArray != null){
		$("#param_groups_example_link").hide();
		$("#param_groups_editor").data('oldVal', JSON.stringify(jsonArray, null, 4))
		$("#param_groups_editor").val(JSON.stringify(jsonArray, null, 4));
		// display any added/missing params
		if (skipValidation != true)
			validateParamGroupsEditor();
		reorderParametersToMatchParamGroupsJson(false);
	} else {
		$("#param_groups_example_link").show();

	}
}


function getParametersJSON()
{
    var parameters = [];
    var pnum = 0;
    $(".parameter").each(function()
    {
        pnum = pnum +1;
        var pname = $(this).find("input[name='p_name']").val();
        var description = $(this).find("textarea[name='p_description']").val();
        var type = $(this).find("select[name='p_type'] option:selected").val();
        var default_val = $(this).find(".defaultValue").val();
        var optional = $(this).find('input[name="p_optional"]').is(':checked') ? "on" : "";
        var fileformatlist = "";
        var mode = "";
        var prefix = "";
        var flag = "";
        var listMode = "";

        if(!$(this).find('select[name="p_list_mode"]').is(":disabled") &&
            $(this).find('select[name="p_list_mode"]').val() !== undefined
            && $(this).find('select[name="p_list_mode"]').val() !== null)
        {
            listMode = $(this).find('select[name="p_list_mode"]').val();
        }

        if($(this).find('select[name="fileformat"]').val() !== undefined
            && $(this).find('select[name="fileformat"]').val() !== null)
        {
            var fileformat = $(this).find('select[name="fileformat"]').val();
            for(var f=0;f< fileformat.length;f++)
            {
                fileformatlist = fileformatlist + fileformat[f];
                if(f+1 < fileformat.length)
                {
                    fileformatlist = fileformatlist + ";";
                }
            }
        }

        if($(this).find("input[name='p_flag']").val() != undefined && $(this).find("input[name='p_flag']").val() !== null)
        {
            flag = $(this).find("input[name='p_flag']").val();
        }

        if(pname == undefined || pname == null || pname.length < 1)
        {
            saveError("A parameter name must be specified for parameter number " + pnum);
            throw("A parameter name is missing");
        }

        var parameter = {};

        var minValue = $(this).find('input[name="minNumValues"]').val();
        var maxValue = $(this).find('input[name="maxNumValues"]').val();
        if($(this).find('input[name="maxNumValues"]').is(':disabled'))
        {
            maxValue = -1;
        }

        if(maxValue != -1 && minValue > maxValue)
        {
            saveError("Maximum number of values must be greater than minimum number of " +
                "values for parameter " + pname);
            throw("Maximum number of values must be greater than minimum number of values for parameter " + pname);
        }

        parameter.minValue = minValue;
        parameter.maxValue = maxValue;

        var minRange = $(this).find('input[name="p_minRange"]').val();
        var maxRange = $(this).find('input[name="p_maxRange"]').val();

        if(minRange != undefined && minRange != null)
        {
            parameter.minRange = minRange;
        }

        if(maxRange != undefined && maxRange != null)
        {
            parameter.maxRange = maxRange;
        }

        //if this is an input file type
        if(type === "Input File")
        {
            mode = "IN";
            type = "FILE";

            //add file group info if available
            var minNumGroups = $(this).data("minNumGroups");
            var maxNumGroups = $(this).data("maxNumGroups");
            var fileGroupsMatch = $(this).data("fileGroupsMatch");
            var groupColumnLabel = $(this).data("groupColumnLabel");
            var fileColumnLabel = $(this).data("fileColumnLabel");

            if(minNumGroups !== undefined && minNumGroups !== null && minNumGroups !== 0)
            {
                parameter.minNumGroups = minNumGroups;

                if (maxNumGroups != -1 && minNumGroups > maxNumGroups) {
                    saveError("Maximum number of file groups must be greater than minimum number of " +
                        "file groups for parameter " + pname);
                    throw("Maximum number of file groups must be greater than minimum number of file groups for parameter " + pname);
                }
                parameter.maxNumGroups = maxNumGroups;

                if (groupColumnLabel !== undefined && groupColumnLabel !== null) {
                    parameter.groupColumnLabel = groupColumnLabel;
                }

                if (fileColumnLabel !== undefined && fileColumnLabel !== null) {
                    parameter.fileColumnLabel = fileColumnLabel;
                }

                if (fileGroupsMatch !== undefined && fileGroupsMatch !== null) {
                    parameter.groupNumValuesMustMatch = fileGroupsMatch.toString();
                }
            }
        }
        else
        {
            var format = $(this).find("select[name='p_format'] option:selected").val();
            if(format === "Directory")
            {
                type = "DIRECTORY";
            }
            else if(format === "Password")
            {
                type = "PASSWORD";
            }
            else if(format === "Integer")
            {
                type = "Integer";
            }
            else if(format === "Floating Point")
            {
                type = "Floating Point";
            }
            else
            {
                type = "TEXT";
            }
        }

        if(listMode != undefined && listMode != null && listMode.length > 0)
        {
            $.extend(parameter, { "listMode": listMode });
        }

        if($(this).find('input[name="p_prefix"]').is(":checked"))
        {
            prefix = $(this).find('input[name="p_flag"]').val();
        }

        $.extend(parameter,
            {"name": pname, "description": description, "TYPE": type,
            "default_value": default_val, "optional": optional,
            "fileFormat": fileformatlist, "MODE": mode, "prefix": prefix, "flag": flag
        });

        parameter["value"] = "";

        //there are choices defined
        if($(this).find('input[name="choicelist"]').val().length > 0)
        {
            if(type == "FILE")
            {
                parameter["choices"] = $(this).find('input[name="choicelist"]').val();
            }
            else
            {
                parameter["value"] = $(this).find('input[name="choicelist"]').val();
            }
        }

        if($(this).find('input[name="choiceDir"]').val().length > 0)
        {
            parameter["choiceDir"] = ($(this).find('input[name="choiceDir"]').val());
        }

        if($(this).find('input[name="choiceDirFilter"]').val().length > 0)
        {
            parameter["choiceDirFilter"] = ($(this).find('input[name="choiceDirFilter"]').val());
        }

        //add other remaining attributes
        var allAttrs = $(this).data("allAttrs");
        if (allAttrs !== undefined && allAttrs !== null) {
            $.each(allAttrs, function(keyName, value) {
                if($.inArray(keyName, Object.keys(parameter)) == -1
                    && keyName != "type" && keyName != "prefix_when_specified" && keyName != "choices"
                    && keyName != "choiceDir" && keyName != "choiceDirFilter" && keyName != "numValues"
                    && keyName != "numGroups" && keyName != "range" && keyName != "listMode" && keyName != "groupInfo" && keyName != "groupColumnLabel"
                    && keyName != "fileColumnLabel" && keyName != "groupNumValuesMustMatch")
                {
                    parameter[keyName] = allAttrs[keyName];
                    console.log("\nsaving unknown parameter attributes: " + keyName + "=" + allAttrs[keyName]);
                }
            });
        }

        parameters.push(parameter);
    });

    validateDefaultChoiceValues();

    return(parameters);
}


function saveAndUpload(runModule)
{
    if(saving)
    {
        return;
    }

    saving = true;

    $("#savingDialog").empty();

    $('<div/>').progressbar({ value: 100 }).appendTo("#savingDialog");

    $("#savingDialog").dialog({
        autoOpen: true,
        modal: true,
        height:130,
        width: 400,
        title: "Saving Module",
        open: function()
        {
            $(".ui-dialog-titlebar-close").hide();
        }
    });

    run = runModule;
    //if no support files need to be upload then skip upload file step
    if(module_editor.filestoupload.length == 0)
    {
        saveModule();
    }
    else
    {
        uploadAllFiles();
    }
}

function uploadAllFiles()
{
    if (module_editor.filestoupload.length)
    {
        var nextFile = module_editor.filestoupload.shift();

        uploadFile(nextFile);
    }
    else
    {
        saveModule();
    }
}

function validateDefaultChoiceValues()
{
    //validate default values for choice parameters during save
    var matchNotFoundParams = [];

    $(".parameter").each(function()
    {
        var choicelistObj  = $(this).find("input[name='choicelist']");
        var defaultValue = $(this).find(".defaultValue").val();

        if(defaultValue == undefined || defaultValue == null || defaultValue.length < 1)
        {
            return;
        }

        if(choicelistObj != undefined && choicelistObj != null)
        {
            var matchFound = false;
            var choicelist = choicelistObj.val();

            if(choicelist.length > 0)
            {
                //ignore default values for dynamic file choice parameters for now
                if($(this).find("input[name='choiceDir']").val() == undefined ||
                    $(this).find("input[name='choiceDir']").val() == null  ||
                    $(this).find("input[name='choiceDir']").val().length == 0)
                {

                    var choices = choicelist.split(';');
                    for(var i=0;i<choices.length;i++)
                    {
                        var rowdata = choices[i].split("=");
                        if(rowdata != undefined && rowdata != null && rowdata.length > 0)
                        {
                            if(rowdata[0] == defaultValue)
                            {
                                matchFound = true;
                            }
                        }
                    }
                    if(!matchFound)
                    {
                        matchNotFoundParams.push($(this).find("input[name='p_name']").val());
                    }
                }
            }

        }
    });


    if(matchNotFoundParams.length > 0)
    {
        saveError("The following parameters "+ matchNotFoundParams.join(", ") + " have default values that could not be found " +
            "in their drop-down list. The default values must be changed before saving.");
    }
}


	function isURL(str){
		   var a  = document.createElement('a');
		   a.href = str;
		   return (a.host && a.host != window.location.host);
		}


jQuery(document).ready(function() {

    $("input[type='text']").val("");

    addsectioncollapseimages();
    updatemodulecategories();
    updatefileformats();
    updatedefaultcontainers();

    //check if this is a request to edit an existing module
    editModule();
    
    
    $(".heading").live("click", function()
    {
        var visible = $(this).next(".hcontent").data("visible");
        //if first time then content is visible
        if(visible == undefined)
        {
            visible = true;
        }

        $(this).next(".hcontent").slideToggle(340);
        $(this).children(".imgcollapse:first").toggle();
        $(this).children(".imgexpand:first").toggle();

        //visibilty has changed to the opposite
        $(this).next(".hcontent").data("visible", !visible);
    });

    $(".hcontent").show();

    mainLayout = $('body').layout({

        //	enable showOverflow on west-pane so CSS popups will overlap north pane
        west__showOverflowOnHover: false

        //	reference only - these options are NOT required because 'true' is the default
        ,	closable:				true
        ,	resizable:				true	// when open, pane can be resized
        ,	slidable:				true	// when closed, pane can 'slide' open over other panes - closes on mouse-out

        //	some resizing/toggling settings
        ,	north__slidable:		false	// OVERRIDE the pane-default of 'slidable=true'
        ,	north__spacing_open:	0		// no resizer-bar when open (zero height)
        ,	north__spacing_closed:	20		// big resizer-bar when open (zero height)
        ,	south__spacing_open:	0

        ,	south__slidable:		false	// OVERRIDE the pane-default of 'slidable=true'
        //some pane-size settings
        ,	north__minHeight:		80
        ,	north__height:		    80
        ,	south__minHeight:		40
        ,	west__size:			    360
        ,	east__size:				300
        ,	south__size:		    34
        ,	center__minWidth:		100
        ,	useStateCookie:			true
    });

    mainLayout.allowOverflow("north");

    $( "#parameters" ).sortable(
        {
            change: function(event, ui)
            {
                setDirty(true);
            },
            start: function(event, ui)
            {
                ui.item.addClass("highlight");
            },
            stop: function(event, ui)
            {
                ui.item.removeClass("highlight");
            }
        });

    $( "#commandlist" ).sortable();

    $("#addone").button().click(function()
    {
        var paramDiv = addparameter();
        // for new param, add its default name to the command line
        paramDiv.find("input[name='p_name']").trigger("keyup");
    });

    $("#addparamnum").val("1");
    $("#addmultiple").button().click(function()
    {
        var numparams = $("#addparamnum").val();

        numparams = parseInt(numparams);
        if(isNaN(numparams))
        {
            alert("Please enter a number greater than 0");
            return;
        }

        //keep track of first parameter added so we can auto scroll to it
        var firstParameterDiv;
        for(var i=0;i<numparams;i++)
        {
            var parameterDiv = addparameter();
           
            // for new param, add its default name to the command line
            parameterDiv.find("input[name='p_name']").trigger("keyup");
            if(i==0)
            {
                firstParameterDiv = parameterDiv;
            }
        }

        var position = parseInt($(".ui-layout-center").scrollTop()) + parseInt(firstParameterDiv.position().top);

        $(".ui-layout-center").animate({
            scrollTop: position

        },2000);

    });
    
    $("#param_groups_example_link").click(function(){
		var pnames = new Array();	
		var skipValidation = false;
		
		$(".parameter").each(function()   {
			var pname = $(this).find("input[name='p_name']").val();
			pnames.push(pname);	});
		if (pnames.length == 0){
			pnames = ["p_1","p_2"];
			
			// if there are 0 parameters and they want example json, show them sone but do not 
			// alert the warning errors unless they try to save it later
			skipValidation = true;
		}
		var example = new Array();
		var group1 = new Object();
		group1["name"]= "Group One";
		group1["description"] = "Group one description.";
		group1["hidden"] = false;
		example.push(group1);
		
		if (pnames.length > 1){
			var pn2 = pnames.splice(pnames.length/2);
		
			group1["parameters"] = pnames;
		
			var group2 = new Object();
			group2["name"]="Group Two";
			group2["description"]= "Group two description.";
			group2["hidden"]=false;
			group2["parameters"]= pn2;
			
			example.push(group2);
		} else {
			group1["parameters"] = pnames;
		}
		
		loadParameterGroups(example, skipValidation);
    });
    
    
    $("#editgroups").button().click(function(){
    	
    	$('#groupEditorDialog').dialog({
    		title: "Edit Parameter Groups",
            autoOpen: true,
            height: 470,
            width: 450,
            buttons: {
            	"Validate": function(){
            		var isOK = validateParamGroupsEditor();
            		if (isOK){
            			alert("paramgroups.json appears to be valid.");
            		}
            	},
                "Save": function() {
                	
                	var isOK = validateParamGroupsEditor();
                	// error alert happens in the validate method
                	if (isOK){
                		// Save this as the new backstop
                		var prev = $("#param_groups_editor").data('oldVal');
                		reorderParametersToMatchParamGroupsJson(false);
                		if (! prev == $("#param_groups_editor").val()){
                			$("#param_groups_editor").data('oldVal', $("#param_groups_editor").val());
                			setDirty(true);
                		}
                    	$( this ).dialog( "close" );
                	}
                   
                },
                "Cancel": function() {
                	var resetToOldVal = $("#param_groups_editor").data('oldVal');
                	$("#param_groups_editor").val(resetToOldVal);
                	
                    $( this ).dialog( "close" );
                }
            },
            resizable: true
        });
    });
    

    $("#collapse_all_params").button().click(function()
    	    {
    	$(".delOptionsCollapsible").hide()
    	$(".parameter_minimized").find(".ui-button-text").html("+");
    	
    	$(".editChoicesDialog").hide();
    	$(".editFileGroupDialog").hide();
    	    });
    $("#expand_all_params").button().click(function()
    	    {
    	$(".delOptionsCollapsible").show()
    	$(".parameter_minimized").find(".ui-button-text").html("-");
    	
    	    });
    

    $("input[name='p_flag']").live("keyup", function()
    {
        var parameterParent = $(this).parents(".parameter");

        var p_prefix = parameterParent.find("input[name='p_prefix']");
        var p_flag = parameterParent.find("input[name='p_flag']");

        if(p_flag.val() !== undefined && p_flag.val() !== null)
        {
            if(p_flag.val() !== "")
            {
                p_prefix.removeAttr("disabled");
            }
            else
            {
                p_prefix.attr("disabled", true);
            }
        }
    });

    $("input[name='p_name'], input[name='p_flag']").live("keyup", function()
    {
        var parameterParent = $(this).parents(".parameter");

        updateparameter(parameterParent);
    });

    $("#parameters").on("click","input[name='p_prefix']", (function()
    {
        var parameterParent = $(this).parents(".parameter");

        updateparameter(parameterParent);
    }));


    $('#commandpreview').children().button().click(function()
    {
        var cmd_args = $('#commandlist').text();

        $("#commandtextarea textarea").attr("value", cmd_args);
        $("#commandtextarea").toggle();
    });

    //check for invalid chars ; and = in parameter choice list
    $("input[name='choicen'], input[name='choicev']").live("keyup", function()
    {
        if($(this).val().indexOf(";") != -1 || $(this).val().indexOf("=") != -1)
        {
            alert("The characters = and ; are not allowed");
            $(this).val("");
        }
    });

    // start with the warning hidden
    $('#forceLowerCategoryWarning').hide();
    $( "#addmodcategorydialog" ).dialog({
        autoOpen: false,
        height: 210,
        width: 330,
        buttons: {
            "OK": function() {
            	$('#forceLowerCategoryWarning').hide();
                var category = $("#newcategoryname").val().toLowerCase();
                var newcategory = $("<option>" +category + "</option>");

                var duplicate = false;
                $("select[name='category']").children("option").each(function(event)
                {
                    if(category == $(this).val())
                    {
                        duplicate = true;
                    }

                });

                if(!duplicate)
                {
                    $("select[name='category']").append(newcategory);
                    var categories = $("select[name='category']").val();

                    if(!categories)
                    {
                        categories = [];
                    }

                    categories.push(category);
                    $("select[name='category']").val(categories);
                    $("select[name='category']").multiselect("refresh");
                    $("#newcategoryname").val("");
                    $(this).dialog("close");
                }
                else
                {
                    createErrorMsg("Duplicate Category Error", "The category \"" + category + "\" already exists.");
                }
            },
            "Cancel": function() {
            	$('#forceLowerCategoryWarning').hide();
                $( this ).dialog( "close" );
            }
        },
        resizable: false
    });

    $("#addcategory").button().click(function()
    {
        $( "#addmodcategorydialog" ).dialog("open");
    });


    $( "#addfileformatdialog" ).dialog({
        autoOpen: false,
        height: 210,
        width: 330,
        buttons: {
            "OK": function() {
                var fileformat = $("#newfileformat").val();
                fileformat = trim(fileformat);

                $("#newfileformat").val("");
                if(fileformat != "")
                {
                    var newfileformat = $("<option value='" + fileformat + "'>" + fileformat + "</option>");

                    var exists = false;
                    //check if fileformat already exists
                    //append to parameter input file format
                    $("select[name='mod_fileformat']").children().each(function()
                    {
                        if($(this).val() == fileformat)
                        {
                            exists = true;
                        }
                    });

                    if(exists)
                    {
                        alert("The file format " + fileformat + " already exists");
                        return;
                    }


                    $("select[name='fileformat']").append(newfileformat);
                    $("select[name='fileformat']").multiselect("refresh");

                    //append to module output file format
                    var modnewfileformat = $("<option value='" + fileformat + "'>" + fileformat + "</option>");
                    $("select[name='mod_fileformat']").append(modnewfileformat);
                    $("select[name='mod_fileformat']").multiselect("refresh");
                    $("#mod_fileformat").multiselectfilter();
                }
                $( this ).dialog( "close" );
            },
            "Cancel": function() {
                $("#newfileformat").val("");
                $( this ).dialog( "close" );
            }
        },
        resizable: false
    });


    $("#addfileformat").button().click(function()
    {
        $( "#addfileformatdialog" ).dialog("open");
    });

    $("#viewparameter").button().click(function()
    {
        var listing = [];

        $("#commandlist").children("li").each(function()
        {
            listing.push($(this).text());
        });

        $("#commandlist").data("prevlisting", listing);

        $( "#clistdialog" ).dialog("open");
    });
    // JTL 083120 hide as its broken and prob unused
    $("#viewparameter").hide();
    

    $( "#clistdialog" ).dialog({
        autoOpen: false,
        height: 440,
        width: 340,
        buttons: {
            "OK": function()
            {
                var prev = $("#commandlist").data("prevlisting");

                var cur = [];
                $("#commandlist").children("li").each(function()
                {
                    cur.push($(this).text());
                });

                //Reorder the parameters in the command line
                if(prev !== cur)
                {
                    var cmdline = $("#commandtextarea textarea").val();

                    for(p=0; p <prev.length; p++)
                    {
                        cmdline = cmdline.replace(prev[p], "+++" + p + "***");
                    }

                    for(p=0;p<prev.length;p++)
                    {
                        cmdline = cmdline.replace("+++" + p + "***", cur[p]);
                    }
                }

                $("#commandtextarea textarea").val(cmdline);
                $( this ).dialog( "close" );
            }
        },
        resizable: true
    });

    $('#savedialogbtn').button().click(function()
    {
    	 $('#saveDialog').dialog({
    	        title: "Save Module",
    	        modal: true,
    	        width:862,
    	        height: 280,
    	        open: function(event, ui) { 
    	        	$('#lsidVersionComment').focus(); 
    	        	updateVersionIncrement(); 
    	        	$('#saveDialogWarningSpan').html("");
    	        	$('#saveDialogWarningLabel').hide();
    	        	// validate paramgroups, presence of docker container, files to be overwritten
    	        	// and display
    	        	if (! (validateParamGroupsEditor(true))){
    	        		var pg = $("<div>There is a problem with the parameterGroups json.</div>");
    	        		$('#saveDialogWarningSpan').append(pg);
    	        		$('#saveDialogWarningLabel').show();
    	        	}
    	        	var dockerImage = $('input[name="dockerImage"]').val();
    	            if (dockerImage == undefined || dockerImage == null || dockerImage.length < 1) {
    	            	var dd = $("<div>No docker image specified. The module will be run on the default genepattern/java-1.7 image.</div>");
    	        		$('#saveDialogWarningSpan').append(dd);
    	        		$('#saveDialogWarningLabel').show();
    	            }
    	            for(i=0;i<module_editor.filestoupload.length;i++) {
    	                var upLoadFileName = module_editor.filestoupload[i].name;
    	                if (module_editor.currentUploadedFiles.indexOf(upLoadFileName) >= 0){
    	                	var ff = $("<div>"+upLoadFileName+" will be overwritten.</div>");
        	        		$('#saveDialogWarningSpan').append(ff);
        	        		$('#saveDialogWarningLabel').show();
    	                }
    	            }
    	            
    	            
    	        },
    	        buttons: {
    	            "Save": function()
    	            {
    	            	 if(!isDirty())
    	                 {
    	                     alert("No changes to save");
    	                 }
    	                 else
    	                 {
    	                     saveAndUpload(false);
    	                    
    	                 }
    	            },
    	            "Save & Run": function()
    	            {
    	            	 //no changes detected so skip to run step
    	                if(!isDirty() && module_editor.lsid != "")
    	                {
    	                    runModule(module_editor.lsid);
    	                }
    	                else
    	                {
    	                    // save and then run the module
    	                    saveAndUpload(true);
    	                    
    	                }
    	            },
    	            // "Submit to GParc": function(){
    	            //     window.open("http://gparc.org");
    	            //     if (module_editor.lsid !== null && module_editor.lsid.length > 0) {
    	            //         window.location.href = "/gp/makeZip.jsp?name=" + encodeURIComponent(module_editor.lsid);
    	            //     }
    	            // },
    	            "Cancel": function(){
    	            	$(this).dialog("close");  
    	            	
    	            }
    	        }
    	    });
    	 
    });
    
  /*  $('#savebtn').button().click(function()
    {
        if(!isDirty())
        {
            alert("No changes to save");
        }
        else
        {
            saveAndUpload(false);
        }
    });

    $('#saveRunbtn').button().click(function()
    {
        //no changes detected so skip to run step
        if(!isDirty() && module_editor.lsid != "")
        {
            runModule(module_editor.lsid);
        }
        else
        {
            // save and then run the module
            saveAndUpload(true);
        }
    }); */

    $('#publishGParc').button().click(function() {
        window.open("http://gparc.org");
        if (module_editor.lsid !== null && module_editor.lsid.length > 0) {
            window.location.href = "/gp/makeZip.jsp?name=" + encodeURIComponent(module_editor.lsid);
        }
    });

    // $('#whatIsGparc').click(function(event) {
    //     showDialog("What is GParc?", '<a href="http://gparc.org"><img src="/gp/css/frozen/modules/styles/images/gparc.png" alt="GParc" style="margin-bottom: 10px;"'+
    //         '/></a><br /><strong>GParc</strong> is a repository and community where users can share and discuss their own GenePattern modules.'+
    //         '<br/><br/>Unregistered users can download modules and rate them.  Registered GParc users can:<ul><li>Submit modules</li>'+
    //         '<li>Download modules</li><li>Rate modules</li><li>Comment on modules</li><li>Access the GParc forum</ul>');
    //     if (event.preventDefault) event.preventDefault();
    //     if (event.stopPropagation) event.stopPropagation();
    // });


    $("select[name='c_type']").change(function()
    {
        var cmdlinetext = $("#commandtextarea textarea").val();
        var type = $(this).val();
        type = type.toLowerCase();

        var prev_cmd = $("#commandtextarea textarea").data("type");

        cmdlinetext = cmdlinetext.replace(prev_cmd, "");
        if(type == "java")
        {
            cmdlinetext = "<java>" + cmdlinetext;
        }
        else if(type == "perl")
        {
            cmdlinetext = "<perl>" + cmdlinetext;
        }


        $("#commandtextarea textarea").data("type", "<" + type +">");
        $("#commandtextarea textarea").val(cmdlinetext);
    });

    $(".licensefile").change(function()
    {

        if(this.files[0].type != "text/plain")
        {
            alert("ERROR: License file must be a text file");
            return;
        }

        if(this.files[0].size > 1024 * 1024 * 1024)
        {
            alert("ERROR: License file cannot be > 1GB");
            return;
        }

        //add to list of files to upload files
        addFileToUpload(this.files[0]);


        var delbutton = $('<button value="' + this.files[0].name + '">x</button>&nbsp;');

        delbutton.button().click(function()
        {
            //remove the license file from the list of files to upload
            removeFileToUpload($(this).val());

            module_editor.licensefile = "";

            //remove display of uploaded license file
            $("#licenseFileNameDiv").remove();

            //show the button to upload a new file
            $(".licensefile").parents("span").show();
        });

        module_editor.licensefile = this.files[0].name;

        var licenseFileNameDiv = $("<div id='licenseFileNameDiv' class='clear'>" + this.files[0].name
            + " (" + bytesToSize(this.files[0].size) + ")" +"</div>");
        licenseFileNameDiv.prepend(delbutton);

        //hide the button to upload a new file
        $(this).parents("span").hide();

        $("#licenseDiv").append(licenseFileNameDiv);

    });

    $(".documentationfile").change(function()
    {
        //add to list of files to upload files
        addFileToUpload(this.files[0]);

        var delbutton = $('<button value="' + this.files[0].name + '">x</button>&nbsp;');

        delbutton.button().click(function()
        {
            //remove the license file from the list of files to upload
            removeFileToUpload($(this).val());

            module_editor.documentationfile = "";

            //remove display of uploaded license file
            $("#documentationFileNameSpan").remove();

            //show the button to upload a new file
            $(".documentationfile").parents("span").show();
        });

        module_editor.documentationfile = this.files[0].name;

        var documentationFileNameSpan = $("<span id='documentationFileNameSpan' class='clear'>" + this.files[0].name
            + " (" + bytesToSize(this.files[0].size) + ")" +"</div>");
        documentationFileNameSpan.prepend(delbutton);

        $("#documentationSpan").after(documentationFileNameSpan);

        //hide the button to upload a new file
        $("#documentationSpan").hide();
    });
    $('input[name="documentationUrl"]').change(function() {
    	var url = $('input[name="documentationUrl"]').first().val();
    	if (!isURL(url)){
    		if (!url.startsWith("http")){
    			url = "https://"+url;
    			if (isURL(url)){
    				$('input[name="documentationUrl"]').first().val(url);
    			} else {
    				alert("Documentation URL does not appear to be a valid url." );
    			}
    		}
    		
    	}
    	
    });
    

    $(".supportfile").live("change", function()
    {
        for(var i=0;i<this.files.length;i++)
        {
            addToSupportFileList(this.files[i]);

        }

        //add a new file input field
        $(this).attr('name', "name" + module_editor.filestoupload.length + "[]");
        var parent = $(this).parent();
        parent.append('<input type="file" class="supportfile" multiple="multiple" >');
        $(this).detach();
    });

    $("select[name='mod_fileformat']").multiselect({
        header: true,
        noneSelectedText: "Specify output file formats",
        selectedList: 4 // 0-based index
    }).multiselectfilter();

   
    //$("#mod_fileformat").multiselect().multiselectfilter();
    
    $("select[name='category']").multiselect({
        header: true,
        selectedList: 1
    }).multiselectfilter();

    $("select[name='privacy'], select[name='quality'], " +
        "select[name='c_type'], select[name='cpu'], select[name='language'], select[name='modversion']").multiselect({
        multiple: false,
        header: false,
        selectedList: 1
    });

 //   $( "select[name='category']" ).multiselect().data( "multiselect" )._setButtonValue = function( value ) {
    	
 //   	this.buttonlabel.html( value );
 //   };

    $("#helpbtn").button().click(function()
    {
        window.open('createhelp.jsp#editingPropertiesHelp', '_blank');
    });

    $("#modtitle").change(function()
    {
        var modtitle = $("#modtitle").val();
        if(modtitle.indexOf(" ") != -1)
        {
            modtitle = modtitle.replace(/ /g, ".");
            $("#modtitle").val(modtitle);
        }
    });

    $("body").change(function()
    {
        setDirty(true);
    });

    //area for dropping support files
    var dropbox = document.getElementById("dropbox");

    // init event handlers
    dropbox.addEventListener("dragenter", dragEnter, true);
    dropbox.addEventListener("dragleave", dragLeave, true);
    dropbox.addEventListener("dragexit", dragExit, false);
    dropbox.addEventListener("dragover", dragOver, false);
    dropbox.addEventListener("drop", drop, false);

    //disable default browser behavior of opening files using drag and drop
    $(document).bind({
        dragenter: function (e) {
            e.stopPropagation();
            e.preventDefault();
            var dt = e.originalEvent.dataTransfer;
            dt.effectAllowed = dt.dropEffect = 'none';
        },
        dragover: function (e) {
            e.stopPropagation();
            e.preventDefault();
            var dt = e.originalEvent.dataTransfer;
            dt.effectAllowed = dt.dropEffect = 'none';
        }
    });
    
    $('#showDeprecatedDocFileRow').click(function(e){
    	$('#deprecatedDocFileRow').toggle(); 
    	$('#showDeprecatedDocFileRow').html(($('#showDeprecatedDocFileRow').text() == 'add documentation file') ? 'hide documentation file' : 'add documentation file');
   
    })
    
    // collapse the jobOptions to its initially closed
    $($("#joboptionheading").find(".imgexpand")[0]).trigger("click")
});

function dragEnter(evt)
{
    $("#dropbox").addClass("highlight");
    evt.stopPropagation();
    evt.preventDefault();
}

function dragLeave(evt)
{
    $("#dropbox").removeClass("highlight");
    evt.stopPropagation();
    evt.preventDefault();
}

function dragExit(evt)
{
    evt.stopPropagation();
    evt.preventDefault();
}

function dragOver(evt)
{
    evt.stopPropagation();
    evt.preventDefault();
}

function drop(evt)
{

    $("#dropbox").removeClass("highlight");
    evt.stopPropagation();
    evt.preventDefault();

    var files = evt.dataTransfer.files;
    var count = files.length;

    // Only call the handler if 1 or more files was dropped.
    if (count > 0)
        handleFiles(files);
}

function addFileToUpload(file)
{
    if(file.name == "manifest")
    {
        alert("You are not allowed to upload files with file name 'manifest'. Please re-name your file and try again.");
        throw("You are not allowed to upload files with file name 'manifest'. Please re-name your file and try again.");
    }

    for(i=0;i<module_editor.filestoupload.length;i++)
    {
        var upLoadFile = module_editor.filestoupload[i].name;
        if(upLoadFile === file.name)
        {
            alert("ERROR: The file " + file.name + " has already been specified for upload. Please remove the file first.")
            throw("ERROR: The file " + file.name + " has already been specified for upload. Please remove the file first.")
        }
    }

    //Now check if the file is in the list of current files in the module
    for(i=0;i<module_editor.currentUploadedFiles.length;i++)
    {
        console.log("current files: " + module_editor.currentUploadedFiles[i]);
        if(module_editor.currentUploadedFiles[i] == file.name)
        {
            //check if file was marked for deletion
            var index = jQuery.inArray(file.name, module_editor.filesToDelete);
            if(index == -1)
            {
                var isOk = confirm("Warning: The file " + file.name + " already exists in the module. " +
                    "The existing file will be overwritten.");
                if (!isOk) {                
                	throw("ERROR: The file" + file.name + " already exists in the module. " +
                    "Please remove the file first.");
                }
            }
        }
    }

    //if you make it here then this is a new file to upload
    module_editor.filestoupload.push(file);
}

function removeFileToUpload(fileName)
{
    var index = -1;
    for(var i=0;i<module_editor.filestoupload.length;i++)
    {
        var value = module_editor.filestoupload[i].name;
        if(value === fileName)
        {
            index = i;
        }
    }

    if(index == -1)
    {
        //do nothing, unable to find file in support listing
        alert("An error occurred while removing file: File not found");
        return;
    }

    module_editor.filestoupload.splice(index,1);
}

function removeFileToDelete(fileName)
{
    //check if file was re-uploaded as a new file
    var index = -1;
    for(var i=0;i<module_editor.filestoupload.length;i++)
    {
        var value = module_editor.filestoupload[i].name;
        if(value === fileName)
        {
            index = i;
        }
    }

    //file was not re-uploaded so it is ok to leave it in the module
    if(index == -1)
    {
        var fIndex = jQuery.inArray(fileName, module_editor.filesToDelete);
        module_editor.filesToDelete.splice(fIndex,1);
    }
    else
    {
        alert("ERROR: The file " + fileName + " was specified for upload. Please remove the file from upload and try again.")
        throw("ERROR: The file " + fileName + " was specified for upload. Please remove the file from upload and try again.")
    }
}

function addToSupportFileList(file)
{
    if(file.name == "manifest")
    {
        alert("You are not allowed to upload files with file name 'manifest'. Please re-name your file and try again.");
        return;
    }

    addFileToUpload(file);

    setDirty(true);

    var sfilelist = $("<li>" + file.name + " (" + bytesToSize(file.size) + ")" + "</li>");
    sfilelist.data("fname", file.name);

    var delbutton = $("<button>x</button>&nbsp;");
    delbutton.button().click(function()
    {
        var selectedFileObj = $(this).parent().data("fname");

        removeFileToUpload(selectedFileObj);
        $(this).parent().remove();
    });

    sfilelist.prepend(delbutton);

    $("#supportfileslist").append(sfilelist);
}

function handleFiles(files)
{

    for(var i=0;i<files.length;i++)
    {
        addToSupportFileList(files[i]);
    }
}

var result = document.getElementById('result');

// upload file
function uploadFile(file)
{
    var destinationUrl = "/gp/ModuleCreator/upload";
    // prepare XMLHttpRequest
    var xhr = new XMLHttpRequest();
    xhr.open('POST', destinationUrl);
    xhr.onload = function() {
        console.log("on load response: " + this.responseText);

        var response = $.parseJSON(this.responseText);
        module_editor.uploadedfiles.push(response.location);

        uploadAllFiles();
    };
    xhr.onerror = function() {
        result.textContent = this.responseText;
        console.log("response: " + this.responseText);
    };
    xhr.upload.onprogress = function(event) {
        console.log("upload progress");
        //handleProgress(event);
    }
    xhr.upload.onloadstart = function(event) {
        console.log("onload start support file upload");
    }

    // prepare FormData
    var formData = new FormData();
    formData.append('myfile', file);
    xhr.send(formData);
}

function hasDocFiles() {
    var uploads = module_editor.currentUploadedFiles.length;
    var hasLicense = module_editor.licensefile !== "";
    if (hasLicense) { uploads--; }
    return uploads > 0;
}
