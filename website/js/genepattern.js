// Implicit variables declared elsewhere
var genomeSpaceEnabled, genomeSpaceLoggedIn, username, jq, currentJobNumber, userLoggedIn,
    openUploadDirectoryDialog, uploadDirectorySelected, openSaveDialog, adminServerAllowed,
    parameter_and_val_groups, run_task_info, fileURL;


// used to make sure that a jquery id selector is escaped properly
function escapeJquerySelector(str) {
    return str.replace(/([ #;&,.+*~\':"!^$[\]()=>|\/@])/g,'\\$1');
}

// toggleCheckBoxes -- used in combination with a "master" checkbox to toggle
// the state of a collection of child checkboxes.  Assumes the children and parent
// share a common container parent
function toggleCheckBoxes(maincheckbox, parentId) {
    "use strict";
    var isChecked = maincheckbox.checked;
    var parentElement = document.getElementById(parentId);
    var elements = parentElement.getElementsByTagName("input");
    for (var i = 0; i < elements.length; i++) {
        if (elements[i].type = "checkbox") {
            elements[i].checked = isChecked;
        }
    }
}

// POST /jobResults/<job>/requestEmailNotification
function requestEmailNotification(cb, jobId, userEmail) {
    "use strict";
    $.ajax({
        type : "POST",
        url : '/gp/jobResults/' + jobId + '/requestEmailNotification',
        data : 'userEmail=' + userEmail,
        dataType : "json",
        success : function(data, textStatus, jqXHR) {
            ajaxEmailResponse(jqXHR);
        },
        error : function(data, textStatus, jqXHR) {
            cb.checked = false;
            alert('Error ' + jqXHR.status);
        }
    });
}

// POST /jobResults/<job>/cancelEmailNotification
function cancelEmailNotification(cb, jobId, userEmail) {
    "use strict";
    $.ajax({
        type : "POST",
        url : '/gp/jobResults/' + jobId + '/cancelEmailNotification',
        data : 'userEmail=' + userEmail,
        dataType : "json",
        success : function(data, textStatus, jqXHR) {
            ajaxEmailResponse(jqXHR);
        },
        error : function(data, textStatus, jqXHR) {
            cb.checked = false;
            alert('Error ' + jqXHR.status);
        }
    });
}

function ajaxEmailResponse(req) {
    "use strict";
    if (req.readyState == 4) {
        if (req.status >= 200 && req.status < 300) {
            // alert('all is well on email submission')
        }
        else {
            alert("There was a problem in email notification:\n" + req.status
                + ' -- ' + req.statusText);
        }
    }
}

// Requires jQuery & jQuery UI
function showDialog(title, message, button) {
    "use strict";
    var alert = document.createElement("div");

    if (typeof (message) == 'string') {
        alert.innerHTML = message;
    }
    else {
        $(alert).append(message);
    }

    if (button === undefined || button === null) {
        button = {
            "OK" : function(event) {
                $(this).dialog("close");
                if (event.preventDefault)
                    event.preventDefault();
                if (event.stopPropagation)
                    event.stopPropagation();
            }
        };
    }

    $(alert).dialog({
        modal : true,
        dialogClass : "top-dialog",
        width : 400,
        title : title,
        buttons : button,
        close : function() {
            $(this).dialog("destroy");
            $(this).remove();
        }
    });

    // Fix z-index for dialog
    var z = parseInt($(alert).parent().css("z-index"));
    if (z < 10000) {
        z += 9000;
        $(".top-dialog").css("z-index", z);
    }

    return alert;
}

/////////////////////////////////////////////////////////////////////////////////////////
//////////////      NEW UI FUNCTIONS
/////////////////////////////////////////////////////////////////////////////////////////

if (typeof $ === 'undefined') {
    //noinspection JSUnusedAssignment
    var $ = jq;
}
var all_modules = null;
var all_modules_map = null;
var all_categories = null;
var all_suites = null;
var kindToModules = null;

function getPinnedModules() {
    var pinned = [];

    $.each(all_modules, function(i, v) {
        for (var j = 0; j < v.tags.length; j++) {
            var tagObj = v.tags[j];
            if (tagObj.tag == "favorite") {
                pinned.push(v);
            }
        }
    });

    // Sort by position
    pinned = pinned.sort(function (a, b) {
        var a_pos = 0;
        //noinspection JSDuplicatedDeclaration
        for (var j = 0; j < a.tags.length; j++) {
            //noinspection JSDuplicatedDeclaration
            var tagObj = a.tags[j];
            if (tagObj.tag == "favorite") {
                a_pos = tagObj.metadata;
            }
        }

        var b_pos = 0;
        //noinspection JSDuplicatedDeclaration
        for (var j = 0; j < b.tags.length; j++) {
            //noinspection JSDuplicatedDeclaration
            var tagObj = b.tags[j];
            if (tagObj.tag == "favorite") {
                b_pos = tagObj.metadata;
            }
        }

        if (a_pos > b_pos) {
            return 1;
        }
        if (a_pos < b_pos) {
            return -1;
        }

        return 0;
    });

    return pinned;
}

function getRecentModules() {
    var recent = [];

    $.each(all_modules, function(i, v) {
        for (var j = 0; j < v.tags.length; j++) {
            var tagObj = v.tags[j];
            if (tagObj.tag == "recent") {
                recent.push(v);
            }
        }
    });

    return recent;
}

function initBrowseSuites() {
    var browse = $('<div id="module-list-suites"></div>').modulelist({
        title: '<a href="#" onclick="$(\'#module-browse\').searchslider(\'show\');">Browse Modules</a> &raquo; Browse Suites',
        data: all_suites,
        droppable: false,
        draggable: false,
        click: function(event) {
            var filter = $(event.currentTarget).find(".module-name").text();
            var modSearch = $("#module-search");
            modSearch.searchslider("show");
            modSearch.searchslider("tagfilter", filter);
            modSearch.searchslider("set_title", '<a href="#" onclick="$(\'#module-browse\').searchslider(\'show\');">Browse Modules</a> &raquo; <a href="#" onclick="$(\'#module-suites\').searchslider(\'show\');">Browse Suites</a> &raquo; ' + filter);
        }
    });

    $('#module-suites').searchslider({
        lists: [browse]
    });
}

function initBrowseModules() {
    return $('<div id="module-list-browse"></div>').modulelist({
        title: 'Browse Modules by Category',
        data: all_categories,
        droppable: false,
        draggable: false,
        click: function(event) {
            var filter = $(event.currentTarget).find(".module-name").text();
            var modSearch = $("#module-search");
            modSearch.searchslider("show");
            modSearch.searchslider("tagfilter", filter);
            modSearch.searchslider("set_title", '<a href="#" onclick="$(\'#module-browse\').searchslider(\'show\');">Browse Modules</a> &raquo; ' + filter);
        }
    });
}

function initBrowseTop() {
    return $('<div id="module-list-allnsuite"></div>').modulelist({
        title: 'Browse Modules &amp; Pipelines',
        data: [
            {
                "lsid": "",
                "name": "All Modules",
                "description": "Browse an alphabetical listing of all installed GenePattern modules and pipelines.",
                "version": "",
                "documentation": "http://genepattern.org",
                "categories": [],
                "suites": [],
                "tags": []
            },

            {
                "lsid": "",
                "name": "Browse by Suite",
                "description": "Browse available modules and pipelines by associated suites.",
                "version": "",
                "documentation": "http://genepattern.org",
                "categories": [],
                "suites": [],
                "tags": []
            }
        ],
        droppable: false,
        draggable: false,
        click: function(event) {
            var button = $(event.currentTarget).find(".module-name").text();
            if (button == 'All Modules') {
                var modSearch = $("#module-search");
                modSearch.searchslider("show");
                modSearch.searchslider("filter", '');
                modSearch.searchslider("set_title", '<a href="#" onclick="$(\'#module-browse\').searchslider(\'show\');">Browse Modules</a> &raquo; All Modules');
            }
            else {
                $("#module-suites").searchslider("show");
            }
        }
    });
}

function initSearchSlider() {
    var still_loading = false;

    var search = $('<div id="module-list-search"></div>').modulelist({
        title: 'Search: Modules &amp; Pipelines',
        data: all_modules,
        droppable: false,
        draggable: true,
        click: function(event) {
            var lsid = $(event.target).closest(".module-listing").module("get_lsid");
            if (!still_loading) {
                still_loading = true;
                setTimeout(function() {
                    console.log(still_loading);
                    still_loading = false;
                }, 400);
                loadRunTaskForm(lsid, false);
            }
        }
    });

    $('#module-search').searchslider({
        lists: [search]
    });
}

function initRecent() {
    var still_loading = false;
    var recent_modules = getRecentModules();

    var rmSelection = $('#recent-modules');
    var recent = rmSelection.modulelist({
        title: "Recent Modules",
        data: recent_modules,
        droppable: false,
        draggable: true,
        click: function(event) {
            var lsid = $(event.target).closest(".module-listing").module("get_lsid");
            if (!still_loading) {
                still_loading = true;
                setTimeout(function() {
                    still_loading = false;
                }, 800);
                loadRunTaskForm(lsid, false);
            }
        }
    });
    recent.modulelist("filter", "recent");
    rmSelection.find('.module-list-empty').text("No Recent Modules");
}

function calcPosition(placeholder) {
    return placeholder.index() - 2;
}

function baseLsid(lsid) {
    return lsid.substr(0, lsid.lastIndexOf(":"));
}

function initPinned() {
    var still_loading = false;
    var pinned_modules = getPinnedModules();

    var pmSelection = $('#pinned-modules');
    var pinned = pmSelection.modulelist({
        title: "Favorite Modules",
        data: pinned_modules,
        droppable: true,
        draggable: false,
        click: function(event) {
            var lsid = $(event.target).closest(".module-listing").module("get_lsid");
            if (!still_loading) {
                still_loading = true;
                setTimeout(function() {
                    console.log(still_loading);
                    still_loading = false;
                }, 400);
                loadRunTaskForm(lsid, false);
            }
        },
        add: function(event, ui) {
            //noinspection JSDuplicatedDeclaration
            var lsid = $(ui.item).find(".module-lsid").text();

            $.ajax({
                cache: false,
                type: 'POST',
                url: '/gp/rest/v1/tags/pin',
                dataType: 'text',
                data: JSON.stringify({
                    user: username,
                    lsid: baseLsid(lsid),
                    position: calcPosition(ui.placeholder)
                }),
                success: function() {
                    console.log("pinned");

                    $(".module-lsid:contains('" + lsid + "')").each(function(index, element) {
                        var module = $(element).parent();
                        module.module("add_tag", "favorite");
                    });
                }
            });

            // Reinitialize the widget as a module
            //noinspection JSDuplicatedDeclaration
            var lsid = $(ui.item).find(".module-lsid").text();							// Get the lsid
            var source = $("#module-list-search").modulelist("get_module", lsid);		// Get the source widget
            var data = source.module("get_data");										// Get the JSON data
            var click = source.module("get_click");										// Get the click event
            $(ui.item).empty();															// Empty the div
            $(ui.item).module({															// Reinitialize
                data: data,
                draggable: false,
                click: click
            });
        },
        remove: function(event, ui) {
            var lsid = $(ui.item).find(".module-lsid").text();

            $.ajax({
                type: 'DELETE',
                url: '/gp/rest/v1/tags/unpin',
                dataType: 'text',
                data: JSON.stringify({
                    user: username,
                    lsid: baseLsid(lsid),
                    position: 0
                }),
                success: function() {
                    console.log("unpinned");

                    $(".module-lsid:contains('" + lsid + "')").each(function(index, element) {
                        var module = $(element).parent();
                        module.module("remove_tag", "favorite");
                    });
                }
            });
        },
        reposition: function(event, ui) {
            $.ajax({
                type: 'PUT',
                url: '/gp/rest/v1/tags/repin',
                dataType: 'text',
                data: JSON.stringify({
                    user: username,
                    lsid: baseLsid($(ui.item).find(".module-lsid").text()),
                    position: calcPosition(ui.placeholder)
                }),
                success: function() {
                    console.log("repinned");
                }
            });
        }
    });
    pinned.modulelist("tagfilter", "favorite");
    pmSelection.find('.module-list-empty').text("Drag Modules Here");
}

function setModuleSearchTitle(filter) {
    if (filter === '') {
        $("#module-search").searchslider("set_title", "Search: Modules &amp; Pipelines");
    }
    else {
        $("#module-search").searchslider("set_title", "Search: " + filter);
    }
}

function updateJobStatusPage() {
    var isJobStatusOpen = $(".on-job-status-page").length > 0 && $("#jobResults:visible").length > 0;
    if (isJobStatusOpen && currentJobNumber !== undefined && currentJobNumber !== null) {
        // Get the status of the current job
        var status = $("#jobStatusIcon" + currentJobNumber).attr("alt");

        // If the job is still running, refresh
        if (status !== 'Finished' && status !== 'Error') {
            loadJobStatus(currentJobNumber);
        }
    }
}

function jobStatusPoll() {
    var _jobStatusPoll = function() {
        var continuePolling = $.data($(".current-job-status")[0], "continuePolling");
        if (continuePolling === undefined) continuePolling = true;

        initRecentJobs();

        // Update the job status page, if open and running
        updateJobStatusPage();

        if (continuePolling) {
            setTimeout(function() {
                _jobStatusPoll();
            }, 30000);
        }
    };

    if (userLoggedIn) {
        _jobStatusPoll();
    }
}

/**
 * Upload the multipart file
 *
 * @param file - File object to upload (as per the HTML5 FIle API)
 * @param directory - The path to the directory to upload to
 * @param done - Boolean array tracking which files from this set have finished uploading
 * @param index - The index of this file in the set
 */
function ajaxFileTabUpload(file, directory, done, index) {
    var _readChunk = function(reader, file, nextChunk, size, loadFunc, errorFunc) {
        reader.onload = loadFunc;
        reader.onerror = errorFunc;

        var start = nextChunk * size;
        var blob = file.slice(start, start + size);
        reader.readAsArrayBuffer(blob);
    };

    // Init the file reader
    var reader = new FileReader();

    var path = directory + encodeURIComponent(file.name); // The full url of the uploaded file
    var step = 1024*1024;                               // The chunk size
    var total = file.size;                              // The total file size
    var totalChunks = Math.ceil(total / step);          // Total number of chunks in file
    var nextChunk = 0;                                  // Index of the next chunk
    var eventQueue = [];                                // The queue of events to execute
    var eventComplete = true ;                          // Flag for if the current event is complete
    var eventError = null;                              // Flag for if the event has encountered an error
    var token = null;                                   // The token for this upload resource
    var totalQueue = null;                              // The total events that were in the queue

    var uploadToasterFile = $(".upload-toaster-file[name='" + escapeJquerySelector(file.name) + "']");
    var progressbar = uploadToasterFile.find(".upload-toaster-file-progress");
    // Set the cancel button functionality
    uploadToasterFile.find(".upload-toaster-file-cancel")
        .click(function() {
            // Set the progressbar cancel message
            progressbar.progressbar("value", 100);
            progressbar
                .find(".ui-progressbar-value")
                .css("background", "#FCF1F3");
            progressbar
                .find(".upload-toaster-file-progress-label")
                .text("Canceled!");

            // Mark this upload as done
            eventError = "Upload canceled";
        });

    var _setPercentComplete = function() {
        var percent = 100 - Math.floor((eventQueue.length / totalQueue) * 100);
        progressbar.progressbar("value", percent);
    };

    // Populate the event queue

    // Read the first chunk - Done now so we can detect error states early
    eventQueue.push(function() {
        _readChunk(reader, file, nextChunk, step,
            function() {
                eventComplete = true;
                _setPercentComplete();
            },
            function() {
                eventError = "Read Error: This could be because your connection closed during the " +
                    "upload or it could be because you were trying to upload a directory. Aborting upload.";
            });
    });

    // Create the upload resource
    eventQueue.push(function() {
        $.ajax({
            type: "POST",
            url: "/gp/rest/v1/upload/multipart/?path=" + encodeURIComponent(path) + "&parts=" + totalChunks,
            success: function(data) {
                eventComplete = true;
                token = data['token'];
                _setPercentComplete();
            },
            error: function(data) {
                eventError = data;
            }
        });
    });

    // Add the first PUT (since we have already read it in)
    eventQueue.push(function() {
        var uploadPayload = new Uint8Array(reader.result);
        $.ajax({
            type: "PUT",
            dataType: "arraybuffer",
            processData: false,
            contentType: false,
            data: uploadPayload,
            url: "/gp/rest/v1/upload/multipart/?path=" + encodeURIComponent(path) + "&token=" + encodeURIComponent(token) + "&index=" + nextChunk + "&parts=" + totalChunks,
            success: function() {
                eventComplete = true;
                nextChunk++;
                _setPercentComplete();
            },
            error: function(data) {
                eventError = data;
            }
        });
    });

    // Add the remaining reads then PUTs
    for (var i = 1; i < totalChunks; i++) {

        // Read the next chunk
        eventQueue.push(function() {
            _readChunk(reader, file, nextChunk, step,
                function() {
                    eventComplete = true;
                    _setPercentComplete();
                },
                function() {
                    eventError = "Uploading directories is not supported. Aborting upload.";
                });
        });

        // Then upload it
        eventQueue.push(function() {
            var uploadPayload = new Uint8Array(reader.result);
            $.ajax({
                type: "PUT",
                dataType: "arraybuffer",
                processData: false,
                contentType: false,
                data: uploadPayload,
                url: "/gp/rest/v1/upload/multipart/?path=" + encodeURIComponent(path) + "&token=" + encodeURIComponent(token) + "&index=" + nextChunk + "&parts=" + totalChunks,
                success: function() {
                    eventComplete = true;
                    nextChunk++;
                    _setPercentComplete();
                },
                error: function(data) {
                    eventError = data;
                }
            });
        });
    }

    // Add the check to make sure everything is uploaded
    eventQueue.push(function() {
        $.ajax({
            type: "GET",
            url: "/gp/rest/v1/upload/multipart/?path=" + encodeURIComponent(path) + "&token=" + encodeURIComponent(token) + "&parts=" + totalChunks,
            success: function(data) {
                var missing = data['missing'];
                if (missing.length > 0) {
                    eventError = "Parts missing: " + missing;
                }
                else {
                    eventComplete = true;
                    _setPercentComplete();
                }
            },
            error: function(data) {
                eventError = data;
            }
        });
    });

    // Add the command to assemble the file
    eventQueue.push(function() {
        progressbar
            .find(".upload-toaster-file-progress-label")
            .text("Assembling File...");
        progressbar.attr("title", "Please wait... For large files this could take several minutes...");
        progressbar.tooltip();
        $.ajax({
            type: "POST",
            url: "/gp/rest/v1/upload/multipart/assemble/?path=" + encodeURIComponent(path) + "&token=" + token + "&parts=" + totalChunks,
            success: function(data) {
                eventComplete = true;
                token = data['token'];
            },
            error: function(data) {
                eventError = data;
            }
        });
    });

    // Add the event to mark this upload as done
    eventQueue.push(function() {
        progressbar.progressbar("value", 100);
        done[index] = true;
        eventComplete = true;
    });


    // Execute the event queue
    var _checkEventQueue = function() {
        if (eventError !== null) {                  // OH SHIT - There's an error
            if (typeof eventError === 'object') {
                eventError = eventError.responseText;
            }

            // Set the top error message
            showErrorMessage(eventError);

            // Set the progressbar error message
            progressbar.progressbar("value", 100);
            progressbar
                .find(".ui-progressbar-value")
                .css("background", "#FCF1F3");
            progressbar
                .find(".upload-toaster-file-progress-label")
                .text("Error!");

            // Mark this upload as done
            done[index] = true;
            return;
        }

        if (eventComplete) {
            eventComplete = false;                  // Next event is not complete
            var event = eventQueue.shift();         // Get the next event
            if (event !== null && event !== undefined) {
                event();                            // Execute it
                setTimeout(_checkEventQueue, 1000); // Check the event queue again in a bit
            }
        }
        else {
            setTimeout(_checkEventQueue, 1000);     // Check the event queue again in a bit
        }
    };

    totalQueue = eventQueue.length;
    _checkEventQueue();
}

function hasSpecialChars(filelist) {
    var regex = new RegExp("[^A-Za-z0-9_.]");
    for (var i = 0; i < filelist.length; i++) {
        var file = filelist[i];
        if (regex.test(file.name)) {
            return true;
        }
    }

    return false;
}

function warnSpecialChars(filelist, directory) {
    showDialog("Special Characters!",
            "One or more files being uploaded has a name containing special characters!<br/><br/>" +
            "Some older GenePattern modules do not handle special characters well. " +
            "Are you sure you want to continue?", {
            "Yes": function() {
                $(this).dialog("close");
                dirPromptIfNecessary(filelist, directory);
            },
            "No": function() {
                $(this).dialog("close");
            }
        });
}

function dirPromptIfNecessary (filelist, directory) {
    if (directory == undefined || directory == null || directory.length == 0) {
        openUploadDirectoryDialog(filelist);
    }
    else {
        uploadAfterDialog(filelist, directory);
    }
}

function initUploadToaster(filelist) {
    // Hide the dropzone
    $("#upload-dropzone-wrapper").hide("slide", { direction: "down" }, 200);

    // Create the dialog contents
    var toaster = $("<div></div>").addClass("upload-toaster-list");
    for (var i = 0; i < filelist.length; i++) {
        var file = filelist[i];
        $("<div></div>")
            .addClass("upload-toaster-file")
            .attr("name", file.name)
            .append(
            $("<span></span>")
                .addClass("upload-toaster-file-name")
                .text(file.name)
        )
            .append(
            $("<div></div>")
                .addClass("upload-toaster-file-progress")
                .progressbar({
                    change: function() {
                        $(this).find(".upload-toaster-file-progress-label").text($(this).progressbar("value") + "%");
                    },
                    complete: function() {
                        $(this).find(".upload-toaster-file-progress-label").text("Complete!");
                        $(this).parent().find(".upload-toaster-file-cancel").button("disable");
                    }
                })
                .append(
                $("<div></div>")
                    .addClass("upload-toaster-file-progress-label")
                    .text("Pending")
            )
        )
            .append(
            $("<button></button>")
                .addClass("upload-toaster-file-cancel")
                .text("Cancel")
                .button()
        )
            .appendTo(toaster);
    }

    // Create the dialog
    toaster
        .attr("id", "upload-toaster")
        .dialog({
            "title" : "GenePattern Uploads",
            "width": 585,
            "height": 250,
            "buttons" : {},
            "dialogClass": "upload-dialog"
        })
        .dialogExtend({
            "closable" : true,
            "maximizable" : false,
            "minimizable" : true,
            "collapsable" : false,
            "minimizeLocation" : "left",
            "load" : function() {
                $(".upload-dialog").find(".ui-dialog-titlebar-close").hide();
            },
            "minimize" : function() {
                $("#dialog-extend-fixed-container")
                    .find(".upload-dialog")
                    .removeAttr("style");
            },
            "icons" : {
                "close" : "ui-icon-close",
                "minimize" : "ui-icon-minus",
                "restore" : "ui-icon-bullet"
            }
        });
}

function cleanUploadToaster() {
    // Close dialog if minimized
    $("#dialog-extend-fixed-container").find(".upload-dialog").remove();

    // Disable minimize button and enable close if not minimized
    var uploadDialog = $(".upload-dialog");
    uploadDialog.find(".ui-dialog-titlebar-minimize").remove();
    uploadDialog.find(".ui-dialog-titlebar-close").show();

    // Show the dropzone
    $("#upload-dropzone-wrapper").show("slide", { direction: "up" }, 200);

    // Refresh the tree
    refreshUploadTree();
}

function uploadAfterDialog(filelist, directory) {
    // Set up the upload toaster
    initUploadToaster(filelist, directory);

    // Create upload done indicator
    var done = [];

    // Do each upload
    for (var i = 0; i < filelist.length; i++) {
        // Create the done indicator
        done[i] = false;

        // Upload the file
        var file = filelist[i];
        ajaxFileTabUpload(file, directory, done, i);
    }

    // Finish all uploads, cycling until done
    var testForCleanup = function() {
        setTimeout(function() {
            if (done.reduce(function(a, b) {return a && b})) {
                cleanUploadToaster();
            }
            else {
                testForCleanup();
            }
        }, 1000);
    };
    testForCleanup();
}

function uploadEnter() {
    this.classList.add('leftnav-highlight');
}

function uploadLeave() {
    this.classList.remove('leftnav-highlight');
}

function uploadExit(evt) {
    evt.stopPropagation();
    evt.preventDefault();
}

function uploadOver(evt) {
    this.classList.add('leftnav-highlight');
    evt.stopPropagation();
    evt.preventDefault();
}

function uploadDrop(event) {
    this.classList.remove('leftnav-highlight');
    event.stopPropagation();
    event.preventDefault();

    var ul = document.createElement("ul");
    var filelist = event.dataTransfer.files;

    // Prevent uploads from interrupting other uploads
    if ($("#upload-dropzone-progress:visible").length > 0) {
        showDialog("Upload Initialization Error", "Please wait for all current uploads to complete before initiating another upload.");
        return;
    }

    if (filelist.length < 1) {
        showDialog("Operation Not Supported", "Sorry! This drag-and-drop operation is not supported.");
        return;
    }

    // Check for special characters
    var directory = $(event.target).closest(".jstree-closed, .jstree-open").find("a:first").attr("href");
    if(hasSpecialChars(filelist))
    {
        warnSpecialChars(filelist, directory);
    }
    else
    {
        dirPromptIfNecessary(filelist, directory);
    }
}

function initUploads() {
    // Attach events to the upload drop zone
    var dropzone = $("#upload-dropzone");
    dropzone[0].addEventListener("dragenter", uploadEnter, true);
    dropzone[0].addEventListener("dragleave", uploadLeave, true);
    dropzone[0].addEventListener("dragexit", uploadExit, false);
    dropzone[0].addEventListener("dragover", uploadOver, false);
    dropzone[0].addEventListener("drop", uploadDrop, false);

    // Set up the exit prompt
    window.onbeforeunload = function() {
        if ($(".upload-dialog:visible").length > 0) {
            //$("#content").effect("shake");
            var cover = $("<div></div>")
                .css("position", "fixed")
                .css("top", 0)
                .css("left", 0)
                .css("width", "100%")
                .css("height", "100%")
                .css("z-index", 10000)
                .css("background-color", "yellow")
                .css("opacity", 0.3)
                .appendTo("body");
            setTimeout(function() {
                cover.remove();
            }, 100);
            return "You are currently uploading files. If you navigate away from this page this will interrupt your file upload.";
        }
        return undefined;
    };

    // Add click to browse functionality
    $("<input />")
        .attr("id", "upload-dropzone-input")
        .attr("multiple", "multiple")
        .attr("type", "file")
        .change(function(event) {
            var origin = $("#upload-dropzone-input").data("origin");
            var filelist = event.target.files;

            var directory = null;
            if(origin != "dropzone")
            {
                directory = origin;
            }

            //check for special characters
            if(hasSpecialChars(filelist)) {
                warnSpecialChars(filelist, directory);
            }
            else
            {
                dirPromptIfNecessary(filelist, directory);
            }
        })
        .appendTo("#upload-dropzone-wrapper");

    dropzone.click(function() {
        var dropzoneInput = $("#upload-dropzone-input");
        dropzoneInput.data("origin", "dropzone");
        dropzoneInput.trigger("click");
    });
}

function initUploadTreeDND() {
    // Ready the drop & drop aspects of the file tree
    var eventsAttached = [];
    $("#uploadTree").find(".jstree-closed, .jstree-open").each(function(index, element) {
        var folder = $(element);

        // Protect against empties & repeats
        if (folder === null || folder === undefined || folder.length < 1) return;
        if ($.inArray(folder[0], eventsAttached) > -1) return;

        folder[0].addEventListener("dragenter", uploadEnter, true);
        folder[0].addEventListener("dragleave", uploadLeave, true);
        folder[0].addEventListener("dragexit", uploadExit, false);
        folder[0].addEventListener("dragover", uploadOver, false);
        folder[0].addEventListener("drop", uploadDrop, false);

        // Add to list to prevent repeats
        eventsAttached.push(folder[0]);

        var uploadTree = $("#uploadTree");
        var ready = uploadTree.data("dndReady");
        if (ready === undefined || ready === null) {
            uploadTree.data("dndReady", {});
            ready = uploadTree.data("dndReady");
        }
        ready[folder.attr("id")] = true;
    });
}

function initAllModulesMap(all_modules) {
    var modMap = {};

    for (var i = 0; i < all_modules.length; i++) {
        var mod = all_modules[i];
        modMap[mod.lsid] = mod;
    }

    all_modules_map = modMap;
}

function lsidsToModules(lsidList) {
    var toReturn = [];
    for (var i = 0; i < lsidList.length; i++) {
        var lsid = lsidList[i];
        var module = all_modules_map[lsid];
        if (module === null || module === undefined) {
            console.log("Error finding LSID to create file widget: " + lsid);
            continue;
        }
        toReturn.push(module);
    }
    return toReturn;
}

function showErrorMessage(message) {
    var messageBox = $("#errorMessageDiv");
    messageBox.find("#errorMessageContent").text(message);
    if (messageBox.is(":visible")) {
        messageBox.effect("shake", {}, 500);
    }
    else {
        messageBox.show("shake", {}, 500);
    }
}

function showSuccessMessage(message) {
    var infoDiv = $("#infoMessageDiv");
    infoDiv.find("#infoMessageContent").text(message);
    infoDiv.show();
}

function createGenomeSpaceWidget(linkElement, appendTo) {
    var _isGenomeSpaceRoot = function(url) {
        var parts = url.split("dm.genomespace.org/datamanager/");
        var path = parts[parts.length-1];
        var pieces = path.split("/");
        return pieces.length <= 4;
    };

    var _constructGenomeSpaceMenuData = function(isRoot, isDirectory) {
        var data = [];

        if (!isRoot || !isDirectory) {
            data.push({
                "lsid": "",
                "name": "Delete " + (isDirectory ? "Directory" : "File"),
                "description": (isDirectory ? "Permanently delete this directory and all child files." : "Permanently delete this file."),
                "version": "<span class='glyphicon glyphicon-remove' ></span>",
                "documentation": "", "categories": [], "suites": [], "tags": []
            });
        }

        if (isDirectory) {
            data.push({
                "lsid": "",
                "name": "Create Subdirectory",
                "description": "Create a subdirectory in this directory.",
                "version": "<span class='glyphicon glyphicon-folder-open' ></span>",
                "documentation": "", "categories": [], "suites": [], "tags": []
            });
        }

        else if (!isDirectory) {
            data.push({
                "lsid": "",
                "name": "Save File",
                "description": "Save a copy of this file to your local computer.",
                "version": "<span class='glyphicon glyphicon-floppy-save' ></span>",
                "documentation": "", "categories": [], "suites": [], "tags": []
            });
        }

        return data;
    };

    var _parseList = function(listString) {
        if (listString === null || listString === undefined) {
            return [];
        }
        else {
            return JSON.parse(listString);
        }
    };

    var _createGenomeSpaceWidgetInner = function(linkElement, appendTo) {
        var link = $(linkElement);
        var url = link.attr("href");
        var name = $(linkElement).text();
        var isDirectory = link.attr("data-directory") === "true";
        var isRoot = _isGenomeSpaceRoot(url);

        var kind = _parseList(linkElement.attr("data-kind"));
        var clients = _parseList(linkElement.attr("data-clients"));
        var convertUrls = _parseList(linkElement.attr("data-converturls"));
        var listList = [];

        var data = _constructGenomeSpaceMenuData(isRoot, isDirectory);
        var actionList = $("<div></div>")
            .attr("class", "file-widget-actions")
            .modulelist({
                title: name,
                data: data,
                droppable: false,
                draggable: false,
                click: function(event) {
                    var saveAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Save File") == 0;
                    var deleteAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Delete") == 0;
                    var subdirAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Create Subdirectory") == 0;

                    var listObject = $(event.target).closest(".search-widget");
                    var url = listObject.attr("name");

                    if (saveAction) {
                        window.location.href = url;
                        $(".search-widget:visible").searchslider("hide");
                    }

                    else if (deleteAction) {
                        if (confirm('Are you sure you want to delete the selected file or directory?')) {
                            $.ajax({
                                type: "DELETE",
                                url: "/gp/rest/v1/genomespace/delete?url=" + url,
                                success: function(data) {
                                    showSuccessMessage(data);

                                    $(".left-nav-genomespace-refresh-real").trigger("click");
                                },
                                error: function(data) {
                                    if (typeof data === 'object') {
                                        data = data.responseText;
                                    }

                                    showErrorMessage(data);
                                }
                            });

                            $(".search-widget:visible").searchslider("hide");
                        }
                    }

                    else if (subdirAction) {
                        showDialog("Name the Subdirectory", "What name would you like to give the subdirectory?" +
                            "<input type='text' class='dialog-subdirectory-name' style='width: 98%;' />", {
                            "Create": function() {
                                var subdirName = $(".dialog-subdirectory-name").val();

                                var _createSubdirectory = function() {
                                    $.ajax({
                                        type: "PUT",
                                        url: "/gp/rest/v1/genomespace/createDirectory?url=" + encodeURIComponent(url) + "&name=" + encodeURIComponent(subdirName),
                                        success: function(data) {
                                            showSuccessMessage(data);

                                            $(".left-nav-genomespace-refresh-real").trigger("click");
                                        },
                                        error: function(data) {
                                            if (typeof data === 'object') {
                                                data = data.responseText;
                                            }

                                            showErrorMessage(data);
                                        }
                                    });
                                };

                                // Check for special characters
                                var regex = new RegExp("[^A-Za-z0-9_.]");
                                var specialCharacters = regex.test(subdirName);
                                if(specialCharacters) {
                                    var outerDialog = $(this);
                                    showDialog("Special Characters!",
                                            "The name you selected contains special characters!<br/><br/>" +
                                            "Some older GenePattern modules do not handle special characters well. " +
                                            "Are you sure you want to continue?", {
                                            "Yes": function() {
                                                _createSubdirectory();
                                                $(this).dialog("close");
                                                $(outerDialog).dialog("close");
                                            },
                                            "No": function() {
                                                $(this).dialog("close");
                                            }
                                        });
                                }
                                else {
                                    _createSubdirectory();
                                    $(this).dialog("close");
                                }
                            },
                            "Cancel": function() {
                                $(this).dialog("close");
                            }
                        });
                        $(".ui-dialog-buttonset:visible button:first").button("disable");
                        $(".dialog-subdirectory-name").keyup(function(event) {
                            if ($(event.target).val() === "") {
                                $(".ui-dialog-buttonset:visible button:first").button("disable");
                            }
                            else {
                                $(".ui-dialog-buttonset:visible button:first").button("enable");
                            }
                        });

                        $(".search-widget:visible").searchslider("hide");
                    }

                    else {
                        console.log("ERROR: Executing click function for " + url);
                        $(".search-widget:visible").searchslider("hide");
                    }
                }
            });
        listList.push(actionList);

        // Create send to tool lists
        var toolData = [];
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < clients.length; i++) {
            var tool = clients[i];
            toolData.push({
                "lsid": tool,
                "name": "Send to " + tool,
                "description": "Send this file from GenePattern to " + tool + " using GenomeSpace.",
                "version": "<img src='/gp/pages/genomespace/genomespace_icon.png' class='module-list-icon'>",
                "documentation": "http://genomespace.org", "categories": [], "suites": [], "tags": []
            });
        }

        var toolList = $("<div></div>")
            .attr("class", "file-widget-actions send-to-tool-list")
            .attr("data-kind", aKind)
            .attr("data-url", convertUrl)
            .modulelist({
                title: "Send to GenomeSpace Tool",
                data: toolData,
                droppable: false,
                draggable: false,
                click: function(event) {
                    var tool = $(event.target).closest(".module-listing").find(".module-lsid").text();
                    window.open("/gp/rest/v1/genomespace/tool?tool=" + encodeURIComponent(tool) + "&url=" + encodeURIComponent(url));
                    $(".search-widget:visible").searchslider("hide");
                }
            });
        listList.push(toolList);

        // Create send to parameter lists
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < kind.length; i++) {
            //noinspection JSDuplicatedDeclaration
            var aKind = kind[i];
            //noinspection JSDuplicatedDeclaration
            var convertUrl = convertUrls[aKind];

            var paramList = $("<div></div>")
                .attr("class", "send-to-param-list")
                .attr("data-kind", aKind)
                .attr("data-url", convertUrl)
                .modulelist({
                    title: "Send to Parameter as " + aKind,
                    data: [],
                    droppable: false,
                    draggable: false,
                    click: function(event) {}
                });
            listList.push(paramList);
        }

        // Create send to module lists
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < kind.length; i++) {
            //noinspection JSDuplicatedDeclaration
            var aKind = kind[i];
            //noinspection JSDuplicatedDeclaration
            var convertUrl = convertUrls[aKind];

            var lsidList = kindToModules[aKind];
            if (lsidList === null || lsidList === undefined) lsidList = [];
            var sendToList = lsidsToModules(lsidList).sort(function (a, b) {
                if (a.name.toLowerCase() < b.name.toLowerCase()) return -1;
                if (a.name.toLowerCase() > b.name.toLowerCase()) return 1;
                return 0;
            });

            var moduleList = $("<div></div>")
                .attr("class", "send-to-module-list")
                .attr("data-kind", aKind)
                .attr("data-url", convertUrl)
                .modulelist({
                    title: "Send to Module as " + aKind,
                    data: sendToList,
                    droppable: false,
                    draggable: true,
                    click: function(event) {
                        var lsid = this.data.lsid;
                        var listObject = $(event.target).closest(".send-to-module-list");
                        var kind = listObject.attr("data-kind");
                        var url = listObject.attr("data-url");

                        loadRunTaskForm(lsid, false, kind, url);

                        var checkForRunTaskLoaded = function() {
                            if (run_task_info.lsid === lsid) {
                                sendToByKind(url, kind);
                            }
                            else {
                                setTimeout(function() {
                                    checkForRunTaskLoaded();
                                }, 100);
                            }
                        };

                        checkForRunTaskLoaded();
                    }
                });
            listList.push(moduleList);
        }

        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < listList.length; i++) {
            var aList = listList[i];
            if (aList.find(".module-listing").length < 1) {
                aList.hide();
            }
        }

        var widget = $("<div></div>")
            .attr("name", link.attr("href"))
            .attr("class", "search-widget file-widget")
            .searchslider({lists: listList});

        $(appendTo).append(widget);

        // Init the initial send to parameters
        var sendToParamList = widget.find(".send-to-param-list");
        sendToParamForMenu(sendToParamList);
    };

    if (all_modules_map !== null) {
        _createGenomeSpaceWidgetInner(linkElement, appendTo);
    }
    else {
        setTimeout(function() {
            createGenomeSpaceWidget(linkElement, appendTo);
        }, 100);
    }
}

