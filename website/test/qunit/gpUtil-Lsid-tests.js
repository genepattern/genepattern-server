var lsidNoVersion="urn:lsid:my-authority:my-namespace:100";

//helper to generate an array of lsid strings
function makeLsidVersions(versions) {
    var a=new Array();
    for(var i=0; i<versions.length; ++i) {
        a.push(lsidNoVersion+":"+versions[i]);
    }
    return a;
};

 /**
 * Test the GpUtil.Lsid class
 *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
 */
QUnit.test("gpUtil.Lsid", function( assert ) { 
    var lsid=new gpUtil.Lsid(lsidNoVersion+":0");
    assert.equal(lsid.getLsid(), lsidNoVersion+":0", "lsid.getLsid()");
    assert.equal(lsid.getLsidNoVersion(), lsidNoVersion, "lsid.getLsidNoVersion()");
    assert.equal(lsid.authority, undefined, "lsid.authority should be private");
    assert.equal(lsid.getAuthority(), "my-authority", "lsid.getAuthority()");
    assert.equal(lsid.getNamespace(), "my-namespace", "lsid.getNamespace()");
    assert.equal(lsid.getIdentifier(), "100", "lsid.getIdentifier()");
    assert.equal(lsid.getVersion(), "0", "lsid.getVersion()");
    assert.deepEqual(lsid.getVersions(), [ 0 ], "lsid.getVersions()");
    assert.equal(lsid.getPatchLevel(), 1, "lsid.getPatchLevel()");
    
    var version=""; lsid=new gpUtil.Lsid(lsidNoVersion); 
        assert.deepEqual(lsid.getVersions(), [], "getVersions(): version='"+version+"'");
        assert.equal(lsid.getPatchLevel(), 1, "getPatchLevel(): version='"+version+"'")
        assert.equal(lsid.getLsidNoVersion(), lsidNoVersion, "lsid.getLsidNoVersion(): version='"+version+"'");
    version="1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version); 
        assert.deepEqual(lsid.getVersions(), [1], "getVersions(): version='"+version+"'");
        assert.equal(lsid.getPatchLevel(), 1, "getPatchLevel(): version='"+version+"'")
        assert.equal(lsid.getLsidNoVersion(), lsidNoVersion, "lsid.getLsidNoVersion(): version='"+version+"'");
    version="0.0.1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version);
        assert.deepEqual(lsid.getVersions(), [0, 0, 1], "getVersions(): version='"+version+"'");
        assert.equal(lsid.getPatchLevel(), 3, "getPatchLevel(): version='"+version+"'")
        assert.equal(lsid.getLsidNoVersion(), lsidNoVersion, "lsid.getLsidNoVersion(): version='"+version+"'");
    version="1.314.0.1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version);
        assert.deepEqual(lsid.getVersions(), [1, 314, 0, 1], "getVersions(): version='"+version+"'");
        assert.equal(lsid.getPatchLevel(), 4, "getPatchLevel(): version='"+version+"'");
        assert.equal(lsid.getLsidNoVersion(), lsidNoVersion, "lsid.getLsidNoVersion(): version='"+version+"'"); 
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
    QUnit.assert.deepEqual(lsid_a.compareVersion(lsid_b), expected, "compareVersion(v"+v0+", v"+v1+")"); 
}

QUnit.test("gpUtil.Lsid.compare", function(assert) {
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
});

QUnit.test("gpUtil.LsidMenu.getAllVersions", function(assert) {
    // sort tests
    var unsorted = [ "1", "2", "3", "10", "1.1", "2.0.1", "2.10.1", "2.9" ];
    var alpha = unsorted.slice();
    alpha.sort();
    assert.deepEqual(alpha.sort(), [ "1", "1.1", "10", "2", "2.0.1", "2.10.1", "2.9", "3" ], "alpha sort");

    var unsortedLsidVersions=makeLsidVersions(unsorted);
    var lsidMenu=new gpUtil.LsidMenu(undefined, unsortedLsidVersions); 
    var sortedLsidVersions=makeLsidVersions(alpha);
    var sortedLsidObjs=new Array();
    for(var i=0; i<sortedLsidVersions.length; i++) {
        sortedLsidObjs.push( new gpUtil.Lsid(sortedLsidVersions[i]) );
    }
    assert.deepEqual(lsidMenu.getAllVersions(), sortedLsidObjs, "LsidMenu.getAllVersions (sorted)");
});

QUnit.test("gpUtil.LsidMenu.getAllVersionsOptions", function(assert) {
    // create menu, no current lsid
    var lsid=undefined;
    var versions=undefined;
    var lsidVersions=undefined;

    // undefined args
    var lsidMenu=new gpUtil.LsidMenu(undefined, undefined);
    assert.deepEqual(lsidMenu.getAllVersionsOptions(), [], "from undefined input");
    
    // undefined lsid, empty lsidVersions
    lsidMenu=new gpUtil.LsidMenu(undefined, []);
    assert.deepEqual(lsidMenu.getAllVersionsOptions(), [], "from empty lsidVersions array");
    
    var versions=["2", "1", "1.0.1" ];
    lsidVersions=makeLsidVersions(versions);
    lsidMenu=new gpUtil.LsidMenu(lsidNoVersion+":4", lsidVersions);
    assert.deepEqual(lsidMenu.getAllVersionsOptions(), 
        [ {"name": "1",     "value": lsidNoVersion+":1"},
          {"name": "1.0.1", "value": lsidNoVersion+":1.0.1"},
          {"name": "2",     "value": lsidNoVersion+":2"} ], 
        "from versions array");
});

function assertLsidMenu_New(lsidMenu, message) {
    var expected_options=[ 
        { value: "major", name: "New major version (v1)" },
        { value: "minor", name: "New minor version (v0.1)" },
        { value: "patch", name: "New patch version (v0.0.1)" },
    ];
    assertLsidMenuNextVersion(lsidMenu, expected_options, "major", true, message);
}

function assertLsidMenu_Latest(version, versions, message) {
    var lsid=lsidNoVersion+":"+version;
    var lsidVersions=makeLsidVersions(versions);
    var lsidMenu=new gpUtil.LsidMenu(lsid, lsidVersions);
    var expected_options=[
        { value: "default", name: "" },
        { value: "major", name: "Next major version (X)" },
        { value: "minor", name: "Next minor version (X.Y)" },
        { value: "patch", name: "Next patch version (X.Y.Z)" }
    ];
    assertLsidMenuNextVersion(lsidMenu, expected_options, "default", true, message);
}

function assertLsidMenu_Default(version, versions, message) {
    var lsid=lsidNoVersion+":"+version;
    var lsidVersions=makeLsidVersions(versions);
    var lsidMenu=new gpUtil.LsidMenu(lsid, lsidVersions);
    var expected_options=[
        { value: "default", name: "" }
    ];
    assertLsidMenuNextVersion(lsidMenu, expected_options, "default", false, message);
}

function assertLsidMenuNextVersion(lsidMenu, expected_options, expected_value, expected_enabled, message) {
    QUnit.assert.deepEqual(lsidMenu.getNextVersionOptions(), expected_options, "LsidMenu(<"+message+">) getNextVersionOptions()");
    QUnit.assert.equal(lsidMenu.getNextVersionValue(),       expected_value,   "LsidMenu(<"+message+">) getNextVersionValue()");
    QUnit.assert.equal(lsidMenu.isNextVersionEnabled(),      expected_enabled, "LsidMenu(<"+message+">) isNextVersionEnabled()");
}

QUnit.test("gpUtil.LsidMenu_nextVersionMenu", function(assert) {
    // tests for New module (variations on input lsid and lsidVersions)
    assertLsidMenu_New(new gpUtil.LsidMenu(), "No arg");
    assertLsidMenu_New(new gpUtil.LsidMenu(""), "''");
    assertLsidMenu_New(new gpUtil.LsidMenu(undefined), "undefined");
    assertLsidMenu_New(new gpUtil.LsidMenu(null), "null");

    // Edit, current lsid is latest major
    var versions=["1", "2", "2.1", "2.1.1", "2.1.2", "2.2", "2.3", "3"];
    assertLsidMenu_Latest("3", versions, "latest major");
    // Edit, current lsid is latest minor
    assertLsidMenu_Latest("2.3", ["1", "2", "2.1", "2.1.1", "2.1.2", "2.2", "2.3"], "latest minor");

    // Edit, current lsid version is latest patch
    assertLsidMenu_Latest("2.1.2", ["1", "2", "2.1", "2.1.1", "2.1.2" ], "latest patch");
    
    // Edit, current lsid version is not-latest
    assertLsidMenu_Default("2",     versions, "lsid<latest: major");
    assertLsidMenu_Default("2.1",   versions, "lsid<latest: minor");
    assertLsidMenu_Default("2.1.1", versions, "lsid<latest: patch");
});

