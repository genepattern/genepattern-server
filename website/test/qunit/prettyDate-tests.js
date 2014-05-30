/**
 * qUnit tests for parsing ISO 8601 Date formats.
 */

test("date formatting basics", function() {
    var now = "2008/01/28 22:25:00";

    equal(prettyDate(now, "2008/01/28 22:24:30"), "just now");
    equal(prettyDate(now, "2008/01/28 22:23:30"), "1 minute ago");
    equal(prettyDate(now, "2008/01/28 21:23:30"), "1 hour ago");
    equal(prettyDate(now, "2008/01/27 22:23:30"), "Yesterday");
    equal(prettyDate(now, "2008/01/26 22:23:30"), "2 days ago");
    equal(prettyDate(now, "2007/01/26 22:23:30"), undefined);
});