function createInputFileWidget(linkElement, appendTo) {
    var _constructFileMenuData = function() {
        var data = [];

        data.push({
            "lsid": "",
            "name": "Save or View File",
            "description": "Will either open the file for viewing or prompt you to save the file.",
            "version": "<span class='glyphicon glyphicon-floppy-save' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
        });

        return data;
    };

    var _createFileWidgetInner = function(linkElement, appendTo) {
        var link = $(linkElement);
        var url = link.attr("href");
        var name = $(linkElement).text();

        var data = _constructFileMenuData();

        var actionList = $("<div></div>")
            .attr("class", "file-widget-actions")
            .modulelist({
                title: name,
                data: data,
                droppable: false,
                draggable: false,
                click: function(event) {
                    var saveAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Save") == 0;

                    if (saveAction) {
                        window.open(url);
                        $(".search-widget:visible").searchslider("hide");
                    }

                    else {
                        console.log("ERROR: Executing click function for " + url);
                        $(".search-widget:visible").searchslider("hide");
                    }
                }
            });

        var widget = $("<div></div>")
            .attr("name", link.attr("href"))
            .attr("class", "search-widget file-widget")
            .searchslider({
                lists: [actionList]});

        $(appendTo).append(widget);
    };

    if (all_modules_map !== null) {
        _createFileWidgetInner(linkElement, appendTo);
    }
    else {
        setTimeout(function() {
            createInputFileWidget(linkElement, appendTo);
        }, 100);
    }
}

