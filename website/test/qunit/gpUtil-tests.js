QUnit.test("gpUtil.gpContext", function(assert) {
    assert.equal(gpUtil.getGpContext(), "/gp", "gpUtil.getGpContext(), default");
    
    var myGpUtil={};
    InitGpUtil.call(myGpUtil);
    assert.equal(myGpUtil.getGpContext(), "/gp", "myGpUtil.getGpContext(), default");

    // setGpContext to ROOT
    myGpUtil.setGpContext("/");
    assert.equal(myGpUtil.getGpContext(), "/", "setGpContext('/')");
    
    // setGpContext to ROOT, special-case for empty string ("")
    myGpUtil.setGpContext("");
    assert.equal(myGpUtil.getGpContext(), "/", "setGpContext('')");
    
    // setGpContext to custom value
    myGpUtil.setGpContext("/custom/gp/context");
    assert.equal(myGpUtil.getGpContext(), "/custom/gp/context", "setGpContext('/custom/gp/context'");    

    // initGpContext as ROOT
    InitGpUtil.call(myGpUtil,"/");
    assert.equal(myGpUtil.getGpContext(), "/", "InitGpUtil with '/'");

    InitGpUtil.call(myGpUtil,"");
    assert.equal(myGpUtil.getGpContext(), "/", "InitGpUtil with ''");
    
    // init custom context
    InitGpUtil.call(myGpUtil,"/custom/gp/context");
    assert.equal(myGpUtil.getGpContext(), "/custom/gp/context", "InitGpUtil with '/custom/gp/context'");    
});

QUnit.test("gpUtil.formatTimezone", function(assert) {
    // test 'gpUtil.formatTimezone'
    assert.equal(gpUtil.formatTimezone(), gpUtil.formatTimezone(new Date().getTimezoneOffset()), "formatTimezone, local timezone");
    assert.equal(gpUtil.formatTimezone(240), "-04:00", "formatTimezone, GMT-4");
    assert.equal(gpUtil.formatTimezone(-60), "+01:00", "formatTimezone, GMT+1");
    assert.equal(gpUtil.formatTimezone(340), "-05:40", "formatTimezone, fraction of an hour");
    assert.equal(gpUtil.formatTimezone(-660), "+11:00", "formatTimezone, GMT+11");
    assert.equal(gpUtil.formatTimezone(0), "Z", "formatTimezone, GMT");
    
    // format am/pm
    assert.equal(gpUtil.formatTimeOfDay(), "", "format am/pm, with null arg");
    assert.equal(gpUtil.formatTimeOfDay(new Date("2007-06-29T15:55:10.237-04:00")), "3:55 pm", "format am/pm, pm");
    assert.equal(gpUtil.formatTimeOfDay(new Date("2007-06-29T11:55:10.237-04:00")), "11:55 am", "format am/pm, am");
});

// simple test that the function returns a value
QUnit.test("gpUtil.getTimezoneOffsetIso", function(assert) {
    // function definition test
    var tzOffsetIso=gpUtil.getTimezoneOffsetIso();
    assert.equal($.type(tzOffsetIso), "string", "jquery, is tzOffsetIso a string");
    assert.equal(typeof tzOffsetIso, "string", "typeof tzOffsetIso");
    assert.ok(tzOffsetIso.length > 0, "tzOffsetIso.length > 0");
});

/**
 * custom 'assertion', to ensure that the date was properly created.
 * Make assertions by using the local timezone.
 * ... need some way to format the date back out again in the original timezone
 * @param d, a valid Date object
 * @param message, the qUnit message in the event of failure
 */
function checkTimeoffset(d, hours, minutes, message) {
    var offsetHours=new Date().getTimezoneOffset() / 60;
    var expectedHours;
    if (hours>offsetHours) {
        expectedHours = hours-offsetHours;
    }
    else {
        expectedHours = hours-offsetHours+24;
    }
    expectedHours = expectedHours % 24;
    QUnit.assert.equal(d.getHours(), expectedHours, message+" check hours");
    QUnit.assert.equal(d.getMinutes()+d.getTimezoneOffset()%60, minutes, message+" check minutes");
}

