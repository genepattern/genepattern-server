//keeps track of number of files that have been successfully uploaded
var numFilesUploaded;

//contains map of param name to files to upload
var files_to_upload = {};

//map of input parameters to a listing of files (local, external urls, internal urls)
var param_file_listing ={};

//contains info about the current selected task
var run_task_info = {};

//contains json object with parameter to value pairing
var parameter_and_val_obj = {};

//contains all the file upload requests
var fileUploadRequests = [];

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
                    loadParameterInfo(response["parameters"]);
                }
            },
            error: function(xhr, ajaxOptions, thrownError)
            {
                alert("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
                alert(thrownError);
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

    var span = $("<span class='.header'/>");
    span.append(run_task_info.name);

    $("#taskHeaderDiv").append(span);

    /*if(module["lsidVersions"] !== undefined)
    {
        updateModuleVersions(module["lsidVersions"]);
        $('select[name="modversion"]').val(module["LSID"]);
        $('select[name="modversion"]').multiselect("refresh");
    }

    if(module["description"] !== undefined)
    {
        $('textarea[name="description"]').val(module["description"]);
    } */
}

function loadParameterInfo(parameters)
{
    var paramsTable = $("<table id=paramsTable/>");
    var inputFileRowIds = [];
    for(var q=0; q < parameters.length;q++)
    {
        var paramRow = $("<tr class='pRow'/>");
        var parameterName = parameters[q].name;
        
        if(parameters[q].optional.length == 0)
        {
           parameterName += "*";
        }

        //replace . with spaces in parameter name
        parameterName = parameterName.replace(/\./g,' ');
        paramRow.append("<td>" + parameterName + "</td>");
        paramRow.data("pname", parameters[q].name);

        var rowId = "pRow" + (q+1);
        var valueTd = $("<td id='"+ rowId +"'/>");

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
                selectedList: 1
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
            valueTd.addClass("dNd");
            valueTd.append("<span class='btn btn-success fileinput-button'>"
                    + "<span><i class='icon-plus'></i>"
                    + "<img src='../css/images/file_add.gif' width='16' height='16'"
                    + "alt='Upload File'/>Upload Files...</span>"
                    + "<input class='uploadedinputfile' name='"+ parameters[q].name +"' type='file'/></span>");
            //valueTd.append("<span class='btn btn-success fileinput-button urlButton'>"
            //                   + "<span><i class='icon-plus'></i>"
            //                   + "<img src='../css/images/file_add.gif' width='16' height='16'"
            //                   + "alt='Specify URL'/>Specify URL...</span></span>");

            valueTd.append("<button class='urlButton'>Specify URL...</button>");

            valueTd.append("<span>or drag and drop files here...</span>");
            paramRow.append(valueTd);
            paramsTable.append(paramRow);

            //switch . with _ since the jquery selector does not work with .
            var idPName = parameters[q].name.replace(/\./g,'_');
            paramsTable.append("<tr id='" + idPName + "'></tr>");
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
    $("#runTaskForm").append(paramsTable);

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
        event.preventDefault();
        var urlDiv = $("<div/>");

        urlDiv.append("Enter url:");
        var urlInput = $("<input type='text' class='urlInput'/>");
        urlDiv.append(urlInput);

        var enterButton = $("<button>Enter</button>");
        enterButton.button().click(function()
        {
            var pName = $(this).parents("tr:first").data("pname");

            var url = $(this).prev().val();
            $(this).parents("td:first").children().show();

            $(this).parents("div:first").remove();

            var fileListing = param_file_listing[pName];
            if(fileListing == null || fileListing == undefined)
            {
                fileListing = [];
            }

            fileListing.push(url);
            param_file_listing[pName] = fileListing;
            updateParamFileTable(pName);
           
        });
        urlDiv.append(enterButton);

        var cancelButton = $("<button>Cancel</button>");
        cancelButton.button().click(function()
        {
            $(this).parents("td:first").children().show();
            $(this).parents("div:first").remove();
        });
        urlDiv.append(cancelButton);

        //first hide everything in this td parent element
        $(this).parents("td:first").children().hide();

        $(this).parents("td:first").append(urlDiv);
    });
}

