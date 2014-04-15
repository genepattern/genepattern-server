// used to make sure that a jquery id selector is escaped properly
function escapeJquerySelector(str) {
	return str.replace(/([ #;&,.+*~\':"!^$[\]()=>|\/@])/g,'\\$1');
}

// toggleCheckBoxes -- used in combination with a "master" checkbox to toggle
// the state of a collection of child checkboxes.  Assumes the children and parent
// share a common container parent
function toggleCheckBoxes(maincheckbox, parentId) {
	var isChecked = maincheckbox.checked;
	var parentElement = document.getElementById(parentId);
	var elements = parentElement.getElementsByTagName("input");
	for (var i = 0; i < elements.length; i++) {
		if (elements[i].type = "checkbox") {
			elements[i].checked = isChecked;
		}
	}
}

//
// stop a running job by its ID
//
function stopJob(button, jobId) {
	var really = confirm('Really stop this Job?');
	if (!really)
		return;
	window.alert('Job not stopped, stopJob should not be called from this page!');
}

// POST /jobResults/<job>/requestEmailNotification
function requestEmailNotification(cb, jobId, userEmail) {
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
	if (req.readyState == 4) {
		if (req.status >= 200 && req.status < 300) {
			// alert('all is well on email submission')
		} else {
			alert("There was a problem in email notification:\n" + req.status
					+ ' -- ' + req.statusText);
		}
	}
}

// Sends an asychronous request to the managed bean specified by the
// elExpression (e.g. jobsBean.taskCode).
// elExpression The expression.
// parameters The parameters to send to the bean method.
// callbackFunction The function to invoke when a response is received from the
// server.
// method Either post or get.

function sendAjaxRequest(elExpression, parameters, callbackFunction, method,
		ajaxServletUrl) {
	$.ajax({
		type : method,
		url : '/gp/jobResults/' + jobId + '/cancelEmailNotification',
		data : parameters + '&el=' + elExpression,
		dataType : "json",
		success : function(data, textStatus, jqXHR) {
			callbackFunction(jqXHR);
		},
		error : function(data, textStatus, jqXHR) {
			alert('Error ' + jqXHR.status + ' -- ' + jqXHR.statusText);
		}
	});
}

// Gets the form parameters for the form with the specified form id.
// The form parameters as a string.

function getFormParameters(formId) {
	var form = $("#" + formId);
	if (form.length < 1) {
		alert("Form " + formId + " not found.");
	}
	else {
		form = form.get(0)
	}
	var params = "";
	for ( var i = 0; i < form.elements.length; i++) {
		if (i > 0) {
			params += "&";
		}
		var e = form.elements[i];
		var val = e.value;
		if (e.type == 'checkbox') {
			val = e.checked ? "on" : "";
		}
		params += e.name + "=" + val;
	}
	return params;
}

function gup(name) {
	name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
	var regexS = "[\\?&]" + name + "=([^&#]*)";
	var regex = new RegExp(regexS);
	var results = regex.exec(window.location.href);
	if (results == null)
		return "";
	else
		return results[1];
}

// Requires jQuery & jQuery UI
function showDialog(title, message, button) {
	if (typeof jq === 'undefined') {
		var jq = $;
	}

	var alert = document.createElement("div");

	if (typeof (message) == 'string') {
		alert.innerHTML = message;
		;
	} else {
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
	var $ = jq;
}
var all_modules = null;
var all_modules_map = null;
var all_categories = null;
var all_suites = null;

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
		for (var j = 0; j < a.tags.length; j++) {
			var tagObj = a.tags[j];
			if (tagObj.tag == "favorite") {
				a_pos = tagObj.metadata;
			}
		}

		var b_pos = 0;
		for (var j = 0; j < b.tags.length; j++) {
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
            $("#module-search").searchslider("show");
            $("#module-search").searchslider("tagfilter", filter);
            $("#module-search").searchslider("set_title", '<a href="#" onclick="$(\'#module-browse\').searchslider(\'show\');">Browse Modules</a> &raquo; <a href="#" onclick="$(\'#module-suites\').searchslider(\'show\');">Browse Suites</a> &raquo; ' + filter);
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
            $("#module-search").searchslider("show");
            $("#module-search").searchslider("tagfilter", filter);
            $("#module-search").searchslider("set_title", '<a href="#" onclick="$(\'#module-browse\').searchslider(\'show\');">Browse Modules</a> &raquo; ' + filter);
        }
    });
}

function initBrowseTop() {
	var allnsuite = $('<div id="module-list-allnsuite"></div>').modulelist({
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
                $("#module-search").searchslider("show");
                $("#module-search").searchslider("filter", '');
                $("#module-search").searchslider("set_title", '<a href="#" onclick="$(\'#module-browse\').searchslider(\'show\');">Browse Modules</a> &raquo; All Modules');
            }
            else {
            	$("#module-suites").searchslider("show");
            }
        }
    });

	return allnsuite;
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

    var modsearch = $('#module-search').searchslider({
        lists: [search]
    });
}

