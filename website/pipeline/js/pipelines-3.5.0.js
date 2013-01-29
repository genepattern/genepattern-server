/*
 * Copyright 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

/**
 * JavaScript used by the GenePattern Pipeline Editor
 * @requires jQuery, jQuery UI, jQuery Tools, jsPlumb
 * @author Thorin Tabor
 */

/**
 * Object representing the pipeline editor, containing all associated methods and properties
 */
var editor = {
    USE_BETA_OPTIONS: false,
	div: "workspace",		// The id of the div used for pipeline editing
    titleSpan: "titleSpan", // The id of the title span of the pipeline
    loading: false,
	workspace: {			// A map containing all the instance data of the current workspace
		idCounter: 0, 		// Used to keep track of module instance IDs
        fileCounter: 1000,  // Used to keep track of file IDs
        dirty: true,        // Used to determine if something has changed since last save
		pipes: [],	        // A list of all current connections in the workspace
        files: [],          // List of all uploaded files, used when saving or uploading
		suggestRow: 0, 		// Used by the GridLayoutManager
		suggestCol: 0,		// Used by the GridLayoutManager

        pipelineName: "UntitledPipeline" + Math.floor(Math.random() * 10000),
        pipelineDescription: "",
        pipelineAuthor: "",
        pipelinePrivacy: "private",
        pipelineVersion: "0",
        pipelineVersionComment: "",
        pipelineDocumentation: "",
        pipelineLicense: "",
        pipelineLsid: ""
	},

	init: function() {
		jsPlumb.Defaults.Connector = ["Bezier", { curviness:50 }];
		jsPlumb.Defaults.DragOptions = { cursor: "pointer", zIndex:2000 };
		jsPlumb.Defaults.PaintStyle = { strokeStyle:"#666699", lineWidth:2 };
		jsPlumb.Defaults.EndpointStyle = { radius:9, fillStyle:"black" };
		jsPlumb.Defaults.Anchors = ["BottomCenter", "TopCenter"];
		jsPlumb.Defaults.Overlays = [[ "Arrow", { width: 13, length: 13, location: 0.92, id: "arrow" } ]];
		jsPlumb.Defaults.MaxConnections = -1;

		jsPlumb.bind("jsPlumbConnection", function(event) {
            // Protect from unwanted firing during loading pipelines
            if (editor.loading) { return; }

            var outputModule = editor.getParentModule(event.sourceEndpoint.canvas);
            var outputPort = editor.getParentPort(event.sourceEndpoint.canvas);
            outputPort = outputModule.getCorrectOutput(outputPort);

            var inputPort = editor.getParentPort(event.targetEndpoint.canvas);
            inputPort.detachAll();

            editor.loading = true;
            var connection = editor.addConnection(outputPort, inputPort);
            editor.loading = false;

            var pipe = new Pipe(connection);
            editor.initPipe(pipe);

            properties.redrawDisplay();
		});

        editor._setPipelineName();
        $("#pipelinePencil").click(function(event) {
            properties.displayPipeline();
            properties.show();
            event.stopPropagation();
        });
        
        $(window).resize(function() {
        	// Hack to get layout to work correctly in Firefox
            if ($.browser.mozilla) {
            	var height = $(document).height() - 142;
            	$("#library").height(height);
            }
        });

        window.onbeforeunload = function (event) {
            // If the workspace is not dirty then do not prompt for confirmation
            if (!editor.workspace.dirty) { return; }

            //noinspection JSDuplicatedDeclaration
            var message = "If you leave all pipeline changes will be lost.", event = event || window.event;
            // For IE and Firefox
            if (event) { event.returnValue = message; }
            // For Safari
            return message;
        };
	},

	removeFile: function(filename) {
		for (var i = 0; i < editor.workspace["files"].length; i++) {
			var filepath = editor.workspace["files"][i];
			if (filepath.indexOf("/" + filename) + filename.length + 1 >= filepath.length) { // If the filepath ends with the filename
				editor.workspace["files"].splice(i, 1);
				return;
			}
		}
	},
	
	pwrCount: function() {
		var count = 0;
        for (var i in editor.workspace) {
            var module = editor.workspace[i];
            if (module instanceof Module) {
                for (var j in module.fileInputs) {
                    var input = module.fileInputs[j];
                    if (input.isPWR()) {
                        count++;
                    }
                }
            }
        }

        return count;
	},
	
	// Fix for the GP 3.3.3 clone bug
    fixCloneBug: function(path) {
        if (path.indexOf("<GenePatternURL>") !== -1 && path.indexOf("<LSID>") === -1) {
        	var parts = path.split("=");
        	if (parts.length !== 3) return path;
        	return parts[0] + "=<LSID>&file=" + parts[2];
        }
        else {
        	return path;
        }
    },

    revert: function() {
    	var buttons = {
            "Continue": function() {
                $(this).dialog("close");
                
                var lsid = editor.workspace["pipelineLsid"];
                var name = editor.workspace["pipelineName"];
                editor.load(lsid);
                editor.workspace["pipelineName"] = name;
                editor._setPipelineName();
                properties.displayPipeline();
                properties.show();
                
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            },
            "Cancel": function() {
                $(this).dialog("close");
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            }};
        editor.showDialog("Confirm Revert", "This will revert the pipeline back to its last saved state.  Continue?", buttons);
    },

    updateAllPorts: function() {
        for (var i in editor.workspace) {
            var module = editor.workspace[i];
            if (module instanceof Module) {
                for (var j = 0; j < module.inputEnds.length; j++) {
                    var input = module.inputEnds[j];
                    input.updateIcon();
                }
            }
        }
    },

    isReservedFilename: function(name) {
        if (name === ".pipelineDesigner") {
            return true;
        }
        if (name === "manifest") {
            return true;
        }
        if (name === ".DS_Store") {
            return true;
        }
        if (name === "Thumbs.db") {
            return true;
        }
        if (name === "desktop.ini") {
            return true;
        }

        // If not found, return false
        return false;
    },

    filenameExists: function(name) {
        // Check documentation
        if (name === editor.workspace["pipelineDocumentation"]) {
            return true;
        }
        
        // Check license
        if (name === editor.workspace["pipelineLicense"]) {
            return true;
        }

        // Check all attached files
        for (var i in editor.workspace) {
            var module = editor.workspace[i];
            if (module instanceof Module && module.isFile() && module.getFilename() === name) {
                return true;
            }
        }

        // If not found, return false
        return false;
    },

    getLastFile: function() {
        var id = editor.workspace.fileCounter - 1;
        return editor.workspace[id];
    },

    expandIfNeeded: function(module) {
        // Expand the workspace so that modules do not get lost beneath the editor
        var PROPERTIES_WIDTH = 300;
        var MODULE_WIDTH = 195;
        var bodyWidth = $(document).width();
        if ($(module.ui).position().left + MODULE_WIDTH > bodyWidth - PROPERTIES_WIDTH) {
            $(".tableDiv").width(bodyWidth + PROPERTIES_WIDTH + 20);
            if (!editor.loading)
            	$("html, body").animate({ scrollLeft: $(module.ui).position().left - 100 }, "slow");
        }
    },

    relocateIfNeeded: function(module) {
        var LIBRARY_WIDTH = 190;
        var HEADER_HEIGHT = 112;

        var repaint = false;

        if ($(module.ui).position().left < LIBRARY_WIDTH) {
            module.ui.style.left = (LIBRARY_WIDTH + 30) + "px";
            repaint = true;
        }

        if ($(module.ui).position().top < HEADER_HEIGHT) {
            module.ui.style.top = (HEADER_HEIGHT + 20) + "px";
            repaint = true;
        }

        if (repaint) {
            jsPlumb.repaintEverything();
        }
    },

    errorCount: function() {
        var count = 0;
        for (var i in editor.workspace) {
            var module = editor.workspace[i];
            if (module instanceof Module) {
                for (var j in module.alerts) {
                    var alert = module.alerts[j];
                    if (alert.level === alert.ERROR) {
                        count++;
                    }
                }
            }
        }

        return count;
    },

    alertCount: function() {
        var count = 0;
        for (var i in editor.workspace) {
            var module = editor.workspace[i];
            if (module instanceof Module) {
                for (var j in module.alerts) {
                    var alert = module.alerts[j];
                    if (alert.level === alert.WARNING) {
                        count++;
                    }
                }
            }
        }

        return count;
    },

    highlightModuleAlerts: function() {
        var foundError = false;

        for (var i in editor.workspace) {
            var module = editor.workspace[i];
            if (module instanceof Module) {
                $(module.ui).removeClass("errorModule");
                $(module.ui).removeClass("alertModule");
                module.checkForWarnings();
                var highest = module.highestAlert();
                if (highest === null) {
                    continue;
                }

                if (highest.level === highest.ERROR) {
                    $(module.ui).addClass("errorModule");
                    foundError = true;
                }

                if (highest.level === highest.WARNING) {
                    $(module.ui).addClass("alertModule");
                    foundError = true;
                }
            }
        }

        return foundError;
    },

    extractFilename: function(path) {
        // Handle chrome upload paths
        if (path.indexOf("\\") > -1) {
            //noinspection JSDuplicatedDeclaration
            var parts = path.split("\\");
            return parts[parts.length -1]
        }

        // Handle URLs
        //noinspection JSDuplicatedDeclaration
        var parts = path.split("/");
        var endPart = parts[parts.length -1];
        if (endPart.indexOf("=") < 0) { return endPart; }

        // Handle GenePattern URLs
        parts = endPart.split("=");
        return parts[parts.length - 1];
    },

    getFileBox: function(path) {
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module && editor.workspace[i].isFile() && editor.workspace[i].lsid === path) {
                return editor.workspace[i];
            }
        }

        return null;
    },

    loadFile: function(name, path, top, left) {
        // Mark the workspace as dirty
        editor.makeDirty();

        var file = new File(name, path);
        file.id = editor._nextFileId();
        this.workspace[file.id] = file;
        file.add(top, left);
        return file;
    },

    addFile: function(name, path) {
        return editor.loadFile(name, path, null, null);
    },
	
	foundInList: function(list, obj) {
	    var i;
	    for (i = 0; i < list.length; i++) {
	        if (list[i] === obj) {
	            return true;
	        }
	    }

	    return false;
	},

    _extractPortFromPipe: function(pipeName) {
        var parts = pipeName.split("_");

        if (parts.length < 3) {
            editor.log("Invalid pipe name given to editor._extractPortFromPipe(): " + pipeName);
            return null;
        }

        var moduleName = parts[1];
        var module = editor.workspace[moduleName];
        var portName = parts[2];
        return module.getPort(portName);
    },

    log: function(message) {
        //noinspection JSCheckFunctionSignatures
        console.log(message);
    },

    makeDirty: function() {
        this.workspace.dirty = true;
    },

    makeClean: function() {
        this.workspace.dirty = false;
    },

    protectAgainstUndefined: function(value) {
        if (value !== undefined) return value;
        else return "";
    },

    isInsideDialog: function(element) {
        if ($(element).hasClass("ui-dialog")) {
            return true;
        }
        else if ($(element).hasClass("ui-widget-overlay") && !$(element).hasClass("properties-overlay")) {
            return true;
        }
        else {
            var parent = element.parentNode;
            if (parent === null || parent === undefined) {
                return false;
            }
            else {
                return editor.isInsideDialog(parent);
            }
        }
    },

    showDialog: function(title, message, button) {
        var alert = document.createElement("div");

        if (typeof(message)=='string') {
            alert.innerHTML = message;
        }
        else {
            alert.appendChild(message);
        }

        if (button === undefined || button === null) {
            button = { "OK": function(event) {
                $(this).dialog("close");
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            }};
        }

        $(alert).dialog({
            modal: true,
            dialogClass: "top-dialog",
            width: 400,
            title: title,
            buttons: button,
            close: function() {
                $(this).dialog("destroy");
                $(this).remove();
            }
        });

        // Fix z-index for dialog
        var z = parseInt($(alert).parent().css("z-index"));
        if (z < 10000) {
            z += 9000;
            $(".top-dialog").css("z-index", z);
        }
        
        return alert;
    },
    
    showOverlay: function() {
    	// Init the overlay if not already created
    	if ($('#propertiesOverlay').length == 0) {
    		$('<div id="propertiesOverlay" class="ui-widget-overlay properties-overlay"></div>').appendTo('body');
        	$('.ui-widget-overlay').width($(document).width());
            $('.ui-widget-overlay').height($(document).height());

            $(window).resize(function() {
            	$('.ui-widget-overlay').width($(document).width());
                $('.ui-widget-overlay').height($(document).height());
            });
    	}
    	else {
    		$('#propertiesOverlay').show();
    	}	
    },
    
    hideOverlay: function() {
    	$('#propertiesOverlay').hide();	
    },

    updateProgressBar: function(setToValue) {
        if (setToValue === undefined || setToValue == null) {
            var progressSoFar = $("#loadingEditorProgressBar").progressbar("value");
            $("#loadingEditorProgressBar").progressbar({ value: progressSoFar + 10 });
        }
        else {
            $("#loadingEditorProgressBar").progressbar({ value: setToValue });
        }
    },

    loadPipelineIfAsked: function() {
        var lsid = editor._loadGetLsid();
        if (lsid !== null) {
            editor.load(lsid);
        }
        properties.displayPipeline();
        properties.show();
    },

    _loadGetLsid: function() {
        var lsid = "lsid";
        var regexS = "[\\?&]" + lsid + "=([^&#]*)";
        var regex = new RegExp(regexS);
        var results = regex.exec(window.location.search);
        if (results == null) { return null; }
        else { return decodeURIComponent(results[1].replace(/\+/g, " ")); }
    },

    _cleanWorkspace: function() {
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                editor.workspace[i].remove();
            }
        }

        this.workspace = {      // A map containing all the instance data of the current workspace
            idCounter: 0, 		// Used to keep track of module instance IDs
            fileCounter: 1000,  // Used to keep track of file IDs
            pipes: [],	        // A list of all current connections in the workspace
            files: [],          // List of all uploaded files, used when saving or uploading
            suggestRow: 0, 		// Used by the GridLayoutManager
            suggestCol: 0,		// Used by the GridLayoutManager

            pipelineName: "UntitledPipeline" + Math.floor(Math.random() * 10000),
            pipelineDescription: "",
            pipelineAuthor: "",
            pipelinePrivacy: "private",
            pipelineVersion: "0",
            pipelineVersionComment: "",
            pipelineDocumentation: "",
            pipelineLicense: "",
            pipelineLsid: ""
        };

        editor.makeDirty();
    },

    _setPipelineName: function() {
        $("#" + this.titleSpan).html(this.workspace["pipelineName"] + ((this.workspace["pipelineVersion"] === "0") ? "" : (" v" + this.workspace["pipelineVersion"])));
    },

    addPipe: function(newIn, newOut) {
        var connection = editor.addConnection(newOut, newIn);
        var newPipe = new Pipe(connection);
        newIn.connectPipe(newPipe);
        newOut.connectPipe(newPipe);

        // Make not draggable
        newIn.disableDrag();

        editor.workspace["pipes"].push(newPipe);
        return newPipe;
    },

    initPipe: function(pipe) {
        var input = pipe.inputPort;
        var output = pipe.outputPort;

        // If the pipe was drawn to the same module or a module upstream, cancel the pipe
        if (output.module === input.module) {
            output.detachAll();
            return;
        }
        if (output.module.isUpstream(input.module)) {
            output.detachAll();
            return;
        }

        // If the new input is already prompt when run, make it not PWR
        input.param.makeNotPWR();

        // If the output is from a file, set the value
        if (output.module.isFile()) {
            input.param.value = output.module.getFilename();
        }

        // Make not draggable
        input.disableDrag();

        input.param.makeUsed();

        input.connectPipe(pipe);
        output.connectPipe(pipe);

        editor.workspace["pipes"].push(pipe);

        // Mark the workspace as dirty
        editor.makeDirty();
	},

	_nextId: function() {
		this.workspace["idCounter"]++;
		return this.workspace["idCounter"] - 1;
	},

    _nextFileId: function() {
        this.workspace["fileCounter"]++;
        return this.workspace["fileCounter"] - 1;
    },

	// Takes a module child id in the form of "prefix_moduleid" and returns the module id.
	_extractModuleId: function(element) {
		var parts = element.split("_");
		return parts [parts.length - 1];
	},

	// Takes a module child element or child element id and returns the parent module
	getParentModule: function(element) {
		if (element.constructor != String) {
			element = element.id;
		}
		if (element === null) {
			editor.log("getParentModule() received null value");
			return null;
		}
		var id = this._extractModuleId(element);
        return editor.workspace[id];
	},

	// Takes a port child id in the form of "prefix_portid_moduleid" and returns the port id.
	_extractPortId: function(element) {
		var parts = element.split("_");
		return parts [parts.length - 2];
	},

	// Takes a port child or child element id and returns the parent port
	getParentPort: function(element) {
		if (element.constructor != String) {
			element = element.id;
		}
		if (element === null) {
            editor.log("getParentPort() received null value");
			return null;
		}
		var moduleId = this._extractModuleId(element);
		var module = editor.workspace[moduleId];
		var portId = this._extractPortId(element);
        return module.getPort(portId);
	},

    replaceModule: function(moduleId, lsid) {
        var oldModule = editor.workspace[moduleId];
        var top = oldModule.ui.style.top;
        var left = oldModule.ui.style.left;

        // Create the new module
        var newModule = editor.addModuleAtLocation(lsid, top, left);

        // Copy over parameter values from the old module to the new module as best we can
        for (var i = 0; i < oldModule.inputs.length; i++) {
            if (i < newModule.inputs.length && oldModule.inputs[i].name == newModule.inputs[i].name) {
                newModule.inputs[i].value = oldModule.inputs[i].value;
                newModule.inputs[i].promptWhenRun = oldModule.inputs[i].promptWhenRun;
            }
        }

        // Remove the old module
        oldModule.remove();

        return newModule;
    },

    removeAllModules: function() {
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                editor.workspace[i].remove();
            }
        }
    },

	removeModule: function(id) {
		delete editor.workspace[id];
	},

	removePipe: function(pipe) {
		for (var i = 0; i < this.workspace["pipes"].length; i++) {
			if (this.workspace["pipes"][i] == pipe) {
                editor.workspace["pipes"].splice(i, 1);
				return
			}
		}
        editor.log("ERROR: Attempted to remove pipe not found in the workspace");
	},

    _addModule: function(lsid, id, top, left) {
        var module = library.modules[lsid];
        if (module === undefined || module === null) {
            editor.log("Error adding module: " + lsid);
            return null;
        }
        var spawn = module.spawn();
        spawn.id = id;
        this.workspace[spawn.id] = spawn;
        spawn.add(top, left);
        return spawn;
    },

    _addBlackBoxModule: function(module, id, top, left) {
        module.id = id;
        this.workspace[module.id] = module;
        module.add(top, left);
        module.checkForWarnings();
        return module;
    },

    loadModule: function(lsid, id, top, left) {
        if (top === undefined) { top = null; }
        if (left === undefined) { left = null; }
        return this._addModule(lsid, id, top, left);
    },

	addModule: function(lsid) {
        // Mark the workspace as dirty
        editor.makeDirty();

        return this._addModule(lsid, this._nextId(), null, null);
	},

    addModuleAtLocation: function(lsid, top, left) {
        return this._addModule(lsid, this._nextId(), top, left);
    },

    addModuleByName: function(name) {
        var module = null;
        for (var i in library.modules) {
            if (library.modules[i].name == name) {
                module = library.modules[i];
                break;
            }
        }
        if (module === null) {
            editor.log("Error adding module: " + name);
            return;
        }
        var spawn = module.spawn();
        spawn.id = this._nextId();
        this.workspace[spawn.id] = spawn;
        spawn.add(null, null);
        return spawn;
    },

	addConnection: function(source, target) {
		var connection = jsPlumb.connect({"source": source.endpoint, "target": target.endpoint});
        connection.canvas.setAttribute("name", "pipe_" + target.module.id + "_" + target.id);
        return connection;
	},

    modulesInWorkspace: function() {
        var count = 0;
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                count++;
            }
        }

        return count;
    },
    
    _getLastPositionedModule: function() {
    	var selected = null;
    	
    	for (var i in editor.workspace) {
    		var module = editor.workspace[i];
            if (module instanceof Module && !module.isFile()) {
                if (selected === null) {
                	if ($(module.ui).position().left !== 0 && $(module.ui).position().top !== 0) {
                		selected = module;
                	}   	
                	continue;
                }
                if ($(module.ui).position().left > $(selected.ui).position().left) {
                	selected = module;
                	continue;
                }
                if ($(module.ui).position().left === $(selected.ui).position().left) {
                	if ($(module.ui).position().top > $(selected.ui).position().top) {
                		selected = module;
                	}
                }
            }
        }
    	
    	return selected;
    },
    
    _getLastPositionedFile: function() {
    	var selected = null;
    	
    	for (var i in editor.workspace) {
    		var module = editor.workspace[i];
            if (module instanceof Module && module.isFile()) {
                if (selected === null) {
                	if ($(module.ui).position().left !== 0 && $(module.ui).position().top !== 0) {
                		selected = module;
                	}   	
                	continue;
                }
                if ($(module.ui).position().left > $(selected.ui).position().left) {
                	selected = module;
                	continue;
                }
                if ($(module.ui).position().left === $(selected.ui).position().left) {
                	if ($(module.ui).position().top > $(selected.ui).position().top) {
                		selected = module;
                	}
                }
            }
        }
    	
    	return selected;
    },
    
    _dynamicLayoutManager: function(module) {
    	var WIDTH = 195;
        var MARGIN = 40;
        var NON_FILE_TOP_MARGIN = 80;
        var EXTRA_TOP_MARGIN = 110;
        var EXTRA_LEFT_MARGIN = 200;
        
        var lastModule = null;
        
        if (module.isFile()) {						// Handle Files
        	lastModule = editor._getLastPositionedFile();
    		if (lastModule === null) {
    			return { "top": MARGIN + EXTRA_TOP_MARGIN, "left": MARGIN  + EXTRA_LEFT_MARGIN };
    		}
    		else {
    			return { "top": MARGIN + EXTRA_TOP_MARGIN, "left": MARGIN + WIDTH + $(lastModule.ui).position().left };
    		}
    		
    	}
    	else if (module.isVisualizer()) {			// Handle Visualizers
    		lastModule = editor._getLastPositionedModule();
    		if (lastModule === null) {
    			return { "top": MARGIN + EXTRA_TOP_MARGIN + NON_FILE_TOP_MARGIN, "left": MARGIN  + EXTRA_LEFT_MARGIN };
    		}
    		else {
    			return { "top": $(lastModule.ui).position().top + MARGIN + lastModule.calculateHeight(), "left": $(lastModule.ui).position().left };
    		}	
    	}
    	else {										// Handle Other Modules
    		lastModule = editor._getLastPositionedModule();
    		if (lastModule === null) {
    			return { "top": MARGIN + EXTRA_TOP_MARGIN + NON_FILE_TOP_MARGIN, "left": MARGIN  + EXTRA_LEFT_MARGIN };
    		}
    		else {
    			return { "top": MARGIN + EXTRA_TOP_MARGIN + NON_FILE_TOP_MARGIN, "left": MARGIN + WIDTH + $(lastModule.ui).position().left };
    		}
    	}
    },

    _p2LayoutManager: function(module) {
        var WIDTH = 195;
        var MARGIN = 40;
        var NON_FILE_TOP_MARGIN = 80;
        var EXTRA_TOP_MARGIN = 110;
        var EXTRA_LEFT_MARGIN = 200;

        // Determine if this is the first module in the layout
        var firstModule = editor.modulesInWorkspace() <= 1;

        // Create the JSON object to return
        var toReturn = { "top": MARGIN + (module.isFile() ? EXTRA_TOP_MARGIN : EXTRA_TOP_MARGIN + NON_FILE_TOP_MARGIN), "left": MARGIN  + EXTRA_LEFT_MARGIN};

        // If this is the first module, please it at the top
        if (firstModule) {
            this.workspace.suggestRow = MARGIN + (module.isFile() ? EXTRA_TOP_MARGIN : EXTRA_TOP_MARGIN + NON_FILE_TOP_MARGIN);
            this.workspace.suggestCol = MARGIN + EXTRA_LEFT_MARGIN;

            // Update for new estimated height
            if (module.isFile()) {
                this.workspace.suggestRow = EXTRA_TOP_MARGIN + MARGIN + NON_FILE_TOP_MARGIN;
            }
            else {
                this.workspace.suggestRow += module.calculateHeight() + MARGIN;
            }

            return toReturn;
        }

        // Determine if this module goes below or beside the last one
        var below = false;
        if (module.isVisualizer() || (this.workspace.suggestRow === EXTRA_TOP_MARGIN + MARGIN + NON_FILE_TOP_MARGIN && !module.isFile())) {
            below = true;
        }

        // Update the appropriate position
        if (below) {
            toReturn.top = this.workspace.suggestRow;
            toReturn.left = this.workspace.suggestCol;

            // Update for new estimated height
            this.workspace.suggestRow += module.calculateHeight() + MARGIN;
        }
        else {
            this.workspace.suggestRow = MARGIN + (module.isFile() ? EXTRA_TOP_MARGIN : EXTRA_TOP_MARGIN + NON_FILE_TOP_MARGIN);
            this.workspace.suggestCol += WIDTH + MARGIN;

            toReturn.top = this.workspace.suggestRow;
            toReturn.left = this.workspace.suggestCol;

            // Update for new estimated height
            if (module.isFile()) {
                this.workspace.suggestRow = EXTRA_TOP_MARGIN + MARGIN + NON_FILE_TOP_MARGIN;
            }
            else {
                this.workspace.suggestRow += module.calculateHeight() + MARGIN;
            }
        }

        return toReturn;
    },

	suggestLocation: function(module) {
		// Pick your layout manager
        return this._dynamicLayoutManager(module);
	},

    _makePipelineNameValid: function(string) {
    	var newName = string.replace(/[^a-zA-Z _0-9.]+/g, "");
        newName = newName.replace(/ /g, ".");
        if (/^\d+/.test(newName)) {
            newName = "Pipeline." + newName;
        }
        if (/^\.+/.test(newName)) {
            newName = "Pipeline" + newName;
        }
        if (newName == "") {
            newName = "Pipeline" + newName;
        }
        return newName;
    },

    saveProps: function(save) {
        this.workspace["pipelineName"] = this._makePipelineNameValid(save["Pipeline Name"]);
        this.workspace["pipelineDescription"] = save["Description"];
        this.workspace["pipelineAuthor"] = save["Author"];
        this.workspace["pipelinePrivacy"] = save["Privacy"];
        this.workspace["pipelineVersionComment"] = save["Version Comment"];
        this.workspace["pipelineDocumentation"] = save["Documentation"];
        this.workspace["pipelineLicense"] = save["License"];
        editor._setPipelineName();

        // Redisplay pipeline editor
        properties.displayPipeline();
        properties.show();

        // Mark the workspace as dirty
        editor.makeDirty();
    },

    _pipelineTransport: function() {
        var transport = {};
        transport["pipelineName"] = this.workspace["pipelineName"];
        transport["pipelineDescription"] = this.workspace["pipelineDescription"];
        transport["pipelineAuthor"] = this.workspace["pipelineAuthor"];
        transport["pipelinePrivacy"] = this.workspace["pipelinePrivacy"];
        transport["pipelineVersion"] = this.workspace["pipelineVersion"];
        transport["pipelineVersionComment"] = this.workspace["pipelineVersionComment"];
        transport["pipelineDocumentation"] = this.workspace["pipelineDocumentation"];
        transport["pipelineLicense"] = this.workspace["pipelineLicense"];
        transport["pipelineLsid"] = this.workspace["pipelineLsid"];
        transport["pipelineFiles"] = this.workspace["files"];
        return transport;
    },

    _pipeTransport: function() {
        var transport = {};
        var pipes = editor.workspace["pipes"];
        for (var i = 0; i < pipes.length; i++) {
            var pipe = pipes[i];

            // Transport only real connections between modules
            if (!pipe.outputModule.isFile()) {
                transport[i] = pipes[i].prepTransport();
            }
        }
        return transport;
    },

    _fileTransport: function() {
        var transport = {};
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                // Transport only files
                if (editor.workspace[i].isFile()) {
                    transport[i] = editor.workspace[i].prepTransport();
                }
            }
        }
        return transport;
    },

    _moduleTransport: function() {
        var transport = {};
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                // Transport only true modules
                if (!editor.workspace[i].isFile()) {
                    transport[i] = editor.workspace[i].prepTransport();
                }
            }
        }
        return transport;
    },

    _bundleForSave: function() {
        var json = {};
        json["pipeline"] = editor._pipelineTransport();
        json["pipes"] = editor._pipeTransport();
        json["modules"] = editor._moduleTransport();
        json["files"] = editor._fileTransport();
        return json;
    },

    _loadPipeline: function(pipeline) {
        this.workspace["pipelineName"] = pipeline["pipelineName"];
        this.workspace["pipelineDescription"] = pipeline["pipelineDescription"];
        this.workspace["pipelineAuthor"] = pipeline["pipelineAuthor"];
        this.workspace["pipelinePrivacy"] = pipeline["pipelinePrivacy"];
        this.workspace["pipelineVersion"] = pipeline["pipelineVersion"];
        this.workspace["pipelineVersionComment"] = pipeline["pipelineVersionComment"];
        this.workspace["pipelineDocumentation"] = pipeline["pipelineDocumentation"];
        this.workspace["pipelineLicense"] = pipeline["pipelineLicense"];
        this.workspace["pipelineLsid"] = pipeline["pipelineLsid"];
        editor._setPipelineName();
    },

    _loadFiles: function(files) {
        this.removeAllModules();

        for (var i in files) {
            // Update the idCounter as necessary
            var file = files[i];

            // Set the top and left position, if available
            var top = null;
            var left = null;
            if (file.top !== undefined && file.top !== null) {
                top = file.top;
            }
            if (file.left !== undefined && file.left !== null) {
                left = file.left;
            }

            // Add each file as it is read
            this.loadFile(file.name, file.path, top, left);
        }
    },

    _loadModules: function(modules) {
//        var givenAlert = false;

        var i = 0;
        while (modules[i.toString()] !== undefined) {
            // Update the idCounter as necessary
            var module = modules[i.toString()];
            var intId = parseInt(module.id);
            if (intId >= this.workspace["idCounter"]) { this.workspace["idCounter"] = intId + 1; }

            // Set the top and left position, if available
            var top = null;
            var left = null;
            if (module.top !== undefined && module.top !== null) {
                top = module.top;
            }
            if (module.left !== undefined && module.left !== null) {
                left = module.left;
            }

            // Add each module as it is read
            var added = this.loadModule(module.lsid, module.id, top, left);

            if (added === null) {
                self.location="/gp/viewPipeline.jsp?name=" + editor.workspace["pipelineLsid"];
                editor.makeClean();
                throw "Stop Loading";

                // Old handling of modules that are not installed
//                added = this._addBlackBoxModule(library._createBlackBoxModule(module.name, module.lsid), module.id, module.top, module.left);
//                if (!givenAlert) {
//                    editor.showDialog("Problem Loading Pipeline", "Unable to load one or more of the modules in the pipeline.  " +
//                        "You may view what the Pipeline Designer could load of the pipeline, but will be unable to save this pipeline.");
//                    $("#saveButton").button("disable");
//                    $("#runButton").button("disable");
//                    givenAlert = true;
//                }
//                i++;
//                continue;
            }

            // Set the correct properties for the module
            added.loadProps(module);

            i++;
        }
    },

    _loadPipes: function(pipes) {
        for (var i in pipes) {
            var outputModule = editor.workspace[pipes[i]["outputModule"]];
            var inputModule = editor.workspace[pipes[i]["inputModule"]];
            var outputId = pipes[i]["outputPort"];
            var inputId = pipes[i]["inputPort"];
            
            // Unescape the IDs if necessary
            outputId = $('<div/>').html(outputId).text();
            inputId = $('<div/>').html(inputId).text();

            if (!outputModule.hasPortByPointer(outputId)) {
                outputModule.addOutput(outputId);
            }
            if (!inputModule.hasPortByPointer(inputId)) {
                inputModule.addInput(inputId);
            }

            var outputPort = outputModule.getOutputByPointer(outputId);
            var inputPort = inputModule.getInputByPointer(inputId);

            // Mark the input param as used
            inputModule.getInputByName(inputId).makeUsed(inputPort);

            editor.addPipe(inputPort, outputPort);
        }
    },

    extractBaseLsid: function(lsid) {
        return lsid.substr(0, lsid.lastIndexOf(":"));
    },

    extractLsidVersion: function(lsid) {
        return lsid.substr(lsid.lastIndexOf(":") + 1, lsid.length);
    },

    _cleanAfterSave: function() {
        editor.workspace["pipelineVersionComment"] = "";
        editor.workspace["files"] = [];
        editor._setPipelineName();
        editor.makeClean();
    },

    _validatePipeline: function() {
        for (var i in editor.workspace) {
            var module = editor.workspace[i];

            // Only perform on modules
            if (module instanceof Module) {
                module.checkForWarnings();
            }
        }

        // Provide an alert if this is not the most recent version of the pipeline
        if (!editor._mostRecentVersion()) {
            editor.showDialog("Warning Loading Pipeline", "The pipeline you are editing is not the most recent version of the pipeline available.");
        }
    },

    _mostRecentVersion: function() {
        var highest = library.getHighestVersion(editor.workspace["pipelineLsid"]);
        if (highest !== null) {
            // Check for equal versions
            if (highest.version === editor.workspace["pipelineVersion"]) {
                return true;
            }
            else {
                return library.higherVersion(editor.workspace["pipelineVersion"], highest.version);
            }
        }
        else {
            return true;
        }
    },

    runPipeline: function(lsid) {
        self.location="/gp/pages/index.jsf?lsid=" + lsid;
    },
    
    enableRevert: function() {
    	$("#revertButton").button({disabled: false});
    },

	load: function(lsid) {
        editor.loading = true;

        if (lsid === undefined || lsid === null || lsid === "") {
            editor._cleanWorkspace();
            editor._setPipelineName();
            return;
        }

        $.ajax({
            type: "POST",
            url: "/gp/PipelineDesigner/load",
            data: { "lsid" : lsid },
            success: function(response) {
                var error = response["ERROR"];
                if (error !== undefined) {
                    editor.showDialog("Error Loading Pipeline", error);
                }
                else {
                    editor._cleanWorkspace();
                    editor._loadPipeline(response["pipeline"]);
                    editor._loadFiles(response["files"]);
                    editor._loadModules(response["modules"]);
                    editor._loadPipes(response["pipes"]);
                    setTimeout("editor.updateAllPorts()", 300);
                    editor._validatePipeline();
                    editor.enableRevert();
                    editor.makeClean();
                }

                properties.displayPipeline();
                editor.loading = false;
            },
            dataType: "json"
        });
	},

	save: function(runImmediately, ignorePrompts) {
		if (ignorePrompts === undefined || ignorePrompts === null) {
			ignorePrompts = {};
		}
		
        // Check for errors
        if (!ignorePrompts["warn"] && !editor.confirmErrors(runImmediately, ignorePrompts)) {
            return;
        }

        // Check for whether the pipeline is Untitled
        if (!ignorePrompts["untitled"] && !editor.confirmIfUntitled(runImmediately, ignorePrompts)) {
            return;
        }

        // Check for pipelines with the same name
        if (!ignorePrompts["sameName"] && !editor.confirmUniqueName(runImmediately, ignorePrompts)) {
            return;
        }
        
        // Check for saving a read-only pipeline
        if (!ignorePrompts["readOnly"] && !editor.confirmClone(runImmediately, ignorePrompts)) {
            return;
        }

		var toSend = editor._bundleForSave();
        $.ajax({
            type: "POST",
            url: "/gp/PipelineDesigner/save",
            data: { "bundle" : JSON.stringify(toSend) },
            success: function(response) {
                var message = response["MESSAGE"];
                var error = response["ERROR"];

                if (error !== undefined && error !== null) {
                    editor.showDialog("ERROR", "<div style='text-align: center; font-weight: bold;'>" + error + "</div>");
                    return;
                }

                var newLsid = response["lsid"];
                var newVersion = editor.extractLsidVersion(newLsid);
                var oldLsid = editor.workspace["pipelineLsid"];
                
                // Update the LSID upon successful save
                if (newLsid !== undefined && newLsid !== null) {
                    editor.workspace["pipelineLsid"] = newLsid;
                    editor.workspace["pipelineVersion"] = editor.extractLsidVersion(newLsid);
                    editor.workspace["pipelineVersionComment"] = "";
                    editor._updateHistoryOnSave();
                    editor.enableRevert();
                    editor._cleanAfterSave();
                }

                if (runImmediately && (error === undefined || error === null)) {
                    editor.runPipeline(newLsid);
                    return;
                }
                
                // Prompt for updating dependent pipelines
                var dependents = response["dependents"];
                if (dependents !== null && dependents !== undefined) {
                	var dialogString = "<div>The pipeline you just saved is embedded in one or more other pipelines. " + 
                    "Would you like to automatically generate a new version of them using the new version of this pipeline?</div>";
	                for (var d in dependents) {
	                	var selected = dependents[d];
	                	if (selected !== null) {
	                		dialogString += "<input type='checkbox' class='updateCheckbox' id='" + selected["lsid"] + "' checked='true' name='" + selected["lsid"] + "'/>" + 
	                		"<label class='updateLabel' for='" + selected["lsid"] + "'>" + selected["name"] + "</label><br/>";
	                	}
	                }
                	
                	editor.showDialog("Pipeline Saved",
                        dialogString, {
                            "Generate Pipelines": function() {
                                editor._updateDependentPipelines(oldLsid, newLsid);
                                $(this).dialog("close");
                                editor._finishSave(message, newVersion, newLsid);
                            },
                            "No Thanks": function() {
                                $(this).dialog("close");
                                editor._finishSave(message, newVersion, newLsid);
                            }
                        });
                	
                	return;
                }
                
                editor._finishSave(message, newVersion, newLsid);
            },
            dataType: "json"
        });
	},
	
	_updateDependentPipelines: function(oldLsid, newLsid) {
		var updateList = [];
		var checkedArray = $(".updateCheckbox[checked=true]");
		checkedArray.each(function(i) {
			var checkbox = $(checkedArray.get(i));
			updateList.push(checkbox.attr("id"));
		});
		
		var bundle = {
			"updateList": updateList,
			"oldLsid": oldLsid,
			"newLsid": newLsid
		};
		
		$.ajax({
            type: "POST",
            url: "/gp/PipelineDesigner/dependents",
            data: { "updates" : JSON.stringify(bundle) },
            success: function(response) {
            	var message = response["MESSAGE"];
                var error = response["ERROR"];
                
            	if (error !== undefined && error !== null) {
                    editor.showDialog("ERROR", "<div style='text-align: center; font-weight: bold;'>" + error + "</div>");
                    return;
                }
            	
            	var changed = response["changed"];
            	var dialogString = "<div>The following pipelines have been updated:</div>";
            	for (var i in changed) {
            		var selected = changed[i];
            		if (selected !== null && selected !== undefined) {
            			dialogString += "<div class='updateCallback'>" + selected["name"] + "</div><br/>";
            		}
            	}
            	
            	editor.showDialog("Pipeline Saved", dialogString);
            },
            dataType: "json"
		});
	},
	
	_finishSave: function(message, newVersion, newLsid) {
		// Update the library to include the new module
        $.getJSON("/gp/PipelineDesigner/library", function(data) {
            library.initStructure(data);
            properties.redrawDisplay();
        });
        
        if (message !== undefined && message !== null) {
            editor.showDialog("Pipeline Saved",
                "<div style='text-align: center; font-weight: bold;'>" + message + "<br />Version: " + newVersion + "</div>", {
                    "Run Pipeline": function() {
                        editor.runPipeline(newLsid);
                    },
                    "Close": function() {
                        $(this).dialog("close");
                    }
                });
        }
	},

    _updateHistoryOnSave: function() {
    	history.pushState(null, "GenePattern Pipeline Editor", location.protocol + "//" + location.host + location.pathname + "?lsid=" + editor.workspace["pipelineLsid"]);
    },

    confirmErrors: function(runImmediately, ignorePrompts) {
        var foundErrors = editor.highlightModuleAlerts();

        // If no errors found, return
        if (!foundErrors) {
            return true;
        }

        var errorCount = editor.errorCount();
        var alertCount = editor.alertCount();

        var message = null;
        if (errorCount > 0) {
            message = errorCount + " errors were found in your pipeline.  Please fix before saving.";
        }
        else {
            message = alertCount + " warnings were found in the pipeline.  Are you sure you want to continue?";
        }

        // Otherwise, prompt the user
        var buttons = null;
        if (errorCount > 0) {
            buttons = {
                "Fix Errors": function(event) {
                    $(this).dialog("close");
                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                }};
        }
        else {
            buttons = {
                "Yes": function() {
                    $(this).dialog("close");
                    ignorePrompts["warn"] = true;
                    editor.save(runImmediately, ignorePrompts);
                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                },
                "No": function() {
                    $(this).dialog("close");
                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                }};
        }
        editor.showDialog("Pipeline Issues", message, buttons);
        return false;
    },
    
    confirmClone: function(runImmediately, ignorePrompts) {
    	var readOnly = false;
        // See if read-only
    	var lsid = editor.workspace["pipelineLsid"];
    	var module = library.modules[lsid];
    	if (module !== null && module !== undefined) {
    		readOnly = !module.write;
    	}

        // If not read only, return
        if (!readOnly) {
            return true;
        }

        // Otherwise, prompt the user
        var buttons = { "Continue": function(event) {
            $(this).dialog("close");
            ignorePrompts["readOnly"] = true;
            editor.save(runImmediately, ignorePrompts);
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        },
        "Cancel": function(event) {
            $(this).dialog("close");
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        }};
        editor.showDialog("Confirm Clone Pipeline", "The pipeline you are attempting to save is read-only.  " +
        		"Saving this pipeline will result in a new copy of the pipeline with your current edits.", buttons);
        return false;
    },

    confirmUniqueName: function(runImmediately, ignorePrompts) {
        var nameFound = false;

        // See if another pipeline or module shares a name
        for (var i in library.modules) {
            var module = library.modules[i];

            // If not a module, ignore this iteration
            if (!module instanceof Module) { continue; }

            if (module.name === editor.workspace["pipelineName"]) {
                // If a shared name is found, make sure it's not a different version of the same pipeline
                var foundBaseLsid = editor.extractBaseLsid(module.lsid);
                var thisBaseLsid = editor.extractBaseLsid(editor.workspace["pipelineLsid"]);
                if (foundBaseLsid !== thisBaseLsid) { nameFound = true; }
            }
        }

        // If none share a name, return
        if (!nameFound) {
            return true;
        }

        // Otherwise, prompt the user
        var buttons = { "Yes": function(event) {
            $(this).dialog("close");
            ignorePrompts["sameName"] = true;
            editor.save(runImmediately, ignorePrompts);
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        },
        "No": function(event) {
            $(this).dialog("close");
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        }};
        editor.showDialog("Confirm Pipeline Name", "One or more pipelines or modules were already found with this name: <strong>"
            + editor.workspace["pipelineName"] + "</strong>.<br /><br />Are you sure you want to save the pipeline with this name?", buttons);
        return false;
    },

    confirmIfUntitled: function(runImmediately, ignorePrompts) {
        var isUntitled = editor.workspace["pipelineName"].indexOf("UntitledPipeline") == 0;

        // If not untitled then continue
        if (!isUntitled) {
            return true;
        }

        // Otherwise prompt the user
        var buttons = { "Yes": function(event) {
            $(this).dialog("close");
            ignorePrompts["untitled"] = true;
            editor.save(runImmediately, ignorePrompts);
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        },
            "No": function(event) {
                $(this).dialog("close");
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            }};
        editor.showDialog("Confirm Pipeline Name", "This pipeline is an untitled pipeline.<br /><br />Are you sure you want to save the pipeline with this name?", buttons);
        return false;
    },

    bundleAsModule: function() {
        var moduleJSON = {
            name: editor.workspace["pipelineName"],
            lsid: editor.workspace["pipelineLsid"],
            version: editor.workspace["pipelineVersion"],
            category: "pipeline",
            write: true,
            outputs: [],
            inputs: []
        };
        return new Pipeline(moduleJSON);
    },

    hasErrors: function() {
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                if (editor.workspace[i].hasError()) {
                    return true;
                }
            }
        }

        return false;
    }
};