jQuery(document).ready(function()
{
    /*jQuery.validator.addMethod("requireInputFile",function(value) {
    total = parseFloat($('#LHSOPERAND').val()) + parseFloat($('#RHSOPERAND').val());
    return total == parseFloat($('#TOTAL').val());
    }, "Amounts do not add up!");

    jQuery.validator.classRuleSettings.checkTotal = { checkTotal: true };
    */
    
    $("#runTaskForm").validate(
    {
        /*highlight: function(label) {
            $(this).closest('tr.pRow').addClass('error');
        },
        success: function(label) {
            $(this).closest('tr.pRow').removeClass('error');
        } */
    });

    $("#toggleDesc").click(function()
    {
        //show descriptions
        $("#paramsTable tr.paramDescription").toggle();
    });

    $("button").button();

    //$(".submitControlsDiv").clone().appendTo("body");
    var lsid = Request.parameter('lsid');
    if(lsid == null || lsid  == "")
    {
        lsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9";
    }

    loadModule(lsid);

    $("input[type='file']").live("change", function()
    {
        var paramName = $(this).attr("name");
        var uploadFileList = files_to_upload[paramName];
        if(uploadFileList == null || uploadFileList == undefined)
        {
            uploadFileList = [];
        }

        var fileListing = param_file_listing[paramName];
        if(fileListing == null || fileListing == undefined)
        {
            fileListing = [];
        }

        //add newly selected files to table of file listing
        for(var f=0; f < this.files.length; f++)
        {
            uploadFileList.push(this.files[f]);
            fileListing.push(this.files[f].name);
        }

        // add to list of files to upload and file listing for
        // the specified parameter
        files_to_upload[paramName] = uploadFileList;
        param_file_listing[paramName] = fileListing;
        updateParamFileTable(paramName);
    });

    $("button.Reset").click(function()
    {
        //TODO: implement Reset function
        alert("Not Implemented");
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

function runJob()
{
    //Step 1: upload all the input files if there are any
    if(files_to_upload != null && Object.keys(files_to_upload).length > 0)
    {
        uploadAllFiles();
    }
    else
    {
        //Step 2: run the job
        //TODO: implement job submission
        submitTask();
    }
}

function submitTask()
{
    if(!$('#runTaskForm').validate().form())
    {
        return;
    }
    //Change text of blocking div
    $('#runTaskSettingsDiv').block({
        message: '<h1> Submitting job...</h1>',
        overlayCSS: { backgroundColor: '#F8F8F8'}
    });

    //TODO: add call to run job
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
        complete:function()
        {
            $('#runTaskSettingsDiv').unblock();
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

    console.log("files in text: " + evt.dataTransfer.getData('Text'));
    console.log("file count: " + count);

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
            var fileListing = param_file_listing[paramName];
            if(fileListing == null || fileListing == undefined)
            {
                fileListing = [];
            }

            fileListing.push(evt.dataTransfer.getData('Text'));
            param_file_listing[paramName] = fileListing;

            updateParamFileTable(paramName);
        }
    }
}

function handleFiles(files, paramName)
{
    var fileUploadsList = files_to_upload[paramName];
    if(fileUploadsList == null || fileUploadsList == undefined)
    {
        fileUploadsList = [];
    }

    var fileListing = param_file_listing[paramName];
    if(fileListing == null || fileListing == undefined)
    {
        fileListing = [];
    }

    for(var i=0;i<files.length;i++)
    {
        fileUploadsList.push(files[i]);
        fileListing.push(files[i].name);
    }

    files_to_upload[paramName] = fileUploadsList;
    param_file_listing[paramName] = fileListing;

    updateParamFileTable(paramName);
}

function updateParamFileTable(paramName)
{
    var files= param_file_listing[paramName];

    var idPName = paramName.replace(/\./g,'_');

    //remove previous file info data
    $("#" + idPName).prev("tr.fileInfo").remove();
    $("#" + idPName).children().remove();
    if(files != null && files != undefined && files.length > 0)
    {      
        var fileInfoRow = $("<tr class='fileInfo'></tr>");
        var tData = $("<td/>");

        if("#" + idPName)
        var editLink = $("<a href='#'>Hide Details...</a>");
        editLink.click(function()
        {
            event.preventDefault();
            
            var editLinkMode = $(this).text();
            if(editLinkMode == "Show Details...")
            {
                $(this).text("Hide Details...");
                $("#" + idPName).removeClass("hidden");
            }
            else
            {
                $(this).text("Show Details...");
                $("#" + idPName).addClass("hidden");
            }
        });

        tData.append(editLink);

        var selectedFiles = "Selected ";
        selectedFiles += files.length + " files";

        tData.append("(" + selectedFiles + ")");
        fileInfoRow.append("<td/>");
        fileInfoRow.append(tData);
        $("#" + idPName).before(fileInfoRow);

        if(files.length > 0)
        var table = $("<table class='paramFilesTable'/>");
        table.append("<tr><th colspan='2'>File Name</th></tr>");
        for(var i=0;i<files.length;i++)
        {
            var fileRow = $("<tr/>");
            var fileTData = $("<td/>");

            //determine if this is a  url
            if(files[i].indexOf("://") != -1)
            {
                fileRow.append("<td><a href='" + files[i] + "'> "+ files[i] + "</a></td>");                    
            }
            else
            {
                fileRow.append("<td>" + files[i] + "</td>");
            }
            var delButton = $("<button>Delete</button>");
            delButton.data("pfile", files[i]);
            delButton.button().click(function()
            {
                var file = $(this).data("pfile");
                //remove from file listing for specified parameter
                var index = param_file_listing[paramName].indexOf(file);
                if(index != -1)
                {
                    param_file_listing[paramName].splice(index,1);
                }

                //check if this file was in the list of files to upload
                // and if so remove the file
                for(var t=0;t<files_to_upload[paramName].length;t++)
                {
                    if(files_to_upload[paramName][t].name == file)
                    {
                        files_to_upload[paramName].splice(t, 1);
                        break;
                    }
                }
                updateParamFileTable(paramName);
            });

            fileTData.append(delButton);
            fileRow.append(fileTData);
            table.append(fileRow);
        }
        var tableData = $("<td/>");
        tableData.append(table);
        $("#"+idPName).append("<td/>");
        $("#"+idPName).append(tableData);
    }
}


function uploadAllFiles()
{
    //disable the parameter listing table so that no changes can be made
    //$("#paramsTable").find("input,button,textarea,select").attr("disabled", "disabled");

    if (Object.keys(files_to_upload).length > 0)
    {
        $('#runTaskSettingsDiv').block({
            message: '<h1> Please wait... </h1>',
            overlayCSS: { backgroundColor: '#F8F8F8' }
        });

        $("#cancelUpload").show();
        numFilesUploaded = 0;
        var count =0;
        for(var paramName in files_to_upload)
        {
            for(var f=0; f < files_to_upload[paramName].length; f++)
            {
                count++;
                var nextFile = files_to_upload[paramName][f];
                uploadFile(paramName, nextFile, count);
            }
        }
    }
}

// upload file
function uploadFile(paramName, file, fileId)
{
    $("#fileUploadDiv").append("<div id='" + fileId + "'/>");
    $("#"+fileId).before("<p>" + file.name + "</p>");
    $("#"+fileId).after("<p id='" + fileId + "Percentage'/>");
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
        var valueListing = parameter_and_val_obj[paramName];
        if(valueListing == undefined || valueListing == null)
        {
            valueListing = [];
        }
        valueListing.push(response.location);
        parameter_and_val_obj[paramName] = valueListing;

        numFilesUploaded++;

        var totalFiles = 0;

        for (var p in files_to_upload)
        {
            totalFiles += files_to_upload[p].length;
        }

        if(totalFiles == 0)
        {
            //something must be wrong
            alert("An error occurred while transferring files!");
        }

        console.log("numFilesUploaded: " + numFilesUploaded);
        console.log("totalFiles: " + totalFiles);

        if(numFilesUploaded == totalFiles)
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
    formData.append('index', fileId);
    xhr.send(formData);

    //keep track of all teh upload request so that they can be canceled
    //using the cancel button
    fileUploadRequests.push(xhr);
}