function refreshUploadTree() {
    var uploadTree = $("#uploadTree");
    uploadTree.data("dndReady", {});
    uploadTree.jstree("refresh");

    $("#uploadDirectoryTree").jstree("refresh");
}

function createFileWidget(linkElement, appendTo) {
    var _constructFileMenuData = function(isRoot, isDirectory, isUpload, isJobFile, isPartialFile) {
        var data = [];

        if (!isRoot || !isDirectory) {
            data.push({
                "lsid": "",
                "name": "Delete " + (isDirectory ? "Directory" : "File"),
                "description": (isDirectory ? "Permanently delete this directory and all child files." : "Permanently delete this file."),
                "version": "<span class='glyphicon glyphicon-remove' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
            });
        }

        if (isDirectory) {
            data.push({
                "lsid": "",
                "name": "Create Subdirectory",
                "description": "Create a subdirectory in this directory.",
                "version": "<span class='glyphicon glyphicon-folder-open' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
            });

            data.push({
                "lsid": "",
                "name": "Upload Files",
                "description": "Upload files to this directory.",
                "version": "<span class='glyphicon glyphicon-cloud-upload' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
            });
        }
        else if (!isPartialFile) {
            data.push({
                "lsid": "",
                "name": "Save File",
                "description": "Save a copy of this file to your local computer.",
                "version": "<span class='glyphicon glyphicon-floppy-save' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
            });

            if (genomeSpaceEnabled && genomeSpaceLoggedIn) {
                data.push({
                    "lsid": "",
                    "name": "Save to Genomespace",
                    "description": "Save a copy of this file to your GenomeSpace account.",
                    "version": "<img src='/gp/pages/genomespace/genomespace_icon.png' class='module-list-icon'>", "documentation": "", "categories": [], "suites": [], "tags": []
                });
            }
        }

        if (!isPartialFile && isUpload) {
            data.push({
                "lsid": "",
                "name": "Rename " + (isDirectory ? "Directory" : "File"),
                "description": "Rename this file or directory",
                "version": "<span class='glyphicon glyphicon-text-width' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
            });

            data.push({
                "lsid": "",
                "name": "Move " + (isDirectory ? "Directory" : "File"),
                "description": "Move this file or directory",
                "version": "<span class='glyphicon glyphicon-share-alt' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
            });
        }

        if (isJobFile) {
            data.push({
                "lsid": "",
                "name": "Create Pipeline",
                "description": "Create a provenance pipeline from this file.",
                "version": "<span class='glyphicon glyphicon-road' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
            });

            data.push({
                "lsid": "",
                "name": "Copy to Files Tab",
                "description": "Copies this file from your job results to your files tab.",
                "version": "<span class='glyphicon glyphicon-share' style='-webkit-transform:rotateY(180deg); -moz-transform:rotateY(180deg); -o-transform:rotateY(180deg); -ms-transform:rotateY(180deg);' ></span>",
                "documentation": "", "categories": [], "suites": [], "tags": []
            });
        }

        return data;
    };

    var _createFileWidgetInner = function(linkElement, appendTo) {
        var link = $(linkElement);
        var url = link.attr("href");
        var name = $(linkElement).text();
        var isDirectory = isDirectoryFromUrl(url);
        var isRoot = isRootFromUrl(url);
        var isUpload = appendTo === "#menus-uploads";
        var isJobFile = appendTo === "#menus-jobs";
        var isPartialFile = linkElement.attr("data-partial") === "true";

        var kind = linkElement.attr("data-kind");

        var lsidList = kindToModules[kind];
        if (lsidList === null || lsidList === undefined) lsidList = [];
        var sendToList = lsidsToModules(lsidList).sort(function (a, b) {
            if (a.name.toLowerCase() < b.name.toLowerCase()) return -1;
            if (a.name.toLowerCase() > b.name.toLowerCase()) return 1;
            return 0;
        });

        var data = _constructFileMenuData(isRoot, isDirectory, isUpload, isJobFile, isPartialFile);

        var actionList = $("<div></div>")
            .attr("class", "file-widget-actions")
            .modulelist({
                title: name,
                data: data,
                droppable: false,
                draggable: false,
                click: function(event) {
                    var saveAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Save File") == 0;
                    var deleteAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Delete") == 0;
                    var subdirAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Create Subdirectory") == 0;
                    var uploadAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Upload") == 0;
                    var pipelineAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Create Pipeline") == 0;
                    var genomeSpaceAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Save to Genomespace") == 0;
                    var renameAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Rename") == 0;
                    var jobCopyAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Copy to Files") == 0;
                    var moveAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Move") == 0;

                    var listObject = $(event.target).closest(".search-widget").find(".send-to-param-list");
                    var url = listObject.attr("data-url");
                    var path = uploadPathFromUrl(url);

                    if (saveAction) {
                        window.location.href = url + "?download";
                        $(".search-widget:visible").searchslider("hide");
                    }

                    else if (jobCopyAction) {
                        openUploadDirectoryDialog(null, function() {
                            var moveToUrl = $(uploadDirectorySelected).attr("href");
                            var moveToPath = uploadPathFromUrl(moveToUrl);

                            $.ajax({
                                type: "POST",
                                url: "/gp/rest/v1/data/copy/?from=" + encodeURIComponent(path) + "&to=" + encodeURIComponent(moveToPath) + encodeURIComponent(name.trim()),
                                success: function(data) {
                                    showSuccessMessage(data);
                                    refreshUploadTree();

                                    $(".search-widget:visible").searchslider("hide");
                                },
                                error: function(data) {
                                    if (typeof data === 'object') {
                                        data = data.responseText;
                                    }

                                    showErrorMessage(data);
                                    $(".search-widget:visible").searchslider("hide");
                                }
                            });
                        });
                    }

                    else if (moveAction) {
                        openUploadDirectoryDialog(null, function() {
                            var moveToUrl = $(uploadDirectorySelected).attr("href");
                            var moveToPath = uploadPathFromUrl(moveToUrl);

                            $.ajax({
                                type: "PUT",
                                url: "/gp/rest/v1/data/move/?from=" + encodeURIComponent(path) + "&to=" + encodeURIComponent(moveToPath) + encodeURIComponent(name.trim()),
                                success: function(data) {
                                    showSuccessMessage(data);
                                    refreshUploadTree();
                                },
                                error: function(data) {
                                    if (typeof data === 'object') {
                                        data = data.responseText;
                                    }

                                    showErrorMessage(data);
                                }
                            });
                        });

                        $(".search-widget:visible").searchslider("hide");
                    }

                    else if (renameAction) {
                        showDialog("Rename the File or Directory", "What name would you like to name the file or directory?" +
                            "<input type='text' class='dialog-rename' value='" + name.trim() + "' style='width: 98%;' />", {
                            "Rename": function() {
                                var newName = $(".dialog-rename").val();

                                var _doRename = function() {
                                    $.ajax({
                                        type: "PUT",
                                        url: "/gp/rest/v1/data/rename?path=" + encodeURIComponent(path) + "&name=" + encodeURIComponent(newName),
                                        success: function(data) {
                                            showSuccessMessage(data);

                                            if (isUpload) {
                                                refreshUploadTree();
                                            }
                                            if (isJobFile) {
                                                initRecentJobs();
                                            }
                                            $(".search-widget:visible").searchslider("hide");
                                        },
                                        error: function(data) {
                                            if (typeof data === 'object') {
                                                data = data.responseText;
                                            }

                                            showErrorMessage(data);
                                            $(".search-widget:visible").searchslider("hide");
                                        }
                                    });
                                };

                                var _extractExtension = function(filename) {
                                    var re = /(?:\.([^.]+))?$/;
                                    var ext = re.exec(filename)[1];
                                    if (ext === undefined || ext === null) return "";
                                    else return ext;
                                };

                                var _extensionChanged = function(oldName, newName) {
                                    return _extractExtension(oldName.trim()) !== _extractExtension(newName.trim());
                                };

                                var _containsSpecialCharacters = function() {
                                    var regex = new RegExp("[^A-Za-z0-9_.]");
                                    return regex.test(newName);
                                };

                                var extChanged = _extensionChanged(name, newName);
                                var specialChar = _containsSpecialCharacters(newName);

                                if (extChanged || specialChar) {
                                    var outerDialog = $(this);
                                    var dialogText = "";
                                    if (extChanged) {
                                        dialogText += "The name you have selected changes the file's file extension! " +
                                            "This may break the ability of modules to recognize the file.<br/><br/>";
                                    }
                                    if (specialChar) {
                                        dialogText += "The name you selected contains special characters! " +
                                            "Some older GenePattern modules do not handle special characters well.<br/><br/>";
                                    }

                                    showDialog("Rename Warning!",
                                            dialogText + "Are you sure you want to continue?", {
                                            "Yes": function() {
                                                _doRename();
                                                $(this).dialog("close");
                                                $(outerDialog).dialog("close");
                                            },
                                            "No": function() {
                                                $(this).dialog("close");
                                            }
                                        });
                                }
                                else {
                                    _doRename();
                                    $(this).dialog("close");
                                }
                            },
                            "Cancel": function() {
                                $(this).dialog("close");
                            }
                        });
                        $(".ui-dialog-buttonset:visible button:first").button("disable");
                        $(".dialog-rename").keyup(function(event) {
                            if ($(event.target).val() === "") {
                                $(".ui-dialog-buttonset:visible button:first").button("disable");
                            }
                            else {
                                $(".ui-dialog-buttonset:visible button:first").button("enable");
                            }
                        });
                    }

                    else if (deleteAction) {
                        if (confirm('Are you sure you want to delete the selected file or directory?')) {
                            $.ajax({
                                type: "DELETE",
                                url: "/gp/rest/v1/data/delete/" + path,
                                success: function(data) {
                                    showSuccessMessage(data);

                                    if (isUpload) {
                                        refreshUploadTree();
                                    }
                                    if (isJobFile) {
                                        initRecentJobs();
                                    }
                                },
                                error: function(data) {
                                    if (typeof data === 'object') {
                                        data = data.responseText;
                                    }

                                    showErrorMessage(data);
                                }
                            });

                            $(".search-widget:visible").searchslider("hide");
                        }
                    }

                    else if (subdirAction) {
                        showDialog("Name the Subdirectory", "What name would you like to give the subdirectory?" +
                            "<input type='text' class='dialog-subdirectory-name' style='width: 98%;' />", {
                            "Create": function() {
                                var subdirName = $(".dialog-subdirectory-name").val();

                                var _createSubdirectory = function() {
                                    $.ajax({
                                        type: "PUT",
                                        url: "/gp/rest/v1/data/createDirectory/" + path + encodeURIComponent(subdirName),
                                        success: function(data) {
                                            showSuccessMessage(data);

                                            if (isUpload) {
                                                refreshUploadTree();
                                            }
                                        },
                                        error: function(data) {
                                            if (typeof data === 'object') {
                                                data = data.responseText;
                                            }

                                            showErrorMessage(data);
                                        }
                                    });
                                };

                                // Check for special characters
                                var regex = new RegExp("[^A-Za-z0-9_.]");
                                var specialCharacters = regex.test(subdirName);
                                if(specialCharacters) {
                                    var outerDialog = $(this);
                                    showDialog("Special Characters!",
                                            "The name you selected contains special characters!<br/><br/>" +
                                            "Some older GenePattern modules do not handle special characters well. " +
                                            "Are you sure you want to continue?", {
                                            "Yes": function() {
                                                _createSubdirectory();
                                                $(this).dialog("close");
                                                $(outerDialog).dialog("close");
                                            },
                                            "No": function() {
                                                $(this).dialog("close");
                                            }
                                        });
                                }
                                else {
                                    _createSubdirectory();
                                    $(this).dialog("close");
                                }
                            },
                            "Cancel": function() {
                                $(this).dialog("close");
                            }
                        });
                        $(".ui-dialog-buttonset:visible button:first").button("disable");
                        $(".dialog-subdirectory-name").keyup(function(event) {
                            if ($(event.target).val() === "") {
                                $(".ui-dialog-buttonset:visible button:first").button("disable");
                            }
                            else {
                                $(".ui-dialog-buttonset:visible button:first").button("enable");
                            }
                        });

                        $(".search-widget:visible").searchslider("hide");
                    }

                    else if (uploadAction) {
                        var directory = $(event.target).closest(".file-widget").attr("name");

                        var dropzoneInput = $("#upload-dropzone-input");
                        dropzoneInput.data("origin", directory);
                        dropzoneInput.trigger("click");
                    }

                    else if (pipelineAction) {
                        showDialog("Name the Pipeline", "What name would you like to give the pipeline?" +
                            "<input type='text' class='dialog-pipeline-name' style='width: 98%;' />", {
                            "Create": function() {
                                var subdirName = $(".dialog-pipeline-name").val();
                                subdirName = makePipelineNameValid(subdirName);

                                $.ajax({
                                    type: "PUT",
                                    url: "/gp/rest/v1/data/createPipeline/" + path + "?name=" + subdirName,
                                    success: function(data, textStatus, jqXHR) {
                                        showSuccessMessage(data);

                                        var forwardUrl = jqXHR.getResponseHeader("pipeline-forward");
                                        if (forwardUrl && forwardUrl.length > 0) {
                                            window.location = forwardUrl;
                                        }
                                    },
                                    error: function(data) {
                                        if (typeof data === 'object') {
                                            data = data.responseText;
                                        }

                                        showErrorMessage(data);
                                    }
                                });

                                $(this).dialog("close");
                            },
                            "Cancel": function() {
                                $(this).dialog("close");
                            }
                        });
                        $(".ui-dialog-buttonset:visible button:first").button("disable");
                        $(".dialog-pipeline-name").keyup(function(event) {
                            if ($(event.target).val() === "") {
                                $(".ui-dialog-buttonset:visible button:first").button("disable");
                            }
                            else {
                                $(".ui-dialog-buttonset:visible button:first").button("enable");
                            }
                        });

                        $(".search-widget:visible").searchslider("hide");
                    }

                    else if (genomeSpaceAction) {
                        fileURL = url;							// Set the URL of the file

                        openSaveDialog();

                        $(".search-widget:visible").searchslider("hide");
                    }

                    else {
                        console.log("ERROR: Executing click function for " + url);
                        $(".search-widget:visible").searchslider("hide");
                    }
                }
            });

        var paramList = $("<div></div>")
            .attr("class", "send-to-param-list")
            .attr("data-kind", kind)
            .attr("data-url", link.attr("href"))
            .modulelist({
                title: "Send to Parameter",
                data: [],
                droppable: false,
                draggable: false,
                click: function(event) {}
            });

        var moduleList = $("<div></div>")
            .attr("class", "send-to-module-list")
            .modulelist({
                title: "Send to Module",
                data: sendToList,
                droppable: false,
                draggable: true,
                click: function(event) {
                    var lsid = this.data.lsid;
                    var listObject = $(event.target).closest(".search-widget").find(".send-to-param-list");
                    var kind = listObject.attr("data-kind");
                    var url = listObject.attr("data-url");

                    loadRunTaskForm(lsid, false, kind, url);

                    var checkForRunTaskLoaded = function() {
                        if (run_task_info.lsid === lsid) {
                            sendToByKind(url, kind);
                        }
                        else {
                            setTimeout(function() {
                                checkForRunTaskLoaded();
                            }, 100);
                        }
                    };

                    checkForRunTaskLoaded();
                }
            });

        if (moduleList.find(".module-listing").length < 1) {
            paramList.hide();
            moduleList.hide();
        }

        if (isPartialFile) {
            moduleList.hide();
        }

        var widget = $("<div></div>")
            .attr("name", link.attr("href"))
            .attr("class", "search-widget file-widget")
            .searchslider({
                lists: [actionList, paramList, moduleList]});

        $(appendTo).append(widget);

        // Init the initial send to parameters
        var sendToParamList = widget.find(".send-to-param-list");
        sendToParamForMenu(sendToParamList);
    };

    if (all_modules_map !== null) {
        _createFileWidgetInner(linkElement, appendTo);
    }
    else {
        setTimeout(function() {
            createFileWidget(linkElement, appendTo);
        }, 100);
    }
}

