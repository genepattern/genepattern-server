/*
 * Copyright 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

/**
 * @author tabor
 * @requires jQuery, jQuery UI, ddsmoothmenu
 * 				Also requires that the createTaskAllowed, createPublicPipelineAllowed, 
 * 				createPrivatePipelineAllowed, createPublicSuiteAllowed, createPrivateSuiteAllowed, 
 * 				adminServerAllowed genomeSpaceEnabled and genomeSpaceLoggedIn variables are set.
 */
var Menu = {
	jquery: null,
		
	ensureJQuery: function() {
		if (typeof jq !== 'undefined') {
			this.jquery = jq;
		}
		else {
			this.jquery = $;
		}
	},
	
	go: function(url) {
		window.open(url, "_blank");
		return void(0);
	},
	
	denyIE: function(url) {
		if (navigator.userAgent.indexOf("MSIE") !== -1) {
			var alert = document.createElement("div");
			this.jquery(alert).text("The Pipeline Designer and Module Integrator don't support Internet Explorer.  Please use Firefox or Chrome.");
			this.jquery(alert).dialog({
	            modal: true,
	            dialogClass: "top-dialog",
	            width: 400,
	            title: "Internet Explorer Not Supported",
	            buttons: "OK",
	            close: function() {
	            	this.jquery(this).dialog("destroy");
	            	this.jquery(this).remove();
	            }
	        });
		}
		else {
			location.href = url;
		}
	},

	buildNavMenu: function() {
		Menu.ensureJQuery();
		
		// Initialize the menu
	    ddsmoothmenu.init({
	        mainmenuid: "navband1", //menu DIV id
	        orientation: 'h', //Horizontal or vertical menu: Set to "h" or "v"
	        classname: 'ddsmoothmenu', //class added to menu's outer DIV
	        contentsource: "markup" //"markup" or ["container_id", "path_to_menu_file"]
	    });
	},
	
	initNavMenu: function() {
		if (!createTaskAllowed) this.jquery(".createTaskAllowed").hide();
	    if (!createPublicPipelineAllowed) this.jquery(".createPublicPipelineAllowed").hide();
	    if (!createPrivatePipelineAllowed) this.jquery(".createPrivatePipelineAllowed").hide();
	    if (!createPublicSuiteAllowed) this.jquery(".createPublicSuiteAllowed").hide();
	    if (!createPrivateSuiteAllowed) this.jquery(".createPrivateSuiteAllowed").hide();
	    if (!adminServerAllowed) this.jquery(".adminServerAllowed").hide();
	    if (!genomeSpaceLoggedIn) this.jquery(".genomeSpaceLoggedIn").hide();
	    if (genomeSpaceLoggedIn) this.jquery(".genomeSpaceLoggedOut").hide();
	    if (!genomeSpaceEnabled) this.jquery(".genomeSpaceMenu").hide();
	    
	    this.jquery("#navband1").show();
	    this.jquery("#navband1 ul li ul").css("top", "23px");
	}
};