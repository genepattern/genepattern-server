/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2012) by the
 Broad Institute. All rights are reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. The Broad Institute cannot be responsible for its
 use, misuse, or functionality.
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

function updateModuleVersions(lsids)
{
    if(lsids == undefined || lsids == null)
    {
        return;
    }

    var currentVersion = $('select[name="modversion"]').val();
    $('select[name="modversion"]').children().remove();
    var modVersionLsidList = lsids;
    for(v =0;v<modVersionLsidList.length;v++)
    {
        var versionnum = modVersionLsidList[v];
        var index = versionnum.lastIndexOf(":");
        if(index == -1)
        {
            alert("An error occurred while loading module versions.\nInvalid lsid: " + moduleVersionLsidList[v]);
        }
        var version = versionnum.substring(index+1, versionnum.length);
        var modversion = "<option value='" + versionnum + "'>" + version + "</option>";
        $('select[name="modversion"]').append(modversion);
        $('select[name="modversion"]').multiselect("refresh");
    }

    $('select[name="modversion"]').change(function()
    {
        var editLocation = "creator.jsf?lsid=" + $(this).val();
        window.open(editLocation, '_self');
    });

    $('select[name="modversion"]').val(currentVersion);
    $('select[name="modversion"]').multiselect("refresh");
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

    var privacy = $('select[name="privacy"] option:selected').val();
    var quality = $('select[name="quality"] option:selected').val();
    var language = $('select[name="language"] option:selected').val();
    var lang_version = $('input[name="lang_version"]').val();
    var os = $('input[name=os]:checked').val();
    var tasktype = $("select[name='category'] option:selected").val();
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

    var lsid = module_editor.lsid;
    var supportFiles = module_editor.uploadedfiles;
    var version = $('input[name="comment"]').val();
    var filesToDelete = module_editor.filesToDelete;

    var json = {};
    json["module"] = {"name": modname, "description": description,
        "author": author, "privacy": privacy, "quality": quality,
        "language": language, "JVMLevel": lang_version, "cpuType": cpu, "taskType": tasktype, "version": version,
        "os": os, "commandLine": commandLine, "LSID": lsid, "supportFiles": supportFiles,
        "filesToDelete": filesToDelete, "fileFormat": fileFormats, "license":licenseFile, "taskDoc":documentationFile};

    //add other remaining attributes
    $.each(module_editor.otherModAttrs, function(keyName, value) {
        console.log("\nsaving other module attributes: " + keyName + "=" + module_editor.otherModAttrs[keyName]);
        json.module[keyName] = module_editor.otherModAttrs[keyName];
    });

    json["parameters"] = getParametersJSON();

    //check if the doc was implicit and so know whether now we should prompt for the correct doc
    if(module_editor.promptForTaskDoc && (json["module"].taskDoc == undefined || json["module"].taskDoc == null
        || json["module"].taskDoc.length < 1))
    {
        var docPromptDialog = $("<div/>");
        //get list of all current support files that could be interpreted as doc
        if(module_editor.docFileNames != undefined && module_editor.docFileNames != null
            && module_editor.docFileNames.length > 0)
        {
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
            updateModuleVersions(versions);

            // Update the LSID upon successful save
            if (newLsid !== undefined && newLsid !== null)
            {
                $("#lsid").empty().append("LSID: " + newLsid);
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
                        buttons: {
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
}

function addparameter()
{
    var paramDiv = $("<div class='parameter'>  \
        <table class='deloptions'>\
        <tr> <td class='dragIndicator'></td>\
        <td class='btntd'>\
        <button class='delparam'>x Delete</button></td><td>\
        <p>Name*: <br/>\
        <input type='text' name='p_name' size='28'/>\
        </p><p>\
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
        </td></tr>\
        </table>\
        <div class='editChoicesDialog'/> \
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

    $('#parameters').append(paramDiv);

    paramDiv.find(".delparam").button().click(function()
    {
        //first remove the parameter from the commandline
        var pelement = $(this).parent().parent().find("input[name='p_name']");

        if(!confirm("Are you sure you want to delete this parameter?"))
        {
            return;
        }

        var felement = $(this).parent().parent().find("input[name='p_flag']");
        pelement.val("");
        felement.val("");

        updateparameter($(this).parent().parent());

        $(this).parents("div:first").remove();

        setDirty(true);
    });

    paramDiv.find("select[name='p_type']").trigger("change");

    return paramDiv;
}

function addtocommandline(flag, name, prevflag, prevname)
{
    var text = "";

    if (flag == "" && name == "" && prevflag ==undefined && prevname == undefined)
    {
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
    if(prevname !== "")
    {
        prevtext = "&lt;" + prevname + "&gt;";
    }

    if(prevflag !== "")
    {
        prevtext = prevflag + prevtext;
    }

    //if no change in value do nothing
    if(prevtext == text)
    {
        return;
    }

    //look for child with matching old parameter value and replace with new parameter value
    var found = false;

    var cmdline= $("#commandtextarea textarea").val();
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

    // if old parameter value was not found then this must be a new parameter so
    // insert it into parameter list
    if(text !== "")
    {
        $('#commandlist').append(item);

        //if argument is already in command which will occur if this is
        // a module edit
        if(cmdline.indexOf(decodedText) == -1)
        {
            cmdline += " " + decodedText;
            $("#commandtextarea textarea").val(cmdline);
        }
    }
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

function changeParameterType(element)
{
    element.multiselect("refresh");

    if(!element.parent().next().children().is("input[name='p_prefix']"))
    {
        element.parent().next().remove();
    }

    var value = element.val();
    element.parents(".pmoptions").parent().find(".textFieldData").remove();

    var fieldDetailsTd = $("<td/>");
    if(value == "Input File")
    {
        fieldDetailsTd.append("File format: <br/>");

        var fileFormatList = $('<select multiple="multiple" name="fileformat"></select>');
        var fileFormatButton = $('<button id="addinputfileformat">New</button>');

        fileFormatButton.button().click(function()
        {
            $( "#addfileformatdialog" ).dialog("open");
        });

        //copy option values from the modules output file format list that was generated earlier
        $('select[name="mod_fileformat"]').children("option").each(function()
        {
            fileFormatList.append("<option>" + $(this).val() + "</option>");
        });

        fieldDetailsTd.append(fileFormatList);
        fieldDetailsTd.append(fileFormatButton);

        fileFormatList.multiselect({
            header: false,
            noneSelectedText: "Specify input file formats",
            selectedList: 4, // 0-based index
            position:
            {
                my: 'left bottom',
                at: 'left top'
            }
        });
    }
    else
    {
        var dataTypeTd = $("<td class='textFieldData'/>");
        dataTypeTd.append("Type of data to enter*: <br/>");
        var formatList = $("<select name='p_format'>\
                <option value='String'>Text</option>\
                <option value='Integer'>Integer</option>\
                <option value='Floating Point'>Floating Point</option>\
                <option value='Directory'>Directory</option>\
                <option value='Password'>Password</option>\
            </select> ");
        formatList.change(function()
        {
            //hide choices info if this is a directory or password entry
            if($(this).val() == "Directory" || $(this).val() == "Password")
            {
                $(this).parents(".parameter:first").find(".choices").hide();
            }
            else
            {
                $(this).parents(".parameter:first").find(".choices").show();
            }
        });

        dataTypeTd.append(formatList);
        formatList.multiselect({
            header: false,
            multiple: false,
            noneSelectedText: "Specify text format",
            selectedList: 1, // 0-based index
            position:
            {
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

    var defaultValueRow = $("<tr/>");
    var defaultValue = $("<input type='text' name='p_defaultvalue' class='defaultValue' size='40'/>");
    $("<td/>").append("Default value:<br/>").append(defaultValue).append("<a href='createhelp.jsp#paramDefault' target='help'> " +
        " <img src='styles/images/help_small.gif' width='12' height='12' alt='help' class='buttonIcon' />"
        + "</a>").appendTo(defaultValueRow);
    typeDetailsTable.append(defaultValueRow);

    var specifyChoicesRow = $("<tr class='choices'/>");
    var editChoicesLink = $("<a href='#' class='choicelink'>add a drop-down list</a>");
    editChoicesLink.click(function(event)
    {
        event.preventDefault();

        var isFile = !($(this).parents(".parameter").find("select[name='p_type']").val() != "Input File");
        var choices = $(this).parents(".parameter").find("input[name='choicelist']").val();
        var pName = $(this).parents(".parameter").find("input[name='p_name']").val();
        var title =  "Create drop-down list";
        if(pName != null && pName != undefined && pName.length > 0)
        {
            pName =  pName.replace(/\./g, " ");

            title = "Edit Choices for "+ pName;
        }

        var editChoicesDialog = $(this).parents(".parameter").find(".editChoicesDialog");
        editChoicesDialog.empty();

        editChoicesDialog.dialog({
            autoOpen: true,
            height: 620,
            width: 600,
            title: title,
            create: function()
            {
                var enterValuesDiv = $("<div class='hcontent'/>");
                $(this).prepend(enterValuesDiv);

                var staticChoiceDiv = $("<div class='staticChoicesDiv'/>");
                enterValuesDiv.append(staticChoiceDiv);
                var choiceButton = $("<button class='choiceadd'>Add Menu Item</button>");
                choiceButton.button().click(function()
                {
                    var choicerow = $("<tr> <td> <div class='sortHandle'><div class='frictionBox'/><div class='frictionBox'/></div></td><td class='defaultChoiceCol'> <input type='radio' name='cradio'/></td>" +
                        "<td> <input type='text' name='choicev' class='choiceFields'/> </td>" +
                        "<td> <input type='text' name='choicen' class='choiceFields'/> </td>" +
                        "<td> <button> X </button></td></tr>");
                    choicerow.find("button").button().click(function()
                    {
                        $(this).parent().parent().remove();
                    });

                    choicerow.find("input[name='cradio']").click(function()
                    {
                        //check if this is the first item in the list
                        var firstListItem = $(this).parents("tr :first").index();
                        if(firstListItem == 1)
                        {
                            //this is the first data item in the list so allow it to be set as the default
                            return;
                        }

                        //check if the actual value is empty
                        var actualValue = $(this).parents("tr:first").find("input[name='choicev']").val();
                        if(actualValue == undefined || actualValue == null || actualValue.length < 1)
                        {
                            alert("Please either specify a value to pass on the command line or make this item the first selection" +
                                " in the list in order to make it the default");
                            $(this).removeAttr("checked");
                            $(this).parents(".editChoicesDialog").find("input[name='cradio']").first().click();
                        }
                    });

                    choicerow.find("input[name='choicev']").focusout(function()
                    {
                        //set the display value if it is empty
                        if($(this).val() != "")
                        {
                            if(choicerow.find("input[name='choicen']").val() == "")
                            {
                                var displayVal = $(this).val();
                                if(isFile)
                                {
                                    displayVal = displayVal.replace(/\/\//g, '');
                                    var url_split = displayVal.split("/");
                                    if(url_split.length > 1)
                                    {
                                        //get last item in parsed file url
                                        displayVal = url_split[url_split.length -1];
                                    }
                                }

                                choicerow.find("input[name='choicen']").val(displayVal);
                            }
                        }
                        else
                        {
                            //check if this was marked as the default and do not allow since
                            //the actual value is blank
                            if($(this).parents("tr :first").index() > 1 && choicerow.find("input[name='cradio']").is(":checked"))
                            {
                                alert("Please either specify a value to pass on the command line or make this item the first selection" +
                                    " in the list in order to make it the default");
                                $(this).parents(".editChoicesDialog").find("input[name='cradio']").first().click();
                            }
                        }
                    });

                    //if this is the only item in the list set it as the default
                    if($(this).parents(".editChoicesDialog").find(".staticChoiceTable").find("input[name='cradio']:checked").length == 0)
                    {
                        $(this).parents(".editChoicesDialog").find(".staticChoiceTable").find("input[name='cradio']:first").click();
                    }

                    $(this).parent().find("table").find("tbody").append(choicerow);

                    var choiceEntries = document.getElementsByClassName("choiceFields");

                    for (var y = 0; y < choiceEntries.length; ++y) {
                        var choiceEntry = choiceEntries[y];
                        choiceEntry.addEventListener("dragenter", function(evt)
                        {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, true);
                        choiceEntry.addEventListener("dragleave", function(evt)
                        {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, true);
                        choiceEntry.addEventListener("dragexit", function(evt)
                        {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, false);
                        choiceEntry.addEventListener("dragover", function(evt)
                        {
                            evt.stopPropagation();
                            evt.preventDefault();
                        }, false);
                        choiceEntry.addEventListener("drop", function(evt)
                        {
                            evt.stopPropagation();
                            evt.preventDefault();

                            console.log("choice entry drop");
                            if(evt.dataTransfer.getData('Text') != null
                                && evt.dataTransfer.getData('Text')  !== undefined
                                && evt.dataTransfer.getData('Text') != "")
                            {
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

                if(isFile)
                {
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
                    "</td> <td>"+
                    "<span class='staticTableHeader'>" + dValueColHeader + "</span>" +
                    "<br/>" +
                    "<span class='shortDescription'>" + dValueColHeaderDescription + "</span>" +
                    " </td> </tr> </thead><tbody></tbody></table>");

                staticChoiceDiv.prepend(table);

                table.find("tbody").sortable(
                {
                    placeholder: "ui-sort-placeholder",
                    forcePlaceholderSize: true,
                    start: function(event, ui)
                    {
                        ui.item.addClass("highlight");
                    },
                    stop: function(event, ui)
                    {
                        var element = ui.item;
                        element.removeClass("highlight");

                        //check if this item is set at as the default and its actual value is blank
                        if(element.find("input[name='cradio']").is(":checked")
                            && (element.find("input[name='choicev']").val() == undefined
                            || element.find("input[name='choicev']").val() == null
                            || element.find("input[name='choicev']").val().length < 1))
                        {
                            alert("You are not allowed to move a default selection with no command line " +
                                "value to any position except the first");
                            element.parents("tbody").sortable("cancel");
                        }
                    },
                    handle: ".sortHandle"
                });

                var result = choices.split(';');
                if(choices== "" || result == null  || result.length < 1)
                {
                    //start with two rows of data
                    choiceButton.click();
                    choiceButton.click();
                }
                else
                {
                    for(var i=0;i<result.length;i++)
                    {
                        var rowdata = result[i].split("=");

                        var displayValue = "";
                        var value = "";
                        if(rowdata.length > 1)
                        {
                            displayValue = rowdata[1];
                            value = rowdata[0];
                        }
                        else
                        {
                            value = rowdata[0];
                        }

                        choiceButton.click();

                        table.find("input[name='cradio']").last().removeAttr("disabled");

                        //check if this should be set as the default
                        if(value != "" && element.parents(".parameter").find(".defaultValue").val() == value)
                        {
                            table.find("input[name='cradio']:checked").removeAttr("checked");
                            table.find("input[name='cradio']").last().attr("checked", "checked");
                        }

                        table.find("input[name='choicev']").last().val(value);
                        table.find("input[name='choicen']").last().val(displayValue);
                    }
                }

                if(isFile)
                {
                    //type is file then display field to input url to retrieve
                    //files from
                    var choiceURLDiv = $("<div class='choicesURLDiv'/>");

                    var choiceURLTable = $("<table/>");
                    choiceURLDiv.append(choiceURLTable);
                    var choiceURLTableTR = $("<tr/>");
                    choiceURLTableTR.append("<td>Ftp directory:</td>");
                    var choiceURL = $("<input name='choiceURL' type='text' size='45'/>");
                    choiceURL.val(element.parents(".parameter").find("input[name='choiceDir']").val());
                    $("<td/>").append(choiceURL).append("<div class='shortDescription'>Enter the ftp directory " +
                        "containing the files to use to populate the drop-down list</div>").appendTo(choiceURLTableTR);
                    choiceURLTable.append(choiceURLTableTR);

                    //add filter box
                    var choiceURLTableFilterTR = $("<tr/>");
                    choiceURLTableFilterTR.append("<td>File filter:</td>");
                    choiceURLTable.append(choiceURLTableFilterTR);
                    var globDoc="Enter comma-separated list if one or more glob patterns (e.g. '*.gct') or anti-patterns (e.g. '!*.cls'). By default, 'readme.*' and '*.md5' files are ignored. "+
                        "By default, sub-directories are ignored. To include sub-directories instead of files set 'type=dir'. "+
                        "To include both files and directories set 'type=all'. The two can be combined (e.g. 'type=dir&hg*').";
                    var fileFilter = $("<input name='choiceURLFilter' type='text'/>");
                    fileFilter.val(element.parents(".parameter").find("input[name='choiceDirFilter']").val());
                    $("<td/>").append(fileFilter).append("<div class='shortDescription'>"+globDoc+"</div>").appendTo(choiceURLTableFilterTR);

                    var altStaticChoiceToggle = $("<input type='checkbox' class='staticChoiceLink'/>");
                    altStaticChoiceToggle.click(function(event)
                    {
                        $(this).parents(".editChoicesDialog").find(".staticChoicesDiv").toggle();

                        //hide the default value column
                        $(this).parents(".editChoicesDialog").find(".staticChoiceTable").find("tr").each(function()
                        {
                            var numElements = $(this).find("td").length;
                            if(numElements > 2)
                            {
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

                    if(choiceURL.val() != undefined && choiceURL.val() != null && choiceURL.val() != "")
                    {
                        $(this).find(".choicesURLDiv").show();
                        $(this).find(".staticChoicesDiv").hide();
                        if(choices != undefined && choices != null &&  choices.length > 1)
                        {
                            altStaticChoiceToggle.click();
                        }
                        dynamicChoiceButton.attr("checked", "checked");
                    }
                    else
                    {
                        $(this).find(".choicesURLDiv").hide();
                        $(this).find(".staticChoicesDiv").show();
                        staticChoiceButton.attr("checked", "checked");
                    }

                    dynamicChoiceButton.click(function()
                    {
                        if($(this).data("prevSelection") == "dynamic")
                        {
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

                    staticChoiceButton.click(function()
                    {
                        if($(this).data("prevSelection") == "static")
                        {
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
            close: function()
            {
                $( this ).dialog( "destroy" );
            },
            buttons: {
                "OK": function() {
                    var choicelist = "";
                    var newDefault = "";

                    if(!isFile || $(this).find(".staticChoice").is(":checked")
                        || ($(this).find(".dynamicChoice").is(":checked") && $(this).find(".staticChoiceLink").is(":checked")))
                    {
                        $(this).find(".staticChoiceTable").find("tr").each(function()
                        {
                            var dvalue = $(this).find("td input[name='choicen']").val();
                            var value = $(this).find("td input[name='choicev']").val();

                            if((dvalue == undefined && value == undefined)
                                || (dvalue == "" && value==""))
                            {
                                return;
                            }

                            if(choicelist !== "")
                            {
                                choicelist += ";";
                            }

                            if(dvalue == undefined || dvalue == null|| dvalue == "")
                            {
                                choicelist += value;
                            }
                            else
                            {
                                choicelist += value + "=" + dvalue;
                            }

                            //set default value
                            if($(this).find("input[name='cradio']").is(":checked"))
                            {
                                //set the default value
                                newDefault = $(this).find("input[name='choicev']").val();
                                newDefault = newDefault.trim();
                            }
                        });
                    }

                    var choiceURL = $(this).find("input[name='choiceURL']").val();
                    //set the dynamic url if there is any
                    if(choiceURL == undefined && choiceURL == null)
                    {
                        choiceURL = "";
                    }

                    if($(this).find(".dynamicChoice").is(":checked") && choiceURL.length < 1)
                    {
                        alert("Please enter an ftp directory or switch to a static drop-down list");
                        return;
                    }

                    element.parents(".parameter").find("input[name='choiceDir']").val(choiceURL);
                    element.parents(".parameter").find("input[name='choiceDir']").trigger("change");

                    //set the dynamic url filter if there is any
                    var choiceURLFilter = $(this).find("input[name='choiceURLFilter']").val();
                    //set the dynamic url if there is any
                    if(choiceURLFilter == undefined && choiceURLFilter == null)
                    {
                        choiceURLFilter = "";
                    }

                    element.parents(".parameter").find("input[name='choiceDirFilter']").val(choiceURLFilter);
                    element.parents(".parameter").find("input[name='choiceDirFilter']").trigger("change");


                    element.parents(".parameter").find("input[name='choicelist']").val(choicelist);
                    element.parents(".parameter").find("input[name='choicelist']").trigger("change");

                    //set default value
                    if(choiceURL == "" &&  choicelist.length > 0)
                    {
                        //element.parents(".parameter").find(".defaultValue").find("option:selected").removeAttr("selected");
                        element.parents(".parameter").find(".defaultValue").val(newDefault);

                        if(newDefault != "" && element.parents(".parameter").find(".defaultValue").val() != newDefault)
                        {
                            element.parents(".parameter").find(".defaultValue").append("<option value='" + newDefault + "'>" +
                                newDefault + "</option>");
                            element.parents(".parameter").find(".defaultValue").val(newDefault);
                        }

                        element.parents(".parameter").find(".defaultValue").multiselect("refresh");
                    }

                    $(this).dialog( "destroy" );
                },
                "Cancel": function()
                {
                    $(this).dialog( "destroy" );
                }
            },
            resizable: true
        });
    });

    $("<td/>").append(editChoicesLink).appendTo(specifyChoicesRow);

    editChoicesLink.parent().append("<a href='createhelp.jsp#paramType' target='help'> " +
        " <img src='styles/images/help_small.gif' width='12' height='12' alt='help' class='buttonIcon' />"
        + "</a>");

    editChoicesLink.parent().append("<div class='staticChoicesInfo'/>");
    editChoicesLink.parent().append("<div class='dynamicChoicesInfo'/>");

    //create hidden link for list of choices
    editChoicesLink.parent().append("<input type='hidden' name='choicelist'/>");

    //also create hidden fields for the ftp directory and file filter
    editChoicesLink.parent().append("<input type='hidden' name='choiceDir'/>");
    editChoicesLink.parent().append("<input type='hidden' name='choiceDirFilter'/>");

    editChoicesLink.parent().find("input[name='choicelist']").change(function()
    {
        var choicelist = $(this).parents(".parameter").find("input[name='choicelist']").val();
        $(this).parents(".parameter").find(".staticChoicesInfo").text("");

        var prevDefaultField = $(this).parents(".parameter").find(".defaultValue");
        var choiceDir = $(this).parents(".parameter").find("input[name='choiceDir']").val();

        if(choicelist != null && choicelist != undefined && choicelist.length > 0)
        {
            //change text of the create drop down link to edit
            $(this).parents(".parameter").find(".choicelink").text("edit drop down list");

            var choicelistArray = choicelist.split(";");
            if(choicelistArray.length > 0)
            {
                $(this).parents(".parameter").find(".staticChoicesInfo").append("Static list: " + choicelistArray.length + " items");
            }

            //change the default value field to a combo box if this is not a dynamic file choice
            if(choiceDir == undefined || choiceDir == null || choiceDir.length < 1)
            {
                var currentDefaultValue = $(this).parents(".parameter").find(".defaultValue").val();

                var defaultValueSelect = $("<select name='p_defaultvalue' class='defaultValue'/>");
                defaultValueSelect.append("<option value=''></option>");
                var defaultValueFound = false;
                for(var t=0;t<choicelistArray.length;t++)
                {
                    var result = choicelistArray[t].split("=");
                    if(result[0] == "" )
                    {
                        continue;
                    }

                    defaultValueSelect.append("<option value='" + result[0]+ "'>" + result[0]+ "</option>");

                    if(result[0] == currentDefaultValue)
                    {
                        defaultValueSelect.val(result[0]);
                        defaultValueFound = true;
                    }
                }

                if(!defaultValueFound && currentDefaultValue != undefined
                    && currentDefaultValue != null && currentDefaultValue != "")
                {
                    invalidDefaultValueFound = true;
                }

                $(this).parents(".parameter").find(".defaultValue").after(defaultValueSelect);
                prevDefaultField.remove();

                $(this).parents(".parameter").find(".defaultValue").multiselect(
                    {
                        header: false,
                        multiple: false,
                        selectedList: 1,
                        position:
                        {
                            my: 'left bottom',
                            at: 'left top'
                        }
                    }
                );
            }
        }
        else
        {
            if(choiceDir == undefined || choiceDir == null || choiceDir.length < 1)
            {
                $(this).parents(".parameter").find(".choicelink").text("add a drop down list");
            }

            prevDefaultField.after("<input name='p_defaultvalue' class='defaultValue' size='40'/>");
            prevDefaultField.remove();
        }
    });

    specifyChoicesRow.find("input[name='choiceDir']").change(function()
    {
        $(this).parents(".parameter").find(".dynamicChoicesInfo").text("");

        var ftpDir = $(this).parents(".parameter").find("input[name='choiceDir']").val();
        if(ftpDir != null && ftpDir != undefined && ftpDir.length > 0)
        {
            //change text of the create drop down link to edit
            $(this).parents(".parameter").find(".choicelink").text("edit drop down list");

            var newFtpDir = ftpDir;
            if(newFtpDir.length > 60)
            {
                newFtpDir = newFtpDir.substring(0, 20) + "..." + newFtpDir.substring(newFtpDir.length-20, newFtpDir.length);
            }
            $(this).parents(".parameter").find(".dynamicChoicesInfo").append("Dynamic directory URL: "
                + newFtpDir);
        }
        else
        {
            if($(this).parents(".parameter").find("input[name='choicelist']").val() == undefined
                || $(this).parents(".parameter").find("input[name='choicelist']").val() == null
                || $(this).parents(".parameter").find("input[name='choicelist']").val().length < 1)
            {
                $(this).parents(".parameter").find(".choicelink").text("add a drop down list");
            }
        }

    });

    typeDetailsTable.append(specifyChoicesRow);
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
                var categories = response["categories"];
                categories = categories.substring(1, categories.length-1);

                var result = categories.split(", ");
                var mcat = $("select[name='category']");

                for(i=0;i < result.length;i++)
                {
                    mcat.append($("<option value='"  + result[i] + "'>" + escapeHTML(result[i]) + "</option>"));
                }
                mcat.multiselect("refresh");
            }
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

                    fileformat.multiselect("refresh");
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

function loadModuleInfo(module)
{
    module_editor.lsid = module["LSID"];
    $("#lsid").empty().append("LSID: " + module_editor.lsid);

    if(module["name"] !== undefined)
    {
        $('#modtitle').val(module["name"]);
    }

    if(module["lsidVersions"] !== undefined)
    {
        updateModuleVersions(module["lsidVersions"]);
        $('select[name="modversion"]').val(module["LSID"]);
        $('select[name="modversion"]').multiselect("refresh");
    }

    if(module["description"] !== undefined)
    {
        $('textarea[name="description"]').val(module["description"]);
    }

    if(module["taskDoc"] !== undefined)
    {
        if(module["taskDoc"] !== "")
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
        }
    }
    else
    {
        module_editor.promptForTaskDoc = true;
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

    if(module["taskType"] !== undefined)
    {
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
        console.log("\nkeys: " + keyName);
        if(keyName != "fileFormat" && keyName != "commandLine" && keyName != "description"
            && keyName != "os" && keyName != "name" && keyName != "author" && keyName != "JVMLevel"
            && keyName != "LSID" && keyName != "lsidVersions" && keyName != "cpuType"
            && keyName != "privacy" && keyName != "language" && keyName != "version"
            && keyName != "supportFiles" && keyName != "taskType"
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

        if(type == "java.lang.Float")
        {
            newParameter.find("select[name='p_format']").val("Floating Point");
            newParameter.find("select[name='p_format']").multiselect("refresh");
            newParameter.find("select[name='p_format']").trigger('change');
        }

        if(type == "PASSWORD")
        {
            newParameter.find("select[name='p_format']").val("Password");
            newParameter.find("select[name='p_format']").multiselect("refresh");
            newParameter.find("select[name='p_format']").trigger('change');
        }

        if(type == "DIRECTORY")
        {
            newParameter.find("select[name='p_format']").val("Directory");
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

            loadModuleInfo(response["module"]);
            loadParameterInfo(response["parameters"]);
            setDirty(false);
            $(this).resize();
        },
        dataType: "json"
    });
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
        //this is an input file type
        if(type === "Input File")
        {
            mode = "IN";
            type = "FILE";
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

        if($(this).find('input[name="p_prefix"]').is(":checked"))
        {
            prefix = $(this).find('input[name="p_flag"]').val();
        }

        var parameter = {
            "name": pname, "description": description, "TYPE": type,
            "default_value": default_val, "optional": optional,
            "fileFormat": fileformatlist, "MODE": mode, "prefix": prefix, "flag": flag
        };

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
                    && keyName != "choiceDir" && keyName != "choiceDirFilter")
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

jQuery(document).ready(function() {

    $("input[type='text']").val("");

    addsectioncollapseimages();
    updatemodulecategories();
    updatefileformats();

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
        addparameter();
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

    $("input[name='p_name'], input[name='p_flag'], input[name='p_prefix']").live("change", function()
    {
        var parameterParent = $(this).parents(".parameter");

        updateparameter(parameterParent);
    });


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


    $( "#addmodcategorydialog" ).dialog({
        autoOpen: false,
        height: 210,
        width: 330,
        buttons: {
            "OK": function() {
                var category = $("#newcategoryname").val();
                var newcategory = $("<option>" +category + "</option>");
                $("select[name='category']").append(newcategory);
                $("select[name='category']").val(category);
                $("select[name='category']").multiselect("refresh");
                $( this ).dialog( "close" );
            },
            "Cancel": function() {
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

    $('#savebtn').button().click(function()
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
    });

    $('#publishGParc').button().click(function() {
        var token = null;
        var buttons = {
            "Submit to GParc": function() {
                $(this).dialog("close");

                var afterHTML = "<div><div style='width:100%;text-align:center;'><img id='gparcUploadProgress' style='height: 32px; margin: 10px;' src='/gp/images/runningJob.gif'/></div>" +
                    "<div id='gparcInfoText'>Uploading your module to GParc</div></div>";
                var afterButtons = {"Finalize on GParc": function() {
                    window.open(token);
                    $(this).dialog("close");
                }};

                showDialog("Uploading to GParc. Please Wait...", $(afterHTML), afterButtons);
                setTimeout(function() {
                    $(".ui-dialog-buttonset > button:visible").button("disable");
                }, 100);

                $.ajax({
                    type: "GET",
                    dataType: "json",
                    url: "/gp/ModuleCreator/gparc?lsid=" + module_editor.lsid,
                    success: function(response) {
                        if (response.token) {
                            token = response.token;
                            $("#gparcUploadProgress").attr("src", "/gp/images/checkbox.gif");
                            var successHTML = 'Your module has been uploaded to GParc. Please click the button below to log into GParc and finalize your submission.<br/><br/>' +
                                '<strong>Remember, in order to finish the submission you will need to a GParc account and will need to be logged in. This account is different ' +
                                'from your GenePattern account.</strong>' +
                                '<ul><li>To register for a GParc account <a href="http://www.broadinstitute.org/software/gparc/user/register" target="_blank" style="text-decoration: underline; color: #000099;">click here</a>.</li>' +
                                '<li>To log in to GParc <a href="http://www.broadinstitute.org/software/gparc/user" target="_blank" style="text-decoration: underline; color: #000099;">click here</a>.</li></ul>' +
                                'Once you have logged in, click the "Finalize on GParc" button below.</div>';
                            $("#gparcInfoText").html(successHTML);
                            setTimeout(function() {
                                $(".ui-dialog-buttonset > button:visible").button("enable");
                            }, 101);
                        }
                        else {
                            token = response.error;
                            $("#gparcUploadProgress").attr("src", "/gp/images/error.gif");
                            var successHTML = 'There was an error submitting your module to GParc. ' + token;
                            $("#gparcInfoText").text(successHTML);
                        }
                    },
                    error: function(error) {
                        alert(error);
                    }
                });
            }
        };
        var dialogHTML = '<div><a href="http://gparc.org"><img src="styles/images/gparc.png" alt="GParc" style="margin-bottom: 10px;" /></a><br />\
			<strong>GParc</strong> is a repository and community where users can share and discuss their own GenePattern modules.<br/><br/>';

        if (isDirty()) {
            dialogHTML += '<img src="styles/images/alert.gif" alt="Alert" /> <span style="color:red;">Changes to this module must be saved before it can be submitted to GParc.</span><br/><br/>';
        }

        if (!hasDocFiles()) {
            dialogHTML += '<img src="styles/images/alert.gif" alt="Alert" /> <span style="color:red;">This module does not have attached documentation.</span><br/><br/>';
        }

        dialogHTML += '<ol><li>To submit a module to GParc the module will need to have attached documentation.</li>' + 
        	'<li>Click the Submit to GParc button below and wait for your module to be uploaded.</li>' +
            '<li><strong>You will need a GParc account and will need to be logged in. This account is different from your GenePattern account.</strong>' +
            '<ul><li>To register for a GParc account <a href="http://www.broadinstitute.org/software/gparc/user/register" target="_blank" style="text-decoration: underline; color: #000099;">click here</a>.</li>' +
            '<li>To log in to GParc <a href="http://www.broadinstitute.org/software/gparc/user" target="_blank" style="text-decoration: underline; color: #000099;">click here</a>.</li></ul></li></ol></div>';
        if (!hasDocFiles() || isDirty()) {
            setTimeout(function() {
                $(".ui-dialog-buttonset > button:visible").button("disable");
            }, 100);
        }
        showDialog("Submit Module to GParc", $(dialogHTML), buttons);
    });

    $('#whatIsGparc').click(function(event) {
        showDialog("What is GParc?", '<a href="http://gparc.org"><img src="styles/images/gparc.png" alt="GParc" style="margin-bottom: 10px;"'+
            '/></a><br /><strong>GParc</strong> is a repository and community where users can share and discuss their own GenePattern modules.'+
            '<br/><br/>Unregistered users can download modules and rate them.  Registered GParc users can:<ul><li>Submit modules</li>'+
            '<li>Download modules</li><li>Rate modules</li><li>Comment on modules</li><li>Access the GParc forum</ul>');
        if (event.preventDefault) event.preventDefault();
        if (event.stopPropagation) event.stopPropagation();
    });


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
        header: false,
        noneSelectedText: "Specify output file formats",
        selectedList: 4 // 0-based index
    });

    $("select[name='category'], select[name='privacy'], select[name='quality'], " +
        "select[name='c_type'], select[name='cpu'], select[name='language'], select[name='modversion']").multiselect({
        multiple: false,
        header: false,
        selectedList: 1
    });

    $( "select[name='category']" ).multiselect().data( "multiselect" )._setButtonValue = function( value ) {
        this.buttonlabel.html( value );
    };

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
                alert("ERROR: The file" + file.name + " already exists in the module. " +
                    "Please remove the file first.");
                throw("ERROR: The file" + file.name + " already exists in the module. " +
                    "Please remove the file first.");
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