/**
 * Class representing the library display at the top of the page
 */
var library = {
    div: "library",			// The id of the div used by the library
    moduleNames: [],		// A list of all module names
    moduleVersionMap: {},   // A map of module names to an array of all module versions
    moduleCategoryMap: {},  // A map of all module categories to an array of modules in that category
    modules: {},			// The JSON structure of all modules in the library
    recent: [],             // List of recently used modules
    loadInit: false,       // Whether the load pipeline dialog has been initialized or not

    init: function(moduleJSON) {
        this.initStructure(moduleJSON);

        this._addModuleComboBox();
        this._addFileButton();
        this._addCategoryModules();

        $("#closeAllCategories").click(function(event) {
            $(".moduleBullet").hide();
            $(".categoryOpen").hide();
            $(".categoryClosed").show();
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        });

        $("#openAllCategories").click(function(event) {
            $(".moduleBullet").show();
            $(".categoryOpen").show();
            $(".categoryClosed").hide();
            
            // Hack to get layout to work correctly in Firefox
            if ($.browser.mozilla) {
            	var height = $(document).height() - 142;
            	$("#library").height(height);
            }
            
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        });
    },
    
    initStructure: function(moduleJSON) {
    	this._readModules(moduleJSON);
        this._readModuleVersions();
        this._readModuleNames();
        this._readModuleCategories();
    },

    isODF: function(type) {
        return type === "Actual Peaks" ||
            type === "Actual Peaks to EM-matched Peaks" ||
            type === "Comparative Marker Selection" ||
            type === "Dataset" ||
            type === "EM Gaussian Mixtures" ||
            type === "Gene List" ||
            type === "KNN Prediction Model" ||
            type === "Prediction Features" ||
            type === "Prediction Results" ||
            type === "Proteomics Analysis Statistics" ||
            type === "SOM Cluster" ||
            type === "Spectra Similarity" ||
            type === "Spectrum Area Change" ||
            type === "Spectrum Peaks" ||
            type === "Spectrum Peaks Locate";
    },

    _addFileButton: function() {
        $("#attachFile").button();
        $("#attachFile").click(function() {
            var label = document.createElement("div");
            label.innerHTML = "<strong>Please select a file below to upload.</strong><br />";
            var uploadForm = document.createElement("form");
            uploadForm.setAttribute("id", "upload_form");
            uploadForm.setAttribute("action", "/gp/PipelineDesigner/upload");
            uploadForm.setAttribute("method", "POST");
            uploadForm.setAttribute("enctype", "multipart/form-data");
            label.appendChild(uploadForm);

            var fileUpload = document.createElement("input");
            fileUpload.setAttribute("type", "file");
            fileUpload.setAttribute("name", "uploadInput");
            fileUpload.setAttribute("id", "uploadInput");
            fileUpload.setAttribute("class", "propertyValue");
            uploadForm.appendChild(fileUpload);

            var path = document.createElement("input");
            path.setAttribute("type", "hidden");
            path.setAttribute("id", "hiddenFilePath");
            uploadForm.appendChild(path);

            // Attach the uploading and done images
            var uploadingImg = document.createElement("img");
            uploadingImg.setAttribute("class", "uploadingImage");
            uploadingImg.setAttribute("src", "images/uploading.gif");
            uploadingImg.setAttribute("id", "fileUploading");
            uploadingImg.setAttribute("style", "display: none;");
            uploadForm.appendChild(uploadingImg);
            var doneImg = document.createElement("img");
            doneImg.setAttribute("src", "images/complete.gif");
            doneImg.setAttribute("id", "fileDone");
            doneImg.setAttribute("style", "display: none;");
            uploadForm.appendChild(doneImg);

            var buttons = {
                "OK": function(event) {
                    // Add the file to the UI
                    editor.addFile($("#uploadInput").val(), $("#hiddenFilePath").val());

                    // Tear down the dialog
                    $(this).dialog("close");
                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                },
                "Cancel": function(event) {
                    $(this).dialog("close");
                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                }
            };

            editor.showDialog("Attach File", label, buttons);
            $(".ui-dialog-buttonpane button:contains('OK')").button("disable");
            $(".ui-dialog-titlebar-close").hide();

            // When the upload form is submitted, send to the servlet
            $("#upload_form").iframePostForm({
                json : false,
                post : function() {
                    var filename = $(fileUpload).val();
                    filename = editor.extractFilename(filename);
                    if (editor.filenameExists(filename)) {
                        editor.showDialog("Attach File Error", "This file name already exists in the pipeline.  Unable to upload.");
                        return false;
                    }
                    if (editor.isReservedFilename(filename)) {
                        editor.showDialog("Attach File Error", "This file has a reserved file name.  Unable to upload.");
                        return false;
                    }

                    $("#fileUploading").show();
                    $("#fileDone").hide();
                },
                complete : function (response) {
                    // Work around a bug in the JSON handling of iframe-post-form
                    response = $.parseJSON($(response)[0].innerHTML);

                    $("#fileUploading").hide();
                    $("#fileDone").show();

                    if (response["ERROR"] !== undefined) {
                        editor.showDialog("Error Uploading File", response["ERROR"]);
                    }
                    else {
                        if (!editor.foundInList(editor.workspace["files"], response.location)) {
                            editor.workspace["files"].push(response.location);
                        }
                        $("#hiddenFilePath").val(response.location);
                        $(".ui-dialog-buttonpane button:contains('OK')").button("enable");
                    }
                }
            });

            // When a file is selected for upload, begin the upload
            $(fileUpload).change(function() {
                $(uploadForm).submit();
            });
        });
    },

    getHighestVersion: function(lsid) {
        var baseLsid = editor.extractBaseLsid(lsid);
        var versionArray = library.moduleVersionMap[baseLsid];
        // Handle the case of there being no version available
        if (versionArray === undefined || versionArray === null) { return null; }
        var highestModule = null;
        var highestVersion = null;
        for (var i = 0; i < versionArray.length; i++) {
            if (this.higherVersion(versionArray[i].version, highestVersion)) {
                highestVersion = versionArray[i].version;
                highestModule = versionArray[i];
            }
        }
        return highestModule;
    },

    _addRecentModule: function(lsid, value) {
        for (var i = 0; i < this.recent.length; i++) {
            if (this.recent[i] == lsid) { return }
        }

        this.recent.push(lsid);
        var removed = null;
        if (this.recent.length > 10) { removed = this.recent.shift(); }
        if (removed !== null) { $("button[name|='" + removed + "']").remove(); }
        this._addModuleButton(value, lsid);
    },

    concatNameForDisplay: function(name, length) {
        var toRemove = name.length - length;

        // No need to concatenate
        if (toRemove < 1) {
            return name;
        }

        var firstPart = name.substring(0, length / 2);
        var lastPart = name.substring(name.length - (length / 2));

        return firstPart + "..." + lastPart;
    },

    _addCategoryModules: function() {
        // Sort Categories
        var sortedCategories = new Array();
        //noinspection JSDuplicatedDeclaration
        for (var cat in library.moduleCategoryMap) {
            sortedCategories.push(cat);
        }
        sortedCategories = library._sortAlphabetically(sortedCategories, true);

        for (var iCat = 0; iCat < sortedCategories.length; iCat++) {
            //noinspection JSDuplicatedDeclaration
            var cat = sortedCategories[iCat];
            var catDiv = document.createElement("div");
            catDiv.setAttribute("class", "categoryContainer");
            var headDiv = document.createElement("div");
            headDiv.setAttribute("class", "categoryHeader");
            headDiv.innerHTML = "&#160;&#160;<img src='/gp/pipeline/images/category-open.gif' alt='Expanded' style='display: none;' class='categoryOpen' />" +
                "<img src='/gp/pipeline/images/category-closed.gif' alt='Collapsed' class='categoryClosed' />&#160;&#160;" +
                properties._encodeToHTML(cat);
            catDiv.appendChild(headDiv);

            // Sort modules in the category
            var moduleArray = library._sortModulesAlphabetically(library.moduleCategoryMap[cat]);

            for (var i = 0; i < moduleArray.length; i++) {
                var module = moduleArray[i];
                var modDiv = document.createElement("div");
                modDiv.setAttribute("class", "moduleBullet");
                modDiv.setAttribute("name", module.lsid);
                modDiv.innerHTML = "&#160;&#160;&#160;&#160;<img src='/gp/pipeline/images/module-bullet.gif' alt='Bullet' />&#160;&#160;" +
                    "<a href='#' onclick='return false;'>" + properties._encodeToHTML(this.concatNameForDisplay(module.name, 20)) + "</a>";
                $(modDiv).click(function() {
                    var lsid = $(this)[0].getAttribute("name");

                    // If unable to find the lsid give up, an error has already been reported
                    if (lsid === null) return;

                    var module = editor.addModule(lsid);

                    // Scroll page to new module
                    $("html, body").animate({ scrollLeft: $(module.ui).position().left - 100 }, "slow");
                });
                catDiv.appendChild(modDiv);
            }

            $("#categoryModules").append(catDiv);
        }
        $(".categoryHeader").click(function() {
            if ($(this).parent().children(".moduleBullet").is(":visible")) {
                $(this).parent().children(".moduleBullet").hide("slow");
                $(this).children(".categoryOpen").hide();
                $(this).children(".categoryClosed").show();
            }
            else {
                $(this).parent().children(".moduleBullet").show("slow");
                $(this).children(".categoryOpen").show();
                $(this).children(".categoryClosed").hide();
                
                // Hack to get layout to work correctly in Firefox
                if ($.browser.mozilla) {
                	var height = $(document).height() - 142;
                	$("#library").height(height);
                }
            }
        });
    },

    _addModuleComboBox: function() {
        $("#modulesDropdown").autocomplete({ source: this.moduleNames });
        $("#addModule").button();

        $("#modulesDropdown").keyup(function(event) {
            if (event.keyCode == 13) {
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
                $("#addModule").click();
            }
        });

        $("#addModule").click(function() {
            var value = $("#modulesDropdown").val();
            var name = library._extractNameFromDropdown(value);
            var lsid = library._lookupLsid(name);

            // If unable to find the lsid give up, an error has already been reported
            if (lsid === null) return;

            $("#modulesDropdown").val("");
            var module = editor.addModule(lsid);

            // Scroll page to new module
            $("html, body").animate({ scrollLeft: $(module.ui).position().left - 100 }, "slow");
        });
    },

    _addModuleButton: function(name, lsid) {
        var modButton = document.createElement("button");
        modButton.innerHTML = name;
        modButton.setAttribute("class", "libraryModuleButton");
        modButton.setAttribute("name", lsid);
        var theSpan = $("#shortcutModules")[0];
        theSpan.insertBefore(modButton, theSpan.firstChild);
        $(modButton).click(function() {
            editor.addModule(this.name);
        });
        $("button[name|='" + lsid + "']").button();
    },

    _lookupLsid: function (name) {
        for (var i in library.modules) {
            var module = library.modules[i];

            // Check for naming match
            if (name === module.name) {
            	var highest = library.getHighestVersion(module.lsid);
            	if (module.lsid === highest.lsid) {
            		return highest.lsid;
            	}
            }
        }

        // If we got through the loop without finding it, report the error
        editor.log("ERROR: Could not find module name: " + name + ".");
        return null;
    },

    _extractNameFromDropdown: function(dropdownText) {
        return dropdownText;
    },

    _readModuleVersions: function() {
        this.moduleVersionMap = {};
        for (var i in library.modules) {
            var base = editor.extractBaseLsid(library.modules[i].lsid);

            if (this.moduleVersionMap[base] === undefined) {
                this.moduleVersionMap[base] = new Array();
            }

            this.moduleVersionMap[base].push(library.modules[i]);
        }
    },

    higherVersion: function(verA, verB) {
        // Handle the null case
        if (verA == null) { return false; }
        if (verB == null) { return true; }

        // Split by periods
        var partsA = verA.split(".");
        var partsB = verB.split(".");

        // Compare each part numerically
        var index = 0;
        while (index < partsA.length || index < partsB.length) {
            // Check for the end of version strings and return appropriately
            if (partsA.length < (index + 1) && partsB.length >= (index + 1)) {
                return false;
            }
            else if (partsA.length >= (index + 1) && partsB.length < (index + 1)) {
                return true;
            }
            else if (partsA.length < (index + 1) || partsB.length < (index + 1)) {
                break;
            }

            // Convert version part to integer
            var aInt = parseInt(partsA[index]);
            var bInt = parseInt(partsB[index]);

            // Compare integers and return
            if (aInt > bInt) {
                return true;
            }
            else if (aInt < bInt) {
                return false;
            }
            // If both integers are equal, move on to the next part
            else {
                index++;
            }
        }

        // Both versions are exactly the same
        editor.log("WARN: library._higherVersion() called on two versions string that are the same");
        return false;
    },

    _addModuleToCategoryMap: function(module) {
        var category = module.category;
        if (category === null || category === "") { category = "Uncategorized"; }

        if (this.moduleCategoryMap[category] === undefined || this.moduleCategoryMap[category] === null) {
            this.moduleCategoryMap[category] = new Array();
        }

        this.moduleCategoryMap[category].push(module);
    },

    _readModuleCategories: function() {
        for (var i in library.moduleVersionMap) {
            var module = library.getHighestVersion(i + ":fakeVersion");
            this._addModuleToCategoryMap(module)
        }
    },

    _readModuleNames: function() {
        this.moduleNames = new Array();
        for (var i in library.moduleVersionMap) {
            var moduleArray = library.moduleVersionMap[i];
            if (moduleArray.length == 1) {
                this.moduleNames.push(moduleArray[0].name);
            }
            else if (moduleArray.length > 1) {
                var highestModule = null;
                var highestVersion = null;
                for (var j = 0; j < moduleArray.length; j++) {
                    if (this.higherVersion(moduleArray[j].version, highestVersion)) {
                        highestVersion = moduleArray[j].version;
                        highestModule = moduleArray[j];
                    }
                }
                this.moduleNames.push(highestModule.name);
            }
            else {
                editor.log("ERROR: Unacceptable length of version array in library._readModuleNames()");
            }

        }

        this.moduleNames = library._sortAlphabetically(this.moduleNames).toArray();
    },

    _sortAlphabetically : function(stringArray, specialCase) {
        return $(stringArray).sort(function(a, b) {
            // Handle special case for "pipeline"
            if (specialCase && a === "pipeline" && b === "pipeline") return 0;
            else if (specialCase && a === "pipeline") return 1;
            else if (specialCase && b === "pipeline") return -1;


            var compA = a.toUpperCase();
            var compB = b.toUpperCase();
            return (compA < compB) ? -1 : (compA > compB) ? 1 : 0;
        });
    },

    _sortModulesAlphabetically : function(moduleArray) {
        return $(moduleArray).sort(function(a, b) {
            var compA = a.name.toUpperCase();
            var compB = b.name.toUpperCase();
            return (compA < compB) ? -1 : (compA > compB) ? 1 : 0;
        });
    },

    _readModules: function(moduleJSON) {
        this.modules = {};
        for (var i in moduleJSON) {
            var module = moduleJSON[i];
            if (module.type == "module") {
                this.modules[module.lsid] = new Module(module);
            }
            else if (module.type == "visualizer") {
                this.modules[module.lsid] = new Visualizer(module);
            }
            else if (module.type == "pipeline") {
                this.modules[module.lsid] = new Pipeline(module);
            }
            else {
                editor.log("Error detecting module type: " + module.name);
            }
        }
    },

    _createBlackBoxParam: function(module, pointer) {
        var inputJSON = {
            name: pointer,
            description: "Assumed Parameter for " + pointer,
            type: "Unknown",
            kinds: [],
            required: false,
            promptWhenRun: null,
            defaultValue: "Unknown",
            choices: []
        };
        var param = new InputParam(module, inputJSON);
        module.inputs[module.inputs.length] = param;
        return param;
    },

    _createBlackBoxModule: function(name, lsid) {
        if (name === undefined) { name = "UnknownModule"; }
        if (lsid === undefined) { lsid = ""; }

        // Manually insert the Black Box dummy module
        var moduleJSON = {
            name: name,
            lsid: lsid,
            version: "",
            category: "",
            write: true,
            outputs: [],
            inputs: []
        };
        var module = new Module(moduleJSON);
        module.blackBox = true;
        return module;
    },

    extractFileInputs: function(inputs) {
        var files = new Array();
        for (var i = 0; i < inputs.length; i++) {
            if (inputs[i].isFile()) {
                files[files.length] = inputs[i];
            }
        }
        return files;
    },

    extractInputs: function(module, inputsJSON) {
        var inputs = new Array();
        for (var i = 0; i < inputsJSON.length; i++) {
            inputs[inputs.length] = new InputParam(module, inputsJSON[i]);
        }
        return inputs;
    },

    displayLoadDialog: function() {
        var moduleList = new Array();
        if (!this.loadInit) {
            // Get the Dialog List Div
            var pipelineList = $("#pipelineSelectList")[0];
            $("#pipelineSelectList").children().remove();

            // Read Pipelines from List
            //noinspection JSDuplicatedDeclaration
            for (var i in library.moduleVersionMap) {
                //noinspection JSDuplicatedDeclaration
                var module = library.getHighestVersion(i + ":fakeVersion");

                if (module === null || module === undefined) {
                    editor.log("ERROR: Reading module array in library.displayLoadDialog()");
                    return;
                }

                // Add to list
                moduleList.push(module);
            }

            // Sort the list
            moduleList = $(moduleList).sort(function(a, b) {
                var compA = a.name.toUpperCase();
                var compB = b.name.toUpperCase();
                return (compA < compB) ? -1 : (compA > compB) ? 1 : 0;
            });

            //noinspection JSDuplicatedDeclaration
            for (var i = 0; i < moduleList.length; i++) {
                //noinspection JSDuplicatedDeclaration
                var module = moduleList[i];

                // Add Pipeline Div to List
                if (module.isPipeline()) {
                    var li = document.createElement("li");
                    li.setAttribute("name", module.lsid);
                    if (module.write) {
                        li.innerHTML = "<img src='images/readwrite.gif' /> ";
                    }
                    else {
                        li.innerHTML = "<img src='images/readonly.gif' /> ";
                    }
                    li.innerHTML += library.concatNameForDisplay(module.name, 40);
                    pipelineList.appendChild(li);
                }
            }

            $(pipelineList).selectable();
            $("#loadPipeline").button();
            $("#loadPipeline").click(function() {
            	if (editor.workspace["dirty"]) {
            		var buttons = { "Yes, Load the Pipeline": function() {
                    	$(this).dialog("close");
                    	var lsid = $("#pipelineSelectList .ui-selected")[0].getAttribute("name");
                        editor.load(lsid);
                        $("#loadPipelineDialog").dialog("close");
                    },
                    "Cancel": function(event) {
                    	$(this).dialog("close");
                    	$("#loadPipelineDialog").dialog("close");
                    	if (event.preventDefault) event.preventDefault();
                        if (event.stopPropagation) event.stopPropagation();
                    }};
            		editor.showDialog("Load Alert", "Your current pipeline has changes.  If you load a new pipeline these changes will be lost.  Continue?", buttons);
            	}
            	else {
            		var lsid = $("#pipelineSelectList .ui-selected")[0].getAttribute("name");
                    editor.load(lsid);
                    $("#loadPipelineDialog").dialog("close");
            	}   
            });
            $("#cancelPipeline").button();
            $("#cancelPipeline").click(function() {
                $("#loadPipelineDialog").dialog("close");
            });

            // Set to initialized
            this.loadInit = true;
        }

        $("#loadPipelineDialog").dialog("open");

        // Fix z-index for dialog
        var z = parseInt($("#loadPipelineDialog").parent().css("z-index"));
        if (z < 10000) {
            z += 9000;
            $(".top-dialog").css("z-index", z);
        }
    }
};

