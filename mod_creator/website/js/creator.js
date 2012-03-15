var mainLayout;

var module_editor = {
    lsid: "",
    uploadedfiles: [],
    supportfileinputs: []
};

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
                   <textarea cols='60' name='p_description' rows='3'></textarea> \
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
                <td colspan='2'>   \
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
        var tSelect = $(this);
        var value = $(this).val();

        if(tSelect.data("editing") !== value)
        {
            if(!tSelect.parent().next().children().is("input[name='p_invisible']"))
            {
                $(this).parent().next().remove();
            }

            if(value == "Choice")
            {
                //added as a workaround - check for duplicate calls to this function
                var editChoiceList = $("<td><input type='text' name='choicelist' size='40'/></td>");
                var editChoiceLink = $("<a href='#'>edit choice</a>");

                editChoiceLink.click(function()
                {
                    var choices = $(this).prev().val();

                    $( "#editchoicedialog" ).dialog({
                        autoOpen: true,
                        height: 500,
                        width: 360,
                        create: function()
                        {
                            $(this).find("tr td").remove();

                            var result = choices.split(';');
                            if(result == null || result == "" || result.length < 0)
                            {
                                //start with two rows of data
                                var trowdef = $("<tr><td> <input type='text' name='choicen' size='15'/> </td>" +
                                     "<td> <input type='text' name='choicev' size='15'/> </td>" +
                                     "<td> <button> X </button></td></tr>");

                                trowdef.find("button").button().click(function()
                                {
                                        $(this).parent().parent().remove();
                                });

                                var trowdef2 = $("<tr><td> <input type='text' name='choicen' size='15'/> </td>" +
                                     "<td> <input type='text' name='choicev' size='15'/> </td>" +
                                     "<td> <button> X </button></td></tr>");

                                trowdef2.find("button").button().click(function()
                                {
                                        $(this).parent().parent().remove();
                                });

                                $(this).find("table").append(trowdef);
                                $(this).find("table").append(trowdef2);
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

                                        if(choicelist !== "")
                                        {
                                            choicelist += ";";
                                        }
                                        choicelist += dvalue + "=" + value;
                                    });

                                    tSelect.find("input[name='choicelist']").each(function()
                                    {
                                        $(this).val(choicelist);
                                        tSelect.data('editing', "Choice");
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
                });

                tSelect.data("editing", "Choice");

                $(this).parent().after(editChoiceList);
                editChoiceList.append(editChoiceLink);
            }
            else if(value == "Input File")
            {

                var fileFormatList = $('<select multiple="multiple" name="fileformat"></select>');
                var fileFormatButton = $('<button id="addinputfileformat">New</button>');

                fileFormatButton.click(function()
                {
                    $( "#addfileformatdialog" ).dialog("open");                
                });

                $('select[name="fileformat"]').children("option").each(function()
                {
                    fileFormatList.append($(this).clone());
                });

                var fileFormatTD = $("<td></td>");
                $(this).parent().after(fileFormatTD);
                fileFormatTD.hide();
                fileFormatTD.append(fileFormatList);
                fileFormatTD.append(fileFormatButton);

                fileFormatTD.show();
                fileFormatList.multiselect({
                    header: false,
                    selectedList: 4 // 0-based index
                });

                tSelect.data("editing", "Input File");
            }
            else
            {
                tSelect.data('editing', "");                   
            }
        }
        
    });

    $("select[name='p_format']").multiselect({
        header: false,
        selectedList: 4 // 0-based index
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
    if(name !== "")
    {
        text = "&lt;" + name + "&gt;";
    }

    if(flag !== "")
    {
        text = flag + delimiter + text;
    }

    var item = $("<li class='commanditem'>" +
                 text +
                "</li>");

    //construct prev parameter value
    var  prevtext = "";
    if(prevname !== "")
    {
        prevtext = "&lt;" + prevname + "&gt;";
    }

    if(prevflag !== "")
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
            if(text !== "")
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
    if(!found && text !== "")
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
        if(cmd_args !== "")
        {
            //add space between arguments;
            cmd_args += " ";
        }
        cmd_args += val;
    });

    var cmdline = cmd_args;
    var cmdtype = $("#commandtextarea textarea").data("type");
    if(cmdtype !== undefined)
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

function updatefileformats()
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/fileformats",
        success: function(response) {
            var error = response["ERROR"];
            if (error !== undefined) {
                alert(error);
            }
            else {
                var fileformats = response["fileformats"];
                fileformats = fileformats.substring(1, fileformats.length-1);

                var result = fileformats.split(", ");
                var mcat = $("select[name='fileformat']");

                for(i=0;i < result.length;i++)
                {
                    mcat.append($("<option>" + result[i] + "</option>"));
                }

                $("select[name='fileformat']").multiselect('refresh');
            }
        },
        dataType: "json"
    });
}

