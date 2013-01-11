var files_to_upload = {};
var run_task = {};

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
            type: "POST",
            url: "/gp/RunTask/load",
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
            dataType: "json"
        });
}

function loadModuleInfo(module)
{
    run_task.lsid = module["LSID"];
    run_task.name = module["name"];

    if(run_task.lsid == undefined)
    {
        throw("Unknown task LSID");
        return;
    }

    if(run_task.name == undefined)
    {
        throw("Unknown task name");
        return;
    }

    var span = $("<span class='.header'/>");
    span.append(run_task.name);

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
        var rowCount = q + 1;
        var paramRow = $("<tr/>");
        var parameterName = parameters[q].name;
        
        if(parameters[q].optional.length == 0)
        {
           parameterName += "*";
        }

        //replace . with spaces in parameter name
        parameterName = parameterName.replace(/\./g,' ');
        paramRow.append("<td>" + parameterName + "</td>");
        paramRow.data("pname", parameters[q].name);
        //paramRow.data("pname", pNameId);

        var rowId = "pRow" + (q+1);
        var valueTd = $("<td id='"+ rowId +"'/>");

        var choicelist = parameters[q].value;
        if(choicelist !== undefined && choicelist !== null && choicelist.length > 0)
        {
            var choiceResult = choicelist.split(';');
            var select = $("<select id='"+ rowCount +"'/>");
            for(var p=0;p<choiceResult.length;p++)
            {

                var rowdata = choiceResult[p].split("=");
                if(rowdata != undefined && rowdata != null && rowdata.length > 1)
                {
                    select.append("<option value='"+rowdata[0] +"'>" + rowdata[1]+ "</option>");
                }
            }

            valueTd.append(select);
            paramRow.append(valueTd);
            paramsTable.append(paramRow);
        }
        else if(parameters[q].type == "java.io.File")
        {
            inputFileRowIds.push(rowId);
            valueTd.addClass("dNd");
            valueTd.append("<span class='btn btn-success fileinput-button'>"
                    + "<span><i class='icon-plus'></i>"
                    + "<img src='../css/images/file_add.gif' width='16' height='16'"
                    + "alt='help' class='helpbutton'/>Upload Files...</span>"
                    + "<input class='uploadedinputfile' name='uploadedinputfile' type='file'/></span>");
            valueTd.append("<button class='urlButton'>"
                                + "<img src='../css/images/file_add.gif' width='16' height='16'"
                                + "alt='help' class='helpbutton'/>Specify URL...</button>");

            valueTd.append("<span>or drag and drop files here...</span>");
            paramRow.append(valueTd);
            paramsTable.append(paramRow);

            //switch . with _ since the selector does not work with .
            var idPName = parameters[q].name.replace(/\./g,'_');
            paramsTable.append("<tr id='" + idPName + "'></tr>");
        }
        else
        {
            valueTd.append("<input type='text' id='" + rowCount + "'/>");
            paramRow.append(valueTd);
            paramsTable.append(paramRow);
        }

        //append parameter description table
        paramsTable.append("<tr class='paramDescription'><td></td><td colspan='3'>" + parameters[q].description +"</td></tr>");
    }
    $("#paramsListingDiv").append(paramsTable);

    //alert("params table: " + paramsTable.html());
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
        var urlDiv = $("<div/>");

        urlDiv.append("Enter url:");
        var urlInput = $("<input type='text' class='urlInput'/>");
        urlDiv.append(urlInput);

        var enterButton = $("<button>Enter</button>");
        enterButton.button().click(function()
        {
            var url = $(this).prev().val();
            alert('url is: ' + url);
            $(this).parents("td:first").children().show();

            $(this).parents("div:first").remove();
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
    $("#toggleDesc").click(function()
    {
        //show descriptions
        $("#paramsTable tr.paramDescription").toggle();
    });

    $("button").button();

    //$(".submitControlsDiv").clone().appendTo("body");
    //var lsid = Request.parameter('lsid');
    var lsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9";

    loadModule(lsid);

    $("input[type='file']").live("change", function()
    {
        var paramName = $(this).parents("tr:first").data("pname");
        var uploadFileList = files_to_upload[paramName];
        if(uploadFileList == null || uploadFileList == undefined)
        {
            uploadFileList = [];
        }

        //add newly selected files to table of file listing
        for(var f=0; f < this.files.length; f++)
        {
            uploadFileList.push(this.files[f]);
        }

        files_to_upload[paramName] = uploadFileList;
        updateParamFileTable(paramName);
    });

    $("button.Reset").click(function()
    {
        alert("Reset fields");
        loadModule(run_task.lsid);
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
});

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

    var tData = $(event.target);
    //alert(tData.parent().data("pname") + " pname");
    // Only call the handler if 1 or more files was dropped.
    if (count > 0)
        handleFiles(files, tData.parent().data("pname"), tData);
}

function handleFiles(files, paramName)
{
    var fileList = files_to_upload[paramName];
    if(fileList == null || fileList == undefined)
    {
        fileList = [];
    }
    for(var i=0;i<files.length;i++)
    {
        fileList.push(files[i]);
    }

    files_to_upload[paramName] = fileList;
    updateParamFileTable(paramName);
}

function updateParamFileTable(paramName)
{
    var files= files_to_upload[paramName];

    var idPName = paramName.replace(/\./g,'_');

    alert("files length: " + files.length);
    //remove previous file info data
    $("#" + idPName).prev("tr.fileInfo").remove();
    $("#" + idPName).children().remove();
    if(files != null && files != undefined)
    {      
        var fileInfoRow = $("<tr class='fileInfo'></tr>");
        var tData = $("<td/>");

        if("#" + idPName)
        var editLink = $("<a href='#'>Hide Details...</a>");
        editLink.click(function()
        {
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
            fileRow.append("<td>" + files[i].name + "</td>");
            var delButton = $("<button>Delete</button>");
            delButton.data("pfile", files[i].name);
            delButton.click(function()
            {
                var file = $(this).data("pfile");
                //remove from file listing for specified parameter
                var index = files_to_upload[paramName].indexOf(file);
                if(index != -1)
                {
                    files_to_upload[paramName].splice(index,1);
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
    if (module_editor.filestoupload.length)
    {
        var nextFile = module_editor.filestoupload.shift();

        uploadFile(nextFile);
    }
    else
    {
        //TODO: add call to run job
    }
}

// upload file
function uploadFile(file)
{
    var destinationUrl = "/gp/RunTask/upload";
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

