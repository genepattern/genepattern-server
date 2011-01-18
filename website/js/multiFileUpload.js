"use strict";
/*global jQuery: false, $: false, Element: false*/
var originalSubmit;
var batchSubmit = "/gp/batchSubmit.jsp";
var submitOnComplete = false;
var inputCount = 1;
var localToRemoteMap = new Object();

var mismatchErrorHead = " You have uploaded multiple files for more than one parameter but the lists of files do not match.<br/>You must either ";
var mismatchErrorBody =  "<ul style='font-weight:bold; color:red;'>" +
		   	"<li> Upload multiple files for a single parameter only.&nbsp&nbsp Or </li>" +
		   	"<li> Each parameter must have a list of files whose names differ only in their extension (e.g.  Parameter 1 has a.gcs, b.gcs, c.gcs and Parameter 2 has a.res, b.res, c.res.)</li>" +
		   "</ul>";

var appletParams = 		"code='jmaster.jumploader.app.JumpLoaderApplet.class'" +
"archive='/gp/downloads/jl_core_z.jar'" +
"width='600' height='400' mayscript='true' >" +
"<param name='uc_uploadUrl' value='/gp/MultiFileUploadReceiver'/>" +	
"<param name='uc_directoriesEnabled' value='true'/>" +	
"<param name='uc_partitionLength' value='5000000'/>"  +
"<param name='ac_fireUploaderFileStatusChanged' value='true'/>" +
"<param name='ac_fireUploaderStatusChanged' value='true'/>" +
"<param name='ac_fireUploaderFileRemoved' value='true'/>" +
"<param name='ac_fireAppletInitialized' value='true'/>" +
"<param name='vc_uploadViewFilesSummaryBarVisible' value='false'/>" +
"<param name='vc_uploadViewStartUploadButtonText' value='Start upload'/>" +
"<param name='vc_uploadViewStartUploadButtonImageUrl' value='/gp/images/media_play_green.png'/>" +
"<param name='vc_uploadViewStopUploadButtonText' value='Stop upload'/>" +
"<param name='vc_uploadViewStopUploadButtonImageUrl' value='/gp/images/media_stop_red.png'/>";

//forward declare functions
var validateSubmit, getRootName, showMismatchError;

//Returns a jquery compatible id, that escapes all . and :
function jq(myid) { 
	return '#' + myid.replace(/:/g, "\\:").replace(/\./g, "\\.");
}


function createJumploader(id){
	jQuery(jq(id + "_div_multifile_launcher")).after(
		"<div id='" + id + "_div_multifile'  >" +
		"<input id='" + id + "_multifile' style='display:none;' type='text' name='" + id + "_multifile'/>" +
		"<td class='jumploaderWindow' id='jlID" + id + "' name='jlID" + id + "'>" +
			"<applet name='jl" + id + "'" +
				appletParams +
				"</applet>" + 
				"<br/>" +
		"</td>" +
		"</div>"
	);

}
function addBatchSubmitLinksToPage() {	
	originalSubmit = jQuery("#taskForm").attr('action');

	//Create uploader for directory type
	jQuery("div .directory_param").each(function () {
		var id = jQuery(this).attr('id');
		jQuery(this).replaceWith(			
			"<td class='jumploaderWindow' id='" + id + "' name='" + id + "'>" +
			"<applet name='di" + id + "'" +
			appletParams  +
			"</applet>" + 
		"</td>"
		);		

		
	});
	
	//For each input file type, find the following radio buttons and append a third
	jQuery("input[type='file']").each(function () {
		var id = jQuery(this).attr('id');
		jQuery(this).parent().parent().find("span:last").after(
			"<span class='description' style='display: inline;'>" +
				"<input id='" + id + "_cb_multifilelaunch' type='radio'  value='multifile' name= '" + id + "_cb_multifilelaunch'/>" +
				"<label for='" + id + "_cb_multifilelaunch'>Upload Multiple Files</label>"
		);
	
		jQuery(this).parent().after(
				"<div id='" + id + "_div_multifile_launcher' style='display: none;'>" +
						"<button type='button' id='" + id + "_button' class='batchprocess'>Launch File Browser</button>" +
						"<p> Easily upload multiple files.  The module will be run once for each file.</p>" +
				"</div>" +
				"<div id='" + id + "_div_multifile_javaoff' style='display: none;'>" + 
					"<p style=color:red> You do not appear to have Java enabled.  Java is required to use the multiple file uploader. </p>" +				
				"</div>"				
		);
	});
	
	
	jQuery("input[type='radio']").click(function () {
		var id, 
		    suffixPos,
		    root,
		    suffix;
		id = jQuery(this).attr('id');
		suffixPos = id.indexOf('_cb_');
		root = id.substring(0, suffixPos);
		suffix = id.substring(suffixPos + 4, id.length);
		if (suffix !== 'multifilelaunch') {
			jQuery(jq(root + "_div_multifile_launcher")).hide();
			jQuery(jq(root + "_div_multifile")).remove();	
			jQuery(jq(root + "_div_multifile_javaoff")).hide();
			jQuery(jq(root + "_cb_multifilelaunch")).attr("checked", false);
		} else {
			jQuery(jq(root + "_cb_file")).attr("checked", false);
			jQuery(jq(root + "_cb_url")).attr("checked", false);
			
			jQuery(jq(root + "_div_file")).hide();
			jQuery(jq(root + "_div_url")).hide();
			if (navigator.javaEnabled()) {
				jQuery(jq(root + "_div_multifile_launcher")).show();				
			} else {
				jQuery(jq(root + "_div_multifile_javaoff")).show();
			}
		}
	});
	
	jQuery(".batchprocess").click(function () {
		var inputId = jQuery(this).attr('id');
		inputId = inputId.substring(0, inputId.length - String("_button").length);
		jQuery(jq(inputId + "_div_multifile_launcher")).hide();		
		createJumploader(inputId);
		
	});
	
	//Retrive the formValidator onsubmit function, but only call if our first verification succeeds
	var formSubmitFunction = document.getElementById("taskForm").onsubmit;
	document.getElementById("taskForm").onsubmit = null;	
	
	jQuery("#taskForm").submit(function () {				
		if (!validateSubmit()) {
			return false;
		} else {
			//Now call the original form submit (validation) function
			return formSubmitFunction();	
		}
			
	});
}

