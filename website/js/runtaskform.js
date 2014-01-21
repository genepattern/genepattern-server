var field_types = {
    FILE: 1,
    CHOICE: 2,
    TEXT: 4,
    PASSWORD: 5
};

//contains info about the current selected task
var run_task_info = {
    lsid: null, //lsid of the module
    name: null, //name of the module
    params: {}, //contains parameter info necessary to build the job submit form, see the initParam() function for details
    sendTo: {}
};

//contains json object with parameter to value pairing
var parameter_and_val_groups = {};

//contains all the file upload requests
var fileUploadRequests = [];

//the field is used to assign unique ids to each file provided for a file input parameter
//in order to make it easier to delete files
var fileId = 0;
//contains the json of parameters received when loading a module
//saved so it can reused when for the reset operation

var Request = {
    parameter: function(name)
    {
        var result = this.parameters()[name];
        if(result != undefined && result != null)
        {
            return  decodeURIComponent(result);
        }

        return result;
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
    },

    cleanJobSubmit: null
};

function htmlEncode(value)
{
    if(value == undefined || value == null || value == "")
    {
        return value;
    }

    return $('<div/>').text(value).html();
}

//For those browsers that dont have it so at least they won't crash.
if (!window.console)
{
    window.console = { time:function(){}, timeEnd:function(){}, group:function(){}, groupEnd:function(){}, log:function(){} };
}

function loadModule(taskId, reloadId)
{
    var url = window.location.href;
    var getParameters = url.slice(url.indexOf('?') + 1);
    getParameters = getParameters.replace(Request.parameter("lsid"), taskId);

    // Remove the reloadJob parameter if it is no longer needed
    if (!reloadId) {
        getParameters = getParameters.split("&reloadJob=")[0];

    }

    var queryString = "?" + getParameters;

    $.ajax({
        type: "GET",
        url: "/gp/rest/RunTask/load" + queryString,
        cache: false,
        data: { "lsid" : taskId, "reloadJob":  reloadId},
        success: function(response) {

            var message = response["MESSAGE"];
            var error = response["ERROR"];

            if (error !== undefined && error !== null)
            {
                alert(error);
            }
            if (message !== undefined && message !== null) {
                alert(message);
            }

            if (response["module"] !== undefined &&
                response["module"] !== null
                && response["parameters"] != undefined
                && response["parameters"] !== undefined )
            {

                run_task_info.reloadJobId = reloadId;
                var module = response["module"];
                loadModuleInfo(module);

                //check if there are missing tasks (only applies to pipelines)
                if(module["missing_tasks"])
                {

                    $("#missingTasksDiv").append("<p class='errorMessage'>WARNING: This pipeline requires modules or module " +
                        "versions which are not installed on this server.</p>");
                    var installTasksButton = $("<button> Install missing tasks</button>");
                    installTasksButton.button().click(function()
                    {
                        window.location.replace("/gp/viewPipeline.jsp?name=" + run_task_info.lsid);
                    });

                    $("#missingTasksDiv").append(installTasksButton);

                    $(".submitControlsDiv").hide();

                    $("#javaCode").parents("tr:first").hide();
                    $("#matlabCode").parents("tr:first").hide();
                    $("#rCode").parents("tr:first").hide();

                }
                else if(module["private_tasks"])
                {
                    $("#missingTasksDiv").append("<p class='errorMessage'>WARNING: This pipeline includes tasks " +
                        "which you do not have permission to run on this server.</p>");
                    $(".submitControlsDiv").hide();

                    $("#javaCode").parents("tr:first").hide();
                    $("#matlabCode").parents("tr:first").hide();
                    $("#rCode").parents("tr:first").hide();
                }
                else if(module["eula"])
                {
                    clearEulas();
                    generateEulas(module["eula"]);
                    $("#eula-block").show();
                }
                else
                {
                    loadParameterInfo(response["parameters"], response["initialValues"]);
                }
                //the parameter form elements have been created now make the form visible
                $("#protocols").hide();
                $("#submitJob").show();

                // Update the history & title
                document.title = "GenePattern - " + run_task_info.name
                if (reloadId) {
                    var reloadJob = "&reloadJob=" + reloadId;
                }
                else {
                    var reloadJob = "";
                }

                // Update the history so "back" works
                history.pushState(null, document.title, location.protocol + "//" + location.host + location.pathname + "?lsid=" + module.LSID + reloadJob);

                // Update send-to-param options in file menus
                clearAllSendToParams();
                sendToMapToMenu();
            }
        },
        error: function(xhr, ajaxOptions, thrownError)
        {
            console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            console.log(thrownError);

            $("#submitJob").show();
            $("#submitJob").empty();
            $("#submitJob").append("An error occurred while loading the task " + taskId + ": <br/>" + xhr.responseText);
        },
        dataType: "json"
    });
}

function generateEulas(eula) {
    var block = $("#eula-block");

    $("<div></div>")
        .addClass("barhead-task")
        .append("<span>" + eula.currentTaskName + "</span>")
        .append($("<span>version " + eula.currentLsidVersion + "</span>")
            .addClass("license-version"))
        .appendTo(block);

    $("<div>LSID=" + eula.currentLsid + "</div>")
        .addClass("license-lsid")
        .appendTo(block);

    $("<h5>You must agree below to the following End-User license agreements before you can run " + eula.currentTaskName + ".</h5>")
        .addClass("license-center")
        .appendTo(block);

    $(eula.pendingEulas).each(function(index, item) {
        var eula = $("<div></div>")
            .addClass("eula")
            .appendTo(block);

        $("<div></div>")
            .addClass("barhead-license-task")
            .append("<span>" + item.moduleName + "</span>")
            .append($("<span>version " + item.moduleLsidVersion + "</span>")
                .addClass("license-version"))
            .appendTo(eula);

        $("<textarea></textarea>")
            .addClass("license-content")
            .attr("rows", 20)
            .attr("readonly", "readonly")
            .text(item.content)
            .appendTo(eula);
    });

    $("<div></div>")
        .addClass("license-center")
        .append($("<form></form>")
            .attr("name", "eula")
            .attr("action", eula.acceptUrl)
            .attr("method", eula.acceptType)
            .append($('<input type="hidden"></input>')
                .attr("name", "lsid")
                .attr("value", eula.currentLsid)
            )
            .append($("<input></input>")
                .attr("type", "hidden")
                .attr("name", "initialQueryString")
                .attr("value", "lsid=" + eula.currentLsid)
            )
            .append($("<input></input>")
                .attr("type", "hidden")
                .attr("name", "reloadJob")
                .attr("value", "")
            )
            .append($("<p>Do you accept all the license agreements?</p>")
                .addClass("license-agree-text")
            )
            .append($("<input></input>")
                .attr("type", "submit")
                .attr("value", "OK")
            )
            .append($("<input></input>")
                .attr("type", "button")
                .attr("onclick", "document.location='/gp/pages/index.jsf'")
                .attr("value", "Cancel")
            )
        )
        .appendTo(block);

        setTimeout(function() {
            $("#submitJob").hide();
        }, 10);
}

function clearEulas() {
    $("#eula-block").empty();
}

function clearAllSendToParams() {
    $(".send-to-param").remove();
}

function sendToMapToMenu() {
    var starts = $(".send-to-param-start");
    starts.each(function(index, start) {
        var kind = $(start).attr("data-kind");
        var url = $(start).attr("data-url");
        var params = run_task_info.sendTo[kind];
        if (params) {
            params = params.slice(0).reverse(); // clone the array and reverse it
            $(start).after($('<tr class="send-to-param"><td><img src="/gp/images/divider-pix2.gif" width="100%" height="1" /></td></tr>'));
            for (var i = 0; i < params.length; i++) {
                var param = params[i];
                $(start).after($("<tr class='send-to-param'><td><a href='#' onclick='setInputField(\"" + param + "\", \"" + url + "\");'>Send to " + param + "</a></td></tr>"));
            }
        }
    });
}

