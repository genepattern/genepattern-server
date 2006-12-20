var fileRows = new Object();

function validateForm() {
   var inputElement = $("nameField");
   var d = $("nameErrorMessageDiv");
   if (inputElement.value == null || inputElement.value == "") {  
       d.style.display = "inline";
       return false;
   }else {        
       d.style.display = "hidden";
       return true;
   }
}
    
function getContext() {
    return '#{facesContext.externalContext.requestContextPath}';
}

function toggleSuiteCheckboxes(id) {
    var jobCheckboxDiv = $("cb_job_" + id);
    var jobCheckbox = jobCheckboxDiv.getElementsByTagName("input")[0];
    var isChecked = jobCheckbox.checked;
    var rowIds = fileRows[id];
    for(var i=0; i<rowIds.length; i++) {
        var row = document.getElementById(rowIds[i]);
        var elements = row.getElementsByTagName("input");
        for(var j=0; j<elements.length; j++) {
          if(elements[j].type == "checkbox") {
             elements[j].checked = isChecked;
          }
        }
    }
}
		
function confirmDelete() {
	return confirm('Are you sure you want to delete the selected suite(s)?');
}

function confirmDeleteSupportFile() {
	return confirm('Are you sure you want to delete this support file?');
}