function isDirectoryFromUrl(url) {
    return url[url.length-1] === '/';
}

function isRootFromUrl(url) {
    var parts = url.split("/gp/");
    var path = parts[parts.length-1];
    var pieces = path.split("/");
    return pieces.length <= 3;
}

function uploadPathFromUrl(url) {
    var fullPath = $("<a>").attr("href", url)[0].pathname;
    fullPath = fullPath.substring(3); // Remove the /gp

    return fullPath;
}

function makePipelineNameValid(string) {
    var newName = string.replace(/[^a-zA-Z _0-9.]+/g, "");
    newName = newName.replace(/ /g, ".");
    if (/^\d+/.test(newName)) {
        newName = "Pipeline." + newName;
    }
    if (/^\.+/.test(newName)) {
        newName = "Pipeline" + newName;
    }
    if (newName == "") {
        newName = "Pipeline" + newName;
    }
    return newName;
}

function openFileWidget(link, context) {
    var url = $(link).attr("href");
    var genomeSpace = context === "#menus-genomespace";
    var inputFile = context === "#menus-inputs";

    // Create the menu widget
    var widgetFound = $(context).find("[name='" + escapeJquerySelector(url) + "']").length > 0;
    if (!widgetFound && genomeSpace) {
        createGenomeSpaceWidget($(link), context);
    }
    else if (!widgetFound && inputFile) {
        createInputFileWidget($(link), context);
    }
    else if (!widgetFound) {
        createFileWidget($(link), context);
    }

    // Open the file slider
    $(context).find("[name='" + escapeJquerySelector(url) + "']").searchslider("show");
}

