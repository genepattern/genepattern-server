// toggleCheckBoxes -- used in combination with a "master" checkbox to toggle
// the state of a collection of child checkboxes.  Assumes the children and parent
// share a common container parent    
function toggleCheckBoxes(maincheckbox, parentId) {	
  var isChecked = maincheckbox.checked;
  var parentElement = document.getElementById(parentId);   
  var elements = parentElement.getElementsByTagName("input");
  for (i = 0; i < elements.length; i++) {	
    if(elements[i].type="checkbox") {		
      elements[i].checked = isChecked;
    }
  }
}


//
// stop a running job by its ID
//
function stopJob(button, jobId) {
    var really = confirm('Really stop this Job?');
    if (!really) return;
    window.open("runPipeline.jsp?cmd=stop&jobID="+jobId, "_blank", "height=100, width=100, directories=no, menubar=no, statusbar=no, resizable=no");
}

//request email notification for a user and a job
 function requestEmailNotification(userEmail, jobId) {            
        var opt = {
          method:    'post',
          postBody:  'cmd=notifyEmailJobCompletion&userID='+userEmail+'&jobID='+jobId,
          onSuccess: ajaxEmailResponse,
          onFailure: function(t) {
            alert('Error ' + t.status + ' -- ' + t.statusText);
          }
        } 
        new  Ajax.Request('./notifyJobCompletion.ajax',opt);
      }

  function cancelEmailNotification(userEmail, jobId) {  
		 		    
        var opt = {
          method:    'post',
          postBody:  'cmd=cancelEmailJobCompletion&userID='+userEmail+'&jobID='+jobId,
          onSuccess: ajaxEmailResponse,
          onFailure: function(t) {
            alert('Error ' + t.status + ' -- ' + t.statusText);
          }
        } 
        new  Ajax.Request('./cancelJobCompletion.ajax',opt);
      }

  function ajaxEmailResponse( req ) {

        if (req.readyState == 4) {
          if (req.status == 200) { // only if "OK"
            //alert('all is well on email submission') 
          } else {
            alert("There was a problem in email notification:\n" + req.statusText);
          }  
        }
      }
