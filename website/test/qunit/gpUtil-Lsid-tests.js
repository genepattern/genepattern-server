var lsidNoVersion="urn:lsid:my-authority:my-namespace:100";

 /**
 * Test the GpUtil.Lsid class
 *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
 */
QUnit.test("gpUtil.Lsid", function( assert ) { 
    var lsid=new gpUtil.Lsid(lsidNoVersion+":0");
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
    version="1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version); 
        assert.deepEqual(lsid.getVersions(), [1], "getVersions(): version='"+version+"'");
        assert.equal(lsid.getPatchLevel(), 1, "getPatchLevel(): version='"+version+"'")
    version="0.0.1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version);
        assert.deepEqual(lsid.getVersions(), [0, 0, 1], "getVersions(): version='"+version+"'");
        assert.equal(lsid.getPatchLevel(), 3, "getPatchLevel(): version='"+version+"'")
    version="1.314.0.1"; lsid=new gpUtil.Lsid(lsidNoVersion+":"+version);
        assert.deepEqual(lsid.getVersions(), [1, 314, 0, 1], "getVersions(): version='"+version+"'");
        assert.equal(lsid.getPatchLevel(), 4, "getPatchLevel(): version='"+version+"'")
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
    
    // sort tests
    var unsorted = [ "1", "2", "3", "10", "1.1", "2.0.1", "2.10.1", "2.9" ];
    var alpha = unsorted.slice();
    alpha.sort();
    assert.deepEqual(alpha.sort(), [ "1", "1.1", "10", "2", "2.0.1", "2.10.1", "2.9", "3" ], "alpha sort");
    
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

function assertLsidMenu_New(lsidMenu, message) {
    var expected_options=[ 
        { value: "major", name: "New major version (v1)" },
        { value: "minor", name: "New minor version (v0.1)" },
        { value: "patch", name: "New patch version (v0.0.1)" },
    ];
    assertLsidMenu(lsidMenu, expected_options, 1, "major", true, message);
}

function assertLsidMenu_Latest(version, versions, expected_versionLevel, message) {
    var lsid=lsidNoVersion+":"+version;
    var lsidVersions=makeLsidVersions(versions);
    var lsidMenu=new gpUtil.LsidMenu(lsid, lsidVersions);
    var expected_options=[
        { value: "default", name: "" },
        { value: "major", name: "Next major version (X)" },
        { value: "minor", name: "Next minor version (X.Y)" },
        { value: "patch", name: "Next patch version (X.Y.Z)" }
    ];
    assertLsidMenu(lsidMenu, expected_options, expected_versionLevel, "default", true, message);
}

function assertLsidMenu_Default(version, versions, expected_versionLevel, message) {
    var lsid=lsidNoVersion+":"+version;
    var lsidVersions=makeLsidVersions(versions);
    var lsidMenu=new gpUtil.LsidMenu(lsid, lsidVersions);
    var expected_options=[
        { value: "default", name: "" }
    ];
    assertLsidMenu(lsidMenu, expected_options, expected_versionLevel, "default", false, message);
}

function assertLsidMenu(lsidMenu, expected_options, expected_versionLevel, expected_value, expected_enabled, message) {
    QUnit.assert.deepEqual(lsidMenu.getOptions(),   expected_options,     "LsidMenu(<"+message+">) getOptions()");
    QUnit.assert.equal(lsidMenu.getPatchLevel(),    expected_versionLevel, "LsidMenu(<"+message+">) getPatchLevel()");
    QUnit.assert.equal(lsidMenu.getSelectedValue(), expected_value,            "LsidMenu(<"+message+">) getSelectedValue()");
    QUnit.assert.equal(lsidMenu.isEnabled(),        expected_enabled,     "LsidMenu(<"+message+">) isEnabled()");
}

QUnit.test("gpUtil.LsidMenu", function(assert) {
    // tests for New module (variations on input lsid and lsidVersions)
    assertLsidMenu_New(new gpUtil.LsidMenu(), "No arg");
    assertLsidMenu_New(new gpUtil.LsidMenu(""), "''");
    assertLsidMenu_New(new gpUtil.LsidMenu(undefined), "undefined");
    assertLsidMenu_New(new gpUtil.LsidMenu(null), "null");

    // Edit, current lsid is latest major
    var versions=["1", "2", "2.1", "2.1.1", "2.1.2", "2.2", "2.3", "3"];
    var level=1;
    assertLsidMenu_Latest("3", versions, level, "latest major");
    // Edit, current lsid is latest minor
    level=2;
    assertLsidMenu_Latest("2.3", ["1", "2", "2.1", "2.1.1", "2.1.2", "2.2", "2.3"], level, "latest minor");

    // Edit, current lsid version is latest patch
    level=3;
    assertLsidMenu_Latest("2.1.2", ["1", "2", "2.1", "2.1.1", "2.1.2" ], level, "latest patch");
    
    // Edit, current lsid version is not-latest
    assertLsidMenu_Default("2", versions, 1, "lsid<latest: major");
    assertLsidMenu_Default("2.1", versions, 2, "lsid<latest: minor");
    assertLsidMenu_Default("2.1.1", versions, 3, "lsid<latest: patch");
});
