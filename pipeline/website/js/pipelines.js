/**
 * JavaScript used by the GenePattern Pipeline Editor
 * @requires jQuery, jQuery UI, jsPlumb
 * @author Thorin Tabor
 */

/**
 * Object representing the pipeline editor, containing all associated methods and properties
 */
var editor = {
	OUTPUT_FILE_STYLE: { isSource: true },
	INPUT_FILE_STYLE: { isTarget: true },

	div: "workspace",		// The id of the div used for pipeline editing
    titleSpan: "titleSpan", // The id of the title span of the pipeline
	workspace: {			// A map containing all the instance data of the current workspace
		idCounter: 0, 		// Used to keep track of module instance IDs
		pipes: [],	        // A list of all current connections in the workspace
		suggestRow: 0, 		// Used by the GridLayoutManager
		suggestCol: 0,		// Used by the GridLayoutManager

        pipelineName: "UntitledPipeline",
        pipelineDescription: "",
        pipelineAuthor: "",
        pipelinePrivacy: "private",
        pipelineVersion: 1,
        pipelineVersionComment: "",
        pipelineDocumentation: "",
        pipelineLsid: 0
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

    nameToId: function (name) {
        return name.replace(/\./g, "");
    },

    _setPipelineName: function() {
        $("#" + this.titleSpan)[0].innerHTML = this.workspace["pipelineName"];
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
		var newIn = input.module.suggestInput();
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
        properties.displayPipe(newPipe);
        properties.show();
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
			if (i == pipe) {
				alert("Got It");
				delete editor.workspace["pipes"][i];
				return
			}
		}
	},

    _addModule: function(lsid, id) {
        var module = library.modules[lsid];
        if (module === null) {
            console.log("Error adding module: " + lsid);
            return;
        }
        var spawn = module.spawn();
        spawn.id = id;
        this.workspace[spawn.id] = spawn;
        spawn.add();
        return spawn;
    },

    loadModule: function(lsid, id) {
        this._addModule(lsid, id);
    },

	addModule: function(lsid) {
        this._addModule(lsid, this._nextId());
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
        spawn.add();
        return spawn;
    },

	addConnection: function(source, target) {
		return jsPlumb.connect({"source": source.endpoint, "target": target.endpoint});
	},

	_gridLayoutManager: function() {
		var location = { "top": this.workspace.suggestRow * 120, "left": this.workspace.suggestCol * 270 };
		this.workspace.suggestCol++;
		if (this.workspace.suggestCol >= 3) {
			this.workspace.suggestCol = 0;
			this.workspace.suggestRow++;
		}
		return location;
	},

	suggestLocation: function() {
		// Pick your layout manager
		return this._gridLayoutManager();
	},

	smartPipeSelection: function(output, input) {

	},

    saveProps: function(save) {
        this.workspace["pipelineName"] = save["Pipeline Name"];
        this.workspace["pipelineDescription"] = save["Description"];
        this.workspace["pipelineAuthor"] = save["Author"];
        this.workspace["pipelinePrivacy"] = save["Privacy"];
        this.workspace["pipelineVersionComment"] = save["Version Comment"];
        this.workspace["pipelineDocumentation"] = save["Documentation"];
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
        this.workspace["pipelineName"] = pipeline["Pipeline Name"];
        this.workspace["pipelineDescription"] = pipeline["Description"];
        this.workspace["pipelineAuthor"] = pipeline["Author"];
        this.workspace["pipelinePrivacy"] = pipeline["Privacy"];
        this.workspace["pipelineVersion"] = pipeline["pipelineVersion"];
        this.workspace["pipelineVersionComment"] = pipeline["Version Comment"];
        this.workspace["pipelineDocumentation"] = pipeline["Documentation"];
        this.workspace["pipelineLsid"] = pipeline["pipelineLsid"];
        editor._setPipelineName();
    },

    _loadModules: function(modules) {
        this.removeAllModules();
        for (var i in modules) {
            // Update the idCounter as necessary
            if (modules[i].id >= this.idCounter) { this.idCounter = modules[i].id + 1; }

            // Add each module as it is read
            var added = this.loadModule(modules[i].lsid, modules[i].id);

            // Set the correct properties for the module
            added.loadProps(modules[i]);
        }
    },

    _loadPipes: function(pipes) {
        for (var i in pipes) {
            var outputModule = editor.workspace[pipes[i]["outputModule"]];
            var inputModule = editor.workspace[pipes[i]["inputModule"]];
            var outputId = pipes[i]["outputPort"];
            var inputId = pipes[i]["inputPort"];

            if (!outputModule.hasPort(outputId)) {
                outputModule.addOutput(outputId);
            }
            if (!inputModule.hasPort(inputId)) {
                inputModule.addInput(inputId);
            }

            var outputPort = outputModule.getPort(outputId);
            var inputPort = inputModule.getPort(inputId);

            editor.addPipe(inputPort, outputPort);
        }

        //transport["outputModule"] = this.outputModule.id;
        //transport["outputPort"] = this.outputPort.id;
        //transport["inputModule"] = this.inputModule.id;
        //transport["inputPort"] = this.inputPort.id;
    },

	load: function(lsid) {
        $.ajax({
            type: "POST",
            url: "/gp/PipelineDesigner/load",
            data: lsid,
            success: function(response) {
                var error = response["ERROR"];
                if (error !== null) {
                    alert(error);
                }
                else {
                    editor._loadPipeline(response["pipeline"]);
                    editor._loadModules(response["modules"]);
                    editor._loadPipes(response["pipes"]);
                }
            },
            dataType: "json"
        });
	},

	save: function() {
		var toSend = editor._bundleForSave();
        $.ajax({
            type: "POST",
            url: "/gp/PipelineDesigner/save",
            data: toSend,
            success: function(response) {
                var message = response["MESSAGE"];
                var error = response["ERROR"];
                if (error !== null) {
                    alert(error);
                }
                if (message !== null) {
                    alert(message);
                }
            },
            dataType: "json"
        });
	}
};

