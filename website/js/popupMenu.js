
var pm_currentId;
var pm_showing = false;

function pm_registerClickHandler() {
    Event.observe(window.document, 'click', pm_clickHandler, false);  
} 

function pm_clickHandler() {
  if(!pm_showing) {
    pm_hideMenu(pm_currentId); 
  } 
  pm_showing = false;
  return true;
}

function pm_showMenu(id, pos, leftOffset, topOffset) {
   pm_currentId = id;
   pm_showing = true;
   var style = $(id).style;
   if(pos) {
     style.left = (pos[0] - leftOffset)  + "px";
     style.top = (pos[1] - topOffset) + "px";
   }
   style.visibility = "";  //<= resets to default
} 
  
function pm_hideMenu(id) {
  var menu = $(id);
  if(menu != null) {
    $(id).style.visibility = "hidden";
    pm_currentId = null;
  }
  
} 