/**
 * Class representing the properties pane
 */
var properties = {
    PROMPT_WHEN_RUN: "PROMPT_WHEN_RUN",
    div: "properties",
    titleDiv: "propertiesTitle",
    subtitleDiv: "propertiesSubtitle",
    versionDiv: "propertiesVersion",
    inputDiv: "propertiesInput",
    current: null,
    hasFocus: false,

    init: function() {
        $("html").click(function(event) {
        	// Check for uploading status and handle accordingly
        	if (properties.confirmWhenUploading()) {
        		return;
        	}
        	
            if (!editor.isInsideDialog(event.target)) {
                this.hasFocus = false;
                properties.hide();
            }
        });

        $("#" + properties.div).click(function(event) {
            this.hasFocus = true;
            event.stopPropagation();
        });

        $("#" + properties.div).click(function(event) {
            this.hasFocus = true;
            event.stopPropagation();
        });
    },

    redrawDisplay: function() {
        if (this.current instanceof Module) {
            this.displayModule(this.current);
            this.current.checkForWarnings();
        }
        else if (this.current instanceof String && this.current == "Pipeline") {
            this.displayPipeline();
        }
    },
    
    confirmWhenUploading: function() {
    	var isUploading = $(".uploadingImage").is(":visible");
        var isDialogOpen = $(".ui-dialog").is(":visible");

        if (isDialogOpen) {
            return true;
        }
    	
    	if (!isUploading) {
    		return false;
    	}

        // Otherwise prompt the user
        var buttons = { "Yes, Exit and Cancel Upload": function() {
        	$(this).dialog("close");
        	properties.hide();
        	properties._clean();
        },
        "Stay in Pane": function(event) {
        	$(this).dialog("close");
        	if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        }};
        editor.showDialog("Confirm Exiting Module Editor", "You are currently uploading a file.  Exiting the module editor pane while uploading will cancel your upload.  Are you sure you want to leave the editor pane?", buttons);
        
        return true;
    },

    listToString: function(list) {
        var toReturn = "";
        for (var i = 0; i < list.length; i++) {
            toReturn += list[i];
            if (i !== (list.length - 1)) {
                toReturn += ", ";
            }
        }

        return toReturn;
    },

    saveToModel: function() {
        if (this.current instanceof Module) {
            //noinspection JSDuplicatedDeclaration
            var save = this._bundleSave();
            this.current.saveProps(save);
        }
        else if (this.current instanceof Pipe) {
            //noinspection JSDuplicatedDeclaration
            var save = this._bundleSave();
            this.current.saveProps(save);
        }
        else if (this.current == "Pipeline") {
            //noinspection JSDuplicatedDeclaration
            var save = this._bundleSave();
            editor.saveProps(save);
        }
        else if (this.current == "Prompt When Run") {
            // No need to save anything
        }
        else {
            editor.log("ERROR: Cannot determine what is being edited to save to the model");
        }
    },

    _findCheckBox: function(checks, name) {
        for (var i = 0; i < checks.length; i++) {
            if (checks[i].getAttribute("name") == name) {
                return checks[i].checked;
            }
        }
        return false;
    },

    _bundleSave: function() {
        var bundle = {};
        var inputs = $(".propertyValue");
        var checks = $(".propertyCheckBox");
        for (var i = 0; i < inputs.length; i++) {
            var name = inputs[i].getAttribute("name");
            var checked = this._findCheckBox(checks, name);

            // Removed the trailing asterisk if a required param
            if (name.substr(-1) === "*") {
                name = name.substr(0, name.length - 1);
            }

            if (checked) {
                bundle[name] = properties.PROMPT_WHEN_RUN;
            }
            else {
                var value = inputs[i].value;
                if (inputs[i].getAttribute("type") == "hidden" && value === properties.PROMPT_WHEN_RUN) {
                	value = "";
                }
                bundle[name] = value;
            }
        }
        return bundle;
    },

    hide: function() {
        this.hasFocus = false;
    },

    show: function() {
        this.hasFocus = true;

        if (this.current instanceof Module) {
            this.current.checkForWarnings();
        }
    },

    _encodeToHTML: function(text) {
        return text.replace(/&/g, "&amp;").replace(/>/g, "&gt;").replace(/</g, "&lt;").replace(/"/g, "&quot;");
    },

    _clean: function() {
        $("#" + this.titleDiv)[0].innerHTML = "";
        $("#" + this.subtitleDiv)[0].innerHTML = "";
        $("#" + this.versionDiv)[0].innerHTML = "";
        $("#" + this.inputDiv)[0].innerHTML = "";
    },

    _setTitle: function(title) {
        $("#" + this.titleDiv)[0].innerHTML = library.concatNameForDisplay(this._encodeToHTML(title), 18);
    },

    _setSubtitle: function(subtitle) {
        $("#" + this.subtitleDiv)[0].innerHTML = this._encodeToHTML(subtitle);
    },

    _setVersion: function(version) {
        var versionDiv = $("#" + this.versionDiv)[0];
        versionDiv.innerHTML = "v" + this._encodeToHTML(version);
    },

    _setVersionDropdown: function(moduleOrLsid) {
    	// Protect against the double dropdown bug
    	if ($("#propertiesVersion select").length !== 0) {
    		return;
    	}
    	
        // Set appropriate variables based on if the param is a module or the pipeline's lsid
        var baseLsid = null;
        var version = null;
        var id = null;
        if (moduleOrLsid instanceof Module) {
            baseLsid = editor.extractBaseLsid(moduleOrLsid.lsid);
            version = moduleOrLsid.version;
            id = moduleOrLsid.id;
        }
        else {
            baseLsid = editor.extractBaseLsid(moduleOrLsid);
            version = editor.workspace["pipelineVersion"];
            id = "pipeline";
        }

        var moduleArray = library.moduleVersionMap[baseLsid];

        // Handle the case of unknown modules
        if (moduleArray === undefined) {
            moduleArray = [];
        }

        var select = document.createElement("select");
        for (var i = 0; i < moduleArray.length; i++) {
            //noinspection JSDuplicatedDeclaration
            var option = document.createElement("option");
            option.setAttribute("value", id + "|" + moduleArray[i].lsid);
            if (moduleArray[i].version == version) {
                option.setAttribute("selected", "selected");
            }
            option.innerHTML = moduleArray[i].version;
            select.appendChild(option);
        }
        
        // Sort select
        $(select).html($("option", $(select)).sort(function(a, b) { 
            var aText = $(a).text();
            var bText = $(b).text();
            return aText == bText ? 0 : library.higherVersion(aText, bText) ? -1 : 1; 
        }));
        
        // Select the correct version
        $(select).find("option[selected='selected']").attr("selected", "selected");

        // Handle the case of an empty select
        if (select.children.length === 0) {
            //noinspection JSDuplicatedDeclaration
            var option = document.createElement("option");
            option.innerHTML = "No Version";
            select.appendChild(option);
        }

        var versionDiv = $("#" + this.versionDiv)[0];
        versionDiv.appendChild(select);

        // If this is a module
        if (moduleOrLsid instanceof Module) {
        	$(select).focus(function() {
        		if ($(this).data("oldValue") === undefined || $(this).data("oldValue") === null) {
        			var oldValue = $(this).val();
                    $(this).data("oldValue", oldValue);
        		}
        	});
        	
            $(select).change(function(event) {
                var value = event.target.value;
                var parts = value.split("|");
                var doIt = confirm("This will swap out the currently edited module with another version.  Continue?");
                if (doIt) {
                	$(this).data("oldValue", null);
                    var newModule = editor.replaceModule(parts[0], parts[1]);
                    setTimeout("editor.updateAllPorts()", 300);
                    newModule.checkForWarnings();
                    properties.displayModule(newModule);
                    properties.show();
                }
                else {
                    var lsid = $(this).data("oldValue");
                    $(this).val(lsid);
                    $(this).data("oldValue", null);
                }
            });
        }
        else { // Otherwise this is the pipeline being edited
            $(select).change(function(event) {
                var value = event.target.value;
                var parts = value.split("|");
                var doIt = confirm("This will load a different version of this pipeline.  Continue?");
                if (doIt) {
                    editor.load(parts[1]);
                }
                else {
                    var lsid = editor.workspace["pipelineLsid"];
                    $(this).val("pipeline|" + lsid);
                }
            });
        }
    },

    _displayInputKey: function() {
        var key = document.createElement("div");
        key.setAttribute("id", "propertiesKey");
        this._addSpacerDiv();
        key.innerHTML += "<em>Check for Prompt When Run&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;* Required</em>";
        $("#" + this.inputDiv).append(key);
        this._addSpacerDiv();
    },

    _addPromptWhenRun: function(parentDiv, name, value, disabled) {
        if (disabled === undefined) { disabled = false; }

        var checkBox = document.createElement("input");
        checkBox.setAttribute("type", "checkbox");
        checkBox.setAttribute("name", name);
        checkBox.setAttribute("class", "propertyCheckBox");
        if (disabled) {
            checkBox.setAttribute("disabled", disabled);
        }
        if (value == properties.PROMPT_WHEN_RUN) {
            checkBox.setAttribute("checked", "true");
        }
        parentDiv.appendChild(checkBox);
        parentDiv.innerHTML += " ";

        return checkBox;
    },

    _addPWRDisplayButton: function() {
        var div = document.createElement("div");
        var label = document.createElement("label");
        label.innerHTML = "Prompt When Runs";
        label.setAttribute("class", "propertiesLabel");
        var button = document.createElement("button");
        button.innerHTML = "Display Prompt When Runs";
        var disable = editor.pwrCount() === 0;
        $(button).button({"disabled": disable});
        $(button).click(function() {
            properties.displayPWRs();
        });
        $(div).append(label);
        $(div).append(button);
        $("#" + this.inputDiv).append(div);
    },

    _addSpacerDiv: function() {
        var div = document.createElement("div");
        div.setAttribute("class", "spacerDiv");
        $("#" + this.inputDiv).append(div);
    },

    _addFileUpload: function(labelText, value, description, pwr, disabled) {
    	var param = null;
    	if (value instanceof InputParam) {
    		param = value;
    		if (param.promptWhenRun) {
    			value = properties.PROMPT_WHEN_RUN;
    		}
    		else if (param.port !== null && param.port.pipes.length > 0) {
    			value = "Receiving " + param.port.pipes[0].outputPort.pointer + " from " + param.port.pipes[0].outputModule.name;
    		}
    		else {
    			value = param.value;
    		}
    	}
    	
        var label = document.createElement("div");

        if (pwr) {
            var checkBox = this._addPromptWhenRun(label, labelText, value, value !== properties.PROMPT_WHEN_RUN && disabled);
        }

        var textLabel = document.createElement("label");
        textLabel.setAttribute("class", "propertiesLabel");
        textLabel.innerHTML += this._encodeToHTML(labelText);
        label.appendChild(textLabel);

        var hiddenField = document.createElement("input");
        hiddenField.setAttribute("type", "hidden");
        hiddenField.setAttribute("class", "propertyValue");
        hiddenField.setAttribute("name", labelText);
        hiddenField.setAttribute("value", param !== null ? param.value : value);
        label.appendChild(hiddenField);

        if (value === properties.PROMPT_WHEN_RUN) {
            properties._showDisplaySettingsButton($(label), labelText);
        }
        else if (value !== undefined && value !== null && value !== "" && value !== properties.PROMPT_WHEN_RUN && pwr) {
            // The upload is disabled due to a pipe being connected to this input, display edit button
            properties._showDeletePipeButton($(label), labelText);
        }
        else if (!pwr && (properties.current == "Pipeline")) {
            properties._createPipelineFileButton(labelText, $(label))
        }

        // If the value has been previously set, attach the value div
        var valueDiv = document.createElement("div");
        valueDiv.setAttribute("class", "fileUploadValue");
        if (value !== undefined && value !== null && value !== "" && value !== properties.PROMPT_WHEN_RUN) {
        	if (properties.current == "Pipeline") {
        		var deleteImage = $("<img src='images/delete.gif' style='height: 9px;' class='deleteFile' name='" + labelText + "' />");
        		$(valueDiv).append(deleteImage);
        		$(valueDiv).append(" ");
        	}
        	
            valueDiv.innerHTML += "<strong>Current Value:</strong> " + properties._encodeToHTML(value.toString());
        }
        label.appendChild(valueDiv);

        $("#" + this.inputDiv).append(label);
        
        $(".deleteFile[name='" + labelText + "']").click(function() {
        	var reallyDelete = confirm("Are you sure you want to delete this file?");
        	if (reallyDelete) {
        		var filename = $("input.propertyValue[type='hidden'][name='" + labelText + "']").val();
				$("input.propertyValue[type='hidden'][name='" + labelText + "']").val("");
				properties.saveToModel();
				editor.removeFile(filename);
        	}
		});

        if (description !== null && description !== false && typeof(description) === "string") {
            var desc = document.createElement("div");
            desc.setAttribute("class", "inputDescription");
            desc.innerHTML = this._encodeToHTML(description);
            $("#" + this.inputDiv).append(desc);
        }
        else if (description !== null && description !== false) {
            $("#" + this.inputDiv).append(description);
        }

        this._addSpacerDiv();

        // When the prompt when run checkbox is checked, enable or disable upload
        if (checkBox !== undefined && checkBox !== null) {
            $(".propertyCheckBox[name='" + labelText + "']").change(function() {
                var module = properties.current;
                if (!(module instanceof Module)) {
                    var id = $(label).attr("name");
                    module = editor.workspace[id];
                }

                var input = module.getInputByName(properties._stripTrailingAstrik(labelText));

                if ($(this).is(":checked")) {
                    properties._showDisplaySettingsButton($(label), labelText);
                    input.makePWR(input.name, input.description);
                }
                else {
                    properties._hideDisplaySettingsButton(labelText);
                    input.makeNotPWR();
                }

                // Save when the select is changed
                properties.saveToModel();
            });
        }

        return label;
    },

    _addDropDown: function(labelText, values, selected, description, pwr) {
        var label = document.createElement("div");

        if (pwr) {
            var checkBox = this._addPromptWhenRun(label, labelText, selected);
        }

        var textLabel = document.createElement("label");
        textLabel.setAttribute("class", "propertiesLabel");
        textLabel.innerHTML += this._encodeToHTML(labelText);
        label.appendChild(textLabel);

        var select = document.createElement("select");
        select.setAttribute("name", labelText);
        select.setAttribute("class", "propertyValue");
        for (var i = 0; i < values.length; i++) {
            var parts = values[i].split("=");
            if (parts.length < 2) parts[1] = parts[0];
            var option = document.createElement("option");
            if (selected == parts[0]) {
                option.setAttribute("selected", "true");
            }
            option.innerHTML = this._encodeToHTML(parts[1]);
            option.value = parts[0];
            select.appendChild(option);
        }
        label.appendChild(select);
        $("#" + this.inputDiv).append(label);

        if (description !== null && description !== false) {
            var desc = document.createElement("div");
            desc.setAttribute("class", "inputDescription");
            desc.innerHTML = this._encodeToHTML(description);
            $("#" + this.inputDiv).append(desc);
        }

        this._addSpacerDiv();

        // If the form element is disabled, set to that initially
        if (selected === properties.PROMPT_WHEN_RUN) {
            $("select.propertyValue[name='" + labelText + "']").hide();
            properties._showDisplaySettingsButton($("select.propertyValue[name='" + labelText + "']").parent(), labelText);
        }

        // When the prompt when run checkbox is checked, enable or disable dropdown
        if (checkBox !== undefined && checkBox !== null) {
            $(".propertyCheckBox[type='checkbox'][name='" + labelText + "']").change(function() {
            	var module = properties.current;
                if (!(module instanceof Module)) {
                    var id = $(label).attr("name");
                    module = editor.workspace[id];
                }
                
                var input = module.getInputByName(properties._stripTrailingAstrik(labelText));
            	
                if ($(this).is(":checked")) {
                    $("select.propertyValue[name='" + labelText + "']").hide();
                    properties._showDisplaySettingsButton($("select.propertyValue[name='" + labelText + "']").parent(), labelText);
                    input.makePWR(input.name, input.description);
                }
                else {
                	var inputParam = properties.current.getInputByName(properties._stripTrailingAstrik(labelText));
                	$("select.propertyValue[name='" + labelText + "']").val(inputParam.defaultValue);
                    $("select.propertyValue[name='" + labelText + "']").show();
                    properties._hideDisplaySettingsButton(labelText);
                    input.makeNotPWR();
                }

                // Save when changed
                properties.saveToModel();
            });
        }

        // Save when the select is changed
        $(select).change(function() {
            properties.saveToModel();
            var selector = ".propertyValue[name='" + select.getAttribute("name") + "']";
        	$(selector).parent().next().next().find(".propertyValue").focus();
        });

        return select;
    },

    _showDisplaySettingsDialog: function(attach, module, name) {
        // Handle the case of PWR view
        if (!(module instanceof Module)) {
            var id = $(attach).parent().attr("name");
            module = editor.workspace[id];
        }

        var dName = null;
        var dDesc = null;

        var input = module.getInputByName(name);
        if (input.promptWhenRun !== null) {
            dName = input.promptWhenRun.name;
            dDesc = input.promptWhenRun.description;
        }
        else {
            dName = name;
            dDesc = input.description;
        }

        var inner = "Define alternative name and description to display when prompting for this input." +
        		"<br /> <table><tr><td>Display Name</td><td><input type='text' id='pwrDisplayName' value=''/>" + 
        		"</td></tr><tr><td>Display Description</td><td><input type='text' id='pwrDisplayDesc' value=''/>" +
        		"</td></tr></table>";
        var button = { "OK": function(event) {
            var displayName = $("#pwrDisplayName").val();
            var displayDesc = $("#pwrDisplayDesc").val();
            module.getInputByName(name).makePWR(displayName, displayDesc);

            $(this).dialog("close");
            $(this).dialog("destroy");
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        }};
        editor.showDialog("Set Prompt When Run Display Settings", inner, button);
        $("#pwrDisplayName").val(dName);
        $("#pwrDisplayDesc").val(dDesc);
    },

    _showDeletePipeButton: function(parent, name) {
        if ($("button.editPipeButton[name='" + name + "']").size() === 0) {
            var button = document.createElement("button");
            button.innerHTML = "Remove Connection";
            button.setAttribute("name", name);
            button.setAttribute("class", "editPipeButton");
            parent.append(button);
            $(button).button();
            $(button).click(function(event) {
                var module = properties.current;
                var paramName = this.getAttribute("name");
                var input = module.getInputByName(properties._stripTrailingAstrik(paramName));
                input.value = "";
                input.makeUnused();
                var port = input.port;

                for (var i = 0; i < port.pipes.length; i++) {
                    port.pipes[i].remove();
                }

                properties.redrawDisplay();
                module.checkForWarnings();

                // Prevent the button from submitting the form if it is inside one
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            });
        }
        else {
            $("button.editPipeButton[name='" + name + "']").show();
        }
    },

    _stripTrailingAstrik: function(name) {
        return name.substr(-1) === "*" ? name.substr(0, name.length - 1) : name;
    },

    _showDisplaySettingsButton: function(parent, name) {
        if ($(parent).find("button.pwrDisplayButton[name='" + name + "']").size() === 0) {
            var button = document.createElement("button");
            button.innerHTML = "Set Prompt When Run Display Settings";
            button.setAttribute("name", name);
            button.setAttribute("class", "pwrDisplayButton");
            parent.append(button);
            $(button).button();
            $(button).click(function(event) {
                // Removed the trailing asterisk if a required param, then show the settings dialog
                properties._showDisplaySettingsDialog(button, properties.current, properties._stripTrailingAstrik(name));

                // Prevent the button from submitting the form if it is inside one
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            });
        }
        else {
            $("button.pwrDisplayButton[name='" + name + "']").show();
        }

    },

    _hideDisplaySettingsButton: function(name) {
        $("button.pwrDisplayButton[name='" + name + "']").hide();
    },

    _addTextBox: function(labelText, value, description, pwr) {
        var label = document.createElement("div");

        if (pwr) {
            var checkBox = this._addPromptWhenRun(label, labelText, value);
        }

        var textLabel = document.createElement("label");
        textLabel.setAttribute("class", "propertiesLabel");
        textLabel.innerHTML += this._encodeToHTML(labelText);
        label.appendChild(textLabel);

        var inputBox = document.createElement("input");
        inputBox.setAttribute("type", "text");
        inputBox.setAttribute("name", labelText);
        inputBox.value = value != properties.PROMPT_WHEN_RUN ? value : "";
        inputBox.setAttribute("class", "propertyValue");
        label.appendChild(inputBox);
        $("#" + this.inputDiv).append(label);

        if (description !== null && description !== false) {
            var desc = document.createElement("div");
            desc.setAttribute("class", "inputDescription");
            desc.innerHTML = this._encodeToHTML(description);
            $("#" + this.inputDiv).append(desc);
        }

        this._addSpacerDiv();

        // If the form element is disabled, set to that initially
        if (value === properties.PROMPT_WHEN_RUN) {
            $(".propertyValue[type='text'][name='" + labelText + "']").hide();
            properties._showDisplaySettingsButton($(".propertyValue[type='text'][name='" + labelText + "']").parent(), labelText);
        }

        // When the prompt when run checkbox is checked, enable or disable upload
        if (checkBox !== undefined && checkBox !== null) {
            $(".propertyCheckBox[type='checkbox'][name='" + labelText + "']").change(function() {
            	var module = properties.current;
                if (!(module instanceof Module)) {
                    var id = $(label).attr("name");
                    module = editor.workspace[id];
                }
                
                var input = module.getInputByName(properties._stripTrailingAstrik(labelText));
            	
                if ($(this).is(":checked")) {
                    $(".propertyValue[type='text'][name='" + labelText + "']").hide();
                    properties._showDisplaySettingsButton($(".propertyValue[type='text'][name='" + labelText + "']").parent(), labelText);
                    input.makePWR(input.name, input.description);
                }
                else {
                	var inputParam = properties.current.getInputByName(properties._stripTrailingAstrik(labelText));
                	$(".propertyValue[type='text'][name='" + labelText + "']").val(inputParam.defaultValue);
                    $(".propertyValue[type='text'][name='" + labelText + "']").show();
                    properties._hideDisplaySettingsButton(labelText);
                    input.makeNotPWR();
                }

                // Save when the select is changed
                properties.saveToModel();
            });
        }

        // Save when the select is changed
        $(inputBox).change(function() {
        	properties.saveToModel();
        	var selector = ".propertyValue[name='" + inputBox.getAttribute("name") + "']";
        	$(selector).parent().next().next().find(".propertyValue").focus();
        });

        return inputBox;
    },

    _addFileInput: function(input) {
        var required = input.required ? "*" : "";
        var disabled = false;
        if (input.port !== null && input.port.pipes.length > 0) {
            disabled = true;
        }
        if (input.promptWhenRun !== null) {
            disabled = true;
        }
        var div = this._addFileUpload(input.name + required, input, input.description, true, disabled);
        div.setAttribute("name", input.module.id); // Used in PWR view
        return div;
    },

    _addTextBoxInput: function(input) {
        var required = input.required ? "*" : "";
        var displayValue = input.promptWhenRun !== null ? properties.PROMPT_WHEN_RUN : input.value;
        return this._addTextBox(input.name + required, displayValue, input.description, true);
    },

    _addDropdownInput: function(input) {
        var required = input.required ? "*" : "";
        var displayValue = input.promptWhenRun !== null ? properties.PROMPT_WHEN_RUN : input.value;
        return this._addDropDown(input.name + required, input.choices, displayValue, input.description, true);
    },

    _deselectOldSelection: function() {
        if (this.current instanceof Module) {
            this.current.deselect();
        }
        if (this.current instanceof Pipe) {
            this.current.deselect();
        }
    },

    _disableDropdownValues: function(dropdown, disabledArray) {
        for (var i = 0; i < dropdown.length; i++) {
            var option = dropdown[i];
            var value = option.value;
            for (var j in disabledArray) {
                if (disabledArray[j] === value) {
                    option.setAttribute("disabled", "true");
                }
            }
        }
    },

    displayFile: function(file) {
        // Build the new display
        this._setTitle(file.getFilename());
        this._setSubtitle(file.getDisplayPath());
        this._addSpacerDiv();
        $("#" + this.inputDiv).append("This is a file that has been included in the pipeline.");
        
        // Hack to get layout to work correctly in Firefox
        if ($.browser.mozilla) {
        	var height = $(document).height() - 142;
        	$("#properties").height(height);
        }
    },

    displayModule: function(module) {
        // Clean the old selection
        this._deselectOldSelection();
        this._clean();

        // Set the new selection
        this.current = module;
        module.select();

        // Special handling for files
        if (module.isFile()) {
            this.displayFile(module);
            return;
        }

        // Build the new display
        this._setTitle(module.name);
        this._setSubtitle(module.lsid);

        // Attach the module buttons to the editor
        var iconDiv = module.createIconSpace(module.ui, module.id);
        module._createButtons(iconDiv);
        $("#" + this.subtitleDiv)[0].appendChild(iconDiv);

        this._setVersionDropdown(module);
        this._displayInputKey();
        var inputs = module.inputs;
        for (var i in inputs) {
            if (!(inputs[i] instanceof InputParam)) {
                continue;
            }
            if (inputs[i].isFile()) {
                this._addFileInput(inputs[i]);
            }
            else if (inputs[i].choices.length > 0) {
                this._addDropdownInput(inputs[i]);
            }
            else {
                this._addTextBoxInput(inputs[i]);
            }

        }

        // Attach button events and tooltips
        module.addModuleButtonCalls();
        module.addInfoTooltips();
        
        // Hack to get layout to work correctly in Firefox
        if ($.browser.mozilla) {
        	var height = $(document).height() - 142;
        	$("#properties").height(height);
        }
    },

    _createPipelineFileButton: function(label, parent) {
        var docButton = document.createElement("button");
        docButton.innerHTML = "Attach " + label;
        $(docButton).button();

        $(docButton).click(function() {
            // Trigger the attach file dialog
            $("#attachFile").trigger("click");
            
            // For licenses, ensure that the license if a txt file
            if (label === "License") {
	            var oldFunction = $("#uploadInput").data('events')["change"][0].handler; // Get the old change function
	            $("#uploadInput").off("change"); 		// Remove the normal click
	            $("#uploadInput").change(function() {	// Add the new function
	            	if ($("#uploadInput").get(0).value.match(/.txt$/) === null) {
	            		editor.showDialog("Incorrect File Type", "Only Text (.txt) files are supported for licenses.  Please select a text file.");
	            	}
	            	else {
	            		oldFunction();
	            	}
	            });
            }
            
            // Remove the normal click event
            $(".ui-dialog-buttonpane button:contains('OK')").off("click");
            // Add the new doc click event
            $(".ui-dialog-buttonpane button:contains('OK')").click(function() {
                // Add the file to the UI
                var path = $("#hiddenFilePath").val();
                var filename = path.replace(/^.*(\\|\/|\:)/, '');

                // Add to the hidden field
                $("input[type='hidden'][name='" + label + "']").val(filename);

                // Tear down the dialog
                $(".ui-dialog-buttonpane button:contains('Cancel')").trigger("click");

                // Save the properties
                properties.saveToModel();
            });
        });

        parent.append(docButton);
    },

    displayPipeline: function() {
        // Clean the old selection
        this._deselectOldSelection();
        this._clean();

        this.current = "Pipeline";
        this._setTitle("Editing Pipeline");
        this._setVersionDropdown(editor.workspace["pipelineLsid"]);
        this._setSubtitle(editor.workspace["pipelineLsid"].length > 0 ? editor.workspace["pipelineLsid"] : "");
        this._addTextBox("Pipeline Name", editor.workspace["pipelineName"], false, false);
        this._addTextBox("Description", editor.workspace["pipelineDescription"], false, false);
        this._addTextBox("Author", editor.workspace["pipelineAuthor"], false, false);
        
        var privacyDropdown = null;
        if (!adminServerAllowed) { privacyDropdown = ["private"]; }
        else { privacyDropdown = ["private", "public"]; }
        this._addDropDown("Privacy", privacyDropdown, editor.workspace["pipelinePrivacy"], false, false);
        
        this._addTextBox("Version Comment", editor.workspace["pipelineVersionComment"], false, false);
        this._addFileUpload("Documentation", editor.workspace["pipelineDocumentation"], false, false, false);
        this._addFileUpload("License", editor.workspace["pipelineLicense"], false, false, false);
        this._addPWRDisplayButton();
        this._addSpacerDiv();
        
        // Hack to get layout to work correctly in Firefox
        if ($.browser.mozilla) {
        	var height = $(document).height() - 142;
        	$("#properties").height(height);
        }
    },

    _writeTitle: function(module) {
        var titleDiv = document.createElement("div");
        titleDiv.setAttribute("class", "modulePropertiesTitle");
        titleDiv.innerHTML = module.name;
        $("#" + this.inputDiv).append(titleDiv);
        this._addSpacerDiv();
    },

    displayPWRs: function() {
        // Clean the old selection
        this._deselectOldSelection();
        this._clean();

        this.current = "Prompt When Run";
        this._setTitle("Prompt When Runs");

        for (var i in editor.workspace) {
            var module = editor.workspace[i];
            var wroteTitle = false;

            // Exit if this is not a true module
            if (!(module instanceof Module)) { continue; }
            if (module.isFile()) { continue; }

            for (var j = 0; j < module.inputs.length; j++) {
                var input = module.inputs[j];
                if (input.promptWhenRun !== null) {
                    if (!wroteTitle) {
                        this._writeTitle(module);
                        wroteTitle = true;
                    }
                    this._addFileInput(input);
                }
            }
        }

        // Hide check boxes
        $(".propertyCheckBox").hide();
        
        // Hack to get layout to work correctly in Firefox
        if ($.browser.mozilla) {
        	var height = $(document).height() - 142;
        	$("#properties").height(height);
        }
    }
};

/**
 * Class representing an alert of which  the user should be made aware.
 * @param module - The module of the prompt when run parameter
 * @param param - The InputParam object associated with this PWRParam
 * @param name - The display name of the PWR parameter
 * @param description - The display description of the PWR parameter
 */
function PWRParam(module, param, name, description) {
    this.module = module;
    this.param = param;
    this.name = name;
    this.description = description;

    this.prepTransport = function() {
        return [this.name, this.description];
    };
}

/**
 * Class representing an alert of which  the user should be made aware.
 * @param key - Something to key the alert off of
 * @param level - The level of the alert set by a constant on this object
 * @param message - A message to display to the user
 */
function Alert(key, level, message) {
    this.ERROR = "ERROR";
    this.WARNING = "WARNING";

    this.key = key;
    this.level = level;
    this.message = message;

    this.getIcon = function() {
        var icon = document.createElement("img");

        if (this.level === this.ERROR) {
            icon.setAttribute("src", "images/error.gif");
            icon.setAttribute("class", "alertButton");
            icon.setAttribute("alt", "Module Error");
            icon.setAttribute("title", "Module Error");
        }
        else {
            icon.setAttribute("src", "images/alert.gif");
            icon.setAttribute("class", "alertButton");
            icon.setAttribute("alt", "Module Warning");
            icon.setAttribute("title", "Module Warning");
        }

        return icon;
    };
}

/**
 * Class representing an available normal module for use in the editor
 * @param moduleJSON - A JSON representation of the module
 */
function Module(moduleJSON) {
	this.json = moduleJSON;
	this.id = null;
	this.name = moduleJSON.name;
	this.lsid = moduleJSON.lsid;
	this.version = moduleJSON.version;
    this.category = moduleJSON.category;
    this.write = moduleJSON.write;
	this.outputs = moduleJSON.outputs;
	this.outputEnds = [];
	this.inputEnds = [];
	this.inputs = library.extractInputs(this, moduleJSON.inputs);
	this.fileInputs = library.extractFileInputs(this.inputs);
	this.type = "module";
	this.ui = null;
    this.alerts = {};
    this.blackBox = false;

    this.getCorrectOutput = function(port) {
        if (port.isMaster()) {
            var pointer = $(this.ui).find("input[type=text]").val();
            if (this.hasOutputByPointer(pointer)) {
                return this.getOutputByPointer(pointer);
            }
            else {
                return this.addOutput(pointer);
            }
        }
        else {
            return port;
        }
    };

    this.calculateHeight = function(expanded) {
        if (expanded === undefined) expanded = true;

        var LINE_HEIGHT = 23;
        var height = 0;

        // Add from inputs
        height += LINE_HEIGHT * this.fileInputs.length;

        // Add from spacing
        height += 20;

        // Add from more/less tab
        height += LINE_HEIGHT;

        // Add from typed outputs
        height += LINE_HEIGHT * this.outputs.length;

        if (expanded) {
            // Add from numbered outputs
            height += LINE_HEIGHT * 4;

            // Add from scatter gather
            height += LINE_HEIGHT * 2;
        }

        return height;
    };

    this._getInputIndex = function(pointer) {
        for (var i = 0; i < this.fileInputs.length; i++) {
            var input = this.fileInputs[i];
            if (input.name === pointer) {
                return i;
            }
        }

        editor.log("ERROR: Finding input param in Module._getInputIndex()");
        return null;
    };

    this.calculatePosition = function(isOutput, pointer) {
        var LINE_HEIGHT = 22.8;
        if (isOutput) {
            //noinspection JSDuplicatedDeclaration
            var position = LINE_HEIGHT + 13 + (this.fileInputs.length * LINE_HEIGHT) + 9;
                position += this.outputEnds.length * (LINE_HEIGHT + 3);
            return position;
        }
        else {
            //noinspection JSDuplicatedDeclaration
            var position = LINE_HEIGHT + 13;
            position += this._getInputIndex(pointer) * LINE_HEIGHT;
            return position;
        }
    };

    this.getInputModules = function() {
        var inputModules = new Array();
        for (var i = 0; i < this.inputEnds.length; i++) {
            var port = this.inputEnds[i];
            if (port.isConnected()) {
                var module = port.pipes[0].outputModule;
                // Only add the module to the list if it's not already in the list
                var inList = false;
                for (var j = 0; j < inputModules.length; j++) {
                    if (inputModules[j] === module) {
                        inList = true;
                    }
                }
                if (!inList) {
                    inputModules.push(module);
                }
            }
        }

        return inputModules;
    };

    this.isUpstream = function(module) {
        var isUpstream = false;
        var upstreamModules = this.getInputModules();
        for (var i = 0; i < upstreamModules.length; i++) {
            if (upstreamModules[i] === module) {
                isUpstream = true;
                break;
            }
            if (upstreamModules[i].isUpstream(module)) {
                isUpstream = true;
                break;
            }
        }

        return isUpstream;
    };

    this.isHighestVersion = function() {
        if (this.isFile()) {
            return true;
        }

        var highest = library.getHighestVersion(this.lsid);
        if (highest === null) {
            editor.log("WARNING: No module found in library for: " + this.name);
            return true;
        }
        return highest.lsid === this.lsid;
    };

    this.getInputByName = function(name) {
        for (var i = 0; i < this.inputs.length; i++) {
            if (this.inputs[i].name === name) {
                return this.inputs[i];
            }
        }

        editor.log("ERROR: Was unable to find an input in " + this.name + " named: " + name);
        return null;
    };

    this.isVisualizer = function() {
        return this.type == "module visualizer";
    };

    this.isPipeline = function() {
        return this.type == "module pipeline";
    };

    this.isFile = function() {
        return this.type == "module file";
    };

    this._loadInputs = function(inputs) {
        if (this.inputs.length != inputs.length) {
            editor.log("ERROR: Inputs lengths do not match when loading: " + this.name);
            return;
        }

        for (var i = 0; i < this.inputs.length; i++) {
            this.inputs[i].loadProps(inputs[i]);
        }
    };

    this.loadProps = function(props) {
        this.id = props["id"];
        this._loadInputs(props["inputs"]);
    };

    this._prepInputs = function() {
        var transport = [];
        for (var i = 0; i < this.inputs.length; i++) {
            transport[transport.length] = this.inputs[i].prepTransport();
        }
        return transport;
    };

    this.prepTransport = function() {
        var transport = {};
        transport["id"] = this.id;
        transport["lsid"] = this.lsid;
        transport["inputs"] = this._prepInputs();
        transport["top"] = this.ui.style.top;
        transport["left"] = this.ui.style.left;
        return transport;
    };

    this.saveProps = function(save) {
        for (var i = 0; i < this.inputs.length; i++) {
            var value = save[this.inputs[i].name];
            if (value === null) continue;
            if (value == properties.PROMPT_WHEN_RUN) {
                // If prompt when run it has already been saved, so do nothing
            }
            else {
                this.inputs[i].makeNotPWR();
                if (!(this.inputs[i].isFile() && value === "")) {
                    this.inputs[i].value = value;
                }
                // Set the file icon if necessary
                if (value !== "" && this.inputs[i].isFile()) {
                    this.inputs[i].value = value;
                }
                if (this.inputs[i].isFile() && value === "" && this.inputs[i].value === properties.PROMPT_WHEN_RUN) {
                    this.inputs[i].value = value;
                }
            }
        }
        this.checkForWarnings();

        // Mark the workspace as dirty
        editor.makeDirty();
    };

    this.highestAlert = function() {
        var toReturn = null;
        for (var i in this.alerts) {
            if (toReturn === null) { toReturn = this.alerts[i]; continue; }
            if (toReturn.level === toReturn.ERROR) { return toReturn; }
        }
        return toReturn;
    };

    this.checkForWarnings = function() {
        this.alerts = {};
        var showAlertDisplay = false;

        // Mark the error flag if there is a missing required param
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.inputs.length; i++) {
            //noinspection JSDuplicatedDeclaration
            var input = this.inputs[i];
            if (input.required && input.value === "" && !input.used && input.promptWhenRun === null) {
                showAlertDisplay = true;
                this.alerts[input.name] = new Alert(input, "ERROR", input.name + " is not set!");
            }
        }

        // Check to see if the module is the latest version
        if (!this.isHighestVersion()) {
            showAlertDisplay = true;
            this.alerts[this.name] = new Alert(this, "WARNING", "This module is version " + this.version + " which is not the latest version.");
        }

        // Display black box warning
        if (this.blackBox) {
            showAlertDisplay = true;
            this.alerts[this.name] = new Alert("Missing Module", "WARNING", "GenePattern is unable to load this module.  You will be unable to change settings on this module or otherwise work with it.");
        }

        // Check for incompatible kinds
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.inputEnds.length; i++) {
            //noinspection JSDuplicatedDeclaration
            var input = this.inputEnds[i];
            if (input.isConnected() && !input.param.isCompatible(input.pipes[0].outputPort)) {
                showAlertDisplay = true;
                this.alerts[input.name] = new Alert(input, "WARNING", input.param.name + " is receiving a file of a incompatible type.");
            }
        }

        // Display icons if appropriate
        if (showAlertDisplay) {
            this.toggleAlertDisplay(showAlertDisplay);
        }
        else {
            this.toggleAlertDisplay(false);
        }
    };

    this.hasInputByPointer = function(pointer) {
        for (var i = 0; i < this.inputEnds.length; i++) {
            if (pointer == this.inputEnds[i].pointer) {
                return true;
            }
        }

        return false;
    };

    this.hasOutputByPointer = function(pointer) {
        for (var i = 0; i < this.outputEnds.length; i++) {
            if (pointer == this.outputEnds[i].pointer) {
                return true;
            }
        }

        return false;
    };

    this.hasPortByPointer = function(pointer) {
        return this.hasInputByPointer(pointer) || this.hasOutputByPointer(pointer);
    };

    this.getInputByPointer = function (pointer) {
        for (var i = 0; i < this.inputEnds.length; i++) {
            if (pointer == this.inputEnds[i].pointer) {
                return this.inputEnds[i];
            }
        }

        editor.log("WARNING: Unable to find input port with pointer: " + pointer + " in module " + this.id);
        return null;
    };

    this.getOutputByPointer = function (pointer) {
        for (var i = 0; i < this.outputEnds.length; i++) {
            if (pointer == this.outputEnds[i].pointer) {
                return this.outputEnds[i];
            }
        }

        editor.log("WARNING: Unable to find output port with pointer: " + pointer + " in module " + this.id);
        return null;
    };

    this.getPortByPointer = function (pointer) {
        var port = this.getInputByPointer(pointer);
        if (port !== null) return port;

        port = this.getOutputByPointer(pointer);
        if (port !== null) return port;

        // Handle the case of BlackBox Modules
        if (this.blackBox) {
            var index = this.inputEnds.length;
            var param = library._createBlackBoxParam(this, pointer);
            this.inputEnds[index] = new Input(this, param);
            return this.inputEnds[index];
        }

        editor.log("WARNING: Unable to find port with pointer: " + pointer + " in module " + this.id);
        return null;
    };

	this.getPort = function (id) {
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.inputEnds.length; i++) {
            if (id == this.inputEnds[i].id) {
                return this.inputEnds[i];
            }
        }

        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.outputEnds.length; i++) {
            if (id == this.outputEnds[i].id) {
                return this.outputEnds[i];
            }
        }

        editor.log("Unable to find port with id: " + id + " in module " + this.id);
    };

	this._createButtons = function (appendTo) {
        var docButton = document.createElement("button");
        docButton.setAttribute("id", "doc_" + this.id);
        docButton.setAttribute("class", "saveLoadButton");
        var docIcon = document.createElement("img");
        docIcon.setAttribute("src", "images/file.gif");
        docIcon.setAttribute("class", "fileButton topRowButton");
        docIcon.setAttribute("alt", "Documentation");
        docIcon.setAttribute("title", "Documentation");
        docButton.appendChild(docIcon);
        docButton.innerHTML += "Documentation";
        appendTo.appendChild(docButton);
        $(docButton).button();

        var openButtonBig = document.createElement("button");
        openButtonBig.setAttribute("id", "open_" + this.id);
        openButtonBig.setAttribute("class", "saveLoadButton");
        var openButton = document.createElement("img");
        openButton.setAttribute("src", "images/open.gif");
        openButton.setAttribute("class", "openButton topRowButton");
        openButton.setAttribute("alt", "Open Module");
        openButton.setAttribute("title", "Open Pipeline in Designer");
        openButtonBig.appendChild(openButton);
        openButtonBig.innerHTML += "Open";
        appendTo.appendChild(openButtonBig);
        $(openButtonBig).button();
        if (!this.isPipeline()) { openButtonBig.style.display = "none"; }

        var alertDiv = document.createElement("div");
        alertDiv.setAttribute("id", "alert_" + this.id);
        alertDiv.setAttribute("class", "alertDiv");
        alertDiv.innerHTML = "WOWIE!";
        appendTo.appendChild(alertDiv);
        alertDiv.style.display = "none";
    };

	this.addModuleButtonCalls = function() {
        $("#" + "doc_" + this.id).click(function() {
            var module = editor.getParentModule(this);
            window.open("/gp/getTaskDoc.jsp?name=" + module.lsid, "_blank");
        });

        $("#" + "open_" + this.id).click(function() {
            var module = editor.getParentModule(this.id);
            if (module.isPipeline()) {
                self.location="/gp/pipeline/index.jsf?lsid=" + module.lsid;
            }
            else {
                self.location="/gp/addTask.jsp?name=" + module.lsid;
            }
        });
    };

    this.select = function() {
        $(this.ui).addClass("ui-selected");
    };

    this.deselect = function() {
        $(this.ui).removeClass("ui-selected");
    };

    /**
     * Called by the properties pane to create the set of buttons for the module
     * @param parentDiv
     * @param baseId
     */
    this.createIconSpace = function(parentDiv, baseId) {
        var iconDiv = document.createElement("div");
        iconDiv.setAttribute("id", "icons_" + baseId);
        iconDiv.setAttribute("class", "iconSpace");
        parentDiv.appendChild(iconDiv);
        return iconDiv;
    };

    this.hasError = function() {
        for (var i in this.alerts) {
            var alert = this.alerts[i];
            if (alert.level === alert.ERROR) {
                return true;
            }
        }

        return false;
    };

    this.toggleAlertDisplay = function(show) {
        var alertDiv = $("#alert_" + this.id);

        // Check to see if this module is displayed, if not exit
        if (alertDiv.length < 1) {
            return;
        }

        // Clear the box
        alertDiv[0].innerHTML = "";

        // Make a list of params to highlight
        var highlight = [];

        //noinspection JSDuplicatedDeclaration
        for (var i in this.alerts) {
            var item = document.createElement("div");
            item.appendChild(this.alerts[i].getIcon());
            item.innerHTML += this.alerts[i].message;
            alertDiv.append(item);

            // Add highlighted to list
            if (this.alerts[i].key instanceof InputParam) { highlight.push(this.alerts[i].key); }
        }

        if (show === undefined) {
            show = !alertDiv.is(":visible");
        }

        if (show) {
            alertDiv.show();

            $(".propertyValue").parent().removeClass("alertParam");
            //noinspection JSDuplicatedDeclaration
            for (var i = 0; i < highlight.length; i++) {
                var name = highlight[i].name + (highlight[i].required ? "*" : "");
                $(".propertyValue[name='" + name + "']").parent().addClass("alertParam");
            }

            if (!this.hasError()) {
                $(this.ui).removeClass("errorModule");
            }
        }
        else {
            alertDiv.hide();
            $(".propertyValue").parent().removeClass("alertParam");

            // Remove module icon styles indicating errors
            $(this.ui).removeClass("errorModule");
            $(this.ui).removeClass("alertModule");
        }
    };

    /**
     * Sets the dirty flag and expand the workspace as needed as the module is dragged
     */
    this._addDragEvents = function() {
        $(this.ui).draggable({
            start: function() {
                editor.makeDirty();
                $(this).addClass("draggedModule");
            },

            stop: function() {
                editor.makeDirty();
                $(this).removeClass("draggedModule");

                // Expand the workspace if necessary
                var module = editor.getParentModule(this);
                editor.expandIfNeeded(module);
                editor.relocateIfNeeded(module);
            }//,

            //containment: "parent"
        });
    };

    this._createInputList = function() {
        var inputList = document.createElement("div");
        inputList.setAttribute("class", "inputListDiv");

        for (var i = 0; i < this.fileInputs.length; i++) {
            var file = this.fileInputs[i];
            var fileDiv = document.createElement("div");
            fileDiv.setAttribute("class", "moduleFileItem");
            fileDiv.setAttribute("id", "inDiv_" + this.id + "_" + file.id);
            fileDiv.innerHTML = library.concatNameForDisplay(file.name, 26) + (file.required ? "*" : "");
            inputList.appendChild(fileDiv);
        }

        return inputList;
    };

    this._createOutputList = function() {
        var outputList = document.createElement("div");
        outputList.setAttribute("class", "outputListDiv");
        return outputList;
    };

    this._createMasterOutput = function() {
        var outputDiv = document.createElement("div");
        outputDiv.setAttribute("class", "moduleFileItem outputFileItem");
        outputDiv.setAttribute("name", "master");

        var comboText = document.createElement("input");
        comboText.setAttribute("type", "text");
        comboText.setAttribute("id", "masterComboText_" + this.id);
        outputDiv.appendChild(comboText);

        var comboSelect = document.createElement("select");
        comboSelect.setAttribute("id", "masterComboSelect_" + this.id);
        outputDiv.appendChild(comboSelect);

        var dropdownList = [];
        var outputs = this.outputs;
        $(outputs).each(function(i) {
            dropdownList.push(outputs[i]);
        });
        dropdownList.push("1st Output");
        dropdownList.push("2nd Output");
        dropdownList.push("3rd Output");
        dropdownList.push("4th Output");
        dropdownList.push("Scatter Each Output");
        dropdownList.push("File List of All Outputs");
        comboText.setAttribute("value", dropdownList[0]);

        for (var i = 0; i < dropdownList.length; i++) {
            var output = dropdownList[i];
            var outputOption = document.createElement("option");
            outputOption.setAttribute("class", "outputOption");
            if (library.isODF(output)) {
                output += " (odf)";
            }
            outputOption.innerHTML = output;
            comboSelect.appendChild(outputOption);
        }

        $(comboSelect).change(function() {
            $(comboText).val($(comboSelect).val());
        });

        var outputList = $(this.ui).find(".outputListDiv");
        outputList.append(outputDiv);
        this.addOutputPort("master", true);
    };

    this.getDescriptor = function() {
        if (this.isFile()) {
            return "File";
        }
        else if (this.isVisualizer()) {
            return "Visualizer";
        }
        else {
            return "Module";
        }
    };

	this._createDiv = function() {
        this.ui = document.createElement("div");
        this.ui.setAttribute("class", this.type);
        if (this.id !== null) {
            this.ui.setAttribute("id", this.id);
        }
        this.ui.setAttribute("name", this.name);

        var titleDiv = document.createElement("div");
        titleDiv.setAttribute("class", "moduleTitle");
        titleDiv.innerHTML = library.concatNameForDisplay(this.name, 22);
        this.ui.appendChild(titleDiv);

        // Create the delete button
        var deleteButton = document.createElement("img");
        deleteButton.setAttribute("id", "del_" + this.id);
        deleteButton.setAttribute("src", "images/delete.gif");
        deleteButton.setAttribute("class", "deleteButton");
        deleteButton.setAttribute("alt", "Remove " + this.getDescriptor());
        deleteButton.setAttribute("title", "Remove " + this.getDescriptor());
        titleDiv.appendChild(deleteButton);

        // Create and append the list of inputs
        var inputList = this._createInputList();
        this.ui.appendChild(inputList);

        // Create and append the master output
        if (!this.isVisualizer()) {
            // Create and append the spacer between inputs and outputs
            var spacerDiv = document.createElement("div");
            spacerDiv.setAttribute("class", "spacerDiv");
            this.ui.appendChild(spacerDiv);

            var outputList = this._createOutputList();
            this.ui.appendChild(outputList);
        }

        // Clicking the div triggers displaying properties
        $(this.ui).click(function () {
            // Hack for FireFox executing this event before properties save
            properties.saveToModel();

            $(this).removeClass("alertModule");
            properties.displayModule(editor.getParentModule(this.id));
            properties.show();
            //event.stopPropagation();
        });

        // Add the tooltip
        $(deleteButton).tooltip({tipClass: "infoTooltip"});

        // Functionality for the delete button
        $(deleteButton).click(function() {
            var module = editor.getParentModule(this.id);
            var confirmed = confirm("Are you sure you want to remove this " + module.getDescriptor() + "?");
            if (confirmed) {
                module.remove();

                // Mark the workspace as dirty
                editor.makeDirty();

                // Remove the tooltip for this delete element, fixes bug in tooltip
                $(".infoTooltip").remove();

                // Remove selection in properties
                properties.displayPipeline();
                properties.show();
            }
        });

        // Display the editor for this module when added
        properties.displayModule(this);
        properties.show();
    };

    this.removeOutput = function(pointer) {
        var foundMatch = false;
        var lastPosition = null;

        for (var i = this.outputEnds.length - 1; i >= 0; i--) {
            var output = this.outputEnds[i];
            if (pointer == output.pointer) {
                // Update variables
                foundMatch = true;
                lastPosition = output.position;

                // Delete the endpoint
                jsPlumb.deleteEndpoint(output.endpoint);

                // Remove port from outputEnds
                this.outputEnds.splice(i, 1);
                continue;
            }

            // Shift the endpoints up
            if (foundMatch) {
                var newPosition = output.position;
                output.reposition(lastPosition);
                lastPosition = newPosition;
            }
        }

        // Remove output div
        var div = $(this.ui).find(".outputFileItem[name='" + pointer + "']");
        div.remove();
        
        // Force connections to endpoints to appear in new position
        jsPlumb.repaintEverything();
    };

    this.addOutput = function(pointer) {
        var outputDiv = document.createElement("div");
        outputDiv.setAttribute("class", "moduleFileItem outputFileItem");
        outputDiv.setAttribute("name", pointer);

        $(outputDiv).text(library.concatNameForDisplay(pointer, 26));

        var outputList = $(this.ui).find(".outputListDiv");
        outputList.prepend(outputDiv);
        return this.addOutputPort(pointer);
    };

	this.addOutputPort = function (pointer, master) {
        var output = new Output(this, pointer);
        if (master) output.master = true;
        this.outputEnds.push(output);
        return output;
    };

    this.shiftOutputsDown = function(newPosition) {
        // Reposition endpoints bottom to top
        var lastPosition = newPosition;
        for (var i = 0; i < this.outputEnds.length; i++) {
            var output = this.outputEnds[i];
            lastPosition = output.reposition(lastPosition);
        }
        
        // Force connections to endpoints to appear in new position
        jsPlumb.repaintEverything();

        // Return the top endpoint position, newly opened
        return lastPosition;
    };

	this.addInput = function(pointer) {
        var param = null;
        if (typeof pointer === "string") {
            var found = false;
            for (var i = 0; i < this.fileInputs.length; i++) {
                if (this.fileInputs[i].name == pointer) {
                    param = this.fileInputs[i];
                    found = true;
                    break;
                }
            }
            if (!found) {
                editor.log("ERROR: Unable to find pointer in Module.inputEnds: " + pointer);
            }
        }
        else if (pointer instanceof InputParam) {
            param = pointer;
        }
        else {
            editor.log("ERROR: Module.addInput() called with invalid param: " + pointer);
            return null;
        }

        var input = new Input(this, param);
        var index = this.inputEnds.length;
        this.inputEnds[index] = input;
        return input;
    };

    this._addInputPorts = function() {
        for (var i = 0; i < this.fileInputs.length; i++) {
            var input = this.fileInputs[i];
            input.port = this.addInput(input);
        }
    };

	this._removePipes = function() {
        while (this.inputEnds.length > 0) {
            this.inputEnds[0].removePipes();
            this.inputEnds[0].remove();
        }

        while (this.outputEnds.length > 0) {
            this.outputEnds[0].removePipes();
            if (this.outputEnds[0] && this.outputEnds[0].isMaster()) {
                this.outputEnds[0].remove();
            }
        }
    };

	this.remove = function() {
        this._removePipes();
        $("#" + this.id).remove();
        editor.removeModule(this.id);
    };

	this.add = function (top, left) {
        var useLayoutManager = false;
        if (top === undefined || left === undefined || top === null || left === null) {
            useLayoutManager = true;
        }

        this._createDiv();

        if (useLayoutManager) {
            var location = editor.suggestLocation(this);
            this.ui.style.top = location["top"] + "px";
            this.ui.style.left = location["left"] + "px";
        }
        else {
            this.ui.style.top = top;
            this.ui.style.left = left;
        }

        // Add the div to the editor then add ports
        $("#" + editor.div)[0].appendChild(this.ui);
        this._addInputPorts();
        
        if (!this.isVisualizer()) {
        	this._createMasterOutput();
        	
        	// Create the combobox widget
            $("#masterComboText_" + this.id).combobox($("#masterComboSelect_" + this.id));
        }

        jsPlumb.draggable(this.ui);
        this._addDragEvents();
        editor.expandIfNeeded(this);
    };

    this.addInfoTooltips = function() {
        // Add tooltips to add appropriate module buttons
        $(".fileButton").tooltip({tipClass: "infoTooltip"});
        $(".openButton").tooltip({tipClass: "infoTooltip"});
        $(".alertButton").tooltip({tipClass: "infoTooltip"});
        $(".errorButton").tooltip({tipClass: "infoTooltip"});
    };

	this.spawn = function() {
        var clone = new Module(this.json);
        clone.type = this.type;
        return clone;
    };
}

