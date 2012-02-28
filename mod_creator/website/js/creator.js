var mainLayout, westlayout;

function addparameter()
{
    var paramDiv = $("<div class='parameter'>  \
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
                    Flag: \
               </td       \
           </tr>   \
           <tr>    \
               <td colspan='2'>  \
                   <input type='text' name='p_name' size='25'/> \
               </td>   \
               <td>    \
                   <textarea cols='30' name='p_description' rows='2'></textarea> \
               </td> \
               <td> \
                   <input type='text' name='p_defaultvalue' size='16'/> \
               </td>\
                <td>        \
                   <input type='text' name='p_flag' size='7'/> \
               </td>  \
            </tr>  \
            <tr>               \
                <td>  \
                    Optional: \
                </td> \
                <td>   \
                   Type: \
               </td> \
                <td colspan='2'>   \
                   Choice: \
               </td> \
            </tr>  \
            <tr>               \
                <td>  \
                    <input type='checkbox' name='p_optional' size='25'/>\
                </td> \
                <td> \
                   <select name='p_type'>\
                       <option value='text'>Text</option> \
                       <option value='Numeric'>Numeric</option>  \
                       <option value='Input File'>Input File</option>\
                       <option value='Password'>Password</option> \
                   </select>  \
               </td> \
                <td>  \
                    <input type='text' name='p_choice' size='25'/>\
                </td> \
            </tr>  \
        </table> \
    </div>");
    $('#parameters').append(paramDiv);

    $(".parameter").click(function() {
        if (!$(this).hasClass("ui-selected"))
        {
            $(this).addClass("ui-selected").siblings().removeClass("ui-selected");
        }
        /*else
        {
            $(this).removeClass("ui-selected");
        } */
    });
}

/* append a command line argument to the list */
function addtocommandline(text)
{
    var item = "<li class='commanditem'>" +
                 text +
                "</li>";
    var ctext = $('#commandlist').text();
    
    if(ctext.indexOf(text) < 0)
    {
        $('#commandlist').append($(item));
    }

    $(".commanditem").click(function() {
        if (!$(this).hasClass("ui-selected")) {
            $(this).addClass("ui-selected").siblings().removeClass("ui-selected");
        }
        else
        {
            $(this).removeClass("ui-selected");   
        }
    });
}

jQuery(document).ready(function() {
    //Used for editing default Module name - jQuery In Place Editor 

     $("#modtitle").editInPlace({
		callback: function(unused, enteredText) { return enteredText; },
		// url: './server.php',
         bg_over: "none",		 
         show_buttons: true
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
            minSize:				80	// ALL panes
        ,	center__paneSelector:	".west-center"
        ,	south__paneSelector:	".west-south"
        ,	south__size:			130
    });

    $(function() {
		$( "#parameters" ).sortable();
        $( "#commandlist" ).sortable();
		$( "#parameters" ).disableSelection();

	});


    $( "#addparam" )
        .button()
        .click(function() {
           addparameter();
        })
        .next()
            .button( {
                text: false,
                icons: {
                    primary: "ui-icon-triangle-1-s"
                }
            })
            .click(function() {
                var hidden = $("#param-bar").children("ul").is(":hidden");
				if (hidden) {
					$("#param-bar")
						.children("ul").toggle();
					}
            })
            .parent()
                .buttonset();    

    $( "#deleteparam" ).button().click(function() {
            $('.ui-selected').each(function() {
                $(this).remove();
            });
        });

    $("#param-bar ul li #addone").click(function()
    {
            $("#param-bar ul").hide();
            addparameter();
    });

    $("#param-bar ul li #addmultiple").click(function()
    {
            $("#param-bar ul").hide();
            alert("Not implemented");
    });

    $('#parameters').focusout(function()
    {
        $('.parameter').each(function() {
            var pname = $(this).find("input[name='p_name']").val();
            var pflag = $(this).find("input[name='p_flag']").val();

            var argument = pflag + " " + pname;
            addtocommandline(argument);
        });
    });

    /*$( "#parameters" ).selectable({
        filter: 'div',
        start: function (event, ui) {
            if ($(ui.selected).hasClass('ui-selected')) {
                $(ui.selected).removeClass('ui-selected');
                // do unselected stuff
            }
        }
    });*/
});