function loadModuleInfo(module)
{
    run_task_info.lsid = module["LSID"];
    run_task_info.name = module["name"];

    if(run_task_info.lsid == undefined)
    {
        throw("Unknown task LSID");
        return;
    }

    if(run_task_info.name == undefined)
    {
        throw("Unknown task name");
        return;
    }

    $("#task_name").prepend(run_task_info.name);

    //add version drop down
    if(module["lsidVersions"] !== undefined)
    {
        for(var v =0; v <module["lsidVersions"].length; v++)
        {
            var versionnum = module["lsidVersions"][v];
            var index = versionnum.lastIndexOf(":");
            if(index == -1)
            {
                alert("An error occurred while loading module versions.\nInvalid lsid: " + module["lsidVersions"][v]);
            }

            var version = versionnum.substring(index+1, versionnum.length);
            var modversion = "<option value='" + versionnum + "'>" + version + "</option>";
            $('#task_versions').append(modversion);

            if(module["lsidVersions"][v] == run_task_info.lsid)
            {
                $('#task_versions').val(versionnum).attr('selected',true);
            }
        }

        //if there is only one version then replace the drop down with text
        if(module["lsidVersions"].length == 1)
        {
            $("#task_versions").replaceWith("<span class='normal'>" + $('#task_versions option:selected').text() + "</span>");
        }

        //disabled until css for multiselect is improved
        /*$('#task_versions').multiselect(
         {
         header: false,
         multiple: false,
         selectedList: 1,
         classes: "multiselect"
         });*/

        $('#task_versions').change(function()
        {
            var changeTaskVersion = "index.jsf?lsid=" + $(this).val();
            window.open(changeTaskVersion, '_self');
        });
    }

    var isPipeline = module["taskType"] == "pipeline";
    var exportLink = "/gp/makeZip.jsp?name=" + run_task_info.lsid;
    $("#export").attr("href", exportLink);
    $("#export").data("pipeline", isPipeline);
    $("#export").click(function(e)
    {
        var isPipeline = $(this).data("pipeline");

        if(isPipeline)
        {
            e.preventDefault();
            //prompt the user if they want to include modules
            var dialog = $("<div><p>Press 'Include modules' to include all modules used by " +  run_task_info.name +
                " in the exported zip file. </p> <p>Press 'Pipeline only' to include only the " + run_task_info.name + " pipeline definition itself. </p></div>");
            dialog.dialog({
                title: "Export Pipeline",
                resizable: true,
                height: 210,
                width: 500,
                modal: true,
                buttons: {
                    "Include modules": function() {
                        window.open("/gp/makeZip.jsp?name=" + run_task_info.lsid + "&includeDependents=1");
                        $( this ).dialog( "close" );
                    },
                    "Pipeline only": function() {
                        window.open("/gp/makeZip.jsp?name=" + run_task_info.lsid);
                        $( this ).dialog( "close" );
                    },
                    "Cancel Export": function() {
                        $( this ).dialog( "close" );
                    }
                }
            });
        }


    });

    var propertiesLink = "/gp/addTask.jsp?name="+run_task_info.lsid+"&view=1";
    if(isPipeline)
    {
        propertiesLink = "/gp/viewPipeline.jsp?name="+run_task_info.lsid;
    }

    var propertiesLink = "/gp/addTask.jsp?name="+run_task_info.lsid+"&view=1";

    if(module["taskType"] == "pipeline")
    {
        propertiesLink = "/gp/viewPipeline.jsp?name="+run_task_info.lsid;
    }

    $("#properties").attr("href", propertiesLink);

    var hasDescription = false;
    if(module["description"] !== undefined
        && module["description"] != "")
    {
        $("#mod_description").append(module["description"]);
        hasDescription = true;
    }

    //if module has doc specified or if for some reason
    // the hasDoc field was not set then show the doc link
    if(module["hasDoc"] == undefined || module["hasDoc"])
    {
        var docLink = "/gp/getTaskDoc.jsp?name=" + run_task_info.lsid;
        $("#documentation").attr("href", docLink);
    }
    else
    {
        //otherwise hide the documentation link
        $("#documentation").hide();
    }


    if(module["editable"] != undefined && module["editable"])
    {
        var editLink = "/gp/modules/creator.jsf?lsid=" + run_task_info.lsid;

        if(module["taskType"] == "pipeline")
        {
            editLink = "/gp/pipeline/index.jsf?lsid=" + run_task_info.lsid;
        }

        $("#otherOptionsSubMenu table tbody").prepend("<tr><td><a href='JavaScript:Menu.denyIE(\"" + editLink + "\");' onclick='jq('.popupMenu').hide();'>Edit</a></td></tr>");
    }

    //add source info
    $("#source_info_tooltip").hide();

    if(module["source_info"] !== undefined && module["source_info"] != null)
    {
        var label = module["source_info"].label;
        var iconUrl = module["source_info"].iconUrl;
        var briefDescription = module["source_info"].briefDesc;
        var fullDescription = module["source_info"].fullDesc;

        var empty = true;
        if(label !== undefined && label !== '' && label !== null)
        {
            $("#source_info").append(label);
        }

        if(iconUrl !== undefined && iconUrl !== '' && iconUrl !== null)
        {
            $("#source_info").prepend("<img src='" + iconUrl + "' width='18' height='16' />");
            empty = false;
        }

        if(briefDescription !== undefined && briefDescription !== '' && briefDescription !== null)
        {
            $("#source_info_tooltip").append(briefDescription);
            $("#source_info").data("hasBriefDescription", true);
        }
        else
        {
            $("#source_info").data("hasBriefDescription", false);
        }

        if(fullDescription !== undefined && fullDescription !== '' && fullDescription !== null)
        {
            var readMoreLink = $("<a href='#'> Read More</a>");
            readMoreLink.click(function(event)
            {
                event.preventDefault();
                $("#source_info_tooltip").hide();

                showDialog(label + " Repository Details", fullDescription, "OK");

            });
            $("#source_info_tooltip").append(readMoreLink);
        }

        $("#source_info").hover(function(e)
        {
            $("#source_info_tooltip").css("position", "absolute");
            $("#source_info_tooltip").css("top", $("#source_info").position().top + 15);
            $("#source_info_tooltip").css("right", 20);

            setTimeout(function()
            {
                if($("#source_info").is(":hover"))
                {
                    if($("#source_info").data("hasBriefDescription"))
                    {
                        $("#source_info_tooltip").show();
                    }
                }
            }, 1150);
        });

        $("body").mousemove(function()
        {
            if(!$("#source_info_tooltip").is(":hover"))
            {
                $("#source_info_tooltip").hide();
            }
        });

        $("#source_info_tooltip").mouseleave(function() {
            $("#source_info_tooltip").hide();
        });

        $("#source_info_tooltip").focusout(function() {
            $("#source_info_tooltip").hide();
        });

        $("#source_info").prepend("Source: ");
    }
}

function setParamFieldType(parameterInfo)
{
    //set the field types of this parameter
    run_task_info.params[parameterInfo.name].type = [];
    var isFile = false;
    var isChoice = false;
    var isPassword = false;
    var isText = false;

    var allowCustomChoice = true;
    if(parameterInfo.choiceInfo != undefined  && parameterInfo.choiceInfo != null && parameterInfo.choiceInfo != '')
    {
        isChoice= true;

        //check if a custom choice is allowed
        if(!parameterInfo.choiceInfo.choiceAllowCustom)
        {
            allowCustomChoice = false;
        }

        run_task_info.params[parameterInfo.name].type.push(field_types.CHOICE);
        run_task_info.params[parameterInfo.name].choiceInfo = parameterInfo.choiceInfo;
    }

    //other types will be set if custom choice is allowed
    if(allowCustomChoice)
    {
        if(parameterInfo.TYPE == "FILE" && parameterInfo.MODE == "IN")
        {
            isFile = true;
            run_task_info.params[parameterInfo.name].type.push(field_types.FILE);
        }

        if(!isFile && !isChoice)
        {
            if(parameterInfo.type == "PASSWORD")
            {
                run_task_info.params[parameterInfo.name].type.push(field_types.PASSWORD);
            }
            run_task_info.params[parameterInfo.name].type.push(field_types.TEXT);
        }
    }
}

function setAllowMultipleValuesForParam(parameterInfo)
{
    run_task_info.params[parameterInfo.name]["allowMultiple"] = false;
    if(parameterInfo.maxValue == undefined
        || parameterInfo.maxValue == null
        || parseInt(parameterInfo.maxValue) != 1)
    {
        run_task_info.params[parameterInfo.name]["allowMultiple"] = true;
    }
}

function setParamOptionalOrRequired(parameterInfo)
{
    //check if this is a required parameter
    run_task_info.params[parameterInfo.name]["required"] = false;

    if(parameterInfo.optional.length == 0 || parameterInfo.minValue != 0)
    {
        run_task_info.params[parameterInfo.name]["required"] = true;
    }
}

function setParamDisplayName(parameterInfo)
{
    //set the display name
    run_task_info.params[parameterInfo.name]["displayname"] = parameterInfo.name;
    //use the alternate name if there is one (this is usually set for pipelines)
    if(parameterInfo.altName != undefined
        && parameterInfo.altName != null
        && parameterInfo.altName.replace(/ /g, '') != "") ////trims spaces to check for empty string
    {
        run_task_info.params[parameterInfo.name]["displayname"] = parameterInfo.altName;
    }

    //replace . with spaces for parameter display name
    run_task_info.params[parameterInfo.name]["displayname"] = run_task_info.params[parameterInfo.name]["displayname"].replace(/\./g,' ');
}

function initParam(parameterInfo, index, initialValues)
{
    run_task_info.params[parameterInfo.name] = {};

    //keep track of position of element in list
    run_task_info.params[parameterInfo.name].index = index;

    //set the display name for the parameter
    setParamDisplayName(parameterInfo);

    //set whether this parameter is required or not
    setParamOptionalOrRequired(parameterInfo);

    //set the type of input field to display
    setParamFieldType(parameterInfo);

    //check if more than one value can be assigned to this parameter
    setAllowMultipleValuesForParam(parameterInfo);

    //a flag to indicate whether the initial values are found in the drop down list if
    //this is a choice parameter
    run_task_info.params[parameterInfo.name].initialChoiceValues = false;

    run_task_info.params[parameterInfo.name].minValue = parameterInfo.minValue;
    run_task_info.params[parameterInfo.name].maxValue = parameterInfo.maxValue;
    run_task_info.params[parameterInfo.name].default_value = parameterInfo.default_value;
    run_task_info.params[parameterInfo.name].description = parameterInfo.description;
    run_task_info.params[parameterInfo.name].altDescription = parameterInfo.altDescription;
    run_task_info.params[parameterInfo.name].groupInfo = parameterInfo.groupInfo;
    run_task_info.params[parameterInfo.name].isBatch = false;

    // Add parameter to send-to map
    addSendToParam(parameterInfo);
}

function addSendToParam(parameterInfo) {
    if (parameterInfo.fileFormat) {
        var formats = parameterInfo.fileFormat.split(";");
        for (var i = 0; i < formats.length; i++) {
            var format = formats[i];
            if (run_task_info.sendTo[format] === undefined) {
                run_task_info.sendTo[format] = [];
            }
            run_task_info.sendTo[format].push(parameterInfo.name);
        }
    }
}

function createTextDiv(parameterName, groupId, initialValuesList)
{
    var textDiv = $("<div class='textDiv'/>");

    var paramDetails = run_task_info.params[parameterName];

    var textField = null;
    var isPassword = $.inArray(field_types.PASSWORD, run_task_info.params[parameterName].type) != -1;
    if(isPassword)
    {
        textField = $("<input type='password' class='pValue' />");
    }
    else
    {
        textField = $("<input type='text' class='pValue' />");
    }

    textField.data("pname", parameterName);
    // Handle link drags
    textField.get(0).addEventListener("dragenter", dragEnter, true);
    textField.get(0).addEventListener("dragleave", dragLeave, true);
    textField.get(0).addEventListener("dragexit", dragExit, false);
    textField.get(0).addEventListener("dragover", dragOver, false);
    textField.get(0).addEventListener("drop", function(event) {
        $(this).removeClass('runtask-highlight');
        var link = event.dataTransfer.getData('Text');
        $(this).val(link);

        //now trigger a change so that this value is added to this parameter
        $(this).trigger("change");
    }, true);

    textField.change(function()
    {
        var valueList = [];
        valueList.push($(this).val());

        var paramName = $(this).data("pname");

        var groupId = $(this).data("groupId");
        updateValuesForGroup(groupId, paramName, valueList);
    });
    textField.val(paramDetails.default_value);
    textField.data("groupId", groupId);

    var textValueList = [];

    if(textField.val() != "")
    {
        textValueList.push(textField.val());
    }

    updateValuesForGroup(groupId, parameterName, textValueList);

    if(paramDetails.required == 0 && paramDetails.minValue != 0)
    {
        textField.addClass("requiredParam");
    }

    //select initial values if there are any
    if( initialValuesList != undefined &&  initialValuesList != null)
    {
        var inputFieldValue = "";
        for(v=0; v <  initialValuesList.length; v++)
        {
            inputFieldValue += initialValuesList[v];

            // add a comma between items in this list
            if(v < ( initialValuesList.length-1))
            {
                inputFieldValue += ",";
            }
        }
        textField.val(inputFieldValue);
        textField.trigger("change");
    }

    textDiv.append(textField);
    return textDiv;
}