/**
 * Class representing an available pipeline for nesting in the editor
 * @param moduleJSON - A JSON representation of the module
 */
function Pipeline(moduleJSON) {
	var module = new Module(moduleJSON);
	module.type = "module pipeline";
	return module;
}

/**
 * Class representing an available visualizer for use in the editor
 * @param moduleJSON - A JSON representation of the module
 */
function Visualizer(moduleJSON) {
	var module = new Module(moduleJSON);
	module.type = "module visualizer";
	return module;
}

/**
 * Class representing an available file for use in the editor
 * @param name - The name of the file
 * @param path - The path to the file
 */
function File(name, path) {
    this._fixFileName = function(filename) {
        var parts = filename.split("\\");
        return parts[parts.length - 1];
    };
    
    var file = new Module({
        "inputs": [],
        "outputs": [this._fixFileName(name)],
        "name": "",
        "lsid": path,
        "version": "",
        "category": "",
        "id": "",
        "write": true});
    file.type = "module file";
    file.name = "Input File";

    file.prepTransport = function() {
        var transport = {};
        transport["name"] = this.getFilename();
        transport["path"] = this.getPath();
        transport["top"] = this.ui.style.top;
        transport["left"] = this.ui.style.left;
        return transport;
    };

    file.getFilename = function() {
        return this.outputs[0];
    };

    file.getPath = function() {
        return this.lsid;
    };

    file.getDisplayPath = function() {
        if (this.lsid.indexOf("ftp://") !== 0 && this.lsid.indexOf("http://") !== 0) {
            return "Uploaded File: " + this.getFilename();
        }
        else {
            return this.getPath();
        }
    };

    file.calculateHeight = function() {
        return 55;
    };

    file._createMasterOutput = function() {
        var outputDiv = document.createElement("div");
        outputDiv.setAttribute("class", "moduleFileItem outputFileItem");
        outputDiv.innerHTML = library.concatNameForDisplay(file.outputs[0], 30);

        return outputDiv;
    };

    file.add = function (top, left) {
        var useLayoutManager = false;
        if (top === undefined || left === undefined || top === null || left === null) {
            useLayoutManager = true;
        }

        this._createDiv();

        if (useLayoutManager) {
            var location = editor.suggestLocation(this);
            this.ui.style.top = location["top"] + "px";
            this.ui.style.left = location["left"] + "px";
        }
        else {
            this.ui.style.top = top;
            this.ui.style.left = left;
        }

        $("#" + editor.div)[0].appendChild(this.ui);
        this.addOutput(this.getFilename());
        jsPlumb.draggable(this.ui);
        this._addDragEvents();
        editor.expandIfNeeded(this);
    };

    return file;
}