// Possible statuses: Pending, Processing, Finished, Error
function createJobStatus(status) {
    if (!status) {
        console.log("status is not set");
        return $("<span></span>")
    }
    // Pending
    if (status.isPending) {
        return $("<div></div>")
            .text("Pending")
            .addClass("job-status-icon job-status-pending");
    }
    // Processing
    else if (!status.isFinished) {
        return $("<img/>")
            .attr("src", "/gp/images/run.gif")
            .addClass("job-status-icon job-status-processing");
    }
    // Finished and Error
    else if (status.hasError) {
        return $("<img/>")
            .attr("src", "/gp/images/error.gif")
            .addClass("job-status-icon job-status-error");
    }
    // must be Finished and Success
    else {
        return $("<img/>")
            .attr("src", "/gp/images/complete.gif")
            .addClass("job-status-icon job-status-finished");
    }
}

function createJobWidget(job) {
    var actionData = [];
    actionData.push({
        "lsid": "",
        "name": "Job Status",
        "description": "View the job status page for this job.",
        "version": "<span class='glyphicon glyphicon-info-sign' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
    });

    if (job.status.isFinished) {
        actionData.push({
            "lsid": "",
            "name": "Download Job",
            "description": "Download a copy of this job, including all input and result files.",
            "version": "<span class='glyphicon glyphicon-download' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
        });
    }

    actionData.push({
        "lsid": "",
        "name": "Reload Job",
        "description": "Reload this job using the same input parameters.",
        "version": "<span class='glyphicon glyphicon-repeat' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
    });

    if (job.status.isFinished) {
        actionData.push({
            "lsid": "",
            "name": "Delete Job",
            "description": "Delete this job from the GenePattern server.",
            "version": "<span class='glyphicon glyphicon-remove' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
        });
    }

    if (!job.status.isFinished) {
        actionData.push({
            "lsid": "",
            "name": "Terminate Job",
            "description": "Terminate this job on the GenePattern server.",
            "version": "<span class='glyphicon glyphicon-off' ></span>", "documentation": "", "categories": [], "suites": [], "tags": []
        });
    }

    var actionList = $("<div></div>")
        .attr("class", "job-widget-actions")
        .modulelist({
            title: job.taskName + " (" + job.jobId + ")",
            data: actionData,
            droppable: false,
            draggable: false,
            click: function(event) {
                var statusAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Job Status") == 0;
                var downloadAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Download") == 0;
                var reloadAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Reload") == 0;
                var deleteAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Delete") == 0;
                var terminateAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("Terminate") == 0;

                var listObject = $(event.target).closest(".search-widget").find(".send-to-param-list");
                var url = listObject.attr("data-url");

                if (statusAction) {
                    loadJobStatus(job.jobId);
                }

                else if (downloadAction) {
                    $(location).attr('href', '/gp/rest/v1/jobs/' + job.jobId + '/download');

                    $(".search-widget:visible").searchslider("hide");
                }

                else if (reloadAction) {
                    $(location).attr('href', '/gp/pages/index.jsf?lsid' + job.taskLsid + "&reloadJob=" + job.jobId);

                    $(".search-widget:visible").searchslider("hide");
                }

                else if (deleteAction) {
                    if (confirm('Are you sure you want to delete the selected job?')) {
                        $.ajax({
                            type: "DELETE",
                            url: "/gp/rest/v1/jobs/" + job.jobId + "/delete",
                            success: function(data) {
                                showSuccessMessage(data);
                                initRecentJobs();
                            },
                            error: function(data) {
                                if (typeof data === 'object') {
                                    data = data.responseText;
                                }

                                showErrorMessage(data);
                            }
                        });
                    }
                    $(".search-widget:visible").searchslider("hide");
                }

                else if (terminateAction) {
                    $.ajax({
                        type: "DELETE",
                        url: "/gp/rest/v1/jobs/" + job.jobId + "/terminate",
                        success: function(data) {
                            showSuccessMessage(data);
                            initRecentJobs();
                        },
                        error: function(data) {
                            if (typeof data === 'object') {
                                data = data.responseText;
                            }

                            showErrorMessage(data);
                        }
                    });

                    $(".search-widget:visible").searchslider("hide");
                }

                else {
                    console.log("ERROR: Executing click function for Job " + job.jobId);
                    $(".search-widget:visible").searchslider("hide");
                }
            }
        });

    var codeData = [
        {
            "lsid": "",
            "name": "View Java Code",
            "description": "View the code for referencing this job programmatically from Java.",
            "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
        },
        {
            "lsid": "",
            "name": "View MATLAB Code",
            "description": "View the code for referencing this job programmatically from MATLAB.",
            "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
        },
        {
            "lsid": "",
            "name": "View R Code",
            "description": "View the code for referencing this job programmatically from R.",
            "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
        }
    ];

    var codeList = $("<div></div>")
        .attr("class", "job-widget-code")
        .modulelist({
            title: "View Code",
            data: codeData,
            droppable: false,
            draggable: false,
            click: function(event) {
                var javaAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("View Java") == 0;
                var matlabAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("View MATLAB") == 0;
                var rAction = $(event.target).closest(".module-listing").find(".module-name").text().trim().indexOf("View R") == 0;

                if (javaAction) {
                    window.open("/gp/rest/v1/jobs/" + job.jobId + "/code?language=Java");
                    $(".search-widget:visible").searchslider("hide");
                }

                else if (matlabAction) {
                    window.open("/gp/rest/v1/jobs/" + job.jobId + "/code?language=MATLAB");
                    $(".search-widget:visible").searchslider("hide");
                }

                else if (rAction) {
                    window.open("/gp/rest/v1/jobs/" + job.jobId + "/code?language=R");
                    $(".search-widget:visible").searchslider("hide");
                }

                else {
                    console.log("ERROR: Executing click function for Job " + job.jobId);
                    $(".search-widget:visible").searchslider("hide");
                }
            }
        });

    var widget = $("<div></div>")
        .attr("name", "job_" + job.jobId)
        .attr("class", "search-widget file-widget")
        .searchslider({
            lists: [actionList, codeList]});

    $("#menus-jobs").append(widget);
}