function createChoiceDiv(parameterName, groupId, initialValuesList)
{
    var selectChoiceDiv = $("<div class='selectChoice'/>");

    //check if there are predefined list of choices for this parameter
    var paramDetails = run_task_info.params[parameterName];
    if(paramDetails.choiceInfo != undefined  && paramDetails.choiceInfo != null && paramDetails.choiceInfo != '')
    {
        if(paramDetails.choiceInfo.status != undefined && paramDetails.choiceInfo.status != null
            && paramDetails.choiceInfo.status != undefined && paramDetails.choiceInfo.status != null
            && paramDetails.choiceInfo.status.flag != "OK")
        {
            var errorDetailsLink = $("<a href='#'> (more...)</a>");

            var errorMessageDiv = $("<p><span class='errorMessage'>No dynamic file selections available</span></p>");
            errorMessageDiv.append(errorDetailsLink);
            selectChoiceDiv.append(errorMessageDiv);
            errorDetailsLink.data("errMsg", paramDetails.choiceInfo.status.message);
            errorDetailsLink.click(function(event)
            {
                event.preventDefault();
                var errorDetailsDiv = $("<div/>");
                errorDetailsDiv.append("<p>"+  $(this).data("errMsg") + "</p>");
                errorDetailsDiv.dialog(
                    {
                        title: "Dynamic File Selection Loading Error"
                    }
                );
            });
        }

        //display drop down showing available file choices
        var choice = $("<select class='choice' />");

        if(paramDetails.allowMultiple)
        {
            choice.attr("multiple", "multiple");
        }

        if(paramDetails.required)
        {
            choice.addClass("requiredParam");
        }

        choice.data("pname", parameterName);
        var longChars = 1;
        for(var c=0;c<paramDetails.choiceInfo.choices.length;c++)
        {
            choice.append("<option value='"+paramDetails.choiceInfo.choices[c].value+"'>"
                + paramDetails.choiceInfo.choices[c].label+"</option>");
            if(paramDetails.choiceInfo.choices[c].label.length > longChars)
            {
                longChars = paramDetails.choiceInfo.choices[c].label.length;
            }
        }

        selectChoiceDiv.append(choice);

        var noneSelectedText = "Select an option";

        var cMinWidth = Math.log(longChars) * 83;

        if(cMinWidth == 0)
        {
            cMinWidth = Math.log(noneSelectedText.length) * 83;
        }

        choice.multiselect({
            multiple: paramDetails.allowMultiple,
            header: paramDetails.allowMultiple,
            selectedList: 2,
            minWidth: cMinWidth,
            noneSelectedText: noneSelectedText,
            classes: 'mSelect'
        });

        choice.multiselect("refresh");

        //disable if no choices are found
        if(paramDetails.choiceInfo.choices.length == 0)
        {
            choice.multiselect("disable");
        }

        choice.data("maxValue", paramDetails.maxValue);
        choice.change(function ()
        {
            var valueList = [];

            var value = $(this).val();

            //if this a multiselect choice, then check that the maximum number of allowable selections was not reached
            if($(this).multiselect("option", "multiple"))
            {
                var maxVal = parseInt($(this).data("maxValue"));
                if(!isNaN(maxVal) && value.length() > maxVal)
                {
                    //remove the last selection since it will exceed max allowed
                    if(value.length == 1)
                    {
                        $(this).val([]);
                    }
                    else
                    {
                        value.pop();
                        $(this).val(value);
                    }

                    alert("The maximum number of selections is " + $(this).data("maxValue"));
                    return;
                }
                valueList = value;
            }
            else
            {
                if(value != "")
                {
                    valueList.push(value);
                }
            }

            var paramName = $(this).data("pname");

            var groupId = getGroupId($(this));
            updateValuesForGroup(groupId, paramName, valueList);
        });

        //set the default value
        choice.children("option").each(function()
        {
            if(paramDetails.default_value != "" && $(this).val() == paramDetails.default_value)
            {
                $(this).parent().val(paramDetails.default_value);
                $(this).parent().data("default_value", paramDetails.default_value);
                $(this).parent().multiselect("refresh");
            }
        });

        //select initial values if there are any
        if( initialValuesList != undefined &&  initialValuesList != null)
        {
            var matchingValueList = [];
            for(var n=0;n<initialValuesList.length;n++)
            {
                choice.find("option").each(function()
                {
                    if(initialValuesList[n] != "" && initialValuesList[n] == $(this).val())
                    {
                        matchingValueList.push(initialValuesList[n]);
                    }
                });
            }

            //should only be one item in the list for now
            //but handle case when there is more than one item
            if(choice.multiselect("option", "multiple"))
            {
                if(matchingValueList.length > 0)
                {
                    //indicate initial value was found in drop-down list
                    run_task_info.params[parameterName].initialChoiceValues = true;
                }

                choice.val(matchingValueList);
            }
            else
            {
                //if there is more than one item in the list then only the first item in the list
                //will be selected since the choice is not multiselect
                if(initialValuesList.length > 0)
                {
                    run_task_info.params[parameterName].initialChoiceValues = false;

                    if(!(paramDetails.default_value == "" && initialValuesList[0] == "")
                        && $.inArray(initialValuesList[0], matchingValueList) != -1)
                    {
                        choice.val( initialValuesList[0]);
                    }

                    if((paramDetails.default_value == "" && initialValuesList[0] == "")
                        || $.inArray(initialValuesList[0], matchingValueList) != -1)
                    {
                        //indicate initial value was found in drop-down list
                        run_task_info.params[parameterName].initialChoiceValues = true;
                    }
                }
            }

            choice.multiselect("refresh");
        }
        else
        {
            run_task_info.params[parameterName].initialChoiceValues = true;
        }

        var valueList = [];
        if(choice.val() != null && choice.val() != "")
        {
            valueList.push(choice.val());
        }
        updateValuesForGroup(groupId, parameterName, valueList);
    }

    //if this is not a reloaded job where the value was from a drop down list
    if(!run_task_info.params[parameterName].initialChoiceValues)
    {
        selectChoiceDiv.hide();
    }

    return selectChoiceDiv;
}