QUnit.test("gpUtil.formatDate", function(assert) {
    // ISO 8601 Date format examples
    // make sure tests will pass in different timezones
    var localOffset=gpUtil.formatTimezone();
    assert.equal( gpUtil.formatDate(new Date("2007-06-29T15:55:10.237"+localOffset)), "Jun 29, 3:55 pm", "formatDate, with milliseconds");
    assert.equal( gpUtil.formatDate(new Date("2007-06-29T15:55:10"+localOffset)), "Jun 29, 3:55 pm", "formatDate, without milliseconds");
    
    // in mozilla, if the timezone is not specified, the UTC timezone is assumed, Don't rely on this
    //equal( gpUtil.formatDate(new Date("2007-06-29T15:55:10.237")), "Jun 29, 3:55 pm", "formatDate, with milliseconds, no offset");
    
    // tests for initializing date objects 
    var pmDate="2007-06-29T15:55:10.237";
    var amDate="2007-06-29T03:55:10.237";
    checkTimeoffset(new Date(pmDate+"Z"),      15, 55, "new pm Date, 'Z' offset");
    checkTimeoffset(new Date(pmDate+"-00:00"), 15, 55, "new pm Date, '-00:00' offset");
    checkTimeoffset(new Date(pmDate+"+00:00"), 15, 55, "new pm Date, '+00:00' offset");

    checkTimeoffset(new Date(pmDate+"-04:00"), 19, 55, "new pm Date, '-04:00' offset");
    checkTimeoffset(new Date(pmDate+"-08:00"), 23, 55, "new pm Date, '-08:00' offset");
    checkTimeoffset(new Date(pmDate+"-11:00"),  2, 55, "new pm Date, '-11:00' offset");
    
    checkTimeoffset(new Date(pmDate+"+01:00"), 14, 55, "new pm Date, '+01:00' offset");
    checkTimeoffset(new Date(pmDate+"+11:00"), 4,  55, "new pm Date, '+11:00' offset");

    checkTimeoffset(new Date(amDate+"Z"),       3, 55, "new am Date, 'Z' offset");
    checkTimeoffset(new Date(amDate+"-00:00"),  3, 55, "new am Date, '-00:00' offset");
    checkTimeoffset(new Date(amDate+"+00:00"),  3, 55, "new am Date, '+00:00' offset");

    checkTimeoffset(new Date(amDate+"-04:00"),  7, 55, "new am Date, '+00:00' offset");
    checkTimeoffset(new Date(amDate+"+03:00"),  0, 55, "new am Date, '+00:00' offset");
    checkTimeoffset(new Date(amDate+"+04:00"), 23, 55, "new am Date, '+00:00' offset");
    checkTimeoffset(new Date(amDate+"-12:00"), 15, 55, "new am Date, '+00:00' offset");

});

QUnit.test("gputil.parseQueryString", function(assert) {
    // useful for encoding/decoding values http://meyerweb.com/eric/tools/dencoder/

    // special-case: No arg
    assert.deepEqual(gpUtil.parseQueryString(), gpUtil.parseQueryString(window.location.search), 
        "No arg, use 'window.location.search'");
    
    // special-case: undefined
    assert.deepEqual(gpUtil.parseQueryString(undefined), gpUtil.parseQueryString(window.location.search), 
            "undefined literal arg, use 'window.location.search'"); 

    var arg_not_defined;
    assert.deepEqual(gpUtil.parseQueryString(arg_not_defined), 
            gpUtil.parseQueryString(window.location.search), 
            "undefined variable arg, use 'window.location.search'"); 
    
    // special-case: null arg
    assert.deepEqual(gpUtil.parseQueryString(null), {}, "null arg, means no query string");
    
    // special=case: empty arg
    assert.deepEqual(gpUtil.parseQueryString(""), {}, "Empty arg (''), means no query string");

    // example of an href as a parameter value (note the '@' in the gp username is encoded as '%40')
    var urlVal="http://127.0.0.1:8080/gp/users/user%40email.com/all_aml_test.gct";
    var encodedVal="http%3A%2F%2F127.0.0.1%3A8080%2Fgp%2Fusers%2Fuser%2540email.com%2Fall_aml_test.gct";

    var search="?";
    assert.deepEqual(gpUtil.parseQueryString(search), {}, "Empty query: '"+search+"'");
    search="?fieldA";
    assert.deepEqual(gpUtil.parseQueryString(search), { "fieldA": [ "" ] }, "Field with no value: '"+search+"'");
    search="?fieldA=";
    assert.deepEqual(gpUtil.parseQueryString(search), { "fieldA": [ "" ] }, "Field with empty value: '"+search+"'");
    search="?fieldA=value01";
    assert.deepEqual(gpUtil.parseQueryString(search), { "fieldA": [ "value01" ] }, "Field with 1 value: '"+search+"'");
    search="?fieldA=value01&fieldA=value02";
    assert.deepEqual(gpUtil.parseQueryString(search), { "fieldA": [ "value01", "value02" ] }, "Field with multiple values: '"+search+"'");
    search="?input.file="+encodedVal;
    assert.deepEqual(gpUtil.parseQueryString(search), { "input.file": [ urlVal ] }, "Field with encoded value: '"+search+"'");

    // Full example query with mock 'window.location.search'
    var exampleQuery="fieldA&fieldB&argA=1.txt&argB=1.txt&argB=2.txt&input.file="+encodedVal+"&argB=3.txt";
    var exampleObject={
            "fieldA": [ "" ],
            "fieldB": [ "" ],
            "argA" : [ "1.txt" ],
            "argB" : [ "1.txt", "2.txt", "3.txt" ],
            "input.file": [ urlVal ]
    };
    var mockWindow = {
            location: {
                href: "http://127.0.0.1:8080/gp/launch.html?"+exampleQuery,
                search: "?"+exampleQuery
            }
    };
    assert.deepEqual(gpUtil.parseQueryString(mockWindow.location.search), exampleObject, "Example query="+exampleQuery);
    
    // Example usage
    assert.deepEqual(gpUtil.parseQueryString(mockWindow.location.search).hasOwnProperty('fieldA'), 
            true, 
            "hasOwnProperty on 'fieldA'"); 
    assert.deepEqual(gpUtil.parseQueryString(mockWindow.location.search).hasOwnProperty('field_not_in_request'), 
            false, 
            "hasOwnProperty on 'field_not_in_request'"); 
});