/**
 * Class representing an input parameter for a module
 * @param paramJSON - A JSON representation of the input param
 */
function InputParam(module, paramJSON) {
    this.id = Math.floor(Math.random() * 1000000000000);
    this.module = module;
    this.name = paramJSON.name;
    this.description = paramJSON.description;
    this.type = paramJSON.type;
    this.kinds = paramJSON.kinds;
    this.required = paramJSON.required;
    this.promptWhenRun = null; // Done in the initPWR() below
    this.defaultValue = editor.protectAgainstUndefined(paramJSON.defaultValue);
    this.choices = paramJSON.choices;
    this.used = false;
    this.port = null;
    this.value = editor.protectAgainstUndefined(this.defaultValue);

    this.initPWR = function(pwrJSON) {
        if (pwrJSON !== undefined && pwrJSON !== null) {
            this.makePWR(pwrJSON[0], pwrJSON[1]);
        }
        else {
        	this.promptWhenRun = null;
            this.value = editor.protectAgainstUndefined(this.defaultValue);
        }

        return this.promptWhenRun;
    };
    this.initPWR(paramJSON.promptWhenRun);

    this.makePWR = function(name, desc) {
        if (name === undefined) name = this.name;
        if (desc === undefined) desc = this.description;

        this.promptWhenRun = new PWRParam(this.module, this.name, name, desc);
        this.value = properties.PROMPT_WHEN_RUN;

        // Update the port's icon
        if (this.port !== null && this.port !== undefined) {
            if (!editor.loading) {
                this.port.updateIcon();
            }
        }
    };

    this.isCompatible = function(outputPort) {
        if (outputPort.isAdvanced()) {
            return true;
        }
        
        if (this.kinds.length < 1) {
            return true;
        }

        if (outputPort.module.isFile()) {
            var filename = outputPort.module.getFilename();
            var extension = filename.substr(filename.lastIndexOf('.') + 1);

            if (extension === "odf") {
                return true;
            }

            //noinspection JSDuplicatedDeclaration
            for (var i = 0; i < this.kinds.length; i++) {
                //noinspection JSDuplicatedDeclaration
                var kind = this.kinds[i];
                if (kind === extension) {
                    return true;
                }
            }
            return false;
        }

        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.kinds.length; i++) {
            //noinspection JSDuplicatedDeclaration
            var kind = this.kinds[i];
            if (kind === outputPort.pointer) {
                return true;
            }
        }
        return false;
    };

    this.isFile = function() {
        return this.type === "java.io.File";
    };

    this.makeUsed = function() {
        this.used = true;
    };

    this.makeUnused = function() {
        this.used = false;
        this.value = this.defaultValue;
    };
    
    this.isPWR = function() {
        return this.promptWhenRun !== null;
    };

    this.makeNotPWR = function() {
        this.promptWhenRun = null;
        this.value = "";

        // Update the port's icon
        if (this.port !== null && this.port !== undefined) {
        	this.port.updateIcon();
        }
    };

    this.prepTransport = function() {
        var transport = {};
        transport["name"] = this.name;
        transport["promptWhenRun"] = this.promptWhenRun !== null ? this.promptWhenRun.prepTransport() : null;
        transport["value"] = this.value;
        return transport;
    };

    this.loadProps = function(input) {
        if (this.name != input["name"]) {
            editor.log("ERROR: Mismatched parameter loading properties: " + this.name + " and " + input["name"]);
        }

        this.initPWR(input["promptWhenRun"]);

        // Create file box and draw pipes if necessary
        if (this.isFile() && input["value"] !== "" && input["value"] !== null && input["value"] !== undefined && this.promptWhenRun === null) {
        	input["value"] = editor.fixCloneBug(input["value"]);
            var path = input["value"];
            var name = editor.extractFilename(path);
            var box = editor.getFileBox(path);
            if (box === null) {
                box = editor.addFile(name, path);
            }
            editor.addPipe(this.port, box.outputEnds[0]);
        }

        this.value = input["value"];
    };
}