function initRecentJobs() {
    // Init the browse button
    $("#left-nav-jobs-browse").button().click(function() {
        loadJobResults("userId=" + username);
    });

    // Init the jobs
    $.ajax({
        cache: false,
        type: "GET",
        url: "/gp/rest/v1/jobs/recent",
        dataType: "json",
        success: function(data) {
            // Clear away the old rendering of the tab
            $("#loading-jobs").hide();
            var tab = $("#left-nav-jobs-list");
            tab.empty();

            // Clear away any old jobs menus
            $("#menus-jobs").empty();

            // For each top-level job
            var recentJobs = data.recentJobs;
            for (var i = 0; i < recentJobs.length; i++) {
                var jobJson = recentJobs[i];

                // Protect against null jobs
                if (jobJson === null) {
                    console.log("ERROR rendering job:");
                    console.log(jobJson);
                    continue;
                }

                renderJob(jobJson, tab);
            }

            // Handle the case of no recent jobs
            if (recentJobs.length === 0) {
                tab.append("<h3 style='text-align:center;'>No Recent Jobs</h3>");
            }

            // Update the Job Status Indicators
            var jobsProcessing = data.numProcessingJobs;
            var statusBoxes = $(".current-job-status a");

            statusBoxes.each(function(index, ui) {
                $(ui).empty();
                if (jobsProcessing > 0) {
                    $(ui).text(" " + jobsProcessing + " Jobs Processing");
                    $(ui).prepend("<img src='/gp/images/spin.gif' alt='Jobs Currently Processing' />");
                    $.data($(ui).parent()[0], "continuePolling", true);
                }
                else {
                    $(ui).text(" No Jobs Processing");
                    $(ui).prepend("<img src='/gp/images/complete.gif' alt='No Jobs Processing' />");
                    $.data($(ui).parent()[0], "continuePolling", false);
                }
            });
        },
        error: function(data) {
            if (typeof data === 'object') {
                data = data.responseText;
            }

            showErrorMessage(data);
        }
    });
}

function toggleJobCollapse(toggleImg) {
    $(toggleImg).closest(".job-box").find(".job-details").toggle("blind");
    var open = $(toggleImg).attr("src").indexOf("arrow-pipelinetask-down.gif") >= 0;
    if (open) {
        $(toggleImg).attr("src", "/gp/images/arrow-pipelinetask-right.gif")
    }
    else {
        $(toggleImg).attr("src", "/gp/images/arrow-pipelinetask-down.gif")
    }
}