function createFileDiv(parameterName, groupId, enableBatch, initialValuesList)
{
    var fileDiv = $("<div class='fileDiv mainDivBorder'>");

    //enable dragging of file between groups
    fileDiv.droppable(
    {
        hoverClass: 'runtask-highlight',
        drop: function(event, ui)
        {
            try
            {
                var target = $(event.target);

                var draggable = ui.draggable;
                var draggablePRow = draggable.find("td").parents(".pRow").first();
                if(draggablePRow == undefined || draggablePRow == null || draggablePRow.size() == 0)
                {
                    //do nothing since this is not droppable
                    return;
                }
                var draggableParamName = draggablePRow.attr("id");
                var draggableGroupId = draggable.parents(".valueEntryDiv").first().data("groupId");

                var targetPRow = target.parents(".pRow").first();
                if(targetPRow == undefined || targetPRow == null || targetPRow.size() == 0)
                {
                    //do nothing since this is not expected
                    return;
                }
                var targetParamName = targetPRow.attr("id");

                var filename = draggable.text();
                filename = $.trim(filename);
                var targetGroupId = target.parents(".valueEntryDiv").first().data("groupId");

                var fileObjListings = getFilesForGroup(targetGroupId, targetParamName);

                validateMaxFiles(targetParamName, fileObjListings.length + 1);

                var fileObj = {
                    name: filename,
                    id: fileId++
                };
                fileObjListings.push(fileObj);
                updateFilesForGroup(targetGroupId , targetParamName, fileObjListings);
                updateParamFileTable(targetParamName, null, groupId);


                var rowIndex = $(ui.helper).find("input[name='rowindex']").val();
                var dfileObjListings = getFilesForGroup(draggableGroupId, draggableParamName);

                dfileObjListings.splice(rowIndex, 1);
                updateFilesForGroup(draggableGroupId , draggableParamName, dfileObjListings);
                updateParamFileTable(draggableParamName, null, draggableGroupId);
            }
            catch(err)
            {
                //drop failed do nothing but log error
                console.log("Error: " + err.message);
            }
        }
    });

    var paramDetails = run_task_info.params[parameterName];

    var uploadFileText = "Upload Files...";
    var addUrlText = "Add Paths or URLs...";
    if(!paramDetails.allowMultiple)
    {
        uploadFileText = "Upload File...";
        addUrlText = "Add Path or URL...";
    }

    var fileInput = $("<input class='uploadedinputfile' type='file'/>");
    fileInput.data("pname", parameterName);

    // Create the single/batch run mode toggle
    if(enableBatch)
    {
        var batchBox = $("<div class='batchBox' title='A job will be launched for every file with a matching type.'></div>");
        // Add the checkbox
        var batchCheck = $("<input type='checkbox' id='batchCheck" + parameterName + "' />");
        batchCheck.change(function()
        {
            var paramName = $(this).parents("tr").first().data("pname");
            if ($(this).is(":checked")) {
                run_task_info.params[paramName].isBatch = true;

                //highlight the div to indicate batch mode
                $(this).closest(".pRow").css("background-color", "#F5F5F5");
                $(this).closest(".pRow").next().css("background-color", "#F5F5F5");

                //allow multi-select from file browser when in batch mode
                $(this).parents(".fileDiv").find(".uploadedinputfile").attr("multiple", "multiple");
            }
            else
            {
                //remove row highlight indicating batch mode
                $(this).closest(".pRow").css("background-color", "#FFFFFF");
                $(this).closest(".pRow").next().css("background-color", "#FFFFFF");

                // Clear the files from the parameter
                var groupId = getGroupId($(this));
                updateFilesForGroup(groupId, paramName, []);
                updateParamFileTable(paramName, $(this).closest(".fileDiv"));

                var maxNum = run_task_info.params[paramName].maxValue;

                //disable multiselect for this param if it is not file list param
                if(maxNum <= 1)
                {
                    $(this).parents(".fileDiv").find(".uploadedinputfile").removeAttr("multiple");
                }

                run_task_info.params[paramName].isBatch = false;
            }
        });
        batchBox.append(batchCheck);
        batchBox.append("<label for='batchCheck" + parameterName + "'>Batch</label>");
        batchBox.tooltip();

        fileDiv.append(batchBox);
    }

    if (paramDetails.allowMultiple)
    {
        //make the file input field multiselect, so you can select more than one file
        fileInput.attr("multiple", "multiple");
    }

    var uploadFileBtn = $("<button class='uploadBtn' type='button'>"+ uploadFileText + "</button>");
    uploadFileBtn.button().click(function()
    {
        console.log("uploadedfile: " + $(this).siblings(".uploadedinputfile").first());
        $(this).parents("div:first").find(".uploadedinputfile:first").click();
    });

    fileDiv.append(uploadFileBtn);
    if(paramDetails.required)
    {
        fileInput.addClass("requiredParam");
    }

    var fileInputDiv = $("<div class='inputFileBtn'/>");
    fileInputDiv.append(fileInput);
    fileDiv.append(fileInputDiv);

    var urlButton = $("<button type='button' class='urlButton'>"+ addUrlText +"</button>");
    fileDiv.append(urlButton);
    urlButton.data("groupId", groupId);
    urlButton.button().click(function()
    {
        var urlDiv = $("<div class='urlDiv'/>");

        urlDiv.append("Enter url:");
        var urlInput = $("<input type='text' class='urlInput'/>");
        urlDiv.append(urlInput);

        var urlActionDiv = $("<div class='center'/>");
        var enterButton = $("<button>Enter</button>");
        enterButton.button().click(function()
        {
            var paramName = $(this).parents("tr:first").data("pname");

            var url = $(this).parents("div:nth-child(2)").find(".urlInput").first().val();

            $(this).parents("td:first").children().show();

            $(this).parents(".urlDiv").first().remove();

            //check if this is not an empty string and
            // no non-space characters were entered
            if($.trim(url) == '')
            {
                return;
            }

            var groupId = getGroupId($(this));
            var fileObjListings = getFilesForGroup(groupId, paramName);

            var totalFileLength = fileObjListings.length + 1;
            validateMaxFiles(paramName, totalFileLength);

            var fileObj = {
                name: url,
                id: fileId++
            };
            fileObjListings.push(fileObj);

            updateFilesForGroup(groupId, paramName, fileObjListings);

            // add to file listing for the specified parameter
            updateParamFileTable(paramName, $(this).closest(".fileDiv"), groupId);
            toggleFileButtons(paramName);

        });
        urlActionDiv.append(enterButton);

        var cancelButton = $("<button>Cancel</button>");
        cancelButton.button().click(function()
        {
            $(this).parents("td:first").children().show();
            $(this).parents(".urlDiv").first().remove();
        });
        urlActionDiv.append(cancelButton);
        urlDiv.append(urlActionDiv);

        $("#dialogUrlDiv").data("groupId", groupId);
        $("#dialogUrlDiv").append(urlDiv);
        openServerFileDialog(this);
    });

    fileDiv.append("<span class='drop-box'>drop files here</span>");
    fileDiv.append("<div class='fileListingDiv'/>");

    //check if there are predefined file values
    var fileObjListings = getFilesForGroup(groupId, parameterName);

    //also check if this parameter is also a choice parameter
    if(initialValuesList != undefined &&  initialValuesList != null
        && initialValuesList.length > 0)
    {
        if(!run_task_info.params[parameterName].initialChoiceValues)
        {
            //check if max file length will be violated
            var totalFileLength = fileObjListings.length +  initialValuesList.length;
            validateMaxFiles(parameterName, totalFileLength);

            for(var v=0; v < initialValuesList.length; v++)
            {
                //check if the file name is not empty
                if( initialValuesList[v] != null &&  initialValuesList[v] != "")
                {
                    var fileObj =
                    {
                        name:  initialValuesList[v],
                        id: fileId++
                    };

                    fileObjListings.push(fileObj);
                }
            }

            updateFilesForGroup(groupId, parameterName, fileObjListings);
            updateParamFileTable(parameterName, fileDiv, groupId);
        }
    }
    else
    {
        //set the value of the parameter to empty array since no initial values were specified
        updateValuesForGroup(groupId, parameterName, []);
    }

    //get the HTMLElement
    var dropbox = fileDiv[0];
    // init event handlers
    dropbox.addEventListener("dragenter", dragEnter, true);
    dropbox.addEventListener("dragleave", dragLeave, true);
    dropbox.addEventListener("dragexit", dragExit, false);
    dropbox.addEventListener("dragover", dragOver, false);
    dropbox.addEventListener("drop", drop, true);

    if(run_task_info.params[parameterName].initialChoiceValues)
    {
        fileDiv.hide();
    }

    return fileDiv;
}

//add toggle to switch between field types for a parameter that has multiple
//i.e. file drop-down parameters (CHOICE and FILE field types)
function createModeToggle(parameterName)
{
    var paramDetails = run_task_info.params[parameterName];

    var toggleChoiceFileDiv = $("<div class='fieldTypeToggle'/>");
    toggleChoiceFileDiv.data("pname", parameterName);

    //this count is used create elements with unique ids for elements associated with the parameter
    var nextCount = run_task_info.params[parameterName].count;
    if(nextCount == undefined || nextCount == null)
    {
        nextCount = 0;
        run_task_info.params[parameterName].count = nextCount;
    }

    nextCount++;
    var idPName = nextCount + "_" + parameterName;
    var fileChoiceOptions = $("<div class='fileChoiceOptions'>");

    if($.inArray(field_types.CHOICE, run_task_info.params[parameterName].type) != -1)
    {
         fileChoiceOptions.append('<input id="selectFile_' + idPName + '" name="field_toggle_' + idPName + '" type="radio" /><label for="selectFile_'+ idPName +'">Select a file</label>');
    }

    if($.inArray(field_types.FILE, run_task_info.params[parameterName].type) != -1)
    {
        fileChoiceOptions.append('<input id="customFile_'+ idPName + '" name="field_toggle_' + idPName + '" type="radio" /><label for="customFile_'+ idPName +'">Upload your own file</label> ');
    }

    fileChoiceOptions.find(":radio:checked").removeAttr("checked");
    if(paramDetails.initialChoiceValues)
    {
        fileChoiceOptions.find('input[id="selectFile_'+ idPName + '"]').attr("checked", "checked");
    }
    else
    {
        fileChoiceOptions.find('input[id="customFile_'+ idPName + '"]').attr("checked", "checked");
    }

    toggleChoiceFileDiv.append(fileChoiceOptions);

    fileChoiceOptions.buttonset();

    fileChoiceOptions.find("label.ui-button:not(:last)").each(function()
    {
        $(this).after('<span class="elemSpacing">  or  </span>');
    });

    fileChoiceOptions.data("pname", parameterName);

    fileChoiceOptions.change(function()
    {
        var selectedOption = ($(this).find(":radio:checked + label").text());
        var pname = $(this).data("pname");

        //clear any values that were set
        var groupId = getGroupId(fileChoiceOptions);
        updateValuesForGroup(groupId, pname, []);
        updateFilesForGroup(groupId, pname, []);
        updateParamFileTable(pname, $(this).parents("td:first").find(".fileDiv"));

        $(this).parents("td:first").find(".fileDiv").toggle();
        $(this).parents("td:first").find(".selectChoice").toggle();

        if(selectedOption == "Select a file")
        {
            var defaultValue = $(this).parents("td:first").find(".choice").data("default_value");

            if(defaultValue == undefined || defaultValue == null)
            {
                defaultValue = "";
            }

            $(this).parents("td:first").find(".choice").val(defaultValue);
            $(this).parents("td:first").find(".choice").trigger("change");
            $(this).parents("td:first").find(".choice").multiselect("refresh");
        }
    });

    return toggleChoiceFileDiv;
}

//initialize the params object with info about the parameters
function initParams(parameters, initialValues)
{
    if(parameters == undefined || parameters == null)
    {
        return;
    }

    for(var q=0;q < parameters.length;q++)
    {
        var parameterName = parameters[q].name;
        initParam(parameters[q], q, initialValues);
    }
}

function getNextGroupId(parameterName)
{
    var paramGroupInfo = parameter_and_val_groups[parameterName];
    if(paramGroupInfo == null)
    {
        paramGroupInfo = {};
        paramGroupInfo.groupCountIncrementer = 0;
        parameter_and_val_groups[parameterName] = paramGroupInfo;
    }

    var nextGroupId = paramGroupInfo.groupCountIncrementer;
    nextGroupId++;
    parameter_and_val_groups[parameterName].groupCountIncrementer = nextGroupId;

    if(parameter_and_val_groups[parameterName].groups == undefined ||
        parameter_and_val_groups[parameterName].groups == null)
    {
        parameter_and_val_groups[parameterName].groups = {};
    }

    parameter_and_val_groups[parameterName].groups[nextGroupId] = {};

    return nextGroupId;
}

function getGroupId(element)
{
    var valueEntryDiv = element.parents(".valueEntryDiv");
    if(valueEntryDiv == undefined || valueEntryDiv == null
        || valueEntryDiv.data("groupId") == undefined
        || valueEntryDiv.data("groupId") == null)
    {
        javascript_abort("Error retrieving group id");
    }

    return valueEntryDiv.data("groupId");
}


function updateValuesForGroup(groupId, paramName, valueList)
{
    if(parameter_and_val_groups[paramName].groups[groupId] == undefined
        || parameter_and_val_groups[paramName].groups[groupId] == null )
    {
        javascript_abort("Error retrieving group Id " + groupId + " for parameter " + paramName);
    }

    parameter_and_val_groups[paramName].groups[groupId].values = valueList;
}

function getFileGroupIdByIndex(paramName, index)
{
    if(parameter_and_val_groups[paramName] == undefined || parameter_and_val_groups[paramName] == null
        || parameter_and_val_groups[paramName].groups == undefined
        || parameter_and_val_groups[paramName].groups == null )
    {
        javascript_abort("Error retrieving first group for parameter " + paramName);
    }

    var groupIds = Object.keys(parameter_and_val_groups[paramName].groups);
    //check if index is out of range
    if(index < groupIds.length )
    {
        return groupIds[index];
    }

    javascript_abort("Error retrieving group: index out of range " + index);
}