function initRecent() {
    var still_loading = false;
	var recent_modules = getRecentModules();

	var recent = $('#recent-modules').modulelist({
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
    $('#recent-modules .module-list-empty').text("No Recent Modules");
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

	var pinned = $('#pinned-modules').modulelist({
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
                success: function(data, status, xhr) {
                	console.log("pinned");

                    $(".module-lsid:contains('" + lsid + "')").each(function(index, element) {
                        var module = $(element).parent();
                        module.module("add_tag", "favorite");
                    });
                }
            });

        	// Reinitialize the widget as a module
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
                success: function(data, status, xhr) {
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
                success: function(data, status, xhr) {
                	console.log("repinned");
                }
            });
        }
    });
	pinned.modulelist("tagfilter", "favorite");
    $('#pinned-modules .module-list-empty').text("Drag Modules Here");
}

function setModuleSearchTitle(filter) {
    if (filter === '') {
        $("#module-search").searchslider("set_title", "Search: Modules &amp; Pipelines");
    }
    else {
        $("#module-search").searchslider("set_title", "Search: " + filter);
    }
}

function jobStatusPoll() {
	var continuePolling = $.data($(".current-job-status")[0], "continuePolling");
	if (continuePolling == undefined || continuePolling) {
 		$.ajax({
            cache: false,
            url: '/gp/rest/v1/jobs/incomplete',
            dataType: 'json',
            success: function(data, status, xhr) {
                var statusBoxes = $(".current-job-status a");

                statusBoxes.each(function(index, ui) {
                    $(ui).empty();
                    if (data.length > 0) {
                        $(ui).text(" " + data.length + " Jobs Processing");
                        $(ui).prepend("<img src='/gp/images/spin.gif' alt='Jobs Currently Processing' />");
                        $.data(ui, "continuePolling", true);
                    }
                    else {
                        $(ui).text(" No Jobs Processing");
                        $(ui).prepend("<img src='/gp/images/complete.gif' alt='No Jobs Processing' />");
                        $.data(ui, "continuePolling", false);
                    }
                });
            }
        });
		}
}

function ajaxFileTabUpload(file, directory, done, index){
    var loaded = 0;
    var step = 1024*1024;
    var total = file.size;
    var start = 0;
    var partitionIndex = 0;
    var partitionCount = Math.ceil(total / step);

    var reader = new FileReader();
    var xhr = null;
    var xhrCanceled = false;

    var progressbar = $(".upload-toaster-file[name='" + escapeJquerySelector(file.name) + "']").find(".upload-toaster-file-progress");

    // Set the cancel button functionality
    var cancelButton = $(".upload-toaster-file[name='" + escapeJquerySelector(file.name) + "']").find(".upload-toaster-file-cancel");
    cancelButton.click(function() {
        //var xhr = progressbar.data("xhr");
        xhr.abort();

        // Set the progressbar cancel message
        progressbar.progressbar("value", 100);
        progressbar
            .find(".ui-progressbar-value")
            .css("background", "#FCF1F3");
        progressbar
            .find(".upload-toaster-file-progress-label")
            .text("Canceled!");

        // Mark this upload as done
        xhrCanceled = true;
        done[index] = true;
    });

    // Handle the directory error condition
    reader.onerror = function(event) {
        // Set the top error message
        var message = "Uploading directories is not supported. Aborting upload.";
        showErrorMessage(message);

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
    };

    reader.onload = function(event) {
        // Double check for canceling
        if (xhrCanceled) {
            xhr.abort();
            return;
        }

        loaded += event.loaded;
        xhr = new XMLHttpRequest();
        progressbar.data("xhr", xhr);

        var upload = xhr.upload;

        upload.addEventListener('load',function(){
            setTimeout(function() {
                var data = xhr.response;

                if (data.match("^Error:")) {
                    xhr.abort();

                    // Set the top error message
                    showErrorMessage(data);

                    // Set the progressbar error message
                    progressbar.progressbar("value", 100);
                    progressbar
                        .find(".ui-progressbar-value")
                        .css("background", "#FCF1F3");
                    progressbar
                        .find(".upload-toaster-file-progress-label")
                        .text("Error!");

                    // Restore the dialog if necessary
                    if ($("#dialog-extend-fixed-container").find(".upload-dialog").length > 0) {
                        $(".upload-dialog").css("z-index", "9000");
                        $(".upload-toaster-list").dialogExtend("restore");
                    }

                    // Mark this upload as done
                    done[index] = true;

                    return;
                }

                if (loaded < total) {
                    blob = file.slice(loaded, loaded + step + 1);
                    reader.readAsArrayBuffer(blob);
                }
                else {
                    loaded = total;
                }

                var progress = Math.min(Math.round((loaded/total) * 100), 100);
                progressbar.progressbar("value", progress);

                if (loaded === total) {
                    if (!data.match("^Error:")) {
                        done[index] = true;
                    }
                }
            }, 10);
        }, false);

        xhr.open("POST", "/gp/AJAXUpload?fileName=" + file.name);
        xhr.overrideMimeType("application/octet-stream");
        xhr.setRequestHeader('partitionCount', partitionCount.toString());
        xhr.setRequestHeader('partitionIndex', partitionIndex.toString());
        xhr.setRequestHeader('filename', file.name);
        xhr.setRequestHeader('uploadPath', directory);
        //xhr.sendAsBinary(event.target.result);
        xhr.send(event.target.result);

        partitionIndex++;

        // Special case for empty files
        if (partitionCount === 0) {
            progressbar.progressbar("value", 100);
            done[index] = true;
        }
    };
    var blob = file.slice(start, start + step + 1);
    reader.readAsArrayBuffer(blob);
}

