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

        // Save the lsid
        this.lsid = this.options.data.lsid;

        // Add the ui elements
        this.docicon = $('<a href="' + this.options.data.documentation + '" target="_blank" class="module-doc"><img src="doc.png"></a>').appendTo(this.element);

        this.version = $('<div>', {
            'class': 'module-version',
            'text': this.options.data.version ? 'v'+ this.options.data.version : ''
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
        for (var category in this.options.data.categories) {
            $('<a>', {
                'class': 'tag',
                'text': this.options.data.categories[category],
                'href': '#'
            }).appendTo(this.tags);
            this.tags.append(', ');
        }
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
        this.docicon.remove();
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

$.widget( "gp.modulelist", {
    // default options
    options: {
        title: null,
        breadcrumbs: {},            // TODO: Implement
        data: {},
        click: function() {}
    },

    // the constructor
    _create: function() {
        this.element.addClass('module-list');

        if (this.options.title) {
            this.title = $('<h4>', {
                'class': 'module-list-title',
                'text': this.options.title
            }).appendTo(this.element);
        }

        this.listings = [];
        for (var id in this.options.data) {
            this.listings.push($('<div>').module({
                data: this.options.data[id],
                click: this.options.click
            }).appendTo(this.element));
        }

        this.empty = $('<h4>', {
            'class': 'module-list-empty',
            'text': "No Results Found"
        }).appendTo(this.element);
    },

    filter: function(filter) {
        var numberHidden = 0
        for (var i = 0; i < this.listings.length; i++) {
            var listing = this.listings[i];
            if (listing.text().toLowerCase().indexOf(filter.toLowerCase()) < 0) {
                listing.hide();
                numberHidden++;
            }
            else {
                listing.show();
            }
        }
        if (numberHidden >= this.listings.length) {
            // All hidden, hide title as well
            this.empty.show();
        }
        else {
            this.empty.hide();
        }
    },

    tagfilter: function(filter) {
        var numberHidden = 0
        for (var i = 0; i < this.listings.length; i++) {
            var listing = this.listings[i];
            var listing_tags = listing.find(".module-tag");
            if (listing_tags.text().toLowerCase().indexOf(filter.toLowerCase()) < 0) {
                listing.hide();
                numberHidden++;
            }
            else {
                listing.show();
            }
        }
        if (numberHidden >= this.listings.length) {
            // All hidden, hide title as well
            this.empty.show();
        }
        else {
            this.empty.hide();
        }
    },

    set_title: function(title) {
        this.element.find(".module-list-title").text(title);
    },

    // events bound via _on are removed automatically
    // revert other modifications here
    _destroy: function() {
        // remove generated elements
        this.title.remove();
        for (var i in this.listings) {
            i.remove();
        }
        this.element
            .removeClass('module-list');
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

$.widget( "gp.searchslider", {
    // default options
    options: {
        lists: []
    },

    // the constructor
    _create: function() {
        var slider = this.element;
        this.element.addClass('search-widget');

        // Add the close button
        this.close = $('<button>', {
            'class': 'slider-close',
            'text': 'Close' })
            .button()
            .click(function() {
                slider.searchslider('hide');
            })
            .appendTo(this.element);

        // Add the module lists
        $(this.options.lists).each(function(index, list) {
            slider.append(list);
        });
    },

    show: function() {
        var visible = $(".search-widget:visible");
        visible.each(function(id, slider) {
            $(slider).css("z-index", 1);
        });
        var shown = this;
        this.element.css("z-index", 2);
        this.element.show('slide', {}, 400);
        setTimeout(function() {
            visible.each(function(id, slider) {
                if (slider !== shown) {
                    $(slider).hide();
                }
            });
        }, 400);
    },

    hide: function() {
        this.element.hide('slide', {}, 400);
    },

    filter: function(filter) {
        $(this.options.lists).each(function(index, list) {
            list.modulelist("filter", filter);
        });
    },

    tagfilter: function(filter) {
        $(this.options.lists).each(function(index, list) {
            list.modulelist("tagfilter", filter);
        });
    },

    set_title: function(title) {
        if (this.options.lists.length >= 1) {
            $(this.options.lists[0]).modulelist("set_title", title);
        }
    },

    // events bound via _on are removed automatically
    // revert other modifications here
    _destroy: function() {
        // remove generated elements
        this.title.remove();
        for (var i in this.listings) {
            i.remove();
        }
        this.element
            .removeClass('module-list');
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