function getFilesForGroup(groupId, paramName)
{
    if(parameter_and_val_groups[paramName].groups[groupId] == undefined
        || parameter_and_val_groups[paramName].groups[groupId] == null )
    {
        javascript_abort("Error retrieving group Id " + groupId + " for parameter " + paramName);
    }

    if(parameter_and_val_groups[paramName].groups[groupId].files == undefined ||
        parameter_and_val_groups[paramName].groups[groupId].files == null)
    {
        parameter_and_val_groups[paramName].groups[groupId].files = [];
    }

    return parameter_and_val_groups[paramName].groups[groupId].files;
}

function updateFilesForGroup(groupId, paramName, filesList)
{
    if(parameter_and_val_groups[paramName].groups[groupId] == undefined
        || parameter_and_val_groups[paramName].groups[groupId] == null )
    {
        javascript_abort("Error retrieving group Id " + groupId + " for parameter " + paramName);
    }
    parameter_and_val_groups[paramName].groups[groupId].files = filesList;
}

function createParamValueEntryDiv(parameterName, initialValuesObj)
{
    var contentDiv = $("<div class='valueEntryDiv'/>");
    var groupId = getNextGroupId(parameterName);
    contentDiv.data("groupId", groupId);

    var enableBatch = true;
    var groupingEnabled = false;

    var groupInfo = run_task_info.params[parameterName].groupInfo;
    if(groupInfo != null && (groupInfo.maxValue == null || groupInfo.maxValue == undefined
        || groupInfo.maxValue > 1))
    {
        groupingEnabled = true;

        //do not allow batch if grouping is enabled
        enableBatch = false;
    }

    var initialValues = null;
    var groupid = null;
    if(initialValuesObj != undefined && initialValuesObj != null)
    {
        initialValues = initialValuesObj.values;
        groupid = initialValuesObj.groupid;
    }

    populateContentDiv(parameterName, contentDiv, groupId, initialValues, enableBatch);

    //check if grouping is enabled
    if(groupingEnabled)
    {
        var groupColumnLabel = groupInfo.groupColumnLabel;
        if(groupColumnLabel == undefined && groupColumnLabel == null)
        {
            groupColumnLabel = "file group";
        }

        var groupingDiv = $("<div class='groupingDiv'/>");
        var groupTextField = $("<input class='groupingTextField' type='text'/>");
        groupTextField.change(function()
        {
            var paramName = $(this).parents(".pRow").first().attr("id");
            var groupId = getGroupId($(this));

            var value = $.trim($(this).val());
            parameter_and_val_groups[paramName].groups[groupId].name = value;
        });

        if(groupid == undefined || groupid == null)
        {
            groupid = "";
        }
        else
        {
            groupTextField.val(groupid);
            parameter_and_val_groups[parameterName].groups[groupId].name = groupid;
        }

        var groupTextLabel = $("<label>" + groupColumnLabel + ": <label/>");
        groupTextLabel.append(groupTextField);
        groupingDiv.append(groupTextLabel);
        contentDiv.prepend(groupingDiv);

        contentDiv.find(".fileDiv").removeClass("mainDivBorder");
        contentDiv.addClass("mainDivBorder");

        //add delete button to remove this group
        var delButton = $("<img class='images floatRight' src='/gp/images/delete-blue.png'/>");
        delButton.button().click(function()
        {
            //TODO remove the grouping;
            var paramName = $(this).parents(".pRow").first().attr("id");
            var groupId = getGroupId($(this));

            //remove this group from the hash
            delete parameter_and_val_groups[paramName].groups[groupId];
            $(this).parents(".valueEntryDiv").remove();
        });

        if($("#"+jqEscape(parameterName)).find(".valueEntryDiv").length != 0)
        {
            contentDiv.prepend(delButton);
        }
    }

    return contentDiv;
}

function populateContentDiv(parameterName, contentDiv, groupId, initialValues, enableBatch)
{
    //create the necessary field types for this parameter
    if($.inArray(field_types.CHOICE, run_task_info.params[parameterName].type) != -1)
    {
        contentDiv.append(createChoiceDiv(parameterName, groupId, initialValues));
    }

    if($.inArray(field_types.FILE, run_task_info.params[parameterName].type) != -1)
    {
        contentDiv.append(createFileDiv(parameterName, groupId, enableBatch, initialValues));
    }

    if($.inArray(field_types.TEXT, run_task_info.params[parameterName].type) != -1)
    {
        //this must be a text entry
        contentDiv.append(createTextDiv(parameterName, groupId, initialValues));
    }

    if(run_task_info.params[parameterName].type.length > 1)
    {
        //multiple field types specified so add a toggle buttons
        //right now this would only be for a file drop-down parameter
        contentDiv.prepend(createModeToggle(parameterName));
    }
}
function loadParameterInfo(parameters, initialValues)
{
    var paramsTable = $("#paramsTable");

    //check if the params object should be initialized
    if(parameters != null)
    {
        initParams(parameters, initialValues);
    }

    if(run_task_info.params == null)
    {
        throw new Error("Error initialization parameters");
    }

    var parameterNames = Object.keys(run_task_info.params);
    for(var q=0;q < parameterNames.length;q++)
    {
        var parameterName = parameterNames[q];

        var paramRow = $("<tr id='" + parameterName + "' class='pRow'/>");
        paramRow.data("pname", parameterName);

        paramsTable.append(paramRow);

        //add the display name of the parameter to the first column of the table
        var nameDiv = $("<div class='pTitleDiv'>");
        $("<td class='pTitle'>").append(nameDiv).appendTo(paramRow);

        nameDiv.append(run_task_info.params[parameterName].displayname);
        if(run_task_info.params[parameterName].required)
        {
            nameDiv.append("*");
        }

        var valueTd = $("<td class='paramValueTd'/>");
        paramRow.append(valueTd);

        //set the initial values
        //can be null or undefined if this is not a job reload
        var initialValuesList = null;

        if(initialValues != null && initialValues != undefined)
        {
            initialValuesList = initialValues[parameterName];
        }
        run_task_info.params[parameterName].initialValues = initialValuesList;

        var initialValuesByGroup = run_task_info.params[parameterName].initialValues;
        if(initialValuesByGroup != undefined && initialValuesByGroup != null)
        {
            for(var g=0;g<initialValuesByGroup.length;g++)
            {
                valueTd.append(createParamValueEntryDiv(parameterName, initialValuesByGroup[g]));
                //check if grouping is enabled
            }
        }
        else
        {
            valueTd.append(createParamValueEntryDiv(parameterName, null));
        }
        //check if grouping is enabled
        var groupInfo = run_task_info.params[parameterName].groupInfo;
        if(groupInfo != null && (groupInfo.maxValue == null || groupInfo.maxValue == undefined
            || groupInfo.maxValue > 1))
        {
            var groupColumnLabel = groupInfo.groupColumnLabel;
            if(groupColumnLabel == undefined && groupColumnLabel == null)
            {
                groupColumnLabel = "file group";
            }

            var addGroupButton = $("<button>Add Another " + groupColumnLabel + "</button>");
            addGroupButton.button().click(function(event)
            {
                event.preventDefault();

                var parameterName = $(this).parents(".pRow").attr("id");
                $(this).parents(".pRow").first().find(".paramValueTd").find(".valueEntryDiv").last().after(createParamValueEntryDiv(parameterName, null));
                //TODO: Add another of these input fields

            });

            $("<div class='fileGroup'/>").append(addGroupButton).appendTo(valueTd);
        }

        paramsTable.append(createParamDescriptionRow(parameterName));
    }
}

function createParamDescriptionRow(parameterName)
{
    var paramDetails = run_task_info.params[parameterName];

    //append parameter description table
    var pDescription = paramDetails.description;
    if(paramDetails.altDescription != undefined
        && paramDetails.altDescription != null
        && paramDetails.altDescription.replace(/ /g, '') != "") //trims spaces to check for empty string
    {
        pDescription = paramDetails.altDescription;
    }

    return $("<tr class='paramDescription'><td></td><td colspan='3'>" + pDescription +"</td></tr>");
}

