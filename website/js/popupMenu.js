
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

function pm_showMenu(id, pos, horizOffset, vertOffset) {
   if(pm_currentId != null) {
     pm_hideMenu(pm_currentId)
   }
   pm_currentId = id;
   pm_showing = true;
   var style = $(id).style;
   if(pos) {
      if(pos[0] < (window.innerWidth / 2)) {
        // Menu is on left side of page, use left align
        style.left = Math.max(0, pos[0] - horizOffset) + "px";
      }
      else {
        // Menu is on right side of page, user right align. 
        style.right = Math.max(0, window.innerWidth - pos[0] - horizOffset) + "px"; 
      }
      if(pos[1] < (window.innerHeight / 2)) {
        // Menu is on top half of page, use top align
        style.top = Math.max(0, pos[1] - vertOffset) + "px";
      }
      else {
        // Menu is on bottom half of page, user bottom align. 
        style.bottom = Math.max(0, window.innerHeight - pos[1] - vertOffset) + "px"; 
      } 
    }
    style.display = "";
} 


  
function pm_hideMenu(id) {
  var menu = $(id);
  if(menu != null) {
    $(id).style.display = "none";
    pm_currentId = null;
  }
  
} 



