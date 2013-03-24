//map of input parameters to a listing of file objs (local, external urls, internal urls)
// a file object contains a name and and also an object, if the file will need to be uploaded
var param_file_listing ={};

//contains info about the current selected task
var run_task_info = {};

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
 	parameter: function(name) {
 		return unescape(this.parameters()[name]);
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

function loadModule(taskId)
{
     $.ajax({
            type: "GET",
            url: "/gp/rest/RunTask/load",
            data: { "lsid" : taskId },
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
                    loadModuleInfo(response["module"]);
                    parametersJson = response["parameters"];
                    loadParameterInfo(parametersJson);
                }
            },
            error: function(xhr, ajaxOptions, thrownError)
            {
                console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
                console.log(thrownError);

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
                alert("An error occurred while loading module versions.\nInvalid lsid: " + moduleVersionLsidList[v]);
            }

            var version = versionnum.substring(index+1, versionnum.length);
            var modversion = "<option value='" + versionnum + "'>" + version + "</option>";
            $('#task_versions').append(modversion);

            if(module["lsidVersions"][v] == run_task_info.lsid)
            {
                $('#task_versions').val(versionnum).attr('selected',true);
            }
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


    $("#export").click(function()
    {
         var exportLink = "/gp/makeZip.jsp?name=" + run_task_info.lsid;
        $("#export").attr("href", exportLink);

    });

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
    else
    {
        $("#description_main").hide();
    }
    
    //if module has doc specified or if for some reason
    // the hasDoc field was not set then show the doc link
    if(module["hasDoc"] == undefined || module["hasDoc"])
    {
        var docLink = "/gp/getTaskDoc.jsp?name=" + run_task_info.lsid;
        $("#documentation").attr("href", docLink);

        if(hasDescription)
        {
            $("#documentation").parent().prepend(" ... ");
        }
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

        $("#otherOptionsSubMenu").prepend("<li><a href='JavaScript:Menu.denyIE(\"" + editLink + "\");'>Edit</a></li>");
    }

    

    //check if there are missing tasks (only applies to pipelines)
    if(module["missing_tasks"])
    {
        $("#missingTasksDiv").append("<p>WARNING: This pipeline requires modules or module " +
                                     "versions which are not installed on this server.</p>");
        var installTasksButton = $("<button> Install missing tasks</button>");
        installTasksButton.button().click(function()
        {
            window.location.replace("/gp/viewPipeline.jsp?name=" + run_task_info.lsid);
        });

        $("#missingTasksDiv").append(installTasksButton);

        $(".submitControlsDiv").hide();

        javascript_abort();
    }
}

function loadParameterInfo(parameters)
{
    var paramsTable = $("#paramsTable");
    var inputFileRowIds = [];
    for(var q=0; q < parameters.length;q++)
    {
        var paramRow = $("<tr class='pRow'/>");
        var parameterName = parameters[q].name;

        //use the alternate name if there is one (this is usually set for pipelines)
        if(parameters[q].altName != undefined
                && parameters[q].altName != null
                && parameters[q].altName.replace(/ /g, '') != "") //trims spaces
        {
            parameterName = parameters[q].altName;
        }

        if(parameters[q].optional.length == 0)
        {
           parameterName += "*";
        }

        //replace . with spaces in parameter name
        parameterName = parameterName.replace(/\./g,' ');
        paramRow.append("<td class='pTitle'>" + parameterName + "</td>");
        paramRow.data("pname", parameters[q].name);

        var rowId = "pRow" + (q+1);
        var valueTd = $("<td/>");

        var choicelist = parameters[q].value;
        if(choicelist !== undefined && choicelist !== null && choicelist.length > 0)
        {
            var choiceResult = choicelist.split(';');
            var select = $("<select id='"+ parameters[q].name +"' name='"
                    + parameters[q].name+ "'/>");

            for(var p=0;p<choiceResult.length;p++)
            {
                var value = "";
                var rowdata = choiceResult[p].split("=");
                if(rowdata != undefined && rowdata != null && rowdata.length > 1)
                {
                    value = rowdata[0];
                    select.append("<option value='"+ value +"'>" + rowdata[1]+ "</option>");
                }
                else
                {
                    value = choiceResult[p];
                    select.append("<option value='"+value +"'>" + value + "</option>");
                }
            }

            select.children("option").each(function()
            {
                if($(this).val() == parameters[q].default_value)
                {
                    $(this).parent().val(parameters[q].default_value);

                    console.log("default value is : " + parameters[q].default_value);
                }
            });

            //initially select the first choice
            select.change(function ()
            {
                var valueList = [];
                valueList.push($(this).val());

                var paramName = $(this).attr("name");
                parameter_and_val_obj[paramName] = valueList;
                console.log("param name: " + paramName);
            });

            valueTd.append(select);
            paramRow.append(valueTd);
            paramsTable.append(paramRow);

            select.multiselect({
                multiple: false,
                header: false,
                selectedList: 1,
                classes: 'mSelect'
            });

            if(parameters[q].optional.length == 0)
            {
                select.addClass("required");
            }

            var valueList = [];
            valueList.push(select.val());
            parameter_and_val_obj[parameters[q].name] = valueList;
        }
        else if(parameters[q].type == "java.io.File")
        {
            inputFileRowIds.push(rowId);

            var uploadFileText = "Upload Files...";
            var addUrlText = "Add Paths or URLs...";
            if(parameters[q].maxValue != undefined
                    && parameters[q].maxValue != null)
            {
                if(parseInt(parameters[q].maxValue) == 1)
                {
                    uploadFileText = "Upload File...";
                    addUrlText = "Add Path or URL...";
                }
            }

            var fileDiv = $("<div id='"+ rowId +"' class='fileDiv'>");

            var uploadFileBtn = $("<button class='uploadBtn' type='button'>"+ uploadFileText + "</button>");
            uploadFileBtn.button().click(function()
            {
                console.log("uploadedfile: " + $(this).siblings(".uploadedinputfile").first());
                $(this).parents("div:first").find(".uploadedinputfile:first").click();
            });

            fileDiv.append(uploadFileBtn);

            var fileInput = $("<input class='uploadedinputfile' id='" + parameters[q].name + "' name='"+ parameters[q].name +"' type='file'/>");

            var fileInputDiv = $("<div class='inputFileBtn'/>");
            fileInputDiv.append(fileInput);
            fileDiv.append(fileInputDiv);
            
            fileDiv.append("<button type='button' class='urlButton'>"+ addUrlText +"</button>");
            
            fileDiv.append("<span>  or  <img class='dNdImg' src='/gp/images/Drag_Drop_icon.gif'/> </span>");

            //switch . with _ since the jquery selector does not work with .
            var idPName = parameters[q].name.replace(/\./g,'_');
            fileDiv.append("<div id='" + idPName + "FileDiv'/>");

            valueTd.append(fileDiv);
            paramRow.append(valueTd);
            paramsTable.append(paramRow);
        }
        else
        {
            console.log("text default value: " + parameters[q].default_value);
            var $textField = $("<input type='text' id='" + parameters[q].name
                        +"' name='" + parameters[q].name + "'/>");
            $textField.change(function ()
            {
                var valueList = [];
                valueList.push($(this).val());

                var paramName = $(this).attr("name");
                parameter_and_val_obj[paramName] = valueList;
                console.log("text field param name: " + paramName);
            });
            $textField.val(parameters[q].default_value);

            var textValueList = [];
            textValueList.push($textField.val());
            parameter_and_val_obj[parameters[q].name] = textValueList;

            valueTd.append($textField);
            paramRow.append(valueTd);
            paramsTable.append(paramRow);

            if(parameters[q].optional.length == 0)
            {
                $textField.addClass("required");
            }
        }

        //append parameter description table
        paramsTable.append("<tr class='paramDescription'><td></td><td colspan='3'>" + parameters[q].description +"</td></tr>");
    }

    for(var r=0;r<inputFileRowIds.length;r++)
    {
        var dropbox = document.getElementById(inputFileRowIds[r]);

        // init event handlers
        dropbox.addEventListener("dragenter", dragEnter, true);
        dropbox.addEventListener("dragleave", dragLeave, true);
        dropbox.addEventListener("dragexit", dragExit, false);
        dropbox.addEventListener("dragover", dragOver, false);
        dropbox.addEventListener("drop", drop, false);
    }

    $("button.urlButton").button().click(function()
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

            //check if max file length will be violated
            var totalFileLength = fileObjListings.length + 1;
            validateMaxFiles(paramName, totalFileLength);

            var fileObj = {
                name: url,
                id: fileId++
            };
            fileObjListings.push(fileObj);

            // add to file listing for the specified parameter
            updateParamFileTable(paramName);
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

        //first hide everything in this td parent element
        //$(this).parents("td:first").children().hide();

        $("#dialogUrlDiv").append(urlDiv);
        openServerFileDialog(this);
    });
    
    // Load parameter values from url
    loadGetParams();
}

