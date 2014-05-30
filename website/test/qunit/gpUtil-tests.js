test("gpUtil", function() {

    // test 'gpUtil.formatTimezone'
    equal(gpUtil.formatTimezone(), gpUtil.formatTimezone(new Date().getTimezoneOffset()), "formatTimezone, local timezone");
    equal(gpUtil.formatTimezone(240), "-04:00", "formatTimezone, GMT-4");
    equal(gpUtil.formatTimezone(-60), "+01:00", "formatTimezone, GMT+1");
    equal(gpUtil.formatTimezone(340), "-05:40", "formatTimezone, fraction of an hour");
    equal(gpUtil.formatTimezone(-660), "+11:00", "formatTimezone, GMT+11");
    equal(gpUtil.formatTimezone(0), "Z", "formatTimezone, GMT");
    
    // format am/pm
    equal(gpUtil.formatTimeOfDay(), "", "format am/pm, with null arg");
    equal(gpUtil.formatTimeOfDay(new Date("2007-06-29T15:55:10.237-04:00")), "3:55 pm", "format am/pm, pm");
    equal(gpUtil.formatTimeOfDay(new Date("2007-06-29T11:55:10.237-04:00")), "11:55 am", "format am/pm, am");

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
    equal(d.getHours(), expectedHours, message+" check hours");
    equal(d.getMinutes()+d.getTimezoneOffset()%60, minutes, message+" check minutes");
}

test("gpUtil.formatDate", function() {
    // ISO 8601 Date format examples
    // make sure tests will pass in different timezones
    var localOffset=gpUtil.formatTimezone();
    equal( gpUtil.formatDate(new Date("2007-06-29T15:55:10.237"+localOffset)), "Jun 29, 3:55 pm", "formatDate, with milliseconds");
    equal( gpUtil.formatDate(new Date("2007-06-29T15:55:10"+localOffset)), "Jun 29, 3:55 pm", "formatDate, without milliseconds");
    
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
