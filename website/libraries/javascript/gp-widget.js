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
        if (this.options.runTask) { this.runTask = this.options.runTask; }
        if (this.options.param) { this.param = this.options.param; }

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
                )
                .append(
                    $("<div></div>")
                        .addClass("file-widget-input-file")
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
    },

    /**
     * Destructor
     *
     * @private
     */
    _destroy: function() {
        this.element.removeClass("file-widget");
    },

    /**
     * Update all options
     *
     * @param options - Object contain options to update
     * @private
     */
    _setOptions: function(options) {
        this._superApply(arguments);
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