jQuery(document).ready(function()
{
    /*jQuery.validator.addMethod("requireInputFile",function(value) {
    total = parseFloat($('#LHSOPERAND').val()) + parseFloat($('#RHSOPERAND').val());
    return total == parseFloat($('#TOTAL').val());
    }, "Amounts do not add up!");

    jQuery.validator.classRuleSettings.checkTotal = { checkTotal: true };
    */

    //Add tabs once properties page is redone
    //$("#submitJob").tabs();

    $("#runTaskForm").validate(
    {
        /*highlight: function(label) {
            $(this).closest('tr.pRow').addClass('error');
        },
        success: function(label) {
            $(this).closest('tr.pRow').removeClass('error');
        } */
    });

    $("#mod_description_hidden").hide();
    $("#mod_description_toggle").click(function()
    {
        var descImg = this;
        if($("#mod_description").is(":visible"))
        {
            descImg.src = descImg.src.replace("minus","plus");
        }
        else
        {
            descImg.src = descImg.src.replace("plus","minus");
        }

        $("#mod_description_hidden").toggle();                                            
        $("#mod_description").toggle();
    });
    
    $("#toggleDesc").click(function()
    {
        //show descriptions
        $("#paramsTable tr.paramDescription").toggle();
    });

    $("#otherOptionsMenu").jMenu(
    {
        absoluteTop: 19,
        absoluteLeft: -214,
        openClick: true
    });

    $("button").button();

    var lsid = Request.parameter('lsid');
    if(lsid == undefined || lsid == null || lsid  == "")
    {
        //lsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9";
        //redirect to splash page
        window.location.replace("/gp/pages/index.jsf");
    }
    else
    {
        loadModule(lsid);
    }

    $("input[type='file']").live("change", function()
    {
        var paramName = $(this).attr("name");

        var fileObjListings = param_file_listing[paramName];
        if(fileObjListings == null || fileObjListings == undefined)
        {
            fileObjListings = [];
            param_file_listing[paramName] = fileObjListings;
        }

        //check if max file length will be vialoated
        var totalFileLength = fileObjListings.length + this.files.length;
        validateMaxFiles(paramName, totalFileLength);

        //add newly selected files to table of file listing
        for(var f=0; f < this.files.length; f++)
        {
            var fileObj = {
                name: this.files[f].name,
                object: this.files[f],
                id: fileId++
            };
            fileObjListings.push(fileObj);
        }

        // add to file listing for the specified parameter
        updateParamFileTable(paramName);
        toggleFileButtons(paramName);
    });

    var selected = function( event, ui ) {
        $(this).popup( "close" );
    };


   /*$('#otherOptions').button({
        text: false,
        icons: {
            primary: "otherOptions"   // Custom icon
        }
    }); */

    $('#otherOptions').iconbutton({
        text: false,
        icons: {
            primary: "otherOptions"   // Custom icon
        }
    }).click(function()
    {
        $( "#menu" ).toggle();    
    });

    $( "#menu" ).hide().menu({
        select: selected,
        trigger: $("#otherOptions")
    });


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
});

