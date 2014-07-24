/*
 * JavaScript Utility functions for the GenePattern project 
 */

var gpUtil = function() {
    
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
        norm = Math.abs(Math.floor(num));
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

    // declare 'public' functions
    return {
        formatTimezone:formatTimezone,
        getTimezoneOffsetIso:getTimezoneOffsetIso,
        formatDate:formatDate,
        formatTimeOfDay:formatTimeOfDay,
        initToggleDiv:initToggleDiv,
        toggleDiv:toggleDiv
    };
}();
