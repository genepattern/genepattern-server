var field_types = {
    FILE: 1,
    CHOICE: 2,
    TEXT: 4,
    PASSWORD: 5,
    INTEGER: 6,
    FLOAT: 7
};

//convenient lookup for finding display value for an actual value of a choice parameter
var choiceMapping = {};

//contains info about the current selected task
var run_task_info = {
    lsid: null, //lsid of the module
    name: null, //name of the module
    params: {}, //contains parameter info necessary to build the job submit form, see the initParam() function for details
    sendTo: {},
    param_group_ids: {}, //contains map of parameter group name to id
    is_js_viewer: false
};

//contains json object with parameter to value pairing
var parameter_and_val_groups = {};

//contains all the file upload requests
var fileUploadRequests = [];

//keep track of when parameter sections where modified and should be expanded
var expandParameterSection = false;

//the field is used to assign unique ids to each file provided for a file input parameter
//in order to make it easier to delete files
var fileId = 0;
//contains the json of parameters received when loading a module
//saved so it can reused when for the reset operation

var Request = {
    parameter: function (name) {
        var result = this.parameters()[name];
        if (result !== undefined && result !== null) {
            return  decodeURIComponent(result);
        }

        return result;
    },

    parameters: function () {
        var result = {};
        var url = window.location.href;
        var parameters = url.slice(url.indexOf('?') + 1).split('&');

        for (var i = 0; i < parameters.length; i++) {
            var parameter = parameters[i].split('=');
            result[parameter[0]] = parameter[1];
        }
        return result;
    },

    cleanJobSubmit: null
};

function htmlEncode(value) {
    if (value === undefined || value === null || value === "") {
        return value;
    }

    return $('<div/>').text(value).html();
}

//For those browsers that dont have it so at least they won't crash.
if (!window.console) {
    window.console = { time: function () {
        }, timeEnd: function () {
        }, group: function () {
        }, groupEnd: function () {
        }, log: function () {
        }
    };
}

function isAboveQuota(diskInfo, diskUsageAddon)
{
    var isAboveQuota = false;
    if(diskInfo !== null && diskInfo.diskUsageFilesTab !== null && diskInfo.diskQuota)
    {
        var diskUsage = diskInfo.diskUsageFilesTab.numBytes;
        var diskQuota = diskInfo.diskQuota.numBytes;
        isAboveQuota = diskUsage > diskQuota;
    }

    //this is to check if adding the specified amount of bytes to
    //the disk usage will cause the disk usage to be exceed
    if(diskUsageAddon !== undefined && diskUsageAddon !== null)
    {
        var diskUsagePlus = diskUsage + diskUsageAddon;
        isAboveQuota = diskUsagePlus > diskQuota;
    }

    return isAboveQuota;
}

function handleDiskQuotaMsg(diskInfo)
{
    //remove any previous disk quota messages
    $("#diskQuotaMessage").remove();

    if(diskInfo !== null && diskInfo.aboveQuota === true)
    {
        //display a message and keep the job submit button disabled
        //disable the job submit button - do not allow the user to submit any jobs
        $("button.Run").attr("disabled", "disabled");
        $("button.Run").removeClass("ui-state-default").addClass("whiteBg");

        var quotaExceededMsg = $("<div id='diskQuotaMessage' class='errorMessageBig'>Disk usage quota exceeded. </div>");
        quotaExceededMsg.prepend("<img class='elemSpacing' src='/gp/images/exclamation.png' width='20' height='17' />");
        quotaExceededMsg.append("Disk Usage: " +  diskInfo.diskUsageFilesTab.displayValue + ". Quota: " + diskInfo.diskQuota.displayValue + ".");
        quotaExceededMsg.append("<p>Job submission has been disabled. Please delete some files from the Files tab.</p>");
        $("#paramsListingDiv").before(quotaExceededMsg);
    }
    else
    {
        //enable the job submit button
        $("button.Run").removeClass("whiteBg").addClass("ui-state-default");
        $("button.Run").removeAttr("disabled");
    }
}

function checkDiskQuota(successFunction)
{
    $.ajax({
        type: "GET",
        url: "/gp/rest/v1/disk",
        cache: false,
        success: function (response) {

            console.log(response);

            if(response !== null)
            {
                handleDiskQuotaMsg(response);

                //update the disk usage status box
                updateDiskUsageBox(response);
            }

            if(successFunction !== undefined && successFunction !== null)
            {
                successFunction(response);
            }
        },
        error: function (xhr, ajaxOptions, thrownError) {
            console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            console.log(thrownError);
        },
        dataType: "json"
    });
}

function loadModule(taskId, reloadId, sendFromKind, sendFromUrl) {
    // Fade in a progress indicator
    $("#loadingContent").fadeIn(800);

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
        data: { "lsid": taskId, "reloadJob": reloadId},
        success: function (response) {
            // Hide the loading indicator
            $("#loadingContent").hide();

            var message = response["MESSAGE"];
            var error = response["ERROR"];

            if (error !== undefined && error !== null) {
                alert(error);
            }
            if (message !== undefined && message !== null) {
                alert(message);
            }

            if (response["module"] !== undefined &&
                response["module"] !== null
                && response["parameters"] !== undefined
                && response["parameters"] !== undefined) {

                run_task_info.reloadJobId = reloadId;
                var module = response["module"];
                loadModuleInfo(module);

                //check if there are missing tasks (only applies to pipelines)
                if (module["missing_tasks"] != undefined && module["missing_tasks"].length > 0) {

                    $("#missingTasksDiv").append("<p class='errorMessage'>The following module versions do not exist on " +
                        "this server. Before running this pipeline you will need to either install the required modules " +
                        "or edit the pipeline to use the available module version.</p>");

                    var missingTaskTable = $("<table class='missingTaskTable' border='1'/>");
                    $("<tr class='missingTaskHeader'/>").append("<th align='left'> Name</th>")
                        .append("<th align='left'> Required Version </th>")
                        .append("<th align='left'> Installed Version </th>")
                        .append("<th align='left'> LSID </th>").appendTo(missingTaskTable);

                    //Output the list of missing tasks
                    var form = $('<form method="post" action="/gp/pages/taskCatalog.jsf"/>');

                    for(var m=0;m<module["missing_tasks"].length;m++)
                    {
                        var missingTaskObj = module["missing_tasks"][m];

                        $("<tr/>").append('<td>'+ missingTaskObj.name +'</td>')
                            .append("<td>" + missingTaskObj.version + "</td>")
                            .append("<td>" + missingTaskObj.installedVersions.join(", ")+ "</td>")
                            .append("<td>" + missingTaskObj.lsid + "</td>").appendTo(missingTaskTable);
                        var missingTaskLSID = missingTaskObj.lsid + ":" + missingTaskObj.version;
                        form.append("<input type='hidden' name='lsid' value='"+ missingTaskLSID+"'></input>");
                    }

                    $("#missingTasksDiv").append(missingTaskTable);

                    var actionDiv = $("<div/>");
                    actionDiv.append(form);

                    var installFromZipBtn = $("<button>Install from Zip</button>")
                        .button().click(function()
                    {
                        window.location.replace("/gp/pages/importTask.jsf");
                    });
                    actionDiv.append(installFromZipBtn);

                    var installFromRepoBtn = $("<button>Install from Repository</button>")
                        .button().click(function()
                    {
                        form.submit();
                    });

                    if(adminServerAllowed)
                    {
                        actionDiv.append(installFromRepoBtn);
                    }

                    $("#missingTasksDiv").append(actionDiv);

                    $(".submitControlsDiv").hide();
                    $("#runTaskMiscDiv").hide();
                    $("#javaCode").parents("tr:first").hide();
                    $("#matlabCode").parents("tr:first").hide();
                    $("#rCode").parents("tr:first").hide();
                    $("#pythonCode").parents("tr:first").hide();

                }
                else if (module["private_tasks"]) {
                    $("#missingTasksDiv").append("<p class='errorMessage'>WARNING: This pipeline includes tasks " +
                        "which you do not have permission to run on this server.</p>");
                    $(".submitControlsDiv").hide();

                    $("#javaCode").parents("tr:first").hide();
                    $("#matlabCode").parents("tr:first").hide();
                    $("#rCode").parents("tr:first").hide();
                    $("#pythonCode").parents("tr:first").hide();
                }
                else if (module["eula"]) {
                    clearEulas();
                    generateEulas(module["eula"], reloadId, sendFromKind, sendFromUrl);
                    $("#eula-block").show();
                }
                else {
                    loadParametersByGroup(module["parameter_groups"], response["parameters"], response["initialValues"], response["batchParams"]);
                }

                //add checkbox to specify whether to open in a new window
                //if this is a Javascription visualizer
                if(run_task_info.is_js_viewer)
                {
                    var launchDiv = $("<div id='launchJSNewWinDiv'/>");
                    launchDiv.append("<label><input type='checkbox' id='launchJSNewWin'/>Launch in a new window</label>");
                    $("#paramsListingDiv").prepend(launchDiv);
                }
                //the parameter form elements have been created now make the form visible
                $("#protocols").hide();
                $("#submitJob").show();

                //add the tags
                $('#jobTags').tagsInput(
                {
                    'defaultText':'Add tag and press enter...',
                    width: '97%',
                    height: '40px',
                    interactive: true,
                    placeholderColor: '#CCC',
                    autocomplete_url: '/gp/rest/v1/tags/',
                    autocomplete:{
                        minLength: 0,
                        response: tagResponse
                    }
                });


                $(".tagsContent").find("input").last().keyup(function()
                {
                    var value = $(this).val();

                    $(this).val(value.toLowerCase());
                });

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

               checkDiskQuota();
            }
        },
        error: function (xhr, ajaxOptions, thrownError) {
            console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            console.log(thrownError);

            // Hide the loading indicator
            $("#loadingContent").hide();

            $("#submitJob").show();
            $("#submitJob").empty();
            $("#submitJob").append("An error occurred while loading the task " + taskId + ": <br/>" + xhr.responseText);
        },
        dataType: "json"
    });
}

