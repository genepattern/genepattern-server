
function toggleVisibility(id) {
    var element = document.getElementById(id + "_panel");
    var panelState = "open";  // default   
    if(element.style.display=="none")
    {
        openCategory(id);
    }
    else
    {
        closeCategory(id);
    }

}

function closeCategory( id ) {
    var tableElement = document.getElementById(id + "_panel");
    var downArrow = document.getElementById(id + "_expanded_img");
    var rightArrow = document.getElementById(id + "_collapsed_img");	
        
    tableElement.style.display="none";
    downArrow.style.display ="none";
    rightArrow.style.display ="inline"	   
    
    updatePanelState(id, "closed"); 
}

function openCategory( id ) {
    var tableElement = document.getElementById(id + "_panel");
    var downArrow = document.getElementById(id + "_expanded_img");
    var rightArrow = document.getElementById(id + "_collapsed_img");	
        
    tableElement.style.display="block";
    downArrow.style.display ="inline";
    rightArrow.style.display ="none"
    
    updatePanelState(id, "open");
}
	
function openAll() {
     var panels = document.getElementsByClassName("category_panel");
     for(var i=0; i<panels.length; i++) {
       var fullId = panels[i].id;
       var index = fullId.lastIndexOf('_');
       var catId = fullId.substring(0, index);
       openCategory(catId);
     }
}
	
function closeAll() {
     var panels = document.getElementsByClassName("category_panel");
     for(var i=0; i<panels.length; i++) {
       var fullId = panels[i].id;
       var index = fullId.lastIndexOf('_');
       var catId = fullId.substring(0, index);
       closeCategory(catId);
     }
 }
 

function updateBeanState(id, state) {            
  var opt = {
    method:    'post',
    postBody:  'el=moduleChooserState.updatePanelState&id=' + id + '&state=' + state,
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
