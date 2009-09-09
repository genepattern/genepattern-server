var originalSubmit;
var batchSubmit = "/gp/batchSubmit.jsp";
var submitOnComplete = 0;
var inputCount = 1;

var mismatchErrorHead = " You have uploaded multiple files for multiple parameters but the lists of files do not match.<br/>For a batch process you must either ";
var mismatchErrorBody =  "<ul style='font-weight:bold; color:red;'>"+
		   	"<li> Upload multiple files for a single parameter only.&nbsp&nbsp Or </li>"+
		   	"<li> Each parameter must have a list of files whose names differ only in their extension (e.g.  Parameter 1 has a.gcs;b.gcs;c.gcs and parameter 2 has a.res;b.res;c.res).</li>"+
		   "</ul>";

var appletParams = 		"code='jmaster.jumploader.app.JumpLoaderApplet.class'"+
"archive='/gp/downloads/jl_core_z.jar'"+
"width='600' height='400' mayscript='true' >"+
"<param name='uc_uploadUrl' value='/gp/MultiFileUploadReceiver'/>"+	
"<param name='uc_directoriesEnabled' value='true'/>"+	
"<param name='uc_partitionLength' value='5000000'/>" +
"<param name='ac_fireUploaderFileStatusChanged' value='true'/>"+
"<param name='ac_fireUploaderStatusChanged' value='true'/>"+
"<param name='ac_fireUploaderFileRemoved' value='true'/>"+
"<param name='ac_fireAppletInitialized' value='true'/>"+
"<param name='vc_uploadViewFilesSummaryBarVisible' value='false'/>"+
"<param name='vc_uploadViewStartUploadButtonText' value='Start upload'/>"+
"<param name='vc_uploadViewStartUploadButtonImageUrl' value='/gp/images/media_play_green.png'/>"+
"<param name='vc_uploadViewStopUploadButtonText' value='Stop upload'/>"+
"<param name='vc_uploadViewStopUploadButtonImageUrl' value='/gp/images/media_stop_red.png'/>";


function addBatchSubmitLinksToPage(){	
	originalSubmit = jQuery("#taskForm").attr('action');


	//Create uploader for directory type
	jQuery("div .directory_param").each( function(){
		var id = jQuery(this).attr('id');
		jQuery(this).replaceWith(			
			"<td class='jumploaderWindow' id='"+ id +"'>" +
			"<applet name='di" + id + "'"+
			appletParams +
			"</applet>"+ 
		"</td>"
		);		

		
	});
	
	//Create links for input files types
	jQuery("input[type='file']").after("" +
			"<a class='batchprocess' href='#' >Batch process...</a>");
	jQuery("a.batchprocess").click(function(){
		var inputId = jQuery(this).prev().attr('id');
		jQuery(jq(inputId+"_td")).hide();
		//See if we've already created the applet and just hidden it.  If so, we'll just show it again
		if (jQuery(jq("jlID"+inputId)).size() > 0){
			jQuery(jq("jlID"+inputId)).show();
			jQuery(jq("revert"+inputId)).show();
		}else{
			if (navigator.javaEnabled()){
				jQuery(jq(inputId+"_td")).after(
						"<td class='jumploaderWindow' id='jlID" + inputId+"'>" +
							"<applet name='jl"+ inputId + "'"+
								appletParams+
							"</applet>"+ 
							"<br/>"+
							"<a id='revert"+inputId+"' href='#'> Load single file</a>"+						
						"</td>"								
				);
			}else{			
					jQuery(jq("jlID"+inputId)).replaceWith(
							"<td id=noJavaErr>"+
								"<p style=color:red>Java is not detected on your machine.  You need Java to upload multiple files at once.</p>"+
								"<a id='revert"+inputId+"' href='#'> Load single file</a>"+
							"</td>"									
					);
			}
			
			jQuery(jq("revert"+inputId)).click( function () {				
				jQuery(this).hide();
				jQuery(jq("noJavaErr")).hide();
				jQuery(jq("jlID"+inputId)).hide();
				jQuery(jq(inputId+"_td")).show();
								
			});
			localToRemoteMap = new Object();
		}	
	});
	
	//Retrive the formValidator onsubmit function, but only call if our first verification succeeds
	var formSubmitFunction = document.getElementById("taskForm").onsubmit;
	document.getElementById("taskForm").onsubmit = null;	
	
	jQuery("#taskForm").submit(function(){				
		if (!validateSubmit()){
			return false;
		}else{
			//Now call the original form submit (validation) function
			return formSubmitFunction();	
		}
			
	});
};

