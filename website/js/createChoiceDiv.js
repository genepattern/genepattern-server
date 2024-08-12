
/**
 * Initialize the choiceDiv. This method is called the first time the job input form is loaded.
 * The buildChoiceDiv is called after a dynamic choice is initialized from the GP server.
 *
 * @param paramaterName
 * @param groupId
 * @param initialValuesList
 */
function initChoiceDiv(parameterName, groupId, enableBatch, initialValuesList)
{
    var selectChoiceDiv = $("<div class='selectChoice' />");
    //check if there are predefined list of choices for this parameter
    var paramDetails = run_task_info.params[parameterName];
    var choiceInfo = paramDetails.choiceInfo;
    return buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, enableBatch, initialValuesList);
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

function buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, enableBatch, initialValuesList) {
    var doLoadChoiceDiv=false;

    // Create the single/batch run mode toggle only if this is not a file choice parameter
    // so that duplicate toggles are not created
    if (enableBatch && $.inArray(field_types.FILE, run_task_info.params[parameterName].type) == -1) {
        var batchBox = $("<div class='batchBox' title='A job will be launched for every value specified.'></div>");
        // Add the checkbox
        var batchCheck = $("<input type='checkbox' id='batchCheck" + parameterName + "' />");
        batchCheck.change(function ()
        {
            var paramName = $(this).parents("tr").first().data("pname");

            var textElement = $(this).closest(".pRow").find(".selectChoice");
            var groupId = getGroupId(textElement);

            var isBatch = $(this).is(":checked");
            updateNonFileView(textElement, paramName, groupId, isBatch);
        });

        batchBox.append(batchCheck);
        batchBox.append("<label for='batchCheck" + parameterName + "'>Batch</label>");
        //batchCheck.button();
        batchBox.tooltip();
        batchBox.append("<a class='batchHelp' href='https://www.genepattern.org/how-batching-works-in-genepattern' target='_blank'><img src='/gp/images/help_small.gif' width='12' height='12'/></a>");

        selectChoiceDiv.append(batchBox);

        //if this is a batch parameter then pre-select the batch checkbox
        if (run_task_info.params[parameterName].isBatch) {
            batchBox.find("input[type='checkbox']").prop('checked', true);
        }
    }

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
        var datalist = null;
		if (paramDetails.userSuppliedOK){

			choice = $("<input type='search' list='"+choiceId+"_defaults'  class='choice' id='"+choiceId+"' />");
			
			datalist = $("<datalist id='"+choiceId+"_defaults'/>")
			
			$( choice ).on( "change", function() {
				// GP-9659 no multiselect possible when you might provide your own value
				// on top of those in the list
				var elem = $(this);
				var paramName = $(elem).data("pname");
				var groupId = getGroupId($(elem));
				valueList = [elem.val()];   
            	updateValuesForGroup(groupId, paramName, valueList);
			} );
			$( choice ).on("change", function(e) {
	        	e.target.setAttribute("placeholder", e.target.value);
	        	e.target.value = "";
	      	});
	      	$( choice ).on("focus", function(e) {
	        	e.target.setAttribute("placeholder", e.target.value);
	        	e.target.value = "";
	      	});
	      	$( choice ).on("blur", function(e) {
	        	e.target.value = e.target.getAttribute("placeholder");
	      	});
		}
		
          
	      	
				
        if(paramDetails.allowMultiple)
        {
            choice.attr("multiple", "multiple");
        }

        if(paramDetails.required)
        {
            choice.addClass("requiredParam");
        }

        choice.data("pname", parameterName);
        choice.data("groupId", groupId);

        var longChars = 1;

        var choiceMapping = run_task_info.params[parameterName].choiceMapping = {};

        for(var c=0;c<choiceInfo.choices.length;c++)
        {
            choiceMapping[choiceInfo.choices[c].value] = choiceInfo.choices[c].label;
        }
        // if its default is blank and blank does not match any of the choices
        // add a default --select an option -- to the top
        // <option disabled selected value> -- select an option -- </option>
        if ((!choiceMapping[""]) && ((paramDetails.default_value == "") || (paramDetails.default_value === null))){

			if (paramDetails.required == true) {
				var pleaseSelectLabel = " -- select an option -- ";
				choiceMapping[""] = pleaseSelectLabel;
				choice.append("<option disabled value=''>" + pleaseSelectLabel + "</option>");

			} else {
				var pleaseSelectLabel = " ";
				choiceMapping[""] = pleaseSelectLabel;
				choice.append("<option value=''>" + pleaseSelectLabel + "</option>");
			}

			if (paramDetails.userSuppliedOK) {
				// GP-9659 its a choice OR a user supplied value.  Do we want this for optional as well as required params?
				var pleaseSelectLabel = " -- select an option or type in a new value -- ";
				choiceMapping[""] = pleaseSelectLabel;
				datalist.append("<option disabled value=''>" + pleaseSelectLabel + "</option>");
				$(choice).attr("placeholder", pleaseSelectLabel);
			}	
        }
        
        
        for(var c=0;c<choiceInfo.choices.length;c++)
        {
        	//    choiceMapping[choiceInfo.choices[c].value] = choiceInfo.choices[c].label;
        	anOption = "<option value='"+choiceInfo.choices[c].value+"'>"
                    + choiceInfo.choices[c].label+"</option>";
            if (paramDetails.userSuppliedOK){
				datalist.append(anOption);
			} else {
					
				choice.append(anOption);
			}
            
            if(choiceInfo.choices[c].label.length > longChars)
            {
                longChars = choiceInfo.choices[c].label.length;
            }
        }
        
        selectChoiceDiv.append(choice);
        
        if (paramDetails.userSuppliedOK){
        	selectChoiceDiv.append(datalist);
		}
		
        var noneSelectedText = "Select an option";

        var cMinWidth = Math.log(longChars) * 100;

        if(cMinWidth == 0)
        {
            cMinWidth = Math.log(noneSelectedText.length) * 90;
        }

        if(cMinWidth < 300)
        {
            cMinWidth = 300;
        }

        var validateSelectionFunc = function (element, val)
        {
            var paramName = $(element).data("pname");

            var valueList = [];

            var value = val;
            if(val == undefined)
            {
                val = $(element).val();
            }

            //if this a multiselect choice, then check that the maximum number of allowable selections was not reached
            if(run_task_info.params[paramName].allowMultiple)
            {
                var maxVal = parseInt($(element).data("maxValue"));
                var numSelected = $(element).multiselect("widget").find("input:checked").length;
                if(!run_task_info.params[paramName].isBatch && !isNaN(maxVal) && numSelected > maxVal)
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
                valueList = value;
            }

            var groupId = getGroupId($(element));
            updateValuesForGroup(groupId, paramName, valueList);

            return true;
        };

 	    if (!paramDetails.userSuppliedOK) {
	        choice.multiselect({
	            multiple: paramDetails.allowMultiple,
	            header: paramDetails.allowMultiple,
	            selectedText:  function(numChecked, numTotal, checkedItems)
	            {
	                var item1 = $(this)[0].element;
	
	                //show the list of values
	                if(numChecked == 1)
	                {
	                    //return $(item1).find("option[value='" + $(item1).val() + "']").html();
	                    return $(item1).find("option:selected").text();
	                }
	                else if(numChecked < 5) //show the list of values
	                {
	                    var paramName = $(item1).data("pname");
	                    var groupId = $(item1).data("groupId");
	
	                    var valuesList = parameter_and_val_groups[paramName].groups[groupId].values;
	                    var textList = [];
	
	                     
	                    for(var l=0 ;l < checkedItems.length;l++)
	                    {
	                        var value = checkedItems[l].value;
	                        var optionText = item1.find("option[value='" + value + "']").text();
	                        textList.push(optionText);
	                    }
	
	                    return textList.join(", ");
	                }
	                else
	                {
	                    return numChecked + " selected";
	                }
	            },
	            minWidth: cMinWidth,
	            menuWidth: cMinWidth,
	            
	            noneSelectedText: noneSelectedText,
	            classes: 'mSelect',
	            checkAll: function() {
	                var result = validateSelectionFunc(this, $(this).val());
	
	                if(result == false)
	                {
	                    //unselect everything since it is over limit
	                    $(this).multiselect("uncheckAll");
	                }
	            },
	            uncheckAll: function(event)
	            {
	                var valuesList = [];
	            },
	            click: function (event, ui) {
	                var paramName = $(this).data("pname");
	                var groupId = getGroupId($(this));
	
	                var values = getValuesForGroup(groupId, paramName).slice();
	
	                if(values === undefined || values === null || !run_task_info.params[paramName].allowMultiple || values == "")
	                {
	                    values = [];
	                }
	
	                if(ui.checked && ui.value !== "")
	                {
	                    //add the value
	                    values.push(ui.value);
	                }
	                else
	                {
	                    //remove the value
	                    var index = $.inArray(ui.value, values);
	                    values.splice(index, 1);
	                }
	                return validateSelectionFunc(this, values);
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
	            } else if(paramDetails.default_value == "" && $(this).val() == paramDetails.default_value){
	            	$(this).parent().val(paramDetails.default_value);
	                $(this).parent().data("default_value", paramDetails.default_value);
	                $(this).parent().multiselect("refresh");
	            }
	            	
	        });
		}
        //select initial values if there are any
        if ( initialValuesList != undefined &&  initialValuesList != null) {
            var matchingValueList = [];
            var optionsHolder = choice;
            if (paramDetails.userSuppliedOK) optionsHolder = datalist;
            
            
            for(var n=0;n<initialValuesList.length;n++)
            {
                optionsHolder.find("option").each(function()
                {
                    if(initialValuesList[n] == $(this).val())
                    {
                        matchingValueList.push(initialValuesList[n]);
                    }
                });
            }

            //should only be one item in the list for now
            //but handle case when there is more than one item
            if(paramDetails.allowMultiple)
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

                    // not (has default, initial is empty) and empty is an option
                    if(!(paramDetails.default_value == "" && initialValuesList[0] == "")
                            && $.inArray(initialValuesList[0], matchingValueList) != -1)
                    {
                        choice.val( initialValuesList[0]);
                    } 

                    // has default, empty initial value list but empty is an option
                    if((paramDetails.default_value == "" && initialValuesList[0] == "")
                            || $.inArray(initialValuesList[0], matchingValueList) != -1)
                    {
                        //indicate initial value was found in drop-down list
                        //run_task_info.params[parameterName].initialChoiceValues = true;
                        paramDetails.initialChoiceValues = true;
                    } 
                    
                    // JTL 1/10/2022  GP-9009
                    // has an initial value that is not part of the list, make sure the drop down does not show the 
                    // default if it is a file type input
                    if ((initialValuesList[0] != "") && ($.inArray(initialValuesList[0], matchingValueList) == -1)
                    		&& !($.inArray(initialValuesList[0], "") == -1)
                    		&& (paramDetails.choiceInfo.choiceAllowCustom == true)) {
                    	console.log("Got an entry that is not in the list");
                    	choice.val("");
                    }
                    
                    
                }
            }

            if (!paramDetails.userSuppliedOK) {
				choice.multiselect("refresh");
			} else {
				// for the user supplied values or list, we cannot do multiselect or batch (without developing a new UI
				// control to do it anyway, so its much simpler here, since there can only be one value)
				choice.val( initialValuesList[0]);
			}
        }
        else
        {
            //run_task_info.params[parameterName].initialChoiceValues = true;
            paramDetails.initialChoiceValues = true;
        }

        var valueList = [];
        if(choice.val() != null && choice.val() != "")
        {
            if(paramDetails.allowMultiple)
            {
                valueList = choice.val(); //already a list of values
            }
            else{
                valueList.push(choice.val());
            }
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
        reloadChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, enableBatch, initialValuesList);
    }

    return selectChoiceDiv;
}

