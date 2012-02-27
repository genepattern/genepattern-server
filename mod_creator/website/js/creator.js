var mainLayout, westlayout;

function addparameter(element)
{
    var paramDiv = jQuery("<div class='parameter'>  \
    <table>    \
           <tr>    \
               <td colspan='2'>   \
                  Name: \
               </td>   \
               <td>   \
                   Description: \
               </td>    \
               <td>        \
                   Default Value: \
               </td>  \
               <td>  \
                    Flag \
               </td       \
           </tr>   \
           <tr>    \
               <td colspan='2'>  \
                   <input type='text'' name='parameter' size='25'/> \
               </td>   \
               <td>    \
                   <textarea cols='30' rows='2'></textarea> \
               </td> \
               <td> \
                   <input type='text' name='parameter' size='25'/> \
               </td>\
                <td>        \
                   <input type='text' name='parameter' size='10'/> \
               </td>  \
            </tr>  \
            <tr>               \
                <td>  \
                    Required: \
                </td> \
                <td>   \
                   Type: \
               </td> \
            </tr>  \
            <tr>               \
                <td>  \
                    <input type='checkbox' name='parameter' size='25'/>\
                </td> \
                <td> \
                   <select>\
                       <option value='text'>Text</option> \
                       <option value='Numeric'>Numeric</option>  \
                       <option value='Input File'>Input File</option>\
                       <option value='Password'>Password</option> \
                   </select>  \
               <td> \
            </tr>  \
        </table> \
    </div>");
    element.append(paramDiv);
}

jQuery(document).ready(function() {
    //Used for editing default Module name - jQuery In Place Editor 

     $("#modtitle").editInPlace({
		callback: function(unused, enteredText) { return enteredText; },
		// url: './server.php',
         bg_over: "#cff",		 
         show_buttons: true
	});

    $("#commandtext").editInPlace({
		callback: function(unused, enteredText) { return enteredText; },
		// url: "./server.php",
		bg_over: "#cff",
		field_type: "textarea",
		textarea_rows: "9",
		textarea_cols: "36",
		saving_image: "./styles/images/ajax-loader.gif"
	});

    // this layout could be created with NO OPTIONS - but showing some here just as a sample...
    // myLayout = $('body').layout(); -- syntax with No Options

    mainLayout = $('body').layout({

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
    ,	north__size:			40
    ,	south__size:			220
    ,	west__size:			    360
    ,	east__size:				300
    ,	center__minWidth:		100
    ,	useStateCookie:			true
    });

    westLayout = $('div.ui-layout-west').layout({
            minSize:				70	// ALL panes
        ,	center__paneSelector:	".west-center"
        ,	south__paneSelector:	".west-south"
        ,	south__size:			200
    });

    $(function() {
		$( "#parameters" ).sortable();
		$( "#parameters" ).disableSelection();
	});


    $( "#addparam" )
        .button()
        .click(function() {
           addparameter($('#parameters'));
        })
        .next()
            .button( {
                text: false,
                icons: {
                    primary: "ui-icon-triangle-1-s"
                }
            })
            .click(function() {
                alert( "not implemented" );
            })
            .parent()
                .buttonset();
    
    $( "#deleteparam" ).button().click(function() {
            $('.ui-selected').each(function() {
                $(this).remove();
            });
        });

    $( "#parameters" ).selectable();
});
