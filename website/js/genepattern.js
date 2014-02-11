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
	for (i = 0; i < elements.length; i++) {
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
	jq.ajax({
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
	jq.ajax({
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
	jq.ajax({
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
	var form = jq("#" + formId);
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
				jq(this).dialog("close");
				if (event.preventDefault)
					event.preventDefault();
				if (event.stopPropagation)
					event.stopPropagation();
			}
		};
	}

	jq(alert).dialog({
		modal : true,
		dialogClass : "top-dialog",
		width : 400,
		title : title,
		buttons : button,
		close : function() {
			jq(this).dialog("destroy");
			jq(this).remove();
		}
	});

	// Fix z-index for dialog
	var z = parseInt(jq(alert).parent().css("z-index"));
	if (z < 10000) {
		z += 9000;
		jq(".top-dialog").css("z-index", z);
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
	
	var modsearch = $('#module-suites').searchslider({
        lists: [browse]
    });
}

function initBrowseModules() {
	var browse = $('<div id="module-list-browse"></div>').modulelist({
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
	
	return browse;
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
                    console.log(still_loading);
                    still_loading = false;
                }, 400);
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
        	$.ajax({
        		type: 'POST',
                url: '/gp/rest/v1/tags/pin',
                dataType: 'text',
                data: JSON.stringify({
                	user: username,
                	lsid: baseLsid($(ui.item).find(".module-lsid").text()),
                	position: calcPosition(ui.placeholder)
                }),
                success: function(data, status, xhr) {
                	console.log("pinned");
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
        	$.ajax({
        		type: 'DELETE',
                url: '/gp/rest/v1/tags/unpin',
                dataType: 'text',
                data: JSON.stringify({
                	user: username,
                	lsid: baseLsid($(ui.item).find(".module-lsid").text()),
                	position: 0
                }),
                success: function(data, status, xhr) {
                	console.log("unpinned");
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
            url: '/gp/rest/v1/jobs/incomplete',
            dataType: 'json',
            success: function(data, status, xhr) {
            	$(".current-job-status a").empty();
            	if (data.length > 0) {
            		$(".current-job-status a").text(" " + data.length + " Jobs Processing");
            		$(".current-job-status a").prepend("<img src='/gp/images/spin.gif' alt='Jobs Currently Processing' />");
            		$.data($(".current-job-status")[0], "continuePolling", true);
            	}
            	else {
            		$(".current-job-status a").text(" No Jobs Processing");
            		$(".current-job-status a").prepend("<img src='/gp/images/complete.gif' alt='No Jobs Processing' />");
            		$.data($(".current-job-status")[0], "continuePolling", false);
            	}
            }
        });
		}
}

function ajaxFileTabUpload(file, directory){
    var loaded = 0;
    var step = 1024*1024;
    var total = file.size;
    var start = 0;
    var partitionIndex = 0;
    var partitionCount = Math.ceil(total / step);

    var reader = new FileReader();

    // Hide the dropzone
    $("#upload-dropzone").hide();
    $("#upload-dropzone-wrapper span").hide();

    // Show the progressbar
    $("#upload-dropzone-progress").show();
    $("#upload-dropzone-progress").progressbar({
        value: false,
        change: function() {
            $("#upload-dropzone-progress-label").text("Uploading: " + $("#upload-dropzone-progress").progressbar( "value" ) + "%");
        },
        complete: function() {
            $("#upload-dropzone-progress-label").text("Upload Complete!");

            // Hide the progressbar
            $("#upload-dropzone-progress").hide();

            // Show the dropzone
            $("#upload-dropzone").show();
            $("#upload-dropzone-wrapper span").show();

            // Refresh the tree
            $("#uploadTree").data("dndReady", {});
            $("#uploadTree").jstree("refresh");
        }
    });

    reader.onprogress = function(event){
        loaded += event.loaded;
        var progress = Math.min(Math.round((loaded/total) * 100), 100);
        $("#upload-dropzone-progress").progressbar("value", progress);
    };

    reader.onload = function(event){
        var xhr = new XMLHttpRequest();
        var upload = xhr.upload;
        upload.addEventListener('load',function(){
            if (loaded <= total) {
                blob = file.slice(loaded, loaded + step + 1);
                reader.readAsBinaryString(blob);
            }
            else {
                loaded = total;
            }
        },false);
        xhr.open("POST", "/gp/AJAXUpload?fileName=" + file.name);
        xhr.overrideMimeType("application/octet-stream");
        xhr.setRequestHeader('partitionCount', partitionCount.toString());
        xhr.setRequestHeader('partitionIndex', partitionIndex.toString());
        xhr.setRequestHeader('filename', file.name);
        xhr.setRequestHeader('uploadPath', directory);
        xhr.sendAsBinary(event.target.result);

        partitionIndex++;
    };
    var blob = file.slice(start, start + step + 1);
    reader.readAsBinaryString(blob);
}

function uploadDrop(event) {
    this.classList.remove('runtask-highlight');
    event.stopPropagation();
    event.preventDefault();

    var ul = document.createElement("ul");
    var filelist = event.dataTransfer.files;

    if (filelist.length < 1) {
        showDialog("Operation Not Supported", "Sorry! We don't support downloading directly " +
            "from URL. Please download the file first and then upload here.");
        return;
    }

    if ($(event.toElement).attr("id") === "upload-dropzone") {
        openUploadDirectoryDialog(filelist);
    }
    else {
        var directory = $(event.toElement).closest(".jstree-open").find("a:first").attr("href");
        uploadAfterDialog(filelist, directory);
    }
}

function uploadAfterDialog(filelist, directory) {
    for (var i = 0; i < filelist.length; i++) {
        var file = filelist[i];
        ajaxFileTabUpload(file, directory);
    }
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
    if(!XMLHttpRequest.prototype.sendAsBinary){
        XMLHttpRequest.prototype.sendAsBinary = function(datastr) {
            function byteValue(x) {
                return x.charCodeAt(0) & 0xff;
            }
            var ords = Array.prototype.map.call(datastr, byteValue);
            var ui8a = new Uint8Array(ords);
            try{
                this.send(ui8a);
            }catch(e){
                this.send(ui8a.buffer);
            }
        };
    }
}

function initUploadTreeDND(folder_id) {
    // Ready the drop & drop aspects of the file tree
    var eventsAttached = new Array();
    var folder = $(folder_id);
    $("#uploadTree li").find("ins, a img").each(function(index, element) {
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
        console.log(eventsAttached);

        var ready = $("#uploadTree").data("dndReady");
        if (ready === undefined || ready === null) {
            $("#uploadTree").data("dndReady", {});
            ready = $("#uploadTree").data("dndReady");
        }
        ready[folder_id] = true;

    });
}