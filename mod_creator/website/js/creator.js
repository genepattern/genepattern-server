var mainLayout;

function addparameter()
{
    var paramDiv = $("<div class='parameter'>  \
        <table>    \
           <tr>    \
               <td>   \
                  <b>Name:</b>  \
               </td>  \
               <td colspan='2'>  \
                   <input type='text' name='p_name' size='28'/> \
               </td>   \
               <td>   \
                   Description: \
               </td> \
               <td colspan='9'>  \
                   <textarea cols='50' name='p_description' rows='3'></textarea> \
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
                    <input type='checkbox' name='p_flag' size='7' checked='true'> insert space after flag</input> \
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
            if($(this).parent().next().next().text().indexOf("edit") == -1)
            {
                var editChoiceList = $("<td><input type='text' name='choicelist' size='30' readonly='readonly'/></td>");
                var editChoiceLink = $("<td> <a href=''>edit choice</a></td>");

                editChoiceLink.click(function()
                {
                    $( "#editchoicedialog" ).dialog("open");
                    $(this).data("editing", true);
                });

                $(this).parent().parent().append(editChoiceList);
                $(this).parent().parent().append(editChoiceLink);
            }
        }
        else
        {
            if($(this).parent().next().next().text().indexOf("edit") != -1)
            {
                $(this).parent().next().remove();
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

        /*var visible = $(this).next(".content").is(":visible");
        alert($(this).next(".content").html());
        if($(this).next(".content").is(":visible").length > 0)
        {
            alert("visible");

           var image = $("<img src='css/images/1330981073_1downarrow1.png' alt='some_text' width='11' height='11'/>");
           $(this).prepend(image);
        }
        else
        {
            alert("hidden");
            var image = $("<img src='css/images/1330981073_1collapsearrow1.png' alt='some_text' width='11' height='11'/>");
           $(this).prepend(image);
        }  */
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
        height: 500,
        width: 360,
        buttons: {
                "OK": function() {
                    var choicelist = "";
                    $($(this)).find("tr").each(function()
                    {
                        var dvalue = $(this).find("td input[name='choicen']").val();
                        var value = $(this).find("td input[name='choicev']").val();


                        if(dvalue == undefined && value == undefined)
                        {
                            return;
                        }

                        if(choicelist != "")
                        {
                            choicelist += ";";
                        }
                        choicelist += dvalue + "=" + value;
                    });

                    $("input[name='choicelist']").each(function()
                    {
                        var editedParameter  =  $(this).parent().next().data('editing');
                        if(editedParameter)
                        {
                            $(this).val(choicelist);
                            $(this).parent().next().data('editing', false);
                        }
                    });

                    $( this ).dialog( "close" );
                },
                "Cancel": function() {
                    var choiceListElement = $("input[name='choicelist']");                    
                    choiceListElement.parent().next().data('editing', false);
                    $( this ).dialog( "close" );
                }
        },
        resizable: true
    });

    $( "#choiceadd" )
        .button()
        .click(function() {
            var choicerow = "<tr><td> <input type='text' name='choicen' size='15'/></td>" +
                            "<td> <input type='text' name='choicev' size='15'/></td></tr>";
            $(this).parent().next("table").append($(choicerow));     
     });

    $( "#choicedelete" )
        .button()
        .click(function() {
           alert("delete");
    });

    $(window).resize(function()
    {
        $("#parameters").resize();    
    });
});