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
	workspace: {			// A map containing all the instance data of the current workspace
		idCounter: 0, 		// Used to keep track of module instance IDs
		pipes: [],	// A list of all current connections in the workspace
		suggestRow: 0, 		// Used by the GridLayoutManager
		suggestCol: 0		// Used by the GridLayoutManager
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
				editor.addPipe(output, input);
			}
		});
	},
	
	addPipe: function(output, input) {
		var newIn = input.module.suggestInput();
		// If there are no valid inputs left return null and cancel the pipe
		if (newIn == null) {
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

		var connection = editor.addConnection(newOut, newIn);
		var newPipe = new Pipe(connection);
		newIn.connectPipe(newPipe);
		newOut.connectPipe(newPipe);
		
		editor.workspace["pipes"].push(newPipe);
		input.module.getMasterInput().detachAll();
		properties.show(output.module.name + " to " + input.module.name);
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
		if (element == null) {
			console.log("getParentModule() received null value");
			return null;
		}
		var id = this._extractModuleId(element);
		var parent = editor.workspace[id];
		return parent;
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
		if (element == null) {
			console.log("getParentPort() received null value");
			return null;
		}
		var moduleId = this._extractModuleId(element);
		var module = editor.workspace[moduleId];
		var portId = this._extractPortId(element);
		var port = module.getPort(portId);
		return port;
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
	
	addModule: function(name) {
		var module = library.modules[name];
		if (module == null) { 
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
	
	_allAmlLayoutManager: function() {
		var position = new Array();
		position[0] = { "top": 15, "left": 516 };
		position[1] = { "top": 14, "left": 1048 };
		position[2] = { "top": 363, "left": 430 };
		position[3] = { "top": 214, "left": 694 };
		position[4] = { "top": 532, "left": 465 };
		position[5] = { "top": 373, "left": 816 };
		position[6] = { "top": 530, "left": 67 };
		position[7] = { "top": 704, "left": 66 };
		position[8] = { "top": 708, "left": 367 };
		position[9] = { "top": 214, "left": 1013 };
		position[10] = { "top": 374, "left": 1158 };
		
		position[11] = { "top": 26, "left": 8 };
		position[12] = { "top": 216, "left": 17 };
		position[13] = { "top": 534, "left": 393 };
		position[14] = { "top": 524, "left": 686 };
		position[15] = { "top": 526, "left": 1006 };
		position[16] = { "top": 697, "left": 677 };
		
		this.workspace.suggestRow++;
		return position[this.workspace.suggestRow - 1];
	},
	
	suggestLocation: function() {
		// Pick your layout manager
		// return this._allAmlLayoutManager();
		return this._gridLayoutManager();
	},
	
	smartPipeSelection: function(output, input) {
		
	},
	
	query: function() {
		return "TODO";
	},

	save: function() {
		return "TODO";
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
			
			for (var i = 0; i < this.recent.length; i++) {
				var name = this.recent[i];
				this._addModuleButton(name);
			}
		},
		
		_addRecentModule: function(name) {
			for (var i = 0; i < this.recent.length; i++) {
				if (this.recent[i] == name) { return }
			}
			
			this.recent.push(name);
			var removed = null;
			if (this.recent.length > 10) { removed = this.recent.shift(); }
			if (removed != null) { $("button[name|=" + removed + "]").remove(); }
			this._addModuleButton(name);
		},
		
		_addModuleComboBox: function() {
			$("#modulesDropdown").autocomplete({ source: this.moduleNames });
			$("#addModule").button();
			
			$("#addModule").click(function() {
				var name = $("#modulesDropdown")[0].value;
				var module = editor.addModule(name);
				if (module != null) { library._addRecentModule(name); }
			});
		},
		
		_addModuleButton: function(name) {
			var modButton = document.createElement("button");
			modButton.innerHTML = name;
			modButton.setAttribute("class", "libraryModuleButton");
			modButton.setAttribute("name", name);
			$("#" + this.div)[0].appendChild(modButton);
			$(modButton).click(function() {
				editor.addModule(this.name);
			});
			$("button[name|=" + name + "]").button();
		},
		
		_readModuleNames: function() {
			this.moduleNames = new Array();
			for (i in library.modules) {
				this.moduleNames.push(i);
			}
		},
		
		_readModules: function(moduleJSON) {
			this.modules = {};
			for (var i = 0; i < moduleJSON.modules.length; i++) {
				var module = moduleJSON.modules[i];
				if (module.type == "Module") {
					this.modules[module.name] = new Module(module);
				}
				else if (module.type == "Visualizer") {
					this.modules[module.name] = new Visualizer(module);
				}
				else if (module.type == "Pipeline") {
					this.modules[module.name] = new Pipeline(module);
				}
				else {
					console.log("Error detecting module type: " + module.name);
				}
			}
		}
};

/**
 * Class representing the properties pane
 */
var properties = {
		init: function() {
			$("#propertiesOk").button();
			$("#propertiesCancel").button();
			
			$("#propertiesOk").click(function () {
				properties.hide();
			});
			
			$("#propertiesCancel").click(function () {
				properties.hide();
			});
		},
		
		hide: function() {
			$("#properties").hide("slide", { direction: "right" }, 500);
		},
		
		show: function(name) {
			$("#propertiesName")[0].innerHTML = name;
			$("#properties").show("slide", { direction: "right" }, 500);
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
	this.output = moduleJSON.output;
	this.outputEnds = [];
	this.inputEnds = [];
	this.fileInput = moduleJSON.fileInput;
	this.type = "module";
	this.ui = null;
	
	this._nameToId = function(name) {
		return name.replace(/ /g,"");
	}
	
	this.getPort = function(id) {
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
	}
	
	// TODO: Eventually replace with smarter suggestions of which endpoint to connect
	this.suggestInput = function() {
		for (i in this.fileInput) {
			var used = this.fileInput[i].used;
			if (used == null) {
				this.fileInput[i].used = true;
				return this._addInput(this._nameToId(i));
			}
		}
	}
	
	// TODO: Eventually replace with smarter suggestions of which endpoint to connect
	this.suggestOutput = function(input) {
		return this._addOutput(this.outputEnds.length);
	}
	
	this._createButtons = function(appendTo, baseId) {
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

	}
	
	this._addModuleButtonCalls = function() {
		$("#" + "prop_" + this.id).click(function() {
			properties.show(this.parentNode.getAttribute("name"));
		});
		
		$("#" + "del_" + this.id).click(function() {
			var module = editor.getParentModule(this.id);
			module.delete();
		});
	}
	
	this._createDiv = function() {
		this.ui = document.createElement("div");
		this.ui.setAttribute("class", this.type);
		if (this.id != null) {
			this.ui.setAttribute("id", this.id);
		}
		this.ui.setAttribute("name", this.name);
		this.ui.innerHTML = "<br /><br />" + this.name + "<br />";
		this._createButtons(this.ui, this.id);
	}
	
	this._addOutput = function(id) {
		var output = new Output(this, id);
		var index = this.outputEnds.length;
		this.outputEnds[index] = output;
		return output;
	}
	
	this._addMasterOutput = function() {
		if (this.type == "module visualizer") { return null; }
		return this._addOutput("master");
	}
	
	this._addInput = function(id) {
		var input = new Input(this, id);
		var index = this.inputEnds.length;
		this.inputEnds[index] = input;
		return input;
	}
	
	this._addMasterInput = function() {
		return this._addInput("master");
	}
	
	this._detachInputs = function() {
		for (var i = 0; i < this.inputEnds.length; i++) {
			this.inputEnds[i].detachAll();
		}
	}
	
	this._detachOutputs = function() {
		for (var i = 0; i < this.outputEnds.length; i++) {
			this.outputEnds[i].detachAll();
		}
	}
	
	this._removePorts = function() {
		for (var i = 0; i < this.inputEnds.length; i++) {
			this.inputEnds[i].delete();
		}
		
		for (var i = 0; i < this.outputEnds.length; i++) {
			this.outputEnds[i].delete();
		}
	}
	
	this._removeUI = function() {
		this._removePorts();
		$("#" + this.id).remove();
	}
	
	this.delete = function() {
		this._detachInputs();
		this._detachOutputs();
		this._removeUI();
		editor.removeModule(this.id);
	}

	this.add = function() {
		this._createDiv();
		var location = editor.suggestLocation();
		this.ui.style.top = location["top"] + "px";
		this.ui.style.left = location["left"] + "px";
		$("#" + editor.div)[0].appendChild(this.ui);
		this._addMasterOutput();
		this._addMasterInput();
		jsPlumb.draggable(this.ui);
		this._addModuleButtonCalls();
	}
	
	this.spawn = function() {
		var clone = new Module(this.json);
		clone.type = this.type;
		return clone;
	}
	
	this.getMasterInput = function() {
		return this.inputEnds[0];
	}
	
	this.getMasterOutput = function() {
		return this.outputEnds[0];
	}
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
 * Class representing a port for connecting modules
 * @param endpoint - A jsPlumb Endpoint object
 */
function Port(module, id) {
	this.module = module;
	this.id = id;
	this.master = id == "master";
	this.type = null;
	this.endpoint = null;
	this.tooltip = null;
	this.pipe = null;
	
	this.init = function() {
		// Set correct color for master port
		var color = "black";
		if (this.master) { color = "blue"; }
		
		// Get correct list on module for type
		var correctList = null;
		if (this.isOutput()) { correctList = this.module.outputEnds; }
		else { correctList = this.module.inputEnds; }
		
		// Get the correct base style
		var baseStyle = null;
		if (this.isOutput()) { baseStyle = editor.OUTPUT_FILE_STYLE; }
		else { baseStyle = editor.INPUT_FILE_STYLE; }
		
		// Get the correct endpoint name prefix
		var prefix = null;
		if (this.isOutput()) { prefix = "out_"; }
		else { prefix = "in_"; }
		
		// Calculate position
		var index = correctList.length;
		var position = 0.2 * (index + 1) - 0.1;
		
		// Get the correct position array
		var posArray = null;
		if (this.isOutput()) { posArray = [position, 1, 0, 1]; }
		else { posArray = [position, 0, 0, -1]; }
		
		// Get the correct number of max connections
		var maxConn = null;
		if (this.isOutput()) { maxConn = -1; }
		else { maxConn = 1; }
		
		// Create endpoint
		this.endpoint = jsPlumb.addEndpoint(this.module.id.toString(), baseStyle, { 
			anchor: posArray, 
			maxConnections: maxConn,
			dragAllowedWhenFull: true,
			paintStyle: {fillStyle: color}
		});
		this.endpoint.canvas.setAttribute("name", prefix + this.id + "_" + this.module.id);
		
		// Add tooltip
		this._createTooltip(this.id);
	}
	
	this.connectPipe = function(pipe) {
		this.pipe = pipe;
	}
	
	this.isConnected = function() {
		if (this.pipe == null) {
			return false;
		}
		else {
			return true;
		}
	}
	
	this.isOutput = function() {
		if (this.type == "output") {
			return true;
		}
		else {
			return false;
		}
	}
	
	this.isInput = function() {
		if (this.type == "input") {
			return true;
		}
		else {
			return false;
		}
	}
	
	this.delete = function() {
		this.detachAll();
		jsPlumb.deleteEndpoint(this.endpoint);
		
		for (var i = 0; i < this.module.inputEnds.length; i++) {
			if (i == this) {
				delete this.module.inputEnds[i];
				return;
			}
		}
		
		for (var i = 0; i < this.module.outputEnds.length; i++) {
			if (i == this) {
				delete this.module.outputEnds[i];
				return;
			}
		}
	}
	
	this.detachAll = function() {
		this.endpoint.detachAll();
	}
	
	this._createButtons = function(appendTo, baseId) {
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
	}
	
	this._addTooltipButtonCalls = function(id) {
		$("#" + "prop_" + id).click(function() {
			var port = editor.getParentPort(this);
			properties.show(port.pipe.outputModule.name + " to " + port.pipe.inputModule.name);
		});
		
		$("#" + "del_" + id).click(function() {
			var port = editor.getParentPort(this);
			port.pipe.delete();
		});
	}
	
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
		$("#" + this.endpoint.canvas.id).tooltip({"tip": "#" + this.tooltip.id, "offset": [-50, 0]});
	}
}

/**
 * Class an input port on a module
 * @param endpoint - A jsPlumb Endpoint object
 */
function Input(module, id) {
	var port = new Port(module, id);
	port.type = "input";
	port.init();
	return port;
}

/**
 * Class an output port on a module
 * @param endpoint - A jsPlumb Endpoint object
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
	
	this._init = function(connection) {
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
	}
	this._init(connection);
	
	this.toMaster = function() {
		return this.inputPort.endpoint.canvas.getAttribute("name").indexOf("_master_") >= 0;
	}
	
	this.delete = function() {
		var deleteOutput = this.outputEnd.connections.length <= 1;
		this.inputPort.detachAll();
		jsPlumb.deleteEndpoint(this.inputPort.endpoint);
		if (deleteOutput) { jsPlumb.deleteEndpoint(this.outputPort.endpoint); }
		editor.removePipe(this);
	}
}