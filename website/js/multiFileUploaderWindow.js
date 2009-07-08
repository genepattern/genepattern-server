function uploaderFileStatusChanged( uploader, file) {
	if (file.getStatus()==2){			
		window.parent.theMultiFileUpload.assignFiles(file.getResponseContent());
	}
}	

function uploaderStatusChanged( uploader ) {
	if (uploader.getStatus()==0){
		if (window.document.getElementById("submitWhenComplete").checked){
			window.parent.document.getElementById('lbMain').style.display='none';
			window.parent.document.getElementById('lbOverlay').style.display='none';
			window.parent.document.getElementById('taskForm').action = "/gp/batchSubmit.jsp";			
			window.parent.document.getElementById('taskForm').submit();			
		}
	}		
}