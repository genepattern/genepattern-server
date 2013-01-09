/**
 * Code for handling the pop-up menus, rewritten to use jQuery
 * by Thorin Tabor
 */

/**
 * Utility function for escaping IDs used in jQuery selection
 */
function jqEscape(str) {
	return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

/**
 * Singleton holding a registry of all created popup menus
 * This is a map with the button ID as the key and the menu ID as a key
 */
var menuRegistry = {
	_register: function(popupMenu) {
		var buttonKey = popupMenu.button.attr("id");
		var menuKey = popupMenu.menu.attr("id");
		this[buttonKey] = popupMenu;
		this[menuKey] = popupMenu;
	},
	
	_create: function(button, menu) {
		setTimeout(function() { new PopupMenu(button, menu); }, 300);
	}
};

/**
 * Class representing a popup menu
 */
function PopupMenu(button, menu) {
	// Set menu and button to jQuery selection
	if (typeof button === "string") { this.button = jq(jqEscape(button)); }
	else { this.button = jq(button); }
	if (typeof menu === "string") { this.menu = jq(jqEscape(menu)); }
	else { this.menu = jq(menu); }
	
	// Make sure menu is a menu widget
	this.menu.menu();
	
	// Register the PopupMenu object with the popups singleton
	menuRegistry._register(this);
	
	// Add click event to button
	this.button.click(function() {
		menuRegistry[this.id].show();
	});
	
	// Add blur event to menu
	this.menu.blur(function(event) {
		menuRegistry[this.id].hide();
	});
	
	// Add click to subdirectory text fields
	this.menu.find(".subdirName").click(function() {
		var focused = this; 
		setTimeout(function() {
			jq(focused).focus();
		},1);
	});
	
	// Add blur to subdirectory text fields
	this.menu.find(".subdirName").blur(function() {
		var focused = this;
		setTimeout(function() {
			if (!jq(document.activeElement).hasClass("popupMenu")) {
				jq(focused).closest(".popupMenu").trigger("blur");
			}
		}, 1);	
	});

    this.show = function() {
    	this._resetPosition();
    	this.menu.show();
    	this.menu.focus();
    };
    
    this.hide = function() {
    	var selectedMenu = this.menu;
    	setTimeout(function() {
    		if (!jq(document.activeElement).hasClass("subdirName")) {
    			selectedMenu.hide();
    		}
        }, 1);
    };

    this._resetPosition = function() {
    	var width = this.menu.width();
    	var height = this.menu.height();
    	
    	var offsetLeft = this.button.offset().left;
    	var offsetTop = this.button.offset().top;
    	
    	if (offsetLeft < (width / 2)) {
    		// Menu is on left side of page, use left align
    		this.menu.css("left", Math.max(0, offsetLeft - 50) + "px");
    	}
    	else {
    		// Menu is on right side of page, user right align
    		this.menu.css("right", Math.max(0, jq(window).width() - offsetLeft - 50) + "px");
    	}
    	
    	//always use top align
	    var menuTop = offsetTop - 50; // Initial guess for the location of the popup menu
	    var menuBottom = menuTop + height;
	    
	    if (menuBottom > jq(window).height()) {
	        var dv = menuBottom - jq(window).height();
	        menuTop = menuTop - dv;
	        menuTop = menuTop - 10; //10 px adjustment to prevent scrollbars
	        
	        //then adjust so that all scrolling is down
	        menuTop = Math.max(0, menuTop);
	    }
	    
	    //make sure the top of the menu is not out of the display area
	    this.menu.css("top", Math.max(71, menuTop) + "px");
    };
}

/**
 * Register a click event with the page to make menu disappear on blur
 */
jq(document).click(function() {
	jq(".p").hide();
});

/*
 * Old Code to Cleanup
 */

var pm_currentId;
var pm_showing = false;
var pm_subdir_focus = false;
var pm_dropdown_focus = false;

function setDropdownFocus() {
	pm_dropdown_focus = true;
}

// Used in 2 JSF
function setSubdirFocus() {
	pm_subdir_focus = true;
}

// Used in 1 JSF
function pm_registerClickHandler() {
    Event.observe(window.document, 'click', pm_clickHandler, false);
}

function pm_clickHandler() {
	if (pm_dropdown_focus) {
		pm_dropdown_focus = false;
		  return;
	  }
	
  if (pm_subdir_focus) {
	  pm_subdir_focus = false;
	  return;
  }
  if(!pm_showing) {
    pm_hideMenu(pm_currentId);
  }
  pm_showing = false;
  pm_subdir_focus = false;
  return true;
}

// Used all over
function pm_showMenu(id, pos, horizOffset, vertOffset) {
   if(pm_currentId != null) {
     pm_hideMenu(pm_currentId)
   }
   pm_currentId = id;
   pm_showing = true;
   
   var cDim = clientDim();
   var width = cDim.w; //  f_clientWidth();
   var height = cDim.h; //  f_clientHeight();

   var style = pt(id).style;
   style.width = 'auto';
   style.height = 'auto';

   if(pos) {
      if(pos[0] < (width / 2)) {
        // Menu is on left side of page, use left align
        style.left = Math.max(0, pos[0] - horizOffset) + "px";
      }
      else {
        // Menu is on right side of page, user right align.
        style.right = Math.max(-f_scrollLeft(), width - pos[0] - horizOffset) + "px";
      }
      //always use top align
      var menuTop = pos[1] - vertOffset; //inital guess for the location of the popup menu
      var menuBottom = menuTop + pt(id).scrollHeight;
      var screenBottom = f_scrollTop() + height;
      //check to see if the menu extends beyond the bottom of the display area
      if (menuBottom > screenBottom) {
        var dv = menuBottom - screenBottom;
        menuTop = menuTop - dv;
        menuTop = menuTop - 10; //10 px adjustment to prevent scrollbars
        //then adjust so that all scrolling is down
        menuTop = Math.max(f_scrollTop(), menuTop);
      }
      //make sure the top of the menu is not out of the display area
      menuTop = Math.max(71, menuTop);
      style.top = menuTop + "px";
   }
   style.visibility = "visible";
}


function pm_hideMenu(id) {
  var menu = pt(id);
  if(menu != null) {
    pt(id).style.visibility = "hidden";
    pt(id).style.width = '10px';
    pt(id).style.height = '10px';

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



