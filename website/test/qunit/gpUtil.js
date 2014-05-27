/*
 * JavaScript Utility functions for the GenePattern project 
 */

var gpUtil = function() {
    var aNull = null;
    var emptyString = "";
    var aString = "thisIsAString";
    function doIt() {
        return true;
    }
    return {
        aNull:aNull,
        emptyString:emptyString,
        aString:aString,
        doIt:doIt
    };
}();
