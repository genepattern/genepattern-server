var mainLayout;
var uploadedfiles= new Array();

function addparameter()
{
    var paramDiv = $("<div class='parameter'>  \
        <table>    \
           <tr>    \
               <td>   \
                  Name*:  \
               </td>  \
               <td colspan='2'>  \
                   <input type='text' name='p_name' size='28'/> \
               </td>   \
               <td>   \
                   Description: \
               </td> \
               <td colspan='3'>  \
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
                    <input type='checkbox' name='p_flagspace' size='7' checked='true'> insert space after flag</input> \
                </td> \
                <td> \
                     Type*: \
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
               <td>   \
                    <input type='checkbox' name='p_invisible' size='7'> visible only on command line</input> \
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
                var editChoiceList = $("<td><input type='text' name='choicelist' size='40'/></td>");
                var editChoiceLink = $("<td> <a href=''>edit choice</a></td>");

                editChoiceLink.click(function()
                {
                    var choices = $(this).parent().prev().children().val();

                    $( "#editchoicedialog" ).dialog({
                        autoOpen: true,
                        height: 500,
                        width: 360,
                        create: function()
                        {
                            $(this).find("tr td").remove();

                            var result = choices.split(';');

                            if(result == null || result.length < 0)
                            {
                                var trowdef = "<tr><td> <input type='text' name='choicen' size='15'/> </td>" +
                                     "<td> <input type='text' name='choicev' size='15'/> </td>" +
                                     "<td> <button> X </button></td></tr>";

                                $(this).find("table").append(trowdef);
                                $(this).find("table").append(trowdef);
                                return;
                            }
                            
                            for(i=0;i<result.length;i++)
                            {
                                var rowdata = result[i].split("=");

                                if(rowdata.length > 1)
                                {
                                    var trow = $("<tr><td> <input type='text' name='choicen' size='15' value='"
                                                + rowdata[0] +"'/> </td>" +
                                            "<td> <input type='text' name='choicev' size='15'value='" 
                                             + rowdata[1] + "'/> </td>" +
                                            "<td> <button> X </button></td></tr>");

                                    trow.find("button").button().click(function()
                                    {
                                        $(this).parent().parent().remove();
                                    });

                                    $(this).find("table").append(trow);
                                }
                            }
                        },
                        buttons: {
                                "OK": function() {
                                    var choicelist = "";
                                    $(this).find("tr").each(function()
                                    {
                                        var dvalue = $(this).find("td input[name='choicen']").val();
                                        var value = $(this).find("td input[name='choicev']").val();


                                        if((dvalue == undefined && value == undefined)
                                           || (dvalue == "" && value==""))
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

                                    $( this ).dialog( "destroy" );
                                },
                                "Cancel": function() {
                                    var choiceListElement = $("input[name='choicelist']");
                                    choiceListElement.parent().next().data('editing', false);
                                    $( this ).dialog( "destroy" );
                                }
                        },
                        resizable: true
                    });


                    $(this).parent().data("editing", true);
                });

                $(this).parent().after(editChoiceLink);                
                $(this).parent().after(editChoiceList);
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

function addtocommandline(flag, name, delimiter, prevflag, prevname, prevdelimiter)
{
    var text = "";

    if (flag == "" && name == "" && prevflag ==undefined && prevname == undefined)
    {
        return;
    }

    //construct new parameter value
    if(name != "")
    {
        text = "&lt;" + name + "&gt;";
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
        prevtext = "&lt;" + prevname + "&gt;";
    }

    if(prevflag != "")
    {
        prevtext = prevflag + prevdelimiter + prevtext;
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

        //decode the prevtext string first and compare it 
        if($(this).text() ==  $('<div/>').html(prevtext).text())
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

    //update the cmd line text area
    //var cmd_args = $('#commandlist').text();
    var cmd_args="";
    $('#commandlist').children('li').each(function()
    {
        var val = $(this).text();
        if(cmd_args != "")
        {
            //add space between arguments;
            cmd_args += " ";
        }
        cmd_args += val;
    });

    var cmdline = cmd_args;
    var cmdtype = $("#commandtextarea textarea").data("type");
    if(cmdtype != undefined)
        cmdline = cmdtype + " " + cmdline; 
    $("#commandtextarea textarea").attr("value", cmdline);
}

//update the specific parameter div
function updateparameter(parameter)
{
    var pelement = parameter.find("input[name='p_name']");
    var felement = parameter.find("input[name='p_flag']");
    var pname_newval = pelement.val();
    var pflag_newval = felement.val();

    var pname_oldval = pelement.data('oldVal');
    var pflag_oldval = felement.data('oldVal');

    pelement.data('oldVal',  pname_newval );
    felement.data('oldVal',  pflag_newval );


    var delement = parameter.find("input[name='p_flagspace']");
    var prevdelimiter = delement.data('oldVal');

    var delimiter = "";

    var addspace = delement.is(':checked');
    if(addspace)
    {
       delimiter = " ";
    }

    delement.data('oldVal',  delimiter);

    addtocommandline(pflag_newval, pname_newval, delimiter, pflag_oldval, pname_oldval, prevdelimiter);

}

function updatemodulecategories()
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/categories",
        success: function(response) {
            var error = response["ERROR"];
            if (error !== undefined) {
                alert(error);
            }
            else {
                var categories = response["categories"];
                categories = categories.substring(1, categories.length-1);

                var result = categories.split(", ");
                var mcat = $("select[name='category']");

                for(i=0;i < result.length;i++)
                {
                    mcat.append($("<option>" + result[i] + "</option>"));                        
                }
            }
        },
        dataType: "json"
    });
}


jQuery(document).ready(function() {



    $(".heading").click(function()
    {        
        $(this).next(".content").slideToggle(340);

        var visible = $(this).next(".content").data("visible");
       /* if(visible == undefined)
        {
            $(this).next(".content").data("visible", false);
        }
        else if (visible == true)
        {
            $(this).next(".content").data("visible", false);
            var image = $("<img src='css/images/1330981073_1collapsearrow1.png' alt='some_text' width='11' height='11'/>");
            $(this).prepend(image);                   
        }
        else
        {
            $(this).next(".content").data("visible", true);
            var image = $("<img src='css/images/1330981073_1downarrow1.png' alt='some_text' width='11' height='11'/>");
            $(this).prepend(image);
        } */

        //alert($(this).next(".content").html());


        /*if($(this).next(".content").is(":visible").length > 0)
        /*if($(this).next(".content").is(":visible").length > 0)
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
    ,	north__size:			45
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
        $('.ui-selected').each(function()
        {
            var pelement = $(this).find("input[name='p_name']");
            var felement = $(this).find("input[name='p_flag']");
            pelement.val("");
            felement.val("");
            
            updateparameter($(this));

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

    $("input[name='p_flagspace'], input[name='p_name'], input[name='p_flag']").live("change", function()
    {
        var parameterParent = $(this).parents(".parameter");
        updateparameter(parameterParent);
    });


    $('#commandpreview').children().button().click(function()
    {
        var cmd_args = $('#commandlist').text();

        $("#commandtextarea textarea").attr("value", cmd_args);
        $("#commandtextarea").toggle();
    });

    $( "#choiceadd" )
        .button()
        .click(function() {
            var choicerow = $("<tr><td> <input type='text' name='choicen' size='15'/></td>" +
                            "<td> <input type='text' name='choicev' size='15'/></td>" +
                            "<td> <button> X </button></td></tr>");
            choicerow.find("button").button().click(function()
            {
                $(this).parent().parent().remove();
            });

            $(this).parent().next("table").append(choicerow);
    });

    $( "#addmodcategorydialog" ).dialog({
            autoOpen: false,
            height: 190,
            width: 340,
            buttons: {
                    "OK": function() {
                        var category = $("#newcategoryname").val();
                        var newcategory = $("<option>" +category + "</option>");
                        $("select[name='category']").append(newcategory);
                        $( this ).dialog( "close" );
                    },
                    "Cancel": function() {
                        $( this ).dialog( "close" );
                    }
            },
            resizable: false
     });

    $("#addcategory").click(function()
    {
       $( "#addmodcategorydialog" ).dialog("open");           
    });


    $( "#addfileformatdialog" ).dialog({
            autoOpen: false,
            height: 220,
            width: 440,
            buttons: {
                    "OK": function() {
                        var fileformat = $("#newfileformat").val();
                        var newfileformat = $("<option>" + fileformat + "</option>");
                        $("select[name='fileformat']").append(newfileformat);
                        $( this ).dialog( "close" );
                    },
                    "Cancel": function() {
                        $( this ).dialog( "close" );
                    }
            },
            resizable: false
    });


    $("#addfileformat").click(function()
    {
       $( "#addfileformatdialog" ).dialog("open");           
    });

    $("#viewparameter").button().click(function()
    {
         $( "#clistdialog" ).dialog("open");            
    });

    $( "#clistdialog" ).dialog({
        autoOpen: false,
        height: 440,
        width: 340,
        buttons: {
                "OK": function() {
                    $( this ).dialog( "close" );
                }
        },
        resizable: true
    });

    $('#savebtn').button().click(function()
    {
        /*for(i=0;i < uploadedfiles.length;i++)
        {
            var fileuploadform = $('<form action="/gp/ModuleCreator/upload" method="post" ' +
                                 'enctype="multipart/form-data" class="fileuploadform"> </form>');

            fileuploadform.append(uploadedfiles[i]);

            //alert("file form html: " + fileuploadform.html());

            //uploadfiles(fileuploadform);
            fileuploadform.iframePostForm
            ({
                json : false,
                post : function ()
                {
                    if ($(this).find('input[type=file]').val().length)
                    {
                        alert("uploading file: " + $(this).find('input[type=file]').val());
                    }
                    else
                    {
                        //cancel request
                        //return false;
                        alert("canceled");
                    }
                },
                complete : function (response)
                {
                    if (response.error !== undefined)
                    {
                        alert(response.error);
                    }
                    else
                    {
                        alert("alert success uploading file: " + response);
                    }
                }
            });

            fileuploadform.submit();
        }          */

        /*var modname = $('#modtitle').text();
        var description = $('textarea[name="description"]').val();
        var author = $('input[name="author"]').val();
        var privacy = $('select[name="privacy"] option:selected').val();
        var quality = $('select[name="quality"] option:selected').val();
        var version = $('input[name="comment"]').val();
        var os = $('input[name=os]:checked').val();
        var tasktype = $("select[name='category'] option:selected").val();
        var cpu = $("select[name='cpu'] option:selected").val();
        var commandLine = $('textarea[name="cmdtext"]').val();

        alert("privacy: " + privacy);

        var json = {};
        json["module"] = {"name": modname, "description": description,
            "author": author, "privacy": "\""+privacy+"\"", "quality": quality,
            "cpuType": cpu, "taskType": tasktype, "version": version,
            "os": os, "commandLine": commandLine};

        //json["parameters"]= getParametersJSON();

        $.ajax({
            type: "POST",
            url: "/gp/ModuleCreator/save",
            data: { "bundle" : JSON.stringify(json) },
            success: function(response) {
                var message = response["MESSAGE"];
                var error = response["ERROR"];
                var newLsid = response["lsid"];
                if (error !== undefined && error !== null) {
                    alert(error);
                }
                if (message !== undefined && message !== null) {
                    alert(message);
                }
                // Update the LSID upon successful save
                if (newLsid !== undefined && newLsid !== null) {
                    alert(newLsid);
                    module_editor["lsid"] = newLsid;
                }
            },
            dataType: "json"
        }); */
    });


    $("select[name='c_type']").change(function()
    {
        var cmdlinetext = $("#commandtextarea textarea").val();
        var type = $(this).val();

        var prev_cmd = $("#commandtextarea textarea").data("type");

        cmdlinetext = cmdlinetext.replace(prev_cmd, "");
        if(type == "java")
        {
            cmdlinetext = "<java>" + cmdlinetext;
        }
        else if(type == "perl")
        {
            cmdlinetext = "<perl>" + cmdlinetext;
        }
       

        $("#commandtextarea textarea").data("type", "<" + type +">");
        $("#commandtextarea textarea").attr("value", cmdlinetext);
    });

    updatemodulecategories();

    $("input[name='supportfiles']").change(function()
    {
        for (var i = 0; i < this.files.length; i++)
        {
            var file = this.files[i];

            var sfilelist = $("<li>" + $(this).val() + "</li>");

            var delbutton = $("<button>x</button>&nbsp;");
            delbutton.button().click(function()
            {
                $(this).parent().remove();
            });

            if($("#uploadedfiles").find(sfilelist).length > 0 )
            {
                alert("File already exists");
                return;
            }
            sfilelist.prepend(delbutton);
            $("#uploadedfiles").append(sfilelist);

            uploadedfiles.push($(this));
        }
    });

});


function uploadfiles(element)
{
    element.iframePostForm
	({
		json : false,
		post : function ()
		{
            alert("starting");
			if ($('input[type=file]').val().length)
			{
			    alert("uploading file: " + $('input[type=file]').val());
			}
			else
			{
				//cancel request
  				//return false;
              alert("canceled");
			}
		},
		complete : function (response)
		{

			if (!response.success)
			{
				alert("error uploading file");
			}
			else
			{
                alert("alert success uploading file");
			}
		}
	});
}