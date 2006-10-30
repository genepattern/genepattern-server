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