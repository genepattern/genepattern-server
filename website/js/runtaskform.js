//map of input parameters to a listing of file objects
// a file object contains a name and and also an input  file object, if the file will need to be uploaded
var param_file_listing ={};

var field_types = {
    FILE: 1,
    CHOICE: 2,
    TEXT: 4,
    PASSWORD: 5
};

//contains info about the current selected task
var run_task_info = {
    lsid: null,
    name: null,
    params: {}
};

//contains json object with parameter to value pairing
var parameter_and_val_obj = {};

//contains all the file upload requests
var fileUploadRequests = [];

//the field is used to assign unique ids to each file provided for a file input parameter
//in order to make it easier to delete files
var fileId = 0;
//contains the json of parameters received when loading a module
//saved so it can reused when for the reset operation
var parametersJson = null;

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
    }
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
                else
                {
                    parametersJson = response["parameters"];
                    loadParameterInfo(parametersJson, response["initialValues"]);
                }
                //the parameter form elements have been created now make the form visible
                $("#submitJob").css('visibility', 'visible');

            }
        },
        error: function(xhr, ajaxOptions, thrownError)
        {
            console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            console.log(thrownError);

            $("#submitJob").css('visibility', 'visible');
            $("#submitJob").empty();
            $("#submitJob").append("An error occurred while loading the task " + taskId + ": <br/>" + xhr.responseText);
        },
        dataType: "json"
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
            var x = e.pageX;
            var y = e.pageY;
            var width = $("#source_info_tooltip").width();
            $("#source_info_tooltip").css("top", y -10);
            $("#source_info_tooltip").css("left", x - width/2);
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
            else
            {
                run_task_info.params[parameterInfo.name].type.push(field_types.TEXT);
            }
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

    if(parameterInfo.optional.length == 0 && parameterInfo.minValue != 0)
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

    //set the initial values
    //can be null or undefined if this is not a job reload
    var initialValuesList = null;

    if(initialValues != null && initialValues != undefined)
    {
        initialValuesList = initialValues[parameterInfo.name];
    }
    run_task_info.params[parameterInfo.name].initialValues = initialValuesList;

    //set the display name for the parameter
    setParamDisplayName(parameterInfo);

    //set whether this parameter is required or not
    setParamOptionalOrRequired(parameterInfo);

    //set the type of input field to display
    setParamFieldType(parameterInfo);

    //check if more than one value can be assigned to this parameter
    setAllowMultipleValuesForParam(parameterInfo);

    run_task_info.params[parameterInfo.name].initialValues = initialValuesList;

    //a flag to indicate whether the initial values are found in the drop down list if
    //this is a choice parameter
    run_task_info.params[parameterInfo.name].initialChoiceValues = false;

    run_task_info.params[parameterInfo.name].minValue = parameterInfo.minValue;
    run_task_info.params[parameterInfo.name].maxValue = parameterInfo.maxValue;
    run_task_info.params[parameterInfo.name].default_value = parameterInfo.default_value;
    run_task_info.params[parameterInfo.name].description = parameterInfo.description;
    run_task_info.params[parameterInfo.name].altDescription = parameterInfo.altDescription;
}

function createTextDiv(parameterName, maskInput)
{
    var textDiv = $("<div class='textDiv'/>");

    var paramDetails = run_task_info.params[parameterName];

    var textField = null;
    if(maskInput)
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
        $(this).removeClass('highlight');
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
        parameter_and_val_obj[paramName] = valueList;
    });
    textField.val(paramDetails.default_value);

    var textValueList = [];

    if(textField.val() != "")
    {
        textValueList.push(textField.val());
    }
    parameter_and_val_obj[parameterName] = textValueList;


    if(paramDetails.required == 0 && paramDetails.minValue != 0)
    {
        textField.addClass("requiredParam");
    }

    //select initial values if there are any
    var initialValuesList = paramDetails.initialValues;

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

function createChoiceDiv(parameterName)
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
            parameter_and_val_obj[paramName] = valueList;
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
        var initialValuesList = paramDetails.initialValues;

        if( initialValuesList != undefined &&  initialValuesList != null)
        {
            //should only be one item in the list for now
            //but handle case when there is more than one item
            if(choice.multiselect("option", "multiple"))
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
                    if(!(paramDetails.default_value == "" && initialValuesList[0] == "")
                        && $.inArray(initialValuesList[0], matchingValueList) != -1)
                    {
                        choice.val( initialValuesList[0]);

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
        parameter_and_val_obj[parameterName] = valueList;
    }

    //if this is not a reloaded job where the value was from a drop down list
    if(!run_task_info.params[parameterName].initialChoiceValues)
    {
        selectChoiceDiv.hide();
    }

    return selectChoiceDiv;
}

