
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

/**
 * Remove all instances of the given value from the given array.
 * @param fromArray
 * @param value
 * @returns
 */
function removeItemsFromArray(fromArray, value) {
    var b = '';
    for (b in fromArray) {
        if (fromArray[b] === value) {
            fromArray.splice(b, 1);
            break;
        }
    }
    return fromArray;
}

/**
 * Helper method, for the given choiceInfo, are there any items
 * which match the given value.
 * 
 * @param choiceInfo
 * @param value
 * @returns {Boolean}
 */
function hasMatchingValue(choiceInfo, value) {
    for(var i=0; i<choiceInfo.choices.length; ++i) {
        var choice=choiceInfo.choices[i];
        if (value === choice.value) {
            return true;
        }
    }
}

/**
 * Helper method, for the given choiceInfo, are there any custom values
 * in the given list of initialValues. 
 * A custom value is defined as anything which is not in the 'choiceInfo.choices'
 * drop-down menu.
 * 
 * Special-case: Ignore the empty string if it is in the list of initialValues.
 * 
 * For example, given a drop-down menu containing:
 *     "A.txt", "B.txt", "C.txt"
 *     
 * Given initialValues=A.txt, return []
 * Given initialValues=D.txt, return [ D.txt ]
 * Given initialValues="" (empty string), return [], empty array
 * 
 * @param choiceInfo
 * @param initialValues
 * @returns
 */
function getCustomChoices(choiceInfo, initialValues) {
    if (!initialValues || initialValues.length==0) {
        return [];
    }
    if (!choiceInfo || !choiceInfo.choices || choiceInfo.length==0) {
        return [];
    }
    
    // check for matching items from the list of initial values
    // ignore empty string values
    var custom=[];
    for(var i=0; i<initialValues.length; ++i) {
        if (initialValues[i] != "") {
            if (!hasMatchingValue(choiceInfo, initialValues[i])) {
                custom.push(initialValues[i]);
            }
        }
    }
    return custom;
}

function buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, initialValuesList) {
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

        var cMinWidth = Math.log(longChars) * 90;

        if(cMinWidth == 0)
        {
            cMinWidth = Math.log(noneSelectedText.length) * 90;
        }

        if(cMinWidth < 300)
        {
            cMinWidth = 300;
        }

        var validateSelectionFunc = function (element)
        {
            var valueList = [];

            var value = $(element).val();

            //if this a multiselect choice, then check that the maximum number of allowable selections was not reached
            if($(element).multiselect("option", "multiple"))
            {
                var maxVal = parseInt($(element).data("maxValue"));
                var numSelected = $(element).multiselect("widget").find("input:checked").length;
                if(!isNaN(maxVal) && numSelected > maxVal)
                {
                    //remove the last selection since it will exceed max allowed
                    if(value.length == 1)
                    {
                        $(element).val([]);
                    }
                    else
                    {
                        value.pop();
                        $(element).val(value);
                    }

                    alert("The maximum number of selections allowed is " + $(element).data("maxValue"));
                    return false;
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

            var paramName = $(element).data("pname");

            var groupId = getGroupId($(element));
            updateValuesForGroup(groupId, paramName, valueList);

            return true;
        };

        choice.multiselect({
            multiple: paramDetails.allowMultiple,
            header: paramDetails.allowMultiple,
            selectedList: 2,
            minWidth: cMinWidth,
            noneSelectedText: noneSelectedText,
            classes: 'mSelect',
            checkAll: function() {
                var result = validateSelectionFunc(this);

                if(result == false)
                {
                    //unselect everything since it is over limit
                    $(this).multiselect("uncheckAll");
                }
            },
            click: function()
            {
                return validateSelectionFunc(this);
            }
        });

        choice.multiselect("refresh");

        //disable if no choices are found
        if(choiceInfo.choices.length == 0)
        {
            choice.multiselect("disable");
        }

        choice.data("maxValue", paramDetails.maxValue);
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
                    if(initialValuesList[n] == $(this).val())
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
            if (window.console) {
                console.log("drop-down loaded from: " + choiceInfo.href); 
                console.log("status: " + JSON.stringify(choiceInfo.status, null, 2));
            }
            $(selectChoiceDiv).empty();
            buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, initialValuesList);
            
            //if it's a custom value then do the same as a send to parameter
            var customChoices=getCustomChoices(choiceInfo, initialValuesList);
            if (customChoices && customChoices.length>0) {
                // only if the first item in the list is not the empty string
                if (customChoices[0] != "") {
                    setInputField(parameterName, customChoices[0], groupId);
                }
            }
        } 
    );
}