function getNumFiles(paramRootName){
	var submitTextField;
	submitTextField = paramRootName + "_multifile";			
	return jQuery(jq(submitTextField)).val().split(";").length - 1;
	
}
function validateSubmit() {
	//Validation check.  If there are multiple file uploaders, make sure
	//that they have the same number of files, and the same file names
	//but different extensions for each.
	
	var firstCheck = true,
	    fileCount,
	    fileRootNames = new Object(), 
	    valid = true,
	    uploader = null,
	    file,
	    fullName,
	    i,
	    exists,
	    paramID;
	jQuery("applet").each(function () {
		uploader = null;
		try {
			uploader = this.getUploader();
		} catch (e) {		
			//applet has since been hidden or removed				
		}
		if (uploader !== null) {
			fileCount = uploader.getFileCount();
			if (fileCount > 1) {
				if (firstCheck) {
					for (i = 0; i < fileCount; i += 1) {
						file = uploader.getFile(i);	
						fullName = file.getName();
						fileRootNames[getRootName(fullName)] = '1';							
					}							
					firstCheck = false;
				} else {
					if (fileCount !== uploader.getFileCount()) {
						showMismatchError();
						valid = false;
					} else {	
						for (i = 0; i < fileCount; i += 1) {
							file = uploader.getFile(i);			
							fullName = file.getName();
							exists = fileRootNames[getRootName(fullName)];
							if (typeof(exists) === 'undefined') {
								showMismatchError();
								valid = false;
							}														
						}
					}						
				}
			}
		}
	});		
	if (!valid) {
		return false;
	}
	
	//start one uploader at a time.  
	//First see if any uploads are in progress.  If so, just wait
	valid = true;	
	jQuery("applet").each(function () {
		uploader = null;
		try {
			uploader = this.getUploader();
		} catch (e) {			
		}
		if (uploader !== null) {
			if (uploader.isUploading()) {
				submitOnComplete = true;	
				valid = false;
			}
		}
	} );
	if (!valid) {
		return false;
	}

	//Next see if any uploader is ready to start.  If so, start just one.
	var uploaderStarted = false;
	jQuery("applet").each(function () {
		uploader = null;
		try {
			uploader = this.getUploader();
		} catch (e) {			
		}
		if (uploader !== null) {
			 if (!uploaderStarted && uploader.canStartUpload()) {
				uploader.startUpload();
				uploaderStarted = true;
				submitOnComplete = true;				
			}
		}
	});
	if (!valid) {
		return false;
	}
	
	//Finally, see if all files uploaded succesfully, or if the retry button is needed
	jQuery("applet").each(function () {
		uploader = null;
		try {
			uploader = this.getUploader();
		} catch (e) {			
		}
		if (uploader !== null) {
			inputType = jQuery(this).attr("name").substring(0, 2);
			if (inputType === "jl") {				
				//see if all the callbacks have occured, and the number
				//of files in the multifile submit input field match the 
				//number of files in the uploader.
				fileCount = uploader.getFileCount();
				paramID = jQuery(this).attr("name").substring(2);
				if (getNumFiles(paramID)!= fileCount) {
					submitOnComplete = true;
					valid = false;
					return;
				}			
			}
		}
	});
	if (!valid) {
		return false;
	}
	
	
	
	
	
	//Assume a regular submit unless we find a multifle checkbox checked
	jQuery("#taskForm").attr('action', originalSubmit);
	jQuery("input[type='file']").each(function () {
		var fileId = jQuery(this).attr('id');
		if (jQuery(jq(fileId + "_cb_multifilelaunch")).attr('checked')) {
			jQuery("#taskForm").attr('action', batchSubmit);
		}			
	});
	return true;
}

