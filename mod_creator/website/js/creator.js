var mainLayout;

function addparameter()
{
    var paramDiv = $("<div class='parameter'>  \
        <table>    \
           <tr>    \
               <td>   \
                  <b>Name:</b>  \
               </td>  \
               <td>  \
                   <input type='text' name='p_name' size='25'/> \
               </td>   \
               <td>   \
                   Description: \
               </td> \
               <td colspan='3'>  \
                   <textarea cols='30' name='p_description' rows='3'></textarea> \
               </td>    \
               <td>        \
                   Default Value: <input type='text' name='p_defaultvalue' size='16'/> \
               </td>  \
               <td>  \
                    Optional: <input type='checkbox' name='p_optional' size='25'/>\
               </td>       \
           </tr>   \
            <tr>               \
                <td>  \
                    Flag: \
                </td> \
                <td> \
                    <input type='text' name='p_flag' size='7'/> \
                </td> \
                <td> \
                     <b>Type:</b> \
                </td>     \
                <td>   \
                    <select name='p_type'>\
                       <option value='text'>Text</option> \
                       <option value='Numeric'>Numeric</option>  \
                       <option value='Input File'>Input File</option>\
                       <option value='Password'>Password</option> \
                       <option value='Choice'>Choice</option> \
                   </select>  \
               </td> \
            </tr>  \
        </table> \
    </div>");
    $('#parameters').append(paramDiv);

    paramDiv.click(function()
    {
        if (!$(this).hasClass("ui-selected"))
        {
            $(this).addClass("ui-selected").siblings().removeClass("ui-selected");
        }
        /*else
        {
            $(this).removeClass("ui-selected");
        } */
    });

    $("select[name='p_type']").change(function()
    {
        var value = $(this).val();
        if(value == "Choice")
        {
            var editChoiceLink = $("<td><a href=''> edit choice</a></td>");
            //var editChoiceLink = $("<a href=''> edit choice</a>");

            editChoiceLink.click(function()
            {
                $( "#editchoicedialog" ).dialog("open");
            });

            $(this).parent().parent().append(editChoiceLink);
            //$(this).parent().append(editChoiceLink);
        }
        else
        {
            if($(this).parent().next().text().contains("edit"))
            {
                $(this).parent().next().remove();
            }
        }
    });
}

function addtocommandline(flag, name, prevflag, prevname, delimiter)
{
    var ctext = $('#commandlist').text();

    var text = "";

    if (flag == "" && name == "" && prevflag ==undefined && prevname == undefined)
    {
        return;
    }

    //construct new parameter value
    if(name != "")
    {
        text = name;
    }

    if(flag != "")
    {
        text = flag + delimiter + text;
    }

    var item = $("<li class='commanditem'>" +
                 text +
                "</li>");

    //construct prev parameter value
    var  prevtext = "";
    if(prevname != "")
    {
        prevtext = prevname;
    }

    if(prevflag != "")
    {
        prevtext = prevflag + delimiter + prevtext;
    }
    
    //if no change in value do nothing
    if(prevtext == text)
    {
        return;
    }

    //look for child with matching old parameter value and replace with new parameter value
    var found = false;

    $('#commandlist').children().each(function()
    {
        if($(this).text() ==  prevtext)
        {
            if(text != "")
            {
                $(this).replaceWith(item);
            }
            else
            {
                $(this).remove();
            }
            found = true;
        }
    });

    // if old parameter value was not found then this must be a new parameter so insert it into list
    if(!found && text != "")
    {
        $('#commandlist').append(item);
            //add ability to select new parameter in list
            item.click(function() {
            if (!$(this).hasClass("ui-selected"))
            {
                $(this).addClass("ui-selected").siblings().removeClass("ui-selected");
            }
            else
            {
                $(this).removeClass("ui-selected");
            }
        });

    }   
}

jQuery(document).ready(function() {

    $(".heading").click(function()
    {
        $(this).next(".content").slideToggle(340);
    });

    $(".content").show();

    
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
    //some pane-size settings
    ,	north__size:			40
    ,	west__size:			    360
    ,	east__size:				300
    ,	center__minWidth:		100
    ,	useStateCookie:			true
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
                $("#param-bar")
                    .children("ul").toggle();
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
        $( "#addparamdialog" ).dialog("open");
    });

    $( "#addparamdialog" ).dialog({
        autoOpen: false,
        height: 185,
        width: 240,
        buttons: {
                "OK": function() {
                    var numparams = $("#multiparam").val();
                    for(i=0;i<numparams;i++)
                    {
                        addparameter();
                    }
                        $( this ).dialog( "close" );
                },
                "Cancel": function() {
                    $( this ).dialog( "close" );
                }
        },
        resizable: false
    });


    $('#parameters').focusout(function()
    {
        $('.parameter').each(function() {

            var pelement = $(this).find("input[name='p_name']");
            var felement = $(this).find("input[name='p_flag']");
            var pname_newval = pelement.val();
            var pflag_newval = felement.val();

            var pname_oldval = pelement.data('oldVal');
            var pflag_oldval = felement.data('oldVal');

            pelement.data('oldVal',  pname_newval );
            felement.data('oldVal',  pflag_newval );

            var delimiter = " ";

            addtocommandline(pflag_newval, pname_newval, pflag_oldval, pname_oldval,delimiter);
        });
    });

    $('#commandpreview').children().button().click(function()
    {
        var cmd_args = $('#commandlist').text();

        // $("#dialog").children().remove();
        // $("#dialog").append($('<p>'+cmd_args +'</p>'));

        // $( "#dialog" ).dialog();
        //$("#commandtextarea").children().remove();
        //$("#commandtextarea textarea").append(cmd_args);

       // var cmdtextarea = "<div id='commandtextarea'> \
       //     <textarea cols='40' rows='5' name='cmdtext'></textarea> \
       // </div>";

        //$(".west-center").append($(cmdtextarea));

        $("#commandtextarea textarea").attr("value", cmd_args);
        $("#commandtextarea").toggle();
    });

    $("#commandtextarea").hide();

$( "#editchoicedialog" ).dialog({
        autoOpen: false,
        height: 340,
        width: 280,
        buttons: {
                "OK": function() {
                    
                        $( this ).dialog( "close" );
                },
                "Cancel": function() {
                    $( this ).dialog( "close" );
                }
        },
        resizable: false
    });

});