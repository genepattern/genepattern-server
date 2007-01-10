
function toggleVisibility(panelStateBean, id) {
    var element = document.getElementById(id + "_panel");
    var hiddenField = document.getElementById("expansion_state_" + id);    
    var panelState = "open";  // default   
    if(element.style.display=="none")
    {
        showCategory(id);
        if(hiddenField != null) hiddenField.value = "true";
        panelState = "closed";
    }
    else
    {
        hideCategory(id);
        if(hiddenField != null) hiddenField.value = "false";
        panelState = "open";
    }
    
    if(panelStateBean != null) {
      var method = panelStateBean + ".updatePanelState";
      updateBeanState(method, id, panelState);
     
    }
}

function hideCategory( id ) {
    var tableElement = document.getElementById(id + "_panel");
    var downArrow = document.getElementById(id + "_expanded_img");
    var rightArrow = document.getElementById(id + "_collapsed_img");	
        
    tableElement.style.display="none";
    downArrow.style.display ="none";
    rightArrow.style.display ="inline"	    
}

function showCategory( id ) {
    var tableElement = document.getElementById(id + "_panel");
    var downArrow = document.getElementById(id + "_expanded_img");
    var rightArrow = document.getElementById(id + "_collapsed_img");	
        
    tableElement.style.display="block";
    downArrow.style.display ="inline";
    rightArrow.style.display ="none"
}
	
function openAll() {
     var panels = document.getElementsByClassName("category_panel");
     for(var i=0; i<panels.length; i++) {
       var fullId = panels[i].id;
       var index = fullId.lastIndexOf('_');
       var catId = fullId.substring(0, index);
       showCategory(catId);
     }
}
	
function closeAll() {
     var panels = document.getElementsByClassName("category_panel");
     for(var i=0; i<panels.length; i++) {
       var fullId = panels[i].id;
       var index = fullId.lastIndexOf('_');
       var catId = fullId.substring(0, index);
       hideCategory(catId);
     }
 }
 
function toggleExpansionState( categoryIdentifier) {
   var id= "expansion_state_" + categoryIdentifier;
   var hiddenField = document.getElementById(id);
   hiddenField.value = (hiddenField.value == "true" ? "false" : "true");
 }
 

function updateBeanState(elExpression, id, state) {            
  var opt = {
    method:    'post',
    postBody:  'el=' + elExpression + '&id=' + id + '&state=' + state,
    onSuccess: receiveResponse,
    onFailure: function(t) {
      alert('Error ' + t.status + ' -- ' + t.statusText);
    }
  } 
  new  Ajax.Request('#{facesContext.externalContext.requestContextPath}/anyThingAtAll.ajax',opt);
}

// The callback function - receive response from server
function receiveResponse( req ) {
  if (req.readyState == 4) {
    if (req.status == 200) { // only if "OK"
      update(req.responseText);
    } 
    else {
      alert("There was a problem retrieving the XML data:\n" + req.statusText);
    }  
  }
}