function renderJob(jobJson, tab) {
    var jobBox = $("<div></div>")
        .addClass("job-box")
        .appendTo(tab);

    var jobName = $("<div></div>")
        .addClass("job-name")
        .appendTo(jobBox);

    $("<img />")
        .attr("src", "/gp/images/arrow-pipelinetask-down.gif")
        .attr("onclick", "toggleJobCollapse(this);")
        .appendTo(jobName);

    $("<a></a>")
        .attr("href", "/gp/jobResults/" + jobJson.jobId + "/")
        .attr("onclick", "openJobWidget(this); return false;")
        .attr("data-jobid", jobJson.jobId)
        .attr("data-json", JSON.stringify(jobJson))
        .text(jobJson.jobId + ". " + jobJson.taskName + " ")
        .append(
        $("<span></span>")
            .attr("class", "glyphicon glyphicon-info-sign")
            .css("color", "darkgray")
    )
        .appendTo(jobName);

    var jobDetails = $("<div></div>")
        .addClass("job-details")
        .append(
        $("<div></div>")
            .addClass("job-status")
            .append(createJobStatus(jobJson.status))
    )
        .append(jobJson.datetime)
        .appendTo(jobBox);

    //noinspection JSDuplicatedDeclaration
    for (var j = 0; j < jobJson.outputFiles.length; j++) {
        var file = jobJson.outputFiles[j];

        var fileBox = $("<div></div>")
            .addClass("job-file")
            .appendTo(jobDetails);

        $("<a></a>")
            .attr("href", file.link.href)
            .attr("onclick", "openFileWidget(this, '#menus-jobs'); return false;")
            .attr("href", file.link.href)
            .attr("data-kind", file.kind)
            .attr("data-sendtomodule", JSON.stringify(file.sendTo))
            .append(
            $("<img />")
                .attr("src", "/gp/images/outputFile.gif"))
            .append(file.link.name)
            .appendTo(fileBox);
    }

    // Handle child jobs
    if (jobJson.children) {
        //noinspection JSDuplicatedDeclaration
        for (var j = 0; j < jobJson.children.items.length; j++) {
            var child = jobJson.children.items[j];
            renderJob(child, jobDetails);
        }
    }
}

function openJobWidget(link) {
    var id = $(link).attr("data-jobid");

    // Create the job widget
    var menuJobs = $("#menus-jobs");
    var widgetFound = menuJobs.find("[name='job_" + id + "']").length > 0;
    if (!widgetFound) {
        // Get the job JSON
        var jobJson = $(link).data("json");

        createJobWidget(jobJson);
    }

    // Open the job slider
    menuJobs.find("[name='job_" + id + "']").searchslider("show");
}

function getURLParameter(sParam) {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++) {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) {
            return sParameterName[1];
        }
    }
    return null;
}

function loadJobStatus(jobId, forceVisualizers) {
    // Abort if no job to load
    if (jobId === undefined || jobId === null || jobId === '') {
        return;
    }

    // Hide the search slider if it is open
    $(".search-widget").searchslider("hide");

    // Hide the protocols, run task form & eula, if visible
    $("#protocols").hide();
    var submitJob = $("#submitJob").hide();
    $("#eula-block").hide();

    // Clean the Run Task Form for future loads
    if (Request.cleanJobSubmit === null) { Request.cleanJobSubmit = submitJob.clone(); }
    else { submitJob.replaceWith(Request.cleanJobSubmit.clone()); }
    run_task_info = {
        lsid: null, //lsid of the module
        name: null, //name of the module
        params: {}, //contains parameter info necessary to build the job submit form, see the initParam() function for details
        sendTo: {},
        param_group_ids: {}
    };
    parameter_and_val_groups = {}; //contains params and their values only

    // Handle open visualizer flag
    var openVisualizers;
    if (forceVisualizers !== undefined) {
        openVisualizers = forceVisualizers
    }
    else {
        openVisualizers = getURLParameter("openVisualizers");
    }
    if (openVisualizers) {
        openVisualizers = "&openVisualizers=true";
    }
    else {
        openVisualizers = "&openVisualizers=false";
    }

    // Hide the send to parameter list
    $(".send-to-param-list").hide();

    // Add to history so back button works
    var visualizerAppend = "";
    if (getURLParameter("openVisualizers")) {
        visualizerAppend = openVisualizers;
    }
    history.pushState(null, document.title, location.protocol + "//" + location.host + location.pathname + "?jobid=" + jobId + visualizerAppend);

    $.ajax({
        type: "GET",
        url: "/gp/pages/jobResult.jsf?jobNumber=" + jobId + openVisualizers,
        cache: false,
        success: function(data) {
            var jobResults = $("#jobResults");
            jobResults.html(data);
            jobResults.show();
        },
        error: function(data) {
            if (typeof data === 'object') {
                data = data.responseText;
            }

            showErrorMessage(data);
        },
        dataType: "html"
    });
}

function getJobFilter() {
    return $.cookie('job-filter');
}

function setJobFilter(filter) {
    $.cookie('job-filter', filter, { path: '/'});
}

function populateJobResultsTable(settings, callback) {
    // Helper functions
    var _statusToImg = function(status) {
        if (status.isPending) return $("<span ></span>").text("Pending");                                 // Pending
        else if (status.hasError) return $("<img />").attr("src", "/gp/images/error.gif");          // Error
        else if (status.isFinished) return $("<img />").attr("src", "/gp/images/complete.gif");     // Finished
        else return $("<img />").attr("src", "/gp/images/run.gif");                                 // Running

    };
    var _formatDate = function(dateString) {
        var date = new Date(dateString);
        // Special case for Safari
        if (!(date instanceof Date && isFinite(date))) {
            return dateString;
        }
        var month = $.datepicker.formatDate("M", date);
        var day = $.datepicker.formatDate("d", date);
        var hours = date.getHours() > 12 ? date.getHours() - 12 : (date.getHours() < 1 ? 12 : date.getHours());
        var minutes = date.getMinutes() < 10 ? "0" + date.getMinutes() : date.getMinutes();
        var time = hours + ":" + minutes + " " + (date.getHours() <= 11 ? "am" : "pm");
        return month + " " + day + ", " + time;
    };
    var _buildChildJobDisplay = function(job, count, appendTo) {
        var child = $("<div></div>").addClass("jobresults-child");
        $("<img />")
            .attr("src", "/gp/images/arrow2.gif")
            .appendTo(child);
        $("<a></a>")
            .attr("href", "/gp/jobResults/" + job.jobId + "/")
            .attr("onclick", "openJobWidget(this); return false;")
            .attr("data-jobid", job.jobId)
            .attr("data-json", JSON.stringify(job))
            .append(count + ". " + job.taskName + " ")
            .append(
            $("<span></span>")
                .attr("class", "glyphicon glyphicon-info-sign")
        )
            .appendTo(child);
        $("<br/>").appendTo(child);

        // Build the job results
        var results = $("<div></div>").addClass("jobresults-files");
        for (var j = 0; j < job.outputFiles.length; j++) {
            var file = job.outputFiles[j];
            $("<img />")
                .attr("src", "/gp/images/outputFile.gif")
                .appendTo(results);
            $("<a></a>")
                .attr("href", file.link.href)
                .attr("onclick", "openFileWidget(this, '#menus-jobs'); return false;")
                .attr("data-kind", file.kind)
                .append(file.link.name)
                .appendTo(results);
            $("<br/>").appendTo(results);
        }
        results.appendTo(child);

        child.appendTo(appendTo);
    };
    var _buildStatus = function(job) {
        var status = _statusToImg(job.status);
        return status[0].outerHTML;
    };
    var _buildJobId = function(job) {
        var id = $("<a></a>")
            .attr("href", "#")
            .attr("onclick", "loadJobStatus(" + job.jobId + "); return false;")
            .text(job.jobId);
        return id[0].outerHTML;
    };
    var _buildDelete = function(job) {
        var del = $("<input />")
            .addClass("job-delete-checkbox")
            .attr("name", job.jobId)
            .attr("type", "checkbox")
            .attr("value", job.jobId);
        return del[0].outerHTML;
    };
    var _buildResultFiles = function(job) {
        // Build the job results
        var results = $("<div></div>").addClass("jobresults-files");
        for (var j = 0; j < job.outputFiles.length; j++) {
            var file = job.outputFiles[j];
            $("<img />")
                .attr("src", "/gp/images/outputFile.gif")
                .appendTo(results);
            $("<a></a>")
                .attr("href", file.link.href)
                .attr("onclick", "openFileWidget(this, '#menus-jobs'); return false;")
                .attr("data-kind", file.kind)
                .append(file.link.name)
                .appendTo(results);
            $("<br/>").appendTo(results);
        }
//        if (job.status.executionLogLocation) {
//            $("<img />")
//                .attr("src", "/gp/images/outputFile.gif")
//                .appendTo(results);
//            $("<a></a>")
//                .attr("href", job.status.executionLogLocation)
//                .attr("onclick", "openFileWidget(this, '#menus-jobs'); return false;")
//                .attr("data-kind", "txt")
//                .append("Execution Log")
//                .appendTo(results);
//            $("<br/>").appendTo(results);
//        }
        return results;
    };
    var _buildChildJobs = function(job) {
        // Build the child jobs
        var children = $("<div></div>").addClass("jobresults-children");
        if (job.children) {
            for (var j = 0; j < job.children.items.length; j++) {
                var child = job.children.items[j];
                _buildChildJobDisplay(child, j + 1, children);
            }
        }
        return children;
    };
    var _buildMain = function(job) {
        var results = _buildResultFiles(job);
        var children = _buildChildJobs(job);

        var wrapper = $("<div></div>")
            .css("overflow", "hidden")
            .append(
            $("<a></a>")
                .addClass("job-task-toggle")
                .attr("href", "#")
                .attr("onclick", "return false;")
                .append(
                $("<img />")
                    .addClass("jobresults-toggle")
                    .attr("src", "/gp/images/triangle_black_run.gif")
            )
        )
            .append(
            $("<a></a>")
                .addClass("job-task")
                .attr("href", "/gp/jobResults/" + job.jobId + "/")
                .attr("onclick", "openJobWidget(this); return false;")
                .attr("data-jobid", job.jobId)
                .attr("data-json", JSON.stringify(job))
                .text(" " + job.taskName + " ")
                .append(
                $("<span></span>")
                    .attr("class", "glyphicon glyphicon-info-sign")
            )
        )
            .append(results)
            .append(children);
        return wrapper[0].outerHTML;
    };
    var _sizeOfJob = function(job) {
        var total = 0;
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < job.outputFiles.length; i++) {
            var file = job.outputFiles[i];
            total += file.fileLength;
        }
        if (job.children) {
            //noinspection JSDuplicatedDeclaration
            for (var i = 0; i < job.children.items.length; i++) {
                var child = job.children.items[i];
                total += _sizeOfJob(child);
            }
        }
        return total;
    };
    var _buildSize = function(job) {
        var total = _sizeOfJob(job);

        return _formatFileSize(total);
    };
    var _buildSubmission = function(job) {
        return _formatDate(job.dateSubmitted);
    };
    var _buildCompletion = function(job) {
        if (job.dateCompleted) {
            return _formatDate(job.dateCompleted);
        }
        else {
            return "";
        }
    };
    var _buildOwner = function(job) {
        return job.userId;
    };
    var _buildSharing = function(job) {
        return job.permissions.isPublic ? "Public" : (job.permissions.isShared ? "Shared" : "Private");
    };
    var _attachMetadata = function(toReturn, data) {
        toReturn.draw = settings.draw;
        toReturn.recordsTotal = data.nav.numItems;
        toReturn.recordsFiltered = data.nav.numItems;
    };
    var _attachRows = function(toReturn, data) {
        var rows = [];

        // Build the table rows
        for (var i = 0; i < data.items.length; i++) {
            var job = data.items[i];
            var row = [];

            // Append the cells
            row.push(_buildStatus(job));
            row.push(_buildJobId(job));
            row.push(_buildDelete(job));
            row.push(_buildMain(job));
            row.push(_buildSize(job));
            row.push(_buildSubmission(job));
            row.push(_buildCompletion(job));
            row.push(_buildOwner(job));
            row.push(_buildSharing(job));

            // Attach row to the list
            rows.push(row);
        }

        toReturn.data = rows;
    };
    var _columnToName = function(col) {
        if (col === 0) return "status";              // Status
        if (col === 1) return "jobId";               // Job ID
        if (col === 3) return "taskName";            // Task
        if (col === 5) return "dateSubmitted";       // Submitted
        if (col === 6) return "dateCompleted";       // Completed
        else {
            console.log("Error: Unknown Job Result Sort");
            return "jobId";
        }
    };
    var _buildRESTUrl = function() {
        var pageSize = settings.length;
        var page = Math.floor(settings.start / pageSize) + 1;
        var filter = getJobFilter();

        // Handle user search
        if (settings.search && settings.search.value) {
            filter = "userId=" + settings.search.value;
        }

        var sort = _columnToName(settings.order[0].column);
        if (settings.order[0].dir === "desc") sort = "-" + sort;
        return "/gp/rest/v1/jobs/?pageSize=" + pageSize + "&page=" + page + "&orderBy=" + sort + "&" + filter;
    };
    var _formatFileSize = function(bytes) {
        var thresh = 1024;
        if(bytes < thresh) return bytes + ' B';
        var units = ['kB','MB','GB','TB','PB','EB','ZB','YB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while(bytes >= thresh);
        return bytes.toFixed(1)+' '+units[u];
    };

    // Make the actual ajax request
    $.ajax({
        type: "GET",
        url: _buildRESTUrl(),
        cache: false,
        dataType: "json",
        success: function(data) {
            // The object to send to the callback in the format expected
            var toReturn = {};

            _attachMetadata(toReturn, data);
            _attachRows(toReturn, data);

            callback(toReturn);

            // Show the table
            $("#jobResults").show();

            // Attach the job show/hide toggle
            $(".job-task-toggle").click(function() {
                var toggleImage = $(this).parent().find(".jobresults-toggle");
                var open = toggleImage.attr("src").indexOf("_run") != -1;
                if (open) {
                    toggleImage.attr("src", "/gp/images/triangle_black.gif");
                    $(this).parent().find(".jobresults-files, .jobresults-child").hide();
                }
                else {
                    toggleImage.attr("src", "/gp/images/triangle_black_run.gif");
                    $(this).parent().find(".jobresults-files, .jobresults-child").show();
                }
            });

            // Set the job results filter options
            var jobResultsFilter = $("#jobresults-filter");
            jobResultsFilter.empty();
            var currentFilter = getJobFilter();
            jobResultsFilter.append(
                $("<option></option>")
                    .val("userId=" + username)
                    .text("My Job Results")
            );
            jobResultsFilter.append(
                $("<option></option>")
                    .val("userId=*")
                    .text("All Job Results")
            );
            if (data.nav.groupIds) {
                //noinspection JSDuplicatedDeclaration
                for (var i = 0; i < data.nav.groupIds.length; i++) {
                    //noinspection JSDuplicatedDeclaration
                    var filter = data.nav.groupIds[i];
                    jobResultsFilter.append(
                        $("<option></option>")
                            .val("groupId=" + filter)
                            .text("Group: " + filter)
                    );
                }
            }
            if (data.nav.batchIds) {
                //noinspection JSDuplicatedDeclaration
                for (var i = 0; i < data.nav.batchIds.length; i++) {
                    //noinspection JSDuplicatedDeclaration
                    var filter = data.nav.batchIds[i];
                    jobResultsFilter.append(
                        $("<option></option>")
                            .val("batchId=" + filter)
                            .text("Batch: " + filter)
                    );
                }
            }
            jobResultsFilter.find("option").each(function() {
                if ($(this).val() === currentFilter) {
                    $(this).attr("selected", "selected");
                }
            });

            // Open or collapse as shown
            var toggleImage = $(".jobresults-all-toggle");
            if (toggleImage.length > 0) {
                var open = toggleImage.attr("src").indexOf("_run") != -1;
                if (!open) {
                    toggleImage.trigger("click");
                    toggleImage.trigger("click");
                }
            }

        },
        error: function(data, textStatus) {
            showErrorMessage(textStatus);
        }
    });
}