/**
 * Ajax call to list the contents of a remote directory, for building a dynamic drop-down menu.
 * @param {choiceDir} choiceDir, the choiceDir object returned by the GP server.
 *
 * @returns a choiceInfo object
 */
function reloadChoiceDiv(selectChoiceDiv, choiceInfoIn, paramDetails, parameterName, groupId, enableBatch, initialValuesList) {
    var protocolLessHref = choiceInfoIn.href;
    var arr = protocolLessHref.split("/");
    if (arr[0] != window.location.protocol){
    	protocolLessHref = protocolLessHref.replace(arr[0], window.location.protocol);
    }
	
	
	$.getJSON( protocolLessHref,
        function( choiceInfo ) {
            if (window.console) {
                console.log("drop-down loaded from: " + choiceInfo.href);
                console.log("status: " + JSON.stringify(choiceInfo.status, null, 2));
            }
            $(selectChoiceDiv).empty();
            buildChoiceDiv(selectChoiceDiv, choiceInfo, paramDetails, parameterName, groupId, enableBatch, initialValuesList);

            //if it's a custom value then do the same as a send to parameter
            var customChoices=getCustomChoices(choiceInfo, initialValuesList);
            if (customChoices && customChoices.length>0) {
                // only if the first item in the list is not the empty string
                if (customChoices[0] != "") {
                    for (i=0; i < customChoices.length; i++){
                        setInputField(parameterName, customChoices[i], groupId);
                    }
                    
                }
            }
        }
    );
}

