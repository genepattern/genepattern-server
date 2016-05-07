var lsidNoVersion="urn:lsid:my-authority:my-namespace:100";

 /**
 * Test the GpUtil.Lsid class
 *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
 */
test("gpUtil.Lsid", function() { 
    var lsid=new gpUtil.Lsid(lsidNoVersion+":0");
    //alert(JSON.stringify(lsid));
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
//    for(var i=0; i<lsidMenu.all.length; i++) {
//        alert("lsidMenu.all.version["+i+"]"+lsidMenu.all[i].getVersion());
//    }
    
});

// helper to generate an array of lsid strings
function makeLsidVersions(versions) {
    var a=[];
    for(var i=0; i<versions.length; ++i) {
        a=lsidNoVersion+":"+versions[i];
    }
    return a;
};

// initial implementation has a static drop-down menu
/*
 * <option value="" selected="selected">default</option>
 * <option value="major">major (X)</option>
 * <option value="minor">minor (X.Y)</option>
 * <option value="patch">patch (X.Y.Z)</option>
 */
test("gpUtil.LsidMenu-static", function() {
    var expected_options=[ 
                          { value: "", name: "default" },
                          { value: "major", name: "major (X)" },
                          { value: "minor", name: "minor (X.Y)" },
                          { value: "patch", name: "patch (X.Y.Z)" },
                         ];
    var lsidMenu=new gpUtil.LsidMenu();
    deepEqual(lsidMenu.getOptions(), expected_options, "new LsidMenu().options");
    equal(lsidMenu.getPatchLevel(), 1, "new LsidMenu().patchLevel");

});

//test("gpUtil.LsidMenu", function() {
//    // New module, lsid is undefined
//    var expected_options=[ 
//                     { value: "major", name: "New major version (v1)" },
//                     { value: "minor", name: "New minor version (v0.1)" },
//                     { value: "patch", name: "New patch version (v0.0.1)" },
//                    ];
//    var lsidMenu=new gpUtil.LsidMenu();
//    deepEqual(lsidMenu.getOptions(), expected_options, "options for New module");
//    equal(lsidMenu.getPatchLevel(), 1, "patchLevel for New module");
//    
//    var opts=lsidMenu.getOptions();
//    for(var i=0; i<opts.length; i++) {
//        opts[i].value;
//        opts[i].name;
//        alert("<option value=\""+opts[i].value+"\">"+opts[i].name+"</option>");
//    }
//    
//    // New module, empty args
//    lsidMenu=new gpUtil.LsidMenu("", []);
//    deepEqual(lsidMenu.getOptions(), expected_options, "options for New module");
//    equal(lsidMenu.getPatchLevel(), 1, "patchLevel for New module");
//    
//    // editing latest major version
//    var lsidVersions=makeLsidVersions(["1"]);
//    var currentLsid=lsidNoVersion+":1";
//    expected_options=[ 
//                          { value: "major", name: "Next major version (v2)" },
//                          { value: "minor", name: "New minor version (v1.1)" },
//                          { value: "patch", name: "New patch version (v1.0.1)" },
//                         ];
//
//    lsidMenu=new gpUtil.LsidMenu(currentLsid, lsidVersions);
//    deepEqual(lsidMenu.getOptions(), expected_options, "options for New module");
//    equal(lsidMenu.getPatchLevel(), 1, "patchLevel for New module");
//    
//    
//});