function buildJobResultsPage() {
    // Clear the div
    var jobResults = $("#jobResults").empty();

    // Build the container
    var container = $("<div ></div>")
        .addClass("statusReport")
        .addClass("jobResults");

    // Build the header
    $("<div ></div>")
        .addClass("title")
        .text("Job Results")
        .append(
        $("<div ></div>")
            .addClass("jobresults-navigation")
            .text("Show: ")
            .append(
            $("<select></select>")
                .attr("id", "jobresults-filter")
                .attr("name", "show")
                .change(function() {
                    var filter = $(this).val();
                    setJobFilter(filter);
                    loadJobResults(filter);
                })
        )
    )
        .appendTo(container);

    // Build the table
    var jobTable = $("<table></table>")
        .attr("id", "jobTable")
        .addClass("summary stripe")
        .append(
        $("<thead></thead>")
            .append(
            $("<tr></tr>")
                .addClass("summaryTitle")
                .append(
                $("<td></td>")
                    .addClass("header-sm")
                    .text("Status")
            )
                .append(
                $("<td></td>")
                    .addClass("header-sm")
                    .text("Job")
            )
                .append(
                $("<td></td>")
                    .addClass("header-sm")
                    .append(
                    $("<a></a>")
                        .addClass("delete-job-action")
                        .attr("href", "#")
                        .text("Delete")
                        .click(function() {
                            // Gather the jobs to delete
                            var jobsDelete = [];
                            $(".job-delete-checkbox:checked").each(function(index, element) {
                                var id = $(element).val();
                                jobsDelete.push(id);
                            });

                            if (confirm("Are you sure you want to delete these " + jobsDelete.length + " jobs?")) {
                                // Make the Delete AJAX call
                                $.ajax({
                                    type: "DELETE",
                                    url: "/gp/rest/v1/jobs/delete?jobs=" + jobsDelete.join(","),
                                    cache: false,
                                    dataType: "text",
                                    success: function(data) {
                                        var filter = getJobFilter();
                                        if (!filter) filter = true;
                                        loadJobResults(filter);
                                        showSuccessMessage(data);
                                    },
                                    error: function(data) {
                                        var filter = getJobFilter();
                                        if (!filter) filter = true;
                                        loadJobResults(filter);
                                        showErrorMessage(data);
                                    }
                                });
                            }
                        })
                )
                    .append(
                    $("<input />")
                        .addClass("job-delete-checkbox-master")
                        .attr("type", "checkbox")
                        .click(function() {
                            var isChecked = $(".job-delete-checkbox-master").prop('checked');
                            $(".job-delete-checkbox").prop('checked', isChecked);
                        })
                )
            )
                .append(
                $("<td></td>")
                    .css("min-width", "255px")
                    .append(
                    $("<a></a>")
                        .addClass("job-all-toggle")
                        .attr("href", "#")
                        .click(function(event) {
                            var toggleImage = $(".jobresults-all-toggle");
                            var open = toggleImage.attr("src").indexOf("_run") != -1;
                            if (open) {
                                toggleImage.attr("src", "/gp/images/triangle_black.gif");
                            }
                            else {
                                toggleImage.attr("src", "/gp/images/triangle_black_run.gif");
                            }

                            $(".jobresults-toggle").each(function() {
                                var thisOpen = $(this).attr("src").indexOf("_run") != -1;

                                if (open && thisOpen) {                   // We're closing them all and this is open
                                    $(this).trigger("click");
                                }
                                else if (!open && !thisOpen) {              // We're opening them all and this is closed
                                    $(this).trigger("click");
                                }
                            });

                            if (event.preventDefault) event.preventDefault();
                            if (event.stopPropagation) event.stopPropagation();
                        })
                        .append(
                        $("<img />")
                            .addClass("jobresults-all-toggle")
                            .attr("src", "/gp/images/triangle_black_run.gif")
                    )
                )
                    .append("Module")
            )
                .append(
                $("<td></td>")
                    .addClass("header-sm")
                    .text("Size")
            )
                .append(
                $("<td></td>")
                    .css("min-width", "100px")
                    .text("Submission Date")
            )
                .append(
                $("<td></td>")
                    .css("min-width", "100px")
                    .text("Completion Date")
            )
                .append(
                $("<td></td>")
                    .addClass("header-md")
                    .text("Owner")
            )
                .append(
                $("<td></td>")
                    .addClass("header-sm")
                    .text("Sharing")
            )
        )
    )
        .appendTo(container);

    // Build the table body
    var tbody = $("<tbody></tbody>")
        .appendTo(jobTable);

    // Init data tables
    jobTable.dataTable({
        serverSide: true,
        "ajax": function(data, callback) {
            populateJobResultsTable(data, callback);
        },
        "order": [[1, "desc"]],
        "columnDefs": [
            { "orderable": false, "targets": [2, 4, 7, 8] }
        ],
        "searching": adminServerAllowed,
        "oLanguage": {
            "sSearch": "Owner Search: "
        },
        "lengthMenu": [10, 20, 50, 100],
        stateSave: true,
        "sPaginationType": "input",
        "bProcessing": true
    });

    // Append the container to the correct past of the page
    jobResults.append(container);
}

function loadJobResults(jobResults) {
    // Abort if not told to load job results
    if (jobResults === undefined || jobResults === null || jobResults === '' || jobResults === "false" || !jobResults) {
        return;
    }

    // Handle default job results loads
    if (jobResults === 'true' || jobResults === true || jobResults === 'null') {
        jobResults = getJobFilter();
        if (jobResults === null) {
            jobResults = "userId=" + username;
            setJobFilter(jobResults);
        }
    }

    // Set the filter
    setJobFilter(jobResults);

    // Hide the search slider if it is open
    $(".search-widget").searchslider("hide");

    // Hide the protocols, run task form & eula, if visible
    $("#protocols").hide();
    var submitJob = $("#submitJob").hide();
    $("#eula-block").hide();

    // Clean the Run Task Form for future loads
    if (Request.cleanJobSubmit === null) { Request.cleanJobSubmit = submitJob.clone(); }
    else { submitJob.replaceWith(Request.cleanJobSubmit.clone()); }
    run_task_info = {
        lsid: null, //lsid of the module
        name: null, //name of the module
        params: {}, //contains parameter info necessary to build the job submit form, see the initParam() function for details
        sendTo: {},
        param_group_ids: {}
    };
    parameter_and_val_groups = {}; //contains params and their values only

    // Hide the send to parameter list
    $(".send-to-param-list").hide();

    // Add to history so back button works
    history.pushState(null, document.title, location.protocol + "//" + location.host + location.pathname + "?jobResults=" + encodeURIComponent(jobResults));

    // Build the page scaffolding
    buildJobResultsPage();
}