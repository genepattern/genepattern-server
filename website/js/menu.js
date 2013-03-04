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
			jq(alert).text("The Pipeline Designer and Module Integrator don't support Internet Explorer.  Please use Firefox, Chrome or Safari.");
			jq(alert).dialog({
	            modal: true,
	            dialogClass: "top-dialog",
	            width: 400,
	            title: "Internet Explorer Not Supported",
	            buttons: "OK",
	            close: function() {
	                $(this).dialog("destroy");
	                $(this).remove();
	            }
	        });
		}
		else {
			location.href = url;
		}
	},

	buildNavMenu: function() {
		Menu.ensureJQuery();
		var menu = this.jquery( // Begin creation of navband HTML
			"<div id=\"navband1\" class=\"navband1 ddsmoothmenu\" style=\"white-space: nowrap; display:none;\">\
	            <ul>\
	                <li><a href=\"/gp/pages/index.jsf\">Modules &#38; Pipelines</a>\
	                    <ul>\
	                        <li class=\"createPrivatePipelineAllowed\"><a href=\"JavaScript:Menu.denyIE('/gp/pipeline/index.jsf');\">New Pipeline</a></li>\
	                        <li class=\"createTaskAllowed\"><a href=\"JavaScript:Menu.denyIE('/gp/modules/creator.jsf');\">New Module</a></li>\
	                        <li class=\"createTaskAllowed\"><a href=\"/gp/pages/taskCatalog.jsf\">Install From Repository</a></li>\
	                        <li><a href=\"/gp/pages/importTask.jsf\">Install From ZIP</a></li>\
	                        <li><a href=\"/gp/pages/manageTasks.jsf\">Manage</a></li>\
	                    </ul>\
	                </li>\
	                <li><a href=\"/gp/pages/manageSuite.jsf\">Suites</a>\
	                    <ul>\
	                        <li class=\"createPrivateSuiteAllowed\"><a href=\"/gp/pages/createSuite.jsf\">New Suite</a></li>\
	                        <li class=\"createPublicSuiteAllowed\"><a href=\"/gp/pages/suiteCatalog.jsf\">Install From Repository</a></li>\
	                        <li><a href=\"/gp/pages/importTask.jsf?suite=1\">Install From ZIP</a></li>\
	                        <li><a href=\"/gp/pages/manageSuite.jsf\">Manage</a></li>\
	                    </ul>\
	                </li>\
	                <li><a href=\"/gp/jobResults\">Job Results</a>\
	                    <ul>\
	                        <li><a href=\"/gp/jobResults\">Results Summary</a></li>\
	                    </ul>\
	                </li>\
	                <li><a href=\"/gp/pages/index.jsf?splash=resources\">Resources</a>\
	                    <ul>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/gp_mail.html')\">Mailing List</a></li>\
	                        <li><a href=\"/gp/pages/contactUs.jsf\">Report Bugs</a></li>\
	                        <li><a href=\"/gp/pages/contactUs.jsf\">Contact Us</a></li>\
	                    </ul>\
	                </li>\
	                <li><a href=\"/gp/pages/index.jsf?splash=downloads\">Downloads</a>\
	                    <ul>\
	                        <li><a href=\"/gp/pages/downloadProgrammingLibaries.jsf\">Programming Languages</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/datasets/')\">Public Datasets</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/software/gparc/')\">GParc</a></li>\
	                    </ul>\
	                </li>\
	                <li class=\"adminServerAllowed\"><a href=\"/gp/pages/serverSettings.jsf\">Administration</a>\
	                    <ul>\
	                        <li><a href=\"/gp/pages/serverSettings.jsf\">Server Settings</a></li>\
	                    </ul>\
	                </li>\
	                <li><a href=\"/gp/pages/index.jsf\">Help</a>\
	                    <ul>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_tutorial.html')\">Tutorial</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/desc/videos')\">Video Tutorial</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_web_client.html')\">User Guide</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_programmer.html')\">Programmers Guide</a></li>\
	                        <li><a href=\"/gp/getTaskDoc.jsp\">Module Documentation</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_fileformats.html')\">File Formats</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/current/')\">Release Notes</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/doc/faq/')\">FAQ</a></li>\
	                        <li><a href=\"/gp/pages/about.jsf\">About</a></li>\
	                    </ul>\
	                </li>\
	                <li class=\"genomeSpaceMenu\">\
	                	<a href=\"/gp/pages/index.jsf\"><img src=\"/gp/pages/genomespace/genomespace_icon.gif\" class=\"genomeSpaceIcon\" alt=\"GenomeSpace\"></img>GenomeSpace</a>\
	                    <ul>\
	                        <li class=\"genomeSpaceLoggedOut\"><a href=\"/gp/pages/genomespace/signon.jsf\">Login</a></li>\
	                        <li class=\"genomeSpaceLoggedIn\"><a href=\"/gp/pages/genomespace/signon.jsf\">Logout</a></li>\
	                        <li class=\"genomeSpaceLoggedOut\"><a href=\"/gp/pages/genomespace/userRegistration.jsf\">Register</a></li>\
	                        <li class=\"genomeSpaceLoggedIn\"><a href=\"/gp/pages/genomespace/privateTool.jsf\">Add as Private Tool</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('https://gsui.genomespace.org/jsui/')\">GenomeSpace UI</a></li>\
	                        <li><a href=\"JavaScript:Menu.go('http://www.genomespace.org/')\">About</a></li>\
	                    </ul>\
	                </li>\
	            </ul>\
	            <br style=\"clear: left\"/>\
	        </div>"
		);
		this.jquery("body").append(menu);
		
		// Initialize the menu
	    ddsmoothmenu.init({
	        mainmenuid: "navband1", //menu DIV id
	        orientation: 'h', //Horizontal or vertical menu: Set to "h" or "v"
	        classname: 'ddsmoothmenu', //class added to menu's outer DIV
	        contentsource: "markup" //"markup" or ["container_id", "path_to_menu_file"]
	    });
	    
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
	}
};