function loadRunTaskForm(lsid, reloadJob) {
    // Hide the search slider if it is open
    $(".search-widget").searchslider("hide");

    // Lazily clone the blank jobSubmit div, and replace a dirty div with the clean one
    if (Request.cleanJobSubmit === null) {
        Request.cleanJobSubmit = $("#submitJob").clone();
    }
    else {
        $("#submitJob").replaceWith(Request.cleanJobSubmit.clone());
    }

    // Hide the EULA if one is visible
    $("#eula-block").hide();

    // Clean the run_task_info variable
    run_task_info = {
        lsid: null, //lsid of the module
        name: null, //name of the module
        params: {}, //contains parameter info necessary to build the job submit form, see the initParam() function for details
        sendTo: {}
    };

    parameter_and_val_groups = {}; //contains params and their values only

    $("#toggleDesc").click(function()
    {
        //show descriptions
        $("#paramsTable tr.paramDescription").toggle();
    });

    $("button").button();
    if (reloadJob !== false) {
        reloadJob = Request.parameter('reloadJob');
    }
    else {
        reloadJob = "";
    }
    if(reloadJob == undefined || reloadJob == null)
    {
        reloadJob = "";
    }

    if (lsid === undefined || lsid === null) {
        lsid = Request.parameter('lsid');
    }

    if((lsid == undefined || lsid == null || lsid  == "")
        && (reloadJob == undefined || reloadJob == null || reloadJob  == ""))
    {
        $("#protocols").show();
        return;
    }
    else
    {
        loadModule(lsid, reloadJob);
    }

    $("input[type='file']").live("change", function()
    {
        var paramName = $(this).data("pname");

        var groupId = getGroupId($(this));
        var fileObjListings = getFilesForGroup(groupId, paramName);

        //create a copy of files so that the input file field
        //can be reset so that files with the same name can be reuploaded
        var uploadedFiles = [];
        for(var t=0;t<this.files.length;t++)
        {
            uploadedFiles.push(this.files[t]);
        }

        //Reset the value of the file input to work around
        //feature in Chrome where uploading the same file sequentially
        //does not trigger a change event
        $(this).val(null);

        checkFileSizes(uploadedFiles);

        var totalFileLength = fileObjListings.length + uploadedFiles.length;
        validateMaxFiles(paramName, totalFileLength);

        //add newly selected files to table of file listing
        for(var f=0;f < uploadedFiles.length;f++)
        {
            var fileObj = {
                name: uploadedFiles[f].name,
                object: uploadedFiles[f],
                id: fileId++
            };
            fileObjListings.push(fileObj);
        }

        // add to file listing for the specified parameter
        updateFilesForGroup(groupId, paramName, fileObjListings);
        updateParamFileTable(paramName, $(this).closest(".fileDiv"));
        toggleFileButtons(paramName);
    });

    /* begin other options menu code*/
    var selected = function( event, ui ) {
        $(this).popup( "close" );
    };

    $("button.Reset").click(function()
    {
        reset();
    });

    $("button.Run").click(function()
    {
        //Submit this job to the server
        runJob();
    });

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

    $(document).bind('drop dragover', function (e) {
        e.preventDefault();
    });

    document.body.addEventListener('drop', function(e) {
        e.preventDefault();
    }, false);

    //add action for when cancel upload button is clicked
    $("#cancelUpload").hide();
    $("#cancelUpload").button().click(function()
    {
        for(var y=0;y<fileUploadRequests.length;y++)
        {
            fileUploadRequests[y].abort();
        }

        $("#cancelUpload").hide();

        //Change text of blocking div
        $('#runTaskSettingsDiv').unblock();
        $("#fileUploadDiv").empty();
    });

    $("#javaCode").data("language", "Java");
    $("#matlabCode").data("language", "MATLAB");
    $("#rCode").data("language", "R");

    $("#removeViewCode").button().click(function()
    {
        $("#viewCodeDiv").hide();
    });

    /*add action for when one of the view code languages is selected */
    $("#viewCodeDiv").hide();
    $(".viewCode").click(function()
    {
        var language = $(this).data("language");
        $("#viewCodeDiv").children().each(function()
        {
            //if this is not the delete button then remove it
            if($(this).attr("id") != "removeViewCode")
            {
                $(this).remove();
            }
        });

        $("#viewCodeDiv").append("<p id='viewCodeHeader'>" + language + " code to call " + run_task_info.name + ":</p>");
        $("#viewCodeDiv").show();

        var url = window.location.href;
        var getParameters = url.slice(url.indexOf('?') + 1);
        var queryString = "?" + getParameters;

        //add parameters and their values to the query string
        var paramNames = Object.keys(parameter_and_val_groups);
        for(var t=0;t<paramNames.length;t++)
        {
            var groupNames = Object.keys(parameter_and_val_groups[paramNames[t]].groups);
            for(var g=0;g<groupNames.length;g++)
            {
                var valuesList =parameter_and_val_groups[paramNames[t]].groups[groupNames[g]].values;
                if(valuesList != undefined && valuesList != null && valuesList.length > 0)
                {
                    queryString += "&" + paramNames[t] + "=" + valuesList[0];
                }
            }
        }

        $.ajax({
            type: "GET",
            url: "/gp/rest/RunTask/viewCode" + queryString,
            cache: false,
            data: { "lsid" : run_task_info.lsid,
                "reloadJob":  run_task_info.reloadJobId,
                "language": language},
            success: function(response) {

                if (response["code"] == undefined || response["code"] == null)
                {
                    $("#viewCodeDiv").append("<p>An error occurred while retrieving the code</p>")
                }
                else
                {
                    $("#viewCodeDiv").append("<p>" + htmlEncode(response["code"]) + "</p>");
                    //add a link to the appropriate programmers guide
                    $("#viewCodeDiv").append("<span><hr/>For more details go to the Programmer's Guide section: <a href='http://www.broadinstitute.org/cancer/software/genepattern/gp_guides/programmers/sections/gp_" + language.toLowerCase()+"'> " +
                        "Using GenePattern from " + language + "</a></span>");
                }
            },
            error: function(xhr, ajaxOptions, thrownError)
            {
                console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
                console.log(thrownError);

                $("#viewCodeDiv").append("<p>An error occurred while retrieving the code: " + xhr.responseText + "</p>");
            },
            dataType: "json"
        });
    });
}

function reset()
{

    $("#paramsTable").empty();

    //remove all input file parameter file listings
    param_file_listing = {};

    loadParameterInfo(null, null);
}

function isText(param)
{
    var selector = "#" + jqEscape(param);
    var input = $(selector);
    if (input.length === 0) return;

    return input.attr("type") === "text";
}

function isDropDown(param)
{
    var selector = "#" + jqEscape(param);
    var input = $(selector);
    if (input.length === 0) return;

    return input.get(0).tagName === "SELECT";
}

function isFile(param)
{
    var selector = "#" + jqEscape(param);
    var input = $(selector);
    if (input.length === 0) return;

    // Determine the input type
    return input.attr("type") === "file";
}

function validate()
{
    var missingReqParameters = [];
    var missingGroupNames = [];
    var paramNames = Object.keys(parameter_and_val_groups);
    for(var p=0;p<paramNames.length;p++)
    {
        var groups = parameter_and_val_groups[paramNames[p]].groups;
        if(groups == null)
        {
            continue;
        }

        var groupIds = Object.keys(groups);
        for(var g=0;g<groupIds.length;g++)
        {
            var values = parameter_and_val_groups[paramNames[p]].groups[groupIds[g]].values;
            var files = parameter_and_val_groups[paramNames[p]].groups[groupIds[g]].files;

            var required = run_task_info.params[paramNames[p]].required;
            //check if it is required and there is no value specified
            if(required && (values == undefined || values == null
                || values.length == 0 || (values.length == 1 && values[0] == ""))
                && (files == undefined || files == null || files.length == 0))
            {
                missingReqParameters.push(paramNames[p]);
                break;
            }

            //check that group names were specified if there is more than one group
            var groupName = parameter_and_val_groups[paramNames[p]].groups[groupIds[g]].name;
            if((groupName == undefined || groupName == null || groupName.length == 0) && groupIds.length > 1)
            {
                //if there is more than one group defined then they must be named
                missingGroupNames.push(paramNames[p]);
                break;
            }
        }
    }

    //remove any existing error messages
    $(".errorMessage").remove();
    $("#missingRequiredParams").remove();
    $(".errorHighlight").each(function()
    {
        $(this).removeClass("errorHighlight");
    });

    if(missingReqParameters.length > 0)
    {
        //create div to list of all parameters with missing values
        var missingReqParamsDiv = $("<div id='missingRequiredParams'/>");

        $("#submitErrors").append(missingReqParamsDiv);
        missingReqParamsDiv.append("<p class='errorMessage'>Please provide a value for the following parameter(s):</p>");

        var pListing = $("<ul class='errorMessage'/>");
        missingReqParamsDiv.append(pListing);

        var errorMessage = "<div class='errorMessage'>This field is required</div>";

        for(p=0;p<missingReqParameters.length;p++)
        {
            var displayname = missingReqParameters[p];

            //check if the parameter has an alternate name
            if(run_task_info.params[missingReqParameters[p]].displayname != undefined
                && run_task_info.params[missingReqParameters[p]].displayname!= "")
            {
                displayname = run_task_info.params[missingReqParameters[p]].displayname;
            }

            pListing.append("<li>"+displayname+"</li>");

            $("#" + jqEscape(missingReqParameters[p])).find(".paramValueTd").addClass("errorHighlight");
            $("#" + jqEscape(missingReqParameters[p])).find(".paramValueTd").append(errorMessage);
        }

        return false;
    }

    //do something similar if missing group names were found
    if(missingGroupNames.length > 0)
    {
        //create div to list of all parameters with missing values
        var missingGroupNamesDiv = $("<div id='missingGroupNamesParams'/>");
        $("#submitErrors").append(missingGroupNamesDiv);
        missingGroupNamesDiv.append("<p class='errorMessage'>Please provide labels for the following parameter(s):</p>");

        var pListing = $("<ul class='errorMessage'/>");
        missingGroupNamesDiv.append(pListing);

        var errorMessage = "<div class='errorMessage'>This value is required</div>";

        for(p=0;p<missingGroupNames.length;p++)
        {
            var displayname = missingGroupNames[p];

            //check if the parameter has an alternate name
            if(run_task_info.params[missingGroupNames[p]].displayname != undefined
                && run_task_info.params[missingGroupNames[p]].displayname!= "")
            {
                displayname = run_task_info.params[missingGroupNames[p]].displayname;
            }

            pListing.append("<li>"+displayname+"</li>");

            $("#" + jqEscape(missingGroupNames[p])).find(".paramValueTd").addClass("errorHighlight");
            $("#" + jqEscape(missingGroupNames[p])).find(".paramValueTd").find(".groupingTextField").each(function()
            {
                if($(this).val() == undefined || $(this).val() == null || $(this).val().length == 0)
                {
                    $(this).after(errorMessage);
                }
            });
        }

        return false;
    }

    return true;
}

function runJob()
{
    //validate that all required inputs have been specified
    if(!validate())
    {
        return;
    }

    //upload all the input files if there are any
    if(!allFilesUploaded())
    {
        uploadAllFiles();
    }
    else
    {
        //add the file locations to the file parameter object that will
        submitTask();
    }
}

function buildBatchList() {
    var batchParams = [];
    var parameterNames = Object.keys(parameter_and_val_groups);
    for (var p=0;p<parameterNames.length;p++) {
        var paramName = parameterNames[p];
        if (isBatch(paramName)) {
            batchParams.push(paramName);
        }
    }

    return batchParams;
}

function submitTask()
{
    setAllFileParamValues();

    //Change text of blocking div
    $('#runTaskSettingsDiv').block({
        message: '<h1> Submitting job...</h1>',
        overlayCSS: { backgroundColor: '#F8F8F8'}
    });

    console.log("submitting task");

    var param_values_by_group = {};
    var parameterNames = Object.keys(parameter_and_val_groups);
    for(var p=0;p<parameterNames.length;p++)
    {
        var paramName = parameterNames[p];
        param_values_by_group[paramName] = [];
        var groupIds = Object.keys(parameter_and_val_groups[paramName].groups);

        //sort the groups by their numeric id to retain order of groups
        //since the ids are generated sequentially
        groupIds.sort(function(a, b) {
            a = parseInt(a);
            b = parseInt(b);

            return a < b ? -1 : (a > b ? 1 : 0);
        });

        for(var g=0;g<groupIds.length;g++)
        {
            var groupInfo = parameter_and_val_groups[paramName].groups[groupIds[g]];
            var groupName = groupInfo.name;
            if(groupName == undefined || groupName == null)
            {
                //if there is more than one group defined then they must be named
                if(groupIds.length == 1)
                {
                    groupName = "";
                }
            }
            var newGroupInfo = {};
            newGroupInfo.name = groupName;
            newGroupInfo.values = groupInfo.values;
            param_values_by_group[paramName].push(newGroupInfo);
        }
    }

    var taskJsonObj =
    {
        "lsid" : run_task_info.lsid,
        "params" : JSON.stringify(param_values_by_group),
        "batchParams": buildBatchList()
    };

    $.ajax({
        type: "POST",
        url: "/gp/rest/RunTask/addJob",
        contentType: 'application/json',
        data: JSON.stringify(taskJsonObj),
        timeout: 60000,  //timeout added to specifically to handle cases of file choice ftp listing taking too long
        success: function(response) {

            var message = response["MESSAGE"];

            if (message !== undefined && message !== null) {
                alert(message);
            }

            if (response.batchId !== undefined) {
                window.location.replace("/gp/jobResults");
            }
            else if (response.jobId != undefined) {
                window.location.replace("/gp/jobResults/"+response.jobId+"?openVisualizers=true");
            }

            console.log("Response text: " + response.text);
        },
        error: function(xhr, ajaxOptions, thrownError)
        {
            alert("Error: \n" + xhr.responseText);
            console.log("Error: " + xhr.responseText);
            console.log("Error on server: " + thrownError);

            //the jobsubmit failed unblock the run task form
            //and remove any file upload progress
            $('#runTaskSettingsDiv').unblock();
            $("#fileUploadDiv").empty();
        },
        dataType: "json"
    });
}

