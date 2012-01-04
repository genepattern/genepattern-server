/**
 * JavaScript used by the GenePattern Pipeline Editor
 * @requires jQuery, jQuery UI, jsPlumb
 * @author tabor
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
		connections: [],	// A list of all current connections in the workspace
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
	},

	_nextId: function() {
		this.workspace["idCounter"]++;
		return this.workspace["idCounter"] - 1;
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
		return jsPlumb.connect({"source": source, "target": target});
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
		
		init: function(moduleJSON) {
			this.readModules(moduleJSON);
			this._readModuleNames();
			$("#modulesDropdown").autocomplete({ source: this.moduleNames });
			$("#addModule").button();
			
			$("#addModule").click(function() {
				editor.addModule($("#modulesDropdown")[0].value);
			});
			
			for (var i = 0; i < this.moduleNames.length; i++) {
				var modButton = document.createElement("button");
				var name = this.moduleNames[i];
				modButton.innerHTML = name;
				modButton.setAttribute("class", "libraryModuleButton");
				modButton.setAttribute("name", name);
				$("#" + this.div)[0].appendChild(modButton);
				$(modButton).click(function() {
					editor.addModule(this.name);
				});
			}
			$(".libraryModuleButton").button();
		},
		
		_readModuleNames: function() {
			this.moduleNames = new Array();
			for (i in library.modules) {
				this.moduleNames.push(i);
			}
		},
		
		readModules: function(moduleJSON) {
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
 * @returns
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
	
	this._createButtons = function() {
		var propertiesButton = document.createElement("img");
		propertiesButton.setAttribute("src", "images/pencil.gif");
		propertiesButton.setAttribute("class", "propertiesButton");
		this.ui.appendChild(propertiesButton);
	}
	
	this._createDiv = function() {
		this.ui = document.createElement("div");
		this.ui.setAttribute("class", this.type);
		if (this.id != null) {
			this.ui.setAttribute("id", this.id);
		}
		this.ui.setAttribute("name", this.name);
		this.ui.innerHTML = "<br /><br />" + this.name + "<br />";
		this._createButtons();
	}
	
	this._addOutputs = function() {
		if (this.type == "module visualizer") {
			return;
		}
		var increment = 1.0 / (4 + this.output.length);
		for (var i = 1; i <= this.output.length + 3; i++) {
			this.outputEnds[this.outputEnds.length] = jsPlumb.addEndpoint(this.id.toString(), editor.OUTPUT_FILE_STYLE, { 
				anchor: [increment * i, 1, 0, 1], 
				maxConnections: -1,
				paintStyle: {fillStyle: "blue"}
			});
		}
	}
	
	this._addInputs = function() {
		var length = 0;
		for (var input in this.fileInput) {
			length++;
		}
		
		var increment = 1.0 / (length + 1);
		var count = 1;
		for (var input in this.fileInput) {
			this.inputEnds[this.inputEnds.length] = jsPlumb.addEndpoint(this.id.toString(), editor.INPUT_FILE_STYLE, { anchor: [increment * count, 0, 0, -1] });
			count++;
		}
	}

	this.add = function() {
		this._createDiv();
		var location = editor.suggestLocation();
		this.ui.style.top = location["top"] + "px";
		this.ui.style.left = location["left"] + "px";
		$("#" + editor.div)[0].appendChild(this.ui);
		this._addOutputs();
		this._addInputs();
		jsPlumb.draggable(this.ui);
	}
	
	this.spawn = function() {
		var clone = new Module(this.json);
		clone.type = this.type;
		return clone;
	}
}

/**
 * Class representing an available pipeline for nesting in the editor
 * @param moduleJSON - A JSON representation of the module
 * @returns
 */
function Pipeline(moduleJSON) {
	var module = new Module(moduleJSON);
	module.type = "module pipeline";
	return module;
}

/**
 * Class representing an available visualizer for use in the editor
 * @param moduleJSON - A JSON representation of the module
 * @returns
 */
function Visualizer(moduleJSON) {
	var module = new Module(moduleJSON);
	module.type = "module visualizer";
	return module;
}