
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
   var cDim = clientDim();
   var width = cDim.w; //  f_clientWidth();
   var height = cDim.h; //  f_clientHeight();

   if(pos) {
      if(pos[0] < (width / 2)) {
        // Menu is on left side of page, use left align
        style.left = Math.max(0, pos[0] - horizOffset) + "px";
      }
      else {
        // Menu is on right side of page, user right align.
        style.right = Math.max(-f_scrollLeft(), width - pos[0] - horizOffset) + "px";
      }
      if(pos[1] < (height / 2)) {
        // Menu is on top half of page, use top align
        style.top = Math.max(0, pos[1] - vertOffset) + "px";
      }
      else {
    //    var agt = navigator.userAgent.toLowerCase();
    //	var isSafari = agt.indexOf("safari") != -1;
    //	if(isSafari) { // height is computed incorrectly in Safari
    //		style.top = Math.max(0, pos[1] - vertOffset) + "px";
    //	} else {
           // Menu is on bottom half of page, user bottom align.
           style.bottom = Math.max(-f_scrollTop(), height - pos[1] - vertOffset)+ "px";
     //   }
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



function clientDim() {
  var w = window;
  var b = document.body;
  var d = new Object();

  if (w.innerWidth) {
    d.w = w.innerWidth;
    d.h = w.innerHeight;
  } else if (b.parentElement.clientWidth) {
    d.w = b.parentElement.clientWidth;
    d.h = b.parentElement.clientHeight;
  } else if (b && b.clientWidth) {
    d.w = b.clientWidth;
    d.h = b.clientHeight;
  }
  return d;
}


// Some (hopefully) browser independent functions for size and position

function f_clientWidth() {
	return f_filterResults (
		window.innerWidth ? window.innerWidth : 0,
		document.documentElement ? document.documentElement.clientWidth : 0,
		document.body ? document.body.clientWidth : 0
	);
}
function f_clientHeight() {
	return f_filterResults (
		window.innerHeight ? window.innerHeight : 0,
		document.documentElement ? document.documentElement.clientHeight : 0,
		document.body ? document.body.clientHeight : 0
	);
}
function f_scrollLeft() {
	return f_filterResults (
		window.pageXOffset ? window.pageXOffset : 0,
		document.documentElement ? document.documentElement.scrollLeft : 0,
		document.body ? document.body.scrollLeft : 0
	);
}
function f_scrollTop() {
	return f_filterResults (
		window.pageYOffset ? window.pageYOffset : 0,
		document.documentElement ? document.documentElement.scrollTop : 0,
		document.body ? document.body.scrollTop : 0
	);
}
function f_filterResults(n_win, n_docel, n_body) {
	var n_result = n_win ? n_win : 0;
	if (n_docel && (!n_result || (n_result > n_docel)))
		n_result = n_docel;
	return n_body && (!n_result || (n_result > n_body)) ? n_body : n_result;
}