/**
 * Class representing a port for connecting modules
 * @param module - A reference to the parent module
 * @param pointer - The text to which the port is pointing to in the display of the tooltip
 * @param param - The InputParam associated with the port
 */
function Port(module, pointer, param, id) {
	this.module = module;
	this.id = id;
    this.pointer = pointer;
    this.param = param;
	this.type = null;
    this.position = null;
	this.endpoint = null;
	this.tooltip = null;
	this.pipes = [];
    this.master = false;

	this.init = function() {
        var OUTPUT = { isTarget: false, isSource: true, paintStyle: { fillStyle: "black" } };
        var INPUT = { isTarget: true, isSource: false, paintStyle: { fillStyle: "black", outlineColor:"black", outlineWidth: 0 } };

        // Get the correct base style
        var baseStyle = null;
        if (this.isOutput()) { baseStyle = OUTPUT; }
        else { baseStyle = INPUT; }

        // Get the correct endpoint name prefix
        var prefix = null;
        if (this.isOutput()) { prefix = "out_"; }
        else { prefix = "in_"; }

        // Get the correct number of max connections
        var maxConn = null;
        if (this.isOutput()) { maxConn = -1; }
        else { maxConn = 1; }

        // Create endpoint
        var image = null;
        if (this.isOutput()) {
            image = "images/output.png";
        }
        else {
        	image = "images/input.png";
        }

        // Add the endpoint
        if (this.isOutput()) {
            var newPosition = this.module.calculatePosition(true, pointer);
            this.position = this.module.shiftOutputsDown(newPosition);      // Calculate position
            //noinspection JSDuplicatedDeclaration
            var posArray = [1, 0, 1, 0, 0, this.position];              // Get the correct position array

            this.endpoint = jsPlumb.addEndpoint(this.module.id.toString(), baseStyle, {
                anchor: posArray,
                maxConnections: maxConn,
                dragAllowedWhenFull: false,
                endpoint : [ "Image", {
                    src: image
                }]
            });
        }
        else {
            this.position = this.module.calculatePosition(false, pointer);     // Calculate position
            //noinspection JSDuplicatedDeclaration
            var posArray = [0, 0, -1, 0, 0, this.position];                    // Get the correct position array

            this.endpoint = jsPlumb.addEndpoint(this.module.id.toString(), baseStyle, {
                anchor: posArray,
                maxConnections: maxConn,
                dragAllowedWhenFull: false,
                endpoint : [ "Image", {
                    src: image
                }]
            });
        }

        this.endpoint.canvas.setAttribute("name", prefix + this.id + "_" + this.module.id);
        this.endpoint.canvas.setAttribute("id", prefix + this.id + "_" + this.module.id);

        // Add context menu
        $(this.endpoint.canvas).click(function() {
            var port = editor.getParentPort(this.id);

            // Show the edited port's module
            properties.displayModule(port.module);
            properties.show();

            // Call the appropriate function when port is clicked
            if (port.isOutput()) {
                port.outputClick(port);
            }
            else {
                port.inputClick(port);
            }
        });

        // Add optional class if necessary
        if (!this.isOutput() && !this.isRequired()) {
            $(this.endpoint.canvas).addClass("optionalPort");
        }
    };
    
    this.inputClick = function(port) {
        var buttons = null;
        if (port.isConnected()) {
            buttons = {
                "Remove Connection": function(event) {
                    $(this).dialog("close");
                    $(".editPipeButton[name='" + port.param.name + (port.param.required ? "*" : "") + "']").trigger("click");
                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                }
            };
        }
        else if (port.param.isPWR()) {
            buttons = {
                "Remove Prompt When Run": function(event) {
                    $(this).dialog("close");
                    $(".propertyCheckBox[name='" + port.param.name + (port.param.required ? "*" : "") + "']").trigger("click");
                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                }
            };
        }
        else {
            buttons = {
                "Prompt When Run": function(event) {
                    $(this).dialog("close");

                    $(".propertyCheckBox[name='" + port.param.name + (port.param.required ? "*" : "") + "']").trigger("click");

                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                },
                "Attach File": function(event) {
                    $(this).dialog("close");

                    // Trigger the attach file dialog
                    $("#attachFile").trigger("click");
                    // Add the new doc click event
                    $(".ui-dialog-buttonpane button:contains('OK')").click(function() {
                        var file = editor.getLastFile();
                        editor.addPipe(port, file.outputEnds[0]);
                    });

                    if (event.preventDefault) event.preventDefault();
                    if (event.stopPropagation) event.stopPropagation();
                }
            };
        }

        var dialog = editor.showDialog("Choose Action", "Choose an action for this input parameter below.", buttons);

        // Hack to make the dialog look as requested
        $(dialog).dialog({
            width: "230px",
            height: "100px",
            position: "center"
        });
        $(dialog).parent().find(".ui-dialog-buttonpane").css("border-width", "0 0 0 0");
        $(dialog).hide();
    };
    
    this.outputClick = function(port) {
        // File outputs cannot be changed
        if (port.module.isFile()) return;

        // Master outputs should have different functionality
        if (port.isMaster()) return;

        var buttons = {
            "OK": function(event) {
                // Get selected output
                var selected = $("#dialogText").val();
                var oldSelection = port.pointer;
                var oldInputs = [];
                //noinspection JSDuplicatedDeclaration
                for (var i = 0; i < port.pipes.length; i++) {
                    var pipe = port.pipes[i];
                    oldInputs.push(pipe.inputPort);
                }

                // Close the dialog
                $(this).dialog("close");

                // Delete old selection if new
                if (oldSelection !== selected) {
                    //noinspection JSDuplicatedDeclaration
                    for (var i = 0; i < oldInputs.length; i++) {
                        //noinspection JSDuplicatedDeclaration
                        var input = oldInputs[i];
                        input.removePipes();
                    }
                } // Otherwise, do nothing
                else { return; }

                // Check if that selection already exists
                var exists = null;
                //noinspection JSDuplicatedDeclaration
                for (var i = 0; i < port.module.outputEnds.length; i++) {
                    var outputEnd = port.module.outputEnds[i];
                    if (outputEnd.pointer === selected) {
                        exists = outputEnd;
                    }
                }

                // If so, add to existing selection
                if (exists !== null) {
                    //noinspection JSDuplicatedDeclaration
                    for (var i = 0; i < oldInputs.length; i++) {
                        //noinspection JSDuplicatedDeclaration
                        var input = oldInputs[i];
                        editor.loading = true;
                        editor.addPipe(input, exists);
                        editor.loading = false;
                        input.module.checkForWarnings();
                    }
                }
                // Otherwise, add new selection
                else {
                    var output = port.module.addOutput(selected);
                    //noinspection JSDuplicatedDeclaration
                    for (var i = 0; i < oldInputs.length; i++) {
                        //noinspection JSDuplicatedDeclaration
                        var input = oldInputs[i];
                        editor.loading = true;
                        editor.addPipe(input, output);
                        editor.loading = false;
                        input.module.checkForWarnings();
                    }
                }

                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            },
            "Cancel": function(event) {
                $(this).dialog("close");
                if (event.preventDefault) event.preventDefault();
                if (event.stopPropagation) event.stopPropagation();
            }};

        var dialogDiv = document.createElement("div");
        $(dialogDiv).text("Choose a new output selection below");
        $(dialogDiv).append(document.createElement("br"));

        var dialogText = document.createElement("input");
        dialogText.setAttribute("id", "dialogText");
        dialogText.setAttribute("type", "text");
        $(dialogDiv).append(dialogText);

        var dialogSelect = document.createElement("select");
        dialogSelect.setAttribute("id", "dialogSelect");
        $(dialogDiv).append(dialogSelect);

        var dropdownList = [];
        var outputs = port.module.outputs;
        $(outputs).each(function(i) {
            dropdownList.push(outputs[i]);
        });
        dropdownList.push("1st Output");
        dropdownList.push("2nd Output");
        dropdownList.push("3rd Output");
        dropdownList.push("4th Output");
        dropdownList.push("Scatter Each Output");
        dropdownList.push("File List of All Outputs");
        dialogText.setAttribute("value", port.pointer);

        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < dropdownList.length; i++) {
            var output = dropdownList[i];
            var outputOption = document.createElement("option");
            outputOption.setAttribute("class", "outputOption");
            if (output == port.pointer) {
                outputOption.setAttribute("selected", "true");
            }
            if (library.isODF(output)) {
                output += " (odf)";
            }
            outputOption.innerHTML = output;
            dialogSelect.appendChild(outputOption);
        }

        // Add Remove All Connections button
        $(dialogDiv).append(document.createElement("br"));
        var removeAllButton = document.createElement("button");
        $(removeAllButton).text("Remove All Connections");
        $(removeAllButton).button();
        $(dialogDiv).append(removeAllButton);
        $(removeAllButton).click(function(event) {
            var dialog = $(this).parent().parent();
            $(dialog).dialog("close");

            port.removePipes();

            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        });

        editor.showDialog("Change Selection", dialogDiv, buttons);

        // Create the combobox widget
        $("#dialogText").combobox($("#dialogSelect"));
    };

    this.updateIcon = function() {
        // Protect against unset params
        if (this.param === null) {
            editor.log("Port.updateIcon() called for a null param: " + this.pointer);
        }

        if (this.param.isPWR()) {
            this.endpoint.canvas.setAttribute("src", "images/pwr.jpeg");
        }
        else {
            this.endpoint.canvas.setAttribute("src", "images/input.png");
        }
    };

    this.reposition = function(top) {
        var oldPosition = this.endpoint.anchor.offsets[1];
        this.position = top;
        this.endpoint.anchor.offsets[1] = top;

        // Force endpoints to appear in new position
        this.endpoint.repaint();

        return oldPosition;
    };

    this.removePipe = function(pipe) {
        for (var i = 0; i < this.pipes.length; i++) {
            if (this.pipes[i] === pipe) {
                // Remove the pipe from the array
                this.pipes.splice(i, 1);
                return;
            }
        }
    };

    this.disableDrag = function() {
        this.endpoint.setEnabled(false);
    };

    this.enableDrag = function() {
        this.endpoint.setEnabled(true);
    };

	this.connectPipe = function(pipe) {
        this.pipes.push(pipe);
    };

    this.isRequired = function() {
        if (this.isInput()) {
            if (this.param === null) {
                return false;
            }
            else {
                return this.param.required;
            }
        }
        else {
            return false;
        }
    };

	this.isConnected = function() {
        return this.pipes.length > 0;
    };

	this.isOutput = function() {
        return this.type == "output";
    };

	this.isInput = function() {
        return this.type == "input";
    };

    this.isMaster = function() {
        return this.master;
    };

    this.isAdvanced = function() {
        for (var i = 0; i < this.module.outputs.length; i++) {
            var kind = this.module.outputs[i];
            if (this.pointer == kind) return false;
        }
        return true;
    };

    this.removePipes = function() {
        while (this.pipes.length > 0) {
            this.pipes[0].remove();
        }
    };

	this.remove = function() {
        jsPlumb.deleteEndpoint(this.endpoint);
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.module.inputEnds.length; i++) {
            if (this.module.inputEnds[i] == this) {
                this.module.inputEnds.splice(i, 1);
                $(this.tooltip).remove();
                return;
            }
        }

        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.module.outputEnds.length; i++) {
            if (this.module.outputEnds[i] == this) {
                this.module.outputEnds.splice(i, 1);
                $(this.tooltip).remove();
                return;
            }
        }

        editor.log("ERROR: Attempted to remove endpoint which was not found");
    };

	this.detachAll = function() {
        this.pipes = [];
        this.endpoint.detachAll();
    };

	this._createButtons = function (appendTo, baseId) {
        var propertiesButton = document.createElement("img");
        propertiesButton.setAttribute("id", "prop_" + baseId);
        propertiesButton.setAttribute("src", "images/pencil.gif");
        propertiesButton.setAttribute("class", "propertiesButton");
        propertiesButton.setAttribute("title", "Edit Connection");
        appendTo.appendChild(propertiesButton);

        var deleteButton = document.createElement("img");
        deleteButton.setAttribute("id", "del_" + baseId);
        deleteButton.setAttribute("src", "images/delete.gif");
        deleteButton.setAttribute("class", "deleteButton");
        deleteButton.setAttribute("title", "Remove Connection");
        appendTo.appendChild(deleteButton);
    };

    this.getInput = function() {
        if (!this.isInput()) {
            editor.log("ERROR: Attempted to getInput() on a non-input port.");
            return null;
        }

        for (var i = 0; i < this.module.fileInputs.length; i++) {
            if (this.module.fileInputs[i].name == this.pointer) {
                return this.module.fileInputs[i];
            }
        }

        editor.log("Unable to find the input " + this.pointer + " in getInput() for " + this.module.name);
        return null
    };

    this.setPointer = function(pointer) {
        this.pointer = pointer;
        $("#tip_point_" + this.id)[0].innerHTML = pointer;
    };
}

