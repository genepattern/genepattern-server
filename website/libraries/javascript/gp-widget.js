/**
 * Widgets for use in GenePattern Notebook or in other apps.
 *
 * @requires jQuery 1.5+, jQuery UI, gp.js
 *
 * @author Thorin Tabor
 */
"use strict";

/**
 * Widget for inputing a file into a GenePattern Notebook.
 * Used for file inputs by the runTask widget.
 *
 * Supported Features:
 *      External URLs
 *      Uploading New Files
 *      Pasted Internal File Paths
 *      Pasted Job Result URLs
 *
 * Non-Supported Features:
 *      GenomeSpace Files
 *      GenePattern Uploaded Files
 */
$.widget("gp.fileInput", {
    options: {
        allowFilePaths: true,
        allowExternalUrls: true,
        allowJobUploads: true,

        // Pointers to associated runTask widget
        runTask: null,
        param: null
    },

    /**
     * Constructor
     *
     * @private
     */
    _create: function() {
        // Save pointers to associated Run Task widget or parameter
        this._setPointers();

        // Set variables
        var widget = this;
        this._value = null;
        this._display = null;

        // Add classes and child elements
        this.element.addClass("file-widget");
        this.element.append(
            $("<div></div>")
                .addClass("file-widget-upload")
                .append(
                    $("<button></button>")
                        .addClass("file-widget-upload-file")
                        .text("Upload File...")
                        .button()
                        .click(function () {
                            $(this).parents(".file-widget").find(".file-widget-input-file").click();
                        })
                )
                .append(
                    $("<input />")
                        .addClass("file-widget-input-file")
                        .attr("type", "file")
                        .change(function () {
                            var newValue = widget.element.find(".file-widget-input-file")[0].files[0];
                            widget.value(newValue);
                        })
                )
                .append(
                    $("<button></button>")
                        .addClass("file-widget-url")
                        .addClass("file-widget-button")
                        .text("Add Path or URL...")
                        .button()
                        .click(function() {
                            widget._pathBox(true);
                        })
                )
                .append(
                    $("<span></span>")
                        .addClass("file-widget-drop")
                        .text("Drag Files Here")
                )
                .append(
                    $("<div></div>")
                        .addClass("file-widget-size")
                        .text(" 2GB file upload limit using the Upload File... button.")
                )
        );
        this.element.append(
            $("<div></div>")
                .addClass("file-widget-listing")
                .css("display", "none")
                .append(
                    $("<div></div>")
                        .addClass("file-widget-value")
                        .append(
                            $("<div></div>")
                                .addClass("file-widget-value-erase")
                                .append(
                                    $("<a></a>")
                                        .html("&times;")
                                        .click(function() {
                                            widget.clear();
                                        })
                                )
                        )
                        .append(
                            $("<span></span>")
                                .addClass("file-widget-value-text")
                        )
                )
        );
        this.element.append(
            $("<div></div>")
                .addClass("file-widget-path")
                .css("display", "none")
                .append(
                    $("<div></div>")
                        .addClass("file-widget-path-label")
                        .text("Enter Path or URL")
                )
                .append(
                    $("<input />")
                        .addClass("file-widget-path-input")
                        .attr("type", "text")
                )
                .append(
                    $("<div></div>")
                        .addClass("file-widget-path-buttons")
                        .append(
                            $("<button></button>")
                                .addClass("file-widget-button")
                                .text("Select")
                                .button()
                                .click(function() {
                                    var boxValue = widget.element.find(".file-widget-path-input").val();
                                    widget.element.find(".file-widget-path-input").val("");
                                    widget._pathBox(false);
                                    widget.value(boxValue);
                                })
                        )
                        .append(" ")
                        .append(
                            $("<button></button>")
                                .addClass("file-widget-button")
                                .text("Cancel")
                                .button()
                                .click(function() {
                                    widget._pathBox(false);
                                    widget.element.find(".file-widget-path-input").val("");
                                })
                        )
                )
        );

        // Initialize the drag & drop functionality
        if (this.options.allowJobUploads) {
            this._initDragDrop();
        }

        // Hide elements if not in use by options
        this._setDisplayOptions();
    },

    /**
     * Destructor
     *
     * @private
     */
    _destroy: function() {
        this.element.removeClass("file-widget");
        this.element.empty();
    },

    /**
     * Initializes the drag & drop functionality in the widget
     *
     * @private
     */
    _initDragDrop: function() {
        var widget = this;
        var dropTarget = this.element[0];

        dropTarget.addEventListener("dragenter", function(event) {
            widget.element.css("background-color", "#dfeffc");
            event.stopPropagation();
            event.preventDefault();
        }, false);
        dropTarget.addEventListener("dragexit", function(event) {
            widget.element.css("background-color", "");
            event.stopPropagation();
            event.preventDefault();
        }, false);
        dropTarget.addEventListener("dragover", function(event) {
            event.stopPropagation();
            event.preventDefault();
        }, false);
        dropTarget.addEventListener("drop", function(event) {
            var files = event['dataTransfer'].files;
            if (files.length > 0) {
                widget.value(files[0]);
            }
            widget.element.css("background-color", "");
            event.stopPropagation();
            event.preventDefault();
        }, false);
    },

    /**
     * Shows or hides the box of selected files
     *
     * @param file - A string if to show, undefined or null if to hide
     * @private
     */
    _fileBox: function(file) {
        if (file) {
            this.element.find(".file-widget-value-text").text(file);
            this.element.find(".file-widget-listing").show();
            this.element.find(".file-widget-upload").hide();
        }
        else {
            this.element.find(".file-widget-upload").show();
            this.element.find(".file-widget-listing").hide();
        }
    },

    /**
     * Takes a value and returns the display string for the value
     *
     * @param value - the value, either a string or File object
     * @returns {string} - the display value
     * @private
     */
    _valueToDisplay: function(value) {
        if (typeof value === 'string') {
            return value;
        }
        else {
            return value.name;
        }
    },

    /**
     * Displays the select path or URL box
     *
     * @param showPathBox - Whether to display or hide the path box
     * @private
     */
    _pathBox: function(showPathBox) {
        if (showPathBox) {
            this.element.find(".file-widget-path").show();
            this.element.find(".file-widget-upload").hide();
        }
        else {
            this.element.find(".file-widget-path").hide();
            this.element.find(".file-widget-upload").show();
        }
    },

    /**
     * Update the pointers to the Run Task widget and parameter
     *
     * @private
     */
    _setPointers: function() {
        if (this.options.runTask) { this._runTask = this.options.runTask; }
        if (this.options.param) { this._param = this.options.param; }
    },

    /**
     * Update the display of the UI to match current options
     *
     * @private
     */
    _setDisplayOptions: function() {
        if (!this.options.allowJobUploads) {
            this.element.find(".file-widget-upload-file").hide();
            this.element.find(".file-widget-drop").hide();
            this.element.find(".file-widget-size").hide();
        }
        if (!this.options.allowExternalUrls && !this.options.allowFilePaths) { this.element.find(".file-widget-url").hide(); }
        else if (!this.options.allowExternalUrls && this.options.allowFilePaths) {
            this.element.find(".file-widget-url").button("option", "label", "Add Path...");
            this.element.find(".file-widget-path-label").text("Enter Path");
        }
        else if (this.options.allowExternalUrls && !this.options.allowFilePaths) {
            this.element.find(".file-widget-url").button("option", "label", "Add URL...");
            this.element.find(".file-widget-path-label").text("Enter URL");
        }
        else if (this.options.allowExternalUrls && this.options.allowFilePaths) {
            this.element.find(".file-widget-url").button("option", "label", "Add Path or URL...");
            this.element.find(".file-widget-path-label").text("Enter Path or URL");
        }
    },

    /**
     * Update all options
     *
     * @param options - Object contain options to update
     * @private
     */
    _setOptions: function(options) {
        this._superApply(arguments);
        this._setPointers();
        this._setDisplayOptions();
    },

    /**
     * Update all options
     *
     * @param key - The name of the option
     * @param value - The new value of the option
     * @private
     */
    _setOption: function(key, value) {
        this._super(key, value);
        this._setPointers();
        this._setDisplayOptions();
    },

    /**
     * Upload the selected file to the server
     *
     * @param pObj - Object containing the following params:
     *                  success: Callback for success, expects url to file
     *                  error: Callback on error, expects exception
     * @returns {boolean} - Whether an upload was just initiated or not
     */
    upload: function(pObj) {
        var currentlyUploading = null;
        var widget = this;

        // Value is a File object
        if (typeof this.value() === 'object') {
            gp.upload({
                file: this.value(),
                success: function(response, url) {
                    widget.value(url);
                    if (pObj.success) {
                        pObj.success(response, url);
                    }
                },
                error: function(exception) {
                    console.log("Error uploading file from file input widget: " + exception.statusText);
                    if (pObj.error) {
                        pObj.error(exception);
                    }
                }
            });
            currentlyUploading = true;
        }
        // If the value is not ste, give an error
        else if (!this.value()) {
            console.log("Cannot upload from file input: value is null.");
            currentlyUploading = false;
        }
        // If the value is a string, do nothing
        else {
            // Else assume we have a non-upload value selected
            currentlyUploading = false;
        }
        return currentlyUploading;
    },

    /**
     * Getter for associated RunTask object
     *
     * @returns {object|null}
     */
    runTask: function() {
        return this._runTask;
    },

    /**
     * Getter for associated parameter
     * @returns {string|null|object}
     */
    param: function() {
        return this._param;
    },

    /**
     * Gets or sets the value of this widget
     *
     * @param [val=optional] - String value for file (undefined is getter)
     * @returns {object|string|null} - The value of this widget
     */
    value: function(val) {
        // Do setter
        if (val) {
            this._value = val;
            this._display = this._valueToDisplay(val);
            this._fileBox(this._display);
        }
        // Do getter
        else {
            return this._value;
        }
    },

    /**
     * Clears the current value of the widget and hides file box
     * @private
     */
    clear: function() {
        this._value = null;
        this._fileBox(null);
    }
});


