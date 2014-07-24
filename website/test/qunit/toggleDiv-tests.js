// these tests depend on   <script src="//cdnjs.cloudflare.com/ajax/libs/jquery-cookie/1.4.1/jquery.cookie.js"></script>

QUnit.test("jqueryCookieTest", function( assert ) {
    assert.equal($.cookie("myCookie"), null, "myCookie, before it's set");
    assert.ok($.cookie("myCookie", "myValue"), "set myCookie");
    assert.equal($.cookie("myCookie"), "myValue", "myCookie, after it's set");
    assert.ok($.removeCookie("myCookie"), "remove myCookie");
    assert.equal($.cookie("myCookie"), null, "myCookie, after it's removed");
});

QUnit.test("hideShowDivTest", function( assert ) { 
    // for the test
    function clearCookies(gpJobNos) {
        for(var i=0; i<gpJobNos.length; i++) {
            $.removeCookie("show_statusDetailsDiv"+gpJobNos[i]);
        }
    }

    var $fixture = $( "#qunit-fixture" );
    var gpJobNos = [ 1, 2, 3 ];

    for(var i=0; i<gpJobNos.length; i++) {
        var id="statusDetailsDiv"+gpJobNos[i];
        $fixture.append("<div id=\""+id+"\" class=\"jobContent\" style=\"display: none;\" >Job details </div>");
        $fixture.append("<div id=\""+id+"Label\" >Show</div>");
        $fixture.append("<div> details</div>");
        $.removeCookie("show_"+id);
    }
    assert.equal( $( "div", $fixture ).length, 3*gpJobNos.length, "divs added successfully!" );
    
    //initially, all hidden
    for(var i=0; i<gpJobNos.length; i++) {
        var id="statusDetailsDiv"+gpJobNos[i];
        assert.equal( $("#"+id).is(":visible"), false, "isVisible, "+id);
        assert.ok( !($.cookie("show_"+id)), "initially, cookie is not set, "+id);
    }
    
    for(var i=0; i<gpJobNos.length; i++) {
        var id="statusDetailsDiv"+gpJobNos[i];
        gpUtil.initToggleDiv(id);
    }

    //after initToggleDiv, all hidden
    for(var i=0; i<gpJobNos.length; i++) {
        var id="statusDetailsDiv"+gpJobNos[i];
        assert.equal( $("#"+id).is(":visible"), false, "after initToggleDiv, isVisible, "+id);
        assert.ok( !($.cookie("show_"+id)), "after initToggleDiv, cookie is not set, "+id);
    }
    
    var id="statusDetailsDiv"+gpJobNos[0];
    gpUtil.toggleDiv(id, "Hide", "Show");
    assert.equal($("#"+id).is(":visible"), true, "after toggleDiv, isVisible, "+id );
    assert.equal($("#"+id+"Label").html(), "Hide", "Label after toggleDiv, "+id );
    
    // remove cookies
    clearCookies(gpJobNos);
    
    // set cookie
    $.cookie("show_statusDetailsDiv1", "true");
    gpUtil.initToggleDiv(id);
    assert.equal($("#"+id).is(":visible"), true, "after set cookie and initToggleDiv, isVisible, "+id );
    assert.equal($("#"+id+"Label").html(), "Hide", "Label after set cookie and initToggleDiv, "+id );
    
    // cleanup
    clearCookies(gpJobNos);
    
    // after initDiv, cookie should not be set

});

