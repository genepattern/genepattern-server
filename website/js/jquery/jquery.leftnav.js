/*
 * Copyright 2013 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

$.widget("gp.module", {
    // default options
    options: {
        display: true,
        data: {},
        draggable: true,

        // callbacks
        click: function() {},
        tagclick: function(event) {
            $('#module-search')
                .searchslider('show')
                .searchslider('tagfilter', $(event.currentTarget).text())
                .searchslider('set_title', "Search Tag: " + $(event.currentTarget).text());
            $(event.currentTarget)
                .closest('.module-listing')
                .module('stopProp', event);
        }
    },

    // the constructor
    _create: function() {
        this.element.addClass('module-listing');

        // Save the lsid
        this.lsid = this._protect(this.options.data.lsid, "");

        // Add the ui elements
        this.docicon = $('<a><img src="/gp/css/frozen/modules/styles/images/file_add.gif"></a>')
            .attr("href", this._protect(this.options.data.documentation, ""))
            .attr("target", "_blank")
            .attr("class", "module-doc")
            .attr("title", "Documentation")
            .attr("onclick", "$(this).closest('.module-listing').module('stopProp', event)")
            .tooltip()
            .appendTo(this.element);

        this.version = $('<div>', {
            'class': 'module-version',
            'text': this.options.data.version ? 'v'+ this.options.data.version : ''
        }).appendTo(this.element);

        this.name = $('<div>', {
            'class': 'module-name',
            'text': this._protect(this.options.data.name, "UNNAMED")
        }).appendTo(this.element);

        this.description = $('<div>', {
            'class': 'module-description',
            'text': this._protect(this.options.data.description, "")
        }).appendTo(this.element);

        this.tags = $('<div>', {
            'class': 'module-tag'
        }).appendTo(this.element);

        // Add tag links
        var all_tags_raw = [];
        $.merge(all_tags_raw, this._protect(this.options.data.categories, []));
        $.merge(all_tags_raw, this._protect(this.options.data.suites, []));
        $.merge(all_tags_raw, this._protect(this.options.data.tags, []));
        var all_tags = [];
        $.each(all_tags_raw, function(i, el) { // Remove duplicates
            if($.inArray(el, all_tags) === -1) all_tags.push(el);
        });
        all_tags.sort(); // Sort
        
        for (var tag in all_tags) {
            $('<a>', {
                'class': 'tag',
                'text': all_tags[tag],
                'href': '#'})
                .attr("onclick", "$(this).closest('.module-listing').module('tagClick', event);")
                .appendTo(this.tags);
            this.tags.append(', ');
        }

        if (!this.options.display) {
            this.element.hide();
        }

        if (this.options.draggable) {
            var module = this;
            this.element.draggable({
                helper:'clone',
                connectToSortable:'#pinned-modules',
                scroll: false,
                start: function(event, ui) {
                    ui.helper.data('dropped', false);
                },
                stop: function(event, ui) {
                    if (ui.helper.data('dropped') !== false) {
                        // This means the module has been dropped into pinned modules, do something
                        // TODO: Implement the ajax callback to save pinned modules here
                    }
                }
            });
        }

        // bind events on the widget
        this._on(this.element, {
            click: this.options.click
        });
    },
    
    _protect: function(string, blankReturn) {
    	if (string === null || string === undefined) {
    		return blankReturn;
    	}
    	else {
    		return string;
    	}
    },

    _isValid: function(toElement) {
        if (!toElement) return false;
        var modlist = $(toElement).closest(".module-list");
        if (modlist.length < 1) return false;
        return $(modlist[0]).hasClass("ui-sortable");
    },
    
    get_lsid: function(event) {
        return this._protect(this.options.data.lsid, "");
    },

    tagClick: function(event) {
        this.options.tagclick(event);
    },

    stopProp: function(event) {
        event.stopPropagation();
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

$.widget("gp.modulelist", {
    // default options
    options: {
        title: null,
        data: {},
        droppable: false,
        draggable: true,
        click: function() {}
    },

    // the constructor
    _create: function() {
        this.element.addClass('module-list');

        if (this.options.title) {
            this.title = $('<h4>', {
                'class': 'module-list-title',
                'html': this.options.title
            }).appendTo(this.element);
        }

        this.listings = [];
        for (var id in this.options.data) {
            this.listings.push($('<div>').module({
                data: this.options.data[id],
                click: this.options.click,
                draggable: this.options.draggable
            }).appendTo(this.element));
        }

        this.empty = $('<h4>', {
            'class': 'module-list-empty',
            'text': "No Results Found"
        }).appendTo(this.element);

        if (this.options.droppable) {
            $(this.element).sortable({
                connectWith: '.module-list',
                scroll: false,
                drop: function(event, ui) {
                    ui.draggable.data('dropped', true);
                }
            });
            $(this.element).disableSelection();
        }
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
        this.element.find(".module-list-title").html(title);
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

$.widget("gp.searchslider", {
    // default options
    options: {
        lists: []
    },

    // the constructor
    _create: function() {
        this.element.addClass('search-widget');

        // Add the inner div
        this.inner = $('<div>', {
            'class': 'search-inner'})
            .appendTo(this.element);
        var inner = this.inner;
        var slider = this.element;

        // Add the close button
        this.close = $("<div>", {
            class: 'slider-close-block'})
            .appendTo(this.inner);;
        $('<button></button>', {
            'class': 'slider-close',
            'text': 'Close' })
            .button()
            .click(function() {
                slider.searchslider('hide');
            })
            .appendTo(this.close);

        // Add the module lists
        $(this.options.lists).each(function(index, list) {
            inner.append(list);
        });
    },

    show: function() {
        var visible = $(".search-widget:visible");
        visible.each(function(id, slider) {
            $(slider).css("z-index", 1);
        });
        var shown = this.element;
        this.element.css("z-index", 2);
        this.element.show('slide', {}, 400);
        setTimeout(function() {
            visible.each(function(id, slider) {
                if (slider !== shown[0]) {
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
        this.inner.remove();
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