/**
 * Class an input port on a module
 * @param module - A reference to the parent module
 * @param param - The InputParam for the input port
 */
function Input(module, param) {
    var pointer = null;
    var input = null;
    if (param instanceof InputParam) {
        pointer = param.name;
        input = param;
    }
    else {
        pointer = param;
    }

	var port = new Port(module, pointer, input, input.id);
	port.type = "input";
	port.init();
	return port;
}

/**
 * Class an output port on a module
 * @param module - A reference to the parent module
 * @param pointer - The output the port is for
 */
function Output(module, pointer) {
	var port = new Port(module, pointer, null, pointer);
	port.type = "output";
	port.init();
	return port;
}

/**
 * Class representing a connection between two modules
 */
function Pipe(connection) {
	this.connection = connection;
	this.outputModule = null;
	this.inputModule = null;
	this.inputPort = null;
	this.outputPort = null;
	this.ui = null;

	this._init = function (connection) {
        // Set output module
        this.outputModule = editor.getParentModule(connection.endpoints[0].canvas.getAttribute("name"));
        // Set input module
        this.inputModule = editor.getParentModule(connection.endpoints[1].canvas.getAttribute("name"));
        // Set output port
        this.outputPort = editor.getParentPort(connection.endpoints[0].canvas.getAttribute("name"));
        // Set input port
        this.inputPort = editor.getParentPort(connection.endpoints[1].canvas.getAttribute("name"));
        // Set canvas UI element
        this.ui = connection.canvas;
    };
	this._init(connection);

    this.select = function() {
        // Hack to get around jquery's inability to set class on SVG elements
        this.ui.setAttribute("class", this.ui.getAttribute("class") + "ui-selected");
        //$(this.ui).addClass("ui-selected");
    };

    this.deselect = function() {
        // Hack to get around jquery's inability to set class on SVG elements
        this.ui.setAttribute("class", "_jsPlumb_connector ");
        //$(this.ui).removeClass("ui-selected");
    };

	this.remove = function() {
        // Make the input port draggable
        this.inputPort.enableDrag();

        // Mark the deleted input port as no longer used
        this.inputPort.getInput().makeUnused();
		this.inputPort.detachAll();

        // Remove pipe from input and output ports
        this.outputPort.removePipe(this);
        this.inputPort.removePipe(this);

        // If the output port is empty, remove the output
        if (!this.outputPort.isConnected()) {
            this.outputPort.module.removeOutput(this.outputPort.pointer);
        }

		this.inputModule.checkForWarnings();
		editor.removePipe(this);
	};

    this.saveProps = function(save) {
        this.outputPort.setPointer(save["Output"]);
        this.inputPort.setPointer(save["Input"]);
        //this.inputPort.param.promptWhenRun = null;

        // Determine if the old port was required or not
        var oldReq = this.inputPort.isRequired();

        // Set the old param to no longer used
        this.inputPort.param.makeUnused();

        // Get the new param from the module
        var newParam = this.inputModule.getInputByName(this.inputPort.pointer);

        // Set this port to the new param and mark it used
        newParam.makeUsed(this.inputPort);

        // Set the new param on this port
        this.inputPort.param = newParam;

        // Determine if the new port is required or not
        var newReq = this.inputPort.isRequired();

        // Check the input module for errors now
        this.inputModule.checkForWarnings();

        // Flip the port's display between optional and required if necessary
        if (oldReq && !newReq) {
            $(this.inputPort.endpoint.canvas).addClass("optionalPort");
        }
        if (!oldReq && newReq) {
            $(this.inputPort.endpoint.canvas).removeClass("optionalPort");
        }

        // Mark the workspace as dirty
        editor.makeDirty();
    };

    this.prepTransport = function() {
        var transport = {};
        transport["outputModule"] = this.outputModule.id;
        transport["outputPort"] = this.outputPort.pointer;
        transport["inputModule"] = this.inputModule.id;
        transport["inputPort"] = this.inputPort.pointer;
        return transport;
    };
}