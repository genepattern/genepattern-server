/*
 * JavaScript Utility functions for the GenePattern project. 
 * 
 * Use "this" as a Namespace Proxy, as suggested in this article,
 *     https://javascriptweblog.wordpress.com/2010/12/07/namespacing-in-javascript/
 */

/** declare global 'gpUtil' namespace variable */
var gpUtil = {};

/**
 * Example usage:
 *     var myGpUtil={};
 *     # 1) custom gp context
 *     InitGpUtil.call(myGpUtil,"/custom/gp/context");
 *     # 2) default gp context
 *     InitGpUtil.call(myGpUtil);
 *     
 */
var InitGpUtil = function(customGpContext) {
    var that = this;
    
    /** private static helper function to initialize gpContext */
    function initGpContext(customGpContext) {
        return   customGpContext === undefined ? "/gp" 
               : customGpContext === "" ? "/" 
               : customGpContext ;
    }

    /** private variable */
    var gpContext = initGpContext(customGpContext); 

    // Declare public functions 
    this.setGpContext = function(customGpContext) {
        gpContext = initGpContext(customGpContext);
    }

    this.getGpContext = function getGpContext() {
        return gpContext;
    };
    
    /**
     * helper function to test if an object is a string
     */
    this.isString = function(s) {
        return typeof(s) === 'string' || s instanceof String;
    }
    
    /**
     * Pretty print the date for display on the job status page in "month day, h:mm am/pm" format, E.g.
     *     'May 27, 9:40 pm'
     * @param date, a valid Date object
     */
    this.formatDate = function(date) {
        var dateFormat = $.datepicker.formatDate('M d, ', date) + this.formatTimeOfDay(date);
        return dateFormat;
    }
    
    /**
     * For the given Date object, pretty print the time of day in am/pm format,
     * E.g. '9:40 pm'
     * 
     * @param date, a valid Date object
     */
    this.formatTimeOfDay = function (date) {
        if (!date) {
            return "";
        }
        return date.getHours() %12 
            + ':' + this.pad(date.getMinutes())
            + ' ' + (date.getHours() >= 12 ? 'pm' : 'am');
    }

    this.pad = function(num) {
        var norm = Math.abs(Math.floor(num));
        return (norm < 10 ? '0' : '') + norm;
    }

    /**
     * Helper function for printing the timezone offset in ISO 8601 format. E.g.
     *     GMT   == 'Z'
     *     GMT-4 == '-04:00'
     *     GMT+7 == '+07:00'
     * 
     * @param offset, the offset in number of minutes, returned by Date.getTimezoneOffset.
     */
    this.formatTimezone = function(offset) {
        if (offset===undefined) {
            offset = new Date().getTimezoneOffset();
        }
        if (offset==0) {
            return "Z";
        }
        //invert the sign to match ISO 8601, e.g. for GMT-4 Date.getTimezoneOffset returns +4.
        return ""+(offset > 0 ? "-" : "+") // sign
            + this.pad(offset / 60) // hours
            + ":"
            + this.pad(offset % 60); // minutes
    }

    /**
     * For the given date, format the offset for ISO 8601 format.
     * E.g.
     *     GMT   == 'Z'
     *     GMT-4 == '-04:00'
     *     GMT+7 == '+07:00'
     */
    this.getTimezoneOffsetIso = function() {
        var theDate = new Date();
        return this.formatTimezone(theDate.getTimezoneOffset());
    }

    /**
     * Hide or show the div based on whether or not the cookie has been set.
     * By default the div is hidden, unless the cookie was set.
     */
    this.initToggleDiv = function(id, openLabel, closedLabel) {
        if ($.cookie("show_"+id)) {
            //show it
            this.toggleHideShowDiv(true, id, openLabel, closedLabel);
        }
        else {
            //hide it
            this.toggleHideShowDiv(false, id, openLabel, closedLabel);
        }
    }

    /**
     * Toggle visibility of the div with the given id, saving visibility state as a cookie for subsequent page reloads.
     */
    this.toggleDiv = function(id, openLabel, closedLabel) {
        var show = !($("#"+id).is(":visible"));
        this.toggleHideShowDiv(show, id, openLabel, closedLabel);
    }

    /**
     * Helper method which optionally changes the content of the id{Label}.
     */
    this.toggleHideShowDiv = function(show, id, openLabel, closedLabel) {
        if (show===true) {
            $("#"+id).show();
        }
        else {
            $("#"+id).hide();
        }
        var isVisible = ($("#"+id).is(":visible"));
        // optionally toggle the label content
        if (openLabel) {
            $("#"+id+"Label").html(isVisible ? openLabel : closedLabel);
        }
        if (isVisible) {
            $.cookie("show_"+id, "true");
        }
        else {
            $.removeCookie("show_"+id);
        }
    }

    /**
     * Parse the window.location.query string of the form '?{name1}={value}&{name2}={value}&{flag}
     * @param the search string from a URL, by default 'window.location.search', which is the '?' character followed
     *     by the queryString.
     * @return a hash of decoded values, where the rhs value is an array. E.g.
     *     '?myFlag', { myFlag: [ "" ] }
     *     '?param=val, { param: [ "val" ] }
     *     '?params=val1&params=val2, { params : [ "val1", "val2" ] }
     */
    this.parseQueryString = function( search ) {
        if (search === undefined) {
            search = window.location.search;
        }
        // strip leading '?'
        var queryString=search;
        if (search && search.charAt(0) == '?') {
            queryString=search.substring(1);
        }
        if (!queryString) {
            return {};
        }
        
        var params = {}, queries, temp, i, l;
     
        // Split into key/value pairs
        queries = queryString.split("&");
     
        // Convert the array of strings into an object
        for ( i = 0, l = queries.length; i < l; i++ ) {
            temp = queries[i].split('=');
            var key=decodeURIComponent(temp[0]);
            var val=""; // undefined value will be set to a string, e.g. '?myFlag' with no equal sign
            if (temp[1]) {
                val=decodeURIComponent(temp[1]); 
            }

            var myArray=params[key];
            if (!myArray) {
                myArray = new Array();
                params[key]=myArray;
            }
            myArray.push(val);
        }     
        return params;
    }

    /**
     * Check whether the string ends with the specified text
     * @param string - the string to search for the suffix
     * @param suffix - the text to search for
     * @returns {boolean}
     */
    this.endsWith = function(string, suffix) {
        return string.length >= suffix.length
            && string.substr(string.length - suffix.length) === suffix;
    }
    
    /*
     *  lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
     */
    this.Lsid = function Lsid(lsid) {
        var lsidSpec = lsid;
        var authority = null;
        var namespace = null;
        var identifier = null;
        var version = null;
        var versions = [];
        
        // parse the lsid into it's parts
        function _init() {
            if (typeof lsid === "string") {
                var elements = lsid.split(":");
                authority = elements[2];
                namespace = elements[3];
                identifier = elements[4];
                version = elements[5];
            }
            // parse the version into it's parts
            if (version) {
                versions = version.split(".").map(Number);
            }
        } 

        _init();

        this.getLsid = function() { return lsidSpec; }
        this.getAuthority = function() { return authority; }
        this.getNamespace = function() { return namespace; }
        this.getIdentifier = function() { return identifier; }
        this.getVersion = function() { return version; }
        this.getVersions = function() { return versions; } 
        this.getPatchLevel = function() { return Math.max(1, versions.length); }
    }
    
    this.Lsid.prototype.toString = function lsidToString() {
        return this.lsidSpec;
    }
    
    this.Lsid.prototype.compareVersion = function(to) {
        if (to === undefined) {
            console.error("compareVersion(to): to is undefined");
            return -1;
        }
        if (to.getVersion === undefined) {
            console.error("compareVersion(to): to.getVersion is undefined");
            return -1;
        }
        
        // special-case, versions.size==0 is always less than versions.size > 0
        if (this.getVersions().length==0) {
            if (to.getVersions().length==0) {
                return 0;
            }
            return -1;
        }
        else if (to.getVersions().length==0) {
            return 1;
        }
        
        for(var i=0; i<this.getVersions().length; ++i) {
            var v=this.getVersions()[i];
            var ov =  i<to.getVersions().length ? to.getVersions()[i] : 0;
            if (v < ov) {
                return -1;
            }
            else if (v > ov) {
                return 1;
            }
        }
        
        // if we are here, it means each element in versions matches each corresponding element in o.versions
        if (this.getVersions().length==to.getVersions().length) {
            return 0;
        }
        else if (this.getVersions().length < to.getVersions().length) {
            return -1;
        }
        else {
            return 1;
        }
    }
    
    /**
     * Helper class for building the versionIncrement menu.
     * @param lsid - the current lsid String
     * @param lsidVersions - an array of lsid strings
     */
    this.LsidMenu = function LsidMenu(lsid, lsidVersions) {
        this.currentLsid=lsid;

        // the level of the current version (aka patchLevel)
        //     1 is major, 2 is minor, 3 is patch, and so on ...
        this.patchLevel=1;
        
        // flag indicating if this is a New module
        this.isNew= !lsid && (lsidVersions === undefined || lsidVersions.length==0);

        // flag indicating if this is the latest version, isNew implies isLatest
        this.isLatest=this.isNew; 
        // initialize isLatest from (current) lsid and listVersions array
        if (!this.isNew && lsid && lsidVersions !== undefined) {
            var all=new Array();
            var last=null;
            for(var i = 0; i < lsidVersions.length; i++) {
                var lsidObj = new gpUtil.Lsid(lsidVersions[i]);
                all.push( lsidObj );
            }
            all.sort(function(a,b) { return a.compareVersion(b); } );
            last=all[all.length-1];
            if (this.currentLsid === last.getLsid()) {
                this.isLatest=true;
            }
        }
        
        // initialize the options array, used to populate the drop-down menu
        // each option has a value=[major | minor | patch | default], and a display-name
        this.options = [];
        this.selectedValue="default";
        this.options=[
            { "value": "default", "name": "" },
            { "value": "major", "name": "major (X)" },
            { "value": "minor", "name": "minor (X.Y)" },
            { "value": "patch", "name": "patch (X.Y.Z)" }
        ];
        
        if (this.isNew) {
            this.selectedValue="major";
            this.options= [ 
                { value: "major", name: "New major version (v1)" },
                { value: "minor", name: "New minor version (v0.1)" },
                { value: "patch", name: "New patch version (v0.0.1)" },
            ];
        }
        else if (this.isLatest) {
            this.options= [ 
                { value: "default", name: "" },
                { value: "major", name: "Next major version (X)" },
                { value: "minor", name: "Next minor version (X.Y)" },
                { value: "patch", name: "Next patch version (X.Y.Z)" },
            ];
        }
        else {
            this.options= [
                { value: "default", name: "" },
            ];
        }

        this.getOptions = function() {
            return this.options;
        }
        
        this.getPatchLevel = function() {
            return this.patchLevel;
        }
        
        this.getSelectedValue = function() {
            return this.selectedValue;
        }
        
        this.isEnabled = function() {
            // drop-down is only enabled for New or Latest version
            return this.isNew || this.isLatest;
        }
    }

};

InitGpUtil.call(gpUtil);