/**
 * Class representing the library display at the top of the page
 */
var library = {
		div: "library",			// The id of the div used by the library
		moduleNames: [],		// A list of all module names
		modules: {},			// The JSON structure of all modules in the library
		recent: [],

		init: function(moduleJSON) {
			this._readModules(moduleJSON);
			this._readModuleNames();

			this._addModuleComboBox();
			this._addDefaultRecent();

			for (var i = 0; i < this.recent.length; i++) {
				var name = this.recent[i];
				this._addModuleButton(name);
			}
		},

		_addDefaultRecent: function() {
			this.recent = ["PreprocessDataset",
			               "ConvertLineEndings",
			               "HierarchicalClustering",
			               "HierarchicalClusteringViewer",
			               "AuDIT",
			               "ComparativeMarkerSelection",
			               "HeatMapViewer"];
		},

		_addRecentModule: function(name) {
			for (var i = 0; i < this.recent.length; i++) {
				if (this.recent[i] == name) { return }
			}

			this.recent.push(name);
			var removed = null;
			if (this.recent.length > 10) { removed = this.recent.shift(); }
			if (removed !== null) { $("button[name|=" + removed + "]").remove(); }
			this._addModuleButton(name);
		},

		_addModuleComboBox: function() {
			$("#modulesDropdown").autocomplete({ source: this.moduleNames });
			$("#addModule").button();

			$("#addModule").click(function() {
				var name = $("#modulesDropdown")[0].value;
				var module = editor.addModuleByName(name);
				if (module !== null) { library._addRecentModule(name); }
			});
		},

		_addModuleButton: function(name) {
			var modButton = document.createElement("button");
			modButton.innerHTML = name;
			modButton.setAttribute("class", "libraryModuleButton");
			modButton.setAttribute("name", name);
			$("#" + this.div)[0].appendChild(modButton);
			$(modButton).click(function() {
				editor.addModuleByName(this.name);
			});
			$("button[name|=" + name + "]").button();
		},

		_readModuleNames: function() {
			this.moduleNames = new Array();
			for (var i in library.modules) {
				this.moduleNames.push(library.modules[i].name);
			}
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

		extractFileInputs: function(inputsJSON) {
			var files = new Array();
			for (var i = 0; i < inputsJSON.length; i++) {
				if (inputsJSON[i].type == "java.io.File") {
					files[files.length] = new InputParam(inputsJSON[i]);
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
        }
};

/**
 * Class representing the properties pane
 */
var properties = {
        PROMPT_WHEN_RUN: "PROMPT_WHEN_RUN",
        div: "properties",
        titleDiv: "propertiesTitle",
        inputDiv: "propertiesInput",
        buttonDiv: "propertiesSubmit",
        current: null,

		init: function() {
			$("#propertiesOk").button();
			$("#propertiesCancel").button();

			$("#propertiesOk").click(function () {
                properties.saveToModel();
				properties.hide();
			});

			$("#propertiesCancel").click(function () {
				properties.hide();
			});
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
                    bundle[name] = inputs[i].value;
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

        _clearInputDiv: function() {
            $("#" + this.inputDiv)[0].innerHTML = "";
        },

        _setTitle: function(title) {
            $("#" + this.titleDiv)[0].innerHTML = this._encodeToHTML(title);
        },

        _displayInputKey: function() {
            var key = document.createElement("div");
            key.innerHTML = "<em>* Required <br /> Check for Prompt When Run</em>";
            $("#" + this.inputDiv).append(key);
            var hr = document.createElement("hr");
            $("#" + this.inputDiv).append(hr);
        },

        _addDropDown: function(labelText, values, description, pwr) {
            var label = document.createElement("div");

            if (pwr) {
                var checkBox = document.createElement("input");
                checkBox.setAttribute("type", "checkbox");
                label.appendChild(checkBox);
                label.innerHTML += " ";
            }

            label.innerHTML += this._encodeToHTML(labelText) + " ";
            var select = document.createElement("select");
            select.setAttribute("name", labelText);
            select.setAttribute("class", "propertyValue");
            for (var i = 0; i < values.length; i++) {
                var parts = values[i].split("=");
                if (parts.length < 2) parts[1] = parts[0];
                var option = document.createElement("option");
                option.innerHTML = this._encodeToHTML(parts[0]);
                option.value = parts[1];
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
        },

        _addTextBox: function(labelText, value, description, pwr) {
            var label = document.createElement("div");

            if (pwr) {
                var checkBox = document.createElement("input");
                checkBox.setAttribute("type", "checkbox");
                checkBox.setAttribute("name", labelText);
                checkBox.setAttribute("class", "propertyCheckBox");
                if (value == properties.PROMPT_WHEN_RUN) {
                    checkBox.setAttribute("checked", "true");
                }
                label.appendChild(checkBox);
                label.innerHTML += " ";
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
        },

        _addTextBoxInput: function(input) {
            var required = input.required ? "*" : "";
            this._addTextBox(input.name + required, input.value, input.description, true);
        },

        displayModule: function(module) {
            this.current = module;
            this._setTitle(module.name);
            this._clearInputDiv();
            this._displayInputKey();
            var inputs = module.inputs;
            for (var i in inputs) {
                this._addTextBoxInput(inputs[i]);
            }
        },

        displayPipe: function(pipe) {
            this.current = pipe;
            this._setTitle(pipe.outputModule.name + " to " + pipe.inputModule.name);
            this._clearInputDiv();
            this._addDropDown("Output", ["1st Output=1", "2nd Output=2", "3rd Output=3", "4th Output=4"].concat(pipe.outputModule.outputs), false, false);

            var inputsToList = new Array();
            for (var i = 0; i < pipe.inputModule.fileInputs.length; i++) {
                inputsToList[inputsToList.length] = pipe.inputModule.fileInputs[i].name;
            }
            this._addDropDown("Input", inputsToList, false, false);
        },

        displayPipeline: function() {
            this.current = new String("Pipeline");
            this._setTitle("Editing Pipeline");
            this._clearInputDiv();
            this._addTextBox("Pipeline Name", editor.workspace["pipelineName"], false, false);
            this._addTextBox("Description", editor.workspace["pipelineDescription"], false, false);
            this._addTextBox("Author", editor.workspace["pipelineAuthor"], false, false);
            this._addTextBox("Privacy", editor.workspace["pipelinePrivacy"], false, false);
            this._addTextBox("Version Comment", editor.workspace["pipelineVersionComment"], false, false);
            this._addTextBox("Documentation", editor.workspace["pipelineDocumentation"], false, false);
        }
};

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
	this.outputs = moduleJSON.outputs;
	this.outputEnds = [];
	this.inputEnds = [];
	this.inputs = library.extractInputs(moduleJSON.inputs);
	this.fileInputs = library.extractFileInputs(moduleJSON.inputs);
	this.type = "module";
	this.ui = null;

    this._loadInputs = function(inputs) {
        if (this.inputs.length != inputs.length) {
            console.log("ERROR: Inputs lengths do not match when loading: " + this.name);
            return;
        }

        for (var i = 0; i < this.inputs.length; i++) {
            this.inputs[i].loadProps(inputs[i]);
        }
    };

    this.loadProps = function(props) {
        this.id = props["id"];
        this._loadInputs(props["inputs"]);
        this.ui.style.top = props["top"];
        this.ui.style.left = props["left"];
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
                this.inputs[i].promptWhenRun = true;
                this.inputs[i].value = properties.PROMPT_WHEN_RUN;
            }
            else {
                this.inputs[i].value = value;
            }
        }
    };

    this.hasPort = function(id) {
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
        for (var i = 0; i < this.inputEnds.length; i++) {
            if (id == this.inputEnds[i].id) {
                return this.inputEnds[i];
            }
        }

        for (var i = 0; i < this.outputEnds.length; i++) {
            if (id == this.outputEnds[i].id) {
                return this.outputEnds[i];
            }
        }

        console.log("Unable to find port with id: " + id + " in module " + this.id);
    };

	// TODO: Eventually replace with smarter suggestions of which endpoint to connect
	this.suggestInput = function () {
        for (var i = 0; i < this.fileInputs.length; i++) {
            var used = this.fileInputs[i].used;
            if (!used) {
                this.fileInputs[i].used = true;
                return this.addInput(editor.nameToId(this.fileInputs[i].name));
            }
        }
    };

	// TODO: Eventually replace with smarter suggestions of which endpoint to connect
	this.suggestOutput = function (input) {
        return this.addOutput(this.outputEnds.length);
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

	this._addModuleButtonCalls = function () {
        $("#" + "prop_" + this.id).click(function () {
            properties.displayModule(editor.getParentModule(this.id));
            properties.show();
        });

        $("#" + "del_" + this.id).click(function () {
            var module = editor.getParentModule(this.id);
            module.remove();
        });
    };

	this._createDiv = function () {
        this.ui = document.createElement("div");
        this.ui.setAttribute("class", this.type);
        if (this.id !== null) {
            this.ui.setAttribute("id", this.id);
        }
        this.ui.setAttribute("name", this.name);
        this.ui.innerHTML = "<br /><br />" + this.name + "<br />";
        this._createButtons(this.ui, this.id);
    };

	this.addOutput = function (id) {
        var output = new Output(this, id);
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

	this.addInput = function (id) {
        var input = new Input(this, id);
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
                this.inputEnds[0].pipe.remove();
            }
        }

        while (this.outputEnds.length > 0) {
            if (this.outputEnds[0].master) {
                this.outputEnds[0].remove();
                continue;
            }

            if (this.outputEnds[0].isConnected()) {
                this.outputEnds[0].pipe.remove();
            }
        }
    };

	this.remove = function () {
        this._removePipes();
        $("#" + this.id).remove();
        editor.removeModule(this.id);
    };

	this.add = function () {
        this._createDiv();
        var location = editor.suggestLocation();
        this.ui.style.top = location["top"] + "px";
        this.ui.style.left = location["left"] + "px";
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
    this.value = this.defaultValue;

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
 * @param id - The id of the port
 */
function Port(module, id) {
	this.module = module;
	this.id = id;
	this.master = id == "master";
	this.type = null;
	this.endpoint = null;
	this.tooltip = null;
	this.pipe = null;

	this.init = function () {
        // Set correct color for master port
        var color = "black";
        if (this.master) {
            color = "blue";
        }

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
            baseStyle = editor.OUTPUT_FILE_STYLE;
        }
        else {
            baseStyle = editor.INPUT_FILE_STYLE;
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
            anchor:posArray,
            maxConnections:maxConn,
            dragAllowedWhenFull:true,
            paintStyle:{fillStyle:color}
        });
        this.endpoint.canvas.setAttribute("name", prefix + this.id + "_" + this.module.id);

        // Add tooltip
        this._createTooltip(this.id);
    };

	this.connectPipe = function (pipe) {
        this.pipe = pipe;
    };

	this.isConnected = function () {
        return this.pipe !== null;
    };

	this.isOutput = function () {
        return this.type == "output";
    };

	this.isInput = function () {
        return this.type == "input";
    };

	this.remove = function () {
        jsPlumb.deleteEndpoint(this.endpoint);

        for (var i = 0; i < this.module.inputEnds.length; i++) {
            if (this.module.inputEnds[i] == this) {
                this.module.inputEnds.splice(i, 1);
                return;
            }
        }

        for (var i = 0; i < this.module.outputEnds.length; i++) {
            if (this.module.outputEnds[i] == this) {
                this.module.outputEnds.splice(i, 1);
                return;
            }
        }
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
            properties.displayPipe(port.pipe);
            properties.show();
        });

        $("#" + "del_" + id).click(function () {
            var port = editor.getParentPort(this);
            port.pipe.remove();
        });
    };

	this._createTooltip = function(name) {
		this.tooltip = document.createElement("div");
		this.tooltip.setAttribute("id", "tip_" + this.endpoint.canvas.getAttribute("name"));
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
			this.tooltip.innerHTML = (this.isOutput() ? "Output Selection:<br />" : "Input Parameter:<br />") + name + "<br />";
			if (this.isInput()) {
				this._createButtons(this.tooltip, this.tooltip.getAttribute("id"));
			}
		}
		$("#" + editor.div)[0].appendChild(this.tooltip);
		if (!this.master && this.isInput()) { this._addTooltipButtonCalls(this.tooltip.getAttribute("id")); }
		$("#" + this.endpoint.canvas.id).tooltip({"tip": "#" + this.tooltip.id, "offset": [-70, 0]});
	};
}

