/**
 * Initialize the choiceDiv. This method is called the first time the job input form is loaded.
 * The buildChoiceDiv is called after a dynamic choice is initialized from the GP server.
 * 
 * @param paramaterName
 * @param groupId
 * @param initialValuesList
 */
function initChoiceDiv(parameterName, groupId, initialValuesList)
{
    var selectChoiceDiv = $("<div class='selectChoice' />");
    //check if there are predefined list of choices for this parameter
    var paramDetails = run_task_info.params[parameterName];
    var choiceInfo = paramDetails.choiceInfo;
    return buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, initialValuesList);
}

/**
 * Helper method, for the given choiceInfo, are there any matching values in the
 * given list of initialValues.
 * 
 * @param choiceInfo
 * @param initialValues
 * 
 * @return an empty array if there are no matches.
 */
function getMatchingChoices(choiceInfo, initialValues) {
    var matching = [];
    if (!choiceInfo || !choiceInfo.choices || choiceInfo.length==0 || !initialValues || initialValues.length==0 ) {
        return matching;
    }
    for(var i=0; i<choiceInfo.choices.length; i++) {
        var choice=choiceInfo.choices[i];
        if (choice.value) {
            for(var j=0; j<initialValues.length; j++) {
                if (choice.value == initialValues[j]) {
                    matching.push(initialValues[j]);
                }
            }
        }
    }
    return matching;
}

/**
 * Helper method, for the given choiceInfo, are there any 'custom values'
 * in the given list of initialValues. 
 * A custom value is defined as anything which is not in the 
 * drop-down menu.
 * 
 * @param choiceInfo
 * @param initialValues
 * @returns
 */
function getCustomChoices(choiceInfo, initialValues) {
    if (!choiceInfo || !choiceInfo.choices || choiceInfo.length==0 || !initialValues || initialValues.length==0 ) {
        console.log("Undefined behavior", choiceInfo, initialValues);
        return [];
    }
    var custom = initialValues.slice(0);
    var matching = getMatchingChoices(choiceInfo, initialValues);
    for (var i=0; i<matching.length; ++i) {
        custom.pop(matching[i]);
    }
    return custom;
}

function buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, initialValuesList) {
    console.log("buildChoiceDiv for "+parameterName, "initialValues="+initialValuesList);
    var doLoadChoiceDiv=false;

    if(paramDetails != undefined && paramDetails != null && choiceInfo != undefined  && choiceInfo != null && choiceInfo != '')
    {
        if(choiceInfo.status != undefined && choiceInfo.status != null
                && choiceInfo.status != undefined && choiceInfo.status != null
                && choiceInfo.status.flag != "OK")
        {
            //special-case, not yet initialized
            if (choiceInfo.status.flag == "NOT_INITIALIZED") {
                doLoadChoiceDiv=true;
                var downloadingChoicesDiv = $("<p><img src='/gp/images/ajax.gif' height='24' width='24' alt='Dowloading drop-down menu' />Downloading drop-down menu ... </p>");
                selectChoiceDiv.append(downloadingChoicesDiv);
            }
            else {             
                var errorDetailsLink = $("<a href='#'> (more...)</a>");

                var errorMessageDiv = $("<p><span class='errorMessage'>No dynamic file selections available</span></p>");
                errorMessageDiv.append(errorDetailsLink);
                selectChoiceDiv.append(errorMessageDiv);
                errorDetailsLink.data("errMsg", choiceInfo.status.message);
                errorDetailsLink.click(function(event) {
                    event.preventDefault();
                    var errorDetailsDiv = $("<div/>");
                    errorDetailsDiv.append("<p>"+  $(this).data("errMsg") + "</p>");
                    errorDetailsDiv.dialog(
                            {
                                title: "Dynamic File Selection Loading Error"
                            }
                    );
                });
            }
        }

        //display drop down showing available file choices
        var choiceId = parameterName;
        if (groupId !== null) {
            choiceId = choiceId+"_"+groupId;
        }
        var choice = $("<select class='choice' id='"+choiceId+"' />");

        if(paramDetails.allowMultiple)
        {
            choice.attr("multiple", "multiple");
        }

        if(paramDetails.required)
        {
            choice.addClass("requiredParam");
        }

        choice.data("pname", parameterName);
        var longChars = 1;
        for(var c=0;c<choiceInfo.choices.length;c++)
        {
            choice.append("<option value='"+choiceInfo.choices[c].value+"'>"
                    + choiceInfo.choices[c].label+"</option>");
            if(choiceInfo.choices[c].label.length > longChars)
            {
                longChars = choiceInfo.choices[c].label.length;
            }
        }

        selectChoiceDiv.append(choice);

        var noneSelectedText = "Select an option";

        var cMinWidth = Math.log(longChars) * 83;

        if(cMinWidth == 0)
        {
            cMinWidth = Math.log(noneSelectedText.length) * 83;
        }

        choice.multiselect({
            multiple: paramDetails.allowMultiple,
            header: paramDetails.allowMultiple,
            selectedList: 2,
            minWidth: cMinWidth,
            noneSelectedText: noneSelectedText,
            classes: 'mSelect'
        });

        choice.multiselect("refresh");

        //disable if no choices are found
        if(choiceInfo.choices.length == 0)
        {
            choice.multiselect("disable");
        }

        choice.data("maxValue", paramDetails.maxValue);
        choice.change(function ()
                {
            var valueList = [];

            var value = $(this).val();

            //if this a multiselect choice, then check that the maximum number of allowable selections was not reached
            if($(this).multiselect("option", "multiple"))
            {
                var maxVal = parseInt($(this).data("maxValue"));
                if(!isNaN(maxVal) && value.length() > maxVal)
                {
                    //remove the last selection since it will exceed max allowed
                    if(value.length == 1)
                    {
                        $(this).val([]);
                    }
                    else
                    {
                        value.pop();
                        $(this).val(value);
                    }

                    alert("The maximum number of selections is " + $(this).data("maxValue"));
                    return;
                }
                valueList = value;
            }
            else
            {
                if(value != "")
                {
                    valueList.push(value);
                }
            }

            var paramName = $(this).data("pname");

            var groupId = getGroupId($(this));
            updateValuesForGroup(groupId, paramName, valueList);
                });

        //set the default value
        choice.children("option").each(function()
                {
            if(paramDetails.default_value != "" && $(this).val() == paramDetails.default_value)
            {
                $(this).parent().val(paramDetails.default_value);
                $(this).parent().data("default_value", paramDetails.default_value);
                $(this).parent().multiselect("refresh");
            }
                });

        //select initial values if there are any
        if( initialValuesList != undefined &&  initialValuesList != null)
        {
            var matchingValueList = [];
            for(var n=0;n<initialValuesList.length;n++)
            {
                choice.find("option").each(function()
                        {
                    if(initialValuesList[n] != "" && initialValuesList[n] == $(this).val())
                    {
                        matchingValueList.push(initialValuesList[n]);
                    }
                        });
            }

            //should only be one item in the list for now
            //but handle case when there is more than one item
            if(choice.multiselect("option", "multiple"))
            {
                if(matchingValueList.length > 0)
                {
                    //indicate initial value was found in drop-down list
                    //run_task_info.params[parameterName].initialChoiceValues = true;
                    paramDetails.initialChoiceValues = true; 
                }

                choice.val(matchingValueList);
            }
            else
            {
                //if there is more than one item in the list then only the first item in the list
                //will be selected since the choice is not multiselect
                if(initialValuesList.length > 0)
                {
                    //run_task_info.params[parameterName].initialChoiceValues = false;
                    paramDetails.initialChoiceValues = false; 

                    if(!(paramDetails.default_value == "" && initialValuesList[0] == "")
                            && $.inArray(initialValuesList[0], matchingValueList) != -1)
                    {
                        choice.val( initialValuesList[0]);
                    }

                    if((paramDetails.default_value == "" && initialValuesList[0] == "")
                            || $.inArray(initialValuesList[0], matchingValueList) != -1)
                    {
                        //indicate initial value was found in drop-down list
                        //run_task_info.params[parameterName].initialChoiceValues = true;
                        paramDetails.initialChoiceValues = true; 
                    }
                }
            }

            choice.multiselect("refresh");
        }
        else
        {
            //run_task_info.params[parameterName].initialChoiceValues = true;
            paramDetails.initialChoiceValues = true; 
        }

        var valueList = [];
        if(choice.val() != null && choice.val() != "")
        {
            valueList.push(choice.val());
        }
        updateValuesForGroup(groupId, parameterName, valueList);
    }

    //if this is not a reloaded job where the value was from a drop down list
    //and the type is not also a file
    if(!paramDetails.initialChoiceValues
    //if(!run_task_info.params[parameterName].initialChoiceValues
            && $.inArray(field_types.FILE, run_task_info.params[parameterName]) != -1)
    {
        selectChoiceDiv.hide();
    }
    
    if (doLoadChoiceDiv === true) {
        // HACK: ignore previously selected custom values
        paramDetails.initialChoiceValues = true; 
        reloadChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, initialValuesList);
    }

    return selectChoiceDiv;
}

/**
 * Ajax call to list the contents of a remote directory, for building a dynamic drop-down menu.
 * @param {choiceDir} choiceDir, the choiceDir object returned by the GP server.
 *     
 * @returns a choiceInfo object
 */
function reloadChoiceDiv(selectChoiceDiv, choiceInfoIn, paramDetails, parameterName, groupId, initialValuesList) {
    $.getJSON( choiceInfoIn.href, 
        function( choiceInfo ) {
            console.log("drop-down loaded from: " + choiceInfo.href); 
            console.log("status: " + JSON.stringify(choiceInfo.status, null, 2));
            $(selectChoiceDiv).empty();
            buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, initialValuesList);
            
            //if it's a custom value then do the same as a send to parameter
            var customChoices=getCustomChoices(choiceInfo, initialValuesList);
            if (customChoices && customChoices.length>0) {
                if (customChoices[0] != "") {
                    setInputField(parameterName, customChoices[0], groupId);
                }
            }
        } 
    );
}

