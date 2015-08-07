/*
 * JavaScript Utility functions for the GenePattern project 
 */

var gpUtil = function() {

    /**
     * helper function to test if an object is a string
     */
    function isString(s) {
        return typeof(s) === 'string' || s instanceof String;
    }
    
    /**
     * Pretty print the date for display on the job status page in "month day, h:mm am/pm" format, E.g.
     *     'May 27, 9:40 pm'
     * @param date, a valid Date object
     */
    function formatDate(date) {
        var dateFormat = $.datepicker.formatDate('M d, ', date) + formatTimeOfDay(date);
        return dateFormat;
    }
    
    /**
     * For the given Date object, pretty print the time of day in am/pm format,
     * E.g. '9:40 pm'
     * 
     * @param date, a valid Date object
     */
    function formatTimeOfDay(date) {
        if (!date) {
            return "";
        }
        return date.getHours() %12 
            + ':' + pad(date.getMinutes())
            + ' ' + (date.getHours() >= 12 ? 'pm' : 'am');
    }

    function pad(num) {
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
    function formatTimezone(offset) {
        if (offset===undefined) {
            offset = new Date().getTimezoneOffset();
        }
        if (offset==0) {
            return "Z";
        }
        //invert the sign to match ISO 8601, e.g. for GMT-4 Date.getTimezoneOffset returns +4.
        return ""+(offset > 0 ? "-" : "+") // sign
            + pad(offset / 60) // hours
            + ":"
            + pad(offset % 60); // minutes
    }

    /**
     * For the given date, format the offset for ISO 8601 format.
     * E.g.
     *     GMT   == 'Z'
     *     GMT-4 == '-04:00'
     *     GMT+7 == '+07:00'
     */
    function getTimezoneOffsetIso() {
        var theDate = new Date();
        return formatTimezone(theDate.getTimezoneOffset());
    }

    /**
     * Hide or show the div based on whether or not the cookie has been set.
     * By default the div is hidden, unless the cookie was set.
     */
    function initToggleDiv(id, openLabel, closedLabel) {
        if ($.cookie("show_"+id)) {
            //show it
            toggleHideShowDiv(true, id, openLabel, closedLabel);
        }
        else {
            //hide it
            toggleHideShowDiv(false, id, openLabel, closedLabel);
        }
    }

    /**
     * Toggle visibility of the div with the given id, saving visibility state as a cookie for subsequent page reloads.
     */
    function toggleDiv(id, openLabel, closedLabel) {
        var show = !($("#"+id).is(":visible"));
        toggleHideShowDiv(show, id, openLabel, closedLabel);
    }

    /**
     * Helper method which optionally changes the content of the id{Label}.
     */
    function toggleHideShowDiv(show, id, openLabel, closedLabel) {
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
     * Parse the given query string of the form '?{name1}={value}&{name2}={value}&{flag}
     * @param the query string from a URL, for example window.location.search
     *        If it is empty then it defaults to window.location.search
     * @return a hash of decoded values, where the rhs value is an array. E.g.
     *     '?myFlag', { myFlag: [ "" ] }
     *     '?param=val, { param: [ "val" ] }
     *     '?params=val1&params=val2, { params : [ "val1", "val2" ] }
     */
    function parseQueryString( queryString ) {
        if (!queryString) {
            queryString= window.location.search;
        }
        // strip leading '?'
        if (queryString.charAt(0) == '?') {
            queryString=queryString.substring(1);
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
    function endsWith(string, suffix) {
        return string.length >= suffix.length
            && string.substr(string.length - suffix.length) === suffix;
    }

    // declare 'public' functions
    return {
        formatTimezone:formatTimezone,
        getTimezoneOffsetIso:getTimezoneOffsetIso,
        formatDate:formatDate,
        formatTimeOfDay:formatTimeOfDay,
        initToggleDiv:initToggleDiv,
        toggleDiv:toggleDiv,
        parseQueryString:parseQueryString,
        endsWith: endsWith
    };
}();