/**
 * Class an input port on a module
 * @param module - A reference to the parent module
 * @param id - The id of the port
 */
function Input(module, id) {
	var port = new Port(module, id);
	port.type = "input";
	port.init();
	return port;
}

/**
 * Class an output port on a module
 * @param module - A reference to the parent module
 * @param id - The id of the port
 */
function Output(module, id) {
	var port = new Port(module, id);
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

	this.toMaster = function () {
        return this.inputPort.endpoint.canvas.getAttribute("name").indexOf("_master_") >= 0;
    };

	this.remove = function() {
		var deleteOutput = this.outputPort.endpoint.connections.length <= 1;
		this.inputPort.detachAll();
		this.inputPort.remove();
		if (deleteOutput) { this.outputPort.remove(); }
		editor.removePipe(this);
	};

    this.saveProps = function(save) {
        this.outputPort.id = save["Output"];
        this.outputPort._createTooltip(save["Output"]);

        console.log(save["Output"]);
        console.log(save["Input"]);
    };

    this.prepTransport = function() {
        var transport = {};
        transport["outputModule"] = this.outputModule.id;
        transport["outputPort"] = this.outputPort.id;
        transport["inputModule"] = this.inputModule.id;
        transport["inputPort"] = this.inputPort.id;
        return transport;
    };
}