function getRootName(fullName) {	
	if (fullName.indexOf(".") > 0) {
		return fullName.substring(0, fullName.indexOf("."));
	} else {
		return fullName;
	}
}

function showMismatchError() {
	var d = $("errorMessageDiv");
	Element.update('errorMessageHeader', mismatchErrorHead);
	d.style.display = "block";
	Element.update('errorMessageContent', mismatchErrorBody);
    return false;
}

function uploaderFileStatusChanged(uploader, file) {
	var thisUploader,
		inputType,
		inputName,
		submitTextField,
		response,
		paramName;
	if (file.getStatus() === 2) {		
		//File upload complete
		jQuery("applet").each(function () {
			thisUploader = null;
			try {
				thisUploader = this.getUploader();
			} catch (e) {
			}
			if (thisUploader !== null) {
				if (thisUploader.equals(uploader)) {
					inputType = jQuery(this).attr("name").substring(0, 2);
					if (inputType === "jl") {				
						inputName =  jQuery(this).attr("name").substring(2);				
						submitTextField = inputName + "_multifile";								
						response = String(file.getResponseContent()).split(";");
						jQuery(jq(submitTextField)).val(jQuery(jq(submitTextField)).val() + response[1] + ';');
						localToRemoteMap[file.getPath()] = response[1];
					} else if (inputType === "di") {
						inputName =  jQuery(this).attr("name").substring(2);
						paramName = inputName.substring(0, inputName.length - 14);
						response = String(file.getResponseContent()).split(";");						
						jQuery(jq(paramName)).val(response[0]);
					}
				}
			}
		});
	} else if (file.getStatus() == 3){
		//failed
		//uploader.retryFileUpload(file);
		//alert (file.getError().getMessage());
	}
	
}	

function uploaderFileRemoved(uploader, file) {	
	var thisUploader,
		inputType,
		submitTextField,
		currentFileList,
		fileToRemove,
		shortenedFileList,
		inputName;

	jQuery("applet").each(function () {
		thisUploader = null;

		try {
			thisUploader = this.getUploader();
		} catch (e) {
		}
		if (thisUploader !== null) {
			if (thisUploader.equals(uploader)) {
				inputType = jQuery(this).attr("name").substring(0, 2);
				if (inputType === "jl") {
					inputName =  jQuery(this).attr("name").substring(2);
					submitTextField = inputName + "_multifile";			
					currentFileList = jQuery(jq(submitTextField)).val();		
					fileToRemove = localToRemoteMap[file.getPath()];
					shortenedFileList = currentFileList.replace(fileToRemove + ";", "");					
					jQuery(jq(submitTextField)).val(shortenedFileList);
				}
			}
		}
	});
}


function uploaderStatusChanged(uploader) {
	if (uploader.getStatus() === 0) {
		if (submitOnComplete) {
			jQuery("#taskForm").submit();			
		}		
	}
}

function appletInitialized(applet) {
	var attrSet,
	    attr;
	applet.getMainView().getUploadView().showOpenDialog();	
	attrSet = applet.getUploader().getAttributeSet();
	attr = attrSet.createStringAttribute("paramId", Math.random());
	attr.setSendToServer(true);
}

jQuery(document).ready(function () {
	
	addBatchSubmitLinksToPage();
	if (jQuery.browser.mozilla) {
		jQuery("form").attr("autocomplete", "off");
	}
	
});