function generateEulas(eula, reloadId, sendFromKind, sendFromUrl) {
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

    $(eula.pendingEulas).each(function (index, item) {
        var thisEula = $("<div></div>")
            .addClass("eula")
            .appendTo(block);

        if (eula.pendingEulas.length > 1) {
            $("<div></div>")
                .addClass("barhead-license-task")
                .append("<span>" + item.moduleName + "</span>")
                .append($("<span>version " + item.moduleLsidVersion + "</span>")
                    .addClass("license-version"))
                .appendTo(thisEula);
        }

        $("<textarea></textarea>")
            .addClass("license-content")
            .attr("rows", 20)
            .attr("readonly", "readonly")
            .text(item.content)
            .appendTo(thisEula);
    });

    var initialQueryString = "lsid=" + encodeURIComponent(eula.currentLsid);
    if (reloadId) {
        initialQueryString = initialQueryString + "&reloadJob=" + encodeURIComponent(reloadId);
    }
    if (sendFromUrl) {
        initialQueryString = initialQueryString + "&_file=" + encodeURIComponent(sendFromUrl);
    }
    if (sendFromKind) {
        initialQueryString = initialQueryString + "&_format=" + encodeURIComponent(sendFromKind);
    }

    $("<div></div>")
        .addClass("license-center")
        .append($("<div></div>")
            .attr("name", "eula")
            .append(eula.pendingEulas.length > 1 ?
                $("<p>Do you accept all the license agreements?</p>").addClass("license-agree-text") :
                $("<p>Do you accept the license agreement?</p>").addClass("license-agree-text")
            )
            .append($("<input/>")
                .attr("type", "submit")
                .attr("value", "OK")
                .click(function() {
                    $.ajax({
                        type: eula.acceptType,
                        url: eula.acceptUrl + "?taskNameOrLsid=" + encodeURIComponent(eula.currentLsid),
                        success: function() {
                            document.location = '/gp/pages/index.jsf?lsid=' + encodeURIComponent(eula.currentLsid);
                        }
                    });
                })
        )
            .append($("<input/>")
                .attr("type", "button")
                .attr("onclick", "document.location='/gp/pages/index.jsf'")
                .attr("value", "Cancel")
        )
    )
        .appendTo(block);

    setTimeout(function () {
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
    // Insert into the old menus
    var starts = $(".send-to-param-start");
    starts.each(function (index, start) {
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

    // Insert into the new menus
    var paramLists = $(".send-to-param-list");
    paramLists.each(function (index, paramList) {
        sendToParamForMenu(paramList);
    });
}

function sendToParamForMenu(paramList) {
    $(paramList).each(function (index, element) {
        var kind = $(element).attr("data-kind");
        var url = $(element).attr("data-url");
        var params = run_task_info.sendTo[kind];
        if (params) {
            for (var i = 0; i < params.length; i++) {
                var param = params[i];

                $("<div></div>")
                    .attr("class", "send-to-param")
                    .attr("name", param)
                    .module({
                        data: {
                            "lsid": "",
                            "name": "Send to " + run_task_info.params[param].displayname,
                            "description": run_task_info.params[param].description,
                            "version": "",
                            "documentation": "http://genepattern.org",
                            "categories": [],
                            "suites": [],
                            "tags": []
                        },
                        click: function (event) {
                            var paramName = $(event.target).closest(".module-listing").attr("name");
                            setInputField(paramName, url);
                            $(element).closest(".search-widget").searchslider("hide");
                        },
                        draggable: false
                    }).appendTo($(element));
            }
        }

        if ($(element).find(".module-listing").length < 1) {
            $(element).hide();
        }
        else {
            $(element).show();
        }
    });
}

/*
 * Intended to be used to update the view of a parameter that has batching enabled
 */
function updateNonFileView(inputElement, parameterName, groupId, isBatch)
{
    if (isBatch)
    {
        //highlight the div to indicate batch mode
        $(inputElement).closest(".pRow").css("background-color", "#F5F5F5");
        $(inputElement).closest(".pRow").next().css("background-color", "#F5F5F5");

        run_task_info.params[parameterName].isBatch = true;

        run_task_info.params[parameterName].allowMultiple = true;
    }
    else
    {
        //remove row highlight indicating batch mode
        $(inputElement).closest(".pRow").css("background-color", "#FFFFFF");
        $(inputElement).closest(".pRow").next().css("background-color", "#FFFFFF");

        run_task_info.params[parameterName].isBatch = false;
        run_task_info.params[parameterName].allowMultiple = false;
    }

    if ($.inArray(field_types.TEXT, run_task_info.params[parameterName].type) !== -1)
    {
        //this must be a text entry
        $(inputElement).replaceWith(createTextDiv(parameterName, groupId, true));
    }

    if ($.inArray(field_types.CHOICE, run_task_info.params[parameterName].type) !== -1) {
        $(inputElement).replaceWith(initChoiceDiv(parameterName, groupId, true));

    }

    if ($.inArray(field_types.INTEGER, run_task_info.params[parameterName].type) !== -1
        || $.inArray(field_types.FLOAT, run_task_info.params[parameterName].type) !== -1) {
        $(inputElement).replaceWith(createNumericDiv(parameterName, groupId, true));
    }

    updateBatchInfo();

    return inputElement;
}

function loadModuleInfo(module) {
    run_task_info.lsid = module["LSID"];
    run_task_info.name = module["name"];

    if (run_task_info.lsid === undefined) {
        throw("Unknown task LSID");
        return;
    }

    if (run_task_info.name === undefined) {
        throw("Unknown task name");
        return;
    }

    $("#task_name").prepend(run_task_info.name);

    //add version drop down
    if (module["lsidVersions"] !== undefined) {
        for (var v = 0; v < module["lsidVersions"].length; v++) {
            var versionnum = module["lsidVersions"][v];
            var index = versionnum.lastIndexOf(":");
            if (index === -1) {
                alert("An error occurred while loading module versions.\nInvalid lsid: " + module["lsidVersions"][v]);
            }

            var version = versionnum.substring(index + 1, versionnum.length);
            var modversion = "<option value='" + versionnum + "'>" + version + "</option>";
            $('#task_versions').append(modversion);

            if (module["lsidVersions"][v] === run_task_info.lsid) {
                $('#task_versions').val(versionnum).attr('selected', true);
            }
        }

        //if there is only one version then replace the drop down with text
        if (module["lsidVersions"].length === 1) {
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

        $('#task_versions').change(function () {
            var changeTaskVersion = "index.jsf?lsid=" + $(this).val();
            window.open(changeTaskVersion, '_self');
        });
    }

    var isPipeline = module["taskType"] === "pipeline";
    var exportLink = "/gp/makeZip.jsp?name=" + run_task_info.lsid;
    $("#export").attr("href", exportLink);
    $("#export").data("pipeline", isPipeline);
    $("#export").click(function (e) {
        var isPipeline = $(this).data("pipeline");

        if (isPipeline) {
            e.preventDefault();
            //prompt the user if they want to include modules
            var dialog = $("<div><p>Press 'Include modules' to include all modules used by " + run_task_info.name +
                " in the exported zip file. </p> <p>Press 'Pipeline only' to include only the " + run_task_info.name + " pipeline definition itself. </p></div>");
            dialog.dialog({
                title: "Export Pipeline",
                resizable: true,
                height: 210,
                width: 500,
                modal: true,
                buttons: {
                    "Include modules": function () {
                        window.open("/gp/makeZip.jsp?name=" + run_task_info.lsid + "&includeDependents=1");
                        $(this).dialog("close");
                    },
                    "Pipeline only": function () {
                        window.open("/gp/makeZip.jsp?name=" + run_task_info.lsid);
                        $(this).dialog("close");
                    },
                    "Cancel Export": function () {
                        $(this).dialog("close");
                    }
                }
            });
        }


    });

    var hasDescription = false;
    if (module["description"] !== undefined
        && module["description"] !== "") {
        $("#mod_description").append(module["description"]);
        hasDescription = true;
    }

    //if module has doc specified or if for some reason
    // the hasDoc field was not set then show the doc link
    if (module["hasDoc"] === undefined || module["hasDoc"]) {
        var docLink = "/gp/getTaskDoc.jsp?name=" + run_task_info.lsid;
        $("#documentation").attr("href", docLink);
    }
    else {
        //otherwise hide the documentation link
        $("#documentation").hide();
    }


    if (module["editable"] !== undefined && module["editable"]) {
        var editLink = "/gp/modules/creator.jsf?lsid=" + run_task_info.lsid;

        if (module["taskType"] === "pipeline") {
            editLink = "/gp/pipeline/index.jsf?lsid=" + run_task_info.lsid;
        }

        $("#otherOptionsMenu").prepend("<li><a href='JavaScript:Menu.denyIE(\"" + editLink + "\");'>Edit</li>");
    }

    //add source info
    $("#source_info_tooltip").hide();

    if (module["source_info"] !== undefined && module["source_info"] !== null) {
        var label = module["source_info"].label;
        var iconUrl = module["source_info"].iconUrl;
        var briefDescription = module["source_info"].briefDesc;
        var fullDescription = module["source_info"].fullDesc;

        var empty = true;
        if (label !== undefined && label !== '' && label !== null) {
            $("#source_info").append(label);
        }

        if (iconUrl !== undefined && iconUrl !== '' && iconUrl !== null) {
            $("#source_info").prepend("<img src='" + iconUrl + "' width='18' height='16' />");
            empty = false;
        }

        if (briefDescription !== undefined && briefDescription !== '' && briefDescription !== null) {
            $("#source_info_tooltip").append(briefDescription);
            $("#source_info").data("hasBriefDescription", true);
        }
        else {
            $("#source_info").data("hasBriefDescription", false);
        }

        if (fullDescription !== undefined && fullDescription !== '' && fullDescription !== null) {
            var readMoreLink = $("<a href='#'> Read More</a>");
            readMoreLink.click(function (event) {
                event.preventDefault();
                $("#source_info_tooltip").hide();

                showDialog(label + " Repository Details", fullDescription, "OK");

            });
            $("#source_info_tooltip").append(readMoreLink);
        }

        $("#source_info").hover(function (e) {
            $("#source_info_tooltip").css("position", "absolute");
            $("#source_info_tooltip").css("top", $("#source_info").position().top + 15);
            $("#source_info_tooltip").css("right", 20);

            setTimeout(function () {
                if ($("#source_info").is(":hover")) {
                    if ($("#source_info").data("hasBriefDescription")) {
                        $("#source_info_tooltip").show();
                    }
                }
            }, 1150);
        });

        $("body").mousemove(function () {
            if (!$("#source_info_tooltip").is(":hover")) {
                $("#source_info_tooltip").hide();
            }
        });

        $("#source_info_tooltip").mouseleave(function () {
            $("#source_info_tooltip").hide();
        });

        $("#source_info_tooltip").focusout(function () {
            $("#source_info_tooltip").hide();
        });

        $("#source_info").prepend("Source: ");
    }

    if(module["taskType"] && module["taskType"] === "javascript")
    {
        run_task_info.is_js_viewer = true;
    }

    //Check if there is a newer Beta version of the module available
    //module["betaVersion"] = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:999999999";
    //check if this module has development i.e BETA quality level set
    $("#betaInfoDiv").hide();

    if(module["quality"] && (module["quality"] === "development" || module["quality"] === "preproduction"))
    {
        $("#betaInfoDiv").append("<div>This is a beta version of the module.</div>");
        $("#betaInfoDiv").show();
    }

    if(module["betaVersion"] && module["betaVersion"].length > 0)
    {
        var betaUrl = "/gp/pages/index.jsf?lsid=" + module["betaVersion"];
        $("#betaInfoDiv").append(
                "<div><a href='"+ betaUrl + "'>A newer beta version of this module is available. Click here to try it out. </a><div/>");
        $("#betaInfoDiv").show();
    }

    // Display properties
    $(".properties-name").text(module["name"]);
    $(".properties-lsid").text(module["LSID"]);
    $(".properties-description").text(module["description"]);
    $(".properties-author").text(module["author"]);
    $(".properties-privacy").text(module["privacy"]);
    $(".properties-license").text(module["eula"] ? "License Acceptance Required" : "None");
    $(".properties-quality").text(module["quality"]);
    $(".properties-documentation").html(module["hasDoc"] ? ("<a href='/gp/getTaskDoc.jsp?name=" + module["LSID"] + "'>Click Here</a>") : "None");
    $(".properties-commandline").text(module["commandLine"]);
    $(".properties-tasktype").text(module["taskType"]);
    $(".properties-categories").text(module["categories"]);
    $(".properties-cpu").text(module["cpuType"]);
    $(".properties-os").text(module["os"]);
    $(".properties-language").text(module["language"]);
    $(".properties-versioncomment").text(module["version"]);
    $(".properties-formats").text(module["fileFormat"]);

    if (module["allFiles"]) {
        for (var i = 0; i < module["allFiles"].length; i++) {
            var file = module["allFiles"][i];
            $(".properties-files").append(
                $("<a></a>")
                    .attr("href", "/gp/getFile.jsp?task=" + encodeURIComponent(module["LSID"]) + "&file=" + encodeURIComponent(file))
                    .append(file)
            )
            .append(", ")
        }
    }

    // Display pipeline properties
    if (module["children"]) {
        for (var i = 0; i < module["children"].length; i++) {
            var child = module["children"][i];
            var childBlock = $("<div></div>")
                .append(
                    $("<div></div>")
                        .addClass("pHeaderTitleDiv background")
                        .css("margin-top", "10px")
                        .append(
                            $("<img/>")
                                .addClass("paramSectionToggle")
                                .attr("height", 19)
                                .attr("width", 19)
                                .attr("src", "/gp/images/toggle_expand.png")
                        )
                        .append((i+1) + ". ")
                        .append(
                            $("<a></a>")
                                .attr("href", "/gp/pages/index.jsf?lsid=" + child["lsid"])
                                .attr("onclick", "event.stopPropagation();")
                                .append(child["name"])
                        )
                        .append(
                        $("<div></div>")
                            .css("float", "right")
                            .css("font-size", "0.75em")
                            .css("padding-right", "5px")
                            .text("Version " + child["version"])
                    )
                );

            if (child["NOT_FOUND"]) {
                var underBlock = $("<div></div>")
                    .addClass("paramGroupSection dotted-border")
                    .css("display", "block")
                    .css("color", "red")
                    .text("This module version is not present on this server!");
                childBlock.append(underBlock);
                childBlock.find("img").attr("src", "/gp/images/toggle_collapse.png");
            }
            else {
                var underBlock = $("<div></div>")
                    .addClass("paramGroupSection dotted-border")
                    .css("display", "none")
                    .text(child["description"])
                childBlock.append(underBlock);
                var inputTable = $("<table></table>")
                    .addClass("paramGroupSection dotted-border");
                for (var j = 0; j < child["params"].length; j++) {
                    var cParam = child["params"][j];
                    inputTable.append(
                        $("<tr></tr>")
                            .append(
                                $("<td></td>")
                                    .css("padding-right", "30px")
                                    .css("font-weight", "bold")
                                    .text(Object.keys(cParam)[0])
                            )
                            .append(
                                $("<td></td>").text(cParam[Object.keys(cParam)[0]]["value"])
                            )
                    )
                }
                underBlock.append(inputTable);
            }

            $(".properties-children").append(childBlock);
        }
    }
}

function setParamFieldType(parameterInfo) {
    //set the field types of this parameter
    run_task_info.params[parameterInfo.name].type = [];
    var isFile = false;
    var isChoice = false;
    var isPassword = false;
    var isText = false;
    var isNumeric = false;

    var allowCustomChoice = true;
    if (parameterInfo.choiceInfo !== undefined && parameterInfo.choiceInfo !== null && parameterInfo.choiceInfo !== '') {
        isChoice = true;

        //check if a custom choice is allowed
        if (!parameterInfo.choiceInfo.choiceAllowCustom) {
            allowCustomChoice = false;
        }

        run_task_info.params[parameterInfo.name].type.push(field_types.CHOICE);
        run_task_info.params[parameterInfo.name].choiceInfo = parameterInfo.choiceInfo;
    }

    //other types will be set if custom choice is allowed
    if (allowCustomChoice) {
        if (parameterInfo.TYPE === "FILE" && parameterInfo.MODE === "IN") {
            isFile = true;
            run_task_info.params[parameterInfo.name].type.push(field_types.FILE);
        }

        if (!isFile && !isChoice) {
            if (parameterInfo.type === "PASSWORD") {
                run_task_info.params[parameterInfo.name].type.push(field_types.PASSWORD);
            }
            else if(parameterInfo.minRange !== undefined || parameterInfo.maxRange !== undefined)
            {
                if(parameterInfo.type === "java.lang.Integer")
                {
                    run_task_info.params[parameterInfo.name].type.push(field_types.INTEGER);
                }

                if(parameterInfo.type === "java.lang.Float")
                {
                    run_task_info.params[parameterInfo.name].type.push(field_types.FLOAT);
                }

                if(parameterInfo.minRange != undefined)
                {
                    run_task_info.params[parameterInfo.name].minRange = parseFloat(parameterInfo.minRange);
                }

                if(parameterInfo.maxRange != undefined)
                {
                    run_task_info.params[parameterInfo.name].maxRange = parseFloat(parameterInfo.maxRange);
                }
            }
            else
            {
                run_task_info.params[parameterInfo.name].type.push(field_types.TEXT);
            }
        }
    }
}

function setAllowMultipleValuesForParam(parameterInfo) {
    run_task_info.params[parameterInfo.name]["allowMultiple"] = false;
    if (parameterInfo.maxValue === undefined
        || parameterInfo.maxValue === null
        || parseInt(parameterInfo.maxValue) !== 1) {
        run_task_info.params[parameterInfo.name]["allowMultiple"] = true;
    }
}

function setParamOptionalOrRequired(parameterInfo) {
    //check if this is a required parameter
    run_task_info.params[parameterInfo.name]["required"] = false;

    if (parameterInfo.optional.length === 0 || parameterInfo.minValue !== 0) {
        run_task_info.params[parameterInfo.name]["required"] = true;
    }
}

function setParamDisplayName(parameterInfo) {
    //set the display name
    run_task_info.params[parameterInfo.name]["displayname"] = parameterInfo.name;
    //use the alternate name if there is one (this is usually set for pipelines)
    if (parameterInfo.altName !== undefined
        && parameterInfo.altName !== null
        && parameterInfo.altName.replace(/ /g, '') !== "") ////trims spaces to check for empty string
    {
        run_task_info.params[parameterInfo.name]["displayname"] = parameterInfo.altName;
    }

    //replace . with spaces for parameter display name
    run_task_info.params[parameterInfo.name]["displayname"] = run_task_info.params[parameterInfo.name]["displayname"].replace(/\./g, ' ');
}

function initParam(parameterInfo, index, batchParams) {
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

    if (batchParams !== undefined && batchParams !== null && batchParams.indexOf(parameterInfo.name) !== -1) {
        run_task_info.params[parameterInfo.name].isBatch = true;
    }
    else {
        run_task_info.params[parameterInfo.name].isBatch = false;
    }

    // Add parameter to send-to map
    addSendToParam(parameterInfo);
}

function addSendToParam(parameterInfo) {
    //add special case for legacy modules which used type=DIRECTORY to
    // indicate parameters that accept directories
    var type = parameterInfo.type;
    var directoryType = "directory";
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
    else if(type !== undefined && type !== null && type.toLowerCase() === directoryType)
    {
        if(run_task_info.sendTo[directoryType] === undefined)
        {
            run_task_info.sendTo["directory"] = [];
        }

        run_task_info.sendTo["directory"].push(parameterInfo.name);
    }
}

function getBatchInfo()
{
    var containsNonFileParam = false;
    var batchParams = [];
    var numBatchJobs = 0;

    var parameterNames = Object.keys(run_task_info.params);
    for (var p = 0; p < parameterNames.length; p++) {
        var paramName = parameterNames[p];
        if (isBatch(paramName))
        {
            batchParams.push(paramName);
            if($.inArray(field_types.FILE, run_task_info.params[paramName].type) == -1)
            {
                containsNonFileParam = true;
            }

            var groups = parameter_and_val_groups[paramName] == undefined ? null :  parameter_and_val_groups[paramName].groups;
            if(groups != undefined && groups != null && Object.keys(groups).length == 1 && groups[1].values != undefined
                && groups[1].values != null)
            {
                //there should be only one group in a batch parameter
                //since batching of group params is not currently allowed
                var numValues = groups[1].values.length;

                if(numBatchJobs < numValues)
                {
                    numBatchJobs = numValues;
                }
            }
        }
    }

    var batchInfoObject = {};
    batchInfoObject["numBatchJobs"] = numBatchJobs;
    batchInfoObject["batchParams"] = batchParams;
    batchInfoObject["containsNonFile"] = containsNonFileParam;

    return batchInfoObject;
}

function updateBatchInfo()
{
    var batchInfoObject = getBatchInfo();

    //display a message if there are multiple batch params and some of them are
    // non-file params
    $(".previewBatch").remove();
    if(batchInfoObject.containsNonFile && batchInfoObject.batchParams.length > 1)
    {
        /*var batchInfoDiv = $("<div/>");
        var link = $("<a>NOTE: Important message when batching non-file parameters.</a>");
        batchInfoDiv.append(link);
        $("#paramsInfoDiv").append(batchInfoDiv);*/

        var previewBatchBtn = $("<button class='previewBatch'><img src='/gp/images/Information_magnifier_icon.png' class='buttonIcon' height='16' width='16'/>Preview Batch</button>");
        previewBatchBtn.button().click(function()
        {
            $( "<div id='batchInfoDialog'/>" ).dialog({
                minWidth: 600,
                minHeight: 500,
                create: function()
                {
                    var batchInfoObj = getBatchInfo();

                    var div = $("<div id='batchInfoDialogHeader'/>");
                    $(div).append("<h4> Total Batch Jobs: " + batchInfoObj.numBatchJobs + "</h4>");
                    //div.append("<div style='font-size: 10px;'>NOTE: Number of batch jobs and pairing of parameter values will vary if directories are specified.</div>");

                    $(this).append(div);

                    for(var b=0;b<batchInfoObj.numBatchJobs;b++)
                    {
                        var table = $("<table/>");
                        table.append("<tr><td colspan='2'>Batch #" + (b+1) + "</td></tr>");

                        for(var p=0;p<batchInfoObj.batchParams.length;p++)
                        {
                            var pName = batchInfoObj.batchParams[p];
                            var tr = $("<tr/>");
                            table.append(tr);
                            var td = $("<td width='40%'/>");
                            td.append(run_task_info.params[pName].displayname);
                            $(tr).append(td).appendTo(table);
                            var groups = parameter_and_val_groups[pName] == undefined ? null : parameter_and_val_groups[pName].groups;
                            if (groups != undefined && groups != null && Object.keys(groups) == 1) {
                                //there should be only one group in a batch parameter
                                //since batching of group params is not currently allowed

                                var values = groups[1].values;
                                var files = groups[1].files;

                                var value = "";
                                var choiceMapping = run_task_info.params[pName].choiceMapping;

                                if(choiceMapping != undefined && choiceMapping != null)
                                {
                                    value = choiceMapping[values[b]];
                                }
                                else if(files != undefined && files != null && b < files.length)
                                {
                                    value = decodeURIComponent(files[b].name);
                                }
                                else if(values != undefined && values != null && b < values.length)
                                {
                                   value = values[b];
                                }
                                else
                                {
                                    value = $("<span style='font-style:italic;color:red;'>No Value Specified...</span>");
                                }

                                td = $("<td width='60%'/>");
                                td.append(value);
                                $(tr).append(td).appendTo(table);
                            }
                        }

                        $("<div/>").append(table).appendTo(this);
                    }
                },
                buttons: {
                    OK: function() {
                        $( this ).dialog( "close" );
                    }
                }
            });
        });

        $(".submitControlsDiv").each(function()
        {
            $(this).find("button").last().before(previewBatchBtn.clone(true));
        });
    }
}

function createNumericDiv(parameterName, groupId, enableBatch, initialValuesList)
{
    var numericDiv = $("<div class='numericDiv'/>");
    // Create the single/batch run mode toggle
    if (enableBatch) {
        var batchBox = $("<div class='batchBox' title='A job will be launched for every value specified.'></div>");
        // Add the checkbox
        var batchCheck = $("<input type='checkbox' id='batchCheck" + parameterName + "' />");
        batchCheck.change(function ()
        {
            var paramName = $(this).parents("tr").first().data("pname");

            var isBatch = $(this).is(":checked");

            var parent =  $(this).closest(".pRow");

            if (isBatch)
            {
                //highlight the div to indicate batch mode
                $(this).closest(".pRow").css("background-color", "#F5F5F5");
                $(this).closest(".pRow").next().css("background-color", "#F5F5F5");

                run_task_info.params[paramName].isBatch = true;

                run_task_info.params[paramName].allowMultiple = true;

                parent.find(".batchBox").append("<a class='batchHelp' href='http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5' target='_blank'><img src='/gp/images/help_small.gif' width='12' height='12'/></a>");
            }
            else
            {
                //remove row highlight indicating batch mode
                $(this).closest(".pRow").css("background-color", "#FFFFFF");
                $(this).closest(".pRow").next().css("background-color", "#FFFFFF");

                run_task_info.params[paramName].isBatch = false;
                run_task_info.params[paramName].allowMultiple = false;
            }

            var numericElement = $(this).closest(".pRow").find(".numericInput");
            var groupId = getGroupId(numericElement);
            $(this).closest(".pRow").find(".numericDiv").replaceWith(createNumericDiv(paramName, groupId, true));

            updateBatchInfo();
        });

        batchBox.append(batchCheck);
        batchBox.append("<label for='batchCheck" + parameterName + "'>Batch</label>");
        //batchCheck.button();
        batchBox.tooltip();
        batchBox.append("<a class='batchHelp' href='http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5' target='_blank'><img src='/gp/images/help_small.gif' width='12' height='12'/></a>");

        numericDiv.append(batchBox);

        //if this is a batch parameter then pre-select the batch checkbox
        if (run_task_info.params[parameterName].isBatch) {
            batchBox.find("input[type='checkbox']").prop('checked', true);
        }
    }

    $("#runTaskSettingsDiv").append(numericDiv);

    var paramDetails = run_task_info.params[parameterName];

    var isFloat = false;
    if($.inArray(field_types.FLOAT, run_task_info.params[parameterName].type) != -1)
    {
        isFloat = true;
    }

    //select initial values if there are any
    if (initialValuesList !== undefined && initialValuesList !== null) {
        for (var v = 0; v < initialValuesList.length; v++) {
            var inputFieldValue = initialValuesList[v];

            if(isFloat)
            {
                parseFloat(inputFieldValue);
            }
            else
            {
                parseInt(inputFieldValue);
            }

            var allowDelete = false;
            if(v > 0 )
            {
                allowDelete = true;
            }

            createNumericInput(parameterName, groupId, numericDiv, allowDelete, inputFieldValue);
        }
    }
    else
    {
        createNumericInput(parameterName, groupId, numericDiv, false);
    }

    if(paramDetails.allowMultiple)
    {
        //add a button to add additional spinners
        var addMoreBtn = $("<button>Add</button>");
        addMoreBtn.button().click(function()
        {
            var groupId = getGroupId($(this));
            var parent = $(this).parents("tr").first();
            var paramName = parent.data("pname");

            var div = $("<div/>");

            createNumericInput(paramName, groupId, div, true, "");

            $(this).before(div);
        });

        $("<div/>").append(addMoreBtn).appendTo(numericDiv);
    }
    numericDiv.detach();

    //add text indicating the range if there is one
    var rangeInfoText = "";

    if($.isNumeric(paramDetails.minRange))
    {
        rangeInfoText = "min = " + paramDetails.minRange + "    ";
    }

    if($.isNumeric(paramDetails.maxRange))
    {
        rangeInfoText += "max = " + paramDetails.maxRange;
    }

    if(rangeInfoText.length > 0)
    {
        numericDiv.prepend("<div style='font-style: italic'>" + rangeInfoText + "</div>");
    }

    return numericDiv;
}

function createNumericInput(parameterName, groupId, container, allowDelete, value)
{
    var isFloat = false;
    if($.inArray(field_types.FLOAT, run_task_info.params[parameterName].type) != -1)
    {
        isFloat = true;
    }

    var div = $("<div/>");
    container.append(div);

    var paramDetails = run_task_info.params[parameterName];

    var nField = $("<input name='numericInput' class='numericInput'/>");
    div.append(nField);

    if(allowDelete)
    {
        //add a delete button
        var deleteBtn = $("<button class='deleteBtn'>X</button>");
        deleteBtn.button().click(function()
        {
            var parent = $(this).parents("tr").first();
            var paramName = parent.data("pname");

            var groupId = getGroupId($(this));

            var index = $(this).parents(".numericDiv").first().find(".deleteBtn").index($(this));
            var valueList = getValuesForGroup(groupId, paramName);

            index = index + 1;
            valueList.splice(index, 1);

            updateValuesForGroup(groupId, paramName, valueList);

            $(this).parents("div").first().remove();
        });

        div.append(deleteBtn);
    }
    else
    {
        //if delete is allowed then this is not the first item so set its value to the default value
        nField.val(paramDetails.default_value);
    }

    nField = nField.spinner();

    if(isFloat)
    {
        nField.spinner( "option", "numberFormat", "n");
        nField.spinner( "option", "step", 0.01);
    }

    if($.isNumeric(paramDetails.minRange))
    {
        nField.spinner( "option","min", paramDetails.minRange);
    }

    if($.isNumeric(paramDetails.maxRange))
    {
        nField.spinner( "option","max", paramDetails.maxRange);
    }

    if(value != undefined && value != null)
    {
        var valueList = getValuesForGroup(groupId, parameterName);
        nField.spinner("value", value);

        valueList.push(value);
        updateValuesForGroup(groupId, parameterName, valueList);
    }

    nField.on("spinchange", function(event, ui)
    {
        var parent = $(this).parents("tr").first();

        var paramName = parent.data("pname");

        //check that the value > than minimum range and < than the maximum range
        var minRange = run_task_info.params[paramName].minRange;
        var maxRange = run_task_info.params[paramName].maxRange;

        if(!$.isNumeric($(this).val()))
        {
            alert("Invalid value: " + $(this).val() + ". Value must be numeric.");
            $(this).val("");
            return;
        }

        if(minRange != undefined && $(this).val() < minRange)
        {
            alert("Invalid value: " + $(this).val() + ". Value must be greater than or equal to " + minRange);
            $(this).val("");

            return;
        }

        if(maxRange != undefined && $(this).val() > maxRange)
        {
            alert("Invalid value: " + $(this).val() + ". Value must be less than or equal to " + maxRange);
            $(this).val("");

            return;
        }


        var valueList = [];

        //get all the numeric values set for this parameter
        parent.find("input[name='numericInput']").each(function()
        {
            valueList.push($(this).val());
        });

        var groupId = getGroupId($(this));
        updateValuesForGroup(groupId, paramName, valueList);
    });
}

function createTextDiv(parameterName, groupId, enableBatch, initialValuesList) {
    var textDiv = $("<div class='tagsContent textDiv '/>");
    $("#runTaskSettingsDiv").append(textDiv);

    // Create the single/batch run mode toggle
    if (enableBatch) {
        var batchBox = $("<div class='batchBox' title='A job will be launched for every value specified.'></div>");
        // Add the checkbox
        var batchCheck = $("<input type='checkbox' id='batchCheck" + parameterName + "' />");
        batchCheck.change(function ()
        {
            var paramName = $(this).parents("tr").first().data("pname");

            var isBatch = $(this).is(":checked");
            if (isBatch)
            {
                //highlight the div to indicate batch mode
                $(this).closest(".pRow").css("background-color", "#F5F5F5");
                $(this).closest(".pRow").next().css("background-color", "#F5F5F5");

                run_task_info.params[paramName].isBatch = true;

                run_task_info.params[paramName].allowMultiple = true;
            }
            else
            {
                //remove row highlight indicating batch mode
                $(this).closest(".pRow").css("background-color", "#FFFFFF");
                $(this).closest(".pRow").next().css("background-color", "#FFFFFF");

                run_task_info.params[paramName].isBatch = false;
                run_task_info.params[paramName].allowMultiple = false;
            }

            var textElement = $(this).closest(".pRow").find(".pValue");
            textElement.val("");
            var groupId = getGroupId(textElement);

            var parent =  $(this).closest(".pRow");
            parent.find(".textDiv").replaceWith(createTextDiv(paramName, groupId, true));

            updateBatchInfo();
        });

        batchBox.append(batchCheck);
        batchBox.append("<label for='batchCheck" + parameterName + "'>Batch</label>");
        //batchCheck.button();
        batchBox.tooltip();
        batchBox.append("<a class='batchHelp' href='http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5' target='_blank'><img src='/gp/images/help_small.gif' width='12' height='12'/></a>");

        textDiv.append(batchBox);

        //if this is a batch parameter then pre-select the batch checkbox
        if (run_task_info.params[parameterName].isBatch) {
            batchBox.find("input[type='checkbox']").prop('checked', true);
        }
    }

    var paramDetails = run_task_info.params[parameterName];

    var textField = null;
    var isPassword = $.inArray(field_types.PASSWORD, run_task_info.params[parameterName].type) !== -1;
    var tagFieldId = parameterName.replace(/\./g, "_") + "Text";

    if (isPassword) {
        textField = $("<input type='password' class='pValue' id='" + tagFieldId + "'/>");
    }
    else if(run_task_info.params[parameterName].allowMultiple)
    {
        textField = $("<input class='pValue' id='" + tagFieldId + "'/>");
    }
    else {
        textField = $("<input type='text' class='pValue' id='" + tagFieldId + "'/>");
    }

    textDiv.append(textField);

    textField.data("pname", parameterName);
    // Handle link drags
    textField.get(0).addEventListener("dragenter", dragEnter, true);
    textField.get(0).addEventListener("dragleave", dragLeave, true);
    textField.get(0).addEventListener("dragexit", dragExit, false);
    textField.get(0).addEventListener("dragover", dragOver, false);
    textField.get(0).addEventListener("drop", function (event) {
        $(this).removeClass('runtask-highlight');
        var link = event.dataTransfer.getData('Text');
        $(this).val(link);

        //now trigger a change so that this value is added to this parameter
        $(this).trigger("change");
    }, true);

    var textValChange= function(element)
    {
        var valueList = [];
        var paramName = $(element).data("pname");

        //split the values only if this is a multi value text field
        if(run_task_info.params[parameterName].allowMultiple)
        {
            valueList = $(element).val().split(",");
        }
        else
        {
            valueList.push($(element).val());
        }

        var groupId = $(element).data("groupId");
        updateValuesForGroup(groupId, paramName, valueList);
    };

    textField.change(function () {
        textValChange(this);
    });
    textField.val(paramDetails.default_value);
    textField.data("groupId", groupId);

    var textValueList = [];

    if (textField.val() !== "")
    {
        textValueList.push(textField.val());
    }

    updateValuesForGroup(groupId, parameterName, textValueList);

    if (paramDetails.required === 0 && paramDetails.minValue !== 0) {
        textField.addClass("requiredParam");
    }

    //select initial values if there are any
    if (initialValuesList !== undefined && initialValuesList !== null)
    {
        var inputFieldValue = "";
        for (var v = 0; v < initialValuesList.length; v++) {
            inputFieldValue += initialValuesList[v];

            // add a comma between items in this list
            if (v < ( initialValuesList.length - 1)) {
                inputFieldValue += ",";
            }
        }

        textField.val(inputFieldValue);

        //textField.val(initialValuesList);
        textField.trigger("change");
    }


    if(!isPassword && paramDetails.allowMultiple)
    {
        textField.tagsInput(
        {
            'defaultText': 'Add value and press enter...',
            width: '88%',
            height: '40px',
            interactive: true,
            placeholderColor: '#CCC',
            onChange: function()
            {
                textValChange(this);
            },
            onAddTag: function()
            {
                $(this).parent().find(".tag").last().find("a").attr("title", "").hide();

                var paramName = $(this).parents("tr").first().data("pname");
                var numTags = $(this).parent().find(".tag").length;
                //check that the max value has not been reached
                if(!validateMaxValues(paramName, numTags))
                {
                    //delete the added tag
                    $(this).parent().find(".tag").last().find("a").click();
                }
                else
                {
                    var text = $(this).parent().find(".tag").last().find("span").text();

                    text = $.trim(text);
                    $(this).parent().find(".tag").last().find("span").text(text);
                }
            },
            onRemoveTag: function()
            {
                $(this).parent().find(".tag").find("a").hide();
            }
        });

        textDiv.find(".tag").find("a").attr("title", "").hide();

        textDiv.find(".tag").each(function()
        {
            var text = $(this).find("span").text();
            text = $.trim(text);
            $(this).find("span").text(text);
        });


        textDiv.find(".tagsinput").on("hover", ".tag", function ()
        {
            if($(this).is(":hover"))
            {
                $(this).find("a").show();

                var textHover = $(this).find("span").text();
                textHover = textHover + " ";
                $(this).find("span").text(textHover);
            }
            else
            {
                $(this).find("a").hide();

                var text = $(this).find("span").text();
                text = $.trim(text);
                $(this).find("span").text(text);
            }
        });
    }

    //replace the textDiv with the new tags input div
    textDiv.detach();

    return textDiv;
}

/**
 * Get a unique id for a file div, based on parameterName and groupId.
 * @param parameterName
 * @param groupId
 */
function createFileDivId(parameterName, groupId) {
    var divId = "fileDiv-";
    if (parameterName) {
        divId += parameterName;
    }
    divId += "-";
    if (groupId) {
        divId += groupId;
    }
    return divId;
}

function createFileDiv(parameterName, groupId, enableBatch, initialValuesList) {
    var fileDivId = createFileDivId(parameterName, groupId);
    var fileUploadDiv = $("<div class='fileUploadDiv'/>");
    var fileDiv = $("<div class='fileDiv mainDivBorder' id='" + fileDivId + "' />");

    //enable dragging of file between groups
    fileDiv.droppable(
        {
            hoverClass: 'runtask-highlight',
            drop: function (event, ui) {
                try {
                    var target = $(event.target);

                    var draggable = ui.draggable;
                    var draggablePRow = draggable.find("td").parents(".pRow").first();
                    if (draggablePRow === undefined || draggablePRow === null || draggablePRow.size() === 0) {
                        //do nothing since this is not droppable
                        return;
                    }
                    var draggableParamName = draggablePRow.attr("id");
                    var draggableGroupId = draggable.parents(".valueEntryDiv").first().data("groupId");

                    var targetPRow = target.parents(".pRow").first();
                    if (targetPRow === undefined || targetPRow === null || targetPRow.size() === 0) {
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
                    updateFilesForGroup(targetGroupId, targetParamName, fileObjListings);
                    updateParamFileTable(targetParamName, null, groupId);


                    var rowIndex = $(ui.helper).find("input[name='rowindex']").val();
                    var dfileObjListings = getFilesForGroup(draggableGroupId, draggableParamName);

                    dfileObjListings.splice(rowIndex, 1);
                    updateFilesForGroup(draggableGroupId, draggableParamName, dfileObjListings);
                    updateParamFileTable(draggableParamName, null, draggableGroupId);
                }
                catch (err) {
                    //drop failed do nothing but log error
                    console.log("Error: " + err.message);
                }
            }
        });

    var paramDetails = run_task_info.params[parameterName];

    var uploadFileText = "Upload Files...";
    var addUrlText = "Add Paths or URLs...";
    if (!paramDetails.allowMultiple) {
        uploadFileText = "Upload File...";
        addUrlText = "Add Path or URL...";
    }

    var fileInput = $("<input class='uploadedinputfile' type='file'/>");
    fileInput.data("pname", parameterName);


    if (paramDetails.allowMultiple) {
        //make the file input field multiselect, so you can select more than one file
        fileInput.attr("multiple", "multiple");
    }

    var uploadFileBtn = $("<button class='uploadBtn' type='button'>" + uploadFileText + "</button>");
    uploadFileBtn.button().click(function () {
        console.log("uploadedfile: " + $(this).siblings(".uploadedinputfile").first());
        $(this).parents(".fileDiv").first().find(".uploadedinputfile:first").click();
    });

    fileUploadDiv.append(uploadFileBtn);
    if (paramDetails.required) {
        fileInput.addClass("requiredParam");
    }

    var fileInputDiv = $("<div class='inputFileBtn'/>");
    fileInputDiv.append(fileInput);
    fileUploadDiv.append(fileInputDiv);

    var urlButton = $("<button type='button' class='urlButton'>" + addUrlText + "</button>");
    fileUploadDiv.append(urlButton);
    urlButton.data("groupId", groupId);
    urlButton.button().click(function () {
        var urlDiv = $("<div class='urlDiv'/>");

        var urlActionDiv = $("<div class='center'/>");
        var addURLBtn = $("<button id='addFileUrl'>Add URL</button>");
        addURLBtn.button().click(function () {
            var urlEntryDiv = $("<div class='urlEntry'> Enter URL:<input type='text' class='urlInput'/> </div>");
            var delBtn = $("<img class='images delBtn' src='/gp/images/delete-blue.png'/>");

            delBtn.button().click(function()
            {
                //$(this).parent(".urlEntry").find(".urlInput").val("");
                $(this).parent(".urlEntry").find(".urlInput").trigger("change");

                //just empty the value if this is the first in the list of url entries
                if(urlDiv.find(".urlEntry").length > 1)
                {
                    $(this).parent(".urlEntry").remove();
                }

                urlDiv.find(".urlEntry").first().find(".delBtn").hide();
            });
            urlEntryDiv.append(delBtn);

            $(this).parent().before(urlEntryDiv);

            urlDiv.find(".urlEntry").first().find(".delBtn").hide();
        });
        urlActionDiv.append(addURLBtn);
        urlDiv.append(urlActionDiv);
        addURLBtn.click();

        //this is the first url entry field so hide it the delete button
        //urlDiv.find(".urlEntry").first().find(".delBtn").hide();

        if(!(paramDetails.allowMultiple || paramDetails.isBatch))
        {
            addURLBtn.hide();
        }

        $("#dialogUrlDiv").data("groupId", groupId);
        $("#dialogUrlDiv").append(urlDiv);
        openServerFileDialog(this);
    });

    fileUploadDiv.append("<span class='drop-box'>Drag Files Here</span>");
    fileUploadDiv.append("<div class='fileSizeCaption'> 2GB file upload limit using the " + uploadFileText + " button. For files > 2GB upload from the Files tab. </div>");

    fileDiv.append(fileUploadDiv);

    fileDiv.append("<div class='fileListingDiv'/>");

    //check if there are predefined file values
    var fileObjListings = getFilesForGroup(groupId, parameterName);

    //also check if this parameter is also a choice parameter
    if (initialValuesList !== undefined && initialValuesList !== null
        && initialValuesList.length > 0) {
        if (!run_task_info.params[parameterName].initialChoiceValues) {
            var totalFileLength = fileObjListings.length + initialValuesList.length;

            //check if max file length will be violated
            //check if we should automatically enable batch in the case when the number of
            //initial files is greater than the maximum allowed
            var maxFiles = getMaxFiles(parameterName);
            if (maxFiles !== null && totalFileLength > maxFiles) {
                paramDetails.isBatch = true;
            }

            validateMaxFiles(parameterName, totalFileLength);

            for (var v = 0; v < initialValuesList.length; v++) {
                //check if the file name is not empty
                if (initialValuesList[v] !== null && initialValuesList[v] !== "") {
                    var fileObj =
                    {
                        name: initialValuesList[v],
                        id: fileId++
                    };

                    fileObjListings.push(fileObj);
                }
            }

            updateFilesForGroup(groupId, parameterName, fileObjListings);
            updateParamFileTable(parameterName, fileDiv, groupId);
        }
    }
    else {
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

    if (run_task_info.params[parameterName].initialChoiceValues) {
        fileDiv.hide();
    }

    return fileDiv;
}

//add toggle to switch between field types for a parameter that has multiple
//i.e. file drop-down parameters (CHOICE and FILE field types)
function createModeToggle(parameterName) {
    var paramDetails = run_task_info.params[parameterName];

    var toggleChoiceFileDiv = $("<div class='fieldTypeToggle'/>");
    toggleChoiceFileDiv.data("pname", parameterName);

    //this count is used create elements with unique ids for elements associated with the parameter
    var nextCount = run_task_info.params[parameterName].count;
    if (nextCount === undefined || nextCount === null) {
        nextCount = 0;
        run_task_info.params[parameterName].count = nextCount;
    }

    nextCount++;
    var idPName = nextCount + "_" + parameterName;
    var fileChoiceOptions = $("<div class='fileChoiceOptions'>");

    if ($.inArray(field_types.CHOICE, run_task_info.params[parameterName].type) !== -1) {
        fileChoiceOptions.append('<input id="selectFile_' + idPName + '" name="field_toggle_' + idPName + '" type="radio" /><label for="selectFile_' + idPName + '">Select a file</label>');
    }

    if ($.inArray(field_types.FILE, run_task_info.params[parameterName].type) !== -1) {
        fileChoiceOptions.append('<input id="customFile_' + idPName + '" name="field_toggle_' + idPName + '" type="radio" /><label for="customFile_' + idPName + '">Upload your own file</label> ');
    }

    fileChoiceOptions.find(":radio:checked").removeAttr("checked");
    if (paramDetails.initialChoiceValues) {
        fileChoiceOptions.find('input[id="selectFile_' + idPName + '"]').attr("checked", "checked");
    }
    else {
        fileChoiceOptions.find('input[id="customFile_' + idPName + '"]').attr("checked", "checked");
    }

    toggleChoiceFileDiv.append(fileChoiceOptions);

    fileChoiceOptions.buttonset();

    fileChoiceOptions.find("label.ui-button:not(:last)").each(function () {
        $(this).after('<span class="elemSpacing">  or  </span>');
    });

    fileChoiceOptions.data("pname", parameterName);

    fileChoiceOptions.change(function () {
        var selectedOption = ($(this).find(":radio:checked + label").text());
        var pname = $(this).data("pname");

        //clear any values that were set
        var groupId = getGroupId(fileChoiceOptions);
        updateValuesForGroup(groupId, pname, []);
        updateFilesForGroup(groupId, pname, []);
        updateParamFileTable(pname, $(this).parents("td:first").find(".fileDiv"));

        $(this).parents("td:first").find(".fileDiv").toggle();
        $(this).parents("td:first").find(".selectChoice").toggle();

        if (selectedOption === "Select a file") {
            var defaultValue = $(this).parents("td:first").find(".choice").data("default_value");

            if (defaultValue === undefined || defaultValue === null) {
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
function initParams(parameterGroups, parameters, batchParams) {
    run_task_info.parameterGroups = parameterGroups;

    if (parameters === undefined || parameters === null) {
        return;
    }

    for (var q = 0; q < parameters.length; q++) {
        var parameterName = parameters[q].name;
        initParam(parameters[q], q, batchParams);
    }
}

function getNextGroupId(parameterName) {
    var paramGroupInfo = parameter_and_val_groups[parameterName];
    if (paramGroupInfo === null || paramGroupInfo === undefined) {
        paramGroupInfo = {};
        paramGroupInfo.groupCountIncrementer = 0;
        parameter_and_val_groups[parameterName] = paramGroupInfo;
    }

    var nextGroupId = paramGroupInfo.groupCountIncrementer;
    nextGroupId++;
    parameter_and_val_groups[parameterName].groupCountIncrementer = nextGroupId;

    if (parameter_and_val_groups[parameterName].groups === undefined ||
        parameter_and_val_groups[parameterName].groups === null) {
        parameter_and_val_groups[parameterName].groups = {};
    }

    parameter_and_val_groups[parameterName].groups[nextGroupId] = {};

    return nextGroupId;
}

function getGroupId(element) {
    var valueEntryDiv = element.parents(".valueEntryDiv");
    if (valueEntryDiv === undefined || valueEntryDiv === null
        || valueEntryDiv.data("groupId") === undefined
        || valueEntryDiv.data("groupId") === null) {
        javascript_abort("Error retrieving group id");
    }

    return valueEntryDiv.data("groupId");
}

function updateValuesForGroup(groupId, paramName, valueList) {
    if (parameter_and_val_groups[paramName].groups[groupId] === undefined
        || parameter_and_val_groups[paramName].groups[groupId] === null) {
        javascript_abort("Error retrieving group Id " + groupId + " for parameter " + paramName);
    }

    parameter_and_val_groups[paramName].groups[groupId].values = valueList;
}

function getFileGroupIdByIndex(paramName, index) {
    if (parameter_and_val_groups[paramName] === undefined || parameter_and_val_groups[paramName] === null
        || parameter_and_val_groups[paramName].groups === undefined
        || parameter_and_val_groups[paramName].groups === null) {
        javascript_abort("Error retrieving first group for parameter " + paramName);
    }

    var groupIds = Object.keys(parameter_and_val_groups[paramName].groups);
    //check if index is out of range
    if (index < groupIds.length) {
        var groupId = parseInt(groupIds[index]);

        if(isNaN(groupId))
        {
            javascript_abort("Error retrieving group: invalid group id " + groupIds[index]);
        }

        return groupId;
    }

    javascript_abort("Error retrieving group: index out of range " + index);
}

function getFilesForGroup(groupId, paramName) {
    if (parameter_and_val_groups[paramName].groups[groupId] === undefined
        || parameter_and_val_groups[paramName].groups[groupId] === null) {
        javascript_abort("Error retrieving group Id " + groupId + " for parameter " + paramName);
    }

    if (parameter_and_val_groups[paramName].groups[groupId].files === undefined ||
        parameter_and_val_groups[paramName].groups[groupId].files === null) {
        parameter_and_val_groups[paramName].groups[groupId].files = [];
    }

    return parameter_and_val_groups[paramName].groups[groupId].files;
}

function getValuesForGroup(groupId, paramName) {
    if (parameter_and_val_groups[paramName].groups[groupId] === undefined
        || parameter_and_val_groups[paramName].groups[groupId] === null) {
        javascript_abort("Error retrieving group Id " + groupId + " for parameter " + paramName);
    }

    if (parameter_and_val_groups[paramName].groups[groupId].values === undefined ||
        parameter_and_val_groups[paramName].groups[groupId].values === null) {
        parameter_and_val_groups[paramName].groups[groupId].values = [];
    }

    return parameter_and_val_groups[paramName].groups[groupId].values;
}

function updateFilesForGroup(groupId, paramName, filesList) {
    if (parameter_and_val_groups[paramName].groups[groupId] === undefined
        || parameter_and_val_groups[paramName].groups[groupId] === null) {
        javascript_abort("Error retrieving group Id " + groupId + " for parameter " + paramName);
    }
    parameter_and_val_groups[paramName].groups[groupId].files = filesList;
}

function createParamValueEntryDiv(parameterName, initialValuesObj) {
    var contentDiv = $("<div class='valueEntryDiv'/>");
    var groupId = getNextGroupId(parameterName);
    contentDiv.data("groupId", groupId);

    var enableBatch = true;
    var groupingEnabled = false;

    var groupInfo = run_task_info.params[parameterName].groupInfo;
    if (groupInfo !== null && groupInfo !== undefined && (groupInfo.maxValue === null || groupInfo.maxValue === undefined || groupInfo.maxValue > 1)) {
        groupingEnabled = true;

        //do not allow batch if grouping is enabled
        enableBatch = false;
    }

    if(run_task_info.is_js_viewer)
    {
        enableBatch = false;
    }

    var initialValues = null;
    var groupid = null;
    if (initialValuesObj !== undefined && initialValuesObj !== null) {
        initialValues = initialValuesObj.values;
        groupid = initialValuesObj.groupid;
    }

    populateContentDiv(parameterName, contentDiv, groupId, initialValues, enableBatch);

    //check if grouping is enabled
    if (groupingEnabled) {
        var groupColumnLabel = groupInfo.groupColumnLabel;
        if (groupColumnLabel === undefined && groupColumnLabel === null) {
            groupColumnLabel = "file group";
        }

        var groupingDiv = $("<div class='groupingDiv'/>");
        var groupTextField = $("<input class='groupingTextField' type='text'/>");
        groupTextField.change(function () {
            var paramName = $(this).parents(".pRow").first().attr("id");
            var groupId = getGroupId($(this));

            var value = $.trim($(this).val());
            parameter_and_val_groups[paramName].groups[groupId].name = value;
        });

        if (groupid === undefined || groupid === null) {
            groupid = "";
        }
        else {
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
        delButton.button().click(function () {
            //TODO remove the grouping;
            var paramName = $(this).parents(".pRow").first().attr("id");
            var groupId = getGroupId($(this));

            //remove this group from the hash
            delete parameter_and_val_groups[paramName].groups[groupId];
            $(this).parents(".valueEntryDiv").remove();

            //enable the add group button if it was previously disabled
            $(this).parents(".paramValueTd").find('addGroupButton').prop("disabled", false);
        });

        //check that this is group 2 or greater before adding a delete button
        if(parameter_and_val_groups[parameterName].groupCountIncrementer > 1)
        {
            contentDiv.prepend(delButton);
        }
    }

    return contentDiv;
}

function populateContentDiv(parameterName, contentDiv, groupId, initialValues, enableBatch) {


    //create the necessary field types for this parameter
    if ($.inArray(field_types.CHOICE, run_task_info.params[parameterName].type) !== -1) {
        contentDiv.append(initChoiceDiv(parameterName, groupId, enableBatch, initialValues));
    }

    if (run_task_info.params[parameterName].type.length > 1) {
        //multiple field types specified so add a toggle buttons
        //right now this would only be for a file drop-down parameter
        contentDiv.prepend(createModeToggle(parameterName));
    }

    if ($.inArray(field_types.FILE, run_task_info.params[parameterName].type) !== -1) {
        contentDiv.append(createFileDiv(parameterName, groupId, enableBatch, initialValues));

        // Create the single/batch run mode toggle
        if (enableBatch) {
            var batchBox = $("<div class='batchBox' title='A job will be launched for every file with a matching type.'></div>");
            // Add the checkbox
            var batchCheck = $("<input type='checkbox' id='batchCheck" + parameterName + "' />");
            batchCheck.change(function () {
                var paramName = $(this).parents("tr").first().data("pname");

                var isBatch = $(this).is(":checked");
                if (isBatch) {
                    run_task_info.params[paramName].isBatch = true;

                    //highlight the div to indicate batch mode
                    $(this).closest(".pRow").css("background-color", "#F5F5F5");
                    $(this).closest(".pRow").next().css("background-color", "#F5F5F5");

                    //allow multi-select from file browser when in batch mode
                    $(this).parents(".valueEntryDiv").find(".uploadedinputfile").attr("multiple", "multiple");
                }
                else
                {
                    //remove row highlight indicating batch mode
                    $(this).closest(".pRow").css("background-color", "#FFFFFF");
                    $(this).closest(".pRow").next().css("background-color", "#FFFFFF");

                    // Clear the files from the parameter
                    var groupId = getGroupId($(this));
                    updateFilesForGroup(groupId, paramName, []);
                    updateParamFileTable(paramName, $(this).parents(".valueEntryDiv").find(".fileDiv"));

                    var maxValue = run_task_info.params[paramName].maxValue;

                    //disable multiselect for this param if it is not file list param
                    if (maxValue <= 1)
                    {
                        $(this).parents(".valueEntryDiv").find(".uploadedinputfile").removeAttr("multiple");
                    }

                    run_task_info.params[paramName].isBatch = false;
                }

                var isChoice = $.inArray(field_types.CHOICE, run_task_info.params[paramName].type);

                //update multiselect of file drop down also
                if(isChoice != -1)
                {
                    var choiceElement = $(this).closest(".pRow").find(".selectChoice");
                    var choiceElementGroupId = getGroupId(choiceElement);
                    updateNonFileView(choiceElement, paramName, choiceElementGroupId, isBatch);
                }
                else
                {
                    updateBatchInfo();
                }

            });
            batchBox.append(batchCheck);
            batchBox.append("<label for='batchCheck" + parameterName + "'>Batch</label>");
            //batchCheck.button();
            batchBox.tooltip();
            batchBox.append("<a class='batchHelp' href='http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5' target='_blank'><img src='/gp/images/help_small.gif' width='12' height='12'/></a>");


            //if this is a batch parameter then pre-select the batch checkbox
            if (run_task_info.params[parameterName].isBatch) {
                batchBox.find("input[type='checkbox']").prop('checked', true);
            }
            contentDiv.prepend(batchBox);
        }
    }

    if ($.inArray(field_types.TEXT, run_task_info.params[parameterName].type) !== -1
        || $.inArray(field_types.PASSWORD, run_task_info.params[parameterName].type) !== -1)
    {
        //this must be a text entry
        contentDiv.append(createTextDiv(parameterName, groupId, enableBatch, initialValues));
    }

    if ($.inArray(field_types.INTEGER, run_task_info.params[parameterName].type) !== -1
        || $.inArray(field_types.FLOAT, run_task_info.params[parameterName].type) !== -1) {
        //this must be a text entry
        contentDiv.append(createNumericDiv(parameterName, groupId, enableBatch, initialValues));
    }
}

function loadParametersByGroup(parameterGroups, parameters, initialValues, batchParams) {
    //check if the params object should be initialized
    if (parameters !== null) {
        initParams(parameterGroups, parameters, batchParams);
    }

    if (parameterGroups === null) {
        parameterGroups = run_task_info.parameterGroups
    }

    if (run_task_info.params === null) {
        throw new Error("Error initializing parameters");
    }

    if (run_task_info.params === null) {
        throw new Error("Error initializing parameter groups");
    }

    $("#runTaskSettingsDiv").off("click.headerTitle").on("click.headerTitle", ".pHeaderTitleDiv",  function () {
        $(this).next().toggle();

        var toggleImg = $(this).find(".paramSectionToggle");

        if (toggleImg === null) {
            //toggle image not found
            // just log error and return
            console.log("Could not find toggle image for hiding and showing parameter groups sections");

            return;
        }

        //change the toggle image to indicate hide or show
        var imageSrc = toggleImg.attr("src");
        if (imageSrc.indexOf('collapse') !== -1) {
            imageSrc = imageSrc.replace("collapse", "expand");
        }
        else {
            imageSrc = imageSrc.replace("expand", "collapse");
        }

        toggleImg.attr("src", imageSrc);
    });

    for (var i = 0; i < run_task_info.parameterGroups.length; i++) {
        //check if any parameters were found in the group
        //if so then do nothing and continue
        if (parameterGroups[i].parameters === undefined || parameterGroups[i].parameters === null
            || parameterGroups[i].parameters.length === 0) {
            continue;
        }

        var pGroupName = parameterGroups[i].name;

        if (pGroupName === undefined || pGroupName === null) {
            pGroupName = "  ";
        }

        //set up the parameter group section(s)
        var headings = pGroupName.split("/");
        var curHeaderDiv = $("#paramsListingDiv");
        for (var h = 0; h < headings.length; h++) {
            var pHeadingId = run_task_info.param_group_ids[headings[h]];
            if (pHeadingId === undefined || pHeadingId === null) {
                pHeadingId = "paramGroup_" + i + "_" + h;
                run_task_info.param_group_ids[headings[h]] = pHeadingId;

                var newHeaderDiv = $("<div id=" + pHeadingId + " class='paramGroupSection'/>");
                //append to the top level parameter listing div
                curHeaderDiv.append(newHeaderDiv);

                var headerTitleDiv = $("<div class='pHeaderTitleDiv'/>");
                var toggleImg = $("<img src ='/gp/images/toggle_collapse.png' width='19' height='19' class='paramSectionToggle'/>");

                if (parameterGroups.length > 1) {
                    //only provide hide/show toggle for a group with a name
                    if (pGroupName.length > 0) {
                        headerTitleDiv.append(toggleImg);
                    }

                    if (h === 0) {
                        newHeaderDiv.addClass("solid-border");
                        newHeaderDiv.addClass("paramgroup-spacing");
                        headerTitleDiv.addClass("top-level-background");
                    }
                    else {
                        newHeaderDiv.addClass("dotted-border");

                        headerTitleDiv.addClass("background");
                    }

                    newHeaderDiv.before(headerTitleDiv);
                }

                headerTitleDiv.append(headings[h]);

                //add a description if this is the last heading item
                if (h === headings.length - 1 && (parameterGroups[i].description !== undefined && parameterGroups[i].description !== null
                    && parameterGroups[i].description.length > 0)) {
                    newHeaderDiv.prepend("<div class='pHeaderDescription'>" + parameterGroups[i].description + "</div>");
                }
            }

            //keep track of top level parent div
            curHeaderDiv = $("#" + pHeadingId);
        }

        var paramTable = createParamTable(parameterGroups[i].parameters, initialValues);

        //check if the new section should be hidden
        if (parameterGroups[i].hidden !== undefined && parameterGroups[i].hidden !== null
            && parameterGroups[i].hidden && !isNonDefaultValues(parameterGroups[i].parameters)) {
            curHeaderDiv.prev().find(".paramSectionToggle").click();
        }

        curHeaderDiv.append(paramTable);
    }
}

/**
 *
 * @param parameterNames , list of parameters in a section
 * @returns {boolean}, returns true if any of the parameters
 *  has an initial value that is not the default
 */
function isNonDefaultValues(parameterNames)
{
    for(var i=0;i<parameterNames.length;i++)
    {
        var parameterName = parameterNames[i];

        var defaultValue = run_task_info.params[parameterName].default_value;
        var initialValues = run_task_info.params[parameterName].initialValues;

        if(initialValues !== undefined && initialValues !== null)
        {
            for(var n=0;n<initialValues.length;n++)
            {
                 var values = initialValues[n].values;

                 //it must non default value if the number of values > 1 since default value
                //is currently only one value
                 if(initialValues.length.length > 1 || values.length > 1
                     || (values.length==1 && values[0] !== defaultValue))
                 {
                     return true;
                 }
            }
        }
    }

    return false;
}

function createParamTable(parameterNames, initialValues) {
    var paramsTable = $("<table class='paramsTable'/>");

    //return empty paramsTable if no parameter names were specified
    if (parameterNames === undefined || parameterNames === null)
    {
        return paramsTable;
    }

    for (var q = 0; q < parameterNames.length; q++) {
        var parameterName = parameterNames[q];

        var paramRow = $("<tr id='" + parameterName + "' class='pRow'/>");
        paramRow.data("pname", parameterName);

        paramsTable.append(paramRow);

        //add the display name of the parameter to the first column of the table
        var nameDiv = $("<div class='pTitleDiv'>");
        $("<td class='pTitle'>").append(nameDiv).appendTo(paramRow);

        nameDiv.append(run_task_info.params[parameterName].displayname);
        if (run_task_info.params[parameterName].required) {
            nameDiv.append("*");
        }

        var valueTd = $("<td class='paramValueTd'/>");
        paramRow.append(valueTd);

        //set the initial values
        //can be null or undefined if this is not a job reload
        var initialValuesList = null;

        if (initialValues !== null && initialValues !== undefined) {
            initialValuesList = initialValues[parameterName];
        }
        run_task_info.params[parameterName].initialValues = initialValuesList;

        var initialValuesByGroup = run_task_info.params[parameterName].initialValues;
        if (initialValuesByGroup !== undefined && initialValuesByGroup !== null) {
            for (var g = 0; g < initialValuesByGroup.length; g++) {
                valueTd.append(createParamValueEntryDiv(parameterName, initialValuesByGroup[g]));
            }
        }
        else {
            valueTd.append(createParamValueEntryDiv(parameterName, null));
        }
        //check if grouping is enabled
        var groupInfo = run_task_info.params[parameterName].groupInfo;
        if (groupInfo !== null && groupInfo !== undefined && (groupInfo.maxValue === null || groupInfo.maxValue === undefined
            || groupInfo.maxValue > 1)) {
            var groupColumnLabel = groupInfo.groupColumnLabel;
            if (groupColumnLabel === undefined && groupColumnLabel === null) {
                groupColumnLabel = "file group";
            }

            var addGroupButton = $("<button class='addGroupButton'>Add Another " + groupColumnLabel + "</button>");
            addGroupButton.button().click(function (event) {
                event.preventDefault();

                var parameterName = $(this).parents(".pRow").attr("id");
                $(this).parents(".pRow").first().find(".paramValueTd").find(".valueEntryDiv").last().after(createParamValueEntryDiv(parameterName, null));
                //TODO: Add another of these input fields

                var numGroupsCreated = $(this).parents(".pRow").first().find(".paramValueTd").find(".valueEntryDiv").length;
                var maxGroupsAllowed = run_task_info.params[parameterName].groupInfo.maxNumGroups;
                if(maxGroupsAllowed !== undefined && maxGroupsAllowed !== null &&
                    maxGroupsAllowed !== -1 && maxGroupsAllowed === numGroupsCreated)
                {
                    $(this).prop("disabled", true);
                    $(this).parents(".pRow").first().find(".paramValueTd").find(".groupLimitText").text("(Group Limit Reached, Maximum Groups Allowed=" + run_task_info.params[parameterName].groupInfo.maxNumGroups + ")");
                }
            });

            var maxGroupsAllowed = groupInfo.maxNumGroups;
            if( maxGroupsAllowed === undefined || maxGroupsAllowed === null || maxGroupsAllowed === -1)
            {
                maxGroupsAllowed = "Unlimited";
            }
            $("<div class='fileGroup'/>").append(addGroupButton).append("<div class='groupLimitText'> (Maximum Groups Allowed="+ maxGroupsAllowed +") </div>").appendTo(valueTd);

            //auto create the minimum of groups specified for this parameter
            //if no initial values where specified
            if (initialValuesList === null) {
                var minGroupInfo = parseInt(groupInfo.minNumGroups);
                for (var i = 0; i < minGroupInfo - 1; i++) {
                    addGroupButton.click();
                }
            }
        }

        paramsTable.append(createParamDescriptionRow(parameterName));
    }

    return paramsTable;
}

function createParamDescriptionRow(parameterName) {
    var paramDetails = run_task_info.params[parameterName];

    //append parameter description table
    var pDescription = paramDetails.description;
    if (paramDetails.altDescription !== undefined
        && paramDetails.altDescription !== null
        && paramDetails.altDescription.replace(/ /g, '') !== "") //trims spaces to check for empty string
    {
        pDescription = paramDetails.altDescription;
    }

    return $("<tr class='paramDescription'><td></td><td colspan='3'>" + pDescription + "</td></tr>");
}

function loadRunTaskForm(lsid, reloadJob, sendFromKind, sendFromUrl)
{
    //remove any tabs created when running a pipeline that contains a Javascript visualizer
    if($("#main-pane").hasClass("ui-tabs"))
    {
        $("#main-pane").tabs("destroy");
        $("#jobResultsTab").remove();
    }

    // Hide the search slider if it is open
    $(".search-widget").searchslider("hide");

    // Hide the protocols if visible
    $("#protocols").hide();
    $("#jobResults").hide();
    $("#mainJsViewerPane").hide();

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
        sendTo: {},
        param_group_ids: {}
    };

    parameter_and_val_groups = {}; //contains params and their values only

    $("#toggleDesc").click(function () {
        //show descriptions
        $("#paramsListingDiv").find(".paramDescription").toggle();
    });

    $("button").button();
    if (reloadJob !== false) {
        reloadJob = Request.parameter('reloadJob');
    }
    else {
        reloadJob = "";
    }
    if (reloadJob === undefined || reloadJob === null) {
        reloadJob = "";
    }

    if (lsid === undefined || lsid === null) {
        lsid = Request.parameter('lsid');
    }

    if ((lsid === undefined || lsid === null || lsid === "")
        && (reloadJob === undefined || reloadJob === null || reloadJob === "")) {
        $("#protocols").show();
        return;
    }
    else
    {
        loadModule(lsid, reloadJob, sendFromKind, sendFromUrl);

        $("#submitJob input[type='file']").live("change", function () {
            var paramName = $(this).data("pname");

            var groupId = getGroupId($(this));
            var fileObjListings = getFilesForGroup(groupId, paramName);

            //create a copy of files so that the input file field
            //can be reset so that files with the same name can be reuploaded
            var uploadedFiles = [];
            for (var t = 0; t < this.files.length; t++) {
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
            for (var f = 0; f < uploadedFiles.length; f++) {
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
        var selected = function (event, ui) {
            $(this).popup("close");
        };

        $("button.Reset").click(function () {
            reset();
        });

        $("button.Run").click(function () {
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

        document.body.addEventListener('drop', function (e) {
            e.preventDefault();
        }, false);

        //add action for when cancel upload button is clicked
        $("#cancelUpload").hide();
        $("#cancelUpload").button().click(function () {
            for (var y = 0; y < fileUploadRequests.length; y++) {
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
        $("#pythonCode").data("language", "Python");

        $("#removeViewCode").button().click(function () {
            $("#viewCodeDiv").hide();
        });

        $("#removeViewProperties").button().click(function () {
            $("#viewProperties").hide();
        });

        /*add action for when one of the view code languages is selected */
        $("#viewCodeDiv").hide();
        $(".viewCode").click(function () {
            var language = $(this).data("language");
            $("#viewCodeDiv").children().each(function () {
                //if this is not the delete button then remove it
                if ($(this).attr("id") !== "removeViewCode") {
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
            for (var t = 0; t < paramNames.length; t++) {
                var groupNames = Object.keys(parameter_and_val_groups[paramNames[t]].groups);
                for (var g = 0; g < groupNames.length; g++) {
                    var valuesList = parameter_and_val_groups[paramNames[t]].groups[groupNames[g]].values;
                    if (valuesList !== undefined && valuesList !== null && valuesList.length > 0) {
                        queryString += "&" + paramNames[t] + "=" + valuesList[0];
                    }
                }
            }

            $.ajax({
                type: "GET",
                url: "/gp/rest/RunTask/viewCode" + queryString,
                cache: false,
                data: { "lsid": run_task_info.lsid,
                    "reloadJob": run_task_info.reloadJobId,
                    "language": language},
                success: function (response) {

                    if (response["code"] === undefined || response["code"] === null) {
                        $("#viewCodeDiv").append("<p>An error occurred while retrieving the code</p>")
                    }
                    else {
                        $("#viewCodeDiv").append("<pre style='overflow: auto;'>" + htmlEncode(response["code"]) + "</pre>");
                        //add a link to the appropriate programmers guide
                        $("#viewCodeDiv").append("<span><hr/>For more details go to the Programmer's Guide section: <a href='http://www.broadinstitute.org/cancer/software/genepattern/programmers-guide#_Using_GenePattern_from_" + language + "'> " +
                            "Using GenePattern from " + language + "</a></span>");
                    }
                },
                error: function (xhr, ajaxOptions, thrownError) {
                    console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
                    console.log(thrownError);

                    $("#viewCodeDiv").append("<p>An error occurred while retrieving the code.</p>");
                },
                dataType: "json"
            });
        });

        $("#otherOptions").click(function (event) {
            var menu = $("#otherOptionsMenu");
            menu.menu();

            var top = $(this).position().top + 22;
            var left = $(this).position().left - menu.width() + $(this).width() + 22;

            menu.css("position", "absolute");
            menu.css("top", top);
            menu.css("left", left);
            menu.show();

            event.stopPropagation();

            $(document).click(function () {
                $("#otherOptionsMenu").hide();
            });
        });
    }
}

$("#optionsMenu").blur(function () {
    menu.hide();
});

$("#optionsMenu").focusout(function () {
    menu.hide();
});

function reset() {
    $(".paramsTable").empty();

    //remove all input file parameter file listings
    param_file_listing = {};
    parameter_and_val_groups = {};

    loadParametersByGroup(null, null, null, null);
}

function isText(param) {
    var selector = "#" + jqEscape(param);
    var input = $(selector);
    if (input.length === 0) return;

    return input.attr("type") === "text";
}

function isDropDown(param) {
    var selector = "#" + jqEscape(param);
    var input = $(selector);
    if (input.length === 0) return;

    return input.get(0).tagName === "SELECT";
}

function isFile(param) {
    var selector = "#" + jqEscape(param);
    var input = $(selector);
    if (input.length === 0) return;

    // Determine the input type
    return input.attr("type") === "file";
}

function validate() {
    var missingReqParameters = [];
    var missingGroupNames = [];
    var paramNames = Object.keys(parameter_and_val_groups);
    for (var p = 0; p < paramNames.length; p++) {
        var groups = parameter_and_val_groups[paramNames[p]].groups;
        if (groups === null) {
            continue;
        }

        var groupIds = Object.keys(groups);
        for (var g = 0; g < groupIds.length; g++) {
            var values = parameter_and_val_groups[paramNames[p]].groups[groupIds[g]].values;
            var files = parameter_and_val_groups[paramNames[p]].groups[groupIds[g]].files;

            var required = run_task_info.params[paramNames[p]].required;
            //check if it is required and there is no value specified
            if (required && (values === undefined || values === null
                || values.length === 0 || (values.length === 1 && values[0] === ""))
                && (files === undefined || files === null || files.length === 0)) {
                missingReqParameters.push(paramNames[p]);
                break;
            }

            //check that group names were specified if there is more than one group
            var groupName = parameter_and_val_groups[paramNames[p]].groups[groupIds[g]].name;
            if ((groupName === undefined || groupName === null || groupName.length === 0) && groupIds.length > 1) {
                //if there is more than one group defined then they must be named
                missingGroupNames.push(paramNames[p]);
                break;
            }
        }
    }

    //remove any existing error messages
    $(".errorMessage").remove();
    $("#missingRequiredParams").remove();
    $(".errorHighlight").each(function () {
        $(this).removeClass("errorHighlight");
    });

    if (missingReqParameters.length > 0) {
        //create div to list of all parameters with missing values
        var missingReqParamsDiv = $("<div id='missingRequiredParams'/>");

        $("#submitErrors").append(missingReqParamsDiv);
        missingReqParamsDiv.append("<p class='errorMessage'>Please provide a value for the following parameter(s):</p>");

        var pListing = $("<ul class='errorMessage'/>");
        missingReqParamsDiv.append(pListing);

        var errorMessage = "<div class='errorMessage'>This field is required</div>";

        for (p = 0; p < missingReqParameters.length; p++) {
            var displayname = missingReqParameters[p];

            //check if the parameter has an alternate name
            if (run_task_info.params[missingReqParameters[p]].displayname !== undefined
                && run_task_info.params[missingReqParameters[p]].displayname !== "") {
                displayname = run_task_info.params[missingReqParameters[p]].displayname;
            }

            pListing.append("<li>" + displayname + "</li>");

            $("#" + jqEscape(missingReqParameters[p])).find(".paramValueTd").addClass("errorHighlight");
            $("#" + jqEscape(missingReqParameters[p])).find(".paramValueTd").append(errorMessage);
        }

        return false;
    }

    //do something similar if missing group names were found
    if (missingGroupNames.length > 0) {
        //create div to list of all parameters with missing values
        var missingGroupNamesDiv = $("<div id='missingGroupNamesParams'/>");
        $("#submitErrors").append(missingGroupNamesDiv);
        missingGroupNamesDiv.append("<p class='errorMessage'>Please provide labels for the following parameter(s):</p>");

        var pListing = $("<ul class='errorMessage'/>");
        missingGroupNamesDiv.append(pListing);

        var errorMessage = "<div class='errorMessage'>This value is required</div>";

        for (p = 0; p < missingGroupNames.length; p++) {
            var displayname = missingGroupNames[p];

            //check if the parameter has an alternate name
            if (run_task_info.params[missingGroupNames[p]].displayname !== undefined
                && run_task_info.params[missingGroupNames[p]].displayname !== "") {
                displayname = run_task_info.params[missingGroupNames[p]].displayname;
            }

            pListing.append("<li>" + displayname + "</li>");

            $("#" + jqEscape(missingGroupNames[p])).find(".paramValueTd").addClass("errorHighlight");
            $("#" + jqEscape(missingGroupNames[p])).find(".paramValueTd").find(".groupingTextField").each(function () {
                if ($(this).val() === undefined || $(this).val() === null || $(this).val().length === 0) {
                    $(this).after(errorMessage);
                }
            });
        }

        return false;
    }

    return true;
}

function runJob() {
    //validate that all required inputs have been specified
    if (!validate()) {
        return;
    }

    //upload all the input files if there are any
    if (!allFilesUploaded()) {
        uploadAllFiles();
    }
    else {
        //add the file locations to the file parameter object that will
        submitTask();
    }
}

function buildBatchList() {
    var batchParams = [];
    var parameterNames = Object.keys(parameter_and_val_groups);
    for (var p = 0; p < parameterNames.length; p++) {
        var paramName = parameterNames[p];
        if (isBatch(paramName)) {
            batchParams.push(paramName);
        }
    }

    return batchParams;
}

function submitTask() {
    setAllFileParamValues();

    //Change text of blocking div
    $('#runTaskSettingsDiv').block({
        message: '<h1> Submitting job...</h1>',
        overlayCSS: { backgroundColor: '#F8F8F8'}
    });

    console.log("submitting task");

    var param_values_by_group = {};
    var parameterNames = Object.keys(parameter_and_val_groups);
    for (var p = 0; p < parameterNames.length; p++) {
        var paramName = parameterNames[p];
        param_values_by_group[paramName] = [];
        var groupIds = Object.keys(parameter_and_val_groups[paramName].groups);

        //sort the groups by their numeric id to retain order of groups
        //since the ids are generated sequentially
        groupIds.sort(function (a, b) {
            a = parseInt(a);
            b = parseInt(b);

            return a < b ? -1 : (a > b ? 1 : 0);
        });

        for (var g = 0; g < groupIds.length; g++) {
            var groupInfo = parameter_and_val_groups[paramName].groups[groupIds[g]];
            var groupName = groupInfo.name;
            if (groupName === undefined || groupName === null) {
                //if there is more than one group defined then they must be named
                if (groupIds.length === 1) {
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
        "lsid": run_task_info.lsid,
        "params": JSON.stringify(param_values_by_group),
        "batchParams": buildBatchList()
    };

    if($("#jobComment").val() !== undefined && $("#jobComment").val() !== null
        && $("#jobComment").val().length > 0 && $("#jobComment").val() != $("#jobComment").attr('placeHolder'))
    {
        taskJsonObj["comment"] = $("#jobComment").val();
    }

    if($("#jobTags").val() !== undefined && $("#jobTags").val() !== null
        && $("#jobTags").val().length > 0)
    {
        taskJsonObj["tags"] = $("#jobTags").val().split(",");
    }

    var RUN_PATH = "/gp/rest/RunTask/addJob";
    if(run_task_info.is_js_viewer)
    {
        RUN_PATH = "/gp/rest/RunTask/launchJsViewer";
    }

    $.ajax({
        type: "POST",
        url: RUN_PATH,
        contentType: 'application/json',
        data: JSON.stringify(taskJsonObj),
        timeout: 60000,  //timeout added to specifically to handle cases of file choice ftp listing taking too long
        success: function (response) {

            $('#runTaskSettingsDiv').unblock();

            var message = response["MESSAGE"];

            if (message !== undefined && message !== null) {
                alert(message);
            }

            if (response.batchId !== undefined) {
                window.location.replace("/gp/pages/index.jsf?jobResults=batchId%3D" + response.batchId);
            }
            else
            {
                var openVisualizers = "&openVisualizers=true";

                if(run_task_info.is_js_viewer && $("#launchJSNewWin").is(":checked"))
                {
                    openVisualizers = "&openVisualizers=false";
                    window.open("/gp/pages/jsViewer.jsf?jobNumber=" + response.jobId);
                }

                window.location.replace("/gp/pages/index.jsf?jobid=" + response.jobId + openVisualizers);
            }
        },
        error: function (xhr, ajaxOptions, thrownError) {
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

function dragEnter(evt) {
    this.classList.add('runtask-highlight');
//    evt.stopPropagation();
//    evt.preventDefault();
}

function dragLeave(evt) {
    this.classList.remove('runtask-highlight');
//    evt.stopPropagation();
//    evt.preventDefault();
}

function dragExit(evt) {
    evt.stopPropagation();
    evt.preventDefault();
}

function dragOver(evt) {
    this.classList.add('runtask-highlight');
    evt.stopPropagation();
    evt.preventDefault();
}

function drop(evt) {
    this.classList.remove('runtask-highlight');
    evt.stopPropagation();
    evt.preventDefault();

    //Check and prevent upload of directories
    //Works only on browsers that support the HTML5 FileSystem API
    //right now this is Chrome v21 and up
    if (evt.dataTransfer && evt.dataTransfer.items) {
        var items = evt.dataTransfer.items;
        var length = items.length;

        for (var i = 0; i < length; i++) {
            var entry = items[i];
            if (entry.getAsEntry) {
                //Standard HTML5 API
                entry = entry.getAsEntry();
            }
            else if (entry.webkitGetAsEntry) {
                //WebKit implementation of HTML5 API.
                entry = entry.webkitGetAsEntry();
            }

            if (entry && entry.isDirectory) {
                //do to continue if any directories are found
                alert("Directory uploads are not allowed.");
                return;
            }
        }
    }

    var files = evt.dataTransfer.files;
    var count = files.length;

    var target = $(evt.target);
    var paramName = target.parents(".pRow").first().data("pname");
    if (paramName === undefined) {
        console.log("Error: Could not find the parameter this file belongs to.");
        return;
    }

    // Only call the handler if 1 or more files was dropped.
    if (count > 0) {
        checkFileSizes(files);

        handleFiles(files, paramName, target);
    }
    else {
        if (evt.dataTransfer.getData('Text') !== null
            && evt.dataTransfer.getData('Text') !== undefined
            && evt.dataTransfer.getData('Text') !== "") {
            // This must be a url and not a file

            var groupId = getGroupId(target);
            var fileObjListings = getFilesForGroup(groupId, paramName);

            // If file list and directory dropped, expand
            var dirUrl = evt.dataTransfer.getData('Text');
            var sourceNode = evt.dataTransfer.mozSourceNode || $("a[href='" + dirUrl + "']");
            if (sourceNode) { // Only expand directory if we can figure out the source node
                var paramDetails = run_task_info.params[paramName];
                var isFileList = paramDetails.allowMultiple;
                var isDirectory = $(sourceNode).data("directory") || $(sourceNode).data("kind") === "directory";
                if (isFileList && isDirectory) {
                    var isGenomeSpace = $(sourceNode).closest(".jstree").attr("id") === "genomeSpaceFileTree";
                    var isUploadFile = $(sourceNode).closest(".jstree").attr("id") === "uploadTree";

                    if (isGenomeSpace || isUploadFile) {
                        // Get the directory's children
                        var servletUrl = isGenomeSpace ? "/gp/GenomeSpace/tree?dir=" : isUploadFile ? "/gp/UploadFileTree/tree?dir=" : null;
                        var fileDiv = $(this).closest(".fileDiv");

                        $.ajax({
                            url: servletUrl + encodeURIComponent(dirUrl),
                            type: "GET",
                            dataType: "json",
                            success: function(data) {
                                // Populate the parameter with the child files
                                console.log(data);

                                var totalFileLength = fileObjListings.length + data.length;
                                validateMaxFiles(paramName, totalFileLength);

                                $.each(data, function(index, file) {
                                    var fileObj = {
                                        name: file.data.attr.href,
                                        id: fileId++
                                    };
                                    var isFile = !file.data.attr["data-directory"];
                                    if (isFile) {
                                        fileObjListings.push(fileObj);
                                    }
                                });

                                updateFilesForGroup(groupId, paramName, fileObjListings);
                                updateParamFileTable(paramName, fileDiv);
                                toggleFileButtons(paramName);
                            },
                            error: function() {
                                showErrorMessage("Unable to expand directory for file list parameter.");
                            }
                        });

                        return;
                    }
                }
            }

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

function handleFiles(files, paramName, fileDiv) {
    var groupId = getGroupId(fileDiv);
    var fileObjListings = getFilesForGroup(groupId, paramName);

    var totalFileLength = fileObjListings.length + files.length;
    validateMaxFiles(paramName, totalFileLength);

    //add newly selected files to table of file listing
    for (var f = 0; f < files.length; f++) {
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

function getMaxFiles(paramName) {
    var paramDetails = run_task_info.params[paramName];

    var maxValue = null;
    if (paramDetails !== null) {
        //in this case the max num of files is not unlimited
        if (paramDetails.maxValue !== undefined || paramDetails.maxValue !== null) {
            maxValue = parseInt(paramDetails.maxValue);
        }
    }

    return maxValue;
}

function validateMaxFiles(paramName, numFiles) {
    //check if max file length will be violated only if this not a batch parameter
    //in the case of batch we want to allow specifying any number of files
    if (isBatch(paramName)) {
        return;
    }

    var paramDetails = run_task_info.params[paramName];

    var maxFilesLimitExceeded = false;

    if (paramDetails !== null) {
        //in this case the max num of files is not unlimited
        if (paramDetails.maxValue !== undefined || paramDetails.maxValue !== null) {
            var maxValue = parseInt(paramDetails.maxValue);
            if (numFiles > maxValue) {
                maxFilesLimitExceeded = true;
            }
        }
    }

    //check that the user did not add more files than allowed
    if (maxFilesLimitExceeded) {
        alert("The maximum number of files that can be provided to the " + paramName +
            " parameter has been reached. Please delete some files " +
            "before continuing.");
        throw new Error("The maximum number of files that can be provided to " +
            "this parameter (" + paramName + ") has been reached. Please delete some files " +
            "before continuing.");
    }
}

function validateMaxValues(paramName, numValues) {
    //check if max values will be violated only if this not a batch parameter
    //in the case of batch we want to allow specifying any number of files
    if (isBatch(paramName)) {
        return true;
    }

    var paramDetails = run_task_info.params[paramName];

    var maxValuesLimitExceeded = false;

    if (paramDetails !== null) {
        //in this case the max num of files is not unlimited
        if (paramDetails.maxValue !== undefined || paramDetails.maxValue !== null) {
            var maxValue = parseInt(paramDetails.maxValue);
            if (numValues > maxValue) {
                maxValuesLimitExceeded = true;
            }
        }
    }

    //check that the user did not add more files than allowed
    if (maxValuesLimitExceeded) {
        alert("The maximum number of values that can be provided to the " + paramName +
            " parameter has been reached.");
        return false;
    }

    return true;
}

function checkFileSizes(files) {
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
                "<a href='http://www.broadinstitute.org/cancer/software/genepattern/user-guide" +
                "/sections/running-modules#_Uploading_Files' target='_blank'>User Guide</a>.</p>");
            errorMessageDiv.dialog({
                title: "Max File Upload Size Exceeded",
                resizable: true,
                height: 210,
                width: 500,
                modal: true,
                buttons: {
                    "OK": function () {
                        $(this).dialog("close");
                    }
                }
            });
            throw new Error("The provided file " + file.name + " is over the 2 GB limit.");
        }
    }
}

function updateParamFileTable(paramName, fileDiv, groupId) {
    if (groupId === undefined || groupId === null) {
        if (fileDiv === undefined || fileDiv === null) {
            javascript_abort("Not able to update file listing for " + paramName);
        }

        groupId = getGroupId(fileDiv);
    }

    var files = getFilesForGroup(groupId, paramName);

    if (fileDiv === null) {
        var paramRow = $("#" + jqEscape(paramName));
        //check if a groupId was given
        if (groupId !== undefined && groupId !== null) {
            paramRow.find(".valueEntryDiv").each(function () {
                if ($(this).data("groupId") === groupId) {
                    fileDiv = $(this).find(".fileDiv");
                }
            });
        }
        else {
            fileDiv = paramRow.find(".fileDiv").first();
        }

        if (fileDiv === null) {
            javascript_abort("Error populating file listing for " + paramName);
        }
    }

    var fileListingDiv = fileDiv.find(".fileListingDiv");

    var hideFiles = false;
    if (fileListingDiv.find(".editFilesLink").text() === "Show Files...") {
        hideFiles = true;
    }

    //remove previous file info data
    $(fileListingDiv).empty();

    if (files !== null && files !== undefined && files.length > 0) {
        //if there is one file and it is null or en empty string then do nothing and return
        if (files.length === 1 && (files[0].name === null || files[0].name === "")) {
            return;
        }

        //toggle view if this is a file choice parameter
        var fileChoiceToggle = fileDiv.parents(".pRow:first").find("input[id^=customFile_]");
        if (fileChoiceToggle.length !== 0) {
            //switch view to custom file view
            fileChoiceToggle.click();

            updateFilesForGroup(groupId, paramName, files);
        }

        var pData = $("<div class='fileDetails'/>");

        var editLink = $("<a class='editFilesLink' href='#'><img src='/gp/images/arrows-down.gif'/>Hide Files...</a>");
        editLink.click(function (event) {
            event.preventDefault();

            var editLinkMode = $(this).text();
            if (editLinkMode === "Show Files...") {
                $(this).text("Hide Files...");
                $(this).prepend("<img src='/gp/images/arrows-down.gif'/>");
                fileListingDiv.find(".paramFilesTable").removeClass("hidden");
            }
            else {
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
        for (var i = 0; i < files.length; i++) {
            //ignore any file names that are empty or null
            if (files[i].name === null || files[i].name === "") {
                continue;
            }

            var fileRow = $("<tr/>");
            var fileTData = $("<td class='pfileAction'/>");

            //determine if this is a  url
            if (files[i].name.indexOf("://") !== -1) {
                fileRow.append("<td><a href='" + files[i].name + "'> " + decodeURIComponent(files[i].name) + "</a></td>");
            }
            else {
                fileRow.append("<td>" + files[i].name + "</td>");
            }
            var delButton = $("<img class='images delBtn' src='/gp/images/delete-blue.png'/>");
            delButton.data("pfile", files[i].name);
            delButton.data("pfileId", files[i].id);

            delButton.button().click(function () {
                // Show the buttons again
                var fileDiv = $(this).closest(".fileDiv");
                fileDiv.find("fileUploadDiv").show();

                var file = $(this).data("pfile");
                var id = $(this).data("pfileId");

                var paramName = $(this).parents(".pRow").first().attr("id");
                var groups = parameter_and_val_groups[paramName].groups;
                for (var group in groups) {
                    var param_files = groups[group].files;
                    var param_values = groups[group].values;
                    for (var t = 0; t < param_files.length; t++) {
                        if (param_files[t].name === file
                            && param_files[t].id === id) {
                            var idx = $.inArray(param_files[t].name, param_values);
                            if (idx > -1) {
                                param_values.splice(idx, 1);
                                updateValuesForGroup(group, paramName, param_values);
                            }
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
            helper: function (event) {
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
        if (hideFiles) {
            editLink.click();
        }
    }

    // Hide or show the buttons if something is selected
    if (!isBatch(paramName) && atMaxFiles(paramName)) {
        fileDiv.find(".fileUploadDiv").hide();
    }
    else {
        fileDiv.find(".fileUploadDiv").show();
    }
}

function getFileCountForParam(paramName) {
    var groups = parameter_and_val_groups[paramName].groups;
    var count = 0;

    if (groups !== undefined || groups !== null) {
        for (var group in groups) {
            if (groups[group].files !== undefined && groups[group].files !== null) {
                count += groups[group].files.length;
            }
        }
    }

    return count;
}
function atMaxFiles(paramName) {
    //if this ia a batch parameter then allow mutiple files to be provided
    if (isBatch(paramName)) {
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


function uploadAllFiles() {
    if (!allFilesUploaded()) {
        $('#runTaskSettingsDiv').block({
            message: '<h1> Please wait... </h1>',
            overlayCSS: { backgroundColor: '#F8F8F8' }
        });

        $("#cancelUpload").show();
        var count = 0;

        var parameterNames = Object.keys(parameter_and_val_groups);
        for (var p = 0; p < parameterNames.length; p++) {
            var paramName = parameterNames[p];
            var groupIds = Object.keys(parameter_and_val_groups[paramName].groups);
            for (var g = 0; g < groupIds.length; g++) {
                var groupId = groupIds[g];
                var group = parameter_and_val_groups[paramName].groups[groupId];

                if (group.files !== undefined && group.files !== null) {
                    for (var f = 0; f < group.files.length; f++) {
                        var fileObj = group.files[f];
                        if (fileObj.object !== undefined && fileObj.object !== null) {
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
function uploadFile(paramName, file, fileOrder, fileId, groupId) {
    var id = fileId;
    fileId = "file_" + fileId;
    $("#fileUploadDiv").append("<div id='" + fileId + "'/>");
    $("#" + fileId).before("<p>" + file.name + "</p>");
    $("#" + fileId).append("<div class='progress-label' id='" + fileId + "Percentage'/>");

    //initialize the progress bar to 0
    $("#" + fileId).progressbar({
        value: 0
    });
    $("#" + fileId + "Percentage").text("0%");

    var destinationUrl = "/gp/rest/RunTask/upload";

    if (paramName === null || paramName === undefined) {
        console.log("An error occurred uploading files for module: " + run_task_info.name);
        console.log("DEBUG: parameter_and_val_groups is " + parameter_and_val_groups);

        //abort the upload since it will fail
        javascript_abort("Error uploading " + file.name + " parameter name not specified");
    }

    // prepare FormData
    var formData = new FormData();
    formData.append('ifile', file);
    formData.append('paramName', paramName);
    formData.append('index', id);

    var progressEvent = function (event) {
        if (event.lengthComputable) {

            var percentComplete = Math.round(event.loaded * 100 / event.total);
            console.log("percent complete: " + percentComplete);

            $("#" + fileId).progressbar({
                value: percentComplete
            });

            $("#" + fileId + "Percentage").text(percentComplete.toString() + "%");
        }
        else {
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
        xhr: function () {
            xhr = new window.XMLHttpRequest();
            //Upload progress
            if (xhr.upload) {
                xhr.upload.addEventListener("progress", progressEvent, false);
            }

            return xhr;
        },
        success: function (event) {
            console.log("on load response: " + event);

            var parsedEvent = typeof event === "string" ? $.parseJSON(event) : event;

            var groupFileInfo = getFilesForGroup(groupId, paramName);
            groupFileInfo[fileOrder].name = parsedEvent.location;
            delete groupFileInfo[fileOrder].object;

            updateFilesForGroup(groupId, paramName, groupFileInfo);

            if (allFilesUploaded()) {
                $("#cancelUpload").hide();
                submitTask();
            }

        },
        error: function (event) {
            $("#cancelUpload").trigger("click");
            $("#fileUploadDiv").html("<span style='color:red;'>There was an unexpected file transfer error while submitting your job. Please check your network connection and try to submit again or use the Uploader in the Files tab.</span>");
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
function setAllFileParamValues() {
    //now set the final file listing values for the file parameters
    var paramNames = Object.keys(parameter_and_val_groups);
    for (var p = 0; p < paramNames.length; p++) {
        var paramName = paramNames[p];
        var groups = parameter_and_val_groups[paramName].groups;
        if (groups === null) {
            javascript_abort("Error: could not retrieve groups for parameter " + paramName);
        }
        var groupNames = Object.keys(groups);
        for (var g = 0; g < groupNames.length; g++) {
            var param_files = parameter_and_val_groups[paramName].groups[groupNames[g]].files;
            var param_value_listing = parameter_and_val_groups[paramName].groups[groupNames[g]].values;
            var setFromFile = parameter_and_val_groups[paramName].groups[groupNames[g]].setFromFile;

            if(setFromFile == undefined || setFromFile == null)
            {
                setFromFile = false;
            }
            if (param_value_listing !== null &&
                param_value_listing !== undefined &&
                (param_value_listing.length > 0 && !setFromFile) || (param_files === undefined || param_files === null)) {
                //check if value already set from a choice list and this is a file parameter
                continue;
            }

            //flag this param as not being set from a choice list
            parameter_and_val_groups[paramName].groups[groupNames[g]].setFromFile = true;
            var fileList = [];

            for (var f = 0; f < param_files.length; f++) {
                var nextFileObj = param_files[f];

                fileList.push(nextFileObj.name);
            }
            updateValuesForGroup(groupNames[g], paramName, fileList);
        }
    }
}

function isBatch(paramName) {
    var paramDetails = run_task_info.params[paramName];
    if (paramDetails === null) {
        javascript_abort("No info found for parameter " + paramName);
    }

    return paramDetails.isBatch;

}

function makeBatch(paramName) {
    var selector = "#" + jqEscape(paramName);
    var input = $(selector);
    input.closest(".fileDiv").find(".ui-buttonset").find("label:last").click();
}

function allFilesUploaded() {
    for (var paramNames in parameter_and_val_groups) {
        var groups = parameter_and_val_groups[paramNames].groups;
        for (var groupId in groups) {
            var files = groups[groupId].files;
            for (var fileObjIndex in files) {
                //check if any file objects still exist
                if (files[fileObjIndex].object !== undefined && files[fileObjIndex].object !== null) {
                    return false;
                }
            }
        }
    }
    return true;
}

function javascript_abort(message) {

    var abortMsg = 'This is not an error. This is just to abort javascript';

    if (message !== undefined && message !== null && message !== "") {
        abortMsg = "Request to abort javascript execution: " + message;
        alert(abortMsg);
    }

    console.log(abortMsg);

    throw new Error(abortMsg);
}

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function getParamValueEntryDivByGroupId(paramName, groupId) {
    var valueElement = null;
    var paramRow = $("#" + jqEscape(paramName));

    if (groupId !== undefined && groupId !== null && groupId.length > 0) {
        paramRow.find(".valueEntryDiv").each(function () {
            if ($(this).data("groupId") === groupId) {
                valueElement = $(this);
            }
        });
    }

    return valueElement;
}

function setParameter(paramName, value, groupId) {
    var paramDetails = run_task_info.params[paramName];

    if (paramDetails === undefined || paramDetails === null) {
        alert("Unable to set the parameter value for " + paramName);
    }

    var paramRow = $("#" + jqEscape(paramName));

    var valueEntryDiv = getParamValueEntryDivByGroupId(paramName, groupId);

    if (valueEntryDiv === undefined || valueEntryDiv === null) {
        //if you get here then the groupId is probably null or not valid
        //in that case just grab the first value entry div
        valueEntryDiv = paramRow.find(".paramValueTd").find(".valueEntryDiv").first();
    }

    //if the type is choice and for the case of a file choice parameter
    //check that the choice is visible and not the upload your own file div
    if ($.inArray(field_types.CHOICE, paramDetails.type) !== -1
        && valueEntryDiv.find(".choice").is(":visible")) {
        valueEntryDiv.find(".choice").val(value);
        valueEntryDiv.find(".choice").multiselect("refresh");
        return
    }

    if ($.inArray(field_types.FILE, paramDetails.type) !== -1) {
        setInputField(paramName, value, groupId);
        return;
    }

    if ($.inArray(field_types.TEXT, paramDetails.type) !== -1) {
        paramRow.find(".paramValueTd").find(".textDiv").find(".pValue").first().val(value);
        paramRow.find(".paramValueTd").find(".textDiv").find(".pValue").first().trigger("change");
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
    return $.trim(parts[parts.length - 1]);
}

function cloneTask() {
    var cloneName = window.prompt("Name for cloned module", "copyOf" + run_task_info.name);
    if (cloneName === null || cloneName.length === 0) {
        return;
    }
    window.location.href = "/gp/saveTask.jsp?clone=1&name=" + run_task_info.name +
        "&LSID=" + encodeURIComponent(run_task_info.lsid) +
        "&cloneName=" + encodeURIComponent(cloneName) +
        "&userid=" + encodeURIComponent(getUsername()) +
        "&forward=" + encodeURIComponent("/gp/pages/index.jsf?lsid=" + encodeURIComponent(cloneName));
}

function sendToByKind(url, kind) {
    var paramNames = run_task_info.sendTo[kind];
    if (paramNames === undefined || paramNames === null || paramNames.length < 1) {
        console.log("ERROR: Sending to kind " + kind);
        return;
    }

    var selectedParam = paramNames[0];

    setInputField(selectedParam, url);
}

function tagResponse(event, ui) {
    var value = $(event.target).val();

    var matcher = new RegExp( "^" + $.ui.autocomplete.escapeRegex( value ), "i" );

    var contentLen = ui.content.length;

    var content = ui.content;

    for(var i=0;i<contentLen;i++)
    {
        if(matcher.test( ui.content[i].value))
        {
            ui.content.push(
                { "label" : ui.content[i].label,
                    "value" : ui.content[i].value
                });
        }
    }

    ui.content.splice(0, contentLen);
}

function showProperties() {
    $("#viewProperties").show();
}