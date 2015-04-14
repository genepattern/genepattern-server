var mainLayout = null;
$(function()
{
    mainLayout = $('#content').layout({
        west__size:					352
        ,	west__spacing_closed:		20
        ,	west__togglerLength_closed:	167
        ,	west__togglerAlign_closed:	"top"
        ,	west__togglerContent_closed:"<BR>L<BR>E<BR>F<BR>T<BR><BR>P<BR>A<BR>N<BR>E<BR>L<BR>"
        ,	west__togglerTip_closed:	"Open Left Panel"
        //,	west__sliderTip:			"Slide Open Menu"
        //,	west__slideTrigger_open:	"mouseover"
        ,	west__slidable:		        false
        ,	west__resizable:		    false
        ,	center__maskContents:		true // IMPORTANT - enable iframe masking
    });
});