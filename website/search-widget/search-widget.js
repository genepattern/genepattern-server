/*
 * Copyright 2013 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

$.widget( "gp.module", {
    // default options
    options: {
        display: true,
        data: {},
        draggable: false,   // TODO: Implement

        // callbacks
        click: function() {},
        drag: null,         // TODO: Implement
        drop: null          // TODO: Implement
    },

    // the constructor
    _create: function() {
        this.element.addClass('module-listing');

        this.version = $('<div>', {
            'class': 'module-version',
            'text': 'v'+ this.options.data.version
        }).appendTo(this.element);

        this.name = $('<div>', {
            'class': 'module-name',
            'text': this.options.data.name
        }).appendTo(this.element);

        this.description = $('<div>', {
            'class': 'module-description',
            'text': this.options.data.description
        }).appendTo(this.element);

        this.tags = $('<div>', {
            'class': 'module-tag'
        }).appendTo(this.element);

        // Add tag links
        for (var tag in this.options.data.tags) {
            $('<a>', {
                'class': 'tag',
                'text': this.options.data.tags[tag],
                'href': '#'
            }).appendTo(this.tags);
            this.tags.append(', ');
        }

        if (!this.options.display) {
            this.element.hide();
        }

        // bind events on the widget
        this._on(this.element, {
            click: this.options.click
        });
    },

    // events bound via _on are removed automatically
    // revert other modifications here
    _destroy: function() {
        // remove generated elements
        this.name.remove();
        this.version.remove();
        this.description.remove();
        this.tags.remove();

        this.element
            .removeClass('module-listing');
    },

    // _setOptions is called with a hash of all options that are changing
    _setOptions: function() {
        // _super and _superApply handle keeping the right this-context
        this._superApply(arguments);
    },

    // _setOption is called for each individual option that is changing
    _setOption: function(key, value) {
        this._super(key, value);
    }
});