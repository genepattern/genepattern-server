/**
 * qunit tests for confirming (or denying) basic JavaScript coding assumptions.
 * 
 * Think of this as a scratch space experimenting with basic JavaScript constructs.
 * For example the difference between '==' and '==='
 */


function initGpContextDefault() {
    return initGpContextDefaultWindow(1)
}

function initGpContextDefaultWindow(level) {
    return initGpContext(level, window)
}

/**
 * A utility method to infer the servlet context path (default="/gp") from the current page, thus
 * avoiding the need to hard-code the context path into the JavaScript library.
 * Example usage, called from top-level page, e.g. '/gp/index.html
 *     var gpContext=initGpContext(1);
 * 
 * Level is an integer >= 1 indicating the number of path-segments to the current page
 * relative to {gpContext}. Examples,
 * 
 *     level: pathname
 *         1: '{gpContext}/'
 *         1: '{gpContext}/index.html
 *         2: '{gpContext}/js/gpUtil.js
 *         3: '{gpContext}/test/gpunit/runner.html
 * 
 * From the Java Servlet Spec
 *     requestURI = contextPath + servletPath + pathInfo
 *     
 * From the javadoc for ServletContext.html#getContextPath(),
 *     The context path is the portion of the request URI that is used to select the context of the request. 
 *     The context path always comes first in a request URI. The path starts with a "/" character but does not
 *     end with a "/" character. For servlets in the default (root) context, this method returns "".
 * 
 * From the URL Spec:
 *     A path-absolute URL must be "/" followed by a path-relative URL.
 *     A path-relative URL must be zero or more path segments, separated from each other by "/", and not start with "/".
 * 
 * 
 * @param level
 * @param mockWindow, when not set, defaults to 'window.location.pathname'
 * @returns
 */
function initGpContext(level, mockWindow) {
    var DEFAULT_CONTEXT_PATH="/gp";
    
    // for testing, use mockWindow when set
    // otherwise default to the 'window' object
    if (mockWindow===undefined) {
        mockWindow=window;
    }
    var pathname=mockWindow.location.pathname;
    if (pathname === undefined || (typeof pathname != "string") || pathname.length==0 || pathname.charAt(0) != '/') {
        console.error("window.location.pathname not available, returning hard-coded contextPath='"+DEFAULT_CONTEXT_PATH+"'");
        return DEFAULT_CONTEXT_PATH;
    }
    if ((typeof level != "integer") && level <= 0) {
        console.error("level='"+level+"', must be an integer > 0, returning hard-coded contextPath='"+DEFAULT_CONTEXT_PATH+"'");
        return DEFAULT_CONTEXT_PATH;
    }
    var pathnames=pathname.split('/');
    var end=pathnames.length-level;
    if (end <= 0) {
        console.error("level="+level+", must be less than the number of path-segments in '"+pathname+"', returning hard-coded contextPath='"+DEFAULT_CONTEXT_PATH+"'");
        return DEFAULT_CONTEXT_PATH;
    }

    return initGpContextNoCheck(level, mockWindow.location.pathname);
}

/**
 * Get the servlet context path. Does not validate input args.
 * @param level (default=1), must be > 0 and < num path-segments
 * @param pathname (default=window.location.pathname)
 */
function initGpContextNoCheck(level, pathname) {
    var pathnames=pathname.split('/');
    var gpContext=pathnames.slice(0, pathnames.length-level).join('/');
    return gpContext;
}

function mockWindow(mockPathname) {
    var w = { 
        location: {
            pathname: mockPathname
        }
    };
    return w;
}

test("javascript.basic", function() {
    //console.debug("window.location.pathname="+window.location.pathname);
    equal(typeof window, "object", "typeof window" );
    equal(typeof 0, "number", "typeof 0") 
    
    deepEqual("".split(".").map(Number), [ 0 ],  "''.split('.')");
    deepEqual("1".split(".").map(Number), [1],  "'1'.split('.').map(Number)");
    deepEqual("1.2".split(".").map(Number), [1, 2],  "'1.2'.split('.').map(Number)");
    deepEqual("1.314.0.1".split(".").map(Number), [1, 314, 0, 1],  "'1.2'.split('.').map(Number)");
})

