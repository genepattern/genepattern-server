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
		if (typeof menu !== 'string') {
			menu = menuRegistry._generate(menu);
		}
		setTimeout(function() { new PopupMenu(button, menu); }, 300);
	},
	
	/**
	 * This accepts a JSON object with the given format:
	 * {
	 * 		id: "idOfMenu",
	 * 		items: [
	 * 			{
	 * 				"value": "displayValueOfOption",
	 * 				"attr": {
	 * 					"href": "attributes of link",
	 * 					"onclick": "no link is created if attr is empty array or null"
	 * 				}
	 * 			},
	 * 			"SEPERATOR" // Constant for inserting a seperator into the menu
	 * 		]
	 * }
	 */
	_generate: function(json) {
		var menuDiv = document.createElement("div");
		jq(menuDiv).attr("id", json.id);
		jq(menuDiv).attr("class", "popupMenu");
		jq(menuDiv).attr("style", "display:none;");
		
		// Create menu items
		for (var i = 0; i < json.items.length; i++) {
			var itemJSON = json.items[i];
			
			// Handle seperators
			if (typeof itemJSON == "string") {
				var seperator = document.createElement("hr");
				jq(menuDiv).append(seperator);
				continue;
			}
			
			// Create the item's div
			var itemDiv = document.createElement("div");
			
			// Handle no links: attr is null, item has no link
			if (itemJSON.attr === undefined || itemJSON.attr === null || jq.isEmptyObject(itemJSON.attr)) {
				jq(itemDiv).text(itemJSON.value);
				jq(menuDiv).append(itemDiv);
				continue;
			}
			
			// Handle menu items with links and attributes
			var itemLink = document.createElement("a");
			for (var key in itemJSON.attr) {
				var value = itemJSON.attr[key];
				jq(itemLink).attr(key, value);
			}
			
			jq(itemLink).text(itemJSON.value);
			jq(itemDiv).append(itemLink);
			jq(menuDiv).append(itemDiv);
		}
		
		jq(menuDiv).menu();
		jq(document.body).append(menuDiv);
		
		return "#" + json.id;
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
		var id = this.id;
		setTimeout(function(){menuRegistry[id].hide();},200);
		return true;
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