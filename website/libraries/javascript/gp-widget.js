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
                            // TODO: Implement
                            alert("OK");
                        })
                )
                .append(
                    $("<button></button>")
                        .addClass("file-widget-url")
                        .text("Add Path or URL...")
                        .button()
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
        );

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
     * Shows or hides the box of selected files
     *
     * @param file - A file object if to show, undefined or null if to hide
     * @private
     */
    _fileBox: function(file) {
        // TODO: Implement
    },

    /**
     * Displays the select path or URL dialog
     *
     * @private
     */
    _pathDialog: function() {
        // TODO: Implement
    },

    /**
     * Update the pointers to the Run Task widget and parameter
     *
     * @private
     */
    _setPointers: function() {
        if (this.options.runTask) { this.runTask = this.options.runTask; }
        if (this.options.param) { this.param = this.options.param; }
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
        else if (!this.options.allowExternalUrls && this.options.allowFilePaths) { this.element.find(".file-widget-url").button("option", "label", "Add Path..."); }
        else if (this.options.allowExternalUrls && !this.options.allowFilePaths) { this.element.find(".file-widget-url").button("option", "label", "Add URL..."); }
        else if (this.options.allowExternalUrls && this.options.allowFilePaths) { this.element.find(".file-widget-url").button("option", "label", "Add Path or URL..."); }
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
     */
    upload: function(pObj) {
        // TODO: Implement
    },

    /**
     * Getter for associated RunTask object
     *
     * @returns {object|null}
     */
    runTask: function() {
        return this.runTask;
    },

    /**
     * Getter for associated parameter
     * @returns {string|null|object}
     */
    param: function() {
        return this.param;
    },

    /**
     * Gets or sets the value of this widget
     *
     * @param val - String value for file (undefined is getter)
     * @returns {string} - The value of this widget
     */
    value: function(val) {
        // TODO: Implement
        return "";
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