function dragEnter(evt)
{
    this.classList.add('runtask-highlight');
    evt.stopPropagation();
    evt.preventDefault();
}

function dragLeave(evt)
{
    this.classList.remove('runtask-highlight');
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
    this.classList.add('runtask-highlight');
    evt.stopPropagation();
    evt.preventDefault();
}

function drop(evt)
{
    this.classList.remove('runtask-highlight');
    evt.stopPropagation();
    evt.preventDefault();

    //Check and prevent upload of directories
    //Works only on browsers that support the HTML5 FileSystem API
    //right now this is Chrome v21 and up
    if(evt.dataTransfer && evt.dataTransfer.items){
        var items = evt.dataTransfer.items;
        var length   = items.length;

        for(var i=0; i<length; i++)
        {
            var entry = items[i];
            if(entry.getAsEntry)
            {
                //Standard HTML5 API
                entry = entry.getAsEntry();
            }
            else if(entry.webkitGetAsEntry)
            {
                //WebKit implementation of HTML5 API.
                entry = entry.webkitGetAsEntry();
            }

            if(entry && entry.isDirectory){
                //do to continur if any directories are found
                alert("Directory uploads are not allowed.");
                return;
            }
        }
    }

    var files = evt.dataTransfer.files;
    var count = files.length;

    var target = $(evt.target);
    var paramName = target.parents(".pRow").first().data("pname");
    if(paramName == undefined)
    {
        console.log("Error: Could not find the parameter this file belongs to.");
        return;
    }

    // Only call the handler if 1 or more files was dropped.
    if (count > 0)
    {
        checkFileSizes(files);

        handleFiles(files, paramName, target);
    }
    else
    {
        if(evt.dataTransfer.getData('Text') != null
            && evt.dataTransfer.getData('Text')  !== undefined
            && evt.dataTransfer.getData('Text') != "")
        {
            //This must be a url and not a file
            var groupId = getGroupId(target);
            var fileObjListings = getFilesForGroup(groupId, paramName);

            var totalFileLength = fileObjListings.length + 1;
            validateMaxFiles(paramName, totalFileLength);

            var fileObj = {
                name: evt.dataTransfer.getData('Text'),
                id: fileId++
            };
            fileObjListings.push(fileObj);

            updateFilesForGroup(groupId, paramName, fileObjListings);
            updateParamFileTable(paramName, $(this).closest(".fileDiv"));
            toggleFileButtons(paramName);
        }
    }
}

function handleFiles(files, paramName, fileDiv)
{
    var groupId = getGroupId(fileDiv);
    var fileObjListings = getFilesForGroup(groupId, paramName);

    var totalFileLength = fileObjListings.length + files.length;
    validateMaxFiles(paramName, totalFileLength);

    //add newly selected files to table of file listing
    for(var f=0; f < files.length; f++)
    {
        var fileObj = {
            name: files[f].name,
            object: files[f],
            id: fileId++
        };
        fileObjListings.push(fileObj);
    }

    // add to file listing for the specified parameter
    updateFilesForGroup(groupId, paramName, fileObjListings);
    updateParamFileTable(paramName, null, groupId);
    toggleFileButtons(paramName);
}

function validateMaxFiles(paramName, numFiles)
{
    //check if max file length will be violated only if this not a batch parameter
    //in the case of batch we want to allow specifying any number of files
    if(isBatch(paramName))
    {
        return;
    }

    var paramDetails = run_task_info.params[paramName];

    var maxFilesLimitExceeded = false;

    if(paramDetails != null)
    {
        //in this case the max num of files is not unlimited
        if(paramDetails.maxValue != undefined || paramDetails.maxValue != null)
        {
            var maxValue = parseInt(paramDetails.maxValue);
            if(numFiles > maxValue)
            {
                maxFilesLimitExceeded =  true;
            }
        }
    }

    //check that the user did not add more files than allowed
    if(maxFilesLimitExceeded)
    {
        alert("The maximum number of files that can be provided to the " + paramName +
            " parameter has been reached. Please delete some files " +
            "before continuing.");
        throw new Error("The maximum number of files that can be provided to " +
            "this parameter (" + paramName +") has been reached. Please delete some files " +
            "before continuing.");
    }
}

function checkFileSizes(files)
{
    //check if any of these files is > 2GB
    for (var i = 0; i < files.length; i++) {
        var file = files[i];
        if (file.size > 2040109466) { //approx 1.9GB in bytes
            /*var errorMessage = "One or more of the selected files exceeds the 2GB limit for this upload method." +
             " Please use the 'Uploads' tab on the right (located next to the Recent Jobs tab) to upload these" +
             " files.More information about using large files can be found in our User Guide available in the " +
             "Help Menu above. http://www.google.com");*/
            var errorMessageDiv = $("<div/>");
            errorMessageDiv.append("<p>One or more of the selected files exceeds the 2GB limit for this upload method.</p>");
            errorMessageDiv.append("<p> Please use the 'Uploads' tab on the right (located next to the Recent Jobs tab)" +
                " to upload these files.</p>");
            errorMessageDiv.append("<p> More information about using large files can be found in our " +
                "<a href='http://www.broadinstitute.org/cancer/software/genepattern/gp_guides/user-guide" +
                "/sections/running-modules#_Uploading_Files' target='_blank'>User Guide</a>.</p>");
            errorMessageDiv.dialog({
                title: "Max File Upload Size Exceeded",
                resizable: true,
                height: 210,
                width: 500,
                modal: true,
                buttons: {
                    "OK": function() {
                        $( this ).dialog( "close" );
                    }
                }
            });
            throw new Error("The provided file " + file.name + " is over the 2 GB limit.");
        }
    }
}

function updateParamFileTable(paramName, fileDiv, groupId)
{
    if(groupId == undefined || groupId == null)
    {
        if(fileDiv == undefined || fileDiv == null)
        {
            javascript_abort("Not able to update file listing for " + paramName);
        }

        groupId = getGroupId(fileDiv);
    }

    var files = getFilesForGroup(groupId, paramName);

    if(fileDiv == null)
    {
        var paramRow = $("#" + jqEscape(paramName));
        //check if a groupId was given
        if(groupId != undefined && groupId != null)
        {
            paramRow.find(".valueEntryDiv").each(function()
            {
                if($(this).data("groupId") == groupId)
                {
                    fileDiv = $(this).find(".fileDiv");
                }
            });
        }
        else
        {
            fileDiv = paramRow.find(".fileDiv").first();
        }

        if(fileDiv == null)
        {
            javascript_abort("Error populating file listing for " + paramName);
        }
    }

    var fileListingDiv = fileDiv.find(".fileListingDiv");

    var hideFiles = false;
    if(fileListingDiv.find(".editFilesLink").text() == "Show Files...")
    {
        hideFiles = true;
    }

    //remove previous file info data
    $(fileListingDiv).empty();

    if(files != null && files != undefined && files.length > 0)
    {
        //if there is one file and it is null or en empty string then do nothing and return
        if(files.length == 1 && (files[0].name == null || files[0].name == ""))
        {
            return;
        }

        //toggle view if this is a file choice parameter
        var fileChoiceToggle = fileDiv.parents(".pRow:first").find("input[id^=customFile_]");
        if(fileChoiceToggle.length != 0)
        {
            //switch view to custom file view
            fileChoiceToggle.click();

            updateFilesForGroup(groupId, paramName, files);
        }

        var pData = $("<div class='fileDetails'/>");

        var editLink = $("<a class='editFilesLink' href='#'><img src='/gp/images/arrows-down.gif'/>Hide Files...</a>");
        editLink.click(function(event)
        {
            event.preventDefault();

            var editLinkMode = $(this).text();
            if(editLinkMode == "Show Files...")
            {
                $(this).text("Hide Files...");
                $(this).prepend("<img src='/gp/images/arrows-down.gif'/>");
                fileListingDiv.find(".paramFilesTable").removeClass("hidden");
            }
            else
            {
                $(this).text("Show Files...");
                $(this).prepend("<img src='/gp/images/arrows.gif'/>");
                fileListingDiv.find(".paramFilesTable").addClass("hidden");
            }
        });
        pData.append(editLink);

        var selectedFiles = "Selected ";
        selectedFiles += files.length + " files";

        pData.append("(" + selectedFiles + ")");
        fileListingDiv.append(pData);

        var table = $("<table class='paramFilesTable'/>");
        var tbody = $("<tbody/>");
        table.append(tbody);
        for(var i=0;i<files.length;i++)
        {
            //ignore any file names that are empty or null
            if(files[i].name == null || files[i].name == "")
            {
                continue;
            }

            var fileRow = $("<tr/>");
            var fileTData = $("<td class='pfileAction'/>");

            //determine if this is a  url
            if(files[i].name.indexOf("://") != -1)
            {
                fileRow.append("<td><a href='" + files[i].name + "'> "+ files[i].name + "</a></td>");
            }
            else
            {
                fileRow.append("<td>" + files[i].name + "</td>");
            }
            var delButton = $("<img class='images delBtn' src='/gp/images/delete-blue.png'/>");
            delButton.data("pfile", files[i].name);
            delButton.data("pfileId", files[i].id);

            delButton.button().click(function()
            {
                // Show the buttons again
                var fileDiv = $(this).closest(".fileDiv");
                fileDiv.find("> button, > span").show();

                var file = $(this).data("pfile");
                var id = $(this).data("pfileId");

                var paramName = $(this).parents(".pRow").first().attr("id");
                var groups = parameter_and_val_groups[paramName].groups;
                for(var group in groups)
                {
                    var param_files = groups[group].files;
                    for(var t=0;t<param_files.length;t++)
                    {
                        if(param_files[t].name == file
                            && param_files[t].id == id)
                        {
                            var fileObjListing = param_files;
                            fileObjListing.splice(t, 1);
                            updateFilesForGroup(group, paramName, fileObjListing);
                        }
                    }
                }
                updateParamFileTable(paramName, fileDiv);
                toggleFileButtons(paramName);
            });

            fileTData.append(delButton);
            fileRow.append(fileTData);
            tbody.append(fileRow);
        }

        var div = $("<div class='scroll'/>");
        div.append(table);
        fileListingDiv.append(div);

        tbody.find("tr").draggable({
            helper: function(event) {
                var object = $('<div class="drag-row"><table class="paramFilesTable"></table></div>')
                    .find('table').append($(event.target).closest('tr').clone()).end();
                var rowIndex = $(event.target) // Get the closest tr parent element
                    .closest('tr')
                    .prevAll() // Find all sibling elements in front of it
                    .length;

                object.append("<input type='hidden' name='rowindex' value='" + rowIndex + "' />");
                return object;
            }
        });

        //set visibility of the file listing to hidden if that was its previous state
        // by default the file listing is visible
        if(hideFiles)
        {
            editLink.click();
        }
    }

    // Hide or show the buttons if something is selected
    if (!isBatch(paramName) && atMaxFiles(paramName)) {
        fileDiv.find("> button, > span").hide();
    }
    else {
        fileDiv.find("> button, > span").show();
    }
}