/**
 * Widget for entering parameters and launching a job from a task.
 *
 * Supported Features:
 *      File Inputs
 *      Text Inputs
 *      Choice Inputs
 *
 * Non-Supported Features:
 *      Batch Parameters
 *      EULA support
 *      Dynamic Dropdowns
 *      Reloaded Jobs
 *      File Lists
 *      Task Source
 */
$.widget("gp.runTask", {
    options: {
        lsid: null,
        name: null
    },

    /**
     * Constructor
     *
     * @private
     */
    _create: function() {
        //TODO: Implement
    },

    /**
     * Destructor
     *
     * @private
     */
    _destroy: function() {
        //TODO: Implement
    },

    /**
     * Update all options
     *
     * @param options - Object contain options to update
     * @private
     */
    _setOptions: function(options) {
        //TODO: Implement
    },

    /**
     * Update all options
     *
     * @param key - The name of the option
     * @param value - The new value of the option
     * @private
     */
    _setOption: function(key, value) {
        //TODO: Implement
    }
});


/**
 * Widget for viewing the job results of a launched job.
 *
 * Supported Features:
 *      Job Status
 *      Access to Job Results
 *      Access to Logs
 *
 * Non-Supported Features:
 *      Job Sharing & Permissions
 *      Access to Job Inputs
 *      Visibility into Child Jobs
 *      Batch Jobs
 */
$.widget("gp.jobResults", {
    options: {
        jobNumber: null
    },

    /**
     * Constructor
     *
     * @private
     */
    _create: function() {
        //TODO: Implement
    },

    /**
     * Destructor
     *
     * @private
     */
    _destroy: function() {
        //TODO: Implement
    },

    /**
     * Update all options
     *
     * @param options - Object contain options to update
     * @private
     */
    _setOptions: function(options) {
        //TODO: Implement
    },

    /**
     * Update all options
     *
     * @param key - The name of the option
     * @param value - The new value of the option
     * @private
     */
    _setOption: function(key, value) {
        //TODO: Implement
    }
});