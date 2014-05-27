function prettyDate(now, time){
    var date = new Date(time || ""),
      diff = (((new Date(now)).getTime() - date.getTime()) / 1000),
      day_diff = Math.floor(diff / 86400);
   
    if ( isNaN(day_diff) || day_diff < 0 || day_diff >= 31 )
      return;
   
    return day_diff == 0 && (
        diff < 60 && "just now" ||
        diff < 120 && "1 minute ago" ||
        diff < 3600 && Math.floor( diff / 60 ) +
          " minutes ago" ||
        diff < 7200 && "1 hour ago" ||
        diff < 86400 && Math.floor( diff / 3600 ) +
          " hours ago") ||
      day_diff == 1 && "Yesterday" ||
      day_diff < 7 && day_diff + " days ago" ||
      day_diff < 31 && Math.ceil( day_diff / 7 ) +
        " weeks ago";
  }

//function createChoiceDiv(parameterName, groupId, initialValuesList)
//{
//    //var selectChoiceDiv = "";
//    var selectChoiceDiv = $("<div class='selectChoice'/>");
//}

//function createChoiceDiv(parameterName, groupId, initialValuesList)
//{
//    var selectChoiceDiv = $("<div class='selectChoice'/>");
//
//    //check if there are predefined list of choices for this parameter
//    var paramDetails = run_task_info.params[parameterName];
//    if(paramDetails.choiceInfo != undefined  && paramDetails.choiceInfo != null && paramDetails.choiceInfo != '')
//    {
//        if(paramDetails.choiceInfo.status != undefined && paramDetails.choiceInfo.status != null
//            && paramDetails.choiceInfo.status != undefined && paramDetails.choiceInfo.status != null
//            && paramDetails.choiceInfo.status.flag != "OK")
//        {
//            var errorDetailsLink = $("<a href='#'> (more...)</a>");
//
//            var errorMessageDiv = $("<p><span class='errorMessage'>No dynamic file selections available</span></p>");
//            errorMessageDiv.append(errorDetailsLink);
//            selectChoiceDiv.append(errorMessageDiv);
//            errorDetailsLink.data("errMsg", paramDetails.choiceInfo.status.message);
//            errorDetailsLink.click(function(event)
//            {
//                event.preventDefault();
//                var errorDetailsDiv = $("<div/>");
//                errorDetailsDiv.append("<p>"+  $(this).data("errMsg") + "</p>");
//                errorDetailsDiv.dialog(
//                    {
//                        title: "Dynamic File Selection Loading Error"
//                    }
//                );
//            });
//        }
//
//        //display drop down showing available file choices
//        var choice = $("<select class='choice' />");
//
//        if(paramDetails.allowMultiple)
//        {
//            choice.attr("multiple", "multiple");
//        }
//
//        if(paramDetails.required)
//        {
//            choice.addClass("requiredParam");
//        }
//
//        choice.data("pname", parameterName);
//        var longChars = 1;
//        for(var c=0;c<paramDetails.choiceInfo.choices.length;c++)
//        {
//            choice.append("<option value='"+paramDetails.choiceInfo.choices[c].value+"'>"
//                + paramDetails.choiceInfo.choices[c].label+"</option>");
//            if(paramDetails.choiceInfo.choices[c].label.length > longChars)
//            {
//                longChars = paramDetails.choiceInfo.choices[c].label.length;
//            }
//        }
//
//        selectChoiceDiv.append(choice);
//
//        var noneSelectedText = "Select an option";
//
//        var cMinWidth = Math.log(longChars) * 83;
//
//        if(cMinWidth == 0)
//        {
//            cMinWidth = Math.log(noneSelectedText.length) * 83;
//        }
//
//        choice.multiselect({
//            multiple: paramDetails.allowMultiple,
//            header: paramDetails.allowMultiple,
//            selectedList: 2,
//            minWidth: cMinWidth,
//            noneSelectedText: noneSelectedText,
//            classes: 'mSelect'
//        });
//
//        choice.multiselect("refresh");
//
//        //disable if no choices are found
//        if(paramDetails.choiceInfo.choices.length == 0)
//        {
//            choice.multiselect("disable");
//        }
//
//        choice.data("maxValue", paramDetails.maxValue);
//        choice.change(function ()
//        {
//            var valueList = [];
//
//            var value = $(this).val();
//
//            //if this a multiselect choice, then check that the maximum number of allowable selections was not reached
//            if($(this).multiselect("option", "multiple"))
//            {
//                var maxVal = parseInt($(this).data("maxValue"));
//                if(!isNaN(maxVal) && value.length() > maxVal)
//                {
//                    //remove the last selection since it will exceed max allowed
//                    if(value.length == 1)
//                    {
//                        $(this).val([]);
//                    }
//                    else
//                    {
//                        value.pop();
//                        $(this).val(value);
//                    }
//
//                    alert("The maximum number of selections is " + $(this).data("maxValue"));
//                    return;
//                }
//                valueList = value;
//            }
//            else
//            {
//                if(value != "")
//                {
//                    valueList.push(value);
//                }
//            }
//
//            var paramName = $(this).data("pname");
//
//            var groupId = getGroupId($(this));
//            updateValuesForGroup(groupId, paramName, valueList);
//        });
//
//        //set the default value
//        choice.children("option").each(function()
//        {
//            if(paramDetails.default_value != "" && $(this).val() == paramDetails.default_value)
//            {
//                $(this).parent().val(paramDetails.default_value);
//                $(this).parent().data("default_value", paramDetails.default_value);
//                $(this).parent().multiselect("refresh");
//            }
//        });
//
//        //select initial values if there are any
//        if( initialValuesList != undefined &&  initialValuesList != null)
//        {
//            var matchingValueList = [];
//            for(var n=0;n<initialValuesList.length;n++)
//            {
//                choice.find("option").each(function()
//                {
//                    if(initialValuesList[n] != "" && initialValuesList[n] == $(this).val())
//                    {
//                        matchingValueList.push(initialValuesList[n]);
//                    }
//                });
//            }
//
//            //should only be one item in the list for now
//            //but handle case when there is more than one item
//            if(choice.multiselect("option", "multiple"))
//            {
//                if(matchingValueList.length > 0)
//                {
//                    //indicate initial value was found in drop-down list
//                    run_task_info.params[parameterName].initialChoiceValues = true;
//                }
//
//                choice.val(matchingValueList);
//            }
//            else
//            {
//                //if there is more than one item in the list then only the first item in the list
//                //will be selected since the choice is not multiselect
//                if(initialValuesList.length > 0)
//                {
//                    run_task_info.params[parameterName].initialChoiceValues = false;
//
//                    if(!(paramDetails.default_value == "" && initialValuesList[0] == "")
//                        && $.inArray(initialValuesList[0], matchingValueList) != -1)
//                    {
//                        choice.val( initialValuesList[0]);
//                    }
//
//                    if((paramDetails.default_value == "" && initialValuesList[0] == "")
//                        || $.inArray(initialValuesList[0], matchingValueList) != -1)
//                    {
//                        //indicate initial value was found in drop-down list
//                        run_task_info.params[parameterName].initialChoiceValues = true;
//                    }
//                }
//            }
//
//            choice.multiselect("refresh");
//        }
//        else
//        {
//            run_task_info.params[parameterName].initialChoiceValues = true;
//        }
//
//        var valueList = [];
//        if(choice.val() != null && choice.val() != "")
//        {
//            valueList.push(choice.val());
//        }
//        updateValuesForGroup(groupId, parameterName, valueList);
//    }
//
//    //if this is not a reloaded job where the value was from a drop down list
//    //and the type is not also a file
//    if(!run_task_info.params[parameterName].initialChoiceValues
//        && $.inArray(field_types.FILE, run_task_info.params[parameterName]) != -1)
//    {
//        selectChoiceDiv.hide();
//    }
//
//    return selectChoiceDiv;
//}
