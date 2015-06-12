var mainLayout = null;
$(function()
{
    mainLayout = $('#content').layout({
        center__minHeight:	'100%'
        ,   center__minWidth:	'100%'
        ,	togglerClass:			"toggler"
        ,   west__size:					352
        ,	west__spacing_open:		0
        ,	west__spacing_closed:		22
        ,	west__togglerLength_closed:	"100%"
        ,	west__togglerAlign_closed:	"top"
        //,	west__togglerContent_closed:"<BR>L<BR>E<BR>F<BR>T<BR><BR>P<BR>A<BR>N<BR>E<BR>L<BR>"
        ,	west__togglerTip_open:	    "Close Left Panel"
        ,	west__togglerTip_closed:	"Open Left Panel"
        //,	west__sliderTip:			"Slide Open Menu"
        //,	west__slideTrigger_open:	"mouseover"
        ,	west__slidable:		        false
        ,	west__resizable:		    false
        ,	center__maskContents:		true // IMPORTANT - enable iframe masking
    });


    //var westSelector = "body > .ui-layout-west"; // outer-west pane
    $("<span></span>").addClass("toggle-btn").prependTo( ".ui-layout-west"); //westSelector );
    mainLayout.addToggleBtn( ".ui-layout-west .toggle-btn", "west");

});