test("servletContextPath", function() {
    equal(initGpContext(3), '/gp', "From 'window.location.pathname', Note: won't work with non-default contextPath");

    var pathname, gpContext, level;
    
    // default gpContext
    gpContext="/gp";
    level=1; pathname=gpContext+"/index.html";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=2; pathname=gpContext+"/js/gpUtil.js";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=3; pathname=gpContext+"/test/gpunit/runner.html"
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    // ... page with trailing slash
    level=1; pathname=gpContext+"/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=2; pathname=gpContext+"/my-servlet-path/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=3; pathname=gpContext+"/my-servlet-path/my-path-info/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");

    // ROOT gpContext
    gpContext="";
    level=1; pathname=gpContext+"/index.html";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=2; pathname=gpContext+"/js/gpUtil.js";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=3; pathname=gpContext+"/test/gpunit/runner.html"
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    // ... page with trailing slash
    level=1; pathname=gpContext+"/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=2; pathname=gpContext+"/my-servlet-path/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=3; pathname=gpContext+"/my-servlet-path/my-path-info/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    
    // custom gpContext, same level
    gpContext="/gp-custom";
    level=1; pathname=gpContext+"/index.html";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=2; pathname=gpContext+"/js/gpUtil.js";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=3; pathname=gpContext+"/test/gpunit/runner.html"
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    // ... page with trailing slash
    level=1; pathname=gpContext+"/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=2; pathname=gpContext+"/my-servlet-path/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=3; pathname=gpContext+"/my-servlet-path/my-path-info/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    
    // custom gpContext, different level
    gpContext="/gp-custom/diff-level";
    level=1; pathname=gpContext+"/index.html";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=2; pathname=gpContext+"/js/gpUtil.js";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    level=3; pathname=gpContext+"/test/gpunit/runner.html"
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname);
    // ... page with trailing slash
    level=1; pathname=gpContext+"/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=2; pathname=gpContext+"/my-servlet-path/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");
    level=3; pathname=gpContext+"/my-servlet-path/my-path-info/";
    equal(initGpContext(level, mockWindow(pathname)), gpContext, "Context='"+gpContext+"', level="+level+", pathname="+pathname+" <-- trailing slash");

});

test("mock-console.error", function() {
    var expected_error="expected error message";
    consoleTest( function() { console.error(expected_error); }, undefined, expected_error, "validate mock console.error");
});

test("servletContextPath-invalid-input", function() {
    // Invalid window.location.pathname 
    consoleTest( function() { return initGpContext(1, { location: {} }); }, 
        "/gp", 
        "window.location.pathname not available, returning hard-coded contextPath='/gp'",
        "Invalid input: window.location.pathname undefined"
    );

    consoleTest( function() { return initGpContext(1, mockWindow(1)); }, 
        "/gp",
        "window.location.pathname not available, returning hard-coded contextPath='/gp'",
        "Invalid input: window.location.pathname is not a string"
    );
    
    consoleTest( function() { return initGpContext(1, mockWindow("")); }, 
        "/gp", 
        "window.location.pathname not available, returning hard-coded contextPath='/gp'",
        "Invalid input: window.location.pathname is an empty string"
    );
    
    consoleTest( function() { return initGpContext(1, mockWindow("index.html")); },
        "/gp", 
        "window.location.pathname not available, returning hard-coded contextPath='/gp'",
        "Invalid input: window.location.pathname does not start with '/'"
    );
    
    // Invalid input: level <= 0
    consoleTest( function() { return initGpContext(0, mockWindow("/gp-custom/index.html")); }, 
        "/gp", 
        "level='0', must be an integer > 0, returning hard-coded contextPath='/gp'",
        "Invalid input: level=0, Must be > 0, use '/gp'"
    );
    
    consoleTest( function() { return initGpContext(-1, mockWindow("/gp-custom/index.html")) }, 
        "/gp",
        "level='-1', must be an integer > 0, returning hard-coded contextPath='/gp'",
        "Invalid input: level=-1, Must be > 0, use '/gp'");
    
    // Invalid input: pathSegments.length < level
    level=3; pathname="/gp-custom/index.html";
    consoleTest( function() { return initGpContext(level, mockWindow(pathname)) }, 
        "/gp", 
        "level=3, must be less than the number of path-segments in '/gp-custom/index.html', returning hard-coded contextPath='/gp'",
        "Invalid input: level > num path-segments, level="+level+", pathname='"+pathname+"'");

    level=4;
    consoleTest( function() { return initGpContext(level, mockWindow(pathname)) }, 
        "/gp",
        "level=4, must be less than the number of path-segments in '/gp-custom/index.html', returning hard-coded contextPath='/gp'",
        "Invalid input: level > num path-segments, level="+level+", pathname='"+pathname+"'");

});

/**
 * Wrapper qunit deepEqual test which suppresses the expected console.error message.
 * Calls,
 *     deepEqual(_fn, expected_output, message);
 *     
 * @param _fn, the method to test
 * @param expected_output, the expected output of the test _fn
 * @param expected_error, the expected console.error message
 * @param message, the test message
 */
function consoleTest(_fn, expected_output, expected_error, message) {
    var orig = console.error;
    try {
        var output = [];
        console.error = function (message) {
            output.push( message );
        }
        if (!message) { message = "actual test"; }
        deepEqual(_fn(), expected_output, message);
    }
    finally {
        console.error=orig;        
    }
    deepEqual(output, [ expected_error ], message + ", expected console.error");
}

