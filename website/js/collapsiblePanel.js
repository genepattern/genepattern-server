                             
	function toggleVisibility(id) {
	    var element = document.getElementById(id + "_panel");
	    if(element.style.display=="none")
	    {
	        showCategory(id);
	    }
	    else
	    {
	        hideCategory(id);
	    }
	}
	
	function hideCategory( id ) {
	    var tableElement = document.getElementById(id + "_panel");
	    var downArrow = document.getElementById(id + "_expanded_img");
	    var rightArrow = document.getElementById(id + "_collapsed_img");	
	        
	    tableElement.style.display="none";
	    downArrow.style.display ="none";
	    rightArrow.style.display ="inline"	    
	}
	
	function showCategory( id ) {
	    var tableElement = document.getElementById(id + "_panel");
	    var downArrow = document.getElementById(id + "_expanded_img");
	    var rightArrow = document.getElementById(id + "_collapsed_img");	
	        
	    tableElement.style.display="block";
	    downArrow.style.display ="inline";
	    rightArrow.style.display ="none"
	}
	
	function openAll() {
      var panels = document.getElementsByClassName("category_panel");
      for(var i=0; i<panels.length; i++) {
        var fullId = panels[i].id;
        var index = fullId.lastIndexOf('_');
        var catId = fullId.substring(0, index);
        showCategory(catId);
      }
	}
	
	function closeAll() {
      var panels = document.getElementsByClassName("category_panel");
      for(var i=0; i<panels.length; i++) {
        var fullId = panels[i].id;
        var index = fullId.lastIndexOf('_');
        var catId = fullId.substring(0, index);
        hideCategory(catId);
      }
      
      function chooserModeChanged() {	  
        var radioPanel = document.getElementById("viewchoiceRadioPanel");
        var radioButtons = radioPanel.getElementsByTagName("input");
        for(var i=0; i<radioButtons.length; i++) {
          if(radioButtons[i].type == "radio") {
            var panelId  = "module_table_" + radioButtons[i].value;
            var panel = document.getElementById(panelId);
            if(radioButtons[i].checked && panel != null) {
              Element.show(panel);
            }
            else {
              Element.hide(panel);
            }
        }
      }
    }
    
    function toggleExpansionState( categoryIdentifier)
    {
      var id= "expansion_state_" + categoryIdentifier;
      var hiddenField = document.getElementById(id);
      hiddenField.value = (hiddenField.value == "true" ? "false" : "true");
    }
	
	}