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
	workspace: {			// A map containing all the instance data of the current workspace
		idCounter: 0, 		// Used to keep track of module instance IDs
		pipes: [],	        // A list of all current connections in the workspace
        files: [],          // List of all uploaded files, used when saving or uploading
		suggestRow: 0, 		// Used by the GridLayoutManager
		suggestCol: 0,		// Used by the GridLayoutManager

        pipelineName: "UntitledPipeline" + Math.floor(Math.random() * 10000),
        pipelineDescription: "",
        pipelineAuthor: "",
        pipelinePrivacy: "private",
        pipelineVersion: "1",
        pipelineVersionComment: "",
        pipelineDocumentation: "",
        pipelineLsid: ""
	},

	init: function() {
		jsPlumb.Defaults.Connector = ["Bezier", { curviness:50 }];
		jsPlumb.Defaults.DragOptions = { cursor: "pointer", zIndex:2000 };
		jsPlumb.Defaults.PaintStyle = { strokeStyle:"black", lineWidth:2 };
		jsPlumb.Defaults.EndpointStyle = { radius:9, fillStyle:"black" };
		jsPlumb.Defaults.Anchors =  ["BottomCenter", "TopCenter"];
		jsPlumb.Defaults.Overlays =  [[ "Arrow", { location:0.9 } ]];
		jsPlumb.Defaults.MaxConnections = -1;

		jsPlumb.bind("jsPlumbConnection", function(event) {
			var pipe = new Pipe(event.connection);
			if (pipe.toMaster()) {
				var output = pipe.outputPort;
				var input = pipe.inputPort;
				editor.addDefaultPipe(output, input);
			}
		});

        editor._setPipelineName();
        $("#pipelinePencil").click(function() {
            properties.displayPipeline();
            properties.show();
        });
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
            pipes: [],	        // A list of all current connections in the workspace
            files: [],          // List of all uploaded files, used when saving or uploading
            suggestRow: 0, 		// Used by the GridLayoutManager
            suggestCol: 0,		// Used by the GridLayoutManager

            pipelineName: "UntitledPipeline" + Math.floor(Math.random() * 10000),
            pipelineDescription: "",
            pipelineAuthor: "",
            pipelinePrivacy: "private",
            pipelineVersion: "1",
            pipelineVersionComment: "",
            pipelineDocumentation: "",
            pipelineLsid: ""
        }
    },

    _setPipelineName: function() {
        $("#" + this.titleSpan)[0].innerHTML = this.workspace["pipelineName"] + " v" + this.workspace["pipelineVersion"];
    },

    addPipe: function(newIn, newOut) {
        var connection = editor.addConnection(newOut, newIn);
        var newPipe = new Pipe(connection);
        newIn.connectPipe(newPipe);
        newOut.connectPipe(newPipe);

        editor.workspace["pipes"].push(newPipe);
        return newPipe;
    },

    addDefaultPipe: function(output, input) {
        // If the pipe was drawn to the same module or a module upstream, cancel the pipe
        if (output.module === input.module) {
            output.module.getMasterOutput().detachAll();
            return;
        }
        if (output.module.isUpstream(input.module)) {
            output.module.getMasterOutput().detachAll();
            return;
        }

		var newIn = input.module.suggestInput(output);
        newIn.param.makeNotPWR();

		// If there are no valid inputs left return null and cancel the pipe
		if (newIn === null) {
			output.module.getMasterOutput().detachAll();
			return;
		}

		// Select the correct output port
		var newOut = null;
		if (output.master) {
			newOut = output.module.suggestOutput(newIn);
		}
		else {
			newOut = output;
		}

        var newPipe = this.addPipe(newIn, newOut);

        input.module.getMasterInput().detachAll();
        input.module.checkForWarnings();
	},

	_nextId: function() {
		this.workspace["idCounter"]++;
		return this.workspace["idCounter"] - 1;
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
			console.log("getParentModule() received null value");
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
			console.log("getParentPort() received null value");
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
        console.log("ERROR: Attempted to remove pipe not found in the workspace");
	},

    _addModule: function(lsid, id, top, left) {
        var module = library.modules[lsid];
        if (module === undefined || module === null) {
            console.log("Error adding module: " + lsid);
            return null;
        }
        var spawn = module.spawn();
        spawn.id = id;
        this.workspace[spawn.id] = spawn;
        spawn.add(top, left);
        spawn.checkForWarnings();
        return spawn;
    },

    loadModule: function(lsid, id) {
        return this._addModule(lsid, id, null, null);
    },

    loadModule: function(lsid, id, top, left) {
        return this._addModule(lsid, id, top, left);
    },

	addModule: function(lsid) {
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
            console.log("Error adding module: " + name);
            return;
        }
        var spawn = module.spawn();
        spawn.id = this._nextId();
        this.workspace[spawn.id] = spawn;
        spawn.add(null, null);
        return spawn;
    },

	addConnection: function(source, target) {
		return jsPlumb.connect({"source": source.endpoint, "target": target.endpoint});
	},

	_gridLayoutManager: function() {
		var location = { "top": this.workspace.suggestRow * 120, "left": this.workspace.suggestCol * 270 };
		this.workspace.suggestCol++;
		if (this.workspace.suggestCol >= 4) {
			this.workspace.suggestCol = 0;
			this.workspace.suggestRow++;
		}
		return location;
	},

    modulesInWorkspace: function() {
        var count = 0
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                count++;
            }
        }

        return count;
    },

    _tLayoutManager: function(module) {
        // Determine if this is the first module in the layout
        var firstModule = editor.modulesInWorkspace() <= 1;

        // If this is the first module, please it at the top and return
        if (firstModule) {
            this.workspace.suggestRow = 0;
            this.workspace.suggestCol = 0;
            return { "top": 0, "left": 0 };
        }

        // Determine if this module goes below or beside the last one
        var below = true;
        if (module.isVisualizer()) below = false;

        // Update the appropriate position and then return
        if (below) {
            this.workspace.suggestRow++;
            this.workspace.suggestCol = 0;
        }
        else {
            this.workspace.suggestCol++;
        }
        return { "top": this.workspace.suggestRow * 120, "left": this.workspace.suggestCol * 270 };
    },

	suggestLocation: function(module) {
		// Pick your layout manager
		// return this._gridLayoutManager();
        return this._tLayoutManager(module);
	},

    _spacesToPeriods: function(string) {
        return string.replace(/ /g, ".");
    },

    saveProps: function(save) {
        this.workspace["pipelineName"] = this._spacesToPeriods(save["Pipeline Name"]);
        this.workspace["pipelineDescription"] = save["Description"];
        this.workspace["pipelineAuthor"] = save["Author"];
        this.workspace["pipelinePrivacy"] = save["Privacy"];
        this.workspace["pipelineVersionComment"] = save["Version Comment"];
        if (save["Documentation"] !== "") {
            this.workspace["pipelineDocumentation"] = save["Documentation"];
        }
        editor._setPipelineName();
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
        transport["pipelineLsid"] = this.workspace["pipelineLsid"];
        transport["pipelineFiles"] = this.workspace["files"];
        return transport;
    },

    _pipeTransport: function() {
        var transport = {};
        var pipes = editor.workspace["pipes"];
        for (var i = 0; i < pipes.length; i++) {
            transport[i] = pipes[i].prepTransport();
        }
        return transport;
    },

    _moduleTransport: function() {
        var transport = {};
        for (var i in editor.workspace) {
            if (editor.workspace[i] instanceof Module) {
                transport[i] = editor.workspace[i].prepTransport();
            }
        }
        return transport;
    },

    _bundleForSave: function() {
        var json = {};
        json["pipeline"] = editor._pipelineTransport();
        json["pipes"] = editor._pipeTransport();
        json["modules"] = editor._moduleTransport();
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
        this.workspace["pipelineLsid"] = pipeline["pipelineLsid"];
        editor._setPipelineName();
    },

    _loadModules: function(modules) {
        this.removeAllModules();
        var givenAlert = false;

        var i = 0;
        while (modules[i.toString()] !== undefined) {
            // Update the idCounter as necessary
            var module = modules[i.toString()];
            var intId = parseInt(module.id)
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
                if (!givenAlert) {
                    alert("Unable to load one or more of the modules in the pipeline.");
                    givenAlert = true;
                }
                i++;
                continue;
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

            if (!outputModule.hasPortByPointer(outputId)) {
                outputModule.addOutput(outputId);
            }
            if (!inputModule.hasPortByPointer(inputId)) {
                inputModule.addInput(inputId);
            }

            var outputPort = outputModule.getPortByPointer(outputId);
            var inputPort = inputModule.getPortByPointer(inputId);

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
            alert("The pipeline you are editing is not the most recent version of the pipeline available.");
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

	load: function(lsid) {
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
                    alert(error);
                }
                else {
                    editor._cleanWorkspace();
                    editor._loadPipeline(response["pipeline"]);
                    editor._loadModules(response["modules"]);
                    editor._loadPipes(response["pipes"]);
                    editor._validatePipeline();
                }
            },
            dataType: "json"
        });
	},

	save: function() {
        if (editor.hasErrors()) {
            alert("The pipeline being edited has errors.  Please fix the errors before saving.");
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
                var newLsid = response["lsid"];
                if (error !== undefined && error !== null) {
                    alert(error);
                }
                if (message !== undefined && message !== null) {
                    alert(message);
                }
                // Update the LSID upon successful save
                if (newLsid !== undefined && newLsid !== null) {
                    editor.workspace["pipelineLsid"] = newLsid;
                    editor.workspace["pipelineVersion"] = editor.extractLsidVersion(newLsid);
                    editor.workspace["pipelineVersionComment"] = "";
                    editor._cleanAfterSave();
                }
                // Add new version to the modules map
                // TODO: Add in this funtionality in a way that works
                //if (newLsid !== undefined && newLsid !== null) {
                //    var baseLsid = editor.extractBaseLsid(newLsid);
                //    library.moduleVersionMap[baseLsid].push(editor.bundleAsModule());
                //    library.loadInit = false;
                //}
            },
            dataType: "json"
        });
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
        this._readModules(moduleJSON);
        this._readModuleVersions();
        this._readModuleNames();
        this._readModuleCategories();

        this._addModuleComboBox();
        this._addCategoryModules();
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
        for (var cat in library.moduleCategoryMap) {
            var catDiv = document.createElement("div");
            catDiv.setAttribute("class", "categoryHeader");
            catDiv.innerHTML = properties._encodeToHTML(cat);

            for (var i = 0; i < library.moduleCategoryMap[cat].length; i++) {
                var module = library.moduleCategoryMap[cat][i];
                var modDiv = document.createElement("div");
                modDiv.setAttribute("class", "moduleBullet");
                modDiv.setAttribute("name", module.lsid);
                modDiv.innerHTML = "<a href='#' onclick='return false;'>" + properties._encodeToHTML(this.concatNameForDisplay(module.name, 22)) + "</a>";
                $(modDiv).click(function() {
                    var lsid = $(this)[0].getAttribute("name");

                    // If unable to find the lsid give up, an error has already been reported
                    if (lsid === null) return;

                    var module = editor.addModule(lsid);

                    // Scroll page to new module
                    $("html, body").animate({ scrollTop: module.ui.style.top }, "slow");
                });
                catDiv.appendChild(modDiv);
            }

            $("#categoryModules").append(catDiv);
        }
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
            var version = library._extractVersionFromDropdown(value);
            var lsid = library._lookupLsid(name, version);

            // If unable to find the lsid give up, an error has already been reported
            if (lsid === null) return;

            var module = editor.addModule(lsid);
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

    _lookupLsid: function (name, version) {
        for (var i in library.modules) {
            var module = library.modules[i];

            // Check for naming match
            if (name == module.name) {
                // If version is null, assume this is the match
                if (version === null || version === undefined) {
                    return library.getHighestVersion(module.lsid).lsid;
                }

                // If versions match, we found it!
                if (version === module.version) {
                    return module.lsid;
                }
            }
        }

        // If we got through the loop without finding it, report the error
        console.log("ERROR: Could not find module name: " + name + " and version: " + version + ".");
        return null;
    },

    _extractNameFromDropdown: function(dropdownText) {
        var spaceIndex = dropdownText.lastIndexOf(" v");

        // No space character + v found in dropdown text
        if (spaceIndex == -1) {
            return dropdownText;
        }

        return dropdownText.substr(0, spaceIndex);
    },

    _extractVersionFromDropdown: function(dropdownText) {
        var spaceIndex = dropdownText.lastIndexOf(" v");

        // No space character + v found in dropdown text
        if (spaceIndex == -1) {
            return null;
        }

        var rawVersion = dropdownText.substr(spaceIndex + 2, dropdownText.length);

        if (rawVersion.length < 1)  return null;
        else return rawVersion;
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
        console.log("WARN: library._higherVersion() called on two versions string that are the same");
        return false;
    },

    _addModuleToCategoryMap: function(module) {
        if (this.moduleCategoryMap[module.category] === undefined || this.moduleCategoryMap[module.category] === null) {
            this.moduleCategoryMap[module.category] = new Array();
        }

        this.moduleCategoryMap[module.category].push(module);
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
                console.log("ERROR: Unacceptable length of version array in library._readModuleNames()");
            }

        }
    },

    _sortAlphabetically : function(stringArray) {
        return $(stringArray).sort(function(a, b) {
            var compA = a.toUpperCase();
            var compB = b.toUpperCase();
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
                console.log("Error detecting module type: " + module.name);
            }
        }
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

    extractInputs: function(inputsJSON) {
        var inputs = new Array();
        for (var i = 0; i < inputsJSON.length; i++) {
            inputs[inputs.length] = new InputParam(inputsJSON[i]);
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
            for (var i in library.moduleVersionMap) {
                var module = library.getHighestVersion(i + ":fakeVersion");

                if (module === null || module === undefined) {
                    console.log("ERROR: Reading module array in library.displayLoadDialog()");
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

            for (var i = 0; i < moduleList.length; i++) {
                var module = moduleList[i];

                // Add Pipeline Div to List
                if (module.isPipeline()) {
                    var li = document.createElement("li");
                    li.setAttribute("name", module.lsid);
                    if (module.write) {
                        li.innerHTML = "<img src='images/pipe.jpeg' /> ";
                    }
                    else {
                        li.innerHTML = "<img src='images/readonly.jpeg' /> ";
                    }
                    li.innerHTML += module.name;
                    pipelineList.appendChild(li);
                }
            }

            $(pipelineList).selectable();
            $("#loadPipeline").button();
            $("#loadPipeline").click(function() {
                var lsid = $("#pipelineSelectList .ui-selected")[0].getAttribute("name");
                editor.load(lsid);
                $("#loadPipelineDialog").dialog("close");
            });
            $("#cancelPipeline").button();
            $("#cancelPipeline").click(function() {
                $("#loadPipelineDialog").dialog("close");
            });

            // Set to initialized
            this.loadInit = true;
        }

        $("#loadPipelineDialog").dialog("open");
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
    buttonDiv: "propertiesSubmit",
    current: null,

    init: function() {
        $("#propertiesOk").button();
        $("#propertiesCancel").button();

        $("#propertiesOk").click(function () {
            properties._deselectOldSelection();
            properties.saveToModel();
            properties.hide();
        });

        $("#propertiesCancel").click(function () {
            properties._deselectOldSelection();
            properties.hide();
        });
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
            var save = this._bundleSave();
            this.current.saveProps(save);
        }
        else if (this.current instanceof Pipe) {
            var save = this._bundleSave();
            this.current.saveProps(save);
        }
        else if (this.current instanceof String && this.current == "Pipeline") {
            var save = this._bundleSave();
            editor.saveProps(save);
        }
        else {
            console.log("ERROR: Cannot determine what is being edited to save to the model");
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
                if (inputs[i].getAttribute("type") == "file") {
                    value = value.replace("C:\\fakepath\\", "");
                }
                bundle[name] = value;
            }
        }
        return bundle;
    },

    hide: function() {
        $("#properties").hide("slide", { direction: "right" }, 500);
    },

    show: function() {
        $("#properties").show("slide", { direction: "right" }, 500);
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
        $("#" + this.titleDiv)[0].innerHTML = this._encodeToHTML(title);
    },

    _setSubtitle: function(subtitle) {
        $("#" + this.subtitleDiv)[0].innerHTML = this._encodeToHTML(subtitle);
    },

    _setVersion: function(version) {
        var versionDiv = $("#" + this.versionDiv)[0];
        versionDiv.innerHTML = "v" + this._encodeToHTML(version);
    },

    _setVersionDropdown: function(module) {
        var baseLsid = editor.extractBaseLsid(module.lsid)
        var moduleArray = library.moduleVersionMap[baseLsid];

        var select = document.createElement("select");
        for (var i = 0; i < moduleArray.length; i++) {
            var option = document.createElement("option");
            option.setAttribute("value", module.id + "|" + moduleArray[i].lsid);
            if (moduleArray[i].version == module.version) {
                option.setAttribute("selected", "true");
            }
            option.innerHTML = moduleArray[i].version;
            select.appendChild(option);
        }

        var versionDiv = $("#" + this.versionDiv)[0];
        versionDiv.appendChild(select);
        $(select).change(function(event) {
            var value = event.target.value;
            var parts = value.split("|");
            var doIt = confirm("This will swap out the currently edited module with another version.  Continue?");
            if (doIt) {
                var newModule = editor.replaceModule(parts[0], parts[1]);
                newModule.checkForWarnings();
                properties.displayModule(newModule);
                properties.show();
            }
            else {
                var lsid = editor.workspace[parts[0]].lsid;
                $(this).val(lsid);
            }
        });
    },

    _displayInputKey: function() {
        var key = document.createElement("div");
        key.setAttribute("id", "propertiesKey")
        var hr1 = document.createElement("hr");
        $("#" + this.inputDiv).append(hr1);
        key.innerHTML += "<em>Check for Prompt When Run&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;* Required</em>";
        $("#" + this.inputDiv).append(key);
        var hr2 = document.createElement("hr");
        $("#" + this.inputDiv).append(hr2);
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

    _addFileUpload: function(labelText, value, description, pwr, disabled) {
        var label = document.createElement("div");
        var uploadForm = document.createElement("form");
        uploadForm.setAttribute("name", labelText + "_form");
        uploadForm.setAttribute("action", "/gp/PipelineDesigner/upload");
        uploadForm.setAttribute("method", "POST");
        uploadForm.setAttribute("enctype", "multipart/form-data");
        label.appendChild(uploadForm);

        if (pwr) {
            var checkBox = this._addPromptWhenRun(uploadForm, labelText, value, value !== properties.PROMPT_WHEN_RUN && disabled);
        }

        uploadForm.innerHTML += this._encodeToHTML(labelText) + " ";
        var fileUpload = document.createElement("input");
        fileUpload.setAttribute("type", "file");
        fileUpload.setAttribute("name", labelText);
        fileUpload.setAttribute("class", "propertyValue");
        if (disabled !== undefined && disabled) {
            fileUpload.setAttribute("disabled", "true");
        }
        uploadForm.appendChild(fileUpload);

        // Attach the uploading and done images
        var uploadingImg = document.createElement("img");
        uploadingImg.setAttribute("src", "images/uploading.gif");
        uploadingImg.setAttribute("name", labelText + "_uploading");
        uploadingImg.setAttribute("style", "display: none;");
        uploadForm.appendChild(uploadingImg);
        var doneImg = document.createElement("img");
        doneImg.setAttribute("src", "images/complete.gif");
        doneImg.setAttribute("name", labelText + "_done");
        doneImg.setAttribute("style", "display: none;");
        uploadForm.appendChild(doneImg);

        // If the value has been previously set, attach the value div
        var valueDiv = document.createElement("div");
        valueDiv.setAttribute("class", "fileUploadValue");
        if (value !== null && value !== "" && value !== properties.PROMPT_WHEN_RUN) {
            valueDiv.innerHTML = "<strong>Current Value:</strong> " + properties._encodeToHTML(value);
        }
        label.appendChild(valueDiv);

        $("#" + this.inputDiv).append(label);

        if (description !== null && description !== false) {
            var desc = document.createElement("div");
            desc.setAttribute("class", "inputDescription");
            desc.innerHTML = this._encodeToHTML(description);
            $("#" + this.inputDiv).append(desc);
        }

        var hr = document.createElement("hr");
        $("#" + this.inputDiv).append(hr);

        // When the upload form is submitted, send to the servlet
        $("[name|='" + labelText + "_form']").iframePostForm({
            json : false,
            post : function () {
                $("[name|='" + labelText + "_uploading']").show();
                $("[name|='" + labelText + "_done']").hide();
            },
            complete : function (response) {
                // Work around a bug in the JSON handling of iframe-post-form
                response = $.parseJSON($(response)[0].innerHTML);

                $("[name|='" + labelText + "_uploading']").hide();
                $("[name|='" + labelText + "_done']").show();

                if (response.error !== undefined) {
                    alert(response.error);
                }
                else {
                    editor.workspace["files"].push(response.location);
                    valueDiv.innerHTML = "<strong>Current Value:</strong> " + $("[type=file][name='" + labelText + "']").val();
                }
            }
        });

        // When a file is selected for upload, begin the upload
        $("[name='" + labelText + "'][type=file]").change(function() {
            $("[name='" + labelText + "_form']").submit();
        });

        // When the prompt when run checkbox is checked, enable or disable upload
        if (checkBox !== undefined && checkBox !== null) {
            $(".propertyCheckBox[type='checkbox'][name='" + labelText + "']").change(function() {
                if ($(this).is(":checked")) {
                    $(".propertyValue[type='file'][name='" + labelText + "']")[0].setAttribute("disabled", "true");
                }
                else {
                    $(".propertyValue[type='file'][name='" + labelText + "']")[0].removeAttribute("disabled");
                }
            });
        }

        return fileUpload;
    },

    _addDropDown: function(labelText, values, selected, description, pwr) {
        var label = document.createElement("div");

        if (pwr) {
            var checkBox = this._addPromptWhenRun(label, labelText, selected);
        }

        label.innerHTML += this._encodeToHTML(labelText) + " ";
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

        var hr = document.createElement("hr");
        $("#" + this.inputDiv).append(hr);

        // When the prompt when run checkbox is checked, enable or disable upload
        if (checkBox !== undefined && checkBox !== null) {
            $(".propertyCheckBox[type='checkbox'][name='" + labelText + "']").change(function() {
                if ($(this).is(":checked")) {
                    $("select.propertyValue[name='" + labelText + "']")[0].setAttribute("disabled", "true");
                }
                else {
                    $("select.propertyValue[name='" + labelText + "']")[0].removeAttribute("disabled");
                }
            });
        }

        return select;
    },

    _addTextBox: function(labelText, value, description, pwr) {
        var label = document.createElement("div");

        if (pwr) {
            var checkBox = this._addPromptWhenRun(label, labelText, value);
        }

        label.innerHTML += this._encodeToHTML(labelText) + " ";
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

        var hr = document.createElement("hr");
        $("#" + this.inputDiv).append(hr);

        // When the prompt when run checkbox is checked, enable or disable upload
        if (checkBox !== undefined && checkBox !== null) {
            $(".propertyCheckBox[type='checkbox'][name='" + labelText + "']").change(function() {
                if ($(this).is(":checked")) {
                    $(".propertyValue[type='text'][name='" + labelText + "']")[0].setAttribute("disabled", "true");
                }
                else {
                    $(".propertyValue[type='text'][name='" + labelText + "']")[0].removeAttribute("disabled");
                }
            });
        }

        return inputBox;
    },

    _addFileInput: function(input) {
        var required = input.required ? "*" : "";
        var displayValue = input.promptWhenRun ? properties.PROMPT_WHEN_RUN : input.value;
        var disabled = false;
        if (input.port !== null) {
            displayValue = "Receiving output " + input.port.pipes[0].outputPort.pointer + " from " + input.port.pipes[0].outputModule.name;
            disabled = true;
        }
        if (input.promptWhenRun) {
            disabled = true;
        }
        return this._addFileUpload(input.name + required, displayValue, input.description, true, disabled);
    },

    _addTextBoxInput: function(input) {
        var required = input.required ? "*" : "";
        var displayValue = input.promptWhenRun ? properties.PROMPT_WHEN_RUN : input.value;
        return this._addTextBox(input.name + required, displayValue, input.description, true);
    },

    _addDropdownInput: function(input) {
        var required = input.required ? "*" : "";
        var displayValue = input.promptWhenRun ? properties.PROMPT_WHEN_RUN : input.value;
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

    displayModule: function(module) {
        // Clean the old selection
        this._deselectOldSelection();
        this._clean();

        // Set the new selection
        this.current = module;
        module.select();

        // Build the new display
        this._setTitle(module.name);
        this._setSubtitle(module.lsid);
        this._setVersionDropdown(module);
        this._displayInputKey();
        var inputs = module.inputs;
        for (var i in inputs) {
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
    },

    displayPipe: function(pipe) {
        // Clean the old selection
        this._deselectOldSelection();
        this._clean();

        // Set the new selection
        this.current = pipe;
        this._setTitle(pipe.outputModule.name + " to " + pipe.inputModule.name);
        pipe.select();

        // Build the new display
        var outSelected = pipe.outputPort.pointer;
        var inSelected = pipe.inputPort.pointer;

        var outputOptions = ["1=1st Output", "2=2nd Output", "3=3rd Output", "4=4th Output"];
        outputOptions = outputOptions.concat(pipe.outputModule.outputs);
        if (editor.USE_BETA_OPTIONS) {
            outputOptions = outputOptions.concat(["?scatter&amp;filter&#061;*=Scatter Each Output", "?filelist&amp;filter&#061;*=File List of All Outputs"])
        }

        this._addDropDown("Output", outputOptions, outSelected, properties.listToString(pipe.outputModule.outputs), false);

        var inputsToList = new Array();
        var selectedInput = pipe.inputModule.fileInputs[0];
        for (var i = 0; i < pipe.inputModule.fileInputs.length; i++) {
            inputsToList[inputsToList.length] = pipe.inputModule.fileInputs[i].name;
            if (inSelected == pipe.inputModule.fileInputs[i].name) {
                selectedInput = pipe.inputModule.fileInputs[i];
            }
        }
        var input = this._addDropDown("Input", inputsToList, inSelected, selectedInput.description, false);

        // Display the correct description upon dropdown selection
        $(input).change(function() {
            var selectedInput = $(input).val();
            for (var i = 0; i < pipe.inputModule.fileInputs.length; i++) {
                if (selectedInput == pipe.inputModule.fileInputs[i].name) {
                    selectedInput = pipe.inputModule.fileInputs[i];
                    break;
                }
            }
            $(".inputDescription").get(1).innerHTML = selectedInput.description;
        });
    },

    displayPipeline: function() {
        // Clean the old selection
        this._deselectOldSelection();
        this._clean();

        this.current = new String("Pipeline");
        this._setTitle("Editing Pipeline");
        this._setVersion(editor.workspace["pipelineVersion"]);
        this._setSubtitle(editor.workspace["pipelineLsid"].length > 0 ? editor.workspace["pipelineLsid"] : "");
        this._addTextBox("Pipeline Name", editor.workspace["pipelineName"], false, false);
        this._addTextBox("Description", editor.workspace["pipelineDescription"], false, false);
        this._addTextBox("Author", editor.workspace["pipelineAuthor"], false, false);
        this._addDropDown("Privacy", ["private", "public"], editor.workspace["pipelinePrivacy"], false, false);
        this._addTextBox("Version Comment", editor.workspace["pipelineVersionComment"], false, false);
        this._addFileUpload("Documentation", editor.workspace["pipelineDocumentation"], false, false, false);
    }
};

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
	this.inputs = library.extractInputs(moduleJSON.inputs);
	this.fileInputs = library.extractFileInputs(this.inputs);
	this.type = "module";
	this.ui = null;
    this.alerts = {};

    this.getInputModules = function() {
        var inputModules = new Array();
        for (var i = 0; i < this.inputEnds.length; i++) {
            var port = this.inputEnds[i];
            if (port.isConnected()) {
                var module = port.pipes[0].outputModule;
                // Only add the module to the list if it's not already in the list
                for (var j = 0; j < inputModules.length; j++) {
                    if (inputModules[j] === module) {
                        continue;
                    }
                }
                inputModules.push(module);
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
        var highest = library.getHighestVersion(this.lsid);
        if (highest.lsid === this.lsid) { return true; }
        else { return false }
    };

    this.getInputByName = function(name) {
        for (var i = 0; i < this.inputs.length; i++) {
            if (this.inputs[i].name === name) {
                return this.inputs[i];
            }
        }

        console.log("ERROR: Was unable to find an input in " + this.name + " named: " + name);
        return null;
    };

    this.isVisualizer = function() {
        if (this.type == "module visualizer") return true;
        else return false;
    };

    this.isPipeline = function() {
        if (this.type == "module pipeline") return true;
        else return false;
    };

    this._loadInputs = function(inputs) {
        if (this.inputs.length != inputs.length) {
            console.log("ERROR: Inputs lengths do not match when loading: " + this.name);
            return;
        }

        var showFileIcon = false;
        for (var i = 0; i < this.inputs.length; i++) {
            this.inputs[i].loadProps(inputs[i]);

            // Set the file icon if necessary
            if (inputs[i].value !== "" && this.inputs[i].isFile()) {
                showFileIcon = true;
            }
        }
        this.toggleFileIcon(showFileIcon);
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
        var showFileIcon = false;
        for (var i = 0; i < this.inputs.length; i++) {
            var value = save[this.inputs[i].name];
            if (value === null) continue;
            if (value == properties.PROMPT_WHEN_RUN) {
                this.inputs[i].makePWR();
            }
            else {
                this.inputs[i].promptWhenRun = false;
                if (!(this.inputs[i].isFile() && value === "")) {
                    this.inputs[i].value = value;
                }
                // Set the file icon if necessary
                if (value !== "" && this.inputs[i].isFile()) {
                    this.inputs[i].value = value;
                    showFileIcon = true;
                }
                if (value === "" && this.inputs[i].isFile() && this.inputs[i].value !== "" && this.inputs[i].value !== properties.PROMPT_WHEN_RUN) {
                    showFileIcon = true;
                }
                if (this.inputs[i].isFile() && value === "" && this.inputs[i].value === properties.PROMPT_WHEN_RUN) {
                    this.inputs[i].value = value;
                }
            }
        }
        this.toggleFileIcon(showFileIcon);
        this.checkForWarnings();
    };

    this.checkForWarnings = function() {
        this.alerts = {};

        var showErrorIcon = false;
        var showAlertIcon = false;

        // Mark the error flag if there is a missing required param
        for (var i = 0; i < this.inputs.length; i++) {
            var input = this.inputs[i];
            if (input.required && input.value === "" && !input.used && input.promptWhenRun !== true) {
                showErrorIcon = true;
                this.alerts[input.name] = new Alert(input.name, "ERROR", "Required parameter " + input.name + " is not set!");
            }
        }

        // Check to see if the module is the latest version
        if (!this.isHighestVersion()) {
            showAlertIcon = true;
            this.alerts[this.name] = new Alert("version", "WARNING", "This module is version " + this.version + " which is not the latest version.");
        }

        // Display icons if appropriate
        this.toggleErrorIcon(showErrorIcon);
        if (!showErrorIcon) {
            this.toggleAlertIcon(showAlertIcon);
        }
        else {
            this.toggleAlertIcon(false);
        }
    };

    this.hasPortByPointer = function(pointer) {
        for (var i = 0; i < this.inputEnds.length; i++) {
            if (pointer == this.inputEnds[i].pointer) {
                return true;
            }
        }

        for (var i = 0; i < this.outputEnds.length; i++) {
            if (pointer == this.outputEnds[i].pointer) {
                return true;
            }
        }

        return false;
    };

    this.getPortByPointer = function (pointer) {
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.inputEnds.length; i++) {
            if (pointer == this.inputEnds[i].pointer) {
                return this.inputEnds[i];
            }
        }

        for (var i = 0; i < this.outputEnds.length; i++) {
            if (pointer == this.outputEnds[i].pointer) {
                return this.outputEnds[i];
            }
        }

        console.log("Unable to find port with pointer: " + pointer + " in module " + this.id);
    };

    this.hasPort = function(id) {
        //noinspection JSDuplicatedDeclaration
        for (var i = 0; i < this.inputEnds.length; i++) {
            if (id == this.inputEnds[i].id) {
                return true;
            }
        }

        for (var i = 0; i < this.outputEnds.length; i++) {
            if (id == this.outputEnds[i].id) {
                return true;
            }
        }

        return false;
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

        console.log("Unable to find port with id: " + id + " in module " + this.id);
    };

	this.suggestInput = function (outputPort) {
        for (var i = 0; i < outputPort.module.outputs.length; i++) {
            var output = outputPort.module.outputs[i];

            for (var j = 0; j < this.fileInputs.length; j++) {
                var input = this.fileInputs[j];
                var used = input.used;

                if (!used) {
                    // Special case for modules with no input types listed
                    if (input.kinds.length == 0) {
                        var inputPort = this.addInput(input.name);
                        input.makeUsed(inputPort);
                        return inputPort;
                    }

                    for (var k = 0; k < input.kinds.length; k++) {
                        var kind = input.kinds[k];

                        if (kind == output) {
                            var inputPort = this.addInput(input.name);
                            input.makeUsed(inputPort);
                            return inputPort;
                        }
                    }
                }
            }
        }

        // Special case for modules with no output types listed
        if (outputPort.module.outputs.length <= 0) {
            //noinspection JSDuplicatedDeclaration
            for (var i = 0; i < this.fileInputs.length; i++) {
                var used = this.fileInputs[i].used;
                if (!used) {
                    var inputPort = this.addInput(this.fileInputs[i].name);
                    this.fileInputs[i].makeUsed(inputPort);
                    return inputPort;
                }
            }
        }

        // No valid input found
        return null;
    };

	this.suggestOutput = function (input) {
        for (var i = 0; i < this.outputs.length; i++) {
            var output = this.outputs[i];

            for (var k = 0; k < input.param.kinds.length; k++) {
                var kind = input.param.kinds[k];

                if (kind == output) {
                    return this.addOutput(output);
                }
            }
        }

        // No matched output type found, return first output
        return this.addOutput(1);
    };

	this._createButtons = function (appendTo, baseId) {
        var fileIcon = document.createElement("img");
        fileIcon.setAttribute("id", "file_" + this.id);
        fileIcon.setAttribute("src", "images/file.gif");
        fileIcon.setAttribute("class", "fileButton");
        fileIcon.setAttribute("alt", "Embedded File");
        fileIcon.style.display = "none";
        appendTo.appendChild(fileIcon);

        var propertiesButton = document.createElement("img");
        propertiesButton.setAttribute("id", "prop_" + baseId);
        propertiesButton.setAttribute("src", "images/pencil.gif");
        propertiesButton.setAttribute("class", "propertiesButton");
        propertiesButton.setAttribute("alt", "Edit Properties");
        appendTo.appendChild(propertiesButton);

        var deleteButton = document.createElement("img");
        deleteButton.setAttribute("id", "del_" + baseId);
        deleteButton.setAttribute("src", "images/delete.gif");
        deleteButton.setAttribute("class", "deleteButton");
        deleteButton.setAttribute("alt", "Delete Module");
        appendTo.appendChild(deleteButton);

        var alertIcon = document.createElement("img");
        alertIcon.setAttribute("id", "alert_" + this.id);
        alertIcon.setAttribute("src", "images/alert.gif");
        alertIcon.setAttribute("class", "alertButton");
        alertIcon.setAttribute("alt", "Module Alert");
        alertIcon.style.display = "none";
        appendTo.appendChild(alertIcon);

        var errorIcon = document.createElement("img");
        errorIcon.setAttribute("id", "error_" + this.id);
        errorIcon.setAttribute("src", "images/error.gif");
        errorIcon.setAttribute("class", "errorButton");
        errorIcon.setAttribute("alt", "Module Error");
        errorIcon.style.display = "none";
        appendTo.appendChild(errorIcon);
    };

	this._addModuleButtonCalls = function () {
        $("#" + "prop_" + this.id).click(function () {
            properties.displayModule(editor.getParentModule(this.id));
            properties.show();
        });

        $("#" + "del_" + this.id).click(function () {
            var confirmed = confirm("Are you sure you want to delete this module?");
            if (confirmed) {
                var module = editor.getParentModule(this.id);
                module.remove();
            }
        });

        var alertFunc = function () {
            var module = editor.getParentModule(this.id);
            var alert = document.createElement("div");
            var list = document.createElement("ul");

            for (var i in module.alerts) {
                var item = document.createElement("li");
                item.innerHTML = "<strong>" + module.alerts[i].level + ":</strong> " + module.alerts[i].message;
                list.appendChild(item);
            }
            alert.appendChild(list);

            $(alert).dialog({
                modal: true,
                width: 400,
                title: "Module Errors & Alerts",
                close: function(event) {
                    $(this).dialog("destroy");
                    $(this).remove();
                }
            });
        };

        $("#" + "error_" + this.id).click(alertFunc);
        $("#" + "alert_" + this.id).click(alertFunc);
    };

    this.select = function() {
        $(this.ui).addClass("ui-selected");
    };

    this.deselect = function() {
        $(this.ui).removeClass("ui-selected");
    };

    this._createIconSpace = function(parentDiv, baseId) {
        var iconDiv = document.createElement("div");
        iconDiv.setAttribute("id", "icons_" + baseId);
        iconDiv.setAttribute("class", "iconSpace");
        parentDiv.appendChild(iconDiv);
        return iconDiv;
    };

    this.toggleFileIcon = function(show) {
        if (show === undefined) {
            if ($("#file_" + this.id).is(":visible")) {
                $("#file_" + this.id).hide();
            }
            else {
                $("#file_" + this.id).show();
            }
        }
        else if (show) {
            $("#file_" + this.id).show();
        }
        else {
            $("#file_" + this.id).hide();
        }
    };

    this.hasError = function() {
        return $("#error_" + this.id).is(":visible");
    };

    this.toggleErrorIcon = function(show) {
        if (show === undefined) {
            if ($("#error_" + this.id).is(":visible")) {
                $("#error_" + this.id).hide();
            }
            else {
                $("#error_" + this.id).show();
            }
        }
        else if (show) {
            $("#error_" + this.id).show();
        }
        else {
            $("#error_" + this.id).hide();
        }
    };

    this.toggleAlertIcon = function(show) {
        if (show === undefined) {
            if ($("#alert_" + this.id).is(":visible")) {
                $("#alert_" + this.id).hide();
            }
            else {
                $("#alert_" + this.id).show();
            }
        }
        else if (show) {
            $("#alert_" + this.id).show();
        }
        else {
            $("#alert_" + this.id).hide();
        }
    };

	this._createDiv = function() {
        this.ui = document.createElement("div");
        this.ui.setAttribute("class", this.type);
        if (this.id !== null) {
            this.ui.setAttribute("id", this.id);
        }
        this.ui.setAttribute("name", this.name);
        this.ui.innerHTML = "<br /><br />" + library.concatNameForDisplay(this.name, 24) + "<br />";
        var iconDiv = this._createIconSpace(this.ui, this.id);
        this._createButtons(iconDiv, this.id);
    };

	this.addOutput = function (pointer) {
        var output = new Output(this, pointer);
        var index = this.outputEnds.length;
        this.outputEnds[index] = output;
        return output;
    };

	this._addMasterOutput = function () {
        if (this.type == "module visualizer") {
            return null;
        }
        return this.addOutput("master");
    };

	this.addInput = function (pointer) {
        var param = null;
        if (typeof pointer === "string") {
            if (pointer === "master") {
                param = pointer;
            }
            else {
                var found = false;
                for (var i = 0; i < this.fileInputs.length; i++) {
                    if (this.fileInputs[i].name == pointer) {
                        param = this.fileInputs[i];
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    console.log("ERROR: Unable to find pointer in Module.inputEnds: " + pointer);
                }
            }
        }
        else if (pointer instanceof InputParam) {
            param = pointer;
        }
        else {
            console.log("ERROR: Module.addInput() called with invalid param: " + pointer);
            return null;
        }

        var input = new Input(this, param);
        var index = this.inputEnds.length;
        this.inputEnds[index] = input;
        return input;
    };

	this._addMasterInput = function () {
        return this.addInput("master");
    };

	this._removePipes = function () {
        while (this.inputEnds.length > 0) {
            if (this.inputEnds[0].master) {
                this.inputEnds[0].remove();
                continue;
            }

            if (this.inputEnds[0].isConnected()) {
                this.inputEnds[0].removePipes();
            }
        }

        while (this.outputEnds.length > 0) {
            if (this.outputEnds[0].master) {
                this.outputEnds[0].remove();
                continue;
            }

            if (this.outputEnds[0].isConnected()) {
                this.outputEnds[0].removePipes();
            }
        }
    };

	this.remove = function () {
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

        $("#" + editor.div)[0].appendChild(this.ui);
        this._addMasterOutput();
        this._addMasterInput();
        jsPlumb.draggable(this.ui);
        this._addModuleButtonCalls();
    };

	this.spawn = function () {
        var clone = new Module(this.json);
        clone.type = this.type;
        return clone;
    };

	this.getMasterInput = function () {
        return this.inputEnds[0];
    };

	this.getMasterOutput = function() {
		return this.outputEnds[0];
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
 * Class representing an input parameter for a module
 * @param paramJSON - A JSON representation of the input param
 */
function InputParam(paramJSON) {
    this.name = paramJSON.name;
    this.description = paramJSON.description;
    this.type = paramJSON.type;
    this.kinds = paramJSON.kinds;
    this.required = paramJSON.required;
    this.promptWhenRun = paramJSON.promptWhenRun;
    this.defaultValue = paramJSON.defaultValue;
    this.choices = paramJSON.choices;
    this.used = false;
    this.port = null;
    this.value = this.defaultValue;

    this.isFile = function() {
        return this.type === "java.io.File";
    };

    this.makeUsed = function(port) {
        this.used = true;
        this.port = port;
    };

    this.makeUnused = function() {
        this.used = false;
        this.port = null;
    };

    this.makePWR = function() {
        this.promptWhenRun = true;
        this.value = properties.PROMPT_WHEN_RUN;
    };

    this.makeNotPWR = function() {
        this.promptWhenRun = false;
        this.value = "";
    };

    this.prepTransport = function() {
        var transport = {};
        transport["name"] = this.name;
        transport["promptWhenRun"] = this.promptWhenRun;
        transport["value"] = this.value;
        return transport;
    };

    this.loadProps = function(input) {
        if (this.name != input["name"]) {
            console.log("ERROR: Mismatched parameter loading properties: " + this.name + " and " + input["name"]);
        }
        this.promptWhenRun = input["promptWhenRun"];
        this.value = input["value"];
    };
}

/**
 * Class representing a port for connecting modules
 * @param module - A reference to the parent module
 * @param pointer - The text to which the port is pointing to in the display of the tooltip
 * @param param - The InputParam associated with the port
 */
function Port(module, pointer, param) {
	this.module = module;
	this.id = Math.floor(Math.random() * 1000000000000);
    this.pointer = pointer;
	this.master = pointer == "master";
    this.param = param;
	this.type = null;
	this.endpoint = null;
	this.tooltip = null;
	this.pipes = [];

	this.init = function () {
        var MASTER_OUTPUT = { isSource: true, paintStyle: { fillStyle: "blue" } };
        var OUTPUT = { isSource: true, paintStyle: { fillStyle: "black" } };
        var MASTER_INPUT = { isTarget: true, paintStyle: { fillStyle: "blue" } };
        var INPUT = { isTarget: true, paintStyle: { fillStyle: "black", outlineColor:"black", outlineWidth: 0 } };

        // Get correct list on module for type
        var correctList = null;
        if (this.isOutput()) {
            correctList = this.module.outputEnds;
        }
        else {
            correctList = this.module.inputEnds;
        }

        // Get the correct base style
        var baseStyle = null;
        if (this.isOutput()) {
            if (this.master) {
                baseStyle = MASTER_OUTPUT;
            }
            else {
                baseStyle = OUTPUT;
            }
        }
        else {
            if (this.master) {
                baseStyle = MASTER_INPUT;
            }
            else {
                baseStyle = INPUT;
            }
        }

        // Get the correct endpoint name prefix
        var prefix = null;
        if (this.isOutput()) {
            prefix = "out_";
        }
        else {
            prefix = "in_";
        }

        // Calculate position
        var index = correctList.length;
        var position = 0.1 * (index + 1);

        // Get the correct position array
        var posArray = null;
        if (this.isOutput()) {
            posArray = [position, 1, 0, 1];
        }
        else {
            posArray = [position, 0, 0, -1];
        }

        // Get the correct number of max connections
        var maxConn = null;
        if (this.isOutput()) {
            maxConn = -1;
        }
        else {
            maxConn = 1;
        }

        // Create endpoint
        this.endpoint = jsPlumb.addEndpoint(this.module.id.toString(), baseStyle, {
            anchor: posArray,
            maxConnections: maxConn,
            dragAllowedWhenFull: true
        });
        this.endpoint.canvas.setAttribute("name", prefix + this.id + "_" + this.module.id);
        this.endpoint.canvas.setAttribute("id", prefix + this.id + "_" + this.module.id);

        // Add optional class if necessary
        if (!this.isOutput() && !this.master && !this.isRequired()) {
            $(this.endpoint.canvas).addClass("optionalPort");
        }

        // Add tooltip
        this._createTooltip(this.pointer);
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

    this.removePipes = function() {
        while (this.pipes.length > 0) {
            this.pipes[0].remove();
            this.pipes.shift();
        }
    };

	this.remove = function () {
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

        console.log("ERROR: Attempted to remove endpoint which was not found");
    };

	this.detachAll = function () {
        this.endpoint.detachAll();
    };

	this._createButtons = function (appendTo, baseId) {
        var propertiesButton = document.createElement("img");
        propertiesButton.setAttribute("id", "prop_" + baseId);
        propertiesButton.setAttribute("src", "images/pencil.gif");
        propertiesButton.setAttribute("class", "propertiesButton");
        appendTo.appendChild(propertiesButton);

        var deleteButton = document.createElement("img");
        deleteButton.setAttribute("id", "del_" + baseId);
        deleteButton.setAttribute("src", "images/delete.gif");
        deleteButton.setAttribute("class", "deleteButton");
        appendTo.appendChild(deleteButton);
    };

	this._addTooltipButtonCalls = function (id) {
        $("#" + "prop_" + id).click(function () {
            var port = editor.getParentPort(this);
            properties.displayPipe(port.pipes[0]);
            properties.show();
        });

        $("#" + "del_" + id).click(function () {
            var port = editor.getParentPort(this);
            port.removePipes();
        });
    };

    this.getInput = function() {
        if (!this.isInput()) {
            console.log("ERROR: Attented to getInput() on a non-input port.");
            return null;
        }

        for (var i = 0; i < this.module.fileInputs.length; i++) {
            if (this.module.fileInputs[i].name == this.pointer) {
                return this.module.fileInputs[i];
            }
        }

        console.log("Unable to find the input " + this.pointer + " in getInput() for " + this.module.name);
        return null
    };

    this.setPointer = function(pointer) {
        this.pointer = pointer;
        $("#tip_point_" + this.id)[0].innerHTML = pointer;
    };

	this._createTooltip = function(name) {
		this.tooltip = document.createElement("div");
		this.tooltip.setAttribute("id", "tip_" + this.id + "_" + this.module.id);
		this.tooltip.setAttribute("class", "tooltip");
		if (this.master) {
			if (this.isOutput()) {
				this.tooltip.innerHTML = "Drag Connections From Here";
			}
			else if (this.isInput()) {
				this.tooltip.innerHTML = "Drag Connections To Here";
			}
		}
		else {
			this.tooltip.innerHTML = (this.isOutput() ? "Output Selection:<br />" : "Input Parameter:<br />") + "<span id='tip_point_" + this.id + "'>" + name + "</span><br />";
			if (this.isInput()) {
				this._createButtons(this.tooltip, this.tooltip.getAttribute("id"));
			}
		}
		$("#" + editor.div)[0].appendChild(this.tooltip);
		if (!this.master && this.isInput()) { this._addTooltipButtonCalls(this.tooltip.getAttribute("id")); }
		$("#" + this.endpoint.canvas.id).tooltip({"tip": "#" + this.tooltip.id, "offset": [-100, -195]});
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

	var port = new Port(module, pointer, input);
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
	var port = new Port(module, pointer, null);
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

	this.toMaster = function () {
        return this.inputPort.master;
    };

	this.remove = function() {
        // Mark the deleted input port as no longer used
        this.inputPort.getInput().makeUnused();

		var deleteOutput = this.outputPort.endpoint.connections.length <= 1;
		this.inputPort.detachAll();
		this.inputPort.remove();
		if (deleteOutput) { this.outputPort.remove(); }
		editor.removePipe(this);
	};

    this.saveProps = function(save) {
        this.outputPort.setPointer(save["Output"]);
        this.inputPort.setPointer(save["Input"]);
        this.inputPort.param.promptWhenRun = false;

        // Determine if the old port was required or not
        var oldReq = this.inputPort.isRequired();

        // Set the old param's port to null
        this.inputPort.param.port = null;

        // Get the new param from the module
        var newParam = this.inputModule.getInputByName(this.inputPort.pointer)

        // Set this port to the new param
        newParam.port = this.inputPort;

        // Set the new param on this port
        this.inputPort.param = newParam;

        // Determine if the new port is required or not
        var newReq = this.inputPort.isRequired();

        // Flip the port's display between optional and required if necessary
        if (oldReq && !newReq) {
            $(this.inputPort.endpoint.canvas).addClass("optionalPort");
        }
        if (!oldReq && newReq) {
            $(this.inputPort.endpoint.canvas).removeClass("optionalPort");
        }
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