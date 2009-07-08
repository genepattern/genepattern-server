//To dos.
//  Remove Batch processs... links from everything else, once they've submitted at least one file through the batch process procedure
//  **After a successful add file, change submit to submitBatchJob.jsp
//  **If the user switches back to single upload, or deletes all files from URL upload, change back to submitJob.jsp
//  Add a submit after upload complete link to the MultiFileUpload.xhtml form

function MultiFileUpload(){
	
	this.curMultiFileInput=null;
	this.originalSubmit = jQuery("#taskForm").attr('action');
	this.initialize();
	this.batchSubmit="/gp/batchSubmit.jsp";	
}

MultiFileUpload.prototype.initialize = function (){	

	//Attach to all  input file dialog boxes, an anchor that allows "batch process..."
	jQuery("input[type='file']").after("<a name='batchprocess' href='multiFileUpload.jsf' rel='lyteframe' rev='width:625px; height:450px; scrolling:auto;' >Batch process...</a>");
	//Our anchor uses the special rel='lyteframe' tag to specify that it's a popup window.  But
	//to convert the tag to actual javascript, we need to call the lytebox initialization function
	initLytebox();
	
	//Track the old this pointer, so we can use it within our jQuery environment
	var me = this;
	
	
	//When the user clicks on batch process we will
	//  1. take note of which to wihch input field the popup window should assign the uploaded files(by setting curMultiFileInput)
	//  2. Switch to the Specify Path or URL mode of the input box
	jQuery("[name='batchprocess']").click(function (event){
		me.curMultiFileInput = jQuery(this).prev().attr('id');
		
	});	
	
	jQuery("#taskForm").submit(function(){
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
						jQuery("#taskForm").attr('action', me.batchSubmit);		
					}
				}
			}			
		});
	});
};

MultiFileUpload.prototype.assignFiles= function(filename){
	//Click the URL link, so the input changes to a text box.  We can't add multiple files
	//to a file-input control for security reasons
	jQuery(jq(this.curMultiFileInput+"_cb_url")).click();	
	var urlName = this.curMultiFileInput+"_url";
	jQuery(jq(urlName)).val( jQuery(jq(urlName)).val() + filename + ';');	
};

jQuery(document).ready(function(){
	theMultiFileUpload = new MultiFileUpload();
	if(jQuery.browser.mozilla) jQuery("form").attr("autocomplete", "off"); 
});

//Returns a jquery compatible id, that escapes all . and :
function jq(myid){ 
	return '#'+myid.replace(/:/g,"\\:").replace(/\./g,"\\.");
}