function reset()
{

    $("#paramsTable").empty();
    loadParameterInfo(parametersJson);
}

function runJob()
{
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

function submitTask()
{
    setAllFileParamValues();

    if(!$('#runTaskForm').validate().form())
    {
        return;
    }
    //Change text of blocking div
    $('#runTaskSettingsDiv').block({
        message: '<h1> Submitting job...</h1>',
        overlayCSS: { backgroundColor: '#F8F8F8'}
    });

    console.log("submitting task");

    var taskJsonObj =
    {
        "lsid" : run_task_info.lsid,
        "params" : JSON.stringify(parameter_and_val_obj)
    };

    $.ajax({
        type: "POST",
        url: "/gp/rest/RunTask/addJob",
        contentType: 'application/json',
        data: JSON.stringify(taskJsonObj),
        success: function(response) {

            var message = response["MESSAGE"];

            if (message !== undefined && message !== null) {
                alert(message);
            }

            window.location.replace("/gp/jobResults/"+response.jobId);
            console.log("Response text: " + response.text);
        },
        error: function(xhr, ajaxOptions, thrownError)
        {
            alert("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            alert(thrownError);
            console.log("Error: " + xhr.responseText);
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

    var files = evt.dataTransfer.files;
    var count = files.length;

    //console.log("files in text: " + evt.dataTransfer.getData('Text'));
    //console.log("file count: " + count);

    var target = $(evt.target);
    console.log("tData" + target.html());
    var paramName = target.parents(".pRow").first().data("pname");
    if(paramName == undefined)
    {
        console.log("Error: Could not find the parameter this file belongs to.");
        return;
    }

    // Only call the handler if 1 or more files was dropped.
    if (count > 0)
    {
        handleFiles(files, paramName);
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

            updateParamFileTable(paramName);
            toggleFileButtons(paramName);
        }
    }
}

function handleFiles(files, paramName)
{
    var fileObjListings = param_file_listing[paramName];
    if(fileObjListings == null || fileObjListings == undefined)
    {
        fileObjListings = [];
        param_file_listing[paramName] = fileObjListings;
    }

    //check if max file length will be vialoated
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
    updateParamFileTable(paramName);
    toggleFileButtons(paramName);
}

function validateMaxFiles(paramName, numFiles)
{
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
        //in this case the max num of files must be unlimited
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
        alert("The maximum number of files that can be provided to " +
                        "this parameter has been reached. Please delete some files " +
                        "before continuing.");
        throw new Error("The maximum number of files that can be provided to " +
                        "this parameter has been reached. Please delete some files " +
                        "before continuing.");
    }
}

function updateParamFileTable(paramName)
{
    var files= param_file_listing[paramName];

    var idPName = paramName.replace(/\./g,'_');
    idPName = "#" + idPName + "FileDiv";

    //remove previous file info data
    $(idPName).empty();
    if(files != null && files != undefined && files.length > 0)
    {
        var pData = $("<div class='fileDetails'/>");

        var editLink = $("<a href='#'><img src='/gp/images/arrows-down.gif'/>Hide Details...</a>");
        editLink.click(function(event)
        {
            event.preventDefault();

            var editLinkMode = $(this).text();
            if(editLinkMode == "Show Details...")
            {
                $(this).text("Hide Details...");
                $(this).prepend("<img src='/gp/images/arrows-down.gif'/>");
                $(idPName).find(".paramFilesTable").removeClass("hidden");
            }
            else
            {
                $(this).text("Show Details...");
                $(this).prepend("<img src='/gp/images/arrows.gif'/>");
                $(idPName).find(".paramFilesTable").addClass("hidden");
            }
        });
        pData.append(editLink);

        var selectedFiles = "Selected ";
        selectedFiles += files.length + " files";

        pData.append("(" + selectedFiles + ")");
        $(idPName).append(pData);

        if(files.length > 0)
        var table = $("<table class='paramFilesTable'/>");
        for(var i=0;i<files.length;i++)
        {
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

                updateParamFileTable(paramName);
                toggleFileButtons(paramName);
            });

            fileTData.append(delButton);
            fileRow.append(fileTData);
            table.append(fileRow);
        }

        var div = $("<div class='scroll'/>");
        div.append(table);
        $(idPName).append(div);

        editLink.click();
    }
}


function uploadAllFiles()
{
    //disable the parameter listing table so that no changes can be made
    //$("#paramsTable").find("input,button,textarea,select").attr("disabled", "disabled");

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
    $("#"+fileId).after("<p id='" + fileId + "Percentage'/>");

    //initialize the progress bar to 0
    $("#"+fileId).progressbar({
        value: 0
    });
    $("#"+fileId + "Percentage").append("0%");

    var destinationUrl = "/gp/rest/RunTask/upload";
    // prepare XMLHttpRequest
    var xhr = new XMLHttpRequest();
    xhr.open('POST', destinationUrl);
    xhr.onload = function() {
        console.log("on load response: " + this.responseText);

        var response = $.parseJSON(this.responseText);

        //add location to value listing for parameter
        //addValueToParameter(paramName, response.location);

        param_file_listing[paramName][fileOrder].name = response.location;
        delete param_file_listing[paramName][fileOrder].object;

        if(allFilesUploaded())
        {
            $("#cancelUpload").hide();
            submitTask();
        }

    };
    xhr.onerror = function() {
        result.textContent = this.responseText;
        console.log("Error uploading the file " + file.name + " :" + this.responseText);
    };
    xhr.upload.onprogress = function(event) {
        if (event.lengthComputable)
        {

            var percentComplete = Math.round(event.loaded * 100 / event.total);
            console.log("percent complete: " + percentComplete);

            $("#"+fileId).progressbar({
                value: percentComplete
            });

            $("#"+fileId + "Percentage").empty();
            $("#"+fileId + "Percentage").append(percentComplete.toString() + "%");
        }
        else
        {
            $("#fileUploadDiv").append('<p>Unable to determine progress</p>');
        }
    }

    xhr.upload.onloadstart = function(event) {
        console.log("onload start support file upload");
    }

    // prepare FormData
    var formData = new FormData();
    formData.append('ifile', file);
    formData.append('paramName', paramName);
    formData.append('index', id);
    xhr.send(formData);

    //keep track of all teh upload request so that they can be canceled
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
        for(var f=0; f < param_file_listing[paramName].length; f++)
        {
            var nextFileObj = param_file_listing[paramName][f];

            fileList.push(nextFileObj.name);
        }

        parameter_and_val_obj[paramName] = fileList;
    }
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

function javascript_abort()
{
   throw new Error('This is not an error. This is just to abort javascript');
}

function jqEscape(str) {
	return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function setParameter(param, value) {
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
}

function loadGetParams() {
	var file = null;
	var format = null;
	
	for (var param in Request.parameters()) {
    	var value = Request.parameter(param);
    	
    	if (value !== undefined && value !== null && param !== "lsid" && param !== "_file" && param !== "_format") {
    		setParameter(param, value);
    	}
    	else if (param === "_file") {
    		file = value;
    	}
    	else if (param === "_format") {
    		format = value;
    	}
    	
    	if (file !== null && format !== null) {
    		assignParameter(file, format);
    		
    		// Already set passed in parameter, reset variables
    		file = null;
    		format = null;
    	}
    }
}

function assignParameter(file, format) {
	for (var json in parametersJson) {
		var param = parametersJson[json];
		if (param === null || param === undefined) return;
		if (param.type !== "java.io.File" && param.type !== "DIRECTORY") continue;
		
		var formatList = param.fileFormat.split(";");
		for (var i = 0; i < formatList.length; i++) {
			var selectedFormat = formatList[i];
			if (selectedFormat === format) {
				setParameter(param.name, file);
				return;
			}
			else if (format === "directory" && param.type === "DIRECTORY") {
				setParameter(param.name, file);
				return;
			}
		}
	}
}

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
	
	// Disable buttons if max reached
    var fileObjListings = param_file_listing[paramName];
    if (fileObjListings == null || fileObjListings == undefined) {
        fileObjListings = [];
    }
    if (fileObjListings.length === maxValue) {
    	$("[id='" + paramName + "']").parent().parent().find("button").button("disable")
    }
    else {
    	$("[id='" + paramName + "']").parent().parent().find("button").button("enable")
    }
}

$.widget('ui.iconbutton', $.extend({}, $.ui.button.prototype, {
    _init: function() {
        $.ui.button.prototype._init.call(this);
        this.element.removeClass('ui-corner-all')
                    .addClass('ui-iconbutton')
                    .unbind('.button');
    }
}));