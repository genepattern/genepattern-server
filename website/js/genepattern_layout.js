var mainLayout = null;
$(function()
{
    mainLayout = $('#content').layout({
        center__minHeight:	'100%'
        ,   center__minWidth:	'100%'
        //,	resizerClass:			"resizer"
        ,	togglerClass:			"toggler"
        ,   west__size:					352
        ,	west__spacing_open:		0
        ,	west__spacing_closed:		23
        ,	west__togglerLength_closed:	20 //"100%"
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

    mainLayout.panes.center.css({'border' : 'none'});

    $("<span></span>").addClass("toggle-btn").prependTo( ".ui-layout-west");
    mainLayout.addToggleBtn( ".ui-layout-west .toggle-btn", "west");

    $(".ui-layout-resizer").click(function()
    {
        mainLayout.open("west");
    });

    //add hover of resize when toogle is hovered
    $(".toggler").hover(function()
    {
        $(".ui-layout-resizer").addClass("ui-layout-resizer-closed-hover");
    });
});