function hasSpecialChars(filelist)
{
    var regex = new RegExp("[^A-Za-z0-9_.]");
    for (var i = 0; i < filelist.length; i++) {
        var file = filelist[i];
        if (regex.test(file.name)) {
            return true;
        }
    }

    return false;
}

function warnSpecialChars(filelist, directory)
{
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

function uploadDrop(event) {
    this.classList.remove('runtask-highlight');
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
        showDialog("Operation Not Supported", "Sorry! We don't support downloading directly " +
            "from URL. Please download the file first and then upload here.");
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

function initUploadToaster(filelist, directory) {
    // Hide the dropzone
    $("#upload-dropzone-wrapper").hide("slide", { direction: "down" }, 200);

    // Create the dialog contents
    var toaster = $("<div/>").addClass("upload-toaster-list");
    for (var i = 0; i < filelist.length; i++) {
        var file = filelist[i];
        $("<div/>")
            .addClass("upload-toaster-file")
            .attr("name", file.name)
            .append(
                $("<span/>")
                    .addClass("upload-toaster-file-name")
                    .text(file.name)
            )
            .append(
                $("<div/>")
                    .addClass("upload-toaster-file-progress")
                    .progressbar({
                        change: function(event) {
                            $(this).find(".upload-toaster-file-progress-label").text($(this).progressbar("value") + "%");
                        },
                        complete: function() {
                            $(this).find(".upload-toaster-file-progress-label").text("Complete!");
                            $(this).parent().find(".upload-toaster-file-cancel").button("disable");
                        }
                    })
                    .append(
                        $("<div/>")
                            .addClass("upload-toaster-file-progress-label")
                            .text("Pending")
                    )
            )
            .append(
                $("<button/>")
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
            "load" : function(evt, dlg){
                $(".upload-dialog").find(".ui-dialog-titlebar-close").hide();
            },
            "minimize" : function(evt, dlg){
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
    $(".upload-dialog").find(".ui-dialog-titlebar-minimize").remove();
    $(".upload-dialog").find(".ui-dialog-titlebar-close").show();

    // Show the dropzone
    $("#upload-dropzone-wrapper").show("slide", { direction: "up" }, 200);

    // Refresh the tree
    $("#uploadTree").data("dndReady", {});
    $("#uploadTree").jstree("refresh");
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

function initUploads() {
    // Attach events to the upload drop zone
    var dropzone = $("#upload-dropzone");
    dropzone[0].addEventListener("dragenter", dragEnter, true);
    dropzone[0].addEventListener("dragleave", dragLeave, true);
    dropzone[0].addEventListener("dragexit", dragExit, false);
    dropzone[0].addEventListener("dragover", dragOver, false);
    dropzone[0].addEventListener("drop", uploadDrop, false);

    // Ready AJAX for uploading as a binary file
    if (!XMLHttpRequest.prototype.sendAsBinary) {
        XMLHttpRequest.prototype.sendAsBinary = function(ui8a) {
            function byteValue(x) {
                return x.charCodeAt(0) & 0xff;
            }
            try {
                this.send(ui8a);
            } catch(e){
                this.send(ui8a.buffer);
            }
        };
    }

    // Set up the exit prompt
    window.onbeforeunload = function(e) {
        if ($(".upload-dialog:visible").length > 0) {
            return "You are currently uploading files. If you navigate away from this page this will interrupt your file upload.";
        }
    };

    // Add click to browse functionality
    $("<input />")
        .attr("id", "upload-dropzone-input")
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
            if(hasSpecialChars(filelist)){
                warnSpecialChars(filelist, directory);
            }
            else
            {
                dirPromptIfNecessary(filelist, directory);
            }
        })
        .appendTo("#upload-dropzone-wrapper");

    $("#upload-dropzone").click(function() {
        $("#upload-dropzone-input").data("origin", "dropzone");
        $("#upload-dropzone-input").trigger("click");
    });
}

function initUploadTreeDND() {
    // Ready the drop & drop aspects of the file tree
    var eventsAttached = new Array();
    $("#uploadTree").find(".jstree-closed, .jstree-open").each(function(index, element) {
        var folder = $(element);

        // Protect against empties & repeats
        if (folder === null || folder === undefined || folder.length < 1) return;
        if ($.inArray(folder[0], eventsAttached) > -1) return;

        folder[0].addEventListener("dragenter", dragEnter, true);
        folder[0].addEventListener("dragleave", dragLeave, true);
        folder[0].addEventListener("dragexit", dragExit, false);
        folder[0].addEventListener("dragover", dragOver, false);
        folder[0].addEventListener("drop", uploadDrop, false);

        // Add to list to prevent repeats
        eventsAttached.push(folder[0]);

        var ready = $("#uploadTree").data("dndReady");
        if (ready === undefined || ready === null) {
            $("#uploadTree").data("dndReady", {});
            ready = $("#uploadTree").data("dndReady");
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
    var toReturn = new Array();
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

function createGenomeSpaceWidget(linkElement, appendTo) {
    var _isGenomeSpaceRoot = function(url) {
        var parts = url.split("dm.genomespace.org/datamanager/");
        var path = parts[parts.length-1];
        var pieces = path.split("/");
        return pieces.length <= 4;
    };

    var _createGenomeSpaceWidgetInner = function(linkElement, appendTo) {
        var link = $(linkElement);
        var url = link.attr("href");
        var name = $(linkElement).text();
        var isDirectory = link.attr("data-directory") === "true";
        var isRoot = _isGenomeSpaceRoot(url);

        var sendToString = linkElement.attr("data-sendtomodule");
        if (sendToString === null || sendToString === undefined) sendToString = '[]';
        var lsidList = JSON.parse(sendToString);
        var sendToList = lsidsToModules(lsidList).sort(function (a, b) {
            if (a.name.toLowerCase() < b.name.toLowerCase()) return -1;
            if (a.name.toLowerCase() > b.name.toLowerCase()) return 1;
            return 0;
        });

        var kind = linkElement.attr("data-kind");
        var clients = linkElement.attr("data-clients");

        var data = _constructGenomepaceMenuData(isRoot, isDirectory, kind, clients);

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

                    var listObject = $(event.target).closest(".search-widget").find(".send-to-param-list");
                    var url = listObject.attr("data-url");
                    var path = uploadPathFromUrl(url);

                    if (saveAction) {
                        window.location.href = url + "?download";
                        $(".search-widget:visible").searchslider("hide");
                        return;
                    }

                    else if (deleteAction) {
                        if (confirm('Are you sure you want to delete the selected file or directory?')) {
                            $.ajax({
                                type: "DELETE",
                                url: "/gp/rest/v1/data/delete/" + path,
                                success: function(data, textStatus, jqXHR) {
                                    $("#infoMessageDiv #infoMessageContent").text(data);
                                    $("#infoMessageDiv").show();

                                    if (isUpload) {
                                        $("#uploadTree").data("dndReady", {});
                                        $("#uploadTree").jstree("refresh");

                                        $("#uploadDirectoryTree").jstree("refresh");
                                    }
                                    if (isJobFile) {
                                        initRecentJobs();
                                    }
                                },
                                error: function(data, textStatus, jqXHR) {
                                    if (typeof data === 'object') {
                                        data = data.responseText;
                                    }

                                    showErrorMessage(data);
                                }
                            });

                            $(".search-widget:visible").searchslider("hide");
                        }
                        return;
                    }

                    else if (subdirAction) {
                        showDialog("Name the Subdirectory", "What name would you like to give the subdirectory?" +
                            "<input type='text' class='dialog-subdirectory-name' style='width: 98%;' />", {
                            "Create": function(event) {
                                var subdirName = $(".dialog-subdirectory-name").val();

                                var _createSubdirectory = function() {
                                    $.ajax({
                                        type: "PUT",
                                        url: "/gp/rest/v1/data/createDirectory/" + path + encodeURIComponent(subdirName),
                                        success: function(data, textStatus, jqXHR) {
                                            $("#infoMessageDiv #infoMessageContent").text(data);
                                            $("#infoMessageDiv").show();

                                            if (isUpload) {
                                                $("#uploadTree").data("dndReady", {});
                                                $("#uploadTree").jstree("refresh");

                                                $("#uploadDirectoryTree").jstree("refresh");
                                            }
                                        },
                                        error: function(data, textStatus, jqXHR) {
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
                                    return;
                                }
                                else {
                                    _createSubdirectory();
                                    $(this).dialog("close");
                                }
                            },
                            "Cancel": function(event) {
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
                        return;
                    }

                    else if (uploadAction) {
                        var directory = $(event.target).closest(".file-widget").attr("name");

                        $("#upload-dropzone-input").data("origin", directory);
                        $("#upload-dropzone-input").trigger("click");
                        return;
                    }

                    else if (pipelineAction) {
                        showDialog("Name the Pipeline", "What name would you like to give the pipeline?" +
                            "<input type='text' class='dialog-pipeline-name' style='width: 98%;' />", {
                            "Create": function(event) {
                                var subdirName = $(".dialog-pipeline-name").val();
                                subdirName = makePipelineNameValid(subdirName);

                                $.ajax({
                                    type: "PUT",
                                    url: "/gp/rest/v1/data/createPipeline/" + path + "?name=" + subdirName,
                                    success: function(data, textStatus, jqXHR) {
                                        $("#infoMessageDiv #infoMessageContent").text(data);
                                        $("#infoMessageDiv").show();

                                        var forwardUrl = jqXHR.getResponseHeader("pipeline-forward");
                                        if (forwardUrl && forwardUrl.length > 0) {
                                            window.location = forwardUrl;
                                        }
                                    },
                                    error: function(data, textStatus, jqXHR) {
                                        if (typeof data === 'object') {
                                            data = data.responseText;
                                        }

                                        showErrorMessage(data);
                                    }
                                });

                                $(this).dialog("close");
                            },
                            "Cancel": function(event) {
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
                        return;
                    }

                    else if (genomeSpaceAction) {
                        fileURL = url;							// Set the URL of the file

                        $('#genomeSpaceSaveDialog').dialog('open');

                        $(".search-widget:visible").searchslider("hide");
                        return;
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

        var widget = $("<div></div>")
            .attr("name", link.attr("href"))
            .attr("class", "search-widget file-widget")
            .searchslider({
                lists: [actionList, paramList, moduleList]});

        $(appendTo).append(widget);

        // Init the initial send to parameters
        var sendToParamList = widget.find(".send-to-param-list");
        sendToParamForMenu(sendToParamList);
    }

    if (all_modules_map !== null) {
        _createGenomeSpaceWidgetInner(linkElement, appendTo);
    }
    else {
        setTimeout(function() {
            createGenomeSpaceWidget(linkElement, appendTo);
        }, 100);
    }
}

function createFileWidget(linkElement, appendTo) {
    var _createFileWidgetInner = function(linkElement, appendTo) {
        var link = $(linkElement);
        var url = link.attr("href");
        var name = $(linkElement).text();
        var isDirectory = isDirectoryFromUrl(url);
        var isRoot = isRootFromUrl(url);
        var isUpload = appendTo === "#menus-uploads";
        var isJobFile = appendTo === "#menus-jobs";
        var isPartialFile = linkElement.attr("data-partial") === "true";

        var sendToString = linkElement.attr("data-sendtomodule");
        if (sendToString === null || sendToString === undefined) sendToString = '[]';
        var lsidList = JSON.parse(sendToString);
        var sendToList = lsidsToModules(lsidList).sort(function (a, b) {
            if (a.name.toLowerCase() < b.name.toLowerCase()) return -1;
            if (a.name.toLowerCase() > b.name.toLowerCase()) return 1;
            return 0;
        });

        var kind = linkElement.attr("data-kind");

        var data = constructFileMenuData(isRoot, isDirectory, isUpload, isJobFile, isPartialFile);

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

                    var listObject = $(event.target).closest(".search-widget").find(".send-to-param-list");
                    var url = listObject.attr("data-url");
                    var path = uploadPathFromUrl(url);

                    if (saveAction) {
                        window.location.href = url + "?download";
                        $(".search-widget:visible").searchslider("hide");
                        return;
                    }

                    else if (deleteAction) {
                        if (confirm('Are you sure you want to delete the selected file or directory?')) {
                            $.ajax({
                                type: "DELETE",
                                url: "/gp/rest/v1/data/delete/" + path,
                                success: function(data, textStatus, jqXHR) {
                                    $("#infoMessageDiv #infoMessageContent").text(data);
                                    $("#infoMessageDiv").show();

                                    if (isUpload) {
                                        $("#uploadTree").data("dndReady", {});
                                        $("#uploadTree").jstree("refresh");

                                        $("#uploadDirectoryTree").jstree("refresh");
                                    }
                                    if (isJobFile) {
                                        initRecentJobs();
                                    }
                                },
                                error: function(data, textStatus, jqXHR) {
                                    if (typeof data === 'object') {
                                        data = data.responseText;
                                    }

                                    showErrorMessage(data);
                                }
                            });

                            $(".search-widget:visible").searchslider("hide");
                        }
                        return;
                    }

                    else if (subdirAction) {
                        showDialog("Name the Subdirectory", "What name would you like to give the subdirectory?" +
                            "<input type='text' class='dialog-subdirectory-name' style='width: 98%;' />", {
                            "Create": function(event) {
                                var subdirName = $(".dialog-subdirectory-name").val();

                                var _createSubdirectory = function() {
                                    $.ajax({
                                        type: "PUT",
                                        url: "/gp/rest/v1/data/createDirectory/" + path + encodeURIComponent(subdirName),
                                        success: function(data, textStatus, jqXHR) {
                                            $("#infoMessageDiv #infoMessageContent").text(data);
                                            $("#infoMessageDiv").show();

                                            if (isUpload) {
                                                $("#uploadTree").data("dndReady", {});
                                                $("#uploadTree").jstree("refresh");

                                                $("#uploadDirectoryTree").jstree("refresh");
                                            }
                                        },
                                        error: function(data, textStatus, jqXHR) {
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
                                    return;
                                }
                                else {
                                    _createSubdirectory();
                                    $(this).dialog("close");
                                }
                            },
                            "Cancel": function(event) {
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
                        return;
                    }

                    else if (uploadAction) {
                        var directory = $(event.target).closest(".file-widget").attr("name");

                        $("#upload-dropzone-input").data("origin", directory);
                        $("#upload-dropzone-input").trigger("click");
                        return;
                    }

                    else if (pipelineAction) {
                        showDialog("Name the Pipeline", "What name would you like to give the pipeline?" +
                            "<input type='text' class='dialog-pipeline-name' style='width: 98%;' />", {
                            "Create": function(event) {
                                var subdirName = $(".dialog-pipeline-name").val();
                                subdirName = makePipelineNameValid(subdirName);

                                $.ajax({
                                    type: "PUT",
                                    url: "/gp/rest/v1/data/createPipeline/" + path + "?name=" + subdirName,
                                    success: function(data, textStatus, jqXHR) {
                                        $("#infoMessageDiv #infoMessageContent").text(data);
                                        $("#infoMessageDiv").show();

                                        var forwardUrl = jqXHR.getResponseHeader("pipeline-forward");
                                        if (forwardUrl && forwardUrl.length > 0) {
                                            window.location = forwardUrl;
                                        }
                                    },
                                    error: function(data, textStatus, jqXHR) {
                                        if (typeof data === 'object') {
                                            data = data.responseText;
                                        }

                                        showErrorMessage(data);
                                    }
                                });

                                $(this).dialog("close");
                            },
                            "Cancel": function(event) {
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
                        return;
                    }

                    else if (genomeSpaceAction) {
                        fileURL = url;							// Set the URL of the file

                        $('#genomeSpaceSaveDialog').dialog('open');

                        $(".search-widget:visible").searchslider("hide");
                        return;
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
    }

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

function constructFileMenuData(isRoot, isDirectory, isUpload, isJobFile, isPartialFile) {
    var data = [];

    if (!isRoot || !isDirectory) {
        data.push({
            "lsid": "",
            "name": "<img src='/gp/pipeline/images/delete.gif' class='module-list-icon'> Delete " + (isDirectory ? "Directory" : "File"),
            "description": (isDirectory ? "Permanently delete this directory and all child files." : "Permanently delete this file."),
            "version": "",
            "documentation": "http://genepattern.org",
            "categories": [],
            "suites": [],
            "tags": []
        });
    }

    if (isDirectory) {
        data.push({
            "lsid": "",
            "name": "Create Subdirectory",
            "description": "Create a subdirectory in this directory.",
            "version": "",
            "documentation": "http://genepattern.org",
            "categories": [],
            "suites": [],
            "tags": []
        });

        data.push({
            "lsid": "",
            "name": "Upload Files",
            "description": "Upload files to this directory.",
            "version": "",
            "documentation": "http://genepattern.org",
            "categories": [],
            "suites": [],
            "tags": []
        });
    }
    else if (!isPartialFile) {
        data.push({
            "lsid": "",
            "name": "<img src='/gp/pipeline/images/save.gif' class='module-list-icon'> Save File",
            "description": "Save a copy of this file to your local computer.",
            "version": "",
            "documentation": "http://genepattern.org",
            "categories": [],
            "suites": [],
            "tags": []
        });

        if (genomeSpaceEnabled && genomeSpaceLoggedIn) {
            data.push({
                "lsid": "",
                "name": "<img src='/gp/pages/genomespace/genomespace_icon.gif' class='module-list-icon'> Save to Genomespace",
                "description": "Save a copy of this file to your GenomeSpace account.",
                "version": "",
                "documentation": "http://genepattern.org",
                "categories": [],
                "suites": [],
                "tags": []
            });
        }
    }

    if (isJobFile) {
        data.push({
            "lsid": "",
            "name": "Create Pipeline",
            "description": "Create a provenance pipeline from this file.",
            "version": "",
            "documentation": "http://genepattern.org",
            "categories": [],
            "suites": [],
            "tags": []
        });
    }

    return data;
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

    // Create the menu widget
    var widgetFound = $(context).find("[name='" + escapeJquerySelector(url) + "']").length > 0;
    if (!widgetFound && !genomeSpace) {
        createFileWidget($(link), context);
    }
    else if (!widgetFound && genomeSpace) {
        createGenomeSpaceWidget($(link), context);
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
            //.text("Pending")
            .addClass("job-status-icon");
    }
    // Processing
    else if (!status.isFinished) {
        return $("<img/>")
            //.attr("src", "/gp/images/run.gif")
            .addClass("job-status-icon");
    }
    // Finished and Error
    else if (status.hasError) {
        return $("<img/>")
            .attr("src", "/gp/images/error.gif")
            .addClass("job-status-icon");
    }
    // must be Finished and Success
    else {
        return $("<img/>")
            .attr("src", "/gp/images/complete.gif")
            .addClass("job-status-icon");
    }
}

function createJobWidget(job) {
    var actionData = [];
    actionData.push({
        "lsid": "",
        "name": "Job Status",
        "description": "View the job status page for this job.",
        "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
    });

    if (job.status.isFinished) {
        actionData.push({
            "lsid": "",
            "name": "Download Job",
            "description": "Download a copy of this job, including all input and result files.",
            "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
        });
    }

    actionData.push({
        "lsid": "",
        "name": "Reload Job",
        "description": "Reload this job using the same input parameters.",
        "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
    });

    if (job.status.isFinished) {
        actionData.push({
            "lsid": "",
            "name": "Delete Job",
            "description": "Delete this job from the GenePattern server.",
            "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
        });
    }

    if (!job.status.isFinished) {
        actionData.push({
            "lsid": "",
            "name": "Terminate Job",
            "description": "Terminate this job on the GenePattern server.",
            "version": "", "documentation": "", "categories": [], "suites": [], "tags": []
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
                var path = uploadPathFromUrl(url);

                if (statusAction) {
                    loadJobStatus(job.jobId);
                    return;
                }

                else if (downloadAction) {
                    $(location).attr('href', '/gp/rest/v1/jobs/' + job.jobId + '/download');

                    $(".search-widget:visible").searchslider("hide");
                    return;
                }

                else if (reloadAction) {
                    $(location).attr('href', '/gp/pages/index.jsf?lsid' + job.taskLsid + "&reloadJob=" + job.jobId);

                    $(".search-widget:visible").searchslider("hide");
                    return;
                }

                else if (deleteAction) {
                    if (confirm('Are you sure you want to delete the selected job?')) {
                        $.ajax({
                            type: "DELETE",
                            url: "/gp/rest/v1/jobs/" + job.jobId + "/delete",
                            success: function(data, textStatus, jqXHR) {
                                $("#infoMessageDiv #infoMessageContent").text(data);
                                $("#infoMessageDiv").show();

                                initRecentJobs();
                            },
                            error: function(data, textStatus, jqXHR) {
                                if (typeof data === 'object') {
                                    data = data.responseText;
                                }

                                showErrorMessage(data);
                            }
                        });
                    }
                    $(".search-widget:visible").searchslider("hide");
                    return;
                }

                else if (terminateAction) {
                    $.ajax({
                        type: "DELETE",
                        url: "/gp/rest/v1/jobs/" + job.jobId + "/terminate",
                        success: function(data, textStatus, jqXHR) {
                            $("#infoMessageDiv #infoMessageContent").text(data);
                            $("#infoMessageDiv").show();

                            initRecentJobs();
                        },
                        error: function(data, textStatus, jqXHR) {
                            if (typeof data === 'object') {
                                data = data.responseText;
                            }

                            showErrorMessage(data);
                        }
                    });

                    $(".search-widget:visible").searchslider("hide");
                    return;
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
                    window.open("/gp/rest/v1/jobs/" + job.jobId + "/code?language=Java")
                    $(".search-widget:visible").searchslider("hide");
                    return;
                }

                else if (matlabAction) {
                    window.open("/gp/rest/v1/jobs/" + job.jobId + "/code?language=MATLAB")
                    $(".search-widget:visible").searchslider("hide");
                    return;
                }

                else if (rAction) {
                    window.open("/gp/rest/v1/jobs/" + job.jobId + "/code?language=R")
                    $(".search-widget:visible").searchslider("hide");
                    return;
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
        window.location = "/gp/jobResults";
    });

    // Init the jobs
    $.ajax({
        cache: false,
        type: "GET",
        url: "/gp/rest/v1/jobs/recent",
        dataType: "json",
        success: function(data, textStatus, jqXHR) {
            // Clear away the old rendering of the tab
            $("#loading-jobs").hide();
            var tab = $("#left-nav-jobs-list");
            tab.empty();

            // Clear away any old jobs menus
            $("#menus-jobs").empty();

            // For each top-level job
            for (var i = 0; i < data.length; i++) {
                var jobJson = data[i];

                // Protect against null jobs
                if (jobJson === null) {
                    console.log("ERROR rendering job:");
                    console.log(jobJson);
                    continue;
                }

                renderJob(jobJson, tab);
            }

            // Handle the case of no recent jobs
            if (data.length === 0) {
                tab.append("<h3 style='text-align:center;'>No Recent Jobs</h3>");
            }
        },
        error: function(data, textStatus, jqXHR) {
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
        .attr("href", "#")
        .attr("onclick", "openJobWidget(this); return false;")
        .attr("data-jobid", jobJson.jobId)
        .attr("data-json", JSON.stringify(jobJson))
        .text(jobJson.taskName + " (" + jobJson.jobId + ")")
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

    for (var j = 0; j < jobJson.outputFiles.length; j++) {
        var file = jobJson.outputFiles[j];

        var fileBox = $("<div></div>")
            .addClass("job-file")
            .appendTo(jobDetails);

        var link = $("<a></a>")
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
        for (var j = 0; j < jobJson.children.items.length; j++) {
            var child = jobJson.children.items[j];
            renderJob(child, jobDetails);
        }
    }
}

function openJobWidget(link) {
    var id = $(link).attr("data-jobid");

    // Create the job widget
    var widgetFound = $("#menus-jobs").find("[name='job_" + id + "']").length > 0;
    if (!widgetFound) {
        // Get the job JSON
        var jobJson = $(link).data("json");

        createJobWidget(jobJson);
    }

    // Open the job slider
    $("#menus-jobs").find("[name='job_" + id + "']").searchslider("show");
}

function loadJobStatus(jobId) {
    // Abort if no job to load
    if (jobId === undefined || jobId === null || jobId === '') {
        return;
    }

    // Hide the search slider if it is open
    $(".search-widget").searchslider("hide");

    // Hide the protocols & run task form, if visible
    $("#protocols").hide();
    $("#submitJob").hide();

    // Handle open visualizer flag
    var openVisualizers = $.param("openVisualizers");
    if (openVisualizers) {
        openVisualizers = "&openVisualizers=true";
    }
    else {
        openVisualizers = "&openVisualizers=false";
    }

    // Add to history so back button works
    history.pushState(null, document.title, location.protocol + "//" + location.host + location.pathname + "?jobid=" + jobId);

    $.ajax({
        type: "GET",
        url: "/gp/pages/jobResult.jsf?jobNumber=" + jobId + openVisualizers,
        cache: false,
        success: function(data, textStatus, jqXHR) {
            $("#jobResults").html(data);
            $("#jobResults").show();
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