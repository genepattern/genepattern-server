var originalSubmit;
var batchSubmit = "/gp/batchSubmit.jsp";
var submitOnComplete = 0;

function addBatchSubmitLinksToPage(){	
	originalSubmit = jQuery("#taskForm").attr('action');

	jQuery("input[type='file']").after("" +
			"<a class='batchprocess' href='#' >Batch process...</a>");
	jQuery("a.batchprocess").click(function(){
		var inputId = jQuery(this).prev().attr('id');
		jQuery(jq(inputId+"_td")).hide();
		jQuery(jq(inputId+"_td")).after(
				"<td class='jumploaderWindow' id='jlID" + inputId+"'>" +
					"<applet name='jl"+ inputId + "'"+
					"code='jmaster.jumploader.app.JumpLoaderApplet.class'"+
					"archive='/gp/downloads/jl_core_z.jar'"+
					"width='600' height='400' mayscript='true' >"+
						"<param name='uc_uploadUrl' value='/gp/MultiFileUploadReceiver'/>"+	
						"<param name='uc_directoriesEnabled' value='true'/>"+	
						"<param name='uc_partitionLength' value='5000000'/>" +
						"<param name='ac_fireUploaderFileStatusChanged' value='true'/>"+
						"<param name='ac_fireUploaderStatusChanged' value='true'/>"+
						"<param name='ac_fireUploaderFileRemoved' value='true'/>"+
						"<param name='ac_fireAppletInitialized' value='true'/>"+
					"</applet>"+
					"<br/> "+
					"<a id='revert"+inputId+"' href='#'> Load single file</a>"+
				"</td>" 				
		);
		jQuery(jq("revert"+inputId)).click( function () {				
			jQuery(this).hide();
			jQuery(jq("jlID"+inputId)).hide();
			jQuery(jq(inputId+"_td")).show();				
		});
		jQuery("applet").each(function() {
			this.localToRemoteMap = new Object();
		});
	});
	
	//Retrive the formValidator onsubmit function, but only call if our first verification succeeds
	var formSubmitFunction = document.getElementById("taskForm").onsubmit;
	document.getElementById("taskForm").onsubmit = null;	
	
	jQuery("#taskForm").submit(function(){		
		
		var uploadingComplete = true;		
		jQuery("applet").each( function() {
			if (typeof(this.getUploader)!= undefined){				
				var uploader = this.getUploader();
				if (uploader.canStartUpload()){
					uploader.startUpload();
					submitOnComplete = submitOnComplete + 1;
					uploadingComplete = false;
				}
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
		
		//Now call the original form submit (validation) function
		return formSubmitFunction();		
	});
};

function uploaderFileStatusChanged( uploader, file) {
	if (file.getStatus()==2){		
		jQuery("applet").each(function() {
			if (typeof(this.getUploader) != undefined){
				if (this.getUploader().equals(uploader)){
					var inputName =  jQuery(this).attr("name").substring(2);
					jQuery(jq(inputName+"_cb_url")).click();	
					var urlName = inputName+"_url";
					jQuery(jq(urlName)).val( jQuery(jq(urlName)).val() + file.getResponseContent() + ';');
					
					this.localToRemoteMap[file.getPath()] = file.getResponseContent();
				}
			}
		});
	}
}	

function uploaderFileRemoved( uploader, file ) {	
	jQuery("applet").each(function() {
		if (typeof(this.getUploader) != undefined){
			if (this.getUploader().equals(uploader)){
				var inputName =  jQuery(this).attr("name").substring(2);
				jQuery(jq(inputName+"_cb_url")).click();	
				var urlName = inputName+"_url";
		
				var currentFileList = jQuery(jq(urlName)).val();		
				var fileToRemove = this.localToRemoteMap[file.getPath()];
				var shortenedFileList = currentFileList.replace(fileToRemove+";","");
				
				jQuery(jq(urlName)).val( shortenedFileList);
			}
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
}

jQuery(document).ready(function(){
	addBatchSubmitLinksToPage();
	if(jQuery.browser.mozilla) jQuery("form").attr("autocomplete", "off"); 
});

//Returns a jquery compatible id, that escapes all . and :
function jq(myid){ 
	return '#'+myid.replace(/:/g,"\\:").replace(/\./g,"\\.");
}