function validateSubmit(){
	//Validation check.  If there are multiple file uploaders, make sure
	//that they have the same number of files, and the same file names
	//but different extensions for each.
	
	var firstCheck = true;
	var fileCount;
	var fileRootNames = new Object();
	
	var valid = true;
	jQuery("applet").each( function() {
		try {
			var uploader = this.getUploader();
		 	fileCount = uploader.getFileCount();
			if (fileCount > 1){
				if (firstCheck){
					for (var i=0; i < fileCount; i++){
						var file = uploader.getFile(i);	
						var fullName = file.getName();
						fileRootNames[getRootName(fullName)] = '1';							
					}							
					firstCheck = false;
				}else{
					if (fileCount != uploader.getFileCount()){
						showMismatchError();
						valid = false;
					}else{	
						for (var i=0; i < fileCount; i++){
							var file = uploader.getFile(i);			
							var fullName = file.getName();
							var exists = fileRootNames[getRootName(fullName)];
							if (typeof(exists) == 'undefined') {
								showMismatchError();
								valid = false;
							}														
						}
					}						
				}
			}
		}catch (e){		
		
		}
	});		
	if (!valid){
		return false;
	}
	
	var uploadingComplete = true;		
	submitOnComplete = 0;
	jQuery("applet").each( function() {
		try{
			var uploader = this.getUploader();
			if (uploader.isUploading()){
				uploadingComplete = false;
				submitOnComplete = submitOnComplete + 1;					
			}else if (uploader.canStartUpload()){
				uploader.startUpload();
				submitOnComplete = submitOnComplete + 1;					
				uploadingComplete = false;	
			}
		}catch(e){
			
		}
	});		
	
	if (!uploadingComplete){
		return false;
	}
	
	//See if any of the _url file input text fields have multiple files
	//If so, change to batch submit		
	jQuery("input[type='file']").each(function() {
		var fileId = jQuery(this).attr('id');
		//See if the input box is in file mode or url mode by checking the radio button (_cb) contents			
		if (jQuery(jq(fileId+"_cb_url")).attr('checked')){
			//We're in url mode.  Read the value and see if it's multi file
			var urlInputBoxContents = jQuery(jq(fileId+"_url")).val();				
			if (urlInputBoxContents){
				if (urlInputBoxContents.indexOf(';') >= 0){
					jQuery("#taskForm").attr('action', batchSubmit);		
				}
			}
		}			
	});
	return true;
}

function getRootName (fullName){	
	if (fullName.indexOf(".") > 0){
		 return fullName.substr(0, fullName.indexOf("."));
	}else{
		return fullName;
	}
}

function showMismatchError(){
	var d = $("errorMessageDiv");
	Element.update('errorMessageHeader', mismatchErrorHead);
	d.style.display = "block";
	Element.update('errorMessageContent', mismatchErrorBody);
    return false;
}

function uploaderFileStatusChanged( uploader, file) {

	if (file.getStatus()==2){		
		//File upload complete
		jQuery("applet").each(function() {
			try{
				if (this.getUploader().equals(uploader)){
					var inputType = jQuery(this).attr("name").substring(0,2);
					if (inputType == "jl"){
				
						var inputName =  jQuery(this).attr("name").substring(2);					
						jQuery(jq(inputName+"_cb_url")).click();	
						var urlName = inputName+"_url";		
						var response = file.getResponseContent().split(";");
						jQuery(jq(urlName)).val( jQuery(jq(urlName)).val() + response[1]+ ';');
						localToRemoteMap[file.getPath()] = response[1];
					}else if (inputType=="di"){
						var inputName =  jQuery(this).attr("name").substring(2);
						var paramName = inputName.substring(0, inputName.length - 14);
						var response = file.getResponseContent().split(";");						
						jQuery(jq(paramName)).val (response[0]);
					}
				}
			}catch(e){
			}
		});
	}
	
}	

function uploaderFileRemoved( uploader, file ) {	
	jQuery("applet").each(function() {
		try{
			if (this.getUploader().equals(uploader)){
				var inputType = jQuery(this).attr("name").substring(0,2);
				if (inputType == "jl"){
					var inputName =  jQuery(this).attr("name").substring(2);
					jQuery(jq(inputName+"_cb_url")).click();	
					var urlName = inputName+"_url";
			
					var currentFileList = jQuery(jq(urlName)).val();		
					var fileToRemove = localToRemoteMap[file.getPath()];
					var shortenedFileList = currentFileList.replace(fileToRemove+";","");
					
					jQuery(jq(urlName)).val( shortenedFileList);
				}
			}
		}catch(e){
		}
	});
}


function uploaderStatusChanged( uploader ) {
	if (uploader.getStatus()==0){
		if (submitOnComplete > 0){
			submitOnComplete = submitOnComplete - 1;
			if (submitOnComplete == 0){
				jQuery("#taskForm").submit();
			}
		}		
	}
}

function appletInitialized( applet){
	applet.getMainView().getUploadView().showOpenDialog();	
	var attrSet = applet.getUploader().getAttributeSet();
	var attr = attrSet.createStringAttribute("paramId", Math.random());
	attr.setSendToServer(true);
}

jQuery(document).ready(function(){
	addBatchSubmitLinksToPage();
	if(jQuery.browser.mozilla) jQuery("form").attr("autocomplete", "off"); 
});

//Returns a jquery compatible id, that escapes all . and :
function jq(myid){ 
	return '#'+myid.replace(/:/g,"\\:").replace(/\./g,"\\.");
}

