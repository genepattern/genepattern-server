/**
 * Test the 'updateTheUrl' method which is called from the openModuleWindow function.
 */
QUnit.test("protocols.openModuleWindow", function(assert) {
    // example 'Open module with example data' href from the Protocols page
    var href="/gp/pages/index.jsf?lsid=PreprocessDataset"+
        "&input.filename=https://software.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_test.gct";
    
    assert.equal(
        // actual
        updateTheUrl(href),
        // expected
        "/gp/pages/index.jsf?lsid=PreprocessDataset"+
            "&input.filename="+ encodeURIComponent(
                    location.protocol+"//software.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_test.gct"), 
        "Default test, using locaton.protocol"
            );
    
});


/**
 * Tests for adjusting links to example data files; 
 * replace https with http as needed.
 * 
 */
QUnit.test("protocols.modifyQueryString", function(assert) {

    var searchPrefix="?lsid=PreprocessDataset&input.filename=";
    var filepath="//software.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_test.gct";
    var filepathEnc = encodeURIComponent( filepath );

    assert.equal(
            // actual
            modifyQueryString( "https:", searchPrefix+"https:"+filepath ),
            // expected
            searchPrefix+"https%3A" + filepathEnc,
            "Default test"
            );

    assert.equal( modifyQueryString( "http:", searchPrefix+"https:"+filepath ), 
            searchPrefix+"http%3A"+filepathEnc, 
            "Link to https file on http server (change from https to http)");

    assert.equal( modifyQueryString( "https:", searchPrefix+filepath ), 
            // expected
            searchPrefix+"https%3A"+filepathEnc, 
            "No protocol in link, change to https");

    assert.equal( modifyQueryString( "http:", searchPrefix+filepath ), 
            // expected
            searchPrefix+"http%3A"+filepathEnc, 
            "No protocol in link, change to http");
    
    // tests when the link is properly url encoded
    var dataPath = "//www.example.com/file.txt";
    assert.equal(
            // actual
            modifyQueryString( "https:", searchPrefix + encodeURIComponent("https:" + dataPath) ),
            // expected
            searchPrefix + encodeURIComponent("https:" + dataPath),
            "encoded dataUrl, link to https file on https server");

    assert.equal(
            // actual
            modifyQueryString( "http:", searchPrefix + encodeURIComponent("https:" + dataPath) ),
            // expected
            searchPrefix + encodeURIComponent("http:" + dataPath),
            "encoded dataUrl, link to http file on https server");

    assert.equal(
            // actual
            modifyQueryStringFromLocation(searchPrefix + encodeURIComponent("https:" + dataPath)),
            // expected
            searchPrefix + encodeURIComponent(window.location.protocol + dataPath),
            "encoded dataUrl, link to https file, use window.location.protocol");

    assert.equal(
            // actual
            modifyQueryStringFromLocation(searchPrefix + encodeURIComponent("http:" + dataPath)),
            // expected
            searchPrefix + encodeURIComponent(window.location.protocol + dataPath),
            "encoded dataUrl, link to http file, use window.location.protocol");

    assert.equal(
            // actual
            modifyQueryString( "https:", searchPrefix + encodeURIComponent(dataPath) ),
            // expected
            searchPrefix + encodeURIComponent("https:" + dataPath),
            "encoded dataUrl, link to '//www.example.com/file.txt' file on https server");
    
    assert.equal(
            // actual
            modifyQueryString( "http:", searchPrefix + encodeURIComponent(dataPath) ),
            // expected
            searchPrefix + encodeURIComponent("http:" + dataPath),
            "encoded dataUrl, link to '//www.example.com/file.txt' file on http server");
    
    assert.equal(
            // actual
            modifyQueryStringFromLocation(searchPrefix + encodeURIComponent(dataPath)),
            // expected
            searchPrefix + encodeURIComponent(window.location.protocol + dataPath),
            "encoded dataUrl, link to '//www.example.com/file.txt', use window.location.protocol");
});