function createFileDiv(parameterName)
{
    var fileDiv = $("<div class='fileDiv'>");

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
    paramDetails.isBatch = false;

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
            param_file_listing[paramName] = [];
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

            var fileObjListings = param_file_listing[paramName];
            if(fileObjListings == null || fileObjListings == undefined)
            {
                fileObjListings = [];
                param_file_listing[paramName] = fileObjListings;
            }

            var totalFileLength = fileObjListings.length + 1;
            validateMaxFiles(paramName, totalFileLength);

            var fileObj = {
                name: url,
                id: fileId++
            };
            fileObjListings.push(fileObj);

            // add to file listing for the specified parameter
            updateParamFileTable(paramName, $(this).closest(".fileDiv"));
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

        $("#dialogUrlDiv").append(urlDiv);
        openServerFileDialog(this);
    });

    fileDiv.append("<span class='drop-box'>drop files here</span>");
    fileDiv.append("<div class='fileListingDiv'/>");

    //check if there are predefined file values
    var fileObjListings = param_file_listing[parameterName];
    if(fileObjListings == null || fileObjListings == undefined)
    {
        fileObjListings = [];
        param_file_listing[parameterName] = fileObjListings;
    }

    var initialValuesList = paramDetails.initialValues;
    //also check if this parameter is also a choice parameter
    if( initialValuesList != undefined &&  initialValuesList != null
        && initialValuesList.length > 0 && !run_task_info.params[parameterName].initialChoiceValues)
    {
        //check if max file length will be violated
        var totalFileLength = fileObjListings.length +  initialValuesList.length;
        validateMaxFiles(parameterName, totalFileLength);

        for(var v=0; v <  initialValuesList.length; v++)
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

        param_file_listing[parameterName] = fileObjListings;
        updateParamFileTable(parameterName, fileDiv);
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
        param_file_listing[pname] = [];
        parameter_and_val_obj[pname] = [];
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

function loadParameterInfo(parameters, initialValues)
{
    var paramsTable = $("#paramsTable");
    for(var q=0; q < parameters.length;q++)
    {
        var parameterName = parameters[q].name;
        initParam(parameters[q], q, initialValues);

        var paramRow = $("<tr id='" + parameterName + "' class='pRow'/>");
        paramRow.data("pname", parameterName);

        paramsTable.append(paramRow);

        //now we know what we need to build
        //so add a row in the table for this parameter

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

        //create the necessary field types for this parameter
        if($.inArray(field_types.CHOICE, run_task_info.params[parameterName].type) != -1)
        {
            valueTd.append(createChoiceDiv(parameterName));
        }

        if($.inArray(field_types.FILE, run_task_info.params[parameterName].type) != -1)
        {
            valueTd.append(createFileDiv(parameterName));
        }

        if($.inArray(field_types.TEXT, run_task_info.params[parameterName].type) != -1)
        {
            //this must be a text entry
            var maskInput = $.inArray(field_types.PASSWORD, run_task_info.params[parameterName].type) != -1;
            valueTd.append(createTextDiv(parameterName, maskInput));
        }

        if(run_task_info.params[parameterName].type.length > 1)
        {
            //multiple field types specified so add a toggle buttons
            //right now this would only be for a file drop-down parameter
            valueTd.prepend(createModeToggle(parameterName));
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

jQuery(document).ready(function()
{
    $("#toggleDesc").click(function()
    {
        //show descriptions
        $("#paramsTable tr.paramDescription").toggle();
    });

    $("button").button();

    var reloadJob = Request.parameter('reloadJob');
    if(reloadJob == undefined || reloadJob == null)
    {
        reloadJob = "";
    }

    var lsid = Request.parameter('lsid');
    var reloadJob = Request.parameter('reloadJob');

    if((lsid == undefined || lsid == null || lsid  == "")
        && (reloadJob == undefined || reloadJob == null || reloadJob  == ""))
    {
        //redirect to splash page
        window.location.replace("/gp/pages/index.jsf");
    }
    else
    {
        loadModule(lsid, reloadJob);
    }

    $("input[type='file']").live("change", function()
    {
        var paramName = $(this).data("pname");

        var fileObjListings = param_file_listing[paramName];
        if(fileObjListings == null || fileObjListings == undefined)
        {
            fileObjListings = [];
            param_file_listing[paramName] = fileObjListings;
        }

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
        var paramNames = Object.keys(parameter_and_val_obj);
        for(var t=0;t<paramNames.length;t++)
        {
            var valuesList = parameter_and_val_obj[paramNames[t]];
            if(valuesList != undefined && valuesList != null && valuesList.length > 0)
            {
                queryString += "&" + paramNames[t] + "=" + valuesList[0];
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
});

function reset()
{

    $("#paramsTable").empty();

    //remove all input file parameter file listings
    param_file_listing = {};

    loadParameterInfo(parametersJson, null);
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

    var paramNames = Object.keys(parameter_and_val_obj);
    for(var p=0;p<paramNames.length;p++)
    {
        var value = parameter_and_val_obj[paramNames[p]];
        var required = run_task_info.params[paramNames[p]].required;
        //check if it is required and there is no value specified
        if(required && (value == undefined || value == null
            || value.length == 0 || value.length == 1))
        {
            if(value.length == 1)
            {
                if(value[0] != "")
                {
                   break;
                }
            }

            missingReqParameters.push(paramNames[p]);
        }
    }

    paramNames = Object.keys(param_file_listing);
    for(p=0;p<paramNames.length;p++)
    {
        var value = param_file_listing[paramNames[p]];
        var required = run_task_info.params[paramNames[p]].required;

        //check if it is required and there is no value specified
        if(required)
        {
            if(value == undefined || value == null || value.length == 0)
            {
                if($.inArray(paramNames[p], Object.keys(parameter_and_val_obj)) == -1
                && $.inArray(paramNames[p], missingReqParameters) == -1)
                {               //add it to list if it is not already there
                    missingReqParameters.push(paramNames[p]);
                }
            }
            else
            {
                var index = $.inArray(paramNames[p], missingReqParameters);
                if(index != -1)
                {
                    missingReqParameters.splice(index,1);
                }
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
        missingReqParamsDiv.append("<p class='errorMessage'>Please provide a value for the following parameter(s):</p>");

        var pListing = $("<ul class='errorMessage'/>");
        missingReqParamsDiv.append(pListing);

        var errorMessage = "<div class='errorMessage'>This field is required</div>";

        for(p=0;p<missingReqParameters.length;p++)
        {
            var displayname = paramNames[p];

            //check if the parameter has an alternate name
            if(run_task_info.params[paramNames[p]].displayname != undefined
                && run_task_info.params[paramNames[p]].displayname != null
                && run_task_info.params[paramNames[p]].displayname!= "")
            {
                displayname = run_task_info.params[paramNames[p]].displayname;
            }

            pListing.append("<li>"+displayname+"</li>");

            $("#" + jqEscape(run_task_info.params[paramNames[p]])).find(".paramValueTd").addClass("errorHighlight");
            $("#" + jqEscape(run_task_info.params[paramNames[p]])).find(".paramValueTd").append(errorMessage);

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
    for (var paramName in param_file_listing) {
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

    var taskJsonObj =
    {
        "lsid" : run_task_info.lsid,
        "params" : JSON.stringify(parameter_and_val_obj),
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
            $('#runTaskSettingsDiv').unblock();
        },
        dataType: "json"
    });
}

function dragEnter(evt)
{
    this.classList.add('highlight');
    evt.stopPropagation();
    evt.preventDefault();
}

function dragLeave(evt)
{
    this.classList.remove('highlight');
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
    this.classList.add('highlight');
    evt.stopPropagation();
    evt.preventDefault();
}

function drop(evt)
{
    this.classList.remove('highlight');
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
            var fileObjListings = param_file_listing[paramName];
            if(fileObjListings == null || fileObjListings == undefined)
            {
                fileObjListings = [];
                param_file_listing[paramName] = fileObjListings;
            }

            var totalFileLength = fileObjListings.length + 1;
            validateMaxFiles(paramName, totalFileLength);

            var fileObj = {
                name: evt.dataTransfer.getData('Text'),
                id: fileId++
            };
            fileObjListings.push(fileObj);

            updateParamFileTable(paramName, $(this).closest(".fileDiv"));
            toggleFileButtons(paramName);
        }
    }
}

function handleFiles(files, paramName, fileDiv)
{
    var fileObjListings = param_file_listing[paramName];
    if(fileObjListings == null || fileObjListings == undefined)
    {
        fileObjListings = [];
        param_file_listing[paramName] = fileObjListings;
    }

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
    updateParamFileTable(paramName, fileDiv);
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

    var paramJSON = null;
    for(var p=0;p<parametersJson.length; p++)
    {
        if(parametersJson[p].name == [paramName])
        {
            paramJSON = parametersJson[p];
        }
    }

    var maxFilesLimitExceeded = false;

    if(paramJSON != null)
    {
        //in this case the max num of files is not unlimited
        if(paramJSON["maxValue"] != undefined || paramJSON["maxValue"] != null)
        {
            var maxValue = parseInt(paramJSON["maxValue"]);
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

function updateParamFileTable(paramName, fileDiv)
{
    var files = param_file_listing[paramName];

    if(fileDiv == null)
    {
        var paramRow = $("#" + jqEscape(paramName));
        fileDiv = paramRow.find(".fileDiv").first();

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

            param_file_listing[paramName] = files;
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

        if(files.length > 0)
            var table = $("<table class='paramFilesTable'/>");
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
            var delButton = $("<img class='images' src='/gp/images/delete-blue.png'/>");
            delButton.data("pfile", files[i].name);
            delButton.data("pfileId", files[i].id);

            delButton.button().click(function()
            {
                // Show the buttons again
                var fileDiv = $(this).closest(".fileDiv");
                fileDiv.find("> button, > span").show();

                var file = $(this).data("pfile");
                var id = $(this).data("pfileId");
                for(var t=0;t<param_file_listing[paramName].length;t++)
                {
                    if(param_file_listing[paramName][t].name == file
                        && param_file_listing[paramName][t].id == id)
                    {
                        var fileObjListing = param_file_listing[paramName];
                        fileObjListing.splice(t, 1);
                    }
                }

                updateParamFileTable(paramName, fileDiv);
                toggleFileButtons(paramName);
            });

            fileTData.append(delButton);
            fileRow.append(fileTData);
            table.append(fileRow);
        }

        var div = $("<div class='scroll'/>");
        div.append(table);
        fileListingDiv.append(div);

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

function atMaxFiles(paramName) {
    //if this ia a batch parameter then allow mutiple files to be provided
    if(isBatch(paramName))
    {
        return false;
    }

    var currentNum = param_file_listing[paramName].length;

    var maxNum = null;
    jq(parametersJson).each(function(i) {
        var param = parametersJson[i];
        if (param.name === paramName) {
            maxNum = param.maxValue
        }
    });

    if (currentNum === maxNum) {
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

        for(var paramName in param_file_listing)
        {
            for(var f=0; f < param_file_listing[paramName].length; f++)
            {
                var nextFileObj = param_file_listing[paramName][f];
                if(nextFileObj.object != undefined && nextFileObj.object != null)
                {
                    count++;
                    uploadFile(paramName, nextFileObj.object, f, count);
                }
            }
        }
    }
}

// upload file
function uploadFile(paramName, file, fileOrder, fileId)
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
            param_file_listing[paramName][fileOrder].name = parsedEvent.location;
            delete param_file_listing[paramName][fileOrder].object;

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
    for(var paramName in param_file_listing)
    {
        var fileList = [];

        //if there is a value set from a choice already for this file then continue
        if(param_file_listing[paramName].length == 0
            || (parameter_and_val_obj[paramName] != null &&
            parameter_and_val_obj[paramName] != undefined &&
            parameter_and_val_obj[paramName].length > 0))
        {
            //check if value already set from a choice list
            continue;
        }

        for(var f=0; f < param_file_listing[paramName].length; f++)
        {
            var nextFileObj = param_file_listing[paramName][f];

            fileList.push(nextFileObj.name);
        }

        parameter_and_val_obj[paramName] = fileList;
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
    for(var paramName in param_file_listing)
    {
        for(var f=0; f < param_file_listing[paramName].length; f++)
        {
            var nextFileObj = param_file_listing[paramName][f];

            //check if any file objects still exist
            if(nextFileObj.object != undefined && nextFileObj.object != null)
            {
                return false;
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
    }

    console.log(abortMsg);

    throw new Error(abortMsg);
}

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function setParameter(paramName, value)
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
        setInputField(paramName, value);
        return;
    }

    if($.inArray(field_types.TEXT, paramDetails.type) != -1)
    {
        paramRow.find(".paramValueTd").find(".textDiv").find(".pValue").first().val(value);
    }

}

/*function setParameter(param, value) {
    var selector = "#" + jqEscape(param);
    var input = $(selector);
    if (input.length === 0) return;

    // Determine the input type
    var isText = input.attr("type") === "text";
    var isDropdown = input.get(0).tagName === "SELECT";
    var isFile = input.attr("type") === "file";

    if (isText) {
        input.val(value);
        return;
    }

    if (isDropdown) {
        input.find("[value='" + value + "']").attr("selected", "true");
        input.multiselect("refresh");
        return;
    }

    if (isFile) {
        setInputField(param, value);
        return;
    }

    throw new Error("Parameter type not recognized for: " + param);
}*/

function toggleFileButtons(paramName) {
    var paramJSON = null;
    for(var p=0;p<parametersJson.length; p++)
    {
        if(parametersJson[p].name == [paramName])
        {
            paramJSON = parametersJson[p];
        }
    }
    var maxValue = parseInt(paramJSON["maxValue"]);
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