function getFileCountForParam(paramName)
{
    var groups = parameter_and_val_groups[paramName].groups;
    var count = 0;

    if(groups != undefined || groups != null)
    {
        for(var group in groups)
        {
            if(groups[group].files != undefined && groups[group].files != null)
            {
                count += groups[group].files.length;
            }
        }
    }

    return count;
}
function atMaxFiles(paramName) {
    //if this ia a batch parameter then allow mutiple files to be provided
    if(isBatch(paramName))
    {
        return false;
    }

    var currentNum = getFileCountForParam(paramName);

    var paramDetails = run_task_info.params[paramName];

    if (currentNum === paramDetails.maxValue) {
        return true;
    }
    else {
        return false;
    }
}


function uploadAllFiles()
{
    if (!allFilesUploaded())
    {
        $('#runTaskSettingsDiv').block({
            message: '<h1> Please wait... </h1>',
            overlayCSS: { backgroundColor: '#F8F8F8' }
        });

        $("#cancelUpload").show();
        var count =0;

        var parameterNames = Object.keys(parameter_and_val_groups);
        for(var p=0;p<parameterNames.length;p++)
        {
            var paramName = parameterNames[p];
            var groupIds = Object.keys(parameter_and_val_groups[paramName].groups);
            for(var g=0;g<groupIds.length;g++)
            {
                var groupId = groupIds[g];
                var group = parameter_and_val_groups[paramName].groups[groupId];

                if(group.files != undefined && group.files != null)
                {
                    for(var f=0;f<group.files.length;f++)
                    {
                        var fileObj = group.files[f];
                        if(fileObj.object != undefined && fileObj.object != null)
                        {
                            count++;
                            uploadFile(paramName, fileObj.object, f, count, groupId);
                        }
                    }
                }
            }
        }
    }
}

// upload file
function uploadFile(paramName, file, fileOrder, fileId, groupId)
{
    var id = fileId;
    fileId = "file_" + fileId;
    $("#fileUploadDiv").append("<div id='" + fileId + "'/>");
    $("#"+fileId).before("<p>" + file.name + "</p>");
    $("#"+fileId).append("<div class='progress-label' id='" + fileId + "Percentage'/>");

    //initialize the progress bar to 0
    $("#"+fileId).progressbar({
        value: 0
    });
    $("#"+fileId + "Percentage").text("0%");

    var destinationUrl = "/gp/rest/RunTask/upload";

    // prepare FormData
    var formData = new FormData();
    formData.append('ifile', file);
    formData.append('paramName', paramName);
    formData.append('index', id);

    var progressEvent = function(event) {
        if (event.lengthComputable)
        {

            var percentComplete = Math.round(event.loaded * 100 / event.total);
            console.log("percent complete: " + percentComplete);

            $("#"+fileId).progressbar({
                value: percentComplete
            });

            $("#"+fileId + "Percentage").text(percentComplete.toString() + "%");
        }
        else
        {
            $("#fileUploadDiv").append('<p>Unable to determine progress</p>');
        }
    };

    var xhr = null;
    $.ajax({
        type: "POST",
        cache: false,
        url: destinationUrl,
        data: formData,
        processData: false,
        contentType: false,
        xhr: function() {
            xhr = new window.XMLHttpRequest();
            //Upload progress
            if (xhr.upload) {
                xhr.upload.addEventListener("progress", progressEvent, false);
            }

            return xhr;
        },
        success: function(event) {
            console.log("on load response: " + event);

            var parsedEvent = typeof event === "string" ? $.parseJSON(event) : event;

            var groupFileInfo = getFilesForGroup(groupId, paramName);
            groupFileInfo[fileOrder].name = parsedEvent.location;
            delete groupFileInfo[fileOrder].object;

            updateFilesForGroup(groupId, paramName, groupFileInfo);

            if(allFilesUploaded())
            {
                $("#cancelUpload").hide();
                submitTask();
            }

        },
        error: function(event) {
            $("#cancelUpload").trigger("click");
            $("#fileUploadDiv").html("<span style='color:red;'>Error uploading file. This may be due to an incompatible browser, such as Internet Explorer. If so, please use a supported browser (Chrome, Firefox, Safari) or use the Java uploader in the Uploads Tab.</span>");
            $("#fileUploadDiv").show();
            console.log("Error uploading the file " + file.name + " :" + event.statusText);
        }
    });

    //keep track of all the upload requests so that they can be canceled
    //using the cancel button
    fileUploadRequests.push(xhr);
}

/*
add the list of file paths/urls specified for file parameters which will be sent to the server
*/
function setAllFileParamValues()
{
    //now set the final file listing values for the file parameters
    var paramNames = Object.keys(parameter_and_val_groups);
    for(var p=0;p<paramNames.length;p++)
    {
        var paramName = paramNames[p];
        var groups = parameter_and_val_groups[paramName].groups;
        if(groups == null)
        {
            javascript_abort("Error: could not retrieve groups for parameter " + paramName);
        }
        var groupNames = Object.keys(groups);
        for(var g=0; g <groupNames.length;g++)
        {
            var param_files = parameter_and_val_groups[paramName].groups[groupNames[g]].files;
            var param_value_listing = parameter_and_val_groups[paramName].groups[groupNames[g]].values;

            if(param_value_listing != null &&
                param_value_listing != undefined &&
                param_value_listing.length > 0 || (param_files == undefined || param_files == null))
            {
                //check if value already set from a choice list and this is a file parameter
                continue;
            }

            var fileList = [];

            for(var f=0; f < param_files.length; f++)
            {
                var nextFileObj = param_files[f];

                fileList.push(nextFileObj.name);
            }
            updateValuesForGroup(groupNames[g], paramName, fileList);
        }
    }
}

function isBatch(paramName) {
    var paramDetails = run_task_info.params[paramName];
    if(paramDetails == null)
    {
        javascript_abort("No info found for parameter " + paramName);
    }

    return paramDetails.isBatch;

}

function makeBatch(paramName) {
    var selector = "#" + jqEscape(paramName);
    var input = $(selector);
    input.closest(".fileDiv").find(".ui-buttonset").find("label:last").click();
}

function allFilesUploaded()
{
    for(var paramNames in parameter_and_val_groups)
    {
        var groups = parameter_and_val_groups[paramNames].groups;
        for(var groupId in groups)
        {
            var files = groups[groupId].files;
            for(var fileObjIndex in files)
            {
                //check if any file objects still exist
                if(files[fileObjIndex].object != undefined && files[fileObjIndex].object != null)
                {
                    return false;
                }
            }
        }
    }
    return true;
}

function javascript_abort(message)
{

    var abortMsg = 'This is not an error. This is just to abort javascript';

    if(message != undefined && message != null && message != "")
    {
        abortMsg = "Request to abort javascript execution: " + message;
        alert(abortMsg);
    }

    console.log(abortMsg);

    throw new Error(abortMsg);
}

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function setParameter(paramName, value, groupId)
{
    var paramDetails = run_task_info.params[paramName];

    if(paramDetails == undefined || paramDetails == null)
    {
        alert("Unable to set the parameter value for " + paramName);
    }

    var paramRow = $("#" + jqEscape(paramName));
    if($.inArray(field_types.CHOICE, paramDetails.type) != -1)
    {
        paramRow.find("paramValueTd").find(".choice").first().val(value);
        paramRow.find("paramValueTd").find(".choice").first().multiselect("refresh");
        return
    }

    if($.inArray(field_types.FILE, paramDetails.type) != -1)
    {
        setInputField(paramName, value, groupId);
        return;
    }

    if($.inArray(field_types.TEXT, paramDetails.type) != -1)
    {
        paramRow.find(".paramValueTd").find(".textDiv").find(".pValue").first().val(value);
        return;
    }

    throw new Error("Parameter type not recognized for: " + param);
}

function toggleFileButtons(paramName) {
    var paramDetails = run_task_info.params[paramName];

    var maxValue = parseInt(paramDetails.maxValue);
}

function getUsername() {
    var parts = $("#systemMessageLink td").text().split(" ");
    return $.trim(parts[parts.length-1]);
}

function cloneTask() {
    var cloneName = window.prompt("Name for cloned module", "copyOf" + run_task_info.name);
    if (cloneName == null || cloneName.length == 0) {
        return;
    }
    window.location.href = "/gp/saveTask.jsp?clone=1&name=" + run_task_info.name +
        "&LSID=" + encodeURIComponent(run_task_info.lsid) +
        "&cloneName=" + encodeURIComponent(cloneName) +
        "&userid=" + encodeURIComponent(getUsername()) +
        "&forward=" + encodeURIComponent("/gp/pages/index.jsf?lsid=" + encodeURIComponent(cloneName));
}