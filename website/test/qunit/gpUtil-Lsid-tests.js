var lsidNoVersion="urn:lsid:my-authority:my-namespace:100";

 /**
 * Test the GpUtil.Lsid class
 *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
 */
test("gpUtil.Lsid", function() { 
    var lsid=new gpUtil.Lsid(lsidNoVersion+":0");
    equal(lsid.authority, undefined, "lsid.authority should be private");
    equal(lsid.getAuthority(), "my-authority", "lsid.getAuthority()");
    equal(lsid.getNamespace(), "my-namespace", "lsid.getNamespace()");
    equal(lsid.getIdentifier(), "100", "lsid.getIdentifier()");
    equal(lsid.getVersion(), "0", "lsid.getVersion()");
    deepEqual(lsid.getVersions(), [ 0 ], "lsid.getVersions()");
    equal(lsid.getPatchLevel(), 1, "lsid.getPatchLevel()");
    
    var version=""; lsid=new gpUtil.Lsid(lsidNoVersion); 
        deepEqual(lsid.getVersions(), [], "getVersions(): version='"+version+"'");
        equal(lsid.getPatchLevel(), 1, "getPatchLevel(): version='"+version+"'")
    version="1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version); 
        deepEqual(lsid.getVersions(), [1], "getVersions(): version='"+version+"'");
        equal(lsid.getPatchLevel(), 1, "getPatchLevel(): version='"+version+"'")
    version="0.0.1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version);
        deepEqual(lsid.getVersions(), [0, 0, 1], "getVersions(): version='"+version+"'");
        equal(lsid.getPatchLevel(), 3, "getPatchLevel(): version='"+version+"'")
    version="1.314.0.1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version);
        deepEqual(lsid.getVersions(), [1, 314, 0, 1], "getVersions(): version='"+version+"'");
        equal(lsid.getPatchLevel(), 4, "getPatchLevel(): version='"+version+"'")

});

/**
 * Helper class for qunit testing, compare two versions
 * @param v0, the lsidVersion string
 * @param v1, the lsidVersion string
 * @param expected,  -1 is '<', 0 is '=', and 1 is '>'
 */
function compareTest(v0, v1, expected) {
    var lsid_a=new gpUtil.Lsid(lsidNoVersion+":"+v0);
    var lsid_b=new gpUtil.Lsid(lsidNoVersion+":"+v1);
    deepEqual(lsid_a.compareVersion(lsid_b), expected, "compareVersion(v"+v0+", v"+v1+")"); 
}

test("gpUtil.Lsid.compare", function() {
    // basic tests
    compareTest("0", "1", -1);
    compareTest("1", "0", 1);
    compareTest("1", "1", 0);
    compareTest("0", "0", 0);
    
    // level tests
    compareTest("1", "1.1", -1);
    compareTest("1.1", "1.2", -1);
    compareTest("2", "1.9", 1);
    compareTest("1.1", "1.1", 0);
    compareTest("1.2", "1.2", 0);
    compareTest("1.2.30", "1.2.30", 0);
    compareTest("1.2", "1.2.30", -1);
    compareTest("1.2.30", "1.2", 1);
    
    // sort tests
    var unsorted = [ "1", "2", "3", "10", "1.1", "2.0.1", "2.10.1", "2.9" ];
    var alpha = unsorted.slice();
    alpha.sort();
    deepEqual(alpha.sort(), [ "1", "1.1", "10", "2", "2.0.1", "2.10.1", "2.9", "3" ], "alpha sort");
    
    var unsorted_lsid = [];
    for(var i=0; i<unsorted.length; i++) {
        unsorted_lsid[i]=lsidNoVersion+":"+unsorted[i];
    }
    var lsidMenu=new gpUtil.LsidMenu(undefined, unsorted_lsid);
    
});

// helper to generate an array of lsid strings
function makeLsidVersions(versions) {
    var a=new Array();
    for(var i=0; i<versions.length; ++i) {
        a.push(lsidNoVersion+":"+versions[i]);
    }
    return a;
};

test("gpUtil.LsidMenu", function() {
    var message, version, lsid, lsidVersions, lsidMenu, expected_options;
    // New module
    expected_options=[ 
        { value: "major", name: "New major version (v1)" },
        { value: "minor", name: "New minor version (v0.1)" },
        { value: "patch", name: "New patch version (v0.0.1)" },
    ];
    lsidMenu=new gpUtil.LsidMenu();
    deepEqual(lsidMenu.getOptions(), expected_options, "LsidMenu().getOptions(), New module");
    equal(lsidMenu.getPatchLevel(), 1, "LsidMenu().getPatchLevel(), New module");
    equal(lsidMenu.getSelectedValue(), "major", "LsidMenu().getSelectedValue()");

    lsidMenu=new gpUtil.LsidMenu("");
    deepEqual(lsidMenu.getOptions(),   expected_options, "LsidMenu('').getOptions(), New module");
    equal(lsidMenu.getPatchLevel(),    1,                "LsidMenu('').getPatchLevel(), New module");
    equal(lsidMenu.getSelectedValue(), "major",          "LsidMenu('').getSelectedValue()");

    lsid=""; lsidVersions=undefined;
    lsidMenu=new gpUtil.LsidMenu(lsid, lsidVersions);
    deepEqual(lsidMenu.getOptions(),   expected_options, "LsidMenu('"+lsid+"', "+lsidVersions+").getOptions(), New module");
    equal(lsidMenu.getPatchLevel(),    1,                "LsidMenu('', "+lsidVersions+").getPatchLevel(), New module");
    equal(lsidMenu.getSelectedValue(), "major",          "LsidMenu('', "+lsidVersions+").getSelectedValue()");

    // Edit module from major
    expected_options=[
        { value: "default", name: "" },
        { value: "major", name: "Next major version (X)" },
        { value: "minor", name: "Next minor version (X.Y)" },
        { value: "patch", name: "Next patch version (X.Y.Z)" }
    ];
    message="Edit major version";
    version="1";
    lsid=lsidNoVersion+":"+version;
    lsidVersions=makeLsidVersions(["1"]);
    lsidMenu=new gpUtil.LsidMenu(lsid, lsidVersions);
    deepEqual(lsidMenu.getOptions(),   expected_options, "LsidMenu('"+lsid+"', "+lsidVersions+").getOptions(), "+message);
    equal(lsidMenu.getPatchLevel(),    1,                "LsidMenu('', "+lsidVersions+").getPatchLevel(), "+message);
    equal(lsidMenu.getSelectedValue(), "default",          "LsidMenu('', "+lsidVersions+").getSelectedValue(), "+message);
    
});
