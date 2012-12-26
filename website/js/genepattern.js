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
	window
			.alert('Job not stopped, stopJob should not be called from this page!');
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
	var form = pt(formId);
	if (form == null) {
		alert("Form " + formId + " not found.");
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
	var alert = document.createElement("div");

	if (typeof (message) == 'string') {
		alert.innerHTML = message;
		;
	} else {
		alert.appendChild(message);
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