function addsectioncollapseimages()
{
    var imagecollapse = $("<img class='imgcollapse' src='styles/images/section_collapsearrow.png' alt='some_text' width='11' height='11'/>");
    var imageexpand = $("<img class='imgexpand' src='styles/images/section_expandarrow.png' alt='some_text' width='11' height='11'/>");

    $(".heading").prepend(imageexpand);    
    $(".heading").prepend(imagecollapse);

    $(".heading").children(".imgcollapse").toggle();

    $(".heading").next(".content").data("visible", true);    
}

/*function getParametersJSON()
{
    var parameters = [];

    $(".parameters").each(function()
    {
        /*p1_MODE=IN
        p1_TYPE=FILE
        p1_default_value=
        p1_description=input filename - .res, .gct, .odf
        p1_fileFormat=res;gct;Dataset
        p1_name=input.filename
        p1_optional=
        p1_prefix_when_specified=
        p1_type=java.io.File
        p1_value=

        var pname = $(this).find('input[name="p_name"').val();
        var type = $(this).find('select[name="p_type"] option:selected').val();
        var default_val = $(this).find('select[name="p_defaultvalue"] option:selected').val(); 
        var optional = $(this).find('input[name="p_optional"]').val();         
    });
    
    return parameters;
}                 */

jQuery(document).ready(function() {

    addsectioncollapseimages();
    updatemodulecategories();
    updatefileformats();

    $(".heading").click(function()
    {
        var visible = $(this).next(".content").data("visible");
        //if first time then content is visible
        if(visible == undefined)
        {
            visible = true;
        }

        $(this).next(".content").slideToggle(340);
        $(this).children(".imgcollapse:first").toggle();
        $(this).children(".imgexpand:first").toggle();
        
        //visibilty has changed to the opposite
        $(this).next(".content").data("visible", !visible);
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
                        $("select[name='fileformat']").each(function()
                        {
                            $(this).append(newfileformat);
                            $(this).multiselect("refresh");
                        });

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
        for(i=0;i < module_editor.supportfileinputs.length;i++)
        {
            var fileuploadform = $('<form action="/gp/ModuleCreator/upload" method="post" ' +
                                 'enctype="multipart/form-data" class="fileuploadform"> </form>');

            fileuploadform.append(module_editor.supportfileinputs[i]);

            fileuploadform.iframePostForm
            ({
                json : false,
                post : function ()
                {
                    //if file is not empty then upload
                    if (!$(this).find('input[type=file]').val().length)
                    {
                        //cancel request
                        return false;
                    }
                },
                complete : function (response)
                {
                    response = $.parseJSON($(response)[0].innerHTML);                    
                    if (response.error !== undefined)
                    {
                        alert(response.error);
                    }
                    else
                    {
                        module_editor["uploadedfiles"].push(response.location);
                    }
                }
            });

            fileuploadform.submit();
        }

        var modname = $('#modtitle').val();
        var description = $('textarea[name="description"]').val();
        var author = $('input[name="author"]').val();
        var privacy = $('select[name="privacy"] option:selected').val();
        var quality = $('select[name="quality"] option:selected').val();
        var version = $('input[name="comment"]').val();
        var os = $('input[name=os]:checked').val();
        var tasktype = $("select[name='category'] option:selected").val();
        var cpu = $("select[name='cpu'] option:selected").val();
        var commandLine = $('textarea[name="cmdtext"]').val();

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
                    module_editor["lsid"] = newLsid;
                }
            },
            dataType: "json"
        }); 
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

    $(".supportfile").live("change", function()
    {
        var file = $(this).val();
        var sfilelist = $("<li>" + file + "</li>");

        var delbutton = $("<button>x</button>&nbsp;");
        delbutton.button().click(function()
        {
            var index;
            for(i=0;i<module_editor.supportfileinputs.length;i++)
            {
                var value1 = module_editor.supportfileinputs[i].val();
                var value2 = $(this).parent().text();
                if(value1 === value2)
                {
                    index = i;
                }
            }

            module_editor.supportfileinputs.splice(index,1);
            $(this).parent().remove();
        });

        sfilelist.prepend(delbutton);

        var exists = false;
        $("#supportfileslist").children().each(function()
        {
            var value = $(this).text();
            if(value === sfilelist.text())
            {
                exists = true;
            }          
        });

        if(exists)
        {
            alert("File " + file + " already exists");            
            return;
        }

        $("#supportfileslist").append(sfilelist);

        $(this).attr('name', "name" + module_editor.supportfileinputs.length);  
        var parent = $(this).parent();
        parent.append('<input type="file" class="supportfile">');
        $(this).detach();
        module_editor.supportfileinputs.push($(this));
    });

    $("select[name='fileformat']").multiselect({
        header: false,
        selectedList: 4 // 0-based index
    });
});
