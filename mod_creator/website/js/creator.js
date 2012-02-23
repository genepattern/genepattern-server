var myLayout;
 
jQuery(document).ready(function() {
    //Used for editing default Module name - jQuery In Place Editor 

     $("#modtitle").editInPlace({
		callback: function(unused, enteredText) { return enteredText; },
		// url: './server.php',
		show_buttons: true
	});

    // this layout could be created with NO OPTIONS - but showing some here just as a sample...
    // myLayout = $('body').layout(); -- syntax with No Options

    myLayout = $('body').layout({

    //	enable showOverflow on west-pane so CSS popups will overlap north pane
        west__showOverflowOnHover: false

    //	reference only - these options are NOT required because 'true' is the default
    ,	closable:				true
    ,	resizable:				true	// when open, pane can be resized
    ,	slidable:				true	// when closed, pane can 'slide' open over other panes - closes on mouse-out

    //	some resizing/toggling settings
    ,	north__slidable:		false	// OVERRIDE the pane-default of 'slidable=true'
    ,	north__spacing_open:	0		// no resizer-bar when open (zero height)
    ,	north__spacing_closed:	20		// big resizer-bar when open (zero height)
    ,	south__resizable:		true	// OVERRIDE the pane-default of 'resizable=true'
    ,	south__spacing_closed:	20		// big resizer-bar when open (zero height)
    ,	south__togglerLength_closed: '100%'	// toggle-button is full-width of resizer-bar
    //	some pane-size settings
    ,	north__size:			45
    ,	south__size:			220
    ,	west__size:			    380
    ,	east__size:				300
    ,	center__minWidth:		100
    ,	useStateCookie:			true
    });
});
