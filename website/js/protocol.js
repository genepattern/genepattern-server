//Note on window names
//    'genepattern' - the main GenePattern window
//    'protocol' - the new window which gets created when linking to a protocol from the main GenePattern window

function openProtocolWindow(theTarget, theEvent) {
  var theURL = theTarget.href;
  if (theEvent != null) { //for IE
    theEvent.returnValue = false;
  }
  if (window.name != 'protocol') {
    window.name = 'genepattern';
  }
  w = window.open(theURL,'protocol','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=515,height=400',false);
  w.focus();
  return false;
}

function openModuleWindow(theTarget, theEvent) {
  var theURL = updateTheUrl( theTarget.href );
  if (theEvent != null) { //for IE
    theEvent.returnValue = false;
  }
  w = window.open(theURL,'genepattern','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600',false);
  w.focus();
  return false;
}

function openIgvWindow(theTarget, theEvent) {
    var theURL = theTarget.href;
    if (theEvent != null) { //for IE
      theEvent.returnValue = false;
    }
    w = window.open(theURL,'igv','toolbar=1,menubar=1,scrollbars=1,resizable=1,width=800,height=600',false);
    w.focus();
    return false;
}

function updateTheUrl(theURL) {
    var idx=theURL.indexOf('?');
    var query = idx >= 0 ? theURL.substring(idx) : null;
    if (query) {
        var updatedQuery = modifyQueryStringFromLocation( query );
        return theURL.substring(0, idx) + updatedQuery;
    }
    else {
        return theURL;
    }
}

/**
 * Helper function to handle http/https protocol before opening links to example data files.
 * Calls modifyQueryString with the current 'window.location.protocol' as input.
 * @param search the queryString to adjust
 */
function modifyQueryStringFromLocation( search ) {
    return modifyQueryString( location.protocol, search );
}

/**
 * Handle 'Open module with example data' links from the Protocols page,
 * so that the links to the data files use the same protocol (http or https)
 * as the target server. Update values of the form:
 *     ?input.filename=[{protocol}]//{host}{path}, 
 *     where protocol := 'https:' | 'http:' | ''
 * 
 * @param protocol e.g. 'https:', (default='window.location.protocol')
 * @param search the url queryString to modifiy, if necessary.
 * @returns
 */
function modifyQueryString( protocol, search ) {
    // strip leading '?'
    var queryString=search;
    if (search && search.charAt(0) == '?') {
        queryString=search.substring(1);
    }
    if (!queryString) {
        return search;
    }

    var updated = "?";
    // Split into key/value pairs
    var queries = queryString.split("&");
    var i, l;
    for ( i = 0, l = queries.length; i < l; i++ ) {
        if ( i > 0) {
            updated += '&';
        }
        updated += modifyRequestParam( protocol, queries[i] );
    }
    return updated;
}

/**
 * @param protocol is one of 'https:' or 'http:'
 * @param keyValuePair from a queryString, e.g. 'input.filename=http://www.example.com/data.txt'
 * @returns a (possibly updated) encoded(key)=encoded(value) String
 */
function modifyRequestParam( protocol, keyValuePair ) {
    var temp = keyValuePair.split('=');
    if (temp[1]) {
        var val=decodeURIComponent(temp[1]); 
        var updated = modifyValue( protocol, val );
        keyValuePair = temp[0] + '=' + encodeURIComponent( updated );
        console.log('protocol='+protocol+', val='+updated);
    }
    return keyValuePair;
}

/**
 * Update the value so that the link matches the given protocol. 
 * Example values when protocol is 'https:':
 *     //www.example.com/path, replace '//' with 'https://'
 *     http://www.example.com/path, replace 'http://' with 'https://'
 *     https://www.example.com/path, no change
 * 
 * @param protocol, the 'window.location.protocol', e.g. 'https:'
 * @param value
 * @returns an updated value
 */
function modifyValue( protocol, value ) {
    return value.replace(/^https:\/\/|^http:\/\/|^\/\//i, protocol+'//');
}
