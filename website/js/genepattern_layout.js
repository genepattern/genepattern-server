var mainLayout = null;
$(function()
{
    mainLayout = $('#content').layout({
        center__minHeight:	'100%'
        ,   center__minWidth:	'100%'
        ,   west__size:					352
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

    /*innerLayout = $('#content').layout({
        center__minHeight:	'100%'
        ,   center__minWidth:	'100%'
        ,   west__size:					352
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
    });*/

    /*mainLayout = $('body').layout({
        center__minHeight:	'100%'
        ,   center__minWidth:	'100%'
        ,   north__size:	88
        ,	north__spacing_open:		0
        , north__slidable:		false
        ,   center_showOverflowOnHover: true
        ,   west_showOverflowOnHover: true
        ,	north__minHeight:		88
        ,	north__height:		    88
        ,	south__spacing_open:		0
        ,  west__size:					352
        ,	west__spacing_closed:		20
        ,	west__togglerLength_closed:	167
        ,	west__togglerAlign_closed:	"top"
        ,	west__togglerContent_closed:"<BR>L<BR>E<BR>F<BR>T<BR><BR>P<BR>A<BR>N<BR>E<BR>L<BR>"
        ,  	west__togglerTip_closed:	"Open Left Panel"
           //,	west__sliderTip:			"Slide Open Menu"
        //,	west__slideTrigger_open:	"mouseover"
        ,	west__slidable:		        false
        ,	west__resizable:		    false
        ,	center__maskContents:		true // IMPORTANT - enable iframe masking
    });
    */
});