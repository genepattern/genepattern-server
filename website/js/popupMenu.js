// Global array to hold popmenu ids
var pm_popupMenuIds = new Array();

var pm_currentId;

// Mouse click handler.  Close  popupMenus

function pm_registerClickHandler() {
    Event.observe(window.document, 'click', function() {
       for(var i=0; i<pm_popupMenuIds.length; i++) {
         if(pm_popupMenuIds[i] != pm_currentId) {
           pm_hideMenu(pm_popupMenuIds[i]); 
         }
       }
       pm_currentId = null;
    }, false);   
} 

function pm_showMenu(id, pos, leftOffset, topOffset) {
   pm_currentId = id;
   var style = $(id).style;
   if(pos) {
     style.left = (pos[0] - leftOffset)  + "px";
     style.top = (pos[1] - topOffset) + "px";
   }
   style.visibility = "";  //<= resets to default
} 
  
function pm_hideMenu(id) {
  $(id).style.visibility = "hidden";
} 

function pm_setMousePosition(e) {
  alert(document.event);
}